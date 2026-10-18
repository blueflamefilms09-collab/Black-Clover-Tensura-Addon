package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.JoJoRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Gold Experience (Giorno Giovanna): give life. Modes: Give Life (heal the ally you look at, or yourself, through the balance law's
 * healing cap, with Regeneration; a mastered Gold Experience also washes harmful effects away) and Sensory Overload (a life-energy shot
 * at the foe you look at that hurts and leaves it reeling: nauseous, blind for a moment and slowed). Passive: your melee hits overload
 * the target's senses, leaving it slowed. Gold Experience Requiem is its Ultimate. Numbers: {@link JoJoRules}.
 */
public class GoldExperienceSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("gold_experience", "Gold Experience",
            "Unique. Give life to heal an ally, or overload a foe's senses with it; your blows leave enemies slowed.");

    public GoldExperienceSkill() { super(SkillType.UNIQUE, TEXT, false); }

    @Override public int getModes(ManasSkillInstance i) { return JoJoRules.modes(); }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean r) { return mode == JoJoRules.GIVE_LIFE ? JoJoRules.OVERLOAD : JoJoRules.GIVE_LIFE; }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return mode == JoJoRules.OVERLOAD ? "goldexp.overload" : "goldexp.life"; }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        return mode == JoJoRules.OVERLOAD ? SkillMsg.mode("goldexp.overload", "Sensory Overload") : SkillMsg.mode("goldexp.life", "Give Life");
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, SkillMsg.t("goldexp.cooldown", "Gold Experience is on cooldown.")); return; }
        Tier tier = tier(instance, player);
        ServerLevel level = player.serverLevel();

        if (mode == JoJoRules.GIVE_LIFE) {
            LivingEntity target = SkillTargets.lookAlly(player, JoJoRules.lifeRange());
            if (target == null) target = player;
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(JoJoRules.lifeCost()))) return;
            BalanceLaw.heal(target, (float) JoJoRules.lifeHeal(tier));
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, JoJoRules.lifeRegenTicks(), 1));
            if (tier != Tier.BASE) SkillUtil.cleanse(target);
            Vec3 at = target.position().add(0, 1, 0);
            SkillFx.spray(level, false, at, 12, 0.5, 0.8, 0.5, 0.0);
            SkillFx.shape(level, VfxShape.LIGHT_FLARE, at, at, 0xFFFFD040, 18, 1.0f);
            SkillFx.sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.6F);
            cooldown(instance, mode, DMUtil.cooldown(JoJoRules.lifeCooldown()));
        } else if (mode == JoJoRules.OVERLOAD) {
            LivingEntity target = SkillTargets.lookFoe(player, JoJoRules.overloadRange());
            if (target == null) { SkillUtil.fail(player, SkillMsg.t("goldexp.no_target", "No target.")); return; }
            if (!SkillUtil.spendMagicules(player, DMUtil.cost(JoJoRules.overloadCost()))) return;
            SkillDamage.hit(player, target, JoJoRules.overloadDamage(tier), masteryFrac(instance), SkillDamage.Kind.MAGIC);
            int ticks = BalanceLaw.controlTicks(target, JoJoRules.overloadTicks(tier));
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ticks, 0));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.min(ticks, 40), 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2));
            Vec3 at = target.position().add(0, target.getBbHeight() * 0.6, 0);
            SkillFx.beam(level, player.getEyePosition().add(0, -0.3, 0), at, false);
            SkillFx.spray(level, false, at, 10, 0.4, 0.5, 0.4, 0.05);
            SkillFx.sound(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.0F);
            cooldown(instance, mode, DMUtil.cooldown(JoJoRules.overloadCooldown()));
        } else {
            return;
        }
        SkillUtil.castVfx(player, 0xFFFFD040);
        grow(instance, player, 1);
    }

    /** Passive: a melee blow overloads the target's senses and slows it. */
    @Override
    public boolean onDamageEntity(ManasSkillInstance instance, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        if (source.getDirectEntity() == owner) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, JoJoRules.hitSlowTicks(), 2));
        return true;
    }
}
