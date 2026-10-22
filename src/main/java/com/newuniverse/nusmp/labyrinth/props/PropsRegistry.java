package com.newuniverse.nusmp.labyrinth.props;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the registration entry point of the dungeon props (1.107.0), the pattern of every package of the Mega-Dungeon: one {@link #init} (mod bus: the weapon rack block, its block item on
 * {@code NUItems.ITEMS}, its block entity, the creative-tab entry, the in-game self-tests) and one {@link #register} (game bus: the handler that turns the armour stands of the templates into
 * racks). Called from {@code MegaSystem}, which runs before {@code NUItems.ITEMS.register}. The rack is a normal breakable block (the Dungeon Guard is what holds it in the dungeon), so a player can
 * also craft and place one at home. The config is {@link PropsConfig#SPEC}.
 */
public final class PropsRegistry {
    private PropsRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredBlock<WeaponRackBlock> RACK = BLOCKS.register("weapon_rack", () -> new WeaponRackBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2.0f, 6.0f).noOcclusion()));

    public static final DeferredItem<BlockItem> RACK_ITEM = NUItems.ITEMS.register("weapon_rack", () -> new BlockItem(RACK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WeaponRackBlockEntity>> RACK_BE = BLOCK_ENTITIES.register("weapon_rack",
            () -> BlockEntityType.Builder.of(WeaponRackBlockEntity::new, RACK.get()).build(null));

    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) e.accept(RACK_ITEM.get());
        });
        PropsSelfTests.register();
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(EventPriority.HIGHEST, (EntityJoinLevelEvent e) -> ArmourStandGate.onJoin(e));          // rooms rebuilt: the template's stands are dropped, the rare stand is dressed
        gameBus.addListener(EventPriority.HIGH, (EntityJoinLevelEvent e) -> RackConverter.onJoin(e));              // legacy rack path: does nothing unless props.legacyRackConversion is on
    }
}
