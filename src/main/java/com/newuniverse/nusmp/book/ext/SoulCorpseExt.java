package com.newuniverse.nusmp.book.ext;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.corpse.NamedCorpses;
import com.newuniverse.nusmp.corpse.SoulCorpseSpells;

import java.util.List;

/**
 * 1.43.0 Why: Rades Spirito's Soul Corpse Magic. The generic Soul Corpse book has five pages every attribute gets; Rades's own
 * pages are APPENDED after them (modes 5 to 9, never reordered or removed): Reanimation, the three named puppets (Jimmy, Alfredo and
 * Carl, each a mid page) and True Soul Corpse Magic (a signature page that sets its own long cooldown from the config). Which pages
 * only open in Rades's grimoire is decided by the lock lines in book.CharacterSpells; the logic is in the {@code corpse} package.
 */
public final class SoulCorpseExt {
    private SoulCorpseExt() {}

    public static List<BookPage> pages() {
        return List.of(
                BookPage.mid("corpse_reanimate", "Soul Corpse: Reanimation", SoulCorpseSpells::reanimate),
                BookPage.mid("corpse_jimmy", "No.4 - Jimmy", (b, i, p, m) -> SoulCorpseSpells.named(b, i, p, NamedCorpses.JIMMY)),
                BookPage.mid("corpse_alfredo", "No.2 - Alfredo", (b, i, p, m) -> SoulCorpseSpells.named(b, i, p, NamedCorpses.ALFREDO)),
                BookPage.mid("corpse_carl", "No.1 - Carl", (b, i, p, m) -> SoulCorpseSpells.named(b, i, p, NamedCorpses.CARL)),
                BookPage.signature("true_soul_corpse", "True Soul Corpse Magic", SoulCorpseSpells::truePact).withCooldown(0));
    }

    /**
     * The two numbered undead that came after the first release: No.3 David (the huge stitched bruiser) and No.0 Michael Caesal (the elite, one at a time, a long cooldown). They are the end
     * of Rades's page list, but the pages of the book are numbered by position, so {@code SoulCorpseBook.familyList} appends them after everything it already had: no old page changes its mode.
     */
    public static List<BookPage> latePages() {
        return List.of(
                BookPage.mid("corpse_david", "No.3 - David", (b, i, p, m) -> SoulCorpseSpells.named(b, i, p, NamedCorpses.DAVID)).withCooldown(NamedCorpses.DAVID_PAGE_COOLDOWN),
                BookPage.signature("corpse_michael_caesal", "No.0 - Michael Caesal", (b, i, p, m) -> SoulCorpseSpells.named(b, i, p, NamedCorpses.MICHAEL_CAESAL)).withCooldown(NamedCorpses.ELITE_PAGE_COOLDOWN));
    }
}
