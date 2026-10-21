package com.newuniverse.nusmp.client.relics.worn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The frame-time rule that replaced the blank armour texture: an armour relic is never invisible, and its painted layer is hidden only when something else is really drawn. */
class ArmorSlotPlanTest {
    private static final String[] SLOTS = {"HEAD", "CHEST", "FEET"};

    @Test
    void aRelicIsNeverInvisibleInAnyCircumstanceExceptTheTwoThePlayerChose() {
        for (String slot : SLOTS) for (boolean has : new boolean[]{true, false}) for (boolean custom : new boolean[]{true, false}) for (boolean models : new boolean[]{true, false})
            for (boolean far : new boolean[]{true, false}) for (WornPlan.View view : WornPlan.View.values()) {
                ArmorSlotPlan.Verdict v = ArmorSlotPlan.decide(slot, has, custom, models, false, view, far);
                boolean cameraInHead = view == WornPlan.View.SELF_FIRST && slot.equals("HEAD");
                if (!cameraInHead) assertTrue(v.visible(), slot + " has=" + has + " custom=" + custom + " models=" + models + " far=" + far + " view=" + view + " -> " + v);
            }
    }

    @Test
    void theOldFailureTheModelNotDrawnAndTheLayerBlankCannotHappen() {
        // the plan skips the relic (a wearer too far): the painted layer must show
        ArmorSlotPlan.Verdict far = ArmorSlotPlan.decide("HEAD", true, false, true, false, WornPlan.View.OTHER, true);
        assertEquals(ArmorSlotPlan.Layer.PAINTED, far.layer());
        // a model the library cannot dress: the painted layer shows
        ArmorSlotPlan.Verdict none = ArmorSlotPlan.decide("FEET", false, false, true, false, WornPlan.View.OTHER, false);
        assertEquals(ArmorSlotPlan.Layer.PAINTED, none.layer());
        assertEquals(WornPlan.Why.VANILLA_ARMOUR, none.why());
        // a model that is drawn: the vanilla layer draws nothing, no double
        ArmorSlotPlan.Verdict drawn = ArmorSlotPlan.decide("HEAD", true, false, true, false, WornPlan.View.OTHER, false);
        assertEquals(ArmorSlotPlan.Layer.EMPTY, drawn.layer());
        assertTrue(drawn.modelDrawn());
        assertFalse(drawn.glow());
    }

    @Test
    void aChestPieceKeepsItsPaintedBodyUnderTheOrnaments() {
        ArmorSlotPlan.Verdict v = ArmorSlotPlan.decide("CHEST", true, false, true, false, WornPlan.View.OTHER, false);
        assertEquals(ArmorSlotPlan.Layer.PAINTED, v.layer());
        assertTrue(v.glow());
    }

    @Test
    void theArmourSwitchHidesTheHelmetAndBootsButNotTheChestBody() {
        assertEquals(ArmorSlotPlan.Layer.EMPTY, ArmorSlotPlan.decide("HEAD", true, false, true, true, WornPlan.View.OTHER, false).layer());
        assertEquals(ArmorSlotPlan.Layer.EMPTY, ArmorSlotPlan.decide("FEET", false, false, true, true, WornPlan.View.OTHER, false).layer());
        ArmorSlotPlan.Verdict chest = ArmorSlotPlan.decide("CHEST", true, false, true, true, WornPlan.View.OTHER, false);
        assertEquals(ArmorSlotPlan.Layer.PAINTED, chest.layer());
        assertFalse(chest.glow(), "the switch also turns the glow off");
    }

    @Test
    void anOwnRendererStandsInForThePaintedLayerOnlyWhenTheRelicHasAModel() {
        assertEquals(ArmorSlotPlan.Layer.EMPTY, ArmorSlotPlan.decide("HEAD", true, true, true, false, WornPlan.View.SELF_THIRD, false).layer());
        assertEquals(ArmorSlotPlan.Layer.PAINTED, ArmorSlotPlan.decide("HEAD", false, true, true, false, WornPlan.View.SELF_THIRD, false).layer());
    }

    @Test
    void theModelsOptionOffStillDressesAnArmourRelicWithItsModel() {
        ArmorSlotPlan.Verdict v = ArmorSlotPlan.decide("HEAD", true, false, false, false, WornPlan.View.OTHER, false);
        assertTrue(v.modelDrawn(), "armour relics ignore relicfx.models like the plan does");
        assertEquals(ArmorSlotPlan.Layer.EMPTY, v.layer());
    }
}
