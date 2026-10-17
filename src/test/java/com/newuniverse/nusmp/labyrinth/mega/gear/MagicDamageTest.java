package com.newuniverse.nusmp.labyrinth.mega.gear;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: "is this damage magic" decides whether Rare gear helps, and the answer must be right for the damage the mod really meets: vanilla magic and
 * indirect magic (witches, evokers, potions), the Tensura elemental and spell types, and NOT a sword, an arrow, a fall, fire, the mod's own neutral
 * {@code thorns} hits, nor Tensura's status, mental, spiritual and physical-skill types. The list of Tensura's damage types below is the constant set of
 * its {@code TensuraDamageTypes} class from the API dump (the registry path is the constant's name in lower case; the real path may differ in a character
 * or two, and the rule splits a path at {@code _ / . -} so it tolerates that). Every one of them is classified here ON PURPOSE, so a type added by a
 * Tensura update fails this test until somebody decides what it is.
 */
class MagicDamageTest {

    private static final Set<String> SPELLS = Set.of(
            "darkness_elemental", "earth_elemental", "fire_elemental", "gravity_elemental", "ice_elemental", "light_elemental", "lightning_elemental",
            "space_elemental", "water_elemental", "wind_elemental", "curse", "holy_damage", "bullet_magic", "black_flame", "black_lightning", "blood_ray",
            "dimension_ray", "flame_breath", "gravity_explode", "gravity_press", "heat_wave", "ice_breath", "lightning", "magic_generic", "megiddo",
            "poisonous_breath", "thunder_breath", "water_blade", "water_breath", "wicked_light_ray", "wind_breath");

    private static final Set<String> NOT_SPELLS = Set.of(
            "corrosion", "fatal_poison", "fear", "infection", "insanity", "magicule_poison", "petrification", "soul_scatter", "suffocate", "bullet", "kunai",
            "spear", "severer_blade", "tempest_scale", "unicorn_horn", "aura_bullet", "aura_slash", "blood_drain", "burn", "death_tornado", "death_bless",
            "death_wish", "devoured", "disintegration", "drowsy_death", "energy_drain", "energy_source_lost", "hazy_blossom_thrust", "heart_eat",
            "infinite_eater", "mind_crush", "mind_requiem", "paralyzing", "reflected", "soul_consumed", "severance", "sound_blast", "synthesise",
            "steel_thread", "suicide");

    @Test
    void vanillaSpellsAreMagicAndEverythingElseOfVanillaIsNot() {
        assertTrue(MagicDamage.isMagicType("minecraft", "magic"));
        assertTrue(MagicDamage.isMagicType("minecraft", "indirect_magic"));
        assertTrue(MagicDamage.isMagicType("minecraft", "dragon_breath"));
        for (String p : List.of("player_attack", "mob_attack", "arrow", "fall", "on_fire", "in_fire", "lava", "drown", "starve", "thorns", "wither", "sonic_boom", "explosion",
                "player_explosion", "lightning_bolt", "generic", "generic_kill", "out_of_world", "freeze", "cactus", "fireball", "trident", "mob_projectile", "thrown"))
            assertFalse(MagicDamage.isMagicType("minecraft", p), "minecraft:" + p + " is not a spell");
    }

    @Test
    void everyTensuraDamageTypeIsClassifiedOnPurpose() {
        List<String> all = GearFx.lines("tensura_damage_types.txt");
        assertEquals(SPELLS.size() + NOT_SPELLS.size(), all.size(), "every Tensura damage type must be in exactly one of this test's two lists; the dump has " + all.size());
        for (String id : all) {
            assertTrue(id.startsWith("tensura:"));
            String path = id.substring("tensura:".length());
            assertTrue(SPELLS.contains(path) ^ NOT_SPELLS.contains(path), id + " must be in exactly one of the two lists");
            assertEquals(SPELLS.contains(path), MagicDamage.isMagicType("tensura", path), id);
        }
    }

    @Test
    void theRuleSplitsAtTheWordsAndNotInsideThem() {
        assertFalse(MagicDamage.isMagicType("tensura", "magicule_poison"), "magicule is not magic");
        assertFalse(MagicDamage.isMagicType("tensura", "rayon"));
        assertFalse(MagicDamage.isMagicType("tensura", "arrayed"));
        assertTrue(MagicDamage.isMagicType("tensura", "elemental/darkness"));
        assertTrue(MagicDamage.isMagicType("tensura", "fire.elemental"));
        assertTrue(MagicDamage.isMagicType("tensura", "Fire_Elemental"), "case does not matter");
        assertTrue(MagicDamage.isMagicType("TENSURA", "holy_damage"));
    }

    @Test
    void otherNamespacesAndNullsAreNotMagic() {
        assertFalse(MagicDamage.isMagicType("nusmp", "conceptual_severance"));
        assertFalse(MagicDamage.isMagicType("nusmp", "magic"));
        assertFalse(MagicDamage.isMagicType("othermod", "fire_elemental"));
        assertFalse(MagicDamage.isMagicType(null, "magic"));
        assertFalse(MagicDamage.isMagicType("minecraft", null));
        assertFalse(MagicDamage.matchesExtra(null, "x", List.of("a:b")));
        assertFalse(MagicDamage.matchesExtra("a", null, List.of("a:b")));
    }

    @Test
    void theOwnersExtraListIsParsedAndMatched() {
        assertEquals(List.of(), MagicDamage.parseExtra(null));
        assertEquals(List.of(), MagicDamage.parseExtra(""));
        assertEquals(List.of(), MagicDamage.parseExtra("  ,, "));
        List<String> extra = MagicDamage.parseExtra(" MyMod:Arcane_Bolt, othermod:*  tensura:death_tornado,nonsense ,");
        assertEquals(List.of("mymod:arcane_bolt", "othermod:*", "tensura:death_tornado"), extra, "a piece with no colon is not an id and is dropped");
        assertTrue(MagicDamage.matchesExtra("mymod", "arcane_bolt", extra));
        assertTrue(MagicDamage.matchesExtra("MyMod", "Arcane_Bolt", extra), "case does not matter");
        assertFalse(MagicDamage.matchesExtra("mymod", "other", extra));
        assertTrue(MagicDamage.matchesExtra("othermod", "anything_at_all", extra), "namespace:* matches the namespace");
        assertFalse(MagicDamage.matchesExtra("othermodx", "anything", extra), "and only that namespace");
        assertTrue(MagicDamage.matchesExtra("tensura", "death_tornado", extra));
        assertFalse(MagicDamage.matchesExtra("tensura", "death_wish", extra));
        assertFalse(MagicDamage.matchesExtra("mymod", "arcane_bolt", List.of()));
    }
}
