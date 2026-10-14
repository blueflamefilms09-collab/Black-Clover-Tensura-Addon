package com.newuniverse.nusmp.core.magic.profile;

/** How a magic is drawn. Only used to decide which shapes a profile hands out; appending a value never changes the others. */
public enum VfxStyle {
    /** The magic has its own layer (three effects named &lt;NAME&gt;_FX1..3, or a hand-picked legacy set). */
    NATIVE,
    /** The shared palette-driven wiki layers (WIKI_MAGIC_CAST / FIELD / BURST). */
    WIKI,
    /** Nothing of its own: the generic wiki-style effect in the magic's colour. The final fallback; it always works. */
    GENERIC
}
