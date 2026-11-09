package com.newuniverse.nusmp.heroweapon.gem;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroAbandon;
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
 * Why: the survival player's way to remove a hero or vassal weapon (3.73.0), crafted from a Gneiss Crystal in the middle of the grid and Tensura's magic crystals round it, so no
 * operator is needed. Right click once: the crystal asks (the weapon's name, what is lost) and waits 30 seconds; right click again within that time: the weapon is released through
 * the same {@code HeroBonds.release} an operator and {@code /multiverse hero abandon} use (the loan ends, a unique Holy Weapon is freed in the register, the forms and the client
 * are told) and ONE crystal is spent (a creative player keeps it). Refused while the curse holds the wielder. Works whatever the {@code abandonAllowed} setting says (that one
 * switches the command). Server side only; state is the abandon question's (one tick per player, capped).
 */
public class ReleaseCrystalItem extends Item {
    public ReleaseCrystalItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.success(stack);
        HeroKind was = HeroAbandon.weapon(p);
        HeroAbandon.Result r = HeroAbandon.confirm(p, true);                                 // a second use inside the window releases
        if (r == HeroAbandon.Result.NOT_ASKED) {
            r = HeroAbandon.ask(p, true);
            if (r == HeroAbandon.Result.ASKED)
                p.displayClientMessage(Component.literal("Release the " + (was == null ? "weapon" : was.display()) + "? Its level, forms and mastery are lost for good. Use the crystal again within 30 seconds.")
                        .withStyle(ChatFormatting.GOLD), false);
        }
        switch (r) {
            case DONE -> {
                stack.consume(1, p);
                p.displayClientMessage(Component.literal("The crystal shatters. The " + (was == null ? "weapon" : was.display()) + " is released, with its level, forms and mastery.").withStyle(ChatFormatting.AQUA), false);
            }
            case NO_BOND -> p.displayClientMessage(Component.literal("You carry no hero or vassal weapon.").withStyle(ChatFormatting.GRAY), true);
            case LOCKED -> p.displayClientMessage(Component.literal("The curse holds you: the weapon cannot be released now.").withStyle(ChatFormatting.RED), true);
            default -> { }
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Right click twice to release your hero or vassal weapon for good.").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("The crystal is spent. Not while the curse holds you.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
