package com.newuniverse.nusmp.client.prop;

import com.newuniverse.nusmp.prop.PropKind;

public final class RecombinationPropPainter {
    private RecombinationPropPainter() {}

    public static void register() {
        PropPainters.register(PropKind.RECOMBINATION_1, (e, partial, age, pose, buffers, light) ->
                ModeledMagicProps.recombination(e, age, pose, buffers, light));
        PropPainters.register(PropKind.RECOMBINATION_2, (e, partial, age, pose, buffers, light) ->
                ModeledMagicProps.recombination(e, age, pose, buffers, light));
    }
}
