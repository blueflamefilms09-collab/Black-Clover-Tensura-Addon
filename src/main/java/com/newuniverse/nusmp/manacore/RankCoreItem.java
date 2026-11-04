package com.newuniverse.nusmp.manacore;

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

/**
 * Why: one class for all 24 rank cores (3.51.0). Right-click is the first active, sneak + right-click the second; the server decides everything ({@link ManaCoreActives}), the client only asks by using the item. The
 * passive works while the core is held (either hand) or worn in a Curios slot ({@link ManaCoreRuntime}). Stateless: it holds no data, so a copied stack is a plain copy.
 */
public class RankCoreItem extends Item {
    private final ManaCoreRules.Core core;

    public RankCoreItem(ManaCoreRules.Core core, Properties props) {
        super(props);
        this.core = core;
    }

    public ManaCoreRules.Core core() { return core; }

    @Override public boolean isFoil(ItemStack stack) { return core.rank() >= 6; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) ManaCoreActives.use(sp, core, sp.isShiftKeyDown() ? 2 : 1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        String id = core.id();
        tip.add(Component.translatable("manacore.tooltip.rank", ManaCoreRules.RANK_LABEL[core.rank()], Component.translatable("manacore.variant." + ManaCoreRules.VARIANTS[core.variant()])).withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("manacore." + id + ".passive").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("manacore." + id + ".a1", ManaCoreRules.mpText(core.a1())).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("manacore." + id + ".a2", ManaCoreRules.mpText(core.a2())).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("manacore.tooltip.use").withStyle(ChatFormatting.DARK_GRAY));
        if (core.will() != null) tip.add(Component.translatable("manacore.tooltip.will").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
