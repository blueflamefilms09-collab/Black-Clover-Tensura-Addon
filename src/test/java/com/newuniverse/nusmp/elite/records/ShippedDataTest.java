package com.newuniverse.nusmp.elite.records;

import com.newuniverse.nusmp.elite.records.rule.Bonuses;
import com.newuniverse.nusmp.elite.records.rule.Catalog;
import com.newuniverse.nusmp.elite.records.rule.Category;
import com.newuniverse.nusmp.elite.records.rule.CondType;
import com.newuniverse.nusmp.elite.records.rule.Defs.RecordDef;
import com.newuniverse.nusmp.elite.records.rule.Defs.TitleDef;
import com.newuniverse.nusmp.elite.records.rule.Engine;
import com.newuniverse.nusmp.elite.records.rule.Facts;
import com.newuniverse.nusmp.elite.records.rule.Rarity;
import com.newuniverse.nusmp.elite.records.rule.RecordData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the shipped JSON is the content of the system (the owner asked for 120 records, 60 titles and 15 synergies), and a typo in it is invisible until a player is stuck. This test reads the real files
 * from the source tree and checks what the owner asked for as numbers: the counts, every one of the twenty condition types in use, every rarity and category present, no parse problem, no unknown
 * reference, no loop, sane bonus sizes, and that every title is actually reachable (its requirements can all be satisfied by some set of facts).
 */
class ShippedDataTest {
    private static final Path ROOT = Path.of("src/main/resources/data/nusmp/elite");

    private static List<Catalog.Source> read(String dir, boolean synergies) throws IOException {
        List<Catalog.Source> out = new ArrayList<>();
        Path base = ROOT.resolve(dir);
        try (Stream<Path> s = Files.walk(base)) {
            for (Path p : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".json")).sorted()::iterator) {
                boolean isSyn = p.getParent().getFileName().toString().equals("synergies");
                if (isSyn != synergies) continue;
                out.add(new Catalog.Source(p.toString(), Files.readString(p, StandardCharsets.UTF_8)));
            }
        }
        return out;
    }

    private static Catalog.Loaded load() throws IOException {
        return Catalog.load(read("records", false), read("titles", false), read("titles/synergies", true));
    }

    @Test
    void everythingParsesAndCrossChecksClean() throws IOException {
        Catalog.Loaded l = load();
        assertEquals(List.of(), l.problems());
    }

    @Test
    void theOwnersMinimumCountsAreMet() throws IOException {
        Catalog c = load().catalog();
        assertTrue(c.records().size() >= 120, "records: " + c.records().size());
        assertTrue(c.titles().size() >= 60, "titles: " + c.titles().size());
        assertTrue(c.synergies().size() >= 15, "synergies: " + c.synergies().size());
    }

    @Test
    void allTwentyConditionTypesAreUsedByRecords() throws IOException {
        Catalog c = load().catalog();
        Set<CondType> used = EnumSet.noneOf(CondType.class);
        for (RecordDef r : c.records()) used.add(r.condition().type());
        Set<CondType> missing = EnumSet.allOf(CondType.class);
        missing.removeAll(used);
        assertEquals(Set.of(), missing, "condition types no record uses");
        Map<CondType, Integer> per = new EnumMap<>(CondType.class);
        for (RecordDef r : c.records()) per.merge(r.condition().type(), 1, Integer::sum);
        for (var e : per.entrySet()) assertTrue(e.getValue() >= 2, e.getKey() + " has a single record");
    }

    @Test
    void everyRarityAndCategoryIsPresentForRecordsAndRarityForTitles() throws IOException {
        Catalog c = load().catalog();
        Set<Rarity> rr = EnumSet.noneOf(Rarity.class), tr = EnumSet.noneOf(Rarity.class);
        Set<Category> cats = EnumSet.noneOf(Category.class);
        for (RecordDef r : c.records()) {
            rr.add(r.rarity());
            cats.add(r.category());
        }
        for (TitleDef t : c.titles()) tr.add(t.rarity());
        assertEquals(EnumSet.allOf(Rarity.class), rr);
        assertEquals(EnumSet.allOf(Rarity.class), tr);
        assertEquals(EnumSet.allOf(Category.class), cats);
    }

    @Test
    void namesAreUniqueNonEmptyAndShortEnoughForTheScreen() throws IOException {
        Catalog c = load().catalog();
        Set<String> names = new HashSet<>();
        for (RecordDef r : c.records()) {
            assertTrue(r.name().length() <= 32, r.id() + " name too long");
            assertTrue(names.add(r.name()), "duplicate record name " + r.name());
            assertFalse(r.description().isBlank(), r.id() + " has no description");
            assertTrue(r.description().length() <= 160, r.id() + " description too long");
        }
        Set<String> tn = new HashSet<>();
        for (TitleDef t : c.titles()) {
            assertTrue(t.name().length() <= 28, t.id() + " name too long");
            assertTrue(tn.add(t.name()), "duplicate title name " + t.name());
        }
    }

    @Test
    void titleBonusesAreSmallEnoughToStack() throws IOException {
        Catalog c = load().catalog();
        for (TitleDef t : c.titles()) {
            for (Bonuses.Attr a : t.bonuses().attributes()) {
                switch (a.op()) {
                    case ADD_VALUE -> assertTrue(Math.abs(a.amount()) <= 6.0, t.id() + " " + a.attribute() + " " + a.amount());
                    default -> assertTrue(Math.abs(a.amount()) <= 0.2, t.id() + " " + a.attribute() + " " + a.amount());
                }
            }
            for (var s : t.bonuses().stats().entrySet()) assertTrue(s.getValue() > 0 && s.getValue() <= 15, t.id() + " stat " + s);
            for (Bonuses.Effect e : t.bonuses().effects()) assertTrue(e.amplifier() <= 1, t.id() + " effect amplifier");
        }
    }

    @Test
    void everyRecordAndTitleIsReachableBySomeFacts() throws IOException {
        // a player who has done everything: every number at a huge value, every set full of every id the conditions name
        Catalog c = load().catalog();
        Facts f = new Facts();
        for (String k : new String[]{"deaths", "kills_all", "kills_hostile", "kills_passive", "kills_player", "kills_boss", "distance_all", "distance_walk", "distance_sprint", "distance_swim",
                "distance_fly", "distance_elytra", "distance_boat", "distance_mount", "ep", "single_hit", "forge_count", "ftb_chunks", "calamity_damage", "calamity_kills",
                "walpurgis", "war_kills", "war_wins"}) f.num(k, 1e12);
        f.num("forge_best_quality", 6).num("demon_status", 2).num("hero_status", 2).num("higher_being", 2);
        Set<String> biomes = new HashSet<>(), structures = new HashSet<>(), skills = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            biomes.add("x:b" + i);
            structures.add("x:s" + i);
            skills.add("x:k" + i);
        }
        for (RecordDef r : c.records()) {
            String b = r.condition().str("biome", ""), s = r.condition().str("structure", ""), k = r.condition().str("skill", "");
            if (!b.isEmpty()) biomes.add(b);
            if (!s.isEmpty()) structures.add(s.endsWith("*") ? s.substring(0, s.length() - 1) + "x" : s);
            if (!k.isEmpty()) skills.add(k);
            String e = r.condition().str("entity", "");
            if (!e.isEmpty()) f.count("kills", e.endsWith("*") ? e.substring(0, e.length() - 1) + "x" : e, 1_000_000);
        }
        f.set("biomes", biomes).set("structures", structures).set("skills", skills);
        for (String t : new String[]{"resistance", "intrinsic", "common", "extra", "unique", "ultimate"}) f.set("skills:" + t, skills);
        for (int q = 1; q <= 6; q++) f.count("forge_quality", Integer.toString(q), 100_000);
        RecordData d = new RecordData();
        Engine.run(c, f, d, 10);
        List<String> lockedRecords = new ArrayList<>();
        for (RecordDef r : c.records()) if (!d.isUnlocked(r.id())) lockedRecords.add(r.id());
        assertEquals(List.of(), lockedRecords, "records no facts can unlock");
        List<String> lockedTitles = new ArrayList<>();
        for (TitleDef t : c.titles()) if (!d.holds(t.id())) lockedTitles.add(t.id());
        assertEquals(List.of(), lockedTitles, "titles no facts can earn");
    }

    @Test
    void aFreshPlayerUnlocksNothingAndOneSmallStepUnlocksTheFirstRecord() throws IOException {
        Catalog c = load().catalog();
        RecordData d = new RecordData();
        assertFalse(Engine.run(c, new Facts().markUnavailable("tensura").markUnavailable("ftbchunks").markUnavailable("skills"), d, 1).any());
        Engine.run(c, new Facts().num("kills_all", 10).markUnavailable("tensura").markUnavailable("ftbchunks").markUnavailable("skills"), d, 2);
        assertTrue(d.isUnlocked("kills_10"));
        assertFalse(d.isUnlocked("kills_100"));
    }

    @Test
    void hiddenRecordsExistButAreAMinority() throws IOException {
        Catalog c = load().catalog();
        long hidden = c.records().stream().filter(RecordDef::hidden).count();
        assertTrue(hidden >= 3 && hidden * 10 < c.records().size(), "hidden: " + hidden);
    }

    @Test
    void completionistIsPossible() throws IOException {
        Catalog c = load().catalog();
        TitleDef t = c.title("completionist");
        assertTrue(t != null);
        int need = (int) t.requirements().get(0).number();
        assertTrue(need <= c.records().size() && need > c.records().size() / 2, "need " + need + " of " + c.records().size());
    }
}
