package com.newuniverse.nusmp.entity.veldanava;

import com.newuniverse.nusmp.entity.zogratis.ZogratisVfx;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the eleven moves of Veldanava, written as small server-side functions of "which tick of the move is it". {@link VeldanavaBoss} owns the timeline (windup, active,
 * recovery) and calls {@link #begin} once and {@link #tick} every tick of the move; this class never keeps a field, everything a move must remember (its aim, its point, its
 * falling stars) lives on the boss and is cleared when the move ends. Every move is bounded by {@link VeldanavaRules}: at most {@code MAX_TARGETS} hurt per tick, at most
 * {@code MAX_MARKS} stars, a few blocks per tick (only with the griefBlocks rule on, all put back), and every effect packet goes through the boss's per-tick budget.
 *
 * <p>Worst case, per tick: Stardust Sweep 24 targets + 6 blocks + 1 packet; Gravity Pull 24 targets + 1 packet; Starfall 1 star (12 per cast) = 1 block + 24 targets + 3 packets;
 * Genesis 24 targets + 24 blocks + 3 packets once.
 *
 * <p>3.30.0, the seven Tensura-themed moves (anime mode only; costs per tick): Absolute Severance 24 targets + 5 blocks + 1 packet once; Black Lightning at most 3 bolts (3 packets, 24 targets once each);
 * Black Lightning Blast 24 targets once + 4 blocks per tick + 1 packet; Death Tornado 24 targets every 5 ticks + 1 packet every 10 ticks; Disintegration 24 targets + 2 blocks every 8 ticks + 2 packets;
 * Hell Flare 24 targets per tick for 24 ticks (the three rings share one list) + 1 packet; Megiddo at most 8 beams (8 sigils, 8 packets over 14 ticks, 24 targets and 1 block per beam).
 */
public final class VeldanavaMoves {
    private VeldanavaMoves() {}

    private static final int TEAL = 0x3EE0D0, GOLD = 0xF2D27A, WHITE = 0xFFFFFF, VIOLET = 0xB58CFF;
    private static final double SWEEP_LENGTH = 56.0, SWEEP_HALF_WIDTH = 3.5, PULL_RADIUS = 38.0;

    // 3.30.0: the colours and sizes of the Tensura-themed moves; the LEAD constants say how many ticks before its hit each effect is sent so that its impact frame lands with the hit (tuned on the previews)
    private static final int CRIMSON = 0xC21E3A, HELL = 0xE0301E, ASH = 0xA9AFB8, HOLY = 0xFFF1B0;
    private static final double SEVER_LENGTH = 44.0, SEVER_HALF_WIDTH = 1.6, BOLT_RADIUS = 3.2, BLAST_RADIUS = 16.0, TORNADO_RADIUS = 5.5, DISINT_RADIUS = 8.0, HELL_OUTER = 18.0, MEGIDDO_RADIUS = 3.5;
    private static final int SEVER_LEAD = 3, BOLT_LEAD = 3, BLAST_LEAD = 11, HELL_LEAD = 6, MEGIDDO_LEAD = 6;

    // ================================================================ start of a move
    public static void begin(VeldanavaBoss b, VeldanavaRules.Spec spec, ServerLevel sl) {
        LivingEntity target = b.castTargetEntity();
        if (target != null) b.setCastYaw(b.yawTo(target.position()));
        switch (spec.id()) {
            case VeldanavaRules.SWEEP -> {
                b.say("Stardust, answer me.");
                tellRay(b, sl, b.castYaw(), SWEEP_LENGTH, 1.2);
            }
            case VeldanavaRules.GRAVITY -> {
                b.say("Fall to me.");
                if (b.fxBudget()) ZogratisVfx.well(sl, b.position(), 12.0, spec.windup(), TEAL, true);
            }
            case VeldanavaRules.STARFALL -> pickMarks(b, sl);
            case VeldanavaRules.ROAR -> sl.playSound(null, b.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 5f, 0.6f);
            case VeldanavaRules.BITE -> { }
            case VeldanavaRules.GOD_SPEED -> {
                if (target != null) b.setCastPoint(target.position());
            }
            case VeldanavaRules.GENESIS_SLASH -> {
                b.say("Genesis.");
                tellRay(b, sl, b.castYaw(), 20.0, 2.5);
            }
            case VeldanavaRules.SERAPH_GUARD -> {
                b.say("Justice King, Michael.");
                b.startGuard(spec.windup() + spec.active());
                if (b.fxBudget()) ZogratisVfx.halo(sl, b, 4.0, spec.windup() + spec.active(), GOLD);
            }
            case VeldanavaRules.URIEL -> {
                b.say("Covenant King, Uriel.");
                Vec3 at = target == null ? b.position() : target.position();
                b.setCastPoint(at);
                if (b.fxBudget()) VfxSpawn.send(sl, VfxShape.SEAL_CHAINS, at, at.add(0, 1, 0), 0xFF000000 | GOLD, spec.windup() + spec.active(), 3.0f);
            }
            case VeldanavaRules.LAW -> {
                b.setCastVariant(b.getRandom().nextInt(3));
                b.say("Law Manipulation: " + lawName(b.castVariant()) + ".");
                if (b.fxBudget()) ZogratisVfx.divineSigil(sl, b.position(), 14.0, spec.windup(), VIOLET, true);
            }
            case VeldanavaRules.GENESIS -> {
                b.say("Genesis: Creation of the World.");
                if (b.fxBudget()) ZogratisVfx.divineSigil(sl, b.position(), 26.0, spec.windup(), WHITE, true);
                sl.playSound(null, b.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 6f, 0.5f);
            }
            case VeldanavaRules.ABSOLUTE_SEVERANCE -> {
                b.say("Absolute Severance.");
                tellRay(b, sl, b.castYaw(), SEVER_LENGTH, 0.8, WHITE);
            }
            case VeldanavaRules.BLACK_LIGHTNING -> {
                b.say("Black Lightning.");
                pickPoints(b, sl, VeldanavaRules.bolts(b.playersNear(b.arenaCenter(), VeldanavaRules.ARENA_RADIUS).size()), 1.5, spec, BOLT_RADIUS + 0.6, CRIMSON);
            }
            case VeldanavaRules.BLACK_BLAST -> {
                b.say("Black Lightning Blast.");
                if (b.fxBudget()) ZogratisVfx.divineSigil(sl, b.position(), BLAST_RADIUS, spec.windup() - BLAST_LEAD, CRIMSON, true);
            }
            case VeldanavaRules.DEATH_TORNADO -> {
                b.say("Death Tornado.");
                Vec3 at = VeldanavaBoss.ground(sl, b.position().add(dirOf(b.yawTo(target == null ? b.position().add(0, 0, 1) : target.position())).scale(9.0)));
                b.setCastPoint(at);
                if (b.fxBudget()) ZogratisVfx.well(sl, at, 7.0, spec.windup(), ASH, true);
            }
            case VeldanavaRules.DISINTEGRATION -> {
                b.say("Disintegration.");
                Vec3 at = VeldanavaBoss.ground(sl, target == null ? b.position() : target.position());
                b.setCastPoint(at);
                if (b.fxBudget()) ZogratisVfx.divineSigil(sl, at, DISINT_RADIUS, spec.windup(), ASH, true);
            }
            case VeldanavaRules.HELL_FLARE -> {
                b.say("Hell Flare.");
                if (b.fxBudget()) ZogratisVfx.divineSigil(sl, b.position(), 7.0, spec.windup() - HELL_LEAD, HELL, true);
            }
            case VeldanavaRules.MEGIDDO -> {
                b.say("Megiddo.");
                pickPoints(b, sl, VeldanavaRules.beams(b.playersNear(b.arenaCenter(), VeldanavaRules.ARENA_RADIUS).size()), 4.0, spec, MEGIDDO_RADIUS + 0.8, HOLY);
                sl.playSound(null, b.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 6f, 0.5f);
            }
            default -> { }
        }
    }

    // ================================================================ every tick of the move (1 .. length)
    public static void tick(VeldanavaBoss b, VeldanavaRules.Spec spec, int tick, ServerLevel sl) {
        int sinceActive = tick - spec.windup();           // < 0 in the windup, 0 on the first active tick
        boolean winding = sinceActive < 0;
        boolean active = sinceActive >= 0 && sinceActive < spec.active();
        LivingEntity target = b.castTargetEntity();
        switch (spec.id()) {
            case VeldanavaRules.SWEEP -> sweep(b, spec, sl, sinceActive, winding, active);
            case VeldanavaRules.GRAVITY -> gravity(b, spec, sl, sinceActive, winding, active);
            case VeldanavaRules.STARFALL -> starfall(b, spec, sl, tick, sinceActive, active);
            case VeldanavaRules.ROAR -> roar(b, sl, sinceActive);
            case VeldanavaRules.BITE -> bite(b, spec, sl, sinceActive);
            case VeldanavaRules.GOD_SPEED -> godSpeed(b, spec, sl, target, sinceActive);
            case VeldanavaRules.GENESIS_SLASH -> genesisSlash(b, spec, sl, target, tick, sinceActive);
            case VeldanavaRules.SERAPH_GUARD -> seraphGuard(b, sl, spec, sinceActive, active);
            case VeldanavaRules.URIEL -> uriel(b, spec, sl, sinceActive, active);
            case VeldanavaRules.LAW -> law(b, spec, sl, sinceActive);
            case VeldanavaRules.GENESIS -> genesis(b, spec, sl, tick, sinceActive);
            case VeldanavaRules.ABSOLUTE_SEVERANCE -> severance(b, spec, sl, target, tick, sinceActive);
            case VeldanavaRules.BLACK_LIGHTNING -> strikePoints(b, spec, sl, tick, BOLT_LEAD, true);
            case VeldanavaRules.BLACK_BLAST -> blackBlast(b, spec, sl, tick, sinceActive);
            case VeldanavaRules.DEATH_TORNADO -> tornado(b, spec, sl, target, sinceActive, active);
            case VeldanavaRules.DISINTEGRATION -> disintegration(b, spec, sl, sinceActive, active);
            case VeldanavaRules.HELL_FLARE -> hellFlare(b, spec, sl, tick, sinceActive);
            case VeldanavaRules.MEGIDDO -> strikePoints(b, spec, sl, tick, MEGIDDO_LEAD, false);
            default -> { }
        }
    }

    // ---------------------------------------------------------------- Stardust Sweep (dragon): a beam that turns one full circle
    private static void sweep(VeldanavaBoss b, VeldanavaRules.Spec spec, ServerLevel sl, int since, boolean winding, boolean active) {
        if (winding) {
            LivingEntity t = b.castTargetEntity();
            if (t != null && since < -8) b.setCastYaw(b.yawTo(t.position()));          // aims until the last 8 ticks, then locks
            b.turnTo(b.castYaw(), 20f);
            return;
        }
        if (!active) return;
        double yaw = VeldanavaRules.sweepYaw(b.castYaw(), since, spec.active());
        b.turnTo((float) yaw, 40f);
        Vec3 head = headPos(b);
        Vec3 dir = dirOf(yaw);
        if (since % 5 == 0 && b.fxBudget()) ZogratisVfx.ray(sl, head, groundAlong(sl, head, dir, SWEEP_LENGTH), 2.4, 8, TEAL, false);
        long now = sl.getGameTime();
        for (ServerPlayer p : b.playersNear(b.position(), SWEEP_LENGTH + 4.0)) {
            if (!VeldanavaRules.inBeam(p.getX() - b.getX(), p.getZ() - b.getZ(), yaw, SWEEP_LENGTH, SWEEP_HALF_WIDTH) || !b.canHit(p.getUUID(), now, 10)) continue;
            b.strike(p, VeldanavaRules.damage(p.getMaxHealth(), spec.share(), 6f, 40f, b.damageScale()) * 0.4f);     // many small hits while the beam passes
        }
        if (since % 3 == 0) destroyAlong(b, new Vec3(b.getX(), b.getY(), b.getZ()), dir, 6, 36, 6);
    }

    // ---------------------------------------------------------------- Gravity Pull (dragon): everything in 38 blocks falls toward him
    private static void gravity(VeldanavaBoss b, VeldanavaRules.Spec spec, ServerLevel sl, int since, boolean winding, boolean active) {
        if (winding) return;
        Vec3 c = b.position();
        if (active) {
            if (since % 20 == 0 && b.fxBudget()) ZogratisVfx.well(sl, c, 14.0, 24, TEAL, false);
            for (ServerPlayer p : b.playersNear(c, PULL_RADIUS)) {
                double d = Math.sqrt(p.distanceToSqr(c));
                double step = VeldanavaRules.pullStep(d, PULL_RADIUS);
                if (step <= 0.0) continue;
                Vec3 dir = c.subtract(p.position()).multiply(1, 0, 1).normalize();
                p.setDeltaMovement(p.getDeltaMovement().add(dir.scale(step)));
                p.hurtMarked = true;
            }
            return;
        }
        if (since == spec.active()) {                                                // the crush, once, when the pull ends
            if (b.fxBudget()) ZogratisVfx.gravityImpact(sl, c, 12.0, TEAL);
            for (ServerPlayer p : b.playersNear(c, 12.0))
                b.strike(p, VeldanavaRules.damage(p.getMaxHealth(), spec.share(), 5f, 30f, b.damageScale()));
        }
    }

    // ---------------------------------------------------------------- Starfall (dragon): marked circles, then stars
    private static void pickMarks(VeldanavaBoss b, ServerLevel sl) {
        List<ServerPlayer> ps = b.playersNear(b.arenaCenter(), VeldanavaRules.ARENA_RADIUS);
        int n = VeldanavaRules.marks(ps.size());
        b.marks().clear();
        for (int i = 0; i < n; i++) {
            ServerPlayer p = ps.get(i % ps.size());
            double ox = (b.getRandom().nextDouble() - 0.5) * 6.0, oz = (b.getRandom().nextDouble() - 0.5) * 6.0;
            b.marks().add(VeldanavaBoss.ground(sl, p.position().add(ox, 0, oz)));
        }
    }

    private static void starfall(VeldanavaBoss b, VeldanavaRules.Spec spec, ServerLevel sl, int tick, int since, boolean active) {
        List<Vec3> marks = b.marks();
        if (since < 0) {                                                            // one circle per tick of the windup
            int i = tick - 1;
            if (i >= 0 && i < marks.size() && b.fxBudget()) ZogratisVfx.divineSigil(sl, marks.get(i), 3.5, spec.windup() + spec.active() - tick, GOLD, true);
            return;
        }
        if (!active || marks.isEmpty()) return;
        int every = Math.max(1, spec.active() / marks.size());
        if (since % every != 0) return;
        int i = since / every;
        if (i >= marks.size()) return;
        Vec3 m = marks.get(i);
        if (b.fxBudget()) VfxSpawn.send(sl, VfxShape.DEMON_METEOR, m.add(0, 28, 0), m, 0xFF000000 | GOLD, 10, 3.0f);
        if (b.fxBudget()) ZogratisVfx.gravityImpact(sl, m, 4.5, GOLD);
        sl.playSound(null, BlockPos.containing(m), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2f, 1.3f);
        for (ServerPlayer p : b.playersNear(m, 4.0)) b.strike(p, VeldanavaRules.damage(p.getMaxHealth(), spec.share(), 6f, 40f, b.damageScale()));
        b.tryDestroy(BlockPos.containing(m).below());
    }

    // ---------------------------------------------------------------- Dragon Roar: a shockwave that throws everyone back (no damage)
    private static void roar(VeldanavaBoss b, ServerLevel sl, int since) {
        if (since != 0) return;
        Vec3 c = b.position();
        if (b.fxBudget()) VfxSpawn.send(sl, VfxShape.WIND_RING, c.add(0, 1, 0), c.add(0, 2, 0), 0xFF000000 | WHITE, 24, 20.0f);
        for (ServerPlayer p : b.playersNear(c, 32.0)) {
            Vec3 away = p.position().subtract(c).multiply(1, 0, 1);
            if (away.lengthSqr() < 1.0E-4) away = new Vec3(0, 0, 1);
            p.setDeltaMovement(p.getDeltaMovement().add(away.normalize().scale(1.4)).add(0, 0.45, 0));
            p.hurtMarked = true;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
    }

    // ---------------------------------------------------------------- Bite: the plain attack of the dragon
    private static void bite(VeldanavaBoss b, VeldanavaRules.Spec spec, ServerLevel sl, int since) {
        if (since < 0) {
            LivingEntity t = b.castTargetEntity();
            if (t != null) b.turnTo(b.yawTo(t.position()), 40f);
            return;
        }
        if (since != 0) return;
        Vec3 mouth = b.position().add(dirOf(b.getYRot()).scale(5.0)).add(0, 3.0, 0);
        if (b.fxBudget()) VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, mouth, mouth.add(0, 1, 0), 0xFF000000 | WHITE, 10, 3.0f);
        for (ServerPlayer p : b.playersNear(mouth, 9.0)) {
            if (p.getY() - b.getY() > 8.0) continue;
            b.strike(p, VeldanavaRules.damage(p.getMaxHealth(), spec.share(), 8f, 50f, b.damageScale()));
            Vec3 push = dirOf(b.getYRot()).scale(0.9).add(0, 0.5, 0);
            p.setDeltaMovement(p.getDeltaMovement().add(push));
            p.hurtMarked = true;
        }
    }

    // ---------------------------------------------------------------- God Speed (human bodies): he is simply there
    private static void godSpeed(VeldanavaBoss b, VeldanavaRules.Spec spec, ServerLevel sl, LivingEntity target, int since) {
        if (since != 0 || target == null) return;
        Vec3 from = b.position();
        Vec3 behind = target.position().subtract(dirOf(b.yawTo(target.position())).scale(2.2));
        Vec3 to = VeldanavaBoss.ground(sl, behind);
        if (!b.blinkTo(to)) return;
        b.turnTo(b.yawTo(target.position()), 360f);
        if (b.fxBudget()) VfxSpawn.send(sl, VfxShape.LIGHT_BLADE, from.add(0, 1.2, 0), to.add(0, 1.2, 0), 0xFF000000 | WHITE, 6, 1.4f);
        for (ServerPlayer p : b.playersNear(to, 2.8)) b.strike(p, VeldanavaRules.damage(p.getMaxHealth(), spec.share(), 6f, 30f, b.damageScale()));
    }

    // ---------------------------------------------------------------- Genesis Slash: a huge crescent in front of him
    private static void genesisSlash(VeldanavaBoss b, VeldanavaRules.Spec spec, ServerLevel sl, LivingEntity target, int tick, int since) {
        if (since < 0) {
            if (target != null && tick < spec.windup() - 8) b.setCastYaw(b.yawTo(target.position()));
            b.turnTo(b.castYaw(), 60f);
            return;
        }
        if (since != 0) return;
        double yaw = b.castYaw();
        Vec3 o = b.position().add(0, 1.0, 0);
        Vec3 dir = dirOf(yaw);
        if (b.fxBudget()) VfxSpawn.send(sl, VfxShape.LIGHT_BLADE, o, o.add(dir.scale(22.0)), 0xFF000000 | TEAL, 8, 4.0f);
        if (b.fxBudget()) ZogratisVfx.tear(sl, o.add(dir.scale(2.0)), o.add(dir.scale(24.0)), 2.5, 12, WHITE);
        sl.playSound(null, b.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 3f, 0.5f);
        for (ServerPlayer p : b.playersNear(b.position(), 22.0)) {
            Vec3 rel = p.position().subtract(b.position()).multiply(1, 0, 1);
            double len = rel.length();
            if (len > 1.0E-3 && rel.scale(1.0 / len).dot(dir) < 0.5) continue;           // inside a cone of about 120 degrees
            b.strike(p, VeldanavaRules.damage(p.getMaxHealth(), spec.share(), 8f, 60f, b.damageScale()));
        }
        destroyAlong(b, o, dir, 2, 18, 4);
    }

    // ---------------------------------------------------------------- Justice King Michael: nothing gets through; at the end a pulse throws the attackers back
    private static void seraphGuard(VeldanavaBoss b, ServerLevel sl, VeldanavaRules.Spec spec, int since, boolean active) {
        if (active && since % 20 == 0 && b.fxBudget()) ZogratisVfx.divineSigil(sl, b.position(), 6.0, 24, GOLD, false);
        if (since == spec.active() - 1) {
            Vec3 c = b.position();
            if (b.fxBudget()) VfxSpawn.send(sl, VfxShape.WIND_RING, c.add(0, 1, 0), c.add(0, 2, 0), 0xFF000000 | GOLD, 18, 9.0f);
            for (ServerPlayer p : b.playersNear(c, 9.0)) {
                Vec3 away = p.position().subtract(c).multiply(1, 0, 1);
                if (away.lengthSqr() < 1.0E-4) away = new Vec3(0, 0, 1);
                p.setDeltaMovement(p.getDeltaMovement().add(away.normalize().scale(1.1)).add(0, 0.35, 0));
                p.hurtMarked = true;
            }
        }
    }

    // ---------------------------------------------------------------- Covenant King Uriel: a prison of chains where the target stood
    private static void uriel(VeldanavaBoss b, VeldanavaRules.Spec spec, ServerLevel sl, int since, boolean active) {
        if (!active) return;
        Vec3 c = b.castPoint() == null ? b.position() : b.castPoint();
        if (since % 5 == 0) {
            for (ServerPlayer p : b.playersNear(c, 2.6)) {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 6, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.JUMP, 12, 128, false, false));
            }
        }
        if (since % 20 == 10) {
            for (ServerPlayer p : b.playersNear(c, 2.6)) b.strike(p, VeldanavaRules.damage(p.getMaxHealth(), spec.share(), 4f, 20f, b.damageScale()));
        }
    }

    // ---------------------------------------------------------------- Law Manipulation: for a few seconds one rule of the world is rewritten
    public static String lawName(int variant) {
        return switch (variant) {
            case 0 -> "Weight (everything is heavy)";
            case 1 -> "Fragility (every blow is weaker)";
            default -> "Decay (the body wastes)";
        };
    }

    private static void law(VeldanavaBoss b, VeldanavaRules.Spec spec, ServerLevel sl, int since) {
        if (since != 0) return;
        if (b.fxBudget()) VfxSpawn.send(sl, VfxShape.WIND_RING, b.position().add(0, 1, 0), b.position().add(0, 2, 0), 0xFF000000 | VIOLET, 20, 30.0f);
        for (ServerPlayer p : b.playersNear(b.position(), 30.0)) {
            switch (b.castVariant()) {
                case 0 -> {
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 2));
                    p.addEffect(new MobEffectInstance(MobEffects.JUMP, 140, 128, false, false));
                }
                case 1 -> {
                    p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 3));
                    p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 140, 2));
                }
                default -> {
                    p.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
                    p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 1));
                }
            }
        }
    }

    // ---------------------------------------------------------------- Genesis: Creation of the World (Dark Avatar's ultimate)
    private static void genesis(VeldanavaBoss b, VeldanavaRules.Spec spec, ServerLevel sl, int tick, int since) {
        Vec3 c = b.position();
        if (since < 0) {                                                            // the charge: a light that grows for five seconds
            if (tick % 20 == 0 && b.fxBudget()) VfxSpawn.send(sl, VfxShape.BORN_NOVA, c.add(0, 1, 0), c.add(0, 2, 0), 0xFF000000 | WHITE, 30, 6.0f + tick * 0.25f);
            if (tick % 25 == 0) sl.playSound(null, b.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 6f, 0.5f + tick / 200f);
            return;
        }
        if (since == 0) {                                                           // the release
            if (b.fxBudget()) VfxSpawn.send(sl, VfxShape.BORN_NOVA, c.add(0, 1, 0), c.add(0, 2, 0), 0xFF000000 | WHITE, 40, 56.0f);
            if (b.fxBudget()) ZogratisVfx.spaceShatter(sl, c.add(0, 1, 0), 30.0, TEAL);
            if (b.fxBudget()) VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, c.add(0, 2, 0), c.add(0, 3, 0), 0xFF000000 | WHITE, 30, 14.0f);
            sl.playSound(null, b.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 8f, 0.4f);
            for (ServerPlayer p : b.playersNear(c, 60.0)) {
                float fall = VeldanavaRules.falloff(Math.sqrt(p.distanceToSqr(c)), 12.0, 56.0, 0.35f);
                b.strike(p, VeldanavaRules.damage(p.getMaxHealth(), spec.share(), 10f, 120f, b.damageScale()) * fall);
                Vec3 away = p.position().subtract(c).multiply(1, 0, 1);
                if (away.lengthSqr() < 1.0E-4) away = new Vec3(0, 0, 1);
                p.setDeltaMovement(p.getDeltaMovement().add(away.normalize().scale(1.2 * fall)).add(0, 0.5, 0));
                p.hurtMarked = true;
            }
        }
        if (since < spec.active()) {                                                // the wave eats a ring of blocks while it passes (griefBlocks only)
            double r = 6.0 + 4.0 * since;
            for (int k = 0; k < 8; k++) {
                double a = (Math.PI * 2.0) * (k + since * 0.37) / 8.0;
                b.tryDestroy(BlockPos.containing(c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r));
            }
        }
    }

    // ================================================================ 3.30.0: the Tensura-themed moves
    // ---------------------------------------------------------------- Absolute Severance: a cut through space that nothing blocks
    private static void severance(VeldanavaBoss b, VeldanavaRules.Spec spec, ServerLevel sl, LivingEntity target, int tick, int since) {
        if (since < 0) {
            if (target != null && tick < spec.windup() - 8) b.setCastYaw(b.yawTo(target.position()));            // aims until the last 8 ticks, then locks
            b.turnTo(b.castYaw(), 60f);
            if (tick == spec.windup() - SEVER_LEAD) {
                Vec3 o = b.position().add(0, 1.2, 0);
                if (b.fxBudget()) VfxSpawn.send(sl, VfxShape.VELD_SEVERANCE, o, o.add(dirOf(b.castYaw()).scale(SEVER_LENGTH)), 0xFF000000 | WHITE, 16, (float) SEVER_HALF_WIDTH);
            }
            return;
        }
        if (since != 0) return;
        double yaw = b.castYaw();
        Vec3 dir = dirOf(yaw);
        Vec3 o = b.position().add(0, 1.2, 0);
        sl.playSound(null, b.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 4f, 0.5f);
        for (ServerPlayer p : b.playersNear(b.position(), SEVER_LENGTH + 2.0)) {
            if (!VeldanavaRules.inBeam(p.getX() - b.getX(), p.getZ() - b.getZ(), yaw, SEVER_LENGTH, SEVER_HALF_WIDTH) || !VeldanavaRules.withinHeight(p.getY() + 0.9 - o.y, 5.0)) continue;
            b.strike(p, VeldanavaRules.damage(p.getMaxHealth(), spec.share(), 8f, 80f, b.damageScale()));        // "severance": nothing blocks it, it is true damage
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }
        destroyAlong(b, o, dir, 2, 40, 3);
    }

    // ---------------------------------------------------------------- shared by Black Lightning and Megiddo: points picked at the start, struck one after another
    /** Picks up to 'count' ground points round the players (the target first), marks each with a sigil that lasts until its strike. At most MAX_MARKS points. */
    private static void pickPoints(VeldanavaBoss b, ServerLevel sl, int count, double spread, VeldanavaRules.Spec spec, double sigilRadius, int color) {
        List<ServerPlayer> ps = b.playersNear(b.arenaCenter(), VeldanavaRules.ARENA_RADIUS);
        b.marks().clear();
        if (ps.isEmpty()) return;
        LivingEntity first = b.castTargetEntity();
        int n = Math.min(count, VeldanavaRules.MAX_MARKS);
        for (int i = 0; i < n; i++) {
            LivingEntity who = i == 0 && first != null ? first : ps.get(i % ps.size());
            double ox = i == 0 ? 0.0 : (b.getRandom().nextDouble() - 0.5) * 2.0 * spread, oz = i == 0 ? 0.0 : (b.getRandom().nextDouble() - 0.5) * 2.0 * spread;
            Vec3 m = VeldanavaBoss.ground(sl, who.position().add(ox, 0, oz));
            b.marks().add(m);
            int until = spec.windup() + VeldanavaRules.strikeTick(i, n, spec.active());
            if (b.fxBudget()) ZogratisVfx.divineSigil(sl, m, sigilRadius, until, color, true);
        }
    }

    /** Sends each point's effect 'lead' ticks before its strike and lands the strike: bolts (Black Lightning) or beams (Megiddo). */
    private static void strikePoints(VeldanavaBoss b, VeldanavaRules.Spec spec, ServerLevel sl, int tick, int lead, boolean bolt) {
        List<Vec3> marks = b.marks();
        int n = marks.size();
        for (int i = 0; i < n; i++) {
            int at = spec.windup() + VeldanavaRules.strikeTick(i, n, spec.active());
            Vec3 m = marks.get(i);
            if (tick == at - lead) {
                if (bolt) { if (b.fxBudget()) VfxSpawn.send(sl, VfxShape.VELD_BLACK_LIGHTNING, m.add(0, 40, 0), m, 0xFF000000 | CRIMSON, 12, (float) BOLT_RADIUS); }
                else if (b.fxBudget()) VfxSpawn.send(sl, VfxShape.VELD_MEGIDDO, m.add(0, 60, 0), m, 0xFF000000 | HOLY, 18, (float) MEGIDDO_RADIUS);
            }
            if (tick != at) continue;
            sl.playSound(null, BlockPos.containing(m), bolt ? SoundEvents.LIGHTNING_BOLT_THUNDER : SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, bolt ? 3f : 4f, bolt ? 0.7f : 0.8f);
            double r = bolt ? BOLT_RADIUS : MEGIDDO_RADIUS;
            for (ServerPlayer p : b.playersNear(m, r)) {
                if (p.getY() - m.y > 8.0) continue;
                b.strike(p, bolt ? VeldanavaRules.damage(p.getMaxHealth(), spec.share(), 6f, 45f, b.damageScale()) : VeldanavaRules.damage(p.getMaxHealth(), spec.share(), 12f, 110f, b.damageScale()));
            }
            b.tryDestroy(BlockPos.containing(m).below());
        }
    }

    // ---------------------------------------------------------------- Black Lightning Blast: a dark orb collapses and bursts into black lightning all round
    private static void blackBlast(VeldanavaBoss b, VeldanavaRules.Spec spec, ServerLevel sl, int tick, int since) {
        Vec3 c = b.position();
        if (since < 0) {
            if (tick == spec.windup() - BLAST_LEAD && b.fxBudget()) VfxSpawn.send(sl, VfxShape.VELD_BLACK_BLAST, c, c, 0xFF000000 | CRIMSON, 26, (float) BLAST_RADIUS);
            return;
        }
        if (since == 0) {
            sl.playSound(null, b.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 6f, 0.5f);
            for (ServerPlayer p : b.playersNear(c, BLAST_RADIUS + 2.0)) {
                double d = Math.sqrt(p.distanceToSqr(c));
                float fall = VeldanavaRules.falloff(d, 4.0, BLAST_RADIUS, 0.4f);
                b.strike(p, VeldanavaRules.damage(p.getMaxHealth(), spec.share(), 8f, 60f, b.damageScale()) * fall);
                Vec3 away = p.position().subtract(c).multiply(1, 0, 1);
                if (away.lengthSqr() < 1.0E-4) away = new Vec3(0, 0, 1);
                p.setDeltaMovement(p.getDeltaMovement().add(away.normalize().scale(0.9 * fall)).add(0, 0.35, 0));
                p.hurtMarked = true;
            }
        }
        if (since < spec.active()) {                                                        // the ring of the blast eats a few blocks while it passes (griefBlocks only)
            double r = 3.0 + 2.0 * since;
            for (int k = 0; k < 4; k++) {
                double a = (Math.PI * 2.0) * (k + since * 0.31) / 4.0;
                b.tryDestroy(BlockPos.containing(c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r));
            }
        }
    }

    // ---------------------------------------------------------------- Death Tornado: a funnel that walks toward the target, lifting and spinning everything it catches (slower than a runner)
    private static void tornado(VeldanavaBoss b, VeldanavaRules.Spec spec, ServerLevel sl, LivingEntity target, int since, boolean active) {
        if (!active || b.castPoint() == null) return;
        Vec3 pt = b.castPoint();
        Vec3 goal = target == null ? pt : target.position();
        Vec3 to = goal.subtract(pt).multiply(1, 0, 1);
        double d = to.length();
        if (d > 0.5) {
            double step = Math.min(VeldanavaRules.TORNADO_SPEED, d);
            Vec3 next = pt.add(to.scale(step / d));
            if (next.distanceToSqr(b.arenaCenter()) < (double) (VeldanavaRules.ARENA_RADIUS - 2) * (VeldanavaRules.ARENA_RADIUS - 2)) pt = next;
        }
        if (since % 5 == 0) pt = VeldanavaBoss.ground(sl, pt);
        b.setCastPoint(pt);
        if (since % 10 == 0 && b.fxBudget()) VfxSpawn.send(sl, VfxShape.VELD_TORNADO, pt, pt.add(0, 1, 0), 0xFF000000 | ASH, 14, (float) TORNADO_RADIUS);
        if (since % 5 != 0) return;
        long now = sl.getGameTime();
        for (ServerPlayer p : b.playersNear(pt, TORNADO_RADIUS)) {
            if (!b.canHit(p.getUUID(), now, 8)) continue;
            b.strike(p, VeldanavaRules.damage(p.getMaxHealth(), spec.share(), 3f, 14f, b.damageScale()));
            Vec3 rel = p.position().subtract(pt).multiply(1, 0, 1);
            Vec3 spin = rel.lengthSqr() < 1.0E-4 ? Vec3.ZERO : new Vec3(-rel.z, 0, rel.x).normalize().scale(0.35).subtract(rel.normalize().scale(0.15));
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x * 0.5 + spin.x, Math.min(0.9, v.y + 0.5), v.z * 0.5 + spin.z);
            p.hurtMarked = true;
        }
    }

    // ---------------------------------------------------------------- Disintegration: a zone where everything turns to dust, pulse after pulse
    private static void disintegration(VeldanavaBoss b, VeldanavaRules.Spec spec, ServerLevel sl, int since, boolean active) {
        if (!active || b.castPoint() == null) return;
        Vec3 c = b.castPoint();
        if ((since == 0 || since == 28) && b.fxBudget()) VfxSpawn.send(sl, VfxShape.VELD_DISINTEGRATION, c, c.add(0, 1, 0), 0xFF000000 | ASH, 30, (float) DISINT_RADIUS);
        if (since % 8 != 0) return;
        long now = sl.getGameTime();
        for (ServerPlayer p : b.playersNear(c, DISINT_RADIUS)) {
            if (!b.canHit(p.getUUID(), now, 6)) continue;
            b.strike(p, VeldanavaRules.damage(p.getMaxHealth(), spec.share(), 3f, 12f, b.damageScale()));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1));
        }
        for (int k = 0; k < 2; k++) {
            double a = b.getRandom().nextDouble() * Math.PI * 2.0, r = b.getRandom().nextDouble() * (DISINT_RADIUS - 1.0);
            b.tryDestroy(BlockPos.containing(c.x + Math.cos(a) * r, c.y - 1.0, c.z + Math.sin(a) * r));
        }
    }

    // ---------------------------------------------------------------- Hell Flare: three rings of hellfire race out from him one after another
    private static void hellFlare(VeldanavaBoss b, VeldanavaRules.Spec spec, ServerLevel sl, int tick, int since) {
        Vec3 c = b.position();
        if (since < 0) {
            if (tick == spec.windup() - HELL_LEAD && b.fxBudget()) VfxSpawn.send(sl, VfxShape.VELD_HELL_FLARE, c, c, 0xFF000000 | HELL, 30, (float) HELL_OUTER);
            return;
        }
        if (since > 24) return;
        if (since == 0) sl.playSound(null, b.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 5f, 0.4f);
        long now = sl.getGameTime();
        List<ServerPlayer> near = b.playersNear(c, HELL_OUTER + 3.0);
        for (int ring = 0; ring < 3; ring++) {
            double radius = VeldanavaRules.hellRingRadius(ring, since, HELL_OUTER);
            if (radius <= 0.0) continue;
            for (ServerPlayer p : near) {
                double d = Math.sqrt((p.getX() - c.x) * (p.getX() - c.x) + (p.getZ() - c.z) * (p.getZ() - c.z));
                if (!VeldanavaRules.inRingBand(d, radius, 2.2) || !VeldanavaRules.withinHeight(p.getY() - c.y, 6.0) || !b.canHit(p.getUUID(), now, 3)) continue;
                b.strike(p, VeldanavaRules.damage(p.getMaxHealth(), spec.share(), 6f, 40f, b.damageScale()));
                p.igniteForSeconds(4f);
            }
        }
    }

    // ================================================================ small helpers
    /** The unit vector a Minecraft yaw (degrees) looks along. */
    static Vec3 dirOf(double yawDeg) {
        double r = Math.toRadians(yawDeg);
        return new Vec3(-Math.sin(r), 0, Math.cos(r));
    }

    /** Where a dragon's head is: in front of the body and high. */
    static Vec3 headPos(VeldanavaBoss b) { return b.position().add(dirOf(b.getYRot()).scale(6.0)).add(0, 6.0, 0); }

    /** The point 'length' blocks along 'dir' from 'from', at the height of the floor there (the beam is drawn from the head down to it). */
    private static Vec3 groundAlong(ServerLevel sl, Vec3 from, Vec3 dir, double length) {
        Vec3 p = from.add(dir.scale(length));
        return VeldanavaBoss.ground(sl, new Vec3(p.x, from.y - 6.0, p.z));
    }

    /** Removes (griefBlocks only, a few, all recorded) the blocks along a path: from 'first' to 'last' blocks out, every 'step', stopping when the boss's block budget is spent. */
    private static void destroyAlong(VeldanavaBoss b, Vec3 origin, Vec3 dir, int first, int last, int step) {
        for (int i = first; i <= last; i += step) if (!b.tryDestroy(BlockPos.containing(origin.add(dir.scale(i))))) return;
    }

    /** A thin line toward the aim: the warning before a beam or a slash. */
    private static void tellRay(VeldanavaBoss b, ServerLevel sl, double yaw, double length, double width) { tellRay(b, sl, yaw, length, width, TEAL); }

    private static void tellRay(VeldanavaBoss b, ServerLevel sl, double yaw, double length, double width, int color) {
        if (!b.fxBudget()) return;
        Vec3 o = b.position().add(0, 1.5, 0);
        ZogratisVfx.ray(sl, o, o.add(dirOf(yaw).scale(length)), width, 30, color, true);
    }
}
