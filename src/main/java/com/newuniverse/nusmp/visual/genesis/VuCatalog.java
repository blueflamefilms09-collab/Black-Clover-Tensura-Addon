package com.newuniverse.nusmp.visual.genesis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The weapons of the genesis_weapons slice of the visual upgrade (tools/visual_upgrade/genesis_weapons): the item ids and, for each, every mesh id the renderer may
 * pick (a bow's draw stages are meshes of their own). The tests and the self-test read the shipped assets for exactly these.
 */
public final class VuCatalog {
    private VuCatalog() {}

    /** Item ids in the order of the slice. */
    public static final List<String> IDS = List.of(
            "astralis_the_eye_of_the_universe", "blackrazor", "cardinal_halo_chakram", "fairy_glitter_gauntlets", "gae_bolg_heartseeker", "magic_emperors_black_halberd",
            "primordial_hearth_blade", "starforged_aeon_bow", "starsplitter_hammer", "sword_of_the_first_dawn", "void_origin_katana", "nekrathuls_pact_scythe", "elsdocia");

    /** The state meshes of an item beyond its rest mesh {@code id}. */
    public static final Map<String, List<String>> STATES = Map.of("starforged_aeon_bow", List.of("starforged_aeon_bow_pull1", "starforged_aeon_bow_pull2", "starforged_aeon_bow_pull3"));

    /** Every mesh id of an item: the rest mesh first. */
    public static List<String> meshes(String id) {
        List<String> out = new ArrayList<>();
        out.add(id);
        out.addAll(STATES.getOrDefault(id, List.of()));
        return out;
    }
}
