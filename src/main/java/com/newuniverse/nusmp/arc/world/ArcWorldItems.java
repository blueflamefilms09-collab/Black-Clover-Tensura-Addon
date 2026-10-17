package com.newuniverse.nusmp.arc.world;

import com.newuniverse.nusmp.arc.arena.ChronicleSigilItem;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;

/**
 * Why: the one new item of the places where characters appear is the Chronicle Sigil ({@code nusmp:chronicle_sigil}), and like the Hourglass of the Final Hour it
 * goes on {@code NUItems.ITEMS}, which means it must be declared before that register is handed to the mod bus. Keeping the declaration in its own small class
 * lets {@code ArcWorldSystem.init} touch it (and so run its static initialiser) at a fixed point, and gives the creative tab one list to read. The sigil stacks
 * to one and is Epic, like the hourglass it descends from.
 */
public final class ArcWorldItems {
    private ArcWorldItems() {}

    /** {@code nusmp:chronicle_sigil}: hold at the Chronicle to be carried to the chosen boss's arena, hold in an arena to go home. */
    public static final DeferredItem<ChronicleSigilItem> CHRONICLE_SIGIL = NUItems.ITEMS.register("chronicle_sigil",
            () -> new ChronicleSigilItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    /** Loads this class (and with it the declaration above) at the point {@code ArcWorldSystem.init} chooses. */
    public static void init() {}

    /** The items for the creative tab, in order. */
    public static List<DeferredItem<? extends Item>> creativeItems() { return List.of(CHRONICLE_SIGIL); }
}
