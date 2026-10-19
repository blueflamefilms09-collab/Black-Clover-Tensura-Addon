package com.newuniverse.nusmp.labyrinth.mega.lock;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: every word of the escape rules (the label over a release rune, what the System says when a seal closes, opens, or a player types {@code /dungeon unstuck}) is a lang key
 * under {@code message.nusmp.mega.system.lock.*} with its English fallback kept here, in ONE pure map, for the reason the System's other texts are: the lead merges the lang
 * fragment ({@code tools/lang/system_floor_info.json}, which is written from this map and from {@code FloorInfoText}) after the code, so every line must read properly before
 * that, and a unit test pins that the fragment and the map cannot drift. The keys share the System prefix so a pop-up line sent through {@code SystemSync.toast} resolves on the
 * client with the System's own lookup. Pure.
 */
public final class LockText {
    private LockText() {}

    private static final String PREFIX = "message.nusmp.mega.system.lock.";
    private static final Map<String, String> EN = new LinkedHashMap<>();

    private static String put(String shortKey, String english) {
        String full = PREFIX + shortKey;
        EN.put(full, english);
        return full;
    }

    // ---- the rune
    public static final String RUNE_RELEASE = put("rune.release", "RELEASE RUNE");
    public static final String RUNE_EXIT = put("rune.exit", "EXIT RUNE");
    public static final String RUNE_RELEASE_SUB = put("rune.release_sub", "Right-click: open the way out");
    public static final String RUNE_EXIT_SUB = put("rune.exit_sub", "Right-click: leave the arena, the boss is restored");

    // ---- the seal
    public static final String SEALED = put("sealed", "The way out is sealed. A release rune appears in %1$s seconds. You can also type /dungeon unstuck.");
    public static final String SEALED_BOSS = put("sealed_boss", "The arena is closed while the boss lives. The exit rune is here: right-click it to leave and reset the fight.");
    public static final String RUNE_SHOWN = put("rune_shown", "A release rune has appeared. Right-click it, or type /dungeon release.");
    public static final String TOAST_SEALED = put("toast.sealed", "SEALED: A RELEASE RUNE APPEARS IN %1$s S");
    public static final String TOAST_RUNE = put("toast.rune", "RELEASE RUNE APPEARED");
    public static final String TOAST_OPEN = put("toast.open", "THE WAY IS OPEN");
    public static final String TOAST_RESCUED = put("toast.rescued", "THE SYSTEM TOOK YOU TO SAFETY");

    // ---- why a seal opened
    public static final String OPEN_ABSENT = put("open.absent", "The seal opens: nobody was left inside.");
    public static final String OPEN_BOSS_DEAD = put("open.boss_dead", "The seal opens: the boss is dead.");
    public static final String OPEN_MAX_TIME = put("open.max_time", "The seal gives way: it held for too long.");
    public static final String OPEN_UNSOLVABLE = put("open.unsolvable", "The seal opens: the party present could not have beaten it.");
    public static final String OPEN_RESCUE = put("open.rescue", "The seal opens: nothing hostile was left in the room.");
    public static final String OPEN_RUNE = put("open.rune", "%1$s used the rune. The way is open.");
    public static final String OPEN_COMMAND = put("open.command", "%1$s released the seal. The way is open.");
    public static final String OPEN_ADMIN = put("open.admin", "An operator released the seal.");

    // ---- /dungeon release
    public static final String RELEASE_NONE = put("release.none", "You are not inside a sealed room.");
    public static final String RELEASE_WAIT = put("release.wait", "The release rune is not ready yet: %1$s seconds. The seal opens by itself within %2$s seconds at the latest.");

    // ---- /dungeon status
    public static final String STATUS_FREE = put("status.free", "You are not sealed in.");
    public static final String STATUS_SEALED = put("status.sealed", "Sealed in by %1$s for %2$s seconds. Rune in %3$s seconds. It opens by itself within %4$s seconds at the latest.");
    public static final String STATUS_RUNE_READY = put("status.rune_ready", "The release rune is ready.");

    // ---- /dungeon unstuck
    public static final String UNSTUCK_OK = put("unstuck.ok", "The System takes you to %1$s.");
    public static final String UNSTUCK_COST = put("unstuck.cost", "It cost %1$s experience level(s).");
    public static final String UNSTUCK_FREE = put("unstuck.free", "You were sealed in, so the System asks nothing for this.");
    public static final String UNSTUCK_PARTY = put("unstuck.party", "%1$s party member(s) were taken to safety.");
    public static final String DEST_LAST_SAFE = put("dest.last_safe", "the last safe room you stood in");
    public static final String DEST_NEAREST_SAFE = put("dest.nearest_safe", "the nearest safe room");
    public static final String DEST_ARRIVAL = put("dest.arrival", "the arrival stairs of this floor");
    public static final String DEST_FORWARD = put("dest.forward", "the forward stairs of this floor");
    public static final String DEST_HUB = put("dest.hub", "the Hub");
    public static final String DEST_POCKET = put("dest.pocket", "the way you came in");
    public static final String REFUSE_COOLDOWN = put("refuse.cooldown", "Not yet: %1$s seconds left on /dungeon unstuck.");
    public static final String REFUSE_NO_XP = put("refuse.no_xp", "You need %1$s experience level(s) for /dungeon unstuck.");
    public static final String REFUSE_HOSTILE = put("refuse.hostile", "Hostile creatures are too close. Get away from them first.");
    public static final String REFUSE_NOT_HERE = put("refuse.not_here", "You are not in the Mega-Dungeon.");
    public static final String REFUSE_OFF = put("refuse.off", "The escape rules are switched off on this server.");
    public static final String REFUSE_NOWHERE = put("refuse.nowhere", "The System could not find a safe place to take you.");

    // ---- /dungeon help
    public static final String HELP = put("help", "/dungeon floor: the floor report. /dungeon status: are you sealed in. /dungeon release: open the seal you stand in when its rune is ready. /dungeon unstuck [party]: be taken to safety.");

    /** The English text of a key of this module, or null when the key is not one of ours. */
    public static String englishOrNull(String key) { return key == null ? null : EN.get(key); }

    /** The English text, or the key itself (never null). */
    public static String english(String key) {
        String s = EN.get(key);
        return s == null ? String.valueOf(key) : s;
    }

    /** Every key with its English text, in declaration order (the lang fragment is written from this). */
    public static Map<String, String> all() { return Collections.unmodifiableMap(EN); }
}
