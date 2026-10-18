package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.elite.skills.rules.UltimateRules;
import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Rapheal (Ultimate, id {@code rapheal}, spelled as in the owner's inventory; designed for this mod from the Tensura Lord of Wisdom; the original addon's source was not available): the mind that
 * has already calculated this. PASSIVE: every blow that would hit you has an 18% chance (25% mastered, +15% while accelerated) of having been foreseen and turned aside completely, at most once
 * a second, never against the void or /kill. Mode 1, Analyze: read out the foe you look at (health, armour, damage, whether it flies or is a boss) and mark it: every blow of yours on it
 * lands 15% harder for twenty seconds. Mode 2, Thought Acceleration: for ten seconds you are fast, your swings quicken and every second a quarter of the remaining cooldown of all your other
 * skills runs off. Mode 3, Dissect: strip every beneficial effect from the foe you look at and leave it weakened and slowed. Numbers: {@link UltimateRules.Rapheal}. Rapheal is the evolution of
 * Meditation in data/nusmp/elite/skills/evolutions.json.
 */
public class RaphealSkill extends EliteSkill {
    static final SkillText TEXT = SkillText.of("rapheal", "Rapheal",
            "Ultimate. Wisdom: some blows are foreseen and turned aside. Analyze reads and marks a foe, Thought Acceleration quickens you and your cooldowns, Dissect strips a foe's blessings.");

    static final String MARK = "MarkUuid", MARK_UNTIL = "MarkUntil", ACCEL_UNTIL = "AccelUntil", LAST_PREDICT = "LastPredict";

    public RaphealSkill() { super(SkillType.ULTIMATE, TEXT, true); }

    @Override
    protected List<EliteMode> modes() {
        return List.of(EliteMode.of("rapheal.analyze", "Analyze", UltimateRules.Rapheal.ANALYZE_COST, UltimateRules.Rapheal.ANALYZE_COOLDOWN),
                EliteMode.of("rapheal.accel", "Thought Acceleration", UltimateRules.Rapheal.ACCEL_COST, UltimateRules.Rapheal.ACCEL_COOLDOWN),
                EliteMode.of("rapheal.dissect", "Dissect", UltimateRules.Rapheal.DISSECT_COST, UltimateRules.Rapheal.DISSECT_COOLDOWN));
    }

    @Override protected int color() { return 0xFF7AD8FF; }

    static boolean accelerated(ManasSkillInstance instance, LivingEntity e) { return e.level().getGameTime() < instance.getOrCreateTag().getLong(ACCEL_UNTIL); }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, int mode) {
        ServerLevel level = player.serverLevel();
        var tag = instance.getOrCreateTag();
        switch (mode) {
            case 0 -> {
                LivingEntity t = EliteBridge.lookFoe(player, UltimateRules.Rapheal.ANALYZE_RANGE);
                if (t == null) {
                    EliteBridge.fail(player, SkillMsg.t("rapheal.no_target", "No foe in your sights."));
                    return false;
                }
                tag.putUUID(MARK, t.getUUID());
                tag.putLong(MARK_UNTIL, level.getGameTime() + UltimateRules.Rapheal.MARK_TICKS);
                instance.markDirty();
                var dmg = t.getAttribute(Attributes.ATTACK_DAMAGE);
                EliteBridge.actionbar(player, SkillMsg.t("rapheal.read", "Analysis: %s, %s of %s health, armour %s, hits for %s%s.", t.getDisplayName(), (int) Math.ceil(t.getHealth()),
                        (int) Math.ceil(t.getMaxHealth()), t.getArmorValue(), dmg == null ? 0 : (int) Math.round(dmg.getValue()),
                        com.newuniverse.nusmp.balance.BalanceLaw.isBoss(t) ? ", a boss" : "").withStyle(ChatFormatting.AQUA));
                t.addEffect(new MobEffectInstance(MobEffects.GLOWING, UltimateRules.Rapheal.MARK_TICKS, 0, true, false));
                EliteBridge.shape(level, VfxShape.EYE_FX1, player.getEyePosition(), t.getBoundingBox().getCenter(), 0xFF7AD8FF, 20, 0.9f);
                EliteBridge.sound(level, player.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 1.6f);
                return true;
            }
            case 1 -> {
                tag.putLong(ACCEL_UNTIL, level.getGameTime() + UltimateRules.Rapheal.ACCEL_TICKS);
                instance.markDirty();
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, UltimateRules.Rapheal.ACCEL_TICKS, 1, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, UltimateRules.Rapheal.ACCEL_TICKS, 1, true, false));
                EliteBridge.follow(level, VfxShape.MANA_CHARGE, player, player.position().add(0, 1, 0), 0xFF7AD8FF, UltimateRules.Rapheal.ACCEL_TICKS, 1.1f);
                EliteBridge.sound(level, player.position(), SoundEvents.BEACON_POWER_SELECT, 1.0f, 1.8f);
                EliteBridge.actionbar(player, SkillMsg.t("rapheal.accel", "Your thoughts accelerate.").withStyle(ChatFormatting.AQUA));
                return true;
            }
            default -> {
                LivingEntity t = EliteBridge.lookFoe(player, UltimateRules.Rapheal.DISSECT_RANGE);
                if (t == null) {
                    EliteBridge.fail(player, SkillMsg.t("rapheal.no_target", "No foe in your sights."));
                    return false;
                }
                int stripped = 0;
                for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) {
                    if (e.getEffect().value().isBeneficial() && t.removeEffect(e.getEffect())) stripped++;
                }
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, true, false));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0, true, false));
                EliteBridge.shape(level, VfxShape.MIRROR_SHATTER, t.getBoundingBox().getCenter(), t.getBoundingBox().getCenter().add(0, 1, 0), 0xFF7AD8FF, 16, 0.8f);
                EliteBridge.sound(level, t.position(), SoundEvents.GLASS_BREAK, 1.0f, 1.4f);
                EliteBridge.actionbar(player, SkillMsg.t("rapheal.dissected", "Dissected: %s blessings stripped.", stripped).withStyle(ChatFormatting.AQUA));
                return true;
            }
        }
    }

    /** Passive: a foreseen blow is turned aside. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance instance, LivingEntity owner, DamageSource source, Changeable<Float> amount) {
        if (!EliteSkillsConfig.enabled() || amount.get() <= 0 || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        var tag = instance.getOrCreateTag();
        long now = owner.level().getGameTime();
        double chance = UltimateRules.Rapheal.predictChance(masteredBy(instance, owner), accelerated(instance, owner));
        if (!UltimateRules.Rapheal.predicts(owner.getRandom().nextDouble(), chance, now, tag.getLong(LAST_PREDICT))) return true;
        tag.putLong(LAST_PREDICT, now);
        amount.set(0f);
        if (owner instanceof ServerPlayer p) {
            EliteBridge.actionbar(p, SkillMsg.t("rapheal.foreseen", "Foreseen.").withStyle(ChatFormatting.AQUA));
            EliteBridge.spray(p.serverLevel(), false, p.position().add(0, 1, 0), 8, 0.4, 0.6, 0.4, 0.03);
        }
        instance.markDirty();
        return true;
    }

    /** Once a second while accelerated: a quarter of every other skill's remaining cooldown runs off. */
    @Override
    public void upkeep(ManasSkillInstance instance, ServerPlayer player) {
        if (!EliteSkillsConfig.enabled() || !accelerated(instance, player)) return;
        for (ManasSkillInstance other : SkillAPI.getSkillsFrom(player).getLearnedSkills()) {
            if (other.getSkillId().equals(instance.getSkillId())) continue;
            boolean any = false;
            for (int m = 0; m < Math.max(1, other.getModes()); m++) {
                int cd = other.getCoolDown(m);
                if (cd <= 0) continue;
                other.setCoolDown(Math.max(0, cd - Math.max(1, (int) Math.ceil(cd * UltimateRules.Rapheal.accelCooldownRate()))), m);
                any = true;
            }
            if (any) other.markDirty();
        }
    }

    /** The multiplier on a blow by this owner at this victim while the victim is the live mark. */
    static double markMultiplier(ManasSkillInstance instance, LivingEntity attacker, LivingEntity victim) {
        var tag = instance.getOrCreateTag();
        if (!tag.hasUUID(MARK) || attacker.level().getGameTime() >= tag.getLong(MARK_UNTIL)) return 1.0;
        UUID id = tag.getUUID(MARK);
        return id.equals(victim.getUUID()) ? 1.0 + UltimateRules.Rapheal.MARK_BONUS : 1.0;
    }
}
