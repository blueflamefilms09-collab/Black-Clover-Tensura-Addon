package com.newuniverse.nusmp.labyrinth.mega.enc;

import java.util.List;
import java.util.Set;

/**
 * Why: the Dungeon Master's encounter table is DATA (one file per theme, {@code data/nusmp/mega/encounters/<theme>.json}), because what lives
 * in a region is the part of the design the owner will keep tuning, and it must change without a rebuild. This class is the parsed form of one
 * file, immutable and free of Minecraft types so the same shape is used by the loader, the roll, the admin command and the tests that read the
 * shipped files. A table is a list of entries; an entry names a size of trouble ({@link Band}), the threat window in which it may happen, what
 * the party must be doing for it to happen ({@link Needs}: dark, lit, noisy), the monsters that make it ({@link MobSpec}: an explicit entity id
 * or "a member of this faction", chosen at run time from the faction's entity-type tag) and how it announces itself ({@link Cue}). The two
 * table-level knobs let a theme lean: {@code rollBonus} adds to the d20 of its sectors (a hunting ground), {@code budgetScale} makes its fights
 * bigger or smaller.
 */
public record EncounterTable(String id, String description, int rollBonus, double budgetScale, String defaultFaction, List<Entry> entries) {
    /** The faction ids of the Mega-Dungeon's contract (the entity-type tags are {@code nusmp:mega/<id>}). */
    public static final Set<String> FACTIONS = Set.of("undead", "goblin", "ogre", "beast", "slime", "construct", "cultist", "demon", "elemental", "aquatic", "insect");

    public static final EncounterTable EMPTY = new EncounterTable("empty", "", 0, 1.0, null, List.of());

    public EncounterTable {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public enum Kind { ENTITY, FACTION }

    /**
     * One kind of monster in an encounter. {@code entity} is an id such as {@code minecraft:zombie} for {@link Kind#ENTITY}; {@code faction} is
     * an optional override for {@link Kind#FACTION} (null means: the entry's faction, else the native faction of the room, else the table's).
     */
    public record MobSpec(Kind kind, String entity, String faction, CountRange count) {}

    /** What the party must be doing. {@code null} means "do not care"; {@code true} requires it, {@code false} requires its opposite. */
    public record Needs(Boolean dark, Boolean lit, Boolean noisy) {
        public static final Needs NONE = new Needs(null, null, null);

        public boolean met(boolean isDark, boolean isLit, boolean isNoisy) {
            return (dark == null || dark == isDark) && (lit == null || lit == isLit) && (noisy == null || noisy == isNoisy);
        }

        public boolean any() { return dark != null || lit != null || noisy != null; }
    }

    /** The telegraph: a sound (registry id), its volume and pitch, and a simple particle (registry id) with a count. Any part may be blank. */
    public record Cue(String sound, float volume, float pitch, String particle, int particles) {
        public static final Cue NONE = new Cue("", 1f, 1f, "", 0);
    }

    /**
     * One row of the table. {@code hunt} is the number of ticks the mobs go looking for the party (-1: the band's default). {@code minRange} and
     * {@code maxRange} are the distance band (in blocks, from the nearest member of the party) the mobs appear in (0 and 0: the config's default).
     * {@code cells} is how many different places the group is split over.
     */
    public record Entry(String id, int weight, Band band, int minThreat, int maxThreat, String faction, List<MobSpec> mobs, Needs needs,
                        boolean scaleWithParty, int minParty, String message, double minRange, double maxRange, int cells, int hunt, Cue cue) {
        public Entry {
            mobs = mobs == null ? List.of() : List.copyOf(mobs);
            if (needs == null) needs = Needs.NONE;
            if (cue == null) cue = Cue.NONE;
        }

        public boolean fitsThreat(int threat) { return threat >= minThreat && threat <= maxThreat; }

        /** Does the table ask for the monsters to come from a faction at all? */
        public boolean usesFaction() {
            for (MobSpec m : mobs) if (m.kind() == Kind.FACTION) return true;
            return false;
        }
    }

    /** The entry with this id, or null. */
    public Entry byId(String entryId) {
        if (entryId == null) return null;
        for (Entry e : entries) if (e.id().equals(entryId)) return e;
        return null;
    }
}
