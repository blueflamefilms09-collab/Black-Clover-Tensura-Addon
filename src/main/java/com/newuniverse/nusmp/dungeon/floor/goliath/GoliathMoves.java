package com.newuniverse.nusmp.dungeon.floor.goliath;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * The Goliath's moves in the world (the glue over {@link GoliathMath}): ring waves that cross the floor and are jumped, a smash on a marked
 * circle, a line of stone spikes, rubble that falls on circles, and the Bedrock Quake. Every effect is a zone or a delayed task of the spell
 * runtime that checks the boss is still alive, is bounded (a handful of rings, a few dozen foes), is shown first by the telegraph the boss draws
 * during its wind-up, and changes no block at all: the stone is particles and sound, the damage is zones. Hits go through the boss's own damage law
 * ({@code strike}: capped at the pvp share of a victim's health), and a move that hits twice or three times shares one {@link GoliathMath.DamageBudget}.
 */
final class GoliathMoves {
    private GoliathMoves() {}

    /** Stone-dust grey, the amber of the cracks, and the red of a heated crack (0xRRGGBB). */
    static final int DUST = 0xB8B4A4, AMBER = 0xFFAA33, HOT = 0xFF5A2E;
    private static final int MAX_VICTIMS = 24;

    /** The look of a blow on a victim: its base damage, how far it throws them up and out, and the slowness it leaves (ticks and level; 0 ticks for none). */
    record Blow(float damage, double lift, double knock, int slowTicks, int slowAmp) {}

    /**
     * Zone steps and delayed tasks run inside the server tick without a net under them, so every one of the Goliath's goes through here: a step that
     * throws is logged once and that zone or task stops, it never takes the tick down.
     */
    static IntConsumer safe(String what, IntConsumer step) {
        boolean[] dead = {false};
        return age -> {
            if (dead[0]) return;
            try {
                step.accept(age);
            } catch (Throwable t) {
                dead[0] = true;
                LogUtils.getLogger().warn("[nusmp] The Goliath's {} stopped: {}", what, t.toString());
            }
        };
    }

    /** {@link #safe(String, IntConsumer)} for a delayed task. */
    static Runnable safe(String what, Runnable task) {
        IntConsumer guarded = safe(what, age -> task.run());
        return () -> guarded.accept(0);
    }

    // ================================================================ the look of the stone
    private static BlockParticleOption chips() { return new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE_BRICKS.defaultBlockState()); }

    private static BlockParticleOption dustFall() { return new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.STONE_BRICKS.defaultBlockState()); }

    private static int ringPoints(double radius) { return (int) Math.max(14, Math.min(72, radius * 4)); }

    /** A ring of stone dust on the floor (the telegraph's line). */
    static void dustRing(ServerLevel sl, Vec3 centre, double radius, int rgb, float size) { Tells.ring(sl, centre, radius, rgb, size, ringPoints(radius)); }

    /** The edge of a wave: chips of stone thrown up along a ring on the floor. */
    static void waveRing(ServerLevel sl, Vec3 centre, double radius) {
        int n = (int) Math.max(16, Math.min(44, radius * 3));
        BlockParticleOption chip = chips();
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            sl.sendParticles(chip, centre.x + Math.cos(a) * radius, centre.y + 0.2, centre.z + Math.sin(a) * radius, 3, 0.25, 0.12, 0.25, 0.06);
        }
    }

    /** A burst of stone from a point: the impact of a fist, a stomp, a falling rock. */
    static void burst(ServerLevel sl, Vec3 at, int count, double spread) {
        sl.sendParticles(chips(), at.x, at.y + 0.3, at.z, count, spread, 0.3, spread, 0.12);
        sl.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.2, at.z, Math.max(4, count / 4), spread, 0.1, spread, 0.05);
    }

    static void sound(ServerLevel sl, Vec3 at, net.minecraft.sounds.SoundEvent ev, float volume, float pitch) {
        sl.playSound(null, BlockPos.containing(at), ev, SoundSource.HOSTILE, volume, pitch);
    }

    /** How far the feet of `e` are above the floor under them (0 if they stand on it or there is none close by). */
    static double floorHeight(ServerLevel sl, LivingEntity e) {
        if (e.onGround()) return 0;
        return Math.max(0, e.getY() - ZogratisBoss.ground(sl, e.position()).y);
    }

    // ================================================================ landing a blow
    /** Takes a gliding player out of the glide before they are thrown, so a shove into a wall can never turn into the game's kinetic-energy damage. */
    private static void grounded(LivingEntity e) {
        if (e instanceof Player p && p.isFallFlying()) p.stopFallFlying();
    }

    /** Throws a victim up and away from where the blow came from (the direction (dirX, dirZ) points from the blow to the victim) and slows them. */
    static void push(LivingEntity e, Blow b, double dirX, double dirZ) {
        grounded(e);
        if (b.knock() > 0) e.knockback(b.knock(), -dirX, -dirZ);
        if (b.lift() > 0) {
            Vec3 v = e.getDeltaMovement();
            e.setDeltaMovement(v.x, Math.max(v.y, b.lift()), v.z);
            e.hurtMarked = true;
        }
        e.fallDistance = 0;
        if (b.slowTicks() > 0) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, b.slowTicks(), b.slowAmp()));
    }

    /**
     * Hurts `e` with the blow through the boss's damage law and throws them. With a budget the hit is cut down so that the victim never loses
     * more than the budget's share of their maximum health to the move in all. False if nothing landed.
     */
    static boolean land(GoliathBoss boss, LivingEntity e, Blow blow, double dirX, double dirZ, GoliathMath.DamageBudget budget) {
        float base = blow.damage();
        double expected = 0.0;
        if (budget != null) {
            expected = boss.bossDamage(e, base);
            double scale = budget.allow(e.getId(), e.getMaxHealth(), expected);
            if (scale <= 0.0) return false;
            base = (float) (base * scale);
            expected *= scale;
        }
        if (!boss.strike(e, base, false)) return false;
        if (budget != null) budget.spend(e.getId(), expected);
        push(e, blow, dirX, dirZ);
        return true;
    }

    // ================================================================ a ring wave: the slam, the stomp, the roar, the quake
    /**
     * A ring that runs out from `centre` at `speed` blocks a tick to `reach`, passing every foe once: whoever it passes on the floor takes the blow
     * unless they have jumped it (feet more than 0.75 off the floor) or stand above or below its plane (a gallery, a heap). The ring is drawn in
     * chips of stone along its edge. `budget` (or null) is shared with the other rings of the same move.
     */
    static void ringWave(GoliathBoss boss, Vec3 centre, double reach, double speed, Blow blow, GoliathMath.DamageBudget budget) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        final Set<Integer> hit = new HashSet<>();
        final int ticks = GoliathMath.waveTicks(reach, speed);
        burst(sl, centre, 24, 0.9);
        GoliathVfx.ringStrike(boss, centre, reach, speed);
        VfxSpawn.send(sl, VfxShape.EARTH_RISE, centre.add(-1.5, 0, 0), centre.add(1.5, 0, 0), 0xFFFFB45A, 24, (float) Math.min(2.0, Math.max(1.0, reach / 4)));
        sound(sl, centre, SoundEvents.STONE_BREAK, 2.0f, 0.4f);
        SpellRuntime.zone(sl, ticks, 1, safe("ring wave", age -> {
            if (!boss.isAlive() || boss.isRemoved()) return;
            double wr = GoliathMath.waveRadius(age, speed);
            if (age % 2 == 0) waveRing(sl, centre, Math.min(wr, reach));
            int seen = 0;
            for (LivingEntity e : boss.foesAround(centre, reach + 3)) {
                if (hit.contains(e.getId())) continue;
                if (++seen > MAX_VICTIMS) break;
                double dx = e.getX() - centre.x, dz = e.getZ() - centre.z, dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > reach + e.getBbWidth() / 2 || !GoliathMath.inBand(e.getY() - centre.y) || !GoliathMath.waveHits(dist, wr)) continue;
                if (GoliathMath.jumpsWave(floorHeight(sl, e))) {
                    if (e instanceof net.minecraft.server.level.ServerPlayer sp && !hit.contains(-e.getId() - 1)) {
                        hit.add(-e.getId() - 1);
                        sp.displayClientMessage(net.minecraft.network.chat.Component.literal("You jumped the wave."), true);
                    }
                    continue;
                }
                hit.add(e.getId());
                double d = Math.max(1e-3, dist);
                land(boss, e, blow, dx / d, dz / d, budget);
            }
        }));
    }

    /** The Ground Slam: a ring of `reach` blocks from the giant's feet that throws up whoever it catches and slows them. */
    static void groundSlam(GoliathBoss boss, double reach, float damage) {
        boss.swing(InteractionHand.MAIN_HAND);
        ringWave(boss, boss.position(), reach, GoliathTable.WAVE_SPEED, new Blow(damage, 0.55, 0.5, 40, 0), null);
    }

    /** The Twin Stomp: two rings 12 ticks apart (the second lighter), one budget between them. */
    static void twinStomp(GoliathBoss boss, double reach, float first, float second) {
        GoliathMath.DamageBudget budget = new GoliathMath.DamageBudget(GoliathTable.STOMP_BUDGET);
        Vec3 at = boss.position();
        ringWave(boss, at, reach, GoliathTable.WAVE_SPEED, new Blow(first, 0.5, 0.4, 30, 0), budget);
        boss.later(GoliathTable.STOMP_GAP, safe("second stomp", () -> {
            if (!(boss.level() instanceof ServerLevel)) return;
            boss.swing(InteractionHand.OFF_HAND);
            ringWave(boss, boss.position(), reach, GoliathTable.WAVE_SPEED, new Blow(second, 0.45, 0.4, 30, 0), budget);
        }));
        if (GoliathMath.stompRings(boss.gatesOpen()) > 2) boss.later(2 * GoliathTable.STOMP_GAP, safe("third stomp", () -> {      // the Gates: a third ring, the same budget
            if (!(boss.level() instanceof ServerLevel)) return;
            boss.swing(InteractionHand.MAIN_HAND);
            ringWave(boss, boss.position(), reach, GoliathTable.WAVE_SPEED, new Blow(GoliathTable.STOMP_THIRD, 0.4, 0.4, 30, 0), budget);
        }));
    }

    /** The Roaring Shockwave: a ring that crosses the whole pit, thin enough to be jumped; whoever it catches is slowed for three seconds. */
    static void shockwave(GoliathBoss boss, double reach, float damage) {
        if (boss.level() instanceof ServerLevel sl) sound(sl, boss.position(), SoundEvents.RAVAGER_ROAR, 3.0f, 0.5f);
        ringWave(boss, boss.position(), reach, GoliathTable.WAVE_SPEED, new Blow(damage, 0.3, 0.4, 60, 1), null);
    }

    /** The Bedrock Quake: three rings from the giant, 14 ticks apart, each small, all sharing one budget so that a run of misses can never be lethal. */
    static void bedrockQuake(GoliathBoss boss, double reach, float damage) {
        GoliathMath.DamageBudget budget = new GoliathMath.DamageBudget(GoliathTable.QUAKE_BUDGET);
        Vec3 at = boss.position();
        if (boss.level() instanceof ServerLevel sl) {
            sound(sl, at, SoundEvents.WARDEN_ROAR, 4.0f, 0.4f);
            sound(sl, at, SoundEvents.ANVIL_LAND, 3.0f, 0.3f);
        }
        for (int i = 0; i < 3; i++) {
            final int ring = i;
            Runnable go = safe("quake ring", () -> {
                if (!(boss.level() instanceof ServerLevel sl)) return;
                boss.swing(ring % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
                ringWave(boss, at, reach, GoliathTable.QUAKE_SPEED, new Blow(damage, 0.45, 0.4, 20, 0), budget);
                sound(sl, at, SoundEvents.STONE_BREAK, 2.4f, 0.3f + 0.1f * ring);
            });
            if (i == 0) go.run();
            else boss.later(i * GoliathTable.QUAKE_GAP, go);
        }
    }

    // ================================================================ the Bedrock Fist
    /** The fist comes down on `mark`: everyone within `radius` of it takes the heavy blow, is thrown up and slowed; a cloud of stone rises. */
    static void bedrockFist(GoliathBoss boss, Vec3 mark, double radius, float damage) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        boss.swing(InteractionHand.MAIN_HAND);
        burst(sl, mark, 60, radius * 0.5);
        GoliathVfx.fistStrike(boss, mark, radius);
        VfxSpawn.send(sl, VfxShape.EARTH_RISE, mark.add(-1.5, 0, 0), mark.add(1.5, 0, 0), 0xFFFFB45A, 20, (float) Math.min(2.0, Math.max(1.0, radius / 2.0)));
        sound(sl, mark, SoundEvents.ANVIL_LAND, 3.0f, 0.4f);
        sound(sl, mark, SoundEvents.STONE_BREAK, 2.5f, 0.35f);
        Blow blow = new Blow(damage, 0.7, 0.8, 30, 2);
        int seen = 0;
        for (LivingEntity e : boss.foesAround(mark, radius + 4)) {
            if (++seen > MAX_VICTIMS) break;
            double dx = e.getX() - mark.x, dz = e.getZ() - mark.z, dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > radius + e.getBbWidth() / 2 || e.getY() - mark.y < -2.0 || e.getY() - mark.y > 4.0) continue;
            double d = Math.max(1e-3, dist);
            land(boss, e, blow, dx / d, dz / d, null);
        }
        if (boss.gatesOpen()) boss.later(GoliathTable.AFTERSHOCK_DELAY, safe("fist aftershock", () ->      // the Gates: the ground rings once more round the fist (drawn in the wind-up)
                ringWave(boss, mark, GoliathTable.AFTERSHOCK_REACH, GoliathTable.WAVE_SPEED, new Blow(GoliathTable.AFTERSHOCK, 0.3, 0.3, 20, 0), null)));
    }

    // ================================================================ the Seismic Line
    /**
     * A line of stone spikes along the floor from `from` in the direction `dir` (unit, flat) for `length` blocks: one stretch rises every two ticks
     * (a block a tick), and whoever is standing on a stretch when it rises takes the blow and is thrown up. Step off the line and it misses.
     */
    static void seismicLine(GoliathBoss boss, Vec3 from, Vec3 dir, double length, float damage) { seismicLine(boss, from, dir, length, damage, new HashSet<>()); }

    /** The two ends of a line turned by 'yaw' radians about its start (1.35.0: the fork of the Gates). */
    static Vec3[] rotatedEnds(Vec3 start, Vec3 end, double yaw) {
        double[] r = GoliathMath.rotate(end.x - start.x, end.z - start.z, yaw);
        return new Vec3[]{start, new Vec3(start.x + r[0], end.y, start.z + r[1])};
    }

    /** As above with a hit list shared with the other lines of the same move, so that the fork can never strike one foe twice. */
    static void seismicLine(GoliathBoss boss, Vec3 from, Vec3 dir, double length, float damage, Set<Integer> hit) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        Vec3 to = from.add(dir.scale(length));
        VfxSpawn.send(sl, VfxShape.EARTH_FISSURE, from, to, 0xFFFFFFFF, 44, 1.0f);              // the crack runs ahead of the spikes
        GoliathVfx.lineStrike(boss, from, to);
        sound(sl, from, SoundEvents.STONE_BREAK, 2.0f, 0.4f);
        Blow blow = new Blow(damage, 0.6, 0.3, 0, 0);
        for (int k = 1; k <= GoliathMath.stretches(length); k++) {
            final double along = GoliathMath.stretchAlong(k);
            boss.later(Math.max(0, GoliathMath.stretchDelay(k) - 3), safe("line spikes", () -> {         // the spikes of this stretch are up when its blow lands
                if (!(boss.level() instanceof ServerLevel level)) return;
                for (double side : new double[]{-0.9, 0.9}) {                                          // two rows, so the spikes fill the width the blow covers
                    Vec3 lat = new Vec3(-dir.z, 0, dir.x).scale(side);
                    VfxSpawn.send(level, VfxShape.STONE_SPIKES, from.add(dir.scale(along - 1.5)).add(lat), from.add(dir.scale(along + 0.5)).add(lat), 0, 20, 1.3f);
                }
            }));
            boss.later(GoliathMath.stretchDelay(k), safe("seismic line", () -> {
                if (!(boss.level() instanceof ServerLevel level)) return;
                Vec3 p = from.add(dir.scale(along));
                burst(level, p, 14, 1.5);
                level.sendParticles(chips(), p.x, p.y + 1.0, p.z, 10, 1.4, 0.9, 1.4, 0.15);
                int seen = 0;
                for (LivingEntity e : boss.foesAround(p, 3.0)) {
                    if (hit.contains(e.getId()) || ++seen > MAX_VICTIMS) continue;
                    double[] f = GoliathMath.lineFrame(from.x, from.z, dir.x, dir.z, e.getX(), e.getZ());
                    double dy = e.getY() - p.y;
                    if (!GoliathMath.stretchHits(f[0], f[1], along, GoliathTable.LINE_HALF_WIDTH) || dy > 3.0 || dy < -4.0) continue;
                    hit.add(e.getId());
                    land(boss, e, blow, dir.x, dir.z, null);
                }
            }));
        }
    }

    // ================================================================ the Rubble Fall
    /**
     * Rubble falls on each of the circles the wind-up showed: for {@link GoliathTable#RUBBLE_LAND} ticks the stones fall (dust and chips from above), then
     * each circle takes its blow; whoever stands in one is hurt once however many circles they stand in, up on a gallery included (the rock comes from the
     * ceiling), and slowed for a moment under the rubble.
     */
    static void rubbleFall(GoliathBoss boss, List<Vec3> spots, double radius, float damage) {
        if (!(boss.level() instanceof ServerLevel sl) || spots.isEmpty()) return;
        sound(sl, boss.position(), SoundEvents.STONE_BREAK, 3.0f, 0.3f);
        for (Vec3 s : spots) GoliathVfx.rubbleStrike(boss, s, radius);
        SpellRuntime.zone(sl, GoliathTable.RUBBLE_LAND, 2, safe("falling rubble", age -> {
            if (!boss.isAlive() || boss.isRemoved()) return;
            for (Vec3 s : spots) sl.sendParticles(dustFall(), s.x, s.y + 7.0, s.z, 10, radius * 0.5, 1.5, radius * 0.5, 0.0);
        }));
        boss.later(GoliathTable.RUBBLE_LAND, safe("rubble landing", () -> {
            if (!(boss.level() instanceof ServerLevel level)) return;
            Set<Integer> hit = new HashSet<>();
            Blow blow = new Blow(damage, 0.25, 0.3, 30, 0);
            for (Vec3 s : spots) {
                burst(level, s, 36, radius * 0.45);
                sound(level, s, SoundEvents.ANVIL_LAND, 1.4f, 0.6f);
                int seen = 0;
                for (LivingEntity e : boss.foesAround(s.add(0, 5, 0), Math.sqrt(radius * radius + 64) + 1)) {
                    if (hit.contains(e.getId()) || ++seen > MAX_VICTIMS) continue;
                    double dx = e.getX() - s.x, dz = e.getZ() - s.z, dist = Math.sqrt(dx * dx + dz * dz), dy = e.getY() - s.y;
                    if (dist > radius + e.getBbWidth() / 2 || dy < -1.5 || dy > 12.0) continue;
                    hit.add(e.getId());
                    double d = Math.max(1e-3, dist);
                    land(boss, e, blow, dx / d, dz / d, null);
                }
            }
        }));
    }
}
