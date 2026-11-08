package com.newuniverse.nusmp.beastwill;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;

import java.util.List;

/**
 * Why: the Beast Will Core (3.45.0). Using it does NOT absorb it: it asks the server to check the player and show the preview ({@link BeastWillAcquire#open}); the core is only taken when the player
 * confirms and every check still holds. A core whose will no longer exists (a removed data file) just says so and stays in the inventory.
 */
public final class BeastWillCoreItem extends Item {
    public BeastWillCoreItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BeastWillComponents.Core c = BeastWillComponents.of(stack);
        if (c == null) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (!BeastWillAcquire.open(sp, c.will(), c.beast(), c.rankValue(), BeastWillAcquire.CORE, c.serial())) BeastWillFeedback.say(sp, "no.unknown_will", false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tip, TooltipFlag flag) {
        BeastWillComponents.Core c = BeastWillComponents.of(stack);
        if (c == null) {
            tip.add(Component.translatable("item.nusmp.beast_will_core.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        tip.add(Component.translatable("item.nusmp.beast_will_core.of", BeastWillText.name(c.will())).withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("item.nusmp.beast_will_core.beast", c.beast().isBlank() ? "?" : c.beast(), c.rankValue().name()).withStyle(ChatFormatting.GRAY));
        String lore = "nusmp.beastwill." + c.will() + ".lore";
        if (net.minecraft.locale.Language.getInstance().has(lore)) tip.add(Component.translatable(lore).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));   // 3.60.0: the remade will's lore line
        tip.add(Component.translatable("item.nusmp.beast_will_core.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
