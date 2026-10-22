package com.newuniverse.nusmp.engraving.codex;

import com.newuniverse.nusmp.grimoire.BookLook;
import com.newuniverse.nusmp.grimoire.BookMotif;

/**
 * The look of the Engravement Codex item, as an entry of the grimoire look core (pure): a deep steel-blue leather cover, gold frame metal, a pale-gold emblem of a rune ring cut by a
 * blade (the engravings) and a cyan glow while it is held, the colours the Codex screen's glass uses. The geometry is the grimoire's own ({@code GrimoireBookPlan}); the textures are
 * {@code emblem_engraving}, {@code cover_art_engraving} and {@code cover_base_engraving} under textures/item/grimoire_book (tools/engraving_codex/gen_engraving_codex_assets.py).
 */
public final class EngravingCodexLook {
    private EngravingCodexLook() {}

    public static final String COVER_ID = "ENGRAVING";
    public static final int COVER = 0x1D3350, TRIM = 0xE8C468, EMBLEM = 0xF4DC8C, GLOW = 0x5CD8FF;

    /** The one look every Engravement Codex has (no per-owner shade, no tattering). */
    public static final BookLook LOOK = new BookLook(COVER_ID, BookMotif.ENGRAVING, false, COVER, TRIM, EMBLEM, GLOW, null, "");

    /** Item colour handler value for a tint layer: ARGB, or -1 (untinted). */
    public static int argb(int tintIndex) {
        int rgb = LOOK.tint(tintIndex);
        return rgb < 0 ? -1 : 0xFF000000 | rgb;
    }
}
