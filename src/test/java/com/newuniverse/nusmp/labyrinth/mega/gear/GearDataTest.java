package com.newuniverse.nusmp.labyrinth.mega.gear;

import com.newuniverse.nusmp.labyrinth.mega.gear.GearTables.GradeTable;
import com.newuniverse.nusmp.labyrinth.mega.gear.GearTables.Pool;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the shipped table is DATA, and a wrong letter in data fails silently in the game (an item id with a typo is a drop that never happens, an
 * enchantment id with a typo is a missing enchantment, and nobody sees either). This test reads the real {@code tables.json} the way the game does and
 * checks it against the facts it cannot check itself: every base item is a real vanilla item (the list is the constants of the API dump's {@code Items}
 * class), every enchantment is a real vanilla enchantment and every engraving a real Tensura one (the list is the owner's id reference); the pieces
 * are what the brief says (Normal plain, Special iron and chainmail with 10 to 20 percent and no resistance, Rare iron and diamond with 20 to 35 percent and a
 * resistance of 5 to 12 on armour and 4 to 6 on a weapon or shield); a base item is of the kind of its slot (a sword slot holds swords); no pool offers two
 * enchantments that exclude each other in vanilla (Protection with Fire Protection) so no piece is illegal; engravings exist only for the held slots (the
 * owner's rule 6 is about weapons); the curves have the shapes the design promises; and nothing in the file is a relic id, because relics live under the
 * Order's ledger and standard gear must never touch it.
 */
class GearDataTest {

    private static GearTables t() { return GearFx.tables(); }

    /** The words that name the material of a vanilla tool or armour id. */
    private static String material(String id) {
        String path = id.substring(id.indexOf(':') + 1);
        int us = path.indexOf('_');
        return us < 0 ? "" : path.substring(0, us);
    }

    @Test
    void everyBaseItemIsARealVanillaItem() {
        Set<String> real = GearFx.set("vanilla_items.txt");
        assertTrue(real.size() > 1000, "the id list looks truncated: " + real.size());
        for (GearGrade g : GearGrade.values()) {
            GradeTable gt = t().grade(g);
            for (GearSlot s : GearSlot.values()) {
                for (String id : gt.items(s)) {
                    assertTrue(id.startsWith("minecraft:"), g + " " + s + ": " + id + " is not a vanilla id (standard gear is ordinary vanilla items, never a relic)");
                    assertTrue(real.contains(id), g + " " + s + ": " + id + " is not in the list of vanilla items");
                }
            }
        }
    }

    @Test
    void everyGradeOffersEverySlotAndTheBaseItemIsOfTheKindOfItsSlot() {
        for (GearGrade g : GearGrade.values()) {
            GradeTable gt = t().grade(g);
            for (GearSlot s : GearSlot.values()) {
                assertTrue(gt.offers(s), g + " has no item for " + s);
                for (String id : gt.items(s)) {
                    String path = id.substring("minecraft:".length());
                    boolean fits = switch (s) {
                        case SWORD -> path.endsWith("_sword");
                        case AXE -> path.endsWith("_axe");
                        case BOW -> path.equals("bow");
                        case CROSSBOW -> path.equals("crossbow");
                        case SHIELD -> path.equals("shield");
                        case HELMET -> path.endsWith("_helmet");
                        case CHESTPLATE -> path.endsWith("_chestplate");
                        case LEGGINGS -> path.endsWith("_leggings");
                        case BOOTS -> path.endsWith("_boots");
                    };
                    assertTrue(fits, id + " does not belong in the " + s + " slot");
                }
            }
        }
    }

    @Test
    void theMaterialsGrowWithTheGrade() {
        Map<GearGrade, Set<String>> allowed = Map.of(
                GearGrade.NORMAL, Set.of("wooden", "stone", "leather", ""),
                GearGrade.SPECIAL, Set.of("iron", "chainmail", ""),
                GearGrade.RARE, Set.of("iron", "diamond", ""));
        for (GearGrade g : GearGrade.values()) {
            for (GearSlot s : GearSlot.values()) {
                for (String id : t().grade(g).items(s)) {
                    String m = s == GearSlot.BOW || s == GearSlot.CROSSBOW || s == GearSlot.SHIELD ? "" : material(id);
                    assertTrue(allowed.get(g).contains(m), g + ": " + id + " has material '" + m + "', which the brief does not give that grade");
                }
            }
        }
        // the brief names the Rare materials iron AND diamond, the Special ones iron AND chainmail: both must actually occur
        Set<String> rare = new java.util.HashSet<>(), special = new java.util.HashSet<>();
        for (GearSlot s : GearSlot.values()) {
            if (s.hasPowerStat()) {
                for (String id : t().grade(GearGrade.RARE).items(s)) rare.add(material(id));
                for (String id : t().grade(GearGrade.SPECIAL).items(s)) special.add(material(id));
            }
        }
        assertTrue(rare.contains("iron") && rare.contains("diamond"), "Rare: " + rare);
        assertTrue(special.contains("iron") && special.contains("chainmail"), "Special: " + special);
    }

    @Test
    void theEnchantmentsAreRealAndLegalTogether() {
        Set<String> vanilla = GearFx.set("vanilla_enchantments.txt");
        Map<String, Integer> maxLevel = Map.ofEntries(
                Map.entry("minecraft:sharpness", 5), Map.entry("minecraft:unbreaking", 3), Map.entry("minecraft:knockback", 2), Map.entry("minecraft:sweeping_edge", 3),
                Map.entry("minecraft:efficiency", 5), Map.entry("minecraft:power", 5), Map.entry("minecraft:punch", 2), Map.entry("minecraft:quick_charge", 3),
                Map.entry("minecraft:piercing", 4), Map.entry("minecraft:protection", 4), Map.entry("minecraft:respiration", 3), Map.entry("minecraft:aqua_affinity", 1),
                Map.entry("minecraft:thorns", 3), Map.entry("minecraft:swift_sneak", 3), Map.entry("minecraft:feather_falling", 4));
        Set<String> protections = Set.of("minecraft:protection", "minecraft:fire_protection", "minecraft:blast_protection", "minecraft:projectile_protection");
        for (GearGrade g : new GearGrade[] {GearGrade.SPECIAL, GearGrade.RARE}) {
            for (GearSlot s : GearSlot.values()) {
                List<Pool> pool = t().grade(g).enchantPool(s);
                assertFalse(pool.isEmpty(), g + " " + s + " has no enchantment pool");
                int protectionFamily = 0;
                Set<String> seen = new java.util.HashSet<>();
                for (Pool p : pool) {
                    assertTrue(p.id().startsWith("minecraft:") && vanilla.contains(p.id()), g + " " + s + ": " + p.id() + " is not a vanilla enchantment");
                    assertTrue(seen.add(p.id()), g + " " + s + ": " + p.id() + " twice in one pool");
                    Integer max = maxLevel.get(p.id());
                    assertNotNull(max, p.id() + " is missing from this test's table of maximum levels");
                    assertTrue(p.max() <= max, g + " " + s + ": " + p.id() + " goes to level " + p.max() + ", above its maximum " + max);
                    if (protections.contains(p.id())) protectionFamily++;
                }
                assertTrue(protectionFamily <= 1, g + " " + s + ": two enchantments that exclude each other in vanilla are in one pool");
            }
        }
        for (GearSlot s : GearSlot.values()) assertTrue(t().grade(GearGrade.NORMAL).enchantPool(s).isEmpty(), "Normal has no enchantments");
    }

    @Test
    void theEnchantmentsFitTheirSlot() {
        Map<GearSlot, Set<String>> fits = Map.of(
                GearSlot.SWORD, Set.of("sharpness", "unbreaking", "knockback", "sweeping_edge"),
                GearSlot.AXE, Set.of("sharpness", "efficiency", "unbreaking"),
                GearSlot.BOW, Set.of("power", "punch", "unbreaking"),
                GearSlot.CROSSBOW, Set.of("quick_charge", "piercing", "unbreaking"),
                GearSlot.SHIELD, Set.of("unbreaking"),
                GearSlot.HELMET, Set.of("protection", "respiration", "aqua_affinity", "unbreaking"),
                GearSlot.CHESTPLATE, Set.of("protection", "thorns", "unbreaking"),
                GearSlot.LEGGINGS, Set.of("protection", "swift_sneak", "unbreaking"),
                GearSlot.BOOTS, Set.of("protection", "feather_falling", "unbreaking"));
        for (GearGrade g : new GearGrade[] {GearGrade.SPECIAL, GearGrade.RARE})
            for (GearSlot s : GearSlot.values())
                for (Pool p : t().grade(g).enchantPool(s))
                    assertTrue(fits.get(s).contains(p.id().substring("minecraft:".length())), g + " " + s + ": " + p.id() + " does not suit this slot");
    }

    @Test
    void theEngravingsAreRealTensuraEngravingsOnWeaponsAndShieldsOnly() {
        Set<String> tensura = GearFx.set("tensura_enchantments.txt");
        assertEquals(34, tensura.size(), "the owner's id reference lists 34 enchantments");
        int rareSeen = 0;
        for (GearGrade g : GearGrade.values()) {
            GradeTable gt = t().grade(g);
            for (GearSlot s : GearSlot.values()) {
                List<Pool> pool = gt.engravePool(s);
                if (s.isArmour() || g == GearGrade.NORMAL) {
                    assertTrue(pool.isEmpty(), g + " " + s + " must carry no engravings");
                    continue;
                }
                assertFalse(pool.isEmpty(), g + " " + s + " is a weapon or shield and needs engravings to pick from");
                for (Pool p : pool) {
                    assertTrue(tensura.contains(p.id()), g + " " + s + ": " + p.id() + " is not a Tensura enchantment");
                    assertFalse(Set.of("tensura:enervation", "tensura:lethargy", "tensura:ruination", "tensura:sealing", "tensura:stagnation").contains(p.id()),
                            p.id() + " is a curse: a drop must never be cursed");
                    if (g == GearGrade.RARE) rareSeen++;
                }
            }
        }
        assertTrue(rareSeen > 0);
        // Rare weapons are engraved against magic: every Rare weapon and shield pool offers Magic Interference, the brief's own example
        for (GearSlot s : GearSlot.values()) {
            if (!s.engraves()) continue;
            boolean interference = false;
            for (Pool p : t().grade(GearGrade.RARE).engravePool(s)) interference |= p.id().equals("tensura:magic_interference");
            assertTrue(interference, "Rare " + s + " can roll Magic Interference");
        }
    }

    @Test
    void theGradesAreWhatTheBriefSays() {
        GradeTable n = t().grade(GearGrade.NORMAL), s = t().grade(GearGrade.SPECIAL), r = t().grade(GearGrade.RARE);
        assertTrue(n.power().isZero() && n.durability().isZero() && n.enchantCount().isZero() && n.engraveCount().isZero() && n.resistArmour().isZero() && n.resistHand().isZero(),
                "Normal is plain");
        assertEquals(10, s.power().min());
        assertEquals(20, s.power().max());
        assertEquals(20, r.power().min());
        assertEquals(35, r.power().max());
        assertTrue(s.durability().min() > 0 && r.durability().min() > s.durability().min() && r.durability().max() > s.durability().max(), "a durability bonus that grows with the grade");
        assertEquals(0, s.enchantCount().min());
        assertEquals(1, s.enchantCount().max());
        assertEquals(1, s.enchantLevelCap(), "a minor enchantment");
        assertEquals(1, r.enchantCount().min());
        assertEquals(2, r.enchantCount().max());
        assertEquals(0, s.engraveCount().min());
        assertEquals(1, s.engraveCount().max());
        assertEquals(1, r.engraveCount().min());
        assertEquals(2, r.engraveCount().max());
        assertTrue(s.resistArmour().isZero() && s.resistHand().isZero(), "Special does not resist magic");
        assertEquals(5, r.resistArmour().min());
        assertEquals(12, r.resistArmour().max());
        assertEquals(4, r.resistHand().min());
        assertEquals(6, r.resistHand().max());
    }

    @Test
    void theSlotWeightsAddUpToAHundred() {
        double sum = 0;
        for (GearSlot s : GearSlot.values()) {
            assertTrue(t().slotWeight(s) > 0, s + " must be possible");
            sum += t().slotWeight(s);
        }
        assertEquals(100.0, sum, 1e-9);
    }

    @Test
    void theCurvesHaveTheShapesTheDesignPromises() {
        GearTables.Curve n = t().curve(GearGrade.NORMAL), s = t().curve(GearGrade.SPECIAL), r = t().curve(GearGrade.RARE);
        for (GearTables.Curve c : List.of(n, s, r)) {
            assertTrue(c.size() >= 3, "a curve with a start, a middle and an end");
            assertEquals(0.0, c.x(0), 0.0, "a curve starts at x = 0");
            assertEquals(1.0, c.y(0), 0.0, "and at 1, so the config's base chance is the chance at floor 1");
            assertEquals(1.0, c.x(c.size() - 1), 0.0, "and ends at x = 1");
        }
        double prevN = 2, prevR = 0, peakAt = 0, peak = 0;
        for (int i = 0; i <= 100; i++) {
            double x = i / 100.0;
            assertTrue(n.at(x) <= prevN + 1e-12, "Normal never rises");
            assertTrue(r.at(x) >= prevR - 1e-12, "Rare never falls");
            prevN = n.at(x);
            prevR = r.at(x);
            if (s.at(x) > peak) { peak = s.at(x); peakAt = x; }
        }
        assertTrue(peakAt >= 0.3 && peakAt <= 0.7, "Special peaks about half-way: " + peakAt);
        assertTrue(peak > s.at(0.0) * 1.5 && peak > s.at(1.0) * 1.2);
        assertTrue(n.at(1.0) < 0.5 && r.at(1.0) >= 5.0);
    }

    @Test
    void theCapsAreSane() {
        for (GearGrade g : GearGrade.values()) {
            assertTrue(t().maxChance(g) > 0.0 && t().maxChance(g) < 1.0, g + " cap");
            assertTrue(t().maxChance(g) <= t().totalCap(), g + " cap above the total cap");
        }
        assertTrue(t().totalCap() >= 0.25 && t().totalCap() <= 0.75, "total cap " + t().totalCap());
        List<Double> caps = new ArrayList<>();
        for (GearGrade g : GearGrade.values()) caps.add(t().maxChance(g));
        assertTrue(caps.get(0) >= caps.get(1) && caps.get(1) >= caps.get(2), "the commoner the grade the higher its cap: " + caps);
    }
}
