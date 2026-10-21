package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: every word of the room lockdown and the ambush director is a lang key with its English fallback kept in ONE pure map, so each line reads properly before the
 * lang fragment ({@code tools/lang/lockdown.json}) is merged and in every language afterwards, and a unit test pins that the fragment and this map cannot drift. The
 * chat lines are under {@code message.nusmp.mega.lockdown.*}; the pop-ups the System shows are under {@code message.nusmp.mega.system.lockdown.*} so they resolve on the
 * client with the System's own lookup, like the escape rules' toasts. Pure.
 */
public final class LockdownText {
    private LockdownText() {}

    private static final String CHAT = "message.nusmp.mega.lockdown.", TOAST = "message.nusmp.mega.system.lockdown.";
    private static final Map<String, String> EN = new LinkedHashMap<>();

    private static String put(String key, String english) {
        EN.put(key, english);
        return key;
    }

    // ---- the block
    public static final String BLOCK = put("block.nusmp.lockdown_barrier", "Lockdown Barrier");

    // ---- the room closes and opens
    public static final String SEALED = put(CHAT + "sealed", "The doors seal behind you. Defeat every enemy in the room to open them (wave %1$s of %2$s).");
    public static final String WAVE = put(CHAT + "wave", "Wave %1$s of %2$s: more are coming.");
    public static final String CLEARED = put(CHAT + "cleared", "The room is cleared. The seals fade.");
    public static final String ESCAPED = put(CHAT + "escaped", "The room was left uncleared. Something will be waiting for you.");
    public static final String TOAST_SEALED = put(TOAST + "sealed", "ROOM SEALED: WAVE %1$s OF %2$s");
    public static final String TOAST_WAVE = put(TOAST + "wave", "WAVE %1$s OF %2$s");
    public static final String TOAST_CLEARED = put(TOAST + "cleared", "ROOM CLEARED");

    // ---- the ambush
    public static final String AMBUSH = put(CHAT + "ambush", "You ran past the fight. Something was waiting in the way ahead.");
    public static final String TOAST_AMBUSH = put(TOAST + "ambush", "AMBUSH AHEAD");

    // ---- the operator's readout
    public static final String STATUS_HEAD = put(CHAT + "status.head", "Lockdown: %1$s rooms locked now, %2$s cleared rooms remembered, %3$s ambushes fired this session.");
    public static final String STATUS_ROOM = put(CHAT + "status.room", "Your room (%1$s, %2$s): %3$s.");
    public static final String STATUS_NONE = put(CHAT + "status.none", "You are not in a room of the Mega-Dungeon.");
    public static final String STATUS_AMBUSH = put(CHAT + "status.ambush", "Ambush ledger: heat %1$s of %2$s, cooldown %3$s s, %4$s fired in the window; verdict: %5$s.");
    public static final String RELEASED = put(CHAT + "released", "Released %1$s locked room(s).");
    public static final String FORGOT = put(CHAT + "forgot", "Forgot the cleared mark of %1$s room(s); they can lock again.");

    public static String english(String key) { return EN.getOrDefault(key, key); }

    public static Map<String, String> all() { return Collections.unmodifiableMap(EN); }
}
