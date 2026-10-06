package com.newuniverse.nusmp.multiverse;

/** Magic Knight ranks (merit and stars, not raw power). Junior, Intermediate and Senior have classes 5th (lowest) to 1st. */
public enum KnightRank {
    JUNIOR("Junior Magic Knight", true),
    INTERMEDIATE("Intermediate Magic Knight", true),
    SENIOR("Senior Magic Knight", true),
    GRAND("Grand Magic Knight", false),
    WIZARD_KING("Wizard King", false);

    public final String displayName;
    public final boolean hasClasses;

    KnightRank(String displayName, boolean hasClasses) { this.displayName = displayName; this.hasClasses = hasClasses; }

    public static KnightRank byName(String n) {
        for (KnightRank r : values()) if (r.name().equalsIgnoreCase(n)) return r;
        return null;
    }

    /** One number for comparisons: Junior 5th = 0, Junior 1st = 4, Intermediate 5th = 5 ... Senior 1st = 14, Grand = 15, Wizard King = 16. */
    public static int step(KnightRank rank, int cls) {
        if (!rank.hasClasses) return rank == GRAND ? 15 : 16;
        return rank.ordinal() * 5 + (5 - Math.max(1, Math.min(5, cls)));
    }

    public static KnightRank rankOfStep(int step) {
        if (step >= 16) return WIZARD_KING;
        if (step >= 15) return GRAND;
        return values()[Math.max(0, step) / 5];
    }

    public static int classOfStep(int step) { return step >= 15 ? 0 : 5 - (Math.max(0, step) % 5); }

    public static String describe(KnightRank rank, int cls) {
        if (!rank.hasClasses) return rank.displayName;
        String[] ord = {"", "1st", "2nd", "3rd", "4th", "5th"};
        return ord[Math.max(1, Math.min(5, cls))] + " Class " + rank.displayName;
    }

    /** Parses "SENIOR", "SENIOR:3" or "senior 3". Returns the step, or -1. */
    public static int parseStep(String s) {
        if (s == null || s.isBlank()) return -1;
        String[] parts = s.trim().split("[: ]+");
        KnightRank r = byName(parts[0]);
        if (r == null) return -1;
        int cls = 5;
        if (parts.length > 1) try { cls = Integer.parseInt(parts[1].replaceAll("\\D", "")); } catch (NumberFormatException ignored) {}
        return step(r, cls);
    }
}
