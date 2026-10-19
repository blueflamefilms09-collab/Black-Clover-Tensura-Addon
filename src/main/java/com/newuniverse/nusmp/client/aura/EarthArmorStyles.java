package com.newuniverse.nusmp.client.aura;

/**
 * 1.93.0 Why: the temporary armours of Sandstone, Rock, Red Ochre, Stone, Earth and Mineral magic all ride on the one shared {@code Aura.WIKI} player layer
 * slot (a new slot would need an edit of the shared enum for nothing: a player holds one wiki armour at a time anyway). The style number the spell hands
 * to {@code PlayerAuras.set} picks the set. The five wiki magics keep their {@code WikiStyles} index (so the generic guard page of the old build already
 * sends the right number); Earth Magic is not a wiki magic and gets 100. Pure numbers, no game classes: the server spells and the painter both read them
 * (a unit test pins them to {@code WikiStyles}).
 */
public final class EarthArmorStyles {
    private EarthArmorStyles() {}

    public static final int MINERAL = 3, RED_OCHRE = 10, ROCK = 11, SANDSTONE = 12, STONE = 26, EARTH = 100;

    /** The style of a magic by its enum constant name, or -1 when the magic has no earth-family armour. */
    public static int of(String enumName) {
        if (enumName == null) return -1;
        return switch (enumName) {
            case "MINERAL" -> MINERAL;
            case "RED_OCHRE" -> RED_OCHRE;
            case "ROCK" -> ROCK;
            case "SANDSTONE" -> SANDSTONE;
            case "STONE" -> STONE;
            case "EARTH" -> EARTH;
            default -> -1;
        };
    }

    public static boolean owns(int style) { return style == MINERAL || style == RED_OCHRE || style == ROCK || style == SANDSTONE || style == STONE || style == EARTH; }
}
