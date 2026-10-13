package com.newuniverse.nusmp.dungeon.nexus.vault;

/**
 * The small rooms the Living Dungeon hides in its rock (docs/living_dungeon.md, "Hidden Vaults and Navigation"). A vault is a room carved into
 * solid rock next to a cave; its wall to the cave is made of Illusion Stone, which looks like the rock round it and has no collision. The
 * first three hold something worth finding; a Descent Shrine holds a Descent Gate, the way down to the next floor.
 */
public enum VaultKind {
    /** A floor of nine plates to be stepped in the order the room hints at; solving it opens the cache behind iron bars. */
    PUZZLE("Puzzle Vault"),
    /** A moss garden of Nutrient Blooms that regrow slowly. */
    NUTRIENT("Nutrient Garden"),
    /** A Vault Guardian over a minor relic. */
    MINIBOSS("Guardian Vault"),
    /** A shrine round a Descent Gate. */
    GATE("Descent Shrine");

    public final String label;

    VaultKind(String label) { this.label = label; }

    /** The kinds a plain vault cell can hold (never the shrine). */
    public static VaultKind vaultByRoll(int percent) {
        if (percent < 40) return PUZZLE;
        if (percent < 70) return NUTRIENT;
        return MINIBOSS;
    }
}
