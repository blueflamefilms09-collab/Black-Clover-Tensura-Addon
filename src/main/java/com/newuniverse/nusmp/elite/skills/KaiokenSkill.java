package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.blackclover.TimedModifiers;
import com.newuniverse.nusmp.elite.skills.rules.IntrinsicRules;
import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;

import java.util.List;

/**
 * Kaioken (Intrinsic, designed for this mod from the Dragon Ball technique; the original addon's source was not available): TOGGLE a multiplier on your own body. The mode is the rung of the
 * ladder: x2, x3, x4, then (mastered) x10 and x20. Each rung raises your melee damage and your speed (Kaioken x2: +30% damage, +20% speed; x20: +400% damage, speed capped at +100%) and burns a share
 * of your MAXIMUM health every second (1% at x2, 10% at x20) together with magicules. It shuts itself off before it can kill you (below 4 hit points or 15% of your health) or when the magicules
 * run out, and leaves you with Weakness and Slowness, longer from a higher rung and a longer hold. Config: {@code kaiokenDamageScale} (the bonuses) and {@code kaiokenDrainScale} (the health burn).
 * Numbers: {@link IntrinsicRules.Kaioken}. Which rung is chosen is kept in the skill's own data when the mode changes; whether the game also reports the chosen mode on the toggle press is not
 * verifiable without the game (the rung is then the last one selected).
 */
public class KaiokenSkill extends EliteSkill {
    static final SkillText TEXT = SkillText.of("kaioken", "Kaioken",
            "Intrinsic. Toggle. Multiply your own body (x2, x3, x4; x10 and x20 once mastered): more melee damage and speed, but it burns your health every second and leaves you spent.");

    static final String LEVEL = "Level", HELD = "HeldSeconds";
    private static final ResourceLocation DAMAGE_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "kaioken_damage"),
            SPEED_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "kaioken_speed"), ATTACK_SPEED_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "kaioken_attack_speed");

    public KaiokenSkill() { super(SkillType.INTRINSIC, TEXT, false); }

    @Override
    protected List<EliteMode> modes() {
        return List.of(EliteMode.of("kaioken.x2", "Kaioken x2", 0.0, 0), EliteMode.of("kaioken.x3", "Kaioken x3", 0.0, 0),
                EliteMode.of("kaioken.x4", "Kaioken x4", 0.0, 0), EliteMode.of("kaioken.x10", "Kaioken x10", 0.0, 0),
                EliteMode.of("kaioken.x20", "Kaioken x20", 0.0, 0));
    }

    @Override protected boolean instant(int mode) { return false; }

    @Override protected boolean use(ManasSkillInstance instance, ServerPlayer player, int mode) { return false; }

    @Override protected int color() { return 0xFFE02828; }

    private static boolean masteredNow(ManasSkillInstance instance) { return instance.getMastery() >= instance.getMaxMastery(); }

    /** Only the rungs the wearer has unlocked can be selected. */
    @Override public int getModes(ManasSkillInstance instance) { return IntrinsicRules.Kaioken.unlocked(masteredNow(instance)); }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        int next = IntrinsicRules.Kaioken.next(mode, entity != null ? instance.isMastered(entity) : masteredNow(instance), reverse);
        chooseLevel(instance, entity, next);
        return next;
    }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        int lv = IntrinsicRules.Kaioken.clampLevel(mode, true);
        return SkillMsg.mode(mode(lv).id(), mode(lv).name());
    }

    @Override public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) { return true; }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        chooseLevel(instance, entity, mode);                                   // if the game reports the selected mode on the toggle press, take it
    }

    private static void chooseLevel(ManasSkillInstance instance, LivingEntity entity, int level) {
        int lv = IntrinsicRules.Kaioken.clampLevel(level, entity != null ? instance.isMastered(entity) : masteredNow(instance));
        if (instance.getOrCreateTag().getInt(LEVEL) == lv) return;
        instance.getOrCreateTag().putInt(LEVEL, lv);
        instance.markDirty();
        if (entity instanceof ServerPlayer p && instance.isToggled()) {
            EliteBridge.actionbar(p, SkillMsg.t("kaioken.level", "Kaioken x%s", IntrinsicRules.Kaioken.multiplier(lv)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }
    }

    @Override
    public void onToggleOn(ManasSkillInstance instance, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!EliteSkillsConfig.enabled() || !IntrinsicRules.Kaioken.canSustain(player.getHealth(), player.getMaxHealth())) {
            EliteBridge.fail(player, SkillMsg.t("kaioken.weak", "You are too hurt to push your body further."));
            instance.setToggled(false);
            instance.markDirty();
            return;
        }
        instance.getOrCreateTag().putInt(HELD, 0);
        int lv = IntrinsicRules.Kaioken.clampLevel(instance.getOrCreateTag().getInt(LEVEL), instance.isMastered(player));
        EliteBridge.actionbar(player, SkillMsg.t("kaioken.on", "Kaioken x%s!", IntrinsicRules.Kaioken.multiplier(lv)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        EliteBridge.sound(player.serverLevel(), player.position(), SoundEvents.BLAZE_SHOOT, 1.0f, 0.6f);
        apply(instance, player, lv);
        instance.markDirty();
    }

    @Override
    public void onToggleOff(ManasSkillInstance instance, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        remove(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID);
        remove(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
        remove(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID);
        int held = instance.getOrCreateTag().getInt(HELD);
        if (held <= 0) return;
        int lv = IntrinsicRules.Kaioken.clampLevel(instance.getOrCreateTag().getInt(LEVEL), true);
        int ticks = IntrinsicRules.Kaioken.aftershockTicks(lv, held);
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 0, true, false));
        instance.getOrCreateTag().putInt(HELD, 0);
        EliteBridge.actionbar(player, SkillMsg.t("kaioken.off", "The strain leaves you spent.").withStyle(ChatFormatting.GRAY));
    }

    /** Once a second while on: the burn, the upkeep and the bonuses (the bonuses are timed, so they also fade by themselves if the skill is lost while on). */
    @Override
    public void upkeep(ManasSkillInstance instance, ServerPlayer player) {
        if (!instance.isToggled()) return;
        if (!EliteSkillsConfig.enabled()) { shutOff(instance, player); return; }
        int lv = IntrinsicRules.Kaioken.clampLevel(instance.getOrCreateTag().getInt(LEVEL), instance.isMastered(player));
        double burn = IntrinsicRules.Kaioken.healthDrain(lv, player.getMaxHealth()) * EliteSkillsConfig.get(EliteSkillsConfig.KAIOKEN_DRAIN_SCALE);
        if (!IntrinsicRules.Kaioken.canSustain(player.getHealth() - burn, player.getMaxHealth())) {
            EliteBridge.actionbar(player, SkillMsg.t("kaioken.limit", "Your body cannot take any more: Kaioken ends.").withStyle(ChatFormatting.RED));
            shutOff(instance, player);
            return;
        }
        if (!EliteUtil.tryDrain(player, EliteSkillsConfig.cost(IntrinsicRules.Kaioken.magiculeDrain(lv)))) {
            EliteBridge.actionbar(player, SkillMsg.t("kaioken.empty", "You have no magicules left: Kaioken ends.").withStyle(ChatFormatting.RED));
            shutOff(instance, player);
            return;
        }
        if (burn > 0) player.setHealth((float) (player.getHealth() - burn));
        apply(instance, player, lv);
        instance.getOrCreateTag().putInt(HELD, instance.getOrCreateTag().getInt(HELD) + 1);
        if (instance.getOrCreateTag().getInt(HELD) % 10 == 0) instance.addMasteryPoint(player);
        EliteBridge.follow(player.serverLevel(), VfxShape.SPIRIT_AURA, player, player.position().add(0, 1, 0), 0xFFE02828, 30, 0.8f + 0.12f * lv);
        instance.markDirty();
    }

    private static void shutOff(ManasSkillInstance instance, ServerPlayer player) {
        instance.setToggled(false);
        instance.onToggleOff(player);
        instance.markDirty();
    }

    private static void apply(ManasSkillInstance instance, ServerPlayer player, int lv) {
        double scale = EliteSkillsConfig.get(EliteSkillsConfig.KAIOKEN_DAMAGE_SCALE);
        TimedModifiers.apply(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID, IntrinsicRules.Kaioken.damageBonus(lv) * scale, 30, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        TimedModifiers.apply(player, Attributes.MOVEMENT_SPEED, SPEED_ID, Math.min(1.0, IntrinsicRules.Kaioken.speedBonus(lv) * scale), 30, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        TimedModifiers.apply(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, Math.min(0.75, IntrinsicRules.Kaioken.speedBonus(lv) * 0.5 * scale), 30, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void remove(ServerPlayer player, Holder<Attribute> attr, ResourceLocation id) {
        var inst = player.getAttribute(attr);
        if (inst != null) inst.removeModifier(id);
    }
}
