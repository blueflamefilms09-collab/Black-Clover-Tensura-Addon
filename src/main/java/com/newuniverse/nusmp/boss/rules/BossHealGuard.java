package com.newuniverse.nusmp.boss.rules;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Why: the glue of {@link BossHealPolicy}. A boss in combat gets nothing from a heal: not the framework's passive regeneration, not
 * Vanica's blood drain, not Lucius's soul drain and purge, not a mod's potion. Two doors lead to health: {@code LivingEntity.heal}
 * (caught by the NeoForge heal event, see {@link BossRulesHooks}) and a direct {@code setHealth} upward (caught in
 * {@code BossBudget.setHealth}, because the budgeted bosses all route through it). Both ask {@link #blocks}. The one scripted refill
 * (PhaseRefill) wraps its write in the budget's bypass, which is checked before this guard; a boss's own reset when a fight is abandoned
 * wraps in {@link #allow}. Why a ThreadLocal depth and not a boolean field: the allowance must not leak to another entity's heal
 * on the same thread, and nesting must unwind in a finally.
 */
public final class BossHealGuard {
    private BossHealGuard() {}

    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    /** Runs 'body' with boss heals allowed (a reset to full after an abandoned fight). Nests; always released. */
    public static void allow(Runnable body) {
        int[] d = DEPTH.get();
        d[0]++;
        try { body.run(); } finally { d[0]--; }
    }

    /** True when a heal on 'e' must be dropped right now. */
    public static boolean blocks(LivingEntity e) {
        if (e == null || e.level() == null || e.level().isClientSide) return false;
        if (!BossRulesConfig.stripRegeneration()) return false;
        if (DEPTH.get()[0] > 0 || !BossIdentity.isBoss(e)) return false;
        boolean target = e instanceof Mob m && m.getTarget() != null && m.getTarget().isAlive();
        return BossHealPolicy.blocksHeal(true, true, BossHealPolicy.inCombat(target, e.tickCount, e.getLastHurtByMobTimestamp()), false);
    }

    /** The reverse, for announcements: may this boss restore health right now? */
    public static boolean mayHeal(LivingEntity e) { return !blocks(e); }
}
