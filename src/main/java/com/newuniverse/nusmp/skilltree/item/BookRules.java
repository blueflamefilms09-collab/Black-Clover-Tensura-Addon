package com.newuniverse.nusmp.skilltree.item;

import com.newuniverse.nusmp.dungeon.sentient.Dice;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Why: what a book does when it is read is the part of the items that must never be wrong in a way nobody notices (a book eaten for nothing, a skill taught twice, a spare copy that vanishes), so the decisions are
 * written here as plain functions with no Minecraft type and no server, and the game class ({@link BookUse}) only carries them out. Since 1.96.0 a Skill Book teaches the player its UNIQUE skill directly: the
 * decision takes four facts (is this the book of a magic that was removed, does the book name a skill that exists, does the reader already own that skill, is the reader creative) and answers with one
 * {@link Outcome}; nothing is consumed unless the outcome says so, and even then only after the skill was really learned (checked in {@link BookUse}). A creative reader keeps the book. A book of a REMOVED magic
 * (an old stack that a world still holds) is not blank: it is turned into a random book the reader does not own yet. The unit tests pin every branch.
 */
public final class BookRules {
    private BookRules() {}

    /** What reading a Skill Book should do. */
    public enum Outcome {
        /** The book names no skill that exists: nothing happens, nothing is consumed. */
        BLANK,
        /** The skill is not owned yet: teach it, then consume the book. */
        GRANT,
        /** The skill is already owned: tell the player, consume nothing. */
        ALREADY_OWNED,
        /** The book belongs to a magic that was removed: swap it for a random book the reader does not own. */
        REPLACE
    }

    /**
     * Decides what a Skill Book does.
     *
     * @param removedMagic does the book id name one of the removed magics ({@code LegacyIds.isRemovedBook})
     * @param skillKnown   does a unique skill of this book exist in the registry
     * @param owned        does the reader already own that skill
     */
    public static Outcome skillBook(boolean removedMagic, boolean skillKnown, boolean owned) {
        if (removedMagic) return Outcome.REPLACE;
        if (!skillKnown) return Outcome.BLANK;
        return owned ? Outcome.ALREADY_OWNED : Outcome.GRANT;
    }

    /** Does an outcome use the book up (once the skill was really learned or the replacement really given)? A creative reader keeps the book: that is decided by {@link #usesUp}. */
    public static boolean consumes(Outcome outcome) { return outcome == Outcome.GRANT || outcome == Outcome.REPLACE; }

    /** True when the book stack must lose one copy: the outcome consumes and the reader is not creative. */
    public static boolean usesUp(Outcome outcome, boolean creative) { return consumes(outcome) && !creative; }

    /**
     * The book a swapped voucher turns into: one of {@code all} the reader does not own yet, chosen by the dice, or null when the reader owns every one.
     *
     * @param all   the bare ids of every book, in a stable order
     * @param owned does the reader own the skill of this book
     */
    public static String pickUnowned(List<String> all, Predicate<String> owned, Dice dice) {
        List<String> pool = new ArrayList<>();
        for (String b : all) if (!owned.test(b)) pool.add(b);
        if (pool.isEmpty()) return null;
        double r = dice == null ? 0.0 : dice.next();
        int i = (int) Math.min(pool.size() - 1, Math.max(0.0, Math.floor(r * pool.size())));
        return pool.get(i);
    }

    /** The short key of what an outcome tells the reader (the message keys are {@code message.nusmp.skillbook.<key>}). */
    public static String messageKey(Outcome outcome) {
        return switch (outcome) {
            case BLANK -> "blank";
            case GRANT -> "learned";
            case ALREADY_OWNED -> "already_owned";
            case REPLACE -> "replaced";
        };
    }
}
