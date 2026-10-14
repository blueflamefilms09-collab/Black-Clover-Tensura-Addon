package com.newuniverse.nusmp.boss.budget;

/**
 * A boss that has the per-second damage budget. A boss entity implements the one method {@link #budget()} (returning a field made with
 * {@code new BossBudget()}) and adds three hooks (see docs/boss_dps_budget.md): {@code baseTick} -> {@link BossBudget#tick},
 * {@code setHealth} -> {@link BossBudget#setHealth} and {@code hurt} -> {@link BossBudget#hurt}. The phase methods are optional.
 */
public interface BudgetedBoss {
    /** The boss's own budget; null only while the entity is still being constructed (the hooks then let everything through). */
    BossBudget budget();

    /** The boss's current phase, 0-based (power stage, act, ...). */
    default int budgetStage() { return 0; }

    /** The last phase number; the cap is scaled up from the one before it. 0 = the boss has no phases. */
    default int budgetFinalStage() { return 0; }
}
