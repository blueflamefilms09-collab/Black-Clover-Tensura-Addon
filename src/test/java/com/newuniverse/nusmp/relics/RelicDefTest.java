package com.newuniverse.nusmp.relics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The def builder: slugs, immutability, defaults. */
class RelicDefTest {
    @Test
    void slugFollowsTheCardNameRule() {
        assertEquals("prides_crown", RelicDef.slug("Pride's Crown"));
        assertEquals("cloak_mimic_tame", RelicDef.slug("Cloak Mimic, tame"));
        assertEquals("alchemists_belt", RelicDef.slug("Alchemist's Belt"));
        assertEquals("astralis_the_eye_of_the_universe", RelicDef.slug("Astralis, the Eye of the Universe"));
        assertEquals("amulet_of_the_juggernauts_blindness", RelicDef.slug("Amulet of the Juggernaut’s Blindness"));
        assertEquals("a_b", RelicDef.slug("  A -- B !! "));
        assertEquals("", RelicDef.slug("'!?"));
    }

    @Test
    void everyCardIdInTheIdsFileIsItsOwnSlug() {
        assertEquals("quicklings_ivy", RelicDef.slug("Quickling's Ivy"));
        assertEquals("bolt_thrower", RelicDef.slug("Bolt Thrower"));
    }

    @Test
    void aDefIsImmutable() {
        RelicDef base = RelicDef.of("test_blade", "Test Blade", Grade.RARE, RelicSlot.MAIN_HAND);
        RelicDef more = base.attune().model3d().desc("x").engrave("severance", "holy_weapon").melee(7, 1.6).passive(Passive.flag("a"));
        assertFalse(base.needsAttunement());
        assertFalse(base.hasModel3d());
        assertTrue(base.engravings().isEmpty());
        assertTrue(base.passives().isEmpty());
        assertTrue(more.needsAttunement());
        assertTrue(more.hasModel3d());
        assertEquals(2, more.engravings().size());
        assertEquals(1, more.passives().size());
        assertEquals("x", more.desc());
        assertEquals("test_blade", more.id());
        assertThrows(UnsupportedOperationException.class, () -> more.engravings().add("x"));
    }

    @Test
    void meleeDefaultsFollowTheGradeAndBothSpeedFormsWork() {
        RelicDef d = RelicDef.of("w", "W", Grade.UNIQUE, RelicSlot.MAIN_HAND);
        assertEquals(7.0, d.meleeDamage(), 1e-9);
        assertEquals(1.6, d.meleeSpeed(), 1e-9);
        assertEquals(-2.4, d.meleeSpeedModifier(), 1e-9);
        RelicDef a = d.melee(9, 1.2);
        assertEquals(9.0, a.meleeDamage(), 1e-9);
        assertEquals(1.2, a.meleeSpeed(), 1e-9);
        assertEquals(-2.8, a.meleeSpeedModifier(), 1e-9);
        RelicDef b = d.melee(9, -2.4);
        assertEquals(1.6, b.meleeSpeed(), 1e-9);
        assertEquals(-2.4, b.meleeSpeedModifier(), 1e-9);
        assertTrue(d.melee(5, 9).meleeSpeedModifier() <= 0);
    }

    @Test
    void armourDefaultsAreSane() {
        RelicDef chest = RelicDef.of("c", "C", Grade.LEGEND, RelicSlot.CHEST);
        RelicDef head = RelicDef.of("h", "H", Grade.LEGEND, RelicSlot.HEAD);
        assertTrue(chest.armorDefence() >= 5 && chest.armorDefence() <= 8);
        assertTrue(head.armorDefence() >= 1 && head.armorDefence() < chest.armorDefence());
        assertEquals(10, chest.armor(10, 4).armorDefence());
        assertEquals(4.0, chest.armor(10, 4).armorToughness(), 1e-9);
        assertTrue(chest.armorToughness() > 0);
    }

    @Test
    void bootsAreArmourWithTheSmallerShareAndTheirOwnLook() {
        assertTrue(RelicSlot.FEET.isArmor());
        assertFalse(RelicSlot.FEET.isCurio());
        assertFalse(RelicSlot.FEET.isHand());
        assertNull(RelicSlot.FEET.curiosSlot());
        assertEquals(RelicSlot.FEET, RelicSlot.byName("feet"));
        assertEquals(RelicSlot.CONSUMABLE.ordinal() + 1, RelicSlot.FEET.ordinal(), "FEET is appended at the end");
        assertEquals(Look.BOOTS, Look.defaultFor(RelicSlot.FEET));
        assertEquals(Look.BOOTS, RelicDef.of("b", "B", Grade.RARE, RelicSlot.FEET).look());
        assertEquals(Look.SANDALS, RelicDef.of("s", "S", Grade.RARE, RelicSlot.FEET).look(Look.SANDALS).look());
        RelicDef chest = RelicDef.of("c", "C", Grade.LEGEND, RelicSlot.CHEST);
        RelicDef feet = RelicDef.of("f", "F", Grade.LEGEND, RelicSlot.FEET);
        assertTrue(feet.armorDefence() >= 1 && feet.armorDefence() < chest.armorDefence());
        assertEquals(RelicDef.of("h", "H", Grade.LEGEND, RelicSlot.HEAD).armorDefence(), feet.armorDefence());
        assertEquals(5, feet.armor(5, 1).armorDefence());
    }

    @Test
    void theLooksAreOnlyAppended() {
        Look[] l = Look.values();
        assertEquals(Look.ORB, l[l.length - 4]);
        assertEquals(Look.BOOTS, l[l.length - 3]);
        assertEquals(Look.SANDALS, l[l.length - 2]);
        assertEquals(Look.GREAVES, l[l.length - 1]);
        assertEquals(Look.SWORD, l[0]);
    }

    @Test
    void anIdMustBeSnakeCase() {
        assertThrows(IllegalArgumentException.class, () -> RelicDef.of("Bad Id", "x", Grade.RARE, RelicSlot.NECK));
        assertThrows(IllegalArgumentException.class, () -> RelicDef.of("ok", "x", null, RelicSlot.NECK));
    }

    @Test
    void actsKeepTheirFluentSettings() {
        Act a = Act.of("k", "K", ctx -> { }).cooldown(200).mana(5).epPercent(2.5).usesPerDay(2).needsSneak().hold(40).toggle();
        assertEquals(200, a.cooldownTicks());
        assertEquals(5, a.manaPercent());
        assertEquals(2.5, a.epPercentCost(), 1e-9);
        assertEquals(2, a.usesPerDay());
        assertTrue(a.sneakRequired());
        assertEquals(40, a.holdTicks());
        assertTrue(a.toggles());
        Act plain = Act.of("p", null, null);
        assertEquals("p", plain.displayName());
        assertEquals(0, plain.cooldownTicks());
        assertFalse(plain.sneakRequired());
        RelicDef d = RelicDef.of("x", "X", Grade.RARE, RelicSlot.CHARM).act(a).act(plain);
        assertSame(a, d.firstAct());
        assertSame(plain, d.actByKey("p"));
        assertNull(d.actByKey("zzz"));
    }

    @Test
    void passiveResistIsClamped() {
        assertEquals(0.9, Passive.resist("minecraft:is_fire", 5).amount(), 1e-9);
        assertEquals("minecraft:is_fire", Passive.resist("#minecraft:is_fire", 0.3).key());
        assertEquals(Passive.Kind.FLAG, Passive.flag("x").kind());
        assertEquals(0, Passive.effect("minecraft:speed", -4).amp());
        Passive sk = Passive.skill("tensura:magic_sense");
        assertEquals(Passive.Kind.SKILL, sk.kind());
        assertEquals("tensura:magic_sense", sk.key());
        assertEquals(5, Passive.Kind.values().length, "kinds are only appended");
        assertEquals(Passive.Kind.SKILL, Passive.Kind.values()[4]);
    }
}
