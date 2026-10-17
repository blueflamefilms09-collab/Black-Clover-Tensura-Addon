package com.newuniverse.nusmp.arc.early;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Why: the five early bosses are built from the same few acts (hurt everything in a circle, a cone or a corridor; run a ring wave out from a stomp; fly a bolt
 * until it meets a wall or a foe; strike a batch of marked circles after a warning; dash along a line), and each act touches the same easy-to-get-wrong
 * game details: which foes count (players in survival and the summons of players, never the Zogratis family), a body's width, a floor that is not flat, a
 * wall between the boss and the foe, a task that throws inside the server tick. Written once here, on the framework's own calls ({@code foesAround},
 * {@code strike}, {@code later}, {@code SpellRuntime.zone}), with the arithmetic in {@link EarlyGeometry}, each boss class keeps to what makes its fight its
 * own (the telegraph, the mechanic, the effect that is drawn). Server thread only. Nothing here changes a block.
 */
public final class EarlyMoves {
    private EarlyMoves() {}

    /** Foes are looked for inside a window from {@value #BELOW} blocks under the floor of a shape to {@value #ABOVE} above it. */
    public static final double BELOW = 2.5, ABOVE = 4.0;
    /** Never more than this many foes are looked at by one act (the framework caps its own the same way). */
    public static final int MAX_FOES = 24;

    // ================================================================ safety
    /**
     * Zone steps and delayed tasks run inside the server tick without a net under them, so every one of a boss's goes through here: a step that throws is
     * logged once and that zone stops, it never takes the tick down.
     */
    public static IntConsumer safe(String what, IntConsumer step) {
        boolean[] dead = {false};
        return age -> {
            if (dead[0]) return;
            try {
                step.accept(age);
            } catch (Throwable t) {
                dead[0] = true;
                LogUtils.getLogger().warn("[nusmp] an early boss's {} stopped: {}", what, t.toString());
            }
        };
    }

    /** {@link #safe(String, IntConsumer)} for a delayed task. */
    public static Runnable safe(String what, Runnable task) {
        IntConsumer guarded = safe(what, age -> task.run());
        return () -> guarded.accept(0);
    }

    // ================================================================ who is hit
    /** Foes whose bodies touch the circle of {@code radius} round {@code centre} (the floor of the shape), within the height window. */
    public static List<LivingEntity> inCircle(ZogratisBoss boss, Vec3 centre, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : boss.foesAround(centre.add(0, 1, 0), radius + 4)) {
            double dy = e.getY() - centre.y;
            if (dy < -BELOW || dy > ABOVE) continue;
            if (Math.hypot(e.getX() - centre.x, e.getZ() - centre.z) - e.getBbWidth() / 2 > radius) continue;
            out.add(e);
            if (out.size() >= MAX_FOES) break;
        }
        return out;
    }

    /** Foes inside the cone from {@code origin} facing {@code facing}, {@code reach} long and {@code halfAngle} radians either side (a body's half width counts as reach). */
    public static List<LivingEntity> inCone(ZogratisBoss boss, Vec3 origin, Vec3 facing, double reach, double halfAngle) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : boss.foesAround(origin.add(0, 1, 0), reach + 4)) {
            double dy = e.getY() - origin.y;
            if (dy < -BELOW || dy > ABOVE) continue;
            if (!EarlyGeometry.inCone(origin.x, origin.z, facing.x, facing.z, reach + e.getBbWidth() / 2, halfAngle, e.getX(), e.getZ())) continue;
            out.add(e);
            if (out.size() >= MAX_FOES) break;
        }
        return out;
    }

    /** Foes inside the corridor of half width {@code halfWidth} along the segment from-to, nearest the start first. */
    public static List<LivingEntity> inCorridor(ZogratisBoss boss, Vec3 from, Vec3 to, double halfWidth) {
        Vec3 mid = from.add(to).scale(0.5);
        double half = from.distanceTo(to) / 2 + halfWidth + 4;
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : boss.foesAround(mid.add(0, 1, 0), half)) {
            double dy = e.getY() - Math.min(from.y, to.y);
            if (dy < -BELOW || dy > ABOVE + Math.abs(to.y - from.y)) continue;
            if (!EarlyGeometry.inCorridor(e.getX(), e.getZ(), from.x, from.z, to.x, to.z, halfWidth + e.getBbWidth() / 2)) continue;
            out.add(e);
        }
        out.sort(Comparator.comparingDouble(e -> EarlyGeometry.segmentT(e.getX(), e.getZ(), from.x, from.z, to.x, to.z)));
        return out.size() > MAX_FOES ? new ArrayList<>(out.subList(0, MAX_FOES)) : out;
    }

    // ================================================================ the world
    /** Can a thing fly from a to b without meeting a block? */
    public static boolean clear(ServerLevel sl, Vec3 a, Vec3 b, Entity context) {
        return sl.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context)).getType() == HitResult.Type.MISS;
    }

    /** Where a thing flying from a towards b stops: b, or the first block in the way. */
    public static Vec3 stopAtWall(ServerLevel sl, Vec3 a, Vec3 b, Entity context) {
        BlockHitResult hit = sl.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context));
        return hit.getType() == HitResult.Type.MISS ? b : hit.getLocation();
    }

    /** How far the feet of {@code e} are above the floor under them (0 when they stand on it). */
    public static double feetAboveFloor(Level level, LivingEntity e) {
        if (e.onGround()) return 0;
        return Math.max(0, e.getY() - ZogratisBoss.ground(level, e.position()).y);
    }

    /** The horizontal unit vector of a direction (the boss's facing when the vector has no length). */
    public static Vec3 flat(Vec3 v, Vec3 fallback) {
        Vec3 h = new Vec3(v.x, 0, v.z);
        if (h.lengthSqr() < 1e-6) {
            h = new Vec3(fallback.x, 0, fallback.z);
            if (h.lengthSqr() < 1e-6) return new Vec3(0, 0, 1);
        }
        return h.normalize();
    }

    // ================================================================ moving foes
    /** Shoves a victim away from a point. */
    public static void shove(LivingEntity victim, Vec3 from, double strength) {
        victim.knockback(strength, from.x - victim.getX(), from.z - victim.getZ());
        victim.hurtMarked = true;
    }

    /** Pulls a victim towards a point: {@code speed} blocks a tick, never past it. A glider is brought down first. */
    public static void pull(LivingEntity victim, Vec3 to, double speed) {
        if (victim instanceof Player p && p.isFallFlying()) p.stopFallFlying();
        Vec3 d = to.subtract(victim.position()).multiply(1, 0, 1);
        double len = d.length();
        if (len < 1e-3) return;
        Vec3 v = d.scale(Math.min(speed, len) / len);
        victim.setDeltaMovement(v.x, Math.max(victim.getDeltaMovement().y, 0.1), v.z);
        victim.hurtMarked = true;
    }

    public static void slow(LivingEntity e, int ticks, int amplifier) {
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amplifier));
    }

    public static void weaken(LivingEntity e, int ticks, int amplifier) {
        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, amplifier));
    }

    /** Frost on the body: the vanilla frozen-ticks counter (the blue vignette of powder snow), never beyond a little over full. A body that cannot freeze is left alone by the game itself. */
    public static void chill(LivingEntity e, int ticks) {
        e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 20, e.getTicksFrozen() + Math.max(0, ticks)));
    }

    /** A line on the action bar of a player (nothing for a summon or a mob). */
    public static void tell(LivingEntity e, String key, Object... args) {
        if (e instanceof ServerPlayer sp) sp.displayClientMessage(Component.translatable(key, args), true);
    }

    public static void sound(ServerLevel sl, Vec3 at, SoundEvent sound, float volume, float pitch) {
        sl.playSound(null, BlockPos.containing(at), sound, SoundSource.HOSTILE, volume, pitch);
    }

    // ================================================================ acts
    /** What happens at each landing of a batch strike, besides the damage. Either part may be null. */
    public interface Landing {
        /** Called once for every victim, with the centre that hit them. */
        void hit(ServerLevel sl, LivingEntity victim, Vec3 centre);
    }

    /** Called once for every centre, when it lands (the effect that is drawn). */
    public interface Burst {
        void at(ServerLevel sl, Vec3 centre);
    }

    /**
     * A batch of marked circles that land together: their rings are drawn in dust for {@code delay} ticks (the telegraph; the closing ring runs inwards), then
     * every foe still inside one is hurt, once however many circles they stand in. Moving out of a ring is the answer.
     */
    public static void batchStrike(ZogratisBoss boss, List<Vec3> centres, double radius, int delay, float damage, boolean magic, int rgb, int accent, Landing landing, Burst burst) {
        if (!(boss.level() instanceof ServerLevel sl) || centres.isEmpty()) return;
        List<Vec3> marks = new ArrayList<>(centres);
        if (delay > 3) {
            SpellRuntime.zone(sl, delay - 1, 3, safe("warning rings", age -> {
                if (!boss.isAlive() || boss.isRemoved()) return;
                double closing = Math.max(0.5, radius * (delay - age) / Math.max(1.0, delay));
                for (Vec3 m : marks) {
                    Tells.ring(sl, m, radius, rgb, 1.3f, (int) Math.min(48, 12 + radius * 5));
                    Tells.ring(sl, m, closing, accent, 1.0f, 20);
                }
            }));
        }
        boss.later(Math.max(1, delay), safe("landing", () -> {
            Set<Integer> hit = new HashSet<>();
            for (Vec3 m : marks) {
                if (burst != null) burst.at(sl, m);
                for (LivingEntity e : inCircle(boss, m, radius)) {
                    if (!hit.add(e.getId())) continue;
                    if (boss.strike(e, damage, magic) && landing != null) landing.hit(sl, e, m);
                }
            }
        }));
    }

    /**
     * A low ring wave out from {@code centre} to {@code reach}: it runs 1.1 blocks a tick and hurts each foe once as its edge passes them, unless their feet are
     * off the floor (they jumped it). The ring is drawn in dust as it runs.
     */
    public static void ringWave(ZogratisBoss boss, Vec3 centre, double reach, float damage, double knock, boolean magic, int rgb) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        Set<Integer> hit = new HashSet<>();
        SpellRuntime.zone(sl, EarlyGeometry.waveTicks(reach), 1, safe("ring wave", age -> {
            if (!boss.isAlive() || boss.isRemoved()) return;
            double wr = EarlyGeometry.waveRadius(age);
            if (age % 2 == 0) Tells.ring(sl, centre, Math.min(wr, reach), rgb, 1.5f, (int) Math.min(80, 12 + Math.min(wr, reach) * 6));
            for (LivingEntity e : boss.foesAround(centre.add(0, 1, 0), reach + 4)) {
                if (hit.contains(e.getId())) continue;
                double dist = Math.hypot(e.getX() - centre.x, e.getZ() - centre.z);
                if (dist > reach + e.getBbWidth() / 2 || e.getY() - centre.y < -BELOW || e.getY() - centre.y > ABOVE) continue;
                if (!EarlyGeometry.ringHits(dist, wr) || EarlyGeometry.jumpsRing(feetAboveFloor(sl, e))) continue;
                hit.add(e.getId());
                if (boss.strike(e, damage, magic) && knock > 0) shove(e, centre, knock);
            }
        }));
    }

    /**
     * Wisps: little bodies of smoke or crystal that start at {@code starts}, walk (through the air, not through walls) towards the nearest foe at
     * {@code speed} blocks a tick, and burst on touching one or when their {@code life} runs out: every foe in {@code burstRadius} of the burst is hurt
     * once per wisp. They are not entities (a party cannot kill them, only outrun them or step aside), they cost one zone for the whole swarm, and they end with
     * the boss.
     */
    public static void wisps(ZogratisBoss boss, List<Vec3> starts, double speed, int life, double burstRadius, float damage, boolean magic, int rgb, Landing landing, Burst burst) {
        if (!(boss.level() instanceof ServerLevel sl) || starts.isEmpty()) return;
        int n = starts.size();
        Vec3[] pos = starts.toArray(new Vec3[0]);
        boolean[] live = new boolean[n];
        LivingEntity[] goal = new LivingEntity[n];
        java.util.Arrays.fill(live, true);
        final int step = 2;
        SpellRuntime.zone(sl, life + step, step, safe("wisps", age -> {
            for (int i = 0; i < n; i++) {
                if (!live[i]) continue;
                if (!boss.isAlive() || boss.isRemoved()) {
                    live[i] = false;
                    continue;
                }
                if (goal[i] == null || !goal[i].isAlive() || age % 10 == 0) goal[i] = nearest(boss, pos[i], 40);
                boolean explode = age >= life;
                LivingEntity g = goal[i];
                if (g != null) {
                    Vec3 to = g.getBoundingBox().getCenter().subtract(pos[i]);
                    double d = to.length();
                    if (d <= 1.5) explode = true;
                    else {
                        Vec3 next = pos[i].add(to.scale(Math.min(speed * step, d) / d));
                        Vec3 stop = stopAtWall(sl, pos[i], next, boss);
                        if (stop != next) explode = true;
                        pos[i] = stop;
                    }
                }
                Tells.gather(sl, pos[i], 0.25, rgb, 3);
                if (!explode) continue;
                live[i] = false;
                if (burst != null) burst.at(sl, pos[i]);
                Vec3 floor = ZogratisBoss.ground(sl, pos[i]);
                for (LivingEntity e : inCircle(boss, floor, burstRadius)) if (boss.strike(e, damage, magic) && landing != null) landing.hit(sl, e, pos[i]);
            }
        }));
    }

    /** The foe nearest to a point within {@code radius}, or null. */
    public static LivingEntity nearest(ZogratisBoss boss, Vec3 p, double radius) {
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : boss.foesAround(p, radius)) {
            double d = e.distanceToSqr(p);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    /** What a bolt does where it ends: {@code victim} is the foe it struck, or null when it ended on a wall or ran out of life. */
    public interface Impact {
        void at(ServerLevel sl, Vec3 where, LivingEntity victim);
    }

    /**
     * A bolt: a point that flies {@code speed} blocks a tick for at most {@code life} ticks, stops at the first block, and hurts the first foe it touches. A
     * raised shield stops it (it is an ordinary blow unless {@code magic}). It is a real moving point, so a foe who steps aside as it comes is missed.
     */
    public static void bolt(ZogratisBoss boss, Vec3 from, Vec3 dir, double speed, int life, double radius, float damage, boolean magic, Impact impact) {
        if (!(boss.level() instanceof ServerLevel sl) || dir.lengthSqr() < 1e-6) return;
        Vec3 vel = dir.normalize().scale(speed);
        Vec3[] pos = {from};
        int[] left = {Math.max(1, life)};
        boolean[] done = {false};
        SpellRuntime.zone(sl, life + 1, 1, safe("bolt", age -> {
            if (done[0]) return;
            if (!boss.isAlive() || boss.isRemoved()) {
                done[0] = true;
                return;
            }
            Vec3 next = pos[0].add(vel);
            BlockHitResult wall = sl.clip(new ClipContext(pos[0], next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
            boolean blocked = wall.getType() != HitResult.Type.MISS;
            if (blocked) next = wall.getLocation();
            AABB sweep = new AABB(pos[0], next).inflate(radius);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, sweep, boss::isFoe)) {
                if (boss.strike(e, damage, magic)) shove(e, pos[0].subtract(vel.scale(2)), 0.4);
                done[0] = true;
                if (impact != null) impact.at(sl, next, e);
                return;
            }
            pos[0] = next;
            if (blocked || --left[0] <= 0) {
                done[0] = true;
                if (impact != null) impact.at(sl, pos[0], null);
            }
        }));
    }

    /**
     * A charge: for {@code ticks} ticks the boss runs along {@code dir} at {@code speed} blocks a tick and hurts each foe it touches once (a damage of 0 is a plain
     * run, a retreat). It stops at a wall.
     * The boss is not teleported: it really runs (the model's run clip plays), and a foe who steps out of its way is missed.
     */
    public static void charge(ZogratisBoss boss, Vec3 dir, double speed, int ticks, double reach, float damage, double knock) {
        if (!(boss.level() instanceof ServerLevel sl) || dir.lengthSqr() < 1e-6) return;
        Vec3 d = dir.normalize();
        Set<Integer> hit = new HashSet<>();
        boss.getNavigation().stop();
        SpellRuntime.zone(sl, ticks, 1, safe("charge", age -> {
            if (!boss.isAlive() || boss.isRemoved()) return;
            boss.setDeltaMovement(d.x * speed, Math.min(boss.getDeltaMovement().y, 0.0), d.z * speed);
            for (LivingEntity e : boss.foesAround(boss.position().add(0, 1, 0), reach + 2)) {
                if (hit.contains(e.getId()) || boss.distanceTo(e) > reach + e.getBbWidth() / 2 + boss.getBbWidth() / 2) continue;
                hit.add(e.getId());
                if (damage > 0 && boss.strike(e, damage, false) && knock > 0) shove(e, boss.position().subtract(d.scale(2)), knock);
            }
        }));
    }
}
