package com.newuniverse.nusmp.prop;

/** 0.53 Gel Magic: the server behaviours of this magic's props (PropKind.GEL_1, PropKind.GEL_2); the gel spells use no prop, only damage hooks. */
public final class GelProps {
    private GelProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        com.newuniverse.nusmp.book.GelArts.hooks();
    }
}
