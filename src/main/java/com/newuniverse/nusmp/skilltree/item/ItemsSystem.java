package com.newuniverse.nusmp.skilltree.item;

import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.loot.BossBookDrops;
import com.newuniverse.nusmp.skilltree.loot.LootConfig;
import com.newuniverse.nusmp.skilltree.loot.SkillBookLootModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the one entry point of the items system: the Skill Book (one item whose component names the book, so a datapack route needs no new item) and the SP Book (raw Skill
 * Points), their use logic, the unlock effects, the Global Loot Modifier and the death listener that drop them in the Mega-Dungeon, the config
 * {@code nusmp-skilltree-loot-server.toml}, and the self-tests {@code tree.items.*}. It is called once from {@code SkillTreeSystem.init} (which owns the order of the
 * systems) and registers everything itself, on the mod bus for what the mod bus owns (the item and component registers, the loot modifier type, the packet) and on the
 * game bus for the rest, so the mod constructor has no line for it besides the skill tree's single one. It is safe to call twice. The unlock show is attached to the
 * skill-tree API as a LISTENER rather than called from the book, so a route unlocked by a command, the shop or a quest gets the same show as one unlocked by a book.
 *
 * <p>{@link #creativeItems} lists a STACK (the component says which book) per route and per SP kind for the creative tab. Whether a second copy of a book for a route the
 * player already has turns into Skill Points is a setting of the CORE system's config ({@code duplicateBookGivesSp}); this system asks the one door for it
 * ({@code SkillTreeApi.duplicateBookGivesSp()}) instead of owning a second copy of the setting.
 */
public final class ItemsSystem {
    private ItemsSystem() {}

    private static boolean done;

    /** Starts the items system; called once from {@code SkillTreeSystem.init}. */
    public static synchronized void init(IEventBus modBus, ModContainer modContainer) {
        if (done) return;
        done = true;
        SkillItems.init(modBus);                                                                    // the two items and the three components
        SkillBookLootModifier.register(modBus);                                                     // the loot modifier type nusmp:skill_books
        modBus.addListener(RouteUnlockedPayload::register);                                         // the S2C packet of the banner
        modContainer.registerConfig(ModConfig.Type.SERVER, LootConfig.SPEC, "nusmp-skilltree-loot-server.toml");
        SkillTreeApi.addUnlockListener(UnlockFx::play);                                            // the show of every unlock, by any means
        NeoForge.EVENT_BUS.addListener(UnlockFx::onServerTick);
        NeoForge.EVENT_BUS.addListener(UnlockFx::onServerStopped);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, BossBookDrops::onDrops);                  // the Guardian rolls no loot table, so its books come from its death
        ItemsSelfTests.register();
    }

    /** One Skill Book per route of the registry (the synced copy on a client; none if it is empty or not ready) and one SP Book per kind, for the creative tab. */
    public static List<ItemStack> creativeItems() {
        List<ItemStack> out = new ArrayList<>();
        try {
            for (SkillRoute route : RouteRegistry.all()) {
                ItemStack book = SkillBookItem.stackFor(route);
                if (!book.isEmpty()) out.add(book);
            }
        } catch (RuntimeException | LinkageError e) {
            // the registry is not ready (very early on a client): the SP Books are still offered
        }
        for (SpKind kind : SpKind.values()) out.add(SPBookItem.stack(kind));
        return out;
    }
}
