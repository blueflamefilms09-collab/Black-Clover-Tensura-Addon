package com.newuniverse.nusmp.client.relics.item;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The class table of weapon sizes and the id to class mapping (docs/weapon_scale_orientation.md; the Python side is tools/relics/weapon_classes.py and
 * the data is tools/relics/weapon_classes.json, copied to assets/nusmp/weapon_classes.json for the game). Pure: no Minecraft imports.
 * <p>
 * Sizes are APPARENT blocks: the mesh length times the third-person display scale, i.e. what a viewer sees of the weapon held in a hand
 * (1 block = 1 metre, Steve is 1.8). {@code min}/{@code max} bound the whole weapon, {@code headMin} the widest part of an axe or hammer head,
 * {@code grip} is the share of the length (from the butt) that stays where it is when the weapon is scaled. The built-in table below is the fallback
 * when the resource is missing; {@link #guess} is the keyword guess for an id nobody has classified (the same keywords, in the same order, as
 * the Python tool).
 */
public final class WeaponClasses implements com.newuniverse.nusmp.visual.grip.GripTable.Classes {
    /** One class of the table. */
    public record Cls(String name, float min, float max, float headMin, float grip) {}

    /** Held trinkets, books and charms have no length range: their largest dimension must reach this many blocks (1.34.0: was 0.40; the handitem class is 0.5 - 0.9). */
    public static final float ITEM_MIN_EXTENT = 0.50f;
    /** The runtime normaliser steps in below this share of a class minimum (1.34.0: was 0.60; the offline fixes aim at the whole range). */
    public static final float RUNTIME_SMALL = 0.75f;

    static final Map<String, Cls> DEFAULTS = new LinkedHashMap<>();
    static {
        // 1.34.0: the owner's size rule (docs/weapon_scale_orientation.md section 10): unless a weapon should be small, make it bigger
        c("dagger", 0.45f, 0.70f, 0f, 0.20f);
        c("shortsword", 1.00f, 1.30f, 0f, 0.20f);
        c("longsword", 1.40f, 1.80f, 0f, 0.20f);
        c("rapier", 1.00f, 1.30f, 0f, 0.20f);
        c("katana", 1.40f, 1.80f, 0f, 0.20f);
        c("sabre", 1.00f, 1.30f, 0f, 0.20f);
        c("dadao", 1.80f, 2.40f, 0f, 0.20f);
        c("greatsword", 2.20f, 3.00f, 0f, 0.22f);
        c("handaxe", 1.10f, 1.40f, 0.50f, 0.25f);
        c("battleaxe", 1.60f, 2.00f, 0.75f, 0.25f);
        c("greataxe", 2.40f, 3.00f, 1.10f, 0.28f);
        c("crescent", 1.60f, 2.00f, 0.75f, 0.25f);
        c("scythe", 2.00f, 2.60f, 0.60f, 0.30f);
        c("cleaver", 0.90f, 1.20f, 0.45f, 0.25f);
        c("spear", 2.80f, 3.60f, 0f, 0.33f);
        c("glaive", 2.80f, 3.60f, 0f, 0.33f);
        c("trident", 2.80f, 3.60f, 0f, 0.33f);
        c("staff", 2.00f, 2.60f, 0f, 0.40f);
        c("whip", 1.60f, 2.40f, 0f, 0.15f);
        c("coilwhip", 1.10f, 1.70f, 0f, 0.15f);
        c("crossbow", 1.00f, 1.20f, 0f, 0.50f);
        c("bow", 1.30f, 1.60f, 0f, 0.50f);
        c("hammer", 1.30f, 1.80f, 0.50f, 0.25f);
        c("greathammer", 2.20f, 2.80f, 0.90f, 0.28f);
        c("mace", 1.30f, 1.80f, 0.50f, 0.25f);
        c("club", 1.30f, 1.80f, 0f, 0.25f);
        c("wand", 0.80f, 1.20f, 0f, 0.20f);
        c("shield", 0.90f, 1.20f, 0f, 0.50f);
        c("item", 0f, 9f, 0f, 0.50f);
        c("handitem", 0.50f, 0.90f, 0f, 0.50f);
    }

    private static void c(String name, float min, float max, float headMin, float grip) {
        DEFAULTS.put(name, new Cls(name, min, max, headMin, grip));
    }

    /** Ordered keyword guess: the first key contained in the id wins. */
    static final String[][] KEYWORDS = {
            {"greataxe", "greataxe"}, {"great_axe", "greataxe"}, {"handaxe", "handaxe"}, {"hand_axe", "handaxe"}, {"crescent", "crescent"},
            {"scythe", "scythe"}, {"axe", "battleaxe"}, {"greatsword", "greatsword"}, {"katana", "katana"}, {"dadao", "dadao"}, {"sabre", "sabre"},
            {"saber", "sabre"}, {"scimitar", "sabre"}, {"rapier", "rapier"}, {"longsword", "longsword"}, {"gladius", "shortsword"},
            {"xiphos", "shortsword"}, {"shortsword", "shortsword"}, {"dagger", "dagger"}, {"knife", "dagger"}, {"dirk", "dagger"}, {"stiletto", "dagger"},
            {"cleaver", "cleaver"}, {"trident", "trident"}, {"glaive", "glaive"}, {"halberd", "glaive"}, {"naginata", "glaive"}, {"spear", "spear"},
            {"lance", "spear"}, {"pike", "spear"}, {"staff", "staff"}, {"wand", "wand"}, {"whip", "whip"}, {"crossbow", "crossbow"}, {"longbow", "bow"}, {"shortbow", "bow"}, {"bow", "crossbow"},
            {"greathammer", "greathammer"}, {"great_hammer", "greathammer"}, {"hammer", "hammer"}, {"maul", "hammer"}, {"mace", "mace"}, {"club", "club"}, {"shield", "shield"}, {"sword", "longsword"}, {"blade", "longsword"},
    };

    private static final Pattern STATE_SUFFIX = Pattern.compile("(_(pull[123]|charged|spent|genesis|5leaf|liquid|off|blue|left|curse)|(?<=lantern)_red)$");      // 1.62.0: + curse (the cursed look of a Holy hero weapon takes its base weapon's class)

    private final Map<String, Cls> table;
    private final Map<String, String> weapons;

    public WeaponClasses(Map<String, Cls> table, Map<String, String> weapons) {
        this.table = table == null || table.isEmpty() ? DEFAULTS : table;
        this.weapons = weapons == null ? new HashMap<>() : weapons;
    }

    public static WeaponClasses defaults() {
        return new WeaponClasses(DEFAULTS, new HashMap<>());
    }

    /** The id of the rest mesh a state mesh (bianca_pull2, needler_charged, marids_valor_liquid ...) belongs to. */
    public static String baseId(String id) {
        return STATE_SUFFIX.matcher(id).replaceFirst("");
    }

    /** The keyword guess for an id; "item" when no keyword is in it. */
    public static String guess(String id) {
        for (String[] kv : KEYWORDS) {
            if (id.contains(kv[0])) return kv[1];
        }
        return "item";
    }

    /** The class name of an id: the reviewed mapping (the id, then its base id), else the keyword guess. */
    public String className(String id) {
        String c = weapons.get(id);
        if (c == null) c = weapons.get(baseId(id));
        return c != null ? c : guess(id);
    }

    /** 1.111.0: true when the reviewed id to class map lists this id (or its base id); false = the class is only a keyword guess (GripTable asks, a data test wants no guess). */
    public boolean isListed(String id) {
        return weapons.containsKey(id) || weapons.containsKey(baseId(id));
    }

    /** The class of an id; never null (an unknown name falls back to the keyword guess, then to "item"). */
    public Cls classOf(String id) {
        Cls c = table.get(className(id));
        if (c == null) c = table.get(guess(id));
        if (c == null) c = DEFAULTS.get("item");
        return c;
    }

    /** 1.111.0: the row of a class by its name (the built-in row of that name when the resource lacks it), null for an unknown name. */
    public Cls named(String name) {
        Cls c = table.get(name);
        return c != null ? c : DEFAULTS.get(name);
    }

    /** A held item (class item or handitem) is measured by its largest dimension, not its length. */
    public boolean isItem(Cls c) {
        return isItemClass(c.name());
    }

    public static boolean isItemClass(String name) {
        return "item".equals(name) || "handitem".equals(name);
    }
}
