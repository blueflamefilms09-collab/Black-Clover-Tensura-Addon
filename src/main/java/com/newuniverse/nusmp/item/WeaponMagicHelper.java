package com.newuniverse.nusmp.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 0.56: "what kind of weapon is in the hand" for spells that need a particular weapon type (data/nusmp/tags/item/*.json: katanas,
 * swords, greatswords, dance_weapons). Datapacks and other mods can add their weapons to the tags; the tag entries of items that
 * do not exist are optional, so a missing id never breaks loading.
 */
public final class WeaponMagicHelper {
    private WeaponMagicHelper() {}

    public static final TagKey<Item> KATANAS = tag("katanas"), SWORDS = tag("swords"), GREATSWORDS = tag("greatswords"), DANCE_WEAPONS = tag("dance_weapons");

    private static TagKey<Item> tag(String name) { return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("nusmp", name)); }

    public static boolean is(ItemStack stack, TagKey<Item> tag) { return !stack.isEmpty() && stack.is(tag); }

    /** True when the main hand holds an item of the tag. */
    public static boolean holds(LivingEntity e, TagKey<Item> tag) { return is(e.getMainHandItem(), tag); }

    /** True when either hand holds an item of the tag. */
    public static boolean holdsAny(LivingEntity e, TagKey<Item> tag) { return is(e.getMainHandItem(), tag) || is(e.getOffhandItem(), tag); }
}
