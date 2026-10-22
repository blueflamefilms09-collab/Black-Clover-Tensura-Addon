package com.newuniverse.nusmp.charset;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.CharacterPagesBulls;
import com.newuniverse.nusmp.book.CharacterSpells;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.grimoire.CanonBook;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.Map;

/**
 * Why: nothing here can be played by its author, so what can be proved without a person is proved by the real game (the CI server smoke test, key {@code magic.character_spells_bulls}): the
 * twelve Black Bulls files load with no problem; every page they name is a real family page of that character's real book; every page the squad added sits after the older pages (so the hooks
 * of the twelve books work), belongs to a character and has an owner lock; no book is over the 24 family pages the unlock mask allows; a rule waits for what the series says it waits for (Double
 * Reflect Refrain for Real Double, Keraunos for the boots); a grimoire bound to Luck hides what he never used and shows Keraunos while the basic grimoire hides it; the flair palette names real
 * effect shapes and stays inside its packet limit.
 */
public final class CharacterSetBullsSelfTests {
    private CharacterSetBullsSelfTests() {}

    private static boolean registered;

    /** The characters of this squad (CanonBook ids). */
    public static final List<String> BULLS = List.of("vanessa", "charmy", "luck", "magna", "gauche", "gordon", "zora", "grey", "henry", "finral", "nacht", "secre");

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.character_spells_bulls", CharacterSetBullsSelfTests::run);
    }

    private static GrimoireBook book(String path) {
        for (var h : NUSkills.BOOKS) if (h.getId().getPath().equals(path)) return h.get();
        return null;
    }

    private static int index(GrimoireBook b, String page) {
        for (int m = 0; m < b.pages().size(); m++) if (b.page(m).id().equals(page)) return m;
        return -1;
    }

    private static RuleFacts facts(double mastery, Map<String, Integer> uses, List<String> open) {
        return new RuleFacts() {
            @Override public double mastery() { return mastery; }
            @Override public int uses(String pageId) { return uses.getOrDefault(pageId, 0); }
            @Override public boolean pageOpen(String pageId) { return open.contains(pageId); }
            @Override public int trust() { return 0; }
            @Override public boolean flag(String name) { return false; }
        };
    }

    private static Result run(MinecraftServer server) {
        if (!CharacterSetConfig.enabled()) return Result.fail("magic.characterSpellSets does not default to true");
        CharacterSetRegistry reg = CharacterSets.registry();
        for (String p : reg.problems()) for (String c : BULLS) if (p.startsWith(c + ":")) return Result.fail("a Black Bulls file has a problem: " + p);
        int pages = 0;
        for (String id : BULLS) {
            CharacterSet set = reg.of(id);
            if (set == null) return Result.fail("no set loaded for " + id);
            GrimoireBook b = book(set.book());
            if (b == null) return Result.fail(id + ": no book " + set.book());
            CanonBook c = CanonBook.byId(id);
            if (c == null) return Result.fail(id + " is not a CanonBook");
            if (!c.magic.equals(set.magic()) || !b.magic.name().equals(set.magic())) return Result.fail(id + ": magic " + set.magic() + " is not the book's magic");
            for (CharacterSet.Spell s : set.spells()) {
                int m = index(b, s.page());
                if (m < 0 || m >= b.familyCount()) return Result.fail(id + ": page " + s.page() + " is not a family page of " + set.book());
                pages++;
            }
            for (String ref : set.spells().stream().flatMap(s -> s.rule().pages().stream()).toList())
                if (!SpellRule.UNIVERSAL_PAGES.contains(ref) && index(b, ref) < 0) return Result.fail(id + ": a rule names unknown page " + ref);
            if (b.familyCount() > 24) return Result.fail(set.book() + " has " + b.familyCount() + " family pages (the unlock mask allows 24)");
        }
        // the pages the squad added sit after every older page and belong to a character
        int added = 0;
        for (String bookPath : CharacterPagesBulls.books()) {
            GrimoireBook b = book(bookPath);
            List<String> ids = CharacterPagesBulls.addedTo(bookPath);
            if (b == null || ids.isEmpty()) return Result.fail(bookPath + ": no added pages");
            for (String id : ids) {
                int m = index(b, id);
                if (m < 0 || m >= b.familyCount()) return Result.fail(bookPath + ": added page " + id + " is not a family page (the book's hook is missing)");
                if (reg.exclusiveOwner(bookPath, id) == null) return Result.fail(bookPath + ": added page " + id + " belongs to no character file");
                if (CharacterSpells.owners(b, b.page(m)) == null) return Result.fail(bookPath + ": added page " + id + " has no owner lock");
                added++;
            }
            // the squad's pages are the LAST of their kind: they follow every page that is not appended by a character twin
            int firstAdded = index(b, ids.get(0));
            for (int k = 0; k < ids.size(); k++) if (index(b, ids.get(k)) != firstAdded + k) return Result.fail(bookPath + ": " + ids.get(k) + " is not appended in order");
        }
        // rules wait for what the series says they wait for
        CharacterSet.Spell drr = reg.of("gauche").spell("double_reflect_refrain");
        if (drr == null || drr.rule().test(facts(1.0, Map.of(), List.of()))) return Result.fail("Double Reflect Refrain opens without a Real Double");
        if (!drr.rule().test(facts(1.0, Map.of("real_double", 2), List.of()))) return Result.fail("Double Reflect Refrain does not open after Real Double");
        CharacterSet.Spell ker = reg.of("luck").spell("keraunos");
        if (ker == null || ker.rule().test(facts(1.0, Map.of(), List.of()))) return Result.fail("Keraunos opens without the boots");
        // visibility on a real instance of the Lightning book
        GrimoireBook lightning = book("book_lightning");
        ManasSkillInstance inst = lightning.createDefaultInstance();
        int salim = index(lightning, "rising_salim"), keraunos = index(lightning, "keraunos"), boots = index(lightning, "thunder_boots");
        inst.getOrCreateTag().putString("Canon", "luck");
        if (!CharacterSets.hidden(inst, salim)) return Result.fail("Luck's grimoire shows Rising Salim, which no source gives him");
        if (CharacterSets.hidden(inst, keraunos) || CharacterSets.hidden(inst, boots)) return Result.fail("Luck's grimoire hides Keraunos or the boots");
        inst.getOrCreateTag().putString("Canon", "kaiser");
        if (CharacterSets.hidden(inst, salim)) return Result.fail("a character without a file lost pages of the Lightning book");
        inst.getOrCreateTag().remove("Canon");
        if (!CharacterSets.hidden(inst, keraunos) && CharacterSetConfig.basicHidesSignatures()) return Result.fail("the basic grimoire still shows Luck's Keraunos");
        if (CharacterSets.hidden(inst, 0)) return Result.fail("mode 0 was hidden");
        // the old lock lines are still there
        BookPage fiend = lightning.page(index(lightning, "thunder_fiend"));
        var owners = CharacterSpells.owners(lightning, fiend);
        if (owners == null || !owners.contains(CanonBook.LUCK)) return Result.fail("Thunder Fiend lost its character lock");
        // the flair
        for (String c : BullsFlairPalette.canons()) {
            BullsFlairPalette.Look look = BullsFlairPalette.of(c);
            for (String name : List.of(look.ring(), look.burst(), look.aura())) {
                try { VfxShape.valueOf(name); } catch (IllegalArgumentException e) { return Result.fail("the flair of " + c + " names an unknown shape " + name); }
            }
        }
        if (BullsFlairPalette.packetsFor(100) > BullsFlairPalette.MAX_PACKETS) return Result.fail("the flair exceeds its packet limit");
        return Result.ok(BULLS.size() + " Black Bulls sets, " + pages + " pages resolve, " + added + " added pages follow the older ones, Double Reflect Refrain waits for Real Double, the flair palette is valid");
    }
}
