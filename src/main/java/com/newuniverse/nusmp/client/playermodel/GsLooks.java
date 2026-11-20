package com.newuniverse.nusmp.client.playermodel;

import java.util.List;

/**
 * Why: which redesigned look the Goku and Sinbad bosses wear (docs/boss_redesign_goku_sinbad.md), as a pure table (no game class, so the unit test, the CI self-test and the previews read it). The looks are
 * {@code gs_<spec id>} in {@code looks_gs.json}, written by {@code tools/bc_playermodels/gen_gs.py}. Goku's ten ladder steps ({@code GokuLadder}) map onto eight looks; Sinbad's synced equip number (the
 * {@code SinbadDjinn} ordinal, 0 = none) maps onto his base look and one body per djinn. The old geo-model look of both bosses stays registered and comes back with the client option
 * {@code bossLooks.bossLookClassic}. Add-only: a new step or djinn appends an entry here and a spec in gsbosses.py.
 */
public final class GsLooks {
    private GsLooks() {}

    public static final String GOKU_BOSS = "goku_boss", SINBAD_BOSS = "sinbad_boss";

    /** The look id of each GokuLadder step (Base, SSJ, SSJ2, SSJ3, God, Blue, Blue Kaio-ken, UI Sign, Perfected UI, True UI). */
    public static final List<String> GOKU = List.of("gs_goku_base", "gs_goku_ssj", "gs_goku_ssj2", "gs_goku_ssj3", "gs_goku_god", "gs_goku_blue", "gs_goku_blue", "gs_goku_ui_sign", "gs_goku_mui", "gs_goku_mui");

    /** The look id of each synced equip number: 0 none, then Baal, Valefor, Zepar, Focalor, Vepar, Crocell, Furfur (the SinbadDjinn order). */
    public static final List<String> SINBAD = List.of("gs_sinbad_base", "gs_sinbad_baal", "gs_sinbad_valefor", "gs_sinbad_zepar", "gs_sinbad_focalor", "gs_sinbad_vepar", "gs_sinbad_crocell", "gs_sinbad_furfur");

    /** The name of the player's {@code Djinn} enum entry whose Equip a synced equip number wears ("" = none). The boss uses the very same armour pieces a player gets. */
    public static final List<String> DJINN_NAME = List.of("", "BAAL", "VALEFOR", "ZEPAR", "FOCALOR", "VEPAR", "CROCELL", "FURFUR");

    /** The Goku step that adds the Kaio-ken red over the Blue look. */
    public static final int GOKU_KAIOKEN_STEP = 6;

    public static String goku(int step) { return GOKU.get(Math.max(0, Math.min(GOKU.size() - 1, step))); }

    public static String sinbad(int equipIdx) { return SINBAD.get(Math.max(0, Math.min(SINBAD.size() - 1, equipIdx))); }

    public static boolean isGsLook(String id) { return id != null && id.startsWith("gs_"); }
}
