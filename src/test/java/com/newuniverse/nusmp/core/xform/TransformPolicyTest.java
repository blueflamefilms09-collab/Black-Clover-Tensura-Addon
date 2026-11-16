package com.newuniverse.nusmp.core.xform;

import com.newuniverse.nusmp.core.xform.TransformPolicy.Reason;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the documented policy (docs/transformation_fixes.md) as a table, so a change to it is a visible test change. */
class TransformPolicyTest {
    @Test
    void deathAndRespawnEndFormsOnlyWhileTheSettingIsOn() {
        for (Reason r : Reason.values()) assertFalse(TransformPolicy.endsForms(r, false), r + " must end nothing with the setting off");
        assertTrue(TransformPolicy.endsForms(Reason.DEATH, true));
        assertTrue(TransformPolicy.endsForms(Reason.RESPAWN, true));
    }

    @Test
    void theEndALoginALogoutAndADimensionChangeAreLeftToTheOwner() {
        for (Reason r : new Reason[]{Reason.END_RETURN, Reason.DIMENSION, Reason.LOGIN, Reason.LOGOUT}) {
            assertFalse(TransformPolicy.endsForms(r, true), r + " is not a death");
            assertTrue(TransformPolicy.ownerDecides(r), r + " belongs to the owning system");
        }
        assertFalse(TransformPolicy.ownerDecides(Reason.DEATH));
        assertFalse(TransformPolicy.ownerDecides(Reason.RESPAWN));
    }

    @Test
    void aCloneIsStrippedOnlyAfterADeathWithTheSettingOn() {
        assertTrue(TransformPolicy.stripsClone(true, true));
        assertFalse(TransformPolicy.stripsClone(false, true));
        assertFalse(TransformPolicy.stripsClone(true, false));
        assertFalse(TransformPolicy.stripsClone(false, false));
    }

    @Test
    void theGuardOnlyTouchesThisModsOwnFlagsAndModifiers() {
        assertTrue(TransformPolicy.sweepable(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "unite_speed")));
        assertFalse(TransformPolicy.sweepable(ResourceLocation.fromNamespaceAndPath("minecraft", "sprinting")));
        assertFalse(TransformPolicy.sweepable(ResourceLocation.fromNamespaceAndPath("tensura", "anything")));
        assertFalse(TransformPolicy.sweepable(null));
        assertTrue(TransformPolicy.strippable("nusmp_armour"));
        assertFalse(TransformPolicy.strippable("nusmp_"));
        assertFalse(TransformPolicy.strippable("PlayerPersisted"));
        assertFalse(TransformPolicy.strippable(null));
        assertEquals(6, Reason.values().length, "a new moment needs a line in the policy table and the doc");
    }
}
