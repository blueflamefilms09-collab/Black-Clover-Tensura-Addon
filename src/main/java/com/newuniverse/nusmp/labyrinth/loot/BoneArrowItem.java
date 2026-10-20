package com.newuniverse.nusmp.labyrinth.loot;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Why: the bone-shard arrow is the simplest crafted use of a monster part, and it should be a real arrow: a vanilla {@code ArrowItem}, shot by any bow or crossbow (Better
 * Combat and the mod's own bows fire whatever the ammo predicate accepts), that makes the vanilla {@code Arrow} entity with one point more base damage than flint. Nothing else
 * changes, so every enchantment and every effect that works on arrows works on it.
 */
public class BoneArrowItem extends ArrowItem {
    private final String lore;

    public BoneArrowItem(Properties props, String lore) {
        super(props);
        this.lore = lore;
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, @Nullable ItemStack weapon) {
        Arrow arrow = new Arrow(level, shooter, ammo.copyWithCount(1), weapon);
        arrow.setBaseDamage(arrow.getBaseDamage() + 1.0);
        return arrow;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("item.nusmp.bone_arrow.lore", lore).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
