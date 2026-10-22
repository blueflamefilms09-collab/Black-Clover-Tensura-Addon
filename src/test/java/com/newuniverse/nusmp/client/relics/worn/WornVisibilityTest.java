package com.newuniverse.nusmp.client.relics.worn;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The visibility contract: a worn relic (Curios or armour) is drawn unless one of the listed reasons holds. The whole table (source x slot x eye x options x view x distance x wearer state)
 * is walked by {@link WornVisibility#checkTable()}, which also runs the same circumstances through {@link WornPlan} and {@link ArmorSlotPlan}, the code the layers really use.
 */
class WornVisibilityTest {
    @Test
    void theWholeTableHoldsAndMatchesThePlan() {
        assertNull(WornVisibility.checkTable());
    }

    private static WornVisibility.Result curio(Boolean eye, boolean show, WornPlan.View view, boolean far, boolean spec, boolean inv) {
        return WornVisibility.decide(WornVisibility.Source.CURIOS, "NECK", eye, false, false, show, view, far, spec, inv);
    }

    private static WornVisibility.Result armour(String slot, boolean hide, boolean show, WornPlan.View view, boolean far, boolean custom) {
        return WornVisibility.decide(WornVisibility.Source.ARMOUR, slot, null, custom, hide, show, view, far, false, false);
    }

    @Test
    void aCuriosRelicIsDrawnByDefaultInEveryViewAndTheUnknownEyeCountsAsOn() {
        for (WornPlan.View v : WornPlan.View.values()) {
            assertTrue(curio(null, true, v, false, false, false).visible(), "eye unknown, " + v);
            assertTrue(curio(Boolean.TRUE, true, v, false, false, false).visible(), "eye on, " + v);
        }
    }

    @Test
    void aCuriosRelicIsHiddenOnlyByTheEyeTheSwitchDistanceOrTheWearer() {
        assertEquals(WornVisibility.Reason.CURIOS_EYE_OFF, curio(Boolean.FALSE, true, WornPlan.View.SELF_THIRD, false, false, false).reason());
        assertEquals(WornVisibility.Reason.SHOW_OFF, curio(Boolean.TRUE, false, WornPlan.View.OTHER, false, false, false).reason());
        assertEquals(WornVisibility.Reason.TOO_FAR, curio(Boolean.TRUE, true, WornPlan.View.OTHER, true, false, false).reason());
        assertEquals(WornVisibility.Reason.SPECTATOR, curio(Boolean.TRUE, true, WornPlan.View.OTHER, false, true, false).reason());
        assertEquals(WornVisibility.Reason.INVISIBLE, curio(Boolean.TRUE, true, WornPlan.View.OTHER, false, false, true).reason());
    }

    @Test
    void anArmourRelicKeepsItsPaintedPieceAtAnyDistanceAndInTheInventoryPreview() {
        for (String slot : new String[]{"HEAD", "CHEST", "FEET"}) {
            assertTrue(armour(slot, false, true, WornPlan.View.SELF_THIRD, true, false).visible(), slot + " far");
            assertTrue(armour(slot, false, true, WornPlan.View.OTHER, false, false).visible(), slot + " other player");
            assertTrue(armour(slot, false, true, WornPlan.View.SELF_THIRD, false, false).visible(), slot + " inventory preview / third person");
        }
    }

    @Test
    void theCameraInTheHeadHidesOnlyAHeadPieceWithoutARendererOfItsOwn() {
        assertEquals(WornVisibility.Reason.FIRST_PERSON_HEAD, armour("HEAD", false, true, WornPlan.View.SELF_FIRST, false, false).reason());
        assertTrue(armour("HEAD", false, true, WornPlan.View.SELF_FIRST, false, true).visible(), "a relic that draws itself handles the camera");
        assertTrue(armour("CHEST", false, true, WornPlan.View.SELF_FIRST, false, false).visible());
        assertTrue(armour("FEET", false, true, WornPlan.View.SELF_FIRST, false, false).visible());
        assertTrue(curio(Boolean.TRUE, true, WornPlan.View.SELF_FIRST, false, false, false).visible());
    }

    @Test
    void thePlayersTwoSwitchesAreObeyedAtAnyDistance() {
        assertEquals(WornVisibility.Reason.ARMOUR_OPTION, armour("HEAD", true, true, WornPlan.View.OTHER, true, false).reason());
        assertEquals(WornVisibility.Reason.ARMOUR_OPTION, armour("FEET", true, true, WornPlan.View.OTHER, false, false).reason());
        assertTrue(armour("CHEST", true, true, WornPlan.View.OTHER, false, false).visible(), "the chest piece keeps its painted body under hideArmourRelics, as it always did");
        assertEquals(WornVisibility.Reason.SHOW_OFF, armour("CHEST", false, false, WornPlan.View.OTHER, true, false).reason());
        assertEquals(WornVisibility.Reason.SHOW_OFF, curio(Boolean.TRUE, false, WornPlan.View.OTHER, true, false, false).reason(), "the all-relics switch outranks the distance");
    }

    @Test
    void everyReasonCanBeReachedAndNoneIsMissing() {
        Set<WornVisibility.Reason> seen = EnumSet.noneOf(WornVisibility.Reason.class);
        seen.add(curio(Boolean.TRUE, true, WornPlan.View.OTHER, false, false, false).reason());
        seen.add(curio(Boolean.FALSE, true, WornPlan.View.OTHER, false, false, false).reason());
        seen.add(curio(Boolean.TRUE, false, WornPlan.View.OTHER, false, false, false).reason());
        seen.add(curio(Boolean.TRUE, true, WornPlan.View.OTHER, true, false, false).reason());
        seen.add(curio(Boolean.TRUE, true, WornPlan.View.OTHER, false, true, false).reason());
        seen.add(curio(Boolean.TRUE, true, WornPlan.View.OTHER, false, false, true).reason());
        seen.add(armour("HEAD", true, true, WornPlan.View.OTHER, false, false).reason());
        seen.add(armour("HEAD", false, true, WornPlan.View.SELF_FIRST, false, false).reason());
        assertEquals(EnumSet.allOf(WornVisibility.Reason.class), seen);
    }

    @Test
    void theDecisionAllocatesNothing() {
        assertSame(curio(null, true, WornPlan.View.OTHER, false, false, false), curio(Boolean.TRUE, true, WornPlan.View.SELF_THIRD, false, false, false));
        assertSame(curio(Boolean.FALSE, true, WornPlan.View.OTHER, false, false, false), curio(Boolean.FALSE, true, WornPlan.View.SELF_THIRD, false, false, false));
    }

    @Test
    void theAllRelicsSwitchHidesEverythingInThePlanAndTheEightArgumentContextIsSwitchedOn() {
        WornPlan.Entry c = new WornPlan.Entry("c", "NECK", "AMULET", WornPlan.Source.CURIOS, null, false, true, 3);
        WornPlan.Entry a = new WornPlan.Entry("a", "HEAD", "HELM", WornPlan.Source.ARMOUR, null, false, true, 3);
        WornPlan.Plan off = WornPlan.decide(new WornPlan.Ctx(false, WornPlan.View.OTHER, false, false, false, true, false, WornPlan.Fallback.BOXES, false), List.of(c, a));
        for (WornPlan.Item it : off.all()) {
            assertEquals(WornPlan.Kind.SKIP, it.kind());
            assertEquals(WornPlan.Why.SHOW_OFF, it.why());
        }
        assertTrue(off.isEmpty());
        WornPlan.Plan on = WornPlan.decide(new WornPlan.Ctx(false, WornPlan.View.OTHER, false, false, false, true, false, WornPlan.Fallback.BOXES), List.of(c, a));
        assertEquals(2, on.drawnCount(), "the older constructor means 'shown'");
        ArmorSlotPlan.Verdict v = ArmorSlotPlan.of(off.all().get(1));
        assertEquals(ArmorSlotPlan.Layer.EMPTY, v.layer(), "the painted armour layer is hidden too");
        assertFalse(v.glow());
        assertEquals(ArmorVisible.Outcome.OFF_ON_PURPOSE, ArmorVisible.resolve(v, 0, false, false, false, false), "the failsafe does not draw boxes over the player's choice");
        assertEquals(ArmorSlotPlan.Layer.PAINTED, ArmorSlotPlan.decide("HEAD", true, false, true, false, WornPlan.View.OTHER, true).layer(), "the seven-argument form is unchanged");
        assertEquals(ArmorSlotPlan.Layer.EMPTY, ArmorSlotPlan.decide("HEAD", true, false, true, false, WornPlan.View.OTHER, true, false).layer());
    }

    @Test
    void modelsOffNeverMakesARelicDisappear() {
        for (boolean hasModel : new boolean[]{true, false}) {
            WornPlan.Entry c = new WornPlan.Entry("c", "BELT", "BELT", WornPlan.Source.CURIOS, Boolean.TRUE, false, hasModel, 3);
            WornPlan.Plan p = WornPlan.decide(new WornPlan.Ctx(false, WornPlan.View.OTHER, false, false, false, false, false, WornPlan.Fallback.BOXES, true), List.of(c));
            assertTrue(p.all().get(0).drawn(), "hasModel=" + hasModel);
            assertEquals(WornPlan.Kind.PLAIN_BOX, p.all().get(0).kind());
            assertTrue(ArmorSlotPlan.decide("CHEST", hasModel, false, false, false, WornPlan.View.OTHER, false, true).visible());
        }
    }

    @Test
    void theCullRangeIsTheWornLayersOwn() {
        assertEquals(32.0, WornVisibility.CULL_RANGE);
    }
}
