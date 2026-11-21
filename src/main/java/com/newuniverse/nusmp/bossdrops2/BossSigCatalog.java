package com.newuniverse.nusmp.bossdrops2;

import java.util.List;

/**
 * Why: the bosses that have a signature drop, in ONE fixed order. The position (1-based) is the {@code custom_model_data} of that boss's signature stack, which picks its icon in
 * {@code models/item/boss_signature.json}, so the order is APPEND ONLY (rule 2: a world's old stacks must keep their icon). The same list is the data file names under
 * {@code data/anime_multiverse/boss_signature/} and {@code tools/boss_drops/signature_index.json} (a test pins that all three agree). Pure: no Minecraft type.
 * {@link #DEFAULT} is the fallback table of a boss without a file of its own (a tagged boss, a boss of an addon).
 */
public final class BossSigCatalog {
    private BossSigCatalog() {}

    public static final String DEFAULT = "default";

    public static final List<String> BOSSES = List.of(
            "meliodas", "escanor", "ban", "diane", "king", "gowther",
            "merlin", "fana", "rhya", "rades_spirito", "patolli", "heath_grice",
            "lotus_whomalt", "mars", "revchi_salik", "vetto", "minotaur", "black_goliath",
            "amphisbaena", "udaeus", "morris_libardirt", "lucifero", "dante_zogratis", "vanica_zogratis",
            "zenon_zogratis", "lucius_zogratis", "devil_avatar", "goliath", "charybdis_spawn", "zagred",
            "zagred_legacy", "goku_boss", "kakashi_hatake", "sinbad_boss", "light_yagami", "charlos",
            "zetsu", "yammy_llargo", "sogou_nobuyuki", "mr_2_bon_clay", "kurozumi_orochi", "malty_melromarc",
            "rachel", "jugram_haschwalth", "big_mom", "code", "bertholdt_hoover", "axe_hand_morgan",
            "foxy", "shou_tucker", "ao", "floor_warden", "labyrinth_guardian", "god_of_order",
            "default");

    /** The tier of each boss, in the order of {@link #BOSSES} (L lesser, M mid, H high, A apex); a test pins it against the data files. The creative menu draws a stack's rarity from it before any data is loaded. */
    private static final String TIERS = "AAHHHHAMMHHMMMLMHHHHHAAAAAAHHAHAAALLLLLLLMMHHMHLLMMLHAM";

    /** The tier of a boss; LESSER for one that is not in the catalog. */
    public static SignatureTable.Tier tier(String boss) {
        int i = BOSSES.indexOf(boss);
        if (i < 0 || i >= TIERS.length()) return SignatureTable.Tier.LESSER;
        return switch (TIERS.charAt(i)) {
            case 'M' -> SignatureTable.Tier.MID;
            case 'H' -> SignatureTable.Tier.HIGH;
            case 'A' -> SignatureTable.Tier.APEX;
            default -> SignatureTable.Tier.LESSER;
        };
    }

    /** The one-of-one relic id of each boss, in the order of {@link #BOSSES} ("" = the boss pays no one-of-one); a test pins it against the data files. */
    private static final List<String> RELICS = List.of(
            "hellblaze_the_demon_sin", "noon_cleaver", "mammons_vault_ring", "hammer_of_thunderbolts",
            "fairy_glitter_gauntlets", "pandoras_actor_mask", "astralis_the_eye_of_the_universe", "",
            "", "", "", "",
            "", "", "", "",
            "minotaur_kings_labrys", "", "", "",
            "", "godking_crown", "starsplitter_hammer", "staff_of_blood_magic",
            "gate_of_babylon_crest", "time_hammer", "incinerator_arm", "",
            "", "series_ancient_grimoire", "", "ruyi_power_pole",
            "perfect_susanoo_plate", "leviathans_crown_trident", "", "",
            "", "", "", "",
            "", "", "", "",
            "", "", "", "",
            "", "", "", "",
            "", "administrators_authority_key", "");

    /** The relic id of this boss's one-of-one, or "" when it has none. */
    public static String relic(String boss) {
        int i = BOSSES.indexOf(boss);
        return i < 0 || i >= RELICS.size() ? "" : RELICS.get(i);
    }

    /** The custom model data of a boss's signature stack (1-based), 0 when the boss is not in the catalog. */
    public static int modelData(String boss) { return BOSSES.indexOf(boss) + 1; }

    public static boolean known(String boss) { return BOSSES.contains(boss); }
}
