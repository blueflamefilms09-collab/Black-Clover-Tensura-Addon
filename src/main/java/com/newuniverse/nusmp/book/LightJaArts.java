package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.prop.LightJaProps;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.song.SongTargets;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Why: the casts of the Light Magic remake after the Black Clover wiki (the table is {@link LightBoneJa}). Light's defining trait is speed, so its blows are as fast as before and every hit is a
 * light hit ({@link LightBook#lightHit}: armour ignored, the spirit cut, only an Aura barrier holds it back). What changed is the shape of each spell and what is seen: every spell has a 3D prop
 * (the {@link PropKind#LIGHT_JA} variants of {@link LightJaProps}), the custom particles and the old light effects.
 * <p>
 * Cost rules (nothing here may lag a public server): the server decides everything and the numbers are capped in {@link LightBoneJa}; a cast touches at most {@link LightBoneJa#MAX_HITS}
 * foes per pulse; every search for targets is made once per pulse or per landing tick (never per tick, never per prop); props are few, capped per caster and for the server
 * ({@link LightBoneJaBudget}) and never moved by the server (the painter moves flying ones from their age); delayed parts check that the caster is still there, alive and in the same
 * dimension; the casts are timed by the performance probe (section {@code s.book.light_ja}).
 * <ul>
 *   <li>Light Sword of Judgment (judgment): 4 to 8 shards with four lines through each rise beside the caster and are thrown one after another at light speed; each ends in a cross-slash.</li>
 *   <li>Light Shaft of Divine Punishment (divine_punishment): a ring marks the ground (it follows a marked foe), then a giant column of light falls and pulses three times.</li>
 *   <li>Healing Ray of Light (healing_ray): a sphere of white light with a halo ring settles on an ally (or you), heals three times and burns away an affliction.</li>
 *   <li>Lamp of Avior Gloria (lamp_of_avior): a huge black-and-white burst grows over the caster and goes off with a flash and spikes over a wide radius.</li>
 *   <li>Bright Judgment Whip, Heaven-Splitting Flash, Arrows of Judgment, Lamp of Avior, Absolute Monarch of Divine Brilliance: appended pages, described at their casts below.</li>
 * </ul>
 */
public final class LightJaArts {
    private LightJaArts() {}

    private static final PerfProbe.Section P = LightBoneJaKit.LIGHT_PROBE;

    /** The page casts, timed by the performance probe. */
    public static BookPage.Cast sword() { return LightBoneJaKit.probed(P, LightJaArts::sword); }
    public static BookPage.Cast shaft() { return LightBoneJaKit.probed(P, LightJaArts::shaft); }
    public static BookPage.Cast healingRay() { return LightBoneJaKit.probed(P, LightJaArts::healingRay); }
    public static BookPage.Cast gloria() { return LightBoneJaKit.probed(P, LightJaArts::gloria); }
    public static BookPage.Cast whip() { return LightBoneJaKit.probed(P, LightJaArts::whip); }
    public static BookPage.Cast flash() { return LightBoneJaKit.probed(P, LightJaArts::flash); }
    public static BookPage.Cast arrows() { return LightBoneJaKit.probed(P, LightJaArts::arrows); }
    public static BookPage.Cast lamp() { return LightBoneJaKit.probed(P, LightJaArts::lamp); }
    public static BookPage.Cast monarch() { return LightBoneJaKit.probed(P, LightJaArts::monarch); }

    // ================================================================ Light Sword of Judgment
    /** Light Sword of Judgment: the swords rise in an arc beside the caster, then cross the field one after another. */
    static boolean sword(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        LivingEntity t = GrimoireBook.target(p, 40);
        int n = LightBoneJa.swordCount(EnergyBridge.modifier(p));
        float size = GrimoireBook.size(i, p) * (0.9f + 0.1f * EnergyBridge.scale(p));
        Vec3 look = p.getViewVector(1f), side = LightBoneJaKit.right(look);
        b.castCircle(p, 0.8f);
        AtomicInteger crosses = new AtomicInteger();
        double speed = LightBoneJa.swordSpeed(EnergyBridge.power(p));
        for (int k = 0; k < n; k++) {
            double f = n == 1 ? 0 : (k / (double) (n - 1)) * 2 - 1;                                  // -1 .. 1 across the arc
            Vec3 from = p.getEyePosition().add(side.scale(f * 1.7)).add(look.scale(0.5)).add(0, 0.45 + 0.5 * (1 - f * f), 0);
            LightBoneJaKit.later(level, 1 + k, P, () -> {
                if (!LightBoneJaKit.here(p, level)) return;
                Vec3 aim = t != null && t.isAlive() ? LightBoneJaKit.centre(t) : p.getEyePosition().add(p.getViewVector(1f).scale(30));
                Vec3 dir = aim.subtract(from);
                if (dir.lengthSqr() < 1e-6) dir = look;
                dir = dir.normalize();
                Vec3 stop = LightBoneJaKit.rayEnd(level, p, from, dir, 48);
                b.vfx(p, VfxShape.LIGHT_BLADE, from, t != null && t.isAlive() ? LightBoneJaKit.centre(t) : stop, 10, 0.7f + 0.2f * size);
                final Vec3 flight = dir;
                LightBoneJaKit.projectile(p, PropKind.LIGHT_JA, LightJaProps.SWORD, from, dir, speed, 0.7, 9, true,
                        (bolt, target) -> LightBook.lightHit(b, i, p, target, mode, LightBoneJa.SWORD_RAW),
                        (bolt, at) -> {
                            b.vfx(p, VfxShape.LIGHT_FLARE, at, at, 12, 0.8f);
                            LightBoneJaKit.lightBurst(level, at, 8, 0.5);
                            if (crosses.getAndIncrement() < 4) LightBoneJaKit.prop(p, PropKind.LIGHT_JA, LightJaProps.CROSS, at, flight, size, 9);
                        }, size);
            });
        }
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 1.8f);
        return true;
    }

    // ================================================================ Light Shaft of Divine Punishment
    /** Light Shaft of Divine Punishment: a ring on the ground, then a giant column of light from the sky that pulses three times. */
    static boolean shaft(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        LivingEntity t = GrimoireBook.target(p, 40);
        Vec3 spot = t != null ? t.position() : LightBoneJaKit.ground(level, p, GrimoireBook.aim(p, 40).add(0, 1.5, 0), 40);
        float size = GrimoireBook.size(i, p);
        double radius = LightBoneJa.shaftRadius(EnergyBridge.scale(p)) * size;
        b.castCircle(p, 1f);
        MagicPropEntity col = LightBoneJaKit.prop(p, PropKind.LIGHT_JA, LightJaProps.COLUMN, spot, new Vec3(0, 0, 1), (float) (radius / 2.6), LightBoneJa.SHAFT_LIFE);
        if (col != null && t != null) col.setTarget(t);                                           // the ring follows a marked foe until the column falls
        b.vfx(p, VfxShape.ELF_CIRCLE, spot.add(0, 0.05, 0), spot.add(0, 1, 0), 30, 0.8f);
        for (int k = 0; k < LightBoneJa.SHAFT_PULSES; k++) {
            LightBoneJaKit.later(level, LightBoneJa.SHAFT_TELEGRAPH + k * LightBoneJa.SHAFT_GAP, P, () -> {
                if (!LightBoneJaKit.here(p, level)) return;
                Vec3 at = col != null && !col.isRemoved() ? col.position() : spot;
                b.vfx(p, VfxShape.LIGHT_BLADE, at.add(0, 22, 0), at, 6, 1.6f);
                b.vfx(p, VfxShape.LIGHT_FLARE, at.add(0, 0.3, 0), at, 12, 1.4f);
                LightBoneJaKit.lightBurst(level, at.add(0, 0.6, 0), 14, radius * 0.5);
                for (LivingEntity e : LightBoneJaKit.nearest(GrimoireBook.around(p, at, radius + 0.4), at, LightBoneJa.MAX_HITS)) {
                    LightBook.lightHit(b, i, p, e, mode, LightBoneJa.SHAFT_RAW);
                    e.setDeltaMovement(e.getDeltaMovement().add(0, 0.25, 0));
                    e.hurtMarked = true;
                }
            });
        }
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.9f);
        return true;
    }

    // ================================================================ Healing Ray of Light
    /** Healing Ray of Light: a sphere of white light with a halo settles on an ally (or you) and mends wounds three times. */
    static boolean healingRay(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        LivingEntity look = GrimoireBook.target(p, 24);
        LivingEntity who = look != null && SongTargets.isAlly(p, look) ? look : p;
        float scale = EnergyBridge.scale(p);
        MagicPropEntity orb = LightBoneJaKit.prop(p, PropKind.LIGHT_JA, LightJaProps.HEAL, who.position().add(0, who.getBbHeight() * 0.55, 0), new Vec3(0, 0, 1), 1f,
                LightBoneJa.HEAL_PULSES * LightBoneJa.HEAL_GAP + 10);
        if (orb != null) orb.setTarget(who);
        b.vfx(p, VfxShape.LIGHT_BLADE, p.getEyePosition(), LightBoneJaKit.centre(who), 10, 0.6f);
        for (int k = 0; k < LightBoneJa.HEAL_PULSES; k++) {
            final int pulse = k;
            LightBoneJaKit.later(level, 2 + k * LightBoneJa.HEAL_GAP, P, () -> {
                if (!LightBoneJaKit.here(p, level) || !who.isAlive()) return;
                Vec3 c = LightBoneJaKit.centre(who);
                BalanceLaw.heal(who, who.getMaxHealth() * LightBoneJa.HEAL_SHARE * scale);
                if (pulse == 0) cleanse(who);
                int shared = 0;
                for (LivingEntity a : SongTargets.allies(p, c, LightBoneJa.HEAL_RADIUS)) {
                    if (a == who || shared >= LightBoneJa.HEAL_MAX_ALLIES) continue;
                    shared++;
                    BalanceLaw.heal(a, a.getMaxHealth() * LightBoneJa.HEAL_SHARE * 0.5f * scale);
                }
                b.vfx(p, VfxShape.LIGHT_FLARE, c, who.position(), 16, 1f);
                LightBoneJaKit.lightPuff(level, c, 12, 0.7);
            });
        }
        level.playSound(null, who.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.6f);
        return true;
    }

    /** Burns away one harmful effect (the first it finds). */
    static void cleanse(LivingEntity who) {
        for (MobEffectInstance e : new ArrayList<>(who.getActiveEffects()))
            if (!e.getEffect().value().isBeneficial()) { who.removeEffect(e.getEffect()); return; }
    }

    // ================================================================ Lamp of Avior Gloria
    /** Lamp of Avior Gloria: the burst of light grows over the caster, then goes off over a wide radius. */
    static boolean gloria(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        float size = GrimoireBook.size(i, p);
        double radius = LightBoneJa.gloriaRadius(size);
        b.castCircle(p, 1.6f);
        MagicPropEntity lamp = LightBoneJaKit.prop(p, PropKind.LIGHT_JA, LightJaProps.GLORIA, p.position().add(0, 5.0, 0), new Vec3(0, 0, 1), (float) (radius / 14.0),
                LightBoneJa.GLORIA_CHARGE + 18);
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.5f);
        LightBoneJaKit.later(level, LightBoneJa.GLORIA_CHARGE, P, () -> {
            if (!LightBoneJaKit.here(p, level)) { if (lamp != null) lamp.expire(); return; }
            Vec3 c = p.position().add(0, 4.5, 0);
            b.vfx(p, VfxShape.LIGHT_FLARE, c, c, 22, 3.0f);
            LightBoneJaKit.lightBurst(level, c, 40, radius * 0.45);
            level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 1.9f);
            blast(b, i, p, mode, p.position(), radius, LightBoneJa.GLORIA_RAW, 60, 0.9);
            LightBoneJaKit.later(level, 6, P, () -> {
                if (LightBoneJaKit.here(p, level)) blast(b, i, p, mode, p.position(), radius, LightBoneJa.GLORIA_RAW * 0.6f, 0, 0.4);
            });
        });
        return true;
    }

    /** A burst of light: every foe within the radius (the nearest few) takes a light hit that falls off with the distance, is blinded and thrown back. */
    static void blast(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, Vec3 centre, double radius, float raw, int blind, double knock) {
        for (LivingEntity e : LightBoneJaKit.nearest(GrimoireBook.around(p, centre, radius), centre, LightBoneJa.MAX_HITS)) {
            double d = e.position().distanceTo(centre) / Math.max(0.1, radius);
            LightBook.lightHit(b, i, p, e, mode, (float) (raw * (1.0 - 0.4 * Math.min(1.0, d))));
            if (blind > 0) e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BalanceLaw.isBoss(e) ? blind / 3 : blind, 0));
            Vec3 away = e.position().subtract(centre).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-6) {
                away = away.normalize().scale(knock * (BalanceLaw.isBoss(e) ? 0.3 : 1.0));
                e.setDeltaMovement(e.getDeltaMovement().add(away.x, 0.2, away.z));
                e.hurtMarked = true;
            }
        }
    }

    // ================================================================ Bright Judgment Whip
    /** Bright Judgment Whip: three long curling lashes of light fan out from the hand. */
    static boolean whip(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        float size = GrimoireBook.size(i, p);
        b.castCircle(p, 0.9f);
        for (int k = 0; k < LightBoneJa.WHIP_LASHES; k++) {
            final int lash = k;
            LightBoneJaKit.later(level, k * 4, P, () -> {
                if (!LightBoneJaKit.here(p, level)) return;
                Vec3 look = p.getViewVector(1f);
                Vec3 dir = LightBoneJaKit.turn(look, LightBoneJa.WHIP_FAN_DEG[lash]).normalize();
                Vec3 from = p.getEyePosition().add(0, -0.35, 0).add(dir.scale(0.5));
                double reach = LightBoneJa.WHIP_REACH * size;
                Vec3 to = LightBoneJaKit.rayEnd(level, p, from, dir, reach);
                LightBoneJaKit.prop(p, PropKind.LIGHT_JA, LightJaProps.WHIP | (lash << 8), from, dir, size, 8);
                b.vfx(p, VfxShape.LIGHT_BLADE, from, to, 6, 0.9f);
                LightBoneJaKit.lightPuff(level, to, 8, 0.4);
                for (LivingEntity e : LightBoneJaKit.nearest(GrimoireBook.along(p, from, to, LightBoneJa.WHIP_WIDTH), from, 8)) {
                    LightBook.lightHit(b, i, p, e, mode, LightBoneJa.WHIP_RAW);
                    Vec3 pull = from.subtract(e.position()).multiply(1, 0, 1);
                    if (pull.lengthSqr() > 1e-6 && !BalanceLaw.isBoss(e)) {                      // the whip drags what it catches a little towards the caster
                        pull = pull.normalize().scale(0.35);
                        e.setDeltaMovement(e.getDeltaMovement().add(pull.x, 0.1, pull.z));
                        e.hurtMarked = true;
                    }
                }
            });
        }
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.1f, 1.4f);
        return true;
    }

    // ================================================================ Heaven-Splitting Flash
    /** Heaven-Splitting Flash: a short charge, then a huge beam of light pierces everything in a straight line. */
    static boolean flash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        float size = GrimoireBook.size(i, p);
        b.castCircle(p, 1.2f);
        Vec3 hand = p.getEyePosition().add(p.getViewVector(1f).scale(1.2));
        b.vfx(p, VfxShape.LIGHT_FLARE, hand, hand, LightBoneJa.FLASH_CHARGE + 6, 1.4f);
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2f, 2.0f);
        LightBoneJaKit.later(level, LightBoneJa.FLASH_CHARGE, P, () -> {
            if (!LightBoneJaKit.here(p, level)) return;
            Vec3 dir = p.getViewVector(1f), from = p.getEyePosition().add(0, -0.2, 0).add(dir.scale(0.8));
            double length = LightBoneJa.flashLength(EnergyBridge.scale(p)) * size;
            Vec3 to = LightBoneJaKit.rayEnd(level, p, from, dir, length);
            double real = from.distanceTo(to);
            LightBoneJaKit.prop(p, PropKind.LIGHT_JA, LightJaProps.BEAM | (Math.min(255, (int) Math.round(real * 2)) << 8), from, dir, size, LightBoneJa.FLASH_TICKS);
            b.vfx(p, VfxShape.LIGHT_BLADE, from, to, 8, 2.2f);
            b.vfx(p, VfxShape.LIGHT_FLARE, to, to, 14, 1.6f);
            LightBoneJaKit.lightBurst(level, to, 16, 0.8);
            for (LivingEntity e : LightBoneJaKit.nearest(GrimoireBook.along(p, from, to, LightBoneJa.FLASH_WIDTH * size), from, LightBoneJa.MAX_HITS)) {
                LightBook.lightHit(b, i, p, e, mode, LightBoneJa.FLASH_RAW);
                Vec3 push = dir.scale(BalanceLaw.isBoss(e) ? 0.3 : 1.0);
                e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.15, push.z));
                e.hurtMarked = true;
            }
            level.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6f, 2.0f);
        });
        return true;
    }

    // ================================================================ Arrows of Judgment
    /** Arrows of Judgment: a rain of light arrows falls over an area (the foe you look at, or where you aim). */
    static boolean arrows(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        LivingEntity t = GrimoireBook.target(p, LightBoneJa.ARROW_RANGE);
        Vec3 centre = t != null ? t.position() : LightBoneJaKit.ground(level, p, GrimoireBook.aim(p, LightBoneJa.ARROW_RANGE).add(0, 1.5, 0), 40);
        int n = LightBoneJa.arrowCount(EnergyBridge.modifier(p));
        float size = GrimoireBook.size(i, p);
        double radius = LightBoneJa.ARROW_RADIUS * (0.85 + 0.15 * size);
        int fall = LightBoneJa.fallTicks(LightBoneJa.ARROW_HEIGHT, LightBoneJa.ARROW_SPEED);
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.ELF_CIRCLE, centre.add(0, 0.05, 0), centre.add(0, 1, 0), 34, 1.2f);
        // the landings are grouped by the tick they land on: ONE search for foes per landing tick, then every landing of that tick is tested against the same list
        List<List<Vec3>> byTick = new ArrayList<>();
        int ticks = 6 + LightBoneJa.ARROW_WAVES * LightBoneJa.ARROW_WAVE_GAP + 3;
        for (int k = 0; k < ticks; k++) byTick.add(new ArrayList<>());
        List<MagicPropEntity> made = new ArrayList<>();
        AtomicInteger hits = new AtomicInteger();
        for (int k = 0; k < n; k++) {
            double[] off = LightBoneJa.arrowOffset(k, n, radius);
            int delay = 6 + (k % LightBoneJa.ARROW_WAVES) * LightBoneJa.ARROW_WAVE_GAP + (k / LightBoneJa.ARROW_WAVES) % 3;
            Vec3 land = LightBoneJaKit.ground(level, p, new Vec3(centre.x + off[0], centre.y + 6, centre.z + off[1]), 16);
            byTick.get(delay).add(land);
            LightBoneJaKit.later(level, delay, P, () -> {
                if (!LightBoneJaKit.here(p, level)) return;
                made.add(LightBoneJaKit.prop(p, PropKind.LIGHT_JA, LightJaProps.ARROW | LightBoneJa.speedBits(LightBoneJa.ARROW_SPEED),
                        new Vec3(land.x, land.y + LightBoneJa.ARROW_HEIGHT, land.z), new Vec3(0, -1, 0), size, fall + 2));
            });
        }
        for (int d = 0; d < ticks; d++) {
            List<Vec3> lands = byTick.get(d);
            if (lands.isEmpty()) continue;
            LightBoneJaKit.later(level, d + fall, P, () -> {
                for (MagicPropEntity a : made) if (a != null && !a.isRemoved() && a.life() >= fall - 1) a.expire();
                if (!LightBoneJaKit.here(p, level)) return;
                Vec3 mid = lands.get(0);
                List<LivingEntity> near = GrimoireBook.around(p, mid, radius * 2 + LightBoneJa.ARROW_BLAST);                 // one search for the whole tick
                int shown = 0;
                for (Vec3 land : lands) {
                    if (shown++ % 2 == 0) { b.vfx(p, VfxShape.LIGHT_FLARE, land, land, 8, 0.6f); LightBoneJaKit.lightPuff(level, land.add(0, 0.2, 0), 5, 0.35); }
                    int local = 0;
                    for (LivingEntity e : near) {
                        if (e.position().distanceToSqr(land) > LightBoneJa.ARROW_BLAST * LightBoneJa.ARROW_BLAST) continue;
                        if (local++ >= 6 || hits.incrementAndGet() > 2 * LightBoneJa.MAX_HITS) break;                       // one cast touches a bounded number of foes
                        LightBook.lightHit(b, i, p, e, mode, LightBoneJa.ARROW_RAW);
                    }
                }
            });
        }
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.7f);
        return true;
    }

    // ================================================================ Lamp of Avior
    /** Lamp of Avior: the light gathers in the raised hand, then the concentrated orb is thrown and goes off in a spiky starburst with a blinding flash. */
    static boolean lamp(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        float size = GrimoireBook.size(i, p);
        b.castCircle(p, 1.1f);
        MagicPropEntity charge = LightBoneJaKit.prop(p, PropKind.LIGHT_JA, LightJaProps.ORB, p.getEyePosition().add(p.getViewVector(1f).scale(1.4)), p.getViewVector(1f), size,
                LightBoneJa.LAMP_CHARGE + 2);
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.1f, 1.2f);
        LightBoneJaKit.later(level, LightBoneJa.LAMP_CHARGE, P, () -> {
            if (charge != null) charge.expire();
            if (!LightBoneJaKit.here(p, level)) return;
            LivingEntity t = GrimoireBook.target(p, LightBoneJa.LAMP_RANGE);
            Vec3 look = p.getViewVector(1f), from = p.getEyePosition().add(look.scale(1.4));
            Vec3 dir = t != null ? LightBoneJaKit.centre(t).subtract(from).normalize() : look;
            Vec3 stop = LightBoneJaKit.rayEnd(level, p, from, dir, LightBoneJa.LAMP_RANGE);
            b.vfx(p, VfxShape.LIGHT_BLADE, from, stop, 8, 1.2f);
            LightBoneJaKit.projectile(p, PropKind.LIGHT_JA, LightJaProps.ORB | LightJaProps.FLIGHT, from, dir, LightBoneJa.LAMP_SPEED, 0.8, 16, false,
                    (bolt, target) -> { },
                    (bolt, at) -> {
                        LightBoneJaKit.prop(p, PropKind.LIGHT_JA, LightJaProps.ORB | LightJaProps.BURST, at, dir, size, 16);
                        b.vfx(p, VfxShape.LIGHT_FLARE, at, at, 16, 2.2f);
                        LightBoneJaKit.lightBurst(level, at, 24, LightBoneJa.LAMP_RADIUS * 0.35);
                        level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9f, 1.7f);
                        blast(b, i, p, mode, at, LightBoneJa.LAMP_RADIUS * size, LightBoneJa.LAMP_RAW, LightBoneJa.LAMP_BLIND, 0.7);
                    }, size);
        });
        return true;
    }

    // ================================================================ Absolute Monarch of Divine Brilliance
    /** Absolute Monarch of Divine Brilliance: a giant of golden light stands behind the caster, shelters the allies near it and strikes the nearest foe now and then. */
    static boolean monarch(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        float size = GrimoireBook.size(i, p);
        b.castCircle(p, 1.8f);
        for (MagicPropEntity old : SongRebuildKit.mine(p, PropKind.LIGHT_JA)) if ((old.param() & 0xFF) == LightJaProps.MONARCH) old.expire();      // one giant per caster
        MagicPropEntity giant = LightBoneJaKit.prop(p, PropKind.LIGHT_JA, LightJaProps.MONARCH, p.position(), p.getViewVector(1f), size, LightBoneJa.MONARCH_TICKS);
        if (giant == null) { GrimoireBook.fail(p, "No room for the light to take shape."); return false; }
        b.vfx(p, VfxShape.LIGHT_FLARE, p.position().add(0, 1, 0), p.position(), 24, 2.4f);
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4f, 1.1f);
        // one step every 10 ticks; the allies are searched once a second and the foes once every 1.5 seconds (never per tick)
        SpellRuntime.zone(level, LightBoneJa.MONARCH_TICKS, 10, age -> {
            if (!LightBoneJaKit.here(p, level) || giant.isRemoved()) return;
            long t0 = PerfProbe.begin();
            try {
                if (age % 20 == 0) shelter(p);
                if (age % LightBoneJa.MONARCH_BLOW_GAP == 0 && age > 0) blow(b, i, p, mode, giant);
            } finally {
                PerfProbe.end(P, t0);
            }
        });
        return true;
    }

    /** The light shelters the allies near the caster: a little damage resistance and a trickle of healing (never hunger). */
    static void shelter(ServerPlayer p) {
        int n = 0;
        for (LivingEntity a : SongTargets.allies(p, p.position(), LightBoneJa.MONARCH_SHELTER)) {
            if (n++ >= SongRebuildKit.MAX_TARGETS) break;
            a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 0, false, false));
            BalanceLaw.heal(a, a.getMaxHealth() * 0.01f);
        }
    }

    /** The giant strikes the nearest foe within reach: a column of its fist-light, a light hit and a knock-back. */
    static void blow(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, MagicPropEntity giant) {
        ServerLevel level = p.serverLevel();
        List<LivingEntity> foes = GrimoireBook.around(p, p.position(), LightBoneJa.MONARCH_REACH);
        if (foes.isEmpty()) return;
        LivingEntity t = LightBoneJaKit.nearest(foes, p.position(), 1).get(0);
        Vec3 c = LightBoneJaKit.centre(t);
        giant.setParam(giant.param() | LightJaProps.STRIKE);
        LightBoneJaKit.later(level, 6, P, () -> { if (!giant.isRemoved()) giant.setParam(giant.param() & ~LightJaProps.STRIKE); });
        b.vfx(p, VfxShape.LIGHT_BLADE, giant.position().add(0, 3.2, 0), c, 8, 1.6f);
        b.vfx(p, VfxShape.LIGHT_FLARE, c, c, 12, 1.2f);
        LightBoneJaKit.lightBurst(level, c, 10, 0.6);
        LightBook.lightHit(b, i, p, t, mode, LightBoneJa.MONARCH_RAW);
        if (!BalanceLaw.isBoss(t)) {
            Vec3 away = t.position().subtract(p.position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-6) { away = away.normalize().scale(0.6); t.setDeltaMovement(t.getDeltaMovement().add(away.x, 0.2, away.z)); t.hurtMarked = true; }
        }
    }

    /** The pages of the remake, for tests and the self test: ids in the order the book shows them. */
    public static List<String> pageIds() { return LightBoneJa.LIGHT_JA; }
}
