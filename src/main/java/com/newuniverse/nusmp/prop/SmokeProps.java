package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SmokeBook;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * 1.93.0 Smoke Magic: the server behaviours of its one prop kind (SMOKE_1; the variant is in the low byte of {@code param}, see {@link AirPropKit}):
 * <ul>
 *   <li>CLONE (0), a smoke clone: a figure of billows that stands where it was made. Every half second it breathes smoke on everything within 3.5 blocks
 *       (blindness and a slow), it makes mobs hunting the owner lose them, it cuts the nearest hostile within 4 blocks for 5 damage, and when it ends it
 *       bursts into a cloud that blinds everything within 4 blocks.</li>
 *   <li>SERPENT (1), a smoke serpent: lunges along the way it faces for 12 ticks at 0.85 blocks a tick, biting every foe it passes through once (damage
 *       and blindness), then thins away.</li>
 *   <li>COIL (2), a coiling serpent: follows the prop's target, holding it in a coil (a slow that cannot be walked off, a little damage every half second)
 *       for as long as the shortened-control time of the target allows.</li>
 * </ul>
 * Everything a prop does to a player belongs to the page that made it (the book's hurt); on its own it only fights hostile mobs.
 */
public final class SmokeProps {
    private SmokeProps() {}

    public static final int CLONE = 0, SERPENT = 1, COIL = 2;
    private static final int COLOR = 0xFF9A90B2;

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.SMOKE_1, new Behavior());
        com.newuniverse.nusmp.book.ext.AttributeEvents.incoming(SmokeBook::dodge);
        SmokeSelfTests.register();
    }

    private static final class Behavior implements MagicProps.Behavior {
        private final java.util.Map<Integer, Set<Integer>> bitten = new java.util.HashMap<>();

        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            int v = AirPropKit.variant(e.param());
            float s = e.scale();
            if (v == CLONE) e.setSize(0.8f * s, 2.0f * s); else e.setSize(0.9f * s, 0.9f * s);
            e.setParam(AirPropKit.pack(v, 0, sl.getGameTime()));
            VfxSpawn.send(sl, VfxShape.SMOKE_FX3, e.position(), e.position().add(0, 1, 0), COLOR, 20, (v == CLONE ? 0.9f : 0.5f) * s);
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            if (owner == null) { e.expire(); return; }
            int v = AirPropKit.variant(e.param());
            long now = sl.getGameTime();
            int st = AirPropKit.state(e.param());
            if (st != AirPropKit.IDLE && AirPropKit.since(e.param(), now) > (v == CLONE ? 18 : 14)) { e.setParam(AirPropKit.pack(v, 0, now)); st = 0; }
            switch (v) {
                case CLONE -> clone(e, sl, owner, now);
                case SERPENT -> serpent(e, sl, owner);
                default -> coil(e, sl, owner, now);
            }
        }

        private void clone(MagicPropEntity e, ServerLevel sl, LivingEntity owner, long now) {
            e.setDeltaMovement(Vec3.ZERO);
            if (e.maxLife() - e.life() < 14) return;                                           // vanishing
            if (e.life() % 10 == 0) {
                for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, e.getBoundingBox().inflate(3.5, 1.5, 3.5), t -> AirPropKit.fair(owner, t))) {
                    t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BalanceLaw.controlTicks(t, 40), 0));
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
                }
                for (Mob m : sl.getEntitiesOfClass(Mob.class, e.getBoundingBox().inflate(16, 6, 16), m -> m.getTarget() == owner)) m.setTarget(null);
            }
            LivingEntity target = AirPropKit.nearest(e, owner, sl, 4.0);
            if (target != null) {
                AirPropKit.face(e, target.position(), 40f);
                if (e.life() % 24 == 0) {
                    AirPropKit.setState(e, AirPropKit.ACT);
                    AirPropKit.hit(e, sl, owner, target, 5f * AirPropKit.powerOf(owner), TensuraDamageTypes.DARKNESS_ELEMENTAL);
                    target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
                }
            }
        }

        private void serpent(MagicPropEntity e, ServerLevel sl, LivingEntity owner) {
            Set<Integer> seen = bitten.computeIfAbsent(e.getId(), k -> new HashSet<>());
            Vec3 dir = Vec3.directionFromRotation(0f, e.getYRot());
            if (e.life() < 12) {
                Vec3 step = dir.scale(0.85);
                AABB sweep = e.getBoundingBox().expandTowards(step).inflate(0.5);
                for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, sweep, t -> AirPropKit.fair(owner, t))) {
                    if (!seen.add(t.getId())) continue;
                    SmokeBook.serpentBite(owner, e, t);
                }
                e.setPos(e.position().add(step));
            } else {
                e.setDeltaMovement(Vec3.ZERO);
            }
        }

        private void coil(MagicPropEntity e, ServerLevel sl, LivingEntity owner, long now) {
            if (!(e.target() instanceof LivingEntity t) || !t.isAlive()) { e.expire(); return; }
            e.setPos(t.getX(), t.getY(), t.getZ());
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 9, false, false));
            if (e.life() % 10 == 0) SmokeBook.coilSqueeze(owner, e, t);
        }

        @Override
        public void end(MagicPropEntity e, ServerLevel sl) {
            bitten.remove(e.getId());
            int v = AirPropKit.variant(e.param());
            Vec3 at = e.position();
            LivingEntity owner = e.owner();
            VfxSpawn.send(sl, VfxShape.SMOKE_FX3, at, at.add(0, 1, 0), COLOR, 24, (v == CLONE ? 1.1f : 0.6f) * e.scale());
            if (v == CLONE && owner != null)
                for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(4.0), t -> AirPropKit.fair(owner, t)))
                    t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BalanceLaw.controlTicks(t, 50), 0));
        }
    }
}
