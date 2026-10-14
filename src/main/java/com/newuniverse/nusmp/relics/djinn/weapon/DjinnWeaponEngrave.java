package com.newuniverse.nusmp.relics.djinn.weapon;

import com.newuniverse.nusmp.relics.djinn.Djinn;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Optional;

/**
 * Engraves a Djinn weapon with its Tensura engravings (CLAUDE.md rule 6): the same method as {@code item.WeaponEngravings.engrave} (an engraving is a
 * Tensura enchantment; one Tensura does not have is skipped; the stack is marked {@code NUEngraved} so it is done once), with the Djinn weapons' own
 * table ({@link DjinnWeaponRules#engravings}) because they are made by the Djinn engine, not looted or crafted.
 */
public final class DjinnWeaponEngrave {
    private DjinnWeaponEngrave() {}

    /** Engraves a stack of a Djinn weapon once; true if it changed. */
    public static boolean engrave(ItemStack stack, Djinn djinn, boolean dark, HolderLookup.Provider registries) {
        if (stack.isEmpty() || djinn == null) return false;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && cd.copyTag().getBoolean("NUEngraved")) return false;
        HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        for (DjinnWeaponRules.Engraving g : DjinnWeaponRules.engravings(djinn, dark)) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("tensura", g.id());
            Optional<? extends Holder<Enchantment>> h = lookup.get(ResourceKey.create(Registries.ENCHANTMENT, id));
            if (h.isEmpty()) continue;
            int level = Math.max(1, Math.min(g.level(), h.get().value().getMaxLevel()));
            if (stack.getEnchantments().getLevel(h.get()) < level) stack.enchant(h.get(), level);
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, (CompoundTag t) -> t.putBoolean("NUEngraved", true));
        return true;
    }
}
