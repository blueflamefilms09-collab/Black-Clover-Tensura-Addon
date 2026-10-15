package com.newuniverse.nusmp.labyrinth;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Why: the Labyrinth masonry. The gate ruins and every spire piece are built from a signature palette of our own blocks instead of
 * vanilla deepslate so that (1) the biome palette processor ({@code nusmp:biome_palette}, processor list {@code nusmp:labyrinth_palette})
 * can swap "the pillar", "the floor", "the accent" by TAG without touching the player's own deepslate builds next to a gate, (2) the vein
 * processor can target "structural stone" ({@code #nusmp:labyrinth/veinable}) without lighting up every deepslate cave, and (3) the
 * structures read as one place from the Overworld arch to the Boss Peak. They are plain {@link Block}s with deepslate-brick numbers
 * (3.5 / 6, pickaxe, {@link SoundType#DEEPSLATE_BRICKS}) so they behave like masonry a player can quarry and reuse; the light-emitting
 * ones ({@code labyrinth_glyph} 6, {@code labyrinth_lantern} 15, {@code magicule_vein} 11) use {@code lightLevel} on the state so no
 * block entity or tick is involved. Block items go on {@link NUItems#ITEMS} like every other block of the mod, which is why
 * {@link #init(IEventBus)} must run BEFORE {@code NUItems.ITEMS.register(modEventBus)} (the item register is a one-shot event; entries
 * added after it are silently lost).
 */
public final class LabyrinthBlocks {
    private LabyrinthBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);

    private static BlockBehaviour.Properties masonry() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3.5f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS);
    }

    /** The wall stone of the Labyrinth: dark blue-black blocks with faint gold seams. */
    public static final DeferredBlock<Block> LABYRINTH_BRICKS = BLOCKS.register("labyrinth_bricks", () -> new Block(masonry()));

    /** The floor stone: smaller tiles of the same rock (swap_floor: the palette processor replaces it per biome). */
    public static final DeferredBlock<Block> LABYRINTH_TILES = BLOCKS.register("labyrinth_tiles", () -> new Block(masonry()));

    /** The column stone: fluted sides, a capped top, rotates on its axis like a log (swap_pillar). */
    public static final DeferredBlock<RotatedPillarBlock> LABYRINTH_PILLAR = BLOCKS.register("labyrinth_pillar", () -> new RotatedPillarBlock(masonry()));

    /** An accent block with a glowing cyan sigil: the marker stone of the builders (swap_accent). Light 6, enough to read the sigil. */
    public static final DeferredBlock<Block> LABYRINTH_GLYPH = BLOCKS.register("labyrinth_glyph", () -> new Block(masonry().lightLevel(s -> 6)));

    /** The lantern of the Labyrinth: a caged mana flame set in the same stone. Light 15. */
    public static final DeferredBlock<Block> LABYRINTH_LANTERN = BLOCKS.register("labyrinth_lantern", () -> new Block(masonry().lightLevel(s -> 15)));

    /** Dark stone shot through with bright magicule veins; the vein processor places it more densely toward the Boss Peak. Light 11. */
    public static final DeferredBlock<Block> MAGICULE_VEIN = BLOCKS.register("magicule_vein", () -> new Block(masonry().lightLevel(s -> 11)));

    public static final DeferredItem<BlockItem> LABYRINTH_BRICKS_ITEM = NUItems.ITEMS.register("labyrinth_bricks", () -> new BlockItem(LABYRINTH_BRICKS.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> LABYRINTH_TILES_ITEM = NUItems.ITEMS.register("labyrinth_tiles", () -> new BlockItem(LABYRINTH_TILES.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> LABYRINTH_PILLAR_ITEM = NUItems.ITEMS.register("labyrinth_pillar", () -> new BlockItem(LABYRINTH_PILLAR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> LABYRINTH_GLYPH_ITEM = NUItems.ITEMS.register("labyrinth_glyph", () -> new BlockItem(LABYRINTH_GLYPH.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> LABYRINTH_LANTERN_ITEM = NUItems.ITEMS.register("labyrinth_lantern", () -> new BlockItem(LABYRINTH_LANTERN.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> MAGICULE_VEIN_ITEM = NUItems.ITEMS.register("magicule_vein", () -> new BlockItem(MAGICULE_VEIN.get(), new Item.Properties()));

    /** The block items in creative-tab order (NUCreativeTab adds them with one loop). */
    public static List<DeferredItem<BlockItem>> items() {
        return List.of(LABYRINTH_BRICKS_ITEM, LABYRINTH_TILES_ITEM, LABYRINTH_PILLAR_ITEM, LABYRINTH_GLYPH_ITEM, LABYRINTH_LANTERN_ITEM, MAGICULE_VEIN_ITEM);
    }

    /** Registers the block register on the mod bus. Call before {@code NUItems.ITEMS.register(modEventBus)}: loading this class is what
     *  puts the block items into NUItems.ITEMS. */
    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
