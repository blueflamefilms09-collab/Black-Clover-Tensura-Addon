package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.EarthFxStyle;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.93.0 The server behaviours of {@link PropKind#EARTH_CONSTRUCT}: nine constructs of the earth-family magics, told apart by {@link EarthConstruct#type}.
 * <ul>
 *   <li>GOLEM (Earth, Clay Guardian): follows the owner and slams the nearest hostile with both fists, an area hit that lifts and slows.</li>
 *   <li>TITAN (Rock, Boulder Titan): slower and heavier; a stomp with a wide area, and every few seconds a roar that throws back everything close and weakens it.</li>
 *   <li>SENTINEL (Stone): stays at the owner's side, turns projectiles back with its tower shield and smashes one foe at a time with the block hammer.</li>
 *   <li>SPHINX (Sandstone): fast; pounces on the nearest hostile from up to seven blocks and claws it (sand in the eyes).</li>
 *   <li>CAGE (Sandstone, Variable Hardness Prison): a block of sandstone that closes round the target it was spawned for, holds it and presses the breath out of it
 *       (damage ticks, rooted), then crumbles; hard control, so the caster's control gate was spent and bosses are held for a shortened time.</li>
 *   <li>TOTEM (Red Ochre): a solid painted totem that spits a pigment bolt at the nearest hostile every 28 ticks (weakness and glowing: it is marked).</li>
 *   <li>SPIRE (Mineral): a crystal growth out of the ground that pulses an area of shards every 24 ticks and slows what it cuts.</li>
 *   <li>BOULDER (Rock Bolt) and LANCE (Mineral Lance): thrown bodies. The spell sets their velocity; the boulder bursts on the first foe or block, the lance
 *       pierces every foe on its line once.</li>
 * </ul>
 * Damage goes through the balance law as the owner's earth-element damage; hits on bosses are capped by it. Constructs never target players on their own (as the
 * bronze statue): only the cage, the boulder and the lance, which the caster aimed, may hit anyone not allied with the owner.
 */
public final class EarthConstructProps {
    private EarthConstructProps() {}

    /** Called once by PropRegistry. */
    public static void init() { MagicProps.register(PropKind.EARTH_CONSTRUCT, new Behavior()); }

    /** Spawns a construct of a type; the caller has set nothing else. */
    public static MagicPropEntity spawn(ServerLevel sl, int type, Vec3 at, float yaw, float scale, int life, LivingEntity owner) {
        return MagicProps.spawn(sl, PropKind.EARTH_CONSTRUCT, at, yaw, scale, life, EarthConstruct.pack(type, EarthConstruct.IDLE, sl.getGameTime()), owner);
    }

    /** Ticks of a summon of 'base' ticks, longer for a mighty owner (at most +50 percent). */
    public static int lifeFor(LivingEntity owner, int base) { return (int) (base * Math.min(1.5, 1.0 + (BalanceLaw.epScale(owner) - 1.0) * 0.05)); }

    /** The size factor of a construct for an owner of this EP (0.9 up to 1.3). */
    public static float sizeFor(LivingEntity owner) { return 0.9f + 0.05f * (float) Math.min(8.0, BalanceLaw.epScale(owner) - 1.0); }

    private static final Map<Integer, Set<Integer>> PIERCED = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------ helpers
    private static boolean hostile(LivingEntity owner, LivingEntity t) {
        if (t == owner || !t.isAlive() || t.isSpectator() || t.isAlliedTo(owner) || t instanceof Player) return false;
        if (t instanceof Enemy) return true;
        return t instanceof Mob m && m.getTarget() == owner;
    }

    private static LivingEntity nearest(MagicPropEntity e, LivingEntity owner, ServerLevel sl, double range) {
        List<LivingEntity> list = sl.getEntitiesOfClass(LivingEntity.class, new AABB(e.position(), e.position()).inflate(range, 4, range), t -> hostile(owner, t));
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity t : list) {
            double d = t.distanceToSqr(e);
            if (d < bd) { bd = d; best = t; }
        }
        return best;
    }

    private static void hit(MagicPropEntity e, ServerLevel sl, LivingEntity owner, LivingEntity t, float raw) {
        DamageSource src = sl.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(TensuraDamageTypes.EARTH_ELEMENTAL)
                .<DamageSource>map(h -> new DamageSource(h, e, owner)).orElseGet(() -> owner.damageSources().indirectMagic(e, owner));
        t.hurt(src, BalanceLaw.damage(owner, t, raw, 0.5));
    }

    private static void face(MagicPropEntity e, Vec3 to, float maxTurn) {
        double dx = to.x - e.getX(), dz = to.z - e.getZ();
        if (dx * dx + dz * dz < 1e-4) return;
        float want = (float) (Mth.atan2(dz, dx) * 57.29577951308232) - 90f, cur = e.getYRot();
        e.setYRot(cur + Mth.clamp(Mth.wrapDegrees(want - cur), -maxTurn, maxTurn));
    }

    private static void walk(MagicPropEntity e, Vec3 to, double speed) {
        Vec3 d = to.subtract(e.position()).multiply(1, 0, 1);
        double len = d.length();
        Vec3 dm = e.getDeltaMovement();
        double vy = e.onGround() ? -0.02 : Math.max(-0.6, dm.y - 0.08);
        Vec3 h = len < 1e-4 ? Vec3.ZERO : d.scale(Math.min(speed, len) / len);
        if (e.horizontalCollision && e.onGround()) vy = 0.42;
        e.setDeltaMovement(h.x, vy, h.z);
        e.move(MoverType.SELF, e.getDeltaMovement());
        if (len > 1e-4) face(e, to, 24f);
    }

    private static void state(MagicPropEntity e, int st) { e.setParam(EarthConstruct.withState(e.param(), st, e.level().getGameTime())); }

    private static void fx(ServerLevel sl, int style, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) {
        VfxSpawn.send(sl, shape, from, to, EarthFxStyle.color(style), ticks, power);
    }

    private static void knock(LivingEntity t, Vec3 from, double force, double lift) {
        Vec3 out = t.position().subtract(from).multiply(1, 0, 1);
        if (out.lengthSqr() < 1e-4) out = new Vec3(0, 0, 1);
        double k = BalanceLaw.isBoss(t) ? force * 0.25 : force;
        t.setDeltaMovement(t.getDeltaMovement().add(out.normalize().scale(k)).add(0, BalanceLaw.isBoss(t) ? lift * 0.3 : lift, 0));
        t.hurtMarked = true;
    }

    private static void slow(LivingEntity t, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.isBoss(t) ? ticks / 2 : ticks, amp));
    }

    // ------------------------------------------------------------------ the behaviour
    private static final class Behavior implements MagicProps.Behavior {
        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            float s = e.scale();
            int type = EarthConstruct.type(e.param());
            switch (type) {
                case EarthConstruct.GOLEM -> { e.setSize(1.2f * s, 2.8f * s); e.setHittable(40f * s); }
                case EarthConstruct.TITAN -> { e.setSize(1.7f * s, 3.4f * s); e.setHittable(60f * s); }
                case EarthConstruct.SENTINEL -> { e.setSize(1.3f * s, 2.9f * s); e.setHittable(50f * s); }
                case EarthConstruct.SPHINX -> { e.setSize(1.1f * s, 1.6f * s); e.setHittable(30f * s); }
                case EarthConstruct.CAGE -> { e.setSize(1.9f * s, 2.0f * s); }
                case EarthConstruct.TOTEM -> { e.setSize(1.0f * s, 3.0f * s); e.setSolid(true); e.setHittable(30f * s); }
                case EarthConstruct.SPIRE -> { e.setSize(1.0f * s, 3.0f * s); e.setSolid(true); e.setHittable(24f * s); }
                case EarthConstruct.BOULDER -> e.setSize(0.9f * s, 0.9f * s);
                case EarthConstruct.LANCE -> e.setSize(0.6f * s, 0.6f * s);
                default -> e.setSize(1f, 1f);
            }
            fx(sl, EarthConstruct.style(type), VfxShape.EARTHFAM_BURST, e.position(), e.position().add(0, 1, 0), 22, type >= EarthConstruct.BOULDER ? 0.5f * s : 1.0f * s);
        }

        @Override
        public boolean hurt(MagicPropEntity e, DamageSource source, float amount) {
            LivingEntity owner = e.owner();
            return !(owner != null && source.getEntity() instanceof LivingEntity l && (l == owner || l.isAlliedTo(owner)));
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            int type = EarthConstruct.type(e.param());
            if (owner == null || !owner.isAlive()) { e.expire(); return; }
            long now = sl.getGameTime();
            int st = EarthConstruct.state(e.param());
            if (st != EarthConstruct.IDLE && EarthConstruct.since(e.param(), now) > 24) { state(e, EarthConstruct.IDLE); st = EarthConstruct.IDLE; }
            switch (type) {
                case EarthConstruct.GOLEM, EarthConstruct.TITAN, EarthConstruct.SENTINEL, EarthConstruct.SPHINX -> tickMobile(e, sl, owner, type, now, st);
                case EarthConstruct.CAGE -> tickCage(e, sl, owner);
                case EarthConstruct.TOTEM -> tickTotem(e, sl, owner, now);
                case EarthConstruct.SPIRE -> tickSpire(e, sl, owner);
                case EarthConstruct.BOULDER -> tickShot(e, sl, owner, false);
                case EarthConstruct.LANCE -> tickShot(e, sl, owner, true);
                default -> { }
            }
        }

        @Override
        public void end(MagicPropEntity e, ServerLevel sl) {
            int type = EarthConstruct.type(e.param());
            float s = e.scale();
            PIERCED.remove(e.getId());
            fx(sl, EarthConstruct.style(type), VfxShape.EARTHFAM_BURST, e.position().add(0, 0.4, 0), e.position().add(0, 1.6, 0), 26, type >= EarthConstruct.BOULDER ? 0.9f * s : 1.3f * s);
        }

        // ---------------------------------------------------------------- golem, titan, sentinel, sphinx
        private void tickMobile(MagicPropEntity e, ServerLevel sl, LivingEntity owner, int type, long now, int st) {
            float s = e.scale();
            if (e.maxLife() - e.life() < 22) { e.setDeltaMovement(Vec3.ZERO); return; }                    // crumbling
            if (type == EarthConstruct.SENTINEL && e.life() % 2 == 0) shield(e, sl, owner, s, st);
            if ((e.life() + e.seed()) % 8 == 0 || !(e.target() instanceof LivingEntity tl && tl.isAlive() && hostile(owner, tl)))
                e.setTarget(nearest(e, owner, sl, type == EarthConstruct.SPHINX ? 14 : 12));
            LivingEntity target = e.target() instanceof LivingEntity l && l.isAlive() ? l : null;
            double reach = switch (type) {
                case EarthConstruct.TITAN -> 3.2 * s;
                case EarthConstruct.SENTINEL -> 2.6 * s;
                case EarthConstruct.SPHINX -> 2.0 * s;
                default -> 2.6 * s;
            };
            double speed = switch (type) {
                case EarthConstruct.TITAN -> 0.17;
                case EarthConstruct.SENTINEL -> 0.2;
                case EarthConstruct.SPHINX -> 0.34;
                default -> 0.22;
            };
            if (type == EarthConstruct.TITAN && st == EarthConstruct.IDLE && e.life() % 160 == 80) roar(e, sl, owner, s);
            if (target != null) {
                double d = Math.sqrt(target.distanceToSqr(e));
                if (type == EarthConstruct.SPHINX && d > reach + 1.5 && d < 7.5 && st == EarthConstruct.IDLE && (e.life() + e.seed()) % 40 == 0) {   // the pounce
                    face(e, target.position(), 90f);
                    Vec3 dir = target.position().subtract(e.position()).multiply(1, 0, 1).normalize();
                    e.setDeltaMovement(dir.x * 0.9, 0.5, dir.z * 0.9);
                    e.move(MoverType.SELF, e.getDeltaMovement());
                    state(e, EarthConstruct.ACT);
                    claw(e, sl, owner, target, s, 6);
                    return;
                }
                if (d > reach + target.getBbWidth() * 0.5) { walk(e, target.position(), speed); return; }
                walk(e, e.position(), 0);
                face(e, target.position(), 40f);
                int cd = switch (type) { case EarthConstruct.TITAN -> 46; case EarthConstruct.SPHINX -> 26; default -> 34; };
                if (st == EarthConstruct.IDLE && EarthConstruct.since(e.param(), now) > cd) {
                    state(e, EarthConstruct.ACT);
                    switch (type) {
                        case EarthConstruct.GOLEM -> slam(e, sl, owner, s, 12, 2.6, 9f, 0.5, 0.45);
                        case EarthConstruct.TITAN -> slam(e, sl, owner, s, 14, 3.6, 13f, 0.7, 0.6);
                        case EarthConstruct.SENTINEL -> smash(e, sl, owner, target, s);
                        default -> claw(e, sl, owner, target, s, 5);
                    }
                }
                return;
            }
            double d = Math.sqrt(owner.distanceToSqr(e));
            if (d > 28) { e.setPos(owner.getX() + 1.5, owner.getY(), owner.getZ() + 1.5); e.setDeltaMovement(Vec3.ZERO); return; }
            double stay = type == EarthConstruct.SENTINEL ? 2.4 : 3.6;
            if (d > stay) walk(e, owner.position(), Math.min(speed + 0.1, speed * 0.7 + (d - stay) * 0.02));
            else { walk(e, e.position(), 0); face(e, owner.position().add(owner.getViewVector(1f).scale(4)), 20f); }
        }

        private void slam(MagicPropEntity e, ServerLevel sl, LivingEntity owner, float s, int delay, double radius, float raw, double lift, double push) {
            SpellRuntime.later(sl, delay, () -> {
                if (e.isRemoved()) return;
                Vec3 front = e.position().add(Vec3.directionFromRotation(0, e.getYRot()).scale(1.4 * s));
                int n = 0;
                for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, new AABB(front, front).inflate(radius * s), x -> hostile(owner, x))) {
                    if (n++ >= 8) break;
                    hit(e, sl, owner, t, raw * s);
                    knock(t, front, push, lift);
                    slow(t, 40, 1);
                }
                fx(sl, EarthConstruct.style(EarthConstruct.type(e.param())), VfxShape.EARTHFAM_BURST, front, front.add(0, 1, 0), 24, (float) radius * 0.5f * s);
            });
        }

        private void roar(MagicPropEntity e, ServerLevel sl, LivingEntity owner, float s) {
            state(e, EarthConstruct.ACT2);
            boolean control = BalanceLaw.beginControl(owner);
            fx(sl, EarthFxStyle.ROCK, VfxShape.EARTHFAM_ZONE, e.position(), e.position().add(0, 1, 0), 30, 5.5f * s);
            int n = 0;
            for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, new AABB(e.position(), e.position()).inflate(6 * s, 3, 6 * s), x -> hostile(owner, x))) {
                if (n++ >= 10) break;
                knock(t, e.position(), 0.8, 0.25);
                if (control) t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, BalanceLaw.controlTicks(t, 80), 1));
            }
        }

        private void smash(MagicPropEntity e, ServerLevel sl, LivingEntity owner, LivingEntity target, float s) {
            SpellRuntime.later(sl, 11, () -> {
                if (e.isRemoved() || !target.isAlive() || target.distanceToSqr(e) > 20 * s * s) return;
                hit(e, sl, owner, target, 11f * s);
                knock(target, e.position(), 0.5, 0.2);
                fx(sl, EarthFxStyle.STONE, VfxShape.EARTHFAM_BURST, target.position().add(0, 0.5, 0), target.position().add(0, 1.5, 0), 20, 0.9f * s);
            });
        }

        private void claw(MagicPropEntity e, ServerLevel sl, LivingEntity owner, LivingEntity target, float s, int delay) {
            SpellRuntime.later(sl, delay, () -> {
                if (e.isRemoved() || !target.isAlive() || target.distanceToSqr(e) > 16 * s * s + 9) return;
                hit(e, sl, owner, target, 8f * s);
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BalanceLaw.isBoss(target) ? 20 : 50, 0));
                fx(sl, EarthFxStyle.SANDSTONE, VfxShape.EARTHFAM_BURST, target.position().add(0, 0.8, 0), target.position().add(0, 1.6, 0), 18, 0.6f * s);
            });
        }

        /** The sentinel's tower shield: projectiles that fly at it or its owner are turned back (at most six a pass). */
        private void shield(MagicPropEntity e, ServerLevel sl, LivingEntity owner, float s, int st) {
            AABB box = new AABB(e.position(), e.position()).inflate(2.8 * s, 2.4 * s, 2.8 * s);
            int n = 0;
            for (Projectile p : sl.getEntitiesOfClass(Projectile.class, box)) {
                if (n++ >= 6) break;
                if (p.getOwner() == owner || p.getOwner() == null && p.getDeltaMovement().lengthSqr() < 0.01) continue;
                if (p.getOwner() instanceof LivingEntity o && o.isAlliedTo(owner)) continue;
                p.setDeltaMovement(p.getDeltaMovement().scale(-0.7));
                p.setOwner(owner);
                p.hurtMarked = true;
                state(e, EarthConstruct.ACT2);
                face(e, p.position(), 90f);
                fx(sl, EarthFxStyle.STONE, VfxShape.EARTHFAM_BURST, p.position(), p.position().add(0, 0.5, 0), 14, 0.5f);
            }
        }

        // ---------------------------------------------------------------- the prison cage
        private void tickCage(MagicPropEntity e, ServerLevel sl, LivingEntity owner) {
            float s = e.scale();
            int age = e.life(), left = e.maxLife() - age;
            if (!(e.target() instanceof LivingEntity t) || !t.isAlive()) { if (left > 18) e.expire(); return; }
            if (left <= 18 || age < 14) return;                                                        // closing, or crumbling: nothing is pressed
            Vec3 c = e.position();
            if (t.distanceToSqr(c.x, t.getY(), c.z) > 1.0) {                                           // the walls hold it inside
                t.teleportTo(c.x + (t.getX() - c.x) * 0.5, t.getY(), c.z + (t.getZ() - c.z) * 0.5);
            }
            t.setDeltaMovement(t.getDeltaMovement().multiply(0.2, 1.0, 0.2));
            t.hurtMarked = true;
            if ((age - 14) % 10 == 0) {
                hit(e, sl, owner, t, 3.2f * s);
                slow(t, 16, 8);
                fx(sl, EarthFxStyle.SANDSTONE, VfxShape.EARTHFAM_BURST, t.position().add(0, 0.2, 0), t.position().add(0, 1.4, 0), 12, 0.35f);
            }
        }

        // ---------------------------------------------------------------- the totem
        private void tickTotem(MagicPropEntity e, ServerLevel sl, LivingEntity owner, long now) {
            float s = e.scale();
            if (e.life() < 14 || e.maxLife() - e.life() < 16 || (e.life() + e.seed()) % 28 != 0) return;
            LivingEntity t = nearest(e, owner, sl, 14);
            if (t == null || !t.hasLineOfSight(e)) return;
            state(e, EarthConstruct.ACT);
            Vec3 from = e.position().add(0, 2.0 * s, 0), to = t.getBoundingBox().getCenter();
            e.setYRot((float) (Mth.atan2(to.z - e.getZ(), to.x - e.getX()) * 57.29577951308232) - 90f);
            fx(sl, EarthFxStyle.RED_OCHRE, VfxShape.EARTHFAM_CAST, from, to, 12, 0.7f * s);
            SpellRuntime.later(sl, 8, () -> {
                if (e.isRemoved() || !t.isAlive()) return;
                hit(e, sl, owner, t, 6f * s);
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, BalanceLaw.isBoss(t) ? 30 : 70, 0));
                t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 70, 0));
                fx(sl, EarthFxStyle.RED_OCHRE, VfxShape.EARTHFAM_BURST, to, to.add(0, 0.5, 0), 16, 0.5f * s);
            });
        }

        // ---------------------------------------------------------------- the crystal spire
        private void tickSpire(MagicPropEntity e, ServerLevel sl, LivingEntity owner) {
            float s = e.scale();
            if (e.life() < 10 || e.maxLife() - e.life() < 12 || (e.life() + e.seed()) % 24 != 0) return;
            state(e, EarthConstruct.ACT);
            int n = 0;
            for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, new AABB(e.position(), e.position()).inflate(3.4 * s, 2.5, 3.4 * s), x -> hostile(owner, x))) {
                if (n++ >= 8) break;
                hit(e, sl, owner, t, 5f * s);
                slow(t, 30, 1);
            }
            fx(sl, EarthFxStyle.MINERAL, VfxShape.EARTHFAM_ZONE, e.position(), e.position().add(0, 1, 0), 16, 3.0f * s);
        }

        // ---------------------------------------------------------------- thrown bodies
        private void tickShot(MagicPropEntity e, ServerLevel sl, LivingEntity owner, boolean lance) {
            float s = e.scale();
            Vec3 v = e.getDeltaMovement();
            if (!lance) v = v.add(0, -0.035, 0);
            e.setDeltaMovement(v);
            e.move(MoverType.SELF, v);
            if (v.horizontalDistanceSqr() > 1e-4) e.setYRot((float) (Mth.atan2(v.z, v.x) * 57.29577951308232) - 90f);
            double r = (lance ? 0.9 : 1.1) * s;
            Set<Integer> done = lance ? PIERCED.computeIfAbsent(e.getId(), k -> new HashSet<>()) : null;
            for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, e.getBoundingBox().inflate(r), x -> x != owner && x.isAlive() && !x.isSpectator() && !x.isAlliedTo(owner))) {
                if (lance) {
                    if (!done.add(t.getId()) || done.size() > 12) continue;
                    hit(e, sl, owner, t, 9f * s);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                    fx(sl, EarthFxStyle.MINERAL, VfxShape.EARTHFAM_BURST, t.position().add(0, 1, 0), t.position().add(0, 1.6, 0), 14, 0.4f * s);
                } else { burst(e, sl, owner, s); return; }
            }
            if (e.horizontalCollision || e.verticalCollision || e.onGround() && !lance) { burst(e, sl, owner, s); return; }
        }

        private void burst(MagicPropEntity e, ServerLevel sl, LivingEntity owner, float s) {
            Vec3 at = e.position();
            boolean lance = EarthConstruct.type(e.param()) == EarthConstruct.LANCE;
            int n = 0;
            for (LivingEntity t : sl.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(2.6 * s), x -> x != owner && x.isAlive() && !x.isSpectator() && !x.isAlliedTo(owner))) {
                if (n++ >= 8) break;
                hit(e, sl, owner, t, (lance ? 5f : 11f) * s);
                knock(t, at, lance ? 0.2 : 0.55, lance ? 0.1 : 0.4);
            }
            e.expire();
        }
    }
}
