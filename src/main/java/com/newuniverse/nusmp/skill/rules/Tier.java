package com.newuniverse.nusmp.skill.rules;

/**
 * Why: every rebuilt skill has the same three power levels (a fresh Unique, a mastered Unique, and its Ultimate), and each used to
 * re-invent the "mastered ? a : b" ternary with its own numbers. One Minecraft-free enum keeps the choice in one place, so the rules
 * classes stay unit-testable and an Ultimate can never come out weaker than the mastered base by accident.
 * {@link #ULTIMATE_MASTERED} (appended, master guide P1 #4) is a mastered classified Ultimate: it reads like {@link #ULTIMATE} everywhere
 * (use {@link #ultimate()}, never {@code == ULTIMATE}) and adds the mastered values ({@link #pick(int, int, int, int)}, {@link #masteryScale}).
 */
public enum Tier {
    BASE, MASTERED, ULTIMATE, ULTIMATE_MASTERED;

    /** Bonus an Ultimate gets once the Ultimate ITSELF is mastered (its tier is already the peak, so mastery adds this on top). */
    public static final double ULTIMATE_MASTERY_BONUS = 1.2;

    /** An Ultimate is always {@link #ULTIMATE}; a Unique is {@link #MASTERED} once mastered. */
    public static Tier of(boolean mastered, boolean ultimate) {
        return ultimate ? ULTIMATE : mastered ? MASTERED : BASE;
    }

    /** The same, but a mastered Ultimate is {@link #ULTIMATE_MASTERED} when {@code split} (the SERVER setting ultimateClasses.masteredTier). */
    public static Tier of(boolean mastered, boolean ultimate, boolean split) {
        return ultimate && mastered && split ? ULTIMATE_MASTERED : of(mastered, ultimate);
    }

    /** True for both Ultimate tiers. */
    public boolean ultimate() { return this == ULTIMATE || this == ULTIMATE_MASTERED; }

    public boolean atLeast(Tier other) { return ordinal() >= other.ordinal(); }

    public int pick(int base, int mastered, int ultimate) {
        return switch (this) { case BASE -> base; case MASTERED -> mastered; case ULTIMATE, ULTIMATE_MASTERED -> ultimate; };
    }

    public double pick(double base, double mastered, double ultimate) {
        return switch (this) { case BASE -> base; case MASTERED -> mastered; case ULTIMATE, ULTIMATE_MASTERED -> ultimate; };
    }

    public boolean pick(boolean base, boolean mastered, boolean ultimate) {
        return switch (this) { case BASE -> base; case MASTERED -> mastered; case ULTIMATE, ULTIMATE_MASTERED -> ultimate; };
    }

    /** With a mastered-Ultimate value. */
    public int pick(int base, int mastered, int ultimate, int ultimateMastered) {
        return this == ULTIMATE_MASTERED ? ultimateMastered : pick(base, mastered, ultimate);
    }

    public double pick(double base, double mastered, double ultimate, double ultimateMastered) {
        return this == ULTIMATE_MASTERED ? ultimateMastered : pick(base, mastered, ultimate);
    }

    /** Extra power for a mastered Ultimate (1.0 for everything else). */
    public double masteryScale(boolean ownMastered) {
        return this == ULTIMATE_MASTERED || this == ULTIMATE && ownMastered ? ULTIMATE_MASTERY_BONUS : 1.0;
    }
}
