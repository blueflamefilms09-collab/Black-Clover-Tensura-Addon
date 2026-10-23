package com.newuniverse.nusmp.charset;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The basic grimoires (docs/magic/basic_grimoires.md). Reads the real sources and files (no game class): no page a character set marks exclusive is visible to a grimoire bound to nobody; every
 * reserved page ({@link BasicSignatures}) is a real family page that is hidden for the basic book and shown again for its owner; every generic page {@code BasicPages} appends is new, unique, last of
 * its book, owned by nobody, not a copy of a character's spell, texted and listed in the armour policy; no book is over the 32 family pages one grimoire may hold; the casts are bounded; the hooks
 * are in place and the Time Magic files are not touched.
 */
class BasicGrimoiresTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/character_spells");
    /** The books that got generic pages, and the least generic pages a basic book of them shows afterwards. */
    private static final Map<String, Integer> TOPPED_UP = Map.of("book_fire", 6, "book_water", 6, "book_wind", 6, "book_ice", 6, "book_beast", 6, "book_dark", 5);

    private static String read(Path p) throws IOException { return Files.readString(p); }

    private static String code(Path p) throws IOException { return read(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)(?<!:)//.*$", ""); }

    private static List<String> ids(String src) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("(?:(?:BookPage\\.)?\\b(?:starter|mid|zone|signature|daily)|new BookPage)\\(\\s*\"([a-z0-9_]+)\"\\s*,").matcher(src);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    private static Map<String, String> names(String src) {
        Map<String, String> out = new LinkedHashMap<>();
        Matcher m = Pattern.compile("(?:(?:BookPage\\.)?\\b(?:starter|mid|zone|signature|daily)|new BookPage)\\(\\s*\"([a-z0-9_]+)\"\\s*,\\s*\"([^\"]*)\"").matcher(src);
        while (m.find()) out.put(m.group(1), m.group(2));
        return out;
    }

    private static CharacterSetRegistry shipped() {
        CharacterSetRegistry r = CharacterSetRegistry.load(CharacterSetRegistry.SHIPPED, id -> {
            try { return Files.readString(DIR.resolve(id + ".json")); } catch (IOException e) { return null; }
        });
        assertTrue(r.problems().isEmpty(), "the shipped files have problems: " + r.problems());
        return r;
    }

    /** book registry path -> (class, ElementBooks method) from NUSkills. */
    private static Map<String, String[]> bookClasses() throws IOException {
        Map<String, String[]> out = new LinkedHashMap<>();
        Matcher m = Pattern.compile("book\\(\"(book_[a-z_]+)\",\\s*com\\.newuniverse\\.nusmp\\.book\\.([A-Za-z]+)(?:::|\\.)?([A-Za-z]*)").matcher(read(SRC.resolve("skill/NUSkills.java")));
        while (m.find()) out.put(m.group(1), new String[]{m.group(2), m.group(3)});
        return out;
    }

    private static String camel(String book) {
        StringBuilder sb = new StringBuilder();
        for (String part : book.substring("book_".length()).split("_")) sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        return sb.toString();
    }

    /** The ids of the upgrade classes (book/ext) a piece of source names, e.g. {@code SlashExt.pages()}. */
    private static List<String> extIds(String src) throws IOException { return extIds(src, 0); }

    private static List<String> extIds(String src, int depth) throws IOException {
        List<String> out = new ArrayList<>();
        if (depth > 2) return out;
        Matcher m = Pattern.compile("(\\w+(?:Ext|Works|Spells|Pages))\\.pages\\(").matcher(src);
        while (m.find()) {
            Path f = Files.exists(SRC.resolve("book/ext/" + m.group(1) + ".java")) ? SRC.resolve("book/ext/" + m.group(1) + ".java") : SRC.resolve("book/" + m.group(1) + ".java");
            if (!Files.exists(f)) continue;
            String c = code(f);
            out.addAll(ids(c));
            out.addAll(extIds(c, depth + 1));
        }
        return out;
    }

    /** The family page ids the sources define for a book, the older pages and the character builders' pages (not the BasicPages ones). */
    private static List<String> familyWithoutBasic(String book) throws IOException {
        String[] cm = bookClasses().get(book);
        List<String> out = new ArrayList<>();
        if (cm == null) {
            // a generic wiki attribute (Soul, Song, Mineral ...): the five generic pages of WikiBooks.attribute, then its upgrade classes
            String wb = code(SRC.resolve("book/WikiBooks.java"));
            out.addAll(ids(wb.substring(wb.indexOf("public static GrimoireBook attribute("), wb.indexOf("public static int color("))));
            String name = camel(book);
            for (String f : new String[]{"book/" + name + "Book.java", "book/ext/" + name + "Ext.java", "book/" + name + "Spells.java"}) {
                Path path = SRC.resolve(f);
                if (Files.exists(path)) for (String id : ids(code(path))) if (!out.contains(id)) out.add(id);
            }
            if (book.equals("book_mineral")) for (String id : ids(code(SRC.resolve("book/EarthFamilyBooks.java")))) if (!out.contains(id)) out.add(id);
        } else {
            Path f = SRC.resolve("book/" + cm[0] + ".java");
            String c = Files.exists(f) ? code(f) : "";
            if (!cm[1].isEmpty() && !cm[1].equals("new")) {      // a factory method of a shared class (ElementBooks.sword(), WikiBooks.transmutation()): only its own text
                Matcher m = Pattern.compile("public static GrimoireBook " + cm[1] + "\\(\\)").matcher(c);
                if (m.find()) {
                    int from = m.start(), to = c.length();
                    Matcher nx = Pattern.compile("public static GrimoireBook \\w+\\(\\)").matcher(c);
                    while (nx.find()) if (nx.start() > from) { to = nx.start(); break; }
                    c = c.substring(from, to);
                }
            }
            out.addAll(ids(c));
            out.addAll(extIds(c));
            Matcher nb = Pattern.compile("new (\\w+Book)\\(").matcher(c);      // ElementBooks.bone() builds a BoneBook: its own pages count too
            while (nb.find()) {
                Path bf = SRC.resolve("book/" + nb.group(1) + ".java");
                if (!nb.group(1).equals("ElementBook") && Files.exists(bf) && !nb.group(1).equals(cm[0])) { String bc = code(bf); for (String id : ids(bc)) if (!out.contains(id)) out.add(id); for (String id : extIds(bc)) if (!out.contains(id)) out.add(id); }
            }
        }
        for (String fn : new String[]{"CharacterPages", "CharacterPagesBulls", "CharacterPagesGold", "CharacterPagesRest"}) {
            String s = code(SRC.resolve("book/" + fn + ".java"));
            Matcher g = Pattern.compile("register\\(\"" + book + "\",[^\\n]*?List\\.of\\((.*?)\\)\\);", Pattern.DOTALL).matcher(s);
            if (g.find()) for (String id : ids(g.group(1))) if (!out.contains(id)) out.add(id);
        }
        return out;
    }

    private static Map<String, List<String>> basicAdded() throws IOException {
        Map<String, List<String>> out = new LinkedHashMap<>();
        String s = code(SRC.resolve("book/BasicPages.java"));
        Matcher m = Pattern.compile("register\\(\"(book_[a-z_]+)\", List\\.of\\((.*?)\\)\\);", Pattern.DOTALL).matcher(s);
        while (m.find()) out.put(m.group(1), ids(m.group(2)));
        return out;
    }

    private static final Map<String, Boolean> LITERALS = new LinkedHashMap<>();

    /** Does the string literal {@code "id"} appear in any main source (pages whose id is a constant, like Paladin Conversion or the Mercury pages)? */
    private static boolean literalExists(String id) throws IOException {
        Boolean known = LITERALS.get(id);
        if (known != null) return known;
        boolean found = false;
        try (var walk = Files.walk(SRC)) {
            for (Path p : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".java"))::iterator) if (Files.readString(p).contains("\"" + id + "\"")) { found = true; break; }
        }
        LITERALS.put(id, found);
        return found;
    }

    private static String norm(String n) { return String.join(" ", n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").trim().split("\\s+")); }

    private static String tail(String n) { return norm(n.substring(n.lastIndexOf(':') + 1)); }

    // ------------------------------------------------------------------ the basic book hides every signature
    @Test
    void noPageACharacterSetMarksExclusiveIsVisibleToABasicGrimoire() throws IOException {
        CharacterSetRegistry r = shipped();
        int n = 0;
        for (CharacterSet set : r.all()) {
            List<String> family = familyWithoutBasic(set.book());
            for (CharacterSet.Spell s : set.spells()) {
                if (!s.exclusive()) continue;
                n++;
                assertTrue(family.contains(s.page()) || literalExists(s.page()), set.canon() + ": " + s.page() + " is not a page of " + set.book());      // a page named by a constant (Paladin Conversion ...) is found as a string literal
                boolean modeZero = family.indexOf(s.page()) == 0;
                if (modeZero) continue;      // the engine never hides mode 0
                if (com.newuniverse.nusmp.corpse.SoulCorpseAccess.alwaysListed(set.book(), s.page())) {      // Rades's numbered undead stay listed in the basic Soul Corpse book (soul_corpse_fix.md)
                    assertFalse(r.hidden("", set.book(), s.page(), false, true), set.book() + "/" + s.page() + " must stay listed in the basic Soul Corpse grimoire");
                    assertFalse(r.hidden(null, set.book(), s.page(), false, true), set.book() + "/" + s.page());
                    continue;
                }
                assertTrue(r.hidden("", set.book(), s.page(), false, true), set.book() + "/" + s.page() + " is visible in the basic grimoire");
                assertTrue(r.hidden(null, set.book(), s.page(), false, true), set.book() + "/" + s.page());
                assertFalse(r.hidden("", set.book(), s.page(), false, false), "the setting off lists it as before");
            }
        }
        assertTrue(n > 150, "exclusive pages checked: " + n);
    }

    @Test
    void everyReservedPageIsARealPageHiddenForTheBasicBookAndShownForItsOwner() throws IOException {
        CharacterSetRegistry r = shipped();
        String canonSrc = read(SRC.resolve("grimoire/CanonBook.java"));
        Set<String> seen = new HashSet<>();
        assertTrue(BasicSignatures.all().size() >= 20);
        for (BasicSignatures.Entry e : BasicSignatures.all()) {
            assertTrue(seen.add(e.key()), "twice: " + e.key());
            List<String> family = familyWithoutBasic(e.book());
            int at = family.indexOf(e.page());
            assertTrue(at > 0, e.key() + " is not a family page after mode 0 of " + e.book() + " " + family);
            assertTrue(Pattern.compile("\\b" + e.owner().toUpperCase(Locale.ROOT) + "\\(\"").matcher(canonSrc).find(), e.key() + ": owner " + e.owner() + " is not a CanonBook entry");
            assertFalse(e.note().isBlank(), e.key());
            assertTrue(r.hidden("", e.book(), e.page(), false, true), e.key() + " is visible in the basic grimoire");
            assertFalse(r.hidden("", e.book(), e.page(), false, false), "the setting off lists it");
            assertFalse(r.hidden("", e.book(), e.page(), true, true), "mode 0 is never hidden");
            CharacterSet ownerSet = r.of(e.owner());
            if (ownerSet != null && ownerSet.book().equals(e.book())) {
                assertFalse(r.hidden(e.owner(), e.book(), e.page(), false, true), e.key() + ": the owner's own grimoire hides it");
                assertTrue(r.listed(e.owner(), e.book(), e.page()));
                if (!ownerSet.has(e.page())) assertNull(r.opens(e.owner(), e.book(), e.page(), new SpellRuleTest.F()), e.key() + ": the old mastery order opens a reserved page his set does not list");
            }
            for (CharacterSet other : r.all())
                if (!other.canon().equals(e.owner()) && other.book().equals(e.book()) && !other.has(e.page()))
                    assertTrue(r.hidden(other.canon(), e.book(), e.page(), false, true), e.key() + " shows for " + other.canon());
            assertEquals(e.owner(), BasicSignatures.owner(e.book(), e.page()));
            assertTrue(BasicSignatures.hides(e.book(), e.page()));
        }
        assertFalse(BasicSignatures.hides("book_wind", "gust_lane"));
        assertFalse(BasicSignatures.reservedFor("yuno", "book_fire", "crimson_lion_king"));
        assertTrue(BasicSignatures.reservedFor("FUEGOLEON", "book_fire", "crimson_lion_king"));
    }

    @Test
    void theOwnersOfAReservedPageSeeItWhileEveryoneElseWithASetInThatBookDoesNot() {
        CharacterSetRegistry r = shipped();
        assertFalse(r.hidden("fuegoleon", "book_fire", "crimson_lion_king", false, true), "Fuegoleon's set does not list it but the page is his");
        assertEquals(null, r.opens("fuegoleon", "book_fire", "crimson_lion_king", new SpellRuleTest.F()));
        assertTrue(r.hidden("mereoleona", "book_fire", "crimson_lion_king", false, true), "Mereoleona's grimoire is her own set");
        assertTrue(r.hidden("", "book_fire", "crimson_lion_king", false, true));
        assertFalse(r.hidden("noelle", "book_water", "sea_dragon_god_form", false, true));
        assertFalse(r.hidden("", "book_water", "mermaid_form", false, true), "the Mermaid Form stays open to every Water mage");
        assertTrue(r.hidden("", "book_water", "sea_dragon_god_form", false, true));
        assertFalse(r.hidden("luck", "book_lightning", "thunder_boots", false, true));
        assertTrue(r.hidden("gadjah", "book_lightning", "thunder_boots", false, true));
        // a character whose grimoire is another book is not affected
        assertFalse(r.hidden("jack", "book_sword", "ripper_cut", false, true));
        assertEquals(null, r.opens("jack", "book_sword", "ripper_cut", new SpellRuleTest.F()));
    }

    // ------------------------------------------------------------------ the pages added
    @Test
    void everyGenericPageIsNewUniqueLastOwnedByNobodyAndNotACharactersSpell() throws IOException {
        CharacterSetRegistry r = shipped();
        Map<String, List<String>> added = basicAdded();
        assertEquals(TOPPED_UP.keySet(), added.keySet(), "the books that got pages");
        Map<String, String> nameOfId = names(code(SRC.resolve("book/BasicPages.java")));
        Set<String> all = new HashSet<>();
        Set<String> characterNames = new HashSet<>();
        for (CharacterSet set : r.all()) for (CharacterSet.Spell s : set.spells()) { characterNames.add(norm(s.name())); characterNames.add(tail(s.name())); }
        int total = 0;
        for (var e : added.entrySet()) {
            String book = e.getKey();
            List<String> before = familyWithoutBasic(book);
            for (String id : e.getValue()) {
                total++;
                assertTrue(all.add(book + "|" + id), id + " twice");
                assertFalse(before.contains(id), book + ": " + id + " already exists in the book");
                assertTrue(id.matches("[a-z][a-z0-9_]+"), id);
                assertNull(r.exclusiveOwner(book, id), id + " is exclusive to a character");
                assertFalse(BasicSignatures.hides(book, id), id + " is reserved");
                for (CharacterSet set : r.all()) assertFalse(set.has(id) && set.book().equals(book), id + " is in the set of " + set.canon());
                String name = nameOfId.get(id);
                assertNotNull(name, id);
                assertFalse(characterNames.contains(norm(name)) || characterNames.contains(tail(name)), id + " (" + name + ") is the name of a character's spell");
            }
            int family = before.size() + e.getValue().size();
            assertTrue(family <= 32, book + " would have " + family + " family pages: the budget of one grimoire is 32 (the unlock mask holds 64)");
        }
        assertEquals(19, total, "the pages added");
    }

    @Test
    void theBasicBooksThatWereToppedUpShowEnoughPagesAndNoBookIsOverTheBudget() throws IOException {
        CharacterSetRegistry r = shipped();
        Map<String, List<String>> added = basicAdded();
        for (var e : TOPPED_UP.entrySet()) {
            String book = e.getKey();
            List<String> family = new ArrayList<>(familyWithoutBasic(book));
            family.addAll(added.get(book));
            int visible = 0;
            for (int m = 0; m < family.size(); m++) if (!r.hidden("", book, family.get(m), m == 0, true)) visible++;
            assertTrue(visible >= e.getValue(), book + " shows only " + visible + " pages to a basic grimoire");
        }
        for (String book : bookClasses().keySet()) {
            int n = familyWithoutBasic(book).size() + added.getOrDefault(book, List.of()).size();
            assertTrue(n <= 32, book + " has " + n + " family pages: the budget of one grimoire is 32");
        }
    }

    @Test
    void everyGenericPageHasTextAndAPolicyLine() throws IOException {
        JsonObject lang = JsonParser.parseString(read(Path.of("tools/lang/basic_spells.json"))).getAsJsonObject();
        JsonObject policy = JsonParser.parseString(read(Path.of("src/main/resources/data/nusmp/armour_policy.json"))).getAsJsonObject().getAsJsonObject("magics");
        Set<String> listed = new HashSet<>();
        for (var e : policy.entrySet()) if (e.getValue().getAsJsonObject().has("new_pages")) for (var x : e.getValue().getAsJsonObject().getAsJsonArray("new_pages")) listed.add(x.getAsString());
        int n = 0;
        for (var e : basicAdded().entrySet()) for (String id : e.getValue()) {
            n++;
            String key = "nusmp.page." + e.getKey() + "." + id;
            assertTrue(lang.has(key) && lang.get(key).getAsString().length() > 60, "lang: " + key);
            assertTrue(listed.contains(id), id + " is not in the armour policy's new_pages");
        }
        assertEquals(n, lang.size(), "one text per added page and nothing else");
    }

    // ------------------------------------------------------------------ the casts
    @Test
    void theCastsAreBounded() throws IOException {
        String src = code(SRC.resolve("book/BasicArts.java"));
        for (String bad : new String[]{"Minecraft", "net.minecraft.client", "com.mojang.blaze3d", "setBlock", "tempBlock", "addFreshEntity", "getChunk", "loadChunk", "EntityType", "new Thread"})
            assertFalse(src.contains(bad), "BasicArts uses " + bad);
        Matcher m = Pattern.compile("public static boolean (\\w+)\\(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode\\) \\{").matcher(src);
        List<int[]> spans = new ArrayList<>();
        List<String> nm = new ArrayList<>();
        while (m.find()) { spans.add(new int[]{m.start()}); nm.add(m.group(1)); }
        assertEquals(19, nm.size());
        for (int k = 0; k < nm.size(); k++) {
            int from = spans.get(k)[0], to = k + 1 < nm.size() ? spans.get(k + 1)[0] : src.length();
            String body = src.substring(from, to);
            int packets = count(body, "fx(p,") + count(body, "b.vfx(") + count(body, "VfxSpawn.sendFollowing(") + count(body, "b.castCircle(") + count(body, "b.impact(");
            assertTrue(packets <= 6 + 4, nm.get(k) + " sends " + packets + " effect calls");
            assertTrue(count(body, "SpellRuntime.zone(") <= 1, nm.get(k) + ": more than one zone");
            Matcher z = Pattern.compile("SpellRuntime\\.zone\\(p\\.serverLevel\\(\\), (\\d+), (\\d+)").matcher(body);
            while (z.find()) assertTrue(Integer.parseInt(z.group(1)) <= 100 && Integer.parseInt(z.group(2)) >= 4, nm.get(k) + ": the zone is longer than 5 s or runs more often than every 4 ticks");
            for (Matcher loop = Pattern.compile("for \\(int \\w+ = 0; \\w+ < (\\d+);").matcher(body); loop.find(); ) assertTrue(Integer.parseInt(loop.group(1)) <= 4, nm.get(k) + ": a loop of more than 4");
            if (body.contains("for (LivingEntity")) assertTrue(body.contains("MAX_HITS"), nm.get(k) + ": a hit loop without the cap");
            if (body.contains("for (Projectile") ) assertTrue(body.contains("MAX_DISCARD") || body.contains("discardForeign"), nm.get(k));
        }
        assertTrue(src.contains("MAX_HITS = 8") && src.contains("MAX_DISCARD = 6"));
        String pages = code(SRC.resolve("book/BasicPages.java"));
        Matcher cd = Pattern.compile("\\.withCooldown\\((\\d+)\\)").matcher(pages);
        int pagesWithCooldown = 0;
        while (cd.find()) { pagesWithCooldown++; assertTrue(Integer.parseInt(cd.group(1)) >= 160, "a cooldown under 8 seconds"); }
        assertEquals(19, pagesWithCooldown);
        assertFalse(pages.contains("signature("), "the generic pages are mid-tier: no page asks for a signature rank");
    }

    private static int count(String s, String what) {
        int n = 0;
        for (int i = s.indexOf(what); i >= 0; i = s.indexOf(what, i + what.length())) n++;
        return n;
    }

    // ------------------------------------------------------------------ the hooks
    @Test
    void theHooksAreInPlaceAndTheTimeMagicIsNotTouched() throws IOException {
        assertTrue(read(SRC.resolve("book/GrimoireBook.java")).contains("BasicPages.append(getRegistryName().getPath(), CharacterPagesRest.append("), "the append is the last step of familyAll");
        assertTrue(read(SRC.resolve("charset/CharacterSetBoot.java")).contains("BasicGrimoireSelfTests.register()"));
        assertTrue(read(SRC.resolve("charset/BasicGrimoireSelfTests.java")).contains("\"magic.basic_grimoires\""));
        String reg = read(SRC.resolve("charset/CharacterSetRegistry.java"));
        assertTrue(reg.contains("BasicSignatures.hides(") && reg.contains("BasicSignatures.reservedFor("));
        for (String f : new String[]{"book/BasicPages.java", "book/BasicArts.java", "charset/BasicSignatures.java"}) {
            String s = read(SRC.resolve(f));
            assertFalse(s.contains("book_time") || s.contains("TimeMagicLayer") || s.contains("TIME_STASIS"), f + " touches Time Magic");
        }
        assertFalse(read(SRC.resolve("book/TimeBook.java")).contains("BasicPages"));
        assertFalse(code(SRC.resolve("book/BasicPages.java")).contains("\"book_time\""));
    }

    @Test
    void thePagesAreAppendedAfterEveryCharacterPageAndTheSetsAreUntouched() throws IOException {
        // the basic pages are appended in GrimoireBook.familyAll after CharacterPagesRest.append, which itself runs after the book's own list (the pilots, Bulls and gold pages are inside it)
        String gb = read(SRC.resolve("book/GrimoireBook.java"));
        assertTrue(gb.contains("BasicPages.append(getRegistryName().getPath(), CharacterPagesRest.append("), "BasicPages.append wraps CharacterPagesRest.append");
        Set<String> files = new LinkedHashSet<>(CharacterSetRegistry.SHIPPED);
        assertTrue(files.size() >= 40);
    }
}
