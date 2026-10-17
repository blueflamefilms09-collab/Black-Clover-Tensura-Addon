package com.newuniverse.nusmp.labyrinth.mega.gear;

import com.newuniverse.nusmp.relics.Grade;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: standard gear is ordinary items, so the only thing that tells a player it is Special or Rare is how it READS, and the owner wants the tiers to look
 * exactly like the relic tiers: the same name colour (grey, green, aqua) and the same label ("Rare Grade") that the relic system already uses, taken from
 * the relic {@code Grade} enum (same three names) so the two can never drift apart. The look is built from plain data components, never client code: a
 * custom name coloured by grade ("Refined Iron Sword", "Warded Diamond Chestplate"; Normal keeps the plain name in grey), and a lore block that says
 * {@code Standard Gear - Rare Grade}, what the tier means, the percentage bonuses the piece got and its magic resistance. The engravings and
 * enchantments are NOT written into the lore: the game already prints them under the name. Every line is a translatable component WITH an English
 * fallback, so a stack reads correctly before the lang fragment is merged, and the text stored in the item keeps its fallback even if a lang entry is
 * removed later. Italics are switched off (custom names and lore are italic by default, which would read as "renamed by the player").
 */
final class GearLook {
    private GearLook() {}

    private static final String P = "message.nusmp.mega.mgr.";

    /** Sets the custom name and the lore of a finished stack. */
    static void apply(ItemStack stack, GearSpec spec) {
        Grade rg = relicGrade(spec.grade());
        stack.set(DataComponents.CUSTOM_NAME, name(stack, spec.grade(), rg));
        stack.set(DataComponents.LORE, new ItemLore(lore(spec, rg)));
    }

    /** The relic grade with the same name; the three share NORMAL, SPECIAL and RARE. */
    static Grade relicGrade(GearGrade g) { return Grade.valueOf(g.name()); }

    static MutableComponent name(ItemStack stack, GearGrade grade, Grade rg) {
        MutableComponent base = Component.translatable(stack.getItem().getDescriptionId());
        MutableComponent named = switch (grade) {
            case NORMAL -> base;
            case SPECIAL -> Component.translatableWithFallback(P + "name.special", "Refined %s", base);
            case RARE -> Component.translatableWithFallback(P + "name.rare", "Warded %s", base);
        };
        return styled(named, rg.colour());
    }

    static List<Component> lore(GearSpec spec, Grade rg) {
        List<Component> out = new ArrayList<>();
        out.add(styled(Component.translatableWithFallback(P + "line.grade", "Standard Gear - %s", Component.literal(rg.label())), rg.colour()));
        out.add(styled(tier(spec.grade()), ChatFormatting.GRAY));
        if (spec.powerPercent() > 0 && spec.slot().hasPowerStat()) {
            out.add(styled(spec.slot().isArmour()
                    ? Component.translatableWithFallback(P + "line.power.armour", "+%s%% armour", spec.powerPercent())
                    : Component.translatableWithFallback(P + "line.power.attack", "+%s%% attack damage", spec.powerPercent()), ChatFormatting.BLUE));
        }
        if (spec.durabilityPercent() > 0) {
            out.add(styled(Component.translatableWithFallback(P + "line.durability", "+%s%% durability", spec.durabilityPercent()), ChatFormatting.BLUE));
        }
        if (spec.resistPercent() > 0) {
            out.add(styled(Component.translatableWithFallback(P + "line.resist", "Magic Resistance: %s%%", spec.resistPercent()), ChatFormatting.DARK_AQUA));
        }
        return out;
    }

    private static MutableComponent tier(GearGrade g) {
        return switch (g) {
            case NORMAL -> Component.translatableWithFallback(P + "line.tier.normal", "Everyday make: no magic.");
            case SPECIAL -> Component.translatableWithFallback(P + "line.tier.special", "Refined craftsmanship.");
            case RARE -> Component.translatableWithFallback(P + "line.tier.rare", "High-grade make: it turns magic aside.");
        };
    }

    private static MutableComponent styled(MutableComponent c, ChatFormatting colour) {
        return c.withStyle(colour).withStyle(s -> s.withItalic(false));
    }
}
