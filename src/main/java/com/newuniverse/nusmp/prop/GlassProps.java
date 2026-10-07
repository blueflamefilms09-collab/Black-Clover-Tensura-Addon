package com.newuniverse.nusmp.prop;

/** 0.53 Glass Magic: the server behaviours of this magic's props (PropKind.GLASS_1, PropKind.GLASS_2); the glass spells use no prop, only a damage hook. */
public final class GlassProps {
    private GlassProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        com.newuniverse.nusmp.book.GlassArts.hooks();
    }
}
