package com.newuniverse.nusmp.book.pages15;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.PageBits;
import com.newuniverse.nusmp.charset.CharacterSetConfig;
import com.newuniverse.nusmp.charset.CharacterSets;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.NUSkills;
import net.minecraft.server.MinecraftServer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: nothing here can be played by its author, so what can be proved without a person is proved by the real game (the CI server smoke test, key {@code magic.pages15}, docs/magic/pages15_recipe.md). ONE shared
 * test covers every page of every batch: for each book of the plan, every page {@link Pages15} appended is a real family page placed after every other page of the book, in order, with a cast, a readable
 * name, a lang text in en_us.json, a tier's cost and a cooldown inside the tier's bounds ({@link Pages15Rules}); no id is used twice anywhere or repeats an older page; a book that got pages shows at least 15 pages in
 * its BASIC grimoire and none shows more than 32; the 64-bit unlock mask keeps mode 33 apart from mode 1; and the slot rule gives a 15-page book every page at full mastery. The result line lists the pages and
 * books per batch. Pure checks live in {@link Pages15Rules}; this class only reads the live game.
 */
public final class Pages15SelfTests {
    private Pages15SelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("magic.pages15", Pages15SelfTests::run);
    }

    private static GrimoireBook book(String path) {
        for (var h : NUSkills.BOOKS) if (h.getId().getPath().equals(path)) return h.get();
        return null;
    }

    private static Set<String> langKeys() {
        Set<String> keys = new HashSet<>();
        try (InputStream in = Pages15SelfTests.class.getResourceAsStream("/assets/nusmp/lang/en_us.json")) {
            if (in != null) {
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (var e : o.entrySet()) if (e.getValue() != null && e.getValue().isJsonPrimitive()) keys.add(e.getKey());
            }
        } catch (Exception ignored) {
            // an unreadable lang file shows up below as a missing key
        }
        return keys;
    }

    private static Result run(MinecraftServer server) {
        if (!CharacterSetConfig.pagesFifteen()) return Result.fail("magic.pagesFifteen does not default to true");
        // the engine pieces the project rests on
        if (!maskKeepsHighModesApart()) return Result.fail("the 64-bit unlock mask aliases a high mode onto a low one");
        if (com.newuniverse.nusmp.multiverse.PageSlots.slots(6, 12, 4, 16, 15, 1.0, false) != 15 || com.newuniverse.nusmp.multiverse.PageSlots.slots(6, 15, 4, 19, 12, 1.0, false) != 15 || com.newuniverse.nusmp.multiverse.PageSlots.slots(6, 12, 4, 16, 9, 0.0, false) != 6)
            return Result.fail("the slot rule does not give a 15-page grimoire all its pages at full mastery");
        Set<String> lang = langKeys();
        int[] pages = new int[Pages15Plan.BATCHES + 1], books = new int[Pages15Plan.BATCHES + 1];
        List<Pages15Rules.Spec> specs = new ArrayList<>();
        Map<String, Integer> base = new HashMap<>();
        Set<String> older = new HashSet<>();
        int basicShort = 0;
        for (String path : Pages15Plan.allBooks()) {
            GrimoireBook b = book(path);
            if (b == null) return Result.fail(path + " is in the plan but is not a registered book");
            List<String> ids = Pages15.addedTo(path);
            int n = ids.size(), first = b.familyCount() - n;
            if (first < 1) return Result.fail(path + ": " + n + " added pages but only " + b.familyCount() + " family pages");
            for (int m = 0; m < first; m++) older.add(b.page(m).id());
            base.put(path, first);
            if (b.familyCount() > Pages15Rules.MAX_FAMILY) return Result.fail(path + " has " + b.familyCount() + " family pages, over the " + Pages15Rules.MAX_FAMILY + " the unlock mask holds");
            var basic = b.createDefaultInstance();
            int visible = CharacterSets.visibleCount(basic, b);
            String vp = Pages15Rules.visibleProblem(path, visible, n > 0);
            if (vp != null) return Result.fail(vp);
            if (n == 0 && visible < Pages15Rules.GOAL) basicShort++;
            if (n == 0) continue;
            int batch = Pages15Plan.batchOf(path);
            books[batch]++;
            for (int k = 0; k < n; k++) {
                BookPage pg = b.page(first + k);
                if (pg == null || !pg.id().equals(ids.get(k))) return Result.fail(path + ": " + ids.get(k) + " is not appended last, in order");
                if (pg.cast() == null) return Result.fail(path + ": " + pg.id() + " has no handler");
                if (!lang.contains(Pages15Rules.langKey(path, pg.id()))) return Result.fail(path + ": no lang text " + Pages15Rules.langKey(path, pg.id()) + " (merge tools/lang/pages15_batch" + batch + ".json)");
                if (CharacterSets.hidden(basic, first + k)) return Result.fail(path + ": " + pg.id() + " is hidden in the basic grimoire");
                specs.add(new Pages15Rules.Spec(path, pg.id(), pg.name(), pg.costPercent(), pg.cooldown()));
                pages[batch]++;
            }
        }
        List<String> problems = Pages15Rules.problems(specs, base, older);
        if (!problems.isEmpty()) return Result.fail(problems.size() + " problems, first: " + problems.get(0));
        StringBuilder sb = new StringBuilder();
        int total = 0;
        for (int i = 1; i <= Pages15Plan.BATCHES; i++) {
            sb.append(i == 1 ? "" : ", ").append('b').append(i).append(' ').append(pages[i]).append(" pages in ").append(books[i]).append(" books");
            total += pages[i];
        }
        return Result.ok(total + " new pages are appended last, with handler, name, lang text, tier cost and bounded cooldown (" + sb + "); " + basicShort + " books are still under 15 basic pages (their batch is not written yet); no grimoire shows more than "
                + Pages15Rules.MAX_VISIBLE + " pages");
    }

    /** Mode 33 must not be mode 1, and clearing one must not clear the other (two real item tags). */
    private static boolean maskKeepsHighModesApart() {
        net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
        PageBits.set(t, PageBits.UNLOCKED, 33);
        if (PageBits.has(t, PageBits.UNLOCKED, 1) || !PageBits.has(t, PageBits.UNLOCKED, 33) || t.getInt(PageBits.UNLOCKED) != 0) return false;
        PageBits.set(t, PageBits.UNLOCKED, 1);
        PageBits.clear(t, PageBits.UNLOCKED, 33);
        return PageBits.has(t, PageBits.UNLOCKED, 1) && !PageBits.has(t, PageBits.UNLOCKED, 33) && t.getInt(PageBits.UNLOCKED + "2") == 0;
    }
}
