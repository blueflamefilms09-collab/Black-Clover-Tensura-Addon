package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.FaithGodRules;
import com.newuniverse.nusmp.skill.rules.FaithGodRules.Move;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The five verbs the canon pass of the Faith gods adds (docs/faith_gods.md "Canon pass"; SERVER setting faithGods.canon): the gravity well (Femto, Ill Ilah's Black Spot), the press (Lucifero's Gravity Magic),
 * holy magic that bites the undead (Aqua), Fairy Law (Mavis: foes only, paid for with life) and the life-point beam (Ra). Same limits as {@link FaithGodMoves}: every target list is {@link SkillTargets} (at most
 * 24), every hit goes through {@link SkillDamage}, nothing spawns, nothing ticks. The ones that cost the caster health (Law, Point-to-Point) live here, take it only after the move worked, never take the caster
 * under one heart ({@link FaithGodRules#hpPaid}) and cost nothing in creative. The health is the player's own on the server; nothing is stored.
 */
final class FaithGodCanonMoves {
    private FaithGodCanonMoves() {}

    private static boolean noFoes(ServerPlayer p) { SkillUtil.fail(p, SkillMsg.t("gods.no_foes", "No foe in reach.")); return false; }

    private static void pay(ServerPlayer p, double paid) { if (paid > 0.0) p.setHealth((float) (p.getHealth() - paid)); }

    private static void push(LivingEntity e, Vec3 motion) {
        e.setDeltaMovement(motion);
        e.hurtMarked = true;      // tells the client of a player target about the push
    }

    /** Gravity well: foes in reach are pulled toward the caster, slowed and hurt. */
    static boolean well(ServerPlayer p, ManasSkillInstance inst, Vec3 at, double r, double amount, double mf, double strength) {
        List<LivingEntity> foes = SkillTargets.foes(p, at, r);
        if (foes.isEmpty()) return noFoes(p);
        int t = (int) Math.max(40, Math.min(200, 100 * Math.sqrt(Math.max(0.1, strength))));
        for (LivingEntity f : foes) {
            Vec3 d = at.subtract(f.position());
            double len = d.length();
            if (len > 1.0) push(f, f.getDeltaMovement().add(d.scale(Math.min(1.1, len * 0.2) / len)));
            f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, t, 2));
            SkillDamage.hit(p, f, amount, mf, SkillDamage.Kind.MAGIC, inst);
        }
        return true;
    }

    /** Gravity Prostration: foes in reach are forced down (no upward motion), slowed, weakened and hurt a little. */
    static boolean press(ServerPlayer p, ManasSkillInstance inst, Vec3 at, double r, double amount, double mf, double strength) {
        List<LivingEntity> foes = SkillTargets.foes(p, at, r);
        if (foes.isEmpty()) return noFoes(p);
        int t = (int) Math.max(40, Math.min(240, 140 * Math.sqrt(Math.max(0.1, strength))));
        for (LivingEntity f : foes) {
            Vec3 v = f.getDeltaMovement();
            push(f, new Vec3(v.x * 0.3, Math.min(v.y, -0.6), v.z * 0.3));
            f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, t, 3));
            f.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, t, 0));
            SkillDamage.hit(p, f, amount, mf, SkillDamage.Kind.MAGIC, inst);
        }
        return true;
    }

    /** Holy magic around the caster: heavy on the undead, light on everything else (Aqua). */
    static boolean holy(ServerPlayer p, ManasSkillInstance inst, Vec3 at, double r, double amount, double mf) {
        List<LivingEntity> foes = SkillTargets.foes(p, at, r);
        if (foes.isEmpty()) return noFoes(p);
        for (LivingEntity f : foes) SkillDamage.hit(p, f, amount * FaithGodRules.holyFactor(f.getType().is(EntityTypeTags.UNDEAD)), mf, SkillDamage.Kind.MAGIC, inst);
        return true;
    }

    /** Fairy Law: only foes (never allies, never bystanders), and the caster pays life for every foe beyond the first. */
    static boolean law(ServerPlayer p, ManasSkillInstance inst, Vec3 at, double r, double amount, double mf) {
        List<LivingEntity> foes = SkillTargets.foes(p, at, r);
        if (foes.isEmpty()) return noFoes(p);
        double paid = p.isCreative() ? 0.0 : FaithGodRules.hpPaid(p.getHealth(), p.getMaxHealth(), FaithGodRules.lawCostFrac(foes.size()));
        for (LivingEntity f : foes) SkillDamage.hit(p, f, amount, mf, SkillDamage.Kind.MAGIC, inst);
        pay(p, paid);
        return true;
    }

    /** Point-to-Point Transfer: a beam whose damage grows with the health the caster pays (up to x1.6 for 30 percent of max health); nothing is paid when the beam finds no foe. */
    static boolean lifeBeam(ServerPlayer p, ManasSkillInstance inst, Move m, double range, double amount, double mf, int rgb) {
        double paid = p.isCreative() ? 0.0 : FaithGodRules.hpPaid(p.getHealth(), p.getMaxHealth(), m.hpFrac());
        double factor = FaithGodRules.lifeBeamFactor(p.isCreative() ? p.getMaxHealth() * m.hpFrac() : paid, p.getMaxHealth());      // only health really paid buys damage
        if (!FaithGodMoves.beam(p, inst, range, amount * factor, mf, rgb)) return false;
        pay(p, paid);
        return true;
    }
}
