package com.newuniverse.nusmp.dungeon.nexus;

import java.util.Locale;

/** The words the Nexus says to the server (config-gated broadcasts) and the rule for when to say them. Pure, so the wording is checked in tests. */
public final class NexusText {
    private NexusText() {}

    /** The owner's wording for a Spatial Nexus seal giving way (docs/living_dungeon.md section 2, "Deep Delving"). */
    public static final String SEAL_LINE = "An immense Mana presence is felt as a Spatial Nexus seal is breached.";

    /** "the Monarch's Vault" from "The Monarch's Vault". */
    public static String the(String label) {
        if (label == null || label.isEmpty()) return "the Variable Core";
        return label.regionMatches(true, 0, "The ", 0, 4) ? "the " + label.substring(4) : "the " + label;
    }

    /** The broadcast when the first player enters a core cell: the owner's line, then who walks which zone. */
    public static String sealBreached(String player, String zoneLabel) { return SEAL_LINE + " " + player + " walks " + the(zoneLabel) + "."; }

    /**
     * Should the Nexus announce this seal itself? The first announcement of a fresh world is made by the older Golden City flow of
     * DungeonEcology when that got there first (its flag is set and no Nexus seal had been broken yet); in a world that already had
     * the flag when the Nexus data was first created, every new seal is announced.
     */
    public static boolean announceSeal(boolean legacyWorld, boolean legacyFlagNow, int sealedBefore) {
        if (legacyWorld) return true;
        return !(legacyFlagNow && sealedBefore == 0);
    }

    /** A zone's name as a command or log word: "monarch_vault". */
    public static String id(NexusZone zone) { return zone.name().toLowerCase(Locale.ROOT); }
}
