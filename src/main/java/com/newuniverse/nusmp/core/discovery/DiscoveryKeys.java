package com.newuniverse.nusmp.core.discovery;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** The discovery keys of the game's own objects (an item, an entity type), built once per object and cached. Safe on both sides. */
public final class DiscoveryKeys {
    private DiscoveryKeys() {}

    private static final Map<Item, String> ITEMS = new ConcurrentHashMap<>();
    private static final String NONE = "";

    /** The key of an item, or null if the item is not tracked (another mod's, or air). */
    public static String item(Item item) {
        if (item == null) return null;
        String k = ITEMS.computeIfAbsent(item, DiscoveryKeys::buildItem);
        return k.isEmpty() ? null : k;
    }

    public static String item(ItemStack stack) { return stack == null || stack.isEmpty() ? null : item(stack.getItem()); }

    /** The key of a registered item by its path in this mod ("heart_of_words"). */
    public static String modItem(String path) { return DiscoveryRules.itemKey("nusmp", path); }

    private static String buildItem(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null || !DiscoveryRules.tracksNamespace(id.getNamespace())) return NONE;
        return DiscoveryRules.itemKey(id.getNamespace(), id.getPath());
    }

    /** The key of an entity type, or null if it is not tracked. */
    public static String mob(EntityType<?> type) {
        if (type == null) return null;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id == null || !DiscoveryRules.tracksNamespace(id.getNamespace())) return null;
        return DiscoveryRules.mobKey(id.getNamespace(), id.getPath());
    }

    /** The key of a skill by its registry id, or null if it is not tracked. */
    public static String skill(ResourceLocation id) {
        if (id == null || !DiscoveryRules.tracksNamespace(id.getNamespace())) return null;
        return DiscoveryRules.skillKey(id.getNamespace(), id.getPath());
    }
}
