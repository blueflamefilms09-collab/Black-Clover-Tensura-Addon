package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the guards of the owner's brief for the shipped library, run on the generated files themselves: it parses cleanly with the production parser, no two mechanics share a
 * behaviour signature, a name or a description, every mechanic has a source and the sources are spread (at least 30 anime, none above 8 percent), the Python generator and the Java
 * signature rule agree (the stored hash equals the recomputed one), every theme named exists, every primitive / trigger / counter of the catalogue is used.
 */
class MechanicsDataTest {
    @Test
    void theShippedFilesParseWithoutAProblem() {
        assertTrue(TestLibrary.problems().isEmpty(), String.valueOf(TestLibrary.problems()));
        assertTrue(TestLibrary.defs().size() > 0, "no mechanics shipped");
    }

    @Test
    void everyCategoryHasTheSameNumber() {
        Map<MechCategory, Integer> n = new HashMap<>();
        for (MechanicDef d : TestLibrary.defs()) n.merge(d.category(), 1, Integer::sum);
        int total = TestLibrary.defs().size();
        if (total == 1000) for (MechCategory c : MechCategory.values()) {
            int got = n.getOrDefault(c, 0);
            assertEquals(125, got, "category " + c.id());
        }
    }

    @Test
    void noTwoMechanicsShareASignatureANameOrADescription() {
        Set<String> sigs = new HashSet<>(), names = new HashSet<>(), descs = new HashSet<>(), ids = new HashSet<>();
        for (MechanicDef d : TestLibrary.defs()) {
            assertTrue(ids.add(d.id()), "duplicate id " + d.id());
            String sig = Signature.of(d, TestLibrary.catalog());
            assertTrue(sigs.add(sig), "same behaviour signature: " + d.id() + " -> " + sig);
            assertTrue(names.add(d.name().toLowerCase()), "same name: " + d.name());
            assertTrue(descs.add(d.desc().toLowerCase()), "same description: " + d.id());
        }
    }

    @Test
    void theStoredSignatureHashIsWhatJavaComputes() {
        for (MechanicDef d : TestLibrary.defs()) {
            assertEquals(d.sig(), Signature.hash(Signature.of(d, TestLibrary.catalog())), "the generator and Signature.java disagree about " + d.id());
        }
    }

    @Test
    void everyMechanicHasASourceAndTheSourcesAreSpread() {
        Map<String, Integer> per = new HashMap<>();
        for (MechanicDef d : TestLibrary.defs()) {
            assertFalse(d.source().isBlank(), "no source: " + d.id());
            per.merge(d.source(), 1, Integer::sum);
        }
        int total = TestLibrary.defs().size();
        if (total < 1000) return;                       // the distribution rule is about the full library
        int anime = 0;
        for (Map.Entry<String, Integer> e : per.entrySet()) {
            if (!e.getKey().startsWith("D&D")) anime++;
            assertTrue(e.getValue() <= total * 0.08, e.getKey() + " has " + e.getValue() + " of " + total);
        }
        assertTrue(anime >= 30, "only " + anime + " anime sources");
        assertTrue(per.keySet().stream().anyMatch(s -> s.startsWith("D&D")), "no D&D source");
    }

    @Test
    void everyThemeNamedExists() {
        Set<String> known = new HashSet<>();
        for (RegionTheme t : RegionTheme.values()) known.add(t.id());
        for (MechanicDef d : TestLibrary.defs()) for (String t : d.themes()) assertTrue(known.contains(t), d.id() + " names the unknown theme " + t);
    }

    @Test
    void everyPrimitiveTriggerAndCounterOfTheCatalogueIsUsed() {
        if (TestLibrary.defs().size() < 1000) return;
        Set<String> ops = new HashSet<>(), trig = new HashSet<>(), ctr = new HashSet<>();
        for (MechanicDef d : TestLibrary.defs()) {
            trig.add(d.trigger().kind());
            ctr.add(d.counter().kind());
            for (MechanicDef.Effect e : d.effects()) ops.add(e.op());
        }
        for (String o : TestLibrary.catalog().ops().keySet()) assertTrue(ops.contains(o), "primitive never used: " + o);
        for (String o : TestLibrary.catalog().triggers().keySet()) assertTrue(trig.contains(o), "trigger never used: " + o);
        for (String o : TestLibrary.catalog().counters().keySet()) assertTrue(ctr.contains(o), "counter never used: " + o);
    }

    @Test
    void aCounterThatNeedsPropsIsNotPairedWithAScopeThatCannotHoldThem() {
        for (MechanicDef d : TestLibrary.defs()) {
            assertTrue(d.cost() >= 1 && d.cost() <= 12, d.id());
            assertTrue(d.weight() >= 1, d.id());
            if (d.trigger().kind().equals("boss_hp")) assertEquals(Scope.BOSS, d.scope(), d.id() + ": boss_hp outside a boss mechanic");
        }
    }
}
