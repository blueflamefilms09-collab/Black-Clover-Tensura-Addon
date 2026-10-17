package com.newuniverse.nusmp.arc.midnight;

/**
 * Why: this is Fana's ultimate, taken from the story. Under the Evil Eye's hatred she gathered her mana into a sphere with Salamander and meant to take
 * everything with her; it was stopped by two people who reached her, and when the Third Eye on her forehead went out the girl under the elf woke. In the
 * fight the sphere grows for a fixed countdown and the party may break it by taking a share of her health before it ends (a plain race the whole party can
 * see and join); if they do the Eye shatters, the sphere dies harmlessly and she is left open; if they do not it detonates over a wide circle with a
 * falloff that rewards standing far and behind cover. This pure class is the countdown, the growing radius, the outcome and the falloff, so the rules are
 * proven by tests and the boss class only draws and applies them.
 */
public final class EvilHeart {

    public enum Phase { IDLE, CHARGING, BROKEN, DETONATE }

    /** The sphere starts this wide and grows to {@link #MAX_RADIUS}; inside {@link #CORE} of the final radius the blast is whole, at the rim it is {@link #RIM} of whole. */
    public static final double START_RADIUS = 3.0, MAX_RADIUS = 17.0, CORE = 0.35, RIM = 0.4;

    private final int total;
    private int elapsed;
    private Phase phase = Phase.IDLE;

    public EvilHeart(int totalTicks) { this.total = Math.max(1, totalTicks); }

    /** Starts the countdown (a second call restarts it). */
    public void begin() {
        elapsed = 0;
        phase = Phase.CHARGING;
    }

    /**
     * One tick. {@code broken} says the party has taken enough of her health. Returns the phase after the tick: BROKEN the moment it is broken, DETONATE when
     * the countdown ends unbroken, otherwise CHARGING. Once BROKEN or DETONATE the phase stays until {@link #reset}; IDLE stays IDLE.
     */
    public Phase tick(boolean broken) {
        if (phase != Phase.CHARGING) return phase;
        if (broken) {
            phase = Phase.BROKEN;
            return phase;
        }
        elapsed++;
        if (elapsed >= total) phase = Phase.DETONATE;
        return phase;
    }

    public Phase phase() { return phase; }

    public boolean charging() { return phase == Phase.CHARGING; }

    public int left() { return Math.max(0, total - elapsed); }

    public int total() { return total; }

    /** 0..1 of the countdown that has passed. */
    public float progress() { return Math.min(1f, elapsed / (float) total); }

    /** The sphere's radius now (blocks): it grows evenly from the start radius to the maximum. */
    public double radius() { return radiusAt(progress()); }

    public static double radiusAt(double progress) { return MidnightMath.lerp(START_RADIUS, MAX_RADIUS, progress); }

    public void reset() {
        elapsed = 0;
        phase = Phase.IDLE;
    }

    /**
     * The share of the blast a foe at {@code distance} from the centre takes: the whole blast inside the core, falling in a straight line to {@link #RIM} at
     * the edge of {@code radius}, nothing beyond it.
     */
    public static double falloff(double distance, double radius) {
        if (radius <= 0 || distance > radius) return 0.0;
        double core = radius * CORE;
        if (distance <= core) return 1.0;
        return MidnightMath.lerp(1.0, RIM, (distance - core) / (radius - core));
    }

    /** The share of the blast that gets through shelter (a wall between the foe and the sphere) and a guard; never less than a fifth. */
    public static double shelter(boolean walled, boolean blocking, boolean antiMagic) {
        double f = MidnightMath.guardFactor(blocking, antiMagic, true) * (walled ? 0.45 : 1.0);
        return Math.max(0.2, f);
    }
}
