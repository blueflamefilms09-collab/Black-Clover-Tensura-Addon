package com.newuniverse.nusmp.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
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
 * 4.65.0: the Infernal Contract is an ordinary craftable item, no longer a relic (no grade, no attunement, no relic acts, not in the Relics catalogue). Using it opens the same three prices
 * of the old bargain as before; the four contracts and the devil worlds are offered by the use hook in {@code Wave0Hooks}, which reads the item, not a relic. The server decides everything.
 */
public class DevilContractItem extends Item {
    public DevilContractItem(Item.Properties props) { super(props); }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.anime_multiverse.devil_contract.desc").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.pass(stack);
        p.sendSystemMessage(Component.literal("A voice from the fifth leaf: \"What will you pay?\"").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        for (String[] o : new String[][]{{"maxhp", "Your life (-" + com.newuniverse.nusmp.NUConfig.FORBIDDEN_MAXHP_PRICE.get() + " max HP)"},
                {"seal", "A page (one unlocked page sealed forever)"}, {"tax", "Your mana (+" + (int) (com.newuniverse.nusmp.NUConfig.FORBIDDEN_MANA_TAX.get() * 100) + "% grimoire cost forever)"}}) {
            p.sendSystemMessage(Component.literal(" [" + o[1] + "]").withStyle(s -> s.withColor(ChatFormatting.RED)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/devilpact accept " + o[0]))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Bind the devil and pay this price")))));
        }
        return InteractionResultHolder.success(stack);
    }
}
