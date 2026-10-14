package com.newuniverse.nusmp.compat;

import com.newuniverse.nusmp.grimoire.BookLook;
import com.newuniverse.nusmp.grimoire.BookMotif;

/**
 * The look of the Compatibility Codex item, as an entry of the grimoire look core (pure): a violet leather cover, gold frame metal, a gold emblem
 * of two interlocking rings (two mods linked) and a cyan glow while it is held, the same colours the Codex screen's glass uses. The geometry is the
 * grimoire's own ({@code GrimoireBookPlan}); the textures are {@code emblem_compat}, {@code cover_art_compat} and {@code cover_base_compat} under
 * textures/item/grimoire_book (tools/compat/gen_compat_codex_assets.py).
 */
public final class CompatCodexLook {
    private CompatCodexLook() {}

    public static final String COVER_ID = "COMPAT";
    public static final int COVER = 0x35295E, TRIM = 0xE2B95A, EMBLEM = 0xF0C860, GLOW = 0x5CD8FF;

    /** The one look every Compatibility Codex has (no per-owner shade, no tattering). */
    public static final BookLook LOOK = new BookLook(COVER_ID, BookMotif.COMPAT, false, COVER, TRIM, EMBLEM, GLOW, null, "");

    /** Item colour handler value for a tint layer: ARGB, or -1 (untinted). */
    public static int argb(int tintIndex) {
        int rgb = LOOK.tint(tintIndex);
        return rgb < 0 ? -1 : 0xFF000000 | rgb;
    }
}
