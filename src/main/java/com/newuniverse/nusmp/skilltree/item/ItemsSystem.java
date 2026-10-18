package com.newuniverse.nusmp.skilltree.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

import java.util.List;

/**
 * Why: the one entry point of the items system: the Skill Book (one item whose component names the book, so a datapack route needs no new item) and the SP Book (raw Skill Points), their use logic, the
 * unlock effects, the Global Loot Modifier that drops them in the Mega-Dungeon, the config {@code nusmp-skilltree-loot-server.toml}, the self-tests {@code tree.items.*}. Owned by the items builder, who
 * replaces this stub. {@link #creativeItems} lists a STACK (the component says which book) per route and per SP variant for the creative tab.
 */
public final class ItemsSystem {
    private ItemsSystem() {}

    public static void init(IEventBus modBus, ModContainer modContainer) {}

    public static List<ItemStack> creativeItems() { return List.of(); }
}
