package com.newuniverse.nusmp.arc.midnight;

/**
 * Why: in the story both Patolli and Fana were beaten by their own magic sent back. Gauche's Mirror Magic returned Patolli's Light Shaft of Divine Punishment
 * and he fell; Asta's anti-magic sword returned Salamander's fireball. A boss whose signature attack can be turned back is a fight with a skill in it, so
 * the attack must say, in one place and in plain numbers, WHO may turn it and WHAT the turning costs the boss. This pure class holds that decision
 * (it is given facts about the foe, never the foe itself) so the beam, the fireball and the tests agree. The mod's own answers are used: a Full Reflection
 * mirror, a live mirror array, and the anti-magic of a demon sword or the Anti-Magic Lord.
 * <p>
 * A beam is instant, so a sword swing cannot meet it (an anti-magic holder merely has it cut by the guard rule, {@code MidnightMath.guardFactor}); a fireball
 * flies for a moment, so a swing can meet it, which is exactly how the story showed it.
 */
public final class ReflectRules {
    private ReflectRules() {}

    /** What turns an attack back. */
    public enum Kind { NONE, FULL_REFLECTION, MIRROR, ANTI_MAGIC_SWING }

    /** Ticks one player must wait before they can turn another attack back (a mirror is a skill, not a wall). */
    public static final int PLAYER_COOLDOWN = 100;

    /** How far from the player's body (blocks) a flying attack is caught by their mirror or their blade. */
    public static final double CATCH_RADIUS = 3.2;

    /**
     * What, if anything, turns the attack back.
     *
     * @param fullReflection the foe has a Full Reflection mirror up
     * @param mirrors        the foe has a live Mirror Magic array
     * @param antiMagicHeld  the foe holds a demon sword or the Anti-Magic Lord
     * @param swinging       the foe is mid-swing right now
     * @param projectile     the attack flies (a fireball) rather than lands at once (a beam)
     */
    public static Kind decide(boolean fullReflection, boolean mirrors, boolean antiMagicHeld, boolean swinging, boolean projectile) {
        if (fullReflection) return Kind.FULL_REFLECTION;
        if (mirrors) return Kind.MIRROR;
        if (projectile && antiMagicHeld && swinging) return Kind.ANTI_MAGIC_SWING;
        return Kind.NONE;
    }

    /** The damage (before the budget) an attack turned back deals to the boss that threw it, as a share of the boss's maximum health. */
    public static float backfireShare(Kind kind) {
        return switch (kind) {
            case FULL_REFLECTION -> 0.06f;
            case MIRROR -> 0.05f;
            case ANTI_MAGIC_SWING -> 0.04f;
            case NONE -> 0f;
        };
    }

    /** Ticks the boss is open after being struck by its own attack (a rule of the boss; the lockout lives in {@link StaggerClock}). */
    public static int openTicks(Kind kind) {
        return switch (kind) {
            case FULL_REFLECTION -> 70;
            case MIRROR -> 60;
            case ANTI_MAGIC_SWING -> 45;
            case NONE -> 0;
        };
    }
}
