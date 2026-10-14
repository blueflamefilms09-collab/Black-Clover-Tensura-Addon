package com.newuniverse.nusmp.relics.djinn.weapon;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.DjinnPhaseRules;
import com.newuniverse.nusmp.relics.djinn.DjinnState;
import com.newuniverse.nusmp.relics.djinn.DjinnWeaponBridge;
import com.newuniverse.nusmp.relics.djinn.dark.DarkDjinn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What a Djinn weapon does when it is used: a right-click is the first act, a sneak + right-click the second (the Full Equip opens it). Every act is
 * paid in Magoi (the same economy and drain factor as the phases), waits its own cooldown ({@link ActClock}), strikes only foes the Djinn spells
 * strike (never creative or spectator players, allies, pets or summons), hits through {@code Fx.damageHeavy} (the balance law and the 45% cap on
 * players), changes no block, and sends a bounded number of particles. The raw numbers are {@link DjinnWeaponRules}'s; the shapes are
 * {@link WeaponGeo}'s.
 */
public final class DjinnWeaponActs {
    private DjinnWeaponActs() {}

    private static final Map<UUID, ActClock> CLOCKS = new HashMap<>();
    private static final Map<UUID, Integer> LAST_USE = new HashMap<>();
    private static final Map<UUID, List<Strike>> STRIKES = new HashMap<>();
    private static final WeaponMarks MARKS = new WeaponMarks();
    /** Pending strikes per bearer (Storm Pillars) are bounded: three pillars, two casts in the air at most. */
    private static final int MAX_STRIKES = 6;

    /** A marked ground point that will be struck at a game time. */
    private record Strike(long due, Vec3 at, double power, boolean dark) {}

    // ================================================================ use
    /** Handles a right-click with a Djinn weapon in the main hand. True if an act ran. */
    static boolean use(ServerPlayer p, ItemStack stack, DjinnWeaponItem item) {
        Djinn d = item.djinn();
        DjinnTemp.Data t = DjinnTemp.get(stack);
        boolean admin = t == null || t.admin();
        DjinnState s = DjinnState.of(p);
        int phase;
        boolean dark;
        if (admin) {
            phase = DjinnPhaseRules.FULL;                                                  // an admin weapon works anywhere, as a Full Equip's
            dark = item.dark();
        } else {
            if (!p.getUUID().equals(t.owner()) || s.active() != d || s.phase() < DjinnPhaseRules.HALF) return false;
            phase = s.phase();
            dark = DarkDjinn.active(s);
        }
        if (DjinnWeaponBridge.charging(p)) {
            p.displayClientMessage(Component.translatable("djinn_weapon.msg.busy"), true);
            return false;
        }
        int act = p.isShiftKeyDown() ? DjinnWeaponRules.ACT_TWO : DjinnWeaponRules.ACT_ONE;
        if (!DjinnWeaponRules.open(act, phase)) {
            p.displayClientMessage(Component.translatable("djinn_weapon.msg.locked", Component.translatable("djinn_weapon.act." + DjinnWeaponRules.spec(d).two().key())), true);
            return false;
        }
        int tick = p.tickCount;
        Integer last = LAST_USE.get(p.getUUID());
        if (last != null && tick >= last && tick - last < DjinnWeaponRules.USE_GUARD) return false;
        LAST_USE.put(p.getUUID(), tick);
        long now = p.level().getGameTime();
        ActClock clock = CLOCKS.computeIfAbsent(p.getUUID(), k -> new ActClock());
        DjinnWeaponRules.Spec spec = DjinnWeaponRules.spec(d);
        int left = clock.left(act, now);
        if (left > 0) {
            p.displayClientMessage(Component.translatable("djinn_weapon.msg.cooldown", Component.translatable("djinn_weapon.act." + spec.act(act).key()), String.format(java.util.Locale.ROOT, "%.1f", left / 20.0)), true);
            return false;
        }
        if (!DjinnWeaponBridge.pay(p, DjinnWeaponRules.cost(spec, act, dark))) {
            p.displayClientMessage(Component.translatable("djinn_weapon.msg.magoi"), true);
            return false;
        }
        clock.start(act, now, DjinnWeaponRules.cooldown(spec, act, phase));
        Ctx c = new Ctx(p, d, spec, act, DjinnWeaponRules.power(spec, act, phase, dark), phase >= DjinnPhaseRules.FULL, dark);
        try {
            run(c);
        } catch (RuntimeException ex) {
            com.mojang.logging.LogUtils.getLogger().error("[nusmp] Djinn weapon act {} failed", spec.act(act).key(), ex);
        }
        return true;
    }

    /** The Equip ended: pending strikes are dropped. The cooldowns stay (running them down is not something a toggle may skip). */
    public static void endEquip(UUID id) { STRIKES.remove(id); }

    /** Forgets everything of a bearer (the player left). */
    public static void forget(UUID id) {
        CLOCKS.remove(id);
        LAST_USE.remove(id);
        STRIKES.remove(id);
    }

    public static void clearAll() {
        CLOCKS.clear();
        LAST_USE.clear();
        STRIKES.clear();
        MARKS.clear();
    }

    /** The extra share of damage a marked foe takes now (Marchosias' Brand). */
    public static double markBonus(UUID target, long now) { return MARKS.bonus(target, now); }

    /** Once every few ticks for a bearer: pending strikes whose time has come fall (the marked ground was drawn when the act began). */
    public static void tick(ServerPlayer p) {
        List<Strike> list = STRIKES.get(p.getUUID());
        if (list == null) return;
        long now = p.level().getGameTime();
        List<Strike> keep = new ArrayList<>();
        for (Strike st : list) {
            if (st.due > now) keep.add(st);
            else strike(p, st);
        }
        if (keep.isEmpty()) STRIKES.remove(p.getUUID());
        else STRIKES.put(p.getUUID(), keep);
    }

    // ================================================================ the context
    private static final class Ctx {
        final ServerPlayer p;
        final ServerLevel l;
        final Djinn d;
        final DjinnWeaponRules.Spec spec;
        final int act;
        final double power, range;
        final boolean full, dark;
        final Vec3 eye, look, flat;
        final int color, glow;

        Ctx(ServerPlayer p, Djinn d, DjinnWeaponRules.Spec spec, int act, double power, boolean full, boolean dark) {
            this.p = p;
            this.l = p.serverLevel();
            this.d = d;
            this.spec = spec;
            this.act = act;
            this.power = power;
            this.range = spec.act(act).range();
            this.full = full;
            this.dark = dark;
            this.eye = p.getEyePosition();
            this.look = p.getLookAngle();
            Vec3 f = new Vec3(look.x, 0, look.z);
            this.flat = f.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : f.normalize();
            this.color = dark ? 0x5A146E : d.color();
            this.glow = dark ? 0xB07CFF : d.glow();
        }

        void hurt(LivingEntity t, double mult) { Fx.damageHeavy(p, t, (float) (power * mult), DjinnWeaponBridge.damageType(d)); }

        ParticleOptions fx() { return DjinnWeaponBridge.particle(d); }

        Vec3 feet() { return p.position(); }
    }

    private static void run(Ctx c) {
        String key = c.spec.act(c.act).key();
        switch (key) {
            case "thunder_throw" -> thunderThrow(c);
            case "judgement_step" -> judgementStep(c);
            case "sun_cleave" -> sunCleave(c);
            case "ember_wall" -> emberWall(c);
            case "tide_spear" -> tideSpear(c);
            case "frost_surge" -> frostSurge(c);
            case "cyclone_sweep" -> cycloneSweep(c);
            case "updraft_leap" -> updraftLeap(c);
            case "bloom_heal" -> bloomHeal(c);
            case "thorn_snare" -> thornSnare(c);
            case "titan_slam" -> titanSlam(c);
            case "rock_breaker" -> rockBreaker(c);
            case "resonance_wave" -> resonanceWave(c);
            case "dissonance" -> dissonance(c);
            case "ember_rush" -> emberRush(c);
            case "brand" -> brand(c);
            case "dawn_arrow" -> dawnArrow(c);
            case "radiant_burst" -> radiantBurst(c);
            case "chain_lightning" -> chainLightning(c);
            case "storm_pillars" -> stormPillars(c);
            case "crescent_wind" -> crescentWind(c);
            case "heavenly_dash" -> heavenlyDash(c);
            case "abyssal_reap" -> abyssalReap(c);
            case "drowning_pull" -> drowningPull(c);
            default -> { }
        }
    }

    // ================================================================ shapes
    private static Vec3 centre(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.5, 0); }

    private static List<LivingEntity> cap(List<LivingEntity> in) {
        return in.size() > DjinnWeaponRules.MAX_TARGETS ? new ArrayList<>(in.subList(0, DjinnWeaponRules.MAX_TARGETS)) : in;
    }

    /** Foes in a cone in front of the bearer (nearest first). */
    private static List<LivingEntity> cone(Ctx c, double range, double halfAngle) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : Fx.foesAround(c.p, range + 1.5))
            if (WeaponGeo.inCone(c.p.getX(), c.p.getZ(), c.flat.x, c.flat.z, t.getX(), t.getZ(), range, halfAngle) && Math.abs(t.getY() - c.p.getY()) <= 2.5 + range * 0.3) out.add(t);
        return cap(out);
    }

    /** Foes within {@code r} of the bearer (nearest first). */
    private static List<LivingEntity> around(Ctx c, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : Fx.foesAround(c.p, r)) if (Math.abs(t.getY() - c.p.getY()) <= 3.0 + r * 0.3) out.add(t);
        return cap(out);
    }

    /** How far the look ray runs before a block stops it (at most {@code range}). */
    private static double rayLength(Ctx c, double range) {
        HitResult h = c.p.pick(range, 1.0f, false);
        if (h.getType() == HitResult.Type.BLOCK) return Math.max(1.0, Math.min(range, h.getLocation().distanceTo(c.eye)));
        return range;
    }

    /** Foes on the segment from the eye along the look for {@code len} blocks, {@code width} wide (nearest to the bearer first). */
    private static List<LivingEntity> beam(Ctx c, double len, double width) {
        Vec3 end = c.eye.add(c.look.scale(len));
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : Fx.foesAround(c.p, len + 2.0)) {
            Vec3 m = centre(t);
            if (WeaponGeo.segmentDistance(c.eye.x, c.eye.y, c.eye.z, end.x, end.y, end.z, m.x, m.y, m.z) <= width + t.getBbWidth() * 0.5) out.add(t);
        }
        out.sort(Comparator.comparingDouble(t -> t.distanceToSqr(c.p)));
        return cap(out);
    }

    /** Foes along the flat path the bearer is about to dash (start to start + flat * len), {@code width} wide. */
    private static List<LivingEntity> path(Ctx c, Vec3 start, double len, double width) {
        Vec3 end = start.add(c.flat.scale(len));
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : Fx.foesAround(c.p, len + 2.0)) {
            Vec3 m = centre(t);
            if (WeaponGeo.segmentDistance(start.x, start.y + 1.0, start.z, end.x, end.y + 1.0, end.z, m.x, m.y, m.z) <= width + t.getBbWidth() * 0.5) out.add(t);
        }
        out.sort(Comparator.comparingDouble(t -> t.distanceToSqr(c.p)));
        return cap(out);
    }

    private static void snd(Ctx c, SoundEvent s, float vol, float pitch) { c.l.playSound(null, c.p.blockPosition(), s, SoundSource.PLAYERS, vol, pitch); }

    private static void snd(ServerLevel l, Vec3 at, SoundEvent s, float vol, float pitch) { l.playSound(null, BlockPos.containing(at), s, SoundSource.PLAYERS, vol, pitch); }

    /** An arc of dust and the Djinn's particle on the ground in front: {@code count} points from -half to +half degrees at {@code radius}. */
    private static void arc(Ctx c, double radius, double half, int count, ParticleOptions o, int rgb) {
        int n = Mth.clamp(count, 4, 40);
        for (int i = 0; i < n; i++) {
            double a = -half + (2 * half) * i / (n - 1);
            double[] v = WeaponGeo.turn(c.flat.x, c.flat.z, a);
            double x = c.p.getX() + v[0] * radius, z = c.p.getZ() + v[1] * radius, y = c.p.getY() + 1.0;
            c.l.sendParticles(o, x, y, z, 1, 0.05, 0.1, 0.05, 0.01);
            if (i % 2 == 0) c.l.sendParticles(DjinnWeaponBridge.dust(rgb, 1.3f), x, y - 0.3, z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    private static void flash(Ctx c, Vec3 at) { c.l.sendParticles(ParticleTypes.FLASH, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0); }

    private static void rings(Ctx c, Vec3 at, double r) {
        DjinnWeaponBridge.ring(c.l, at, r, c.color, (int) (r * 6), 0.15);
        DjinnWeaponBridge.ring(c.l, at, r * 0.6, c.glow, (int) (r * 4), 0.3);
    }

    // ================================================================ BAAL
    private static void thunderThrow(Ctx c) {
        double len = rayLength(c, c.range);
        Vec3 end = c.eye.add(c.look.scale(len));
        List<LivingEntity> hit = beam(c, len, 1.2);
        if (hit.size() > 5) hit = new ArrayList<>(hit.subList(0, 5));
        for (LivingEntity t : hit) {
            c.hurt(t, 1.0);
            DjinnWeaponBridge.effect(t, "minecraft:slowness", 30, 1);
        }
        LivingEntity from = hit.isEmpty() ? null : hit.get(hit.size() - 1);
        int links = c.full ? 3 : 2;
        List<LivingEntity> seen = new ArrayList<>(hit);
        Vec3 at = from == null ? end : centre(from);
        for (int i = 0; i < links && from != null; i++) {
            LivingEntity next = null;
            double best = 36.0;
            for (LivingEntity o : DjinnWeaponBridge.foesNear(c.p, at, 6.0)) {
                double dd = centre(o).distanceToSqr(at);
                if (!seen.contains(o) && dd < best) { best = dd; next = o; }
            }
            if (next == null) break;
            c.hurt(next, 0.6);
            DjinnWeaponBridge.line(c.l, at, centre(next), ParticleTypes.ELECTRIC_SPARK, 12);
            seen.add(next);
            at = centre(next);
        }
        Vec3 start = c.eye.add(c.look.scale(0.8)).add(0, -0.3, 0);
        DjinnWeaponBridge.line(c.l, start, end, ParticleTypes.ELECTRIC_SPARK, 40);
        DjinnWeaponBridge.line(c.l, start, end, DjinnWeaponBridge.dust(c.glow, 1.5f), 30);
        flash(c, end);
        snd(c, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.0f, 1.6f);
    }

    private static void judgementStep(Ctx c) {
        Vec3 start = c.feet();
        List<LivingEntity> hit = path(c, start, 8.0, 1.8);
        Fx.dash(c.p, 8.0);
        for (LivingEntity t : hit) {
            c.hurt(t, 0.9);
            DjinnWeaponBridge.effect(t, "minecraft:slowness", 40, 2);
        }
        Vec3 end = start.add(c.flat.scale(8.0));
        DjinnWeaponBridge.line(c.l, start.add(0, 1, 0), end.add(0, 1, 0), ParticleTypes.ELECTRIC_SPARK, 40);
        DjinnWeaponBridge.line(c.l, start.add(0, 0.4, 0), end.add(0, 0.4, 0), DjinnWeaponBridge.dust(c.glow, 1.6f), 30);
        flash(c, start);
        snd(c, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8f, 1.6f);
    }

    // ================================================================ AMON
    private static void sunCleave(Ctx c) {
        for (LivingEntity t : cone(c, c.range, 60.0)) {
            c.hurt(t, 1.0);
            t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 100));
            DjinnWeaponBridge.push(t, c.feet(), 0.5, 0.15);
        }
        arc(c, 3.0, 60, 24, ParticleTypes.FLAME, c.glow);
        arc(c, 5.5, 60, 28, ParticleTypes.FLAME, c.color);
        c.l.sendParticles(ParticleTypes.LAVA, c.p.getX() + c.flat.x * 3, c.p.getY() + 1.2, c.p.getZ() + c.flat.z * 3, 4, 0.6, 0.3, 0.6, 0.0);
        snd(c, SoundEvents.FIRECHARGE_USE, 1.2f, 0.8f);
    }

    private static void emberWall(Ctx c) {
        double px = c.p.getX(), pz = c.p.getZ();
        for (LivingEntity t : around(c, c.range + 1.0)) {
            if (!WeaponGeo.inWall(px, pz, c.flat.x, c.flat.z, t.getX(), t.getZ(), 1.5, 5.0, 3.5)) continue;
            c.hurt(t, 0.8);
            t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 120));
            DjinnWeaponBridge.push(t, c.feet(), 1.0, 0.2);
        }
        Fx.addEffect(c.p, "minecraft:fire_resistance", 100, 0);
        Vec3 side = new Vec3(-c.flat.z, 0, c.flat.x);
        for (int i = -3; i <= 3; i++) {
            Vec3 base = c.feet().add(c.flat.scale(3.2)).add(side.scale(i));
            DjinnWeaponBridge.column(c.l, base.x, base.y, base.y + 2.4, base.z, ParticleTypes.FLAME, 7);
            c.l.sendParticles(DjinnWeaponBridge.dust(c.glow, 1.4f), base.x, base.y + 1.2, base.z, 2, 0.1, 0.6, 0.1, 0.0);
        }
        snd(c, SoundEvents.BLAZE_SHOOT, 1.2f, 0.7f);
    }

    // ================================================================ VINEA
    private static void tideSpear(Ctx c) {
        double len = rayLength(c, c.range);
        for (LivingEntity t : beam(c, len, 1.0)) {
            c.hurt(t, 1.0);
            DjinnWeaponBridge.push(t, c.feet(), 1.2, 0.15);
            DjinnWeaponBridge.effect(t, "minecraft:slowness", 40, 0);
        }
        Vec3 start = c.eye.add(c.look.scale(0.8)).add(0, -0.3, 0), end = c.eye.add(c.look.scale(len));
        DjinnWeaponBridge.line(c.l, start, end, ParticleTypes.BUBBLE_POP, 40);
        DjinnWeaponBridge.line(c.l, start, end, DjinnWeaponBridge.dust(c.glow, 1.5f), 30);
        c.l.sendParticles(ParticleTypes.SPLASH, end.x, end.y, end.z, 20, 0.5, 0.4, 0.5, 0.1);
        snd(c, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.0f, 1.2f);
    }

    private static void frostSurge(Ctx c) {
        for (LivingEntity t : cone(c, c.range, 50.0)) {
            c.hurt(t, 1.0);
            t.setTicksFrozen(t.getTicksRequiredToFreeze() + 60);
            DjinnWeaponBridge.effect(t, "minecraft:slowness", BalanceLaw.controlTicks(t, 80), 2);
        }
        arc(c, 3.0, 50, 20, ParticleTypes.SNOWFLAKE, c.glow);
        arc(c, 6.0, 50, 24, ParticleTypes.SNOWFLAKE, c.color);
        c.l.sendParticles(ParticleTypes.CLOUD, c.p.getX() + c.flat.x * 3, c.p.getY() + 1.0, c.p.getZ() + c.flat.z * 3, 10, 1.0, 0.4, 1.0, 0.02);
        snd(c, SoundEvents.PLAYER_HURT_FREEZE, 1.2f, 0.8f);
    }

    // ================================================================ FOCALOR
    private static void cycloneSweep(Ctx c) {
        for (LivingEntity t : around(c, c.range)) {
            c.hurt(t, 0.8);
            DjinnWeaponBridge.push(t, c.feet(), 1.0, 0.35);
        }
        rings(c, c.feet(), c.range);
        c.l.sendParticles(ParticleTypes.GUST, c.p.getX(), c.p.getY() + 1.0, c.p.getZ(), 1, 0.5, 0.2, 0.5, 0.0);
        c.l.sendParticles(ParticleTypes.CLOUD, c.p.getX(), c.p.getY() + 0.6, c.p.getZ(), 20, c.range * 0.4, 0.3, c.range * 0.4, 0.15);
        snd(c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.7f);
    }

    private static void updraftLeap(Ctx c) {
        for (LivingEntity t : around(c, 3.5)) {
            c.hurt(t, 0.4);
            DjinnWeaponBridge.push(t, c.feet(), 0.1, 0.9);
        }
        c.p.setDeltaMovement(c.look.x * 0.5, 1.15, c.look.z * 0.5);
        c.p.hurtMarked = true;
        c.p.fallDistance = 0;
        Fx.addEffect(c.p, "minecraft:slow_falling", 100, 0);
        rings(c, c.feet(), 3.5);
        DjinnWeaponBridge.column(c.l, c.p.getX(), c.p.getY(), c.p.getY() + 4, c.p.getZ(), ParticleTypes.CLOUD, 20);
        snd(c, SoundEvents.PHANTOM_FLAP, 1.0f, 1.2f);
    }

    // ================================================================ ZAGAN
    private static void bloomHeal(Ctx c) {
        for (ServerPlayer a : DjinnWeaponBridge.alliesNear(c.p, c.range)) {
            BalanceLaw.heal(a, (float) c.power);
            Fx.addEffect(a, "minecraft:regeneration", 80, 0);
            c.l.sendParticles(ParticleTypes.HAPPY_VILLAGER, a.getX(), a.getY() + 1, a.getZ(), 10, 0.4, 0.6, 0.4, 0.05);
        }
        for (LivingEntity t : around(c, c.range - 2.0)) {
            c.hurt(t, 0.5);
            DjinnWeaponBridge.effect(t, "minecraft:slowness", BalanceLaw.controlTicks(t, 60), 3);
        }
        rings(c, c.feet(), c.range * 0.8);
        c.l.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.p.getX(), c.p.getY() + 1, c.p.getZ(), 40, c.range * 0.3, 0.8, c.range * 0.3, 0.05);
        snd(c, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 1.3f);
    }

    private static void thornSnare(Ctx c) {
        List<LivingEntity> in = cone(c, c.range, 30.0);
        int n = Math.min(3, in.size());
        for (int i = 0; i < n; i++) {
            LivingEntity t = in.get(i);
            c.hurt(t, 1.0);
            DjinnWeaponBridge.effect(t, "minecraft:slowness", BalanceLaw.controlTicks(t, 80), 4);
            DjinnWeaponBridge.effect(t, "minecraft:poison", 100, 0);
            Vec3 m = centre(t);
            DjinnWeaponBridge.line(c.l, c.eye.add(0, -0.4, 0), m, ParticleTypes.CRIT, 24);
            DjinnWeaponBridge.column(c.l, t.getX(), t.getY(), t.getY() + 1.8, t.getZ(), DjinnWeaponBridge.dust(c.glow, 1.4f), 10);
        }
        arc(c, 4.0, 30, 12, ParticleTypes.HAPPY_VILLAGER, c.color);
        snd(c, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, 1.5f, 0.6f);
    }

    // ================================================================ ZEPAR
    private static void titanSlam(Ctx c) {
        Vec3 at = c.feet();
        for (LivingEntity t : around(c, c.range)) {
            double dist = Math.sqrt(t.distanceToSqr(at.x, at.y, at.z));
            c.hurt(t, 1.0 - 0.3 * Math.min(1.0, dist / c.range));
            DjinnWeaponBridge.push(t, at, 0.6, 0.8);
        }
        for (int k = 1; k <= 3; k++) DjinnWeaponBridge.ring(c.l, at, c.range * k / 3.0, k == 3 ? c.glow : c.color, (int) (c.range * 5 * k / 2.0), 0.15 * k);
        c.l.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.6, at.z, 3, c.range * 0.3, 0.2, c.range * 0.3, 0.0);
        c.l.sendParticles(ParticleTypes.CRIT, at.x, at.y + 0.5, at.z, 24, c.range * 0.4, 0.3, c.range * 0.4, 0.3);
        snd(c, SoundEvents.IRON_GOLEM_ATTACK, 1.6f, 0.5f);
        snd(c, SoundEvents.ANVIL_LAND, 0.8f, 0.6f);
    }

    private static void rockBreaker(Ctx c) {
        List<LivingEntity> in = cone(c, c.range, 40.0);
        int n = Math.min(3, in.size());
        for (int i = 0; i < n; i++) {
            LivingEntity t = in.get(i);
            c.hurt(t, 1.0 + Math.min(20.0, t.getArmorValue()) / 20.0);
            DjinnWeaponBridge.effect(t, "minecraft:weakness", 100, 1);
            Fx.addEffect(t, "tensura:fragility", 160, 0);
            DjinnWeaponBridge.push(t, c.feet(), 0.4, 0.1);
            c.l.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 14, 0.3, 0.4, 0.3, 0.3);
        }
        arc(c, 3.5, 40, 14, ParticleTypes.CRIT, c.color);
        snd(c, SoundEvents.ANVIL_LAND, 1.0f, 1.4f);
    }

    // ================================================================ FURFUR
    private static void resonanceWave(Ctx c) {
        for (LivingEntity t : cone(c, c.range, 45.0)) {
            c.hurt(t, 1.0);
            DjinnWeaponBridge.push(t, c.feet(), 1.3, 0.25);
            EnergyBridge.effect(t, "silence", 80, 0);
        }
        for (int k = 1; k <= 3; k++) arc(c, 2.5 * k, 45, 8 + 4 * k, ParticleTypes.NOTE, k == 3 ? c.glow : c.color);
        snd(c, SoundEvents.NOTE_BLOCK_BELL.value(), 1.6f, 0.6f);
        snd(c, SoundEvents.WARDEN_SONIC_BOOM, 0.5f, 1.6f);
    }

    private static void dissonance(Ctx c) {
        for (LivingEntity t : around(c, c.range)) {
            c.hurt(t, 0.7);
            DjinnWeaponBridge.effect(t, "minecraft:nausea", 120, 0);
            DjinnWeaponBridge.effect(t, "minecraft:slowness", 60, 0);
        }
        rings(c, c.feet(), c.range);
        c.l.sendParticles(ParticleTypes.NOTE, c.p.getX(), c.p.getY() + 1.5, c.p.getZ(), 16, c.range * 0.3, 0.6, c.range * 0.3, 1.0);
        snd(c, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), 1.6f, 0.5f);
    }

    // ================================================================ MARCHOSIAS
    private static void emberRush(Ctx c) {
        Vec3 start = c.feet();
        List<LivingEntity> hit = path(c, start, c.range, 1.6);
        Fx.dash(c.p, 7.0);
        for (LivingEntity t : hit) {
            c.hurt(t, 1.0);
            t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 80));
        }
        Vec3 end = start.add(c.flat.scale(c.range));
        DjinnWeaponBridge.line(c.l, start.add(0, 1, 0), end.add(0, 1, 0), ParticleTypes.FLAME, 40);
        DjinnWeaponBridge.line(c.l, start.add(0, 0.5, 0), end.add(0, 0.5, 0), DjinnWeaponBridge.dust(c.glow, 1.6f), 30);
        snd(c, SoundEvents.FIRECHARGE_USE, 1.4f, 0.7f);
    }

    private static void brand(Ctx c) {
        LivingEntity target = null;
        List<LivingEntity> in = cone(c, c.range, 20.0);
        if (!in.isEmpty()) target = in.get(0);
        else {
            double best = Double.MAX_VALUE;
            for (LivingEntity t : Fx.foesAround(c.p, c.range)) {
                double dd = t.distanceToSqr(c.p);
                if (dd < best) { best = dd; target = t; }
            }
        }
        if (target == null) {
            c.p.displayClientMessage(Component.translatable("djinn_weapon.msg.nothing"), true);
            return;
        }
        c.hurt(target, 1.0);
        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 80));
        DjinnWeaponBridge.effect(target, "minecraft:glowing", 160, 0);
        MARKS.mark(target.getUUID(), c.p.level().getGameTime(), 160, target instanceof Player ? 0.12 : 0.25);
        Vec3 m = centre(target);
        DjinnWeaponBridge.line(c.l, c.eye.add(0, -0.4, 0), m, DjinnWeaponBridge.dust(c.glow, 1.4f), 24);
        DjinnWeaponBridge.ring(c.l, target.position(), 1.2, c.glow, 24, 0.1);
        DjinnWeaponBridge.ring(c.l, target.position().add(0, 1.0, 0), 1.0, c.color, 24, 0.0);
        snd(c, SoundEvents.BLAZE_SHOOT, 1.0f, 1.4f);
    }

    // ================================================================ SITRI
    private static void dawnArrow(Ctx c) {
        double len = rayLength(c, c.range);
        List<LivingEntity> hit = beam(c, len, 0.7);
        int n = Math.min(2, hit.size());
        for (int i = 0; i < n; i++) {
            LivingEntity t = hit.get(i);
            double mult = (i == 0 ? 1.0 : 0.6) * (t.getType().is(EntityTypeTags.UNDEAD) ? 2.0 : 1.0);
            c.hurt(t, mult);
            DjinnWeaponBridge.effect(t, "minecraft:glowing", 60, 0);
            c.l.sendParticles(ParticleTypes.END_ROD, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 8, 0.2, 0.3, 0.2, 0.05);
        }
        Vec3 start = c.eye.add(c.look.scale(0.8)).add(0, -0.3, 0), end = c.eye.add(c.look.scale(len));
        DjinnWeaponBridge.line(c.l, start, end, ParticleTypes.END_ROD, 40);
        DjinnWeaponBridge.line(c.l, start, end, DjinnWeaponBridge.dust(c.glow, 1.4f), 30);
        flash(c, end);
        snd(c, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 1.8f);
    }

    private static void radiantBurst(Ctx c) {
        for (LivingEntity t : around(c, c.range)) {
            c.hurt(t, 1.0);
            DjinnWeaponBridge.effect(t, "minecraft:blindness", 60, 0);
        }
        for (ServerPlayer a : DjinnWeaponBridge.alliesNear(c.p, c.range)) BalanceLaw.heal(a, (float) (c.power * 0.35));
        flash(c, c.feet());
        rings(c, c.feet(), c.range);
        c.l.sendParticles(ParticleTypes.END_ROD, c.p.getX(), c.p.getY() + 1.0, c.p.getZ(), 40, c.range * 0.3, 0.8, c.range * 0.3, 0.2);
        snd(c, SoundEvents.BEACON_ACTIVATE, 1.0f, 1.8f);
    }

    // ================================================================ BIFRON
    private static void chainLightning(Ctx c) {
        LivingEntity first = null;
        List<LivingEntity> in = cone(c, c.range, 35.0);
        if (!in.isEmpty()) first = in.get(0);
        else {
            List<LivingEntity> any = DjinnWeaponBridge.foesNear(c.p, c.feet(), c.range);
            if (!any.isEmpty()) first = any.get(0);
        }
        if (first == null) {
            c.p.displayClientMessage(Component.translatable("djinn_weapon.msg.nothing"), true);
            return;
        }
        int links = c.full ? 5 : 4;
        List<LivingEntity> seen = new ArrayList<>();
        Vec3 at = c.eye.add(0, -0.4, 0);
        LivingEntity cur = first;
        for (int i = 0; i < links && cur != null; i++) {
            c.hurt(cur, WeaponGeo.chainFactor(i, 0.85));
            Vec3 m = centre(cur);
            DjinnWeaponBridge.line(c.l, at, m, ParticleTypes.ELECTRIC_SPARK, 16);
            DjinnWeaponBridge.line(c.l, at, m, DjinnWeaponBridge.dust(c.glow, 1.2f), 10);
            seen.add(cur);
            at = m;
            LivingEntity next = null;
            double best = 36.0;
            for (LivingEntity o : DjinnWeaponBridge.foesNear(c.p, m, 6.0)) {
                double dd = centre(o).distanceToSqr(m);
                if (!seen.contains(o) && dd < best) { best = dd; next = o; }
            }
            cur = next;
        }
        snd(c, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.9f, 1.8f);
    }

    private static void stormPillars(Ctx c) {
        List<Vec3> marks = new ArrayList<>();
        for (LivingEntity t : DjinnWeaponBridge.foesNear(c.p, c.feet(), c.range)) {
            if (marks.size() >= 3) break;
            marks.add(t.position());
        }
        double ahead = rayLength(c, 12.0);
        for (int i = 0; marks.size() < 3; i++) {
            double d = Math.min(ahead, 5.0 + 3.0 * i);
            marks.add(c.feet().add(c.flat.scale(d)));
        }
        long due = c.p.level().getGameTime() + DjinnWeaponRules.STRIKE_DELAY;
        List<Strike> list = new ArrayList<>(STRIKES.getOrDefault(c.p.getUUID(), List.of()));
        for (Vec3 m : marks) {
            if (list.size() >= MAX_STRIKES) break;
            list.add(new Strike(due, m, c.power, c.dark));
            DjinnWeaponBridge.ring(c.l, m, 2.4, c.glow, 24, 0.15);
            DjinnWeaponBridge.ring(c.l, m, 1.2, c.color, 14, 0.2);
        }
        STRIKES.put(c.p.getUUID(), list);
        snd(c, SoundEvents.BEACON_POWER_SELECT, 1.0f, 1.8f);
    }

    /** A marked pillar falls: foes within 2.4 take the act's power and a held breath of stillness. */
    private static void strike(ServerPlayer p, Strike st) {
        ServerLevel l = p.serverLevel();
        Vec3 at = st.at;
        for (LivingEntity t : DjinnWeaponBridge.foesNear(p, at, 2.4)) {
            Fx.damageHeavy(p, t, (float) st.power, "minecraft:lightning_bolt");
            DjinnWeaponBridge.effect(t, "minecraft:slowness", BalanceLaw.controlTicks(t, 20), 3);
        }
        int glow = st.dark ? 0xB07CFF : 0xD2C2FF;
        DjinnWeaponBridge.column(l, at.x, at.y, at.y + 14, at.z, ParticleTypes.ELECTRIC_SPARK, 32);
        DjinnWeaponBridge.column(l, at.x, at.y, at.y + 12, at.z, DjinnWeaponBridge.dust(glow, 1.8f), 24);
        DjinnWeaponBridge.ring(l, at, 2.4, glow, 20, 0.2);
        l.sendParticles(ParticleTypes.FLASH, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0);
        snd(l, at, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.4f, 1.1f);
    }

    // ================================================================ PAIMON
    private static void crescentWind(Ctx c) {
        double px = c.p.getX(), pz = c.p.getZ();
        for (LivingEntity t : around(c, c.range + 1.0)) {
            if (!WeaponGeo.inCrescents(px, pz, c.flat.x, c.flat.z, t.getX(), t.getZ(), c.range, 28.0, 22.0)) continue;
            c.hurt(t, 1.0);
            DjinnWeaponBridge.effect(t, "minecraft:wither", 40, 0);
            DjinnWeaponBridge.push(t, c.feet(), 0.4, 0.1);
        }
        for (int side = -1; side <= 1; side += 2) {
            double[] v = WeaponGeo.turn(c.flat.x, c.flat.z, side * 28.0);
            double nx = -v[1], nz = v[0];
            Vec3 prev = null;
            for (int i = 0; i <= 6; i++) {
                double r = 3.0 + i * 1.0, bend = side * Math.sin(i / 6.0 * Math.PI) * 1.2;
                Vec3 pt = new Vec3(px + v[0] * r + nx * bend, c.p.getY() + 1.0, pz + v[1] * r + nz * bend);
                if (prev != null) DjinnWeaponBridge.line(c.l, prev, pt, ParticleTypes.CLOUD, 6);
                c.l.sendParticles(DjinnWeaponBridge.dust(c.glow, 1.4f), pt.x, pt.y, pt.z, 1, 0.05, 0.05, 0.05, 0.0);
                prev = pt;
            }
        }
        snd(c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 1.4f);
    }

    private static void heavenlyDash(Ctx c) {
        Vec3 start = c.feet();
        List<LivingEntity> hit = path(c, start, c.range, 1.8);
        Fx.dash(c.p, 9.0);
        c.p.setDeltaMovement(c.p.getDeltaMovement().add(0, 0.25, 0));
        c.p.hurtMarked = true;
        Fx.addEffect(c.p, "minecraft:slow_falling", 60, 0);
        for (LivingEntity t : hit) {
            c.hurt(t, 1.0);
            DjinnWeaponBridge.push(t, start, 0.5, 0.2);
        }
        Vec3 end = start.add(c.flat.scale(c.range));
        DjinnWeaponBridge.line(c.l, start.add(0, 1, 0), end.add(0, 1.4, 0), ParticleTypes.CLOUD, 40);
        DjinnWeaponBridge.line(c.l, start.add(0, 0.6, 0), end.add(0, 1.0, 0), DjinnWeaponBridge.dust(c.glow, 1.5f), 30);
        snd(c, SoundEvents.PHANTOM_FLAP, 1.0f, 1.5f);
    }

    // ================================================================ VASSAGO
    private static void abyssalReap(Ctx c) {
        List<LivingEntity> in = cone(c, c.range, 70.0);
        int reaped = 0;
        for (LivingEntity t : in) {
            c.hurt(t, 1.0);
            Fx.addEffect(t, "tensura:fear", BalanceLaw.controlTicks(t, 100), 0);
            c.l.sendParticles(ParticleTypes.SQUID_INK, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
            reaped++;
        }
        if (reaped > 0) BalanceLaw.heal(c.p, (float) (c.power * 0.25 * Math.min(3, reaped)));
        arc(c, 3.0, 70, 24, ParticleTypes.SQUID_INK, c.color);
        arc(c, 5.0, 70, 30, DjinnWeaponBridge.particle(c.d), c.glow);
        snd(c, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.5f);
        snd(c, SoundEvents.WARDEN_HEARTBEAT, 1.2f, 0.6f);
    }

    private static void drowningPull(Ctx c) {
        Vec3 at = c.feet();
        for (LivingEntity t : around(c, c.range)) {
            Vec3 in = at.subtract(t.position()).multiply(1, 0, 1);
            Vec3 pull = in.lengthSqr() < 1.0E-4 ? Vec3.ZERO : in.normalize().scale(Math.min(1.1, 0.5 + in.length() * 0.08));
            t.setDeltaMovement(t.getDeltaMovement().add(pull.x, 0.2, pull.z));
            t.hurtMarked = true;
            c.hurt(t, 0.5);
            DjinnWeaponBridge.effect(t, "minecraft:slowness", 60, 1);
            t.setAirSupply(Math.max(-20, t.getAirSupply() - 150));
        }
        for (int arm = 0; arm < 3; arm++)
            for (int i = 0; i < 8; i++) {
                double rr = c.range * (1.0 - i / 9.0), a = arm * 2.0944 + i * 0.6;
                c.l.sendParticles(ParticleTypes.BUBBLE_POP, at.x + Math.cos(a) * rr, at.y + 0.3 + i * 0.12, at.z + Math.sin(a) * rr, 1, 0, 0, 0, 0);
            }
        rings(c, at, c.range);
        snd(c, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.0f, 0.5f);
    }
}
