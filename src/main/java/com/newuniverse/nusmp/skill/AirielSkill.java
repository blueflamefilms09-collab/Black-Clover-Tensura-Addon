package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.skill.rules.WindRules;
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

/**
 * Airiel / Tempest (Ais Wallenstein): wind enchantment. Modes: Tempest (a timed wind buff: speed, jump, slow fall, strength, and the
 * wind turns aside half of every arrow or thrown weapon that would hit you) and Wind Dash (a burst forward; while the Tempest blows the
 * dash recovers twice as fast). Mastery lengthens the Tempest, strengthens it and adds power to the dash. Numbers: {@link WindRules}.
 */
public class AirielSkill extends NUSkill {
    private static final String TEMPEST_UNTIL = "TempestUntil";

    static final SkillText TEXT = SkillText.of("airiel", "Airiel",
            "Unique. Tempest: wrap yourself in wind for speed, power and protection from arrows, or ride it in a sudden dash that recovers faster while the wind blows.");

    public AirielSkill() { super(SkillType.UNIQUE, TEXT, false); }

    @Override public int getModes(ManasSkillInstance instance) { return 2; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean reverse) { return mode == WindRules.TEMPEST ? WindRules.DASH : WindRules.TEMPEST; }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return mode == WindRules.TEMPEST ? "airiel.tempest" : "airiel.dash"; }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        return mode == WindRules.TEMPEST ? SkillMsg.mode("airiel.tempest", "Tempest") : SkillMsg.mode("airiel.dash", "Wind Dash");
    }

    private static boolean tempestOn(ManasSkillInstance instance, LivingEntity who) {
        return WindRules.tempestActive(who.level().getGameTime(), instance.getOrCreateTag().getLong(TEMPEST_UNTIL));
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, SkillMsg.t("airiel.cooldown", "Airiel is on cooldown.")); return; }
        ServerLevel level = player.serverLevel();
        Tier tier = tier(instance, player);

        if (mode == WindRules.TEMPEST) {
            if (!SkillUtil.spendMagicules(player, NUConfig.AIRIEL_MAGICULE_COST.get())) return;
            int ticks = WindRules.tempestTicks(tier);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, WindRules.speedAmp(tier)));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 1));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, WindRules.strengthAmp(tier)));
            instance.getOrCreateTag().putLong(TEMPEST_UNTIL, level.getGameTime() + ticks);
            SkillFx.follow(level, VfxShape.WIND_ZEPHYR, player, player.position(), 0xFF8CFFC2, 60, 1.0f);
            SkillFx.shape(level, VfxShape.WIND_RING, player.position(), player.position().add(0, 1, 0), 0xFF8CFFC2, 24, 1.0f);
            SkillFx.sound(level, player.position(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.0F, 1.0F);
            cooldown(instance, WindRules.TEMPEST, NUConfig.AIRIEL_COOLDOWN.get());
        } else {
            if (!SkillUtil.spendMagicules(player, WindRules.dashCost(NUConfig.AIRIEL_MAGICULE_COST.get()))) return;
            Vec3 look = player.getViewVector(1.0F);
            double power = WindRules.dashPower(tier);
            player.setDeltaMovement(look.x * power, Math.max(0.3, look.y * power * 0.6), look.z * power);
            player.hurtMarked = true;
            player.fallDistance = 0;
            Vec3 from = player.position().add(0, 0.8, 0);
            SkillFx.shape(level, VfxShape.WIND_GALE, from, from.add(look.scale(6.0)), 0xFF8CFFC2, 14, 1.0f);
            SkillFx.spray(level, false, player.position().add(0, 0.5, 0), 10, 0.3, 0.2, 0.3, 0.05);
            SkillFx.sound(level, player.position(), SoundEvents.BREEZE_JUMP, 1.0F, 1.2F);
            cooldown(instance, WindRules.DASH, WindRules.dashCooldown(tempestOn(instance, player)));
        }
        SkillUtil.castVfx(player, 0xFF8CFFC2);
        grow(instance, player, 1);
    }

    /** While the Tempest blows, the wind turns aside part of every arrow and thrown weapon. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (amount.get() <= 0 || !source.is(DamageTypeTags.IS_PROJECTILE) || !tempestOn(instance, owner)) return true;
        amount.set((float) (amount.get() * (1.0 - WindRules.projectileShield(tier(instance, owner)))));
        if (owner.level() instanceof ServerLevel level) SkillFx.spray(level, false, owner.position().add(0, 1, 0), 5, 0.4, 0.5, 0.4, 0.03);
        return true;
    }
}
