package com.newuniverse.nusmp.client.skilltree;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: a route and a node name their icon as an item id in the JSON ({@code minecraft:blaze_powder}), which keeps the data free of textures and lets a datapack use any item of any
 * mod, but the screen draws {@code ItemStack}s, and it draws them every frame. Resolving an id walks the item registry, so it is done once per id and remembered; an id that does not
 * resolve (a typo in a datapack, an item of a mod that is not installed on this client) becomes a book instead of a crash or an invisible node, because a tree that opens with one
 * odd icon is still a tree. Client thread only, like every screen call, so the plain map needs no locking.
 */
final class ItemIcons {
    private ItemIcons() {}

    private static final Map<String, ItemStack> CACHE = new HashMap<>();

    /** The stack to draw for an item id; {@code fallbackId} is tried first when {@code id} is empty (a node without an icon uses its route's icon). */
    static ItemStack of(String id, String fallbackId) {
        String key = id == null || id.isEmpty() ? (fallbackId == null ? "" : fallbackId) : id;
        return CACHE.computeIfAbsent(key, ItemIcons::resolve);
    }

    private static ItemStack resolve(String id) {
        ResourceLocation rl = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        if (rl != null) {
            Item item = BuiltInRegistries.ITEM.get(rl);
            if (item != Items.AIR) return new ItemStack(item);
        }
        return new ItemStack(Items.BOOK);
    }

    /** Forgets every remembered stack (the player left the world: the next world may have other items). */
    static void clear() { CACHE.clear(); }
}
