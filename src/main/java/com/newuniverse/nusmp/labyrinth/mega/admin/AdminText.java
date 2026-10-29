package com.newuniverse.nusmp.labyrinth.mega.admin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: every sentence the {@code /dungeon} admin tree says is a translation key under {@code message.nusmp.dungeon.admin.*} with its English text kept here, in ONE pure map. The
 * owner's rule is that new lang strings go into their own flat fragment ({@code tools/lang/dungeon_admin.json}) and the big {@code en_us.json} is not edited, so the code builds
 * every component with {@code Component.translatableWithFallback(key, english, args)}: before the fragment is merged a command still reads in plain English instead of printing a raw
 * key, afterwards a language file can change the wording. A unit test pins that the fragment and this map say exactly the same thing (same keys, same words, same order) so the two
 * can never drift. Placeholders are numbered ({@code %1$s}) because a translation may reorder them; arguments passed to them are only strings, numbers or components (the game cannot
 * send any other kind of argument to a client, and the sender of a command is usually a client). Pure.
 */
public final class AdminText {
    private AdminText() {}

    /** The prefix of every key of this module. */
    public static final String PREFIX = "message.nusmp.dungeon.admin.";

    private static final Map<String, String> EN = new LinkedHashMap<>();

    private static String put(String shortKey, String english) {
        String full = PREFIX + shortKey;
        if (EN.put(full, english) != null) throw new IllegalStateException("duplicate key " + full);
        return full;
    }

    // ---- general
    public static final String NEED_PLAYER = put("need_player", "This command needs a player to act on. Run it in game, or as a player with /execute as <player> run dungeon ...");
    public static final String NO_DIM = put("no_dim", "The Mega-Dungeon dimension is not loaded on this server.");
    public static final String NOT_IN_DUNGEON = put("not_in_dungeon", "You are not on a floor of the Mega-Dungeon (you are in %1$s).");
    public static final String NO_SECTOR = put("no_sector", "You are in the Mega-Dungeon but on no floor (a gap between two sectors). Walk onto a floor first.");
    public static final String FAILED = put("failed", "That did not work: %1$s. The server log has the details.");

    public static final String ROAD_HOLY = put("road.holy", "Holy road (Ascension)");
    public static final String ROAD_ABYSSAL = put("road.abyssal", "Abyssal road (Descent)");

    // ---- tp
    public static final String TP_RANGE = put("tp.range", "Floor %1$s is out of range. Use -1000 to +1000 (0 is the Sanctuary Hub).");
    public static final String TP_OK = put("tp.ok", "Teleported to floor %1$s (level %2$s, sector %3$s, party %4$s).");
    public static final String TP_OK_BOSS = put("tp.ok_boss", "This is a boss floor: the Guardian waits at the end of it.");
    public static final String TP_OK_BAND = put("tp.ok_band", "Floor %1$s lies inside a ten-floor sector (floors %2$s to %3$s): you stand at the sector's landing.");
    public static final String TP_HUB = put("tp.hub", "Teleported to Floor 0, the Sanctuary Hub.");
    public static final String TP_HUB_NONE = put("tp.hub_none", "Floor 0 cannot be reached: the Sanctuary dimension is not loaded.");
    public static final String TP_QUEUED = put("tp.queued", "Floor %1$s is not built yet. %2$s floor(s) are queued (about %3$s seconds); you are teleported the moment it stands. Use /dungeon tp again to replace the request.");
    public static final String TP_PROGRESS = put("tp.progress", "Building the way to floor %1$s: %2$s%% (%3$s floor(s) to go)");
    public static final String TP_REPLACED = put("tp.replaced", "The earlier teleport request to floor %1$s is dropped.");
    public static final String TP_LOCKED = put("tp.locked", "The Directional Lock closes the %1$s to you. Free yourself first: /multiverse mega floors unlock <you> (or floors set <you> <road>), or turn floors.opsBypass on.");
    public static final String TP_NO_PARTY = put("tp.no_party", "The dungeon has no free lane for your party. Raise party.maxParties.");
    public static final String TP_OFF = put("tp.off", "Floor %1$s does not exist yet and the dungeon cannot grow: mega.expansion is switched off.");
    public static final String TP_LIMIT = put("tp.limit", "The dungeon reached its limit of %1$s sectors (mega.maxSectors) before floor %2$s. Raise the limit in the server config.");
    public static final String TP_FAILED_GEN = put("tp.failed_gen", "A floor on the way failed to generate (sector %1$s). Retry it with /multiverse mega regen %1$s, then run the teleport again.");
    public static final String TP_STUCK = put("tp.stuck", "The dungeon has not moved for %1$s seconds, so the teleport to floor %2$s is cancelled. Check /multiverse mega status.");
    public static final String TP_REFUSED = put("tp.refused", "The generator refused the next floor: %1$s");
    public static final String TP_NO_PAD = put("tp.no_pad", "Sector %1$s has no landing to stand on (its data is incomplete). See the server log.");
    public static final String TP_CHUNK = put("tp.chunk", "Floor %1$s is loading...");

    // ---- skip
    public static final String SKIP_ZERO = put("skip.zero", "Skipping 0 floors does nothing.");
    public static final String SKIP_HUB_BACK = put("skip.hub_back", "You are on Floor 0: there is nothing behind it. Skip forward, or use /dungeon tp.");
    public static final String SKIP_GO = put("skip.go", "Skipping %1$s floor(s) %2$s from floor %3$s, to floor %4$s.");
    public static final String SKIP_FORWARD = put("skip.forward", "forward");
    public static final String SKIP_BACK = put("skip.back", "back");
    public static final String SKIP_CAPPED = put("skip.capped", "The skip was cut short: the road ends at floor %1$s (+-1000), and going back stops at Floor 0.");

    // ---- boss
    public static final String BOSS_LEVEL = put("boss.level", "Dungeon level %1$s does not exist. Use -100 to -1 (Abyssal road) or 1 to 100 (Holy road); 0 is the Hub.");
    public static final String BOSS_SPAWN_OK = put("boss.spawn_ok", "The boss of level %1$s (floor %2$s) appears at %3$s.");
    public static final String BOSS_SPAWN_FAIL = put("boss.spawn_fail", "No boss could be spawned for level %1$s here.");
    public static final String BOSS_KILL_OK = put("boss.kill_ok", "Slew %1$s boss(es) near you. A Mana Core drops for the Gatekeeper.");
    public static final String BOSS_KILL_NONE = put("boss.kill_none", "There is no boss within reach to slay.");

    // ---- rift
    public static final String RIFT_OK = put("rift.ok", "A %1$s rift tears open at %2$s.");
    public static final String RIFT_FAIL = put("rift.fail", "No rift could be opened at %1$s: the place is not free, or a shortcut needs a floor of the Mega-Dungeon under you.");
    public static final String RIFT_KIND_POCKET = put("rift.kind.pocket", "pocket");
    public static final String RIFT_KIND_SHORTCUT = put("rift.kind.shortcut", "shortcut");

    // ---- skill

    // ---- reset
    public static final String RESET_ALIGNMENT = put("reset.alignment", "Alignment cleared: no road, no lock, no record of the deepest floors, and the tracker forgot your floor.");
    public static final String RESET_WAYPOINT = put("reset.waypoint", "Waypoint cleared, with the memory of the deepest Landing: the next entry starts at the first floor.");
    public static final String RESET_WAYPOINT_NONE = put("reset.waypoint_none", "You had no waypoint and no deepest-Landing memory; nothing to clear.");
    public static final String RESET_ALL = put("reset.all", "Everything of the dungeon is cleared for you: alignment and waypoint.");

    // ---- debug
    public static final String DEBUG_ON = put("debug.on", "WFC debug ON for %1$s minutes (or until you leave the dimension). Green = on a loop, yellow = single link, red = dead end. Run it again to stop.");
    public static final String DEBUG_OFF = put("debug.off", "WFC debug OFF.");
    public static final String DEBUG_AUTO_TIME = put("debug.auto_time", "WFC debug switched itself off: the %1$s minutes are over.");
    public static final String DEBUG_AUTO_DIM = put("debug.auto_dim", "WFC debug switched itself off: you left the dimension.");
    public static final String DEBUG_NO_FLOOR = put("debug.no_floor", "WFC debug needs a built floor of the Mega-Dungeon under you.");
    public static final String DEBUG_SUMMARY = put("debug.summary", "Floor %1$s (sector %2$s): %3$s rooms, %4$s doorways, dead ends %5$s, loops %6$s, rooms on a loop %7$s, single-link rooms %8$s, separate parts %9$s.");
    public static final String DEBUG_VERDICT_OK = put("debug.verdict_ok", "OK: no dead ends, one connected floor.");
    public static final String DEBUG_VERDICT_BAD = put("debug.verdict_bad", "PROBLEM: %1$s dead end(s) and %2$s separate part(s); the red rooms and lines show where.");
    public static final String DEBUG_INFO = put("debug.info", "Floor %1$s, level %2$s, theme %3$s, seed %4$s.");
    public static final String DEBUG_INFO_MORE = put("debug.info_more", "Sector %1$s on the %2$s road, party %3$s, state %4$s, %5$s rooms%6$s.");
    public static final String DEBUG_INFO_BOSS = put("debug.info_boss", ", a boss floor");
    public static final String DEBUG_INFO_HUB = put("debug.info_hub", "Floor 0, the Sanctuary Hub: no level, no theme, no seed of its own.");
    public static final String DEBUG_INFO_WAYPOINT = put("debug.info_waypoint", "You are outside the dungeon. Your waypoint: %1$s.");
    public static final String DEBUG_INFO_NO_WAYPOINT = put("debug.info_no_waypoint", "none");
    public static final String DEBUG_INFO_WAYPOINT_AT = put("debug.info_waypoint_at", "floor %1$s, sector %2$s");

    // ---- help
    public static final String HELP_HEAD = put("help.head", "Dungeon admin commands (permission level 2). They act on you; use /execute as <player> run dungeon ... for someone else:");
    public static final String HELP_TP = put("help.tp", "Teleport to a floor (-1000 to +1000, 0 is the Hub); a floor not built yet is queued and you go the moment it stands.");
    public static final String HELP_SKIP = put("help.skip", "Go forward one floor along your road (a negative number goes back towards the Hub).");
    public static final String HELP_SKIP_N = put("help.skip_n", "Go forward that many floors (-1000 to 1000).");
    public static final String HELP_BOSS_SPAWN = put("help.boss_spawn", "Spawn the Decennial boss of a dungeon level (-100 to -1, 1 to 100) where you stand.");
    public static final String HELP_BOSS_KILL = put("help.boss_kill", "Slay the boss near you; its Mana Core drops to test the Gatekeeper.");
    public static final String HELP_RIFT_POCKET = put("help.rift_pocket", "Open a pocket-dungeon rift at your crosshair.");
    public static final String HELP_RIFT_SHORTCUT = put("help.rift_shortcut", "Open a floor-shortcut rift at your crosshair.");
    public static final String HELP_RESET_ALIGNMENT = put("help.reset_alignment", "Forget your road, the lock and the deepest floors.");
    public static final String HELP_RESET_WAYPOINT = put("help.reset_waypoint", "Forget your waypoint and the deepest Landing.");
    public static final String HELP_RESET_ALL = put("help.reset_all", "Both of the above.");
    public static final String HELP_DEBUG_WFC = put("help.debug_wfc", "Toggle the particle view of this floor's room graph (green loop, yellow single link, red dead end).");
    public static final String HELP_DEBUG_INFO = put("help.debug_info", "Say the floor, level, theme and seed you are standing in.");
    public static final String HELP_ADMIN_HELP = put("help.admin_help", "This list.");

    /** The English text of a key of this module, or null for a key that is not one of ours. */
    public static String englishOrNull(String key) { return EN.get(key); }

    /** The English text of a key, or the key itself when it is unknown (never null, so a component can always be built). */
    public static String english(String key) {
        String s = EN.get(key);
        return s == null ? key : s;
    }

    /** Every key with its English text, in declaration order. */
    public static Map<String, String> all() { return Collections.unmodifiableMap(EN); }
}
