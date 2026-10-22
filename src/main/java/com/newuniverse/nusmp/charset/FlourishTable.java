package com.newuniverse.nusmp.charset;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the table of {@link CharacterFlourishRest} as pure data (no game class, so the JUnit shim can read it): for each character of the "rest" builder the two extra effect layers that go
 * around every cast of his grimoire. The shapes are the names of constants of {@code vfx.VfxShape} (a test checks each name exists).
 */
public final class FlourishTable {
    private FlourishTable() {}

    /** Where a layer is drawn. */
    public enum Where { FEET, AIM, LINE, FOLLOW }

    /** One extra layer of a character's flourish. */
    public record Layer(String shape, Where where, int color, int ticks, float power) {}

    private static final Map<String, List<Layer>> BY_CANON = Map.ofEntries(
            Map.entry("charlotte", List.of(new Layer("BRIAR_FX2", Where.FEET, 0xFF3A6AD8, 26, 1.6f), new Layer("BRIAR_FX3", Where.AIM, 0xFFC8202C, 20, 1.0f))),
            Map.entry("jack", List.of(new Layer("SLASH_BLADES", Where.FEET, 0xFF30D090, 14, 1.2f), new Layer("SLASH_WAVE", Where.LINE, 0xFF9CFFD8, 14, 1.0f))),
            Map.entry("rill", List.of(new Layer("PAINT_PALETTE", Where.FEET, 0xFFFFC84A, 24, 1.4f), new Layer("PAINT_SPLAT", Where.AIM, 0xFF3A7BFF, 18, 1.2f))),
            Map.entry("dorothy", List.of(new Layer("DREAM_MANIFEST", Where.FEET, 0xFFE88AA0, 28, 1.4f), new Layer("DREAM_TRANSITION", Where.AIM, 0xFFF0B0D8, 20, 1.0f))),
            Map.entry("zenon", List.of(new Layer("BONE_FX1", Where.AIM, 0xFFE0E0EC, 20, 1.2f), new Layer("SPACE_CRACK", Where.AIM, 0xFF8A5AD0, 18, 1.0f))),
            Map.entry("vanica", List.of(new Layer("BLOOD_SIGIL", Where.FEET, 0xFFB01020, 26, 1.4f), new Layer("BLOOD_ARCS", Where.LINE, 0xFF5A0A12, 14, 1.1f))),
            Map.entry("dante", List.of(new Layer("GRAVITY_IMPACT", Where.AIM, 0xFF7A3CC8, 20, 1.3f), new Layer("GRAVITY_AURA", Where.FOLLOW, 0xFF3A2A4A, 24, 1.0f))),
            Map.entry("lucius", List.of(new Layer("SOUL_VORTEX", Where.AIM, 0xFF72D9CC, 22, 1.2f), new Layer("DIVINE_HALO", Where.FOLLOW, 0xFFE8D070, 26, 1.0f))),
            Map.entry("rades", List.of(new Layer("CORPSE_PACT", Where.FEET, 0xFF9AA6C8, 26, 1.4f), new Layer("SOUL_CORPSE_FX1", Where.AIM, 0xFF6A7298, 20, 1.1f))),
            Map.entry("gadjah", List.of(new Layer("FX_LIGHTNING_ARC", Where.LINE, 0xFFFFE65A, 10, 1.0f), new Layer("LIGHTNINGFX_BURST", Where.AIM, 0xFFFFF6C8, 16, 1.2f))),
            Map.entry("mars", List.of(new Layer("CRYSTAL_FX1", Where.FEET, 0xFF78A8C8, 24, 1.3f), new Layer("EARTHFAM_BURST", Where.AIM, 0xFFD8E8F4, 20, 1.2f))),
            Map.entry("kahono", List.of(new Layer("SONG_NOTES", Where.FOLLOW, 0xFFE8C0F0, 40, 1.2f), new Layer("SONG_RING", Where.FEET, 0xFFE8D8A0, 30, 2.0f))),
            Map.entry("kiato", List.of(new Layer("BUTOH_DANCE_RIPPLE", Where.FEET, 0xFF78D0E8, 28, 1.6f), new Layer("BUTOH_AFTERIMAGE", Where.FOLLOW, 0xFF18506A, 20, 1.0f))));

    /** The extra layers of a character (empty for everyone else). */
    public static List<Layer> layersOf(String canon) { return canon == null ? List.of() : BY_CANON.getOrDefault(canon, List.of()); }

    /** The characters that have a flourish. */
    public static Set<String> characters() { return BY_CANON.keySet(); }
}
