package com.newuniverse.nusmp.relics;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Optional;

/**
 * Tensura engravings for the relics (CLAUDE.md rule 6): the {@link RelicDef#engravings()} of a def are enchantments of the #tensura:engraving tag,
 * put on the stack once, the first time it sits in a player's inventory. Same marker as {@code item.WeaponEngravings} ("NUEngraved"), so an item that
 * table already engraved is not done twice. An id may carry a level ("severance@2"); without one the level follows the grade (1 up to RARE, 2 at
 * UNIQUE and LEGEND, 3 from GOD) and is capped at the engraving's own maximum. An id Tensura does not have is skipped.
 */
public final class RelicEngravings {
    private RelicEngravings() {}

    private static final String FLAG = "NUEngraved";

    /** Engraves the stack if it is a relic with engravings that has not been engraved yet; true if it changed. */
    public static boolean engrave(ItemStack stack, HolderLookup.Provider registries) {
        RelicDef def = Relics.defOf(stack);
        if (def == null || def.engravings().isEmpty()) return false;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && cd.copyTag().getBoolean(FLAG)) return false;
        HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        for (String spec : def.engravings()) {
            String id = spec;
            int level = defaultLevel(def.grade());
            int at = spec.indexOf('@');
            if (at > 0) {
                id = spec.substring(0, at);
                try { level = Integer.parseInt(spec.substring(at + 1).trim()); } catch (NumberFormatException ignored) { }
            }
            ResourceLocation rl = id.contains(":") ? ResourceLocation.parse(id) : ResourceLocation.fromNamespaceAndPath("tensura", id);
            Optional<? extends Holder<Enchantment>> h = lookup.get(ResourceKey.create(Registries.ENCHANTMENT, rl));
            if (h.isEmpty()) continue;
            int lv = Math.max(1, Math.min(level, h.get().value().getMaxLevel()));
            if (stack.getEnchantments().getLevel(h.get()) < lv) stack.enchant(h.get(), lv);
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, (CompoundTag t) -> t.putBoolean(FLAG, true));
        return true;
    }

    static int defaultLevel(Grade g) {
        if (g.atLeast(Grade.GOD)) return 3;
        if (g.atLeast(Grade.UNIQUE)) return 2;
        return 1;
    }

    /** Once a second per player (staggered): engrave any relic in the inventory that is not yet. */
    static void engraveInventory(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) engrave(s, p.registryAccess());
        }
    }
}
