package com.newuniverse.nusmp.prop;

/** 0.53 Bronze Magic: the server behaviours of this magic's props (PropKind.BRONZE_1, PropKind.BRONZE_2); the bronze spells use no prop, only a damage hook. */
public final class BronzeProps {
    private BronzeProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        com.newuniverse.nusmp.book.BronzeArts.hooks();
    }
}
