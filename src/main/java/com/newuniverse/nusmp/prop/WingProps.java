package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.WingBook;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Wing Magic: the server behaviours of its one prop kind (WING_1; variant in the low byte of {@code param}, see {@link AirPropKit}):
 * <ul>
 *   <li>HAWK (0), a feather hawk: circles its owner two and a half blocks out and above, and dives at the nearest hostile within 14 blocks (or at the target
 *       a page gave it): 7 damage x EP power and a push, then climbs back; one dive every 1.5 s. A hawk given a target by a page stays on that target until
 *       it has struck once.</li>
 *   <li>BLADE (1), a feather blade: orbits the owner at shoulder height; every second, when a hostile is within 10 blocks, it flies at it (0.95 blocks a tick),
 *       cuts it for 5 damage x EP power and returns to its orbit.</li>
 * </ul>
 * Everything flies (no gravity). Props only auto-target hostile mobs; players are hit by a page that gave the prop its target.
 */
public final class WingProps {
    private WingProps() {}

    public static final int HAWK = 0, BLADE = 1;
    private static final int COLOR = 0xFFEFEAFF;

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.WING_1, new Behavior());
        com.newuniverse.nusmp.book.ext.AttributeEvents.incoming(WingBook::featherFall);
        WingSelfTests.register();
    }

    private static final class Behavior implements MagicProps.Behavior {
        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            int v = AirPropKit.variant(e.param());
            float s = e.scale();
            e.setSize((v == HAWK ? 0.9f : 0.5f) * s, (v == HAWK ? 0.8f : 0.9f) * s);
            e.setParam(AirPropKit.pack(v, 0, sl.getGameTime()));
            VfxSpawn.send(sl, VfxShape.WING_FX3, e.position(), e.position().add(0, 0.5, 0), COLOR, 18, 0.4f * s);
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            if (owner == null) { e.expire(); return; }
            int v = AirPropKit.variant(e.param());
            long now = sl.getGameTime();
            if (AirPropKit.state(e.param()) != AirPropKit.IDLE && AirPropKit.since(e.param(), now) > (v == HAWK ? 16 : 10)) AirPropKit.setState(e, AirPropKit.IDLE);
            if (v == HAWK) hawk(e, sl, owner, now); else blade(e, sl, owner, now);
        }

        private LivingEntity pick(MagicPropEntity e, ServerLevel sl, LivingEntity owner, double range) {
            if (e.target() instanceof LivingEntity t && t.isAlive()) return t;
            LivingEntity n = AirPropKit.nearest(e, owner, sl, range);
            e.setTarget(n);
            return n;
        }

        private void steer(MagicPropEntity e, Vec3 to, double speed) {
            Vec3 d = to.subtract(e.position());
            double len = d.length();
            if (len < 1e-4) return;
            Vec3 step = d.scale(Math.min(speed, len) / len);
            e.setPos(e.position().add(step));
            AirPropKit.face(e, to, 50f);
            e.setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))));
        }

        private void hawk(MagicPropEntity e, ServerLevel sl, LivingEntity owner, long now) {
            int phase = e.life() % 36;
            LivingEntity target = pick(e, sl, owner, 14);
            boolean diving = target != null && phase < 14 && (e.life() > 8);
            if (diving) {
                Vec3 aim = target.getBoundingBox().getCenter();
                steer(e, aim, 0.75);
                if (AirPropKit.state(e.param()) != AirPropKit.ACT2) AirPropKit.setState(e, AirPropKit.ACT2);
                if (e.position().distanceTo(aim) < 1.5 && phase > 1) {
                    AirPropKit.hit(e, sl, owner, target, 7f * AirPropKit.powerOf(owner), TensuraDamageTypes.WIND_ELEMENTAL);
                    Vec3 push = target.position().subtract(e.position()).multiply(1, 0, 1);
                    if (push.lengthSqr() > 1e-4) { Vec3 p = push.normalize().scale(0.5); target.setDeltaMovement(target.getDeltaMovement().add(p.x, 0.25, p.z)); target.hurtMarked = true; }
                    VfxSpawn.send(sl, VfxShape.WING_FX3, aim, aim.add(0, 0.5, 0), COLOR, 16, 0.3f);
                    e.setTarget(null);
                    if (e.maxLife() < 120) e.expire();
                }
            } else {
                double a = e.life() * 0.09 + e.seed() % 7;
                Vec3 home = owner.position().add(Math.cos(a) * 2.5, 2.6 + 0.3 * Math.sin(e.life() * 0.12), Math.sin(a) * 2.5);
                steer(e, home, 0.45);
                e.setXRot(0f);
            }
        }

        private void blade(MagicPropEntity e, ServerLevel sl, LivingEntity owner, long now) {
            int st = AirPropKit.state(e.param());
            LivingEntity target = st == AirPropKit.ACT && e.target() instanceof LivingEntity t && t.isAlive() ? t : null;
            if (target == null && (e.life() + e.seed()) % 20 == 0) {
                target = AirPropKit.nearest(e, owner, sl, 10);
                if (target != null) { e.setTarget(target); AirPropKit.setState(e, AirPropKit.ACT); }
            }
            if (target != null && AirPropKit.state(e.param()) == AirPropKit.ACT) {
                Vec3 aim = target.getBoundingBox().getCenter();
                steer(e, aim, 0.95);
                if (e.position().distanceTo(aim) < 1.2) {
                    AirPropKit.hit(e, sl, owner, target, 5f * AirPropKit.powerOf(owner), TensuraDamageTypes.WIND_ELEMENTAL);
                    VfxSpawn.send(sl, VfxShape.WING_FX3, aim, aim.add(0, 0.5, 0), COLOR, 14, 0.25f);
                    e.setTarget(null);
                    AirPropKit.setState(e, AirPropKit.IDLE);
                }
            } else {
                int slot = Math.floorMod(e.seed(), 4);
                double a = e.life() * 0.13 + slot * Mth.HALF_PI;
                Vec3 home = owner.position().add(Math.cos(a) * 1.7, 1.4 + 0.15 * Math.sin(e.life() * 0.2 + slot), Math.sin(a) * 1.7);
                steer(e, home, 0.7);
                e.setXRot(-20f);
            }
        }

        @Override
        public void end(MagicPropEntity e, ServerLevel sl) {
            VfxSpawn.send(sl, VfxShape.WING_FX3, e.position(), e.position().add(0, 0.5, 0), COLOR, 18, 0.35f * e.scale());
        }
    }
}
