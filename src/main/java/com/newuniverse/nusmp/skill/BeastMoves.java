package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.veldanava.skills.JudgmentEffects;
import com.newuniverse.nusmp.skill.rules.BeastSkillRules;
import com.newuniverse.nusmp.skill.rules.BeastSkillRules.Spec;
import com.newuniverse.nusmp.skill.rules.DbRules;
import com.newuniverse.nusmp.skill.rules.DbRules.Mode;
import com.newuniverse.nusmp.skill.rules.DbRules.Move;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 3.58.0: what the active modes of the Beast Will skills DO. Beam, blast, stop, rush, barrage, disc, seal and psychic are the Dragon Ball dispatcher's ({@link DbMoves}, the same bounded code: at most 24
 * creatures and 24 projectiles per press, one entity query per area, damage through {@link SkillDamage}, control through {@link BalanceLaw}); guard, veil, mend and mark are the beast's own, written here
 * with no persistent hooks (they use plain timed effects only). The finisher adds one effect shape of its own ({@link Spec#fx()}). Nothing here spawns an entity, places a block or sends more than the
 * dispatcher's own effect packets plus one shape. Server thread only.
 */
final class BeastMoves {
    private BeastMoves() {}

    /** Runs the move of a mode; false when it did nothing (the caller gives the cost back and starts no cooldown). */
    static boolean cast(Spec spec, DbRules.Spec db, boolean ult, Mode mode, ServerPlayer p, double mastery) {
        Move mv = mode.move();
        ServerLevel level = p.serverLevel();
        boolean done = switch (mv.prim()) {
            case GUARD -> guard(level, p, mv, ult);
            case VEIL -> veil(level, p, mv);
            case CAPSULE -> mend(level, p, mv, ult, BeastSkillRules.power(spec, ult));
            case SCAN -> mark(level, p, mv);
            default -> DbMoves.cast(db, ult, mode, p, mastery);
        };
        if (!done) return false;
        if (mv.has(DbRules.HEAL) && mv.prim() != DbRules.Prim.GUARD && mv.prim() != DbRules.Prim.CAPSULE)
            p.heal((float) (p.getMaxHealth() * BeastSkillRules.FINISHER_HEAL_SHARE * BeastSkillRules.power(spec, ult)));
        if (mode.mastery()) finisherFx(level, p, spec, mv);
        return true;
    }

    /** The finisher's own look, one shape: {@code CLASS:SHAPE} of the beast's table row. A shape this build does not know is skipped. */
    private static void finisherFx(ServerLevel level, ServerPlayer p, Spec spec, Move mv) {
        String fx = spec.fx();
        int colon = fx.indexOf(':');
        if (colon != 1) return;
        VfxShape shape;
        try { shape = VfxShape.valueOf(fx.substring(2)); } catch (IllegalArgumentException e) { return; }
        Vec3 eye = p.getEyePosition(), aim = DMUtil.lookPoint(p, Math.max(8.0, Math.min(40.0, mv.range() > 0 ? mv.range() : 16.0)));
        float power = (float) Math.max(1.0, Math.min(4.0, Math.max(mv.radius(), 3.0) / 3.0));
        switch (fx.charAt(0)) {
            case 'L' -> SkillFx.shape(level, shape, eye.add(0, -0.3, 0), aim, spec.rgb(), 30, power);
            case 'G' -> SkillFx.shape(level, shape, aim, aim.add(0, 1, 0), spec.rgb(), 40, power);
            case 'F' -> {
                Vec3 look = p.getViewVector(1.0f);
                SkillFx.shape(level, shape, p.position().add(look.x * 3.0, 1.0, look.z * 3.0), eye.add(look.scale(8.0)), spec.rgb(), 40, power);
            }
            default -> SkillFx.follow(level, shape, p, p.position().add(0, 1, 0), spec.rgb(), 40, power);
        }
    }

    // ---------------------------------------------------------------- the beast's own four
    /** Absorption and resistance for the caster and the allies near (at most 24), for the move's lock ticks. No persistent hook: the effects are the whole guard. */
    private static boolean guard(ServerLevel level, ServerPlayer p, Move mv, boolean ult) {
        int ticks = Math.max(40, mv.lock());
        List<LivingEntity> who = new ArrayList<>(List.of(p));
        if (mv.radius() > 0) for (LivingEntity a : SkillTargets.alliesAround(p, p.position(), mv.radius())) if (a != p && who.size() < DbRules.MAX_TARGETS) who.add(a);
        for (LivingEntity a : who) {
            a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, ult ? 3 : 1, false, true, true));
            a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, ult ? 2 : 1, false, true, true));
            if (mv.has(DbRules.HEAL)) a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 1, false, true, true));
        }
        if (mv.has(DbRules.FREEZE_PROJ)) fireWard(p, ticks);
        SkillFx.aura(level, p, false);
        SkillFx.ring(level, p.position().add(0, 0.2, 0), 2.5, mv.rgb() & 0xFFFFFF);
        return true;
    }

    /** Fire and projectile ward: the caster burns no more for the guard's duration (fire resistance). */
    private static void fireWard(ServerPlayer p, int ticks) { p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks, 0, false, false, true)); }

    /** Vanish for four seconds, faster, and blind the foes within {@code range} blocks (at most 12) for three. */
    private static boolean veil(ServerLevel level, ServerPlayer p, Move mv) {
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 2, false, true, true));
        int n = 0;
        for (LivingEntity t : SkillTargets.foes(p, p.position(), Math.max(4.0, mv.range()))) {
            if (n++ >= 12) break;
            t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BalanceLaw.controlTicks(t, 60), 0, false, true, true));
        }
        SkillFx.burst(level, p.position().add(0, 1, 0), true, 20, 0.8);
        SkillFx.ring(level, p.position().add(0, 0.2, 0), 3.0, mv.rgb() & 0xFFFFFF);
        return true;
    }

    /** Heal the caster and the allies within the radius (at most 24) by a share of their health, plus a little regeneration. */
    private static boolean mend(ServerLevel level, ServerPlayer p, Move mv, boolean ult, double k) {
        List<LivingEntity> who = new ArrayList<>(List.of(p));
        for (LivingEntity a : SkillTargets.alliesAround(p, p.position(), Math.max(4.0, mv.radius()))) if (a != p && who.size() < DbRules.MAX_TARGETS) who.add(a);
        for (LivingEntity a : who) {
            a.heal((float) (a.getMaxHealth() * BeastSkillRules.MEND_SHARE * k));
            a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ult ? 200 : 100, 1, false, true, true));
        }
        SkillFx.burst(level, p.position().add(0, 1, 0), false, 16, 0.8);
        SkillFx.ring(level, p.position().add(0, 0.2, 0), Math.min(mv.radius(), 12), mv.rgb() & 0xFFFFFF);
        SkillUtil.actionbar(p, SkillMsg.t("beast.mend", "The beast's blessing mends %s.", who.size()));
        return true;
    }

    /** Mark the foes within the radius (at most {@code n}, at most 24): glowing, slowed, and whatever the flags add. Pure control, no damage. */
    private static boolean mark(ServerLevel level, ServerPlayer p, Move mv) {
        int cap = Math.min(Math.max(1, mv.n()), DbRules.MAX_TARGETS), count = 0;
        for (LivingEntity t : SkillTargets.foes(p, p.position(), Math.min(48.0, Math.max(4.0, mv.radius())))) {
            if (count++ >= cap) break;
            t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false, true));
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.controlTicks(t, 100), 0, false, true, true));
            if (mv.has(DbRules.ARMOR_BREAK)) t.addEffect(new MobEffectInstance(JudgmentEffects.ARMOR_BREAK, 200, 0, false, true, true));
        }
        SkillFx.ring(level, p.position().add(0, 0.2, 0), Math.min(mv.radius(), 24), mv.rgb() & 0xFFFFFF);
        SkillUtil.actionbar(p, SkillMsg.t("beast.mark", "The beast marks %s foes.", Math.min(count, cap)));
        return true;
    }
}
