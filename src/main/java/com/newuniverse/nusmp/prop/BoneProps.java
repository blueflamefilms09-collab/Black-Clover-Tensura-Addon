package com.newuniverse.nusmp.prop;

/** Server lifetime and movement for Bone Magic's summoned spear and ossuary props. */
public final class BoneProps {
    private BoneProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.BONE_1, (e, sl) -> e.setNoGravity(true));
        MagicProps.register(PropKind.BONE_2, (e, sl) -> e.setNoGravity(true));
    }
}
