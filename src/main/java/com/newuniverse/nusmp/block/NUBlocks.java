package com.newuniverse.nusmp.block;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NUBlocks {
    private NUBlocks() {}
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);

    public static final DeferredBlock<GrimoireAltarBlock> GRIMOIRE_ALTAR = BLOCKS.register("grimoire_altar",
            () -> new GrimoireAltarBlock(BlockBehaviour.Properties.of()));
    public static final DeferredItem<BlockItem> GRIMOIRE_ALTAR_ITEM = NUItems.ITEMS.register("grimoire_altar",
            () -> new BlockItem(GRIMOIRE_ALTAR.get(), new Item.Properties()));

    /** Infinity Stones Phase B: the altar of Vormir (the Soul Stone's trial; creative / operators place it). */
    public static final DeferredBlock<com.newuniverse.nusmp.stones.VormirAltarBlock> VORMIR_ALTAR = BLOCKS.register("vormir_altar",
            () -> new com.newuniverse.nusmp.stones.VormirAltarBlock(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.COLOR_ORANGE)));
    public static final DeferredItem<BlockItem> VORMIR_ALTAR_ITEM = NUItems.ITEMS.register("vormir_altar",
            () -> new BlockItem(VORMIR_ALTAR.get(), new Item.Properties()));

    /** 0.47: Kotodama's underworld matter (temporary; see book.UnderworldMatter). No item. */
    public static final DeferredBlock<UnderworldMatterBlock> UNDERWORLD_MATTER = BLOCKS.register("underworld_matter", UnderworldMatterBlock::new);

    /** Touch the class so its entries are added before the registers are attached. */
    public static void init() {}
}
