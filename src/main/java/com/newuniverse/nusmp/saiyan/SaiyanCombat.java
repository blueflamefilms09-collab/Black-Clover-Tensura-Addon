package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Arrays;

/**
 * Why: everything a Saiyan's fighting does to the damage numbers and to the Saiyan's growth (3.65.0, Stage 2), in one place the damage event calls. DEFENDING: an automatic dodge (the race's Warrior's
 * Reflex, or the stronger dodge of an Ultra Instinct form) that is a chance with a cool-down, a stamina price and a budget per window and can never pass {@link SaiyanCombatRules#HARD_DODGE_CAP}; Battle
 * Adaptation (a resistance of at most fifteen percent to the kind of damage taken again and again, gone when the fight is over); the durability bonus and the active form's defence; and the mark of a
 * near-death against a worthy foe that a later recovery turns into a Zenkai. ATTACKING: the strength bonus, the active form, the counter window a perfect dodge opens, and the training that worthy blows earn
 * (under the daily caps). The arithmetic is {@link SaiyanCombatRules}; this class only reads the game and applies the result. Cost: a handful of comparisons per damage event of a Saiyan; no entity query.
 */
final class SaiyanCombat {
    private SaiyanCombat() {}

    /** True while the Saiyan's own scripted blow (a technique that already computed its damage) is being dealt, so the strength and form bonuses are not applied a second time. Server thread only. */
    static boolean skipBonus;

    static SaiyanCombatRules.Kind kindOf(DamageSource src) {
        if (src.is(DamageTypeTags.IS_EXPLOSION)) return SaiyanCombatRules.Kind.EXPLOSION;
        if (src.is(DamageTypeTags.IS_FIRE)) return SaiyanCombatRules.Kind.FIRE;
        if (src.is(DamageTypeTags.IS_PROJECTILE)) return SaiyanCombatRules.Kind.PROJECTILE;
        if (src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC)) return SaiyanCombatRules.Kind.MAGIC;
        if (src.getDirectEntity() instanceof LivingEntity) return SaiyanCombatRules.Kind.MELEE;
        return SaiyanCombatRules.Kind.OTHER;
    }

    // ================================================================== a blow lands on a Saiyan
    static void defend(LivingIncomingDamageEvent e, ServerPlayer v, SaiyanState s, SaiyanManager.Run r, long now) {
        DamageSource src = e.getSource();
        r.lastDamaged = now;
        r.lastCombat = now;
        boolean attacked = src.getEntity() != null || src.getDirectEntity() != null;
        if (attacked && e.getAmount() > 0f && dodge(e, v, s, r, now)) return;

        double resist = 0.0;
        if (attacked) {
            int k = kindOf(src).ordinal();
            if (SaiyanCombatRules.adaptationOver(now, r.adaptLast, SaiyanConfig.adaptReset())) Arrays.fill(r.adapt, 0);
            resist = SaiyanCombatRules.adaptation(r.adapt[k], SaiyanConfig.adaptMax(), SaiyanConfig.adaptHits());
            r.adapt[k] = Math.min(r.adapt[k] + 1, 10_000);
            r.adaptLast = now;
        }
        double mul = SaiyanRules.takenFactor(SaiyanConfig.bonuses(s.battlePower).durability()) * SaiyanFormEngine.takenFactor(s, r) * (1.0 - resist);
        float after = (float) (e.getAmount() * mul);
        e.setAmount(after);

        if (src.getEntity() instanceof LivingEntity a && a != v) {
            double power = SaiyanManager.powerOfAny(a);
            double w = SaiyanRules.difficultyWeight(power, s.battlePower, SaiyanConfig.minRatio());
            if (w > 0.0) train(s, SaiyanTraining.Cat.PHYSICAL, w, now);
            if (SaiyanConfig.zenkaiEnabled() && r.nearDeathAt < 0 && SaiyanCombatRules.nearDeath(v.getHealth() - after, v.getMaxHealth(), SaiyanConfig.zenkaiNear())
                    && SaiyanCombatRules.worthy(power, s.battlePower, SaiyanConfig.zenkaiRatio())) r.nearDeathAt = now;
        }
    }

    /** The dodge: the race's reflex, or the better dodge of the active Ultra Instinct form. True = the blow is cancelled. */
    private static boolean dodge(LivingIncomingDamageEvent e, ServerPlayer v, SaiyanState s, SaiyanManager.Run r, long now) {
        SaiyanForms.Form f = SaiyanFormEngine.active(r);
        SaiyanForms.Dodge d = f == null ? null : SaiyanForms.dodge(f.id(), s.mastery(f.id()));
        double chance = SaiyanCombatRules.reflexChance(SaiyanConfig.reflexBase(), SaiyanConfig.reflexMax(), s.battlePower);
        int cooldown = SaiyanConfig.reflexCooldown(), perWindow = 3, window = 400;
        double stamina = SaiyanConfig.reflexStamina();
        if (d != null) {
            chance = Math.max(chance, d.chance());
            cooldown = d.cooldown();
            stamina = d.stamina();
            perWindow = d.perWindow();
            window = d.window();
            chance = SaiyanCombatRules.capped(chance, SaiyanCombatRules.flowNow(r.flow, now, r.flowAt, 200) * d.flowPerStack());
        }
        if (now < r.reflexReadyAt || !(chance > 0.0)) return false;
        if (!v.isCreative() && s.stamina < stamina) return false;
        if (now - r.dodgeWinStart > window || now < r.dodgeWinStart) {
            r.dodgeWinStart = now;
            r.dodgeUsed = 0;
        }
        if (!SaiyanCombatRules.budgetOk(r.dodgeUsed, r.dodgeWinStart, now, window, perWindow)) return false;
        if (!SaiyanCombatRules.dodged(v.getRandom().nextDouble(), chance)) return false;

        e.setCanceled(true);
        r.dodgeUsed++;
        r.reflexReadyAt = now + cooldown;
        if (!v.isCreative()) s.stamina = SaiyanRules.stamina(s.stamina, -stamina, SaiyanManager.staminaMax(s));
        if (d != null && d.flowPerStack() > 0.0) {
            r.flow = SaiyanCombatRules.flowAfter(r.flow, now, r.flowAt, 200, 5);
            r.flowAt = now;
        }
        if (d != null && d.counter() > 0.0) {
            r.counterUntil = now + 60;
            r.counterBonus = d.counter();
        }
        r.dirty = true;
        var level = v.serverLevel();
        Fx.spray(level, FxThemes.WORLD, FxOld.ELECTRIC_SPARK, v.getX(), v.getY() + 1.0, v.getZ(), 6, 0.4, 0.6, 0.4, 0.08);
        level.playSound(null, v.getX(), v.getY(), v.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.35f, 1.9f);
        return true;
    }

    // ================================================================== a Saiyan's blow lands
    /** The Saiyan's own hand hits: strength, form, counter window; and the training a worthy foe gives. */
    static void attack(LivingIncomingDamageEvent e, ServerPlayer hitter, SaiyanState s, SaiyanManager.Run r, long now) {
        if (skipBonus) return;
        double mul = SaiyanRules.damageFactor(SaiyanConfig.bonuses(s.battlePower).strength()) * SaiyanFormEngine.damageFactor(s, r);
        if (now <= r.counterUntil) {
            mul *= 1.0 + r.counterBonus;
            r.counterUntil = -1;
        }
        e.setAmount((float) (e.getAmount() * mul));
        r.lastCombat = now;
        trainOn(s, e.getEntity(), SaiyanTraining.Cat.MARTIAL, now);
        trainOn(s, e.getEntity(), SaiyanTraining.Cat.PHYSICAL, now);
    }

    /** Training from a hit on a creature: nothing for a weak one, a share of the category's per-hit value for a worthy one, under the daily cap. */
    static void trainOn(SaiyanState s, LivingEntity target, SaiyanTraining.Cat c, long now) {
        double w = SaiyanRules.difficultyWeight(SaiyanManager.powerOfAny(target), s.battlePower, SaiyanConfig.minRatio());
        if (w > 0.0) train(s, c, w, now);
    }

    static void train(SaiyanState s, SaiyanTraining.Cat c, double weight, long now) { s.train(c, SaiyanTraining.perHit(c) * Math.min(1.5, weight), now, SaiyanConfig.trainingScale()); }

    // ================================================================== once a second: the Zenkai
    /** A near-death that was survived and recovered from (the fight over) becomes a Zenkai: permanent Battle Power, under the cool-down, the day's limit and a shrinking return. */
    static void second(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        if (r.nearDeathAt < 0) return;
        if (!SaiyanConfig.zenkaiEnabled() || now - r.nearDeathAt > 12000 || now < r.nearDeathAt) {
            r.nearDeathAt = -1;
            return;
        }
        if (now - r.lastDamaged < 200 || !SaiyanCombatRules.recovered(p.getHealth(), p.getMaxHealth(), SaiyanConfig.zenkaiRecover())) return;
        r.nearDeathAt = -1;
        s.zenkaiDayCount(now);
        if (!SaiyanCombatRules.zenkaiAllowed(now, s.zenkaiAt, SaiyanConfig.zenkaiCooldown(), s.zenkaiToday, SaiyanConfig.zenkaiPerDay())) return;
        grantZenkai(p, s, r, now);
    }

    /** Gives one Zenkai (also used by the operator command): the count, the growth and the message. */
    static double grantZenkai(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        double gain = SaiyanCombatRules.zenkaiGain(s.battlePower, SaiyanConfig.zenkaiGain(), s.zenkai, SaiyanConfig.zenkaiDecay());
        gain = Math.min(gain, Math.max(0.0, SaiyanConfig.maxBp() - s.battlePower));
        s.zenkaiDayCount(now);
        s.zenkai = Math.min(100_000, s.zenkai + 1);
        s.zenkaiToday = Math.min(1000, s.zenkaiToday + 1);
        s.zenkaiAt = now;
        s.setBattlePower(s.battlePower + gain);
        r.dirty = true;
        p.displayClientMessage(Component.translatable("nusmp.saiyan.msg.zenkai", Math.round(gain)), false);
        Fx.spray(p.serverLevel(), FxThemes.WORLD, FxOld.FIREWORK, p.getX(), p.getY() + 1.0, p.getZ(), 24, 0.6, 0.9, 0.6, 0.15);
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.8f);
        return gain;
    }
}
