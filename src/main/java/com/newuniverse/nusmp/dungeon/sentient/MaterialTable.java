package com.newuniverse.nusmp.dungeon.sentient;

import java.util.Map;

/**
 * The rare synthesis materials the dead of the Dungeon may leave (the owner's design: "a Direwolf pelt, a Goblin dagger"): one item per
 * kind of mob, four times in a hundred per kill, two more per Looting level. The Juggernaut's heart is not rolled for: it always drops. Pure.
 */
public final class MaterialTable {
    private MaterialTable() {}

    public static final double BASE_CHANCE = 0.04, PER_LOOTING = 0.02, CEILING = 0.5;

    /** The item of the heart a Juggernaut always leaves. */
    public static final String HEART = "juggernaut_heart";

    /** Every material's item id (the path in the mod's namespace). */
    public static final String[] ALL = {
            "goblin_dagger", "kobold_fang", "slime_core", "aqua_slime_gel", "direwolf_pelt", "lizardman_scale",
            "water_spirit_essence", "megalodon_tooth", "devil_horn", "disciple_sigil", "beast_hide", HEART};

    private static final Map<String, String> BY_MOB = Map.ofEntries(
            Map.entry("goblin", "goblin_dagger"), Map.entry("hound_dog", "kobold_fang"), Map.entry("slime", "slime_core"),
            Map.entry("direwolf", "direwolf_pelt"), Map.entry("lizardman", "lizardman_scale"), Map.entry("undine", "water_spirit_essence"),
            Map.entry("megalodon", "megalodon_tooth"), Map.entry("lesser_daemon", "devil_horn"), Map.entry("dark_disciple", "disciple_sigil"),
            Map.entry("giant_bear", "beast_hide"), Map.entry("horned_bear", "beast_hide"), Map.entry("orc", "beast_hide"), Map.entry("war_gnome", "beast_hide"));

    /** The chance of a material per kill. */
    public static double chance(int looting) { return Math.min(CEILING, BASE_CHANCE + PER_LOOTING * Math.max(0, looting)); }

    /** The material a kind of mob (the path of its id, "goblin") can leave, or null; an aqua slime leaves its gel instead of a slime core. */
    public static String materialFor(String mobPath, boolean aqua) {
        if (aqua && "slime".equals(mobPath)) return "aqua_slime_gel";
        return BY_MOB.get(mobPath);
    }

    /** The material that drops from this kill (the item's path), or null. */
    public static String roll(String mobPath, boolean aqua, int looting, Dice dice) {
        String m = materialFor(mobPath, aqua);
        return m != null && dice.next() < chance(looting) ? m : null;
    }

    /** The name a tooltip gives for where a material comes from ("Dropped by Goblins"). */
    public static String source(String material) {
        return switch (material) {
            case "goblin_dagger" -> "Goblins";
            case "kobold_fang" -> "Kobolds";
            case "slime_core" -> "Slimes";
            case "aqua_slime_gel" -> "Aqua Slimes";
            case "direwolf_pelt" -> "Direwolves";
            case "lizardman_scale" -> "Lizardmen";
            case "water_spirit_essence" -> "Water Spirits";
            case "megalodon_tooth" -> "Megalodons";
            case "devil_horn" -> "Lesser Devils";
            case "disciple_sigil" -> "Dark Disciples";
            case "beast_hide" -> "Bears, Orcs and War Gnomes";
            case HEART -> "Juggernauts";
            default -> "the Dungeon";
        };
    }
}
