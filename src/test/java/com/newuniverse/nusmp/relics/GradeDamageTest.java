package com.newuniverse.nusmp.relics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GradeDamageTest {
    private static final String[] GRADES = {"normal", "special", "rare", "unique", "legend", "god", "genesis"};
    private static final int[][] EXPECT = {{20, 30}, {35, 55}, {60, 85}, {90, 115}, {130, 175}, {190, 240}, {260, 350}};
    private static final String[] TYPES = {"dagger", "shortsword", "longsword", "rapier", "katana", "sabre", "dadao", "greatsword", "handaxe", "battleaxe",
            "greataxe", "crescent", "scythe", "cleaver", "spear", "glaive", "trident", "staff", "whip", "coilwhip", "crossbow", "bow", "hammer", "greathammer",
            "mace", "club", "wand", "shield", "item", "handitem", "sword", "axe", "halberd", "lance", "bogus"};

    @Test void rangesExact() {
        for (int i = 0; i < 7; i++) {
            int[] r = GradeDamage.range(GRADES[i]);
            assertEquals(EXPECT[i][0], r[0]);
            assertEquals(EXPECT[i][1], r[1]);
        }
        assertEquals(20, GradeDamage.range("COMMON")[0]);
        assertEquals(30, GradeDamage.range("Common")[1]);
        assertEquals(260, GradeDamage.range("GeNeSiS")[0]);
    }

    @Test void damageInsideRange() {
        for (int i = 0; i < 7; i++) for (String t : TYPES) {
            double d = GradeDamage.damage(GRADES[i], t);
            assertTrue(d >= EXPECT[i][0] && d <= EXPECT[i][1], GRADES[i] + " " + t + " " + d);
        }
    }

    @Test void monotonicByGrade() {
        for (String t : TYPES) for (int i = 1; i < 7; i++)
            assertTrue(GradeDamage.damage(GRADES[i], t) > GradeDamage.damage(GRADES[i - 1], t), t);
    }

    @Test void typeOrder() {
        assertTrue(GradeDamage.damage("rare", "dagger") < GradeDamage.damage("rare", "longsword"));
        assertTrue(GradeDamage.damage("rare", "longsword") < GradeDamage.damage("rare", "greatsword"));
        assertTrue(GradeDamage.damage("rare", "rapier") < GradeDamage.damage("rare", "spear"));
        assertTrue(GradeDamage.damage("rare", "spear") < GradeDamage.damage("rare", "hammer"));
        assertTrue(GradeDamage.damage("rare", "hammer") < GradeDamage.damage("rare", "scythe"));
    }

    @Test void raisedStaysInEffectiveRange() {
        for (int b = 0; b < 7; b++) for (int e = b; e < 7; e++) for (String t : TYPES) {
            double base = GradeDamage.damage(b, t);
            double d = GradeDamage.raised(b, e, base, t);
            if (e == b) assertEquals(base, d, 1e-9);
            else assertTrue(d >= EXPECT[e][0] && d <= EXPECT[e][1], b + "->" + e + " " + t + " " + d);
            assertTrue(d >= base);
        }
    }
}
