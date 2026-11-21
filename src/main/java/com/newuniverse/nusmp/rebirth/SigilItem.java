package com.newuniverse.nusmp.rebirth;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Why: the two craftable rites. The item only hands the click to {@link RebirthService} on the server; all rules live there and in {@link RebirthRules}. */
public class SigilItem extends Item {
    private final RebirthRules.Rite rite;

    public SigilItem(Properties props, RebirthRules.Rite rite) {
        super(props);
        this.rite = rite;
    }

    public RebirthRules.Rite rite() { return rite; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.pass(stack);
        RebirthService.use(p, stack, rite);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        String id = rite == RebirthRules.Rite.REBIRTH ? "sigil_of_rebirth" : "sigil_of_the_second_book";
        tip.add(Component.translatable("item.anime_multiverse." + id + ".tip1").withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("item.anime_multiverse." + id + ".tip2").withStyle(ChatFormatting.GRAY));
    }
}
