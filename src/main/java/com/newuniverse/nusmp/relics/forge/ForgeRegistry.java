package com.newuniverse.nusmp.relics.forge;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the Relic Forge needs a block, its item and a menu type, and keeping them in their own DeferredRegisters (the way the Labyrinth and the Dungeon
 * do) means this feature adds nothing to a shared registry class and can be wired, or left out, by one {@link #init} call. The block item goes on
 * {@code NUItems.ITEMS} so it sits with the mod's other items and the creative tab; that register is filled the moment this class loads, so the
 * constructor must call {@link #init} BEFORE {@code NUItems.ITEMS.register(modBus)}. The menu is created with NeoForge's {@code IMenuTypeExtension}
 * because the client half is built from the network buffer (this menu reads nothing from it: it holds no position, the server owns the contents).
 */
public final class ForgeRegistry {
    private ForgeRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NUSMP.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, NUSMP.MODID);

    /** The table. Iron-hard so it takes a pickaxe, and it lights a little: its rune glows. */
    public static final DeferredBlock<RelicForgeBlock> RELIC_FORGE = BLOCKS.register("relic_forge", () -> new RelicForgeBlock(
            BlockBehaviour.Properties.of().strength(3.5f, 12.0f).requiresCorrectToolForDrops().sound(SoundType.ANVIL).lightLevel(s -> 7)));

    public static final DeferredItem<BlockItem> RELIC_FORGE_ITEM = NUItems.ITEMS.register("relic_forge",
            () -> new BlockItem(RELIC_FORGE.get(), new Item.Properties()));

    public static final DeferredHolder<MenuType<?>, MenuType<RelicForgeMenu>> MENU = MENUS.register("relic_forge",
            () -> IMenuTypeExtension.create((id, inv, buf) -> RelicForgeMenu.client(id, inv)));

    /** Call from the mod constructor before {@code NUItems.ITEMS.register(modBus)}. */
    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        MENUS.register(modBus);
    }
}
