package com.newuniverse.nusmp.engraving.codex;

import com.newuniverse.nusmp.franchise.FranchiseData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Why: the Engravement Codex lists EVERY engraving of the mod and what it does, and must never go stale. This pure class turns the data (the franchise engravings table, the mod's own
 * enchantments, the Tensura engravings the mod puts on items, and which items carry each) into the list of entries the screen shows. Nothing is typed by hand: the effect text is the
 * engraving's language text (itself generated from the same table by {@code tools/franchise/gen_engravings.py}), the cooldowns and caps are {@link CodexFacts}, and the "Found on" lists
 * come from the item tables. No Minecraft types, so it is unit tested without the game; the game side that gathers the inputs is {@link CodexGather}.
 * <p>
 * Order of the groups (the contents list): this mod's own enchantments, Tensura Reincarnated's engravings, then one group per franchise in the franchise table's order.
 */
public final class CodexEntries {
    private CodexEntries() {}

    /** Group ids that are not franchises (they start with an underscore because the franchise "tensura" is a real franchise id; their lang keys end in "nusmp" and "tensura"). */
    public static final String GROUP_MOD = "_mod", GROUP_TENSURA = "_tensura_engravings";

    /** What an entry is: an engraving of this mod's own, one of Tensura's, or one of a franchise. */
    public enum Kind { MOD, TENSURA, FRANCHISE }

    /** A group of the contents list. {@code count} is the number of entries in it. */
    public record Group(String id, String name, String shortName, int colour, Kind kind, int count) {}

    /**
     * One engraving of the book. {@code key} is the enchantment id ({@code nusmp:fr_falna}), {@code search} is the lower-case text the search box looks in (name, group, effect text,
     * facts and the items that carry it), {@code foundOn} is the sorted display names of the items that carry it.
     */
    public record Entry(String key, String groupId, Kind kind, String groupName, int colour, String name, String scope, String text, List<String> facts, List<String> foundOn, String search) {}

    /** An enchantment of this mod that is not a franchise engraving. {@code scope} is {@code main} or {@code any}; {@code maxLevel} 0 = unknown. */
    public record Own(String id, String scope, int maxLevel, Collection<String> carriers) {}

    /** A Tensura engraving: its path ({@code swift}), the name the game shows (null = none), its highest level (0 = unknown) and the names of the items of this mod that carry it. */
    public record TensuraRef(String id, String name, int maxLevel, Collection<String> carriers) {}

    /**
     * Everything the book is made of. {@code franchiseCarriers} maps a franchise id to the display names of the items that carry its engravings; {@code lang} answers a language key
     * with its text, or null when it has none (the book then falls back to the data).
     */
    public record Input(FranchiseData.Table franchises, Map<String, ? extends Collection<String>> franchiseCarriers, List<Own> own, List<TensuraRef> tensura,
                        TensuraNotes.Table tensuraNotes, Function<String, String> lang) {}

    /** The whole book: the groups in order and the entries in order (own, then Tensura, then each franchise). */
    public record Book(List<Group> groups, List<Entry> entries) {}

    // ---------------------------------------------------------------- the lang keys the book reads (all with a data fallback)
    public static String nameKey(String ns, String path) { return "enchantment." + ns + "." + path; }

    public static String descKey(String ns, String path) { return "enchantment." + ns + "." + path + ".desc"; }

    public static String franchiseKey(String id) { return "franchise.nusmp." + id; }

    public static String groupKey(String id) { return "nusmp.engraving_codex.group." + id; }

    private static String tr(Function<String, String> lang, String key, String fallback) {
        String v = lang == null ? null : lang.apply(key);
        return v == null || v.isBlank() ? fallback : v;
    }

    /** "magic_weapon" becomes "Magic Weapon". */
    public static String pretty(String path) {
        StringBuilder sb = new StringBuilder();
        for (String w : path.split("_")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    private static List<String> sorted(Collection<String> names) {
        if (names == null || names.isEmpty()) return List.of();
        TreeSet<String> set = new TreeSet<>(Comparator.comparing((String s) -> s.toLowerCase(Locale.ROOT)).thenComparing(Comparator.naturalOrder()));
        for (String n : names) if (n != null && !n.isBlank()) set.add(n);
        return List.copyOf(set);
    }

    private static String searchText(String name, String group, String scope, String text, List<String> facts, List<String> foundOn) {
        StringBuilder sb = new StringBuilder(160 + text.length());
        sb.append(name).append(' ').append(group).append(' ').append(scope).append(' ').append(text);
        for (String f : facts) sb.append(' ').append(f);
        for (String f : foundOn) sb.append(' ').append(f);
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    /** Builds the whole book. Never throws on missing text: every entry gets a name and a text. */
    public static Book build(Input in) {
        Function<String, String> lang = in.lang();
        List<Entry> own = new ArrayList<>();
        List<Entry> tensura = new ArrayList<>();
        List<Entry> franchise = new ArrayList<>();
        List<Group> groups = new ArrayList<>();
        Map<String, List<String>> shared = new LinkedHashMap<>();           // one sorted list per franchise, shared by its engravings

        // ---- this mod's own enchantments (not franchise engravings)
        String modGroup = tr(lang, groupKey("nusmp"), "Multiverse of Anime");
        for (Own o : in.own()) {
            String name = tr(lang, nameKey("nusmp", o.id()), pretty(o.id()));
            String scope = "main".equals(o.scope()) ? CodexFacts.scopeWords("main") : CodexFacts.scopeWords("any");
            String text = tr(lang, descKey("nusmp", o.id()), "An enchantment of Multiverse of Anime.");
            List<String> facts = new ArrayList<>();
            if (o.maxLevel() > 0) facts.add("Highest level: " + o.maxLevel());
            List<String> found = sorted(o.carriers());
            own.add(new Entry("nusmp:" + o.id(), GROUP_MOD, Kind.MOD, modGroup, 0xE8C468, name, scope, text, List.copyOf(facts), found, searchText(name, modGroup, scope, text, facts, found)));
        }
        own.sort(Comparator.comparing(e -> e.name().toLowerCase(Locale.ROOT)));

        // ---- Tensura Reincarnated's engravings
        String tensuraGroup = tr(lang, groupKey("tensura"), "Tensura Reincarnated");
        TensuraNotes.Table notes = in.tensuraNotes();
        for (TensuraRef t : in.tensura()) {
            String name = t.name() != null && !t.name().isBlank() ? t.name() : tr(lang, nameKey("tensura", t.id()), pretty(t.id()));
            String text = tr(lang, descKey("tensura", t.id()), notes.lineOf(t.id()));
            List<String> facts = new ArrayList<>();
            if (t.maxLevel() > 0) facts.add("Levels 1 to " + t.maxLevel() + " in Tensura");
            facts.add(notes.credit());
            String scope = "On the item itself";
            List<String> found = sorted(t.carriers());
            tensura.add(new Entry("tensura:" + t.id(), GROUP_TENSURA, Kind.TENSURA, tensuraGroup, 0x4FB8E8, name, scope, text, List.copyOf(facts), found, searchText(name, tensuraGroup, scope, text, facts, found)));
        }
        tensura.sort(Comparator.comparing(e -> e.name().toLowerCase(Locale.ROOT)));

        // ---- the franchise engravings, franchise by franchise in the table's order
        FranchiseData.Table table = in.franchises();
        List<Group> franchiseGroups = new ArrayList<>();
        for (FranchiseData.Franchise f : table.franchises()) {
            String fname = tr(lang, franchiseKey(f.id()), f.name());
            List<FranchiseData.Engraving> list = table.of(f.id());
            if (list.isEmpty()) continue;
            List<String> found = shared.computeIfAbsent(f.id(), k -> sorted(in.franchiseCarriers().get(f.id())));
            for (FranchiseData.Engraving en : list) {
                String path = en.id();
                String name = tr(lang, nameKey("nusmp", path), en.name());
                String scope = CodexFacts.scopeWords(en.scope());
                String text = CodexFacts.trimBoilerplate(tr(lang, descKey("nusmp", path), CodexFacts.fallbackText(en)));   // the scope and the caps are said by the scope line and the facts
                List<String> facts = CodexFacts.of(en);
                franchise.add(new Entry("nusmp:" + path, f.id(), Kind.FRANCHISE, fname, f.colour(), name, scope, text, facts, found, searchText(name, fname, scope, text, facts, found)));
            }
            franchiseGroups.add(new Group(f.id(), fname, CodexNames.table().getOrDefault(f.id(), fname), f.colour(), Kind.FRANCHISE, list.size()));
        }

        groups.add(new Group(GROUP_MOD, modGroup, tr(lang, groupKey("nusmp.short"), "Mod enchantments"), 0xE8C468, Kind.MOD, own.size()));
        groups.add(new Group(GROUP_TENSURA, tensuraGroup, tr(lang, groupKey("tensura.short"), "Tensura engravings"), 0x4FB8E8, Kind.TENSURA, tensura.size()));
        groups.addAll(franchiseGroups);
        List<Entry> all = new ArrayList<>(own.size() + tensura.size() + franchise.size());
        all.addAll(own);
        all.addAll(tensura);
        all.addAll(franchise);
        return new Book(Collections.unmodifiableList(groups), Collections.unmodifiableList(all));
    }

    /** The "Found on" line: the first {@code max} names, then "+M more"; {@code label}, {@code more} and {@code none} are the translated words (null = English). */
    public static String foundOnLine(List<String> names, int max, String label, String more, String none) {
        String lab = label == null ? "Found on" : label;
        if (names.isEmpty()) return lab + ": " + (none == null ? "no item of this mod carries it yet" : none);
        int shown = Math.max(1, Math.min(max, names.size()));
        StringBuilder sb = new StringBuilder(lab).append(": ");
        for (int i = 0; i < shown; i++) {
            if (i > 0) sb.append(", ");
            sb.append(names.get(i));
        }
        int rest = names.size() - shown;
        if (rest > 0) sb.append(", ").append((more == null ? "+%d more" : more).replace("%d", Integer.toString(rest)));
        return sb.toString();
    }
}
