package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 1.93.0: the four prop behaviours the constructs of Shakudo, Scale, Spike, Nail and Hair Magic are made from. Each magic's {@code prop.<Name>Props}
 * registers its two {@link PropKind}s with one of them and a small config, so a construct is a handful of numbers, not a new class:
 * <ul>
 *   <li>{@link Companion}: follows its owner, walks to the nearest hostile within 12 blocks and strikes it (the Shakudo golem, the Scale serpent);</li>
 *   <li>{@link Guard}: holds a place (a solid slab) or hovers in front of / round the owner, turns or destroys projectiles that fly at it, and can mend
 *       the owner (the Shakudo bulwark, the Scale shield, the Hair cocoon);</li>
 *   <li>{@link Strike}: waits a few ticks, then hits everything round it once and may linger, bind or hold its target (the Spike pillar, the Nail
 *       hammer, the Hair tendril);</li>
 *   <li>{@link Missile}: flies straight or homes in, hits the first thing it meets, bursts or sticks in (the Nail bolt, the Spike urchin).</li>
 * </ul>
 * The synced word {@code param} is laid out as: bits 0-1 the state (0 idle or moving, 1 act, 2 act2, 3 stuck / done), bits 2-17 the game time (mod 65536)
 * at which the state began (so the painter plays the one-shot clips from their first frame), bits 18-21 a variant the spell chose at spawn. Damage
 * always goes through the balance law, bosses get shortened control, every tick runs inside {@link SafeCast#tick}.
 */
public final class ShakudoPropKit {
    private ShakudoPropKit() {}

    public static final int IDLE = ShakudoPropState.IDLE, ACT = ShakudoPropState.ACT, ACT2 = ShakudoPropState.ACT2, DONE = ShakudoPropState.DONE;

    // ------------------------------------------------------------------ the synced word (see ShakudoPropState)
    public static int state(int param) { return ShakudoPropState.state(param); }

    public static int since(int param, long gameTime) { return ShakudoPropState.since(param, gameTime); }

    public static int variant(int param) { return ShakudoPropState.variant(param); }

    public static int pack(int variant, int state, long gameTime) { return ShakudoPropState.pack(variant, state, gameTime); }

    static void setState(MagicPropEntity e, int st) { e.setParam(pack(variant(e.param()), st, e.level().getGameTime())); }

    // ------------------------------------------------------------------ helpers
    @FunctionalInterface public interface Hit { void apply(LivingEntity target, LivingEntity owner, MagicPropEntity prop, ServerLevel level); }
    public static final Hit NONE = (t, o, e, sl) -> {};

    public static Hit slow(int ticks, int amp) {
        return (t, o, e, sl) -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.isBoss(t) ? ticks / 3 : ticks, amp));
    }

    public static Hit all(Hit... hits) { return (t, o, e, sl) -> { for (Hit h : hits) h.apply(t, o, e, sl); }; }

    public static Hit effect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> eff, int ticks, int amp) {
        return (t, o, e, sl) -> t.addEffect(new MobEffectInstance(eff, BalanceLaw.isBoss(t) ? ticks / 2 : ticks, amp));
    }

    /** Knocks the target away from the prop. */
    public static Hit knock(double power, double up) {
        return (t, o, e, sl) -> {
            Vec3 out = t.position().subtract(e.position()).multiply(1, 0, 1);
            if (out.lengthSqr() < 1e-4) return;
            double k = BalanceLaw.isBoss(t) ? 0.2 : 1.0;
            t.setDeltaMovement(t.getDeltaMovement().add(out.normalize().scale(power * k)).add(0, up * k, 0));
            t.hurtMarked = true;
        };
    }

    static boolean hostile(LivingEntity owner, LivingEntity t) {
        if (t == owner || !t.isAlive() || t.isSpectator() || t.isAlliedTo(owner) || t instanceof Player) return false;
        if (t instanceof Enemy) return true;
        return t instanceof Mob m && m.getTarget() == owner;
    }

    /** Anything a strike may hurt: every living thing that is not the owner, an ally or a spectator (players too, they are fair targets of a caster's zone). */
    static boolean hurtable(LivingEntity owner, LivingEntity t) {
        return t != owner && t.isAlive() && !t.isSpectator() && !t.isAlliedTo(owner) && !(t instanceof Player p && p.isCreative());
    }

    static LivingEntity nearest(MagicPropEntity e, LivingEntity owner, ServerLevel sl, double range) {
        List<LivingEntity> list = sl.getEntitiesOfClass(LivingEntity.class, new AABB(e.position(), e.position()).inflate(range, 4, range), t -> hostile(owner, t));
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity t : list) {
            double d = t.distanceToSqr(e);
            if (d < bd) { bd = d; best = t; }
        }
        return best;
    }

    /** Balance-law damage from a prop as its owner's magic. */
    public static void hit(MagicPropEntity e, ServerLevel sl, LivingEntity owner, LivingEntity t, float raw, ResourceKey<DamageType> type) {
        DamageSource src = sl.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(type)
                .<DamageSource>map(h -> new DamageSource(h, e, owner))
                .orElseGet(() -> owner.damageSources().indirectMagic(e, owner));
        t.hurt(src, BalanceLaw.damage(owner, t, raw, 0.5));
    }

    static void face(MagicPropEntity e, Vec3 to, float maxTurn) {
        double dx = to.x - e.getX(), dz = to.z - e.getZ();
        if (dx * dx + dz * dz < 1e-4) return;
        float want = (float) (Mth.atan2(dz, dx) * 57.29577951308232) - 90f, cur = e.getYRot();
        e.setYRot(cur + Mth.clamp(Mth.wrapDegrees(want - cur), -maxTurn, maxTurn));
    }

    static void walk(MagicPropEntity e, Vec3 to, double speed) {
        Vec3 d = to.subtract(e.position()).multiply(1, 0, 1);
        double len = d.length();
        Vec3 dm = e.getDeltaMovement();
        double vy = e.onGround() ? -0.02 : Math.max(-0.6, dm.y - 0.08);
        Vec3 h = len < 1e-4 ? Vec3.ZERO : d.scale(Math.min(speed, len) / len);
        if (e.horizontalCollision && e.onGround()) vy = 0.42;                          // hop over a step
        e.setDeltaMovement(h.x, vy, h.z);
        e.move(MoverType.SELF, e.getDeltaMovement());
        if (len > 1e-4) face(e, to, 28f);
    }

    /** A summon lasts longer for a mighty caster (up to +50 percent). */
    public static int lifeFor(LivingEntity owner, int base) { return (int) (base * Math.min(1.5, 1.0 + (BalanceLaw.epScale(owner) - 1.0) * 0.05)); }

    // ================================================================== Companion
    public record CompanionCfg(String name, float w, float h, double speed, double reach, int windup, int clip, int cooldown, float damage, double radius,
                               ResourceKey<DamageType> type, Hit hit, int crumble, int color, VfxShape fx) {}

    public static final class Companion implements MagicProps.Behavior {
        private final CompanionCfg c;
        private final String key;

        public Companion(CompanionCfg c) { this.c = c; this.key = "prop-" + c.name(); }

        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            float s = e.scale();
            e.setSize(c.w() * s, c.h() * s);
            e.setParam(pack(variant(e.param()), IDLE, sl.getGameTime()));
            VfxSpawn.send(sl, c.fx(), e.position(), e.position().add(0, 1, 0), c.color(), 22, 0.9f * s);
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) { SafeCast.tick(key, () -> run(e, sl)); }

        private void run(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            if (owner == null) { e.expire(); return; }
            long now = sl.getGameTime();
            int st = state(e.param());
            if (st != IDLE && since(e.param(), now) > c.clip()) { setState(e, IDLE); st = IDLE; }
            if (e.maxLife() - e.life() < c.crumble()) { e.setDeltaMovement(Vec3.ZERO); return; }          // crumbling: it stops
            if ((e.life() + e.seed()) % 8 == 0 || !(e.target() instanceof LivingEntity tl && tl.isAlive() && hostile(owner, tl))) e.setTarget(nearest(e, owner, sl, 12));
            LivingEntity target = e.target() instanceof LivingEntity l && l.isAlive() ? l : null;
            float s = e.scale();
            if (target != null) {
                double d = Math.sqrt(target.distanceToSqr(e));
                if (d > c.reach() * s + target.getBbWidth() * 0.5) { walk(e, target.position(), c.speed()); return; }
                walk(e, e.position(), 0);
                face(e, target.position(), 40f);
                if (st == IDLE && since(e.param(), now) > c.cooldown()) {
                    setState(e, ACT);
                    SpellRuntime.later(sl, c.windup(), () -> strike(e, sl, owner));
                }
                return;
            }
            double d = Math.sqrt(owner.distanceToSqr(e));
            if (d > 28) { e.setPos(owner.getX() + 1.5, owner.getY(), owner.getZ() + 1.5); e.setDeltaMovement(Vec3.ZERO); return; }
            if (d > 3.5) walk(e, owner.position(), Math.min(c.speed() * 1.4, c.speed() * 0.6 + (d - 3.5) * 0.02));
            else { walk(e, e.position(), 0); face(e, owner.position().add(owner.getViewVector(1f).scale(4)), 20f); }
        }

        private void strike(MagicPropEntity e, ServerLevel sl, LivingEntity owner) {
            if (e.isRemoved()) return;
            float s = e.scale();
            Vec3 front = e.position().add(Vec3.directionFromRotation(0f, e.getYRot()).scale(c.reach() * 0.6 * s));
            double r = c.radius() * s;
            for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, new AABB(front, front).inflate(r, 1.6, r), x -> hurtable(owner, x) && !(x instanceof Player))) {
                hit(e, sl, owner, t, c.damage() * s, c.type());
                c.hit().apply(t, owner, e, sl);
            }
            VfxSpawn.send(sl, c.fx(), front.add(0, 0.2, 0), front.add(0, 1.2, 0), c.color(), 16, 0.7f * s);
        }

        @Override
        public void end(MagicPropEntity e, ServerLevel sl) {
            float s = e.scale();
            VfxSpawn.send(sl, c.fx(), e.position().add(0, 0.3, 0), e.position().add(0, 1.5, 0), c.color(), 26, 1.1f * s);
        }
    }

    // ================================================================== Guard
    /**
     * @param follow  0 = stays where it was put; > 0 = hovers this many blocks in front of the owner; < 0 = stays on the owner (a cocoon)
     * @param health  hit points of the slab (0 = it cannot be hit)
     * @param block   blocks within this radius turn their projectiles away (0 = none)
     * @param reflect true: a stopped projectile flies back owned by the owner; false: it is destroyed
     * @param mend    hit points the owner regains every second (scaled by EP), and a burst of the same times 3 when the prop ends
     */
    public record GuardCfg(String name, float w, float h, double follow, boolean solid, float health, double block, boolean reflect, float mend, int color, VfxShape fx) {}

    public static final class Guard implements MagicProps.Behavior {
        private final GuardCfg c;
        private final String key;

        public Guard(GuardCfg c) { this.c = c; this.key = "prop-" + c.name(); }

        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            float s = e.scale();
            e.setSize(c.w() * s, c.h() * s);
            e.setSolid(c.solid());
            if (c.health() > 0) e.setHittable(c.health() * (e.owner() == null ? 1f : (float) Math.min(4.0, BalanceLaw.epScale(e.owner()))));
            e.setParam(pack(variant(e.param()), IDLE, sl.getGameTime()));
            VfxSpawn.send(sl, c.fx(), e.position(), e.position().add(0, 1, 0), c.color(), 22, 0.9f * s);
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) { SafeCast.tick(key, () -> run(e, sl)); }

        private void run(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            if (owner == null) { e.expire(); return; }
            long now = sl.getGameTime();
            if (state(e.param()) != IDLE && since(e.param(), now) > 14) setState(e, IDLE);
            if (c.follow() != 0) {
                Vec3 look = owner.getViewVector(1f).multiply(1, 0, 1);
                look = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
                Vec3 at = c.follow() > 0 ? owner.position().add(look.scale(c.follow())) : owner.position();
                e.setPos(at.x, owner.getY(), at.z);
                e.setYRot(c.follow() > 0 ? owner.getYRot() : e.getYRot() + 1.5f);
            }
            if (c.block() > 0 && e.life() % 2 == 0) {
                float s = e.scale();
                int n = 0;
                Vec3 centre = e.position().add(0, c.h() * s * 0.5, 0);
                for (Projectile p : sl.getEntitiesOfClass(Projectile.class, new AABB(centre, centre).inflate(c.block() * s, c.h() * s * 0.6, c.block() * s))) {
                    if (n++ >= 6) break;
                    if (p.getOwner() == owner || p.getOwner() == null && p.getDeltaMovement().lengthSqr() < 0.01) continue;
                    if (p.getOwner() instanceof LivingEntity o && o.isAlliedTo(owner)) continue;
                    if (c.reflect()) {
                        p.setDeltaMovement(p.getDeltaMovement().scale(-0.7));
                        p.setOwner(owner);
                        p.hurtMarked = true;
                    } else p.discard();
                    e.setParam(pack(variant(e.param()), ACT, now));
                    VfxSpawn.send(sl, c.fx(), p.position(), p.position().add(0, 0.5, 0), c.color(), 14, 0.5f);
                }
            }
            if (c.mend() > 0 && e.life() % 20 == 10 && owner.getHealth() < owner.getMaxHealth()) BalanceLaw.heal(owner, c.mend() * (float) Math.min(3.0, BalanceLaw.epScale(owner)));
        }

        @Override
        public void end(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            if (c.mend() > 0 && owner != null) BalanceLaw.heal(owner, c.mend() * 3f * (float) Math.min(3.0, BalanceLaw.epScale(owner)));
            VfxSpawn.send(sl, c.fx(), e.position().add(0, 0.3, 0), e.position().add(0, 1.5, 0), c.color(), 26, 1.2f * e.scale());
        }
    }

    // ================================================================== Strike
    /**
     * @param delay    ticks until the strike
     * @param follow   true: until it strikes the prop stays on its target
     * @param hold     ticks the target is held after the strike (0 = none; shortened for bosses, behind the control gate of the spell)
     * @param linger   0 = one strike; > 0 = it hits again every this many ticks until its life ends, for {@code lingerDamage}
     */
    public record StrikeCfg(String name, float w, float h, int delay, double radius, float damage, ResourceKey<DamageType> type, boolean follow, int hold,
                            int linger, float lingerDamage, Hit hit, int color, VfxShape fx) {}

    public static final class Strike implements MagicProps.Behavior {
        private final StrikeCfg c;
        private final String key;

        public Strike(StrikeCfg c) { this.c = c; this.key = "prop-" + c.name(); }

        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            float s = e.scale();
            e.setSize(c.w() * s, c.h() * s);
            e.setParam(pack(variant(e.param()), IDLE, sl.getGameTime()));
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) { SafeCast.tick(key, () -> run(e, sl)); }

        private void run(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            if (owner == null) { e.expire(); return; }
            int age = e.life();
            LivingEntity target = e.target() instanceof LivingEntity l && l.isAlive() ? l : null;
            if (age < c.delay() && c.follow() && target != null) e.setPos(target.getX(), target.getY(), target.getZ());
            if (age == c.delay()) {
                e.setParam(pack(variant(e.param()), ACT, sl.getGameTime()));
                strike(e, sl, owner, c.damage(), true);
            } else if (age > c.delay() && c.linger() > 0 && (age - c.delay()) % c.linger() == 0) strike(e, sl, owner, c.lingerDamage(), false);
            int hold = c.hold() > 0 && target != null ? BalanceLaw.controlTicks(target, c.hold()) : 0;
            if (hold > 0 && age > c.delay() && age <= c.delay() + hold) {
                Vec3 v = target.getDeltaMovement();
                target.setDeltaMovement(0, Math.min(0, v.y), 0);
                target.hurtMarked = true;
                if (age % 10 == 0) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 14, 5));
                if (e.getY() - target.getY() > 0.01 || target.getY() - e.getY() > 3) e.setPos(target.getX(), target.getY(), target.getZ());
            }
        }

        private void strike(MagicPropEntity e, ServerLevel sl, LivingEntity owner, float raw, boolean first) {
            float s = e.scale();
            double r = c.radius() * s;
            Vec3 at = e.position();
            for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r, 2.2 * s, r), x -> hurtable(owner, x) && x.distanceToSqr(at.x, x.getY(), at.z) <= r * r)) {
                hit(e, sl, owner, t, raw * s, c.type());
                if (first) c.hit().apply(t, owner, e, sl);
            }
            if (first) VfxSpawn.send(sl, c.fx(), at.add(0, 0.1, 0), at.add(0, 1, 0), c.color(), 18, (float) Math.max(0.6, r * 0.7));
        }

        @Override
        public void end(MagicPropEntity e, ServerLevel sl) {
            VfxSpawn.send(sl, c.fx(), e.position().add(0, 0.2, 0), e.position().add(0, 1.2, 0), c.color(), 18, 0.6f * e.scale());
        }
    }

    // ================================================================== Missile
    /**
     * @param speed   blocks per tick (used when the spell has not set a velocity)
     * @param homing  0 = straight, otherwise the share of its velocity turned toward the target each tick (0.05 .. 0.3)
     * @param aoe     0 = hits one thing; > 0 = bursts on contact in this radius
     * @param stick   true: after it hits a block it stays embedded until its life ends
     */
    public record MissileCfg(String name, float w, float h, double hitRadius, float damage, double aoe, ResourceKey<DamageType> type, double homing, boolean stick,
                             Hit hit, int color, VfxShape fx) {}

    public static final class Missile implements MagicProps.Behavior {
        private final MissileCfg c;
        private final String key;

        public Missile(MissileCfg c) { this.c = c; this.key = "prop-" + c.name(); }

        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            float s = e.scale();
            e.setSize(c.w() * s, c.h() * s);
            e.setParam(pack(variant(e.param()), IDLE, sl.getGameTime()));
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) { SafeCast.tick(key, () -> run(e, sl)); }

        private void run(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            if (owner == null) { e.expire(); return; }
            if (state(e.param()) == DONE) { e.setDeltaMovement(Vec3.ZERO); return; }                 // embedded
            Vec3 v = e.getDeltaMovement();
            if (c.homing() > 0) {
                LivingEntity t = e.target() instanceof LivingEntity l && l.isAlive() ? l : null;
                if (t != null) {
                    Vec3 want = t.getBoundingBox().getCenter().subtract(e.position()).normalize().scale(Math.max(0.2, v.length()));
                    v = v.scale(1 - c.homing()).add(want.scale(c.homing()));
                }
            }
            if (v.lengthSqr() < 1e-4) { burst(e, sl, owner); return; }
            Vec3 from = e.position(), to = from.add(v);
            HitResult wall = sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e));
            Vec3 stop = wall.getType() != HitResult.Type.MISS ? wall.getLocation() : to;
            double r = c.hitRadius() * e.scale();
            for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, new AABB(from, stop).inflate(r), x -> hurtable(owner, x))) {
                e.setPos(t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ());
                hitAt(e, sl, owner, t);
                return;
            }
            e.setDeltaMovement(v);
            e.setPos(stop.x, stop.y, stop.z);
            float yaw = (float) (Mth.atan2(v.x, v.z) * 57.29577951308232), pitch = (float) -(Mth.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z)) * 57.29577951308232);
            e.setYRot(yaw);
            e.setXRot(pitch);
            if (wall.getType() != HitResult.Type.MISS) {
                if (c.aoe() > 0) burst(e, sl, owner);
                else if (c.stick()) { setState(e, DONE); VfxSpawn.send(sl, c.fx(), stop, stop.add(0, 0.5, 0), c.color(), 12, 0.4f); }
                else e.expire();
            }
        }

        private void hitAt(MagicPropEntity e, ServerLevel sl, LivingEntity owner, LivingEntity first) {
            if (c.aoe() > 0) { burst(e, sl, owner); return; }
            hit(e, sl, owner, first, c.damage() * e.scale(), c.type());
            c.hit().apply(first, owner, e, sl);
            VfxSpawn.send(sl, c.fx(), e.position(), e.position().add(0, 0.5, 0), c.color(), 12, 0.5f);
            e.expire();
        }

        private void burst(MagicPropEntity e, ServerLevel sl, LivingEntity owner) {
            double r = c.aoe() * e.scale();
            Vec3 at = e.position();
            if (c.aoe() > 0) {
                for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r), x -> hurtable(owner, x) && x.distanceToSqr(at) <= r * r)) {
                    hit(e, sl, owner, t, c.damage() * e.scale(), c.type());
                    c.hit().apply(t, owner, e, sl);
                }
                VfxSpawn.send(sl, c.fx(), at, at.add(0, 1, 0), c.color(), 20, (float) Math.max(0.6, r * 0.6));
            }
            e.expire();
        }
    }
}
