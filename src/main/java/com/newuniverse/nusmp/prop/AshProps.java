package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.AshSpells;
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
 * 1.93.0 Ash Magic: the server behaviours of its one prop kind (ASH_1; variant in the low byte of {@code param}, see {@link AirPropKit}):
 * <ul>
 *   <li>SOLDIER (0), an ash soldier: follows its owner, walks up to the nearest hostile within 12 blocks and strikes it (6 damage x EP power, wither 2 s),
 *       crumbles over its last 20 ticks. Hittable (30 health).</li>
 *   <li>TRAP (1), an ash trap: lies in the ground as a mound of ash; when a foe comes within 2 blocks it springs (splintered branches rise): 8 damage x EP
 *       power, wither II 4 s, blind 3 s, a root for 1.5 s (shortened on bosses), a glow; it falls apart a second later. Armed for 30 s.</li>
 *   <li>SPRUNG (2), the picture of a trap that has just sprung (spawned under the attacker by Revelation of the Cowardly, whose damage was already dealt):
 *       only a visual, 30 ticks.</li>
 * </ul>
 * Props only auto-target hostile mobs (a trap springs on any foe that is not the owner or an ally, a player too, as the page that made it intends).
 */
public final class AshProps {
    private AshProps() {}

    public static final int SOLDIER = 0, TRAP = 1, SPRUNG = 2;
    private static final int COLOR = 0xFFB0AAA2;

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(PropKind.ASH_1, new Behavior());
        com.newuniverse.nusmp.book.ext.AttributeEvents.incoming(AshSpells::witherThorns);
        AshSelfTests.register();
    }

    private static final class Behavior implements MagicProps.Behavior {
        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            int v = AirPropKit.variant(e.param());
            float s = e.scale();
            if (v == SOLDIER) { e.setSize(0.8f * s, 2.0f * s); e.setHittable(30f); } else e.setSize(1.4f * s, 0.6f * s);
            int st = AirPropKit.state(e.param());
            e.setParam(AirPropKit.pack(v, st, sl.getGameTime()));
            if (v != TRAP) VfxSpawn.send(sl, VfxShape.ASH_FX3, e.position(), e.position().add(0, 1, 0), COLOR, 20, (v == SOLDIER ? 0.7f : 0.6f) * s);
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            if (owner == null) { e.expire(); return; }
            int v = AirPropKit.variant(e.param());
            long now = sl.getGameTime();
            if (v == SOLDIER && AirPropKit.state(e.param()) != AirPropKit.IDLE && AirPropKit.since(e.param(), now) > 18) AirPropKit.setState(e, AirPropKit.IDLE);
            switch (v) {
                case SOLDIER -> soldier(e, sl, owner);
                case TRAP -> trap(e, sl, owner, now);
                default -> e.setDeltaMovement(Vec3.ZERO);
            }
        }

        private void soldier(MagicPropEntity e, ServerLevel sl, LivingEntity owner) {
            if (e.maxLife() - e.life() < 20) { e.setDeltaMovement(Vec3.ZERO); return; }
            if ((e.life() + e.seed()) % 8 == 0 || !(e.target() instanceof LivingEntity tl && tl.isAlive() && AirPropKit.hostile(owner, tl))) e.setTarget(AirPropKit.nearest(e, owner, sl, 12));
            LivingEntity target = e.target() instanceof LivingEntity l && l.isAlive() ? l : null;
            Vec3 goal = target != null ? target.position() : owner.position();
            double dist = e.position().distanceTo(goal), reach = target != null ? 1.9 : 3.0;
            if (dist > reach) walk(e, goal, 0.22); else e.setDeltaMovement(0, e.onGround() ? -0.02 : e.getDeltaMovement().y - 0.08, 0);
            if (target != null && dist <= 2.3 && e.life() % 22 == 0) {
                AirPropKit.setState(e, AirPropKit.ACT);
                AirPropKit.face(e, target.position(), 60f);
                AirPropKit.hit(e, sl, owner, target, 6f * AirPropKit.powerOf(owner), TensuraDamageTypes.MAGIC_GENERIC);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
            }
        }

        private void walk(MagicPropEntity e, Vec3 to, double speed) {
            Vec3 d = to.subtract(e.position()).multiply(1, 0, 1);
            double len = d.length();
            double vy = e.onGround() ? -0.02 : Math.max(-0.6, e.getDeltaMovement().y - 0.08);
            Vec3 h = len < 1e-4 ? Vec3.ZERO : d.scale(Math.min(speed, len) / len);
            if (e.horizontalCollision && e.onGround()) vy = 0.42;
            e.setDeltaMovement(h.x, vy, h.z);
            e.move(MoverType.SELF, e.getDeltaMovement());
            if (len > 1e-4) AirPropKit.face(e, to, 28f);
        }

        private void trap(MagicPropEntity e, ServerLevel sl, LivingEntity owner, long now) {
            e.setDeltaMovement(Vec3.ZERO);
            if (AirPropKit.state(e.param()) == AirPropKit.ACT) {
                if (AirPropKit.since(e.param(), now) > 22) e.expire();
                return;
            }
            Vec3 at = e.position();
            for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(2.0, 1.2, 2.0), t -> AirPropKit.fair(owner, t))) {
                AirPropKit.setState(e, AirPropKit.ACT);
                for (LivingEntity v : sl.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(2.2, 1.5, 2.2), v -> AirPropKit.fair(owner, v))) {
                    AirPropKit.hit(e, sl, owner, v, 8f * AirPropKit.powerOf(owner), TensuraDamageTypes.MAGIC_GENERIC);
                    v.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
                    v.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BalanceLaw.controlTicks(v, 60), 0));
                    v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(v, 30), 9));
                    v.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0));
                }
                VfxSpawn.send(sl, VfxShape.ASH_FX3, at, at.add(0, 1, 0), COLOR, 22, 0.55f);
                return;
            }
        }

        @Override
        public void end(MagicPropEntity e, ServerLevel sl) {
            VfxSpawn.send(sl, VfxShape.ASH_FX3, e.position(), e.position().add(0, 1, 0), COLOR, 22, 0.6f * e.scale());
        }
    }
}
