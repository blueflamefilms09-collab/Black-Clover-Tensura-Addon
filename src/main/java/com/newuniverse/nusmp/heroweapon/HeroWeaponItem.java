package com.newuniverse.nusmp.heroweapon;

import com.newuniverse.nusmp.heroweapon.skill.HeroUse;
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
 * Why: the item a hero weapon is while it is summoned. One class for all 21 weapons: what a weapon DOES is in its logic and its forms, what the item must do
 * is common (hand the input to the dispatcher, show its class in the tooltip, be unbreakable, never be dropped). The core builder owns this class and
 * replaces the minimal body below with the real one (attributes per style, the use animation of a raised Shield or a drawn Bow, the binding rules).
 */
public class HeroWeaponItem extends Item {
    private final HeroKind kind;

    public HeroWeaponItem(HeroKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public HeroKind kind() { return kind; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) HeroUse.fire(sp, player.isShiftKeyDown() ? HeroTrigger.SNEAK_USE : HeroTrigger.USE);
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal(kind.heroClass().label()).withStyle(ChatFormatting.GOLD));
    }
}
