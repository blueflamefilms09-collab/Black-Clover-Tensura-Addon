package com.newuniverse.nusmp.labyrinth.mega.foundry;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Why: "400 different mobs, 200 for each road" is a promise that must be checked by a machine, in three places that must agree: the admin command ({@code /multiverse mega foundry
 * verify}), the in-game self-test the CI smoke test runs, and the JUnit test that reads the shipped files. They all call this one pure function over a loaded roster and library
 * and print what it returns, one problem per line (empty = the roster keeps its promises): 200 mobs on each road and exactly 40 in each tier of each road; no two mobs with the same
 * name or id; every mob names 2 or 3 abilities and an archetype the data really has, and a faction of the contract; every archetype of the contract exists with a style; every
 * mob has a source, at least 30 different sources are used and none is more than 8 percent of the roster. (The LOOK uniqueness is checked where the looks are made: the python
 * guard tools/foundry/guard.py, which fails the generator.)
 */
public final class FoundryVerify {
    private FoundryVerify() {}

    public static final int PER_ROAD = 200, PER_TIER = 40, MIN_SOURCES = 30;
    public static final double MAX_SHARE = 0.08;

    /** Every problem of the roster against the promises above; {@code full} false relaxes the counts (a roster that is still being written). */
    public static List<String> check(FoundryRoster roster, FoundryLibrary library, boolean full) {
        List<String> bad = new ArrayList<>();
        for (String a : FoundryArchetype.CONTRACT) if (library.archetype(a) == null) bad.add("archetype '" + a + "' of the contract is missing from the library");
        for (FoundryAbility a : library.abilities().values()) if (a.kind() == null) bad.add("ability " + a.id() + " has no kind");
        if (full) {
            for (Polarity p : Polarity.values()) {
                if (roster.count(p) != PER_ROAD) bad.add("road " + p.id() + " has " + roster.count(p) + " mobs, the roster needs " + PER_ROAD);
                for (int t = 1; t <= FoundryScaling.TIERS; t++) {
                    int n = roster.of(p, t).size();
                    if (n != PER_TIER) bad.add("road " + p.id() + " tier " + t + " has " + n + " mobs, the roster needs " + PER_TIER);
                }
            }
        }
        Set<String> names = new HashSet<>();
        Map<String, Integer> sources = new HashMap<>();
        for (FoundryDef d : roster.all()) {
            if (!names.add(d.name().toLowerCase(Locale.ROOT))) bad.add("two mobs are called '" + d.name() + "'");
            if (d.abilities().size() < 2 || d.abilities().size() > 3) bad.add(d.id() + " has " + d.abilities().size() + " abilities, 2 or 3 are wanted");
            for (String a : d.abilities()) if (library.ability(a) == null) bad.add(d.id() + " names the unknown ability " + a);
            if (library.archetype(d.archetype()) == null) bad.add(d.id() + " names the unknown archetype " + d.archetype());
            if (d.source().isBlank()) bad.add(d.id() + " has no source");
            else sources.merge(d.source().trim().toLowerCase(Locale.ROOT), 1, Integer::sum);
        }
        if (full) {
            if (sources.size() < MIN_SOURCES) bad.add("only " + sources.size() + " different sources, at least " + MIN_SOURCES + " are wanted");
            for (Map.Entry<String, Integer> e : sources.entrySet()) {
                if (e.getValue() > MAX_SHARE * roster.size() + 1e-9) bad.add("source '" + e.getKey() + "' has " + e.getValue() + " mobs, more than " + (int) (MAX_SHARE * 100) + " percent of the roster");
            }
        }
        return bad;
    }

    /** Different sources used, for the report. */
    public static int sourceCount(FoundryRoster roster) {
        Set<String> s = new HashSet<>();
        for (FoundryDef d : roster.all()) s.add(d.source().trim().toLowerCase(Locale.ROOT));
        return s.size();
    }
}
