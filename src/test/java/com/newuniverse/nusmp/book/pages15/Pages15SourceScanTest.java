package com.newuniverse.nusmp.book.pages15;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.DocRow;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Spec;
import com.newuniverse.nusmp.book.pages15.Pages15Rules.Tier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: nothing here can be played, so the "15 spells for every grimoire" wiring is pinned by reading the real sources: the hook is the outermost wrapper of {@code GrimoireBook.familyAll()} and the old appenders
 * are untouched; the server setting defaults to true and the slot defaults are 15 / 19; every unlock-mask reader goes through {@code PageBits} (no {@code 1 << mode} against the "Unlocked" tag is left); the dispatcher
 * asks each batch only about its own books; and, for the pages the eight batches write (none yet in step 0, then up to ~700), the data that has to exist for every page: a unique readable id, a name, a tier with
 * its cooldown bounds, a lang text in the batch's own fragment, a row with a canon source or "original" in the batch's own doc, and the id in the batch's own armour-policy file. The checks over the pages are
 * data-driven, so they pass now (no pages) and bite as soon as a builder adds one. Pure (java.nio, regular expressions, Gson); the working directory is the project root.
 */
class Pages15SourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");
    private static final Path PAGES15 = SRC.resolve("book/pages15");

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    private static String code(Path p) throws IOException {
        return read(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", "");
    }

    // ------------------------------------------------------------------ the hook and the settings
    @Test
    void thePagesAreAppendedLastByTheOutermostWrapperAndTheOldAppendersAreUntouched() throws IOException {
        String gb = code(SRC.resolve("book/GrimoireBook.java"));
        assertTrue(gb.contains("Pages15.append(getRegistryName().getPath(), BasicPages.append(getRegistryName().getPath(), CharacterPagesRest.append(getRegistryName().getPath(), f)))"),
                "Pages15.append wraps BasicPages.append wraps CharacterPagesRest.append");
        assertTrue(gb.contains("Pages15.enabled()"), "the setting is asked (so false restores the old lists)");
        for (String old : List.of("BasicPages.java", "CharacterPagesRest.java", "CharacterPages.java", "CharacterPagesBulls.java", "CharacterPagesGold.java"))
            assertFalse(read(SRC.resolve("book/" + old)).contains("Pages15"), old + " is a pinned appender: new pages never go there");
        String p15 = code(PAGES15.resolve("Pages15.java"));
        assertTrue(p15.contains("if (bookPath == null || !enabled()) return base;"), "false returns the list unchanged");
        assertTrue(p15.contains("MAX_FAMILY"), "a book never grows past the 64 modes of the unlock mask");
        assertTrue(p15.contains("catch (RuntimeException | LinkageError"), "a batch that throws is ignored, not a crash of every grimoire");
        for (int n = 1; n <= 8; n++) assertTrue(p15.contains("case " + n + " -> Batch" + n + ".pages(bookPath);"), "batch " + n + " is dispatched");
    }

    @Test
    void theServerSettingDefaultsToTrueAndTheSlotDefaultsAre15And19() throws IOException {
        String cfg = code(SRC.resolve("charset/CharacterSetConfig.java"));
        assertTrue(Pattern.compile("\\.define\\(\"pagesFifteen\", true\\)").matcher(cfg).find(), "magic.pagesFifteen defaults to true");
        assertTrue(cfg.contains("b.push(\"magic\")"), "in the magic section of the character spell file (a SERVER config: it changes what a player can cast)");
        assertTrue(code(SRC.resolve("charset/CharacterSetBoot.java")).contains("registerConfig(ModConfig.Type.SERVER, CharacterSetConfig.SPEC"));
        assertTrue(code(SRC.resolve("charset/CharacterSetBoot.java")).contains("Pages15Boot.init()"), "the self-test is registered");
        String mc = code(SRC.resolve("multiverse/MultiverseConfig.java"));
        assertTrue(mc.contains("defineInRange(\"slotsCeiling\", 15, 1, 64)"), "slotsCeiling 15");
        assertTrue(mc.contains("defineInRange(\"slotsCap\", 19, 1, 64)"), "slotsCap 19");
        assertTrue(mc.contains("defineInRange(\"slotsFloor\", 6, 1, 64)") && mc.contains("defineInRange(\"slotsContractBonus\", 4, 0, 64)"), "the pacing stays: floor 6, contract +4");
        String mp = code(SRC.resolve("multiverse/MasteryPages.java"));
        assertTrue(mp.contains("PageSlots.slots(") && mp.contains("CharacterSets.visibleCount(inst, book)"), "the ceiling follows the visible pages");
    }

    @Test
    void everyReaderAndWriterOfThePageMasksGoesThroughPageBits() throws IOException {
        List<String> offenders = new ArrayList<>();
        Pattern raw = Pattern.compile("\"(Unlocked|GateTold)\"");
        try (Stream<Path> s = Files.walk(SRC)) {
            for (Path f : s.filter(p -> p.toString().endsWith(".java")).toList()) {
                String rel = SRC.relativize(f).toString().replace('\\', '/');
                if (rel.startsWith("client/")) continue;      // the client only reads the COUNT the server puts in a different tag of the status screen
                String c = code(f);
                Matcher m = raw.matcher(c);
                while (m.find()) {
                    // the only places that may name the tag: PageBits (the constants), MultiverseSync (writes the COUNT of open pages into a status tag), the avatar's own item tag writer through PageBits
                    if (rel.equals("book/PageBits.java") || rel.equals("multiverse/MultiverseSync.java")) continue;
                    offenders.add(rel + " names " + m.group());
                }
                assertFalse(Pattern.compile("getInt\\(\"Unlocked\"\\)[^;]*1 << ").matcher(c).find(), rel + " still reads the Unlocked tag as an int bit mask");
            }
        }
        assertEquals(List.of(), offenders, "the page masks are only read and written through book.PageBits (64 bits)");
    }

    @Test
    void nothingInThePages15PackageTouchesAClientClass() throws IOException {
        try (Stream<Path> s = Files.walk(PAGES15)) {
            for (Path f : s.filter(p -> p.toString().endsWith(".java")).toList()) {
                String c = code(f);
                assertFalse(c.contains("net.minecraft.client") || c.contains("com.mojang.blaze3d") || c.contains("nusmp.client") || c.contains("Minecraft.getInstance"), f.getFileName() + " touches a client class");
            }
        }
    }

    // ------------------------------------------------------------------ the pages the batches write
    private record Page(int batch, String book, String id, String name, Tier tier, int cooldown) {}

    private static final Pattern DEF = Pattern.compile("\\b(starter|mid|zone|signature)\\(\\s*\"([a-z0-9_]+)\"\\s*,\\s*\"([^\"]+)\"");

    /** The pages of one Batch file, with the book of the {@code register("book", ...)} block each sits in. */
    private static List<Page> pagesOf(int batch) throws IOException {
        String c = code(PAGES15.resolve("Batch" + batch + ".java"));
        List<Page> out = new ArrayList<>();
        Matcher reg = Pattern.compile("register\\(\\s*\"([a-z_]+)\"\\s*,").matcher(c);
        List<int[]> blocks = new ArrayList<>();
        List<String> books = new ArrayList<>();
        while (reg.find()) {
            int open = c.indexOf('(', reg.start());
            blocks.add(new int[]{reg.end(), close(c, open)});
            books.add(reg.group(1));
        }
        for (int k = 0; k < blocks.size(); k++) {
            String body = c.substring(blocks.get(k)[0], blocks.get(k)[1]);
            Matcher d = DEF.matcher(body);
            while (d.find()) {
                // the cooldown of THIS page: a .withCooldown(n) before the next page definition, else the tier default
                int from = d.end();
                Matcher next = DEF.matcher(body);
                int to = next.find(from) ? next.start() : body.length();
                Matcher cd = Pattern.compile("\\.withCooldown\\((\\d+)\\)").matcher(body.substring(from, to));
                Tier t = Tier.valueOf(d.group(1).toUpperCase(Locale.ROOT));
                out.add(new Page(batch, books.get(k), d.group(2), d.group(3), t, cd.find() ? Integer.parseInt(cd.group(1)) : t.cooldown));
            }
        }
        return out;
    }

    private static int close(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '"') {
                for (i++; i < s.length() && s.charAt(i) != '"'; i++) if (s.charAt(i) == '\\') i++;
            } else if (ch == '(') depth++;
            else if (ch == ')' && --depth == 0) return i;
        }
        return s.length();
    }

    /** Ids of every older page of any book (the sources under book/ except this package), to keep new ids from repeating one. */
    private static Set<String> olderIds() throws IOException {
        Set<String> ids = new HashSet<>();
        Pattern p = Pattern.compile("(?:\\b(?:starter|mid|zone|signature|daily)|new BookPage)\\(\\s*\"([a-z0-9_]+)\"");
        try (Stream<Path> s = Files.walk(SRC.resolve("book"))) {
            for (Path f : s.filter(x -> x.toString().endsWith(".java") && !x.startsWith(PAGES15)).toList()) {
                Matcher m = p.matcher(read(f));
                while (m.find()) ids.add(m.group(1));
            }
        }
        return ids;
    }

    @Test
    void everyBatchOnlyWritesPagesForItsOwnBooksAndEveryPageObeysTheRules() throws IOException {
        List<Spec> specs = new ArrayList<>();
        Map<String, Integer> perBook = new LinkedHashMap<>();
        for (int b = 1; b <= Pages15Plan.BATCHES; b++) {
            for (Page p : pagesOf(b)) {
                assertEquals(b, Pages15Plan.batchOf(p.book()), "Batch" + b + " writes pages for " + p.book() + ", which belongs to batch " + Pages15Plan.batchOf(p.book()));
                specs.add(new Spec(p.book(), p.id(), p.name(), p.tier().costPercent, p.cooldown()));
                perBook.merge(p.book(), 1, Integer::sum);
            }
        }
        List<String> problems = Pages15Rules.problems(specs, Map.of(), olderIds());
        assertEquals(List.of(), problems, "problems of the new pages");
    }

    @Test
    void everyNewPageHasItsLangTextItsDocRowAndItsPolicyLineInItsOwnBatchFiles() throws IOException {
        for (int b = 1; b <= Pages15Plan.BATCHES; b++) {
            List<Page> pages = pagesOf(b);
            Path lang = ROOT.resolve("tools/lang/pages15_batch" + b + ".json"), doc = ROOT.resolve("docs/magic/pages15_batch" + b + ".md");
            Path policy = ROOT.resolve("src/test/resources/armour_policy/pages15_batch" + b + "_new_pages.json");
            assertTrue(Files.isRegularFile(lang) && Files.isRegularFile(doc) && Files.isRegularFile(policy), "batch " + b + " files exist: " + lang + ", " + doc + ", " + policy);
            JsonObject keys = JsonParser.parseString(read(lang)).getAsJsonObject();
            Set<String> listed = new HashSet<>();
            for (var e : JsonParser.parseString(read(policy)).getAsJsonObject().entrySet()) e.getValue().getAsJsonArray().forEach(x -> listed.add(x.getAsString()));
            Map<String, DocRow> rows = new LinkedHashMap<>();
            for (String line : read(doc).split("\n")) {
                DocRow r = Pages15Rules.parseDocRow(line);
                if (r != null) assertTrue(rows.put(r.book() + "/" + r.id(), r) == null, "doc row twice: " + r.book() + "/" + r.id());
            }
            for (Page p : pages) {
                String key = Pages15Rules.langKey(p.book(), p.id());
                assertTrue(keys.has(key) && keys.get(key).getAsString().length() >= 30, "tools/lang/pages15_batch" + b + ".json needs " + key + " (one or two sentences)");
                assertTrue(listed.contains(p.id()), "pages15_batch" + b + "_new_pages.json needs " + p.id());
                DocRow r = rows.get(p.book() + "/" + p.id());
                assertNotNull(r, "docs/magic/pages15_batch" + b + ".md needs a table row for " + p.book() + "/" + p.id());
                assertNotNull(r.origin(), p.id() + ": the origin must be 'original' or 'canon: <source>'");
                assertEquals(p.tier().name().toLowerCase(Locale.ROOT), r.tier(), p.id() + ": the tier in the doc");
                assertEquals(p.name(), r.name(), p.id() + ": the name in the doc");
            }
            // nothing in the fragments or the doc that is not a page of this batch
            Set<String> mine = new HashSet<>();
            for (Page p : pages) mine.add(p.book() + "/" + p.id());
            for (String key : keys.keySet()) {
                Matcher m = Pattern.compile("nusmp\\.page\\.([a-z_]+)\\.([a-z0-9_]+)").matcher(key);
                assertTrue(m.matches() && mine.contains(m.group(1) + "/" + m.group(2)), "tools/lang/pages15_batch" + b + ".json has a key for a page Batch" + b + " does not define: " + key);
            }
            for (String key : rows.keySet()) assertTrue(mine.contains(key), "docs/magic/pages15_batch" + b + ".md lists a page Batch" + b + " does not define: " + key);
        }
    }
}
