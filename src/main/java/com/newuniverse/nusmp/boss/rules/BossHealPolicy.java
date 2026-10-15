package com.newuniverse.nusmp.boss.rules;

/**
 * Why: the pure half of "a boss does not heal in a fight". The owner asked for no second wind and no healing loops; the one deliberate
 * exception is the scripted refill at the Gates (PhaseRefill), which sets health directly and never goes through a heal call, so
 * the rule can be stated with three booleans and a clock: a boss that is a boss, with the strip switch on, in combat, outside a
 * whitelisted scope, gets 0 from any heal. Kept free of Minecraft types so the combat clock (a target, or hurt in the last 200 ticks) is
 * tested by itself; the glue ({@link BossHealGuard}) reads the entity and calls this.
 */
public final class BossHealPolicy {
    private BossHealPolicy() {}

    /** Ticks after the last hit a boss is still "in combat" for the purpose of healing (ten seconds). */
    public static final int COMBAT_TICKS = 200;

    /**
     * Is the boss in combat?
     * @param hasTarget      it has a live attack target
     * @param tickCount      its age in ticks
     * @param lastHurtByMob  the age (in ticks) at which something last hurt it, 0 = never
     */
    public static boolean inCombat(boolean hasTarget, int tickCount, int lastHurtByMob) {
        if (hasTarget) return true;
        return lastHurtByMob > 0 && tickCount - lastHurtByMob < COMBAT_TICKS;
    }

    /** True when a heal on this entity must be dropped. 'allowedScope' is true inside {@link BossHealGuard#allow}. */
    public static boolean blocksHeal(boolean strip, boolean isBoss, boolean inCombat, boolean allowedScope) {
        return strip && isBoss && inCombat && !allowedScope;
    }
}
