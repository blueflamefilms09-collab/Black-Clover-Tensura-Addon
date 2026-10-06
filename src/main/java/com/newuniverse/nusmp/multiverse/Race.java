package com.newuniverse.nusmp.multiverse;

/** Races (append only: the ordinal is never stored, the name is). */
public enum Race {
    HUMAN("Human"), ELF("Elf"), DEVIL("Devil"), SPIRIT_BONDED("Spirit-bonded"), DWARF("Dwarf"), HYBRID("Hybrid");

    public final String displayName;
    Race(String displayName) { this.displayName = displayName; }

    public static Race byName(String n) {
        for (Race r : values()) if (r.name().equalsIgnoreCase(n)) return r;
        return null;
    }
}
