package com.newuniverse.nusmp.relics.djinn;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The roster is data: twelve rows, each inside the bounds the fairness rules set. */
class DjinnTableTest {
    @Test
    void twelveDjinnInTheContractOrder() {
        String[] want = {"BAAL", "AMON", "VINEA", "FOCALOR", "ZAGAN", "ZEPAR", "FURFUR", "MARCHOSIAS", "SITRI", "BIFRON", "PAIMON", "VASSAGO"};
        Djinn[] all = Djinn.values();
        assertEquals(want.length, all.length);
        for (int i = 0; i < want.length; i++) assertEquals(want[i], all[i].name(), "enum entries are only appended");
    }

    @Test
    void everyAffinityIsHeldBySomeone() {
        Set<Affinity> seen = EnumSet.noneOf(Affinity.class);
        for (Djinn d : Djinn.values()) seen.add(d.affinity());
        assertEquals(EnumSet.allOf(Affinity.class), seen);
    }

    @Test
    void namesColoursAndFormsAreDistinct() {
        Set<String> names = new HashSet<>(), forms = new HashSet<>(), extremes = new HashSet<>();
        Set<Integer> colours = new HashSet<>();
        for (Djinn d : Djinn.values()) {
            assertTrue(names.add(d.displayName()), d.name());
            assertTrue(forms.add(d.formName()), d.name());
            assertTrue(extremes.add(d.extreme().name()), d.name());
            assertTrue(colours.add(d.color()), d.name() + " colour");
            assertNotEquals(d.color(), d.glow());
            assertTrue(d.color() >= 0 && d.color() <= 0xFFFFFF && d.glow() >= 0 && d.glow() <= 0xFFFFFF);
            assertEquals(d.name().toLowerCase(), d.id());
            assertSame(d, Djinn.byName(d.id()));
            assertSame(d, Djinn.byOrdinal(d.ordinal()));
        }
        assertNull(Djinn.byName("nobody"));
        assertNull(Djinn.byOrdinal(-1));
        assertNull(Djinn.byOrdinal(99));
    }

    @Test
    void halfEquipStaysInsideThePvpCap() {
        for (Djinn d : Djinn.values()) {
            Djinn.Trait t = d.trait();
            assertTrue(t.bonusFrac() > 0 && t.bonusFrac() <= 0.35, d.name() + " share");
            assertTrue(t.bonusCap() > 0 && t.bonusCap() <= 6.0, d.name() + ": 30% of a 20 HP player is 6");
            assertNotEquals(Djinn.Special.NONE, t.special(), d.name());
            assertEquals(t.effectId().isEmpty(), t.effectTicks() == 0 && t.effectAmp() == 0 && t.effectId().isEmpty(), d.name());
            assertTrue(t.effectTicks() <= 120, d.name() + ": a short effect");
            assertFalse(t.blurb().isBlank());
        }
    }

    @Test
    void extremeMagicIsBoundedAndTelegraphed() {
        for (Djinn d : Djinn.values()) {
            Djinn.Extreme x = d.extreme();
            assertTrue(x.radius() >= 8 && x.radius() <= 20, d.name() + " radius");
            assertTrue(x.damage() > 0 && x.damage() <= 40, d.name() + " raw damage");
            assertTrue(x.effectTicks() <= 120, d.name());
            assertFalse(x.blurb().isBlank());
            assertTrue(DjinnPhaseRules.EXTREME_CHARGE >= 40, "every Extreme Magic gives at least two seconds of warning");
        }
    }

    @Test
    void fullEquipHasAFormToDraw() {
        for (Djinn d : Djinn.values()) {
            Djinn.Look l = d.look();
            int parts = (l.horns() != Djinn.Look.Horns.NONE ? 1 : 0) + (l.wings() != Djinn.Look.Wings.NONE ? 1 : 0)
                    + (l.tail() != Djinn.Look.Tail.NONE ? 1 : 0) + (l.halo() != Djinn.Look.Halo.NONE ? 1 : 0);
            assertTrue(parts >= 3, d.name() + " is drawn with at least three of horns, wings, tail, halo");
            assertNotEquals(Djinn.Look.Wings.NONE, l.wings(), d.name() + ": every Maso flies");
        }
    }

    @Test
    void everyPhaseLendsTensuraSkillsThatBuildUp() {
        for (Affinity a : Affinity.values()) {
            assertTrue(DjinnSkills.forPhase(a, 0).isEmpty());
            java.util.List<String> l1 = DjinnSkills.forPhase(a, 1), l2 = DjinnSkills.forPhase(a, 2), l3 = DjinnSkills.forPhase(a, 3);
            assertEquals(java.util.List.of(DjinnSkills.MAGIC_SENSE), l1);
            assertTrue(l2.containsAll(l1) && l3.containsAll(l2) && l3.size() > l2.size() && l2.size() > l1.size(), a.name());
            for (String id : l3) assertTrue(id.matches("tensura:[a-z_]+"), id);
            assertNotEquals(DjinnSkills.manipulation(a), DjinnSkills.domination(a));
        }
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Affinity a : Affinity.values()) assertTrue(seen.add(DjinnSkills.manipulation(a)), "one manipulation skill per affinity");
    }
}
