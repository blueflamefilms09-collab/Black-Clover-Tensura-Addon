package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.book.LegionArts;
import com.newuniverse.nusmp.book.ext.AttributeEvents;

/** Legion's chess pieces and battlefield board are short-lived, client-painted model props. */
public final class LegionProps {
    private LegionProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.LEGION_1, (e, sl) -> e.setNoGravity(true));
        MagicProps.register(PropKind.LEGION_2, (e, sl) -> e.setNoGravity(true));
        AttributeEvents.logout(LegionArts::forget);
    }
}
