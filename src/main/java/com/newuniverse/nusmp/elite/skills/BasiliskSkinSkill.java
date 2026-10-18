package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.elite.skills.rules.UltimateRules;
import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Basilisk Skin (Ultimate, designed for this mod from the Tensura Basilisk; the original addon's source was not available): the hide of the serpent king. PASSIVE: scales turn aside 35% of
 * ordinary damage (42% once Basilisk Skin is mastered), poison and wither cannot take hold, and a quarter of what a foe hits you with in melee (18%) is thrown back at it as poison damage with
 * a coat of venom. Mode 1, Petrifying Gaze: the foes in a narrow cone before you (14 blocks) are turned to stone: slowed to a crawl, weakened and fatigued for five seconds (two for players),
 * and struck for damage; the stoning is a hard control, so it obeys the shared 12-second control lock of the balance law (without it they are only slowed). Mode 2, Molt: shed the old skin: all
 * harmful effects cleansed and a shield of 40% of your health (50%) for twenty seconds. Numbers: {@link UltimateRules.Basilisk}. Basilisk Skin is the evolution of Crocodile Skin
 * (data/nusmp/elite/skills/evolutions.json).
 */
public class BasiliskSkinSkill extends EliteSkill {
    static final SkillText TEXT = SkillText.of("basilisk_skin", "Basilisk Skin",
            "Ultimate. Scales turn aside much ordinary damage, poison and wither cannot take hold, and melee hits are thrown back as venom. Petrifying Gaze turns the foes before you to stone; Molt sheds the skin for a shield.");

    private static boolean reflecting;

    public BasiliskSkinSkill() { super(SkillType.ULTIMATE, TEXT, true); }

    @Override
    protected List<EliteMode> modes() {
        return List.of(EliteMode.of("basilisk_skin.gaze", "Petrifying Gaze", UltimateRules.Basilisk.GAZE_COST, UltimateRules.Basilisk.GAZE_COOLDOWN),
                EliteMode.of("basilisk_skin.molt", "Molt", UltimateRules.Basilisk.MOLT_COST, UltimateRules.Basilisk.MOLT_COOLDOWN));
    }

    @Override protected int color() { return 0xFF4A7A3A; }

    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (!EliteSkillsConfig.enabled() || amount.get() <= 0) return true;
        boolean ownMastered = masteredBy(instance, owner);
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR) && !source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            double before = amount.get();
            amount.set((float) (before * (1.0 - UltimateRules.Basilisk.reduction(ownMastered))));
            // the venom: a share of a MELEE blow is thrown back (guarded so two basilisks cannot echo forever)
            if (!reflecting && source.getDirectEntity() instanceof LivingEntity attacker && attacker != owner && owner instanceof ServerPlayer p && EliteBridge.isAimable(p, attacker)) {
                reflecting = true;
                try {
                    attacker.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.cap(attacker, (float) (before * UltimateRules.Basilisk.reflect(ownMastered) * EliteSkillsConfig.damageScale())));
                    attacker.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1, true, false));
                } finally {
                    reflecting = false;
                }
            }
        }
        return true;
    }

    /** Once a second: poison and wither cannot take hold. */
    @Override
    public void upkeep(ManasSkillInstance instance, ServerPlayer player) {
        if (!EliteSkillsConfig.enabled()) return;
        if (player.hasEffect(MobEffects.POISON)) player.removeEffect(MobEffects.POISON);
        if (player.hasEffect(MobEffects.WITHER)) player.removeEffect(MobEffects.WITHER);
    }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, int mode) {
        boolean ownMastered = masteredBy(instance, player);
        ServerLevel level = player.serverLevel();
        if (mode == 1) {
            EliteBridge.cleanse(player);
            player.setAbsorptionAmount((float) Math.max(player.getAbsorptionAmount(), player.getMaxHealth() * UltimateRules.Basilisk.moltShield(ownMastered)));
            EliteBridge.aura(level, player, true);
            EliteBridge.shape(level, VfxShape.POISON_BREATH, player.position(), player.position().add(0, 1, 0), 0xFF4A7A3A, 30, 1.0f);
            EliteBridge.sound(level, player.position(), SoundEvents.SNIFFER_DIGGING_STOP, 1.0f, 0.6f);
            EliteBridge.actionbar(player, SkillMsg.t("basilisk_skin.molt", "You shed your old skin.").withStyle(ChatFormatting.DARK_GREEN));
            return true;
        }
        List<LivingEntity> seen = EliteBridge.foesInCone(player, UltimateRules.Basilisk.GAZE_RANGE, UltimateRules.Basilisk.GAZE_DOT);
        if (seen.isEmpty()) {
            EliteBridge.fail(player, SkillMsg.t("basilisk_skin.nobody", "Nothing stands in your gaze."));
            return false;
        }
        boolean lock = BalanceLaw.beginControl(player);
        double mastery = masteryFrac(instance);
        for (LivingEntity t : seen) {
            boolean human = t instanceof Player;
            int stone = UltimateRules.Basilisk.stoneTicks(human);
            if (lock) {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stone, 6, true, false));
                t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, stone, 3, true, false));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stone, 2, true, false));
            } else {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, true, false));
            }
            EliteBridge.hit(player, t, UltimateRules.Basilisk.GAZE_DAMAGE * EliteSkillsConfig.damageScale(), mastery, true);
            EliteBridge.spray(level, true, t.position().add(0, t.getBbHeight() * 0.6, 0), 8, 0.3, 0.4, 0.3, 0.02);
        }
        EliteBridge.shape(level, VfxShape.EYE_FX1, player.getEyePosition(), player.getEyePosition().add(player.getViewVector(1.0F).scale(UltimateRules.Basilisk.GAZE_RANGE)), 0xFF9AD84A, 24, 1.0f);
        EliteBridge.sound(level, player.position(), SoundEvents.ENDER_DRAGON_GROWL, 0.5f, 1.6f);
        EliteBridge.actionbar(player, SkillMsg.t("basilisk_skin.gaze", "Your gaze turns %s foes to stone.", seen.size()).withStyle(ChatFormatting.DARK_GREEN));
        return true;
    }
}
