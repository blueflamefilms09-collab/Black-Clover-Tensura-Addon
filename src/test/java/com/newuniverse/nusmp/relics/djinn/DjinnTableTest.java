package com.newuniverse.nusmp.relics.djinn;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The roster is data: thirty-six rows (the twelve of the first release, the twenty-four appended in 1.93.0), each inside the bounds the fairness rules
 * set. 1.93.0 buffed the twelve on purpose, so four bounds were widened by the owner's request and are marked below: the rider cap of a row
 * (6.0 to 9.0: the engine still clamps a hit on a player to {@link TraitRules#PLAYER_RIDER_CAP}), the rider share (0.35 to 0.40), the length of an
 * effect (120 to 180 ticks) and the Extreme Magic's reach and damage (20 to 24 blocks, 40 to 48 raw).
 */
class DjinnTableTest {
    private static final String[] FIRST_TWELVE = {"BAAL", "AMON", "VINEA", "FOCALOR", "ZAGAN", "ZEPAR", "FURFUR", "MARCHOSIAS", "SITRI", "BIFRON", "PAIMON", "VASSAGO"};
    private static final String[] NEW_TWENTY_FOUR = {"BAEL", "AGARES", "VALEFOR", "BARBATOS", "GUSION", "VEPAR", "SHAX", "BUER", "ANDREALPHUS", "KIMARIS", "AMDUSIAS", "BELIAL",
            "DECARABIA", "SEERE", "DANTALION", "ANDROMALIUS", "GLASYA_LABOLAS", "HAAGENTI", "CROCELL", "FURCAS", "BALAM", "ALLOCES", "CAMIO", "MURMUR"};

    /** The most one target can take from one Extreme Magic casting, in raw damage, before the balance law (bound widened from 40 to 48 in 1.93.0). */
    private static final double CEILING = 48.0;

    @Test
    void thirtySixDjinnInTheContractOrder() {
        Djinn[] all = Djinn.values();
        assertEquals(FIRST_TWELVE.length + NEW_TWENTY_FOUR.length, all.length);
        assertEquals(36, all.length);
        for (int i = 0; i < FIRST_TWELVE.length; i++) assertEquals(FIRST_TWELVE[i], all[i].name(), "enum entries are only appended: the first twelve keep their ordinals");
        for (int i = 0; i < NEW_TWENTY_FOUR.length; i++) assertEquals(NEW_TWENTY_FOUR[i], all[12 + i].name(), "the twenty-four come after Vassago, in the owner's order");
    }

    @Test
    void theNewRowsAreExactlyWhatTheOwnerSpecified() {
        // name | title | affinity | body | glow
        Object[][] want = {
                {"Bael", "King of the First Throne", Affinity.STRENGTH, 0xC9A227, 0xFFF0A0},
                {"Agares", "Rider of Crocodile Tides", Affinity.WATER, 0x1FA37A, 0xB8FFE0},
                {"Valefor", "Thief of Storm-Hearts", Affinity.LIGHTNING, 0x5A3DB5, 0xE0D2FF},
                {"Barbatos", "Hunter of the Wild Green", Affinity.LIFE, 0x2E7D32, 0xCFFFB0},
                {"Gusion", "Seer of the Tide Mirror", Affinity.WATER, 0x3D7EAA, 0xD6F2FF},
                {"Vepar", "Mermaid Sovereign of Wrecks", Affinity.WATER, 0x00A6B8, 0xBFFCFF},
                {"Shax", "Silencer of the Dove", Affinity.SOUND, 0x8E8EA8, 0xF0F0FF},
                {"Buer", "Healer of the Wheel", Affinity.LIFE, 0x9CCC3B, 0xF4FFC0},
                {"Andrealphus", "Peacock of the Stars", Affinity.LIGHT, 0x2F6FDB, 0xA8D4FF},
                {"Kimaris", "Rider of the Dark Steed", Affinity.STRENGTH, 0x3A2A3F, 0xFF6A8A},
                {"Amdusias", "Maestro of the Unicorn Horn", Affinity.SOUND, 0xD58AF0, 0xFFE3FF},
                {"Belial", "Lord of Hollow Thrones", Affinity.HEAT, 0x7A0F2E, 0xFF7096},
                {"Decarabia", "Star Crab of the Deep", Affinity.LIGHT, 0xE0B45A, 0xFFF4C8},
                {"Seere", "Swift Wind Prince", Affinity.WIND, 0xE8E8C0, 0xFFFFFF},
                {"Dantalion", "Many-Faced Scholar", Affinity.LIGHT, 0xC0A0FF, 0xF2E8FF},
                {"Andromalius", "Seeker of Hidden Things", Affinity.LIGHTNING, 0x2B2B6B, 0x9CA8FF},
                {"Glasya-Labolas", "Hound of Slaughter", Affinity.STRENGTH, 0xA00000, 0xFF9090},
                {"Haagenti", "Alchemist of the Winged Bull", Affinity.STRENGTH, 0xB87333, 0xFFD9A0},
                {"Crocell", "Frost Sage", Affinity.WATER, 0x9ADCF0, 0xFFFFFF},
                {"Furcas", "Old Knight of the Ice Wind", Affinity.WIND, 0xAAB7C8, 0xFFFFFF},
                {"Balam", "Three-Headed Prophet", Affinity.HEAT, 0xD1622A, 0xFFE0A0},
                {"Alloces", "Lion-Eyed Knight", Affinity.HEAT, 0xE8892B, 0xFFF0B0},
                {"Camio", "Songbird of the Crows", Affinity.SOUND, 0x2A3340, 0xA0C8FF},
                {"Murmur", "Death Drummer", Affinity.SOUND, 0x6B3FA0, 0xE8C8FF},
        };
        assertEquals(24, want.length);
        for (int i = 0; i < want.length; i++) {
            Djinn d = Djinn.values()[12 + i];
            assertEquals(want[i][0], d.displayName(), d.name());
            assertEquals(want[i][1], d.title(), d.name());
            assertSame(want[i][2], d.affinity(), d.name());
            assertEquals((int) (Integer) want[i][3], d.color(), d.name() + " body colour");
            assertEquals((int) (Integer) want[i][4], d.glow(), d.name() + " glow colour");
        }
        assertEquals("glasya_labolas", Djinn.GLASYA_LABOLAS.id());
        assertSame(Djinn.GLASYA_LABOLAS, Djinn.byName("glasya_labolas"));
    }

    @Test
    void everyAffinityIsHeldBySomeone() {
        Set<Affinity> seen = EnumSet.noneOf(Affinity.class);
        for (Djinn d : Djinn.values()) seen.add(d.affinity());
        assertEquals(EnumSet.allOf(Affinity.class), seen);
        for (Affinity a : Affinity.values()) {
            int n = 0;
            for (Djinn d : Djinn.values()) if (d.affinity() == a) n++;
            assertTrue(n >= 3, a + " is held by at least three Djinn now that there are thirty-six");
        }
    }

    @Test
    void namesColoursAndFormsAreDistinct() {
        Set<String> names = new HashSet<>(), forms = new HashSet<>(), extremes = new HashSet<>(), ids = new HashSet<>();
        Set<Integer> colours = new HashSet<>();
        for (Djinn d : Djinn.values()) {
            assertTrue(names.add(d.displayName()), d.name());
            assertTrue(forms.add(d.formName()), d.name());
            assertTrue(extremes.add(d.extreme().name()), d.name());
            assertTrue(colours.add(d.color()), d.name() + " colour");
            assertTrue(ids.add(d.id()), d.name());
            assertNotEquals(d.color(), d.glow());
            assertTrue(d.color() >= 0 && d.color() <= 0xFFFFFF && d.glow() >= 0 && d.glow() <= 0xFFFFFF);
            assertEquals(d.name().toLowerCase(), d.id());
            assertTrue(d.id().matches("[a-z_]+"), "an id is a legal item path: " + d.id());
            assertSame(d, Djinn.byName(d.id()));
            assertSame(d, Djinn.byOrdinal(d.ordinal()));
            assertTrue(d.ordinal() + 1 <= 127, "the sync packet writes the ordinal plus one as a byte");
        }
        assertNull(Djinn.byName("nobody"));
        assertNull(Djinn.byOrdinal(-1));
        assertNull(Djinn.byOrdinal(99));
    }

    @Test
    void extremeMagicNamesFollowTheHouseStyle() {
        for (Djinn d : Djinn.values()) {
            String n = d.extreme().name();
            int colon = n.indexOf(": ");
            assertTrue(colon > 0, d.name() + ": \"<Djinn> <Word>: <Epithet>\"");
            String head = n.substring(0, colon);
            assertTrue(head.startsWith(d.displayName() + " "), d.name() + ": the name begins with the Djinn's own");
            assertTrue(head.split(" ").length >= 2, d.name());
            assertTrue(n.length() > colon + 3, d.name());
        }
    }

    @Test
    void halfEquipStaysInsideTheBounds() {
        for (Djinn d : Djinn.values()) {
            Djinn.Trait t = d.trait();
            assertTrue(t.bonusFrac() > 0 && t.bonusFrac() <= 0.40, d.name() + " share (widened from 0.35 in 1.93.0)");
            assertTrue(t.bonusCap() > 0 && t.bonusCap() <= 9.0, d.name() + " cap (widened from 6.0 in 1.93.0; a hit on a player is clamped to " + TraitRules.PLAYER_RIDER_CAP + " by the engine)");
            assertEquals(6.0f, TraitRules.PLAYER_RIDER_CAP, 0f, "30% of a 20 HP player");
            assertNotEquals(Djinn.Special.NONE, t.special(), d.name());
            assertEquals(t.effectId().isEmpty(), t.effectTicks() == 0 && t.effectAmp() == 0 && t.effectId().isEmpty(), d.name());
            assertTrue(t.effectTicks() <= 180, d.name() + ": a short effect (widened from 120 in 1.93.0)");
            assertFalse(t.blurb().isBlank());
        }
    }

    @Test
    void theHalfEquipOfTheTwelveIsBuffedAndNeverWeaker() {
        // old (share, cap, effect ticks) -> the row now
        Object[][] old = {
                {Djinn.BAAL, 0.25, 6.0, 40}, {Djinn.AMON, 0.30, 6.0, 60}, {Djinn.VINEA, 0.20, 5.0, 60}, {Djinn.FOCALOR, 0.25, 6.0, 0}, {Djinn.ZAGAN, 0.20, 5.0, 0},
                {Djinn.ZEPAR, 0.30, 6.0, 60}, {Djinn.FURFUR, 0.20, 5.0, 60}, {Djinn.MARCHOSIAS, 0.25, 6.0, 100}, {Djinn.SITRI, 0.25, 6.0, 60}, {Djinn.BIFRON, 0.25, 6.0, 20},
                {Djinn.PAIMON, 0.25, 6.0, 40}, {Djinn.VASSAGO, 0.25, 6.0, 60}};
        for (Object[] o : old) {
            Djinn d = (Djinn) o[0];
            assertTrue(d.trait().bonusFrac() > (Double) o[1], d.name() + " share grew");
            assertTrue(d.trait().bonusCap() > (Double) o[2], d.name() + " cap grew");
            assertTrue(d.trait().effectTicks() >= (Integer) o[3], d.name() + " effect is not shorter");
        }
    }

    @Test
    void everySpecialIsUsedAndEachNewDjinnHasItsOwn() {
        Set<Djinn.Special> seen = EnumSet.noneOf(Djinn.Special.class);
        for (Djinn d : Djinn.values()) seen.add(d.trait().special());
        Set<Djinn.Special> all = EnumSet.allOf(Djinn.Special.class);
        all.remove(Djinn.Special.NONE);
        assertEquals(all, seen, "every special has a Djinn that uses it");
        Set<Djinn.Special> newOnes = EnumSet.noneOf(Djinn.Special.class);
        for (int i = 12; i < 36; i++) assertTrue(newOnes.add(Djinn.values()[i].trait().special()), Djinn.values()[i].name() + " has a special of its own");
        // the original thirteen constants keep their order (they are never saved, but the lang and the doc name them)
        assertEquals(Djinn.Special.NONE, Djinn.Special.values()[0]);
        assertEquals(Djinn.Special.DROWN, Djinn.Special.values()[12]);
        assertEquals(Djinn.Special.KNEEL, Djinn.Special.values()[13], "the new specials are appended after Drown");
    }

    @Test
    void extremeMagicIsBoundedAndTelegraphed() {
        for (Djinn d : Djinn.values()) {
            Djinn.Extreme x = d.extreme();
            assertTrue(x.radius() >= 8 && x.radius() <= 24, d.name() + " radius (widened from 20 in 1.93.0)");
            assertTrue(x.damage() > 0 && x.damage() <= 40, d.name() + " raw damage per strike (still 40: the balance law's per-hit ceiling)");
            assertTrue(x.damage() * x.shape().maxScale() <= CEILING, d.name() + ": one target never takes more than " + CEILING + " raw in a casting");
            assertTrue(x.effectTicks() <= 180, d.name() + " (widened from 120 in 1.93.0)");
            assertFalse(x.blurb().isBlank());
            assertTrue(DjinnPhaseRules.EXTREME_CHARGE >= 40, "every Extreme Magic gives at least two seconds of warning");
            assertTrue(x.shape().steps() >= 1 && x.shape().steps() * 4 <= 80, d.name() + ": a casting is over in under four seconds");
        }
    }

    @Test
    void theExtremeMagicOfTheTwelveIsBuffedAndNeverWeaker() {
        Object[][] old = {
                {Djinn.BAAL, 14.0, 26.0}, {Djinn.AMON, 12.0, 28.0}, {Djinn.VINEA, 15.0, 22.0}, {Djinn.FOCALOR, 14.0, 20.0}, {Djinn.ZAGAN, 12.0, 16.0}, {Djinn.ZEPAR, 10.0, 30.0},
                {Djinn.FURFUR, 16.0, 20.0}, {Djinn.MARCHOSIAS, 20.0, 32.0}, {Djinn.SITRI, 13.0, 24.0}, {Djinn.BIFRON, 13.0, 24.0}, {Djinn.PAIMON, 14.0, 22.0}, {Djinn.VASSAGO, 15.0, 24.0}};
        for (Object[] o : old) {
            Djinn d = (Djinn) o[0];
            assertTrue(d.extreme().radius() > (Double) o[1], d.name() + " reach grew");
            assertTrue(d.extreme().damage() > (Double) o[2], d.name() + " damage grew");
            assertTrue(d.extreme().damage() <= (Double) o[2] * 1.2, d.name() + ": by no more than a fifth");
        }
    }

    @Test
    void everyShapeIsUsedAndTheNewOnesAreAppended() {
        Set<Djinn.Shape> seen = EnumSet.noneOf(Djinn.Shape.class);
        for (Djinn d : Djinn.values()) seen.add(d.extreme().shape());
        assertEquals(EnumSet.allOf(Djinn.Shape.class), seen, "every shape has a Djinn that casts it");
        assertEquals(Djinn.Shape.HEAL_FIELD, Djinn.Shape.values()[6], "the original seven shapes keep their order");
        assertEquals(Djinn.Shape.RAIN, Djinn.Shape.values()[7], "the new shapes are appended after Heal Field");
        assertEquals(9, Djinn.Shape.BOLTS.steps());
        assertEquals(1, Djinn.Shape.NOVA.steps());
    }

    @Test
    void fullEquipHasAFormToDraw() {
        Set<List<Object>> looks = new HashSet<>();
        for (Djinn d : Djinn.values()) {
            Djinn.Look l = d.look();
            int parts = (l.horns() != Djinn.Look.Horns.NONE ? 1 : 0) + (l.wings() != Djinn.Look.Wings.NONE ? 1 : 0)
                    + (l.tail() != Djinn.Look.Tail.NONE ? 1 : 0) + (l.halo() != Djinn.Look.Halo.NONE ? 1 : 0);
            assertTrue(parts >= 3, d.name() + " is drawn with at least three of horns, wings, tail, halo");
            assertNotEquals(Djinn.Look.Wings.NONE, l.wings(), d.name() + ": every Maso flies");
            assertTrue(looks.add(List.of(l.horns(), l.wings(), l.tail(), l.halo(), l.ears())), d.name() + " has a look of its own (horns, wings, tail, halo, ears)");
        }
        assertEquals(36, looks.size());
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
        for (Djinn d : Djinn.values()) assertFalse(DjinnSkills.forPhase(d.affinity(), 3).isEmpty(), d.name() + " lends skills through its affinity");
    }
}
