package com.newuniverse.nusmp.grimoire;

/** Page count / body depth. Half-depth is in model pixels (the book is centred on z = 8). APPEND ONLY. */
public enum Thickness {
    THIN("Thin", 1.2f, 0.45f),
    STANDARD("Standard", 1.9f, 0.45f),
    TOME("Tome", 3.0f, 0.45f),
    /** Rades style: a single loose page between two slim boards. */
    SINGLE_PAGE("Single Page", 0.55f, 0.20f);

    public final String displayName;
    public final float halfDepth;
    public final float board;

    Thickness(String displayName, float halfDepth, float board) { this.displayName = displayName; this.halfDepth = halfDepth; this.board = board; }

    private static final Thickness[] VALUES = values();
    public static Thickness byOrdinal(int i) { return VALUES[Math.floorMod(i, VALUES.length)]; }
}
