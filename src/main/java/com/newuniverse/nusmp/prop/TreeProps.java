package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.book.TreeKit;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Tree Magic: the server behaviours of PropKind.TREE_1 (the variant is the low byte of the param; the painter is client.prop.TreePropPainter and reads the same numbers):
 * <ul>
 *   <li>variant 0, the Heartwood Guardian (Tree Magic: Heartwood Guardian): a treant of 3.2 blocks that rises from the ground in its first 22 ticks, follows the caster, and slams the nearest foe
 *       within 3.4 blocks every 44 ticks (the fists land 14 ticks after it starts: 12 damage, thrown back, rooted). It can be hit (60 health). Param bits 8-9: 0 idle, 1 slam.</li>
 *   <li>variant 1, the Root Lance (Tree Magic: Root Lance): a lance of roots that bursts out of the ground; at tick 6 it hurts every foe within 1.5 blocks (6 damage, thrown up, rooted) and
 *       stays 30 ticks. Painter clip: thrust.</li>
 * </ul>
 * This class also holds the helpers the other five nature magics' props use ({@link #walk}, {@link #phase}, {@link #colorOf}, {@link #burst}) and starts their {@code init()} so that the
 * prop registry needs a single line.
 */
public final class TreeProps {
    private TreeProps() {}

    public static final int VARIANT = 0xFF;

    public static void init() {
        MagicProps.register(PropKind.TREE_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) {
                float s = e.scale();
                if ((e.param() & VARIANT) == 0) { e.setSize(1.6f * s, 3.2f * s); e.setHittable(60); } else e.setSize(1.2f * s, 2.4f * s);
            }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { if ((e.param() & VARIANT) == 0) guardian(e, sl); else lance(e, sl); }
            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                if ((e.param() & VARIANT) == 0) burst(e, sl, VfxShape.TREE_FX3, 1.1f);
                TreeKit.unbind(e);
            }
        });
        VineProps.init();
        PlantProps.init();
        PoisonPlantProps.init();
        MudProps.init();
        MucusProps.init();
    }

    // ================================================================ shared helpers
    /** The colour of the book a prop belongs to (a fixed green when it is not bound). */
    public static int colorOf(MagicPropEntity e) {
        var b = TreeKit.bookOf(e);
        return b == null ? 0xFF88FF88 : b.color;
    }

    /** A burst of the magic's own effect at the prop. */
    public static void burst(MagicPropEntity e, ServerLevel sl, VfxShape shape, float power) {
        VfxSpawn.send(sl, shape, e.position(), e.position().add(0, 1, 0), colorOf(e), 24, power);
    }

    /** Writes the animation phase (bits 8-9) and the ticks since that action began (bits 10-15, 0 to 63) of the param, keeping the variant: the painter plays the clip at that time. */
    public static void phase(MagicPropEntity e, int phase, int since) {
        int v = (e.param() & VARIANT) | ((phase & 3) << 8) | ((Math.max(0, Math.min(63, since))) << 10);
        if (v != e.param()) e.setParam(v);
    }

    public static void phase(MagicPropEntity e, int phase) { phase(e, phase, 0); }

    /** Moves the prop toward the goal on the ground at the given blocks per tick (stops when closer than 'stop'), turning to face it. */
    public static void walk(MagicPropEntity e, ServerLevel sl, Vec3 goal, double speed, double stop) {
        Vec3 pos = e.position();
        double dx = goal.x - pos.x, dz = goal.z - pos.z, d = Math.sqrt(dx * dx + dz * dz);
        double nx = pos.x, nz = pos.z;
        if (d > stop) {
            double step = Math.min(speed, d - stop);
            nx += dx / d * step;
            nz += dz / d * step;
        }
        if (d > 0.05) e.setYRot(Mth.approachDegrees(e.getYRot(), (float) (Mth.atan2(-dx, dz) * (180.0 / Math.PI)), 24f));
        double gy = TreeKit.groundY(sl, new Vec3(nx, pos.y + 1.5, nz));
        e.setPos(nx, pos.y + (gy - pos.y) * 0.5, nz);
        e.setDeltaMovement(Vec3.ZERO);
    }

    /** Teleports the prop near the owner when it has fallen far behind (a teleport or a long fall). */
    public static void keepNear(MagicPropEntity e, LivingEntity owner) {
        if (e.distanceToSqr(owner) > 24 * 24) e.setPos(owner.getX() + 1.5, owner.getY(), owner.getZ() + 1.5);
    }

    // ================================================================ Tree
    private static void guardian(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !TreeKit.bound(e)) { e.expire(); return; }
        CompoundTag d = e.getPersistentData();
        int t = e.life();
        keepNear(e, p);
        if (t < 22) { phase(e, 0); return; }                                          // rising out of the ground
        LivingEntity foe = TreeKit.nearest(p, p.position(), 14);
        Vec3 goal = foe != null ? foe.position() : p.position().add(Math.cos(t * 0.02) * 3.0, 0, Math.sin(t * 0.02) * 3.0);
        int slam = d.getInt("slam");
        if (slam > 0) {                                                               // a slam in progress: stand still
            int k = t - slam;
            if (k == 14) {
                Vec3 at = e.position().add(Vec3.directionFromRotation(0, e.getYRot()).scale(1.6 * e.scale()));
                burst(e, sl, VfxShape.TREE_FX3, 0.9f);
                for (LivingEntity v : TreeKit.foes(p, at, 3.6 * e.scale())) {
                    TreeKit.hurt(e, v, 12f);
                    TreeKit.knock(v, e.position(), 0.9, 0.35);
                    TreeKit.root(v, 30, 2);
                }
            }
            if (k >= 26) { d.putInt("slam", 0); d.putInt("cd", t + 18); phase(e, 0); } else phase(e, 1, k);
            return;
        }
        walk(e, sl, goal, 0.16, foe != null ? 2.2 : 3.0);
        if (foe != null && e.distanceTo(foe) < 3.4 + e.scale() && t >= d.getInt("cd")) { d.putInt("slam", t); phase(e, 1); }
    }

    private static void lance(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !TreeKit.bound(e)) { e.expire(); return; }
        if (e.life() == 6) {
            burst(e, sl, VfxShape.TREE_FX3, 0.5f);
            for (LivingEntity v : TreeKit.foes(p, e.position().add(0, 0.8, 0), 1.5 * e.scale())) {
                TreeKit.hurt(e, v, 6f);
                TreeKit.lift(v, 0.8);
                TreeKit.root(v, 30, 2);
            }
        }
    }
}
