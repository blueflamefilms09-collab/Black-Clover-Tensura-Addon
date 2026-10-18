package com.newuniverse.nusmp.skilltree.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: every word the Tensura-menu integration shows (the nested panel, the ON and OFF tooltips, the equip button, the admin inspect switch, the description of a stand-in skill) must be a
 * translatable string, and this job may not edit {@code en_us.json}: the new strings are collected in the flat file {@code tools/tensura_menu/lang_tensura_menu.json} for the lead to merge. Until it is
 * merged the game must not show a raw key, so every string is read with {@code Component.translatableWithFallback(key, english)}: a language file that has the key wins, one that does not shows the English
 * text declared here. This class is the single list of keys and their English texts (the same pattern as {@code LineText}); a unit test compares it with the fragment in both directions, so the file the
 * lead merges cannot drift from what the code asks for. Common code (the stand-in skill's description is read on the server too).
 */
public final class MenuText {
    private MenuText() {}

    private static final Map<String, String> TEXTS = new LinkedHashMap<>();

    private static String def(String key, String english) {
        TEXTS.put(key, english);
        return key;
    }

    // ---- the nested panel
    public static final String ARTS = def("gui.nusmp.tmenu.arts", "Arts");
    public static final String PASSIVES = def("gui.nusmp.tmenu.passives", "Passives");
    public static final String STATS = def("gui.nusmp.tmenu.stats", "Stat bonuses");
    public static final String ON = def("gui.nusmp.tmenu.on", "ON");
    public static final String OFF = def("gui.nusmp.tmenu.off", "OFF");
    public static final String EQUIPPED = def("gui.nusmp.tmenu.equipped", "EQUIPPED");
    public static final String NOT_EQUIPPED = def("gui.nusmp.tmenu.not_equipped", "Not equipped: this line's passives are asleep.");
    public static final String EQUIP = def("gui.nusmp.tmenu.equip", "Equip");
    public static final String EQUIP_TIP = def("gui.nusmp.tmenu.equip.tip", "Only one Unique Skill Line can be equipped at a time. Equipping this one puts the line you use now to sleep.");
    public static final String EQUIP_NONE = def("gui.nusmp.tmenu.equip.none", "This line cannot be equipped.");
    public static final String OPEN_TREE = def("gui.nusmp.tmenu.open_tree", "Skill Tree");
    public static final String OPEN_TREE_TIP = def("gui.nusmp.tmenu.open_tree.tip", "Open the Skill Tree to spend Skill Points on this line.");
    public static final String NOT_OWNED = def("gui.nusmp.tmenu.not_owned", "Not owned (inspect)");
    public static final String LOADING = def("gui.nusmp.tmenu.loading", "Asking the server for this line...");
    public static final String NOTHING_BOUGHT = def("gui.nusmp.tmenu.nothing", "Nothing bought on this line yet. Spend Skill Points in the Skill Tree.");
    public static final String LEVEL = def("gui.nusmp.tmenu.level", "Level %s/%s");
    public static final String UNLOCKED = def("gui.nusmp.tmenu.unlocked", "%s of %s unlocked");
    public static final String TYPE_ART = def("gui.nusmp.tmenu.type.art", "Art");
    public static final String TYPE_STANCE = def("gui.nusmp.tmenu.type.stance", "Stance");
    public static final String TYPE_PASSIVE = def("gui.nusmp.tmenu.type.passive", "Passive");
    public static final String TYPE_STAT = def("gui.nusmp.tmenu.type.stat", "Stat bonus");
    public static final String SWITCH_HINT = def("gui.nusmp.tmenu.switch_hint", "Click the box to switch it.");

    // ---- the admin switch
    public static final String INSPECT = def("gui.nusmp.tmenu.inspect", "ADMIN INSPECT");
    public static final String INSPECT_ON = def("gui.nusmp.tmenu.inspect.on", "ADMIN INSPECT: ON");
    public static final String INSPECT_TIP = def("gui.nusmp.tmenu.inspect.tip", "Lists every Unique Skill Line in this menu, owned or not. Read-only unless you own the line.");

    // ---- the stand-in skill itself (what Tensura's own list and detail show)
    public static final String STANDIN_DESC = def("gui.nusmp.tmenu.standin", "A Unique Skill Line. Its arts and passives are listed beside this menu when you select it.");

    /** The English text of a key, or the key itself when it is not one of ours. */
    public static String english(String key) { return TEXTS.getOrDefault(key, key); }

    /** A translatable component with the English text as the fallback, so a missing language entry never shows the raw key. */
    public static MutableComponent tr(String key, Object... args) { return Component.translatableWithFallback(key, english(key), args); }

    /** Every key with its English text, in declaration order (what the lang fragment must hold). */
    public static Map<String, String> all() { return Collections.unmodifiableMap(TEXTS); }

    /** The lang key of a stand-in skill's name: {@code nusmp.skill.line_<bare>}, the same {@code nusmp.skill.<id>} shape every rebuilt skill uses. */
    public static String standInNameKey(String bare) { return "nusmp.skill." + LineIndex.skillPath(bare); }
}
