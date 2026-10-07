package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.book.LegionArts;
import com.newuniverse.nusmp.book.ext.AttributeEvents;

/** 0.53 Legion Magic: the server behaviours of this magic's props (PropKind.LEGION_1, PropKind.LEGION_2). The soldiers are Legion VFX, so no prop is needed; this only drops a caster's army on logout. */
public final class LegionProps {
    private LegionProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        AttributeEvents.logout(LegionArts::forget);
    }
}
