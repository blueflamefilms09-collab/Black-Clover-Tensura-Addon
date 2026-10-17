package com.newuniverse.nusmp.labyrinth.mega.gear;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: a chest of the Mega-Dungeon may hold one piece of standard gear, but a loot table is only a resource id and the global loot modifier sees EVERY table
 * that is rolled, so something has to decide which ids are chests worth a piece, which are treasure chests worth a better one, and which must never get one.
 * That decision is a few string rules, so it lives here with no Minecraft type and is pinned by tests. A table counts only if its path has a {@code chests/}
 * segment (the convention of vanilla and of this mod; a mob's table, a block's, a fishing or an archaeology table never has it). A TREASURE table is one whose
 * path names something precious (a vault, a treasure, a boss, a guardian, a crucible, a reward): it rolls the better grades more often. And the
 * relic-bearing tables ({@code nusmp:chests/dungeon_relic_vault} and {@code nusmp:chests/labyrinth_pedestal}, the two the relic system and the Crucible's offer
 * are built on) are NEVER touched, whatever a data pack says ({@link #ALWAYS_EXCLUDED}): the pedestal holds exactly the relic it was made for, and the owner's
 * rule is that the Crucible is the only source of the high relics, so the standard gear stays out of those containers altogether. The data file
 * ({@code tables.json}, section {@code chests}) can add excluded ids and treasure words; it can never remove the two fixed exclusions. Pure.
 *
 * @param excluded      full ids ({@code namespace:path}, lower case) that never get gear, added by the data file to {@link #ALWAYS_EXCLUDED}
 * @param treasureWords lower-case words; a chest table whose path contains one is a treasure table
 */
public record ChestRules(Set<String> excluded, List<String> treasureWords) {

    /** What a chest table is for the gear. */
    public enum Kind { NONE, STANDARD, TREASURE }

    /** The two tables that carry relics. Fixed in code on purpose: no data file can switch this protection off. */
    public static final Set<String> ALWAYS_EXCLUDED = Set.of("nusmp:chests/dungeon_relic_vault", "nusmp:chests/labyrinth_pedestal");

    /** The treasure words the shipped file lists; also what a file with no {@code chests} section gets, so an old or partial data pack behaves the same. */
    public static final List<String> DEFAULT_TREASURE_WORDS = List.of("treasure", "vault", "boss", "guardian", "crucible", "antechamber", "reward");

    /** The rules of a table file that says nothing about chests. */
    public static final ChestRules DEFAULT = new ChestRules(Set.of(), DEFAULT_TREASURE_WORDS);

    public ChestRules {
        List<String> words = new ArrayList<>();
        if (treasureWords != null) for (String w : treasureWords) if (w != null && !w.isBlank()) words.add(w.trim().toLowerCase(Locale.ROOT));
        treasureWords = List.copyOf(words);
        List<String> ids = new ArrayList<>();
        if (excluded != null) for (String id : excluded) if (id != null && !id.isBlank()) ids.add(id.trim().toLowerCase(Locale.ROOT));
        excluded = Set.copyOf(ids);
    }

    /**
     * What a table is: NONE for anything that is not a chest table (no {@code chests/} segment, a null or an unknown id) and for an excluded id,
     * TREASURE when the path holds a treasure word, STANDARD otherwise. Case does not matter.
     */
    public Kind classify(String namespace, String path) {
        if (namespace == null || path == null) return Kind.NONE;
        String p = path.toLowerCase(Locale.ROOT);
        if (!p.contains("chests/")) return Kind.NONE;
        String full = namespace.toLowerCase(Locale.ROOT) + ":" + p;
        if (ALWAYS_EXCLUDED.contains(full) || excluded.contains(full)) return Kind.NONE;
        for (String w : treasureWords) if (p.contains(w)) return Kind.TREASURE;
        return Kind.STANDARD;
    }
}
