package com.newuniverse.nusmp.labyrinth.trap;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Why: the one registry of the Labyrinth's traps and puzzles (1.36.0), in the pattern of the trial and seal registries: its own
 * {@code DeferredRegister.Blocks} and block-entity register, block items on {@code NUItems.ITEMS} (so {@code LabyrinthSystem.init} must call
 * {@link #init} before that register is handed to the bus) and a {@link #register(IEventBus)} for the one game-bus handler. Nothing here is
 * unbreakable at the property level (the Dungeon Guard holds the Labyrinth already, and outside it the pieces are ordinary hard blocks an
 * admin can fix by hand), which differs from the seals on purpose: a pitfall tile that could not be removed would make a test room
 * impossible to clean up. The Sigil Plate is one block with a {@code color} property, so there is one block item that places colour 0; the
 * {@link #sigilPlateStack(int)} helper builds the item stack of any colour by the vanilla {@code minecraft:block_state} data component
 * (the lead's creative tab can list all four with it); creative players can also cycle the colour by sneak-clicking a placed plate.
 */
public final class TrapRegistry {
    private TrapRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NUSMP.MODID);

    /** A floor tile that gives way under a player (or, {@code safe=true}, an ordinary floor that looks the same). noOcclusion: the open hole must not hide its neighbours' faces. */
    public static final DeferredBlock<PitfallTileBlock> PITFALL_TILE = BLOCKS.register("pitfall_tile", () -> new PitfallTileBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3.0f, 9.0f).sound(SoundType.DEEPSLATE_TILES).noOcclusion()));

    /** The glyph that snuffs light: dark violet, gives no light of its own, randomly ticking as the safety net of its scheduled tick. */
    public static final DeferredBlock<SnufferGlyphBlock> SNUFFER_GLYPH = BLOCKS.register("snuffer_glyph", () -> new SnufferGlyphBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.0f, 9.0f).sound(SoundType.DEEPSLATE_TILES).randomTicks()));

    /** A coloured floor plate pressed by right click; it glows a little more once pressed. */
    public static final DeferredBlock<SigilPlateBlock> SIGIL_PLATE = BLOCKS.register("sigil_plate", () -> new SigilPlateBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0f, 9.0f).sound(SoundType.DEEPSLATE_TILES).noOcclusion()
                    .lightLevel(s -> s.getValue(SigilPlateBlock.LIT) ? 9 : 2)));

    /** The stone that states the riddle and remembers the progress. */
    public static final DeferredBlock<RiddleStoneBlock> RIDDLE_STONE = BLOCKS.register("riddle_stone", () -> new RiddleStoneBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(50.0f, 1200.0f).sound(SoundType.DEEPSLATE_BRICKS).lightLevel(s -> 5)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RiddleStoneBlockEntity>> RIDDLE_STONE_BE = BLOCK_ENTITIES.register("riddle_stone",
            () -> BlockEntityType.Builder.of(RiddleStoneBlockEntity::new, RIDDLE_STONE.get()).build(null));

    public static final DeferredItem<BlockItem> PITFALL_TILE_ITEM = NUItems.ITEMS.register("pitfall_tile", () -> new BlockItem(PITFALL_TILE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> SNUFFER_GLYPH_ITEM = NUItems.ITEMS.register("snuffer_glyph", () -> new BlockItem(SNUFFER_GLYPH.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> SIGIL_PLATE_ITEM = NUItems.ITEMS.register("sigil_plate", () -> new BlockItem(SIGIL_PLATE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> RIDDLE_STONE_ITEM = NUItems.ITEMS.register("riddle_stone", () -> new BlockItem(RIDDLE_STONE.get(), new Item.Properties()));

    /** The block items, in the order the creative tab shows them. */
    public static List<DeferredItem<BlockItem>> creativeItems() { return List.of(PITFALL_TILE_ITEM, SNUFFER_GLYPH_ITEM, SIGIL_PLATE_ITEM, RIDDLE_STONE_ITEM); }

    /** A Sigil Plate item that places the given colour (0..3, clamped): vanilla's block-state data component carries the property. */
    public static ItemStack sigilPlateStack(int colour) {
        ItemStack s = new ItemStack(SIGIL_PLATE_ITEM.get());
        s.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(SigilPlateBlock.COLOR, Math.max(0, Math.min(PuzzleState.COLOURS - 1, colour))));
        return s;
    }

    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

    /** The game-bus handlers (the snuffer glyph's refusal to place a light): call once from {@code LabyrinthSystem}. */
    public static void register(IEventBus gameBus) { TrapEvents.register(gameBus); }
}
