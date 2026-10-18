package com.newuniverse.nusmp.skill.rules;

/**
 * Why: every rebuilt skill has the same three power levels (a fresh Unique, a mastered Unique, and its Ultimate), and each used to
 * re-invent the "mastered ? a : b" ternary with its own numbers. One Minecraft-free enum keeps the choice in one place, so the rules
 * classes stay unit-testable and an Ultimate can never come out weaker than the mastered base by accident.
 */
public enum Tier {
    BASE, MASTERED, ULTIMATE;

    /** Bonus an Ultimate gets once the Ultimate ITSELF is mastered (its tier is already the peak, so mastery adds this on top). */
    public static final double ULTIMATE_MASTERY_BONUS = 1.2;

    /** An Ultimate is always {@link #ULTIMATE}; a Unique is {@link #MASTERED} once mastered. */
    public static Tier of(boolean mastered, boolean ultimate) {
        return ultimate ? ULTIMATE : mastered ? MASTERED : BASE;
    }

    public boolean atLeast(Tier other) { return ordinal() >= other.ordinal(); }

    public int pick(int base, int mastered, int ultimate) {
        return switch (this) { case BASE -> base; case MASTERED -> mastered; case ULTIMATE -> ultimate; };
    }

    public double pick(double base, double mastered, double ultimate) {
        return switch (this) { case BASE -> base; case MASTERED -> mastered; case ULTIMATE -> ultimate; };
    }

    public boolean pick(boolean base, boolean mastered, boolean ultimate) {
        return switch (this) { case BASE -> base; case MASTERED -> mastered; case ULTIMATE -> ultimate; };
    }

    /** Extra power for a mastered Ultimate (1.0 for everything else). */
    public double masteryScale(boolean ownMastered) {
        return this == ULTIMATE && ownMastered ? ULTIMATE_MASTERY_BONUS : 1.0;
    }
}
