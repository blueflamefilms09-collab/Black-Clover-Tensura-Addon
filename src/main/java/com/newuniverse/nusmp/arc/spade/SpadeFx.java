package com.newuniverse.nusmp.arc.spade;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.zogratis.DevilStyle;
import com.newuniverse.nusmp.entity.zogratis.DanteMoves;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.function.IntConsumer;

/**
 * Why: the two Spade bosses do the same small things in the world (a ring of dust with doors cut out of it, a blow of gravity that goes through the
 * damage law and Tensura's own gravity damage types, a foe forced to the floor, a hint on the action bar, a guard around a timer step), and writing each
 * twice would let them drift apart. The Zogratis helpers of the same kind are package-private to {@code entity.zogratis}; this is the small set the Spade
 * bosses need, written against the public framework only. Everything here is server-thread glue over {@code ZogratisBoss}; the rules themselves are in
 * the pure classes next to it.
 */
final class SpadeFx {
    private SpadeFx() {}

    /** ARGB colours for the VFX payloads: Lucifero's gravity violet, Morris's modification orange. */
    static final int VIOLET = 0xFF000000 | SpadeRoster.LUCIFERO_ACCENT, ORANGE = 0xFF000000 | SpadeRoster.MORRIS_COLOR;

    // ---------------------------------------------------------------- guards
    /**
     * Zone steps and delayed tasks run inside the server tick without a net under them, so every one of the Spade bosses' goes through here: a step that
     * throws is logged once and that zone or task stops, it never takes the tick down.
     */
    static IntConsumer safe(String who, String what, IntConsumer step) {
        boolean[] dead = {false};
        return age -> {
            if (dead[0]) return;
            try {
                step.accept(age);
            } catch (Throwable t) {
                dead[0] = true;
                LogUtils.getLogger().warn("[nusmp] {}'s {} stopped: {}", who, what, t.toString());
            }
        };
    }

    /** {@link #safe(String, String, IntConsumer)} for a delayed task. */
    static Runnable safe(String who, String what, Runnable task) {
        IntConsumer guarded = safe(who, what, age -> task.run());
        return () -> guarded.accept(0);
    }

    // ---------------------------------------------------------------- vectors
    static Vec3 flat(Vec3 v) { return new Vec3(v.x, 0, v.z); }

    /** The unit vector of {@code v} flattened to the ground, or a fixed one when it has no horizontal part. */
    static Vec3 flatUnit(Vec3 v) {
        Vec3 f = flat(v);
        return f.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : f.normalize();
    }

    static Vec3 rotateY(Vec3 v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    /** The side vector (left of {@code unit} seen from above). */
    static Vec3 side(Vec3 unit) { return new Vec3(-unit.z, 0, unit.x); }

    /** Is the entity inside the wedge of {@code reach} blocks and half-angle {@code cosHalf} (a cosine) in front of {@code from}, looking along {@code facingUnit}? Closer than 1.2 blocks always counts. */
    static boolean inWedge(Vec3 from, Vec3 facingUnit, Entity e, double reach, double cosHalf) {
        Vec3 to = flat(e.position().subtract(from));
        double d = to.length();
        if (d > reach + e.getBbWidth() / 2) return false;
        return d <= 1.2 || to.normalize().dot(facingUnit) >= cosHalf;
    }

    // ---------------------------------------------------------------- dust
    static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(DevilStyle.red(rgb), DevilStyle.green(rgb), DevilStyle.blue(rgb)), size);
    }

    /** A flat ring of dust round {@code c} with {@code doors} (angles, may be null) left open: the wall of Morris's cage. */
    static void ringWithDoors(ServerLevel sl, Vec3 c, double radius, int rgb, double[] doors, int points) {
        DustParticleOptions d = dust(rgb, 1.4f);
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            if (CageMath.inDoor(a, doors)) continue;
            sl.sendParticles(d, c.x + Math.cos(a) * radius, c.y + 0.1, c.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    /** A spoke of dust from {@code c} outwards at {@code angle}, {@code len} blocks long. */
    static void spoke(ServerLevel sl, Vec3 c, double angle, double len, int rgb) {
        DustParticleOptions d = dust(rgb, 1.0f);
        int n = (int) Math.max(3, Math.min(40, len * 1.5));
        for (int i = 1; i <= n; i++) {
            double t = len * i / n;
            sl.sendParticles(d, c.x + Math.cos(angle) * t, c.y + 0.15, c.z + Math.sin(angle) * t, 1, 0, 0, 0, 0);
        }
    }

    // ---------------------------------------------------------------- the Balance Law
    /**
     * One ask of the Balance Law's shared twelve-second lock for a whole order (a Bow, a fan of tendrils, the sky falling), made only when somebody is
     * really going to be held: a Bow that everyone obeyed, or tendrils that found nobody, must not use up the boss's next twelve seconds of hard control.
     */
    static final class Control {
        private final ZogratisBoss boss;
        private boolean asked, granted;

        Control(ZogratisBoss boss) { this.boss = boss; }

        /** Asks on the first call, then answers the same for the rest of the order. */
        boolean granted() {
            if (!asked) {
                asked = true;
                granted = BalanceLaw.beginControl(boss);
            }
            return granted;
        }
    }

    // ---------------------------------------------------------------- the hits
    /** What kind of hit a damage source is (see {@link DamageKind}): the three facts the pure classifier needs, read off the source. */
    static DamageKind kindOf(DamageSource source) {
        boolean anti = DanteMoves.antiMagicHit(source);
        boolean projectile = source.is(DamageTypeTags.IS_PROJECTILE);
        String path = source.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse("");
        return DamageKind.classify(source.getEntity() != null, projectile, path, anti);
    }

    // ---------------------------------------------------------------- the foes
    /** A demon sword in hand, or the Anti-Magic Lord, or the Anti-Magic grimoire out: gravity and modification find nothing to grip. */
    static boolean antiMagic(LivingEntity e) {
        return e instanceof Player p && DanteMoves.antiMagicHeld(p);
    }

    /** Holding sneak (a player), at this tick. */
    static boolean crouching(LivingEntity e) {
        return e instanceof Player p && (p.isShiftKeyDown() || p.isCrouching());
    }

    /** The factor on a crushing blow for someone who guards: a raised shield 0.35, anti-magic 0.4 (the better of the two), the same cuts as Dante's collapse. */
    static double guard01(LivingEntity e) {
        double f = 1.0;
        if (e.isBlocking()) f = Math.min(f, 0.35);
        if (antiMagic(e)) f = Math.min(f, 0.40);
        return f;
    }

    /** The factor on a MAGIC blow (a spell does not care for a raised shield): anti-magic cuts it to 40%, nothing else does. */
    static double magicGuard(LivingEntity e) { return antiMagic(e) ? 0.40 : 1.0; }

    /** Gravity wins over wings: a foe gliding on an elytra is brought out of the glide before any forced push. */
    static void grounded(LivingEntity e) {
        if (e instanceof Player p && p.isFallFlying()) p.stopFallFlying();
    }

    /** Drives {@code e} into the floor at {@code speed} blocks a tick, with no fall damage to come of it. */
    static void slam(LivingEntity e, double speed) {
        grounded(e);
        e.setDeltaMovement(e.getDeltaMovement().x * 0.3, -Math.abs(speed), e.getDeltaMovement().z * 0.3);
        e.hurtMarked = true;
        e.fallDistance = 0;
    }

    /** How far the feet of {@code e} are above the floor under them (0 on the ground or with no floor close by). */
    static double floorHeight(Level level, LivingEntity e) {
        if (e.onGround()) return 0;
        return Math.max(0, e.getY() - ZogratisBoss.ground(level, e.position()).y);
    }

    /**
     * A crushing blow through Tensura's own gravity damage type (so gravity resistance counts), plain magic if the type is missing. The amount is the
     * boss's damage law applied to {@code base}. False when the victim is not a foe or nothing landed.
     */
    static boolean crush(ZogratisBoss boss, LivingEntity victim, float base, ResourceKey<DamageType> type) {
        if (victim == null || !victim.isAlive() || !boss.isFoe(victim)) return false;
        float amount = boss.bossDamage(victim, base);
        if (amount <= 0f) return false;
        DamageSource src = boss.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(type)
                .<DamageSource>map(h -> new DamageSource(h, boss))
                .orElseGet(() -> boss.damageSources().indirectMagic(boss, boss));
        return victim.hurt(src, amount);
    }

    /**
     * Puts {@code e} on its knees for {@code ticks} ticks: the Kneeling effect plus a push to the floor. Never on a holder of anti-magic or on someone who
     * is not a foe. Returns whether it was applied.
     */
    static boolean kneel(ZogratisBoss boss, LivingEntity e, int ticks) {
        if (ticks <= 0 || !boss.isFoe(e) || antiMagic(e)) return false;
        try {
            e.addEffect(new MobEffectInstance(SpadeEffects.KNEELING, ticks, 0, false, true, true));
        } catch (RuntimeException notRegistered) {
            LogUtils.getLogger().warn("[nusmp] the Kneeling effect is not registered (SpadeEffects.EFFECTS must be wired in NUSMP): {}", notRegistered.toString());
            e.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, ticks, 5));
        }
        slam(e, 1.0);
        return true;
    }

    /** Applies the Flattened Air effect for {@code ticks} (falls back to nothing when the effect is not registered; the caller still brings fliers down). */
    static void flatten(LivingEntity e, int ticks) {
        try {
            e.addEffect(new MobEffectInstance(SpadeEffects.FLATTENED_AIR, ticks, 0, false, true, true));
        } catch (RuntimeException notRegistered) {
            // the velocity clamp the caller applies still brings fliers down; only the jump lock is lost
        }
    }

    // ---------------------------------------------------------------- hints
    /** A line on the action bar of one player. */
    static void hint(Player p, String key, String fallback, Object... args) {
        if (p instanceof ServerPlayer sp) sp.displayClientMessage(Component.translatableWithFallback(key, fallback, args), true);
    }

    /** A line on the action bar of every player within {@code radius} blocks of the boss. */
    static void hintAround(ZogratisBoss boss, double radius, String key, String fallback, Object... args) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(boss) < radius * radius) hint(p, key, fallback, args);
    }
}
