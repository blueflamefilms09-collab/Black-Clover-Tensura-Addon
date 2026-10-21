package com.newuniverse.nusmp.labyrinth.loot.backpack;

import com.newuniverse.nusmp.labyrinth.loot.LuckGauss;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure rules of the backpack: the five tiers, the upgrade rule, the window's layout, and the drop rules. */
class BackpackLogicTest {
    @Test
    void theFiveTiersHaveTheSpecsSlotCounts() {
        int[] want = {27, 36, 45, 54, 63};
        assertEquals(5, BackpackTier.values().length);
        for (int i = 0; i < 5; i++) {
            BackpackTier t = BackpackTier.values()[i];
            assertEquals(want[i], t.slots(), t.id());
            assertEquals(0, t.slots() % BackpackTier.COLUMNS, "a whole number of rows");
            assertEquals(t.slots() / 9, t.rows());
        }
        assertEquals("leather", BackpackTier.LEATHER.id());
        assertEquals(BackpackTier.MAX_SLOTS, BackpackTier.NETHERITE.slots());
        assertEquals(BackpackTier.LEATHER, BackpackTier.of(-4), "below the first is the first");
        assertEquals(BackpackTier.NETHERITE, BackpackTier.of(99), "above the last is the last");
        for (BackpackTier t : BackpackTier.values()) {
            assertSame(t, BackpackTier.of(t.ordinal()));
            assertSame(t, BackpackTier.fromId(t.id().toUpperCase()));
        }
        assertNull(BackpackTier.fromId("wood"));
        assertNull(BackpackTier.fromId(null));
    }

    @Test
    void savedOrdinalsAreFrozen() {
        // the ordinal is the saved number (block state, item component): the order must never change, only grow at the end
        assertEquals(0, BackpackTier.LEATHER.ordinal());
        assertEquals(1, BackpackTier.IRON.ordinal());
        assertEquals(2, BackpackTier.GOLD.ordinal());
        assertEquals(3, BackpackTier.DIAMOND.ordinal());
        assertEquals(4, BackpackTier.NETHERITE.ordinal());
    }

    @Test
    void upgradesOnlyEverGrowAPack() {
        for (BackpackTier cur : BackpackTier.values()) {
            for (BackpackTier up : BackpackTier.values()) {
                BackpackTier r = BackpackTier.upgraded(cur, up);
                assertTrue(r.slots() >= cur.slots(), "never smaller: " + cur + " + " + up);
                assertEquals(up.ordinal() > cur.ordinal() ? up : cur, r);
            }
            assertSame(cur, BackpackTier.upgraded(cur, null), "no upgrade, no change");
        }
    }

    @Test
    void upgradeItemsNameTheirTiers() {
        assertNull(BackpackTier.LEATHER.upgradeItemId(), "leather is the plain pack");
        Set<String> ids = new HashSet<>();
        for (BackpackTier t : BackpackTier.values()) {
            if (t == BackpackTier.LEATHER) continue;
            assertEquals("backpack_upgrade_" + t.id(), t.upgradeItemId());
            assertSame(t, BackpackTier.ofUpgrade(t.upgradeItemId()));
            assertTrue(ids.add(t.upgradeItemId()));
        }
        assertNull(BackpackTier.ofUpgrade("backpack_upgrade_wood"));
        assertSame(BackpackTier.NETHERITE, BackpackTier.NETHERITE.next(), "the last tier has no next");
        assertFalse(BackpackTier.NETHERITE.hasNext());
        assertSame(BackpackTier.IRON, BackpackTier.LEATHER.next());
    }

    @Test
    void aListOfAnySizeFindsTheTierThatFitsIt() {
        assertSame(BackpackTier.LEATHER, BackpackTier.fitting(0));
        assertSame(BackpackTier.LEATHER, BackpackTier.fitting(27));
        assertSame(BackpackTier.LEATHER, BackpackTier.fitting(35));
        assertSame(BackpackTier.IRON, BackpackTier.fitting(36));
        assertSame(BackpackTier.GOLD, BackpackTier.fitting(50));
        assertSame(BackpackTier.NETHERITE, BackpackTier.fitting(500));
    }

    @Test
    void theLayoutHasNoOverlapAndEverySlotIsInsideThePanel() {
        for (BackpackTier t : BackpackTier.values()) {
            int rows = t.rows();
            int h = BackpackLayout.height(rows);
            Set<Long> taken = new HashSet<>();
            for (int i = 0; i < t.slots(); i++) place(taken, BackpackLayout.slotX(i), BackpackLayout.slotY(i), h);
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) place(taken, 8 + col * 18, BackpackLayout.inventoryY(rows) + row * 18, h);
            for (int col = 0; col < 9; col++) place(taken, 8 + col * 18, BackpackLayout.hotbarY(rows), h);
            assertEquals(t.slots() + 36, taken.size());
            assertTrue(BackpackLayout.inventoryLabelY(rows) > BackpackLayout.slotY(t.slots() - 1) + 16, "the Inventory label is under the last row");
        }
        // the vanilla chest: three rows, the inventory at y = 85 and the hotbar at y = 143
        assertEquals(85, BackpackLayout.inventoryY(3));
        assertEquals(143, BackpackLayout.hotbarY(3));
        assertEquals(168, BackpackLayout.height(3));
    }

    private static void place(Set<Long> taken, int x, int y, int panelHeight) {
        assertTrue(x >= 0 && x + 18 <= BackpackLayout.WIDTH, "x inside the panel: " + x);
        assertTrue(y >= 0 && y + 18 <= panelHeight, "y inside the panel: " + y + " of " + panelHeight);
        assertTrue(taken.add(((long) x << 32) | y), "two slots at " + x + "," + y);
        for (long other : taken) {
            int ox = (int) (other >> 32), oy = (int) other;
            if (ox == x && oy == y) continue;
            assertFalse(Math.abs(ox - x) < 18 && Math.abs(oy - y) < 18, "slots overlap at " + x + "," + y + " and " + ox + "," + oy);
        }
    }

    // ---------------------------------------------------------------- drops
    @Test
    void theFirstFloorsOnlyDropThePlainPack() {
        Random rnd = new Random(1);
        int hits = 0;
        for (int i = 0; i < 20000; i++) {
            BackpackDrops.Drop d = BackpackDrops.roll(rnd::nextDouble, 20.0, 3, LuckGauss.Kind.CHEST, 1.0);
            assertNull(d.upgrade(), "no upgrade before the first upgrade floor");
            if (d.backpack()) hits++;
        }
        assertTrue(hits > 1000, "a lucky chest on floor 3 leaves packs: " + hits);
    }

    @Test
    void anUpgradeNeverExceedsWhatTheFloorAllows() {
        Random rnd = new Random(2);
        for (int floor : new int[]{5, 10, 39, 40, 119, 120, 299, 300, 1500}) {
            BackpackTier cap = BackpackDrops.cap(floor);
            for (int i = 0; i < 5000; i++) {
                BackpackDrops.Drop d = BackpackDrops.roll(rnd::nextDouble, 20.0, floor, LuckGauss.Kind.BOSS, 20.0);
                if (d.upgrade() != null) {
                    assertTrue(d.upgrade().ordinal() <= cap.ordinal(), "floor " + floor + " dropped " + d.upgrade() + " over its cap " + cap);
                    assertTrue(d.upgrade().ordinal() >= 1, "an upgrade is never the leather one");
                    assertFalse(d.backpack(), "one item per roll");
                }
            }
        }
        assertEquals(BackpackTier.LEATHER, BackpackDrops.cap(0));
        assertEquals(BackpackTier.IRON, BackpackDrops.cap(10));
        assertEquals(BackpackTier.NETHERITE, BackpackDrops.cap(-300), "the abyssal floors count like the holy ones");
        for (BackpackTier t : BackpackTier.values()) assertEquals(t, BackpackDrops.cap(BackpackDrops.firstFloor(t)));
    }

    @Test
    void luckAndTheRateRaiseTheDropRateAndZeroSwitchesItOff() {
        int[] hits = new int[4];
        double[][] cfg = {{0.0, 1.0}, {20.0, 1.0}, {0.0, 0.0}, {0.0, 5.0}};
        for (int k = 0; k < 4; k++) {
            Random rnd = new Random(7);
            for (int i = 0; i < 40000; i++) if (BackpackDrops.roll(rnd::nextDouble, cfg[k][0], 60, LuckGauss.Kind.KILL, cfg[k][1]).any()) hits[k]++;
        }
        assertTrue(hits[1] > hits[0] * 2, "Luck 20 should more than double the rate: " + hits[0] + " vs " + hits[1]);
        assertEquals(0, hits[2], "a rate of zero drops nothing");
        assertTrue(hits[3] > hits[0] * 2, "a rate of five is clearly above one: " + hits[0] + " vs " + hits[3]);
        assertTrue(hits[0] > 0, "even an unlucky kill can leave one, rarely");
        assertTrue(hits[0] < 40000 * 0.02, "but not often: " + hits[0]);
    }

    @Test
    void bossesLeaveUpgradesMoreOftenThanChests() {
        int bossUp = 0, chestUp = 0, bossAll = 0, chestAll = 0;
        Random a = new Random(3), b = new Random(3);
        for (int i = 0; i < 60000; i++) {
            BackpackDrops.Drop x = BackpackDrops.roll(a::nextDouble, 10.0, 200, LuckGauss.Kind.BOSS, 1.0);
            BackpackDrops.Drop y = BackpackDrops.roll(b::nextDouble, 10.0, 200, LuckGauss.Kind.CHEST, 1.0);
            if (x.any()) bossAll++;
            if (y.any()) chestAll++;
            if (x.upgrade() != null) bossUp++;
            if (y.upgrade() != null) chestUp++;
        }
        assertTrue(bossAll > chestAll, "a boss leaves one more often");
        assertTrue(bossUp / (double) bossAll > chestUp / (double) chestAll, "and more of them are upgrades");
    }
}
