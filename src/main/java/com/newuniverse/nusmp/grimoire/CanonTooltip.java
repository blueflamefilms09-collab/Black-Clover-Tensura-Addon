package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.blackclover.GrimoireCover;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Canon leaves: the tooltip lines of a grimoire (the meaning of its cover, then its luck or its devil's price). Text only, no client class; the lang keys fall back to the English text. */
public final class CanonTooltip {
    private CanonTooltip() {}

    public static void add(List<Component> tooltip, GrimoireCover c) {
        if (c == null) return;
        tooltip.add(Component.translatableWithFallback(GrimoireMeaning.key(c), GrimoireMeaning.text(c)).withStyle(GrimoireMeaning.modOnly(c) ? ChatFormatting.DARK_GRAY : ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (CanonLeaves.luckCover(c) && CanonLeaves.luckChance() > 0.0) {
            String chance = String.valueOf(Math.round(CanonLeaves.luckChance() * 100)), mult = String.format(java.util.Locale.ROOT, "%.2f", CanonLeaves.luckMult());
            tooltip.add(Component.translatableWithFallback("anime_multiverse.canon.luck", "Luck: %s%% of spell hits deal x%s", chance, mult).withStyle(ChatFormatting.GREEN));
        }
        if (CanonLeaves.paysDevil(c) && CanonLeaves.devilShare() > 0.0) {
            String share = String.valueOf(Math.round(CanonLeaves.devilShare() * 100));
            tooltip.add(Component.translatableWithFallback("anime_multiverse.canon.devil", "Devil's price: it holds %s%% of your mana, and takes a little health when you are nearly dry", share).withStyle(ChatFormatting.DARK_RED));
        }
    }
}
