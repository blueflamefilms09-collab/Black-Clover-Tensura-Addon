package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.book.TreeKit;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Mud Magic: the server behaviours of PropKind.MUD_1 (variant = low byte of the param; painter client.prop.MudPropPainter):
 * <ul>
 *   <li>variant 0, the Mud Golem (Mud Magic: Mud Golem): a golem of 3.4 blocks that rises out of the ground in its first 22 ticks, follows the caster and pounds the nearest foe within 3.6
 *       blocks every 48 ticks (both fists land 16 ticks after it starts: 14 damage, thrown back). It can be hit (80 health). Param bits 8-9: 0 idle, 1 slam.</li>
 *   <li>variant 1, the Mud Spout (Mud Magic: Mud Spout): a spout of mud; at tick 8 it erupts under whoever is within 1.8 blocks (7 damage, thrown up); it stays 30 ticks. Phase 1 from the eruption.</li>
 * </ul>
 */
public final class MudProps {
    private MudProps() {}

    public static void init() {
        MagicProps.register(PropKind.MUD_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) {
                float s = e.scale();
                if ((e.param() & TreeProps.VARIANT) == 0) { e.setSize(1.8f * s, 3.4f * s); e.setHittable(80); } else e.setSize(1.6f * s, 2.8f * s);
            }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { if ((e.param() & TreeProps.VARIANT) == 0) golem(e, sl); else spout(e, sl); }
            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                if ((e.param() & TreeProps.VARIANT) == 0) TreeProps.burst(e, sl, VfxShape.MUD_FX3, 1.1f);
                TreeKit.unbind(e);
            }
        });
    }

    private static void golem(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !TreeKit.bound(e)) { e.expire(); return; }
        CompoundTag d = e.getPersistentData();
        int t = e.life();
        TreeProps.keepNear(e, p);
        if (t < 22) { TreeProps.phase(e, 0); return; }
        LivingEntity foe = TreeKit.nearest(p, p.position(), 14);
        Vec3 goal = foe != null ? foe.position() : p.position().add(Math.cos(t * 0.02) * 3.5, 0, Math.sin(t * 0.02) * 3.5);
        int slam = d.getInt("slam");
        if (slam > 0) {
            int k = t - slam;
            if (k == 16) {
                Vec3 at = e.position().add(Vec3.directionFromRotation(0, e.getYRot()).scale(1.8 * e.scale()));
                TreeProps.burst(e, sl, VfxShape.MUD_FX3, 0.9f);
                for (LivingEntity v : TreeKit.foes(p, at, 3.6 * e.scale())) {
                    TreeKit.hurt(e, v, 14f);
                    TreeKit.knock(v, e.position(), 1.0, 0.4);
                }
            }
            if (k >= 28) { d.putInt("slam", 0); d.putInt("cd", t + 20); TreeProps.phase(e, 0); } else TreeProps.phase(e, 1, k);
            return;
        }
        TreeProps.walk(e, sl, goal, 0.13, foe != null ? 2.4 : 3.2);
        if (foe != null && e.distanceTo(foe) < 3.6 + e.scale() && t >= d.getInt("cd")) { d.putInt("slam", t); TreeProps.phase(e, 1); }
    }

    private static void spout(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !TreeKit.bound(e)) { e.expire(); return; }
        TreeProps.phase(e, e.life() >= 8 ? 1 : 0, e.life() - 8);
        if (e.life() == 8) {
            TreeProps.burst(e, sl, VfxShape.MUD_FX3, 0.8f);
            for (LivingEntity v : TreeKit.foes(p, e.position().add(0, 0.8, 0), 1.8 * e.scale())) {
                TreeKit.hurt(e, v, 7f);
                TreeKit.lift(v, 0.9);
            }
        }
    }
}
