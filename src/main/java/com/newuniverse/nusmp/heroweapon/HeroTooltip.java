package com.newuniverse.nusmp.heroweapon;

import com.newuniverse.nusmp.client.heroweapon.ClientHeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroLevels;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;

/**
 * Why: the tooltip of a hero weapon is the player's first and most frequent explanation of it, so it says the things the owner's text makes important: what
 * family the weapon is, whose it is (it is bound to a person, not to the stack), how far it has come and what it can do right now (level, active form,
 * the techniques on their inputs), which keys to press, and, for the Holy Weapons, the three restrictions in one line. The level, form and techniques come from
 * the client's copy of the bond ({@code ClientHeroBond}) and are shown only on a stack of the weapon the local player is bound to. The names of the keys are
 * whatever the player bound them to: the client installs {@link #keyNames} at start-up (a common class must not touch the client's key objects itself), and the
 * defaults are the keys the mod ships with. All text is lang keys of the {@code tooltip.nusmp.hero.*} family.
 */
public final class HeroTooltip {
    private HeroTooltip() {}

    private static final String[] DEFAULT_KEYS = {"Z", "X", "C", "B", "N"};

    /** The name of hero key {@code i}: 0 to 2 the technique keys, 3 summon and put away, 4 change form. The client replaces it with the player's real bindings. */
    public static volatile IntFunction<String> keyNames = i -> i >= 0 && i < DEFAULT_KEYS.length ? DEFAULT_KEYS[i] : "?";

    /** Adds the lines of a hero weapon's tooltip. */
    public static void build(HeroKind kind, ItemStack stack, Item.TooltipContext context, List<Component> lines) {
        lines.add(Component.literal(kind.heroClass().label()).withStyle(ChatFormatting.GOLD));
        ownerLine(stack, context, lines);
        if (ClientHeroBond.kind() == kind) {
            try {
                bondLines(kind, lines);
            } catch (RuntimeException unreadable) {
                // a tooltip must never break the screen it is drawn on: the level, form and techniques are simply left out
            }
        }
        lines.add(Component.translatable("tooltip.nusmp.hero.keys", keyNames.apply(0) + " " + keyNames.apply(1) + " " + keyNames.apply(2), keyNames.apply(3), keyNames.apply(4))
                .withStyle(ChatFormatting.DARK_AQUA));
        if (kind.isHoly()) lines.add(Component.translatable("tooltip.nusmp.hero.holy").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        lines.add(Component.translatable("tooltip.nusmp.hero.bound_body").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    private static void ownerLine(ItemStack stack, Item.TooltipContext context, List<Component> lines) {
        String owner = HeroComponents.owner(stack);
        if (owner.isEmpty()) {
            lines.add(Component.translatable("tooltip.nusmp.hero.unbound").withStyle(ChatFormatting.GRAY));
            return;
        }
        String name = null;
        try {
            if (context != null && context.level() != null) {
                Player who = context.level().getPlayerByUUID(UUID.fromString(owner));
                if (who != null) name = who.getGameProfile().getName();
            }
        } catch (IllegalArgumentException notAUuid) {
            // a stack with a damaged owner reads as "its wielder"
        }
        lines.add(name != null ? Component.translatable("tooltip.nusmp.hero.bound", name).withStyle(ChatFormatting.GRAY)
                : Component.translatable("tooltip.nusmp.hero.bound_unknown").withStyle(ChatFormatting.GRAY));
    }

    /** Level, form and the techniques of the active form, from the client's copy of the local player's bond. */
    private static void bondLines(HeroKind kind, List<Component> lines) {
        long exp = ClientHeroBond.exp();
        int level = HeroLevels.level(exp);
        String progress = level >= HeroLevels.MAX ? "max" : (exp - HeroLevels.expFor(level)) + " / " + (HeroLevels.expFor(level + 1) - HeroLevels.expFor(level));
        lines.add(Component.translatable("tooltip.nusmp.hero.level", level, progress).withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("tooltip.nusmp.hero.form", ClientHeroBond.formName().isEmpty() ? ClientHeroBond.form() : ClientHeroBond.formName()).withStyle(ChatFormatting.YELLOW));
        List<String> slots = ClientHeroBond.slots();
        for (int i = 0; i < slots.size() && i < HeroTrigger.values().length; i++) {
            String id = slots.get(i);
            if (id.isEmpty()) continue;
            HeroSkill skill = HeroLogics.skill(id);
            Component name = skill == null ? Component.literal(id) : Component.translatableWithFallback("hero.skill." + id + ".name", skill.name());
            lines.add(Component.translatable("tooltip.nusmp.hero.skill", slotLabel(i), name).withStyle(ChatFormatting.GRAY));
        }
    }

    /** The label of an input: the use key, sneak and use, or the name of the technique key. */
    private static Component slotLabel(int slot) {
        return switch (slot) {
            case 0 -> Component.translatable("tooltip.nusmp.hero.slot.use");
            case 1 -> Component.translatable("tooltip.nusmp.hero.slot.sneak");
            default -> Component.literal(keyNames.apply(slot - 2));
        };
    }
}
