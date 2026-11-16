package com.newuniverse.nusmp.attune;

import com.newuniverse.nusmp.attune.AttuneRules.Element;
import com.newuniverse.nusmp.attune.AttuneRules.Pair;
import com.newuniverse.nusmp.attune.AttuneRules.Power;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The meditation attunement rules: slots, the element table (symmetric), rejection, resonance, suppression, the damage arithmetic and the never-lethal backlash. */
class AttuneRulesTest {
    private static int el(String s) { return AttuneRules.elementsOf(s); }

    private static Power p(String id, int system, String element, int rank) { return new Power(id, system, el(element), rank); }

    @Test
    void slotsAreClampedToTwo() {
        assertEquals(1, AttuneRules.slots(0));
        assertEquals(2, AttuneRules.MAX_SLOTS);
        assertEquals(2, AttuneRules.slots(3));
        assertEquals(2, AttuneRules.slots(99));
    }

    @Test
    void theTableIsSymmetricAndNeverBothOpposedAndAllied() {
        for (Element a : Element.values())
            for (Element b : Element.values()) {
                assertEquals(AttuneRules.pair(AttuneRules.bit(a), AttuneRules.bit(b)), AttuneRules.pair(AttuneRules.bit(b), AttuneRules.bit(a)), a + "/" + b);
                assertFalse(AttuneRules.opposed(a, b) && AttuneRules.allied(a, b), a + "/" + b);
                assertEquals(AttuneRules.opposed(a, b), AttuneRules.opposed(b, a));
            }
        for (Element a : Element.values()) assertEquals(Pair.RESONATE, AttuneRules.pair(AttuneRules.bit(a), AttuneRules.bit(a)), a + " with itself");
    }

    @Test
    void labelsOfTheFourSystemsReadAsTheirElements() {
        assertEquals(AttuneRules.bit(Element.FIRE), el("Fire"));
        assertEquals(AttuneRules.bit(Element.FIRE), el("HEAT"));
        assertEquals(AttuneRules.bit(Element.FIRE), el("FLAME"));
        assertEquals(AttuneRules.bit(Element.LIGHTNING), el("LIGHTNING"));
        assertEquals(AttuneRules.bit(Element.LIGHT), el("Light"));
        assertEquals(AttuneRules.bit(Element.DARK), el("ANTI_MAGIC"));
        assertEquals(AttuneRules.bit(Element.SPACE), el("SPATIAL"));
        assertEquals(AttuneRules.bit(Element.WIND) | AttuneRules.bit(Element.FIRE), el("Storm Fire"));
        assertEquals(AttuneRules.bit(Element.WATER) | AttuneRules.bit(Element.DARK), el("Abyss"));
        assertEquals(0, el("STRENGTH"));
        assertEquals(0, el("Aether"));
        assertEquals(0, el(null));
    }

    @Test
    void theOwnersExampleRejects() {
        Power fireWill = p("beast:flame_serpent_will", AttuneRules.BEAST, "Fire", 2), iceDjinn = p("djinn:x", AttuneRules.DJINN, "frost", 3);
        AttuneRules.Refusal r = AttuneRules.check(List.of(fireWill, iceDjinn), Set.of(fireWill.id(), iceDjinn.id()), 3);
        assertNotNull(r);
        assertEquals("reject", r.reason());
        assertEquals(0, r.first());
        assertEquals(1, r.second());
        assertEquals(Pair.REJECT, AttuneRules.pair(el("Light"), el("Shadow")));
        assertEquals(Pair.REJECT, AttuneRules.pair(el("WATER"), el("FLAME")));
    }

    @Test
    void checkRefusesWhatTheServerMustRefuse() {
        Power a = p("beast:a", AttuneRules.BEAST, "Fire", 2), b = p("djinn:b", AttuneRules.DJINN, "WIND", 3), c = p("devil:c", AttuneRules.DEVIL, "FLAME", 2), d = p("spirit:d", AttuneRules.SPIRIT, "LIGHT", 2);
        Set<String> owned = Set.of("beast:a", "djinn:b", "devil:c", "spirit:d", "beast:e");
        assertNull(AttuneRules.check(List.of(a, b), owned, 2));
        assertEquals("too_many", AttuneRules.check(List.of(a, b, c), owned, 3).reason(), "a Beast Will, a Djinn and a Devil: three is too many, whatever the setting");
        assertEquals("not_owned", AttuneRules.check(List.of(p("beast:z", AttuneRules.BEAST, "", 2)), owned, 2).reason());
        assertEquals("duplicate", AttuneRules.check(List.of(a, a), owned, 2).reason());
        assertNull(AttuneRules.check(List.of(a, p("beast:e", AttuneRules.BEAST, "", 2)), owned, 2), "two Beast Wills are allowed");
        assertNull(AttuneRules.check(List.of(c, p("djinn:n", AttuneRules.DJINN, "", 3)), Set.of("devil:c", "djinn:n"), 2), "a Devil and a Djinn are allowed");
        AttuneRules.Refusal ds = AttuneRules.check(List.of(p("devil:n", AttuneRules.DEVIL, "", 2), p("spirit:n", AttuneRules.SPIRIT, "", 2)), Set.of("devil:n", "spirit:n"), 2);
        assertEquals("devil_spirit", ds.reason(), "a Devil and a Spirit never, even neutral ones");
        assertEquals(0, ds.first());
        assertEquals(1, ds.second());
        assertEquals("devil_spirit", AttuneRules.check(List.of(d, c), owned, 2).reason(), "devil_spirit is told before an element clash");
        assertNull(AttuneRules.check(List.of(), owned, 2));
    }

    @Test
    void resonanceCountsAlliedPairsAndIsCapped() {
        Power fire = p("beast:a", AttuneRules.BEAST, "Fire", 2), wind = p("djinn:b", AttuneRules.DJINN, "WIND", 3), bolt = p("spirit:c", AttuneRules.SPIRIT, "LIGHTNING", 2);
        assertEquals(3, AttuneRules.resonatingPairs(List.of(fire, wind, bolt)));
        assertEquals(1.2, AttuneRules.resonance(2, 0.10), 1e-9);
        assertEquals(1.0 + AttuneRules.MAX_RESONANCE, AttuneRules.resonance(50, 0.30), 1e-9);
        assertEquals(1.0, AttuneRules.resonance(3, Double.NaN), 1e-9);
        assertEquals(0, AttuneRules.resonatingPairs(List.of(fire, p("devil:d", AttuneRules.DEVIL, "WORD", 1))));
    }

    @Test
    void suppressionKeepsTheStrongerAndNeverAClash() {
        Power fireWill = p("beast:a", AttuneRules.BEAST, "Fire", 2), iceDjinn = p("djinn:b", AttuneRules.DJINN, "frost", 3), fireSpirit = p("spirit:c", AttuneRules.SPIRIT, "FLAME", 2);
        assertEquals(Set.of(0), AttuneRules.suppressed(List.of(fireWill, iceDjinn)));
        assertEquals(Set.of(0, 2), AttuneRules.suppressed(List.of(fireWill, iceDjinn, fireSpirit)));
        Power iceWill = p("beast:d", AttuneRules.BEAST, "frost", 2);
        assertEquals(Set.of(1), AttuneRules.suppressed(List.of(fireSpirit, iceWill)), "equal strength: the earlier (attuned first) wins");
        assertTrue(AttuneRules.suppressed(List.of(fireWill, fireSpirit)).isEmpty());
    }

    @Test
    void damageAimsAtATotalAndHoldsThePvpCap() {
        assertEquals(1.0, AttuneRules.extra(25, 25), 1e-9);
        assertEquals(12.0, AttuneRules.extra(12, 1), 1e-9);
        assertEquals(1.0 / 25, AttuneRules.extra(1, 25), 1e-9);
        assertEquals(1.0, AttuneRules.extra(Double.NaN, 0.5), 1e-9);
        assertEquals(120f, AttuneRules.dealt(10f, 12, false, 20f, 0.4), 1e-4);
        assertEquals(10f, AttuneRules.dealt(10f, 12, true, 20f, 0.4), 1e-4);                // 40 % of 20 is 8: held there, but never below the blow itself
        assertEquals(40f, AttuneRules.dealt(10f, 12, true, 100f, 0.4), 1e-4);
        assertEquals(1f, AttuneRules.dealt(25f, 1.0 / 25, false, 20f, 0.4), 1e-4);
        assertEquals(0f, AttuneRules.dealt(0f, 12, false, 20f, 0.4), 1e-9);
    }

    @Test
    void theBacklashIsNeverLethal() {
        assertEquals(AttuneRules.BACKLASH_MAX, AttuneRules.backlash(20f), 1e-6);
        assertEquals(0.5f, AttuneRules.backlash(1.5f), 1e-6);
        assertEquals(0f, AttuneRules.backlash(1f), 1e-6);
        assertEquals(0f, AttuneRules.backlash(Float.NaN), 1e-6);
    }

    @Test
    void cooldownStillnessAndIds() {
        assertEquals(0, AttuneRules.cooldownLeft(0, 100, 600));
        assertEquals(500, AttuneRules.cooldownLeft(100, 200, 600));
        assertEquals(0, AttuneRules.cooldownLeft(100, 700, 600));
        assertTrue(AttuneRules.stillMeditating(0.5, 0, 0.5, 1.5, 100, 200, 2400));
        assertFalse(AttuneRules.stillMeditating(3, 0, 0, 1.5, 100, 200, 2400));
        assertFalse(AttuneRules.stillMeditating(0, 0, 0, 1.5, 100, 5000, 2400));
        assertTrue(AttuneRules.validId("beast:dragon_will"));
        assertTrue(AttuneRules.validId("spirit:lord"));
        assertFalse(AttuneRules.validId("beast:Dragon"));
        assertFalse(AttuneRules.validId("sword:x"));
        assertFalse(AttuneRules.validId(null));
        assertEquals(AttuneRules.DEVIL, AttuneRules.systemOf("devil:lucifero"));
        assertEquals(-1, AttuneRules.systemOf("x"));
    }

    @Test
    void theEquipLimitBlocksTheThirdAndADevilWithASpirit() {
        Power will = p("beast:w", AttuneRules.BEAST, "", 2), djinn = p("djinn:j", AttuneRules.DJINN, "", 3), devil = p("devil:d", AttuneRules.DEVIL, "", 2), spirit = p("spirit:s", AttuneRules.SPIRIT, "", 2);
        // nothing meditated: two out is fine, a third is refused
        assertNull(AttuneRules.gate(List.of(will), djinn, List.of()));
        assertNull(AttuneRules.gate(List.of(will, djinn), djinn, List.of()), "what is already out counts once");
        assertEquals("limit", AttuneRules.gate(List.of(will, djinn), devil, List.of()), "a Beast Will, a Djinn and a Devil: the third is refused");
        // the meditated two always work, an extra does not
        assertNull(AttuneRules.gate(List.of(will, djinn), devil, List.of("devil:d", "beast:w")));
        assertEquals("limit", AttuneRules.gate(List.of(will, devil), djinn, List.of("devil:d", "beast:w")));
        // a Devil and a Spirit: the one not attuned is blocked, else the weaker, else the later
        assertEquals("devil_spirit", AttuneRules.gate(List.of(devil), spirit, List.of()), "equal strength: the newcomer (later) is refused");
        assertNull(AttuneRules.gate(List.of(devil), spirit, List.of("spirit:s")), "the attuned Spirit works ...");
        assertEquals("devil_spirit", AttuneRules.gate(List.of(spirit), devil, List.of("spirit:s")), "... and the Devil beside it does not");
        Power lord = p("spirit:lord", AttuneRules.SPIRIT, "", 3);
        assertNull(AttuneRules.gate(List.of(devil), lord, List.of()), "neither attuned: the stronger works");
        assertEquals("devil_spirit", AttuneRules.gate(List.of(lord), devil, List.of()));
        assertNull(AttuneRules.gate(List.of(will, djinn), null, List.of()));
    }

    @Test
    void oldSavesAreHeldToTheLimitToo() {
        Power will = p("beast:w", AttuneRules.BEAST, "", 2), djinn = p("djinn:j", AttuneRules.DJINN, "", 3), devil = p("devil:d", AttuneRules.DEVIL, "", 2), spirit = p("spirit:s", AttuneRules.SPIRIT, "", 2);
        List<Power> ordered = AttuneRules.order(List.of(will, djinn, devil), List.of("devil:d"));
        assertEquals(List.of(devil, will, djinn), ordered, "attuned first, then the rest as found");
        assertEquals(Set.of(2), AttuneRules.blocked(ordered, List.of("devil:d")), "three out: the attuned one and the first other keep working, the third is blocked");
        assertEquals(Set.of(2), AttuneRules.blocked(AttuneRules.order(List.of(will, djinn, devil), List.of()), List.of()), "three out and nothing meditated: the first two keep working, the third waits for /meditate");
        assertTrue(AttuneRules.blocked(List.of(will, djinn), List.of()).isEmpty());
        assertEquals(Set.of(1), AttuneRules.blocked(AttuneRules.order(List.of(devil, spirit), List.of("devil:d", "spirit:s")), List.of("devil:d", "spirit:s")), "an old save with both attuned: the later of equals is blocked");
    }
}
