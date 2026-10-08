package com.newuniverse.nusmp.client.prop;

import com.newuniverse.nusmp.prop.PropKind;

public final class BonePropPainter {
    private BonePropPainter() {}

    public static void register() {
        PropPainters.register(PropKind.BONE_1, (e, partial, age, pose, buffers, light) ->
                ModeledMagicProps.bone(e, age, pose, buffers, light));
        PropPainters.register(PropKind.BONE_2, (e, partial, age, pose, buffers, light) ->
                ModeledMagicProps.bone(e, age, pose, buffers, light));
    }
}
