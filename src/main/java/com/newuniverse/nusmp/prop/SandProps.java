package com.newuniverse.nusmp.prop;

/** Non-interactive sand spell models; collision and damage remain owned by their grimoire pages. */
public final class SandProps {
    private SandProps() {}

    public static void init() {
        MagicProps.register(PropKind.SAND_1, (e, sl) -> e.setNoGravity(true));
        MagicProps.register(PropKind.SAND_2, (e, sl) -> e.setNoGravity(true));
    }
}
