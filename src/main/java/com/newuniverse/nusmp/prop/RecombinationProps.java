package com.newuniverse.nusmp.prop;

/** Visual timber constructs accompany Recombination Magic's existing functional spells. */
public final class RecombinationProps {
    private RecombinationProps() {}

    public static void init() {
        MagicProps.register(PropKind.RECOMBINATION_1, (e, sl) -> e.setNoGravity(true));
        MagicProps.register(PropKind.RECOMBINATION_2, (e, sl) -> e.setNoGravity(true));
    }
}
