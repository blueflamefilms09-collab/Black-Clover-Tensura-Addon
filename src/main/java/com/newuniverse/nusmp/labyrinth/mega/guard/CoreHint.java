package com.newuniverse.nusmp.labyrinth.mega.guard;

/**
 * Why: a Mana Core that is only a key leaves a party that fled the Guardian's hall with a glowing stone and no idea where its door is. Held
 * and used (right click in the air) it tells which way its seal lies and how far, in plain compass words, the way the old explorers'
 * compasses of the Dungeon do. The arithmetic is here, free of Minecraft types, so the eight sectors of the compass and the distance are
 * tested; the item only reads the answer and picks the lang key {@code message.nusmp.mega.mgd.dir.<name>}.
 */
public final class CoreHint {
    private CoreHint() {}

    /** The eight directions, clockwise from north, as lang-key suffixes. North is -z and east is +x, like Minecraft's own axes. */
    public static final String[] NAMES = {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"};

    /** The direction of the offset ({@code dx}, {@code dz}); a zero offset reads as "north" (the caller says "right here" first). */
    public static String direction(double dx, double dz) {
        if (dx == 0.0 && dz == 0.0) return NAMES[0];
        double deg = Math.toDegrees(Math.atan2(dx, -dz));
        if (deg < 0) deg += 360.0;
        int idx = (int) Math.floor((deg + 22.5) / 45.0) % 8;
        return NAMES[idx];
    }

    /** Horizontal distance in whole blocks. */
    public static int distance(double dx, double dz) { return (int) Math.round(Math.hypot(dx, dz)); }

    /** Within this many blocks the core says the seal is right here instead of naming a direction. */
    public static final int HERE = 4;
}
