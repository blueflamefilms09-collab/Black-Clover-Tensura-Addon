package com.newuniverse.nusmp.client.prop;

import com.newuniverse.nusmp.prop.PropKind;

public final class SandPropPainter {
    private SandPropPainter() {}

    public static void register() {
        PropPainters.register(PropKind.SAND_1, (e, partial, age, pose, buffers, light) ->
                ModeledMagicProps.sand(e, age, pose, buffers, light));
        PropPainters.register(PropKind.SAND_2, (e, partial, age, pose, buffers, light) ->
                ModeledMagicProps.sand(e, age, pose, buffers, light));
    }
}
