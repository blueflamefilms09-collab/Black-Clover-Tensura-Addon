package com.newuniverse.nusmp.prop;

/** Server lifetime and movement for Slash Magic's projected blades and reaping scythe. */
public final class SlashProps {
    private SlashProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.SLASH_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, net.minecraft.server.level.ServerLevel sl) {
                e.setNoGravity(true);
                e.setSize(0.6f * e.scale(), 0.6f * e.scale());
            }
            @Override public void tick(MagicPropEntity e, net.minecraft.server.level.ServerLevel sl) {
                double yaw = Math.toRadians(e.getYRot());
                e.setPos(e.getX() - Math.sin(yaw) * 1.15, e.getY(), e.getZ() + Math.cos(yaw) * 1.15);
            }
        });
        MagicProps.register(PropKind.SLASH_2, (e, sl) -> e.setNoGravity(true));
    }
}
