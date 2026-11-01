package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.ControlRules;
import com.newuniverse.nusmp.skill.rules.GateRules;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Gate of Babylon (Fate), an Ultimate with ten modes (3.47.0, new; see {@link GateRules} for the numbers and the reasoning). A golden gate opens above what you look at and a weapon falls (Gate),
 * six of them fall on six creatures in front of you (Volley), a rain of them falls over a place (Rain); Enkidu's Chains of Heaven hold a creature (a boss for two fifths as long, a player for half
 * as long), the Golden Armour takes three tenths off every hit and gives Absorption, Vimana throws you forward and lets you glide, the King's Aura lifts those who follow you and weighs on your
 * foes, Gae Bolg is a spear through three, Ea (Enuma Elish) is one great line through everything in it, and the Elixir heals and cleanses.
 * All state is the game time each window ends at, kept in the skill tag of the one player who owns it: nothing ticks, nothing is cleaned up, nothing is shared. A press looks at no more than 24
 * creatures, strikes at most 12 (24 for Ea) and draws at most three gates and six beams.
 * Switch: server setting {@code gateEnabled}. Obtain: {@code /tensura edit <player> ability grant nusmp:gate_of_babylon}.
 */
public class GateOfBabylonSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("gate_of_babylon", "Gate of Babylon",
            "Ultimate. The King of Heroes' vault from Fate in ten modes: Gate, Volley, Rain, Chains of Heaven, Golden Armour, Vimana, King's Aura, Gae Bolg, Ea and Elixir.");

    /** The cast colour of each mode (ARGB). */
    private static final int[] COLORS = {0xFFFFD060, 0xFFFFC040, 0xFFFFE080, 0xFFC8A040, 0xFFFFB020, 0xFFFFF0A0, 0xFFFFD890, 0xFFE05030, 0xFFFF4020, 0xFFA0FFB0};
    /** At most this many gates and beams are drawn by one press, whatever it hits. */
    private static final int FX_GATES = 3, FX_BEAMS = 6;

    public GateOfBabylonSkill() { super(SkillType.ULTIMATE, TEXT, true); }

    @Override public int getModes(ManasSkillInstance i) { return GateRules.MODES; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) { return Math.floorMod(mode + (reverse ? -1 : 1), GateRules.MODES); }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return "gate." + GateRules.IDS[Math.floorMod(mode, GateRules.MODES)]; }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        return switch (Math.floorMod(mode, GateRules.MODES)) {
            case GateRules.GATE -> SkillMsg.mode("gate.gate", "Gate");
            case GateRules.VOLLEY -> SkillMsg.mode("gate.volley", "Volley");
            case GateRules.RAIN -> SkillMsg.mode("gate.rain", "Rain");
            case GateRules.CHAINS -> SkillMsg.mode("gate.chains", "Chains of Heaven");
            case GateRules.ARMOR -> SkillMsg.mode("gate.armor", "Golden Armour");
            case GateRules.VIMANA -> SkillMsg.mode("gate.vimana", "Vimana");
            case GateRules.KING -> SkillMsg.mode("gate.king", "King's Aura");
            case GateRules.GAE -> SkillMsg.mode("gate.gae", "Gae Bolg");
            case GateRules.EA -> SkillMsg.mode("gate.ea", "Ea");
            default -> SkillMsg.mode("gate.elixir", "Elixir");
        };
    }

    private static boolean open(CompoundTag tag, int mode, long now) { return GateRules.active(tag.getLong(GateRules.untilKey(mode)), now); }

    // ---------------------------------------------------------------- the press
    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer p) || !p.isAlive() || !GateRules.valid(mode)) return;
        if (!NUConfig.GATE_ENABLED.get()) { SkillUtil.fail(p, SkillMsg.t("gate.off", "The Gate of Babylon is switched off on this server.")); return; }
        CompoundTag tag = instance.getOrCreateTag();
        long now = p.level().getGameTime();
        if (GateRules.cancellable(mode) && open(tag, mode, now)) { end(instance, p, tag, mode); return; }   // a second press ends the window (its cooldown is already running)
        if (instance.onCoolDown(mode)) { SkillUtil.fail(p, SkillMsg.t("gate.cooldown", "The vault is still closing.")); return; }
        boolean done = switch (mode) {
            case GateRules.GATE -> gate(instance, p);
            case GateRules.VOLLEY -> volley(instance, p);
            case GateRules.RAIN -> rain(instance, p);
            case GateRules.CHAINS -> chains(p);
            case GateRules.ARMOR -> armor(p, tag, now);
            case GateRules.VIMANA -> vimana(p, tag, now);
            case GateRules.KING -> king(p);
            case GateRules.GAE -> gae(instance, p);
            case GateRules.EA -> ea(instance, p);
            default -> elixir(p);
        };
        if (!done) return;
        cooldown(instance, mode, DMUtil.cooldown(GateRules.cooldown(mode)));
        SkillFx.sound(p.serverLevel(), p.position(), SoundEvents.BEACON_ACTIVATE, 0.6F, 0.8F);
        SkillUtil.castVfx(p, COLORS[mode]);
        grow(instance, p, 1);
    }

    /** A golden gate in the air and a weapon falling from it to a creature (the gate is drawn above; the weapon is a beam). */
    private static void fall(ServerLevel level, LivingEntity target, boolean drawGate) {
        Vec3 hit = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 gate = target.position().add(0, target.getBbHeight() + 3.0, 0);
        if (drawGate) SkillFx.shape(level, com.newuniverse.nusmp.vfx.VfxShape.MAGIC_CIRCLE, gate, gate.add(0, -1, 0), 0xFFFFD060, 20, 0.7f);
        SkillFx.beam(level, gate, hit, false);
        SkillFx.burst(level, hit, false, 6, 0.3);
    }

    private boolean gate(ManasSkillInstance instance, ServerPlayer p) {
        LivingEntity t = SkillTargets.lookFoe(p, GateRules.gateReach());
        if (t == null) { SkillUtil.fail(p, SkillMsg.t("gate.no_target", "Nothing to strike.")); return false; }
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(GateRules.cost(GateRules.GATE)))) return false;
        fall(p.serverLevel(), t, true);
        SkillDamage.hit(p, t, GateRules.gateDamage(), masteryFrac(instance));
        return true;
    }

    private boolean volley(ManasSkillInstance instance, ServerPlayer p) {
        List<LivingEntity> foes = SkillTargets.foesInCone(p, GateRules.volleyRange(), GateRules.volleyCone());
        if (foes.isEmpty()) { SkillUtil.fail(p, SkillMsg.t("gate.no_target", "Nothing to strike.")); return false; }
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(GateRules.cost(GateRules.VOLLEY)))) return false;
        int struck = 0, drawn = 0;
        for (LivingEntity t : foes) {
            if (struck >= GateRules.volleyMax()) break;
            if (drawn < FX_BEAMS) { fall(p.serverLevel(), t, drawn < FX_GATES); drawn++; }   // at most six beams and three gates, whatever is refused
            if (SkillDamage.hit(p, t, GateRules.volleyDamage(), masteryFrac(instance)) > 0f) struck++;
        }
        return true;
    }

    private boolean rain(ManasSkillInstance instance, ServerPlayer p) {
        Vec3 centre = DMUtil.lookPoint(p, GateRules.rainReach());
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(GateRules.cost(GateRules.RAIN)))) return false;
        ServerLevel level = p.serverLevel();
        SkillFx.ring(level, centre.add(0, 0.2, 0), GateRules.rainRadius(), 0xFFD060);
        Vec3 sky = centre.add(0, 7.0, 0);
        SkillFx.shape(level, com.newuniverse.nusmp.vfx.VfxShape.MAGIC_CIRCLE, sky, sky.add(0, -1, 0), 0xFFFFD060, 24, 1.1f);
        int struck = 0, drawn = 0;
        for (LivingEntity t : SkillTargets.foes(p, centre, GateRules.rainRadius())) {
            if (struck >= GateRules.rainMax()) break;
            if (drawn < FX_BEAMS) { fall(level, t, false); drawn++; }
            if (SkillDamage.hit(p, t, GateRules.rainDamage(), masteryFrac(instance)) > 0f) struck++;
        }
        return true;
    }

    /** Enkidu: the creature you look at cannot move for five seconds (two for a boss, two and a half for a player) and is weakened for ten. */
    private boolean chains(ServerPlayer p) {
        LivingEntity t = SkillTargets.lookFoe(p, GateRules.chainsReach());
        if (t == null) { SkillUtil.fail(p, SkillMsg.t("gate.no_target", "Nothing to strike.")); return false; }
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(GateRules.cost(GateRules.CHAINS)))) return false;
        boolean boss = GodTier.isBoss(t), player = t instanceof Player;
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ControlRules.controlTicks(GateRules.chainsTicks(), boss, player), GateRules.chainsSlowAmp()));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ControlRules.controlTicks(GateRules.chainsWeakTicks(), boss, player), 0));
        SkillFx.beam(p.serverLevel(), p.getEyePosition(), t.position().add(0, t.getBbHeight() * 0.5, 0), false);
        SkillFx.ring(p.serverLevel(), t.position().add(0, 0.2, 0), 2.0, 0xC8A040);
        SkillUtil.actionbar(p, SkillMsg.t("gate.chains", "The Chains of Heaven bind it.").withStyle(ChatFormatting.GOLD));
        return true;
    }

    private boolean armor(ServerPlayer p, CompoundTag tag, long now) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(GateRules.cost(GateRules.ARMOR)))) return false;
        tag.putLong(GateRules.untilKey(GateRules.ARMOR), GateRules.endsAt(now, GateRules.ARMOR));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, GateRules.duration(GateRules.ARMOR), GateRules.armorAbsorptionAmp()));
        SkillFx.aura(p.serverLevel(), p, false);
        SkillUtil.actionbar(p, SkillMsg.t("gate.armor", "The Golden Armour closes around you.").withStyle(ChatFormatting.GOLD));
        return true;
    }

    private boolean vimana(ServerPlayer p, CompoundTag tag, long now) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(GateRules.cost(GateRules.VIMANA)))) return false;
        Vec3 look = p.getLookAngle();
        double s = GateRules.vimanaSpeed();
        p.setDeltaMovement(look.x * s, Math.max(0.0, look.y * s) + GateRules.vimanaLift(), look.z * s);
        p.hurtMarked = true;
        p.fallDistance = 0;
        tag.putLong(GateRules.untilKey(GateRules.VIMANA), GateRules.endsAt(now, GateRules.VIMANA));
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, GateRules.duration(GateRules.VIMANA), 0, false, false, true));
        SkillFx.burst(p.serverLevel(), p.position().add(0, 0.5, 0), false, 14, 0.6);
        SkillUtil.actionbar(p, SkillMsg.t("gate.vimana", "Vimana carries you.").withStyle(ChatFormatting.YELLOW));
        return true;
    }

    /** True for yourself and for everyone who is on your side: a teammate, a player you may not harm, your own creatures (never a player you could hurt). */
    private static boolean friendly(ServerPlayer p, LivingEntity e) {
        if (e == p || e.isAlliedTo(p)) return true;
        return e instanceof Player o ? !p.canHarmPlayer(o) : true;
    }

    private boolean king(ServerPlayer p) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(GateRules.cost(GateRules.KING)))) return false;
        int lifted = 0;
        for (LivingEntity a : SkillTargets.alliesAround(p, p.position(), GateRules.kingRadius())) {
            if (!friendly(p, a)) continue;
            a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, GateRules.kingBuffTicks(), 0));
            a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, GateRules.kingBuffTicks(), 0));
            lifted++;
        }
        for (LivingEntity f : SkillTargets.foes(p, p.position(), GateRules.kingRadius())) {
            f.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, GateRules.kingWeakTicks(), 0));
        }
        SkillFx.ring(p.serverLevel(), p.position().add(0, 0.2, 0), GateRules.kingRadius() * 0.8, 0xFFD060);
        SkillUtil.actionbar(p, SkillMsg.t("gate.king", "The King's Aura: %s lifted.", lifted).withStyle(ChatFormatting.GOLD));
        return true;
    }

    private boolean gae(ManasSkillInstance instance, ServerPlayer p) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(GateRules.cost(GateRules.GAE)))) return false;
        Vec3 from = p.getEyePosition(), to = DMUtil.lookPoint(p, GateRules.gaeRange());
        SkillFx.beam(p.serverLevel(), from, to, false);
        int hits = 0;
        for (LivingEntity t : SkillTargets.foesAlong(p, from, to, GateRules.gaeWidth())) {
            if (hits >= GateRules.gaePierce()) break;
            if (SkillDamage.hit(p, t, GateRules.gaeDamage(), masteryFrac(instance)) > 0f) hits++;
        }
        return true;
    }

    /** Ea: one great line (40 blocks, wide); everything in it that you may hurt (at most 24) is struck. */
    private boolean ea(ManasSkillInstance instance, ServerPlayer p) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(GateRules.cost(GateRules.EA)))) return false;
        ServerLevel level = p.serverLevel();
        Vec3 from = p.getEyePosition(), to = DMUtil.lookPoint(p, GateRules.eaRange());
        SkillFx.beam(level, from, to, false);
        SkillFx.shape(level, com.newuniverse.nusmp.vfx.VfxShape.MAGIC_CIRCLE_EXPLOSION, from, to, 0xFFFF4020, 24, 1.2f);
        SkillFx.sound(level, p.position(), SoundEvents.ANVIL_LAND, 0.8F, 0.5F);
        for (LivingEntity t : SkillTargets.foesAlong(p, from, to, GateRules.eaWidth())) {
            SkillDamage.hit(p, t, GateRules.eaDamage(), masteryFrac(instance), SkillDamage.Kind.MAGIC);
        }
        return true;
    }

    private boolean elixir(ServerPlayer p) {
        if (!SkillUtil.spendMagicules(p, DMUtil.cost(GateRules.cost(GateRules.ELIXIR)))) return false;
        BalanceLaw.heal(p, (float) (p.getMaxHealth() * GateRules.elixirHeal()));
        SkillUtil.cleanse(p);
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, GateRules.elixirRegenTicks(), 0));
        SkillFx.burst(p.serverLevel(), p.position().add(0, 1, 0), false, 16, 0.6);
        SkillUtil.actionbar(p, SkillMsg.t("gate.elixir", "The Elixir restores you.").withStyle(ChatFormatting.GREEN));
        return true;
    }

    /** A second press (or the owner's choice): the window closes, and what it gave goes with it. */
    private void end(ManasSkillInstance instance, ServerPlayer p, CompoundTag tag, int mode) {
        tag.putLong(GateRules.untilKey(mode), 0L);
        if (mode == GateRules.ARMOR) dropOwn(p, MobEffects.ABSORPTION, GateRules.armorAbsorptionAmp());
        if (mode == GateRules.VIMANA) dropOwn(p, MobEffects.SLOW_FALLING, 0);
        instance.markDirty();
        SkillUtil.actionbar(p, SkillMsg.t("gate.ended", "The vault closes.").withStyle(ChatFormatting.GRAY));
    }

    private static void dropOwn(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amp) {
        MobEffectInstance cur = p.getEffect(effect);
        if (cur != null && cur.getAmplifier() == amp) p.removeEffect(effect);
    }

    // ---------------------------------------------------------------- the damage hook
    /** The Golden Armour takes three tenths off a hit; what bypasses invulnerability (the void, kill) is never touched. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (amount.get() <= 0 || !NUConfig.GATE_ENABLED.get() || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        double m = GateRules.takenMultiplier(open(instance.getOrCreateTag(), GateRules.ARMOR, owner.level().getGameTime()));
        if (m != 1.0) amount.set((float) (amount.get() * m));
        return true;
    }
}
