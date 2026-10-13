package com.newuniverse.nusmp.dungeon.nexus.relic;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The blocks of the Treasury: the relic pedestal and the mana-locked gate. Both are unbreakable and drop nothing. */
public final class RelicBlocks {
    private RelicBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);

    public static final DeferredBlock<Block> ARTIFACT_BLOCK = BLOCKS.register("artifact_block", () -> new ArtifactBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1f, 3600000f).sound(SoundType.DEEPSLATE_BRICKS).noOcclusion()
                    .lightLevel(s -> s.getValue(ArtifactBlock.TAKEN) ? 3 : s.getValue(ArtifactBlock.CENTRAL) ? 13 : 9).noLootTable().pushReaction(PushReaction.BLOCK)));

    public static final DeferredBlock<Block> MANA_GATE = BLOCKS.register("mana_gate", () -> new ManaGateBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1f, 3600000f).sound(SoundType.AMETHYST).noOcclusion().randomTicks()
                    .lightLevel(s -> s.getValue(ManaGateBlock.OPEN) ? 4 : 9).noLootTable().pushReaction(PushReaction.BLOCK)
                    .isViewBlocking((s, l, p) -> !s.getValue(ManaGateBlock.OPEN)).isSuffocating((s, l, p) -> !s.getValue(ManaGateBlock.OPEN))));

    public static final DeferredItem<BlockItem> ARTIFACT_BLOCK_ITEM = NUItems.ITEMS.register("artifact_block", () -> new BlockItem(ARTIFACT_BLOCK.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> MANA_GATE_ITEM = NUItems.ITEMS.register("mana_gate", () -> new BlockItem(MANA_GATE.get(), new Item.Properties().rarity(Rarity.RARE)));

    public static void init() {}
}
