package gui;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
    The effects of a hard drop:
    - the screen shakes, harder the further the piece fell
    - a speed trail streaks down each column of the piece
    - the landed blocks flash white
    - a shockwave line spreads out sideways from where it hit
    - particles burst out from under the piece and fall back down

    Everything is in pixels and calculated from the time since the drop, so nothing
    has to be updated every frame. The game thread starts impacts and the GUI thread
    draws them, so all methods are synchronized.
*/
class DropImpact {
    private static final long LIFE_NANOS = 700_000_000L; // after this an impact is removed

    private static final long SHAKE_NANOS = 200_000_000L;
    private static final double SHAKE_PIXELS = 9; // for a drop from the top of the board
    private static final double SHAKE_MIN_PIXELS = 2; // even for a short drop
    private static final int FULL_STRENGTH_ROWS = 14; // drops this long shake the most

    private static final long TRAIL_NANOS = 220_000_000L;
    private static final int TRAIL_ALPHA = 170;

    private static final long FLASH_NANOS = 140_000_000L;
    private static final int FLASH_ALPHA = 220;

    private static final long WAVE_NANOS = 260_000_000L;
    private static final int WAVE_SPREAD = 150; // pixels it grows on each side
    private static final int WAVE_HEIGHT = 3;

    private static final int PARTICLES_PER_BLOCK = 7;
    private static final double GRAVITY = 1400; // pixels per second squared
    private static final double MIN_PARTICLE_LIFE = 0.35; // seconds
    private static final double MAX_PARTICLE_LIFE = 0.65;

    private final Random random = new Random();
    private final List<Impact> impacts = new ArrayList<>();

    // One hard drop
    private static class Impact {
        final long startNanos = System.nanoTime();
        final List<int[]> from; // top left of each block before the drop, in pixels
        final List<int[]> to; // and after
        final int blockSize;
        final Color color;
        final double strength; // 0 to 1, how far it fell
        final List<Particle> particles = new ArrayList<>();

        Impact(List<int[]> from, List<int[]> to, int blockSize, Color color, double strength) {
            this.from = from;
            this.to = to;
            this.blockSize = blockSize;
            this.color = color;
            this.strength = strength;
        }

        double seconds(long now) {
            return (now - startNanos) / 1_000_000_000.0;
        }

        long nanos(long now) {
            return now - startNanos;
        }
    }

    // Starts at (x, y) with a speed and flies in an arc, nothing is updated per frame
    private static class Particle {
        final double x;
        final double y;
        final double speedX; // pixels per second
        final double speedY; // negative is up
        final double life; // seconds
        final int size;
        final Color color;

        Particle(double x, double y, double speedX, double speedY, double life, int size,
                Color color) {
            this.x = x;
            this.y = y;
            this.speedX = speedX;
            this.speedY = speedY;
            this.life = life;
            this.size = size;
            this.color = color;
        }
    }

    // from and to are the top left corners of the piece's blocks before and after the drop
    synchronized void start(List<int[]> from, List<int[]> to, int blockSize, Color color) {
        int rows = (to.get(0)[1] - from.get(0)[1]) / blockSize;
        double strength = Math.min(1.0, (double) rows / FULL_STRENGTH_ROWS);
        Impact impact = new Impact(from, to, blockSize, color, strength);
        addParticles(impact);
        impacts.add(impact);
    }

    // Particles come out of the bottom of every block that has no piece block under it
    private void addParticles(Impact impact) {
        for (int[] block : impact.to) {
            if (contains(impact.to, block[0], block[1] + impact.blockSize)) {
                continue;
            }
            double bottom = block[1] + impact.blockSize;
            int count = (int) Math.round(PARTICLES_PER_BLOCK * (0.5 + impact.strength));
            for (int i = 0; i < count; ++i) {
                double x = block[0] + random.nextDouble() * impact.blockSize;
                double speedX = (random.nextDouble() - 0.5) * 360;
                double speedY = -(120 + random.nextDouble() * 320) * (0.6 + impact.strength);
                double life = MIN_PARTICLE_LIFE
                        + random.nextDouble() * (MAX_PARTICLE_LIFE - MIN_PARTICLE_LIFE);
                int size = 3 + random.nextInt(4);
                Color color = random.nextBoolean() ? impact.color : Color.white;
                impact.particles.add(new Particle(x, bottom, speedX, speedY, life, size, color));
            }
        }
    }

    private static boolean contains(List<int[]> blocks, int x, int y) {
        for (int[] block : blocks) {
            if (block[0] == x && block[1] == y) {
                return true;
            }
        }
        return false;
    }

    // How far to move the whole board this frame, {x, y} in pixels
    synchronized int[] shakeOffset() {
        long now = System.nanoTime();
        impacts.removeIf(impact -> impact.nanos(now) > LIFE_NANOS);
        double amount = 0;
        for (Impact impact : impacts) {
            double left = 1 - (double) impact.nanos(now) / SHAKE_NANOS;
            if (left > 0) {
                double pixels = SHAKE_MIN_PIXELS + (SHAKE_PIXELS - SHAKE_MIN_PIXELS) * impact.strength;
                amount = Math.max(amount, pixels * left * left); // fades out fast
            }
        }
        if (amount < 0.5) {
            return new int[]{0, 0};
        }
        // mostly down, like the board took the hit
        int x = (int) Math.round((random.nextDouble() - 0.5) * amount);
        int y = (int) Math.round(random.nextDouble() * amount);
        return new int[]{x, y};
    }

    // The trails, under the blocks
    synchronized void drawTrails(Graphics2D g2) {
        long now = System.nanoTime();
        for (Impact impact : impacts) {
            double left = 1 - (double) impact.nanos(now) / TRAIL_NANOS;
            if (left <= 0) {
                continue;
            }
            // one streak per column, from the top of the piece before to the top after
            for (int[] block : impact.to) {
                int top = topOf(impact.from, block[0]);
                int bottom = topOf(impact.to, block[0]);
                if (block[1] != bottom || bottom <= top) {
                    continue; // only once per column
                }
                int alpha = (int) (TRAIL_ALPHA * left);
                g2.setPaint(new GradientPaint(0, top, withAlpha(impact.color, 0),
                        0, bottom, withAlpha(impact.color, alpha)));
                // narrower than a block, and it gets thinner as it fades
                int width = (int) (impact.blockSize * (0.4 + 0.5 * left));
                int x = block[0] + (impact.blockSize - width) / 2;
                g2.fillRect(x, top, width, bottom - top);
            }
        }
    }

    // The flash, shockwave and particles, over the blocks
    synchronized void drawEffects(Graphics2D g2) {
        long now = System.nanoTime();
        for (Impact impact : impacts) {
            drawFlash(g2, impact, now);
            drawWave(g2, impact, now);
            drawParticles(g2, impact, now);
        }
    }

    private void drawFlash(Graphics2D g2, Impact impact, long now) {
        double left = 1 - (double) impact.nanos(now) / FLASH_NANOS;
        if (left <= 0) {
            return;
        }
        g2.setColor(withAlpha(Color.white, (int) (FLASH_ALPHA * left)));
        for (int[] block : impact.to) {
            g2.fillRect(block[0], block[1], impact.blockSize, impact.blockSize);
        }
    }

    // A thin white line along the bottom of the piece that grows sideways and fades
    private void drawWave(Graphics2D g2, Impact impact, long now) {
        double progress = (double) impact.nanos(now) / WAVE_NANOS;
        if (progress >= 1) {
            return;
        }
        int left = Integer.MAX_VALUE;
        int right = Integer.MIN_VALUE;
        int bottom = Integer.MIN_VALUE;
        for (int[] block : impact.to) {
            left = Math.min(left, block[0]);
            right = Math.max(right, block[0] + impact.blockSize);
            bottom = Math.max(bottom, block[1] + impact.blockSize);
        }
        double eased = 1 - Math.pow(1 - progress, 3);
        int spread = (int) (WAVE_SPREAD * eased * (0.5 + impact.strength / 2));
        g2.setColor(withAlpha(Color.white, (int) (200 * (1 - progress))));
        g2.fillRect(left - spread, bottom - WAVE_HEIGHT / 2, right - left + 2 * spread,
                WAVE_HEIGHT);
    }

    private void drawParticles(Graphics2D g2, Impact impact, long now) {
        double t = impact.seconds(now);
        for (Particle particle : impact.particles) {
            if (t >= particle.life) {
                continue;
            }
            double x = particle.x + particle.speedX * t;
            double y = particle.y + particle.speedY * t + GRAVITY * t * t / 2;
            double left = 1 - t / particle.life;
            int size = Math.max(1, (int) Math.round(particle.size * (0.4 + 0.6 * left)));
            g2.setColor(withAlpha(particle.color, (int) (255 * left)));
            g2.fillRect((int) x - size / 2, (int) y - size / 2, size, size);
        }
    }

    // Highest pixel y of the blocks in the column at pixel x
    private static int topOf(List<int[]> blocks, int x) {
        int top = Integer.MAX_VALUE;
        for (int[] block : blocks) {
            if (block[0] == x) {
                top = Math.min(top, block[1]);
            }
        }
        return top;
    }

    private static Color withAlpha(Color color, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamped);
    }
}
