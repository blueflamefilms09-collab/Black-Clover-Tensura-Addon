package com.newuniverse.nusmp.skilltree.line;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: every word the line engine puts in front of a player (a refused cast, the Arts bar, the Passives panel, the inspect toggle) must be a translatable string, and the owner does not want
 * {@code en_us.json} edited by this job: the new strings are collected in {@code tools/lines/lang_engine.json}, a flat key to text file the lead merges later. Until it is merged the game must not show a raw
 * key like {@code gui.nusmp.lines.passives.title}, so every string is read with {@code Component.translatableWithFallback(key, english)}: a language file that has the key wins, and one that does not shows the
 * English text declared here. This class is the single list of keys and their English texts; a test compares it with {@code lang_engine.json} in both directions (every key used in code is in the file with the
 * same text, every key of the file is used), so the file the lead merges cannot drift from what the code asks for. Common code only (no client class), so the server's refusals and the client's panels share it.
 */
public final class LineText {
    private LineText() {}

    private static final Map<String, String> TEXTS = new LinkedHashMap<>();

    private static String def(String key, String english) {
        TEXTS.put(key, english);
        return key;
    }

    // ---- server messages (action bar)
    public static final String CAST_LOCKED = def("msg.nusmp.line.locked", "That art is locked. Equip its line and buy it in the Skill Tree.");
    public static final String CAST_COOLDOWN = def("msg.nusmp.line.cooldown", "%s is recovering: %s s.");
    public static final String CAST_NO_TARGET = def("msg.nusmp.line.no_target", "%s has nothing to hit.");
    public static final String CAST_TOO_FAST = def("msg.nusmp.line.too_fast", "Not so fast.");
    public static final String TOGGLE_REFUSED = def("msg.nusmp.line.toggle_refused", "That passive cannot be switched.");
    public static final String INSPECT_REFUSED = def("msg.nusmp.line.inspect_refused", "Only an operator or a creative player may inspect lines.");
    public static final String STANCE_UP = def("msg.nusmp.line.stance_up", "%s is up.");
    public static final String STANCE_DOWN = def("msg.nusmp.line.stance_down", "%s is down.");

    // ---- keys
    public static final String KEY_NEXT = def("key.nusmp.line_arts_next", "Arts bar: next art");
    public static final String KEY_PREV = def("key.nusmp.line_arts_prev", "Arts bar: previous art");
    public static final String KEY_CAST = def("key.nusmp.line_arts_cast", "Arts bar: use the art");
    public static final String KEY_PASSIVES = def("key.nusmp.line_passives", "Open the Passives panel");

    // ---- the Arts bar
    public static final String ARTS_TITLE = def("gui.nusmp.lines.arts.title", "Arts");
    public static final String ARTS_READY = def("gui.nusmp.lines.arts.ready", "Ready");
    public static final String ARTS_STANCE_ON = def("gui.nusmp.lines.arts.stance_on", "ON");
    public static final String ARTS_HINT = def("gui.nusmp.lines.arts.hint", "%s / %s to choose, %s to use");

    // ---- the Passives panel
    public static final String PASSIVES_BUTTON = def("gui.nusmp.lines.passives.button", "Passives");
    public static final String PASSIVES_BUTTON_TIP = def("gui.nusmp.lines.passives.button.tip", "Switch the passives and stat bonuses of your lines on and off.");
    public static final String PASSIVES_TITLE = def("gui.nusmp.lines.passives.title", "Passives");
    public static final String PASSIVES_CLOSE = def("gui.nusmp.lines.passives.close", "Back");
    public static final String PASSIVES_EMPTY = def("gui.nusmp.lines.passives.empty", "You have no passives yet. Read a Skill Book and buy passive nodes in the Skill Tree.");
    public static final String PASSIVES_ON = def("gui.nusmp.lines.passives.on", "ON");
    public static final String PASSIVES_OFF = def("gui.nusmp.lines.passives.off", "OFF");
    public static final String PASSIVES_EQUIPPED = def("gui.nusmp.lines.passives.equipped", "EQUIPPED");
    public static final String PASSIVES_NOT_EQUIPPED = def("gui.nusmp.lines.passives.not_equipped", "Not equipped: this line's passives are asleep.");
    public static final String PASSIVES_LEVEL = def("gui.nusmp.lines.passives.level", "Level %s");
    public static final String PASSIVES_COUNT = def("gui.nusmp.lines.passives.count", "%s passives");
    public static final String PASSIVES_ALL_ON = def("gui.nusmp.lines.passives.all_on", "All on");
    public static final String PASSIVES_ALL_OFF = def("gui.nusmp.lines.passives.all_off", "All off");
    public static final String PASSIVES_TYPE_STAT = def("gui.nusmp.lines.passives.type.stat", "Stat bonus");
    public static final String PASSIVES_TYPE_PASSIVE = def("gui.nusmp.lines.passives.type.passive", "Passive");
    public static final String PASSIVES_SCROLL_HINT = def("gui.nusmp.lines.passives.hint", "Scroll for more. Click a row to switch it.");

    // ---- the library, inspect mode, backgrounds
    public static final String INSPECT_TOGGLE = def("gui.nusmp.lines.inspect.toggle", "ADMIN INSPECT MODE");
    public static final String INSPECT_TOGGLE_TIP = def("gui.nusmp.lines.inspect.tip", "Lists and opens every registered line for testing. Read-only unless you own the line.");
    public static final String INSPECT_BANNER = def("gui.nusmp.lines.inspect.banner", "INSPECT MODE: you are looking at lines you do not own. Nothing here can be bought.");
    public static final String INSPECT_LOADING = def("gui.nusmp.lines.inspect.loading", "Asking the server for this line...");
    public static final String INSPECT_NOT_OWNED = def("gui.nusmp.lines.inspect.not_owned", "Not owned (inspect)");
    public static final String LIBRARY_EMPTY = def("gui.nusmp.lines.library.empty", "Your library is empty. Skill Books found in the Mega-Dungeon and sold by the Black Market open new lines here.");

    /** The English text of a key, or the key itself when it is not one of ours. */
    public static String english(String key) { return TEXTS.getOrDefault(key, key); }

    /** A translatable component with the English text as the fallback, so a missing language entry never shows the raw key. */
    public static MutableComponent tr(String key, Object... args) { return Component.translatableWithFallback(key, english(key), args); }

    /** Every key with its English text, in declaration order (what {@code lang_engine.json} must hold). */
    public static Map<String, String> all() { return Collections.unmodifiableMap(TEXTS); }
}
