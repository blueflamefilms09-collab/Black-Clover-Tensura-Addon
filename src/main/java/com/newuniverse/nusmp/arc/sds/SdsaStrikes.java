package com.newuniverse.nusmp.arc.sds;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.DanteMoves;
import com.newuniverse.nusmp.entity.zogratis.FxMath;
import com.newuniverse.nusmp.entity.zogratis.GravityMath;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/**
 * Why: the three bosses of the first Sins group (Meliodas, Escanor, Ban) throw cones, lines, rings, shots and fields at the party, and each of those is the same
 * few lines of geometry and bookkeeping (who stands in the strip, what a wall stops, who has jumped the wave, how a typed blow is dealt through the boss's damage
 * law, how a zone step that throws is kept from taking the tick down). The Zogratis helpers that do this are package-private to {@code entity.zogratis}, so the
 * arc bosses cannot call them; this class is the same toolbox for the {@code arc.sds} package, written once so that no move of the three re-implements it and a
 * fix lands in all of them. Everything is bounded (a shot is a point on a clipped ray, a wave and a field look at a few dozen foes) and nothing changes a block.
 * Server thread only. The damage always goes through {@link ZogratisBoss#bossDamage} (the power tuning, the victim's EP, the balance law's caps, so one hit never
 * takes more than the share of a player's health the server allows).
 */
public final class SdsaStrikes {
    private SdsaStrikes() {}

    /** The line the framework announces when the Gates open, word for word (the key of the rewrite each boss makes in its own voice). */
    public static final String QLIPHOTH_GATES = "The Gates of the Tree of Qliphoth open!";

    // ================================================================ safety
    /** A zone step that throws is logged once and that zone stops; it never takes the tick down. */
    public static IntConsumer safe(String what, IntConsumer step) {
        boolean[] dead = {false};
        return age -> {
            if (dead[0]) return;
            try {
                step.accept(age);
            } catch (Throwable t) {
                dead[0] = true;
                LogUtils.getLogger().warn("[nusmp] a Seven Deadly Sins boss's {} stopped: {}", what, t.toString());
            }
        };
    }

    /** {@link #safe(String, IntConsumer)} for a delayed task. */
    public static Runnable safe(String what, Runnable task) {
        IntConsumer guarded = safe(what, age -> task.run());
        return () -> guarded.accept(0);
    }

    // ================================================================ geometry
    /** The vector with its height taken out. */
    public static Vec3 flat(Vec3 v) { return new Vec3(v.x, 0, v.z); }

    /** The unit vector of the horizontal part of {@code v}, or {@code fallback} when it has none. */
    public static Vec3 unitFlat(Vec3 v, Vec3 fallback) {
        Vec3 f = flat(v);
        return f.lengthSqr() < 1e-6 ? fallback : f.normalize();
    }

    /** {@code v} turned round the vertical axis by {@code angle} radians. */
    public static Vec3 rotateY(Vec3 v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    /** How far along the unit direction {@code dir} (from {@code origin}) the point {@code p} lies, horizontally. */
    public static double along(Vec3 origin, Vec3 dir, Vec3 p) { return (p.x - origin.x) * dir.x + (p.z - origin.z) * dir.z; }

    /** How far to the side of the line (origin, unit direction {@code dir}) the point {@code p} lies, horizontally (always positive). */
    public static double lateral(Vec3 origin, Vec3 dir, Vec3 p) { return Math.abs((p.x - origin.x) * -dir.z + (p.z - origin.z) * dir.x); }

    /**
     * Is {@code p} inside the cone with its apex at {@code apex}, a unit facing {@code facing}, a {@code reach} and a half angle (radians)? Something very close to
     * the apex counts as inside whatever the angle (a body is wider than a point), and {@code slack} widens the reach by the target's own radius.
     */
    public static boolean inCone(Vec3 apex, Vec3 facing, double reach, double halfAngle, Vec3 p, double slack) {
        Vec3 to = flat(p.subtract(apex));
        double d = to.length();
        if (d > reach + slack) return false;
        if (d < 1.2) return true;
        return to.normalize().dot(facing) >= Math.cos(halfAngle);
    }

    /** How far the feet of {@code e} are above the floor under them (0 when they stand on it). */
    public static double floorHeight(Level level, LivingEntity e) {
        if (e.onGround()) return 0;
        return Math.max(0, e.getY() - ZogratisBoss.ground(level, e.position()).y);
    }

    /** Does a block stand between {@code from} and the body of {@code victim}? */
    public static boolean covered(ServerLevel sl, Vec3 from, LivingEntity victim) {
        BlockHitResult r = sl.clip(new ClipContext(from, victim.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, victim));
        return r.getType() != HitResult.Type.MISS;
    }

    /** The point {@code want} seen from {@code from}, brought back to just before the first wall in between (the boss's body being the context). */
    public static Vec3 clipEnd(ServerLevel sl, ZogratisBoss boss, Vec3 from, Vec3 want) {
        BlockHitResult r = sl.clip(new ClipContext(from, want, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
        if (r.getType() == HitResult.Type.MISS) return want;
        Vec3 dir = want.subtract(from);
        if (dir.lengthSqr() < 1e-6) return from;
        return r.getLocation().subtract(dir.normalize().scale(0.8));
    }

    // ================================================================ who stands where
    /** The foes in a cone (apex, unit facing, reach, half angle in radians): inside it horizontally, within three blocks of the apex's height. */
    public static List<LivingEntity> foesInCone(ZogratisBoss boss, Vec3 apex, Vec3 facing, double reach, double halfAngle) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : boss.foesAround(apex, reach + 3.0)) {
            if (Math.abs(e.getY() - apex.y) > 3.0) continue;
            if (inCone(apex, facing, reach, halfAngle, e.position(), e.getBbWidth() / 2.0)) out.add(e);
        }
        return out;
    }

    /** The foes in a strip that starts at {@code from} and runs {@code length} blocks along the unit direction {@code dir}, {@code halfWidth} to each side. */
    public static List<LivingEntity> foesInStrip(ZogratisBoss boss, Vec3 from, Vec3 dir, double length, double halfWidth) {
        List<LivingEntity> out = new ArrayList<>();
        Vec3 mid = from.add(dir.scale(length / 2.0));
        for (LivingEntity e : boss.foesAround(mid, length / 2.0 + halfWidth + 3.0)) {
            if (Math.abs(e.getY() - from.y) > 3.0) continue;
            double a = along(from, dir, e.position());
            if (a < -0.5 || a > length + 0.5) continue;
            if (lateral(from, dir, e.position()) <= halfWidth + e.getBbWidth() / 2.0) out.add(e);
        }
        return out;
    }

    /** The foes inside a disc of radius {@code r} round {@code centre} (a body counts by its own width), within three blocks of its height. */
    public static List<LivingEntity> foesInDisc(ZogratisBoss boss, Vec3 centre, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : boss.foesAround(centre, r + 3.0)) {
            if (Math.abs(e.getY() - centre.y) > 3.0) continue;
            double dx = e.getX() - centre.x, dz = e.getZ() - centre.z, reach = r + e.getBbWidth() / 2.0;
            if (dx * dx + dz * dz <= reach * reach) out.add(e);
        }
        return out;
    }

    /**
     * Where {@code n} zones of {@code radius} go: the first on the target, the next on other foes that stand further than a radius from every mark placed so far,
     * and the rest on a ring round the target at 2.1 radii (turned by a random phase), skipping any spot closer than 1.5 radii to a mark. Always at least one.
     */
    public static List<Vec3> spread(ZogratisBoss boss, LivingEntity target, int n, double radius) {
        Level level = boss.level();
        List<Vec3> out = new ArrayList<>();
        out.add(ZogratisBoss.ground(level, target.position()));
        for (LivingEntity e : boss.foes(44)) {
            if (out.size() >= n) break;
            if (e == target) continue;
            boolean apart = true;
            for (Vec3 m : out) if (m.distanceToSqr(e.position()) <= radius * radius) apart = false;
            if (apart) out.add(ZogratisBoss.ground(level, e.position()));
        }
        double phase = boss.getRandom().nextDouble() * Math.PI * 2;
        for (int k = 0; out.size() < n && k < 12; k++) {
            double a = phase + k * Math.PI * 2 / 6.0;
            Vec3 p = ZogratisBoss.ground(level, target.position().add(Math.cos(a) * radius * 2.1, 0, Math.sin(a) * radius * 2.1));
            boolean apart = true;
            for (Vec3 m : out) if (m.distanceToSqr(p) <= radius * radius * 2.25) apart = false;
            if (apart) out.add(p);
        }
        return out;
    }

    // ================================================================ telegraph shapes
    /** The outline of a cone in dust: both edges and the arc between them. */
    public static void tellCone(ServerLevel sl, Vec3 apex, Vec3 facing, double reach, double halfAngle, int rgb) {
        Vec3 base = apex.add(0, 0.15, 0);
        Vec3 prev = base.add(rotateY(facing, halfAngle).scale(reach));
        Tells.line(sl, base, prev, rgb);
        Tells.line(sl, base, base.add(rotateY(facing, -halfAngle).scale(reach)), rgb);
        final int steps = 8;
        for (int i = 1; i <= steps; i++) {
            Vec3 p = base.add(rotateY(facing, halfAngle - 2.0 * halfAngle * i / steps).scale(reach));
            Tells.line(sl, prev, p, rgb);
            prev = p;
        }
    }

    /** The outline of a strip in dust: both long edges and the far end. */
    public static void tellStrip(ServerLevel sl, Vec3 from, Vec3 dir, double length, double halfWidth, int rgb) {
        Vec3 base = from.add(0, 0.15, 0), side = new Vec3(-dir.z, 0, dir.x).scale(halfWidth), end = base.add(dir.scale(length));
        Tells.line(sl, base.add(side), end.add(side), rgb);
        Tells.line(sl, base.subtract(side), end.subtract(side), rgb);
        Tells.line(sl, end.add(side), end.subtract(side), rgb);
    }

    // ================================================================ effects
    /**
     * Sends one effect to the clients near it. {@code strike} picks the strike voice of the shapes that have a telegraph voice and a strike voice (the seed's low
     * bit, see {@code FxMath.seed}); the others ignore it. {@code rgb} is 0xRRGGBB.
     */
    public static void fx(ServerLevel sl, VfxShape shape, Vec3 from, Vec3 to, int rgb, int ticks, float power, boolean strike) {
        long seed = FxMath.seed(sl.random.nextLong(), !strike, false);
        VfxSpawn.send(sl, new VfxPayload(shape.ordinal(), from, to, 0xFF000000 | rgb, Math.max(1, ticks), power, -1, seed));
    }

    public static void sound(ServerLevel sl, Vec3 at, SoundEvent sound, float volume, float pitch) {
        sl.playSound(null, BlockPos.containing(at), sound, SoundSource.HOSTILE, volume, pitch);
    }

    /** Shows a line on the action bar of every player within {@code r} of {@code at}. */
    public static void bar(ServerLevel sl, Vec3 at, double r, Component text) {
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(at) <= r * r) p.displayClientMessage(text, true);
    }

    // ================================================================ damage
    /** A damage source of the boss with a Tensura (or vanilla) damage type; plain magic damage of the boss if the type is missing from the registry. */
    public static DamageSource source(ZogratisBoss boss, ResourceKey<DamageType> type) {
        return boss.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(type)
                .<DamageSource>map(h -> new DamageSource(h, boss))
                .orElseGet(() -> boss.damageSources().indirectMagic(boss, boss));
    }

    /**
     * A blow through the boss's damage law, as damage of a Tensura type (so the victim's resistances to it count; plain magic damage if the type is missing).
     * False when the victim is not a foe, is dead, or the law leaves nothing of the blow.
     */
    public static boolean hit(ZogratisBoss boss, LivingEntity victim, float base, ResourceKey<DamageType> type) {
        if (victim == null || !victim.isAlive() || !boss.isFoe(victim)) return false;
        float amount = boss.bossDamage(victim, base);
        if (amount <= 0f) return false;
        return victim.hurt(source(boss, type), amount);
    }

    /** The share of a blow that reaches someone who guards: a raised shield cuts 65%, anti-magic 60%, the better of the two (the framework's own rule for its collapses). */
    public static float guard(LivingEntity e) {
        return (float) GravityMath.mitigation(e.isBlocking(), e instanceof Player p && DanteMoves.antiMagicHeld(p));
    }

    /** The path of the damage type of a blow ("fire_elemental", "player_attack"), or an empty string. */
    public static String typePath(DamageSource s) {
        return s.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse("");
    }

    /** Pushes {@code victim} away from {@code from} with the knockback {@code strength} and a little lift (0 for none), and tells the client. */
    public static void push(LivingEntity victim, Vec3 from, double strength, double lift) {
        Vec3 away = flat(victim.position().subtract(from));
        Vec3 n = away.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : away.normalize();
        victim.knockback(strength, -n.x, -n.z);
        if (lift > 0) victim.setDeltaMovement(victim.getDeltaMovement().add(0, lift, 0));
        victim.hurtMarked = true;
    }

    // ================================================================ movement
    /** Puts the boss at {@code to} if its body fits there. */
    public static boolean dashTo(ZogratisBoss boss, Vec3 to) {
        if (!(boss.level() instanceof ServerLevel sl)) return false;
        AABB moved = boss.getBoundingBox().move(to.subtract(boss.position()));
        if (!sl.noCollision(boss, moved)) return false;
        boss.teleportTo(to.x, to.y, to.z);
        boss.getNavigation().stop();
        return true;
    }

    // ================================================================ shots
    /**
     * A shot: a point that flies {@code speed} blocks a tick along {@code dir} for {@code life} ticks, stops at the first wall and hurts the first foe it touches
     * (within {@code radius}). {@code onHit} is told the victim and the place, {@code trail} is called with the point every tick (the caller draws its dust).
     */
    public static void shoot(ZogratisBoss boss, ServerLevel sl, Vec3 start, Vec3 dir, double speed, int life, double radius, float damage,
                             ResourceKey<DamageType> type, BiConsumer<LivingEntity, Vec3> onHit, java.util.function.Consumer<Vec3> trail) {
        final Vec3 vel = dir.normalize().scale(speed);
        final Vec3[] pos = {start};
        final int[] left = {Math.max(1, life)};
        final boolean[] done = {false};
        SpellRuntime.zone(sl, life + 2, 1, safe("shot", age -> {
            if (done[0] || !boss.isAlive() || boss.isRemoved()) return;
            Vec3 next = pos[0].add(vel);
            BlockHitResult wall = sl.clip(new ClipContext(pos[0], next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
            boolean blocked = wall.getType() != HitResult.Type.MISS;
            if (blocked) next = wall.getLocation();
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(pos[0], next).inflate(radius), boss::isFoe)) {
                hit(boss, e, damage, type);
                if (onHit != null) onHit.accept(e, next);
                done[0] = true;
                return;
            }
            pos[0] = next;
            if (trail != null) trail.accept(next);
            if (blocked || --left[0] <= 0) done[0] = true;
        }));
    }

    // ================================================================ rings and fields
    /**
     * A low ring wave from {@code centre} out to {@code reach} (the framework's wave arithmetic: it travels 1.1 blocks a tick and is 1.3 blocks thick): each foe it
     * passes is hurt once unless they have jumped it (feet 0.75 blocks off the floor); a shield takes a plain blow. Hurts with the boss's plain blow, or with
     * {@code type} when it is not null, and knocks outwards.
     */
    public static void wave(ZogratisBoss boss, ServerLevel sl, Vec3 centre, double reach, float damage, double knock, int rgb, ResourceKey<DamageType> type) {
        final Set<Integer> hit = new HashSet<>();
        SpellRuntime.zone(sl, GravityMath.waveTicks(reach), 1, safe("ring wave", age -> {
            if (!boss.isAlive() || boss.isRemoved()) return;
            double wr = GravityMath.waveRadius(age);
            if (age % 2 == 0) Tells.ring(sl, centre, Math.min(wr, reach), rgb, 1.5f, (int) Math.min(80, 12 + Math.min(wr, reach) * 6));
            for (LivingEntity e : boss.foesAround(centre, reach + 3)) {
                if (hit.contains(e.getId())) continue;
                double dx = e.getX() - centre.x, dz = e.getZ() - centre.z, dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > reach + e.getBbWidth() / 2 || !GravityMath.waveHits(dist, wr) || GravityMath.jumpsWave(floorHeight(sl, e))) continue;
                hit.add(e.getId());
                boolean landed = type == null ? boss.strike(e, damage, false) : hit(boss, e, damage, type);
                if (landed) push(e, centre, knock, 0.0);
            }
        }));
    }
}
