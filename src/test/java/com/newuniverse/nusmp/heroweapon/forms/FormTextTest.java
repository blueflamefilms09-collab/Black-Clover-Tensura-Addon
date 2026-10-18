package com.newuniverse.nusmp.heroweapon.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Why: a number printed two ways in two places looks like a bug; the wording helpers are pinned so the book, the chat and the commands agree.
 */
class FormTextTest {
    @Test
    void idsBecomeWords() {
        assertEquals("Leaf Shield", FormText.pretty("leaf_shield"));
        assertEquals("Oak Leaves", FormText.pretty("minecraft:oak_leaves"));
        assertEquals("Iron", FormText.pretty("c:ingots/iron"));
        assertEquals("Chimera Viper Shield", FormText.pretty("nusmp:chimera_viper_shield"));
        assertEquals("", FormText.pretty(""));
        assertEquals("", FormText.pretty(null));
        assertEquals("Any Leaves", FormText.tagLabel("minecraft:leaves"));
    }

    @Test
    void statsPrintWithSignAndUnit() {
        assertEquals("+2", FormText.statValue("armor", 2.0));
        assertEquals("-0.4", FormText.statValue("attack_speed", -0.4));
        assertEquals("+4%", FormText.statValue("movement_speed", 0.04));
        assertEquals("+20%", FormText.statValue("knockback_resistance", 0.2));
        assertEquals("+1.5", FormText.statValue("reach", 1.5));
        assertEquals("+0.25", FormText.statValue("armor", 0.25));
        assertEquals("Armor +2", FormText.statLine("armor", 2.0));
        assertEquals("Movement speed +4%", FormText.statLine("movement_speed", 0.04));
        assertEquals("Armor toughness +1", FormText.statLine("armor_toughness", 1));
        assertEquals("Luck +0.33", FormText.statLine("luck", 0.3333));
    }

    @Test
    void numbersTimesAndSlots() {
        assertEquals("2", FormText.number(2.0));
        assertEquals("0.5", FormText.number(0.5));
        assertEquals("0.33", FormText.number(1.0 / 3));
        assertEquals("8 s", FormText.seconds(160));
        assertEquals("1.5 s", FormText.seconds(30));
        assertEquals("2%", FormText.percent(0.02));
        assertEquals("Sneak + Use", FormText.slotLabel("sneak"));
        assertEquals("Key 3", FormText.slotLabel("key3"));
        assertEquals("Use", FormText.slotLabel("use"));
    }
}
