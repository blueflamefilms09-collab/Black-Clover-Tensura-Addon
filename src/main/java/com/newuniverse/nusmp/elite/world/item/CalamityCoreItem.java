package com.newuniverse.nusmp.elite.world.item;

import com.newuniverse.nusmp.elite.world.WorldData;
import com.newuniverse.nusmp.elite.world.WorldItems;
import com.newuniverse.nusmp.elite.world.rules.CalamityDef;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * Why: the calamity CORE dropped when a calamity falls. It remembers which calamity it came from (in the stack's custom data, key {@value #KEY}), is the ritual item that calls that calamity
 * back when used on a charged Mother Rock, and is a trophy and a crafting material for other systems (it shines like an enchanted item). One item for every calamity, including the ones a
 * datapack adds; the name and the tooltip come from the calamity's data.
 */
public class CalamityCoreItem extends Item {
    public static final String KEY = "calamity";

    public CalamityCoreItem(Properties properties) { super(properties); }

    /** A core of this calamity. */
    public static ItemStack stackFor(String calamityId) {
        ItemStack s = new ItemStack(WorldItems.CALAMITY_CORE.get());
        CompoundTag t = new CompoundTag();
        t.putString(KEY, calamityId);
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        return s;
    }

    /** The calamity id stored in a core, or "" for a blank one. */
    public static String calamityOf(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? "" : d.copyTag().getString(KEY);
    }

    @Override
    public Component getName(ItemStack stack) {
        String id = calamityOf(stack);
        if (id.isEmpty()) return super.getName(stack);
        CalamityDef def = WorldData.calamity(id);
        String english = def == null ? id : def.name();
        return Component.translatableWithFallback("nusmp.elite.calamity." + id + ".core", "Core of " + english);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.nusmp.calamity_core.tip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.nusmp.calamity_core.tip2").withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }
}
