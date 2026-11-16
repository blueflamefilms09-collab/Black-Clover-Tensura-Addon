package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.AuraRules;
import com.newuniverse.nusmp.skill.rules.FireForceRules;
import com.newuniverse.nusmp.skill.rules.ShinraRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Shinra Kusakabe (Fire Force), id {@code hell_walker} (docs/shinra_rework.md). The skill is the main character: a wheel of actives and one ON/OFF switch for the passives.
 * <ul>
 *   <li>Wheel: Ignition Dash and Devil's Kick (the old Devil's Footprints, folded in: {@link DevilsFootprintsSkill#dash}/{@link DevilsFootprintsSkill#kick}) and the Rapid-Man Kick (a short ignition dash
 *       that kicks and ignites everything on its line). Its Ultimate, {@link AdollaBurstSkill}, swaps the dash for Light Speed and adds the Adolla Burst blast.</li>
 *   <li>Always on: your own flames cannot burn you (fire damage is cancelled), as Devil's Footprints had it.</li>
 *   <li>Switch on (Hell Mode, 60 magicules a second, the old Hell Walker): the lower your health the harder you hit; under a third of health the Devil's Grin (Speed I); a mastered
 *       skill under a quarter of health is in Adolla Link (Speed II, Strength I, moves recharge 40 % faster).</li>
 * </ul>
 * Hell Conqueror (the old Ultimate of this skill) extends this class and keeps its own hellfire aura. The SERVER setting {@code shinraRework=false} restores the old behaviour: no wheel, no fire
 * immunity, the plain toggle. Numbers: {@link AuraRules} (the damage curve), {@link FireForceRules} (dash, kick, burst), {@link ShinraRules}. No state is kept per player outside the skill instance,
 * so nothing has to be cleared on logout.
 */
public class HellWalkerSkill extends ToggleAuraSkill {
    static final SkillText TEXT = SkillText.of("hell_walker", "Shinra Kusakabe",
            "Unique. Shinra Kusakabe of Fire Force, the Devil's Footprints. Wheel: Ignition Dash, Devil's Kick and the Rapid-Man Kick. Your own flames cannot burn you. Toggle Hell Mode: the closer you are to death the harder you hit, with the Devil's Grin under a third of health and Adolla Link when mastered.");

    public HellWalkerSkill() { this(SkillType.UNIQUE, TEXT, false); }

    protected HellWalkerSkill(SkillType type, SkillText text, boolean ultimate) { super(type, text, ultimate); }

    /** True for Adolla Burst: Light Speed, Devil's Kick, Adolla Burst, Rapid-Man Kick. Everything else (this skill, Hell Conqueror) has the three-move wheel. */
    protected boolean fullWheel() { return false; }

    /** True when the Shinra wheel and fire immunity are on: the SERVER setting {@code shinraRework}. Adolla Burst overrides it to stay on (its wheel is the old one with the setting off). */
    protected boolean wheelOn() { return NUConfig.shinraRework(); }

    @Override protected int interval() { return AuraRules.HELL_INTERVAL; }
    @Override protected double drainPerPulse(Tier tier) { return AuraRules.HELL_DRAIN; }

    // ------------------------------------------------------------------ the wheel
    @Override public int getModes(ManasSkillInstance i) { return wheelOn() ? ShinraRules.wheel(fullWheel()).length : super.getModes(i); }

    @Override
    public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) {
        if (!wheelOn()) return super.nextMode(e, i, mode, reverse);
        int count = getModes(i);
        return Math.floorMod(mode + (reverse ? -1 : 1), count);
    }

    @Override
    public String getModeId(ManasSkillInstance i, int mode) {
        if (!wheelOn()) return super.getModeId(i, mode);
        return switch (ShinraRules.move(fullWheel(), mode)) {
            case ShinraRules.DASH -> fullWheel() ? "fireforce.light_speed" : "fireforce.dash";
            case ShinraRules.KICK -> "fireforce.kick";
            case ShinraRules.BURST -> "fireforce.burst";
            default -> "shinra.rapid";
        };
    }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        if (!wheelOn()) return super.getModeName(i, mode);
        return switch (ShinraRules.move(fullWheel(), mode)) {
            case ShinraRules.DASH -> fullWheel() ? SkillMsg.mode("fireforce.light_speed", "Light Speed") : SkillMsg.mode("fireforce.dash", "Ignition Dash");
            case ShinraRules.KICK -> SkillMsg.mode("fireforce.kick", "Devil's Kick");
            case ShinraRules.BURST -> SkillMsg.mode("fireforce.burst", "Adolla Burst");
            default -> SkillMsg.mode("shinra.rapid", "Rapid-Man Kick");
        };
    }

    /** The cooldown of a move, shortened while Adolla Link burns (only with Hell Mode on). */
    protected final void moveCooldown(ManasSkillInstance instance, ServerPlayer player, int mode, int baseTicks) {
        Tier tier = tier(instance, player);
        double f = instance.isToggled() ? ShinraRules.cooldownFactor(player.getHealth(), player.getMaxHealth(), tier) : 1.0;
        cooldown(instance, mode, (int) Math.round(DMUtil.cooldown(baseTicks) * f));
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!wheelOn() || !(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        int move = ShinraRules.move(fullWheel(), mode);
        int slot = Math.floorMod(mode, ShinraRules.wheel(fullWheel()).length);
        if (instance.onCoolDown(slot)) { SkillUtil.fail(player, SkillMsg.t("fireforce.cooldown", "Your flames need a moment.")); return; }
        Tier tier = tier(instance, player);

        if (move == ShinraRules.DASH) {
            if (!pay(instance, player, slot, DMUtil.cost(FireForceRules.dashCost(tier)))) return;
            DevilsFootprintsSkill.dash(player, tier);
            moveCooldown(instance, player, slot, FireForceRules.dashCooldown(tier));
        } else if (move == ShinraRules.KICK) {
            if (!pay(instance, player, slot, DMUtil.cost(FireForceRules.kickCost(tier)))) return;
            DevilsFootprintsSkill.kick(instance, player, tier);
            moveCooldown(instance, player, slot, FireForceRules.kickCooldown(tier));
        } else if (move == ShinraRules.RAPID) {
            if (!pay(instance, player, slot, DMUtil.cost(ShinraRules.rapidCost(tier)))) return;
            rapid(instance, player, tier);
            moveCooldown(instance, player, slot, ShinraRules.rapidCooldown(tier));
        } else {
            return;
        }
        SkillUtil.castVfx(player, 0xFFFF5A1E);
        grow(instance, player, 1);
    }

    /** The Rapid-Man Kick: rocket forward along your sight line, kicking and igniting every foe on the way (at most {@link ShinraRules#maxTargets()}, one query). */
    protected static void rapid(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 a = player.position().add(0, 1, 0);
        Vec3 b = a.add(look.scale(ShinraRules.rapidRange(tier)));
        int hit = 0;
        for (LivingEntity t : SkillTargets.foesAlong(player, a, b, ShinraRules.rapidWidth())) {
            if (hit >= ShinraRules.maxTargets()) break;
            if (SkillDamage.hit(player, t, ShinraRules.rapidDamage(tier), masteryFrac(instance), instance) > 0f) {
                t.igniteForSeconds(ShinraRules.rapidBurnSeconds(tier));
                t.knockback(0.8, -look.x, -look.z);
                hit++;
            }
        }
        double p = ShinraRules.rapidPower(tier);
        player.setDeltaMovement(look.x * p, Math.max(0.2, look.y * p), look.z * p);
        player.hurtMarked = true;
        player.fallDistance = 0;
        SkillFx.shape(level, VfxShape.FLAME_TRAIL, a, a.add(look.scale(-6.0)), 0xFFFF5A1E, 16, 1.0f);
        SkillFx.spray(level, false, player.position().add(0, 0.2, 0), 10, 0.2, 0.1, 0.2, 0.08);
        SkillFx.sound(level, player.position(), SoundEvents.FIRECHARGE_USE, 1.0F, 1.2F);
        SkillAnimeFx2.devilKick(player, ShinraRules.rapidRange(tier));
    }

    /** Shinra's own flames do not burn him (the old Devil's Footprints passive); the setting off keeps the old Hell Walker, which did not have it. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (wheelOn() && FireForceRules.fireImmune(true) && source.is(DamageTypeTags.IS_FIRE)) amount.set(0.0F);
        return true;
    }

    // ------------------------------------------------------------------ Hell Mode (the toggle)
    @Override
    protected void pulse(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        double hp = player.getHealth(), max = player.getMaxHealth();
        if (hp < max * 0.5F) {
            SkillFx.spray(player.serverLevel(), true, player.position().add(0, 0.2, 0), 6, 0.3, 0.1, 0.3, 0.01);
        }
        if (!NUConfig.shinraRework()) return;
        int speed = ShinraRules.speedAmp(hp, max, tier);          // the Devil's Grin / Adolla Link
        if (speed >= 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ShinraRules.EFFECT_TICKS, speed, true, false));
            SkillFx.spray(player.serverLevel(), false, player.position().add(0, 0.1, 0), 4, 0.3, 0.05, 0.3, 0.05);
        }
        int strength = ShinraRules.strengthAmp(hp, max, tier);
        if (strength >= 0) player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ShinraRules.EFFECT_TICKS, strength, true, false));
    }

    @Override
    protected void turnedOn(ManasSkillInstance instance, ServerPlayer player) {
        SkillFx.sound(player.serverLevel(), player.position(), SoundEvents.WITHER_AMBIENT, 0.6F, 0.6F);
        SkillFx.spray(player.serverLevel(), true, player.position().add(0, 1, 0), 12, 0.4, 0.6, 0.4, 0.02);
        SkillAnimeFx.sigil(player.serverLevel(), SkillAnimeFx.HELL, player.position(), 2.5, 0xFF8A0A0A, 30);      // VFX wave 3: the ground cracks into hell
    }

    @Override
    public boolean onDamageEntity(ManasSkillInstance instance, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        if (!instance.isToggled() || owner.getMaxHealth() <= 0 || amount.get() <= 0) return true;
        Tier tier = tier(instance, owner);
        float dealt = (float) (amount.get() * AuraRules.hellMultiplier(owner.getHealth(), owner.getMaxHealth(), tier));
        amount.set(dealt);
        double steal = AuraRules.hellLifesteal(owner.getHealth(), owner.getMaxHealth(), tier);
        if (steal > 0) BalanceLaw.heal(owner, (float) (dealt * steal));
        return true;
    }

    /** Adolla Burst uses it: the hellfire aura and the berserk of the old Hell Conqueror, now part of its Adolla Link. At most one target query a second. */
    protected static void hellfire(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        double hp = player.getHealth(), max = player.getMaxHealth();
        if (AuraRules.hellfire(hp, max, tier)) {
            List<LivingEntity> foes = SkillTargets.foes(player, player.position(), AuraRules.hellfireRadius());
            for (LivingEntity foe : foes) {
                SkillDamage.hit(player, foe, AuraRules.hellfireDamage(), masteryFrac(instance), SkillDamage.Kind.MAGIC, instance);
                foe.igniteForSeconds(3);
            }
            Vec3 at = player.position();
            SkillFx.shape(player.serverLevel(), VfxShape.FIRE_WILD, at, at.add(0, 1, 0), 0xFFFF2A1E, 20, (float) AuraRules.hellfireRadius());
        }
        if (AuraRules.berserk(hp, max, tier)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1, true, false));
        }
    }
}
