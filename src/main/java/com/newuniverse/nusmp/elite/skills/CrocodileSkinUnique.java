package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.elite.skills.rules.UniqueRules;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Crocodile Skin (Unique, designed for this mod; the original addon's source was not available): the hide and the bite of an ambush hunter. PASSIVE: a fifth of all ordinary damage (not magic, not
 * fall or void) bounces off the hide, 30% once mastered, and fifteen points more while you stand in water, where your melee also hits a quarter harder. PRESS: Death Roll grips the foe you
 * look at (up to six blocks) and rolls it three times, half a second apart: each roll bites for damage and returns a quarter of it to you as health, and the grip itself (slow and weak for two
 * seconds) is a hard control, so it obeys the shared 12-second control lock of the balance law. A mastered Crocodile Skin can become Basilisk Skin (data/nusmp/elite/skills/evolutions.json).
 * Numbers: {@link UniqueRules.Crocodile}.
 */
public class CrocodileSkinUnique extends EliteSkill {
    static final SkillText TEXT = SkillText.of("crocodile_skin", "Crocodile Skin",
            "Unique. A hide that turns aside a fifth of ordinary damage (more in water, where you also hit harder). Death Roll grips the foe you look at and rolls it three times, healing you.");

    public CrocodileSkinUnique() { super(SkillType.UNIQUE, TEXT, false); }

    @Override
    protected List<EliteMode> modes() {
        return List.of(EliteMode.of("crocodile_skin.roll", "Death Roll", UniqueRules.Crocodile.ROLL_COST, UniqueRules.Crocodile.ROLL_COOLDOWN));
    }

    @Override protected int color() { return 0xFF5E8A3A; }

    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (!EliteSkillsConfig.enabled() || amount.get() <= 0 || source.is(DamageTypeTags.BYPASSES_ARMOR) || source.is(DamageTypeTags.WITCH_RESISTANT_TO)) return true;
        double cut = UniqueRules.Crocodile.reduction(masteredBy(instance, owner), owner.isInWater());
        amount.set((float) (amount.get() * (1.0 - cut)));
        return true;
    }

    @Override
    public boolean onDamageEntity(ManasSkillInstance instance, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        if (!EliteSkillsConfig.enabled() || source.getDirectEntity() != owner || !owner.isInWater()) return true;
        amount.set((float) (amount.get() * UniqueRules.Crocodile.waterDamage(true, masteredBy(instance, owner))));
        return true;
    }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, int mode) {
        LivingEntity target = EliteBridge.lookFoe(player, UniqueRules.Crocodile.ROLL_RANGE);
        if (target == null) {
            EliteBridge.fail(player, SkillMsg.t("crocodile_skin.no_target", "No foe within reach."));
            return false;
        }
        boolean mastered = masteredBy(instance, player);
        double mastery = masteryFrac(instance);
        ServerLevel level = player.serverLevel();
        UUID id = target.getUUID();
        boolean grip = BalanceLaw.beginControl(player);
        if (grip) {
            int bind = UniqueRules.Crocodile.bindTicks(mastered);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, bind, 4, true, false));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, bind, 1, true, false));
        }
        EliteBridge.actionbar(player, SkillMsg.t("crocodile_skin.roll", "Death Roll!").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
        EliteBridge.startZone(player, UniqueRules.Crocodile.ROLL_INTERVAL, UniqueRules.Crocodile.ROLL_HITS, (owner, lvl, index) -> {
            if (!(lvl.getEntity(id) instanceof LivingEntity t) || !t.isAlive() || t.distanceTo(owner) > UniqueRules.Crocodile.ROLL_RANGE + 3.0) return;
            float dealt = EliteBridge.hit(owner, t, UniqueRules.Crocodile.ROLL_DAMAGE * EliteSkillsConfig.damageScale(), mastery, false);
            if (dealt > 0f) BalanceLaw.heal(owner, (float) (dealt * UniqueRules.Crocodile.ROLL_HEAL));
            if (grip) {
                Vec3 toward = owner.position().subtract(t.position());
                if (toward.lengthSqr() > 2.25) {
                    t.setDeltaMovement(t.getDeltaMovement().scale(0.4).add(toward.normalize().scale(0.35)));
                    t.hurtMarked = true;
                }
            }
            EliteBridge.shape(lvl, VfxShape.WATER_SPLASH, t.position(), t.position().add(0, 1, 0), 0xFF5E8A3A, 10, 0.8f);
            EliteBridge.sound(lvl, t.position(), SoundEvents.PLAYER_ATTACK_STRONG, 0.9f, 0.7f);
        });
        return true;
    }
}
