package com.newuniverse.nusmp.charset;

import com.newuniverse.nusmp.book.BasicPages;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.CharacterSpells;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.NUSkills;
import net.minecraft.server.MinecraftServer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: nothing here can be played by its author, so what can be proved without a person is proved by the real game (the CI server smoke test, key {@code magic.basic_grimoires}, see
 * docs/magic/basic_grimoires.md): every page a character set marks exclusive is hidden in a grimoire bound to nobody; every page {@link BasicSignatures} reserves is a real family page of its
 * book, is hidden in the basic grimoire, and shows again in the grimoire of its owner when the owner's set is for that book; every page {@link BasicPages} appended is a real family page placed
 * after every other page of the book, has a cast and a cooldown that keeps it cheap, belongs to nobody (no lock, no set lists it) and is visible in the basic grimoire; and no book is over the
 * 24 family pages the unlock mask allows.
 */
public final class BasicGrimoireSelfTests {
    private BasicGrimoireSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.basic_grimoires", BasicGrimoireSelfTests::run);
    }

    private static GrimoireBook book(String path) {
        for (var h : NUSkills.BOOKS) if (h.getId().getPath().equals(path)) return h.get();
        return null;
    }

    private static int index(GrimoireBook b, String page) {
        for (int m = 0; m < b.familyCount(); m++) if (b.page(m).id().equals(page)) return m;
        return -1;
    }

    private static Result run(MinecraftServer server) {
        CharacterSetRegistry reg = CharacterSets.registry();
        if (!CharacterSetConfig.enabled() || !CharacterSetConfig.basicHidesSignatures()) return Result.fail("magic.characterSpellSets / basicBookHidesSignatures do not default to true");
        // 1. the size budget of every book
        for (var h : NUSkills.BOOKS) {
            GrimoireBook b = h.get();
            if (b.familyCount() > 24) return Result.fail(h.getId().getPath() + " has " + b.familyCount() + " family pages, over the 24 the unlock mask allows");
        }
        // 2. every exclusive page is out of the basic grimoire
        int exclusive = 0;
        for (CharacterSet set : reg.all()) {
            GrimoireBook b = book(set.book());
            if (b == null) return Result.fail("no book " + set.book());
            var basic = b.createDefaultInstance();
            for (CharacterSet.Spell s : set.spells()) {
                if (!s.exclusive()) continue;
                int m = index(b, s.page());
                if (m < 0) return Result.fail(set.canon() + ": exclusive page " + s.page() + " is not a family page of " + set.book());
                if (m > 0 && !CharacterSets.hidden(basic, m)) return Result.fail("the basic " + set.book() + " grimoire still shows " + s.page() + " (exclusive to " + set.canon() + ")");
                exclusive++;
            }
        }
        // 3. the reserved pages
        int reserved = 0;
        for (BasicSignatures.Entry e : BasicSignatures.all()) {
            GrimoireBook b = book(e.book());
            if (b == null) return Result.fail("reserved page " + e.key() + ": no such book");
            int m = index(b, e.page());
            if (m <= 0) return Result.fail("reserved page " + e.key() + " is not a family page after mode 0");
            CanonBook owner = CanonBook.byId(e.owner());
            if (owner == null) return Result.fail("reserved page " + e.key() + ": " + e.owner() + " is not a CanonBook");
            var inst = b.createDefaultInstance();
            if (!CharacterSets.hidden(inst, m)) return Result.fail("the basic " + e.book() + " grimoire still shows the reserved page " + e.page());
            inst.getOrCreateTag().putString("Canon", e.owner());
            CharacterSet set = reg.of(e.owner());
            if (set != null && set.book().equals(e.book()) && CharacterSets.hidden(inst, m)) return Result.fail(e.owner() + "'s own grimoire hides his reserved page " + e.page());
            reserved++;
        }
        // 4. the appended generic pages
        int added = 0;
        Set<String> seen = new HashSet<>();
        for (String bookPath : BasicPages.books()) {
            GrimoireBook b = book(bookPath);
            List<String> ids = BasicPages.addedTo(bookPath);
            if (b == null || ids.isEmpty()) return Result.fail(bookPath + ": no added pages");
            int first = b.familyCount() - ids.size();
            for (int k = 0; k < ids.size(); k++) {
                BookPage pg = b.page(first + k);
                if (pg == null || !pg.id().equals(ids.get(k))) return Result.fail(bookPath + ": " + ids.get(k) + " is not appended last, in order");
                if (!seen.add(bookPath + "|" + pg.id())) return Result.fail(bookPath + ": " + pg.id() + " twice");
                if (pg.cast() == null) return Result.fail(bookPath + ": " + pg.id() + " has no cast");
                if (pg.cooldown() < 160) return Result.fail(bookPath + ": " + pg.id() + " has a cooldown of " + pg.cooldown() + " ticks, under the 160 that keeps a page cheap");
                if (CharacterSpells.owners(b, pg) != null) return Result.fail(bookPath + ": " + pg.id() + " is locked to a character; basic pages belong to nobody");
                if (reg.exclusiveOwner(bookPath, pg.id()) != null) return Result.fail(bookPath + ": " + pg.id() + " is exclusive to a character");
                if (BasicSignatures.hides(bookPath, pg.id())) return Result.fail(bookPath + ": " + pg.id() + " is both appended and reserved");
                var basic = b.createDefaultInstance();
                if (CharacterSets.hidden(basic, first + k)) return Result.fail(bookPath + ": " + pg.id() + " is hidden in the basic grimoire");
                added++;
            }
        }
        return Result.ok(exclusive + " exclusive pages are out of the basic grimoires, " + reserved + " more are reserved, " + added + " generic pages are appended last in " + BasicPages.books().size() + " books, no book is over 24 pages");
    }
}
