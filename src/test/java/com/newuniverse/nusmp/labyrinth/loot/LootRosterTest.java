package com.newuniverse.nusmp.labyrinth.loot;

import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the roster is the one list every other part of the loot is built from, so its promises are checked here once: the owner's counts (about thirty foods, about twenty-four
 * gear pieces with at least six weapons and twelve armour pieces and trinkets, about twenty-four monster parts), unique ids, the hunger ban (food only restores: no hunger effect,
 * no negative nutrition), sane drop rows, an engraving on every weapon and a use for every material.
 */
class LootRosterTest {
    private static final LootRoster ROSTER = LootRoster.parse(new InputStreamReader(LootRosterTest.class.getResourceAsStream(LootRoster.RESOURCE), StandardCharsets.UTF_8));

    /** Effects that drain or punish; none may appear on anything that is eaten or drunk. */
    private static final Set<String> BANNED = Set.of("minecraft:hunger", "minecraft:poison", "minecraft:wither", "minecraft:weakness", "minecraft:slowness", "minecraft:mining_fatigue",
            "minecraft:nausea", "minecraft:blindness", "minecraft:instant_damage", "minecraft:levitation", "minecraft:unluck", "minecraft:bad_omen", "minecraft:darkness");

    @Test
    void theRosterIsRead() {
        assertEquals("", ROSTER.problem(), ROSTER.problem());
        assertFalse(ROSTER.foods().isEmpty());
    }

    @Test
    void theOwnersCountsAreMet() {
        assertTrue(ROSTER.foods().size() >= 30, "foods and elixirs: " + ROSTER.foods().size());
        assertTrue(ROSTER.materials().size() >= 24, "monster parts: " + ROSTER.materials().size());
        int gear = ROSTER.weapons().size() + ROSTER.armour().size() + ROSTER.trinkets().size();
        assertTrue(gear >= 24, "gear pieces: " + gear);
        assertTrue(ROSTER.weapons().size() >= 6, "weapons: " + ROSTER.weapons().size());
        assertTrue(ROSTER.armour().size() + ROSTER.trinkets().size() >= 12, "armour and trinkets: " + (ROSTER.armour().size() + ROSTER.trinkets().size()));
        long elixirs = ROSTER.foods().stream().filter(LootRoster.FoodDef::elixir).count();
        assertTrue(elixirs >= 3, "mana and aura restorers: " + elixirs);
    }

    @Test
    void everyIdIsUniqueAndLowerSnakeCase() {
        Set<String> seen = new HashSet<>();
        for (String id : ROSTER.ids()) {
            assertTrue(id.matches("[a-z][a-z0-9_]*"), "id shape: " + id);
            assertTrue(seen.add(id), "duplicate id: " + id);
        }
        assertEquals(seen.size(), ROSTER.ids().size());
    }

    @Test
    void foodOnlyRestoresAndNothingDrainsHunger() {
        for (LootRoster.FoodDef f : ROSTER.foods()) {
            assertTrue(f.nutrition() >= 0 && f.saturation() >= 0.0f, f.id() + " has a negative nutrition or saturation");
            assertTrue(f.eat() > 0.0, f.id() + " eat time");
            assertTrue(f.mana() >= 0 && f.aura() >= 0 && f.mana() <= 100 && f.aura() <= 100, f.id() + " mana or aura percent");
            for (LootRoster.EffectDef e : f.effects()) {
                assertFalse(BANNED.contains(e.effect()), f.id() + " carries the banned effect " + e.effect());
                assertTrue(e.seconds() > 0 && e.amp() >= 0, f.id() + " effect numbers");
            }
        }
    }

    @Test
    void everyDropRowIsSane() {
        Set<String> families = new HashSet<>(LootFamilies.all());
        families.add("any");
        List<String> bad = new ArrayList<>();
        for (String id : ROSTER.ids()) {
            for (LootRoster.DropRow r : rows(id)) {
                if (!families.contains(r.family())) bad.add(id + " family " + r.family());
                if (!(r.chance() > 0.0 && r.chance() <= 1.0)) bad.add(id + " chance " + r.chance());
                if (r.minBand() < 0 || r.minBand() > 4) bad.add(id + " band " + r.minBand());
            }
        }
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void everyDroppableThingHasADropRowOrIsCraftedOrIsBossLoot() {
        List<String> orphans = new ArrayList<>();
        for (String id : ROSTER.ids()) {
            boolean crafted = ROSTER.crafted().stream().anyMatch(c -> c.id().equals(id)) || ROSTER.ammo().stream().anyMatch(c -> c.id().equals(id));
            boolean boss = ROSTER.bossMaterials().contains(id) || ROSTER.bossFoods().contains(id);
            if (!crafted && !boss && rows(id).isEmpty()) orphans.add(id);
        }
        assertTrue(orphans.isEmpty(), "nothing drops these: " + orphans);
    }

    @Test
    void everyWeaponIsEngravedWithRealLevels() {
        for (LootRoster.WeaponDef w : ROSTER.weapons()) {
            assertFalse(w.engravings().isEmpty(), w.id() + " has no engravings");
            for (LootRoster.Engraving e : w.engravings()) assertTrue(e.level() >= 1 && e.level() <= 3 && !e.id().isBlank(), w.id() + " " + e);
        }
    }

    @Test
    void everyArmourPieceBelongsToASetAndEverySetHasItsPieces() {
        for (LootRoster.ArmourDef a : ROSTER.armour()) assertTrue(ROSTER.armourSets().containsKey(a.set()), a.id() + " set " + a.set());
        for (String set : ROSTER.armourSets().keySet()) {
            Set<String> pieces = new HashSet<>();
            for (LootRoster.ArmourDef a : ROSTER.armour()) if (a.set().equals(set)) pieces.add(a.piece());
            assertEquals(Set.of("head", "chest", "legs", "feet"), pieces, set + " pieces");
        }
    }

    @Test
    void everyMaterialHasAUse() {
        // a use is a crafting recipe that takes it (checked against the recipe files in LootAssetsTest) or an anvil use written in the roster; this checks the roster half
        for (String id : ROSTER.codeUses().keySet()) assertTrue(ROSTER.material(id) != null, "a code use for something that is not a material: " + id);
        assertTrue(ROSTER.codeUses().size() >= 3, "at least three anvil or code uses: " + ROSTER.codeUses().keySet());
    }

    @Test
    void theFloorBossTableNamesRealItems() {
        assertFalse(ROSTER.bossMaterials().isEmpty());
        assertFalse(ROSTER.bossFoods().isEmpty());
        for (String id : ROSTER.bossMaterials()) assertTrue(ROSTER.material(id) != null, "boss material " + id);
        for (String id : ROSTER.bossFoods()) assertTrue(ROSTER.food(id) != null, "boss food " + id);
    }

    private static List<LootRoster.DropRow> rows(String id) {
        List<LootRoster.DropRow> out = new ArrayList<>();
        for (LootRoster.FoodDef f : ROSTER.foods()) if (f.id().equals(id)) out.addAll(f.drops());
        for (LootRoster.MaterialDef m : ROSTER.materials()) if (m.id().equals(id)) out.addAll(m.drops());
        for (LootRoster.WeaponDef w : ROSTER.weapons()) if (w.id().equals(id)) out.addAll(w.drops());
        for (LootRoster.ArmourDef a : ROSTER.armour()) if (a.id().equals(id)) out.addAll(a.drops());
        for (LootRoster.TrinketDef t : ROSTER.trinkets()) if (t.id().equals(id)) out.addAll(t.drops());
        return out;
    }
}
