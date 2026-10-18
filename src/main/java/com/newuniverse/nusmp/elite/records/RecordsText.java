package com.newuniverse.nusmp.elite.records;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: every fixed word the records system puts in front of a player (messages, the key name, the screen's labels, the command replies) is a translatable string, and the owner does not want
 * {@code en_us.json} edited by this job: the strings are collected in {@code tools/elite/records_lang.json}, a flat key to text file the lead merges later. Until it is merged the game must not show a
 * raw key, so every string is read with {@code Component.translatableWithFallback(key, english)}: a language file that has the key wins, and one that does not shows the English text declared here.
 * This class is the single list of keys and their English texts; a test compares it with {@code records_lang.json} in both directions. The names and descriptions of records, titles and synergies are
 * NOT here: they are data and travel as the data files wrote them. Common code only (no client class), so the server's messages and the client's screen share it.
 */
public final class RecordsText {
    private RecordsText() {}

    private static final Map<String, String> TEXTS = new LinkedHashMap<>();

    private static String def(String key, String english) {
        TEXTS.put(key, english);
        return key;
    }

    /** All keys with their English text, in declaration order (read only; used by the lang-file test). */
    public static Map<String, String> all() { return Collections.unmodifiableMap(TEXTS); }

    public static MutableComponent tr(String key, Object... args) {
        String english = TEXTS.getOrDefault(key, key);
        return Component.translatableWithFallback(key, english, args);
    }

    public static String str(String key, Object... args) { return tr(key, args).getString(); }

    public static MutableComponent category(com.newuniverse.nusmp.elite.records.rule.Category c) { return tr("gui.nusmp.records.category." + c.id()); }

    public static MutableComponent rarity(com.newuniverse.nusmp.elite.records.rule.Rarity r) { return tr("gui.nusmp.records.rarity." + r.id()); }

    // ---- server messages
    public static final String MSG_RECORD = def("msg.nusmp.records.unlocked", "Record unlocked: %s");
    public static final String MSG_TITLE = def("msg.nusmp.records.title_earned", "Title earned: %s");
    public static final String MSG_ANNOUNCE = def("msg.nusmp.records.announce", "%s unlocked the %s record: %s");
    public static final String MSG_ACTIVE_SET = def("msg.nusmp.records.active_set", "Active title: %s");
    public static final String MSG_ACTIVE_CLEARED = def("msg.nusmp.records.active_cleared", "Active title cleared.");
    public static final String MSG_ACTIVE_REFUSED = def("msg.nusmp.records.active_refused", "You do not hold that title.");

    // ---- key
    public static final String KEY_OPEN = def("key.nusmp.records_open", "Open the Records screen");

    // ---- menu button
    public static final String BUTTON = def("gui.nusmp.records.button", "Records");
    public static final String BUTTON_TIP = def("gui.nusmp.records.button.tip", "Records, achievements and titles.");

    // ---- screen
    public static final String TITLE = def("gui.nusmp.records.title", "Records and Titles");
    public static final String CLOSE = def("gui.nusmp.records.close", "Close");
    public static final String TAB_ALL = def("gui.nusmp.records.tab.all", "All records");
    public static final String TAB_TITLES = def("gui.nusmp.records.tab.titles", "Titles");
    public static final String TAB_SYNERGIES = def("gui.nusmp.records.tab.synergies", "Synergies");
    public static final String CAT_EXPLORATION = def("gui.nusmp.records.category.exploration", "Exploration");
    public static final String CAT_COMBAT = def("gui.nusmp.records.category.combat", "Combat");
    public static final String CAT_SURVIVAL = def("gui.nusmp.records.category.survival", "Survival");
    public static final String CAT_POWER = def("gui.nusmp.records.category.power", "Power");
    public static final String CAT_SKILLS = def("gui.nusmp.records.category.skills", "Skills");
    public static final String CAT_CRAFTING = def("gui.nusmp.records.category.crafting", "Crafting");
    public static final String CAT_DOMINION = def("gui.nusmp.records.category.dominion", "Dominion");
    public static final String CAT_WAR = def("gui.nusmp.records.category.war", "War");
    public static final String CAT_CALAMITY = def("gui.nusmp.records.category.calamity", "Calamity");
    public static final String RARITY_COMMON = def("gui.nusmp.records.rarity.common", "Common");
    public static final String RARITY_UNCOMMON = def("gui.nusmp.records.rarity.uncommon", "Uncommon");
    public static final String RARITY_RARE = def("gui.nusmp.records.rarity.rare", "Rare");
    public static final String RARITY_EPIC = def("gui.nusmp.records.rarity.epic", "Epic");
    public static final String RARITY_LEGENDARY = def("gui.nusmp.records.rarity.legendary", "Legendary");
    public static final String RARITY_MYTHIC = def("gui.nusmp.records.rarity.mythic", "Mythic");
    public static final String FILTER_ALL = def("gui.nusmp.records.filter.all", "Showing: all");
    public static final String FILTER_UNLOCKED = def("gui.nusmp.records.filter.unlocked", "Showing: unlocked");
    public static final String FILTER_LOCKED = def("gui.nusmp.records.filter.locked", "Showing: locked");
    public static final String SUMMARY = def("gui.nusmp.records.summary", "%s records unlocked   %s of %s points");
    public static final String ACTIVE_TITLE = def("gui.nusmp.records.active_title", "Active title: %s");
    public static final String NO_ACTIVE_TITLE = def("gui.nusmp.records.no_active_title", "No active title");
    public static final String UNLOCKED_ON = def("gui.nusmp.records.unlocked_on", "Unlocked %s");
    public static final String HIDDEN_NAME = def("gui.nusmp.records.hidden_name", "???");
    public static final String UNAVAILABLE = def("gui.nusmp.records.unavailable", "Unavailable on this server");
    public static final String POINTS = def("gui.nusmp.records.points", "%s pts");
    public static final String EMPTY = def("gui.nusmp.records.empty", "Nothing to show here.");
    public static final String WAITING = def("gui.nusmp.records.waiting", "Waiting for the server...");
    public static final String OFF = def("gui.nusmp.records.off", "The records system is off on this server.");
    public static final String HELD = def("gui.nusmp.records.title.held", "HELD");
    public static final String ACTIVE_TAG = def("gui.nusmp.records.title.active", "ACTIVE");
    public static final String SET_ACTIVE = def("gui.nusmp.records.title.set_active", "Make active");
    public static final String CLEAR_ACTIVE = def("gui.nusmp.records.title.clear_active", "Remove");
    public static final String REQUIRES = def("gui.nusmp.records.title.requires", "Needs:");
    public static final String GIVES = def("gui.nusmp.records.title.gives", "Gives:");
    public static final String BONUSES_NOW = def("gui.nusmp.records.bonuses_now", "Bonuses in effect");
    public static final String SYN_ACTIVE = def("gui.nusmp.records.synergy.active", "IN EFFECT");
    public static final String SYN_PROGRESS = def("gui.nusmp.records.synergy.progress", "%s of %s");
    public static final String SYN_NEEDS_TITLES = def("gui.nusmp.records.synergy.needs", "Hold: %s");
    public static final String SYN_NEEDS_TAG = def("gui.nusmp.records.synergy.needs_tag", "Hold %s titles tagged %s");
    public static final String SYN_NEEDS_ANY = def("gui.nusmp.records.synergy.needs_any", "Hold %s titles");
    public static final String HINT = def("gui.nusmp.records.hint", "Scroll with the wheel. Click a title row to make it active.");

    // ---- toasts
    public static final String TOAST_RECORD = def("gui.nusmp.records.toast.record", "Record unlocked");
    public static final String TOAST_TITLE = def("gui.nusmp.records.toast.title", "Title earned");

    // ---- commands
    public static final String CMD_SUMMARY = def("cmd.nusmp.records.summary", "Records: %s of %s unlocked, %s points. Titles held: %s. Active: %s.");
    public static final String CMD_GRANTED = def("cmd.nusmp.records.granted", "%s now has the record %s.");
    public static final String CMD_REVOKED = def("cmd.nusmp.records.revoked", "%s no longer has the record %s.");
    public static final String CMD_TITLE_GRANTED = def("cmd.nusmp.records.title_granted", "%s now holds the title %s.");
    public static final String CMD_TITLE_REVOKED = def("cmd.nusmp.records.title_revoked", "%s no longer holds the title %s.");
    public static final String CMD_UNKNOWN = def("cmd.nusmp.records.unknown", "No such record or title: %s");
    public static final String CMD_NOTHING = def("cmd.nusmp.records.nothing", "Nothing changed.");
    public static final String CMD_RESET = def("cmd.nusmp.records.reset", "All record data of %s was erased.");
    public static final String CMD_PROBLEMS_NONE = def("cmd.nusmp.records.problems_none", "The records data has no problems (%s records, %s titles, %s synergies).");
    public static final String CMD_PROBLEMS = def("cmd.nusmp.records.problems", "%s problem(s) in the records data:");
    public static final String CMD_OFF = def("cmd.nusmp.records.off", "The records system is switched off in its config.");
}
