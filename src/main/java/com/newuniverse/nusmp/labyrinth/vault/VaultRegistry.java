package com.newuniverse.nusmp.labyrinth.vault;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the registry of the Treasure Vault: the Relic Pedestal block, its block item, its block entity type and the {@code nusmp:relic} loot
 * pool entry type. One class with its own DeferredRegisters and a single {@link #init} is the pattern every Labyrinth package follows so
 * {@code LabyrinthSystem} can wire them in order; the block item goes on the mod's shared item register ({@code NUItems.ITEMS}) like every
 * other block item so the creative tab and the lang file see it in the usual place. The loot entry type lives in the vanilla registry
 * {@code Registries.LOOT_POOL_ENTRY_TYPE}: a datapack loot table can then name {@code "type": "nusmp:relic"} and vanilla's loot codec
 * resolves it like {@code minecraft:item}.
 */
public final class VaultRegistry {
    private VaultRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NUSMP.MODID);
    public static final DeferredRegister<LootPoolEntryType> LOOT_ENTRIES = DeferredRegister.create(Registries.LOOT_POOL_ENTRY_TYPE, NUSMP.MODID);

    /**
     * The Relic Pedestal: dark stone with gold trim, one relic floating above it. Hard (50 / 1200, obsidian-like) so it is a deliberate
     * decoration outside the dungeon; inside, the dungeon guard makes it unbreakable. Light 7: the relic glow is a real light source.
     */
    public static final DeferredBlock<RelicPedestalBlock> PEDESTAL = BLOCKS.register("relic_pedestal", () -> new RelicPedestalBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(50f, 1200f).sound(SoundType.DEEPSLATE_BRICKS).noOcclusion().lightLevel(s -> 7)));

    public static final DeferredItem<BlockItem> PEDESTAL_ITEM = NUItems.ITEMS.register("relic_pedestal", () -> new BlockItem(PEDESTAL.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RelicPedestalBlockEntity>> PEDESTAL_BE = BLOCK_ENTITIES.register("relic_pedestal",
            () -> BlockEntityType.Builder.of(RelicPedestalBlockEntity::new, PEDESTAL.get()).build(null));

    /** {@code nusmp:relic}: one unissued relic of a grade range (both halves of a pair). */
    public static final DeferredHolder<LootPoolEntryType, LootPoolEntryType> RELIC_ENTRY = LOOT_ENTRIES.register("relic", () -> new LootPoolEntryType(RelicLootEntry.CODEC));

    /** Registers the three registers on the mod bus (called by LabyrinthSystem). */
    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        LOOT_ENTRIES.register(modBus);
    }
}
