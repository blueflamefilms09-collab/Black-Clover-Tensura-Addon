package com.newuniverse.nusmp.elite.nations.econ;

/**
 * Why: the interest hook of the treasury ("UBS Cash & Interest hooks"). One method: given a nation id and its balance, how many coins does it earn for one period. The built-in policy is
 * {@link Simple}; the optional bridge to the UBS (Ultimate Banking System) mod is another implementation (see {@code UbsBridge}); other systems may register their own with
 * {@code NationApi.setInterestPolicy}. A policy never throws (a throw is treated as 0 by the caller) and never returns a negative number.
 */
@FunctionalInterface
public interface InterestPolicy {
    long interest(String nationId, long balance);

    /** No interest. */
    InterestPolicy NONE = (id, balance) -> 0L;

    /**
     * The built-in simple interest: {@code balance * perMille / 1000}, only above {@code minBalance}, capped at {@code maxPerPeriod}.
     */
    record Simple(int perMille, long minBalance, long maxPerPeriod) implements InterestPolicy {
        @Override public long interest(String nationId, long balance) {
            if (perMille <= 0 || balance <= 0 || balance < minBalance) return 0L;
            long raw = (long) Math.floor(balance * (double) perMille / 1000.0);
            return Math.max(0L, Math.min(raw, maxPerPeriod <= 0 ? Long.MAX_VALUE : maxPerPeriod));
        }
    }
}
