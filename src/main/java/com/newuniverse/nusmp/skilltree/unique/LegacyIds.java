package com.newuniverse.nusmp.skilltree.unique;

import java.util.List;
import java.util.Set;

/**
 * Why: 1.96.0 removed the Fairy Tail magics from the skill book system, but a world saved before that may still hold their registry ids: a player whose skill list names {@code nusmp:ft_fire_dragons_roar}
 * or {@code nusmp:line_celestial_spirit_magic}, a chest or inventory that holds a {@code nusmp:skill_book} whose id component names one of the removed books. A registry entry that disappears can make such a
 * save fail to load, so the ids are kept here, as plain strings (no Minecraft type, so a unit test can read them), and {@link UniqueSkills} registers one INERT, hidden stub skill under each of them (they do
 * nothing, cannot be used, and are forgotten from the player at the next login by {@link LegacyMigration}). The data, the lang, the loot and the shop never mention them again: the test that scans those
 * for the removed magics exempts nothing except this class. The book ids are what an old book stack carries; reading one converts it into a random book the player does not know yet.
 */
public final class LegacyIds {
    private LegacyIds() {}

    /** The line ids (the file stems of the removed route files) of the 29 removed magics. */
    public static final List<String> REMOVED_LINES = List.of(
            "arc_of_time", "beast_arm_takeover", "card_magic", "celestial_spirit_magic", "crash_magic", "dark_ecriture", "end_demon_awakening", "exceed_aera", "fairy_law",
            "fire_dragon_slayer", "gravity_change", "heavenly_fire_god_slayer", "ice_make", "iron_dragon_slayer", "lightning_dragon_slayer", "poison_dragon_slayer",
            "requip_gunslinger", "requip_titania", "satan_soul", "seith_totems", "shadow_dragon_slayer", "sky_dragon_slayer", "solid_script", "telekinesis_mind_hand",
            "titan_magic", "water_body", "white_dragon_slayer", "wizard_saint_rock_magic", "wood_make");

    /** The paths of the technique skills the four classic routes of the removed magics registered (the registry ids stay, as inert stubs). */
    public static final List<String> REMOVED_TECHNIQUES = List.of(
            "ft_fire_dragons_iron_fist", "ft_fire_dragons_roar", "ft_fire_dragons_claw", "ft_fire_dragons_brilliant_flame", "ft_fire_dragons_wing_attack", "ft_fire_dragons_sword_horn",
            "ft_crimson_lotus_exploding_flame_blade",
            "ft_lightning_dragons_iron_fist", "ft_lightning_dragons_roar", "ft_lightning_dragons_breakdown_fist", "ft_lightning_dragons_heavenward_halberd", "ft_thunder_palace",
            "ft_lightning_dragons_jaw", "ft_lightning_dragons_flash", "ft_roaring_thunder",
            "ft_sky_dragons_roar", "ft_sky_dragons_wing_attack", "ft_sky_dragons_wave_wind", "ft_sky_dragons_breath", "ft_sky_troia", "ft_sky_arms_vernier", "ft_sky_drill",
            "ft_ice_make_lance", "ft_ice_make_floor", "ft_ice_make_shield", "ft_ice_make_cannon", "ft_ice_make_saucer", "ft_ice_make_eagle", "ft_ice_make_hammer", "ft_ice_make_excalibur");

    private static final Set<String> LINE_SET = Set.copyOf(REMOVED_LINES);

    /** The book id a removed line's Skill Book carries ({@code nusmp:<line>_book}). */
    public static String bookId(String line) { return "nusmp:" + line + "_book"; }

    /** True when the bare line id (or {@code nusmp:<line>}) is one of the removed magics. */
    public static boolean isRemovedLine(String lineOrRouteId) {
        if (lineOrRouteId == null) return false;
        int c = lineOrRouteId.indexOf(':');
        return LINE_SET.contains(c < 0 ? lineOrRouteId : lineOrRouteId.substring(c + 1));
    }

    /** True when the book id (the {@code skill_book_id} of a stack) names a removed magic's book. */
    public static boolean isRemovedBook(String bookId) {
        if (bookId == null || !bookId.startsWith("nusmp:") || !bookId.endsWith("_book")) return false;
        return LINE_SET.contains(bookId.substring("nusmp:".length(), bookId.length() - "_book".length()));
    }
}
