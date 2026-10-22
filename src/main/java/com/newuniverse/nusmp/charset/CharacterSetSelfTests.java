package com.newuniverse.nusmp.charset;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.CharacterPages;
import com.newuniverse.nusmp.book.CharacterSpells;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: nothing here can be played by its author, so what can be proved without a person is proved by the real game (the CI server smoke test, key {@code magic.character_spells}): the switch
 * defaults on; every shipped file loads with no problem; every page a file names is a real family page of that character's real book; every page this feature added sits after the older pages
 * and belongs to a character; Spirit of Zephyr really waits for the Spirit Dive; a grimoire bound to Yuno hides the pages he never used while one bound to nobody hides his signature spells and one
 * bound to a character without a file hides nothing; mode 0 of a book is never hidden; and the old lock lines are still there.
 */
public final class CharacterSetSelfTests {
    private CharacterSetSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.character_spells", CharacterSetSelfTests::run);
    }

    private static GrimoireBook book(String path) {
        for (var h : NUSkills.BOOKS) if (h.getId().getPath().equals(path)) return h.get();
        return null;
    }

    private static int index(GrimoireBook b, String page) {
        for (int m = 0; m < b.pages().size(); m++) if (b.page(m).id().equals(page)) return m;
        return -1;
    }

    private static RuleFacts facts(double mastery, Map<String, Integer> uses, int trust, List<String> flags) {
        return new RuleFacts() {
            @Override public double mastery() { return mastery; }
            @Override public int uses(String pageId) { return uses.getOrDefault(pageId, 0); }
            @Override public boolean pageOpen(String pageId) { return false; }
            @Override public int trust() { return trust; }
            @Override public boolean flag(String name) { return flags.contains(name); }
        };
    }

    private static Result run(MinecraftServer server) {
        if (!CharacterSetConfig.enabled()) return Result.fail("magic.characterSpellSets does not default to true");
        CharacterSetRegistry reg = CharacterSets.registry();
        if (!reg.problems().isEmpty()) return Result.fail("a character file has problems: " + String.join(" | ", reg.problems().subList(0, Math.min(3, reg.problems().size()))));
        for (String id : CharacterSetRegistry.SHIPPED) if (reg.of(id) == null) return Result.fail("no set loaded for " + id);
        int pages = 0;
        for (CharacterSet set : reg.all()) {
            GrimoireBook b = book(set.book());
            if (b == null) return Result.fail(set.canon() + ": no book " + set.book());
            CanonBook c = CanonBook.byId(set.canon());
            if (c == null) return Result.fail(set.canon() + " is not a CanonBook");
            if (!c.magic.equals(set.magic()) || !b.magic.name().equals(set.magic())) return Result.fail(set.canon() + ": magic " + set.magic() + " is not the book's magic");
            for (CharacterSet.Spell s : set.spells()) {
                int m = index(b, s.page());
                if (m < 0 || m >= b.familyCount()) return Result.fail(set.canon() + ": page " + s.page() + " is not a family page of " + set.book());
                pages++;
            }
            for (String ref : set.spells().stream().flatMap(s -> s.rule().pages().stream()).toList())
                if (!SpellRule.UNIVERSAL_PAGES.contains(ref) && index(b, ref) < 0) return Result.fail(set.canon() + ": a rule names unknown page " + ref);
        }
        // the pages this feature added sit after every older page
        for (String bookPath : List.of("book_wind", "book_water", "book_fire", "book_dark")) {
            GrimoireBook b = book(bookPath);
            List<String> added = CharacterPages.addedTo(bookPath);
            if (b == null || added.isEmpty()) return Result.fail(bookPath + ": no added pages");
            int first = b.familyCount() - added.size();
            for (int k = 0; k < added.size(); k++) if (!b.page(first + k).id().equals(added.get(k))) return Result.fail(bookPath + ": " + added.get(k) + " is not appended in order");
            for (String id : added) {
                if (reg.exclusiveOwner(bookPath, id) == null) return Result.fail(bookPath + ": added page " + id + " belongs to no character file");
                if (CharacterSpells.owners(b, b.page(index(b, id))) == null) return Result.fail(bookPath + ": added page " + id + " has no owner lock");
            }
        }
        // Spirit of Zephyr waits for the Spirit Dive
        CharacterSet yuno = reg.of("yuno");
        CharacterSet.Spell zephyr = yuno.spell("spirit_of_zephyr");
        if (zephyr == null) return Result.fail("Yuno has no Spirit of Zephyr");
        if (zephyr.rule().test(facts(1.0, Map.of(), 100, List.of("spirit_bond", "saint_stage")))) return Result.fail("Spirit of Zephyr opens without a Spirit Dive");
        if (!zephyr.rule().test(facts(1.0, Map.of("spirit_dive", 5), 100, List.of("spirit_bond", "saint_stage")))) return Result.fail("Spirit of Zephyr does not open after the Spirit Dive");
        // visibility on a real instance of the Wind book
        GrimoireBook wind = book("book_wind");
        ManasSkillInstance inst = wind.createDefaultInstance();
        int gust = index(wind, "gust_lane"), storm = index(wind, "spirit_storm"), kama = index(wind, "kamaitachi");
        inst.getOrCreateTag().putString("Canon", "yuno");
        if (!CharacterSets.hidden(inst, gust)) return Result.fail("Yuno's grimoire shows Gust Lane, which he never used");
        if (CharacterSets.hidden(inst, storm)) return Result.fail("Yuno's grimoire hides Spirit Storm");
        inst.getOrCreateTag().putString("Canon", "kaiser");
        if (CharacterSets.hidden(inst, gust) || CharacterSets.hidden(inst, storm)) return Result.fail("a character without a file lost pages");
        inst.getOrCreateTag().remove("Canon");
        if (!CharacterSets.hidden(inst, storm) && CharacterSetConfig.basicHidesSignatures()) return Result.fail("the basic grimoire still shows Yuno's Spirit Storm");
        if (CharacterSets.hidden(inst, kama) || CharacterSets.hidden(inst, 0)) return Result.fail("mode 0 was hidden");
        // the old lock lines are still there
        BookPage z = wind.page(index(wind, "spirit_of_zephyr"));
        var owners = CharacterSpells.owners(wind, z);
        if (owners == null || !owners.contains(CanonBook.YUNO)) return Result.fail("Spirit of Zephyr lost its character lock");
        List<String> shown = new ArrayList<>();
        for (CharacterSet s : reg.all()) shown.add(s.canon() + ":" + s.spells().size());
        return Result.ok(reg.all().size() + " character sets (" + String.join(", ", shown) + "), " + pages + " pages resolve, the added pages follow the older ones, Zephyr waits for the Dive");
    }
}
