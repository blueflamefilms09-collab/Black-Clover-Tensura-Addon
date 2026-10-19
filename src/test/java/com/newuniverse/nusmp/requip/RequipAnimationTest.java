package com.newuniverse.nusmp.requip;

import com.newuniverse.nusmp.client.aura.EquipAnimation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: every armour swap and the dismiss of Requip: The Knight must use the Djinn Full Equip's animation ({@code client.aura.EquipAnimation}). The rules class keeps the numbers as plain ints (so the pure
 * tests never load a client class); this test pins them to the animation's own constants and runs the animation's arithmetic over the real piece table: every piece has fully forged in before the shatter
 * window opens and is gone before the dismissed armour's aura ends.
 */
class RequipAnimationTest {
    @Test
    void theShatterWindowAndTheForgeTimeAreTheAnimationsOwn() {
        assertEquals(EquipAnimation.SHATTER_WINDOW, (float) RequipRules.SHATTER_WINDOW, 0f);
        assertEquals(EquipAnimation.FORGE_TICKS + 12f, (float) RequipRules.FORGE_TICKS, 0f);
        assertEquals(RequipRules.SHATTER_WINDOW, RequipRules.SWAP_GAP);
    }

    @Test
    void everyPieceIsFullyOnAfterTheForgeAndGoneWhenTheShatterEnds() {
        for (Armour a : Armour.values()) for (ArmourSets.Piece p : ArmourSets.of(a)) {
            float t = RequipRules.FORGE_TICKS;
            assertEquals(1f, EquipAnimation.assembled(t, 1000f, p.delay()), 1e-6, a.id() + "/" + p.name() + " is on after the forge");
            assertEquals(0f, EquipAnimation.assembled(1000f, 0f, p.delay()), 1e-6, a.id() + "/" + p.name() + " is gone at the end");
            float first = EquipAnimation.assembled(RequipRules.SHATTER_WINDOW + 40f, RequipRules.SHATTER_WINDOW - 0.5f, 0f);
            assertTrue(first == 1f || first < 0f, "the shatter starts only inside the window");
            assertTrue(EquipAnimation.assembled(100f, RequipRules.SHATTER_WINDOW + 0.1f, p.delay()) == 1f, "outside the window it is whole");
        }
    }

    @Test
    void aDismissedArmourGetsExactlyTheShatterWindowOfAura() {
        // the dismiss sets the aura to SHATTER_WINDOW ticks long: at that moment the last piece (the largest delay) has not yet started to shatter, and by the end all are gone
        for (Armour a : Armour.values()) {
            float d = ArmourSets.maxDelay(a);
            assertTrue(EquipAnimation.assembled(500f, RequipRules.SHATTER_WINDOW, d) == 1f, a.id());
            assertEquals(0f, EquipAnimation.assembled(500f + RequipRules.SHATTER_WINDOW, 0f, d), 1e-6, a.id());
        }
    }
}
