package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.AirBook;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 1.93.0 Air Magic: the server behaviours of its one prop kind (AIR_1; variant in the low byte of {@code param}, see {@link AirPropKit}):
 * <ul>
 *   <li>BLADE (0), a flying air blade: flies the way it was aimed (yaw and pitch of the prop) at 0.9 blocks a tick for 20 ticks, cutting every foe it passes
 *       once for 11 damage x EP power and pushing it aside; it ends where a wall stops it.</li>
 *   <li>ORB (1), a pressure orb: hangs where it was made and swells for 1.5 s; at the end it bursts: 12 damage x EP power within 4.5 blocks and a hard push
 *       (a boss at a third of the push).</li>
 *   <li>ORBIT (2), an orbiting air blade: circles the owner at shoulder height; every second, when a hostile is within 10 blocks, it flies at it, cuts it for
 *       5 damage x EP power and returns.</li>
 * </ul>
 * Props only auto-target hostile mobs; players are hit by the page that made the blade or the burst.
 */
public final class AirProps {
    private AirProps() {}

    public static final int BLADE = 0, ORB = 1, ORBIT = 2;
    private static final int COLOR = 0xFFB8F2FF;

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.AIR_1, new Behavior());
        com.newuniverse.nusmp.book.ext.AttributeEvents.incoming(AirBook::cushion);
        AirSelfTests.register();
    }

    private static final class Behavior implements MagicProps.Behavior {
        private final Map<Integer, Set<Integer>> cut = new HashMap<>();

        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            int v = AirPropKit.variant(e.param());
            float s = e.scale();
            e.setSize((v == ORB ? 1.0f : 0.8f) * s, (v == ORB ? 1.0f : 0.5f) * s);
            e.setParam(AirPropKit.pack(v, 0, sl.getGameTime()));
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            if (owner == null) { e.expire(); return; }
            int v = AirPropKit.variant(e.param());
            long now = sl.getGameTime();
            if (AirPropKit.state(e.param()) != AirPropKit.IDLE && AirPropKit.since(e.param(), now) > 10) AirPropKit.setState(e, AirPropKit.IDLE);
            switch (v) {
                case BLADE -> blade(e, sl, owner);
                case ORB -> e.setDeltaMovement(Vec3.ZERO);
                default -> orbit(e, sl, owner);
            }
        }

        private void blade(MagicPropEntity e, ServerLevel sl, LivingEntity owner) {
            Set<Integer> seen = cut.computeIfAbsent(e.getId(), k -> new HashSet<>());
            float yaw = e.getYRot() * Mth.DEG_TO_RAD, pitch = e.getXRot() * Mth.DEG_TO_RAD;
            Vec3 step = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch)).scale(0.9);
            Vec3 next = e.position().add(step);
            var hit = sl.clip(new net.minecraft.world.level.ClipContext(e.position(), next, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, e));
            if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) { e.expire(); return; }
            for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, e.getBoundingBox().expandTowards(step).inflate(0.6), t -> AirPropKit.fair(owner, t))) {
                if (!seen.add(t.getId())) continue;
                AirBook.bladeCut(owner, e, t);
            }
            e.setPos(next);
        }

        private void orbit(MagicPropEntity e, ServerLevel sl, LivingEntity owner) {
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
                    VfxSpawn.send(sl, VfxShape.AIR_FX3, aim, aim.add(0, 0.5, 0), COLOR, 14, 0.25f);
                    e.setTarget(null);
                    AirPropKit.setState(e, AirPropKit.IDLE);
                }
            } else {
                int slot = Math.floorMod(e.seed(), 5);
                double a = e.life() * 0.14 + slot * Mth.TWO_PI / 5;
                steer(e, owner.position().add(Math.cos(a) * 1.8, 1.3 + 0.15 * Math.sin(e.life() * 0.2 + slot), Math.sin(a) * 1.8), 0.7);
                e.setXRot(0f);
            }
        }

        private void steer(MagicPropEntity e, Vec3 to, double speed) {
            Vec3 d = to.subtract(e.position());
            double len = d.length();
            if (len < 1e-4) return;
            e.setPos(e.position().add(d.scale(Math.min(speed, len) / len)));
            AirPropKit.face(e, to, 70f);
        }

        @Override
        public void end(MagicPropEntity e, ServerLevel sl) {
            cut.remove(e.getId());
            int v = AirPropKit.variant(e.param());
            Vec3 at = e.position();
            LivingEntity owner = e.owner();
            VfxSpawn.send(sl, VfxShape.AIR_FX3, at, at.add(0, 0.5, 0), COLOR, v == ORB ? 24 : 14, (v == ORB ? 1.1f : 0.3f) * e.scale());
            if (v != ORB || owner == null) return;
            for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(4.5), t -> AirPropKit.fair(owner, t))) {
                AirPropKit.hit(e, sl, owner, t, 12f * AirPropKit.powerOf(owner), TensuraDamageTypes.WIND_ELEMENTAL);
                Vec3 out = t.position().add(0, t.getBbHeight() / 2, 0).subtract(at);
                if (out.lengthSqr() > 1e-4) {
                    Vec3 o = out.normalize().scale(BalanceLaw.isBoss(t) ? 0.4 : 1.4);
                    t.setDeltaMovement(t.getDeltaMovement().add(o.x, 0.3 + Math.max(0, o.y * 0.3), o.z));
                    t.hurtMarked = true;
                }
            }
        }
    }
}
