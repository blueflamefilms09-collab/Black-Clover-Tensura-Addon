package com.newuniverse.nusmp.book.ext;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.seal.BasicSeal;
import com.newuniverse.nusmp.seal.SecreSeals;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.56 Seal Magic upgrade. The pages returned here are APPENDED to the existing SEALING book (never reorder or remove the book's own pages):
 * the basic random-combo seal, then Secre Swallowtail's four exclusive spells (character-locked in CharacterSpells).
 */
public final class SealExt {
    private SealExt() {}

    public static List<BookPage> pages() {
        SecreSeals.init();
        List<BookPage> all = new ArrayList<>();
        all.add(BookPage.mid("random_seal", "Seal Magic: Sigil Combo", BasicSeal::cast));
        all.addAll(SecreSeals.pages());
        return List.copyOf(all);
    }
}
