package com.newuniverse.nusmp.prop;

import net.minecraft.server.level.ServerLevel;

/**
 * 1.93.0 Mirror Magic: the server side of the two summon models (client.prop.MirrorPropPainter draws them); the gameplay stays in the book (MirrorWorks catches shots,
 * jams casters and cuts; these only stand or fly where the spell put them, never saved).
 * <ul>
 *   <li>{@link PropKind#MIRROR_1}: a standing ornate mirror hanging where it was summoned. Yaw = the way it faces, scale = size (1 = a person's height), param = the
 *       shape (bits 0 and 1: 0 oval, 1 round, 2 arch, 3 diamond) with bit 16 = follow its owner. Its position is the middle of the glass.</li>
 *   <li>{@link PropKind#MIRROR_2}: the blade-edged mirror. param 0 flies forward along its yaw (0.9 blocks a tick, Mirrors Slash); param 1 falls (1.0 block a tick,
 *       Mirrors Meteorite). Its position is the middle of the blade.</li>
 * </ul>
 */
public final class MirrorProps {
    private MirrorProps() {}

    /** Called once by PropRegistry. */
    public static void init() {
        com.newuniverse.nusmp.book.MirrorRebuild.initHooks();
        com.newuniverse.nusmp.book.SpatialRebuildChecks.register("mirror", com.newuniverse.nusmp.blackclover.MagicType.MIRROR,
                java.util.List.of("reflect_refrain", "real_double", "reflect_ray", "large_reflect_ray", "full_reflection", "mirror_array", "mirror_step", "mirrors_slash", "mirrors_meteorite"),
                java.util.List.of("reflect_iris", "glass_garb"), "MIRROR",
                java.util.List.of("mirror_helm", "mirror_chest", "mirror_arm_r", "mirror_arm_l", "mirror_leg_r", "mirror_leg_l", "mirror_back", "mirror_halo", "mirror_oval", "mirror_round", "mirror_arch", "mirror_diamond", "mirror_blade"),
                java.util.List.of("MIRROR_1", "MIRROR_2"), java.util.List.of("MIRROR_FX1", "MIRROR_FX2", "MIRROR_FX3"));
        MagicProps.register(PropKind.MIRROR_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setNoGravity(true); e.setSize(1.2f * e.scale(), 2.2f * e.scale()); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) {
                if ((e.param() & 16) == 0) return;                                  // bit 16: the mirror stands before its owner and follows them
                net.minecraft.world.entity.LivingEntity o = e.owner();
                if (o == null) return;
                net.minecraft.world.phys.Vec3 look = o.getViewVector(1f).multiply(1, 0, 1);
                if (look.lengthSqr() < 1.0E-4) return;
                net.minecraft.world.phys.Vec3 at = o.position().add(look.normalize().scale(1.9)).add(0, 1.2, 0);
                e.setPos(at.x, at.y, at.z);
                e.setYRot(o.getYRot());
            }
        });
        MagicProps.register(PropKind.MIRROR_2, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setNoGravity(true); e.setSize(0.6f * e.scale(), 0.6f * e.scale()); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) {
                if (e.param() == 1) { e.setPos(e.getX(), e.getY() - 1.0, e.getZ()); return; }
                double yaw = Math.toRadians(e.getYRot());
                e.setPos(e.getX() - Math.sin(yaw) * 0.9, e.getY(), e.getZ() + Math.cos(yaw) * 0.9);
            }
        });
    }
}
