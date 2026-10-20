package com.newuniverse.nusmp.labyrinth.mega.decennial.arena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * Why: the registry of the thirty-six hardcoded boss halls. A boss profile names its hall by id ({@code graveyard}, {@code ruined_throne_room} ...); this class turns an id and a size into the plan of
 * that hall. It is a closed table in code on purpose (the owner asked for hardcoded halls, and a hall is a build with interactive parts that a datapack cannot describe), which also lets a test assert
 * that every hall a boss file names really exists. A plan is built on demand and the last few are kept (a floor of the Mega-Dungeon rebuilds the same hall for every party; building one costs about
 * a hundred thousand writes), so asking twice is cheap. The size is clamped to an odd number from 41 to 61: a hall in a floor of the Mega-Dungeon is built at 45 (the footprint of the Guardian's
 * hall), a hall an admin builds is built at the theme's own size.
 */
public final class ArenaThemes {
    private ArenaThemes() {}

    /** A hall: its id, its name, which side of the dungeon it belongs to ("holy", "evil" or "any"), the size it is built at when it stands alone, and how to build it. */
    public record Def(String id, String title, String side, int size, IntFunction<BossArenaStructure> builder) {}

    private static final Map<String, Def> DEFS = new LinkedHashMap<>();

    private static void add(String id, String title, String side, int size, IntFunction<BossArenaStructure> builder) {
        if (DEFS.put(id, new Def(id, title, side, size, builder)) != null) throw new IllegalStateException("duplicate arena theme " + id);
    }

    static {
        // the halls of the dead, the bled and the pit (Evil)
        add("graveyard", "The Weeping Graveyard", "evil", 55, ThemesUndead::graveyard);
        add("ruined_throne_room", "The Ruined Throne Room", "evil", 51, ThemesUndead::ruinedThroneRoom);
        add("bone_cathedral", "The Bone Cathedral", "evil", 55, ThemesUndead::boneCathedral);
        add("blood_arena", "The Blood Arena", "evil", 51, ThemesUndead::bloodArena);
        add("forbidden_library", "The Library of Forbidden Lore", "evil", 53, ThemesUndead::forbiddenLibrary);
        add("vampire_ballroom", "The Crimson Ballroom", "evil", 51, ThemesUndead::vampireBallroom);
        add("hell_pit", "The Hell Pit", "evil", 55, ThemesFiend::hellPit);
        add("abyss_rift_ring", "The Ring of the Rift", "evil", 57, ThemesFiend::abyssRiftRing);
        add("witch_hollow", "The Witch's Hollow", "evil", 51, ThemesFiend::witchHollow);
        add("plague_swamp", "The Plague Swamp", "evil", 53, ThemesFiend::plagueSwamp);
        add("spider_den", "The Spider's Den", "evil", 51, ThemesFiend::spiderDen);
        add("mind_hive", "The Hive of Minds", "evil", 53, ThemesFiend::mindHive);
        // the halls of the light (Holy)
        add("cathedral", "The Cathedral of Dawn", "holy", 55, ThemesHoly::cathedral);
        add("celestial_court", "The Celestial Court", "holy", 57, ThemesHoly::celestialCourt);
        add("sun_temple", "The Temple of the Sun", "holy", 55, ThemesHoly::sunTemple);
        add("hanging_gardens", "The Hanging Gardens", "holy", 55, ThemesHoly::hangingGardens);
        add("sky_bridge", "The Bridge of Sky", "holy", 61, ThemesHoly::skyBridge);
        add("crystal_sanctum", "The Crystal Sanctum", "holy", 53, ThemesHoly::crystalSanctum);
        add("moonlit_glade", "The Moonlit Glade", "holy", 55, ThemesHoly::moonlitGlade);
        add("storm_spire", "The Storm Spire", "holy", 53, ThemesHoly::stormSpire);
        // the halls of both roads
        add("volcanic_forge", "The Volcanic Forge", "any", 55, ThemesShared::volcanicForge);
        add("frozen_keep", "The Frozen Keep", "any", 51, ThemesShared::frozenKeep);
        add("sunken_temple", "The Sunken Temple", "any", 53, ThemesShared::sunkenTemple);
        add("dragon_hoard_cavern", "The Dragon's Hoard", "any", 61, ThemesShared::dragonHoardCavern);
        add("colosseum", "The Colosseum", "any", 61, ThemesShared::colosseum);
        add("mirror_hall", "The Hall of Mirrors", "any", 51, ThemesShared::mirrorHall);
        add("clockwork_vault", "The Clockwork Vault", "any", 55, ThemesShared::clockworkVault);
        add("void_observatory", "The Observatory of the Void", "any", 55, ThemesShared::voidObservatory);
        // the Underworld Root (Evil): the seven gate chambers and the root throne
        String[] ordinal = {"First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh"};
        for (int k = 1; k <= 7; k++) {
            final int gate = k;
            add("qliphoth_gate_" + k, "The " + ordinal[k - 1] + " Gate of the Underworld Root", "evil", 45 + 2 * (k - 1), size -> ThemesQliphoth.gate(gate, size));
        }
        add("qliphoth_root_throne", "The Root Throne", "evil", 59, ThemesQliphoth::rootThrone);
    }

    // ================================================================ lookups
    /** Every hall id, in registration order. */
    public static List<String> ids() { return Collections.unmodifiableList(new ArrayList<>(DEFS.keySet())); }

    public static boolean exists(String id) { return id != null && DEFS.containsKey(id); }

    public static Def def(String id) { return id == null ? null : DEFS.get(id); }

    public static int defaultSize(String id) {
        Def d = DEFS.get(id);
        return d == null ? BossArenaStructure.SECTOR_SIZE : d.size();
    }

    /** The size a request is built at: within 41..61 and odd (an even size is rounded up). */
    public static int normalSize(int size) {
        int s = Math.max(BossArenaStructure.MIN_SIZE, Math.min(BossArenaStructure.MAX_SIZE, size));
        return (s & 1) == 0 ? Math.min(BossArenaStructure.MAX_SIZE, s + 1) : s;
    }

    // ================================================================ building (with a small cache)
    private static final int CACHE = 6;
    private static final LinkedHashMap<String, BossArenaStructure> CACHED = new LinkedHashMap<>(16, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, BossArenaStructure> eldest) { return size() > CACHE; }
    };

    /** The plan of hall {@code id} at {@code size} (clamped and made odd), or null for an unknown id. Plans are shared: treat them as read-only. */
    public static BossArenaStructure build(String id, int size) {
        Def d = DEFS.get(id);
        if (d == null) return null;
        int s = normalSize(size);
        String key = id + "@" + s;
        synchronized (CACHED) {
            BossArenaStructure hit = CACHED.get(key);
            if (hit != null) return hit;
        }
        BossArenaStructure plan = d.builder().apply(s);
        synchronized (CACHED) {
            CACHED.put(key, plan);
        }
        return plan;
    }

    /** The plan at the hall's own size (an admin's hall). */
    public static BossArenaStructure build(String id) { return build(id, defaultSize(id)); }

    /** The plan at the size of a floor of the Mega-Dungeon (the Guardian's hall footprint). */
    public static BossArenaStructure buildForFloor(String id) { return build(id, BossArenaStructure.SECTOR_SIZE); }
}
