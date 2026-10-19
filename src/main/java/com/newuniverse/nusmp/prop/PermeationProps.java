package com.newuniverse.nusmp.prop;

import net.minecraft.server.level.ServerLevel;

/**
 * 1.93.0 Permeation Magic: the server side of the two summon models (client.prop.PermeationPropPainter draws them); both only stand where the spell put them (no gravity,
 * never saved), the gameplay is the spell's.
 * <ul>
 *   <li>{@link PropKind#PERMEATION_1}: the phase echo, a hollow ghost of the caster left standing where they stepped out of the world (Phase Step). Yaw = the caster's
 *       yaw, scale = 1 for a person.</li>
 *   <li>{@link PropKind#PERMEATION_2}: the phase door, a doorway of thin bars with a membrane of wisps standing in a wall (Phase Through). Yaw = the way it faces, scale = size.</li>
 * </ul>
 */
public final class PermeationProps {
    private PermeationProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        com.newuniverse.nusmp.book.PermeationWorks.initHooks();
        com.newuniverse.nusmp.book.SpatialRebuildChecks.register("permeation", com.newuniverse.nusmp.blackclover.MagicType.PERMEATION,
                java.util.List.of("attribute_bolt", "attribute_cut", "attribute_field", "attribute_domain", "attribute_guard"),
                java.util.List.of("phase_step", "phase_through", "permeating_strike", "ghost_zone"), "PERMEATION",
                java.util.List.of("permeation_helm", "permeation_chest", "permeation_arm_r", "permeation_arm_l", "permeation_leg_r", "permeation_leg_l", "permeation_back", "permeation_halo", "permeation_echo", "permeation_door"),
                java.util.List.of("PERMEATION_1", "PERMEATION_2"), java.util.List.of("PERMEATION_FX1", "PERMEATION_FX2", "PERMEATION_FX3"));
        MagicProps.register(PropKind.PERMEATION_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setNoGravity(true); e.setSize(0.7f * e.scale(), 1.9f * e.scale()); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { }
        });
        MagicProps.register(PropKind.PERMEATION_2, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setNoGravity(true); e.setSize(1.6f * e.scale(), 2.8f * e.scale()); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { }
        });
    }
}
