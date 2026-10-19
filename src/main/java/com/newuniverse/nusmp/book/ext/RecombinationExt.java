package com.newuniverse.nusmp.book.ext;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.RecombinationArts;
import com.newuniverse.nusmp.book.RecombinationRules;

import java.util.List;

/**
 * 1.93.0 Recombination Magic rebuild: the pages APPENDED after Henry's four old ones (Mana Corkscrew, Bulwark, Room Swap, The Raging Black Bull keep their ids, order and mastery bits;
 * WikiBooks.recombination() joins this list after them). Ids are save keys: never rename them.
 */
public final class RecombinationExt {
    private RecombinationExt() {}

    private static final List<BookPage> PAGES = List.of(
            BookPage.signature(RecombinationRules.HOUSE_ARMOUR, "House Armour", RecombinationArts::houseArmour).withCooldown(900).withAnim("signature"),
            BookPage.mid(RecombinationRules.ROOM_REBUILD, "Recombination: Room Rebuild", RecombinationArts::roomRebuild).withCooldown(400),
            BookPage.signature(RecombinationRules.ULTRA_BULL, "Ultra Giant Bull", RecombinationArts::ultraGiantBull).withCooldown(4800).withAnim("signature"),
            BookPage.mid(RecombinationRules.PLANK_SNARE, "Recombination: Plank Snare", RecombinationArts::plankSnare).withCooldown(300));

    public static List<BookPage> pages() { return PAGES; }
}
