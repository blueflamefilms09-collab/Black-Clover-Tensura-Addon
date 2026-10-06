package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.multiverse.SpiritLordSkill;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Spirit Lord Skill: a sealed spirit scroll. Using it bonds you to a spirit lord and teaches the Spirit Lord skill (no grimoire
 * needed). A scroll may name its spirit (CustomData "Spirit": Salamander / Undine / Sylph / Gnome / Anti); a blank one bonds the
 * first elemental lord no mage holds yet. The scroll is used up only after the bond succeeds.
 */
public class SpiritLordSkillItem extends Item {
    public SpiritLordSkillItem() { super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)); }

    public static ItemStack of(String spirit) {
        ItemStack s = new ItemStack(NUItems.SPIRIT_LORD_SKILL.get());
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("Spirit", spirit);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return s;
    }

    private static String spirit(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        String s = d == null ? "" : d.copyTag().getString("Spirit");
        return s.isEmpty() ? null : s;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.success(stack);
        String why = SpiritLordSkill.bond(p, spirit(stack));
        if (why != null) {
            p.displayClientMessage(Component.literal(why).withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        String s = spirit(stack);
        tooltip.add(Component.literal(s == null ? "Bonds the first free elemental spirit lord" : "Bonds " + (s.equals("Anti") ? "the Anti-Magic lord" : s))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("No grimoire needed: Spirit Channeling, Overdrive, Nova, Cataclysm").withStyle(ChatFormatting.DARK_GRAY));
    }
}
