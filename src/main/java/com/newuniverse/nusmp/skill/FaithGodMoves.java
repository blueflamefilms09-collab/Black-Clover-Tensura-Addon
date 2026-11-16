package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.FaithGodRules;
import com.newuniverse.nusmp.skill.rules.FaithGodRules.God;
import com.newuniverse.nusmp.skill.rules.FaithGodRules.Move;
import com.newuniverse.nusmp.skill.rules.FaithGodRules.Passive;
import com.newuniverse.nusmp.skill.rules.FaithWorshipRules.Worshipper;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * What each {@link FaithGodRules.Verb} does, once, for all eight gods (docs/faith_gods.md): the effects are built from code the mod already has, so no new entity, no new packet and no new effect shape. Every target
 * list is the existing {@link SkillTargets} (at most 24 entries), every hit goes through {@link SkillDamage} (BalanceLaw: EP scaling, the PvP hit cap, kill credit), every heal through {@link BalanceLaw#heal} (the
 * 50 % cap), and the look of a cast is one sigil, one ring and (for a beam) one line of the existing {@link SkillFx} / {@link SkillAnimeFx}, sent to nearby players only. The passives run once a second for the holder
 * and for at most {@link FaithGodRules#MAX_SHARED} worshippers (looked up by UUID, no world scan).
 */
final class FaithGodMoves {
    private FaithGodMoves() {}

    /** The longest potion duration a move gives (ticks), whatever the strength. */
    static final int MAX_EFFECT_TICKS = 1200;
    /** At most this many allies get a spray of effect motes in one cast (the rest still get the potion effects). */
    static final int MAX_FX_ALLIES = 8;

    /** The skill instance whose tag holds the Faith: the player's Faith / Goddess of Faith, or the god skill itself when the player has neither (an admin gave it). */
    static ManasSkillInstance faithPool(ServerPlayer p, ManasSkillInstance god) {
        ManasSkillInstance faith = FaithWorship.faithOf(p);
        return faith != null ? faith : god;
    }

    static int worshipperCount(ManasSkillInstance pool) { return pool == null ? 0 : FaithWorship.worshippers(pool).size(); }

    private static int ticks(double base, double strength) { return (int) Math.max(20, Math.min(MAX_EFFECT_TICKS, base * Math.sqrt(Math.max(0.1, strength)))); }

    /** Runs the move. False = nothing happened (no target, no room): the caller refunds the magicules and keeps the Faith. */
    static boolean cast(God g, Move m, boolean ult, ServerPlayer p, ManasSkillInstance inst, double strength, double masteryFrac) {
        ServerLevel level = p.serverLevel();
        double r = m.radius() * NUSkill.milestoneRange(inst);
        double amount = m.power() * strength;
        Vec3 at = p.position();
        int amp = ult && g.cls() == com.newuniverse.nusmp.skill.rules.UltimateClass.GOD ? 2 : (ult ? 1 : 0);
        boolean done;
        switch (m.verb()) {
            case BLESS -> done = bless(p, at, r, strength, amp);
            case HEAL -> done = heal(p, at, r, amount);
            case SMITE -> done = smite(p, inst, at, r, amount, masteryFrac);
            case BEAM -> done = beam(p, inst, r, amount, masteryFrac, g.rgb());
            case WARD -> done = ward(p, at, r, strength, amp);
            case BIND -> done = bind(p, at, r, strength);
            case BLINK -> done = blink(p, inst, r, amount, masteryFrac);
            case DRAIN -> done = drain(p, inst, r, amount, masteryFrac);
            case PURGE -> done = purge(p, at, r, strength);
            case SURGE -> done = surge(p, strength, amp);
            default -> done = domain(p, at, r, strength, amp);
        }
        if (!done) return false;
        int argb = 0xFF000000 | g.rgb();
        SkillAnimeFx.sigil(level, g.sigil(), p.position(), Math.max(3.0, Math.min(14.0, r > 0 ? r : 4.0)), argb, 30);
        if (r > 0) SkillFx.ring(level, p.position(), Math.min(14.0, r), g.rgb());
        SkillFx.sound(level, p.position(), SoundEvents.BEACON_ACTIVATE, 0.8F, 1.0F + (g.rgb() & 7) * 0.05F);
        SkillUtil.actionbar(p, net.minecraft.network.chat.Component.literal(m.name()));
        return true;
    }

    // ------------------------------------------------------------------ verbs
    private static void give(LivingEntity e, Holder<MobEffect> effect, int ticks, int amp) { e.addEffect(new MobEffectInstance(effect, ticks, amp)); }

    private static boolean bless(ServerPlayer p, Vec3 at, double r, double strength, int amp) {
        int t = ticks(600, strength);
        int n = 0;
        for (LivingEntity ally : SkillTargets.alliesAround(p, at, r)) {
            give(ally, MobEffects.DAMAGE_BOOST, t, amp);
            give(ally, MobEffects.DAMAGE_RESISTANCE, t, 0);
            give(ally, MobEffects.REGENERATION, t / 2, 0);
            give(ally, MobEffects.ABSORPTION, t, amp);
            if (n++ < MAX_FX_ALLIES) SkillFx.spray(p.serverLevel(), false, ally.position().add(0, ally.getBbHeight() + 0.3, 0), 6, 0.3, 0.2, 0.3, 0.02);
        }
        return true;
    }

    private static boolean heal(ServerPlayer p, Vec3 at, double r, double amount) {
        int n = 0;
        for (LivingEntity ally : SkillTargets.alliesAround(p, at, r)) {
            BalanceLaw.heal(ally, (float) amount);
            if (n++ < MAX_FX_ALLIES) SkillFx.spray(p.serverLevel(), false, ally.position().add(0, ally.getBbHeight() + 0.3, 0), 6, 0.3, 0.2, 0.3, 0.02);
        }
        return true;
    }

    private static boolean smite(ServerPlayer p, ManasSkillInstance inst, Vec3 at, double r, double amount, double mf) {
        List<LivingEntity> foes = SkillTargets.foes(p, at, r);
        if (foes.isEmpty()) { SkillUtil.fail(p, SkillMsg.t("gods.no_foes", "No foe in reach.")); return false; }
        for (LivingEntity f : foes) SkillDamage.hit(p, f, amount, mf, SkillDamage.Kind.MAGIC, inst);
        return true;
    }

    private static boolean beam(ServerPlayer p, ManasSkillInstance inst, double range, double amount, double mf, int rgb) {
        Vec3 from = p.getEyePosition();
        Vec3 to = from.add(p.getViewVector(1.0F).scale(range));
        List<LivingEntity> foes = SkillTargets.foesAlong(p, from, to, 1.2);
        if (foes.isEmpty()) { SkillUtil.fail(p, SkillMsg.t("gods.no_foes", "No foe in reach.")); return false; }
        for (LivingEntity f : foes) SkillDamage.hit(p, f, amount, mf, SkillDamage.Kind.MAGIC, inst);
        SkillFx.beam(p.serverLevel(), from, to, false);
        SkillAnimeFx.heroBeam(p.serverLevel(), from, to, 3);
        return true;
    }

    private static boolean ward(ServerPlayer p, Vec3 at, double r, double strength, int amp) {
        int t = ticks(500, strength);
        for (LivingEntity ally : SkillTargets.alliesAround(p, at, r)) {
            give(ally, MobEffects.ABSORPTION, t, 1 + amp);
            give(ally, MobEffects.DAMAGE_RESISTANCE, t / 2, amp > 0 ? 1 : 0);
        }
        return true;
    }

    private static boolean bind(ServerPlayer p, Vec3 at, double r, double strength) {
        List<LivingEntity> foes = SkillTargets.foes(p, at, r);
        if (foes.isEmpty()) { SkillUtil.fail(p, SkillMsg.t("gods.no_foes", "No foe in reach.")); return false; }
        int t = ticks(160, strength);
        for (LivingEntity f : foes) {
            give(f, MobEffects.MOVEMENT_SLOWDOWN, t, 2);
            give(f, MobEffects.WEAKNESS, t, 0);
            give(f, MobEffects.GLOWING, t, 0);
        }
        return true;
    }

    private static boolean blink(ServerPlayer p, ManasSkillInstance inst, double range, double amount, double mf) {
        if (!com.newuniverse.nusmp.relics.Fx.teleportLook(p, range)) { SkillUtil.fail(p, SkillMsg.t("gods.no_room", "No room to move.")); return false; }
        for (LivingEntity f : SkillTargets.foes(p, p.position(), 4.0)) SkillDamage.hit(p, f, amount, mf, SkillDamage.Kind.ATTACK, inst);
        return true;
    }

    private static boolean drain(ServerPlayer p, ManasSkillInstance inst, double range, double amount, double mf) {
        LivingEntity t = SkillTargets.lookFoe(p, range);
        if (t == null) { SkillUtil.fail(p, SkillMsg.t("gods.no_target", "Look at a foe.")); return false; }
        float dealt = SkillDamage.hit(p, t, amount, mf, SkillDamage.Kind.MAGIC, inst);
        if (dealt > 0f) BalanceLaw.heal(p, dealt * 0.5f);
        SkillFx.beam(p.serverLevel(), t.getBoundingBox().getCenter(), p.getEyePosition(), true);
        return true;
    }

    private static boolean purge(ServerPlayer p, Vec3 at, double r, double strength) {
        int t = ticks(100, strength);
        for (LivingEntity ally : SkillTargets.alliesAround(p, at, r)) {
            SkillUtil.cleanse(ally);
            give(ally, MobEffects.DAMAGE_RESISTANCE, t, 1);
        }
        return true;
    }

    private static boolean surge(ServerPlayer p, double strength, int amp) {
        int t = ticks(400, strength);
        give(p, MobEffects.DAMAGE_BOOST, t, amp);
        give(p, MobEffects.MOVEMENT_SPEED, t, amp);
        return true;
    }

    private static boolean domain(ServerPlayer p, Vec3 at, double r, double strength, int amp) {
        List<LivingEntity> foes = SkillTargets.foes(p, at, r);
        int t = ticks(200, strength);
        for (LivingEntity f : foes) {
            give(f, MobEffects.MOVEMENT_SLOWDOWN, t, 2);
            give(f, MobEffects.WEAKNESS, t, 1);
        }
        for (LivingEntity ally : SkillTargets.alliesAround(p, at, r)) {
            give(ally, MobEffects.DAMAGE_BOOST, t, amp);
            give(ally, MobEffects.DAMAGE_RESISTANCE, t, 0);
        }
        return true;
    }

    // ------------------------------------------------------------------ passives (once a second)
    private static Holder<MobEffect> effectOf(FaithGodRules.Pas pas) {
        return switch (pas) {
            case REGEN -> MobEffects.REGENERATION;
            case RESIST -> MobEffects.DAMAGE_RESISTANCE;
            case STRENGTH -> MobEffects.DAMAGE_BOOST;
            case SPEED -> MobEffects.MOVEMENT_SPEED;
            case ABSORB -> MobEffects.ABSORPTION;
            case SIGHT -> MobEffects.NIGHT_VISION;
            case LEAP -> MobEffects.JUMP;
            case FIRE -> MobEffects.FIRE_RESISTANCE;
            case HASTE -> MobEffects.DIG_SPEED;
            case WATER -> MobEffects.WATER_BREATHING;
        };
    }

    /** Keeps the god's passives on the holder for the next few seconds (one potion effect each, no particles); the first passive is shared with the online worshippers within 32 blocks. */
    static void passives(God g, boolean ult, ManasSkillInstance inst, ServerPlayer p) {
        ManasSkillInstance pool = faithPool(p, inst);
        List<Worshipper> list = FaithWorship.worshippers(pool);
        int amp = FaithGodRules.passiveAmp(g, ult, list.size());
        List<Passive> pas = FaithGodRules.passives(g, ult);
        for (Passive pa : pas) refresh(p, effectOf(pa.effect()), pa.effect() == FaithGodRules.Pas.SIGHT ? 260 : 80, pa.effect() == FaithGodRules.Pas.SIGHT || pa.effect() == FaithGodRules.Pas.FIRE || pa.effect() == FaithGodRules.Pas.WATER ? 0 : amp);
        if (pas.isEmpty() || list.isEmpty() || p.getServer() == null) return;
        Holder<MobEffect> shared = effectOf(pas.get(0).effect());
        int shown = 0;
        for (Worshipper w : list) {
            if (shown >= FaithGodRules.MAX_SHARED) break;
            ServerPlayer o = p.getServer().getPlayerList().getPlayer(w.id());
            if (o == null || !o.isAlive() || o.level() != p.level() || o.distanceToSqr(p) > 1024.0) continue;
            refresh(o, shared, 80, Math.max(0, amp - 1));
            shown++;
        }
    }

    private static void refresh(LivingEntity e, Holder<MobEffect> effect, int ticks, int amp) { e.addEffect(new MobEffectInstance(effect, ticks, amp, true, false)); }
}
