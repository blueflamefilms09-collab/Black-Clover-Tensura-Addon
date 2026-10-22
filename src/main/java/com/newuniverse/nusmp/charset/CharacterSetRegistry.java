package com.newuniverse.nusmp.charset;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Why: one place that knows every shipped character set and answers the two questions the grimoire asks of it: "is this page hidden for this player's character?" and "when does it open?". Pure (no
 * game classes) so the rules are tested without a game. A character without a file is simply absent here and keeps today's generic grimoire (rule 2: the old stays).
 *
 * <p>The visibility rules ({@link #hidden}):
 * <ul>
 *   <li>A grimoire bound to a character WITH a set shows exactly that set's pages (plus mode 0, the book's default page, which is never hidden).</li>
 *   <li>A grimoire bound to a character WITHOUT a set shows the whole book as before (unfinished characters keep working).</li>
 *   <li>A grimoire bound to nobody (the basic grimoire) hides the pages some character set marks {@code exclusive}: a character's signature spells never appear in the basic book.</li>
 * </ul>
 */
public final class CharacterSetRegistry {
    /** The characters that have a file in data/nusmp/character_spells. A content builder appends the id of a new character here (one line). */
    public static final List<String> SHIPPED = List.of("yuno", "noelle", "fuegoleon", "asta", "yami",
            "vanessa", "charmy", "luck", "magna", "gauche", "gordon", "zora", "grey", "henry", "finral", "nacht", "secre");      // + the Black Bulls (docs/magic/character_spells_bulls.md)
    public static final String RESOURCE_DIR = "/data/nusmp/character_spells/";

    private final Map<String, CharacterSet> byCanon = new LinkedHashMap<>();
    private final Map<String, String> exclusive = new HashMap<>();          // "book|page" -> canon id
    private final Map<String, Set<String>> tracked = new HashMap<>();       // canon -> pages whose casts are counted
    private final List<String> problems = new ArrayList<>();

    public CharacterSetRegistry() {}

    /** Loads the shipped files from the class path; a file with any problem is skipped (and listed in {@link #problems}). */
    public static CharacterSetRegistry loadShipped() {
        return load(SHIPPED, id -> {
            try (InputStream in = CharacterSetRegistry.class.getResourceAsStream(RESOURCE_DIR + id + ".json")) {
                return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (java.io.IOException e) {
                return null;
            }
        });
    }

    public static CharacterSetRegistry load(Collection<String> ids, Function<String, String> reader) {
        CharacterSetRegistry r = new CharacterSetRegistry();
        for (String id : ids) {
            String text = reader.apply(id);
            if (text == null) { r.problems.add(id + ": file not found"); continue; }
            List<String> bad = new ArrayList<>();
            CharacterSet set = CharacterSetLoader.parse(text, bad);
            if (set != null && !set.canon().equals(id)) bad.add("the file declares canon '" + set.canon() + "' but is named " + id + ".json");
            if (set == null || !bad.isEmpty()) { for (String b : bad) r.problems.add(id + ": " + b); continue; }
            r.add(set);
        }
        return r;
    }

    /** Adds a set; a page that another set already marks exclusive is a problem and the set is skipped. */
    public boolean add(CharacterSet set) {
        List<String> clash = new ArrayList<>();
        for (CharacterSet.Spell s : set.spells()) {
            String key = set.book() + "|" + s.page();
            String owner = exclusive.get(key);
            if (s.exclusive() && owner != null && !owner.equals(set.canon())) clash.add(s.page() + " is already exclusive to " + owner);
        }
        if (!clash.isEmpty()) { for (String c : clash) problems.add(set.canon() + ": " + c); return false; }
        byCanon.put(set.canon(), set);
        for (CharacterSet.Spell s : set.spells()) if (s.exclusive()) exclusive.put(set.book() + "|" + s.page(), set.canon());
        Set<String> t = new HashSet<>();
        t.add("spirit_dive");
        for (CharacterSet.Spell s : set.spells()) t.addAll(s.rule().pages());
        tracked.put(set.canon(), t);
        return true;
    }

    public CharacterSet of(String canon) { return canon == null ? null : byCanon.get(canon.toLowerCase(java.util.Locale.ROOT)); }

    public Collection<CharacterSet> all() { return byCanon.values(); }

    public List<String> problems() { return problems; }

    /** The character this page is exclusive to in the given book, or null. */
    public String exclusiveOwner(String book, String page) { return exclusive.get(book + "|" + page); }

    /** The pages whose successful casts are counted for this character (those some rule names, plus the Spirit Dive). Empty without a set. */
    public Set<String> tracked(String canon) {
        CharacterSet s = of(canon);
        return s == null ? Set.of() : tracked.getOrDefault(s.canon(), Set.of());
    }

    /**
     * Is this page of this book hidden for a grimoire bound to {@code canon} (null or empty = the basic grimoire)?
     *
     * @param modeZero         the page is mode 0 of the book: the default page, never hidden
     * @param hideSignatures   the basic grimoire hides exclusive pages (config {@code basicBookHidesSignatures})
     */
    public boolean hidden(String canon, String book, String page, boolean modeZero, boolean hideSignatures) {
        if (modeZero) return false;
        CharacterSet set = of(canon);
        if (set != null) return set.book().equals(book) && !set.has(page);
        if (canon == null || canon.isEmpty()) return hideSignatures && exclusive.containsKey(book + "|" + page);
        return false;
    }

    /**
     * The set's verdict on opening a page for a grimoire of this character: null = the character has no set (the old mastery rule applies), false = not yet (or not his), true = open it now.
     * The rank / race gates and the free-slot rule are NOT decided here.
     */
    public Boolean opens(String canon, String book, String page, RuleFacts facts) {
        CharacterSet set = of(canon);
        if (set == null || !set.book().equals(book)) return null;
        CharacterSet.Spell s = set.spell(page);
        return s != null && s.rule().test(facts);
    }

    /** Does the grimoire of this character list this page (so the old "locked to another grimoire" line must not block it)? */
    public boolean listed(String canon, String book, String page) {
        CharacterSet set = of(canon);
        return set != null && set.book().equals(book) && set.has(page);
    }
}
