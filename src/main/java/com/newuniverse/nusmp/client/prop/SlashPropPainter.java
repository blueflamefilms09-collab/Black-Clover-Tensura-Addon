package com.newuniverse.nusmp.client.prop;

import com.newuniverse.nusmp.prop.PropKind;

public final class SlashPropPainter {
    private SlashPropPainter() {}

    public static void register() {
        PropPainters.register(PropKind.SLASH_1, (e, partial, age, pose, buffers, light) ->
                ModeledMagicProps.slash(e, age, pose, buffers, light));
        PropPainters.register(PropKind.SLASH_2, (e, partial, age, pose, buffers, light) ->
                ModeledMagicProps.slash(e, age, pose, buffers, light));
    }
}
