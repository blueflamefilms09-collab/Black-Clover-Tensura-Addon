package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.labyrinth.loot.KillLoot.Drop;
import com.newuniverse.nusmp.labyrinth.loot.KillLoot.Input;
import com.newuniverse.nusmp.labyrinth.loot.KillLoot.Rates;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The drop decision of {@link KillLoot} over the real roster: caps, duplicates, families, depth, luck, bosses. */
class KillLootTest {
    private static final LootRoster ROSTER = roster();
    private static final LootTables TABLES = LootTables.of(ROSTER);

    private static LootRoster roster() {
        return LootRoster.parse(new InputStreamReader(KillLootTest.class.getResourceAsStream(LootRoster.RESOURCE), StandardCharsets.UTF_8));
    }

    private static List<Drop> roll(Random rnd, String family, int depth, boolean boss, double luck, Rates rates) {
        return KillLoot.roll(rnd::nextDouble, new Input(family, depth, boss, luck), TABLES, rates);
    }

    @Test
    void neverMoreThanTheCapNeverTheSameItemTwiceAtMostOneGearAndOneFood() {
        Random rnd = new Random(1);
        for (String family : LootFamilies.all()) {
            for (int depth : new int[]{1, 12, 55, 150, 800}) {
                for (int i = 0; i < 400; i++) {
                    List<Drop> d = roll(rnd, family, depth, false, rnd.nextInt(4), new Rates(3.0, 3.0, 3.0, 3, 3.0));
                    assertTrue(d.size() <= 3, "capped: " + d.size());
                    Set<String> ids = new HashSet<>();
                    int gear = 0, food = 0;
                    for (Drop x : d) {
                        assertTrue(ids.add(x.id()), "no duplicates: " + d);
                        if (x.kind() == LootTables.Kind.GEAR) gear++;
                        if (x.kind() == LootTables.Kind.FOOD) food++;
                        assertTrue(x.count() >= 1 && x.count() <= 3);
                        assertTrue((x.kind() == LootTables.Kind.GEAR) == (x.rarity() != null), "only gear has a rarity");
                    }
                    assertTrue(gear <= 1 && food <= 1, "one gear, one food: " + d);
                }
            }
        }
    }

    @Test
    void aFamilyDropsItsOwnPartsAndTheGenericRowsOnly() {
        Random rnd = new Random(2);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 20000; i++) for (Drop d : roll(rnd, "goblin", 60, false, 0, new Rates(5.0, 5.0, 5.0, 3, 3.0))) seen.add(d.id());
        assertTrue(seen.contains("goblin_ear") && seen.contains("goblin_tooth"), "goblins drop goblin parts");
        for (String foreign : new String[]{"orc_tusk", "lich_dust", "drake_scale", "golem_core", "slime_gel", "imp_horn", "tide_pearl"}) assertFalse(seen.contains(foreign), "a goblin does not drop " + foreign);
        assertTrue(seen.contains("dungeon_ration_stew") || seen.contains("hearth_bread"), "the generic rows serve every family");
    }

    @Test
    void everyMaterialDropsFromItsFamilyAtSomeDepth() {
        for (LootRoster.MaterialDef m : ROSTER.materials()) {
            boolean found = false;
            for (LootTables.Entry e : TABLES.entries(m.family())) if (e.id().equals(m.id())) found = true;
            assertTrue(found, m.id() + " is in the table of " + m.family());
            for (LootTables.Entry e : TABLES.entries(m.family())) if (e.id().equals(m.id())) assertTrue(e.minBand() <= 4);
        }
    }

    @Test
    void rarePartsWaitForTheirBand() {
        Random rnd = new Random(3);
        Set<String> shallow = new HashSet<>();
        for (int i = 0; i < 40000; i++) for (Drop d : roll(rnd, "undead", 3, false, 0, new Rates(6.0, 6.0, 6.0, 3, 3.0))) shallow.add(d.id());
        assertFalse(shallow.contains("wraith_essence"), "wraith essence waits for band 2");
        assertFalse(shallow.contains("lich_dust"), "lich dust waits for band 1");
        assertTrue(shallow.contains("bone_shard"));
        Set<String> deep = new HashSet<>();
        for (int i = 0; i < 40000; i++) for (Drop d : roll(rnd, "undead", 120, false, 0, new Rates(6.0, 6.0, 6.0, 3, 3.0))) deep.add(d.id());
        assertTrue(deep.contains("wraith_essence") && deep.contains("lich_dust"));
    }

    @Test
    void deeperAndLuckierDropsMore() {
        Random rnd = new Random(4);
        int shallow = 0, deep = 0, plain = 0, lucky = 0;
        for (int i = 0; i < 30000; i++) {
            shallow += roll(rnd, "ogre", 2, false, 0, Rates.DEFAULT).size();
            deep += roll(rnd, "ogre", 400, false, 0, Rates.DEFAULT).size();
            plain += roll(rnd, "beast", 40, false, 0, Rates.DEFAULT).size();
            lucky += roll(rnd, "beast", 40, false, 8, Rates.DEFAULT).size();
        }
        assertTrue(deep > shallow * 1.1, "deeper floors are richer: " + shallow + " vs " + deep);
        assertTrue(lucky > plain * 1.1, "luck is richer: " + plain + " vs " + lucky);
    }

    @Test
    void theRateMultiplierScalesTheDropsAndZeroSwitchesThemOff() {
        Random rnd = new Random(5);
        int off = 0, base = 0, rich = 0;
        for (int i = 0; i < 20000; i++) {
            off += roll(rnd, "slime", 30, false, 0, new Rates(0.0, 1.0, 1.0, 3, 3.0)).size();
            base += roll(rnd, "slime", 30, false, 0, Rates.DEFAULT).size();
            rich += roll(rnd, "slime", 30, false, 0, new Rates(2.0, 1.0, 1.0, 3, 3.0)).size();
        }
        assertEquals(0, off);
        assertTrue(rich > base * 1.5);
        assertEquals(0, roll(rnd, "slime", 30, false, 0, new Rates(5.0, 5.0, 5.0, 0, 3.0)).size(), "a cap of 0 drops nothing");
    }

    @Test
    void aBossAlwaysLeavesRareMaterialsGearAndATonic() {
        Random rnd = new Random(6);
        for (String family : LootFamilies.all()) {
            for (int depth : new int[]{1, 40, 500}) {
                for (int i = 0; i < 100; i++) {
                    List<Drop> d = roll(rnd, family, depth, true, 0, Rates.DEFAULT);
                    int mats = 0, gear = 0, food = 0;
                    Set<String> ids = new HashSet<>();
                    for (Drop x : d) {
                        assertTrue(ids.add(x.id()), "no duplicates in a boss's loot");
                        switch (x.kind()) {
                            case MATERIAL -> {
                                mats++;
                                assertTrue(ROSTER.material(x.id()) != null);
                            }
                            case GEAR -> gear++;
                            case FOOD -> food++;
                        }
                    }
                    assertTrue(mats >= 2 && gear == 1 && food >= 1, family + " boss: " + d);
                    assertTrue(d.size() <= 6, "a boss's cap is the cap plus three");
                }
            }
        }
    }

    @Test
    void aBossDropsBetterGearThanACommonKill() {
        Random rnd = new Random(8);
        double boss = 0, kill = 0;
        int nb = 0, nk = 0;
        for (int i = 0; i < 20000; i++) {
            for (Drop d : roll(rnd, "ogre", 120, true, 0, Rates.DEFAULT)) if (d.rarity() != null) {
                boss += d.rarity().ordinal();
                nb++;
            }
            for (Drop d : roll(rnd, "ogre", 120, false, 0, new Rates(8.0, 8.0, 1.0, 3, 3.0))) if (d.rarity() != null) {
                kill += d.rarity().ordinal();
                nk++;
            }
        }
        assertTrue(nb > 0 && nk > 0);
        assertTrue(boss / nb > kill / nk, "a boss's gear is a tier better on average");
    }

    @Test
    void theSameDiceGiveTheSameDrops() {
        for (long s = 0; s < 50; s++) {
            assertEquals(roll(new Random(s), "demon", 77, false, 2, Rates.DEFAULT), roll(new Random(s), "demon", 77, false, 2, Rates.DEFAULT));
            assertEquals(roll(new Random(s), "demon", 77, true, 2, Rates.DEFAULT), roll(new Random(s), "demon", 77, true, 2, Rates.DEFAULT));
        }
    }

    @Test
    void familiesAreResolvedFromTagsThenWords() {
        assertEquals("goblin", LootFamilies.resolve("goblin", "ogre", "whatever"));
        assertEquals("ogre", LootFamilies.resolve(null, "ogre", "whatever"));
        assertEquals("undead", LootFamilies.resolve(null, null, "wither_skeleton"));
        assertEquals("ogre", LootFamilies.resolve("not_a_family", null, "hill_giant_troll"));
        assertEquals("insect", LootFamilies.fromPath("cave_ant"));
        assertEquals(LootFamilies.ANY, LootFamilies.fromPath("pimp"), "pimp is no imp");
        assertEquals("ogre", LootFamilies.fromPath("giant"));
        assertEquals("goblin", LootFamilies.fromPath("tensura/hobgoblin"));
        assertEquals(LootFamilies.ANY, LootFamilies.fromPath("sheep"));
        assertEquals(LootFamilies.ANY, LootFamilies.fromPath(null));
        assertEquals(0, LootBand.of(0));
        assertEquals(0, LootBand.of(9));
        assertEquals(1, LootBand.of(-10));
        assertEquals(2, LootBand.of(99));
        assertEquals(3, LootBand.of(299));
        assertEquals(4, LootBand.of(2000));
    }

    @Test
    void salesUseTheRosterValues() {
        assertEquals(6L, LootValue.unit(ROSTER, "goblin_ear", null));
        assertEquals(6L * 12, LootValue.stack(ROSTER, "goblin_ear", null, 12));
        assertEquals(0L, LootValue.unit(ROSTER, "not_an_item", null));
        assertEquals(0L, LootValue.stack(ROSTER, "goblin_ear", null, 0));
        GearRoll epic = GearRollMath.roll(1L, 100, LootRarity.EPIC, GearKind.WEAPON, StatKind.ATTACK_DAMAGE, 10);
        assertTrue(LootValue.unit(ROSTER, "orc_cleaver", epic) > LootValue.unit(ROSTER, "orc_cleaver", null));
        assertEquals(LootValue.unit(ROSTER, "orc_cleaver", null), GearRollMath.sellValue(ROSTER.value("orc_cleaver"), LootRarity.COMMON, 0));
    }
}
