package com.newuniverse.nusmp.labyrinth.mega.enc;

import java.util.Locale;

/**
 * Why: a tabletop encounter is not "spawn some monsters", it is a size of trouble: nothing, a nuisance, a fair fight, a hard fight, an ambush.
 * The Dungeon Master's d20 lands in one of these bands and everything downstream reads the band, not the raw number: the encounter table
 * only offers entries of that band, the budget is scaled by the band's factor (a nuisance is half a standard fight, an ambush more than two),
 * and the hunt orders last as long as the band deserves. Keeping it an enum (not strings or magic numbers) means the table files, the roll,
 * the budget and the admin command all spell the bands the same way, and a typo in a data file is caught by the parser. Pure.
 */
public enum Band {
    /** The quiet result: the dungeon lets the party be. */
    NOTHING(0.0, 0),
    MINOR(0.5, 400),
    STANDARD(1.0, 800),
    HARD(1.6, 1200),
    AMBUSH(2.2, 1600);

    private final double budgetFactor;
    private final int huntTicks;

    Band(double budgetFactor, int huntTicks) {
        this.budgetFactor = budgetFactor;
        this.huntTicks = huntTicks;
    }

    /** How much of the base budget this band is worth. */
    public double budgetFactor() { return budgetFactor; }

    /** How long, in ticks, the mobs of this band go looking for the party before they are left to their own devices. */
    public int huntTicks() { return huntTicks; }

    /** The band one step up (an ambush stays an ambush), used by a natural 20. */
    public Band up() { return this == AMBUSH ? AMBUSH : values()[ordinal() + 1]; }

    /** The band one step down (nothing stays nothing), used when a band has no usable entry. */
    public Band down() { return this == NOTHING ? NOTHING : values()[ordinal() - 1]; }

    /** The lower-case name used in the data files ({@code "standard"}). */
    public String label() { return name().toLowerCase(Locale.ROOT); }

    /** The band with this name (any case), or null when unknown. */
    public static Band byName(String name) {
        if (name == null) return null;
        for (Band b : values()) if (b.label().equalsIgnoreCase(name.trim())) return b;
        return null;
    }

    /**
     * The band for the margin by which the d20 total beat the difficulty class: below zero nothing; 0-1 minor; 2-3 standard; 4-5 hard; 6 and
     * more an ambush. Two points per band, so a modifier of +2 is worth one band, which is the scale the modifiers were calibrated on.
     */
    public static Band ofMargin(int margin) {
        if (margin < 0) return NOTHING;
        if (margin < 2) return MINOR;
        if (margin < 4) return STANDARD;
        if (margin < 6) return HARD;
        return AMBUSH;
    }
}
