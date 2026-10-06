package com.newuniverse.nusmp.book;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Server-side spell objects with real hitboxes, ticked every server tick:
 * bolts (moving hit volumes, optional homing/piercing), zones, delayed strikes, temporary blocks.
 * No entity registration needed; visuals come from the nusmp VFX payloads.
 */
public final class SpellRuntime {
    private SpellRuntime() {}

    // ---------------------------------------------------------------- bolts
    @FunctionalInterface public interface OnHit { void hit(Bolt bolt, LivingEntity target); }
    @FunctionalInterface public interface OnEnd { void end(Bolt bolt, Vec3 at); }

    public static final class Bolt {
        public final ServerLevel level;
        public final ServerPlayer owner;
        public Vec3 pos, vel;
        public final double radius;
        public int life;
        public final boolean pierce;
        public final LivingEntity homing;
        final OnHit onHit;
        final OnEnd onEnd;
        final Set<Entity> hit = new HashSet<>();
        boolean dead;

        Bolt(ServerPlayer owner, Vec3 pos, Vec3 vel, double radius, int life, boolean pierce, LivingEntity homing, OnHit onHit, OnEnd onEnd) {
            this.level = owner.serverLevel(); this.owner = owner; this.pos = pos; this.vel = vel; this.radius = radius;
            this.life = life; this.pierce = pierce; this.homing = homing; this.onHit = onHit; this.onEnd = onEnd;
        }
    }

    private static final List<Bolt> BOLTS = new ArrayList<>();

    /** Fire a bolt. vel = blocks per tick. homing may be null. */
    public static Bolt bolt(ServerPlayer owner, Vec3 start, Vec3 vel, double radius, int life, boolean pierce,
                            LivingEntity homing, OnHit onHit, OnEnd onEnd) {
        Bolt b = new Bolt(owner, start, vel, radius, life, pierce, homing, onHit, onEnd);
        BOLTS.add(b);
        return b;
    }

    private static void tickBolt(Bolt b) {
        if (b.homing != null && b.homing.isAlive()) {
            Vec3 want = b.homing.getBoundingBox().getCenter().subtract(b.pos).normalize().scale(b.vel.length());
            b.vel = b.vel.scale(0.75).add(want.scale(0.25));
        }
        Vec3 next = b.pos.add(b.vel);
        HitResult wall = b.level.clip(new ClipContext(b.pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, b.owner));
        if (wall.getType() != HitResult.Type.MISS) next = wall.getLocation();
        for (LivingEntity e : b.level.getEntitiesOfClass(LivingEntity.class, new AABB(b.pos, next).inflate(b.radius),
                e -> e != b.owner && e.isAlive() && !e.isSpectator() && !e.isAlliedTo(b.owner))) {
            if (!b.hit.add(e)) continue;
            b.onHit.hit(b, e);
            if (!b.pierce) { end(b, e.getBoundingBox().getCenter()); return; }
        }
        b.pos = next;
        if (wall.getType() != HitResult.Type.MISS || --b.life <= 0) end(b, b.pos);
    }

    private static void end(Bolt b, Vec3 at) {
        if (b.dead) return;
        b.dead = true;
        if (b.onEnd != null) b.onEnd.end(b, at);
    }

    // ---------------------------------------------------------------- zones & timers
    private record Zone(ServerLevel level, long until, int interval, IntConsumer step, long[] age) {}
    private record Later(ServerLevel level, long at, Runnable run) {}
    private record Temp(ServerLevel level, BlockPos pos, BlockState state, long until) {}

    private static final List<Zone> ZONES = new ArrayList<>();
    private static final List<Later> LATER = new ArrayList<>();
    private static final List<Temp> TEMP = new ArrayList<>();

    /** Runs step(age) every 'interval' ticks for 'ticks' ticks. */
    public static void zone(ServerLevel level, int ticks, int interval, IntConsumer step) {
        ZONES.add(new Zone(level, level.getGameTime() + ticks, Math.max(1, interval), step, new long[]{0}));
    }

    public static void later(ServerLevel level, int delay, Runnable run) {
        LATER.add(new Later(level, level.getGameTime() + delay, run));
    }

    /** Places a block in an air/replaceable spot and removes it after 'ticks'. */
    public static boolean tempBlock(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        BlockState now = level.getBlockState(pos);
        if (!now.isAir() && !now.canBeReplaced()) return false;
        level.setBlock(pos, state, 3);
        TEMP.add(new Temp(level, pos.immutable(), state, level.getGameTime() + ticks));
        return true;
    }

    // ---------------------------------------------------------------- tick
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Bolt b : new ArrayList<>(BOLTS)) if (!b.dead) tickBolt(b);
        BOLTS.removeIf(b -> b.dead);

        List<Zone> zs = new ArrayList<>(ZONES);
        ZONES.clear();
        for (Zone z : zs) {
            long now = z.level().getGameTime();
            if (now > z.until()) continue;
            if (z.age()[0] % z.interval() == 0) z.step().accept((int) z.age()[0]);
            z.age()[0]++;
            ZONES.add(z);
        }

        List<Later> due = new ArrayList<>();
        LATER.removeIf(l -> { if (l.level().getGameTime() >= l.at()) { due.add(l); return true; } return false; });
        due.forEach(l -> l.run().run());

        TEMP.removeIf(t -> {
            if (t.level().getGameTime() < t.until()) return false;
            if (t.level().getBlockState(t.pos()).is(t.state().getBlock())) t.level().removeBlock(t.pos(), false);
            return true;
        });
    }
}
