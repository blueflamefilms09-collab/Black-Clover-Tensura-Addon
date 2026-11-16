package com.newuniverse.nusmp.dragonballs;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Dragon Radar. Right click: the SERVER reads its registry of ball positions (at most 16 rows, no world scan, no chunk load) and tells the player, on the action bar, the nearest ball within
 * {@code radarRange} blocks: which set and how many stars, how far and in which compass direction. One second of cooldown per use. It reads what the registry last knew (a carried ball reports every five
 * seconds; a ball in a chest is where its carrier last stood), so it is a radar, not a guarantee.
 */
public class DragonRadarItem extends Item {
    public DragonRadarItem() { super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp && !sp.getCooldowns().isOnCooldown(this)) {
            sp.getCooldowns().addCooldown(this, 20);
            sp.displayClientMessage(DbManager.radar(sp), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("anime_multiverse.dragonballs.radar.tip", DbConfig.radarRange()).withStyle(ChatFormatting.GRAY));
    }
}
