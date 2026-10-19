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
            case SHAKUDO -> com.newuniverse.nusmp.book.ShakudoSpells.pages();      // 1.93.0: the rebuilt wiki magics (docs/attributes/<key>_rebuild.md)
            case SCALE -> com.newuniverse.nusmp.book.ScaleSpells.pages();
            case SPIKE -> com.newuniverse.nusmp.book.SpikeSpells.pages();
            case NAIL -> com.newuniverse.nusmp.book.NailSpells.pages();
            case HAIR -> com.newuniverse.nusmp.book.HairSpells.pages();
            default -> List.of();
        };
    }
}
