package com.newuniverse.nusmp.charter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The seven Charter Grimoires as data (pure Java, no Minecraft): which boss each belongs to, how likely the first kill is to leave it, how it
 * looks, and the Tensura skills it lends while it is held. Boss ids are stable strings (they are also the keys of the world's drop lock, see
 * {@link DropLedger}); the entity types are found by their registry path, and only entities of this mod's namespace are ever bosses (the
 * isolation law of the overhaul: nothing vanilla or from base Tensura is touched).
 * <p>
 * Skill sets are thematic and capped ({@link #MAX_SKILLS}). Every id is in the Tensura 2.0.1.3 id reference. {@link #NEVER_LENT} lists the
 * skills that can take things from others: they are not lent (non-destructive law), whatever a boss owns.
 */
public final class CharterTable {
    private CharterTable() {}

    /** The only namespace whose entities count as bosses. */
    public static final String NAMESPACE = "nusmp";
    /** The key prefix of the world's drop lock: {@code has_dropped_charter_grimoire_<bossId>}. */
    public static final String FLAG_PREFIX = "has_dropped_charter_grimoire_";
    public static final String ITEM_PREFIX = "charter_grimoire_";
    /** The most skills one grimoire lends. */
    public static final int MAX_SKILLS = 12;
    /** Skills no grimoire lends: they take abilities from their target, and a grimoire only ever copies. */
    public static final Set<String> NEVER_LENT = Set.of("tensura:usurper");

    /**
     * One grimoire. {@code grade} is a name of {@code relics.Grade} (LEGEND or above); {@code cover}/{@code trim} are 0xRRGGBB for the item's
     * art; {@code chance} is the base chance (0 to 1) that a first-ever kill leaves the grimoire.
     */
    public record Boss(String id, String entityPath, String name, String grade, double chance, int cover, int trim, String lore, List<String> skills) {
        public String itemId() { return ITEM_PREFIX + id; }

        public String flag() { return FLAG_PREFIX + id; }
    }

    private static final Map<String, Boss> BY_ID = new LinkedHashMap<>();

    private static void add(String id, String entityPath, String name, String grade, double chance, int cover, int trim, String lore, String... skills) {
        List<String> list = new ArrayList<>();
        for (String s : skills) list.add("tensura:" + s);
        BY_ID.put(id, new Boss(id, entityPath, name, grade, chance, cover, trim, lore, Collections.unmodifiableList(list)));
    }

    static {
        add("dante", "dante_zogratis", "Dante Zogratis", "LEGEND", 0.25, 0x4B2D8A, 0xC9A7FF,
                "The Charter of the Gravity Devil: weight that bends the room around its holder.",
                "gravity_domination", "gravity_manipulation", "gravity_field", "gravity_flight", "gravity_attack_resistance", "burden", "oppressor", "strengthen_body");
        add("vanica", "vanica_zogratis", "Vanica Zogratis", "LEGEND", 0.25, 0x9B1230, 0xFF8FA3,
                "The Charter of the Curse-Warding Devil: blood and curses on loan.",
                "blood_mist", "curse", "curse_bind", "darkness", "shadow_bind", "shadow_motion", "drain", "self_regeneration");
        add("zenon", "zenon_zogratis", "Zenon Zogratis", "LEGEND", 0.25, 0x1C7C8C, 0x8CF2FF,
                "The Charter of the Spatial Devil: doors where there are no doors.",
                "spatial_manipulation", "spatial_domination", "spatial_motion", "spatial_storage", "spatial_attack_resistance", "dimension_cutter", "instant_move", "danger_sense");
        add("lucius", "lucius_zogratis", "Lucius Zogratis", "GOD", 0.25, 0xD9B43A, 0x2B2410,
                "The Charter of the Time Devil's Vessel: nothing it names is allowed to stay as it was.",
                "anti_skill", "tuner", "absolute_severance", "megiddo", "true_darkness", "dimension_cutter", "thought_acceleration",
                "spiritual_attack_resistance", "holy_attack_resistance");
        add("devil_avatar", "devil_avatar", "the Devil Avatar", "GOD", 0.40, 0x2A2024, 0xFF6A1A,
                "The Charter of a devil that walked: hellfire borrowed for as long as you hold it.",
                "hellfire", "black_flame", "flame_manipulation", "flame_domination", "flame_attack_resistance", "darkness_attack_resistance", "demon_lord_haki", "summon_daemon");
        add("goliath", "goliath", "Goliath", "LEGEND", 0.40, 0x6B5A3C, 0xE3C98A,
                "The Charter of the Pit's giant: stone, weight and a very large fist.",
                "earth_manipulation", "earth_domination", "earth_wall", "earth_spikes", "earthshatter_kick", "giantification", "physical_attack_resistance", "steel_strength", "strengthen_body");
        add("charybdis_spawn", "charybdis_spawn", "the Charybdis Spawn", "LEGEND", 0.40, 0x1D4F9C, 0x9AD8FF,
                "The Charter of the Basin's whirlpool: every current answers to it.",
                "water_manipulation", "water_domination", "water_current_control", "water_jail", "water_blade", "water_breathing", "water_attack_resistance", "hydraulic_propulsion");
    }

    /** Every grimoire, in the fixed order of the table. */
    public static List<Boss> all() { return Collections.unmodifiableList(new ArrayList<>(BY_ID.values())); }

    public static List<String> ids() { return Collections.unmodifiableList(new ArrayList<>(BY_ID.keySet())); }

    /** The grimoire of a boss id, or null. */
    public static Boss byId(String bossId) { return bossId == null ? null : BY_ID.get(bossId.toLowerCase(Locale.ROOT)); }

    /**
     * The boss an entity type is, or null. The isolation guard: only the registry namespace {@link #NAMESPACE} can ever match, so a vanilla or
     * base-Tensura entity (even one with the same path as a boss) is never a boss.
     */
    public static Boss byEntity(String namespace, String path) {
        if (!NAMESPACE.equals(namespace) || path == null) return null;
        for (Boss b : BY_ID.values()) if (b.entityPath.equals(path)) return b;
        return null;
    }

    /** The grimoire an item path belongs to ({@code charter_grimoire_dante} gives the Dante entry), or null. */
    public static Boss byItemPath(String path) {
        if (path == null || !path.startsWith(ITEM_PREFIX)) return null;
        return byId(path.substring(ITEM_PREFIX.length()));
    }

    /** "gravity_field" or "tensura:gravity_field" as "Gravity Field". */
    public static String pretty(String skillId) {
        String s = skillId.substring(skillId.indexOf(':') + 1).replace('_', ' ');
        StringBuilder b = new StringBuilder();
        for (String w : s.split(" ")) {
            if (w.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return b.toString();
    }
}
