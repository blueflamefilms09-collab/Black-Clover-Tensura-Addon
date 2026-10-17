package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.sds.kit.FourSinsKit;
import com.newuniverse.nusmp.arc.sds.kit.FourSinsMath;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.boss.vfx.BossMoveVfx;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Why: the parts of Diane's moves that touch the world and need nothing of the boss's own state (the hammer's blows, the rocks, the pillars of Rising Meteor, Queen
 * Embrace's wave of earth, the rings of Drole's Dance, the quake of a growth), kept apart from {@link DianeBoss} so that class can stay about the fight's rules (her size,
 * the metal, the shield, the dance). The stone effects are the Goliath's own ({@code BossMoveVfx}: a telegraph form and a strike form of the same ring, column, trail and
 * crack), the rings and the wall are timed by {@link WaveMath} so what is drawn is what hurts, and every blow goes through {@code boss.strike} (the Zogratis damage law with
 * its per-hit cap). Nothing here holds a player (Diane has no control move at all: her weight is her answer), changes a block or spawns an entity.
 */
final class DianeMoves {
    private DianeMoves() {}

    static final int RGB = DianeSheet.RGB, ACCENT = DianeSheet.ACCENT;
    /** The heat of the stone's cracks (amber; the Gates make it red), the dust of a ring, the colour of a thrown rock, a warning. */
    static final int AMBER = 0xFFAA33, HOT = 0xFF5A2A, DUST = 0xB4A48C, ROCK = 0x8A7A5A, WARN = 0xFF5A7A;
    private static final String WHO = "Diane";

    static IntConsumer safe(String what, IntConsumer step) { return FourSinsKit.safe(WHO, what, step); }

    static Runnable safe(String what, Runnable task) { return FourSinsKit.safe(WHO, what, task); }

    static int tint(boolean gates) { return gates ? HOT : AMBER; }

    // ================================================================ Gideon
    /** The hammer comes down on a marked ring: a column and a ring of stone, and everyone inside is struck and thrown outwards. Returns how many were struck. */
    static int smash(DianeBoss b, Vec3 at, boolean gates) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        BossMoveVfx.strike(sl, VfxShape.BOSS_STONE_COLUMN, at, at.add(0, 1, 0), tint(gates), DianeTable.SMASH_RADIUS * 0.5);
        BossMoveVfx.strike(sl, VfxShape.BOSS_STONE_RING, at, at.add(0, 1, 0), tint(gates), DianeTable.SMASH_RADIUS + 1.0);
        FourSinsKit.sound(sl, at, SoundEvents.ANVIL_LAND, 2.2f, 0.5f);
        int n = 0;
        for (LivingEntity e : FourSinsKit.inDisc(b, at, DianeTable.SMASH_RADIUS, 2, 4)) {
            if (b.strike(e, DianeTable.SMASH, false)) {
                n++;
                e.knockback(1.0, at.x - e.getX(), at.z - e.getZ());
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.2, 0));
                e.hurtMarked = true;
            }
        }
        return n;
    }

    /** The hammer sweeps: a crack along the ground and a cloud of stone dust, and everyone in the wide cone is struck and thrown along it. Returns how many were struck. */
    static int sweep(DianeBoss b, Vec3 tip, Vec3 dir, boolean gates) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        Vec3 far = tip.add(dir.x * DianeTable.SWEEP_RANGE, 0, dir.z * DianeTable.SWEEP_RANGE);
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_STONE_FISSURE, tip.add(0, 0.1, 0), far.add(0, 0.1, 0), tint(gates), 24, 1.6);
        Tells.burst(sl, tip.add(dir.scale(DianeTable.SWEEP_RANGE * 0.6)).add(0, 0.8, 0), DUST, 30);
        FourSinsKit.sound(sl, tip, SoundEvents.PLAYER_ATTACK_SWEEP, 2.0f, 0.5f);
        int n = 0;
        for (LivingEntity e : FourSinsKit.inCone(b, tip, dir, DianeTable.SWEEP_RANGE, DianeTable.SWEEP_HALF)) {
            if (b.strike(e, DianeTable.SWEEP, false)) {
                n++;
                e.knockback(1.1, -dir.x, -dir.z);
                e.hurtMarked = true;
            }
        }
        return n;
    }

    /** The small hammer's swing at human size: a cone in front, struck and pushed back. Returns how many were struck. */
    static int swing(DianeBoss b, Vec3 tip, Vec3 dir) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        Tells.burst(sl, tip.add(dir.scale(DianeTable.SWING_RANGE * 0.5)).add(0, 1.0, 0), DUST, 14);
        FourSinsKit.sound(sl, tip, SoundEvents.PLAYER_ATTACK_STRONG, 1.5f, 0.6f);
        int n = 0;
        for (LivingEntity e : FourSinsKit.inCone(b, tip, dir, DianeTable.SWING_RANGE, DianeTable.SWING_HALF)) {
            if (b.strike(e, DianeTable.SWING, false)) {
                n++;
                e.knockback(0.9, -dir.x, -dir.z);
                e.hurtMarked = true;
            }
        }
        return n;
    }

    // ================================================================ Rush Rock
    /** {@code count} rocks in a fan round {@code aim}, two ticks apart: straight, slow, each hurting the first foe it touches. */
    static void rocks(DianeBoss b, Vec3 aim, int count, float damage) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 axis = aim == null || aim.lengthSqr() < 1e-4 ? b.getViewVector(1f) : aim.normalize();
        int n = Math.max(1, count);
        double[] fan = FourSinsMath.fan(n, Math.min(1.0, 0.14 * n));
        for (int i = 0; i < n; i++) {
            final double offset = fan[i];
            Runnable launch = () -> {
                Vec3 dir = FourSinsKit.rotateY(axis, offset);
                Vec3 origin = b.position().add(0, b.getBbHeight() * 0.7, 0);
                new FourSinsKit.Bolt(b, origin.add(dir.scale(0.8)), dir, 0.95, 38, 0.8, 0.0, null, ROCK, e -> {
                    if (b.strike(e, damage, false)) e.knockback(0.3, b.getX() - e.getX(), b.getZ() - e.getZ());
                }).launch(sl);
                FourSinsKit.sound(sl, b.position(), SoundEvents.STONE_BREAK, 1.0f, 0.8f);
            };
            if (i == 0) launch.run();
            else b.later(i * 2, safe("rock", launch));
        }
    }

    // ================================================================ Rising Meteor
    /** One star-shaped pillar rises under a marked ring: everyone inside who has not been struck by an earlier pillar of this volley is struck and thrown up. */
    static void pillar(DianeBoss b, Vec3 at, Set<Integer> struck, boolean gates) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        BossMoveVfx.strike(sl, VfxShape.BOSS_STONE_COLUMN, at, at.add(0, 1, 0), tint(gates), DianeTable.METEOR_RADIUS * 0.6);
        BossMoveVfx.strike(sl, VfxShape.BOSS_STONE_RING, at, at.add(0, 1, 0), tint(gates), DianeTable.METEOR_RADIUS + 0.6);
        FourSinsKit.sound(sl, at, SoundEvents.STONE_PLACE, 1.6f, 0.5f);
        for (LivingEntity e : FourSinsKit.inDisc(b, at, DianeTable.METEOR_RADIUS, 2, 4)) {
            if (!struck.add(e.getId())) continue;
            if (b.strike(e, DianeTable.METEOR, false)) {
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.7, 0));
                e.hurtMarked = true;
            }
        }
    }

    // ================================================================ Queen Embrace
    /**
     * A wave of earth runs down the lane from {@code origin} along {@code dir}: a block's width of wall at the front, {@link DianeTable#QUEEN_LENGTH} blocks of lane, and
     * everyone it passes is struck once and thrown along it. Timed by {@link WaveMath}.
     */
    static void queenEmbrace(DianeBoss b, Vec3 origin, Vec3 dir, boolean gates) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 flat = dir.multiply(1, 0, 1);
        if (flat.lengthSqr() < 1e-6) return;
        Vec3 d = flat.normalize();
        int ticks = WaveMath.ticksToReach(DianeTable.QUEEN_LENGTH, DianeTable.QUEEN_SPEED) + 2;
        VfxSpawn.send(sl, VfxShape.EARTH_RAMPAGE, origin, origin.add(d.scale(DianeTable.QUEEN_LENGTH)), 0xFF000000 | tint(gates), ticks, 1.0f);
        FourSinsKit.sound(sl, origin, SoundEvents.GENERIC_EXPLODE.value(), 2.0f, 0.4f);
        Set<Integer> struck = new HashSet<>();
        SpellRuntime.zone(sl, ticks, 1, safe("queen embrace", age -> {
            double before = WaveMath.front(age, DianeTable.QUEEN_SPEED), after = WaveMath.front(age + 1, DianeTable.QUEEN_SPEED);
            if (before > DianeTable.QUEEN_LENGTH + 2) return;
            Vec3 front = origin.add(d.scale(after));
            Tells.gather(sl, front.add(0, 0.6, 0), DianeTable.QUEEN_HALF * 0.8, DUST, 6);
            for (LivingEntity e : b.foesAround(front, DianeTable.QUEEN_HALF + 4)) {
                if (struck.contains(e.getId()) || Math.abs(e.getY() - origin.y) > 4) continue;
                double rx = e.getX() - origin.x, rz = e.getZ() - origin.z;
                double along = rx * d.x + rz * d.z, across = rx * d.z - rz * d.x;
                if (along > DianeTable.QUEEN_LENGTH || !WaveMath.laneTouches(along, across, e.getBbWidth() / 2, DianeTable.QUEEN_HALF, before, after)) continue;
                struck.add(e.getId());
                if (b.strike(e, DianeTable.QUEEN, false)) {
                    e.knockback(1.2, -d.x, -d.z);
                    e.setDeltaMovement(e.getDeltaMovement().add(0, 0.4, 0));
                    e.hurtMarked = true;
                }
            }
        }));
    }

    /** The lane of a Queen Embrace in a telegraph: two lines the length of the lane, a ring at its end. */
    static void drawLane(ServerLevel sl, Vec3 origin, Vec3 dir, int rgb) {
        FourSinsKit.laneTell(sl, origin.add(0, 0.1, 0), dir, DianeTable.QUEEN_LENGTH, DianeTable.QUEEN_HALF, rgb);
    }

    // ================================================================ Drole's Dance
    /** One stomp of the dance: a ring of earth leaves her feet and runs out to the reach; a body on the ground it crosses is struck once, a body in the air is not. */
    static void stomp(DianeBoss b, Vec3 feet, boolean gates) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        int ticks = WaveMath.ticksToReach(DianeTable.DANCE_RADIUS, DianeTable.DANCE_SPEED) + 1;
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_STONE_RING, feet, feet.add(0, 1, 0), tint(gates), ticks, DianeTable.DANCE_RADIUS);
        FourSinsKit.sound(sl, feet, SoundEvents.ANVIL_LAND, 2.4f, 0.4f);
        Set<Integer> struck = new HashSet<>();
        SpellRuntime.zone(sl, ticks, 1, safe("dance ring", age -> {
            double before = WaveMath.front(age, DianeTable.DANCE_SPEED), after = WaveMath.front(age + 1, DianeTable.DANCE_SPEED);
            if (age % 2 == 0 && after > 0.5) Tells.ring(sl, feet.add(0, 0.12, 0), after, DUST, 1.2f, (int) Math.min(64, 12 + after * 5));
            for (LivingEntity e : b.foesAround(feet, DianeTable.DANCE_RADIUS + 3)) {
                if (struck.contains(e.getId()) || Math.abs(e.getY() - feet.y) > 5) continue;
                double d = Math.hypot(e.getX() - feet.x, e.getZ() - feet.z);
                if (!WaveMath.touches(d, e.getBbWidth() / 2, before, after)) continue;
                if (WaveMath.clears(FourSinsKit.floorHeight(sl, e), DianeTable.DANCE_RING_HEIGHT)) continue;      // a jump clears the ring
                struck.add(e.getId());
                if (b.strike(e, DianeTable.DANCE_WAVE, false)) e.knockback(0.5, feet.x - e.getX(), feet.z - e.getZ());
            }
        }));
    }

    /** The foot rises: a thin ring at the full reach of the wave and a ring that closes in as the stomp nears. */
    static void drawBeat(ServerLevel sl, Vec3 feet, int into, boolean first) {
        Tells.ring(sl, feet.add(0, 0.1, 0), DianeTable.DANCE_RADIUS, AMBER, 1.0f, 56);
        double closing = Math.max(1.0, DianeTable.DANCE_RADIUS * (1.0 - (double) into / Math.max(1, DianeTable.PREBEAT)));
        Tells.ring(sl, feet.add(0, 0.1, 0), closing, ACCENT, 1.1f, 32);
        if (first) Tells.burst(sl, feet.add(0, 1.0, 0), DUST, 10);
    }

    // ================================================================ the growth
    /** The quake of a growth: a ring of stone round her feet, and everyone within the reach is struck and thrown outwards. Returns how many were struck. */
    static int growQuake(DianeBoss b, Vec3 at, boolean gates) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        BossMoveVfx.strike(sl, VfxShape.BOSS_STONE_RING, at, at.add(0, 1, 0), tint(gates), DianeTable.GROW_RADIUS + 1.0);
        FourSinsKit.sound(sl, at, SoundEvents.GENERIC_EXPLODE.value(), 1.8f, 0.5f);
        int n = 0;
        for (LivingEntity e : FourSinsKit.inDisc(b, at, DianeTable.GROW_RADIUS, 2, 4)) {
            if (b.strike(e, DianeTable.GROW_QUAKE, false)) {
                n++;
                e.knockback(1.1, at.x - e.getX(), at.z - e.getZ());
                e.hurtMarked = true;
            }
        }
        return n;
    }
}
