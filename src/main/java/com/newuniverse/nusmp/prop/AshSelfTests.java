package com.newuniverse.nusmp.prop;

import com.newuniverse.nusmp.book.AshBook;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;

import java.util.List;

/**
 * Why: the CI server smoke test checks the rebuilt Ash book keeps its four original page ids in their original order (saves and mastery bits depend on them),
 * appends three, and resolves to its own shapes. Key: magic.ash_rebuild.
 */
final class AshSelfTests {
    private AshSelfTests() {}

    static void register() { SelfTests.register("magic.ash_rebuild", server -> check()); }

    static Result check() {
        AshBook book = new AshBook();
        List<String> want = List.of("ash_bullets", "ash_cloud", "ash_absorbing_formation", "revelation_of_the_cowardly", "ash_shroud", "ash_soldiers", "cursed_ash");
        List<BookPage> pages = book.pages();
        for (int i = 0; i < want.size(); i++) if (i >= pages.size() || !pages.get(i).id().equals(want.get(i))) return Result.fail("page " + i + " should be " + want.get(i));
        if (book.familyCount() != want.size()) return Result.fail("family count " + book.familyCount());
        if (pages.get(3).cooldown() != 900) return Result.fail("revelation_of_the_cowardly lost its 900 tick cooldown");
        if (com.newuniverse.nusmp.core.magic.profile.MagicShapes.of("ASH", false).cast() != com.newuniverse.nusmp.vfx.VfxShape.ASH_FX1) return Result.fail("ASH does not resolve to its own shapes");
        return Result.ok("7 pages in order, own shapes");
    }
}
