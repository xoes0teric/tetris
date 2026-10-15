package gui;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
    Plays all the game's sounds: one music track and any number of sound effects
    at the same time, mixed here in a background thread into ONE output, which is only
    open while something is playing:
    - on Linux, the pacat program. Java can't talk to PipeWire/PulseAudio, only to the
      sound card directly, which picks the wrong device (HDMI) or fails when another
      program already uses the speakers. pacat goes through the sound server like any app
    - everywhere else (or without pacat), Java's default audio line

    Sounds are stereo samples between -1 and 1: left, right, left, right, ...
    Load them with loadWav, or make them in code (see SpinSound.tone).

    Sound is optional: if the audio device is missing, everything stays silent and
    the game keeps working.
*/
class SoundMixer {
    static final float SAMPLE_RATE = 44100f;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);
    private static final int CHUNK_FRAMES = 441; // 10 ms mixed at a time
    private static final int LINE_BUFFER_FRAMES = 4096; // small, so effects aren't late
    private static final int PACAT_LATENCY_MS = 60; // same idea for pacat
    // The mixer never gets further ahead of real time than this. Without it, pipes and
    // buffers can hold almost half a second, and effects would come late
    private static final long MAX_LEAD_NANOS = 40_000_000L;

    // Everything below is shared with the mixing thread, so it is only used inside
    // synchronized methods
    private final List<Playing> effects = new ArrayList<>();
    private Playing music; // null when no music is playing
    private boolean musicPaused = false;
    private Thread mixer;
    private boolean stopRequested = false;
    private boolean broken = false; // set once the audio device failed, then never tried again

    // A sound that is playing and how far it is, in frames (one left + right sample)
    private static class Playing {
        final float[] samples;
        final float volume;
        int frame = 0;

        Playing(float[] samples, float volume) {
            this.samples = samples;
            this.volume = volume;
        }

        boolean isDone() {
            return frame * 2 >= samples.length;
        }
    }

    // Plays a sound effect on top of everything else. Does nothing if sound is null
    synchronized void play(float[] sound, float volume) {
        if (sound == null) {
            return;
        }
        effects.add(new Playing(sound, volume));
        startMixer();
    }

    // Plays the music from the start, replacing music that is already playing
    synchronized void playMusic(float[] sound, float volume) {
        if (sound == null) {
            return;
        }
        music = new Playing(sound, volume);
        musicPaused = false;
        startMixer();
    }

    // Paused music keeps its place, effects keep playing
    synchronized void setMusicPaused(boolean paused) {
        musicPaused = paused;
    }

    // Stops all sounds right away
    synchronized void stopAll() {
        effects.clear();
        music = null;
        stopRequested = true;
    }

    // A WAV file as stereo samples in the mixer's format, null if it can't be read.
    // Paths are relative to the folder the game is started from (run.sh starts it in the repo)
    static float[] loadWav(String path) {
        try (AudioInputStream file = AudioSystem.getAudioInputStream(new File(path));
                AudioInputStream stream = AudioSystem.getAudioInputStream(FORMAT, file)) {
            byte[] bytes = stream.readAllBytes();
            float[] samples = new float[bytes.length / 2];
            for (int i = 0; i < samples.length; ++i) {
                short sample = (short) ((bytes[i * 2] & 0xff) | (bytes[i * 2 + 1] << 8));
                samples[i] = sample / (float) Short.MAX_VALUE;
            }
            return samples;
        } catch (Exception | LinkageError e) {
            return null;
        }
    }

    // ---- Mixing thread ----

    private void startMixer() {
        stopRequested = false;
        if (broken || mixer != null) {
            return;
        }
        mixer = new Thread(this::mixLoop, "sound-mixer");
        mixer.setDaemon(true); // never keeps the game open
        mixer.start();
    }

    // Opens the output, mixes and writes 10 ms at a time until nothing is left to play,
    // then closes it. If something new started meanwhile, opens it again
    private void mixLoop() {
        while (true) {
            Output output = openOutput();
            if (output == null) {
                giveUp();
                return;
            }
            byte[] bytes = new byte[CHUNK_FRAMES * FORMAT.getFrameSize()];
            long startNanos = System.nanoTime();
            long framesWritten = 0;
            try {
                while (mixNextChunk(bytes)) {
                    output.write(bytes);
                    framesWritten += CHUNK_FRAMES;
                    waitForRealTime(startNanos, framesWritten);
                }
            } catch (IOException e) {
                output.close();
                giveUp();
                return;
            }
            output.close();
            synchronized (this) {
                if (!hasWork()) {
                    mixer = null;
                    return;
                }
            }
        }
    }

    // Sleeps while the written sound is more than MAX_LEAD_NANOS ahead of the clock
    private static void waitForRealTime(long startNanos, long framesWritten) {
        long writtenNanos = (long) (framesWritten * 1_000_000_000.0 / SAMPLE_RATE);
        long ahead = writtenNanos - (System.nanoTime() - startNanos);
        if (ahead > MAX_LEAD_NANOS) {
            try {
                Thread.sleep((ahead - MAX_LEAD_NANOS) / 1_000_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Audio doesn't work here, stay silent from now on
    private synchronized void giveUp() {
        broken = true;
        mixer = null;
    }

    private boolean hasWork() {
        return !stopRequested && (music != null || !effects.isEmpty());
    }

    // Fills bytes with the next chunk, returns false when there is nothing left to play
    private synchronized boolean mixNextChunk(byte[] bytes) {
        if (!hasWork()) {
            return false;
        }
        for (int frame = 0; frame < CHUNK_FRAMES; ++frame) {
            float left = 0;
            float right = 0;
            if (music != null && !musicPaused && !music.isDone()) {
                left += music.samples[music.frame * 2] * music.volume;
                right += music.samples[music.frame * 2 + 1] * music.volume;
                ++music.frame;
            }
            for (Playing effect : effects) {
                if (!effect.isDone()) {
                    left += effect.samples[effect.frame * 2] * effect.volume;
                    right += effect.samples[effect.frame * 2 + 1] * effect.volume;
                    ++effect.frame;
                }
            }
            writeSample(bytes, frame * 4, left);
            writeSample(bytes, frame * 4 + 2, right);
        }
        effects.removeIf(Playing::isDone);
        if (music != null && music.isDone()) {
            music = null;
        }
        return true;
    }

    // 16 bit little endian, clipped so loud parts don't wrap around
    private static void writeSample(byte[] bytes, int index, float value) {
        float clipped = Math.max(-1f, Math.min(1f, value));
        short sample = (short) (clipped * Short.MAX_VALUE);
        bytes[index] = (byte) sample;
        bytes[index + 1] = (byte) (sample >> 8);
    }

    // ---- Outputs ----

    // Where the mixed sound goes
    private interface Output {
        void write(byte[] bytes) throws IOException;

        // Plays what is still buffered, then closes
        void close();
    }

    // pacat on Linux, else Java's default line, null if nothing works
    private static Output openOutput() {
        boolean linux = System.getProperty("os.name").toLowerCase().contains("linux");
        if (linux && isOnPath("pacat")) {
            try {
                return openPacat();
            } catch (IOException e) {
                // fall back to Java's line below
            }
        }
        try {
            return openJavaLine();
        } catch (Exception | LinkageError e) {
            return null;
        }
    }

    // Starts pacat reading raw samples from us, in the same format we mix
    private static Output openPacat() throws IOException {
        Process process = new ProcessBuilder("pacat", "--playback", "--raw",
                "--format=s16le", "--rate=" + (int) SAMPLE_RATE, "--channels=2",
                "--latency-msec=" + PACAT_LATENCY_MS, "--client-name=Tetris")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        OutputStream input = process.getOutputStream();
        return new Output() {
            @Override
            public void write(byte[] bytes) throws IOException {
                input.write(bytes);
                input.flush();
            }

            @Override
            public void close() {
                try {
                    input.close(); // pacat plays the rest and exits
                } catch (IOException e) {
                    process.destroy();
                }
            }
        };
    }

    private static Output openJavaLine() throws Exception {
        SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT);
        line.open(FORMAT, LINE_BUFFER_FRAMES * FORMAT.getFrameSize());
        line.start();
        return new Output() {
            @Override
            public void write(byte[] bytes) {
                line.write(bytes, 0, bytes.length);
            }

            @Override
            public void close() {
                line.drain();
                line.close();
            }
        };
    }

    private static boolean isOnPath(String program) {
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        for (String folder : path.split(File.pathSeparator)) {
            if (new File(folder, program).canExecute()) {
                return true;
            }
        }
        return false;
    }
}
