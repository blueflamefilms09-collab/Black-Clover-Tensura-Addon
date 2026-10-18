package com.newuniverse.nusmp.elite.world;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.elite.world.item.CalamityCoreItem;
import com.newuniverse.nusmp.elite.world.item.MagiculeGaugeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the items of the world engine, in a register of their own: the block items of the five crystal stages, the Magicule Shard (what crystals drop), the Magicule Gauge (the detector) and
 * the Calamity Core. The creative tab entries are added to the mod's main tab by {@link WorldEvents} so the lead does not have to edit {@code NUCreativeTab}.
 */
public final class WorldItems {
    private WorldItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);

    public static final DeferredItem<BlockItem> BUD_SMALL = ITEMS.registerSimpleBlockItem("magic_bud_small", WorldBlocks.BUD_SMALL);
    public static final DeferredItem<BlockItem> BUD_MEDIUM = ITEMS.registerSimpleBlockItem("magic_bud_medium", WorldBlocks.BUD_MEDIUM);
    public static final DeferredItem<BlockItem> BUD_LARGE = ITEMS.registerSimpleBlockItem("magic_bud_large", WorldBlocks.BUD_LARGE);
    public static final DeferredItem<BlockItem> CLUSTER = ITEMS.registerSimpleBlockItem("magic_crystal_cluster", WorldBlocks.CLUSTER);
    public static final DeferredItem<BlockItem> MOTHER_ROCK = ITEMS.registerSimpleBlockItem("mother_rock", WorldBlocks.MOTHER_ROCK, new Item.Properties().rarity(Rarity.RARE));

    public static final DeferredItem<Item> MAGICULE_SHARD = ITEMS.registerSimpleItem("magicule_shard", new Item.Properties());
    public static final DeferredItem<MagiculeGaugeItem> MAGICULE_GAUGE = ITEMS.register("magicule_gauge", () -> new MagiculeGaugeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<CalamityCoreItem> CALAMITY_CORE = ITEMS.register("calamity_core", () -> new CalamityCoreItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)));

    /** Everything that belongs in the creative tab, in a fixed order. */
    public static List<Item> creative() {
        List<Item> l = new ArrayList<>();
        l.add(MAGICULE_GAUGE.get());
        l.add(MAGICULE_SHARD.get());
        l.add(BUD_SMALL.get());
        l.add(BUD_MEDIUM.get());
        l.add(BUD_LARGE.get());
        l.add(CLUSTER.get());
        l.add(MOTHER_ROCK.get());
        return l;
    }
}
