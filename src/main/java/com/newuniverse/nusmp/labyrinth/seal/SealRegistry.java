package com.newuniverse.nusmp.labyrinth.seal;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: everything the Labyrinth's seals register, in one class with its own deferred registers for blocks, block entities and entities,
 * the pattern of {@link com.newuniverse.nusmp.dungeon.DungeonEntities} and {@code ZogratisEntities}: {@code LabyrinthSystem.init} calls
 * {@link #init} once and the mod class never learns the names. Items (the two block items, the Shadow Key, the spawn egg) go on
 * {@link NUItems#ITEMS} like every other item of the mod, which is why this init must run before that register is handed to the bus. The
 * attribute supplier is put on the mod bus here ({@link EntityAttributeCreationEvent}) so the entity type and its attributes live in the
 * same file. Both blocks are unbreakable ({@code strength(-1, 3600000)}) and immovable ({@code PushReaction.BLOCK}): they are the locks of
 * the Spire, and a piston or a pickaxe must not be a way round the key.
 */
public final class SealRegistry {
    private SealRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NUSMP.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    // ---------------------------------------------------------------- blocks
    public static final DeferredBlock<GatekeeperDoorBlock> GATEKEEPER_DOOR = BLOCKS.register("gatekeeper_door", () -> new GatekeeperDoorBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).sound(SoundType.NETHERITE_BLOCK).strength(-1f, 3600000f)
                    .pushReaction(PushReaction.BLOCK).noOcclusion().lightLevel(s -> s.getValue(GatekeeperDoorBlock.OPEN) ? 0 : 6)));

    public static final DeferredBlock<PeakAltarBlock> PEAK_ALTAR = BLOCKS.register("peak_altar", () -> new PeakAltarBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).sound(SoundType.DEEPSLATE_BRICKS).strength(-1f, 3600000f)
                    .pushReaction(PushReaction.BLOCK).noOcclusion().lightLevel(s -> 9)));

    // ---------------------------------------------------------------- items (on the mod's item register)
    public static final DeferredItem<BlockItem> GATEKEEPER_DOOR_ITEM = NUItems.ITEMS.register("gatekeeper_door", () -> new BlockItem(GATEKEEPER_DOOR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> PEAK_ALTAR_ITEM = NUItems.ITEMS.register("peak_altar", () -> new BlockItem(PEAK_ALTAR.get(), new Item.Properties()));
    public static final DeferredItem<ShadowKeyItem> SHADOW_KEY = NUItems.ITEMS.register("shadow_key", ShadowKeyItem::new);

    // ---------------------------------------------------------------- block entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PeakAltarBlockEntity>> PEAK_ALTAR_BE = BLOCK_ENTITIES.register("peak_altar",
            () -> BlockEntityType.Builder.of(PeakAltarBlockEntity::new, PEAK_ALTAR.get()).build(null));

    // ---------------------------------------------------------------- entity
    public static final DeferredHolder<EntityType<?>, EntityType<FloorWardenEntity>> FLOOR_WARDEN = ENTITIES.register("floor_warden",
            () -> EntityType.Builder.<FloorWardenEntity>of(FloorWardenEntity::new, MobCategory.MONSTER).sized(1.4F, 3.2F).fireImmune().clientTrackingRange(10).build("floor_warden"));

    public static final DeferredItem<DeferredSpawnEggItem> FLOOR_WARDEN_EGG = NUItems.ITEMS.register("floor_warden_spawn_egg",
            () -> new DeferredSpawnEggItem(FLOOR_WARDEN, 0x1A1026, 0x8A4DE0, new Item.Properties()));

    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ENTITIES.register(modBus);
        modBus.addListener(SealRegistry::attributes);
    }

    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(FLOOR_WARDEN.get(), FloorWardenEntity.attributes().build());
    }
}
