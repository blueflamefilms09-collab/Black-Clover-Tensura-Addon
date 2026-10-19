package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Why: the floor report of the System (the window that says, on every arrival, which floor this is, which way it goes, what THEME it wears and which
 * MECHANICS are in effect and how to beat them) needs words, and the owner's rule is that new lang strings go in their own fragment
 * ({@code tools/lang/system_floor_info.json}) and the big {@code en_us.json} is not edited. So the words live here, in ONE pure map under the System's usual key
 * prefix: the server (chat, command), the client (window) and the pop-up lines all read their English fallback from this map through
 * {@link SystemText#english}, so a key that is not merged into the game's lang file yet still shows real words and never a raw key, and a unit test pins that the
 * fragment file and this map say exactly the same thing. The one-line description of every theme is here too, keyed by the theme id, with
 * {@link #themeDescKey}; a theme added later has no entry and falls back to a generic line built from its data (see {@link FloorInfoRules#genericThemeDesc}),
 * so new themes and new mechanics are covered without touching this file. Pure.
 */
public final class FloorInfoText {
    private FloorInfoText() {}

    private static final Map<String, String> EN = new LinkedHashMap<>();

    private static String put(String shortKey, String english) {
        String full = SystemText.PREFIX + "floor." + shortKey;
        EN.put(full, english);
        return full;
    }

    // ---- the headline
    public static final String TITLE = put("title", "FLOOR %1$s  -  %2$s");
    public static final String DIR_UP = put("dir.up", "UP");
    public static final String DIR_DOWN = put("dir.down", "DOWN");
    public static final String ROAD_HOLY = put("road.holy", "HOLY ROAD: the way on climbs UP");
    public static final String ROAD_ABYSSAL = put("road.abyssal", "ABYSSAL ROAD: the way on descends DOWN");
    public static final String NOTE_ARRIVE = put("note.arrive", "You arrive on floor %1$s.");
    public static final String NOTE_FORWARD = put("note.forward", "You go %1$s, to floor %2$s.");
    public static final String NOTE_BACK = put("note.back", "You go back %1$s, to floor %2$s.");
    public static final String NOTE_REQUEST = put("note.request", "Floor report, as it stands now.");
    public static final String NOTE_UPDATE = put("note.update", "The floor has changed.");
    public static final String BOSS = put("boss", "BOSS FLOOR: the Guardian waits at the end of this floor.");

    // ---- the theme
    public static final String THEME = put("theme", "THEME: %1$s");
    public static final String THEME_NONE = put("theme.none", "THEME: not shaped yet");
    public static final String ALIGN_HOLY = put("align.holy", "HOLY ROAD");
    public static final String ALIGN_ABYSSAL = put("align.abyssal", "ABYSSAL ROAD");
    public static final String ALIGN_ANY = put("align.any", "BOTH ROADS");
    public static final String ALIGN_ONLY = put("align.only", "%1$s ONLY");
    public static final String ALIGN_FAVOURED = put("align.favoured", "%1$s FAVOURED");
    public static final String DANGER = put("danger", "DANGER x%1$s");
    public static final String THEME_GENERIC = put("theme.generic", "A floor of its own palette. Its Crucible leans towards: %1$s.");
    public static final String THEME_GENERIC_PLAIN = put("theme.generic_plain", "A floor of its own palette; nothing more is recorded about it.");

    // ---- the mechanics
    public static final String MECH_HEAD = put("mech.head", "FLOOR MECHANICS (%1$s)");
    public static final String MECH_NONE = put("mech.none", "No floor-wide mechanic is in effect on this floor.");
    public static final String MECH_OFF = put("mech.off", "Mechanics are switched off on this server.");
    public static final String MECH_COUNTER = put("mech.counter", "Counter: %1$s");
    public static final String MECH_COUNTER_NONE = put("mech.counter_none", "Counter: none. Survive it, or leave its area.");
    public static final String MECH_COUNTER_UNKNOWN = put("mech.counter_unknown", "Counter: see the mechanic's own hint in the room.");
    public static final String MECH_STATE_ARMED = put("mech.state.armed", "ACTIVE");
    public static final String MECH_STATE_WAITING = put("mech.state.waiting", "WAITING");
    public static final String MECH_STATE_SOLVED = put("mech.state.solved", "OVERCOME");
    public static final String MECH_HERE_HEAD = put("mech.here_head", "ACTIVE HERE (%1$s)");
    public static final String MECH_ROOMS = put("mech.rooms", "%1$s more mechanic(s) wait in the rooms of this floor. You are told when you step into one.");
    public static final String MECH_MORE = put("mech.more", "+%1$s more");
    public static final String MECH_SCOPE_FLOOR = put("mech.scope.floor", "whole floor");
    public static final String MECH_SCOPE_ROOM = put("mech.scope.room", "this room");
    public static final String MECH_SCOPE_ZONE = put("mech.scope.zone", "this spot");
    public static final String MECH_SCOPE_BOSS = put("mech.scope.boss", "boss arena");

    // ---- the depth
    public static final String DEPTH = put("depth", "DEPTH %1$s: monsters +%2$s%% health and +%3$s%% damage, mind level %4$s, kills give x%5$s experience.");
    public static final String DEPTH_FLAT = put("depth.flat", "DEPTH %1$s: the first floor, monsters have their base strength.");

    // ---- the pop-up lines and the hints
    public static final String TOAST_MECH_NEW = put("toast.mech_new", "MECHANIC: %1$s");
    public static final String TOAST_MECH_DONE = put("toast.mech_done", "MECHANIC OVERCOME: %1$s");
    public static final String TOAST_MECH_END = put("toast.mech_end", "MECHANIC ENDED: %1$s");
    public static final String HINT = put("hint", "Press %1$s, or type /dungeon floor, to read this again.");
    public static final String FOOT_STUCK = put("foot.stuck", "Stuck? Type /dungeon unstuck.");

    // ---- commands
    public static final String CMD_OFF = put("cmd.off", "The System floor report is switched off on this server.");
    public static final String CMD_NOT_HERE = put("cmd.not_here", "You are not on a floor of the Mega-Dungeon.");
    public static final String CMD_SENT = put("cmd.sent", "Floor report sent to %1$s.");
    public static final String CMD_HEAD = put("cmd.head", "[SYSTEM] Floor report");

    // ---- the key
    public static final String KEY_FLOOR = "key.nusmp.system_floor";
    static {
        EN.put(KEY_FLOOR, "Show the floor report (theme and mechanics)");
    }

    // ---- one line for every theme that exists today (a later theme falls back to the generic line)
    private static final Map<String, String> THEME_DESC = new LinkedHashMap<>();

    private static void theme(String id, String english) {
        THEME_DESC.put(id, english);
        EN.put(themeDescKey(id), english);
    }

    /** The lang key of a theme's one-line description. */
    public static String themeDescKey(String themeId) { return SystemText.PREFIX + "floor.theme." + themeId + ".desc"; }

    static {
        theme("gravity", "The baseline deepslate hold of falling stone, heavy weights and lightning rods.");
        theme("blood", "Nether brick and crimson wood: blood, bone and torture, with the Crimson Reliquary at its heart.");
        theme("spatial", "End stone, purpur and crying obsidian: folded space, mirrors and doors that lead nowhere.");
        theme("flame", "Blackstone, magma and lava forges: heat, sparks and molten channels.");
        theme("frost", "Packed ice, snow and spruce: a freezing hold where the cold slows everything.");
        theme("verdant", "Mossy stone, azalea and cherry: an overgrown ruin the forest has taken back.");
        theme("storm", "Oxidised copper, tuff and lightning rods: a vault that crackles with the weather.");
        theme("shadow", "Polished blackstone, sculk and soul lanterns: halls of shadow that hide what stalks them.");
        theme("astral", "Quartz, calcite, prismarine and gold: a cold observatory of stars.");
        theme("radiant", "White quartz, smooth stone and gold with light everywhere, the air thick with Light Rukh.");
        theme("abyss", "Dark organic rock, sculk, moss and netherrack growing through deepslate, lit by soul fire.");
        theme("feywild_grove", "The Feywild: giant glowing flowers and mushrooms, will-o'-wisps, tricks and illusions.");
        theme("dwarven_forgehold", "A dwarven hold of dressed stone, copper and iron, anvils, lava behind glass and a vault of kings.");
        theme("crystal_geode", "A giant amethyst geode of calcite and basalt, budding crystals, crystal bridges and a resonance puzzle.");
        theme("sunken_temple", "The temple of a forgotten good god, flooded to the knees: prismarine, sea lanterns and tidal gates.");
        theme("holy_church", "A great cathedral: nave and pews, stained glass, a bell tower, a crypt and a reliquary.");
        theme("celestial_court", "The marble terraces, golden gates and colonnades of a heavenly court.");
        theme("sky_citadel", "A cloud giant's castle in the sky: white stone, wind and long drops.");
        theme("mountain_monastery", "A monks' retreat of timber and paper: a dojo, a bell and a hall of prayer wheels.");
        theme("cursed_archive", "A library of forbidden knowledge: endless shelves, reading halls, chained tomes and a scriptorium.");
        theme("necropolis", "A city of the dead: sandstone catacombs, bone pillars, mausoleums and bone bridges over an abyss.");
        theme("vampire_castle", "A grey gothic keep with crimson trim: a dining hall, a desecrated chapel, coffin crypts and mirrors that show nothing.");
        theme("haunted_manor", "A haunted house: a nursery with a rocking horse, a ballroom of dancing ghosts, long corridors and sudden big rooms.");
        theme("fungal_underdark", "The Underdark: forests of huge mushrooms, glowing spores, obsidian drow halls and a dark lake.");
        theme("infernal_foundry", "The Nine Hells as a factory: red nether brick, chains, cages, lava crucibles, anvils and a contract hall.");
        theme("drowned_crypt", "A sunken crypt of dark prismarine and mossy stone, barnacles, kelp-grown sarcophagi, tide gates and air pockets.");
        theme("spider_warrens", "A spider queen's warren: black stone and tuff veiled in cobweb, silk bridges, egg sacs and the queen's lair.");
    }

    /** The built-in one-line description of a theme, or null when none is recorded (a theme added after this file). */
    public static String themeDesc(String themeId) { return themeId == null ? null : THEME_DESC.get(themeId.toLowerCase(Locale.ROOT)); }

    /** The theme ids that have a built-in description, in the order of the enum. */
    public static java.util.Set<String> describedThemes() { return Collections.unmodifiableSet(THEME_DESC.keySet()); }

    /** The English text of a key of this module, or null when the key is not one of ours. */
    public static String englishOrNull(String key) { return key == null ? null : EN.get(key); }

    /** Every key with its English text, in declaration order (the lang fragment is written from this). */
    public static Map<String, String> all() { return Collections.unmodifiableMap(EN); }
}
