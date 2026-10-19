package com.newuniverse.nusmp.book.ext;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.TransmutationArts;
import com.newuniverse.nusmp.book.TransmutationRules;

import java.util.List;

/**
 * 1.93.0 Transmutation Magic rebuild: the pages APPENDED after Grey's four old ones (Iron Spikes, Magic Convert, Quagmire, Grand Transmutation keep their ids, order and mastery bits;
 * WikiBooks.transmutation() joins this list after them). Ids are save keys: never rename them.
 */
public final class TransmutationExt {
    private TransmutationExt() {}

    private static final List<BookPage> PAGES = List.of(
            BookPage.mid(TransmutationRules.BLADES, "Transmutation: Alchemic Blades", TransmutationArts::alchemicBlades).withCooldown(300),
            BookPage.mid(TransmutationRules.FLOWERING, "Transmutation: Flowering", TransmutationArts::flowering).withCooldown(260),
            BookPage.mid(TransmutationRules.SENTINEL, "Transmutation: Transmuted Sentinel", TransmutationArts::sentinel).withCooldown(400),
            BookPage.signature(TransmutationRules.ARMOUR, "Alchemist's Armour", TransmutationArts::alchemistArmour).withCooldown(900).withAnim("signature"));

    public static List<BookPage> pages() { return PAGES; }
}
