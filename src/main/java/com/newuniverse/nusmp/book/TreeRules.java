package com.newuniverse.nusmp.book;

import java.util.List;
import java.util.Map;

/**
 * 1.93.0 nature-magic rebuild: the pure decisions of the rebuild, kept free of Minecraft classes so that they are unit-tested ({@code TreeRulesTest}) and checked in the real game
 * ({@link TreeSelfTests}): which page ids a rebuilt book must keep (the first five ids of the wiki family, the four of Plant Magic) and which it appends, how much a running temporary armour
 * softens a blow, and how a summon's variant, animation phase and animation clock are packed into the one int of the prop's param (low byte = variant, bits 8-9 = phase, bits 10-15 = ticks
 * since the action began) that the server writes and the painter reads.
 */
public final class TreeRules {
    private TreeRules() {}

    /** The pages of every generic wiki attribute, in order: the rebuilt Tree, Vine, Poison Plant, Mud and Mucus books keep these ids at these places (saves, mastery bits and cooldowns depend on it). */
    public static final List<String> GENERIC_IDS = List.of("attribute_bolt", "attribute_cut", "attribute_field", "attribute_domain", "attribute_guard");

    /** The first four pages of Plant Magic, which the rebuild keeps (the last two are the borrowed captain spells). */
    public static final List<String> PLANT_IDS = List.of("guidepost", "hundred_flowers", "yggdrasil", "briar_prison");

    /** The pages the rebuild appends, per magic key (the MagicType name), in order. */
    public static final Map<String, List<String>> APPENDED = Map.of(
            "TREE", List.of("heartwood_guardian", "seedling_volley", "timber_wall", "verdant_canopy", "heartwood_fist"),
            "VINE", List.of("vine_serpent", "vine_wall", "vine_swing", "strangling_bloom", "lash_barrage"),
            "PLANT", List.of("seed_bloom", "healing_bloom", "flower_sprite", "ivy_barrier", "blossom_dress", "petal_storm"),
            "POISON_PLANT", List.of("trap_maw", "spore_burst", "antivenom_graft", "pitcher_swallow", "thorn_volley"),
            "MUD", List.of("mud_golem", "mud_spout", "mud_wall", "mud_shackle", "mud_barrage"),
            "MUCUS", List.of("mucus_construct", "smother", "absorption_wall", "sticky_trail", "mucus_burst"));

    /** A grimoire may hold at most this many family pages (unlocks are the bits of an int and four special pages follow the family). */
    public static final int MAX_FAMILY = 24;

    /** The ids a rebuilt book must have, in order: the kept ones, then the appended ones. Null for an unknown magic. */
    public static List<String> expectedIds(String magic) {
        List<String> add = APPENDED.get(magic);
        if (add == null) return null;
        List<String> kept = magic.equals("PLANT") ? PLANT_IDS : GENERIC_IDS;
        java.util.ArrayList<String> out = new java.util.ArrayList<>(kept);
        out.addAll(add);
        return out;
    }

    /** What is wrong with the page ids of a rebuilt book (null when it is right): wrong order, a duplicate, too many pages. */
    public static String check(String magic, List<String> ids) {
        List<String> want = expectedIds(magic);
        if (want == null) return "unknown magic " + magic;
        if (ids.size() > MAX_FAMILY) return ids.size() + " pages, more than " + MAX_FAMILY;
        if (new java.util.HashSet<>(ids).size() != ids.size()) return "a page id is used twice";
        if (!ids.equals(want)) return "the page ids are " + ids + " but must be " + want;
        return null;
    }

    /** The factor a blow is multiplied by while the magic's temporary armour runs (smaller = harder armour). */
    public static float softening(String magic) {
        return switch (magic) {
            case "TREE" -> 0.85f;
            case "VINE" -> 0.9f;
            case "PLANT" -> 0.88f;
            case "POISON_PLANT" -> 0.92f;
            case "MUD" -> 0.7f;
            case "MUCUS" -> 0.65f;
            default -> 1f;
        };
    }

    // ------------------------------------------------------------------ the param of a summon
    public static final int VARIANT_MASK = 0xFF;

    public static int pack(int variant, int phase, int since) {
        return (variant & VARIANT_MASK) | ((phase & 3) << 8) | (Math.max(0, Math.min(63, since)) << 10);
    }

    public static int variantOf(int param) { return param & VARIANT_MASK; }

    public static int phaseOf(int param) { return (param >> 8) & 3; }

    public static int sinceOf(int param) { return (param >> 10) & 63; }
}
