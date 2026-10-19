package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.book.TreeKit;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Vine Magic: the server behaviours of PropKind.VINE_1 (variant = low byte of the param; painter client.prop.VinePropPainter):
 * <ul>
 *   <li>variant 0, the Vine Serpent: rises out of the ground (first 20 ticks), then hunts the foe nearest to the caster within 14 blocks (0.28 blocks per tick); within 2.4 blocks it strikes every
 *       30 ticks (the bite lands 6 ticks after it starts: 8 damage, rooted, poisoned). Without a foe it circles the caster. Hittable (30 health). Param bits 8-9: 0 idle, 1 strike.</li>
 *   <li>variant 1, the Vine Whip: a whip that grows out of the ground at a foe and lashes once (it lives 14 ticks; the spell that spawned it did the damage); purely visual.</li>
 *   <li>variant 2, the Thicket Wall: a solid wall of living vines (3.4 wide, 2.6 high); every 10 ticks whoever stands within 2.4 blocks of it is cut (1.5), rooted and pricked.</li>
 * </ul>
 */
public final class VineProps {
    private VineProps() {}

    public static void init() {
        MagicProps.register(PropKind.VINE_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) {
                float s = e.scale();
                switch (e.param() & TreeProps.VARIANT) {
                    case 0 -> { e.setSize(1.4f * s, 1.0f * s); e.setHittable(30); }
                    case 1 -> e.setSize(0.8f * s, 2.6f * s);
                    default -> { e.setSize(3.4f * s, 2.6f * s); e.setSolid(true); }
                }
            }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) {
                switch (e.param() & TreeProps.VARIANT) {
                    case 0 -> serpent(e, sl);
                    case 2 -> wall(e, sl);
                    default -> { }
                }
            }
            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                if ((e.param() & TreeProps.VARIANT) != 1) TreeProps.burst(e, sl, VfxShape.VINE_FX3, 0.9f);
                TreeKit.unbind(e);
            }
        });
    }

    private static void serpent(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !TreeKit.bound(e)) { e.expire(); return; }
        CompoundTag d = e.getPersistentData();
        int t = e.life();
        TreeProps.keepNear(e, p);
        if (t < 20) { TreeProps.phase(e, 0); return; }
        LivingEntity foe = TreeKit.nearest(p, p.position(), 14);
        Vec3 goal = foe != null ? foe.position() : p.position().add(Math.cos(t * 0.05) * 3.5, 0, Math.sin(t * 0.05) * 3.5);
        int bite = d.getInt("bite");
        if (bite > 0) {
            int k = t - bite;
            if (k == 6) {
                Vec3 at = e.position().add(Vec3.directionFromRotation(0, e.getYRot()).scale(1.6 * e.scale()));
                for (LivingEntity v : TreeKit.foes(p, at, 2.4 * e.scale())) {
                    TreeKit.hurt(e, v, 8f);
                    TreeKit.root(v, 40, 3);
                    TreeKit.wound(v, 60, 0, 0.003);
                }
            }
            if (k >= 14) { d.putInt("bite", 0); d.putInt("cd", t + 16); TreeProps.phase(e, 0); } else TreeProps.phase(e, 1, k);
            return;
        }
        TreeProps.walk(e, sl, goal, 0.28, foe != null ? 1.6 : 2.5);
        if (foe != null && e.distanceTo(foe) < 2.4 + e.scale() && t >= d.getInt("cd")) { d.putInt("bite", t); TreeProps.phase(e, 1); }
    }

    private static void wall(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !TreeKit.bound(e)) { e.expire(); return; }
        if (e.life() % 10 != 0) return;
        for (LivingEntity v : TreeKit.foes(p, e.position().add(0, 1, 0), 2.4 * e.scale())) {
            TreeKit.hurt(e, v, 1.5f);
            TreeKit.root(v, 20, 2);
            TreeKit.wound(v, 30, 0, 0.002);
        }
    }
}
