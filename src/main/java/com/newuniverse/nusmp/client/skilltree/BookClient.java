package com.newuniverse.nusmp.client.skilltree;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.skilltree.item.BookLook;
import com.newuniverse.nusmp.skilltree.item.SkillItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.slf4j.Logger;

import java.io.Reader;

/**
 * Why: the client half of the 3D Skill Book (1.82.0), kept out of {@link ItemsClient} so the two stay small. Three jobs. (1) Load {@code assets/nusmp/skill_books.json}, the shipped
 * table that lets a client dress a book whose line it has not unlocked (the server syncs only owned lines), on every resource reload (F3+T, a resource pack may override it).
 * (2) The item property {@code nusmp:franchise} that the item model's overrides read to pick the franchise's cover model ({@code (index + 1) / 20}, see {@link BookLook}); the
 * known route's own {@code magic} is the fall-back when the table has no entry, so a datapack line of an existing franchise still gets its cover. (3) {@link #coverColor}, the tint of
 * the cover (tint index 1 of the 3D model AND of the old 2D sprite): the route's colour when the client has the route, else the table's, else a neutral grey. Client only; started
 * from {@link ItemsClient#init}. A failure to read the table is logged once and leaves it empty (books then fall back to the grey generic cover, never a crash).
 */
public final class BookClient {
    private BookClient() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final ResourceLocation TABLE = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "skill_books.json");
    /** The tint of a book that neither the registry nor the table knows. */
    public static final int UNKNOWN_COVER = 0x8A8A9A;

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterClientReloadListenersEvent e) -> e.registerReloadListener((ResourceManagerReloadListener) BookClient::reload));
        modBus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(BookClient::registerProperty));
    }

    private static void reload(ResourceManager manager) {
        BookLook.set(BookLook.Index.EMPTY);
        manager.getResource(TABLE).ifPresent(res -> {
            try (Reader reader = res.openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                BookLook.set(BookLook.parse(root));
            } catch (Exception ex) {
                LOG.warn("[nusmp] could not read assets/nusmp/skill_books.json: books of unknown lines will use the generic cover", ex);
            }
        });
    }

    /** {@code nusmp:franchise}: 0 for no known franchise (the generic cover), else (index + 1) / steps. Read per frame, so it only does two map lookups and allocates nothing. */
    private static void registerProperty() {
        ItemProperties.register(SkillItems.SKILL_BOOK.get(), ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "franchise"), (stack, level, entity, seed) -> {
            String bookId = SkillItems.bookIdOf(stack);
            return BookLook.franchiseValue(bookId, null);
        });
    }

    /** The cover tint of a Skill Book stack (0xRRGGBB): the route's colour, else the shipped table's, else grey. */
    public static int coverColor(ItemStack stack) {
        String bookId = SkillItems.bookIdOf(stack);
        return BookLook.colorOf(bookId, -1, UNKNOWN_COVER);
    }
}
