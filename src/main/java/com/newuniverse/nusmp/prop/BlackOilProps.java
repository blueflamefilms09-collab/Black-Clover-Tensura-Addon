package com.newuniverse.nusmp.prop;

/** 0.53 Black Oil Magic: the server behaviours of this magic's props (PropKind.BLACK_OIL_1, PropKind.BLACK_OIL_2); the oil spells use no prop, only damage hooks. */
public final class BlackOilProps {
    private BlackOilProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        com.newuniverse.nusmp.book.BlackOilArts.hooks();
    }
}
