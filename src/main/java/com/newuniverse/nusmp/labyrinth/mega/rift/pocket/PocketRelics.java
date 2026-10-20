package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import java.util.Map;
import java.util.Set;

/**
 * Why: the relics of the exclusive tombs drop nowhere but in their tombs, so the places that roll a relic at random (the relic loot entries of the vaults, the Crucible of Choice, the mimics) must
 * never pick them. They ask here: {@link #exclusive} is true for the 26 relics of the 13 tombs, whatever the datapack says (the built-in set, written from tools/rift/pocket_roster.py and checked
 * against the data by {@code PocketDataTest}) and for any relic a loaded pocket definition names (a tomb added by a datapack is covered the moment it loads). The loot gate of the tombs themselves
 * ({@link PocketGate}) is the other half: it lets a tomb's relic out only inside that tomb.
 */
public final class PocketRelics {
    private PocketRelics() {}

    /** The boss weapons (wave 78) and the primary artifacts (wave 79) of the 13 tombs. */
    public static final Set<String> BUILT_IN = Set.of(
            "ignareths_forge_hammer", "thornmothers_briar_bow", "rimeheart_greatblade", "deepwatch_harpoon", "valkarins_stormlance", "anheteps_sunscourge", "nekrathuls_pact_scythe",
            "orials_starfall_staff", "sanguine_wardens_cleaver", "chronarchs_pendulum_sabre", "reflected_kings_mirrorblade", "aurliels_seraph_halberd", "broodqueens_stinger",
            "ashen_forgeheart", "reliquary_seedheart", "silent_winter_mantle", "orrery_of_drowned_hours", "aerie_courier_gloves", "sun_eaters_locket", "pactbreaker_shroud",
            "archivists_starbound_codex", "bloodbound_gauntlets", "vaultmakers_pocketwatch", "silvered_sash_of_reflections", "basilica_halo", "hivemind_carapace_pendant");

    /** The relics the loaded pocket definitions name, each with its tomb (replaced whole by {@link PocketDefs} on every load; the class itself holds no Minecraft type). */
    private static volatile Map<String, String> loaded = Map.of();

    static void loaded(Map<String, String> relicToTomb) { loaded = Map.copyOf(relicToTomb); }

    /** Is this relic one that only an exclusive tomb may give? */
    public static boolean exclusive(String relicId) { return relicId != null && (BUILT_IN.contains(relicId) || loaded.containsKey(relicId)); }

    /** The id of the tomb whose relic this is (the loaded definitions), or null. */
    public static String tombOf(String relicId) { return relicId == null ? null : loaded.get(relicId); }
}
