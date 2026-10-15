package gui;

import upgrades.Rarity;

/**
    The sounds of the Gamble spin, played with the SoundMixer:
    - music (assets/musordrop.wav) that starts with the spin. Its drop hits at
      MUSIC_DROP_NANOS, and Spin.SPIN_NANOS is the same, so the reel stops on the drop
    - a short click for every card that passes the marker
    - a chime when the result is revealed (higher and longer for rarer results,
      a low buzz for cursed), only if the music is missing, so it doesn't clash with the drop

    The clicks and chimes are made from sine waves, so they need no files.
*/
class SpinSound {
    public static final long MUSIC_DROP_NANOS = 5_930_000_000L;
    private static final String MUSIC_FILE = "assets/musordrop.wav";
    private static final float EFFECT_VOLUME = 0.35f;
    private static final float MUSIC_VOLUME = 0.9f;

    private final SoundMixer mixer;
    private final float[] tick = tone(1900, 0.012, 300, false);
    private final float[] music = SoundMixer.loadWav(MUSIC_FILE);

    SpinSound(SoundMixer mixer) {
        this.mixer = mixer;
    }

    // Plays the music from the start, called when a spin starts
    void startSpin() {
        mixer.playMusic(music, MUSIC_VOLUME);
    }

    void tick() {
        mixer.play(tick, EFFECT_VOLUME);
    }

    void reveal(Rarity rarity) {
        if (music != null) {
            return; // the drop is the reveal sound
        }
        switch (rarity) {
            case COMMON -> mixer.play(notes(0.12, 660, 880), EFFECT_VOLUME);
            case RARE -> mixer.play(notes(0.12, 660, 880, 1100), EFFECT_VOLUME);
            case LEGENDARY -> mixer.play(notes(0.11, 660, 880, 1100, 1320, 1760), EFFECT_VOLUME);
            case CURSED -> mixer.play(tone(110, 0.45, 5, true), EFFECT_VOLUME);
        }
    }

    // Pausing the game pauses the music, so it stays in time with the spin
    void setPaused(boolean paused) {
        mixer.setMusicPaused(paused);
    }

    // Several tones one after another, each lasting noteSeconds, the last one rings longer
    private static float[] notes(double noteSeconds, double... frequencies) {
        float[][] parts = new float[frequencies.length][];
        int length = 0;
        for (int i = 0; i < frequencies.length; ++i) {
            double seconds = i == frequencies.length - 1 ? noteSeconds * 3 : noteSeconds;
            parts[i] = tone(frequencies[i], seconds, 8, false);
            length += parts[i].length;
        }
        float[] all = new float[length];
        int offset = 0;
        for (float[] part : parts) {
            System.arraycopy(part, 0, all, offset, part.length);
            offset += part.length;
        }
        return all;
    }

    // A stereo tone that fades out, decay is how fast. Square waves sound harsher (for cursed)
    private static float[] tone(double frequency, double seconds, double decay, boolean square) {
        int frames = (int) (seconds * SoundMixer.SAMPLE_RATE);
        float[] samples = new float[frames * 2];
        for (int i = 0; i < frames; ++i) {
            double time = i / SoundMixer.SAMPLE_RATE;
            double wave = Math.sin(2 * Math.PI * frequency * time);
            if (square) {
                wave = Math.signum(wave) * 0.5;
            }
            float sample = (float) (wave * Math.exp(-decay * time));
            samples[i * 2] = sample; // left
            samples[i * 2 + 1] = sample; // right
        }
        return samples;
    }
}
