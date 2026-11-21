package com.newuniverse.nusmp.wizard;

/**
 * How many leaves the grimoire gets (3, 4 or 5). Pure numbers, no game classes: the thresholds are tuned against the resolve questions of {@link LongQuestions} by the Monte Carlo test
 * LeafRollTest (docs/wizard_long_questionnaire.md). Two rules:
 * <ul>
 *   <li>{@link #oldLeaves}: the original rule of the 12-question sheet (setting wizard.longQuestionnaire = false): resolve plus a luck of 0..2, 5 from 5, 4 from 3.</li>
 *   <li>{@link #leaves}: the long sheet: the total resolve of the six resolve questions plus a luck of 0 or 1 ({@link #LUCK_SIDES}).</li>
 * </ul>
 */
public final class LeafRoll {
    private LeafRoll() {}

    /** The luck roll of the long sheet is rng.nextInt(LUCK_SIDES): 0 or 1, so the answers decide and fortune only breaks a close call. */
    public static final int LUCK_SIDES = 2;
    /** Total resolve (answers + luck) from which the cover has 4 leaves / 5 leaves on the long sheet. */
    public static final int FOUR_FROM = 15, FIVE_FROM = 18;

    public static int leaves(int resolveTotal, int luck) {
        int s = resolveTotal + luck;
        return s >= FIVE_FROM ? 5 : s >= FOUR_FROM ? 4 : 3;
    }

    /** The rule of the old sheet: score = resolve + rng.nextInt(3). */
    public static int oldLeaves(int resolveTotal, int luck) {
        int s = resolveTotal + luck;
        return s >= 5 ? 5 : s >= 3 ? 4 : 3;
    }
}
