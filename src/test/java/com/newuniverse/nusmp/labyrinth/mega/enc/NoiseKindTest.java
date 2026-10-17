package com.newuniverse.nusmp.labyrinth.mega.enc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the noise table is read from vanilla game-event names, and a wrong name is a silent loss of a whole sense of the dungeon. These tests pin the mapping,
 * the ones that must stay silent (damage is counted once, by its own hook), the calibration order and the flags a wandering boss relies on.
 */
class NoiseKindTest {
    @Test
    void gameEventNamesMapToTheirKinds() {
        assertEquals(NoiseKind.STEP_WALK, NoiseKind.byGameEvent("step", false));
        assertEquals(NoiseKind.STEP_SPRINT, NoiseKind.byGameEvent("step", true));
        assertEquals(NoiseKind.BLOCK, NoiseKind.byGameEvent("block_destroy", false));
        assertEquals(NoiseKind.BLOCK, NoiseKind.byGameEvent("block_place", false));
        assertEquals(NoiseKind.DOOR, NoiseKind.byGameEvent("block_open", false));
        assertEquals(NoiseKind.DOOR, NoiseKind.byGameEvent("block_close", false));
        assertEquals(NoiseKind.EXPLOSION, NoiseKind.byGameEvent("explode", false));
        assertEquals(NoiseKind.IMPACT, NoiseKind.byGameEvent("projectile_land", false));
        assertEquals(NoiseKind.SHOT, NoiseKind.byGameEvent("projectile_shoot", false));
        assertEquals(NoiseKind.LAND, NoiseKind.byGameEvent("hit_ground", false));
        assertEquals(NoiseKind.SHRIEK, NoiseKind.byGameEvent("shriek", false));
        assertEquals(NoiseKind.LIGHTNING, NoiseKind.byGameEvent("lightning_strike", false));
        assertEquals(NoiseKind.DEATH, NoiseKind.byGameEvent("entity_die", false));
    }

    @Test
    void aNamespaceIsAcceptedAndUnknownNamesAreSilent() {
        assertEquals(NoiseKind.EXPLOSION, NoiseKind.byGameEvent("minecraft:explode", false));
        assertEquals(NoiseKind.SILENT, NoiseKind.byGameEvent("eat", false));
        assertEquals(NoiseKind.SILENT, NoiseKind.byGameEvent("elytra_glide", false));
        assertEquals(NoiseKind.SILENT, NoiseKind.byGameEvent("", false));
        assertEquals(NoiseKind.SILENT, NoiseKind.byGameEvent(null, true));
        assertEquals(0.0, NoiseKind.SILENT.amount(), 0.0);
    }

    @Test
    void theCalibrationKeepsItsOrder() {
        assertTrue(NoiseKind.STEP_WALK.amount() < NoiseKind.STEP_SPRINT.amount());
        assertTrue(NoiseKind.STEP_SPRINT.amount() < NoiseKind.BLOCK.amount());
        assertTrue(NoiseKind.BLOCK.amount() < NoiseKind.HURT.amount());
        assertTrue(NoiseKind.HURT.amount() < NoiseKind.EXPLOSION.amount());
        assertTrue(NoiseKind.STEP_WALK.earshot() < NoiseKind.EXPLOSION.earshot());
        for (NoiseKind k : NoiseKind.values()) {
            assertTrue(k.amount() >= 0.0, k.name());
            assertEquals(k.amount() > 0.0, k.earshot() > 0, k.name() + ": a noise that cannot be heard anywhere has no amount");
        }
    }

    @Test
    void onlyLoudSuddenThingsAttractABoss() {
        assertFalse(NoiseKind.STEP_SPRINT.burst());
        assertFalse(NoiseKind.DOOR.burst());
        assertTrue(NoiseKind.EXPLOSION.burst());
        assertTrue(NoiseKind.BLOCK.burst());
        assertTrue(NoiseKind.HURT.burst());
        assertFalse(NoiseKind.SILENT.burst());
    }

    @Test
    void footstepsAndDoorsAreThePartysButBlastsAreEveryones() {
        assertTrue(NoiseKind.STEP_SPRINT.playerOnly());
        assertTrue(NoiseKind.BLOCK.playerOnly());
        assertTrue(NoiseKind.DOOR.playerOnly());
        assertFalse(NoiseKind.EXPLOSION.playerOnly());
        assertFalse(NoiseKind.DEATH.playerOnly());
        assertFalse(NoiseKind.ROAR.playerOnly());
    }

    @Test
    void damageAndEatingAreNotGameEventNoisesBecauseTheDamageHookCountsHurtOnce() {
        // a blow is counted by the direct damage hook; counting its game event as well would make every hit twice as loud
        for (String quiet : new String[] {"entity_damage", "eat", "drink", "equip", "entity_interact", "entity_action", "elytra_glide", "flap", "item_interact_start", "block_change", "container_open_x"}) {
            assertEquals(NoiseKind.SILENT, NoiseKind.byGameEvent(quiet, false), quiet);
            assertEquals(NoiseKind.SILENT, NoiseKind.byGameEvent("minecraft:" + quiet, true), quiet);
        }
    }
}
