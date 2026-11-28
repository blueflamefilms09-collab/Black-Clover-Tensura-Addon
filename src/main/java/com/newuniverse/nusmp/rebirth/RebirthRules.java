package com.newuniverse.nusmp.rebirth;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the two rebirth rites (docs/rebirth.md) must decide the same way on every server and be testable without a game, so every decision is a pure function here: may a rite be used, how long
 * the cooldown still runs, is a confirming click inside its window, what the player's rebirth mark becomes, and which magic the second book gets. No Minecraft class is imported.
 */
public final class RebirthRules {
    private RebirthRules() {}

    public static final long TICKS_PER_DAY = 24000L;
    public static final int DEFAULT_COOLDOWN_DAYS = 7, MAX_COOLDOWN_DAYS = 365;
    public static final int DEFAULT_CONFIRM_SECONDS = 15, MIN_CONFIRM_SECONDS = 5, MAX_CONFIRM_SECONDS = 120;
    public static final int DEFAULT_SECOND_MIN_LEAVES = 4, DEFAULT_SECOND_MIN_RESOLVE = 3, MAX_XP_COST = 100;
    /** The resolve value that means "the questionnaire never wrote one": treated as fine. */
    public static final int RESOLVE_UNKNOWN = -1;

    public enum Rite { REBIRTH, SECOND_BOOK }

    /** The player's rebirth mark (persistent key nusmp_rebirth): RESET / SECOND wait for the questionnaire, DONE is the finished state, NONE no mark. */
    public enum Mode { NONE, RESET, SECOND, DONE }

    public enum Verdict { OK, DISABLED, COOLDOWN, NO_GRIMOIRE, TIER_TOO_LOW, ALREADY_TWO, RESOLVE_TOO_LOW, NO_WIZARD, NOT_ENOUGH_XP, ALREADY_WAITING }

    // ---------------------------------------------------------------- clamps (a hand-edited file cannot break a bound)
    public static int cooldownDays(int v) { return Math.max(0, Math.min(MAX_COOLDOWN_DAYS, v)); }

    public static int confirmSeconds(int v) { return Math.max(MIN_CONFIRM_SECONDS, Math.min(MAX_CONFIRM_SECONDS, v)); }

    public static int windowTicks(int seconds) { return confirmSeconds(seconds) * 20; }

    public static int xpCost(int v) { return Math.max(0, Math.min(MAX_XP_COST, v)); }

    public static int minLeaves(int v) { return Math.max(3, Math.min(5, v)); }

    // ---------------------------------------------------------------- the cooldown
    /** Ticks left of the cooldown. {@code lastAt < 0} means never used. A clock that went backwards (now before lastAt) counts as a full cooldown, never as a negative one. */
    public static long cooldownLeft(long now, long lastAt, int days) {
        long full = cooldownDays(days) * TICKS_PER_DAY;
        if (lastAt < 0 || full == 0) return 0;
        long since = now - lastAt;
        if (since < 0) return full;
        return Math.max(0, full - since);
    }

    /** Whole game days (rounded up) a left-over cooldown still lasts, for the chat message. */
    public static int daysLeft(long ticksLeft) { return (int) ((Math.max(0, ticksLeft) + TICKS_PER_DAY - 1) / TICKS_PER_DAY); }

    // ---------------------------------------------------------------- the checks
    /**
     * May this rite be used now?
     *
     * @param grimoires     how many grimoire skills the player has
     * @param bestTier      the highest leaf tier among them (0 when none)
     * @param resolve       the questionnaire's resolve score, or {@link #RESOLVE_UNKNOWN}
     * @param minResolve    the setting; 0 turns the resolve check off
     * @param wizardMode    the Tower Wizard setting (the second book needs his questionnaire)
     * @param alreadyWaiting the player already carries a mark that waits for the questionnaire
     */
    public static Verdict verdict(Rite rite, boolean enabled, long now, long lastAt, int cooldownDays, int grimoires, int bestTier, int minLeaves,
                                  int resolve, int minResolve, boolean wizardMode, int xpLevels, int xpCost, boolean alreadyWaiting) {
        if (!enabled) return Verdict.DISABLED;
        if (grimoires <= 0) return Verdict.NO_GRIMOIRE;
        if (alreadyWaiting) return Verdict.ALREADY_WAITING;
        if (cooldownLeft(now, lastAt, cooldownDays) > 0) return Verdict.COOLDOWN;
        if (rite == Rite.SECOND_BOOK) {
            if (!wizardMode) return Verdict.NO_WIZARD;
            if (grimoires >= 2) return Verdict.ALREADY_TWO;
            if (bestTier < minLeaves(minLeaves)) return Verdict.TIER_TOO_LOW;
            if (minResolve > 0 && resolve != RESOLVE_UNKNOWN && resolve < minResolve) return Verdict.RESOLVE_TOO_LOW;
        }
        if (xpCost(xpCost) > 0 && xpLevels < xpCost(xpCost)) return Verdict.NOT_ENOUGH_XP;
        return Verdict.OK;
    }

    // ---------------------------------------------------------------- the confirm window
    /** A confirming use: sneaking, a warning of the same rite was given, and it is no older than the window. {@code warnedAt < 0} means no warning. */
    public static boolean confirmed(long warnedAt, long now, int windowTicks, boolean sneaking) {
        return sneaking && warnedAt >= 0 && now >= warnedAt && now - warnedAt <= windowTicks;
    }

    // ---------------------------------------------------------------- the mark
    public static Mode parse(String s) {
        if (s == null) return Mode.NONE;
        for (Mode m : Mode.values()) if (m.name().equals(s)) return m;
        return Mode.NONE;
    }

    /** The mark a rite leaves behind when it is used. */
    public static Mode afterUse(Rite rite) { return rite == Rite.REBIRTH ? Mode.RESET : Mode.SECOND; }

    /** The mark once the questionnaire has granted the book(s). */
    public static Mode afterQuestionnaire(Mode m) { return m == Mode.RESET || m == Mode.SECOND ? Mode.DONE : m; }

    /** Does this mark let the Tower Wizard run the questionnaire for a player who may already carry a grimoire? Only SECOND does (RESET has no grimoire left). */
    public static boolean waitsForQuestionnaire(Mode m, int grimoires) {
        return m == Mode.SECOND ? grimoires >= 1 && grimoires < 2 : m == Mode.RESET && grimoires == 0;
    }

    // ---------------------------------------------------------------- the second book's magic
    /** The questionnaire pool for the second book: the first book's magic is taken out (unless that would leave nothing), so the result is a different attribute. */
    public static <T> List<T> poolWithout(List<T> pool, T first) {
        List<T> out = new ArrayList<>(pool);
        if (first != null) out.remove(first);
        return out.isEmpty() ? new ArrayList<>(pool) : out;
    }

    /** The magic the second grimoire gets: the result's primary, or (blend equal to the first book) one of its two parents that differs; null if all are the first magic. */
    public static String secondMagic(String primary, String parentA, String parentB, String first) {
        for (String m : new String[]{primary, parentA, parentB}) if (m != null && !m.equals(first)) return m;
        return null;
    }

    // ---------------------------------------------------------------- two books on a fusion
    /** The setting {@code twoBooksOnFusion}: a high-resolve (leaves 4 or more) fused result gets two real grimoires. A devil (forbidden) cover stays a single book. */
    public static boolean twoBooksOnFusion(boolean setting, boolean fused, int leaves, boolean forbiddenCover) {
        return setting && fused && leaves >= 4 && !forbiddenCover;
    }

    // ---------------------------------------------------------------- the refusal text (5.8.1)
    /**
     * Why a book of {@code have} leaves is turned away by a rite that needs {@code need}: said in chat as well as on the action bar, because the bar line is gone in two seconds and a
     * three-leaf mage was left thinking the sigil was broken. Empty when the book is good enough.
     */
    public static String tierHint(int have, int need) {
        int n = minLeaves(need);
        if (have >= n) return "";
        return "Your grimoire has " + Math.max(0, have) + " leaves; the Second Book needs " + n + ". A grimoire's leaves are fixed when it is given (the Tower Wizard's questionnaire: "
                + "careful, resolute answers earn a four-leaf or five-leaf), they do not grow with use. To try again for more leaves, use the Sigil of Rebirth (it erases your grimoire and asks the questions anew); "
                + "the server owner can also lower this rule (secondBookMinLeaves, 3 to 5).";
    }
}
