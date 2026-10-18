package com.newuniverse.nusmp.heroweapon.rules;

/**
 * Why: the shock of a Holy Weapon that rejects another weapon must hurt a little and NEVER kill (the owner's rule is a restriction, not an execution). How much
 * damage the shock does at a given health is the one number a mistake would turn into a death, so it is pure and a test pins it. How often the shock may repeat
 * (it must not be a way to stun-lock a player who holds the attack button) is {@link Spacing}'s question.
 */
public final class ShockRules {
    private ShockRules() {}

    /** The shock damage: the configured amount, but never more than leaves the victim at half a heart (health 1), never negative. */
    public static float damage(float configured, float health) {
        float room = Math.max(0.0f, health - 1.0f);
        return Math.max(0.0f, Math.min(configured, room));
    }
}
