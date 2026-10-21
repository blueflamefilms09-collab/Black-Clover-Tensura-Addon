package com.newuniverse.nusmp.labyrinth.props;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the weapon rack's decisions are pure and pinned here: the slot poses (no two items overlap, all inside the frame), the level of detail by distance, the modes, the theme-to-family mapping
 * (every region theme has a family), where a rack goes for an armour stand, how a stand's equipment is packed into five slots, which racks are the loot variant, and the switch.
 */
class PropsLogicTest {
    @Test
    void noTwoItemsOverlapAndAllStayInsideTheFrame() {
        for (int i = 0; i < RackLayout.SLOTS; i++) {
            assertTrue(RackLayout.x(i) - RackLayout.halfWidth(i) >= 0f && RackLayout.x(i) + RackLayout.halfWidth(i) <= 1f, "slot " + i + " across");
            assertTrue(RackLayout.y(i) - RackLayout.halfHeight(i) >= 0.3f && RackLayout.y(i) + RackLayout.halfHeight(i) <= RackLayout.FRAME_HEIGHT, "slot " + i + " up");
            assertTrue(RackLayout.z(i) > 0.5f && RackLayout.z(i) < 0.8f, "in front of the rails, inside the plank: slot " + i);
            for (int j = i + 1; j < RackLayout.SLOTS; j++) {
                boolean across = Math.abs(RackLayout.x(i) - RackLayout.x(j)) < RackLayout.halfWidth(i) + RackLayout.halfWidth(j);
                boolean up = Math.abs(RackLayout.y(i) - RackLayout.y(j)) < RackLayout.halfHeight(i) + RackLayout.halfHeight(j);
                assertFalse(across && up, "slots " + i + " and " + j + " overlap");
            }
        }
        assertEquals(3, RackLayout.BAR);
        assertTrue(RackLayout.y(0) > RackLayout.y(RackLayout.BAR), "weapons hang above the shelf");
    }

    @Test
    void theLevelOfDetailFollowsTheDistanceAndNothingIsDrawnBeyondTheViewDistance() {
        assertEquals(5, RackLayout.visibleSlots(0));
        assertEquals(5, RackLayout.visibleSlots(13.9 * 13.9));
        assertEquals(3, RackLayout.visibleSlots(14.1 * 14.1));
        assertEquals(2, RackLayout.visibleSlots(30 * 30));
        assertEquals(0, RackLayout.visibleSlots(40 * 40));
        assertEquals(0, RackLayout.visibleSlots(1e9));
    }

    @Test
    void modesSayWhatAHandMayDoAndAnUnknownIdIsTheSafeOne() {
        assertFalse(RackMode.DECOR.canTake() || RackMode.DECOR.canPut());
        assertTrue(RackMode.LOOT.canTake() && !RackMode.LOOT.canPut());
        assertTrue(RackMode.OPEN.canTake() && RackMode.OPEN.canPut());
        for (RackMode m : RackMode.values()) assertEquals(m, RackMode.byId(m.id()));
        assertEquals(RackMode.DECOR, RackMode.byId(99));
        assertEquals(RackMode.DECOR, RackMode.byId(-1));
    }

    @Test
    void everyRegionThemeHasAFamilyAndAnUnknownOneIsPlain() {
        for (RegionTheme t : RegionTheme.values()) assertTrue(RackFamilies.known(t.id()), "theme " + t.id() + " has no rack family");
        assertEquals(RackFamilies.PLAIN, RackFamilies.family("some_future_theme"));
        assertEquals(RackFamilies.PLAIN, RackFamilies.family(null));
        assertEquals(RackFamilies.PLAIN, RackFamilies.family(""));
        assertEquals(RackFamilies.CRYPT, RackFamilies.family(" Necropolis "));
        assertEquals("racks/armoury", RackFamilies.tablePath(RackFamilies.ARMOURY));
        assertEquals("racks/plain", RackFamilies.tablePath(null));
        // all six families are used by at least one theme
        for (String fam : RackFamilies.ALL) {
            if (fam.equals(RackFamilies.PLAIN)) continue;
            boolean used = false;
            for (RegionTheme t : RegionTheme.values()) used |= RackFamilies.family(t.id()).equals(fam);
            assertTrue(used, "family " + fam + " is used by no theme");
        }
    }

    @Test
    void theRackGoesWhereTheStandStood() {
        assertEquals(RackPlacement.Spot.AT, RackPlacement.target(RackPlacement.Feet.AIR, false));
        assertEquals(RackPlacement.Spot.AT, RackPlacement.target(RackPlacement.Feet.AIR, true));
        assertEquals(RackPlacement.Spot.REPLACE_SLAB, RackPlacement.target(RackPlacement.Feet.BOTTOM_SLAB, false));
        assertEquals(RackPlacement.Spot.ABOVE, RackPlacement.target(RackPlacement.Feet.OTHER, true));
        assertEquals(RackPlacement.Spot.NONE, RackPlacement.target(RackPlacement.Feet.OTHER, false));
    }

    @Test
    void theStandsYawBecomesTheRacksFacing() {
        assertEquals(0, RackPlacement.facing(0f));
        assertEquals(1, RackPlacement.facing(90f));
        assertEquals(2, RackPlacement.facing(180f));
        assertEquals(3, RackPlacement.facing(270f));
        assertEquals(3, RackPlacement.facing(-90f));
        assertEquals(2, RackPlacement.facing(-180f));
        assertEquals(0, RackPlacement.facing(360f));
        assertEquals(0, RackPlacement.facing(44f));
        assertEquals(1, RackPlacement.facing(46f));
        assertEquals(1, RackPlacement.facing(450f));
    }

    @Test
    void aStandsSixPlacesPackIntoFiveSlotsInOrder() {
        // main hand, off hand, head, chest, legs, feet
        assertArrayEqualsInt(new int[]{0, -1, 1, -1, -1, -1}, RackPlacement.pack(new boolean[]{true, false, true, false, false, false}));
        assertArrayEqualsInt(new int[]{0, 1, 2, 3, 4, -1}, RackPlacement.pack(new boolean[]{true, true, true, true, true, true}));   // the boots do not fit
        assertArrayEqualsInt(new int[]{-1, -1, 0, 1, 2, 3}, RackPlacement.pack(new boolean[]{false, false, true, true, true, true}));
        assertArrayEqualsInt(new int[]{-1, -1, -1, -1, -1, -1}, RackPlacement.pack(new boolean[6]));
    }

    private static void assertArrayEqualsInt(int[] want, int[] got) {
        assertEquals(want.length, got.length);
        for (int i = 0; i < want.length; i++) assertEquals(want[i], got[i], "index " + i);
    }

    @Test
    void theLootVariantIsAStablePickWithTheRightShare() {
        for (long k = 0; k < 50; k++) {
            assertFalse(RackPlacement.lootVariant(k, 0.0));
            assertTrue(RackPlacement.lootVariant(k, 1.0));
            assertEquals(RackPlacement.lootVariant(k, 0.3), RackPlacement.lootVariant(k, 0.3), "stable");
        }
        int hits = 0, n = 20000;
        for (long k = 0; k < n; k++) if (RackPlacement.lootVariant(k * 7919L, 0.25)) hits++;
        double share = hits / (double) n;
        assertTrue(share > 0.22 && share < 0.28, "share " + share);
    }

    @Test
    void onlyTheModsOwnFreshStandsConvertAndOnlyWithTheFlagOn() {
        assertTrue(RackPlacement.shouldConvert(true, true, true, false));
        assertFalse(RackPlacement.shouldConvert(false, true, true, false), "flag off: the stands stay");
        assertFalse(RackPlacement.shouldConvert(true, false, true, false), "outside the dungeon nothing is touched");
        assertFalse(RackPlacement.shouldConvert(true, true, false, false), "a stand a player placed is left alone");
        assertFalse(RackPlacement.shouldConvert(true, true, true, true), "a stand loaded from disk is left alone");
    }
}
