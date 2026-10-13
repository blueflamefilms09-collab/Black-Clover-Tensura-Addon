package com.newuniverse.nusmp.dungeon.nexus.relic;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The two-relic limit of the Dungeon Relic slots (the owner's design, section 3). */
class RelicLimitTest {
    private record Found(String name, boolean eligible) {}

    private static List<String> names(List<Found> found) {
        return RelicLimit.counted(found, Found::eligible).stream().map(Found::name).toList();
    }

    @Test
    void theCapIsExactlyTwo() { assertEquals(2, RelicLimit.CAP); }

    @Test
    void oneOrTwoRelicsAllCount() {
        assertEquals(List.of(), names(List.of()));
        assertEquals(List.of("band"), names(List.of(new Found("band", true))));
        assertEquals(List.of("band", "sheath"), names(List.of(new Found("band", true), new Found("sheath", true))));
    }

    @Test
    void aThirdOrFourthRelicDoesNotCountFirstTwoFoundWin() {
        List<Found> found = List.of(new Found("a", true), new Found("b", true), new Found("c", true), new Found("d", true));
        assertEquals(List.of("a", "b"), names(found));
    }

    @Test
    void relicsThatCannotWorkFromTheirPlaceDoNotTakeASlot() {
        List<Found> found = List.of(new Found("off-hand chalice with curios", false), new Found("band", true), new Found("sheath", true), new Found("page", true));
        assertEquals(List.of("band", "sheath"), names(found), "the first one never worked, so it never used up a slot");
        assertEquals(List.of("page"), names(List.of(new Found("x", false), new Found("page", true))));
        assertEquals(List.of(), names(List.of(new Found("x", false), new Found("y", false))));
    }

    @Test
    void theOffHandPageCountsOnlyWhenARelicSlotIsFree() {
        Found curiosOne = new Found("curios 1", true), curiosTwo = new Found("curios 2", true), offHandPage = new Found("off-hand page", true);
        assertEquals(List.of("curios 1", "off-hand page"), names(List.of(curiosOne, offHandPage)), "one in a slot and the page in the hand: two");
        assertEquals(List.of("curios 1", "curios 2"), names(List.of(curiosOne, curiosTwo, offHandPage)), "two in the slots: the hand's third does not count");
        assertEquals(List.of("off-hand page"), names(List.of(offHandPage)), "without Curios the hand is the one place");
    }

    // ---------------------------------------------------------------- where a relic may work from, and the two together: what Worn does
    private record Held(Relic relic, boolean offHand) {}

    private static List<Relic> working(boolean curios, Held... held) {
        return RelicLimit.counted(List.of(held), h -> RelicLimit.fits(h.relic(), h.offHand(), curios)).stream().map(Held::relic).toList();
    }

    @Test
    void anyRelicWorksFromACuriosSlot() {
        for (Relic r : Relic.values()) {
            assertTrue(RelicLimit.fits(r, false, true), r.name());
            assertTrue(RelicLimit.fits(r, false, false), r.name() + " (even if Curios is thought absent: it is in a slot)");
        }
    }

    @Test
    void theOffHandTakesThePageAlwaysAndTheOthersOnlyWithoutCurios() {
        for (Relic r : Relic.values()) {
            assertEquals(r == Relic.GAMBLERS_GRIMOIRE_PAGE, RelicLimit.fits(r, true, true), r.name() + " in the off-hand, with Curios");
            assertTrue(RelicLimit.fits(r, true, false), r.name() + " in the off-hand, without Curios");
        }
    }

    @Test
    void neverMoreThanTwoWorkWhateverTheyAreWornIn() {
        Held band = new Held(Relic.BAND_OF_THE_SPATIAL_ANCHOR, false), sheath = new Held(Relic.DEVIL_BOUND_SHEATH, false), amulet = new Held(Relic.AMULET_OF_THE_JUGGERNAUTS_BLINDNESS, false);
        Held chalice = new Held(Relic.GLUTTONS_CHALICE, false);
        assertEquals(List.of(Relic.BAND_OF_THE_SPATIAL_ANCHOR, Relic.DEVIL_BOUND_SHEATH), working(true, band, sheath, amulet, chalice), "four in Curios slots: the first two");
        Held page = new Held(Relic.GAMBLERS_GRIMOIRE_PAGE, true), offChalice = new Held(Relic.GLUTTONS_CHALICE, true);
        assertEquals(List.of(Relic.BAND_OF_THE_SPATIAL_ANCHOR, Relic.GAMBLERS_GRIMOIRE_PAGE), working(true, band, page), "a slot and the page in the hand: two");
        assertEquals(List.of(Relic.BAND_OF_THE_SPATIAL_ANCHOR, Relic.DEVIL_BOUND_SHEATH), working(true, band, sheath, page), "the hand's page is the third: it waits");
        assertEquals(List.of(Relic.BAND_OF_THE_SPATIAL_ANCHOR), working(true, band, offChalice), "with Curios a chalice in the hand never worked");
        assertEquals(List.of(Relic.GLUTTONS_CHALICE), working(false, offChalice), "without Curios the hand is the one place: one relic");
        for (int n = 0; n <= 6; n++) {
            Held[] all = new Held[n];
            for (int i = 0; i < n; i++) all[i] = new Held(Relic.values()[i % Relic.values().length], false);
            assertTrue(working(true, all).size() <= 2, n + " relics in Curios slots");
        }
    }

    @Test
    void theInputIsNotChangedAndTheResultIsAFreshList() {
        List<Found> found = List.of(new Found("a", true), new Found("b", true), new Found("c", true));
        List<Found> got = RelicLimit.counted(found, Found::eligible);
        assertEquals(3, found.size());
        assertEquals(2, got.size());
        got.add(new Found("z", true));                                                                 // modifiable: Worn may reorder or extend it
        assertEquals(3, got.size());
    }
}
