package com.newuniverse.nusmp.labyrinth.mega.enc;

/**
 * Why: "noise" is the Dungeon Master's way of knowing how loudly a party is playing, and the numbers that say how loud each thing is belong in
 * one readable table, not scattered over event handlers. Each kind has an {@code amount} (what it adds to a listener's noise level at point
 * blank), an {@code earshot} (how far, in blocks, it carries: the amount fades to nothing there) and a flag for whether a wandering boss can
 * turn its head to it ({@code burst}: only sudden, loud things, so a sprinting player is heard when close but is not a beacon across the whole
 * sector). The scale is calibrated so a steady sprint settles near 3, a brawl climbs past 10 within seconds and a blast spikes by 6, which the d20
 * modifier ({@link NoiseMeter#mod}) then reads as +1, +4 and +2. {@code byGameEvent} maps the vanilla game-event names (the registry path of a
 * {@code GameEvent}) to a kind so the game glue holds no table of its own. Pure (no Minecraft types).
 */
public enum NoiseKind {
    /** Not a noise at all (the game event is not in the table). */
    SILENT(0.0, 0, false),
    STEP_WALK(0.05, 12, false),
    STEP_SPRINT(0.20, 20, false),
    LAND(0.15, 16, false),
    SPLASH(0.30, 16, false),
    DOOR(0.40, 18, false),
    CONTAINER(0.50, 14, false),
    SHOT(0.60, 20, false),
    BLOCK(1.00, 24, true),
    IMPACT(1.00, 24, true),
    /** A creature was hurt in a fight that involves a player. */
    HURT(2.00, 32, true),
    DEATH(1.50, 28, true),
    LIGHTNING(3.00, 48, true),
    EXPLOSION(6.00, 64, true),
    SHRIEK(4.00, 48, true),
    /** A boss roar or a Vital Strike: no vanilla game event, the systems that know call it. */
    ROAR(4.00, 48, true);

    private final double amount;
    private final int earshot;
    private final boolean burst;

    NoiseKind(double amount, int earshot, boolean burst) {
        this.amount = amount;
        this.earshot = earshot;
        this.burst = burst;
    }

    /** What it adds to the noise level of a listener standing next to it. */
    public double amount() { return amount; }

    /** The distance in blocks at which it can no longer be heard. */
    public int earshot() { return earshot; }

    /** Can a wandering boss turn towards it from far away? */
    public boolean burst() { return burst; }

    /**
     * Does only a PLAYER make this noise (a footstep, a block placed, a door opened) or does anyone (a blast, a death, a roar)? A zombie's
     * footsteps are not the party's noise: they would raise the encounter rate just because the dungeon is already awake.
     */
    public boolean playerOnly() {
        return switch (this) {
            case STEP_WALK, STEP_SPRINT, LAND, SPLASH, DOOR, CONTAINER, SHOT, BLOCK -> true;
            default -> false;
        };
    }

    /**
     * The kind for a vanilla game event, by its registry path ({@code "step"}, {@code "block_destroy"} ...); {@link #SILENT} when the event is
     * not a noise. {@code sprinting} only matters for a step. A name with a namespace ({@code minecraft:step}) is accepted too.
     */
    public static NoiseKind byGameEvent(String name, boolean sprinting) {
        if (name == null) return SILENT;
        String n = name.startsWith("minecraft:") ? name.substring(10) : name;
        return switch (n) {
            case "step" -> sprinting ? STEP_SPRINT : STEP_WALK;
            case "hit_ground" -> LAND;
            case "splash", "swim" -> SPLASH;
            case "block_open", "block_close", "block_activate", "block_deactivate" -> DOOR;
            case "container_open", "container_close" -> CONTAINER;
            case "projectile_shoot" -> SHOT;
            case "block_destroy", "block_place" -> BLOCK;
            case "projectile_land" -> IMPACT;
            case "entity_die" -> DEATH;
            case "lightning_strike" -> LIGHTNING;
            case "explode" -> EXPLOSION;
            case "shriek" -> SHRIEK;
            default -> SILENT;
        };
    }
}
