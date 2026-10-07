package com.newuniverse.nusmp.prop;

/** Curse Magic: the damage and death hooks of Hex Marks and Hex Aegis. The magic needs no props (PropKind.CURSE_1 / _2 stay unused). */
public final class CurseProps {
    private CurseProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        com.newuniverse.nusmp.book.CurseArts.hooks();
    }
}
