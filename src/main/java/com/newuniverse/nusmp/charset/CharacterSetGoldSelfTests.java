package com.newuniverse.nusmp.charset;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.CharacterPagesGold;
import com.newuniverse.nusmp.book.CharacterSpells;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.NUSkills;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nothing here can be played by its author, so what can be proved without a person is proved by the real game (the CI server smoke test, key {@code magic.character_spells_gold}): the twelve
 * character files of the Golden Dawn / Crimson Lion / Silver Eagle / Wizard King side (docs/magic/character_spells_gold.md) load with no problem; every page they name is a real family page of
 * the real book of that character's CanonBook magic; every page {@link CharacterPagesGold} appended sits after the older pages in the order it was declared, belongs to the character whose name it
 * carries, has a cast, and has a cooldown that keeps it cheap (no page under 8 seconds, no signature under 30); the late spells really wait for the earlier ones (the rules are run on fake facts);
 * and a grimoire bound to Mereoleona hides what she never used while one bound to nobody hides her signature spells.
 */
public final class CharacterSetGoldSelfTests {
    private CharacterSetGoldSelfTests() {}

    /** The characters this builder owns (CanonBook ids). */
    public static final List<String> OWNED = List.of("mereoleona", "leopold", "mimosa", "william", "klaus", "langris", "lemiel", "julius", "kirsch", "karna", "nozel", "kaiser", "letoile");

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.character_spells_gold", CharacterSetGoldSelfTests::run);
    }

    private static GrimoireBook book(String path) {
        for (var h : NUSkills.BOOKS) if (h.getId().getPath().equals(path)) return h.get();
        return null;
    }

    private static int index(GrimoireBook b, String page) {
        for (int m = 0; m < b.pages().size(); m++) if (b.page(m).id().equals(page)) return m;
        return -1;
    }

    private static RuleFacts facts(double mastery, java.util.Set<String> open, java.util.Map<String, Integer> uses, List<String> flags) {
        return new RuleFacts() {
            @Override public double mastery() { return mastery; }
            @Override public int uses(String pageId) { return uses.getOrDefault(pageId, 0); }
            @Override public boolean pageOpen(String pageId) { return open.contains(pageId); }
            @Override public int trust() { return 0; }
            @Override public boolean flag(String name) { return flags.contains(name); }
        };
    }

    private static Result run(MinecraftServer server) {
        CharacterSetRegistry reg = CharacterSets.registry();
        if (!CharacterSetConfig.enabled()) return Result.fail("magic.characterSpellSets does not default to true");
        for (String p : reg.problems()) for (String id : OWNED) if (p.startsWith(id + ":")) return Result.fail("a gold character file has problems: " + p);
        int pages = 0;
        for (String id : OWNED) {
            CharacterSet set = reg.of(id);
            if (set == null) return Result.fail("no set loaded for " + id);
            GrimoireBook b = book(set.book());
            if (b == null) return Result.fail(id + ": no book " + set.book());
            CanonBook c = CanonBook.byId(id);
            if (c == null) return Result.fail(id + " is not a CanonBook");
            if (!c.magic.equals(set.magic()) || !b.magic.name().equals(set.magic())) return Result.fail(id + ": magic " + set.magic() + " is not the book's magic " + b.magic.name() + " / the canon grimoire's " + c.magic);
            for (CharacterSet.Spell s : set.spells()) {
                int m = index(b, s.page());
                if (m < 0 || m >= b.familyCount()) return Result.fail(id + ": page " + s.page() + " is not a family page of " + set.book());
                pages++;
            }
            for (String ref : set.spells().stream().flatMap(s -> s.rule().pages().stream()).toList())
                if (!SpellRule.UNIVERSAL_PAGES.contains(ref) && index(b, ref) < 0) return Result.fail(id + ": a rule names unknown page " + ref);
        }
        // the pages appended by this builder: after the older pages, in order, owned, cast, cheap
        int added = 0;
        for (String bookPath : CharacterPagesGold.books()) {
            GrimoireBook b = book(bookPath);
            List<String> ids = CharacterPagesGold.addedTo(bookPath);
            if (b == null || ids.isEmpty()) return Result.fail(bookPath + ": no added pages");
            int first = index(b, ids.get(0));      // other builders' twin classes may append after these, so the block is found, not assumed to be last
            if (first < 0) return Result.fail(bookPath + ": " + ids.get(0) + " is not in the book");
            for (int k = 0; k < ids.size(); k++) {
                BookPage pg = b.page(first + k);
                if (pg == null || !pg.id().equals(ids.get(k))) return Result.fail(bookPath + ": " + ids.get(k) + " is not appended in order");
                if (pg.cast() == null) return Result.fail(bookPath + ": " + pg.id() + " has no cast");
                String owner = reg.exclusiveOwner(bookPath, pg.id());
                if (owner == null || !OWNED.contains(owner) || !pg.id().startsWith(owner + "_")) return Result.fail(bookPath + ": added page " + pg.id() + " is not exclusive to the character whose name it carries (" + owner + ")");
                var locks = CharacterSpells.owners(b, pg);
                if (locks == null || locks.size() != 1 || !locks.contains(CanonBook.byId(owner))) return Result.fail(bookPath + ": added page " + pg.id() + " is not locked to " + owner);
                int floor = pg.costPercent() >= 30 ? 600 : 160;
                if (pg.cooldown() < floor) return Result.fail(bookPath + ": " + pg.id() + " has a cooldown of " + pg.cooldown() + " ticks, under the " + floor + " that keeps a page cheap");
                added++;
            }
        }
        // the late spells wait for the early ones (rules run on fake facts)
        CharacterSet mere = reg.of("mereoleona");
        var none = facts(1.0, java.util.Set.of(), java.util.Map.of(), List.of());
        if (mere.spell("mereoleona_hellfire_incarnate").rule().test(none)) return Result.fail("Hellfire Incarnate opens without the Abyss form");
        if (mere.spell("mereoleona_brachium_barrage").rule().test(none)) return Result.fail("the Barrage opens without Calidus Brachium having been cast");
        var ready = facts(1.0, java.util.Set.of("mereoleona_brachium_barrage", "mereoleona_purgatory", "mereoleona_purgatory_abyss"), java.util.Map.of("calidos_brachium", 3, "mereoleona_purgatory", 2), List.of());
        for (CharacterSet.Spell s : mere.spells()) if (!s.rule().test(ready)) return Result.fail("Mereoleona's " + s.page() + " does not open once everything before it is done");
        CharacterSet karna = reg.of("karna");
        if (karna.spell("karna_lunatic_crisis").rule().test(facts(1.0, java.util.Set.of(), java.util.Map.of(), List.of("mana_zone")))) return Result.fail("Lunatic Crisis opens without the Crescent Cut Full Moon");
        // visibility on a real instance of the Fire book
        GrimoireBook fire = book("book_fire");
        var inst = fire.createDefaultInstance();
        int calidos = index(fire, "calidos_brachium"), sol = index(fire, "sol_linea"), spiral = index(fire, "spiral_flame");
        inst.getOrCreateTag().putString("Canon", "mereoleona");
        if (CharacterSets.hidden(inst, calidos)) return Result.fail("Mereoleona's grimoire hides Calidus Brachium");
        if (!CharacterSets.hidden(inst, sol)) return Result.fail("Mereoleona's grimoire shows Sol Linea, which is Fuegoleon's");
        inst.getOrCreateTag().putString("Canon", "leopold");
        if (CharacterSets.hidden(inst, spiral) || !CharacterSets.hidden(inst, calidos)) return Result.fail("Leopold's grimoire does not show Spiral Flame / still shows Calidus Brachium");
        inst.getOrCreateTag().remove("Canon");
        if (CharacterSetConfig.basicHidesSignatures() && (!CharacterSets.hidden(inst, calidos) || !CharacterSets.hidden(inst, spiral))) return Result.fail("the basic grimoire still shows the Vermillion signature spells");
        List<String> shown = new ArrayList<>();
        for (String id : OWNED) shown.add(id + ":" + reg.of(id).spells().size());
        return Result.ok(OWNED.size() + " gold character sets (" + String.join(", ", shown) + "), " + pages + " pages resolve, " + added + " added pages are owned, cast and appended in order");
    }
}
