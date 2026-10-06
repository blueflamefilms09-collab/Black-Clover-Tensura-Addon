package com.newuniverse.nusmp.antimagic;

import com.newuniverse.nusmp.item.MagicWeaponItem;
import com.newuniverse.nusmp.item.NUItems;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Anti-Magic Spirit Lord (Unique). No mana: you run on Anti-Magic Power (AMP), gathered by
 * absorbing magic that hits you, by demon-sword hits and slowly over time.
 * Passive physique: +15% attack damage, knockback resistance, 30% less magic damage, half fall damage.
 * Toggle - Black Form (needs 100 AMP, or mastery): magic can't hurt you, +25% damage, speed; drains AMP.
 * Grants the Demon-Slayer sword; mastering it grants Demon-Dweller and Demon-Destroyer.
 */
public class AntiMagicLordSkill extends Skill {
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/skill/grimoire/anti_magic.png");
    private static final ResourceLocation PHYS_DMG = ResourceLocation.fromNamespaceAndPath("nusmp", "anti_magic_physique");
    private static final ResourceLocation PHYS_KB = ResourceLocation.fromNamespaceAndPath("nusmp", "anti_magic_knockback");
    private static final ResourceLocation FORM_DMG = ResourceLocation.fromNamespaceAndPath("nusmp", "black_form");

    public AntiMagicLordSkill() { super(SkillType.UNIQUE); }

    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override public boolean canBeToggled(ManasSkillInstance i, LivingEntity e) { return true; }
    @Override public boolean canTick(ManasSkillInstance i, LivingEntity e) { return true; }

    private static void modifier(LivingEntity e, Holder<Attribute> a, ResourceLocation id, double v, AttributeModifier.Operation op, boolean on) {
        var inst = e.getAttribute(a);
        if (inst == null) return;
        if (on && !inst.hasModifier(id)) inst.addTransientModifier(new AttributeModifier(id, v, op));
        if (!on && inst.hasModifier(id)) inst.removeModifier(id);
    }

    @Override
    public void onLearnSkill(ManasSkillInstance i, LivingEntity e) {
        super.onLearnSkill(i, e);
        if (e instanceof ServerPlayer p) {
            p.getInventory().placeItemBackInInventory(MagicWeaponItem.bound(NUItems.DEMON_SLAYER.get(), p));
            p.sendSystemMessage(Component.literal("Your grimoire opens - a demon's sword falls into your hand.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD));
        }
    }

    @Override
    public void onForgetSkill(ManasSkillInstance i, LivingEntity e) {
        super.onForgetSkill(i, e);
        modifier(e, Attributes.ATTACK_DAMAGE, PHYS_DMG, 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, false);
        modifier(e, Attributes.KNOCKBACK_RESISTANCE, PHYS_KB, 0, AttributeModifier.Operation.ADD_VALUE, false);
        modifier(e, Attributes.ATTACK_DAMAGE, FORM_DMG, 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, false);
    }

    @Override
    public void onSkillMastered(ManasSkillInstance i, LivingEntity e) {
        super.onSkillMastered(i, e);
        if (e instanceof ServerPlayer p) {
            p.getInventory().placeItemBackInInventory(MagicWeaponItem.bound(NUItems.DEMON_DWELLER.get(), p));
            p.getInventory().placeItemBackInInventory(MagicWeaponItem.bound(NUItems.DEMON_DESTROYER.get(), p));
            p.getInventory().placeItemBackInInventory(MagicWeaponItem.bound(NUItems.DEMON_SLASHER_KATANA.get(), p));
            p.sendSystemMessage(Component.literal("More demon swords answer you: Demon-Dweller, Demon-Destroyer and Demon-Slasher.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD));
        }
    }

    @Override
    public void onToggleOn(ManasSkillInstance i, LivingEntity e) {
        if (!(e instanceof ServerPlayer p)) return;
        if (AntiMagic.amp(i) < 100 && !i.isMastered(e)) {
            i.setToggled(false);
            p.displayClientMessage(Component.literal("Not enough Anti-Magic Power (" + AntiMagic.amp(i) + "/100).").withStyle(ChatFormatting.RED), true);
            return;
        }
        p.displayClientMessage(Component.literal("Black Form.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD), true);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SPIRIT_AURA, p, p.position().add(0, 1, 0), 0xFF1A1018, 60, 1.4f);
        VfxSpawn.send(p.serverLevel(), VfxShape.ANTI_MAGIC_SLASH, p.getEyePosition(), p.getEyePosition().add(p.getViewVector(1f).scale(3)), 0xFF2A0A30, 18, 2.0f);
    }

    @Override
    public void onToggleOff(ManasSkillInstance i, LivingEntity e) {
        modifier(e, Attributes.ATTACK_DAMAGE, FORM_DMG, 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, false);
    }

    @Override
    public void onTick(ManasSkillInstance i, LivingEntity e) {
        if (!(e instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        modifier(p, Attributes.ATTACK_DAMAGE, PHYS_DMG, 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, true);
        modifier(p, Attributes.KNOCKBACK_RESISTANCE, PHYS_KB, 0.3, AttributeModifier.Operation.ADD_VALUE, true);
        if (!i.isToggled()) { AntiMagic.setAmp(i, AntiMagic.amp(i) + 5); return; }
        int amp = AntiMagic.amp(i) - 25;
        if (amp <= 0) {
            AntiMagic.setAmp(i, 0);
            i.setToggled(false);
            modifier(p, Attributes.ATTACK_DAMAGE, FORM_DMG, 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, false);
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            p.displayClientMessage(Component.literal("Your Anti-Magic runs dry. Black Form ends.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        AntiMagic.setAmp(i, amp);
        modifier(p, Attributes.ATTACK_DAMAGE, FORM_DMG, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, true);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false));
        if (p.tickCount % 60 == 0) VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SPIRIT_AURA, p, p.position().add(0, 1, 0), 0xFF1A1018, 62, 1.4f);
        p.displayClientMessage(Component.literal("Black Form - AMP " + amp + "/" + AntiMagic.MAX_AMP).withStyle(ChatFormatting.DARK_GRAY), true);
    }

    /** Black Form: magic simply does not reach you (and feeds your AMP). */
    @Override
    public boolean onBeingDamaged(ManasSkillInstance i, LivingEntity e, DamageSource s, float amount) {
        if (i.isToggled() && AntiMagic.isMagic(s)) {
            AntiMagic.setAmp(i, AntiMagic.amp(i) + (int) (amount * 5));
            return false;
        }
        return true;
    }

    /** Physique: 30% less magic damage (absorbed as AMP), half fall damage. */
    @Override
    public boolean onTakenDamage(ManasSkillInstance i, LivingEntity e, DamageSource s, Changeable<Float> amount) {
        if (AntiMagic.isMagic(s)) {
            AntiMagic.setAmp(i, AntiMagic.amp(i) + (int) (amount.get() * 5));
            amount.set(amount.get() * 0.7f);
        }
        if (s.is(DamageTypeTags.IS_FALL)) amount.set(amount.get() * 0.5f);
        return true;
    }

    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, DamageSource s, Changeable<Float> amount) {
        if (owner instanceof ServerPlayer p && s.getDirectEntity() == owner && p.getRandom().nextInt(4) == 0) {
            addMasteryPoint(i, owner);
            i.markDirty();
        }
        return true;
    }
}
