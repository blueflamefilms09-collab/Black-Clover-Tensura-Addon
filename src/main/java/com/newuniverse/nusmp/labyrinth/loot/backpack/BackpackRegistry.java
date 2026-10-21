package com.newuniverse.nusmp.labyrinth.loot.backpack;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: everything the backpack registers, in one class with its own deferred registers (the pattern of the other packages of the Labyrinth), so the loot system's {@code init} calls
 * {@link #init} once and no shared registry has to learn the names: the block (breakable by hand, so a player who placed one can take it back, and so the dungeon guard's
 * placed-block exemption has a block to exempt), its block entity, the menu type, the data component {@code nusmp:backpack_tier} (the tier a backpack ITEM is; its contents ride
 * in the vanilla {@code minecraft:container} component, so a backpack picked up or dropped carries them), the item, the four upgrade items, and the crafting recipe that applies an
 * upgrade to a backpack while keeping its contents. The items are on this class's own item register, attached to the mod bus by {@link #init}.
 */
public final class BackpackRegistry {
    private BackpackRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NUSMP.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, NUSMP.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, NUSMP.MODID);

    /** {@code nusmp:backpack_tier}: the tier (0 leather .. 4 netherite) of a backpack item. A stack without it is a leather backpack. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TIER = COMPONENTS.registerComponentType("backpack_tier",
            b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Soft, hand-breakable, and a little sturdier than a flower pot: a backpack is leather and cloth, not stone. */
    public static final DeferredBlock<BackpackBlock> BLOCK = BLOCKS.register("backpack", () -> new BackpackBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.8f).sound(SoundType.WOOL).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BackpackBlockEntity>> BLOCK_ENTITY = BLOCK_ENTITIES.register("backpack",
            () -> BlockEntityType.Builder.of(BackpackBlockEntity::new, BLOCK.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<BackpackMenu>> MENU = MENUS.register("backpack",
            () -> IMenuTypeExtension.create((id, inv, buf) -> BackpackMenu.client(id, inv, buf)));

    public static final DeferredItem<BackpackItem> ITEM = ITEMS.register("backpack", () -> new BackpackItem(BLOCK.get(), new Item.Properties().stacksTo(1)));

    private static final List<DeferredItem<BackpackUpgradeItem>> UPGRADES = new ArrayList<>();

    static {
        for (BackpackTier t : BackpackTier.values()) {
            if (t.upgradeItemId() == null) continue;
            UPGRADES.add(ITEMS.register(t.upgradeItemId(), () -> new BackpackUpgradeItem(new Item.Properties().stacksTo(16), t)));
        }
    }

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BackpackUpgradeRecipe>> UPGRADE_RECIPE = SERIALIZERS.register("backpack_upgrade",
            () -> new SimpleCraftingRecipeSerializer<>(BackpackUpgradeRecipe::new));

    /** The upgrade items, iron first. */
    public static List<DeferredItem<BackpackUpgradeItem>> upgrades() { return UPGRADES; }

    /** The upgrade item of a tier, or null for leather. */
    public static Item upgradeItem(BackpackTier tier) {
        for (DeferredItem<BackpackUpgradeItem> u : UPGRADES) if (u.isBound() && u.get().tier() == tier) return u.get();
        return null;
    }

    /** Call from the loot system's {@code init} (mod bus, before the registries freeze). */
    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        COMPONENTS.register(modBus);
        SERIALIZERS.register(modBus);
        BackpackSelfTests.register();
    }
}
