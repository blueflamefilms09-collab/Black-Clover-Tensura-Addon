package com.newuniverse.nusmp.prop;

import net.minecraft.server.level.ServerLevel;

/**
 * 1.93.0 Gravity Magic: the server side of the two summon models (client.prop.GravityPropPainter draws them). Both fly by the velocity the spell set on them
 * ({@code e.setDeltaMovement}); the spell owns the damage (a bolt travelling the same path) and ends the prop with {@code e.expire()} when the bolt ends, so the
 * models cost nothing but a position update.
 * <ul>
 *   <li>{@link PropKind#GRAVITY_1}: the singularity orb (Gravity Sphere). Scale = size; param bit 0 = flying (the model stretches along the flight).</li>
 *   <li>{@link PropKind#GRAVITY_2}: the compressed rock sword (Evil God's Pressure Craft). Yaw = where it points, param = stage (bits 0 to 7: 0 forming, 1 hovering,
 *       2 launched) | (pitch in degrees + 90) &lt;&lt; 8. While forming it rises out of the ground; once launched it flies by its velocity.</li>
 * </ul>
 */
public final class GravityProps {
    private GravityProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        com.newuniverse.nusmp.book.GravityWorks.initHooks();
        com.newuniverse.nusmp.book.SpatialRebuildChecks.register("gravity", com.newuniverse.nusmp.blackclover.MagicType.GRAVITY,
                java.util.List.of("heavy_infighting", "demon_king", "gravity_sphere", "gravity_well", "crushing_weight"),
                java.util.List.of("heavy_infighting_gladiator", "evil_gods_pressure_craft", "lightened_step", "weighted_blows"), "GRAVITY",
                java.util.List.of("gravity_helm", "gravity_chest", "gravity_arm_r", "gravity_arm_l", "gravity_leg_r", "gravity_leg_l", "gravity_back", "gravity_halo", "gravity_orb", "gravity_sword"),
                java.util.List.of("GRAVITY_1", "GRAVITY_2"), java.util.List.of("GRAVITY_FX1", "GRAVITY_FX2", "GRAVITY_FX3"));
        MagicProps.register(PropKind.GRAVITY_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setNoGravity(true); e.setSize(0.9f * e.scale(), 0.9f * e.scale()); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { e.setPos(e.position().add(e.getDeltaMovement())); }
        });
        MagicProps.register(PropKind.GRAVITY_2, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setNoGravity(true); e.setSize(0.6f * e.scale(), 0.6f * e.scale()); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) {
                int stage = GravitySwordParam.stageOf(e.param());
                if (stage == GravitySwordParam.FORMING) e.setPos(e.getX(), e.getY() + 0.045, e.getZ());
                else if (stage == GravitySwordParam.LAUNCHED) e.setPos(e.position().add(e.getDeltaMovement()));
            }
        });
    }
}
