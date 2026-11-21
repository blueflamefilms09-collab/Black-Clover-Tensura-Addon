package com.newuniverse.nusmp.bossdrops2;

import java.util.function.BooleanSupplier;

/**
 * Why: the rule of a boss's one-of-one, kept pure so it can be tested without a game. The God of Order's relic ledger is the authority on whether the relic exists (a world holds at most one live copy);
 * this only asks it once and turns the answer into what the kill pays: {@link Outcome#ISSUED} (the unique was made: it goes to the top fighter), {@link Outcome#EXISTS} (it already exists: the top
 * fighter gets an echo instead) or {@link Outcome#NONE} (this boss has no one-of-one, or the feature is off). The issuer is asked at most once per kill and only when there is somebody to give it to.
 */
public final class OneOfOne {
    private OneOfOne() {}

    public enum Outcome { NONE, ISSUED, EXISTS }

    /**
     * @param wanted whether this kill may pay a one-of-one at all (the table has one, its relic is valid, there is a fighter to receive it)
     * @param issue  asks the ledger to make the unique now: true = made (nobody held it), false = a live copy exists
     */
    public static Outcome resolve(boolean wanted, BooleanSupplier issue) {
        if (!wanted || issue == null) return Outcome.NONE;
        return issue.getAsBoolean() ? Outcome.ISSUED : Outcome.EXISTS;
    }
}
