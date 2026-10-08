package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.balance.BalanceLaw;
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

import java.util.List;

/**
 * 0.53 Bronze Magic: the server behaviours of this magic's props and the damage hook of its spells.
 * <ul>
 *   <li>BRONZE_1, the guardian statue: follows its owner, walks up to the nearest hostile within 12 blocks and cuts it with its short sword,
 *       and blocks projectiles that fly at it or its owner with the shield (they are turned back, owned by the owner).</li>
 *   <li>BRONZE_2, a poison lizard: scuttles to the nearest hostile, bites (poison) and spits venom from a distance.</li>
 * </ul>
 * The animation state is synced in {@code param}: bits 0-1 = state (0 idle or walking, 1 strike / bite, 2 block / spit), bits 2-17 = the game
 * time (mod 65536) at which the state began, so the painter plays the one-shot clips from their first frame ({@link #state}, {@link #since}).
 */
public final class BronzeProps {
    private BronzeProps() {}

    public static final int IDLE = 0, ACT = 1, ACT2 = 2;
    private static final int COLOR = 0xFFD09A4A;

    /** Called once by PropRegistry. */
    public static void init() {
        com.newuniverse.nusmp.book.BronzeArts.hooks();
        MagicProps.register(PropKind.BRONZE_1, new Behavior(true));
        MagicProps.register(PropKind.BRONZE_2, new Behavior(false));
    }

    // ------------------------------------------------------------------ the synced state word
    public static int state(int param) { return param & 3; }

    /** Ticks since the state began (the clock is the game time mod 65536). */
    public static int since(int param, long gameTime) { return (int) ((gameTime - ((param >> 2) & 0xFFFF)) & 0xFFFF); }

    private static void setState(MagicPropEntity e, int st) {
        long now = e.level().getGameTime();
        e.setParam(st | (int) ((now & 0xFFFF) << 2));
    }

    // ------------------------------------------------------------------ helpers
    private static boolean hostile(LivingEntity owner, MagicPropEntity me, LivingEntity t) {
        if (t == owner || !t.isAlive() || t.isSpectator() || t.isAlliedTo(owner) || t instanceof Player) return false;
        if (t instanceof Enemy) return true;
        return t instanceof Mob m && (m.getTarget() == owner);
    }

    private static LivingEntity nearest(MagicPropEntity e, LivingEntity owner, ServerLevel sl, double range) {
        List<LivingEntity> list = sl.getEntitiesOfClass(LivingEntity.class, new AABB(e.position(), e.position()).inflate(range, 4, range),
                t -> hostile(owner, e, t));
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity t : list) {
            double d = t.distanceToSqr(e);
            if (d < bd) { bd = d; best = t; }
        }
        return best;
    }

    /** Balance-law damage from the prop (cast as earth magic of the owner). */
    private static void hit(MagicPropEntity e, ServerLevel sl, LivingEntity owner, LivingEntity t, float raw) {
        DamageSource src = sl.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(TensuraDamageTypes.EARTH_ELEMENTAL)
                .<DamageSource>map(h -> new DamageSource(h, e, owner))
                .orElseGet(() -> owner.damageSources().indirectMagic(e, owner));
        t.hurt(src, BalanceLaw.damage(owner, t, raw, 0.5));
    }

    private static void face(MagicPropEntity e, Vec3 to) {
        double dx = to.x - e.getX(), dz = to.z - e.getZ();
        if (dx * dx + dz * dz < 1e-4) return;
        float want = (float) (Mth.atan2(dz, dx) * 57.29577951308232) - 90f;
        float cur = e.getYRot();
        float next = cur + Mth.clamp(Mth.wrapDegrees(want - cur), -28f, 28f);
        e.setYRot(next);
    }

    /** Walks toward a point on the ground with simple gravity; returns false when it did not move. */
    private static void walk(MagicPropEntity e, Vec3 to, double speed) {
        Vec3 d = to.subtract(e.position()).multiply(1, 0, 1);
        double len = d.length();
        Vec3 dm = e.getDeltaMovement();
        double vy = e.onGround() ? -0.02 : Math.max(-0.6, dm.y - 0.08);
        Vec3 h = len < 1e-4 ? Vec3.ZERO : d.scale(Math.min(speed, len) / len);
        if (e.horizontalCollision && e.onGround()) vy = 0.42;                                     // hop over a step
        e.setDeltaMovement(h.x, vy, h.z);
        e.move(MoverType.SELF, e.getDeltaMovement());
        if (len > 1e-4) face(e, to);
    }

    private static double life(LivingEntity owner) { return Math.min(1.5, 1.0 + (BalanceLaw.epScale(owner) - 1.0) * 0.05); }

    /** Page helper: life in ticks of a summon of 'base' ticks, longer with the owner's EP (at most +50 %). */
    public static int lifeFor(LivingEntity owner, int base) { return (int) (base * life(owner)); }

    // ------------------------------------------------------------------ the behaviours
    private static final class Behavior implements MagicProps.Behavior {
        private final boolean statue;

        Behavior(boolean statue) { this.statue = statue; }

        @Override
        public void init(MagicPropEntity e, ServerLevel sl) {
            float s = e.scale();
            if (statue) e.setSize(0.9f * s, 2.0f * s); else e.setSize(0.8f * s, 0.5f * s);
            e.setParam(0);
            VfxSpawn.send(sl, VfxShape.BRONZE_FX3, e.position(), e.position().add(0, 1, 0), COLOR, 22, statue ? 1.1f * s : 0.6f * s);
        }

        @Override
        public void tick(MagicPropEntity e, ServerLevel sl) {
            LivingEntity owner = e.owner();
            if (owner == null) { e.expire(); return; }
            long now = sl.getGameTime();
            int st = state(e.param());
            if (st != IDLE && since(e.param(), now) > (statue ? (st == ACT ? 20 : 16) : (st == ACT ? 12 : 18))) { e.setParam(0); st = IDLE; }
            if (statue && e.maxLife() - e.life() < 26) {                                               // crumbling: it stops
                e.setDeltaMovement(Vec3.ZERO);
                return;
            }
            if ((e.life() + e.seed()) % 8 == 0 || !(e.target() instanceof LivingEntity tl && tl.isAlive() && hostile(owner, e, tl)))
                e.setTarget(nearest(e, owner, sl, 12));
            LivingEntity target = e.target() instanceof LivingEntity l && l.isAlive() ? l : null;
            if (statue) tickStatue(e, sl, owner, target, now, st); else tickLizard(e, sl, owner, target, now, st);
        }

        private void tickStatue(MagicPropEntity e, ServerLevel sl, LivingEntity owner, LivingEntity target, long now, int st) {
            float s = e.scale();
            // 1. the shield: turn back projectiles flying at the statue or the owner
            if (e.life() % 2 == 0) {
                AABB box = new AABB(e.position(), e.position()).inflate(2.6 * s, 2.2 * s, 2.6 * s);
                int n = 0;
                for (Projectile p : sl.getEntitiesOfClass(Projectile.class, box)) {
                    if (n++ >= 6) break;
                    if (p.getOwner() == owner || p.getOwner() == null && p.getDeltaMovement().lengthSqr() < 0.01) continue;
                    if (p.getOwner() instanceof LivingEntity o && o.isAlliedTo(owner)) continue;
                    Vec3 v = p.getDeltaMovement();
                    p.setDeltaMovement(v.scale(-0.7));
                    p.setOwner(owner);
                    p.hurtMarked = true;
                    setState(e, ACT2);
                    st = ACT2;
                    face(e, p.position());
                    VfxSpawn.send(sl, VfxShape.BRONZE_FX3, p.position(), p.position().add(0, 0.5, 0), COLOR, 14, 0.5f);
                }
            }
            // 2. the sword
            if (target != null) {
                double d = Math.sqrt(target.distanceToSqr(e));
                if (d > 2.0 * s + target.getBbWidth() * 0.5) { walk(e, target.position(), 0.22); }
                else {
                    walk(e, e.position(), 0);
                    face(e, target.position());
                    if (st == IDLE && (e.life() + e.seed()) % 22 == 0 || st == IDLE && since(e.param(), now) > 30) {
                        setState(e, ACT);
                        final LivingEntity tt = target;
                        com.newuniverse.nusmp.book.SpellRuntime.later(sl, 11, () -> {
                            if (e.isRemoved() || !tt.isAlive() || tt.distanceToSqr(e) > 16 * s * s) return;
                            hit(e, sl, owner, tt, 6f * s);
                            Vec3 out = tt.position().subtract(e.position()).multiply(1, 0, 1);
                            if (out.lengthSqr() > 1e-4) {
                                double k = BalanceLaw.isBoss(tt) ? 0.1 : 0.35;
                                tt.setDeltaMovement(tt.getDeltaMovement().add(out.normalize().scale(k)).add(0, 0.12, 0));
                                tt.hurtMarked = true;
                            }
                            VfxSpawn.send(sl, VfxShape.BRONZE_FX3, tt.position().add(0, 1, 0), tt.position().add(0, 2, 0), COLOR, 14, 0.5f);
                        });
                    }
                }
                return;
            }
            // 3. follow the owner, a step behind and to the side
            double d = Math.sqrt(owner.distanceToSqr(e));
            if (d > 28) { e.setPos(owner.getX() + 1.5, owner.getY(), owner.getZ() + 1.5); e.setDeltaMovement(Vec3.ZERO); return; }
            if (d > 3.5) walk(e, owner.position(), Math.min(0.3, 0.16 + (d - 3.5) * 0.02));
            else { walk(e, e.position(), 0); face(e, owner.position().add(owner.getViewVector(1f).scale(4))); }
        }

        private void tickLizard(MagicPropEntity e, ServerLevel sl, LivingEntity owner, LivingEntity target, long now, int st) {
            float s = e.scale();
            if (target != null) {
                double d = Math.sqrt(target.distanceToSqr(e));
                int since = since(e.param(), now);
                if (d <= 1.5 * s + target.getBbWidth() * 0.5) {
                    walk(e, e.position(), 0);
                    face(e, target.position());
                    if (st == IDLE && since > 22) {
                        setState(e, ACT);
                        final LivingEntity tt = target;
                        com.newuniverse.nusmp.book.SpellRuntime.later(sl, 5, () -> {
                            if (e.isRemoved() || !tt.isAlive() || tt.distanceToSqr(e) > 9 * s * s + 4) return;
                            hit(e, sl, owner, tt, 3.5f * s);
                            poison(tt, 100, 1);
                        });
                    }
                    return;
                }
                if (d > 3 && d < 9 && st == IDLE && since > 60 && target.hasLineOfSight(e)) {         // spit
                    setState(e, ACT2);
                    face(e, target.position());
                    final LivingEntity tt = target;
                    com.newuniverse.nusmp.book.SpellRuntime.later(sl, 10, () -> {
                        if (e.isRemoved() || !tt.isAlive()) return;
                        Vec3 from = e.position().add(0, 0.5 * s, 0);
                        VfxSpawn.send(sl, VfxShape.BRONZE_FX1, from, tt.getBoundingBox().getCenter(), COLOR, 10, 0.5f);
                        hit(e, sl, owner, tt, 2.5f * s);
                        poison(tt, 80, 0);
                    });
                    return;
                }
                if (st == ACT2) { walk(e, e.position(), 0); return; }
                walk(e, target.position(), 0.34);
                return;
            }
            double d = Math.sqrt(owner.distanceToSqr(e));
            if (d > 28) { e.setPos(owner.getX(), owner.getY(), owner.getZ()); e.setDeltaMovement(Vec3.ZERO); return; }
            if (d > 3.0) walk(e, owner.position().add(Math.cos(e.seed()) * 1.6, 0, Math.sin(e.seed()) * 1.6), 0.28);
            else walk(e, e.position(), 0);
        }

        @Override
        public void end(MagicPropEntity e, ServerLevel sl) {
            float s = e.scale();
            VfxSpawn.send(sl, VfxShape.BRONZE_FX3, e.position().add(0, 0.3, 0), e.position().add(0, 1.5, 0), COLOR, 26, statue ? 1.3f * s : 0.7f * s);
        }
    }

    private static void poison(LivingEntity t, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(MobEffects.POISON, BalanceLaw.isBoss(t) ? ticks / 2 : ticks, amp));
    }
}
