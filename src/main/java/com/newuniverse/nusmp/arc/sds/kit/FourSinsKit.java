package com.newuniverse.nusmp.arc.sds.kit;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.DanteMoves;
import com.newuniverse.nusmp.entity.zogratis.SpellSeal;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
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
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Why: Merlin, Gowther, King and Diane are four bosses built on the same framework and they keep needing the same dozen small things: a guard that stops a
 * broken zone step from taking the server tick down, rotating a vector, finding a standable spot, listing the foes in a line / a cone / a disc, reading what
 * KIND of blow a damage source was, silencing a grimoire under the balance law, drawing a locked ground mark, a bounded homing bolt. Writing them four times
 * would mean four chances to get one wrong, so they live here once, in the glue half (the arithmetic is in {@link FourSinsMath}, tested). Nothing here
 * changes the world's blocks, spawns an entity or keeps state between calls: every method takes what it needs and returns, and the only things it starts are
 * zone steps and delayed tasks that end by themselves. All draw calls use {@link Tells} (the sanctioned server-side telegraph dust), never raw particle types.
 */
public final class FourSinsKit {
    private FourSinsKit() {}

    // ================================================================ guards
    /** A zone step that throws is logged once and that zone stops: it never takes the server tick down. */
    public static IntConsumer safe(String who, String what, IntConsumer step) {
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
    public static Runnable safe(String who, String what, Runnable task) {
        IntConsumer guarded = safe(who, what, age -> task.run());
        return () -> guarded.accept(0);
    }

    // ================================================================ geometry
    public static Vec3 rotateY(Vec3 v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    /** The horizontal unit vector from {@code a} towards {@code b}; {@code fallback} (or +z) if they are on top of each other. */
    public static Vec3 flatDir(Vec3 a, Vec3 b, Vec3 fallback) {
        Vec3 d = b.subtract(a).multiply(1, 0, 1);
        if (d.lengthSqr() < 1e-4) {
            d = fallback == null ? Vec3.ZERO : fallback.multiply(1, 0, 1);
            if (d.lengthSqr() < 1e-4) d = new Vec3(0, 0, 1);
        }
        return d.normalize();
    }

    /** A spot near {@code want} where {@code who} fits on solid floor, or null: never a spot in mid-air over a pit or the void. */
    public static Vec3 standable(Level level, Entity who, Vec3 want) {
        Vec3 g = ZogratisBoss.groundOrNull(level, want);
        if (g == null) return null;
        for (double dy : new double[]{0, 1, -1, 2}) {
            Vec3 c = new Vec3(g.x, g.y + dy, g.z);
            if (level.noCollision(who, who.getBoundingBox().move(c.subtract(who.position())))) return c;
        }
        return null;
    }

    /** Where a straight line from {@code a} to {@code b} first meets a wall ({@code b} itself if it is clear). */
    public static Vec3 clipWall(ServerLevel sl, Entity by, Vec3 a, Vec3 b) {
        BlockHitResult hit = sl.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, by));
        return hit.getType() == HitResult.Type.MISS ? b : hit.getLocation();
    }

    /** How far the feet of {@code e} are above the floor under them (0 if it stands on the ground or there is none close by). */
    public static double floorHeight(Level level, LivingEntity e) {
        if (e.onGround()) return 0;
        return Math.max(0, e.getY() - ZogratisBoss.ground(level, e.position()).y);
    }

    /** Alive, not a spectator, not in creative: the only things a control effect may touch. */
    public static boolean fair(LivingEntity e) {
        return e != null && e.isAlive() && !e.isSpectator() && !(e instanceof Player p && p.isCreative());
    }

    // ================================================================ foes
    /** The foes within {@code halfWidth} of the segment a-b on the ground plane (and within three blocks above or four below its ends). */
    public static List<LivingEntity> inLine(ZogratisBoss boss, Vec3 a, Vec3 b, double halfWidth) {
        List<LivingEntity> out = new ArrayList<>();
        Vec3 mid = a.add(b).scale(0.5);
        double reach = a.distanceTo(b) / 2 + halfWidth + 2;
        double lo = Math.min(a.y, b.y) - 4, hi = Math.max(a.y, b.y) + 3;
        for (LivingEntity e : boss.foesAround(mid, reach + 4)) {
            if (e.getY() < lo || e.getY() > hi) continue;
            if (FourSinsMath.distToSegment(e.getX(), e.getZ(), a.x, a.z, b.x, b.z) <= halfWidth + e.getBbWidth() / 2) out.add(e);
        }
        return out;
    }

    /** The foes inside a horizontal cone from {@code tip} along {@code dir}, {@code range} blocks long, {@code halfAngle} radians to each side. */
    public static List<LivingEntity> inCone(ZogratisBoss boss, Vec3 tip, Vec3 dir, double range, double halfAngle) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : boss.foesAround(tip, range + 2)) {
            if (Math.abs(e.getY() - tip.y) > 5) continue;
            if (FourSinsMath.inCone(tip.x, tip.z, dir.x, dir.z, e.getX(), e.getZ(), range + e.getBbWidth() / 2, halfAngle)) out.add(e);
        }
        return out;
    }

    /** The foes within {@code radius} of {@code c} on the ground plane and between {@code below} under and {@code above} over its height. */
    public static List<LivingEntity> inDisc(ZogratisBoss boss, Vec3 c, double radius, double below, double above) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : boss.foesAround(c.add(0, (above - below) / 2, 0), Math.sqrt(radius * radius + Math.pow((above + below) / 2, 2)) + 1)) {
            double dy = e.getY() - c.y;
            if (dy < -below || dy > above) continue;
            if (Math.hypot(e.getX() - c.x, e.getZ() - c.z) <= radius + e.getBbWidth() / 2) out.add(e);
        }
        return out;
    }

    // ================================================================ what a blow was
    /** The kind of a blow: a blade, an arrow, a spell, an element ... (see {@link DamageKind}). */
    public static DamageKind kindOf(DamageSource s) {
        String path = s.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse("");
        Entity direct = s.getDirectEntity(), by = s.getEntity();
        boolean melee = by instanceof LivingEntity && direct == by;
        return DamageKind.of(new DamageKind.Facts(s.is(DamageTypeTags.IS_FREEZING), s.is(DamageTypeTags.IS_FIRE), s.is(DamageTypeTags.IS_EXPLOSION),
                s.is(DamageTypeTags.IS_LIGHTNING), s.is(DamageTypeTags.IS_PROJECTILE), SpellSeal.isSpellDamage(s), melee, path));
    }

    /** A spell by the mod's own definition (vanilla magic, or a Tensura damage type that is not physical). */
    public static boolean isSpell(DamageSource s) { return SpellSeal.isSpellDamage(s); }

    /** A demon sword's blow, the Anti-Magic Lord's, or a player fighting with their Anti-Magic grimoire out. */
    public static boolean isAntiMagic(DamageSource s) { return DanteMoves.antiMagicHit(s); }

    /** Where the blow came from: the one who threw or cast it, else the thing that struck; null for the weather, falls and the like. */
    public static Vec3 originOf(DamageSource s) {
        Entity e = s.getEntity() != null ? s.getEntity() : s.getDirectEntity();
        return e == null ? null : e.position();
    }

    /** The blow came from beyond arm's reach: a projectile in between, or a thrower further than 4.5 blocks away. */
    public static boolean isRanged(Entity self, DamageSource s) {
        Entity direct = s.getDirectEntity(), by = s.getEntity();
        if (direct != null && by != null && direct != by) return true;
        Vec3 o = originOf(s);
        return o != null && o.distanceToSqr(self.position()) > 4.5 * 4.5;
    }

    // ================================================================ control, under the balance law
    /**
     * Silences a player's grimoire for up to {@code baseTicks} (the balance law caps it at three seconds and halves it on a repeat) by writing the mod's own
     * sealing flag. The CALLER pays the shared control cool-down once per move with {@code BalanceLaw.beginControl(boss)}. Refuses creative players,
     * spectators and anyone holding anti-magic. Returns the ticks applied, 0 if none.
     */
    public static int silence(LivingEntity victim, int baseTicks) {
        if (!(victim instanceof Player p) || !fair(victim) || DanteMoves.antiMagicHeld(p)) return 0;
        int t = BalanceLaw.controlTicks(victim, baseTicks);
        long until = victim.level().getGameTime() + t;
        if (p.getPersistentData().getLong(SpellSeal.SEALED_FLAG) < until) p.getPersistentData().putLong(SpellSeal.SEALED_FLAG, until);
        return t;
    }

    // ================================================================ talking to the player
    /** A line on the action bar of one player (nothing for a mob). */
    public static void note(LivingEntity who, String line, ChatFormatting colour) {
        if (who instanceof ServerPlayer p) p.displayClientMessage(Component.literal(line).withStyle(colour), true);
    }

    /** A line on the action bar of everyone within {@code r} of {@code at}. */
    public static void bar(ServerLevel sl, Vec3 at, double r, String line, ChatFormatting colour) {
        Component c = Component.literal(line).withStyle(colour, ChatFormatting.BOLD);
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(at) < r * r) p.displayClientMessage(c, true);
    }

    public static void sound(ServerLevel sl, Vec3 at, SoundEvent ev, float volume, float pitch) {
        sl.playSound(null, BlockPos.containing(at), ev, SoundSource.HOSTILE, volume, pitch);
    }

    // ================================================================ telegraphs
    /** The first call of a wind-up (the tell runs every third tick, so the first is within three ticks of the start). */
    public static boolean first(int left, int total) { return left >= total - 3; }

    /** A locked ground mark: its rim, a ring that closes as the wind-up runs out, and a column. Called on every tell tick. */
    public static void markTell(ServerLevel sl, Vec3 c, double radius, int left, int total, int rgb, int accent) {
        Tells.ring(sl, c, radius, rgb, 1.4f, (int) Math.min(80, 14 + radius * 5));
        Tells.ring(sl, c, Math.max(0.6, radius * left / Math.max(1, total)), accent, 1.1f, 28);
        Tells.column(sl, c, 4, accent);
    }

    /** A lane: two parallel lines {@code halfWidth} either side of the line from {@code from} along {@code dir} for {@code length} blocks. */
    public static void laneTell(ServerLevel sl, Vec3 from, Vec3 dir, double length, double halfWidth, int rgb) {
        Vec3 flat = dir.multiply(1, 0, 1);
        if (flat.lengthSqr() < 1e-6) return;
        Vec3 d = flat.normalize(), side = new Vec3(-d.z, 0, d.x).scale(halfWidth);
        Vec3 a = from.add(0, 0.2, 0), b = a.add(d.scale(length));
        Tells.line(sl, a.add(side), b.add(side), rgb);
        Tells.line(sl, a.subtract(side), b.subtract(side), rgb);
    }

    /** A cone: its two edges and a few points of the arc, drawn in dust. */
    public static void coneTell(ServerLevel sl, Vec3 tip, Vec3 dir, double range, double halfAngle, int rgb) {
        Vec3 flat = dir.multiply(1, 0, 1);
        if (flat.lengthSqr() < 1e-6) return;
        Vec3 d = flat.normalize(), base = tip.add(0, 0.2, 0);
        Tells.line(sl, base, base.add(rotateY(d, halfAngle).scale(range)), rgb);
        Tells.line(sl, base, base.add(rotateY(d, -halfAngle).scale(range)), rgb);
        for (int i = -2; i <= 2; i++) {
            Vec3 p = base.add(rotateY(d, halfAngle * i / 2.0).scale(range));
            Tells.gather(sl, p, 0.1, rgb, 1);
        }
    }

    // ================================================================ a bounded homing bolt
    /**
     * A bolt that flies {@code speed} blocks a tick for {@code life} ticks, bends towards {@code homing} by {@code turn} (0 = straight, 0.1 = a gentle curve),
     * stops at the first wall and calls {@code onHit} on the first foe of the boss it touches. It is a point, not an entity: one guarded zone step per tick,
     * ended by its own life or by its owner leaving the world. Drawn in telegraph dust.
     */
    public static final class Bolt {
        private final ZogratisBoss owner;
        private Vec3 pos, vel;
        private int life;
        private final double speed, reach, turn;
        private final LivingEntity homing;
        private final int rgb;
        private final Consumer<LivingEntity> onHit;
        private boolean done;

        public Bolt(ZogratisBoss owner, Vec3 start, Vec3 dir, double speed, int life, double reach, double turn, LivingEntity homing, int rgb, Consumer<LivingEntity> onHit) {
            this.owner = owner;
            this.pos = start;
            this.speed = speed;
            this.vel = dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, speed) : dir.normalize().scale(speed);
            this.life = Math.max(1, life);
            this.reach = reach;
            this.turn = Math.max(0, Math.min(1, turn));
            this.homing = homing;
            this.rgb = rgb;
            this.onHit = onHit;
        }

        /** Starts the bolt. */
        public void launch(ServerLevel sl) {
            SpellRuntime.zone(sl, life + 2, 1, safe(owner.bossName(), "bolt", age -> step(sl)));
        }

        private void step(ServerLevel sl) {
            if (done) return;
            if (!owner.isAlive() || owner.isRemoved()) { done = true; return; }
            if (homing != null && homing.isAlive() && turn > 0) {
                Vec3 want = homing.getBoundingBox().getCenter().subtract(pos);
                if (want.lengthSqr() > 1e-4) {
                    vel = vel.scale(1 - turn).add(want.normalize().scale(speed * turn));
                    double l = vel.length();
                    if (l > 1e-4) vel = vel.scale(speed / l);
                }
            }
            Vec3 next = pos.add(vel);
            BlockHitResult wall = sl.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
            boolean blocked = wall.getType() != HitResult.Type.MISS;
            if (blocked) next = wall.getLocation();
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(pos, next).inflate(reach), owner::isFoe)) {
                onHit.accept(e);
                Tells.burst(sl, next, rgb, 8);
                done = true;
                return;
            }
            pos = next;
            Tells.gather(sl, pos, 0.05, rgb, 2);
            if (blocked || --life <= 0) {
                Tells.burst(sl, pos, rgb, 8);
                done = true;
            }
        }
    }
}
