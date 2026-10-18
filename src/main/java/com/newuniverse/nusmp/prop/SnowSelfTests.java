package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.WikiBooks;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;

import java.util.List;

/** Why: the CI server smoke test checks the rebuilt Snow book keeps its five page ids in order, appends three, and resolves to its own shapes. Key: magic.snow_rebuild. */
final class SnowSelfTests {
    private SnowSelfTests() {}

    static void register() { SelfTests.register("magic.snow_rebuild", server -> check()); }

    static Result check() {
        GrimoireBook book = WikiBooks.attribute(MagicType.SNOW);
        List<String> want = List.of("attribute_bolt", "attribute_cut", "attribute_field", "attribute_domain", "attribute_guard", "snow_drift", "frost_crystal", "whiteout");
        List<BookPage> pages = book.pages();
        for (int i = 0; i < want.size(); i++) if (i >= pages.size() || !pages.get(i).id().equals(want.get(i))) return Result.fail("page " + i + " should be " + want.get(i));
        if (book.familyCount() != want.size()) return Result.fail("family count " + book.familyCount());
        if (com.newuniverse.nusmp.core.magic.profile.MagicShapes.of("SNOW", true).cast() != com.newuniverse.nusmp.vfx.VfxShape.SNOW_FX1) return Result.fail("SNOW does not resolve to its own shapes");
        return Result.ok("8 pages in order, own shapes");
    }
}
