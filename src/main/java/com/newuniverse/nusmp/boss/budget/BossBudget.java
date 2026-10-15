package com.newuniverse.nusmp.boss.budget;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The Minecraft-facing half of the budget: one per boss entity (a field), wrapping its {@link DamageBudget} and the bypass state.
 * Every method that can touch health is here so a boss class only forwards to it.
 */
public final class BossBudget {
    private final DamageBudget budget = new DamageBudget(0f);
    private int bypass;
    private boolean sized;

    public DamageBudget raw() { return budget; }

    /** True while the boss's own code (restore, load, the damage already counted by hurt) is changing the health. */
    public boolean bypassing() { return bypass > 0; }

    /** Runs 'body' with the budget bypassed (nests; always released). */
    public void bypass(Runnable body) {
        bypass++;
        try { body.run(); } finally { bypass--; }
    }

    private static boolean active() { return BossBudgetConfig.enabled(); }

    /** The cap of this boss now. */
    private static float capNow(LivingEntity e, BudgetedBoss b) {
        double scale = BudgetPolicy.phaseScale(b.budgetStage(), b.budgetFinalStage(), BossBudgetConfig.finalScale());
        return BudgetPolicy.capPerSecond(e.getMaxHealth(), BossBudgetConfig.fraction(), BossBudgetConfig.minimum(), scale);
    }

    /** From the boss's baseTick (server side only): keeps the cap current and restores the budget every 20 ticks. */
    public void tick(LivingEntity e, BudgetedBoss b) {
        if (e.level().isClientSide) return;
        float cap = capNow(e, b);
        if (!sized) {
            sized = true;
            budget.setCap(cap);
            budget.reset();
        } else if (cap != budget.cap()) {
            budget.setCap(cap);
        }
        budget.tick();
    }

    private boolean exempt(Entity attacker) {
        return attacker instanceof Player p && p.isCreative() && BossBudgetConfig.ignoreCreative();
    }

    /** The vanilla hurt, called with the amount the budget allowed. */
    @FunctionalInterface
    public interface Hit { boolean apply(float amount); }

    /**
     * From hurt(), on the final amount (after the boss's own mitigation and PowerCurve clamps, so it is only truncated, never reduced
     * twice): hands the allowed amount to 'vanillaHurt' (normally {@code a -> super.hurt(source, a)}) with the budget bypassed, so the
     * setHealth inside it is not counted a second time. Nothing left in the budget: false, nothing happens. A creative attacker's
     * blow (config) passes whole.
     */
    public boolean hurt(LivingEntity e, DamageSource source, float amount, Hit vanillaHurt) {
        if (e.level().isClientSide || !sized || !active() || bypass > 0 || !(amount > 0f) || exempt(source.getEntity())) {
            bypass++;                                                       // creative or unbudgeted: the whole blow, and its setHealth, pass
            try { return vanillaHurt.apply(amount); } finally { bypass--; }
        }
        float allowed = budget.allow(amount);
        if (allowed <= 0f) return false;
        bypass++;
        try { return vanillaHurt.apply(allowed); } finally { bypass--; }
    }

    /**
     * From setHealth(): the health to store. Heals, restores and loads pass; a fall (true damage, /kill's last step, any mod) is cut to
     * the budget. The fall inside hurt() was already counted there.
     */
    public float setHealth(LivingEntity e, float requested) {
        float current = e.getHealth();
        if (e.level() == null || e.level().isClientSide) return requested;
        if (bypass == 0 && requested > current && com.newuniverse.nusmp.boss.rules.BossHealGuard.blocks(e)) return current;     // 1.37.0: no second wind in combat; the refill and loads run bypassed and pass
        if (!sized || !active() || bypass > 0) return requested;
        if (!(requested < current) || requested >= e.getMaxHealth()) return requested;
        float drop = current - requested;
        return BudgetPolicy.settle(current, requested, budget.allow(drop));
    }

    /** One line for the op-only debug command. */
    public String describe(LivingEntity e, BudgetedBoss b) {
        return String.format(java.util.Locale.ROOT, "cap %.1f/s (stage %d/%d), left %.1f, tick %d/20, health %.1f/%.1f",
                budget.cap(), b.budgetStage(), b.budgetFinalStage(), budget.left(), budget.ticksInWindow(), e.getHealth(), e.getMaxHealth());
    }
}
