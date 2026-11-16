package com.newuniverse.nusmp.dragonballs;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The wish table and its caps: nothing unbounded, nothing forbidden, Super never weaker than Earth, every row reachable. */
class WishRulesTest {
    @Test
    void wishCountIsAtLeastOneAndAtMostThree() {
        assertEquals(1, WishRules.wishCount(BallSet.EARTH, 0));
        assertEquals(1, WishRules.wishCount(BallSet.EARTH, 1));
        assertEquals(2, WishRules.wishCount(BallSet.SUPER, 2));
        assertEquals(3, WishRules.wishCount(BallSet.SUPER, 50));
        assertEquals(1, BallSet.EARTH.defaultWishes());
        assertEquals(2, BallSet.SUPER.defaultWishes());
    }

    @Test
    void powerIsCappedForLife() {
        assertEquals(1, WishRules.powerLevels(BallSet.EARTH));
        assertEquals(2, WishRules.powerLevels(BallSet.SUPER));
        assertEquals(1, WishRules.addable(0, 1, 10));
        assertEquals(2, WishRules.addable(8, 2, 10));
        assertEquals(1, WishRules.addable(9, 2, 10));
        assertEquals(0, WishRules.addable(10, 2, 10));
        assertEquals(0, WishRules.addable(15, 2, 10));
        assertEquals(WishRules.POWER_CAP_MAX, 20);
        assertEquals(2, WishRules.addable(WishRules.POWER_CAP_MAX - 2, 5, 9999), "a huge configured cap is clamped to the hard ceiling");
        assertEquals(0, WishRules.addable(WishRules.POWER_CAP_MAX, 5, 9999));
        assertEquals(0, WishRules.addable(0, 3, -4), "a negative cap grants nothing");
        assertEquals(1, WishRules.addable(-7, 1, 10), "a corrupt negative level counts as zero");
        double hp = WishRules.POWER_CAP_MAX * WishRules.healthPerLevel(), atk = WishRules.POWER_CAP_MAX * WishRules.attackPerLevel();
        assertTrue(hp <= 40.0 && atk <= 10.0, "worst case +" + hp + " health, +" + atk + " attack");
    }

    @Test
    void coinsAreOneFixedAmountAndSuperIsMore() {
        assertTrue(WishRules.coins(BallSet.EARTH) > 0);
        assertTrue(WishRules.coins(BallSet.SUPER) > WishRules.coins(BallSet.EARTH));
        assertEquals(0L, WishRules.coins(BallSet.EARTH) % 10_000L, "whole gold coins only");
        assertEquals(0L, WishRules.coins(BallSet.SUPER) % 10_000L);
    }

    @Test
    void forbiddenWishesAreRefusedWhateverElseTheyAsk() {
        for (String s : List.of("make me immortal", "I wish to live forever", "kill everyone on the server", "destroy their house", "mind control the king",
                "change the world", "delete all their items", "make me invincible", "Never die!", "erase the nether", "steal his sword"))
            assertTrue(WishRules.forbidden(s), s);
        for (String s : List.of("revive me", "I want power", "give me coins", "a skill please", "the ruyi power pole", "become a saiyan"))
            assertFalse(WishRules.forbidden(s), s);
    }

    @Test
    void everyRowIsReachableByItsIdAndItsKeywords() {
        for (WishRules.Wish w : WishRules.ALL) {
            for (BallSet s : BallSet.values()) {
                if (w.superOnly() && s == BallSet.EARTH) {
                    assertNull(WishRules.match(s, w.id()), w.id() + " is Super only");
                    continue;
                }
                assertSame(w, WishRules.match(s, w.id()), w.id() + " by id in " + s);
                assertSame(w, WishRules.byId(w.id().toUpperCase()));
            }
            assertFalse(w.keywords().isEmpty());
        }
        assertNull(WishRules.match(BallSet.SUPER, "banana"));
        assertNull(WishRules.match(BallSet.SUPER, ""));
        assertNull(WishRules.match(BallSet.SUPER, null));
        assertNull(WishRules.byId(null));
        assertNull(WishRules.byId("nothing"));
    }

    @Test
    void earthNeverOffersTheWeaponAndSuperOffersEverything() {
        assertTrue(WishRules.forSet(BallSet.EARTH).stream().noneMatch(WishRules.Wish::superOnly));
        assertEquals(WishRules.ALL.size(), WishRules.forSet(BallSet.SUPER).size());
        assertEquals(WishRules.Kind.WEAPON, WishRules.match(BallSet.SUPER, "I want the ruyi staff").kind());
        assertNull(WishRules.match(BallSet.EARTH, "the ruyi staff"));
    }

    @Test
    void wishIdsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (WishRules.Wish w : WishRules.ALL) assertTrue(ids.add(w.id()), w.id());
        assertEquals(WishRules.Kind.values().length, WishRules.ALL.size(), "one row per kind");
    }

    @Test
    void giftTablesAreBoundedAndSuperIsNotWeaker() {
        for (BallSet s : BallSet.values()) {
            List<WishRules.Gift> items = WishRules.items(s);
            assertFalse(items.isEmpty());
            Set<String> seen = new HashSet<>();
            for (WishRules.Gift g : items) {
                assertTrue(g.count() >= 1 && g.count() <= 64, g.id());
                assertTrue(g.weight() >= 1, g.id());
                assertTrue(g.id().contains(":"));
                assertTrue(seen.add(g.id()), "duplicate " + g.id());
            }
            Random r = new Random(7);
            for (int i = 0; i < 200; i++) assertTrue(items.contains(WishRules.pick(items, r)));
            assertFalse(WishRules.skills(s).isEmpty());
            assertEquals(WishRules.skills(s).size(), new HashSet<>(WishRules.skills(s)).size());
        }
        assertTrue(WishRules.SUPER_SKILLS.size() >= WishRules.EARTH_SKILLS.size() - 2);
    }

    @Test
    void namedSkillFindsAWordOfTheId() {
        assertEquals("tensura:eye_of_truth", WishRules.namedSkill(WishRules.SUPER_SKILLS, "skill Eye of Truth please"));
        assertNull(WishRules.namedSkill(WishRules.SUPER_SKILLS, "skill banana"));
        assertEquals("Eye Of Truth", WishRules.skillName("tensura:eye_of_truth"));
    }
}
