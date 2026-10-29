package com.newuniverse.nusmp.skilltree.unique;

import java.util.List;
import java.util.Set;

/**
 * Why: 1.96.0 removed the Fairy Tail magics from the skill book system, and the Skill Books with all their unique skills were removed later ({@link #RETIRED_LINES}); but a world saved before that may still hold their registry ids: a player whose skill list names {@code nusmp:ft_fire_dragons_roar}
 * or {@code nusmp:line_celestial_spirit_magic}, a chest or inventory that holds a {@code nusmp:skill_book} whose id component names one of the removed books. A registry entry that disappears can make such a
 * save fail to load, so the ids are kept here, as plain strings (no Minecraft type, so a unit test can read them), and {@link LegacySkills} registers one INERT, hidden stub skill under each of them (they do
 * nothing, cannot be used, and are forgotten from the player at the next login by {@link LegacyMigration}). The data, the lang, the loot and the shop never mention them again: the test that scans those
 * for the removed magics exempts nothing except this class. The book ids are what an old book stack carries; reading one converts it into a random book the player does not know yet.
 */
public final class LegacyIds {
    private LegacyIds() {}

    /**
     * The line ids of the 284 unique skills the Skill Books taught, retired together with the books (the books and the skills they alone taught were removed at the owner's request, see
     * {@code docs/skill_books_removed.md}). Each id was the registry id {@code nusmp:line_<id>}; it stays registered as an inert, hidden stub so a player's saved skill list that names it still loads, and the
     * stub is forgotten at the next login ({@link LegacyMigration}). Registered FIRST by {@link LegacySkills}, in this (id) order, which is the order the live skills had.
     */
    public static final List<String> RETIRED_LINES = List.of(
            "abyssal_black_flame", "aether_bow", "aether_destruction", "aether_step", "aevum_hourglass", "aggro_manipulation", "airiel_wind_maiden",
            "ais_sword_princess", "amazon_berserker", "animation_cancel", "anti_magic_blade_dancer", "argonaut_charge", "ashborn_shadow_monarch",
            "asura_ascension", "baran_demon_monarch", "basic_swordsmanship", "beast_communion", "beast_monarch", "beast_will", "beastkin_lord_carrion",
            "bell_firebolt", "beru_ant_king", "beta_tester_insight", "black_bulls_vanguard", "black_heavenly_demon_sword", "bloodlust_pressure",
            "blue_rose_cavalier", "boar_warrior_might", "bonded_beast", "bookmark", "borg_barrier_adept", "boss_pattern_reader", "braver_command",
            "breaking_the_sky_sword", "cauldron_witch_alchemist", "cha_hae_in_sword", "character_list", "cheese_strategy", "cinder_ella",
            "constellation_patronage", "consumable_optimization", "coral_peacock_trickster", "core_refinement", "crimson_lion_blazeguard", "crimson_sovereign",
            "crozzo_magic_sword", "cursed_equipment_mastery", "damage_rotation", "dark_rukh_fallen_magi", "dark_triad_hexer", "debuff_stack_doctrine",
            "demon_king_of_salvation", "devil_union_vessel", "diamond_crystal_bulwark", "dian_cecht_healer", "djinn_mirage", "dokkaebi_bundle",
            "double_dungeon_survivor", "dragon_king_antares", "dragon_nova_milim", "dragon_will", "dryad_world_tree", "dungeon_conqueror_captain",
            "dungeon_supporter", "dwarf_guardian", "dwarven_forge", "elemental_resist_meta", "elenoir_wind_weaver", "elven_rite_bearer", "ember_flame_art",
            "endure_ability", "erza_requip_knight", "extreme_magic_adept", "fanalis_brawler", "flame_djinn_swordsman", "flame_spirit_ifrit",
            "flute_magi_prodigy", "fog_troupe_bandit", "fourth_wall", "freya_charm", "frost_djinn_blade", "frost_monarch", "gajeel_iron_dragon",
            "gildarts_crash", "go_gunhee_chairman", "goh_apollo_charyeok", "goh_ares_charyeok", "goh_artemis_charyeok", "goh_athena_charyeok", "goh_capoeira",
            "goh_charyeok_vessel", "goh_hades_charyeok", "goh_haetae_jade", "goh_hapkido", "goh_hephaestus_charyeok", "goh_heracles_labors",
            "goh_hermes_charyeok", "goh_ilpyo_sword", "goh_judo", "goh_jumeok_fist", "goh_kendo", "goh_ki_cultivation", "goh_lu_bu_moonlight",
            "goh_mighty_style", "goh_monkey_king", "goh_muay_thai", "goh_nyuh_weapon_forms", "goh_pankration_grappler", "goh_poseidon", "goh_taejin_charyeok",
            "goh_taekwondo", "goh_tiger_crane_kungfu", "goh_tournament_contender", "goh_zeus_charyeok", "golden_dawn_prodigy", "goldsmith_radiance",
            "goliath_colossal_fist", "gray_ice_make", "great_sage_analysis", "greatsword_doctrine", "green_mantis_ambusher", "grimoire_page_scholar",
            "grinding_protocol", "guardian_absolute_defense", "hakurou_blade_way", "haruhime_uchide", "heart_kingdom_lifebinder", "heewon_righteous_knight",
            "hellflare_flame_lord", "hephaistos_forge", "hestia_falna", "hestia_knife", "hidden_class_berserker", "hinata_moonlit_blade", "hoarfrost_art",
            "household_vessel_wielder", "hunter_ability", "hunter_healer", "hunter_mage", "hunter_ranger", "hunter_sword_dance", "igris_blood_red",
            "imperial_fan_dancer", "invincibility_frames", "iron_body_monarch", "iron_shadow_guard", "jihye_twin_blades", "juvia_water_body", "kamish_wrath",
            "kasaka_venom_fang", "labyrinth_maker_ramiris", "lady_luck", "lance_bulwark", "lance_shadowblade", "lance_vanguard", "laxus_lightning_dragon",
            "lee_seolhwa_sword", "lefiya_viridis_mage", "legionary_phalanx_shield", "liaris_freese", "little_rookie", "loot_duplication_glitch",
            "luminous_wind", "magic_broom_aerialist", "magic_carpet_rider", "magic_knight_duelist", "magic_tool_artificer", "magnostadt_staff_wizard",
            "magoi_siphoner", "mana_beast_hunter", "mana_rotation", "mana_skin_brawler", "mana_zone_sentinel", "metal_vessel_conqueror", "min_max_build",
            "mirajane_satan_soul", "monarchs_domain", "monster_tamer", "multi_vessel_warlord", "natsu_fire_dragon", "new_game_plus_memory",
            "oath_of_the_heavy_armor", "ogre_lord_might", "omniscient_viewpoint", "orc_lord_famine", "overweight_build", "party_support_rotation",
            "perfect_parry_timing", "plague_monarch", "predator_gluttony", "primordial_black_diablo", "primordial_red_destruction", "prisoner_golden_headband",
            "probability_stories", "puppet_master_clayman", "purple_orca_tidecaller", "quicksilver_rush", "raid_captain_command", "rea_laevateinn",
            "realmheart_physique", "regis_leywin_devour", "regressor_hundred_lives", "rukh_caller_priest", "rulers_authority", "ryuu_astrea_blade",
            "sangah_war_room", "save_point_anchor", "scenario_clearer", "scythe_reaper", "sea_of_stories", "secretive_plotter", "sequence_break_route",
            "seven_seas_swordmaster", "shadow_djinn_assassin", "shadow_exchange", "shadow_extraction", "shadow_ninja_souei", "shield_throw_art",
            "shin_yoosung_child", "shuna_holy_weaving", "siege_calculation", "silver_eagle_marksman", "slime_body_shapeshift", "smoldering_fang_of_insanity",
            "solomon_wisdom_sage", "spade_beastblood_hunter", "spatial_manipulation", "spirit_dive_rider", "stealth_skill", "steel_bulwark", "stigma_bearer",
            "stone_djinn_bulwark", "storm_dragon_veldora", "storm_wolf_ranga", "storyteller", "sword_saint_style", "system_daily_quest", "takemikazuchi_bushi",
            "temple_delver", "thunder_art", "thunder_djinn_duelist", "tide_djinn_dancer", "tog_agnis_ice", "tog_anak_zahard", "tog_beast_tamer",
            "tog_black_march", "tog_faerie", "tog_fisherman", "tog_floor_guard", "tog_fug_assassin", "tog_guide", "tog_headon_channel", "tog_inventory",
            "tog_irregular", "tog_jahad_royal", "tog_light_bearer", "tog_lighthouse_keeper", "tog_rak_wraithraiser", "tog_ranker_authority",
            "tog_regular_climber", "tog_reverse_flow_control", "tog_rose_shower", "tog_scout", "tog_shinsu_reinforcement", "tog_shinsu_shooting",
            "tog_spear_bearer", "tog_suspendium_arms", "tog_true_flame", "tog_wave_controller", "tog_wolhaiguk_moon", "tog_zahard_decree",
            "tundra_harpoon_hunter", "tyrant_dark_mirror", "vampire_queen_night", "velzard_frost_dragon", "vritra_blood_rite", "wall_clip_glitch",
            "way_of_the_wind", "wendy_sky_dragon", "werewolf_gale_kick", "white_flame_sovereign", "white_tiger_baek", "wizard_king_sovereign",
            "wynn_fimbulvetr");

    /** The paths of the twelve Heavy Knight technique skills ({@code nusmp:hk_*}) that ran as modes of four of the retired skills; they were never learned, their ids stay as inert stubs. */
    public static final List<String> RETIRED_TECHNIQUES = List.of(
            "hk_provoke", "hk_shield_wall", "hk_shield_bash", "hk_power_strike", "hk_swift_step", "hk_crescent_cut", "hk_gamblers_coin", "hk_jackpot", "hk_ember_bite", "hk_insanity",
            "hk_howl_of_ruin", "hk_fang_of_insanity");

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
