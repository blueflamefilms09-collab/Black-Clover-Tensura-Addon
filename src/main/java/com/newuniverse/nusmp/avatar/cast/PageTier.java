package com.newuniverse.nusmp.avatar.cast;

/**
 * Why: a player opens the pages of a grimoire by mastery, but an Avatar that casts its Magic Aptitude from a fresh book has no mastery to open them with, so
 * how many pages of the family are open for it is a function of how strong it is (its EP) and how clever (INT): a newly made Avatar casts the starter page, a
 * strong one has most of the book. Pure arithmetic; {@link VirtualSkillLoadout} turns the count into the book's own "Unlocked" bit mask. The pages are still
 * filtered by {@link AvatarSkillList}, so an open page the whitelist refuses is never cast.
 */
public final class PageTier {
    private PageTier() {}

    /** EP that buys the first extra page (the n-th extra page costs n squared times this). */
    public static final double EP_PER_STEP = 150.0;

    /** How many of the family's pages are open for an Avatar with this EP and INT score: at least the starter, at most the whole family. */
    public static int unlockedPages(int familyCount, double ep, int intelligence) {
        if (familyCount <= 0) return 0;
        int n = 1 + (int) Math.floor(Math.sqrt(Math.max(0.0, ep) / EP_PER_STEP));
        if (intelligence >= 16) n++;
        return Math.max(1, Math.min(familyCount, n));
    }

    /** The grimoire's "Unlocked" mask for the first {@code pages} modes. */
    public static int mask(int pages) {
        if (pages <= 0) return 0;
        if (pages >= 31) return 0x7FFFFFFF;
        return (1 << pages) - 1;
    }
}
