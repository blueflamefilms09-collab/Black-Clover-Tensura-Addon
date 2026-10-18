package com.newuniverse.nusmp.elite.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.elite.items.armor.NightingaleRules;
import com.newuniverse.nusmp.elite.items.consumable.CatalystRules;
import com.newuniverse.nusmp.elite.items.crate.CrateTier;
import com.newuniverse.nusmp.elite.items.plush.PlushKinds;
import com.newuniverse.nusmp.elite.items.weapon.AbilityParser;
import com.newuniverse.nusmp.elite.items.weapon.RuneRules;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the small pure rule classes of the subsystem: the rune slots of the Runic Swords (slots by tier, one rune once, the full sword refuses, a rune whose enchantment is gone frees its slot), the
 * Potential Catalyst's limit and health arithmetic, the Nightingale set bonus thresholds, the id tables (no duplicates, crates and keys named by tier), the plushie pitch by size, and the technique
 * parser's defences (an unknown kind is refused, numbers out of range are clamped and reported, bad effects dropped, a file without a usable primary refused).
 */
class ElitePureRulesTest {
    @Test
    void runeSlotsFollowTheTier() {
        assertEquals(1, RuneRules.slots("runic_sword_renewed"));
        assertEquals(2, RuneRules.slots("runic_sword_ancient"));
        assertEquals(3, RuneRules.slots("runic_sword_mythical"));
        assertEquals(0, RuneRules.slots("astral_edge"));
        assertEquals(0, RuneRules.slots(null));
        assertTrue(RuneRules.damagePerRune("runic_sword_renewed") < RuneRules.damagePerRune("runic_sword_mythical"));
        assertEquals(4.0, RuneRules.bonusDamage("runic_sword_ancient", 2) + 1.0, 1e-9);
        assertEquals(0.0, RuneRules.bonusDamage("runic_sword_ancient", -3), 1e-9);
    }

    @Test
    void slottingRefusesDuplicatesFullSwordsAndBadIds() {
        List<String> none = List.of();
        assertEquals(RuneRules.Slotting.OK, RuneRules.check(none, 1, "tensura:severance"));
        assertEquals(RuneRules.Slotting.DUPLICATE, RuneRules.check(List.of("tensura:severance"), 2, "tensura:severance"));
        assertEquals(RuneRules.Slotting.FULL, RuneRules.check(List.of("tensura:swift"), 1, "tensura:severance"));
        assertEquals(RuneRules.Slotting.INVALID, RuneRules.check(none, 3, "Severance"));
        assertEquals(RuneRules.Slotting.INVALID, RuneRules.check(none, 3, null));
        assertEquals(List.of("tensura:swift", "tensura:severance"), RuneRules.add(List.of("tensura:swift"), 2, "tensura:severance"));
        assertEquals(List.of("tensura:swift"), RuneRules.add(List.of("tensura:swift"), 1, "tensura:severance"));
    }

    @Test
    void aRuneWhoseEnchantmentIsGoneFreesItsSlot() {
        List<String> stored = List.of("tensura:a", "tensura:b", "tensura:c", "tensura:a", "bad id");
        assertEquals(List.of("tensura:a", "tensura:c"), RuneRules.reconcile(stored, 3, r -> !r.equals("tensura:b")));
        assertEquals(List.of("tensura:a"), RuneRules.reconcile(stored, 1, r -> true));
        assertEquals(List.of(), RuneRules.reconcile(List.of(), 3, r -> true));
    }

    @Test
    void catalystLimitAndHealth() {
        assertTrue(CatalystRules.canUse(0, 10));
        assertTrue(CatalystRules.canUse(9, 10));
        assertFalse(CatalystRules.canUse(10, 10));
        assertFalse(CatalystRules.canUse(-1, 10));
        assertFalse(CatalystRules.canUse(0, 0));
        assertEquals(6.0, CatalystRules.healthBonus(3, 2), 1e-9);
        assertEquals(0.0, CatalystRules.healthBonus(-5, 2), 1e-9);
        assertEquals(0.0, CatalystRules.healthBonus(5, -2), 1e-9);
        assertEquals(7, CatalystRules.left(3, 10));
        assertEquals(0, CatalystRules.left(30, 10));
    }

    @Test
    void nightingaleThresholds() {
        assertFalse(NightingaleRules.bonusActive(3));
        assertTrue(NightingaleRules.bonusActive(4));
        assertTrue(NightingaleRules.needsNightVision(0));
        assertTrue(NightingaleRules.needsNightVision(7));
        assertFalse(NightingaleRules.needsNightVision(8));
        assertFalse(NightingaleRules.needsNightVision(-1));
        assertTrue(NightingaleRules.NIGHT_VISION_TICKS > 200, "longer than the 10 s (200 ticks) at which vanilla starts to flash Night Vision");
        assertTrue(NightingaleRules.lullabySecond(0) && !NightingaleRules.lullabySecond(1) && NightingaleRules.lullabySecond(2));
    }

    @Test
    void idsAreUniqueAndNamedByTier() {
        Set<String> all = EliteIds.allItems();
        int expected = EliteIds.WEAPONS.size() + EliteIds.ARMOR.size() + EliteIds.CONSUMABLES.size() + EliteIds.crates().size() + EliteIds.keys().size() + EliteIds.plushies().size();
        assertEquals(expected, all.size(), "no id may be used twice");
        assertEquals(6, EliteIds.WEAPONS.size());
        assertEquals(4, EliteIds.ARMOR.size());
        assertEquals(List.of("common_crate", "rare_crate", "elite_crate"), EliteIds.crates());
        assertEquals(List.of("common_crate_key", "rare_crate_key", "elite_crate_key"), EliteIds.keys());
        assertNotNull(CrateTier.byId("RARE"));
        assertNull(CrateTier.byId("mythic"));
        for (String id : all) assertTrue(id.matches("[a-z][a-z0-9_]*"), id);
    }

    @Test
    void squeakGetsLowerAsPlushiesGetBigger() {
        assertTrue(PlushKinds.byId("bat").pitch() > PlushKinds.byId("cow").pitch());
        assertTrue(PlushKinds.byId("cow").pitch() > PlushKinds.byId("warden").pitch());
        assertEquals(0, PlushKinds.indexOf("allay"));
        assertEquals(-1, PlushKinds.indexOf("nothing"));
        assertNull(PlushKinds.byId(null));
    }

    @Test
    void abilityParserDefendsItself() {
        assertNull(AbilityParser.parse("not json").abilities());
        assertNull(AbilityParser.parse("{\"item\":\"nusmp:x\"}").abilities());
        AbilityParser.Result badKind = AbilityParser.parse("{\"item\":\"nusmp:x\",\"primary\":{\"kind\":\"summon_dragon\",\"name\":\"n\"}}");
        assertNull(badKind.abilities());
        assertTrue(badKind.errors().stream().anyMatch(e -> e.contains("unknown kind")));
        String json = "{\"item\":\"nusmp:x\",\"primary\":{\"kind\":\"nova\",\"name\":\"N\",\"cooldown\":99999999,\"damage\":-5,\"radius\":4,\"effects\":[{\"id\":\"BAD ID\"},{\"id\":\"minecraft:slowness\",\"ticks\":40}]}}";
        AbilityParser.Result r = AbilityParser.parse(json);
        assertNotNull(r.abilities());
        assertEquals(72000, r.abilities().primary().cooldown());
        assertEquals(0f, r.abilities().primary().damage(), 0f);
        assertEquals(1, r.abilities().primary().foeEffects().size());
        assertEquals(3, r.errors().size(), String.valueOf(r.errors()));
        assertTrue(r.abilities().alt() == r.abilities().primary(), "without an alt the primary stands in");
    }
}
