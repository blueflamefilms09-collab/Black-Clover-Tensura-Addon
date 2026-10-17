package com.newuniverse.nusmp.labyrinth.mega.gen;

/**
 * Why: the assembler is pure Java (no Minecraft types, so JUnit can run it thousands of times a second), but it must turn pieces exactly the way
 * the game does, or a sector planned here would be placed rotated by the game. This is the horizontal half of that agreement: the four compass
 * directions in CLOCKWISE order seen from above (north, east, south, west), the same order a vanilla {@code Rotation.CLOCKWISE_90} walks them in
 * (north becomes east, east becomes south ...). {@code dx} / {@code dz} are the vanilla steps (north is -z, east is +x). A "quarter turn" count
 * everywhere in the generator is the number of such clockwise steps (0..3), which is also the ordinal of the vanilla {@code Rotation} enum.
 */
public enum Facing {
    NORTH(0, -1), EAST(1, 0), SOUTH(0, 1), WEST(-1, 0);

    private static final Facing[] VALUES = values();

    /** The block step of one cell in this direction (x and z; y never changes). */
    public final int dx, dz;

    Facing(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    /** The direction behind this one. */
    public Facing opposite() { return VALUES[(ordinal() + 2) & 3]; }

    /** A quarter turn clockwise (north to east). */
    public Facing cw() { return VALUES[(ordinal() + 1) & 3]; }

    /** A quarter turn counter-clockwise (north to west). */
    public Facing ccw() { return VALUES[(ordinal() + 3) & 3]; }

    /** This direction after {@code quarterTurns} clockwise quarter turns (any integer; negative turns go counter-clockwise). */
    public Facing turn(int quarterTurns) { return VALUES[(ordinal() + quarterTurns) & 3]; }

    /** How many clockwise quarter turns (0..3) take this direction to {@code other}. */
    public int turnsTo(Facing other) { return (other.ordinal() - ordinal()) & 3; }

    /** Do the two directions lie on the same axis (equal or opposite)? */
    public boolean sameAxis(Facing other) { return ((ordinal() ^ other.ordinal()) & 1) == 0; }

    /** Is this direction along the x axis (east or west)? */
    public boolean alongX() { return dx != 0; }

    /** The lower-case name used in NBT, JSON and the plan file ({@code north}). */
    public String lower() { return name().toLowerCase(java.util.Locale.ROOT); }

    /** The direction with this name (case-insensitive), or null for anything that is not a horizontal compass name. */
    public static Facing byName(String name) {
        if (name == null) return null;
        for (Facing f : VALUES) if (f.name().equalsIgnoreCase(name.trim())) return f;
        return null;
    }

    /** The direction with this clockwise index (0 north .. 3 west); any integer is reduced modulo 4. */
    public static Facing byIndex(int i) { return VALUES[i & 3]; }
}
