package com.newuniverse.nusmp.entity.avatar;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.GravityMath;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.VanicaMath;
import com.newuniverse.nusmp.entity.zogratis.VanicaMoves;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * The Devil Avatar's moves as plain static functions, and their telegraphs. The gravity moves (Ceiling Pull, Floor Crush, Inversion Field)
 * use velocity and effects only, never a block: a foe is lifted to the roof by a bounded climb and held by Levitation, let down by Slow
 * Falling, slammed by one strong downward push, or carried up and down by a field; every push is skipped for creative players and
 * spectators, halved for a holder of a Mana Zone or of anti-magic, and every fall is cushioned. The curse moves are the Hex Volley
 * ({@link HexVolley}), the Dread Brand and the Curse Nova, built on Megicula's Curse of Decay and the Zogratis wave maths. Every move is
 * telegraphed on the ground first and can be left, jumped or steered out of; none is lethal on its own.
 */
final class AvatarMoves {
    private AvatarMoves() {}

    /** The first call of a wind-up (the tell runs every third tick, so the first is within three ticks of the start). */
    static boolean first(int left, int total) { return left >= total - 3; }

    // ================================================================ Void Claws
    /** A sweep of the long arms: a 140-degree cone, 6 blocks long, a plain blow (a raised shield takes it). */
    static void voidClaws(DevilAvatarBoss boss, float damage) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        boss.swing(InteractionHand.MAIN_HAND);
        AvatarVfx.clawsStrike(boss);
        Vec3 f = AvatarFx.facing(boss);
        double half = AvatarMath.CLAW_HALF_ANGLE, reach = AvatarMath.CLAW_REACH;
        Vec3 o = boss.position().add(0, boss.getBbHeight() * 0.55, 0);
        for (int claw = -2; claw <= 2; claw++) {                                   // five dark rakes across the sweep
            Vec3 d = AvatarFx.rotateY(f, claw * half / 2.5);
            for (int s = 1; s <= 5; s++)
                sl.sendParticles(AvatarFx.dust(AvatarFx.rgb(boss), 1.3f), o.x + d.x * reach * s / 5, o.y, o.z + d.z * reach * s / 5, 1, 0.05, 0.1, 0.05, 0.0);
        }
        Vec3 mid = o.add(f.scale(reach * 0.6));
        Fx.spray(sl, FxThemes.BOSS_AVATAR, FxOld.SQUID_INK, mid.x, mid.y, mid.z, 12, 0.8, 0.3, 0.8, 0.02);
        AvatarFx.sound(sl, boss.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.5f);
        for (LivingEntity e : boss.foes(reach + 2)) {
            Vec3 to = e.position().subtract(boss.position());
            if (Math.abs(to.y) > 3.5 || !AvatarMath.inCone(to.x, to.z, f.x, f.z, half, reach, e.getBbWidth() / 2)) continue;
            if (boss.strike(e, damage, false)) {
                Vec3 flat = to.multiply(1, 0, 1);
                Vec3 n = flat.lengthSqr() < 1e-4 ? f : flat.normalize();
                e.knockback(0.9, -n.x, -n.z);
                e.hurtMarked = true;
            }
        }
    }

    static void tellClaws(DevilAvatarBoss boss, int left, int total) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        Vec3 f = AvatarFx.facing(boss), o = boss.position().add(0, 0.15, 0);
        double half = AvatarMath.CLAW_HALF_ANGLE, reach = AvatarMath.CLAW_REACH;
        if (first(left, total)) AvatarVfx.clawsTell(boss);
        Vec3 prev = null;
        for (int i = 0; i <= 6; i++) {
            Vec3 p = o.add(AvatarFx.rotateY(f, -half + i * (2 * half / 6)).scale(reach));
            if (prev != null) Tells.line(sl, prev, p, AvatarFx.WARN);
            prev = p;
        }
        Tells.line(sl, o, o.add(AvatarFx.rotateY(f, -half).scale(reach)), AvatarFx.WARN);
        Tells.line(sl, o, o.add(AvatarFx.rotateY(f, half).scale(reach)), AvatarFx.WARN);
    }

    // ================================================================ Hex Volley
    /** Five to nine bolts at the target and, a third of them, at the other foes: each a small hit, and a Curse of Decay for whoever they land on. */
    static void hexVolley(DevilAvatarBoss boss, LivingEntity target) {
        if (!(boss.level() instanceof ServerLevel sl) || target == null) return;
        List<LivingEntity> others = new ArrayList<>();
        for (LivingEntity e : boss.foes(40)) if (e != target) others.add(e);
        int n = AvatarMath.boltCount(boss.tuning().castSlots());
        List<LivingEntity> aims = new ArrayList<>();
        for (int i = 0; i < n; i++) aims.add(others.isEmpty() || i % 3 != 2 ? target : others.get((i / 3) % others.size()));
        AvatarVfx.volleyStrike(boss);
        new HexVolley(boss, aims, true).launch(sl);
    }

    static void tellVolley(DevilAvatarBoss boss, LivingEntity target, int left, int total) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        if (first(left, total)) AvatarVfx.volleyTell(boss, total);
        Vec3 from = boss.position().add(0, boss.getBbHeight() * 0.78, 0);
        Vec3 aim = target.getBoundingBox().getCenter().subtract(from);
        if (aim.lengthSqr() < 1e-4) return;
        aim = aim.normalize();
        int n = Math.min(5, AvatarMath.boltCount(boss.tuning().castSlots()));
        for (int i = 0; i < n; i++) Tells.line(sl, from, from.add(AvatarFx.rotateY(aim, (i - (n - 1) / 2.0) * 0.07).scale(16)), AvatarFx.rgb(boss));
        Tells.gather(sl, from, 0.8, AvatarFx.accent(boss), 5);
    }

    // ================================================================ Shadow Step
    /** The spot behind the target where the Avatar will step out, on floor it can stand on; null when there is none. */
    static Vec3 stepExit(DevilAvatarBoss boss, LivingEntity target) {
        double[] b = AvatarMath.behind(target.getX(), target.getZ(), target.getYRot(), AvatarMath.SHADOW_BEHIND);
        return ZogratisBoss.groundOrNull(boss.level(), new Vec3(b[0], target.getY(), b[1]));
    }

    /** A visible blink to the locked spot behind the target (or the nearest that fits), then a quick strike round the place it arrives at. */
    static void shadowStep(DevilAvatarBoss boss, LivingEntity target, Vec3 locked) {
        if (!(boss.level() instanceof ServerLevel sl) || target == null) return;
        List<Vec3> spots = new ArrayList<>();
        if (locked != null) spots.add(locked);
        Vec3 now = stepExit(boss, target);
        if (now != null) spots.add(now);
        Vec3 toBoss = boss.position().subtract(target.position()).multiply(1, 0, 1);
        if (toBoss.lengthSqr() > 1e-4) {                                         // last resort: between the target and where the Avatar stood
            Vec3 g = ZogratisBoss.groundOrNull(sl, target.position().add(toBoss.normalize().scale(AvatarMath.SHADOW_BEHIND)));
            if (g != null) spots.add(g);
        }
        boolean moved = false;
        for (Vec3 s : spots) {
            if (boss.blinkTo(s)) {
                moved = true;
                break;
            }
        }
        if (!moved) {
            boss.announce("The Avatar's step finds no room to land, and the dark closes over it.");
            return;
        }
        boss.later(AvatarMath.SHADOW_STRIKE_DELAY, AvatarFx.safe("shadow strike", () -> shadowStrike(boss)));
    }

    private static void shadowStrike(DevilAvatarBoss boss) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        boss.swing(InteractionHand.MAIN_HAND);
        AvatarVfx.stepStrike(boss);
        Vec3 at = boss.position();
        Tells.burst(sl, at.add(0, 1.2, 0), AvatarFx.accent(boss), 36);
        Fx.spray(sl, FxThemes.BOSS_AVATAR, FxOld.SQUID_INK, at.x, at.y + 1.2, at.z, 16, 1.0, 0.6, 1.0, 0.03);
        AvatarFx.sound(sl, at, SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.6f);
        for (LivingEntity e : boss.foes(AvatarMath.SHADOW_STRIKE_RADIUS + 2)) {
            Vec3 to = e.position().subtract(at);
            double flat = Math.hypot(to.x, to.z);
            if (flat > AvatarMath.SHADOW_STRIKE_RADIUS + e.getBbWidth() / 2 || Math.abs(to.y) > 3.5) continue;
            if (boss.strike(e, AvatarTable.SHADOW, false)) {
                double d = Math.max(1e-3, flat);
                e.knockback(0.8, -to.x / d, -to.z / d);
                e.hurtMarked = true;
            }
        }
    }

    static void tellStep(DevilAvatarBoss boss, LivingEntity target, int left, int total) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            boss.stepExit = stepExit(boss, target);
            boss.hint(AvatarTable.SHADOW_STEP, "The Avatar marks where it will step out, behind you. Leave the ring before it arrives.");
            AvatarVfx.stepTell(boss, boss.stepExit, total);
        }
        Tells.gather(sl, boss.position().add(0, boss.getBbHeight() * 0.5, 0), 0.8, AvatarFx.rgb(boss), 6);
        Vec3 x = boss.stepExit;
        if (x == null) return;
        Tells.ring(sl, x, AvatarMath.SHADOW_STRIKE_RADIUS, AvatarFx.WARN, 1.3f, 36);
        Tells.column(sl, x, 4, AvatarFx.accent(boss));
        Tells.line(sl, boss.position().add(0, 1, 0), x.add(0, 1, 0), AvatarFx.rgb(boss));
    }

    // ================================================================ Ceiling Pull
    /** A bounded climb towards a height: the foe, the y to climb to, and whether it has got there. */
    private static final class Lift {
        final LivingEntity e;
        final double targetY;
        boolean done;

        Lift(LivingEntity e, double targetY) {
            this.e = e;
            this.targetY = targetY;
        }
    }

    /**
     * Every foe within 'radius' of the Avatar (and between 6 blocks under it and 14 over it) is pulled up towards the ceiling: a climb of up
     * to {@link AvatarMath#LIFT_MAX} blocks (less under a low roof, half for a holder of a Mana Zone), then Levitation for 3 s holds it and
     * Slow Falling for 6 more lets it down. Nobody is lifted into a roof or onto a ledge; creative players and spectators are never touched.
     * Returns the foes that were lifted.
     */
    static List<LivingEntity> ceilingPull(DevilAvatarBoss boss, double radius) {
        List<LivingEntity> lifted = new ArrayList<>();
        if (!(boss.level() instanceof ServerLevel sl)) return lifted;
        Vec3 c = boss.position();
        VfxSpawn.send(sl, VfxShape.ASH_FORMATION, c, c.add(0, 1, 0), AvatarFx.argb(boss), 28, (float) Math.min(9, radius * 0.4));
        AvatarVfx.pullStrike(boss, radius);
        AvatarFx.sound(sl, c, SoundEvents.WARDEN_SONIC_CHARGE, 2.5f, 0.6f);
        List<Lift> lifts = new ArrayList<>();
        int caught = 0;
        for (LivingEntity e : boss.foesAround(c, Math.sqrt(radius * radius + 16 * 16) + 1)) {
            double dx = e.getX() - c.x, dz = e.getZ() - c.z, dy = e.getY() - c.y;
            if (dx * dx + dz * dz > radius * radius || dy < -AvatarMath.PULL_BELOW || dy > AvatarMath.PULL_ABOVE || !AvatarFx.pushable(e)) continue;
            if (++caught > AvatarMath.PULL_MAX_VICTIMS) break;
            double rise = AvatarMath.liftHeight(AvatarFx.headGap(sl, e), AvatarFx.resistOf(e));
            if (rise <= 0) continue;
            e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, AvatarMath.LEVITATION_TICKS, 0, false, true, true));
            e.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, AvatarMath.SLOW_FALL_TICKS, 0, false, true, true));
            lifts.add(new Lift(e, e.getY() + rise));
            lifted.add(e);
            if (e instanceof ServerPlayer sp)
                sp.displayClientMessage(Component.literal("Gravity lets go of you: the Avatar pulls you to the ceiling!").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        if (!lifts.isEmpty()) SpellRuntime.zone(sl, AvatarMath.LIFT_TICKS, 1, AvatarFx.safe("ceiling lift", age -> liftStep(sl, lifts)));
        return lifted;
    }

    private static void liftStep(ServerLevel sl, List<Lift> lifts) {
        for (Lift l : lifts) {
            LivingEntity e = l.e;
            if (l.done) continue;
            double need = l.targetY - e.getY();
            if (!e.isAlive() || e.isRemoved() || need <= 0.3 || AvatarFx.blockedAbove(sl, e)) {
                l.done = true;
                continue;
            }
            Vec3 v = e.getDeltaMovement();
            AvatarFx.push(e, v.x * 0.6, AvatarMath.liftSpeed(need), v.z * 0.6);
            e.fallDistance = 0;
        }
    }

    /** The hex bolts that rain on the foes hanging at the ceiling: one each, at most six, from the Avatar. */
    static void rain(DevilAvatarBoss boss, List<LivingEntity> lifted) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        List<LivingEntity> aims = new ArrayList<>();
        for (LivingEntity e : lifted) {
            if (aims.size() >= AvatarMath.RAIN_MAX_BOLTS) break;
            if (e.isAlive() && !e.onGround() && boss.isFoe(e) && e.distanceToSqr(boss) < 44 * 44) aims.add(e);
        }
        new HexVolley(boss, aims, false).launch(sl);
    }

    static void tellPull(DevilAvatarBoss boss, double radius, int left, int total) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        Vec3 c = boss.position();
        if (first(left, total)) {
            VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, c.add(0, 0.05, 0), c.add(0, 1, 0), AvatarFx.argb(boss), total, (float) (radius / 1.6));
            AvatarVfx.pullTell(boss, radius, total);
        }
        Tells.ring(sl, c, radius, AvatarFx.rgb(boss), 1.4f, (int) Math.min(96, 16 + radius * 5));
        Tells.ring(sl, c, Math.max(1.0, radius * left / Math.max(1, total)), AvatarFx.accent(boss), 1.1f, 36);
        AvatarFx.motes(sl, Fx.options(FxThemes.BOSS_AVATAR, FxOld.REVERSE_PORTAL), c, radius, 16, 0.32);
        Tells.gather(sl, c.add(0, boss.getBbHeight() * 0.8, 0), 0.9, AvatarFx.accent(boss), 5);
    }

    // ================================================================ Floor Crush
    /** The floor under (x, y, z) as a mark: the first solid surface within 64 blocks below, else y itself. */
    private static Vec3 markOn(ServerLevel sl, Entity by, double x, double y, double z) {
        double fy = AvatarFx.floorY(sl, by, new Vec3(x, y + 0.5, z), 64);
        return new Vec3(x, Double.isNaN(fy) ? y : fy, z);
    }

    /** The crush marks: on the floor under the target, then under the other foes, never closer together than 1.6 mark widths, at most 'count'. */
    static List<Vec3> crushMarks(DevilAvatarBoss boss, LivingEntity target, int count) {
        if (!(boss.level() instanceof ServerLevel sl) || target == null) return List.of();
        List<double[]> pts = new ArrayList<>();
        pts.add(new double[]{target.getX(), target.getZ(), target.getY()});
        for (LivingEntity e : boss.foes(44)) if (e != target) pts.add(new double[]{e.getX(), e.getZ(), e.getY()});
        List<Vec3> out = new ArrayList<>();
        for (double[] p : AvatarMath.spreadMarks(pts, AvatarMath.CRUSH_RADIUS * 1.6, count)) out.add(markOn(sl, boss, p[0], p[2], p[1]));
        return out;
    }

    /**
     * The floor presses on every mark: everyone within the mark's radius, from just under its floor to 26 blocks over it, is slammed down (one
     * strong push, Slowness III for 3 s, the Levitation and Slow Falling of a pull ended) and crushed: 'base' damage times 1.0 on the ground up
     * to 1.7 in the air (the foes of a pull are in the air), halved by a raised shield or anti-magic, through the boss's damage law. A foe is
     * hit once however many marks it stands in. Returns how many were slammed.
     */
    static int floorCrush(DevilAvatarBoss boss, List<Vec3> marks, float base) {
        if (!(boss.level() instanceof ServerLevel sl) || marks.isEmpty()) return 0;
        final double radius = AvatarMath.CRUSH_RADIUS;
        Set<Integer> hit = new HashSet<>();
        List<LivingEntity> slammed = new ArrayList<>();
        for (Vec3 m : marks) {
            VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, m.add(0, 0.05, 0), m.add(0, 1, 0), AvatarFx.argb(boss), 18, (float) (radius / 1.2));
            Tells.burst(sl, m.add(0, 1, 0), AvatarFx.accent(boss), 36);
            AvatarVfx.crushStrike(boss, m);
            AvatarFx.sound(sl, m, SoundEvents.WITHER_SPAWN, 1.6f, 0.7f);
            for (LivingEntity e : boss.foesAround(m.add(0, 8, 0), Math.sqrt(radius * radius + 20 * 20) + 1)) {
                if (hit.contains(e.getId()) || !AvatarFx.pushable(e)) continue;
                double dx = e.getX() - m.x, dz = e.getZ() - m.z, dy = e.getY() - m.y;
                if (dx * dx + dz * dz > radius * radius || dy < -AvatarMath.CRUSH_BELOW || dy > AvatarMath.CRUSH_ABOVE) continue;
                hit.add(e.getId());
                double h = AvatarFx.feetAboveFloor(sl, boss, e);
                float damage = (float) (base * AvatarMath.crushMultiplier(h) * AvatarFx.guard(e));
                AvatarFx.hurtWith(boss, e, damage, TensuraDamageTypes.GRAVITY_PRESS);
                if (!e.isAlive()) continue;
                e.removeEffect(MobEffects.LEVITATION);
                e.removeEffect(MobEffects.SLOW_FALLING);
                AvatarFx.push(e, 0, -AvatarMath.slamSpeed(h, AvatarFx.resistOf(e)), 0);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, AvatarMath.CRUSH_SLOW_TICKS, AvatarMath.CRUSH_SLOW_AMP));
                e.fallDistance = 0;
                slammed.add(e);
            }
        }
        AvatarFx.cushion(sl, slammed);
        return slammed.size();
    }

    static void tellCrush(DevilAvatarBoss boss, LivingEntity target, int left, int total) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            boss.marks = crushMarks(boss, target, AvatarMath.markCount(boss.power()));
            boss.hint(AvatarTable.FLOOR_CRUSH, "The floor is marked under you. Leave the rings before it presses: the air is the worst place to be.");
            for (Vec3 m : boss.marks) {
                VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, m.add(0, 0.05, 0), m.add(0, 1, 0), AvatarFx.argb(boss), total, (float) (AvatarMath.CRUSH_RADIUS / 1.6));
                AvatarVfx.crushTell(boss, m, total);
            }
            AvatarFx.sound(sl, boss.position(), SoundEvents.WARDEN_SONIC_CHARGE, 2f, 0.5f);
        }
        for (Vec3 m : boss.marks) {
            Tells.ring(sl, m, AvatarMath.CRUSH_RADIUS, AvatarFx.WARN, 1.4f, 36);
            Tells.ring(sl, m, Math.max(0.5, AvatarMath.CRUSH_RADIUS * left / Math.max(1, total)), AvatarFx.accent(boss), 1.1f, 24);
            AvatarFx.motes(sl, Fx.options(FxThemes.BOSS_AVATAR, FxOld.CRIMSON_SPORE), m, AvatarMath.CRUSH_RADIUS, 5, -0.25);
            Tells.column(sl, m, 5, AvatarFx.rgb(boss));
        }
        Tells.gather(sl, boss.position().add(0, boss.getBbHeight() * 0.7, 0), 0.9, AvatarFx.accent(boss), 4);
    }

    // ================================================================ Inversion Field
    /**
     * A field of {@link AvatarMath#FIELD_RADIUS} blocks on 'mark' for 8 s whose pull flips every 30 ticks, up and then down and so on:
     * everyone in it is carried the way it pulls (an ease-in after each flip, a lift that stops 3 blocks under its top and under any roof,
     * half the push for a holder of a Mana Zone or anti-magic), and no fall in it hurts. It ends with the fight or the boss.
     */
    static void inversionField(DevilAvatarBoss boss, Vec3 mark) {
        if (!(boss.level() instanceof ServerLevel sl) || mark == null) return;
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, mark.add(0, 0.05, 0), mark.add(0, 1, 0), AvatarFx.argb(boss), AvatarMath.FIELD_TICKS, (float) (AvatarMath.FIELD_RADIUS / 1.6));
        AvatarFx.sound(sl, mark, SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 0.4f);
        AvatarVfx.fieldStrike(boss, mark);
        SpellRuntime.zone(sl, AvatarMath.FIELD_TICKS, AvatarMath.FIELD_STEP, AvatarFx.safe("inversion field", age -> fieldStep(boss, sl, mark, age)));
    }

    private static void fieldStep(DevilAvatarBoss boss, ServerLevel sl, Vec3 c, int age) {
        if (!boss.isAlive() || boss.isRemoved() || !boss.fightActive()) {              // the fight is over: whoever hangs in the field is let down gently
            fieldLetGo(boss, sl, c);
            return;
        }
        int dir = AvatarMath.inversionDir(age);
        if (dir == 0) return;
        double r = AvatarMath.FIELD_RADIUS;
        if (age % AvatarMath.FLIP_TICKS == 0) {
            AvatarFx.sound(sl, c, SoundEvents.BEACON_ACTIVATE, 1.6f, dir > 0 ? 1.5f : 0.5f);
            Tells.burst(sl, c.add(0, 1, 0), dir > 0 ? AvatarFx.accent(boss) : AvatarFx.WARN, 24);
        }
        if (age % 6 == 0) Tells.ring(sl, c, r, dir > 0 ? AvatarFx.accent(boss) : AvatarFx.WARN, 1.3f, 40);
        AvatarFx.motes(sl, dir > 0 ? Fx.options(FxThemes.BOSS_AVATAR, FxOld.REVERSE_PORTAL) : Fx.options(FxThemes.BOSS_AVATAR, FxOld.CRIMSON_SPORE), c, r, 8, dir > 0 ? 0.25 : -0.3);
        int seen = 0;
        for (LivingEntity e : boss.foesAround(c.add(0, 4, 0), Math.sqrt(r * r + 100) + 1)) {
            double dx = e.getX() - c.x, dz = e.getZ() - c.z, dy = e.getY() - c.y;
            if (dx * dx + dz * dz > r * r || dy < -AvatarMath.FIELD_BELOW || dy > AvatarMath.FIELD_ABOVE || !AvatarFx.pushable(e)) continue;
            if (++seen > AvatarMath.PULL_MAX_VICTIMS) break;
            double v = AvatarMath.inversionPush(age, AvatarFx.resistOf(e));
            e.fallDistance = 0;
            if (v > 0 && (dy >= AvatarMath.FIELD_ABOVE - 3 || AvatarFx.blockedAbove(sl, e))) continue;
            Vec3 m = e.getDeltaMovement();
            AvatarFx.push(e, m.x * 0.9, v, m.z * 0.9);
        }
        if (age >= AvatarMath.FIELD_TICKS - 3 * AvatarMath.FIELD_STEP) fieldLetGo(boss, sl, c);      // the last steps: nobody is left to fall out of the field
    }

    /** Slow Falling for 5 s (once) for every foe in the air inside the field, with the fall counter cleared: no field of the Avatar ends in a fall. */
    private static void fieldLetGo(DevilAvatarBoss boss, ServerLevel sl, Vec3 c) {
        double r = AvatarMath.FIELD_RADIUS;
        for (LivingEntity e : boss.foesAround(c.add(0, 4, 0), Math.sqrt(r * r + 100) + 1)) {
            double dx = e.getX() - c.x, dz = e.getZ() - c.z, dy = e.getY() - c.y;
            if (dx * dx + dz * dz > r * r || dy < -AvatarMath.FIELD_BELOW || dy > AvatarMath.FIELD_ABOVE + 3 || !AvatarFx.pushable(e) || e.onGround()) continue;
            e.fallDistance = 0;
            if (!e.hasEffect(MobEffects.SLOW_FALLING)) e.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, true, true));
        }
    }

    static void tellField(DevilAvatarBoss boss, LivingEntity target, int left, int total) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            boss.marks = List.of(markOn(sl, boss, target.getX(), target.getY(), target.getZ()));
            Vec3 m = boss.marks.get(0);
            VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, m.add(0, 0.05, 0), m.add(0, 1, 0), AvatarFx.argb(boss), total, (float) (AvatarMath.FIELD_RADIUS / 1.6));
            boss.hint(AvatarTable.INVERSION_FIELD, "The Avatar opens a field that pulls up and presses down in turn, every second. A Mana Zone halves it; leave the ring if you cannot steer.");
            AvatarVfx.fieldTell(boss, m, total);
        }
        if (boss.marks.isEmpty()) return;
        Vec3 m = boss.marks.get(0);
        Tells.ring(sl, m, AvatarMath.FIELD_RADIUS, AvatarFx.rgb(boss), 1.4f, 48);
        Tells.ring(sl, m, Math.max(0.6, AvatarMath.FIELD_RADIUS * left / Math.max(1, total)), AvatarFx.accent(boss), 1.1f, 28);
        AvatarFx.motes(sl, Fx.options(FxThemes.BOSS_AVATAR, FxOld.REVERSE_PORTAL), m, AvatarMath.FIELD_RADIUS, 6, 0.25);
        AvatarFx.motes(sl, Fx.options(FxThemes.BOSS_AVATAR, FxOld.CRIMSON_SPORE), m, AvatarMath.FIELD_RADIUS, 4, -0.3);
        Tells.column(sl, m, 6, AvatarFx.accent(boss));
    }

    // ================================================================ Dread Brand
    /** The foe farthest from the Avatar within 'radius', or null when there is none. */
    static LivingEntity farthestFoe(DevilAvatarBoss boss, double radius) {
        LivingEntity best = null;
        double bestD = -1;
        for (LivingEntity e : boss.foes(radius)) {
            double d = e.distanceToSqr(boss);
            if (d > bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    /**
     * Brands 'foe' with Megicula's Curse of Decay for 3 s (the existing sigil: leave it to break the brand) if it is still inside the ring the
     * wind-up drew at 'mark'; if it stepped out in time the sigil closes on empty ground.
     */
    static void dreadBrand(DevilAvatarBoss boss, LivingEntity foe, Vec3 mark) {
        if (foe == null || !foe.isAlive() || foe.isRemoved() || !boss.isFoe(foe)) {
            boss.announce("The dread brand closes on empty ground.");
            return;
        }
        int level = VanicaMath.curseLevel(boss.power());
        double r = VanicaMath.sigilRadius(level);
        Vec3 m = mark != null ? mark : foe.position();
        double dx = foe.getX() - m.x, dz = foe.getZ() - m.z;
        if (dx * dx + dz * dz > r * r) {
            boss.announce("The dread brand closes on empty ground: its mark stepped out in time.");
            return;
        }
        AvatarVfx.brandStrike(boss, m, r);
        VanicaMoves.weakeningCurse(boss, foe, AvatarMath.DREAD_TICKS, level);
    }

    static void tellBrand(DevilAvatarBoss boss, int left, int total) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            LivingEntity far = farthestFoe(boss, 40);
            boss.brandTarget = far == null ? -1 : far.getId();
            boss.brandMark = far == null ? null : ZogratisBoss.ground(sl, far.position());
            boss.hint(AvatarTable.DREAD_BRAND, "The Avatar marks the foe farthest from it for a dread brand. Leave the sigil and the brand breaks.");
            if (boss.brandMark != null)
                VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, boss.brandMark.add(0, 0.05, 0), boss.brandMark.add(0, 1, 0), 0xFFFF5A4A, total,
                        (float) (VanicaMath.sigilRadius(VanicaMath.curseLevel(boss.power())) / 1.6));
            AvatarVfx.brandTell(boss, boss.brandMark, VanicaMath.sigilRadius(VanicaMath.curseLevel(boss.power())), total);
        }
        Tells.gather(sl, boss.position().add(0, boss.getBbHeight() * 0.8, 0), 0.8, AvatarFx.accent(boss), 5);
        Vec3 m = boss.brandMark;
        if (m == null) return;
        double r = VanicaMath.sigilRadius(VanicaMath.curseLevel(boss.power()));
        Tells.ring(sl, m, r, AvatarFx.WARN, 1.4f, (int) Math.min(80, 14 + r * 5));
        Tells.ring(sl, m, Math.max(0.6, r * left / Math.max(1, total)), AvatarFx.accent(boss), 1.1f, 28);
        Tells.column(sl, m, 4, AvatarFx.accent(boss));
        Tells.line(sl, boss.position().add(0, boss.getBbHeight() * 0.8, 0), m.add(0, 1, 0), AvatarFx.rgb(boss));
    }

    // ================================================================ Curse Nova
    /**
     * A wave of curse out from 'centre' to 'reach' blocks at the speed of Dante's stomp: everyone it passes is hurt once (magic, so armour
     * does not help) and weakened for 4 s, unless their feet are off the floor by a jump's height or more.
     */
    static void cursePulse(DevilAvatarBoss boss, Vec3 centre, double reach, float damage, int weakAmp) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        final Set<Integer> hit = new HashSet<>();
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, centre.add(0, 0.05, 0), centre.add(0, 1, 0), AvatarFx.argb(boss), 14, (float) (reach / 3));
        AvatarFx.sound(sl, centre, SoundEvents.ELDER_GUARDIAN_CURSE, 2f, 0.6f);
        AvatarVfx.novaStrike(boss, centre, reach);
        SpellRuntime.zone(sl, GravityMath.waveTicks(reach), 1, AvatarFx.safe("curse nova", age -> {
            if (!boss.isAlive() || boss.isRemoved()) return;
            double wr = GravityMath.waveRadius(age);
            if (age % 2 == 0) Tells.ring(sl, centre, Math.min(wr, reach), AvatarFx.rgb(boss), 1.5f, (int) Math.min(80, 12 + Math.min(wr, reach) * 6));
            for (LivingEntity e : boss.foesAround(centre, reach + 3)) {
                if (hit.contains(e.getId())) continue;
                double dx = e.getX() - centre.x, dz = e.getZ() - centre.z;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > reach + e.getBbWidth() / 2 || !GravityMath.waveHits(dist, wr) || GravityMath.jumpsWave(AvatarFx.feetAboveFloor(sl, boss, e))) continue;
                hit.add(e.getId());
                if (boss.strike(e, damage, true)) {
                    double d = Math.max(1e-3, dist);
                    e.knockback(0.5, -dx / d, -dz / d);
                    e.hurtMarked = true;
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, weakAmp));
                }
            }
        }));
    }

    static void tellNova(DevilAvatarBoss boss, double reach, int left, int total) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        if (first(left, total)) AvatarVfx.novaTell(boss, reach, total);
        Tells.ring(sl, boss.position(), Math.max(1.5, reach * (total - left) / Math.max(1, total)), AvatarFx.rgb(boss), 1.3f, 48);
        if (left % 6 == 0) Tells.ring(sl, boss.position(), reach, AvatarFx.WARN, 1.1f, 56);
        Tells.gather(sl, boss.position().add(0, 0.6, 0), 1.2, AvatarFx.accent(boss), 5);
    }

    // ================================================================ Brimstone Eruption
    /**
     * Spokes of brimstone burst out of the floor round the Avatar along the lines the wind-up drew (5 spokes; 7 at the Gates, and then a second wave
     * on the gaps of the first, {@link AvatarMath#ERUPT_SECOND_AT} ticks later, which the wind-up drew too). Whoever stands on a spoke when it bursts
     * is struck once per wave and thrown up; the gaps between the spokes are safe. Nothing is placed or broken.
     */
    static void eruption(DevilAvatarBoss boss, double base) {
        if (!(boss.level() instanceof ServerLevel)) return;
        boss.swing(InteractionHand.MAIN_HAND);
        final Vec3 c = boss.position();
        final int n = AvatarMath.eruptionSpokes(boss.gatesOpen());
        eruptWave(boss, c, base, n);
        if (boss.gatesOpen()) boss.later(AvatarMath.ERUPT_SECOND_AT, AvatarFx.safe("second eruption", () -> eruptWave(boss, c, base + Math.PI / n, n)));
    }

    private static void eruptWave(DevilAvatarBoss boss, Vec3 c, double base, int n) {
        if (!(boss.level() instanceof ServerLevel sl) || !boss.isAlive() || boss.isRemoved()) return;
        AvatarVfx.eruptionStrike(boss, c, base, n);
        AvatarFx.sound(sl, c, SoundEvents.GENERIC_EXPLODE.value(), 1.6f, 0.6f);
        for (int k = 0; k < n; k++) {
            double a = base + Math.PI * 2 * k / n;
            for (int s = 1; s <= 4; s++)
                Fx.spray(sl, FxThemes.BOSS_AVATAR, FxOld.LAVA, c.x + Math.cos(a) * AvatarMath.ERUPT_LENGTH * s / 4, c.y + 0.3, c.z + Math.sin(a) * AvatarMath.ERUPT_LENGTH * s / 4, 1, 0.3, 0.2, 0.3, 0.0);
        }
        for (LivingEntity e : boss.foesAround(c, AvatarMath.ERUPT_LENGTH + 3)) {
            if (Math.abs(e.getY() - c.y) > 4.0 || !AvatarFx.pushable(e)) continue;
            if (!AvatarMath.inSpoke(e.getX() - c.x, e.getZ() - c.z, base, n, AvatarMath.ERUPT_HALF_WIDTH + e.getBbWidth() / 2, AvatarMath.ERUPT_LENGTH)) continue;
            if (boss.strike(e, AvatarTable.ERUPT, true)) {
                Vec3 v = e.getDeltaMovement();
                AvatarFx.push(e, v.x, 0.5, v.z);
                e.fallDistance = 0;
            }
        }
    }

    static void tellEruption(DevilAvatarBoss boss, LivingEntity target, int left, int total) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        if (first(left, total) || Double.isNaN(boss.eruptBase)) {
            Vec3 to = target.position().subtract(boss.position());
            boss.eruptBase = Math.atan2(to.z, to.x);
            boss.hint(AvatarTable.BRIMSTONE_ERUPTION, "Brimstone bursts along the lines the Avatar draws: stand in a gap between two lines. At the Gates a second wave bursts in the gaps of the first.");
            AvatarVfx.eruptionTell(boss, boss.eruptBase, AvatarMath.eruptionSpokes(boss.gatesOpen()), boss.gatesOpen(), total);
            AvatarFx.sound(sl, boss.position(), SoundEvents.WARDEN_SONIC_CHARGE, 2f, 0.5f);
        }
        Vec3 c = boss.position().add(0, 0.15, 0);
        int n = AvatarMath.eruptionSpokes(boss.gatesOpen());
        for (int k = 0; k < n; k++) {
            double a = boss.eruptBase + Math.PI * 2 * k / n;
            Tells.line(sl, c.add(Math.cos(a) * 1.5, 0, Math.sin(a) * 1.5), c.add(Math.cos(a) * AvatarMath.ERUPT_LENGTH, 0, Math.sin(a) * AvatarMath.ERUPT_LENGTH), AvatarFx.WARN);
            if (boss.gatesOpen()) {
                double b = a + Math.PI / n;
                Tells.line(sl, c.add(Math.cos(b) * 1.5, 0, Math.sin(b) * 1.5), c.add(Math.cos(b) * AvatarMath.ERUPT_LENGTH, 0, Math.sin(b) * AvatarMath.ERUPT_LENGTH), AvatarFx.rgb(boss));
            }
        }
        Tells.gather(sl, boss.position().add(0, 0.6, 0), 1.2, AvatarFx.accent(boss), 5);
    }

    // ================================================================ Abyssal Collapse
    /**
     * The ultimate: a wider Ceiling Pull; 26 ticks later the floor is marked under every foe still hanging (at most eight marks, drawn for the
     * 32 ticks they have to steer out of them); at 58 ticks the Floor Crush presses on the marks, with the ultimate's damage; at 64 a Curse
     * Nova leaves the Avatar. The framework leaves the Avatar exposed for 3 s afterwards. Every follow-up is skipped if the Avatar is gone.
     */
    static void abyssalCollapse(DevilAvatarBoss boss) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        final List<LivingEntity> lifted = ceilingPull(boss, AvatarMath.ULT_PULL_RADIUS);
        final List<Vec3> marks = new ArrayList<>();
        boss.later(AvatarMath.ULT_MARK_AT, AvatarFx.safe("collapse marks", () -> {
            List<double[]> pts = new ArrayList<>();
            for (LivingEntity e : lifted) if (e.isAlive() && !e.onGround()) pts.add(new double[]{e.getX(), e.getZ(), e.getY()});
            for (double[] p : AvatarMath.spreadMarks(pts, AvatarMath.CRUSH_RADIUS * 1.6, AvatarMath.ULT_MAX_MARKS)) marks.add(markOn(sl, boss, p[0], p[2], p[1]));
            if (marks.isEmpty()) return;
            boss.announce("The floor is marked under everyone still hanging. Steer out of the rings before it presses!");
            for (LivingEntity e : lifted)
                if (e instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal("The floor will crush! Steer out of the marked rings!").withStyle(ChatFormatting.RED), true);
            for (Vec3 m : marks) {
                VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, m.add(0, 0.05, 0), m.add(0, 1, 0), AvatarFx.argb(boss), AvatarMath.ULT_CRUSH_AT - AvatarMath.ULT_MARK_AT,
                        (float) (AvatarMath.CRUSH_RADIUS / 1.6));
                AvatarVfx.crushTell(boss, m, AvatarMath.ULT_CRUSH_AT - AvatarMath.ULT_MARK_AT);
            }
            SpellRuntime.zone(sl, AvatarMath.ULT_CRUSH_AT - AvatarMath.ULT_MARK_AT, 3, AvatarFx.safe("collapse rings", age -> {
                if (!boss.isAlive() || boss.isRemoved()) return;
                for (Vec3 m : marks) {
                    Tells.ring(sl, m, AvatarMath.CRUSH_RADIUS, AvatarFx.WARN, 1.4f, 36);
                    Tells.column(sl, m, 5, AvatarFx.accent(boss));
                }
            }));
        }));
        boss.later(AvatarMath.ULT_CRUSH_AT, AvatarFx.safe("collapse crush", () -> floorCrush(boss, marks, AvatarTable.ULT_CRUSH)));
        boss.later(AvatarMath.ULT_NOVA_AT, AvatarFx.safe("collapse nova", () -> cursePulse(boss, boss.position(), AvatarMath.ULT_NOVA_REACH, AvatarTable.ULT_NOVA, 1)));
    }

    static void tellCollapse(DevilAvatarBoss boss, int left, int total) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        Vec3 c = boss.position();
        if (first(left, total)) {
            Vec3 o = c.add(0, boss.getBbHeight() + 1.6, 0);
            VfxSpawn.send(sl, VfxShape.DARK_BLACK_HOLE, o, o.add(0, 1, 0), AvatarFx.argb(boss), left + 4, 2.0f);
            VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, c.add(0, 0.05, 0), c.add(0, 1, 0), AvatarFx.argb(boss), total, (float) (AvatarMath.ULT_PULL_RADIUS / 1.6));
            boss.hint(AvatarTable.ABYSSAL_COLLAPSE, "Abyssal Collapse: the Avatar pulls everyone in the ring to the ceiling, then presses the marked floor and sends out a wave of curse. Leave the ring, or steer clear of the marks and jump the wave.");
            AvatarFx.sound(sl, c, SoundEvents.WARDEN_SONIC_CHARGE, 3f, 0.4f);
            AvatarVfx.pullTell(boss, AvatarMath.ULT_PULL_RADIUS, total);
        }
        Tells.ring(sl, c, AvatarMath.ULT_PULL_RADIUS, AvatarFx.rgb(boss), 1.5f, 96);
        Tells.ring(sl, c, Math.max(1.5, AvatarMath.ULT_PULL_RADIUS * left / Math.max(1, total)), AvatarFx.accent(boss), 1.2f, 40);
        if (left % 6 == 0) Tells.ring(sl, c, AvatarMath.ULT_NOVA_REACH, AvatarFx.WARN, 1.1f, 64);
        AvatarFx.motes(sl, Fx.options(FxThemes.BOSS_AVATAR, FxOld.REVERSE_PORTAL), c, AvatarMath.ULT_PULL_RADIUS, 24, 0.35);
        Tells.gather(sl, c.add(0, boss.getBbHeight() + 1.6, 0), 2.0, AvatarFx.accent(boss), 8);
    }
}
