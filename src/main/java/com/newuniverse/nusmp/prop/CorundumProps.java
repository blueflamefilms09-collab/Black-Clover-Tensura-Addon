package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.book.CorundumSummons;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 0.54 Corundum Magic: the server behaviours of the two props (the casts and the damage: book.CorundumSummons).
 * CORUNDUM_1, the Ideal Closer fist: param bit 0 = gem (0 ruby, 1 sapphire), bit 1 = hammer-fist (the second blow). The position of the prop is the
 * CENTRE of the gauntlet (the painter lowers the model to match). Its timeline, in ticks since it appeared (the painter keeps the same numbers):
 * 0 to 14 it flies from the caster to its stand-off point in front of the foe, 14 to the strike it draws back, the strike (2 ticks, 3 for the hammer)
 * ends in the blow at 22 (24), then it returns to the stand-off point by 34 and hovers until its life runs out.
 * CORUNDUM_2, a gem shard: param = slot (bits 0-2) | shard count << 3 | gem << 6; the server writes its state into bits 8-9 (0 orbit, 1 launched, 2 returning)
 * for the painter. It orbits the caster at 1.7 blocks; every ~20 ticks, when a foe is within 12 blocks of the caster, it darts through the foe
 * (piercing everything on the way, once each) and returns.
 * Both follow their owner's life: when the owner dies or logs out they end, and every end is a burst of gem.
 */
public final class CorundumProps {
    private CorundumProps() {}

    private static final int RUBY = 0xFFFF6A7A, SAPPHIRE = 0xFF6AA6FF;
    /** The fist: the tick the hand starts to draw back, where the strike starts (punch / hammer), where it lands (punch / hammer) and where it is home again. */
    public static final int FIST_WINDUP = 14, FIST_STRIKE = 20, FIST_STRIKE_SLAM = 21, FIST_HIT = 22, FIST_HIT_SLAM = 24, FIST_BACK = 34;
    /** The shard: ticks of a dart (it flies on through the foe), its speed in blocks per tick and its reach from the caster. */
    private static final int DART_TICKS = 13;
    private static final double DART_SPEED = 1.5, SHARD_REACH = 12;

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.CORUNDUM_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setSize(0.8f * e.scale(), 0.8f * e.scale()); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { fistTick(e, sl); }
            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                Vec3 c = e.position();
                VfxSpawn.send(sl, VfxShape.CORUNDUM_FX3, c, c.add(0, 0.6, 0), (e.param() & 1) == 0 ? RUBY : SAPPHIRE, 24, 1.1f);
                CorundumSummons.unbind(e);
            }
        });
        MagicProps.register(PropKind.CORUNDUM_2, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setSize(0.5f, 0.5f); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { shardTick(e, sl); }
            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                VfxSpawn.send(sl, VfxShape.CORUNDUM_FX3, e.position(), e.position().add(0, 0.4, 0), ((e.param() >> 6) & 1) == 0 ? RUBY : SAPPHIRE, 18, 0.7f);
                CorundumSummons.unbind(e);
            }
        });
    }

    /** Minecraft yaw (0 = +z, the prop's front) of a horizontal direction. */
    private static float yawOf(double dx, double dz) { return (float) (Mth.atan2(-dx, dz) * (180.0 / Math.PI)); }

    private static Vec3 lerp(Vec3 a, Vec3 b, double k) { return a.add(b.subtract(a).scale(k)); }

    private static double smooth(double x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    // ================================================================ the fist
    private static void fistTick(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !CorundumSummons.bound(e)) { e.expire(); return; }
        CompoundTag d = e.getPersistentData();
        int t = e.life();
        boolean slam = (e.param() & 2) != 0;
        float s = e.scale();
        if (t == 1) { d.putDouble("sx", e.getX()); d.putDouble("sy", e.getY()); d.putDouble("sz", e.getZ()); }
        // the foe's centre (the last known one when it is gone)
        Vec3 tc;
        double half = 0.4, height = 1.0;
        LivingEntity foe = e.target() instanceof LivingEntity l && l.isAlive() ? l : null;
        if (foe != null) {
            tc = foe.getBoundingBox().getCenter();
            half = foe.getBbWidth() * 0.5;
            height = foe.getBbHeight();
            d.putDouble("tx", tc.x); d.putDouble("ty", tc.y); d.putDouble("tz", tc.z);
        } else if (d.contains("tx")) {
            tc = new Vec3(d.getDouble("tx"), d.getDouble("ty"), d.getDouble("tz"));
        } else {
            tc = p.getEyePosition().add(p.getViewVector(1f).scale(6));
        }
        Vec3 flat = tc.subtract(p.position()).multiply(1, 0, 1);
        flat = flat.lengthSqr() < 1e-4 ? p.getViewVector(1f).multiply(1, 0, 1) : flat;
        flat = flat.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : flat.normalize();
        double reach = 1.2 * s;                                                           // centre to knuckles of the model
        double lift = slam ? 0.6 * s : 0;
        Vec3 home = tc.subtract(flat.scale(reach + 2.0 + half)).add(0, lift, 0);
        Vec3 back = home.subtract(flat.scale(0.9 * s));
        Vec3 hit = tc.subtract(flat.scale(reach * 0.8 + half)).add(0, slam ? Math.min(0.5, height * 0.25) : 0, 0);
        int strike = slam ? FIST_STRIKE_SLAM : FIST_STRIKE, land = slam ? FIST_HIT_SLAM : FIST_HIT;
        Vec3 start = new Vec3(d.getDouble("sx"), d.getDouble("sy"), d.getDouble("sz"));
        Vec3 pos;
        if (t < FIST_WINDUP) pos = lerp(start, home, smooth(t / (double) FIST_WINDUP));
        else if (t < strike) pos = lerp(home, back, smooth((t - FIST_WINDUP) / (double) (strike - FIST_WINDUP)));
        else if (t < land) pos = lerp(back, hit, (t - strike) / (double) (land - strike));
        else if (t < FIST_BACK) pos = lerp(hit, home, smooth((t - land) / (double) (FIST_BACK - land)));
        else pos = home.add(0, 0.12 * Math.sin(t * 0.15), 0);
        if (t == land && foe != null) CorundumSummons.fistImpact(e, p, foe, slam);
        e.setPos(pos.x, pos.y, pos.z);
        e.setDeltaMovement(Vec3.ZERO);
        double fx = tc.x - pos.x, fz = tc.z - pos.z;
        if (fx * fx + fz * fz > 1e-4) e.setYRot(Mth.approachDegrees(e.getYRot(), yawOf(fx, fz), t < FIST_WINDUP ? 30f : 60f));
    }

    // ================================================================ the shards
    private static void shardTick(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !CorundumSummons.bound(e)) { e.expire(); return; }
        int slot = e.param() & 7, n = Math.max(1, (e.param() >> 3) & 7), t = e.life();
        CompoundTag d = e.getPersistentData();
        int phase = d.getInt("cp"), pt = d.getInt("ct");
        double a = t * 0.11 + slot * (Math.PI * 2 / n);
        Vec3 home = p.position().add(Math.cos(a) * 1.7, 1.25 + 0.3 * Math.sin(t * 0.09 + slot * 1.9), Math.sin(a) * 1.7);
        Vec3 pos = e.position();
        if (pos.distanceToSqr(home) > 30 * 30) pos = home;                                 // the owner teleported: snap back
        Vec3 goal = home;
        double k = 0.35;
        float yaw = yawOf(-Math.sin(a), Math.cos(a));                                      // the tip leads along the orbit
        float turn = 18f;
        if (phase == 0) {
            int cd = d.getInt("cc");
            if (cd > 0) d.putInt("cc", cd - 1);
            else if (t % 3 == slot % 3) {
                LivingEntity foe = CorundumSummons.nearest(p, p.position(), SHARD_REACH);
                if (foe != null) {
                    Vec3 tc = foe.getBoundingBox().getCenter();
                    Vec3 dir = tc.subtract(pos);
                    dir = dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : dir.normalize();
                    d.putDouble("dx", dir.x); d.putDouble("dy", dir.y); d.putDouble("dz", dir.z);
                    d.putIntArray("ch", new int[0]);
                    e.setTarget(foe);
                    phase = 1;
                    pt = 0;
                }
            }
        }
        if (phase == 1) {
            Vec3 dir = new Vec3(d.getDouble("dx"), d.getDouble("dy"), d.getDouble("dz"));
            LivingEntity foe = e.target() instanceof LivingEntity l && l.isAlive() ? l : null;
            if (foe != null && pt < 6) {                                                   // home in during the first ticks
                Vec3 want = foe.getBoundingBox().getCenter().subtract(pos);
                if (want.lengthSqr() > 1e-6) dir = lerp(dir, want.normalize(), 0.4).normalize();
                d.putDouble("dx", dir.x); d.putDouble("dy", dir.y); d.putDouble("dz", dir.z);
            }
            pos = pos.add(dir.scale(DART_SPEED));
            goal = pos;
            k = 1;
            yaw = yawOf(dir.x, dir.z);
            turn = 90f;
            int[] hit = d.getIntArray("ch");
            for (LivingEntity l : CorundumSummons.foes(p, pos, 1.35)) {
                boolean seen = false;
                for (int id : hit) if (id == l.getId()) { seen = true; break; }
                if (seen) continue;
                int[] more = java.util.Arrays.copyOf(hit, hit.length + 1);
                more[hit.length] = l.getId();
                hit = more;
                CorundumSummons.shardPierce(e, p, l);
            }
            d.putIntArray("ch", hit);
            pt++;
            if (pt >= DART_TICKS) { phase = 2; pt = 0; e.setTarget(null); }
        } else if (phase == 2) {
            turn = 40f;
            if (pos.distanceToSqr(home) < 1.0) { phase = 0; d.putInt("cc", 14 + slot * 6); }
            k = 0.3;
            Vec3 dd = home.subtract(pos);
            if (dd.lengthSqr() > 1e-4) yaw = yawOf(dd.x, dd.z);
        }
        d.putInt("cp", phase);
        d.putInt("ct", pt);
        int sync = (e.param() & 0xFF) | (phase << 8);                                     // the painter reads the phase from param bits 8-9
        if (sync != e.param()) e.setParam(sync);
        Vec3 np = phase == 1 ? goal : pos.add(goal.subtract(pos).scale(k));
        e.setPos(np.x, np.y, np.z);
        e.setDeltaMovement(Vec3.ZERO);
        e.setYRot(Mth.approachDegrees(e.getYRot(), yaw, turn));
    }
}
