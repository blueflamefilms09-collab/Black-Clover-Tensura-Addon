package com.newuniverse.nusmp.client.prop;

import com.newuniverse.nusmp.prop.PropKind;

public final class LegionPropPainter {
    private LegionPropPainter() {}

    public static void register() {
        PropPainters.register(PropKind.LEGION_1, (e, partial, age, pose, buffers, light) ->
                ModeledMagicProps.legionPiece(e, pose, buffers, light));
        PropPainters.register(PropKind.LEGION_2, (e, partial, age, pose, buffers, light) ->
                ModeledMagicProps.legionBoard(e, pose, buffers, light));
    }
}
