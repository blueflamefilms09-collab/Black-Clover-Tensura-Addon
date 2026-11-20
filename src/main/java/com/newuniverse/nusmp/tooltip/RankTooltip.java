package com.newuniverse.nusmp.tooltip;

import com.newuniverse.nusmp.balance.DamageScale;
import com.newuniverse.nusmp.balance.DamageScaleConfig;
import com.newuniverse.nusmp.relics.Rankings;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.Locale;

/**
 * Why: every item of the mod shows its rank the same way (owner request, docs/rankings.md). One shared helper: {@link #rankOf} resolves a stack (a relic card's own grade first, then the
 * generic table {@link Rankings}, then the tooltip category's default) and {@link #line} is the one "Rank: X" line, in the grade's colour. {@link #register} puts it under the item's name on
 * the client's tooltip event; the event never fires on a dedicated server, and the helpers are plain common code (no client class), so the server smoke test runs them.
 */
public final class RankTooltip {
    private RankTooltip() {}

    public static final String NAMESPACE = "anime_multiverse";

    public static void register(IEventBus gameBus) { gameBus.addListener(RankTooltip::onTooltip); }

    /** The registry path of a mod item, or null for another namespace, an empty stack and a spawn egg (the owner left eggs out of the tooltip rework). */
    static String pathOf(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() instanceof SpawnEggItem) return null;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && NAMESPACE.equals(id.getNamespace()) ? id.getPath() : null;
    }

    /** The rank of a stack of the mod, or null if it is not an item of the mod. Never null for one that is: the category default is the last answer. */
    public static Rankings.Rank rankOf(ItemStack stack) {
        String path = pathOf(stack);
        if (path == null) return null;
        Rankings.Rank table = Rankings.of(path);
        RelicDef def = Relics.defOf(stack);
        if (def != null) {
            com.newuniverse.nusmp.relics.Grade g = com.newuniverse.nusmp.relics.tier.Tiers.effectiveGrade(stack, def);
            int place = table != null && table.grade() == g ? table.place() : 0;
            return new Rankings.Rank(g, place, 0, "relic");
        }
        if (table != null) return table;
        return new Rankings.Rank(UnifiedTooltipItems.styleOf(stack, null).grade(), 0, 0, "category");
    }

    /** The one rank line ("Rank: God Grade #4"; a Djinn weapon adds its per-hit number), or null for an item of another mod. */
    public static Component line(ItemStack stack) {
        Rankings.Rank r = rankOf(stack);
        if (r == null) return null;
        boolean classic = RankDisplayConfig.classic();
        // the relic card's own rank line: the stars, then the grade name and the place, all in the grade colour
        Component label = Component.literal(classic ? r.label() : r.card());
        if (!classic) {
            if (stack.getItem() instanceof DjinnWeaponItem w) {
                int rank = Rankings.djinnRank(w.djinn().id());
                if (rank > 0) {
                    var l = DamageScaleConfig.limits();
                    String hit = String.format(Locale.ROOT, "%,.0f", DamageScale.djinnHit(rank, Rankings.djinnCount(), l.djinnMin(), l.djinnMax()));
                    return Component.translatableWithFallback("tooltip.anime_multiverse.rank_card_hit", "%s  -  weapon hit %s", label, hit).withStyle(r.grade().colour());
                }
            }
            return Component.empty().append(label).withStyle(r.grade().colour());
        }
        if (stack.getItem() instanceof DjinnWeaponItem w) {
            int rank = Rankings.djinnRank(w.djinn().id());
            if (rank > 0) {
                var l = DamageScaleConfig.limits();
                String hit = String.format(Locale.ROOT, "%,.0f", DamageScale.djinnHit(rank, Rankings.djinnCount(), l.djinnMin(), l.djinnMax()));
                return Component.translatableWithFallback("tooltip.anime_multiverse.rank_hit", "Rank: %s  -  weapon hit %s", label, hit).withStyle(r.grade().colour());
            }
        }
        return Component.translatableWithFallback("tooltip.anime_multiverse.rank", "Rank: %s", label).withStyle(r.grade().colour());
    }

    static void onTooltip(ItemTooltipEvent e) {
        try {
            List<Component> tip = e.getToolTip();
            Component l = line(e.getItemStack());
            if (l != null && !tip.isEmpty()) tip.add(1, l);
        } catch (RuntimeException ignored) {
            // a tooltip line is never worth a crash
        }
    }
}
