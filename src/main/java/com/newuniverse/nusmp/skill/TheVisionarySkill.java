package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.skill.rules.GodTierRules;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The Visionary (Gremmy Thoumeaux): a standalone god-tier Ultimate. Whatever you imagine becomes real. Meteor: imagine a meteor onto the
 * spot you look at (damages and ignites foes, never breaks or burns a block). Void: imagine a void that lifts, withers, blinds and
 * darkens every foe in an area. Perfect Body: imagine your own body stronger for a minute. Only foes are hurt. Numbers:
 * {@link GodTierRules}.
 */
public class TheVisionarySkill extends NUSkill {
    private static final String[] KEYS = {"meteor", "void", "body"};

    static final SkillText TEXT = SkillText.of("the_visionary", "The Visionary",
            "Ultimate. Whatever you imagine becomes reality: a meteor, a void, a perfect body.");

    public TheVisionarySkill() { super(SkillType.ULTIMATE, TEXT, true); }

    @Override public int getModes(ManasSkillInstance i) { return GodTierRules.MODES; }
    @Override public int nextMode(LivingEntity e, ManasSkillInstance i, int mode, boolean r) { return r ? (mode + GodTierRules.MODES - 1) % GodTierRules.MODES : (mode + 1) % GodTierRules.MODES; }
    @Override public String getModeId(ManasSkillInstance i, int mode) { return "visionary." + KEYS[Math.floorMod(mode, KEYS.length)]; }

    @Override
    public Component getModeName(ManasSkillInstance i, int mode) {
        return switch (mode) {
            case GodTierRules.METEOR -> SkillMsg.mode("visionary.meteor", "Meteor");
            case GodTierRules.VOID -> SkillMsg.mode("visionary.void", "Void");
            default -> SkillMsg.mode("visionary.body", "Perfect Body");
        };
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        if (instance.onCoolDown(mode)) { SkillUtil.fail(player, SkillMsg.t("visionary.cooldown", "Your imagination needs rest.")); return; }
        ServerLevel level = player.serverLevel();
        boolean mastered = instance.isMastered(player);
        double mastery = masteryFrac(instance);

        switch (mode) {
            case GodTierRules.METEOR -> {
                if (!SkillUtil.spendMagicules(player, DMUtil.cost(GodTierRules.meteorCost()))) return;
                Vec3 c = DMUtil.lookPoint(player, 64);
                Vec3 sky = c.add(8, 40, 0);
                SkillFx.beam(level, sky, c, false);
                SkillFx.shape(level, VfxShape.DEMON_METEOR, sky, c, 0xFFFFA0E0, 24, 2.0f);
                SkillFx.shape(level, VfxShape.FIRE_BURST, c, c.add(0, 1, 0), 0xFFFFA0E0, 24, (float) GodTierRules.meteorRadius(mastered) * 0.4f);
                for (LivingEntity t : SkillTargets.foes(player, c, GodTierRules.meteorRadius(mastered))) {
                    if (SkillDamage.hit(player, t, GodTierRules.meteorDamage(mastered), mastery, SkillDamage.Kind.MAGIC) > 0f) t.igniteForSeconds(6);
                }
                SkillFx.sound(level, c, SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.7F);
                cooldown(instance, mode, DMUtil.cooldown(GodTierRules.meteorCooldown()));
            }
            case GodTierRules.VOID -> {
                if (!SkillUtil.spendMagicules(player, DMUtil.cost(GodTierRules.voidCost()))) return;
                Vec3 c = DMUtil.lookPoint(player, 40);
                for (LivingEntity t : SkillTargets.foes(player, c, GodTierRules.voidRadius(mastered))) {
                    t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, BalanceLaw.controlTicks(t, 60), 2));
                    t.addEffect(new MobEffectInstance(MobEffects.WITHER, GodTierRules.voidWitherTicks(), 2));
                    t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0));
                    t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                }
                SkillFx.shape(level, VfxShape.NIHILITY_ZONE, c, c.add(0, 1, 0), 0xFF7A3FB8, 60, (float) GodTierRules.voidRadius(mastered));
                SkillFx.spray(level, true, c.add(0, 1, 0), 12, 3, 2, 3, 0.02);
                SkillFx.sound(level, c, SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.0F, 0.5F);
                cooldown(instance, mode, DMUtil.cooldown(GodTierRules.voidCooldown()));
            }
            case GodTierRules.BODY -> {
                if (!SkillUtil.spendMagicules(player, DMUtil.cost(GodTierRules.bodyCost()))) return;
                int ticks = GodTierRules.bodyTicks(mastered);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 2));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 2));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 1));
                SkillFx.aura(level, player, false);
                SkillFx.spray(level, false, player.position().add(0, 1, 0), 12, 0.5, 1, 0.5, 0.3);
                SkillFx.sound(level, player.position(), SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.2F);
                cooldown(instance, mode, DMUtil.cooldown(GodTierRules.bodyCooldown()));
            }
            default -> { return; }
        }
        SkillUtil.castVfx(player, 0xFFFFA0E0);
        grow(instance, player, 1);
    }
}
