package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.WikiBooks;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;

import java.util.List;

/**
 * Why: nothing visual can be played by the author, so the CI server smoke test runs this in the real game: the rebuilt Smoke book keeps the five page ids
 * and their order (saves and mastery bits depend on them), appends its three new pages after them, resolves to its own effect shapes, and the prop word
 * packing round-trips. Key: magic.smoke_rebuild.
 */
final class SmokeSelfTests {
    private SmokeSelfTests() {}

    static void register() { SelfTests.register("magic.smoke_rebuild", server -> check()); }

    static Result check() {
        GrimoireBook book = WikiBooks.attribute(MagicType.SMOKE);
        List<String> want = List.of("attribute_bolt", "attribute_cut", "attribute_field", "attribute_domain", "attribute_guard", "smoke_step", "coiling_serpent", "smoke_wave");
        List<BookPage> pages = book.pages();
        for (int i = 0; i < want.size(); i++) {
            if (i >= pages.size() || !pages.get(i).id().equals(want.get(i))) return Result.fail("page " + i + " should be " + want.get(i));
        }
        if (book.familyCount() != want.size()) return Result.fail("family count " + book.familyCount());
        var shapes = com.newuniverse.nusmp.core.magic.profile.MagicShapes.of("SMOKE", true);
        if (shapes.cast() != com.newuniverse.nusmp.vfx.VfxShape.SMOKE_FX1) return Result.fail("SMOKE does not resolve to its own shapes");
        int w = AirPropKit.pack(2, AirPropKit.ACT, 70000L);
        if (AirPropKit.variant(w) != 2 || AirPropKit.state(w) != AirPropKit.ACT || AirPropKit.since(w, 70005L) != 5) return Result.fail("prop word round trip");
        return Result.ok("8 pages in order, own shapes, prop word");
    }
}
