package com.newuniverse.nusmp.requip;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: a partial armour is the same aura drawn with some of its pieces; the subset, the share of the stats and the number that rides with the aura must be right for all fourteen armours. */
class PartialArmourMaskTest {
    @Test
    void everyPieceOfEveryArmourBelongsToExactlyOneFamily() {
        for (Armour a : Armour.values())
            for (ArmourSets.Piece p : ArmourSets.of(a)) {
                int f = PartialArmourMask.familyOf(p.name());
                assertTrue(f != 0 && Integer.bitCount(f) == 1, a.id() + "/" + p.name() + " has no family");
                assertTrue((f & PartialArmourMask.FULL) == f);
                assertTrue(PartialArmourMask.valueOf(p.name()) > 0, a.id() + "/" + p.name() + " is worth nothing");
            }
        assertEquals(PartialArmourMask.FAMILIES, Integer.bitCount(PartialArmourMask.FULL));
        // every family is used by at least one armour
        Set<Integer> used = new HashSet<>();
        for (Armour a : Armour.values()) for (ArmourSets.Piece p : ArmourSets.of(a)) used.add(PartialArmourMask.familyOf(p.name()));
        assertEquals(PartialArmourMask.FAMILIES, used.size());
        assertEquals(0, PartialArmourMask.familyOf("no_such_piece"));
    }

    @Test
    void theFullMaskKeepsEverythingAndIsWholeArmour() {
        for (Armour a : Armour.values()) {
            assertEquals(ArmourSets.of(a).size(), PartialArmourMask.subset(a, PartialArmourMask.FULL).size());
            assertEquals(1.0, PartialArmourMask.fraction(a, PartialArmourMask.FULL), 1e-9);
            assertEquals(1.0, PartialArmourMask.fraction(a, 0), 1e-9);                 // nothing chosen means the whole armour
            assertTrue(PartialArmourMask.isFull(0));
            assertTrue(PartialArmourMask.flies(a, PartialArmourMask.FULL));
        }
    }

    @Test
    void aSubsetKeepsOnlyTheChosenFamiliesInTheArmoursOwnOrder() {
        int fists = PartialArmourMask.GAUNTLETS | PartialArmourMask.GREAVES;
        List<ArmourSets.Piece> kept = PartialArmourMask.subset(Armour.ADAMANTINE, fists);
        assertEquals(4, kept.size());
        Set<String> names = new HashSet<>();
        for (ArmourSets.Piece p : kept) names.add(p.name());
        assertEquals(Set.of("gauntlet_r", "gauntlet_l", "greave_r", "greave_l"), names);
        // the order is the armour's own
        List<ArmourSets.Piece> all = ArmourSets.of(Armour.ADAMANTINE);
        int last = -1;
        for (ArmourSets.Piece p : kept) { int at = all.indexOf(p); assertTrue(at > last); last = at; }
        assertTrue(PartialArmourMask.includes(fists, "greave_l"));
        assertFalse(PartialArmourMask.includes(fists, "helm"));
        assertFalse(PartialArmourMask.includes(fists, "no_such_piece"));
    }

    @Test
    void theStatFractionIsTheShareOfTheValueKept() {
        for (Armour a : Armour.values()) {
            double sum = 0;
            for (int bit = 0; bit < PartialArmourMask.FAMILIES; bit++) sum += PartialArmourMask.fraction(a, 1 << bit);
            assertEquals(1.0, sum, 1e-9, a.id() + ": the families add up to the whole");
            // more pieces never give less
            assertTrue(PartialArmourMask.fraction(a, PartialArmourMask.GAUNTLETS | PartialArmourMask.GREAVES) > PartialArmourMask.fraction(a, PartialArmourMask.GAUNTLETS));
            assertTrue(PartialArmourMask.fraction(a, PartialArmourMask.FULL & ~PartialArmourMask.HELM) < 1.0);
        }
        // the chest is worth more than the gauntlets, and the gauntlets plus greaves are a small part of Adamantine
        assertTrue(PartialArmourMask.fraction(Armour.ADAMANTINE, PartialArmourMask.CHEST) > PartialArmourMask.fraction(Armour.ADAMANTINE, PartialArmourMask.GAUNTLETS));
        double fists = PartialArmourMask.fraction(Armour.ADAMANTINE, PartialArmourMask.GAUNTLETS | PartialArmourMask.GREAVES);
        assertTrue(fists > 0.10 && fists < 0.35, "gauntlets and greaves: " + fists);
        // a single small piece does not open the special attacks, a good half does
        assertFalse(PartialArmourMask.canFight(Armour.ADAMANTINE, PartialArmourMask.GORGET));
        assertTrue(PartialArmourMask.canFight(Armour.ADAMANTINE, PartialArmourMask.CHEST | PartialArmourMask.HELM));
        assertTrue(PartialArmourMask.canFight(Armour.ADAMANTINE, PartialArmourMask.FULL));
    }

    @Test
    void aWingedArmourFliesOnlyWithItsWings() {
        for (Armour a : Armour.values()) {
            boolean winged = ArmourSets.of(a).stream().anyMatch(p -> p.name().startsWith("wing_"));
            assertEquals(!winged, PartialArmourMask.flies(a, PartialArmourMask.FULL & ~PartialArmourMask.SIGNATURE), a.id());
            assertTrue(PartialArmourMask.flies(a, PartialArmourMask.SIGNATURE), a.id());
        }
    }

    @Test
    void theMaskRidesInTheAuraStyleWithoutTouchingTheArmourNumber() {
        for (Armour a : Armour.values()) {
            // a whole armour is the plain ordinal, exactly as the aura had it before partial sets existed
            assertEquals(a.ordinal(), PartialArmourMask.encode(a.ordinal(), PartialArmourMask.FULL));
            assertEquals(a.ordinal(), PartialArmourMask.encode(a.ordinal(), 0));
            assertEquals(PartialArmourMask.FULL, PartialArmourMask.maskOf(a.ordinal()));
            for (int mask : new int[]{1, 0x41, 0x1C0, 0x3FE, PartialArmourMask.FULL - 1}) {
                int style = PartialArmourMask.encode(a.ordinal(), mask);
                assertEquals(a.ordinal(), PartialArmourMask.armourOf(style));
                assertEquals(mask, PartialArmourMask.maskOf(style));
                assertTrue(style >= 0, "the style stays positive");
            }
        }
        // the old painter read the style as the ordinal; a style from a full set is still that
        assertEquals(Armour.GRAND_CHARIOT, Armour.byOrdinal(PartialArmourMask.encode(Armour.GRAND_CHARIOT.ordinal(), PartialArmourMask.FULL)));
    }

    @Test
    void togglingKeepsAtLeastOneFamily() {
        int m = PartialArmourMask.FULL;
        for (int bit = 0; bit < PartialArmourMask.FAMILIES - 1; bit++) m = PartialArmourMask.toggle(m, bit);
        assertEquals(PartialArmourMask.SIGNATURE, m);
        assertEquals(PartialArmourMask.SIGNATURE, PartialArmourMask.toggle(m, PartialArmourMask.FAMILIES - 1));      // the last one cannot be switched off
        assertEquals(PartialArmourMask.SIGNATURE | PartialArmourMask.HELM, PartialArmourMask.toggle(m, 0));
        assertEquals(PartialArmourMask.FULL, PartialArmourMask.normalize(0));
        assertEquals(PartialArmourMask.FULL, PartialArmourMask.normalize(1 << 20));                     // bits outside the families mean nothing chosen
        assertEquals(PartialArmourMask.FULL, PartialArmourMask.toggle(PartialArmourMask.FULL, 99));
        assertEquals(PartialArmourMask.FAMILIES, PartialArmourMask.count(0));
    }

    @Test
    void thePresetsStartWithTheWholeArmourAndAreAllUsable() {
        assertEquals(PartialArmourMask.FULL, PartialArmourMask.preset(0).mask());
        Set<String> ids = new HashSet<>();
        Set<Integer> masks = new HashSet<>();
        for (PartialArmourMask.Preset p : PartialArmourMask.PRESETS) {
            assertTrue(ids.add(p.id()));
            assertTrue(masks.add(p.mask()), "two presets with the same mask");
            assertTrue(p.mask() > 0 && p.mask() <= PartialArmourMask.FULL);
            assertFalse(p.english().isEmpty());
        }
        for (int i = 0; i < PartialArmourMask.PRESETS.size(); i++) assertEquals(i, PartialArmourMask.presetIndexOf(PartialArmourMask.preset(i).mask()));
        assertEquals(-1, PartialArmourMask.presetIndexOf(PartialArmourMask.HELM));
        assertEquals(PartialArmourMask.preset(0).mask(), PartialArmourMask.preset(PartialArmourMask.PRESETS.size()).mask());       // the cycle wraps
        assertEquals(PartialArmourMask.preset(PartialArmourMask.PRESETS.size() - 1).mask(), PartialArmourMask.preset(-1).mask());
        // the example of the request: only the gauntlets and greaves of Adamantine
        PartialArmourMask.Preset fists = PartialArmourMask.PRESETS.get(1);
        assertEquals("fists_and_boots", fists.id());
        assertEquals(4, PartialArmourMask.subset(Armour.ADAMANTINE, fists.mask()).size());
    }
}
