package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The one block the Golden City's garden brings of its own: the leaf of its golden trees. Like the Dungeon's other blocks it is protected
 * by the guard where it stands (the garden's trees cannot be cut), but it never decays and it has no state, so a tree of it is a tree for good.
 */
public final class GoldenBlocks {
    private GoldenBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);

    /** A golden leaf: a full block that lets light through its gaps and glows a little, so a tree of them lights the ground under it. */
    public static final DeferredBlock<Block> GOLDEN_LEAVES = BLOCKS.register("golden_leaves", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(0.3f).sound(SoundType.AZALEA_LEAVES).noOcclusion().lightLevel(s -> 7).noLootTable()
                    .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));

    public static final DeferredItem<BlockItem> GOLDEN_LEAVES_ITEM = NUItems.ITEMS.register("golden_leaves", () -> new BlockItem(GOLDEN_LEAVES.get(), new Item.Properties()));

    public static void init() {}
}
