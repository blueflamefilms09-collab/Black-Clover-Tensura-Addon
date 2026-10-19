package com.newuniverse.nusmp.skilltree.item;

import com.newuniverse.nusmp.skilltree.loot.BossBookDrops;
import com.newuniverse.nusmp.skilltree.loot.LinesLoot;
import com.newuniverse.nusmp.skilltree.loot.LootConfig;
import com.newuniverse.nusmp.skilltree.loot.SkillBookLootModifier;
import com.newuniverse.nusmp.skilltree.unique.UniqueSkills;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Skill Book, the legacy SP Book, the loot modifiers and the unlock show are one small system that must start exactly once and in one order, and the book system should have ONE line for all of it.
 * This is that line: the two items and the three data components, the loot modifier type {@code nusmp:skill_books} and the rift chests' modifier {@code nusmp:rift_books}, the packet of the banner, the loot
 * config file, the unlock show, and the Guardian's drop (the Guardian rolls no loot table, so its books come from its death). Safe to call twice.
 */
public final class ItemsSystem {
    private ItemsSystem() {}

    private static boolean done;

    /** Starts the items system; called once from {@code SkillBookSystem.init}. */
    public static synchronized void init(IEventBus modBus, ModContainer modContainer) {
        if (done) return;
        done = true;
        SkillItems.init(modBus);                                                                    // the two items and the three components
        SkillBookLootModifier.register(modBus);                                                     // the loot modifier type nusmp:skill_books
        LinesLoot.init(modBus);                                                                     // the rift chests' modifier nusmp:rift_books, the pity memory
        modBus.addListener(RouteUnlockedPayload::register);                                         // the S2C packet of the banner
        modContainer.registerConfig(ModConfig.Type.SERVER, LootConfig.SPEC, "nusmp-skilltree-loot-server.toml");
        NeoForge.EVENT_BUS.addListener(UnlockFx::onServerTick);
        NeoForge.EVENT_BUS.addListener(UnlockFx::onServerStopped);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, BossBookDrops::onDrops);                  // the Guardian rolls no loot table, so its books come from its death
    }

    /** One Skill Book per unique skill of the table, for the creative tab (the old SP Books are no longer offered). */
    public static List<ItemStack> creativeItems() {
        List<ItemStack> out = new ArrayList<>();
        try {
            for (var line : UniqueSkills.lines()) {
                ItemStack book = SkillBookItem.stackFor(line);
                if (!book.isEmpty()) out.add(book);
            }
        } catch (RuntimeException | LinkageError e) {
            // the table is not ready (very early on a client): the tab simply has no books yet
        }
        return out;
    }
}
