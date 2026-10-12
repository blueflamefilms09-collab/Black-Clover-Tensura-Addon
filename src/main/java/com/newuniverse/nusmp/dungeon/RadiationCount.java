package com.newuniverse.nusmp.dungeon;

/**
 * The Golden City's magicule radiation, counted one second at a time (docs/living_dungeon.md section 3). Pure: {@link RadiationShield}
 * reads the player and feeds the answers in. A Mana Zone or a Golden Ward shuts the radiation out and lets it drain away; Magic Sense
 * does not shut it out but dulls it, so it builds only every other second; anyone else is soaked in it.
 */
public final class RadiationCount {
    private RadiationCount() {}

    /**
     * What this second does to the radiation: +1 it builds, 0 it is held, -1 it ebbs. {@code pass} is the counter {@link #next} keeps
     * while Magic Sense holds the radiation.
     */
    public static int step(boolean shielded, boolean sense, int pass) {
        if (shielded) return -1;
        if (!sense) return 1;
        return (pass & 1) == 1 ? 1 : 0;
    }

    /** The counter after a second spent with Magic Sense dulling the radiation (it only ever needs to tell odd from even). */
    public static int next(int pass) { return (pass + 1) & 1; }
}
