package com.newuniverse.nusmp.elite.forge.craft;

import com.newuniverse.nusmp.elite.forge.ForgeComponents;
import com.newuniverse.nusmp.elite.forge.rules.Category;
import com.newuniverse.nusmp.elite.forge.rules.EngravingRules;
import com.newuniverse.nusmp.elite.forge.rules.Quality;
import com.newuniverse.nusmp.elite.forge.rules.Specialization;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Why: the Engraving Handler of the forge, the Minecraft half of {@link EngravingRules}. It engraves Tensura's engravings (the enchantments of the
 * {@code #tensura:engraving} tag) onto a forged item: the specialization's pool is put in a seeded order, and each id is tried in turn until the wanted number is
 * on the item or the quality's free slots are used up. An id is SKIPPED, never an error, when the installed Tensura does not have it, when it is not in the engraving
 * tag (so only real engravings are ever applied), when the item cannot carry it, or when it is incompatible with an enchantment the item already has or is already on it.
 * The level is the plan's level capped by the enchantment's own maximum. The number the forge has put on the item is counted in {@code nusmp:forge_engraved}, which is
 * what the quality's slots are measured against; engravings an item came with (the mod's {@code WeaponEngravings}) do not use the forge's slots.
 */
public final class EngravingHandler {
    private EngravingHandler() {}

    public static final TagKey<Enchantment> ENGRAVING_TAG = TagKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("tensura", "engraving"));

    /** One engraving that was applied. */
    public record Applied(String id, int level) {}

    /** The ids (path only) of the Tensura enchantments already on the stack. */
    static List<String> present(ItemStack stack) {
        List<String> ids = new ArrayList<>();
        ItemEnchantments ench = stack.getEnchantments();
        for (Holder<Enchantment> h : ench.keySet()) h.unwrapKey().ifPresent(k -> {
            if ("tensura".equals(k.location().getNamespace())) ids.add(k.location().getPath());
        });
        return ids;
    }

    /** How many more engravings the forge may still put on this stack. */
    public static int freeSlots(ItemStack stack, Quality q, Specialization s, Category c) {
        return Math.max(0, EngravingRules.totalSlots(q, s, c) - ForgeComponents.engravedCount(stack));
    }

    /**
     * Engraves up to {@code want} engravings (and no more than the free slots), choosing from the specialization's pool in the order the seed gives.
     * Returns what was applied (possibly empty).
     */
    public static List<Applied> engrave(HolderLookup.Provider registries, ItemStack stack, Quality q, Specialization s, Category c, int want, long seed) {
        List<Applied> done = new ArrayList<>();
        if (stack.isEmpty() || q == null) return done;
        int n = Math.min(Math.max(0, want), freeSlots(stack, q, s, c));
        if (n <= 0) return done;
        HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        int level = EngravingRules.levelFor(q, s);
        for (String id : EngravingRules.order(s, seed, present(stack))) {
            if (done.size() >= n) break;
            Optional<? extends Holder<Enchantment>> h = lookup.get(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("tensura", id)));
            if (h.isEmpty() || !h.get().is(ENGRAVING_TAG)) continue;
            Holder<Enchantment> holder = h.get();
            if (!holder.value().isSupportedItem(stack)) continue;
            if (stack.getEnchantments().getLevel(holder) > 0) continue;
            boolean clash = false;
            for (Holder<Enchantment> other : stack.getEnchantments().keySet()) if (!Enchantment.areCompatible(holder, other)) clash = true;
            if (clash) continue;
            int lv = Math.max(1, Math.min(level, holder.value().getMaxLevel()));
            stack.enchant(holder, lv);
            done.add(new Applied(id, lv));
        }
        if (!done.isEmpty()) stack.set(ForgeComponents.ENGRAVED.get(), ForgeComponents.engravedCount(stack) + done.size());
        return done;
    }
}
