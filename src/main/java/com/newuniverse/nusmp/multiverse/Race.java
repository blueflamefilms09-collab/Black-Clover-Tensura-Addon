package com.newuniverse.nusmp.multiverse;

/**
 * A player's race as this mod sees it (0.27). Every race except DEVIL is a family of Tensura's own races, read live from the
 * player's Tensura race (TensuraRaceBridge): the mod adds a small buff on top of it (RaceBuffs) and uses it for page gates.
 * DEVIL is this mod's only own race and is set by command only for now (/multiverse profile race <player> devil|none).
 * The name is stored, never the ordinal.
 */
public enum Race {
    HUMAN("Human"), ELF("Elf"), DEVIL("Devil"), DWARF("Dwarf"),
    BEASTFOLK("Beastfolk"), OGRE("Ogre / Oni"), GOBLIN("Goblin"), GIANT("Giant"), ORC("Orc"), DRAGONEWT("Lizardman / Dragonewt"),
    MERFOLK("Merfolk"), SLIME("Slime"), VAMPIRE("Ghoul / Vampire"), UNDEAD("Wight / Skeleton"), DAEMON("Daemon");

    public final String displayName;
    Race(String displayName) { this.displayName = displayName; }

    public static Race byName(String n) {
        for (Race r : values()) if (r.name().equalsIgnoreCase(n)) return r;
        return null;
    }

    /** The family of a Tensura race id path (e.g. "enlightened_human" -> HUMAN). Unknown races (other add-ons) -> null. */
    public static Race ofTensuraRace(String path) {
        if (path == null) return null;
        String p = path.toLowerCase();
        if (p.contains("human")) return HUMAN;
        if (p.contains("elf")) return ELF;
        if (p.contains("dwarf")) return DWARF;
        if (p.contains("beast") || p.contains("harpy") || p.contains("bird")) return BEASTFOLK;
        if (p.contains("ogre") || p.contains("oni") || p.contains("kijin") || p.contains("divine_fighter")) return OGRE;
        if (p.contains("goblin")) return GOBLIN;
        if (p.contains("giant")) return GIANT;
        if (p.contains("orc") || p.contains("boar")) return ORC;
        if (p.contains("lizard") || p.contains("dragon")) return DRAGONEWT;
        if (p.contains("merfolk") || p.contains("fish")) return MERFOLK;
        if (p.contains("slime")) return SLIME;
        if (p.contains("ghoul") || p.contains("vampire")) return VAMPIRE;
        if (p.contains("wight") || p.contains("skeleton")) return UNDEAD;
        if (p.contains("daemon") || p.contains("devil")) return DAEMON;
        return null;
    }
}
