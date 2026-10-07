package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.book.KeySummons;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 0.54 Key Magic: the server behaviours of the two props (casts and damage: book.KeySummons).
 * KEY_1, the Gate of Keys: stands where it was cast facing the nearest foe; its doors open after 10 ticks and every 10 ticks it looses a bolt of
 * the element in param (0 fire, 1 ice, 2 lightning, 3 wind) from its keyhole at the nearest foe within 14 blocks; the doors close in its last 14 ticks.
 * KEY_2, a Great Key: param = slot (bits 0-1) | key count << 2. It orbits its owner at 1.9 blocks and, whenever a foe is within 12 blocks of the
 * owner, winds back, thrusts its tip into it and returns (target synced so the painter can aim, tick counter in its data).
 * Both follow their owner's life: when the owner dies or logs out they end, and every end is a golden burst.
 */
public final class KeyProps {
    private KeyProps() {}

    private static final int GOLD = 0xFFFFD04A;
    /** The gate: ticks until the doors are open, ticks between bolts, ticks it closes before its end, reach. */
    public static final int GATE_OPEN_AT = 10, GATE_FIRE_EVERY = 10, GATE_CLOSE = 14;
    private static final double GATE_REACH = 14;

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.KEY_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setSize(1.4f, 3.2f); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { gateTick(e, sl); }
            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                Vec3 c = e.position().add(0, 1.6 * e.scale(), 0);
                VfxSpawn.send(sl, VfxShape.KEY_FX3, c, c.add(0, 1, 0), GOLD, 26, 1.4f);
                KeySummons.unbind(e);
            }
        });
        MagicProps.register(PropKind.KEY_2, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setSize(0.6f, 0.6f); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { guardTick(e, sl); }
            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                VfxSpawn.send(sl, VfxShape.KEY_FX3, e.position(), e.position().add(0, 0.5, 0), GOLD, 20, 0.8f);
                KeySummons.unbind(e);
            }
        });
    }

    /** Minecraft yaw (0 = +z, the prop's front) of a horizontal direction. */
    private static float yawOf(double dx, double dz) { return (float) (Mth.atan2(-dx, dz) * (180.0 / Math.PI)); }

    // ================================================================ the gate
    private static void gateTick(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !KeySummons.bound(e)) { e.expire(); return; }
        int t = e.life();
        Vec3 mouth = e.position().add(0, 1.75 * e.scale(), 0);
        if (t % 5 == 0) e.setTarget(KeySummons.nearest(p, mouth, GATE_REACH));
        Entity tg = e.target();
        if (tg instanceof LivingEntity l && l.isAlive()) {
            Vec3 d = l.position().subtract(e.position());
            e.setYRot(Mth.approachDegrees(e.getYRot(), yawOf(d.x, d.z), 10f));
        }
        if (t < GATE_OPEN_AT + 6 || t > e.maxLife() - GATE_CLOSE || t % GATE_FIRE_EVERY != 0) return;
        if (tg instanceof LivingEntity l && l.isAlive()) {
            double yaw = Math.toRadians(e.getYRot());
            Vec3 from = mouth.add(-Math.sin(yaw) * 0.9 * e.scale(), 0, Math.cos(yaw) * 0.9 * e.scale());
            KeySummons.gateBolt(e, p, l, from, e.param() & 3);
        }
    }

    // ================================================================ the great keys
    private static void guardTick(MagicPropEntity e, ServerLevel sl) {
        if (!(e.owner() instanceof ServerPlayer p) || !KeySummons.bound(e)) { e.expire(); return; }
        int slot = e.param() & 3, n = Math.max(1, (e.param() >> 2) & 7), t = e.life();
        CompoundTag d = e.getPersistentData();
        double a = t * 0.085 + slot * (Math.PI * 2 / n);
        Vec3 home = p.position().add(Math.cos(a) * 1.9, 1.55 + 0.25 * Math.sin(t * 0.13 + slot * 2.1), Math.sin(a) * 1.9);
        Vec3 pos = e.position();
        if (pos.distanceToSqr(home) > 26 * 26) pos = home;                                 // the owner teleported: snap back
        int ks = d.getInt("ks");
        LivingEntity foe = e.target() instanceof LivingEntity l && l.isAlive() ? l : null;
        if (ks > 0 && foe == null) { ks = 0; d.putInt("kc", 12); e.setTarget(null); }
        if (ks == 0) {
            int cd = d.getInt("kc");
            if (cd > 0) d.putInt("kc", cd - 1);
            else if (t % 4 == slot % 4) {
                LivingEntity f = KeySummons.nearest(p, p.position(), 12);
                if (f != null) { e.setTarget(f); foe = f; ks = 1; }
            }
        }
        Vec3 goal = home;
        double k = 0.35;
        float yaw;
        if (ks > 0 && foe != null) {
            Vec3 tc = foe.getBoundingBox().getCenter();
            Vec3 dir = tc.subtract(pos);
            dir = dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : dir.normalize();
            double reach = 34.0 / 16.0 * e.scale() + 0.15;                                 // centre to tip of the model, plus a little
            if (ks <= 6) goal = home.subtract(dir.scale(0.5));                             // wind back
            else if (ks <= 11) { goal = tc.subtract(dir.scale(reach)); k = 0.65; }         // thrust
            else goal = home;                                                              // recover
            if (ks == 9 && pos.distanceTo(tc) < reach + 1.6) KeySummons.strike(e, p, foe);
            yaw = yawOf(dir.x, dir.z);
            ks++;
            if (ks > 20) { ks = 0; d.putInt("kc", 16 + slot * 7); e.setTarget(null); }
            d.putInt("ks", ks);
        } else {
            d.putInt("ks", 0);
            yaw = yawOf(-Math.sin(a), Math.cos(a));                                        // the tip leads along the orbit
        }
        int sync = (e.param() & 0xFF) | (Math.min(ks, 255) << 8);                       // the painter reads the stab tick from param bits 8-15
        if (sync != e.param()) e.setParam(sync);
        Vec3 np = pos.add(goal.subtract(pos).scale(k));
        e.setPos(np.x, np.y, np.z);
        e.setDeltaMovement(Vec3.ZERO);
        e.setYRot(Mth.approachDegrees(e.getYRot(), yaw, ks > 0 ? 40f : 18f));
    }
}
