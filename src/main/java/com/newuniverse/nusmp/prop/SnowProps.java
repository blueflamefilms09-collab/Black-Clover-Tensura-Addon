package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SnowBook;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1.93.0 Snow Magic: the server behaviours of its one prop kind (SNOW_1; variant in the low byte of {@code param}, see {@link AirPropKit}):
 * <ul>
 *   <li>GOLEM (0), a snow golem: follows its owner, walks up to the nearest hostile within 12 blocks and slams it (8 damage x EP power in a 2.2 block ring,
 *       slow II for 3 s, frost), and crumbles over its last 24 ticks. Hittable (it takes 40 damage).</li>
 *   <li>CRYSTAL (1), a frost crystal: stands where it was grown; every second it pulses frost on everything within 4.5 blocks (3 damage, slow, frostbite), and
 *       when it ends it shatters (an ice burst, 6 damage, slow).</li>
 * </ul>
 * Props only auto-target hostile mobs; players are hit by the pages that made them.
 */
public final class SnowProps {
    private SnowProps() {}

    public static final int GOLEM = 0, CRYSTAL = 1;
    private static final int COLOR = 0xFFBFE6FF;

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.SNOW_1, new Behavior());
        com.newuniverse.nusmp.book.ext.AttributeEvents.incoming(SnowBook::frostThorns);
        SnowSelfTests.register();
    }

    private static void chill(LivingEntity t, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, ticks), amp));
        t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 40, t.getTicksFrozen() + 60));
    }

    private static final class Behavior implements MagicProps.Behavior {
        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            int v = AirPropKit.variant(e.param());
            float s = e.scale();
            if (v == GOLEM) { e.setSize(1.3f * s, 2.9f * s); e.setHittable(40f); } else e.setSize(0.9f * s, 1.9f * s);
            e.setParam(AirPropKit.pack(v, 0, sl.getGameTime()));
            VfxSpawn.send(sl, VfxShape.SNOW_FX3, e.position(), e.position().add(0, 1, 0), COLOR, 22, (v == GOLEM ? 0.9f : 0.6f) * s);
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            if (owner == null) { e.expire(); return; }
            int v = AirPropKit.variant(e.param());
            long now = sl.getGameTime();
            if (AirPropKit.state(e.param()) != AirPropKit.IDLE && AirPropKit.since(e.param(), now) > 20) AirPropKit.setState(e, AirPropKit.IDLE);
            if (v == GOLEM) golem(e, sl, owner, now); else crystal(e, sl, owner);
        }

        private void golem(MagicPropEntity e, ServerLevel sl, LivingEntity owner, long now) {
            if (e.maxLife() - e.life() < 24) { e.setDeltaMovement(Vec3.ZERO); return; }          // crumbling
            if ((e.life() + e.seed()) % 8 == 0 || !(e.target() instanceof LivingEntity tl && tl.isAlive() && AirPropKit.hostile(owner, tl)))
                e.setTarget(AirPropKit.nearest(e, owner, sl, 12));
            LivingEntity target = e.target() instanceof LivingEntity l && l.isAlive() ? l : null;
            Vec3 goal = target != null ? target.position() : owner.position();
            double dist = e.position().distanceTo(goal);
            double reach = target != null ? 2.3 : 3.0;
            if (dist > reach) walk(e, goal, 0.2);
            else e.setDeltaMovement(0, e.onGround() ? -0.02 : e.getDeltaMovement().y - 0.08, 0);
            if (target != null && dist <= 3.0 && e.life() % 26 == 0) {
                AirPropKit.setState(e, AirPropKit.ACT);
                AirPropKit.face(e, target.position(), 60f);
                Vec3 at = e.position();
                for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(2.2, 1.5, 2.2), t -> AirPropKit.fair(owner, t))) {
                    AirPropKit.hit(e, sl, owner, t, 8f * AirPropKit.powerOf(owner), TensuraDamageTypes.ICE_ELEMENTAL);
                    chill(t, 60, 1);
                }
                VfxSpawn.send(sl, VfxShape.SNOW_FX3, at.add(0, 0.1, 0), at.add(0, 1, 0), COLOR, 20, 0.5f * e.scale());
            }
        }

        private void walk(MagicPropEntity e, Vec3 to, double speed) {
            Vec3 d = to.subtract(e.position()).multiply(1, 0, 1);
            double len = d.length();
            Vec3 dm = e.getDeltaMovement();
            double vy = e.onGround() ? -0.02 : Math.max(-0.6, dm.y - 0.08);
            Vec3 h = len < 1e-4 ? Vec3.ZERO : d.scale(Math.min(speed, len) / len);
            if (e.horizontalCollision && e.onGround()) vy = 0.42;
            e.setDeltaMovement(h.x, vy, h.z);
            e.move(MoverType.SELF, e.getDeltaMovement());
            if (len > 1e-4) AirPropKit.face(e, to, 28f);
        }

        private void crystal(MagicPropEntity e, ServerLevel sl, LivingEntity owner) {
            e.setDeltaMovement(Vec3.ZERO);
            if (e.life() > 6 && e.life() % 20 == 0) {
                Vec3 at = e.position();
                boolean any = false;
                for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(4.5, 2.5, 4.5), t -> AirPropKit.fair(owner, t))) {
                    AirPropKit.hit(e, sl, owner, t, 3f * AirPropKit.powerOf(owner), TensuraDamageTypes.ICE_ELEMENTAL);
                    chill(t, 50, 0);
                    any = true;
                }
                if (any) AirPropKit.setState(e, AirPropKit.ACT);
                VfxSpawn.send(sl, VfxShape.SNOW_FX2, at.add(0, 0.05, 0), at.add(0, 1, 0), COLOR, 24, 4.5f / 1.5f);
            }
        }

        @Override
        public void end(MagicPropEntity e, ServerLevel sl) {
            int v = AirPropKit.variant(e.param());
            Vec3 at = e.position();
            LivingEntity owner = e.owner();
            VfxSpawn.send(sl, VfxShape.SNOW_FX3, at, at.add(0, 1, 0), COLOR, 24, (v == GOLEM ? 1.0f : 0.8f) * e.scale());
            if (v == CRYSTAL && owner != null)
                for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(3.5), t -> AirPropKit.fair(owner, t))) {
                    AirPropKit.hit(e, sl, owner, t, 6f * AirPropKit.powerOf(owner), TensuraDamageTypes.ICE_ELEMENTAL);
                    chill(t, 50, 1);
                }
        }
    }
}
