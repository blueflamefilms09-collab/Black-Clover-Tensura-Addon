package com.newuniverse.nusmp.skilltree.item;

import com.newuniverse.nusmp.dungeon.sentient.Dice;

/**
 * Why: what a book does when it is read is the part of the items that must never be wrong in a way nobody notices (a book eaten for nothing, a pile of Skill Points
 * from a hand-typed negative number, a spare copy that is worth nothing and vanishes anyway), so the decisions are written here as plain functions with no Minecraft type and no
 * server, and the game class ({@link BookUse}) only carries them out. The Skill Book decision takes three facts (does the route of this book exist, does the
 * player already have it, does a second copy give Skill Points) and answers with one {@link Outcome}; nothing is consumed unless the outcome says so, and even then
 * only after the server's skill-tree API has really done the thing (the API result is checked in {@link BookUse}, because only the API knows whether the tree is
 * switched off). The SP Book decision turns the stack's two components into a number of Skill Points: the stack's own amount wins over the kind's default (so
 * the shop can sell a stack with another number), a gamble rolls through {@link JackInTheBox}, a missing or negative amount is clamped (a crafted
 * {@code sp_amount} must never remove points), and a book with neither a known kind nor an amount is blank. The unit tests pin every branch.
 */
public final class BookRules {
    private BookRules() {}

    /** The most Skill Points one SP Book can give, whatever its component says (the tree's own cap is configurable and applies after this). */
    public static final int MAX_AMOUNT = 9999;
    /** The value that means "the stack has no {@code sp_amount} component". */
    public static final int NO_AMOUNT = -1;

    /** What reading a Skill Book should do. */
    public enum Outcome {
        /** The book names no route that exists: nothing happens, nothing is consumed. */
        BLANK,
        /** The route is not yet known: unlock it, then consume the book. */
        UNLOCK,
        /** The route is already known and a copy is worth Skill Points: add them, then consume the book. */
        DUPLICATE_SP,
        /** The route is already known and a copy is worth nothing (or the server refuses duplicates): tell the player, consume nothing. */
        DUPLICATE_REFUSED
    }

    /**
     * Decides what a Skill Book does.
     *
     * @param routeKnown      does the route that lists this book exist in the registry
     * @param alreadyUnlocked does the reader already have that route
     * @param duplicateGivesSp the server setting: a second copy converts to Skill Points (true) or is refused (false)
     * @param duplicateSp     the Skill Points the route says a second copy is worth
     */
    public static Outcome skillBook(boolean routeKnown, boolean alreadyUnlocked, boolean duplicateGivesSp, int duplicateSp) {
        if (!routeKnown) return Outcome.BLANK;
        if (!alreadyUnlocked) return Outcome.UNLOCK;
        return duplicateGivesSp && duplicateSp > 0 ? Outcome.DUPLICATE_SP : Outcome.DUPLICATE_REFUSED;
    }

    /** Does an outcome use the book up (once the API has done what the outcome asks)? A creative reader still keeps it: that is decided where the stack is shrunk. */
    public static boolean consumes(Outcome outcome) { return outcome == Outcome.UNLOCK || outcome == Outcome.DUPLICATE_SP; }

    /**
     * The Skill Points one reading of an SP Book gives, 0 for a blank book.
     *
     * @param kind      the stack's kind, or null when it has none or an unknown one
     * @param component the stack's {@code sp_amount}, or {@link #NO_AMOUNT}
     * @param dice      the random source for a gamble
     */
    public static int spBookAmount(SpKind kind, int component, Dice dice) {
        int amount;
        if (kind == null) {
            amount = component;
        } else if (kind.isGamble()) {
            amount = JackInTheBox.roll(component >= 0 ? component : kind.amount(), dice);
        } else {
            amount = component >= 0 ? component : kind.amount();
        }
        return Math.max(0, Math.min(MAX_AMOUNT, amount));
    }
}
