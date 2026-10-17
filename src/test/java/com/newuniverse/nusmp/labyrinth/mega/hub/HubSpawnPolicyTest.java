package com.newuniverse.nusmp.labyrinth.mega.hub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: "only passive mobs live in the Sanctuary" must hold for every spawn and every stray, and must NOT hold for what an admin asked for or what a player
 * owns. The decisions are plain booleans, so the whole truth table of both is walked here, and the sweep is checked to be the stricter of the two (it
 * cannot know how a mob came, so it needs a mark or a name to spare one).
 */
class HubSpawnPolicyTest {

    @Test
    void aSpawnIsCancelledOnlyForAHostileMobNobodyAskedForAndNobodyOwns() {
        for (boolean passive : new boolean[]{false, true}) {
            for (boolean hostile : new boolean[]{false, true}) {
                for (boolean admin : new boolean[]{false, true}) {
                    for (boolean owned : new boolean[]{false, true}) {
                        boolean want = passive && hostile && !admin && !owned;
                        assertTrue(HubSpawnPolicy.cancels(passive, hostile, admin, owned) == want,
                                "cancels(passive=" + passive + ", hostile=" + hostile + ", admin=" + admin + ", owned=" + owned + ")");
                    }
                }
            }
        }
        assertTrue(HubSpawnPolicy.cancels(true, true, false, false), "the zombie that walks in at night");
        assertFalse(HubSpawnPolicy.cancels(true, false, false, false), "a cow");
        assertFalse(HubSpawnPolicy.cancels(true, true, true, false), "a zombie an admin summoned");
        assertFalse(HubSpawnPolicy.cancels(true, true, false, true), "a hostile creature a player tamed or owns");
        assertFalse(HubSpawnPolicy.cancels(false, true, false, false), "the switch is off");
    }

    @Test
    void theSweepTakesTheStraysAndSparesTheNamedPersistentOwnedAndAllowed() {
        for (int bits = 0; bits < 64; bits++) {
            boolean passive = (bits & 1) != 0, hostile = (bits & 2) != 0, owned = (bits & 4) != 0, named = (bits & 8) != 0, persistent = (bits & 16) != 0, allowed = (bits & 32) != 0;
            boolean want = passive && hostile && !owned && !named && !persistent && !allowed;
            assertTrue(HubSpawnPolicy.sweeps(passive, hostile, owned, named, persistent, allowed) == want, "sweeps bits " + bits);
        }
        assertTrue(HubSpawnPolicy.sweeps(true, true, false, false, false, false), "a stray zombie");
        assertFalse(HubSpawnPolicy.sweeps(true, true, false, true, false, false), "a name-tagged zombie");
        assertFalse(HubSpawnPolicy.sweeps(true, false, false, false, false, false), "a cow");
    }

    @Test
    void whatTheSpawnHandlerLetsThroughTheSweepNeverBlamesAnAdminSpawnedMobAfterTheMarkWasSet() {
        // the handler marks an admin's hostile mob; with that mark the sweep spares it, without it the same mob is a stray
        assertFalse(HubSpawnPolicy.cancels(true, true, true, false), "the admin's spawn passes");
        assertFalse(HubSpawnPolicy.sweeps(true, true, false, false, false, true), "and the mark keeps it");
        assertTrue(HubSpawnPolicy.sweeps(true, true, false, false, false, false), "an unmarked one is a stray");
    }
}
