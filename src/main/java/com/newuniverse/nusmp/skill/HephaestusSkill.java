package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.GodRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Hephaestus, Goddess of the Forge: repairs the item in your hand; once mastered also your off-hand item and every piece of armour you
 * wear. If there is nothing damaged to repair the skill is not used (no magicules spent, no cooldown). Numbers: {@link GodRules.Hephaestus}.
 */
public class HephaestusSkill extends GodSkill {
    static final SkillText TEXT = SkillText.of("hephaestus", "Hephaestus",
            "Unique. Goddess of the Forge. Restore the weapon in your hand, and your off-hand and armour once mastered.");

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public HephaestusSkill() { super(TEXT); }

    @Override protected double cost() { return GodRules.Hephaestus.COST; }
    @Override protected int cooldownTicks() { return GodRules.Hephaestus.COOLDOWN; }
    @Override protected int color() { return 0xFFFF8A3A; }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, Tier tier) {
        int points = repair(player.getItemInHand(InteractionHand.MAIN_HAND), tier);
        if (GodRules.Hephaestus.offhand(tier)) points += repair(player.getItemInHand(InteractionHand.OFF_HAND), tier);
        if (GodRules.Hephaestus.armor(tier)) for (EquipmentSlot slot : ARMOR) points += repair(player.getItemBySlot(slot), tier);
        if (points <= 0) {
            SkillUtil.fail(player, SkillMsg.t("hephaestus.nothing", "Nothing to repair."));
            return false;
        }
        ServerLevel level = player.serverLevel();
        Vec3 at = player.position().add(0, 1, 0);
        SkillFx.spray(level, false, at, 12, 0.4, 0.4, 0.4, 0.04);
        SkillFx.shape(level, VfxShape.ALCHEMY_CIRCLE, player.position().add(0, 0.05, 0), at, 0xFFFF8A3A, 24, 1.2f);
        SkillFx.sound(level, at, SoundEvents.ANVIL_USE, 1.0F, 1.0F);
        SkillUtil.actionbar(player, SkillMsg.t("hephaestus.done", "Forged anew: %s durability restored.", points));
        return true;
    }

    /** Repairs one stack; returns the durability points restored. */
    private static int repair(ItemStack stack, Tier tier) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return 0;
        int restored = GodRules.Hephaestus.restored(stack.getDamageValue(), stack.getMaxDamage(), tier);
        if (restored > 0) stack.setDamageValue(stack.getDamageValue() - restored);
        return restored;
    }
}
