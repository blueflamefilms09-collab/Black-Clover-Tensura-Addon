package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: everything the guard builder of the Mega-Dungeon registers, in one class with its own deferred registers (the pattern of
 * {@code SealRegistry}), so {@code MegaSystem.init} calls {@link #init} once and the mod class never learns the names: the Guardian Altar
 * block (unbreakable and immovable like every block of the Labyrinth: {@code strength(-1, 3600000)} and {@code PushReaction.BLOCK}, so a
 * pickaxe or a piston is no way round the boss), its block entity, the Labyrinth Guardian entity with its attributes, the Mana Core item
 * and the {@link SectorCore} data component that makes one core the key of one sector. The two items go on {@link NUItems#ITEMS} like every
 * other item of the mod, which is why {@code init} must run before that register is handed to the bus ({@code LabyrinthSystem} guarantees
 * it). The attribute supplier is put on the mod bus here so the entity type and its attributes live in one file. The game bus gets ONE
 * handler: a Mana Core that enters the world as an item entity (dropped by a dying player, thrown, forged) is made indestructible at once,
 * so a party can never lose the key to the next sector in the first tick of its fall.
 */
public final class GuardRegistry {
    private GuardRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NUSMP.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    // ---------------------------------------------------------------- the data component
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SectorCore>> SECTOR_CORE = COMPONENTS.registerComponentType("sector_core",
            b -> b.persistent(SectorCore.CODEC).networkSynchronized(SectorCore.STREAM_CODEC));

    // ---------------------------------------------------------------- the altar
    public static final DeferredBlock<GuardianAltarBlock> GUARDIAN_ALTAR = BLOCKS.register("guardian_altar", () -> new GuardianAltarBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).sound(SoundType.DEEPSLATE_BRICKS).strength(-1f, 3600000f)
                    .pushReaction(PushReaction.BLOCK).noOcclusion().lightLevel(s -> 12)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GuardianAltarBlockEntity>> ALTAR_BE = BLOCK_ENTITIES.register("guardian_altar",
            () -> BlockEntityType.Builder.of(GuardianAltarBlockEntity::new, GUARDIAN_ALTAR.get()).build(null));

    // ---------------------------------------------------------------- items (on the mod's item register)
    public static final DeferredItem<BlockItem> GUARDIAN_ALTAR_ITEM = NUItems.ITEMS.register("guardian_altar", () -> new BlockItem(GUARDIAN_ALTAR.get(), new Item.Properties()));
    public static final DeferredItem<ManaCoreItem> MANA_CORE = NUItems.ITEMS.register("mana_core", ManaCoreItem::new);

    // ---------------------------------------------------------------- the Guardian (sized like the Floor Warden; the SCALE attribute grows it with the threat)
    public static final DeferredHolder<EntityType<?>, EntityType<LabyrinthGuardianEntity>> GUARDIAN = ENTITIES.register("labyrinth_guardian",
            () -> EntityType.Builder.<LabyrinthGuardianEntity>of(LabyrinthGuardianEntity::new, MobCategory.MONSTER).sized(1.4F, 3.2F).clientTrackingRange(10).build("labyrinth_guardian"));

    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        modBus.addListener(GuardRegistry::attributes);
        GuardSelfTests.register();                                                       // the in-game self-tests (/multiverse mega selftest guard.*)
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(GuardRegistry::onEntityJoin);
        DndEvents.register(gameBus);                                                       // 1.65.0: the D&D layer's listeners
    }

    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(GUARDIAN.get(), LabyrinthGuardianEntity.guardianAttributes().build());
    }

    /** A Mana Core item entity is indestructible from the moment it exists (its own per-tick hook keeps it so). */
    public static void onEntityJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        try {
            if (e.getEntity() instanceof ItemEntity item && item.getItem().getItem() instanceof ManaCoreItem) ManaCoreItem.protect(item);
        } catch (RuntimeException ex) {
            GuardLog.failed("core join", ex);
        }
    }
}
