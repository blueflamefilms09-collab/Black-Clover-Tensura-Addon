package com.newuniverse.nusmp.prop;

/** Eye Magic: the server behaviours of this magic's props (PropKind.EYE_1, PropKind.EYE_2). Eye Magic needs no props; it registers the Mark of Sight damage hook. */
public final class EyeProps {
    private EyeProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        com.newuniverse.nusmp.book.ext.AttributeEvents.incoming(com.newuniverse.nusmp.book.EyeArts::onIncoming);   // Mark of Sight
    }
}
