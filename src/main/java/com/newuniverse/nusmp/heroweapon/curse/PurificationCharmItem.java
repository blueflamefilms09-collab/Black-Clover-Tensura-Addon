package com.newuniverse.nusmp.heroweapon.curse;

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
 * Why: in the series the Bless Series is what breaks a curse, and the owner asked for the Curse Series to cost something real, so there has to be a way to pay
 * that cost back that the owner can hand out, craft and tune. The Purification Charm is the stand-in: right click and it clears the corruption of the wielder's
 * hero weapon, and the first one a wielder ever uses also gives back some of the hearts the curse took for good (how many, and how many charms may do so, are
 * in the config). It does nothing for a player without a hero weapon and nothing while the weapon holds them in an Overrun, and in those cases it is not used up. The
 * decision is made on the server; the client only plays the click.
 */
public class PurificationCharmItem extends Item {
    /** Ticks before the same player can use a charm again (stops a held key from wasting a stack on one cleansing). */
    private static final int COOLDOWN_TICKS = 20;

    public PurificationCharmItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(stack);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (!CurseService.charm(sp, CurseService.now(sp), CurseEvents.tuning(sp.getServer()))) return InteractionResultHolder.fail(stack);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        if (!player.isCreative()) stack.shrink(1);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(CurseMessages.tr(CurseText.CHARM_TIP_1).withStyle(ChatFormatting.GOLD));
        lines.add(CurseMessages.tr(CurseText.CHARM_TIP_2).withStyle(ChatFormatting.GRAY));
        lines.add(CurseMessages.tr(CurseText.CHARM_TIP_3).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
