package com.newuniverse.nusmp.compat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/**
 * Thin glue for the Compatibility Codex: turns the pages of {@link CompatBook} into a vanilla written book (1.21.1 data component form)
 * with the live mod-list state. The text and the page rules live in the pure {@link CompatBook}.
 */
public final class CompatBookItem {
    private CompatBookItem() {}

    /** Is this mod loaded right now? False when the mod list is not ready yet. */
    public static boolean isLoaded(String modId) {
        try {
            ModList list = ModList.get();
            return list != null && list.isLoaded(modId);
        } catch (RuntimeException notReady) {
            return false;
        }
    }

    /** A fresh copy of the book, built from the current mod list. */
    public static ItemStack stack() {
        List<Filterable<Component>> pages = new ArrayList<>();
        for (String page : CompatBook.pages(CompatBookItem::isLoaded)) pages.add(Filterable.passThrough(Component.literal(page)));
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(Filterable.passThrough(CompatBook.TITLE), CompatBook.AUTHOR, 0, pages, true));
        return book;
    }
}
