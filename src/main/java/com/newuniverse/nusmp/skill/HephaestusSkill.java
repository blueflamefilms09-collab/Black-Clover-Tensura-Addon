package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Hephaestus, Goddess of the Forge: repairs your held item (and your armor when mastered). */
public class HephaestusSkill extends GodSkill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/iron_ingot.png");
    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override protected double cost() { return 2000; }
    @Override protected int cooldownTicks() { return 2400; }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, boolean mastered) {
        boolean repaired = repair(player.getItemInHand(InteractionHand.MAIN_HAND));
        if (mastered) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                repaired |= repair(player.getItemBySlot(slot));
            }
        }
        if (!repaired) { SkillUtil.fail(player, "Nothing to repair."); return false; }
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 1, player.getZ(), 15, 0.4, 0.4, 0.4, 0.0);
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static boolean repair(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem() || stack.getDamageValue() == 0) return false;
        stack.setDamageValue(0);
        return true;
    }
}
