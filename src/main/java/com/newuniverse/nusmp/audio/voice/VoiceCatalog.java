package com.newuniverse.nusmp.audio.voice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Why: the one list of the spoken voice lines (120 of the System, the bosses, the NPCs and the Djinn, and the Gnome's own, appended as a fifth role: ids as in {@code tools/audio/voice_lines.json}, made by {@code tools/audio/voice_lines.py}). Pure data with no Minecraft
 * class, so the tests, the payload codec, the cooldown logic and the registry ({@link VoiceSounds}) all read the same ids and cannot drift apart. An id is {@code <role>_<rest>}; the
 * role is also the folder of the file ({@code sounds/voice/<role>/<id>.ogg}) and the sound event is {@code nusmp:voice.<id>}.
 */
public final class VoiceCatalog {
    private VoiceCatalog() {}

    public static final String SYSTEM = "system", BOSS = "boss", NPC = "npc", DJINN = "djinn", GNOME = "gnome";

    /** The System announcer's lines (suffix after {@code system_}). */
    public static final List<String> SYSTEM_LINES = List.of("floor_enter", "floor_sealed", "lockdown_start", "lockdown_clear", "ambush_warning", "rest_safe", "boss_ahead", "rift_open",
            "floor_clear", "low_health");

    /** The boss families, in the manifest's order. */
    public static final List<String> BOSS_FAMILIES = List.of("holy", "evil", "undead", "fire", "ice", "storm", "void", "dragon", "devil", "angel");
    /** The boss events that have a line. */
    public static final List<String> BOSS_EVENTS = List.of("intro", "phase2", "death");

    public static final List<String> NPC_LINES = List.of("merchant_greet", "merchant_sell", "merchant_buy", "merchant_farewell", "quest_offer", "quest_done", "quest_farewell", "guide_greet");

    /** The 36 Djinn that speak, in the manifest's order. */
    public static final List<String> DJINN_NAMES = List.of("baal", "amon", "vinea", "focalor", "zagan", "zepar", "furfur", "marchosias", "sitri", "bifron", "paimon", "vassago", "bael", "agares",
            "valefor", "barbatos", "gusion", "vepar", "shax", "buer", "andrealphus", "kimaris", "amdusias", "belial", "decarabia", "seere", "dantalion", "andromalius", "glasya_labolas",
            "haagenti", "crocell", "furcas", "balam", "alloces", "camio", "murmur");
    public static final List<String> DJINN_EVENTS = List.of("summon", "attack");

    /**
     * The Gnome's lines (suffix after {@code gnome_}; docs/magic/gnome.md section 7): deep, slow, gravelly words of the Earth Spirit, said by the server through {@code gnome.Gnome} to the players within 32
     * blocks of it. A line is listed here only when its recording exists (tools/audio/gnome_voice.py).
     */
    public static final List<String> GNOME_LINES = List.of("greet_1");

    private static final List<String> IDS;
    private static final Set<String> SET;

    static {
        List<String> ids = new ArrayList<>();
        for (String s : SYSTEM_LINES) ids.add(SYSTEM + "_" + s);
        for (String f : BOSS_FAMILIES) for (String e : BOSS_EVENTS) ids.add(BOSS + "_" + f + "_" + e);
        for (String n : NPC_LINES) ids.add("npc_" + n);
        for (String d : DJINN_NAMES) for (String e : DJINN_EVENTS) ids.add(DJINN + "_" + d + "_" + e);
        for (String g : GNOME_LINES) ids.add(gnomeId(g));
        IDS = Collections.unmodifiableList(ids);
        SET = Collections.unmodifiableSet(new LinkedHashSet<>(ids));
    }

    /** Every line id, in manifest order. */
    public static List<String> ids() { return IDS; }

    public static boolean has(String id) { return id != null && SET.contains(id); }

    /** The role of a line ({@code system}, {@code boss}, {@code npc}, {@code djinn}), or null for an unknown id. */
    public static String role(String id) {
        if (!has(id)) return null;
        return id.substring(0, id.indexOf('_'));
    }

    /** The sound event path of a line: {@code voice.system_floor_enter}. */
    public static String eventId(String id) { return "voice." + id; }

    /** The resource path of the file, without the namespace and extension: {@code voice/system/system_floor_enter}. */
    public static String filePath(String id) { return "voice/" + role(id) + "/" + id; }

    /** Which role folders there are. */
    public static List<String> roles() { return List.of(SYSTEM, BOSS, NPC, DJINN, GNOME); }

    /** The role -> its ids, for reports. */
    public static Map<String, List<String>> byRole() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        for (String r : roles()) m.put(r, new ArrayList<>());
        for (String id : IDS) m.get(role(id)).add(id);
        return m;
    }

    /** Normalises a name to the id form: lower case, non-alphanumerics to underscores ({@code Glasya-Labolas} -> {@code glasya_labolas}). */
    public static String slug(String name) {
        if (name == null) return "";
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    public static String systemId(String line) { return SYSTEM + "_" + line; }

    public static String bossId(String family, String event) { return BOSS + "_" + family + "_" + event; }

    public static String npcId(String line) { return "npc_" + line; }

    public static String djinnId(String name, String event) { return DJINN + "_" + slug(name) + "_" + event; }

    public static String gnomeId(String line) { return GNOME + "_" + line; }
}
