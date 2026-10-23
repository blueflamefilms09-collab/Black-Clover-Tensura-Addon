package com.newuniverse.nusmp.charset;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.CharacterPagesRest;
import com.newuniverse.nusmp.book.CharacterSpells;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.corpse.SoulCorpseAccess;
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
 * Why: nothing here can be played by its author, so what can be proved without a person is proved by the real game (the CI server smoke test, key {@code magic.character_spells_rest}) for the
 * thirteen character sets of docs/magic/character_spells_rest.md: every file loads with no problem and names a real family page of the real book of its character's real magic; the pages
 * {@code CharacterPagesRest} appended sit after the older pages and belong to a character; a grimoire bound to each character shows his pages and hides the others (mode 0 never hidden), the basic
 * grimoire hides his signature spells, a character without a file hides nothing; the rules that make a signature spell wait for the spell before it do not open it too early; the Soul Corpse
 * summons keep the rule of the Soul Corpse fix (the generic ones are nobody's signature, the numbered undead are Rades's); the flourish table has an entry for each character.
 */
public final class CharacterRestSelfTests {
    private CharacterRestSelfTests() {}

    /** The characters of this builder (a set that is not loaded fails the test). */
    public static final List<String> REST = List.of("charlotte", "jack", "rill", "dorothy", "zenon", "vanica", "dante", "lucius", "rades", "gadjah", "mars", "kahono", "kiato");

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.character_spells_rest", CharacterRestSelfTests::run);
    }

    private static GrimoireBook book(String path) {
        for (var h : NUSkills.BOOKS) if (h.getId().getPath().equals(path)) return h.get();
        return null;
    }

    private static int index(GrimoireBook b, String page) {
        for (int m = 0; m < b.pages().size(); m++) if (b.page(m).id().equals(page)) return m;
        return -1;
    }

    private static RuleFacts facts(double mastery, Map<String, Integer> uses, List<String> flags) {
        return new RuleFacts() {
            @Override public double mastery() { return mastery; }
            @Override public int uses(String pageId) { return uses.getOrDefault(pageId, 0); }
            @Override public boolean pageOpen(String pageId) { return false; }
            @Override public int trust() { return 0; }
            @Override public boolean flag(String name) { return flags.contains(name); }
        };
    }

    private static Result run(MinecraftServer server) {
        if (!CharacterSetConfig.enabled()) return Result.fail("magic.characterSpellSets does not default to true");
        CharacterSetRegistry reg = CharacterSets.registry();
        if (!reg.problems().isEmpty()) return Result.fail("a character file has problems: " + String.join(" | ", reg.problems().subList(0, Math.min(3, reg.problems().size()))));
        int pages = 0;
        for (String id : REST) {
            CharacterSet set = reg.of(id);
            if (set == null) return Result.fail("no set loaded for " + id);
            CanonBook c = CanonBook.byId(id);
            GrimoireBook b = book(set.book());
            if (c == null) return Result.fail(id + " is not a CanonBook");
            if (b == null) return Result.fail(id + ": no book " + set.book());
            if (!c.magic.equals(set.magic()) || !b.magic.name().equals(set.magic())) return Result.fail(id + ": magic " + set.magic() + " is not the book's magic (" + b.magic.name() + ") and his canon magic (" + c.magic + ")");
            if (b.familyCount() > 64) return Result.fail(set.book() + " has " + b.familyCount() + " family pages: over the 64 bit unlock mask");
            for (CharacterSet.Spell s : set.spells()) {
                int m = index(b, s.page());
                if (m < 0 || m >= b.familyCount()) return Result.fail(id + ": page " + s.page() + " is not a family page of " + set.book());
                pages++;
            }
            for (String ref : set.spells().stream().flatMap(s -> s.rule().pages().stream()).toList())
                if (!SpellRule.UNIVERSAL_PAGES.contains(ref) && index(b, ref) < 0) return Result.fail(id + ": a rule names unknown page " + ref);
            if (!CharacterFlourishRest.characters().contains(id) || CharacterFlourishRest.layersOf(id).size() != 2) return Result.fail(id + ": no two-layer flourish");
        }
        // the pages appended by CharacterPagesRest: after the older pages, in order, owned by a character
        for (String bookPath : CharacterPagesRest.books()) {
            GrimoireBook b = book(bookPath);
            List<String> added = CharacterPagesRest.addedTo(bookPath);
            if (b == null || added.isEmpty()) return Result.fail(bookPath + ": no added pages");
            int first = -1;
            for (int m = 0; m < b.familyCount(); m++) if (b.page(m).id().equals(added.get(0))) first = m;
            if (first < 0) return Result.fail(bookPath + ": " + added.get(0) + " is not a family page");
            for (int k = 0; k < added.size(); k++) if (first + k >= b.familyCount() || !b.page(first + k).id().equals(added.get(k))) return Result.fail(bookPath + ": " + added.get(k) + " is not appended in order");
            for (String id : added) {
                String owner = reg.exclusiveOwner(bookPath, id);
                if (owner == null) return Result.fail(bookPath + ": added page " + id + " belongs to no character file");
                var owners = CharacterSpells.owners(b, b.page(index(b, id)));
                if (owners == null || !owners.contains(CanonBook.byId(owner))) return Result.fail(bookPath + ": added page " + id + " is not locked to " + owner);
            }
        }
        // visibility on a real instance of each character's book
        for (String id : REST) {
            CharacterSet set = reg.of(id);
            GrimoireBook b = book(set.book());
            ManasSkillInstance inst = b.createDefaultInstance();
            inst.getOrCreateTag().putString("Canon", id);
            for (int m = 0; m < b.familyCount(); m++) {
                boolean listed = set.spell(b.page(m).id()) != null;
                if (m == 0 && CharacterSets.hidden(inst, 0)) return Result.fail(id + ": mode 0 was hidden");
                if (m > 0 && listed == CharacterSets.hidden(inst, m)) return Result.fail(id + ": page " + b.page(m).id() + (listed ? " is hidden although it is in his set" : " is shown although it is not in his set"));
            }
            for (CharacterSet.Spell s : set.spells()) {
                int m = index(b, s.page());
                if (!s.exclusive() || m == 0) continue;
                inst.getOrCreateTag().remove("Canon");
                if (!com.newuniverse.nusmp.corpse.SoulCorpseAccess.alwaysListed(set.book(), s.page()) && !CharacterSets.hidden(inst, m) && CharacterSetConfig.basicHidesSignatures()) return Result.fail(id + ": the basic grimoire shows his signature spell " + s.page());
                inst.getOrCreateTag().putString("Canon", id);
            }
            inst.getOrCreateTag().putString("Canon", "floga");
            for (int m = 0; m < b.familyCount(); m++) if (CharacterSets.hidden(inst, m) && reg.exclusiveOwner(set.book(), b.page(m).id()) == null) return Result.fail("a character without a file lost the page " + b.page(m).id() + " of " + set.book());
        }
        // a signature spell waits for the spell before it
        CharacterSet charlotte = reg.of("charlotte");
        CharacterSet.Spell queen = charlotte.spell("queen_of_briars");
        if (queen == null) return Result.fail("Charlotte has no Queen of Briars");
        if (queen.rule().test(facts(1.0, Map.of(), List.of("mana_zone")))) return Result.fail("Queen of Briars opens without the Corpse-Hunting Briar Tree");
        if (!queen.rule().test(facts(1.0, Map.of("corpse_hunting_briar_tree", 5), List.of("mana_zone")))) return Result.fail("Queen of Briars does not open after the Corpse-Hunting Briar Tree");
        CharacterSet.Spell dains = reg.of("zenon").spell("demon_sword_dainsleif");
        if (dains == null || dains.rule().test(facts(1.0, Map.of(), List.of()))) return Result.fail("Dainsleif opens without Eternal Fangs");
        CharacterSet.Spell death = reg.of("jack").spell("lunatic_slash");
        if (death == null || death.rule().test(facts(1.0, Map.of("forearm_blades", 9), List.of()))) return Result.fail("Lunatic Slash opens without the Death Scythe");
        // Soul Corpse: the generic summons are nobody's signature, the numbered undead are Rades's (docs/magic/soul_corpse_fix.md)
        CharacterSet rades = reg.of("rades");
        for (String g : SoulCorpseAccess.GENERIC_SUMMONS) {
            CharacterSet.Spell s = rades.spell(g);
            if (s == null) return Result.fail("Rades has no " + g);
            if (s.exclusive()) return Result.fail(g + " is a generic summon and must not be exclusive");
        }
        for (String g : SoulCorpseAccess.RADES_ONLY) {
            CharacterSet.Spell s = rades.spell(g);
            if (s == null || !s.exclusive()) return Result.fail(g + " is Rades's and must be exclusive in his file");
        }
        List<String> shown = new ArrayList<>();
        for (String id : REST) shown.add(id + ":" + reg.of(id).spells().size());
        return Result.ok(REST.size() + " character sets (" + String.join(", ", shown) + "), " + pages + " pages resolve, " + CharacterPagesRest.books().size() + " books got added pages, the signature spells wait for the spell before them");
    }
}
