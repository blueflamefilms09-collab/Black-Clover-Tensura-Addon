package com.newuniverse.nusmp.prop;

import net.minecraft.server.level.ServerLevel;

/**
 * 1.93.0 Spatial Magic: the server side of the two things the spells stand in the world. Both only hang where they were put (no gravity, a small
 * hitbox, never saved): the gameplay of a gate (the hop between the pair) and of a rift (the cut damage) stays in the book's own zones, so the
 * models are pure presentation and cost nothing when nobody looks.
 * <ul>
 *   <li>{@link PropKind#SPATIAL_1}: the gate frame (Fallen Angel Gate, Door of Fate). Yaw = the way the gate faces, scale = size (1 = a door), param 0.</li>
 *   <li>{@link PropKind#SPATIAL_2}: a rift, a standing tear in space (Spatial Cut, Myriad Black, Dimension Collapse). Yaw = the way it was cut, scale = height factor, param 1 = a collapse (bigger, turns).</li>
 * </ul>
 */
public final class SpatialProps {
    private SpatialProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        com.newuniverse.nusmp.book.SpatialWorks.initHooks();      // the Gate Garb's incoming-damage hook
        com.newuniverse.nusmp.book.SpatialRebuildChecks.register("spatial", com.newuniverse.nusmp.blackclover.MagicType.SPATIAL,
                java.util.List.of("domination", "fallen_angel_gate", "space_cut", "unopening_red_room", "myriad_black", "door_of_fate"),
                java.util.List.of("dimensional_armour", "gate_strike", "gate_swap", "dimension_collapse"), "SPATIAL",
                java.util.List.of("spatial_helm", "spatial_chest", "spatial_arm_r", "spatial_arm_l", "spatial_leg_r", "spatial_leg_l", "spatial_back", "spatial_halo", "spatial_gate", "spatial_rift"),
                java.util.List.of("SPATIAL_1", "SPATIAL_2"), java.util.List.of("SPATIAL_FX1", "SPATIAL_FX2", "SPATIAL_FX3"));
        MagicProps.register(PropKind.SPATIAL_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setNoGravity(true); e.setSize(1.6f * e.scale(), 2.4f * e.scale()); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { }
        });
        MagicProps.register(PropKind.SPATIAL_2, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setNoGravity(true); e.setSize(0.8f * e.scale(), 2.4f * e.scale()); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { }
        });
    }
}
