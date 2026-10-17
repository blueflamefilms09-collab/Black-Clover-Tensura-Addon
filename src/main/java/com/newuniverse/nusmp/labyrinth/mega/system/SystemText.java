package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: every word the System shows (the overlay's labels, the pop-up lines, the chat lines, the item tooltip, the command output) goes through a lang key
 * {@code message.nusmp.mega.system.*}, so it can be translated; but the lead merges the lang file after the code, and a datapack quest may have no lang entry,
 * so every consumer also needs the English text to fall back on. Keeping the English in ONE pure map, next to the keys, means the server (chat, command), the
 * client (overlay, toasts) and the tooltip all read the same fallback, a test can pin that no key is empty, and the lang fragment the lead merges is written
 * from this very map (so the file and the code cannot drift). The sync payload never carries a translated string: it carries keys and numbers, and the
 * receiving side resolves them in ITS language. Pure.
 */
public final class SystemText {
    private SystemText() {}

    /** The prefix of every key of this module (the contract: {@code message.nusmp.mega.<key>.*}). */
    public static final String PREFIX = "message.nusmp.mega.system.";

    private static final Map<String, String> EN = new LinkedHashMap<>();

    private static String put(String shortKey, String english) {
        String full = PREFIX + shortKey;
        EN.put(full, english);
        return full;
    }

    // ---- overlay labels
    public static final String HEADER = put("ui.header", "[ SYSTEM ]");
    public static final String FLOOR = put("ui.floor", "FLOOR");
    public static final String HOLY = put("ui.holy", "HOLY");
    public static final String ABYSSAL = put("ui.abyssal", "ABYSSAL");
    public static final String THREAT = put("ui.threat", "THREAT");
    public static final String NO_QUEST = put("ui.no_quest", "NO QUEST ON THIS FLOOR");
    public static final String BOSS_FLOOR = put("ui.boss_floor", "BOSS FLOOR: NO QUEST");
    public static final String COMPLETE = put("ui.complete", "COMPLETE");
    public static final String FAILED = put("ui.failed", "FAILED");
    public static final String UNHARMED = put("ui.unharmed", "UNHARMED");
    public static final String TIME = put("ui.time", "TIME");
    public static final String STAT_STR = put("ui.stat.str", "STR");
    public static final String STAT_AGI = put("ui.stat.agi", "AGI");
    public static final String STAT_VIT = put("ui.stat.vit", "VIT");
    public static final String STAT_INT = put("ui.stat.int", "INT");
    public static final String STAT_SENSE = put("ui.stat.sense", "SENSE");
    public static final String QUEST_FLOOR = put("ui.quest_floor", "QUEST OF FLOOR %1$s");
    public static final String TAG = put("ui.tag", "[SYSTEM]");
    public static final String WINDOW_SHOWN = put("ui.window_shown", "System window: shown");
    public static final String WINDOW_HIDDEN = put("ui.window_hidden", "System window: hidden");

    // ---- pop-up lines (the "[SYSTEM]" header is drawn by the overlay)
    public static final String TOAST_QUEST_ARRIVED = put("toast.quest_arrived", "QUEST ARRIVED");
    public static final String TOAST_QUEST_COMPLETE = put("toast.quest_complete", "QUEST COMPLETE");
    public static final String TOAST_QUEST_FAILED = put("toast.quest_failed", "QUEST FAILED");
    public static final String TOAST_REWARD_STAT = put("toast.reward_stat", "REWARD: +%1$s %2$s");
    public static final String TOAST_REWARD_CORE = put("toast.reward_core", "REWARD: SHADOW CORE");
    public static final String TOAST_REWARD_RECHARGE = put("toast.reward_recharge", "REWARD: SHADOW CORE RECHARGED");
    public static final String TOAST_FLOOR_REACHED = put("toast.floor_reached", "FLOOR %1$s REACHED");
    public static final String TOAST_EXTRACTION_READY = put("toast.extraction_ready", "EXTRACTION READY");
    public static final String TOAST_EXTRACTED = put("toast.extracted", "SHADOW EXTRACTED: %1$s");
    public static final String TOAST_CORE_SPENT = put("toast.core_spent", "THE SHADOW CORE CRUMBLES");
    public static final String TOAST_MINION_LOST = put("toast.minion_lost", "A SHADOW HAS FALLEN: %1$s");
    public static final String TOAST_MINION_RELEASED = put("toast.minion_released", "THE OLDEST SHADOW IS RELEASED: %1$s");

    // ---- chat and action-bar lines
    public static final String MSG_NOTHING_TO_EXTRACT = put("msg.nothing_to_extract",
            "Nothing to extract here. Slay a hostile creature on the Holy floors, then use the core within %1$s seconds, close to where it fell.");
    public static final String MSG_CHARGES_LEFT = put("msg.charges_left", "Shadow Core: %1$s charge(s) left.");
    public static final String MSG_BAD_TARGET = put("msg.bad_target", "That creature cannot be made a shadow: %1$s.");
    public static final String MSG_MINION_NAME = put("msg.minion_name", "Shadow %1$s");
    public static final String MSG_SYSTEM_OFF = put("msg.system_off", "The System is switched off on this server.");

    // ---- the item
    public static final String TIP_CHARGES = put("tip.charges", "Charges: %1$s / %2$s");
    public static final String TIP_USE = put("tip.use", "Use it within %1$s seconds of slaying a hostile creature on the Holy floors, close to where it fell, to raise it as your shadow.");
    public static final String TIP_LIMIT = put("tip.limit", "At most %1$s shadows follow you; raising another releases the oldest.");
    public static final String TIP_LORE = put("tip.lore", "The System keeps what it is owed.");

    // ---- the admin command (permission 2)
    public static final String CMD_STATUS_HEAD = put("cmd.status.head", "The System of %1$s: %2$s");
    public static final String CMD_STATUS_FLOOR = put("cmd.status.floor", "Floor %1$s (%2$s), threat %3$s, deepest floor reached %4$s");
    public static final String CMD_STATUS_QUEST = put("cmd.status.quest", "Quest '%1$s' [%2$s, %3$s]: %4$s / %5$s on floor %6$s");
    public static final String CMD_STATUS_NOQUEST = put("cmd.status.noquest", "Quest: none");
    public static final String CMD_STATUS_STATS = put("cmd.status.stats", "Stats: STR %1$s  AGI %2$s  VIT %3$s  INT %4$s  SENSE %5$s (at most %6$s each)");
    public static final String CMD_STATUS_COUNTERS = put("cmd.status.counters",
            "Quests done %1$s, failed %2$s; Shadow Cores handed out %3$s; shadows %4$s of %5$s; deaths waiting to be extracted: %6$s");
    public static final String CMD_ON = put("cmd.on", "on");
    public static final String CMD_OFF = put("cmd.off", "off");
    public static final String CMD_QUEST_ROLLED = put("cmd.quest.rolled", "%1$s now has the quest '%2$s' (%3$s) on floor %4$s.");
    public static final String CMD_QUEST_NONE = put("cmd.quest.none", "Nothing could be rolled: no quest template fits this floor (data/nusmp/mega/system/quests.json).");
    public static final String CMD_QUEST_COMPLETED = put("cmd.quest.completed", "The quest of %1$s is completed.");
    public static final String CMD_QUEST_FAILED = put("cmd.quest.failed", "The quest of %1$s is failed.");
    public static final String CMD_QUEST_NOACTIVE = put("cmd.quest.noactive", "%1$s has no active quest.");
    public static final String CMD_QUEST_CLEARED = put("cmd.quest.cleared", "The quest of %1$s was removed.");
    public static final String CMD_QUEST_UNKNOWN = put("cmd.quest.unknown", "There is no quest template '%1$s'. Known: %2$s");
    public static final String CMD_STAT_GIVEN = put("cmd.stat.given", "%1$s now has %2$s point(s) of %3$s (%4$s changed).");
    public static final String CMD_STAT_UNKNOWN = put("cmd.stat.unknown", "'%1$s' is not a stat. The stats are STR, AGI, VIT, INT and SENSE.");
    public static final String CMD_CORE_GIVEN = put("cmd.core.given", "Gave a Shadow Core to %1$s.");
    public static final String CMD_MINIONS_CLEARED = put("cmd.minions.cleared", "Released %1$s shadow(s) of %2$s.");

    // ---- the key binding and the item name (the lang fragment carries them too)
    public static final String KEY_TOGGLE = "key.nusmp.system_toggle";
    public static final String ITEM_NAME = "item.nusmp.shadow_core";

    static {
        EN.put(KEY_TOGGLE, "Toggle the System window");
        EN.put(ITEM_NAME, "Shadow Core");
    }

    /** The English text of a key, or the key itself when it is not one of ours (never null, so a missing entry shows as the key, not as a crash). */
    public static String english(String key) {
        String s = EN.get(key);
        return s == null ? String.valueOf(key) : s;
    }

    /** Every key with its English text, in declaration order (the lang fragment is written from this). */
    public static Map<String, String> all() { return Collections.unmodifiableMap(EN); }
}
