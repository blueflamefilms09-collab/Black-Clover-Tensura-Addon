package com.newuniverse.nusmp.dungeon.sentient;

/**
 * 1.35.0: the moves and the second wind of the Dungeon's Juggernaut, as pure numbers (tested). It has two things besides its grown body:
 * the Antibody Pulse (a ring marked on the floor, then a shockwave that only catches those standing on the ground within it: jump it) and the
 * Immune Surge (once, at a quarter of its health, the immune system pours everything into it: the health bar refills, it cannot be hurt for
 * two seconds and every move comes round faster with a bigger mark). Nothing here is unavoidable: the ring is shown for at least a second.
 */
public final class JuggernautMoves {
    private JuggernautMoves() {}

    /** The ring of the Antibody Pulse: blocks, ticks of warning (longer when surged: a bigger mark), and the share of a victim's health it takes. */
    public static final double PULSE_RADIUS = 5.0, SURGE_EXTRA_RADIUS = 1.5, TRIGGER_DISTANCE = 9.0;
    public static final int PULSE_WINDUP = 30, SURGE_EXTRA_WINDUP = 8, MIN_WARNING = 24;
    public static final float PULSE_SHARE = 0.20f, SURGE_PULSE_SHARE = 0.26f;
    /** Ticks between pulses, and the share of health at which the Immune Surge comes. */
    public static final int PULSE_COOLDOWN = 200, SURGE_COOLDOWN = 130;
    public static final float SURGE_AT = 0.25f;
    /** The Surge: ticks of refill, and ticks of Resistance V (cannot be hurt) laid on it. */
    public static final int SURGE_TICKS = 40, SURGE_GUARD = 60;

    public static double radius(boolean surged) { return PULSE_RADIUS + (surged ? SURGE_EXTRA_RADIUS : 0.0); }

    public static int windup(boolean surged) { return PULSE_WINDUP + (surged ? SURGE_EXTRA_WINDUP : 0); }

    public static int cooldown(boolean surged) { return surged ? SURGE_COOLDOWN : PULSE_COOLDOWN; }

    public static float share(boolean surged) { return surged ? SURGE_PULSE_SHARE : PULSE_SHARE; }

    /** Is the Surge due: health at or under a quarter, not yet used, and the Juggernaut alive. */
    public static boolean surgeDue(float health, float max, boolean used) {
        return !used && max > 0f && health > 0f && health / max <= SURGE_AT;
    }

    /** Should a pulse be started: off cooldown, no pulse running, and a target near enough for the ring to matter. */
    public static boolean pulseDue(long now, long readyAt, boolean running, double nearestTarget) {
        return !running && now >= readyAt && nearestTarget <= TRIGGER_DISTANCE;
    }

    /** Does the shockwave catch a body: only one standing on the floor, inside the ring (plus half its width). */
    public static boolean catches(boolean onGround, double dist, double radius, double halfWidth, double dy) {
        return onGround && dist <= radius + halfWidth && Math.abs(dy) <= 3.0;
    }
}
