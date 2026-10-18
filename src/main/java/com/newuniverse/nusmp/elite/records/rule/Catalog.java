package com.newuniverse.nusmp.elite.records.rule;

import com.newuniverse.nusmp.elite.records.rule.Defs.RecordDef;
import com.newuniverse.nusmp.elite.records.rule.Defs.SynergyDef;
import com.newuniverse.nusmp.elite.records.rule.Defs.TitleDef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the loaded definitions in one immutable snapshot: the records, the titles and the synergies (the built-in ones plus the data ones; a data synergy with a built-in's id replaces it). One
 * snapshot is swapped in at every data reload, so a reader on the server thread never sees half a reload. It also holds the cross-checks no single file can do ({@link #validate}): a title that needs
 * a record that does not exist, a synergy that names a title that does not exist, a title chain that loops, a tag count nobody can reach. The tests run it on the shipped data, and the reload logs its
 * answer, so a typo is found at the desk and not by a player who never gets a title. Pure.
 */
public final class Catalog {
    public static final Catalog EMPTY = new Catalog(List.of(), List.of(), List.of(), false);

    private final Map<String, RecordDef> records = new LinkedHashMap<>();
    private final Map<String, TitleDef> titles = new LinkedHashMap<>();
    private final Map<String, SynergyDef> synergies = new LinkedHashMap<>();

    /** Later definitions replace earlier ones with the same id (so a datapack overrides). Built-in synergies come first. */
    public Catalog(Collection<RecordDef> recs, Collection<TitleDef> tits, Collection<SynergyDef> syns, boolean withBuiltinSynergies) {
        for (RecordDef r : recs) records.put(r.id(), r);
        for (TitleDef t : tits) titles.put(t.id(), t);
        if (withBuiltinSynergies) for (SynergyDef s : BuiltinSynergies.all()) synergies.put(s.id(), s);
        for (SynergyDef s : syns) synergies.put(s.id(), s);
    }

    public Collection<RecordDef> records() { return Collections.unmodifiableCollection(records.values()); }

    public Collection<TitleDef> titles() { return Collections.unmodifiableCollection(titles.values()); }

    public Collection<SynergyDef> synergies() { return Collections.unmodifiableCollection(synergies.values()); }

    public RecordDef record(String id) { return records.get(id); }

    public TitleDef title(String id) { return titles.get(id); }

    public SynergyDef synergy(String id) { return synergies.get(id); }

    public boolean isEmpty() { return records.isEmpty() && titles.isEmpty(); }

    /** The tags of a title, plus the implicit {@code rarity:<id>}; empty for an unknown title. */
    public Set<String> tagsOf(String titleId) {
        Set<String> cached = tagCache.get(titleId);
        if (cached != null) return cached;
        TitleDef t = titles.get(titleId);
        if (t == null) return Set.of();
        Set<String> s = new HashSet<>(t.tags());
        s.add("rarity:" + t.rarity().id());
        Set<String> frozen = Collections.unmodifiableSet(s);
        tagCache.put(titleId, frozen);
        return frozen;
    }

    private final Map<String, Set<String>> tagCache = new java.util.concurrent.ConcurrentHashMap<>();

    /** The points an unlocked record is worth (its rarity's), 0 for an unknown id. */
    public int pointsOf(String recordId) {
        RecordDef r = records.get(recordId);
        return r == null ? 0 : r.rarity().points;
    }

    public int totalPoints() {
        int n = 0;
        for (RecordDef r : records.values()) n += r.rarity().points;
        return n;
    }

    /** Cross-reference problems of the whole catalog (empty when it is sound). */
    public List<String> validate() {
        List<String> out = new ArrayList<>();
        for (TitleDef t : titles.values()) {
            List<String> recIds = new ArrayList<>(), titIds = new ArrayList<>();
            for (Requirement r : t.requirements()) {
                r.collectRecordIds(recIds);
                r.collectTitleIds(titIds);
            }
            for (String id : recIds) if (!records.containsKey(id)) out.add("title " + t.id() + " needs the unknown record '" + id + "'");
            for (String id : titIds) {
                if (!titles.containsKey(id)) out.add("title " + t.id() + " needs the unknown title '" + id + "'");
                if (id.equals(t.id())) out.add("title " + t.id() + " needs itself");
            }
            for (var a : t.bonuses().stats().keySet()) if (!StatNames.ALL.contains(a)) out.add("title " + t.id() + " grants the unknown stat '" + a + "'");
            for (Bonuses.Attr a : t.bonuses().attributes()) if (a.attribute().indexOf(':') < 0) out.add("title " + t.id() + " names the attribute '" + a.attribute() + "' without a namespace");
        }
        for (TitleDef t : titles.values()) if (loops(t.id(), new HashSet<>())) out.add("title " + t.id() + " is part of a requirement loop");
        for (SynergyDef s : synergies.values()) {
            for (String id : s.requires()) if (!titles.containsKey(id)) out.add("synergy " + s.id() + " needs the unknown title '" + id + "'");
            if (!s.tag().isEmpty() && s.tagCount() > 0) {
                int have = 0;
                for (String tid : titles.keySet()) if (s.tag().equals("*") || tagsOf(tid).contains(s.tag())) have++;
                if (have < s.tagCount()) out.add("synergy " + s.id() + " needs " + s.tagCount() + " titles tagged '" + s.tag() + "' but only " + have + " exist");
            }
            for (var a : s.bonuses().stats().keySet()) if (!StatNames.ALL.contains(a)) out.add("synergy " + s.id() + " grants the unknown stat '" + a + "'");
        }
        return out;
    }

    private boolean loops(String id, Set<String> path) {
        if (!path.add(id)) return true;
        TitleDef t = titles.get(id);
        if (t != null) {
            List<String> need = new ArrayList<>();
            for (Requirement r : t.requirements()) r.collectTitleIds(need);
            for (String n : need) if (titles.containsKey(n) && loops(n, new HashSet<>(path))) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ building from raw file texts
    /** One file's text with its name, for the loader. */
    public record Source(String name, String json) {}

    public record Loaded(Catalog catalog, List<String> problems) {}

    /** Parses all files and builds the catalog; problems of single entries and of the cross-check are in the result (the catalog still holds everything that was sound). */
    public static Loaded load(List<Source> recordFiles, List<Source> titleFiles, List<Source> synergyFiles) {
        return load(recordFiles, titleFiles, synergyFiles, true);
    }

    /** Like the three-argument form; {@code withBuiltin = false} leaves the code's synergies out (tests with a small made-up catalog). */
    public static Loaded load(List<Source> recordFiles, List<Source> titleFiles, List<Source> synergyFiles, boolean withBuiltin) {
        List<String> problems = new ArrayList<>();
        List<RecordDef> recs = new ArrayList<>();
        List<TitleDef> tits = new ArrayList<>();
        List<SynergyDef> syns = new ArrayList<>();
        for (Source s : recordFiles) {
            var p = DataParser.records(s.json(), s.name());
            recs.addAll(p.items());
            problems.addAll(p.problems());
        }
        for (Source s : titleFiles) {
            var p = DataParser.titles(s.json(), s.name());
            tits.addAll(p.items());
            problems.addAll(p.problems());
        }
        for (Source s : synergyFiles) {
            var p = DataParser.synergies(s.json(), s.name());
            syns.addAll(p.items());
            problems.addAll(p.problems());
        }
        Catalog c = new Catalog(recs, tits, syns, withBuiltin);
        problems.addAll(c.validate());
        return new Loaded(c, problems);
    }
}
