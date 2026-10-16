package com.newuniverse.nusmp.book.ext;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;

import java.util.List;

/**
 * 1.43.0 Why: Soul, Soul Corpse and Song Magic are generic wiki attributes (WikiBooks.attribute). Their exclusive caster pages
 * (Lucius, Rades, Kahono) are appended after the generic five through this one dispatch, so three features can grow their own
 * page lists without anyone editing WikiBooks again. Each list lives in its own class and only ever grows at the end.
 */
public final class ExclusiveExt {
    private ExclusiveExt() {}

    public static List<BookPage> pages(MagicType magic) {
        return switch (magic) {
            case SOUL -> SoulExt.pages();
            case SOUL_CORPSE -> SoulCorpseExt.pages();
            case SONG -> SongExt.pages();
            default -> List.of();
        };
    }
}
