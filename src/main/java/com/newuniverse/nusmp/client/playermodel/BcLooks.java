package com.newuniverse.nusmp.client.playermodel;

import java.util.Map;

/**
 * Why: the Black Clover bosses have a redesigned player-model look each (docs/bc_playermodels.md): id {@code bc_<old look id>}, in {@code looks_bc.json}, written by
 * {@code tools/bc_playermodels/gen_bc.py}. This pure table (no game class, so the unit test and the CI self-test read it) says which old look has a new one and which particle theme its idle shimmer
 * uses, so a body asks {@link PlayerAssets#bcId} with the old id and gets the new one only while the client option {@code bossLooks.bcPlayerModel} is on. Nothing is looked up by string
 * concatenation per frame: the maps are built once.
 */
public final class BcLooks {
    private BcLooks() {}

    /** The prefix of every redesigned look. */
    public static final String PREFIX = "bc_";

    /** Old look id to the id of its redesigned look. Add-only: an entry here needs its look in looks_bc.json (BcPlayerModelsTest). */
    private static final Map<String, String> NEW_OF = Map.ofEntries(
            Map.entry("lucius_zogratis", "bc_lucius_zogratis"), Map.entry("lucius_zogratis_form2", "bc_lucius_zogratis_form2"),
            Map.entry("dante_zogratis", "bc_dante_zogratis"), Map.entry("dante_zogratis_form2", "bc_dante_zogratis_form2"),
            Map.entry("vanica_zogratis", "bc_vanica_zogratis"), Map.entry("vanica_zogratis_form2", "bc_vanica_zogratis_form2"),
            Map.entry("zenon_zogratis", "bc_zenon_zogratis"), Map.entry("zenon_zogratis_form2", "bc_zenon_zogratis_form2"),
            Map.entry("zagred", "bc_zagred"), Map.entry("zagred_form2", "bc_zagred_form2"),
            Map.entry("zagred_legacy", "bc_zagred_legacy"), Map.entry("zagred_legacy_form2", "bc_zagred_legacy_form2"),
            Map.entry("lucifero", "bc_lucifero"), Map.entry("lucifero_form2", "bc_lucifero_form2"));

    /** The particle theme of each redesigned look's shimmer (an id of particle_themes.json). */
    private static final Map<String, String> THEME_OF = Map.ofEntries(
            Map.entry("bc_lucius_zogratis", "boss.lucius"), Map.entry("bc_lucius_zogratis_form2", "boss.lucius"),
            Map.entry("bc_dante_zogratis", "boss.dante"), Map.entry("bc_dante_zogratis_form2", "boss.dante"),
            Map.entry("bc_vanica_zogratis", "boss.vanica"), Map.entry("bc_vanica_zogratis_form2", "boss.vanica"),
            Map.entry("bc_zenon_zogratis", "boss.zenon"), Map.entry("bc_zenon_zogratis_form2", "boss.zenon"),
            Map.entry("bc_zagred", "boss.zagred"), Map.entry("bc_zagred_form2", "boss.zagred"),
            Map.entry("bc_zagred_legacy", "boss.zagred"), Map.entry("bc_zagred_legacy_form2", "boss.zagred"),
            Map.entry("bc_lucifero", "boss.zagred"), Map.entry("bc_lucifero_form2", "boss.zagred"));

    /** The redesigned look id of an old look id, or null when there is none. */
    public static String of(String oldId) {
        String s = oldId == null ? null : NEW_OF.get(oldId);
        return s != null ? s : BcWave2.of(oldId);                       // wave 2 (docs/bosses_bc_wave2.md) has its own table
    }

    /** Is this the id of a redesigned look? */
    public static boolean isBc(String lookId) { return lookId != null && lookId.startsWith(PREFIX); }

    /** The particle theme id of a redesigned look's shimmer, or null. */
    public static String theme(String bcId) {
        String t = bcId == null ? null : THEME_OF.get(bcId);
        if (t != null) return t;
        t = BcWave2.theme(bcId);
        return t != null ? t : KakashiLooks.theme(bcId);                 // Kakashi Hatake (docs/bosses/kakashi.md)
    }

    /** Every old look id that has a redesigned look (for the tests and docs). */
    public static java.util.Set<String> oldIds() { return NEW_OF.keySet(); }

    /** Wave 1 and wave 2 together: every old look id that has a redesigned look (the audit walks this). */
    public static java.util.Set<String> allOldIds() {
        java.util.Set<String> all = new java.util.LinkedHashSet<>(NEW_OF.keySet());
        all.addAll(BcWave2.oldIds());
        return all;
    }
}
