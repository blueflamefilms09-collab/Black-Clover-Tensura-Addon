package com.newuniverse.nusmp.dungeon.floor.charybdis;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.DevilStyle;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.VolleyBudget;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.function.IntConsumer;

/**
 * What the Charybdis Spawn does ({@link CharybdisTable} has the numbers, {@link CharybdisMath} the geometry): for each move its telegraph and its
 * code. Every telegraph is drawn from the first tick of the wind-up in teal and goes red for the last third, and everything that lands is placed
 * where the wind-up drew it (the point or the line is locked in the boss when the wind-up begins, so moving is always the answer). Hits go through
 * the boss's damage law ({@link ZogratisBoss#strike}); the whirlpool moves foes by velocity only; nothing here changes a block.
 */
final class CharybdisMoves {
    private CharybdisMoves() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    static final int TEAL = 0x2FD6C8, GLOW = 0x8DF6FF, WARN = 0xFF5A4A;

    /** Zone steps and delayed tasks run inside the server tick without a net under them: one that throws is logged once and stops. */
    static IntConsumer safe(String what, IntConsumer step) {
        boolean[] dead = {false};
        return age -> {
            if (dead[0]) return;
            try {
                step.accept(age);
            } catch (Throwable t) {
                dead[0] = true;
                LOG.warn("[nusmp] The Charybdis Spawn's {} stopped: {}", what, t.toString());
            }
        };
    }

    static Runnable safe(String what, Runnable task) {
        IntConsumer guarded = safe(what, age -> task.run());
        return () -> guarded.accept(0);
    }

    private static CharybdisSpawnBoss of(ZogratisBoss b) { return (CharybdisSpawnBoss) b; }

    /** The first call of a wind-up (the tell runs every third tick, so the first is within three ticks of the start). */
    private static boolean first(int left, int total) { return left >= total - 3; }

    private static int colour(int left, int total) { return left * 3 < total ? WARN : TEAL; }

    // ================================================================ telegraphs
    static Move.MoveTell tellOf(String id) {
        return switch (id) {
            case CharybdisTable.SCALE_VOLLEY -> (b, t, left, total) -> tellVolley(of(b), t, left, total);
            case CharybdisTable.TENTACLE_SWEEP -> (b, t, left, total) -> tellSweep(of(b), t, left, total);
            case CharybdisTable.WHIRLPOOL -> (b, t, left, total) -> tellWhirlpool(of(b), t, left, total);
            case CharybdisTable.BEAK_LUNGE -> (b, t, left, total) -> tellLunge(of(b), t, left, total);
            case CharybdisTable.SUMMON -> (b, t, left, total) -> tellSummon(of(b), t, left, total);
            case CharybdisTable.TIDAL_CRUSH -> (b, t, left, total) -> tellCrush(of(b), t, left, total);
            case CharybdisTable.MAELSTROM -> (b, t, left, total) -> tellMaelstrom(of(b), left, total);
            default -> null;
        };
    }

    /** Lines along the edges and the middle of the fan, and scales gathering at its mouth. */
    private static void tellVolley(CharybdisSpawnBoss boss, LivingEntity target, int left, int total) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        Vec3 from = mouth(boss), aim = target.getBoundingBox().getCenter().subtract(from);
        if (aim.lengthSqr() < 1e-4) return;
        int n = CharybdisTable.fanCount(boss.power());
        double[] fan = CharybdisMath.fan(n);
        double reach = Math.min(26, aim.length() + 5);
        int rgb = colour(left, total);
        for (int idx : new int[]{0, (n - 1) / 2, n - 1}) {
            double[] d = CharybdisMath.aim(aim.x, aim.y, aim.z, fan[idx], CharybdisMath.fanPitch(idx, n));
            Tells.line(sl, from, from.add(d[0] * reach, d[1] * reach, d[2] * reach), rgb);
        }
        Tells.gather(sl, from, 0.7, GLOW, 6);
        if (first(left, total)) sl.playSound(null, boss.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.6f, 1.4f);
    }

    /** The arc is locked toward the target when the wind-up begins and drawn from the boss's middle: both edges and the sweep itself. */
    private static void tellSweep(CharybdisSpawnBoss boss, LivingEntity target, int left, int total) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        Vec3 centre = boss.position().add(0, boss.getBbHeight() / 2, 0);
        if (first(left, total) || !boss.isLocked(CharybdisTable.TENTACLE_SWEEP)) {
            Vec3 flat = target.position().subtract(boss.position()).multiply(1, 0, 1);
            boss.lock(CharybdisTable.TENTACLE_SWEEP, null, flat.lengthSqr() < 1e-4 ? boss.facing() : flat.normalize());
        }
        Vec3 dir = boss.lockedDir();
        if (dir == null) return;
        double yaw = Math.atan2(dir.z, dir.x), half = CharybdisMath.SWEEP_HALF_ARC, reach = CharybdisMath.SWEEP_REACH;
        int rgb = colour(left, total);
        for (double s : new double[]{-half, half}) Tells.line(sl, centre, centre.add(Math.cos(yaw + s) * reach, 0, Math.sin(yaw + s) * reach), rgb);
        DustParticleOptions dust = dust(rgb, 1.2f);
        int points = 22;
        for (int i = 0; i <= points; i++) {
            double a = yaw - half + 2 * half * i / points;
            sl.sendParticles(dust, centre.x + Math.cos(a) * reach, centre.y, centre.z + Math.sin(a) * reach, 1, 0, 0, 0, 0);
        }
        Tells.gather(sl, centre, 1.2, GLOW, 4);
    }

    /** The ring is locked on the target's feet when the wind-up begins: the pull reaches this far and no further. */
    private static void tellWhirlpool(CharybdisSpawnBoss boss, LivingEntity target, int left, int total) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        if (first(left, total) || !boss.isLocked(CharybdisTable.WHIRLPOOL)) boss.lock(CharybdisTable.WHIRLPOOL, target.position(), null);
        Vec3 c = boss.peekPoint(CharybdisTable.WHIRLPOOL);
        if (c == null) return;
        int rgb = colour(left, total);
        Tells.ring(sl, c, CharybdisMath.WHIRL_RADIUS, rgb, 1.3f, 44);
        Tells.ring(sl, c, Math.max(0.8, CharybdisMath.WHIRL_RADIUS * left / Math.max(1, total)), GLOW, 1.0f, 24);
        sl.sendParticles(ParticleTypes.CURRENT_DOWN, c.x, c.y + 1.0, c.z, 6, CharybdisMath.WHIRL_RADIUS * 0.5, 0.8, CharybdisMath.WHIRL_RADIUS * 0.5, 0.02);
    }

    /** Two lines the width of the dive from the boss along the way it will go, to the first wall. */
    private static void tellLunge(CharybdisSpawnBoss boss, LivingEntity target, int left, int total) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        Vec3 from = boss.position().add(0, boss.getBbHeight() / 2, 0);
        if (first(left, total) || !boss.isLocked(CharybdisTable.BEAK_LUNGE)) {
            Vec3 aim = target.getBoundingBox().getCenter().subtract(from);
            boss.lock(CharybdisTable.BEAK_LUNGE, null, aim.lengthSqr() < 1e-4 ? boss.getViewVector(1f) : aim.normalize());
        }
        Vec3 dir = boss.lockedDir();
        if (dir == null) return;
        Vec3 end = rayEnd(sl, boss, from, dir, Swim.lungeLength() + 1.5);
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        side = side.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : side.normalize().scale(1.4);
        int rgb = colour(left, total);
        Tells.line(sl, from.add(side), end.add(side), rgb);
        Tells.line(sl, from.subtract(side), end.subtract(side), rgb);
        Tells.gather(sl, from, 1.0, GLOW, 5);
    }

    /** Rings on the wall where the megalodons will tear loose, chosen when the wind-up begins. */
    private static void tellSummon(CharybdisSpawnBoss boss, LivingEntity target, int left, int total) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        if (first(left, total) || !boss.isLocked(CharybdisTable.SUMMON)) boss.lockSpots(CharybdisMinions.wallSpots(boss, target));
        for (Vec3 s : boss.peekSpots()) {
            Tells.ring(sl, s, 1.6, colour(left, total), 1.3f, 18);
            sl.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, s.x, s.y, s.z, 5, 0.5, 0.8, 0.5, 0.05);
        }
        Tells.gather(sl, boss.position().add(0, boss.getBbHeight() / 2, 0), 1.4, TEAL, 6);
        if (first(left, total)) sl.playSound(null, boss.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0f, 0.6f);
    }

    /** The circle on the floor under the target, locked when the wind-up begins: a ring that closes in as the slam comes. */
    private static void tellCrush(CharybdisSpawnBoss boss, LivingEntity target, int left, int total) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        if (first(left, total) || !boss.isLocked(CharybdisTable.TIDAL_CRUSH)) boss.lock(CharybdisTable.TIDAL_CRUSH, floorUnder(sl, target.position()), null);
        Vec3 c = boss.peekPoint(CharybdisTable.TIDAL_CRUSH);
        if (c == null) return;
        int rgb = colour(left, total);
        Tells.ring(sl, c, CharybdisMath.CRUSH_RADIUS, rgb, 1.5f, 40);
        Tells.ring(sl, c, Math.max(0.6, CharybdisMath.CRUSH_RADIUS * left / Math.max(1, total)), WARN, 1.1f, 22);
        Tells.column(sl, c, 6, rgb);
        if (first(left, total)) sl.playSound(null, BlockPos.containing(c), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0f, 0.5f);
    }

    private static void tellMaelstrom(CharybdisSpawnBoss boss, int left, int total) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        Vec3 c = boss.position();
        Tells.ring(sl, c, CharybdisMath.MAELSTROM_RADIUS, colour(left, total), 1.4f, 52);
        Tells.ring(sl, c, Math.min(14, 2 + (total - left) * 0.2), GLOW, 1.0f, 32);
        Tells.gather(sl, c.add(0, boss.getBbHeight() / 2, 0), 2.0, GLOW, 10);
        sl.sendParticles(ParticleTypes.CURRENT_DOWN, c.x, c.y + 1.5, c.z, 10, CharybdisMath.MAELSTROM_RADIUS * 0.6, 1.0, CharybdisMath.MAELSTROM_RADIUS * 0.6, 0.02);
        if (first(left, total)) {
            boss.announce(CharybdisText.maelstrom());
            sl.playSound(null, boss.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 3.0f, 0.5f);
        }
    }

    // ================================================================ the moves
    static Move.MoveRun runOf(String id) {
        return switch (id) {
            case CharybdisTable.SCALE_VOLLEY -> (b, t) -> fire(of(b), t);
            case CharybdisTable.TENTACLE_SWEEP -> (b, t) -> sweep(of(b));
            case CharybdisTable.WHIRLPOOL -> (b, t) -> {
                Vec3 at = of(b).takePoint(CharybdisTable.WHIRLPOOL);
                whirl(of(b), at != null ? at : t.position(), CharybdisMath.WHIRL_RADIUS, CharybdisMath.WHIRL_TICKS);
            };
            case CharybdisTable.BEAK_LUNGE -> (b, t) -> lunge(of(b), t);
            case CharybdisTable.SUMMON -> (b, t) -> CharybdisMinions.summon(of(b), t);
            case CharybdisTable.TIDAL_CRUSH -> (b, t) -> {
                CharybdisSpawnBoss boss = of(b);
                if (!(boss.level() instanceof ServerLevel sl)) return;
                Vec3 at = boss.takePoint(CharybdisTable.TIDAL_CRUSH);
                crush(boss, at != null ? at : floorUnder(sl, t.position()));
            };
            case CharybdisTable.MAELSTROM -> (b, t) -> maelstrom(of(b), t);
            default -> (b, t) -> { };
        };
    }

    /** Where the scales leave it: a little ahead of its mouth, at the middle of its height. */
    static Vec3 mouth(CharybdisSpawnBoss boss) {
        return boss.position().add(0, boss.getBbHeight() * 0.5, 0).add(boss.getViewVector(1f).scale(2.1));
    }

    // ---------------------------------------------------------------- Magicule Scale Volley
    /** The fan: 5, 7 or 9 scales (by power), each aimed from the centre line at the target, sharing one budget of hits per victim and one pool of wounds. */
    static void fire(CharybdisSpawnBoss boss, LivingEntity target) {
        if (!(boss.level() instanceof ServerLevel sl) || target == null || !target.isAlive()) return;
        Vec3 from = mouth(boss), aim = target.getBoundingBox().getCenter().subtract(from);
        if (aim.lengthSqr() < 1e-4) aim = boss.getViewVector(1f);
        int n = CharybdisTable.fanCount(boss.power());
        double[] fan = CharybdisMath.fan(n);
        VolleyBudget budget = new VolleyBudget(CharybdisTable.HITS_PER_VICTIM);
        ParryRules.Pool pool = new ParryRules.Pool(boss.getMaxHealth());
        for (int i = 0; i < n; i++) {
            double[] d = CharybdisMath.aim(aim.x, aim.y, aim.z, fan[i], CharybdisMath.fanPitch(i, n));
            MagiculeScaleEntity.launch(sl, boss, from.add(d[0] * 0.4, d[1] * 0.4, d[2] * 0.4), new Vec3(d[0], d[1], d[2]), budget, pool);
        }
        sl.sendParticles(ParticleTypes.GLOW, from.x, from.y, from.z, 16, 0.5, 0.5, 0.5, 0.1);
        sl.playSound(null, boss.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2.0f, 0.8f);
    }

    // ---------------------------------------------------------------- Tentacle Sweep
    /** Everything in the 200 degree arc the wind-up drew, within reach and a few blocks up or down, takes the blow and is thrown outward. */
    static void sweep(CharybdisSpawnBoss boss) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        Vec3 dir = boss.takeDir(CharybdisTable.TENTACLE_SWEEP);
        if (dir == null) dir = boss.facing();
        Vec3 centre = boss.position().add(0, boss.getBbHeight() / 2, 0);
        for (LivingEntity f : boss.foesAround(centre, CharybdisMath.SWEEP_REACH + 1.0)) {
            Vec3 at = f.getBoundingBox().getCenter();
            if (Math.abs(at.y - centre.y) > CharybdisMath.SWEEP_HEIGHT) continue;
            if (!CharybdisMath.inArc(centre.x, centre.z, dir.x, dir.z, f.getX(), f.getZ(), CharybdisMath.SWEEP_REACH, CharybdisMath.SWEEP_HALF_ARC)) continue;
            if (boss.strike(f, CharybdisTable.SWEEP, false)) shove(f, f.position().subtract(centre).multiply(1, 0, 1), 0.7, 0.12);
        }
        double yaw = Math.atan2(dir.z, dir.x);
        for (int i = 0; i <= 12; i++) {
            double a = yaw - CharybdisMath.SWEEP_HALF_ARC + 2 * CharybdisMath.SWEEP_HALF_ARC * i / 12.0, r = CharybdisMath.SWEEP_REACH * 0.8;
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, centre.x + Math.cos(a) * r, centre.y, centre.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.BUBBLE, centre.x + Math.cos(a) * r, centre.y, centre.z + Math.sin(a) * r, 6, 0.5, 0.5, 0.5, 0.15);
        }
        sl.playSound(null, boss.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.0f, 0.5f);
        boss.swing(InteractionHand.MAIN_HAND);
    }

    // ---------------------------------------------------------------- Whirlpool
    /** A ring of water that pulls foes inward and round and slows them, by velocity only: gentle enough to swim out of, for 'ticks' ticks. */
    static void whirl(CharybdisSpawnBoss boss, Vec3 centre, double radius, int ticks) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        sl.playSound(null, BlockPos.containing(centre), SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundSource.HOSTILE, 2.0f, 0.5f);
        SpellRuntime.zone(sl, ticks, CharybdisMath.WHIRL_EVERY, safe("whirlpool", age -> whirlStep(boss, sl, centre, radius, age)));
    }

    private static void whirlStep(CharybdisSpawnBoss boss, ServerLevel sl, Vec3 centre, double radius, int age) {
        if (!boss.isAlive() || boss.isRemoved()) return;
        for (LivingEntity f : boss.foesAround(centre, radius + 0.5)) {
            if (Math.abs(f.getY() - centre.y) > CharybdisMath.WHIRL_HEIGHT) continue;
            double[] p = CharybdisMath.whirlpool(f.getX() - centre.x, f.getZ() - centre.z, radius);
            if (p[0] == 0 && p[1] == 0) continue;
            Vec3 v = f.getDeltaMovement();
            f.setDeltaMovement(v.x + p[0], v.y, v.z + p[1]);
            f.hurtMarked = true;
            slow(f, CharybdisMath.SLOW_TICKS, boss.gatesOpen() ? 1 : 0);
        }
        double a = age * 0.45;
        for (int k = 0; k < 3; k++) {
            double r = radius * (0.35 + 0.65 * ((age + k * 7) % 11) / 10.0), th = a + k * 2.1;
            sl.sendParticles(ParticleTypes.CURRENT_DOWN, centre.x + Math.cos(th) * r, centre.y + 0.6 + k * 0.8, centre.z + Math.sin(th) * r, 1, 0.1, 0.2, 0.1, 0.0);
            sl.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, centre.x + Math.cos(th + 1.0) * r, centre.y + 0.3, centre.z + Math.sin(th + 1.0) * r, 1, 0.1, 0.4, 0.1, 0.02);
        }
        if (age % 10 == 0) Tells.ring(sl, centre, radius, TEAL, 1.0f, 36);
    }

    // ---------------------------------------------------------------- Beak Lunge
    static void lunge(CharybdisSpawnBoss boss, LivingEntity target) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        Vec3 dir = boss.takeDir(CharybdisTable.BEAK_LUNGE);
        if (dir == null) {
            Vec3 aim = target.getBoundingBox().getCenter().subtract(boss.position().add(0, boss.getBbHeight() / 2, 0));
            dir = aim.lengthSqr() < 1e-4 ? boss.getViewVector(1f) : aim.normalize();
        }
        boss.lungeHits().clear();
        boss.swim().lunge(dir.x, dir.y, dir.z);
        sl.playSound(null, boss.blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 2.0f, 0.7f);
        boss.swing(InteractionHand.MAIN_HAND);
    }

    /** Every tick of the dive: each foe the body touches is struck once, shoved along the dive and slowed. */
    static void lungeTick(CharybdisSpawnBoss boss) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        Vec3 v = boss.getDeltaMovement();
        for (LivingEntity f : boss.foesAround(boss.position().add(0, boss.getBbHeight() / 2, 0), 6.0)) {
            if (boss.lungeHits().contains(f.getId())) continue;
            if (!boss.getBoundingBox().inflate(0.8).intersects(f.getBoundingBox())) continue;
            boss.lungeHits().add(f.getId());
            if (boss.strike(f, CharybdisTable.LUNGE, false)) {
                shove(f, v, 0.9, 0.1);
                slow(f, 40, 0);
            }
            sl.sendParticles(ParticleTypes.CRIT, f.getX(), f.getY() + 1, f.getZ(), 12, 0.3, 0.4, 0.3, 0.2);
        }
        sl.sendParticles(ParticleTypes.BUBBLE, boss.getX(), boss.getY() + boss.getBbHeight() / 2, boss.getZ(), 6, 1.0, 0.8, 1.0, 0.1);
    }

    // ---------------------------------------------------------------- Tidal Crush
    /** The marked circle: foes inside it, up to ten blocks above the floor, take the crush and are pressed down. */
    static void crush(CharybdisSpawnBoss boss, Vec3 floorCentre) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        for (LivingEntity f : boss.foesAround(floorCentre.add(0, 3, 0), CharybdisMath.CRUSH_RADIUS + 8)) {
            if (!CharybdisMath.inDisc(floorCentre.x, floorCentre.z, f.getX(), f.getZ(), CharybdisMath.CRUSH_RADIUS)) continue;
            if (f.getY() < floorCentre.y - 1.5 || f.getY() > floorCentre.y + CharybdisMath.CRUSH_HEIGHT) continue;
            if (boss.strike(f, CharybdisTable.CRUSH, true)) {
                Vec3 v = f.getDeltaMovement();
                f.setDeltaMovement(v.x * 0.3, CharybdisMath.CRUSH_SLAM, v.z * 0.3);
                f.hurtMarked = true;
                slow(f, 40, 0);
            }
        }
        for (int i = 1; i <= 3; i++) Tells.ring(sl, floorCentre.add(0, 0.2 * i, 0), CharybdisMath.CRUSH_RADIUS * (0.5 + 0.25 * i), GLOW, 1.4f, 40);
        sl.sendParticles(ParticleTypes.BUBBLE_POP, floorCentre.x, floorCentre.y + 1, floorCentre.z, 40, CharybdisMath.CRUSH_RADIUS * 0.6, 1.2, CharybdisMath.CRUSH_RADIUS * 0.6, 0.1);
        sl.playSound(null, BlockPos.containing(floorCentre), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.5f, 0.6f);
        boss.swing(InteractionHand.MAIN_HAND);
    }

    // ---------------------------------------------------------------- Maelstrom of Scales
    /** A whirlpool round the boss for as long as the move lasts, and two fans of scales into it, a second and a half apart. */
    static void maelstrom(CharybdisSpawnBoss boss, LivingEntity target) {
        whirl(boss, boss.position().add(0, 0.5, 0), CharybdisMath.MAELSTROM_RADIUS, CharybdisMath.MAELSTROM_TICKS);
        fire(boss, target);
        boss.later(30, safe("maelstrom volley", () -> {
            LivingEntity tg = boss.getTarget();
            if (tg != null && tg.isAlive()) fire(boss, tg);
        }));
    }

    // ================================================================ helpers
    private static void slow(LivingEntity f, int ticks, int level) {
        MobEffectInstance cur = f.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (cur == null || cur.getDuration() < ticks / 2 || cur.getAmplifier() < level) f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, level));
    }

    /** A shove along the floor-plane direction 'away' (any length but zero) with a little lift; players are told of it. */
    private static void shove(LivingEntity f, Vec3 away, double push, double lift) {
        Vec3 flat = away.multiply(1, 0, 1);
        if (flat.lengthSqr() < 1e-6) return;
        Vec3 d = flat.normalize().scale(push), v = f.getDeltaMovement();
        f.setDeltaMovement(v.x + d.x, v.y + lift, v.z + d.z);
        f.hurtMarked = true;
    }

    private static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(DevilStyle.red(rgb), DevilStyle.green(rgb), DevilStyle.blue(rgb)), size);
    }

    /** The top of the first solid block under a point (looking down at most twelve blocks), as a point; the point itself if there is none. */
    static Vec3 floorUnder(ServerLevel sl, Vec3 p) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int x = Mth.floor(p.x), z = Mth.floor(p.z), y0 = Mth.floor(p.y);
        for (int y = y0; y > y0 - 12 && y > sl.getMinBuildHeight(); y--) {
            pos.set(x, y, z);
            if (sl.getBlockState(pos).blocksMotion()) return new Vec3(p.x, y + 1.0, p.z);
        }
        return p;
    }

    /** Where a ray from 'from' along 'dir' (unit) ends: at the first block in the way, or 'max' blocks on. */
    static Vec3 rayEnd(ServerLevel sl, Entity who, Vec3 from, Vec3 dir, double max) {
        Vec3 to = from.add(dir.scale(max));
        BlockHitResult hit = sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, who));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }
}
