package com.newuniverse.nusmp.boss.bars;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Why: which of the 36 styles a boss wears must be decided the same way on the server (it names the style in the packet) and in the tests (every one of the 200 Decennial bosses, every
 * arc and every floor boss must land on a style that exists), so it is a pure table and nothing else. The order of the questions for a Decennial boss is: its archetype (the most specific:
 * "lich", "famous:strahd"), then the theme of its hall (a frozen keep is ice, a clockwork vault is clockwork), then its creature type, then its side. Arc characters go by their saga (the
 * Midnight Sun, the Spade Kingdom, the Seven Deadly Sins, DanMachi) and a few by name; the named bosses of the mod go by their entity type. Never throws, never returns null from the
 * {@code for*} methods that end in a fallback.
 */
public final class BarStyleResolver {
    private BarStyleResolver() {}

    private static final Map<String, String> ARCHETYPE = new HashMap<>();
    private static final Map<String, String> THEME = new HashMap<>();
    private static final Map<String, String> CREATURE = new HashMap<>();
    private static final Map<String, String> ENTITY = new HashMap<>();
    private static final Map<String, String> CHARACTER = new HashMap<>();
    private static final Map<String, String> SAGA = new HashMap<>();

    private static void put(Map<String, String> m, String style, String... keys) {
        for (String k : keys) m.put(k, style);
    }

    static {
        put(ARCHETYPE, BarStyleIds.LICH, "lich", "necromancer", "famous:acererak", "famous:lich_king", "famous:vecna");
        put(ARCHETYPE, BarStyleIds.FALLEN_KING, "fallen_king", "dread_knight");
        put(ARCHETYPE, BarStyleIds.UNDEAD, "banshee_matriarch", "wraith_king", "shadow_reaper", "gladiator_revenant");
        put(ARCHETYPE, BarStyleIds.PLAGUE, "plague_lord", "rot_wyrm", "hag", "spider_queen", "famous:lolth");
        put(ARCHETYPE, BarStyleIds.WATER, "leviathan", "famous:kraken", "tide_warden");
        put(ARCHETYPE, BarStyleIds.SUPREME_DEVIL, "archdevil", "famous:asmodeus");
        put(ARCHETYPE, BarStyleIds.DEVIL, "famous:zariel", "demon_lord", "famous:demogorgon", "famous:orcus", "doom_herald", "hell_warden");
        put(ARCHETYPE, BarStyleIds.BLOOD, "sin_duchess", "vampire_lord", "famous:strahd", "sanguine_prophet");
        put(ARCHETYPE, BarStyleIds.EVIL, "cult_prophet");
        put(ARCHETYPE, BarStyleIds.CLOCKWORK, "clockwork_tyrant", "guardian_golem", "time_keeper");
        put(ARCHETYPE, BarStyleIds.MIRROR, "mirror_duke", "prism_sage", "inquisitor");
        put(ARCHETYPE, BarStyleIds.VOID, "abyssal_horror", "eye_tyrant", "mind_flayer", "star_eater", "famous:beholder", "famous:mind_flayer_elder_brain", "star_oracle");
        put(ARCHETYPE, BarStyleIds.DRAGON, "dracolich", "famous:tiamat", "famous:bahamut", "order_dragon", "platinum_wyrm");
        put(ARCHETYPE, BarStyleIds.GOLIATH, "famous:tarrasque", "stone_colossus");
        put(ARCHETYPE, BarStyleIds.ANGEL, "archangel", "herald", "cherub", "sky_knight", "archivist_angel");
        put(ARCHETYPE, BarStyleIds.ARCHON, "archon", "judgement_angel", "seraph", "famous:solar");
        put(ARCHETYPE, BarStyleIds.STORM, "thunder_seraph");
        put(ARCHETYPE, BarStyleIds.FIRE, "phoenix_avatar", "forge_saint", "forge_tyrant");
        put(ARCHETYPE, BarStyleIds.ICE, "crystal_warden", "frost_sentinel", "frost_tyrant");
        put(ARCHETYPE, BarStyleIds.FOREST, "griffon_lord", "verdant_archon", "unicorn_elder", "moon_priestess");
        put(ARCHETYPE, BarStyleIds.HOLY, "knight_commander", "martyr", "paladin", "sun_priest");

        put(THEME, BarStyleIds.ANGEL, "celestial_court");
        put(THEME, BarStyleIds.STORM, "sky_bridge", "storm_spire");
        put(THEME, BarStyleIds.HOLY, "colosseum", "cathedral");
        put(THEME, BarStyleIds.ARCHON, "sun_temple");
        put(THEME, BarStyleIds.MIRROR, "crystal_sanctum", "mirror_hall");
        put(THEME, BarStyleIds.ICE, "frozen_keep");
        put(THEME, BarStyleIds.DRAGON, "dragon_hoard_cavern");
        put(THEME, BarStyleIds.VOID, "void_observatory", "mind_hive");
        put(THEME, BarStyleIds.FOREST, "hanging_gardens", "moonlit_glade");
        put(THEME, BarStyleIds.FIRE, "volcanic_forge");
        put(THEME, BarStyleIds.CLOCKWORK, "clockwork_vault");
        put(THEME, BarStyleIds.WATER, "sunken_temple");
        put(THEME, BarStyleIds.UNDERWORLD, "abyss_rift_ring", "qliphoth_root_throne");
        put(THEME, BarStyleIds.LICH, "forbidden_library");
        put(THEME, BarStyleIds.UNDEAD, "graveyard", "bone_cathedral");
        put(THEME, BarStyleIds.DEVIL, "hell_pit");
        put(THEME, BarStyleIds.FALLEN_KING, "ruined_throne_room");
        put(THEME, BarStyleIds.BLOOD, "vampire_ballroom", "blood_arena");
        put(THEME, BarStyleIds.PLAGUE, "plague_swamp", "spider_den", "witch_hollow");
        for (int g = 1; g <= 7; g++) THEME.put("qliphoth_gate_" + g, BarStyleIds.QLIPHOTH_GATE);

        put(CREATURE, BarStyleIds.DRAGON, "dragon");
        put(CREATURE, BarStyleIds.DEVIL, "devil", "demon", "fiend");
        put(CREATURE, BarStyleIds.UNDEAD, "undead", "spirit");
        put(CREATURE, BarStyleIds.BLOOD, "vampire");
        put(CREATURE, BarStyleIds.ANGEL, "celestial");
        put(CREATURE, BarStyleIds.CLOCKWORK, "construct");
        put(CREATURE, BarStyleIds.FIRE, "elemental");
        put(CREATURE, BarStyleIds.GOLIATH, "giant");
        put(CREATURE, BarStyleIds.VOID, "aberration");
        put(CREATURE, BarStyleIds.FOREST, "fey", "plant", "beast");
        put(CREATURE, BarStyleIds.PLAGUE, "hag", "monstrosity");
        put(CREATURE, BarStyleIds.EVIL, "cultist");

        put(ENTITY, BarStyleIds.ZOGRATIS, "dante_zogratis", "vanica_zogratis", "zenon_zogratis", "devil_avatar");
        put(ENTITY, BarStyleIds.LUCIUS, "lucius_zogratis");
        put(ENTITY, BarStyleIds.GOLIATH, "goliath");
        put(ENTITY, BarStyleIds.CHARYBDIS, "charybdis_spawn");
        put(ENTITY, BarStyleIds.GOD_OF_ORDER, "god_of_order");
        put(ENTITY, BarStyleIds.ZAGRED, "zagred");
        put(ENTITY, BarStyleIds.SPIRIT_KING, "spirit_king");
        put(ENTITY, BarStyleIds.WARDEN, "floor_warden", "labyrinth_guardian");

        put(CHARACTER, BarStyleIds.MIDNIGHT_SUN, "fana", "patolli", "rades", "rhya");
        put(CHARACTER, BarStyleIds.ZAGRED, "zagred");
        put(CHARACTER, BarStyleIds.LUCIUS, "lucius_zogratis", "lucius");
        put(CHARACTER, BarStyleIds.SPADE_KINGDOM, "morris", "lucifero");

        put(SAGA, BarStyleIds.MAGIC_KNIGHTS, "elf_reincarnation", "allies");
        put(SAGA, BarStyleIds.SPADE_KINGDOM, "spade_raid");
        put(SAGA, BarStyleIds.LUCIUS, "wizard_king");
        put(SAGA, BarStyleIds.SEVEN_SINS, "seven_deadly_sins");
        put(SAGA, BarStyleIds.DANMACHI, "danmachi");
    }

    private static String low(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT); }

    /** The style of a side alone: the last fallback of everything ("holy" or "evil"; anything else is evil, the dungeon's default). */
    public static String bySide(String side) { return "holy".equals(low(side)) ? BarStyleIds.HOLY : BarStyleIds.EVIL; }

    /** A Decennial boss: archetype, hall theme, creature type, side, in that order. {@code devil} is the profile's devil name ("" if none). */
    public static String forDecennial(String side, String archetype, String theme, String creature, String devil) {
        String t = low(theme), a = low(archetype);
        if (!low(devil).isEmpty() && t.equals("qliphoth_root_throne")) return BarStyleIds.SUPREME_DEVIL;     // the three Supreme Devils on the last levels
        String s = ARCHETYPE.get(a);
        if (s != null) return s;
        s = THEME.get(t);
        if (s != null) return s;
        s = CREATURE.get(low(creature));
        return s != null ? s : bySide(side);
    }

    /** A character of the arcs: a few by name, the rest by saga; a boss of an unknown saga falls to the Magic Knights. */
    public static String forArc(String sagaId, String characterId) {
        String s = CHARACTER.get(low(characterId));
        if (s != null) return s;
        s = SAGA.get(low(sagaId));
        return s != null ? s : BarStyleIds.MAGIC_KNIGHTS;
    }

    /** A boss by the path of its entity type ({@code nusmp:<path>}), or null when the path is not a named boss (the caller then asks something more specific). */
    public static String forEntityPath(String path) { return ENTITY.get(low(path)); }

    /** The style of a Labyrinth Guardian that is not a Decennial boss: a pocket tomb's mega-boss wears the gold tomb frame, any other Guardian the Warden frame. */
    public static String forGuardian(boolean pocket) { return pocket ? BarStyleIds.POCKET_TOMB : BarStyleIds.WARDEN; }

    /** Every theme the table knows (for the test that every hall resolves). */
    public static java.util.Set<String> knownThemes() { return java.util.Collections.unmodifiableSet(THEME.keySet()); }
}
