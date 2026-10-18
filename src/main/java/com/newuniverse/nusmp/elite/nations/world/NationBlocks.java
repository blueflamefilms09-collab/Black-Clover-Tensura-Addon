package com.newuniverse.nusmp.elite.nations.world;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the two blocks of the Nations system in their own registers (so the system adds nothing to the shared registries and the order against {@code NUItems.ITEMS} does not matter): the NATION BANNER
 * ({@code nusmp:nation_banner}, the capital of a nation, placed by its leader) and the CONTRACT BOARD ({@code nusmp:contract_board}). Neither has a block entity: the banner is found by position in the saved
 * data, the board shows the player's own nation. Both open the nations screen on right click.
 */
public final class NationBlocks {
    private NationBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);

    public static final DeferredBlock<NationBannerBlock> BANNER = BLOCKS.register("nation_banner", () -> new NationBannerBlock(
            BlockBehaviour.Properties.of().strength(5.0f, 3_600_000.0f).sound(SoundType.WOOD).noOcclusion().lightLevel(s -> 8)));
    public static final DeferredBlock<ContractBoardBlock> BOARD = BLOCKS.register("contract_board", () -> new ContractBoardBlock(
            BlockBehaviour.Properties.of().strength(2.5f, 6.0f).sound(SoundType.WOOD)));

    public static final DeferredItem<BlockItem> BANNER_ITEM = ITEMS.register("nation_banner", () -> new BlockItem(BANNER.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BlockItem> BOARD_ITEM = ITEMS.register("contract_board", () -> new BlockItem(BOARD.get(), new Item.Properties()));

    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
