package com.newuniverse.nusmp.prop;

/** 0.53 Bubble Magic: the server behaviours of this magic's props (PropKind.BUBBLE_1, PropKind.BUBBLE_2); the bubble spells use no prop, only damage hooks. */
public final class BubbleProps {
    private BubbleProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        com.newuniverse.nusmp.book.BubbleArts.hooks();
    }
}
