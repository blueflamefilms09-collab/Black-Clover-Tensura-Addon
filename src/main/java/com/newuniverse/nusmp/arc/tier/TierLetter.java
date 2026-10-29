package com.newuniverse.nusmp.arc.tier;

import java.util.Locale;

/**
 * Why: the owner's tier list (docs/bosses/tierlist.md) is the one source of how strong a character is and where in the Mega-Dungeon it stands, so the numbers of a
 * tier live in one place and no character carries its own stat line. A letter fixes the floor band (low tier = early floors), the stats (health, melee damage,
 * speed, armor), the threat tier of the global buff ladder ({@code ArcBossProfile}, 1..5) and the Magic Stones it drops. The whole ladder is stated now although only
 * F and D are built; the later releases add characters to a tier, they do not change the tier. Only ever APPEND letters (the order is the strength order).
 */
public enum TierLetter {
    F("F", "Bad", 1, 700, 10, 0.28, 4, 1, 20, 2),
    D("D", "Below average", 1, 1000, 13, 0.28, 6, 1, 20, 3),
    C("C", "Average", 2, 1500, 16, 0.29, 8, 15, 35, 4),
    B("B", "Decent", 2, 2200, 20, 0.29, 10, 25, 50, 5),
    A("A", "Good", 3, 3200, 24, 0.30, 12, 40, 70, 6),
    S("S", "Great", 3, 4800, 29, 0.30, 14, 55, 85, 8),
    SS("SS", "Amazing", 4, 7000, 34, 0.31, 16, 70, 95, 10),
    SS_PLUS("SS+", "Almost perfect", 5, 10000, 40, 0.31, 18, 90, 100, 12),
    TOP("SSS+++", "Top 5", 5, 15000, 48, 0.32, 20, 90, 100, 16);

    private final String label, title;
    private final int buffTier, floorMin, floorMax, stones;
    private final double health, attack, speed, armor;

    TierLetter(String label, String title, int buffTier, double health, double attack, double speed, double armor, int floorMin, int floorMax, int stones) {
        this.label = label;
        this.title = title;
        this.buffTier = buffTier;
        this.health = health;
        this.attack = attack;
        this.speed = speed;
        this.armor = armor;
        this.floorMin = floorMin;
        this.floorMax = floorMax;
        this.stones = stones;
    }

    /** The letter as the owner's list prints it ("F", "SS+", "SSS+++"). */
    public String label() { return label; }

    /** The owner's name of the tier ("Bad", "Below average", ...). */
    public String title() { return title; }

    /** The rung of the global buff ladder (1..5) its bosses get. */
    public int buffTier() { return buffTier; }

    public double health() { return health; }

    public double attack() { return attack; }

    public double speed() { return speed; }

    public double armor() { return armor; }

    /** Lowest absolute floor any character of this tier may stand on. */
    public int floorMin() { return floorMin; }

    /** Highest absolute floor any character of this tier may stand on. */
    public int floorMax() { return floorMax; }

    /** Magic Stones dropped on each kill (the boss adds a little random on top, never more than this again). */
    public int stones() { return stones; }

    /** Is this a light boss (the shared-body level of the owner's two levels), as opposed to a full boss with its own model? A..F are light. */
    public boolean light() { return ordinal() <= A.ordinal(); }

    /** The letter with this label ("ss+", "F"), or null. */
    public static TierLetter fromLabel(String s) {
        if (s == null) return null;
        String t = s.trim().toUpperCase(Locale.ROOT);
        for (TierLetter l : values()) if (l.label.equals(t)) return l;
        return null;
    }
}
