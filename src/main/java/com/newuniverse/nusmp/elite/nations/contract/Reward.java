package com.newuniverse.nusmp.elite.nations.contract;

/**
 * Why: the reward structure of a contract: copper coins (paid through the wallet hook, minus the nation's tax), nation XP (levels the nation) and reputation with the issuing nation. Pure.
 */
public record Reward(long coins, long nationXp, int reputation) {
    public static final Reward NONE = new Reward(0, 0, 0);

    /** Percent bonus on coins and XP (a nation perk); reputation is not scaled. */
    public Reward withBonusPercent(double percent) {
        if (percent <= 0) return this;
        double f = 1.0 + percent / 100.0;
        return new Reward(Math.round(coins * f), Math.round(nationXp * f), reputation);
    }
}
