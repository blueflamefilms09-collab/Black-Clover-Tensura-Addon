package com.newuniverse.nusmp.heroweapon.rules;

import com.newuniverse.nusmp.relics.Grade;

/**
 * Why (4.88.1): a hero weapon rejects every weapon of another class, and the Genesis relics (the primordial ones: Primordial Hearth Blade, Starforged Aeon Bow, Void Origin Katana, ...)
 * are classed as ordinary swords, bows and hammers, so a Holy or Vassal wielder was shocked for using them. With the setting {@code genesisRelicsAllowed} (default on) a relic of the Genesis
 * or the EX-Singularity grade is never rejected. Pure: no game types beyond the grade enum.
 */
public final class GenesisExempt {
    private GenesisExempt() {}

    /** True when a held relic of this grade is exempt from the class rejection. */
    public static boolean exempt(Grade grade, boolean allowed) {
        return allowed && grade != null && grade.tier() >= Grade.GENESIS.tier();
    }
}
