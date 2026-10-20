package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.elite.hub.CoinMath;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;

import java.util.List;
import java.util.Locale;

/**
 * Why: the tooltip is how a player reads a roll, so it says exactly what the game applies: the rarity and the floor the piece came from, a weapon's real total damage (the
 * grade table plus the rolled line) and its attacks per second, then every other stat line in blue, then the marks a piece can carry (shadow-tempered at an anvil, venom coating).
 * The vanilla attribute list is switched off on loot gear (the modifiers are stored with the tooltip flag off) because it would print the same numbers a second time in a
 * different shape. Every line is a translatable component with an English fallback ({@link LootText}). Client and server both call it; it reads only the stack.
 */
final class GearTooltip {
    private GearTooltip() {}

    static final String VENOM = "nusmp_venom";

    static void gear(ItemStack stack, LootGear g, List<Component> tip) {
        GearRoll roll = GearStacks.rollOf(stack);
        if (roll == null) return;
        LootRarity r = roll.rarity();
        ChatFormatting colour = ChatFormatting.valueOf(r.colour());
        tip.add(LootText.text("tooltip.nusmp.loot.header", LootText.text("tooltip.nusmp.loot.rarity." + r.id()), roll.depth() == 0 ? "?" : Integer.toString(roll.depth())).withStyle(colour));
        if (g.gearKind() == GearKind.WEAPON) {
            double dmg = GearRollMath.totalDamage(r, g.weaponType(), roll);
            String grade = r.grade() == 0 ? "normal" : r.grade() == 1 ? "special" : "rare";
            tip.add(LootText.text("tooltip.nusmp.loot.damage", trim(dmg), LootText.text("tooltip.nusmp.loot.grade." + grade)).withStyle(ChatFormatting.GRAY));
            tip.add(LootText.text("tooltip.nusmp.loot.speed", trim(g.attackSpeed() + roll.sum(StatKind.ATTACK_SPEED))).withStyle(ChatFormatting.GRAY));
        }
        for (GearRoll.Line l : roll.lines()) {
            if (g.gearKind() == GearKind.WEAPON && (l.kind() == StatKind.ATTACK_DAMAGE || l.kind() == StatKind.ATTACK_SPEED)) continue;
            tip.add(LootText.text("tooltip.nusmp.loot.stat." + l.kind().id(), GearRollMath.format(l.kind(), l.value())).withStyle(ChatFormatting.BLUE));
        }
        if (roll.tempered()) tip.add(LootText.text("tooltip.nusmp.loot.tempered").withStyle(ChatFormatting.DARK_PURPLE));
        int venom = venom(stack);
        if (venom > 0) tip.add(LootText.text("tooltip.nusmp.loot.venom", venom).withStyle(ChatFormatting.DARK_GREEN));
        value(stack, tip, roll);
    }

    static void value(ItemStack stack, List<Component> tip, GearRoll roll) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        long v = LootValue.unit(LootRoster.get(), id, roll);
        if (v > 0L) tip.add(LootText.text("tooltip.nusmp.loot.value", CoinMath.describe(v)).withStyle(ChatFormatting.DARK_GRAY));
    }

    static int venom(ItemStack stack) {
        CustomData d = stack.get(DataComponents.CUSTOM_DATA);
        return d == null ? 0 : d.copyTag().getInt(VENOM);
    }

    private static String trim(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }
}
