package com.newuniverse.nusmp.prop;

/** 0.53 Curse-Warding Magic: the server behaviours of this magic's props (PropKind.CURSE_WARDING_1, PropKind.CURSE_WARDING_2). the Curse-Warding spells use no prop, only damage and tick hooks. */
public final class CurseWardingProps {
    private CurseWardingProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        com.newuniverse.nusmp.book.CurseWardingArts.hooks();
    }
}
