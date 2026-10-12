package com.newuniverse.nusmp.dungeon.nexus.nav;

import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The Dungeon Delving Gear and the first organ (docs/living_dungeon.md sections 3 and 6): the Ki-Resonance Lantern, the Anti-Bird Compass
 * and the Venom Gland, registered with the mod's other items (ids fixed by the Nexus contract). Their 3D models and sprites are the
 * relic-art pass's; the Venom Gland's flat texture is drawn by tools/gen_nexus_nav_assets.py.
 */
public final class NavItems {
    private NavItems() {}

    public static final DeferredItem<Item> KI_RESONANCE_LANTERN = NUItems.ITEMS.register("ki_resonance_lantern",
            () -> new LanternItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> ANTI_BIRD_COMPASS = NUItems.ITEMS.register("anti_bird_compass",
            () -> new AntiBirdCompassItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final DeferredItem<Item> VENOM_GLAND = NUItems.ITEMS.register("venom_gland",
            () -> new VenomGlandItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static void init() {}
}
