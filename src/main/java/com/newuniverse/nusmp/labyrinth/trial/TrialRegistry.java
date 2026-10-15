package com.newuniverse.nusmp.labyrinth.trial;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Why: the one registry of the trial chambers (1.35.0), in the pattern of {@link com.newuniverse.nusmp.dungeon.DungeonBlocks}: its own
 * {@code DeferredRegister.Blocks}, the mod's first {@code DeferredRegister} of block entity types, and block items on {@code NUItems.ITEMS}
 * so the lead's creative tab line can list them. {@code LabyrinthSystem.init} calls {@link #init} during mod construction, before
 * {@code NUItems.ITEMS} is handed to the bus. The keystone is hard but not unbreakable (50 / 1200: a builder outside the Labyrinth can
 * remove it; inside, the Dungeon Guard holds it anyway). The barrier IS unbreakable, piston-proof and loot-less: see
 * {@link MagicBarrierBlock}. Runes and glyphs are cheap plain blocks with a little light so a player can read a chamber before it seals.
 * The barrier has a block item on purpose: an admin fixing a chamber by hand, or a creative builder making a test room, needs to place one.
 */
public final class TrialRegistry {
    private TrialRegistry() {}

    /** The scoreboard tag every mob a Sealed Archway spawns carries: other systems (loot, the Warden, commands) can tell a trial mob apart. */
    public static final String MOB_TAG = "nusmp_trial";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NUSMP.MODID);

    /** The keystone: dark stone with an eye that opens while the trial runs (light 4 idle, 10 active). */
    public static final DeferredBlock<SealedArchwayBlock> SEALED_ARCHWAY = BLOCKS.register("sealed_archway", () -> new SealedArchwayBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(50.0f, 1200.0f).sound(SoundType.DEEPSLATE_BRICKS)
                    .lightLevel(s -> s.getValue(SealedArchwayBlock.PHASE) == SealedArchwayBlock.Phase.ACTIVE ? 10 : 4)));

    /** The wall of light in a doorway: see {@link MagicBarrierBlock} for every property's reason. */
    public static final DeferredBlock<MagicBarrierBlock> MAGIC_BARRIER = BLOCKS.register("magic_barrier", () -> new MagicBarrierBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).noOcclusion().strength(-1.0f, 3600000.0f).lightLevel(s -> 7).noLootTable()
                    .pushReaction(PushReaction.BLOCK).sound(SoundType.AMETHYST)
                    .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false).isRedstoneConductor((s, l, p) -> false).isValidSpawn((s, l, p, t) -> false)));

    /** The floor tile a barrier column rises from. */
    public static final DeferredBlock<WardRuneBlock> WARD_RUNE = BLOCKS.register("ward_rune", () -> new WardRuneBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3.0f, 9.0f).sound(SoundType.DEEPSLATE_TILES).lightLevel(s -> 3)));

    /** The floor tile a trial mob appears on. */
    public static final DeferredBlock<SpawnGlyphBlock> SPAWN_GLYPH = BLOCKS.register("spawn_glyph", () -> new SpawnGlyphBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0f, 9.0f).sound(SoundType.DEEPSLATE_TILES).lightLevel(s -> 3)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SealedArchwayBlockEntity>> SEALED_ARCHWAY_BE = BLOCK_ENTITIES.register("sealed_archway",
            () -> BlockEntityType.Builder.of(SealedArchwayBlockEntity::new, SEALED_ARCHWAY.get()).build(null));

    public static final DeferredItem<BlockItem> SEALED_ARCHWAY_ITEM = NUItems.ITEMS.register("sealed_archway", () -> new BlockItem(SEALED_ARCHWAY.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> MAGIC_BARRIER_ITEM = NUItems.ITEMS.register("magic_barrier", () -> new BlockItem(MAGIC_BARRIER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> WARD_RUNE_ITEM = NUItems.ITEMS.register("ward_rune", () -> new BlockItem(WARD_RUNE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> SPAWN_GLYPH_ITEM = NUItems.ITEMS.register("spawn_glyph", () -> new BlockItem(SPAWN_GLYPH.get(), new Item.Properties()));

    /** The block items, in the order the creative tab shows them. */
    public static List<DeferredItem<BlockItem>> creativeItems() { return List.of(SEALED_ARCHWAY_ITEM, MAGIC_BARRIER_ITEM, WARD_RUNE_ITEM, SPAWN_GLYPH_ITEM); }

    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
