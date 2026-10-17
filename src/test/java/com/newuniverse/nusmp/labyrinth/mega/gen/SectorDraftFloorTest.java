package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: in the floor model (1.58.0) a sector is one floor, but a THEME belongs to a decade of floors (1..10, 11..20 ...), so which theme a floor wears is decided in
 * three different ways depending on where it stands: the first floor of a road wears the road's signature, the first floor of a later decade draws one (or takes the one the
 * Crucible's relic chose), and every other floor keeps the theme of the floor before. A mistake here changes the whole look of a road without any test of the generator
 * noticing, so each rule is written out longhand, next to the unchanged rule of the banded model it must not disturb.
 */
class SectorDraftFloorTest {
    private static final long SEED = 0xF100DL;

    @Test
    void theFirstFloorOfARoadWearsTheRoadsSignatureTheme() {
        assertEquals(RegionTheme.RADIANT, SectorDraft.first(SEED, 0, Polarity.HOLY, 1).theme());
        assertEquals(RegionTheme.ABYSS, SectorDraft.first(SEED, 1, Polarity.ABYSSAL, 1).theme());
        assertEquals(1, SectorDraft.first(SEED, 0, Polarity.HOLY, 1).perRegion());
        assertEquals(10, SectorDraft.first(SEED, 0, Polarity.HOLY).perRegion(), "the one-argument form is still the ten-floor sector of 1.54.0");
    }

    @Test
    void theRestOfADecadeKeepsTheThemeOfTheFloorBeforeWhatTheCrucibleSaid() {
        for (int k = 1; k < 10; k++) {
            SectorDraft d = SectorDraft.of(SEED, k, Polarity.HOLY, k, 1, RegionTheme.FROST, RegionTheme.BLOOD);
            assertEquals(RegionTheme.FROST, d.theme(), "floor " + (k + 1) + " is inside the first decade: the relic of an earlier vault does not change it");
        }
        for (int k = 11; k < 20; k++) {
            SectorDraft d = SectorDraft.of(SEED, k, Polarity.ABYSSAL, k, 1, RegionTheme.SHADOW, RegionTheme.FLAME);
            assertEquals(RegionTheme.SHADOW, d.theme(), "floor -" + (k + 1) + " is inside the second decade");
        }
    }

    @Test
    void theFirstFloorOfALaterDecadeTakesTheCrucibleChoiceElseDrawsOneThatIsNotTheLast() {
        SectorDraft forced = SectorDraft.of(SEED, 10, Polarity.HOLY, 10, 1, RegionTheme.RADIANT, RegionTheme.STORM);
        assertEquals(RegionTheme.STORM, forced.theme(), "floor +11 wears the theme the relic of the floor +10 vault chose");
        for (int decade = 1; decade < 12; decade++) {
            int k = decade * 10;
            SectorDraft a = SectorDraft.of(SEED, k, Polarity.HOLY, k, 1, RegionTheme.FROST, null);
            SectorDraft b = SectorDraft.of(SEED, k + 5, Polarity.HOLY, k, 1, RegionTheme.FROST, null);
            assertEquals(a.theme(), b.theme(), "the draw belongs to the place, not to the global id");
            assertTrue(RegionTheme.FROST != a.theme(), "decade " + decade + " never repeats the theme before it");
            assertEquals(RegionTheme.pick(SEED, decade, RegionTheme.FROST, Polarity.HOLY), a.theme(), "the draw is the one made for the decade index");
        }
    }

    @Test
    void aWholeRoadChangesThemeOnlyWhereADecadeBegins() {
        for (Polarity road : Polarity.values()) {
            RegionTheme previous = null;
            int changes = 0;
            for (int k = 0; k < 120; k++) {
                RegionTheme t = SectorDraft.of(SEED, k, road, k, 1, previous, null).theme();
                if (previous != null && t != previous) {
                    changes++;
                    assertTrue(FloorMap.startsDecade(k, 1), road.id() + " floor " + (k + 1) + " changed theme in the middle of a decade");
                }
                if (k % 10 != 0 && previous != null) assertEquals(previous, t, road.id() + " floor " + (k + 1));
                if (k % 10 == 0 && k > 0) assertTrue(previous != t, road.id() + " floor " + (k + 1) + " opens a decade and must not repeat the last theme");
                previous = t;
            }
            assertEquals(11, changes, road.id() + ": twelve decades, a change at each of the eleven openings after the first");
        }
    }

    @Test
    void theRoadsKeepTheirOwnSignatureThroughTheFirstDecade() {
        RegionTheme holy = null, abyss = null;
        for (int k = 0; k < 10; k++) {
            holy = SectorDraft.of(SEED, k, Polarity.HOLY, k, 1, holy, null).theme();
            abyss = SectorDraft.of(SEED, k, Polarity.ABYSSAL, k, 1, abyss, null).theme();
            assertEquals(RegionTheme.RADIANT, holy, "holy floor " + (k + 1));
            assertEquals(RegionTheme.ABYSS, abyss, "abyssal floor " + (k + 1));
        }
    }

    @Test
    void aFloorIsDraftedWithItsPlacesSeedThreatAndOneFloor() {
        for (int k = 0; k < 25; k++) {
            for (Polarity road : Polarity.values()) {
                SectorDraft d = SectorDraft.of(SEED, 99, road, k, 1, RegionTheme.GRAVITY, null);
                assertEquals(1, d.perRegion());
                assertEquals(k, d.chainIndex());
                assertEquals(k + 1, d.threat(), "the threat of a floor is its distance from Floor 0");
                assertEquals(SectorPlanner.sectorSeed(SEED, SectorDraft.seedKey(road, k)), d.seed());
                assertEquals(d.theme().relicCount(SEED, SectorDraft.seedKey(road, k)), d.relicCount());
                assertEquals(FloorMap.floorOfSector(road, k), FloorMap.firstFloor(road, d.chainIndex(), d.perRegion()));
            }
        }
    }

    @Test
    void theBandedModelIsUnchanged() {
        SectorDraft forced = SectorDraft.of(SEED, 3, Polarity.HOLY, 3, 10, RegionTheme.FROST, RegionTheme.BLOOD);
        assertEquals(RegionTheme.BLOOD, forced.theme(), "ten floors per sector: the relic's choice forces the next sector's theme, as in 1.55.0");
        SectorDraft drawn = SectorDraft.of(SEED, 3, Polarity.HOLY, 3, 10, RegionTheme.FROST, null);
        assertEquals(RegionTheme.pick(SEED, 3, RegionTheme.FROST, Polarity.HOLY), drawn.theme(), "and the draw is made per sector, not per decade");
        assertEquals(10, drawn.perRegion());
        assertEquals(31, drawn.threat());
    }
}
