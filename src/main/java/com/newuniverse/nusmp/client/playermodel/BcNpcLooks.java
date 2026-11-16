package com.newuniverse.nusmp.client.playermodel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the Black Clover NPCs and the Midnight Sun elves have a redesigned player-model look each (docs/npcs_bc.md), the same kind as {@link BcLooks} has for the Zogratis bosses: id {@code bc_<old look id>},
 * in {@code looks_bc_npc.json}, written by {@code tools/bc_playermodels/specs_npc.py}. This pure table (no game class, so the unit test and the CI self-test read it) says which old look has a new one;
 * {@link PlayerAssets#bcId} asks it after {@link BcLooks}, under the same client option {@code bossLooks.bcPlayerModel}, so the old look is one switch away. It is a separate class so the table of the
 * bosses is not touched. The maps are built once; a lookup allocates nothing.
 */
public final class BcNpcLooks {
    private BcNpcLooks() {}

    /** The old look ids that have a redesigned look, in the generator's order (Midnight Sun, Black Bulls, Magic Knights). Add-only: an entry needs its look in looks_bc_npc.json (BcNpcPlayerModelsTest). */
    private static final List<String> IDS = List.of(
            "rhya", "rhya_form2", "fana", "fana_form2", "patolli", "patolli_form2", "rades_spirito", "rades_spirito_form2",
            "asta", "yami_sukehiro", "noelle_silva", "finral_roulacase", "magna_swing", "luck_voltia", "charmy_pappitson", "vanessa_enoteca", "gordon_agrippa", "zora_ideale", "gauche_adlai", "grey",
            "secre_swallowtail", "nacht_faust",
            "julius_novachrono", "marx_francois", "yuno", "mimosa_vermillion", "klaus_lunettes", "leopold_vermillion", "fuegoleon_vermillion", "mereoleona_vermillion", "charlotte_roselei",
            "nozel_silva", "solid_silva", "jack_the_ripper", "dorothy_unsworth", "william_vangeance", "kahono", "kiato");

    private static final Map<String, String> NEW_OF = build();

    private static Map<String, String> build() {
        Map<String, String> m = new LinkedHashMap<>();
        for (String id : IDS) m.put(id, BcLooks.PREFIX + id);
        return m;
    }

    /** The redesigned look id of an old look id, or null when there is none. */
    public static String of(String oldId) { return oldId == null ? null : NEW_OF.get(oldId); }

    /** Every old look id that has a redesigned look here (for the tests and docs). */
    public static Set<String> oldIds() { return NEW_OF.keySet(); }
}
