package com.newuniverse.nusmp.heroweapon.weapon.hammer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Why: Weapon Form Synthesis is the Hammer's canon power-up, and its rule has two edges that are easy to get wrong: only the synthesising form borrows the effects of the forms
 * the wielder has learned, and only learned forms lend theirs. Both edges, and the form with no signature, are pinned.
 */
class HammerSynthesisTest {
    private static final List<String> TOP = List.of("synthesis", "crushing_weight");
    private static final List<String> STONE = List.of("rubble");

    @Test
    void aPlainFormKeepsOnlyItsOwnEffect() {
        assertTrue(HammerSynthesis.effective("rubble", STONE, List.of("base", "stone_hammer", "anvil_hammer")));
        assertFalse(HammerSynthesis.effective("anvil_stance", STONE, List.of("base", "stone_hammer", "anvil_hammer")), "no synthesis, no borrowed effect");
        assertFalse(HammerSynthesis.effective("heavy_blows", STONE, List.of("base", "stone_hammer")), "even the base effect is not borrowed");
    }

    @Test
    void theSynthesisFormKeepsTheEffectOfEveryLearnedForm() {
        List<String> learned = List.of("base", "stone_hammer", "anvil_hammer", "synthesis_hammer");
        assertTrue(HammerSynthesis.effective("heavy_blows", TOP, learned));
        assertTrue(HammerSynthesis.effective("rubble", TOP, learned));
        assertTrue(HammerSynthesis.effective("anvil_stance", TOP, learned));
        assertTrue(HammerSynthesis.effective("crushing_weight", TOP, learned));
    }

    @Test
    void aFormThatWasNeverLearnedLendsNothing() {
        List<String> learned = List.of("base", "anvil_hammer");
        assertFalse(HammerSynthesis.effective("rubble", TOP, learned), "the stone form was never learned");
        assertTrue(HammerSynthesis.effective("anvil_stance", TOP, learned));
    }

    @Test
    void anUnknownFormHasNoSignatureAndAnUnknownPassiveIsNeverInForce() {
        assertNull(HammerSynthesis.signature("mystery"));
        assertNull(HammerSynthesis.signature(null));
        assertFalse(HammerSynthesis.effective("levitation", TOP, List.of("base", "stone_hammer", "anvil_hammer", "synthesis_hammer")));
    }
}
