package com.newuniverse.nusmp.prop;

/**
 * Server lifetime and movement for Slash Magic's projected blades, reaping scythe and dash afterimages.
 * <ul>
 *   <li>SLASH_1 param 0: the projected crescent flies forward 1.15 blocks a tick along its yaw (as before); 1.93.0: param 1 is the Ripper Dash afterimage, which stays where it was left.</li>
 *   <li>SLASH_2: the Death Scythe hangs where it was cast and sweeps round it (the damage is the spell's).</li>
 * </ul>
 */
public final class SlashProps {
    private SlashProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        com.newuniverse.nusmp.book.SpatialRebuildChecks.register("slash", com.newuniverse.nusmp.blackclover.MagicType.SLASH,
                java.util.List.of("slash_wave", "forearm_blades", "ripper_dash", "death_scythe"),
                java.util.List.of("lunatic_slash", "adaptive_edge"), "SLASH",
                java.util.List.of("slash_helm", "slash_chest", "slash_arm_r", "slash_arm_l", "slash_leg_r", "slash_leg_l", "slash_back", "slash_halo", "slash_crescent", "slash_scythe", "slash_ghost"),
                java.util.List.of("SLASH_1", "SLASH_2"), java.util.List.of("SLASH_FX1", "SLASH_FX2", "SLASH_FX3"));
        MagicProps.register(PropKind.SLASH_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, net.minecraft.server.level.ServerLevel sl) {
                e.setNoGravity(true);
                e.setSize(0.6f * e.scale(), 0.6f * e.scale());
            }
            @Override public void tick(MagicPropEntity e, net.minecraft.server.level.ServerLevel sl) {
                if (e.param() == 1) return;                                 // an afterimage stays
                double yaw = Math.toRadians(e.getYRot());
                e.setPos(e.getX() - Math.sin(yaw) * 1.15, e.getY(), e.getZ() + Math.cos(yaw) * 1.15);
            }
        });
        MagicProps.register(PropKind.SLASH_2, (e, sl) -> e.setNoGravity(true));
    }
}
