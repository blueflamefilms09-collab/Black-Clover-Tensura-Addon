package com.newuniverse.nusmp.labyrinth.mega.enc;

/**
 * Why: the Dungeon Master needs a number for "how loud has this party been lately", and the number has to forget. A level that only rose would
 * make every long expedition end in a siege; a level that reset after every roll would ignore the fight that ended three seconds ago. So it is
 * an accumulator that decays exponentially: each event adds its amount (faded by the distance between the listener and the event), and the
 * level loses a fixed fraction of itself every second ({@code noiseDecay}: 0.2 means a fifth, a half-life of about three seconds). It is
 * evaluated lazily from the game time of the last touch, so nothing ticks: a meter that nobody reads costs one object and no work. One meter per
 * player; the party's reading is the maximum over its members (four players hearing the same door slam are not four door slams). The reading
 * becomes a d20 modifier through {@link #mod}. Pure (no Minecraft types).
 */
public final class NoiseMeter {
    /** The level never exceeds this, so a very long fight cannot push the scale (and the cooldown) past anything a table was written for. */
    public static final double CEILING = 40.0;

    private double level;
    private long stamp = Long.MIN_VALUE;

    /** The decayed level at {@code now} (game ticks) without changing it. */
    public double peek(long now, double decayPerSecond) {
        if (stamp == Long.MIN_VALUE || now <= stamp) return level;
        return decayed(level, now - stamp, decayPerSecond);
    }

    /** The decayed level at {@code now}; the meter is brought up to date. */
    public double level(long now, double decayPerSecond) {
        settle(now, decayPerSecond);
        return level;
    }

    /** Adds {@code amount} (already faded for distance) at {@code now}. Negative and non-finite amounts add nothing. */
    public void add(double amount, long now, double decayPerSecond) {
        settle(now, decayPerSecond);
        if (amount > 0.0 && Double.isFinite(amount)) level = Math.min(CEILING, level + amount);
    }

    public void clear() {
        level = 0.0;
        stamp = Long.MIN_VALUE;
    }

    private void settle(long now, double decayPerSecond) {
        if (stamp == Long.MIN_VALUE) {
            stamp = now;
            return;
        }
        if (now > stamp) {
            level = decayed(level, now - stamp, decayPerSecond);
            stamp = now;
        }
    }

    /** {@code level} after {@code ticks} game ticks, losing the fraction {@code decayPerSecond} (0..1) of itself every second. */
    public static double decayed(double level, long ticks, double decayPerSecond) {
        if (ticks <= 0 || level <= 0.0) return Math.max(0.0, level);
        double keep = 1.0 - Math.max(0.0, Math.min(1.0, decayPerSecond));
        if (keep <= 0.0) return 0.0;
        double v = level * Math.pow(keep, ticks / 20.0);
        return v < 1e-4 ? 0.0 : v;
    }

    /**
     * How much of an event's amount reaches a listener {@code distance} blocks away when it carries {@code earshot} blocks: full at the event,
     * quadratic fade, nothing at the earshot and beyond (a sound is mostly still close to its source and then drops away).
     */
    public static double falloff(double distance, double earshot) {
        if (earshot <= 0.0 || !(distance < earshot)) return 0.0;
        double f = 1.0 - Math.max(0.0, distance) / earshot;
        return f * f;
    }

    /**
     * The d20 modifier of a noise reading: nothing below 2.5, then one point per 2.5 up to {@code cap}. A steady sprint (about 3) is +1, a
     * fight (10 and more) is +4.
     */
    public static int mod(double reading, int cap) {
        if (!(reading >= 2.5)) return 0;
        return (int) Math.max(0, Math.min(Math.max(0, cap), Math.floor(reading / 2.5)));
    }
}
