package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.djinn.dark.DarkDjinn;
import com.newuniverse.nusmp.relics.djinn.dark.DarkRules;
import com.newuniverse.nusmp.relics.djinn.weapon.WeaponGeo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * What the Djinn does in the world, server side: the Half Equip's blow (extra elemental damage and the Djinn's special), the Lesser Summon's bolts,
 * the elemental calm of a bearer, and the Extreme Magics. The rules for all of it:
 * <ul>
 *   <li>damage goes through the relics' {@link Fx} (the balance law, then a cap on players: 30% of their health, 45% for an Extreme Magic that
 *       costs 60% of the Magoi); no spell breaks, burns or places a block;</li>
 *   <li>an Extreme Magic is drawn on the ground for three seconds before it strikes, hits any one target once (twice for bolts) and at most 24
 *       targets, and never a creative or spectator player, an ally or a tamed pet;</li>
 *   <li>everything per tick is bounded: a telegraph sends at most a few hundred particles a call.</li>
 * </ul>
 */
final class DjinnMagic {
    private DjinnMagic() {}

    /** Set while a Half Equip's own side effects hurt someone, so they cannot trigger another bonus. */
    private static boolean inBonus;

    /** Blows in a row per bearer (Bloodlust, Drumbeat); forgotten on logout and when the server stops. */
    static final BlowStreak STREAKS = new BlowStreak();

    // ================================================================ Half Equip
    /** The bearer's melee blow: the Djinn adds elemental damage and its special. */
    static void onOutgoing(LivingIncomingDamageEvent e) {
        DamageSource src = e.getSource();
        if (inBonus || !(src.getEntity() instanceof ServerPlayer p) || src.getDirectEntity() != p || !src.is(DamageTypes.PLAYER_ATTACK)) return;
        LivingEntity target = e.getEntity();
        if (target == p || !target.isAlive() || (target instanceof Player o && (o.isCreative() || o.isSpectator() || !p.canHarmPlayer(o)))) return;
        DjinnState s = DjinnState.of(p);
        Djinn d = s.djinn();
        if (d == null || s.phase() < DjinnPhaseRules.HALF) return;
        Djinn.Trait t = d.trait();
        float amount = e.getAmount();
        boolean dark = DarkDjinn.active(s);                                                         // the black rukh: +35% of the rider, its cap x1.5 (a mob's rider; a player's stays at the 6-point clamp)
        double frac = DarkRules.riderFrac(t.bonusFrac(), dark), cap = DarkRules.riderCap(t.bonusCap(), dark);
        int streak = t.special() == Djinn.Special.BLOODLUST || t.special() == Djinn.Special.DRUMBEAT ? STREAKS.hit(p.getUUID(), p.level().getGameTime()) : 0;
        TraitRules.Facts facts = new TraitRules.Facts(target.getType().is(EntityTypeTags.UNDEAD), target.getArmorValue(),
                target.getHealth() / Math.max(1.0f, target.getMaxHealth()), target.isInvisible(), DjinnTraits.ailments(target),
                Math.max(Math.hypot(p.getX() - p.xo, p.getZ() - p.zo), p.getDeltaMovement().horizontalDistance()), streak);       // a player's speed: this tick's step, or the motion the server knows
        frac *= TraitRules.fracFactor(t.special(), facts);                                          // Smite, Crush, Hunt, Trample, Reveal, Carrion, Bloodlust, Drumbeat read the blow
        cap *= TraitRules.capFactor(t.special(), facts);
        float bonus = TraitRules.rider(amount, frac, cap, target instanceof Player);               // the 6-point clamp on players is TraitRules.PLAYER_RIDER_CAP
        if (bonus <= 0f) return;
        e.setAmount(amount + bonus);
        inBonus = true;
        try {
            special(p, d, t, target, amount + bonus, bonus, streak);
        } finally {
            inBonus = false;
        }
        ServerLevel l = p.serverLevel();
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.djinn.DjinnFx.align(d.affinity()), at.x, at.y, at.z, 6, 0.25, 0.3, 0.25, 0.05);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.Fx.align(d.glow()), at.x, at.y, at.z, 4, 0.25, 0.3, 0.25, 0.02);
    }

    private static void special(ServerPlayer p, Djinn d, Djinn.Trait t, LivingEntity target, float total, float bonus, int streak) {
        int ticks = t.effectTicks();
        if (ticks > 0 && !t.effectId().isEmpty()) {
            boolean hard = t.effectAmp() >= 2 || t.special() == Djinn.Special.PARALYZE;
            DjinnFx.effect(target, t.effectId(), hard ? BalanceLaw.controlTicks(target, ticks) : ticks, t.effectAmp());
        }
        switch (t.special()) {
            case CHAIN -> {
                LivingEntity next = null;
                double best = 25.0;
                for (LivingEntity o : DjinnTargets.foesNear(p, target.position(), 5.0)) {
                    double dd = o.distanceToSqr(target);
                    if (o != target && dd < best) { best = dd; next = o; }
                }
                if (next != null) {
                    next.hurt(p.damageSources().indirectMagic(p, p), Math.max(1f, next instanceof Player ? Math.min(bonus, TraitRules.PLAYER_RIDER_CAP) : bonus));      // the 6-point player cap holds for the chained hit too (a mob's dark rider reaches more)
                    DjinnFx.line(p.serverLevel(), target.position().add(0, 1, 0), next.position().add(0, 1, 0), com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), 10);
                }
            }
            case MELT -> {
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 140));
                target.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                target.removeEffect(MobEffects.ABSORPTION);
            }
            case FREEZE -> target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 40, target.getTicksFrozen() + 60));
            case PUSH -> push(target, p.position(), 0.9, 0.15);
            case LIFESTEAL -> BalanceLaw.heal(p, total * 0.30f);
            case DEAFEN -> EnergyBridge.effect(target, "silence", 90, 0);
            case BRAND -> target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 120));
            case DROWN -> target.setAirSupply(Math.max(-20, target.getAirSupply() - 120));
            case CRUSH -> push(target, p.position(), 0.5, 0.1);
            default -> DjinnTraits.apply(p, t.special(), target, bonus, streak);                    // the twenty-four specials of 1.93.0
        }
    }

    /** The share of a damage source the bearer's elemental calm takes away (Half Equip half of it): a Heat Djinn's bearer shrugs off fire. */
    static double resist(Djinn d, DamageSource src, int phase) {
        double f = switch (d.affinity()) {
            case HEAT -> src.is(DamageTypeTags.IS_FIRE) ? 0.8 : 0.0;
            case LIGHTNING -> src.is(DamageTypeTags.IS_LIGHTNING) ? 0.8 : 0.0;
            case WATER -> src.is(DamageTypeTags.IS_DROWNING) ? 1.0 : 0.0;
            case WIND -> src.is(DamageTypeTags.IS_FALL) ? 1.0 : 0.0;
            case LIFE -> src.is(DamageTypes.WITHER) || src.is(DamageTypes.MAGIC) ? 0.4 : 0.0;
            case STRENGTH -> src.is(DamageTypeTags.IS_EXPLOSION) ? 0.4 : 0.0;
            case SOUND -> src.is(DamageTypes.SONIC_BOOM) ? 0.8 : 0.0;
            case LIGHT -> src.is(DamageTypes.MAGIC) ? 0.25 : 0.0;
        };
        return phase >= DjinnPhaseRules.FULL ? f : f * 0.5;
    }

    /** The bearer takes less of their own element. */
    static void onIncoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        DjinnState s = DjinnState.of(p);
        Djinn d = s.djinn();
        if (d == null || s.phase() < DjinnPhaseRules.HALF) return;
        double f = DarkRules.calm(resist(d, e.getSource(), s.phase()), DarkDjinn.active(s));      // a dark Djinn's calm is half
        if (f > 0) e.setAmount((float) (e.getAmount() * (1.0 - f)));
    }

    // ================================================================ Lesser Summon
    /** The small manifestation strikes the nearest hostile mob within 12 blocks (never a player) for a little. */
    static void lesserBolt(ServerPlayer p, Djinn d) {
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity t : Fx.foesAround(p, 12.0)) {
            if (!DjinnTargets.isMob(t)) continue;
            double dd = t.distanceToSqr(p);
            if (dd < bd) { bd = dd; best = t; }
        }
        if (best == null) return;
        Vec3 from = p.position().add(0, p.getBbHeight() + 0.5, 0), to = best.position().add(0, best.getBbHeight() * 0.6, 0);
        Fx.damage(p, best, 2.0f, DjinnFx.damageType(d.affinity()));
        DjinnFx.line(p.serverLevel(), from, to, DjinnFx.particle(d.affinity()), 14);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(p.serverLevel(), com.newuniverse.nusmp.relics.Fx.align(d.glow()), to.x, to.y, to.z, 5, 0.2, 0.2, 0.2, 0.02);
        sound(p.serverLevel(), best.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f, 1.5f);
    }

    // ================================================================ Extreme Magic
    /** How many strikes an Extreme Magic makes, four ticks apart. */
    static int steps(Djinn.Shape shape) { return shape.steps(); }

    /** The marked points of a bolt or column spell (empty for the others): fixed when the cast begins so the telegraph shows exactly where they fall. */
    static List<Vec3> points(Djinn d, Vec3 centre, Vec3 dir, RandomSource rng) {
        List<Vec3> out = new ArrayList<>();
        Djinn.Extreme x = d.extreme();
        if (x.shape() == Djinn.Shape.BOLTS) {
            double golden = Math.PI * (3.0 - Math.sqrt(5.0)), start = rng.nextDouble() * Math.PI * 2;
            for (int i = 0; i < 9; i++) {
                double r = 3.0 + (x.radius() - 4.0) * Math.sqrt((i + 0.5) / 9.0), a = start + i * golden;
                out.add(centre.add(Math.cos(a) * r, 0, Math.sin(a) * r));
            }
        } else if (x.shape() == Djinn.Shape.COLUMNS) {
            double start = rng.nextDouble() * Math.PI * 2;
            for (int i = 0; i < 8; i++) {
                double a = start + i * Math.PI / 4, r = x.radius() * (i % 2 == 0 ? 0.55 : 0.8);
                out.add(centre.add(Math.cos(a) * r, 0, Math.sin(a) * r));
            }
        } else if (x.shape() == Djinn.Shape.RAIN) {
            int n = Djinn.Shape.RAIN.steps();                                   // the golden-angle spiral again, out from the caster so the rain sweeps outward
            double golden = Math.PI * (3.0 - Math.sqrt(5.0)), start = rng.nextDouble() * Math.PI * 2;
            for (int i = 0; i < n; i++) {
                double r = 2.5 + (x.radius() - 3.5) * Math.sqrt((i + 0.5) / n), a = start + i * golden;
                out.add(centre.add(Math.cos(a) * r, 0, Math.sin(a) * r));
            }
        }
        return out;
    }

    /** What the three seconds before the strike look like: the reach drawn on the ground, a ring growing to it, the marks, the caster lit. */
    static void telegraph(ServerPlayer p, DjinnEngine.Run r, long age) {
        ServerLevel l = p.serverLevel();
        Djinn d = r.djinn;
        Djinn.Extreme x = d.extreme();
        Vec3 c = r.centre;
        double R = x.radius(), prog = Math.min(1.0, age / (double) DjinnPhaseRules.EXTREME_CHARGE);
        DjinnFx.ring(l, c, R, d.color(), (int) (R * 5), 0.12);
        DjinnFx.ring(l, c, Math.max(1.0, R * prog), d.glow(), (int) (R * 4 * Math.max(0.3, prog)), 0.22);
        if (age == 0 || age == 32) Fx.ring(l, c, R * 0.6, d.glow());
        switch (x.shape()) {
            case BOLTS -> { for (Vec3 pt : r.points) DjinnFx.ring(l, pt, 1.6, prog > 0.6 ? d.glow() : d.color(), 12, 0.15); }
            case COLUMNS -> { for (Vec3 pt : r.points) DjinnFx.ring(l, pt, 2.6, prog > 0.6 ? d.glow() : d.color(), 16, 0.15); }
            case BEAM -> {
                Vec3 end = c.add(r.dir.scale(R));
                DjinnFx.line(l, c.add(0, 0.2, 0), end.add(0, 0.2, 0), DjinnFx.dust(d.glow(), 1.4f), 40);
                Vec3 side = new Vec3(-r.dir.z, 0, r.dir.x).scale(2.2);
                DjinnFx.line(l, c.add(side).add(0, 0.2, 0), end.add(side).add(0, 0.2, 0), DjinnFx.dust(d.color(), 1.1f), 24);
                DjinnFx.line(l, c.subtract(side).add(0, 0.2, 0), end.subtract(side).add(0, 0.2, 0), DjinnFx.dust(d.color(), 1.1f), 24);
            }
            case VORTEX -> {
                for (int arm = 0; arm < 3; arm++)
                    for (int i = 0; i < 8; i++) {
                        double rr = R * (1.0 - i / 9.0), a = age * 0.25 + arm * 2.0944 + i * 0.5;
                        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.djinn.DjinnFx.align(d.affinity()), c.x + Math.cos(a) * rr, c.y + 0.3 + i * 0.35, c.z + Math.sin(a) * rr, 1, 0, 0, 0, 0);
                    }
            }
            case HEAL_FIELD -> DjinnFx.ring(l, c, R * 0.5, 0xA8FF9A, 24, 0.3);
            case RAIN -> { for (Vec3 pt : r.points) DjinnFx.ring(l, pt, 1.8, prog > 0.6 ? d.glow() : d.color(), 8, 0.15); }
            case CONE -> {
                for (int side = -1; side <= 1; side += 2) {
                    double[] v = WeaponGeo.turn(r.dir.x, r.dir.z, side * CONE_HALF_ANGLE);
                    DjinnFx.line(l, c.add(0, 0.2, 0), c.add(v[0] * R, 0.2, v[1] * R), DjinnFx.dust(prog > 0.6 ? d.glow() : d.color(), 1.2f), 28);
                }
                for (int i = 0; i <= 12; i++) {
                    double[] v = WeaponGeo.turn(r.dir.x, r.dir.z, -CONE_HALF_ANGLE + 2 * CONE_HALF_ANGLE * i / 12.0);
                    com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.Fx.align(d.glow()), c.x + v[0] * R, c.y + 0.2, c.z + v[1] * R, 1, 0, 0, 0, 0);
                }
            }
            case QUAKE -> { for (int k = 1; k < 3; k++) DjinnFx.ring(l, c, R * k / 3.0, k == 2 ? d.glow() : d.color(), (int) (R * 2.5 * k), 0.15); }
            case CROSS -> {
                Vec3 side = new Vec3(-r.dir.z, 0, r.dir.x);
                for (Vec3 axis : new Vec3[]{r.dir, r.dir.scale(-1), side, side.scale(-1)})
                    DjinnFx.line(l, c.add(0, 0.2, 0), c.add(axis.scale(R)).add(0, 0.2, 0), DjinnFx.dust(prog > 0.6 ? d.glow() : d.color(), 1.3f), 24);
            }
            case IMPLODE -> DjinnFx.ring(l, c, Math.max(1.0, R * (1.0 - prog) + 1.0), d.glow(), 28, 0.3);
            default -> { }
        }
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.djinn.DjinnFx.align(d.affinity()), p.getX(), p.getY() + 1.0, p.getZ(), 6, 0.4, 0.8, 0.4, 0.03);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.Fx.align(d.glow()), p.getX(), p.getY() + 2.0, p.getZ(), 4, 0.3, 0.5, 0.3, 0.0);
        if (age % 20 == 0) sound(l, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, 2.2f, 0.6f + (float) prog);
    }

    /** One strike of the spell (step 0..steps-1). */
    static void fire(ServerPlayer p, DjinnEngine.Run r, int step) {
        Djinn d = r.djinn;
        Djinn.Extreme x = d.extreme();
        switch (x.shape()) {
            case BOLTS -> { if (step < r.points.size()) bolt(p, r, r.points.get(step)); }
            case COLUMNS -> { for (Vec3 pt : r.points) column(p, r, pt); }
            case WAVE -> burst(p, r, 1.5, 0.3, true);
            case VORTEX -> vortex(p, r);
            case BEAM -> beam(p, r);
            case HEAL_FIELD -> healField(p, r);
            case RAIN -> { if (step < r.points.size()) drop(p, r, r.points.get(step)); }
            case CONE -> cone(p, r);
            case QUAKE -> quake(p, r, step);
            case CROSS -> cross(p, r);
            case IMPLODE -> implode(p, r, step);
            default -> burst(p, r, 0.9, 0.35, false);
        }
        if (step == 0) removeRoot(p);
    }

    private static void removeRoot(ServerPlayer p) { p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN); }

    /** Hurts a foe for this spell: the Djinn's damage times {@code scale}, then its effect. A target is hit at most {@code cap} times in one casting. */
    private static boolean hit(ServerPlayer p, DjinnEngine.Run r, LivingEntity t, double scale, int cap) {
        int n = r.hits.getOrDefault(t.getUUID(), 0);
        if (n >= cap) return false;
        r.hits.put(t.getUUID(), n + 1);
        Djinn.Extreme x = r.djinn.extreme();
        Fx.damageHeavy(p, t, (float) (DarkRules.extremeDamage(x.damage(), r.dark) * scale), DjinnFx.damageType(r.djinn.affinity()));
        if (!x.effectId().isEmpty()) {
            int ticks = x.effectAmp() >= 2 ? BalanceLaw.controlTicks(t, x.effectTicks()) : x.effectTicks();
            DjinnFx.effect(t, x.effectId(), ticks, x.effectAmp());
        }
        return true;
    }

    /** Throws a living thing away from {@code from}: horizontal strength and a little lift, bounded. */
    static void push(LivingEntity t, Vec3 from, double strength, double up) {
        Vec3 away = t.position().subtract(from).multiply(1, 0, 1);
        if (away.lengthSqr() < 1.0E-4) away = new Vec3(1, 0, 0);
        away = away.normalize().scale(Mth.clamp(strength, 0.0, 2.0));
        t.setDeltaMovement(t.getDeltaMovement().add(away.x, Mth.clamp(up, 0.0, 1.0), away.z));
        t.hurtMarked = true;
    }

    private static void sound(ServerLevel l, BlockPos at, SoundEvent s, float vol, float pitch) { l.playSound(null, at, s, SoundSource.PLAYERS, vol, pitch); }

    /** A ball of power round the centre (Nova) or a rolling wall (Wave): everything within the reach, weaker at the edge. */
    private static void burst(ServerPlayer p, DjinnEngine.Run r, double shove, double lift, boolean wave) {
        ServerLevel l = p.serverLevel();
        Djinn d = r.djinn;
        double R = d.extreme().radius();
        Vec3 c = r.centre;
        for (LivingEntity t : DjinnTargets.foesNear(p, c, R)) {
            double dist = Math.sqrt(t.distanceToSqr(c.x, c.y, c.z));
            if (hit(p, r, t, 1.0 - 0.4 * Math.min(1.0, dist / R), 1)) push(t, c, shove * (wave ? 1.0 : 1.0 - 0.3 * dist / R), lift);
        }
        for (int k = 1; k <= 3; k++) DjinnFx.ring(l, c, R * k / 3.0, k == 3 ? d.glow() : d.color(), (int) (R * 4 * k / 2.0), 0.25 * k);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, c.x, c.y + 1, c.z, 1, 0, 0, 0, 0);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, c.x, c.y + 0.8, c.z, wave ? 80 : 6, R * 0.35, 0.6, R * 0.35, 0.1);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.djinn.DjinnFx.align(d.affinity()), c.x, c.y + 0.8, c.z, 60, R * 0.4, 0.8, R * 0.4, 0.12);
        sound(l, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), 3.0f, 0.7f);
    }

    private static void bolt(ServerPlayer p, DjinnEngine.Run r, Vec3 at) {
        ServerLevel l = p.serverLevel();
        Djinn d = r.djinn;
        for (LivingEntity t : DjinnTargets.foesNear(p, at, 3.0)) hit(p, r, t, 0.7, 2);
        DjinnFx.column(l, at.x, at.y, at.y + 16, at.z, com.newuniverse.nusmp.particle.rukh.RukhParticles.light(), 32);
        DjinnFx.column(l, at.x, at.y, at.y + 12, at.z, DjinnFx.dust(d.glow(), 1.6f), 24);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0);
        DjinnFx.ring(l, at, 3.0, d.glow(), 24, 0.2);
        sound(l, BlockPos.containing(at), SoundEvents.LIGHTNING_BOLT_IMPACT, 2.0f, 1.0f);
        sound(l, BlockPos.containing(at), SoundEvents.LIGHTNING_BOLT_THUNDER, 2.5f, 1.2f);
    }

    private static void column(ServerPlayer p, DjinnEngine.Run r, Vec3 at) {
        ServerLevel l = p.serverLevel();
        Djinn d = r.djinn;
        for (LivingEntity t : DjinnTargets.foesNear(p, at, 2.6)) if (hit(p, r, t, 1.0, 1)) push(t, at, 0.2, 0.9);
        DjinnFx.column(l, at.x, at.y, at.y + 14, at.z, DjinnFx.particle(d.affinity()), 36);
        DjinnFx.column(l, at.x, at.y, at.y + 14, at.z, DjinnFx.dust(d.glow(), 1.8f), 30);
        DjinnFx.ring(l, at, 2.6, d.color(), 20, 0.2);
        sound(l, BlockPos.containing(at), SoundEvents.WARDEN_SONIC_BOOM, 1.2f, 1.4f);
    }

    private static void vortex(ServerPlayer p, DjinnEngine.Run r) {
        ServerLevel l = p.serverLevel();
        Djinn d = r.djinn;
        double R = d.extreme().radius();
        Vec3 c = r.centre;
        for (LivingEntity t : DjinnTargets.foesNear(p, c, R)) {
            if (!hit(p, r, t, 1.0, 1)) continue;
            Vec3 in = c.subtract(t.position()).multiply(1, 0, 1);
            Vec3 pull = in.lengthSqr() < 1.0E-4 ? Vec3.ZERO : in.normalize().scale(Math.min(1.1, in.length() * 0.18));
            t.setDeltaMovement(t.getDeltaMovement().add(pull.x, 0.85, pull.z));
            t.hurtMarked = true;
        }
        for (int arm = 0; arm < 4; arm++)
            for (int i = 0; i < 14; i++) {
                double rr = R * (1.0 - i / 15.0), a = arm * Math.PI / 2 + i * 0.55;
                com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.djinn.DjinnFx.align(d.affinity()), c.x + Math.cos(a) * rr, c.y + 0.3 + i * 0.45, c.z + Math.sin(a) * rr, 2, 0.1, 0.1, 0.1, 0.02);
            }
        DjinnFx.ring(l, c, R, d.glow(), (int) (R * 5), 0.3);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, c.x, c.y + 1, c.z, 2, 1.0, 0.5, 1.0, 0.0);
        sound(l, p.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, 2.0f, 0.6f);
    }

    private static void beam(ServerPlayer p, DjinnEngine.Run r) {
        ServerLevel l = p.serverLevel();
        Djinn d = r.djinn;
        double R = d.extreme().radius();
        Vec3 c = r.centre, dir = r.dir;
        for (LivingEntity t : DjinnTargets.foesNear(p, c.add(dir.scale(R * 0.5)), R * 0.5 + 3.0)) {
            Vec3 rel = t.position().subtract(c);
            double along = rel.dot(dir), side = Math.abs(rel.x * -dir.z + rel.z * dir.x);
            if (along >= -1.0 && along <= R && side <= 2.4 && hit(p, r, t, 1.0, 1)) push(t, c, 0.7, 0.3);
        }
        Vec3 end = c.add(dir.scale(R));
        DjinnFx.line(l, c.add(0, 1.0, 0), end.add(0, 1.0, 0), DjinnFx.particle(d.affinity()), 40);
        DjinnFx.line(l, c.add(0, 1.0, 0), end.add(0, 1.0, 0), DjinnFx.dust(d.glow(), 2.0f), 40);
        DjinnFx.line(l, c.add(0, 0.3, 0), end.add(0, 0.3, 0), DjinnFx.dust(d.color(), 1.6f), 40);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, end.x, end.y + 1, end.z, 1, 0, 0, 0, 0);
        p.setDeltaMovement(dir.x * 1.4, 0.2, dir.z * 1.4);
        p.hurtMarked = true;
        sound(l, p.blockPosition(), SoundEvents.FIRECHARGE_USE, 2.0f, 0.6f);
        sound(l, BlockPos.containing(end), SoundEvents.GENERIC_EXPLODE.value(), 2.0f, 0.9f);
    }

    private static void healField(ServerPlayer p, DjinnEngine.Run r) {
        ServerLevel l = p.serverLevel();
        Djinn d = r.djinn;
        double R = d.extreme().radius();
        Vec3 c = r.centre;
        for (LivingEntity t : DjinnTargets.foesNear(p, c, R)) hit(p, r, t, 1.0, 1);
        for (ServerPlayer a : DjinnTargets.alliesNear(p, R)) {
            BalanceLaw.heal(a, 8f);
            a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 140, 1, true, false));
            com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, a.getX(), a.getY() + 1, a.getZ(), 12, 0.4, 0.6, 0.4, 0.05);
        }
        for (int k = 1; k <= 3; k++) DjinnFx.ring(l, c, R * k / 3.0, k == 3 ? d.glow() : d.color(), (int) (R * 3 * k), 0.25 * k);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, c.x, c.y + 1, c.z, 90, R * 0.4, 0.9, R * 0.4, 0.05);
        sound(l, p.blockPosition(), SoundEvents.TOTEM_USE, 1.6f, 1.2f);
    }

    // ---------------------------------------------------------------- the shapes added in 1.93.0
    /** The half angle (degrees) of the Cone shape: a fan of 110 degrees in front of the caster. */
    static final double CONE_HALF_ANGLE = 55.0;
    /** The half width of a Cross arm and the Quake's band count. */
    private static final double ARM = 2.2;

    /** Pulls a living thing towards {@code to} (horizontally): the strength is bounded by 0.25 of its distance so it never overshoots the caster. */
    static void pull(LivingEntity t, Vec3 to, double strength, double up) {
        Vec3 in = to.subtract(t.position()).multiply(1, 0, 1);
        double len = in.length();
        if (len < 1.0E-4) return;
        double s = Mth.clamp(Math.min(strength, len * 0.25), 0.0, 2.0);
        t.setDeltaMovement(t.getDeltaMovement().add(in.normalize().scale(s)).add(0, Mth.clamp(up, 0.0, 1.0), 0));
        t.hurtMarked = true;
    }

    /** One drop of a Rain: foes within 2.6 of the mark take half the damage (a target is hit twice at most, so a whole casting is the row's damage once). */
    private static void drop(ServerPlayer p, DjinnEngine.Run r, Vec3 at) {
        ServerLevel l = p.serverLevel();
        Djinn d = r.djinn;
        for (LivingEntity t : DjinnTargets.foesNear(p, at, 2.6)) hit(p, r, t, 0.5, 2);
        DjinnFx.column(l, at.x, at.y, at.y + 12, at.z, DjinnFx.particle(d.affinity()), 24);
        DjinnFx.column(l, at.x, at.y, at.y + 10, at.z, DjinnFx.dust(d.glow(), 1.5f), 18);
        DjinnFx.ring(l, at, 2.6, d.glow(), 16, 0.2);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0);
        sound(l, BlockPos.containing(at), SoundEvents.WARDEN_SONIC_BOOM, 0.7f, 1.7f);
    }

    /** A fan of power in front of the caster: foes within the reach and 55 degrees of the cast's direction, weaker at the edge, shoved forward. */
    private static void cone(ServerPlayer p, DjinnEngine.Run r) {
        ServerLevel l = p.serverLevel();
        Djinn d = r.djinn;
        double R = d.extreme().radius();
        Vec3 c = r.centre, dir = r.dir;
        for (LivingEntity t : DjinnTargets.foesNear(p, c, R)) {
            if (!WeaponGeo.inCone(c.x, c.z, dir.x, dir.z, t.getX(), t.getZ(), R, CONE_HALF_ANGLE)) continue;
            double dist = Math.sqrt(t.distanceToSqr(c.x, c.y, c.z));
            if (hit(p, r, t, 1.0 - 0.3 * Math.min(1.0, dist / R), 1)) push(t, c, 1.0 - 0.3 * dist / R, 0.35);
        }
        for (int k = 1; k <= 3; k++)
            for (int i = 0; i <= 14; i++) {
                double[] v = WeaponGeo.turn(dir.x, dir.z, -CONE_HALF_ANGLE + 2 * CONE_HALF_ANGLE * i / 14.0);
                double rr = R * k / 3.0;
                com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, k == 3 ? com.newuniverse.nusmp.relics.Fx.align(d.glow()) : com.newuniverse.nusmp.relics.djinn.DjinnFx.align(d.affinity()),
                        c.x + v[0] * rr, c.y + 0.4 + 0.3 * k, c.z + v[1] * rr, 2, 0.15, 0.15, 0.15, 0.04);
            }
        DjinnFx.line(l, c.add(0, 1.0, 0), c.add(dir.scale(R)).add(0, 1.0, 0), DjinnFx.dust(d.glow(), 2.0f), 40);
        sound(l, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.8f);
        sound(l, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, 2.0f, 0.5f);
    }

    /** One wave of a Quake (step 0..2): the foes in that third of the reach are hurt and thrown up. The outer waves come four ticks after the inner. */
    private static void quake(ServerPlayer p, DjinnEngine.Run r, int step) {
        ServerLevel l = p.serverLevel();
        Djinn d = r.djinn;
        double R = d.extreme().radius(), inner = R * step / 3.0, outer = R * (step + 1) / 3.0;
        Vec3 c = r.centre;
        for (LivingEntity t : DjinnTargets.foesNear(p, c, R)) {
            double dist = Math.sqrt((t.getX() - c.x) * (t.getX() - c.x) + (t.getZ() - c.z) * (t.getZ() - c.z));
            boolean in = dist >= inner && (dist < outer || (step == 2 && dist <= outer));
            if (in && hit(p, r, t, 1.0, 1)) push(t, c, 0.3, 0.8);
        }
        DjinnFx.ring(l, c, outer, d.glow(), (int) (outer * 5), 0.3);
        DjinnFx.ring(l, c, Math.max(1.0, outer - 1.0), d.color(), (int) (outer * 4), 0.1);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.djinn.DjinnFx.align(d.affinity()), c.x, c.y + 0.5, c.z, 50, outer * 0.4, 0.5, outer * 0.4, 0.25);
        sound(l, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), 2.0f, 0.5f + 0.2f * step);
        sound(l, p.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, 1.6f, 0.5f);
    }

    /** Four arms of power from the caster (forward, back, left, right), each 4.4 wide and the reach long. */
    private static void cross(ServerPlayer p, DjinnEngine.Run r) {
        ServerLevel l = p.serverLevel();
        Djinn d = r.djinn;
        double R = d.extreme().radius();
        Vec3 c = r.centre, dir = r.dir, side = new Vec3(-dir.z, 0, dir.x);
        for (LivingEntity t : DjinnTargets.foesNear(p, c, R + 2.0)) {
            Vec3 rel = t.position().subtract(c);
            double a = rel.x * dir.x + rel.z * dir.z, b = rel.x * side.x + rel.z * side.z;
            boolean in = (Math.abs(b) <= ARM && Math.abs(a) <= R) || (Math.abs(a) <= ARM && Math.abs(b) <= R);
            if (in && hit(p, r, t, 1.0, 1)) push(t, c, 0.8, 0.3);
        }
        for (Vec3 axis : new Vec3[]{dir, dir.scale(-1), side, side.scale(-1)}) {
            Vec3 end = c.add(axis.scale(R));
            DjinnFx.line(l, c.add(0, 1.0, 0), end.add(0, 1.0, 0), DjinnFx.particle(d.affinity()), 40);
            DjinnFx.line(l, c.add(0, 1.0, 0), end.add(0, 1.0, 0), DjinnFx.dust(d.glow(), 2.0f), 40);
            DjinnFx.line(l, c.add(0, 0.3, 0), end.add(0, 0.3, 0), DjinnFx.dust(d.color(), 1.6f), 30);
            com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, end.x, end.y + 1, end.z, 1, 0, 0, 0, 0);
        }
        DjinnFx.ring(l, c, 3.0, d.glow(), 24, 0.3);
        sound(l, p.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, 2.0f, 0.9f);
        sound(l, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), 2.0f, 1.0f);
    }

    /** Step 0 drags every foe in the reach towards the caster (no damage); step 1, four ticks later, collapses the hollow: hurt, harder near the middle. */
    private static void implode(ServerPlayer p, DjinnEngine.Run r, int step) {
        ServerLevel l = p.serverLevel();
        Djinn d = r.djinn;
        double R = d.extreme().radius();
        Vec3 c = r.centre;
        if (step == 0) {
            for (LivingEntity t : DjinnTargets.foesNear(p, c, R)) pull(t, c, 1.3, 0.25);
            for (int arm = 0; arm < 4; arm++)
                for (int i = 0; i < 10; i++) {
                    double rr = R * (1.0 - i / 11.0), a = arm * Math.PI / 2 + i * 0.5;
                    com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.djinn.DjinnFx.align(d.affinity()), c.x + Math.cos(a) * rr, c.y + 0.4 + i * 0.2, c.z + Math.sin(a) * rr, 1, 0.1, 0.1, 0.1, 0.02);
                }
            DjinnFx.ring(l, c, R, d.color(), (int) (R * 4), 0.3);
            sound(l, p.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, 2.0f, 0.8f);
            return;
        }
        for (LivingEntity t : DjinnTargets.foesNear(p, c, R)) {
            double dist = Math.sqrt(t.distanceToSqr(c.x, c.y, c.z));
            if (hit(p, r, t, 1.3 - 0.6 * Math.min(1.0, dist / R), 1)) push(t, c, 1.0 - 0.4 * dist / R, 0.5);
        }
        for (int k = 1; k <= 3; k++) DjinnFx.ring(l, c, R * k / 3.0, k == 3 ? d.glow() : d.color(), (int) (R * 4 * k / 2.0), 0.25 * k);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment.LIGHT, c.x, c.y + 1, c.z, 2, 0, 0, 0, 0);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.djinn.DjinnFx.align(d.affinity()), c.x, c.y + 0.8, c.z, 60, R * 0.4, 0.8, R * 0.4, 0.2);
        sound(l, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), 3.0f, 0.6f);
    }

    /** A bolt-shaped decoration for the cast's first moment: the caster flares. */
    static void flare(ServerPlayer p, Djinn d, ParticleOptions extra) {
        ServerLevel l = p.serverLevel();
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.Fx.alignOf(extra), p.getX(), p.getY() + 1, p.getZ(), 24, 0.5, 0.9, 0.5, 0.05);
        com.newuniverse.nusmp.particle.rukh.RukhFx.spray(l, com.newuniverse.nusmp.relics.Fx.align(d.glow()), p.getX(), p.getY() + 1, p.getZ(), 24, 0.5, 0.9, 0.5, 0.0);
    }
}
