package com.newuniverse.nusmp.labyrinth.mega.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: "without taking magic damage" is the brief's own example, and the classification is the one thing that decides whether the quest is fair: a spell that
 * the mod calls magic must fail it, an arrow, a fall or a zombie must not. Pinned over the vanilla ids and the families of Tensura damage types.
 */
class MagicDamageTest {
    @Test void theTwoVanillaMagicTypesAreMagic() {
        assertTrue(MagicDamage.isMagicId("minecraft:magic"));
        assertTrue(MagicDamage.isMagicId("minecraft:indirect_magic"));
        assertTrue(MagicDamage.isMagicId("magic"), "no colon means the vanilla namespace");
    }

    @Test void ordinaryDamageIsNotMagic() {
        for (String id : new String[] {"minecraft:player_attack", "minecraft:mob_attack", "minecraft:arrow", "minecraft:fall", "minecraft:in_fire", "minecraft:drown",
                "minecraft:explosion", "minecraft:generic", "minecraft:thorns", "minecraft:wither", "minecraft:lava", "minecraft:out_of_world"})
            assertFalse(MagicDamage.isMagicId(id), id);
    }

    @Test void tensuraFamiliesAreMagic() {
        for (String id : new String[] {"tensura:darkness_elemental", "tensura:holy_magic", "tensura:flame_elemental", "tensura:curse", "tensura:spatial_attack",
                "tensura:space_cut", "tensura:dark_slash", "tensura:light_beam", "tensura:magic_generic"})
            assertTrue(MagicDamage.isMagicId(id), id);
    }

    @Test void tensuraPhysicalDamageIsNotMagic() {
        for (String id : new String[] {"tensura:physical", "tensura:claw", "tensura:bite", "tensura:thrust"})
            assertFalse(MagicDamage.isMagicId(id), id);
    }

    @Test void theFamilyWordsApplyOnlyToTensuraButTheWordMagicAppliesEverywhere() {
        assertFalse(MagicDamage.isMagicId("othermod:holy_ground"), "a word of the Tensura family in another mod's type is not enough");
        assertTrue(MagicDamage.isMagicId("othermod:chaos_magic"));
        assertTrue(MagicDamage.isMagicId("othermod:ARCANE_MAGIC"), "case does not matter");
    }

    @Test void nullAndEmptyAreNotMagic() {
        assertFalse(MagicDamage.isMagicId(null));
        assertFalse(MagicDamage.isMagicId(""));
        assertFalse(MagicDamage.isMagicId("minecraft:"));
        assertFalse(MagicDamage.isMagicType(null, null));
        assertFalse(MagicDamage.isMagicType("tensura", ""));
    }
}
