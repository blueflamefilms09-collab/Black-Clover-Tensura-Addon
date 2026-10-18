package com.newuniverse.nusmp.elite.hub;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: money must never be lost, minted or wrap around, so the whole coin arithmetic (value of a holding, the best mix, the plan that takes a price out of mixed coins and returns change) is
 * pinned here at the edges and by a randomised conservation check, and the coin table is pinned against the mod's one existing coin rule (a gold coin is a hundred silver, Wave16).
 */
class CoinMathTest {

    private static long[] have(long bronze, long silver, long gold, long stellar) { return new long[] {bronze, silver, gold, stellar}; }

    @Test
    void theTableIsHundredfoldAndMatchesTheRepoGoldCoinRule() throws Exception {
        assertEquals(1L, CoinKind.BRONZE.value());
        assertEquals(100L, CoinKind.SILVER.value());
        assertEquals(10_000L, CoinKind.GOLD.value());
        assertEquals(1_000_000L, CoinKind.STELLAR_GOLD.value());
        for (int k = 1; k < CoinKind.COUNT; k++) assertEquals(100L, CoinKind.values()[k].value() / CoinKind.values()[k - 1].value(), "each coin is a hundred of the one below");
        assertEquals("tensura:gold_coin", CoinKind.GOLD.itemId());
        assertEquals(CoinKind.SILVER, CoinKind.ofItemId("tensura:silver_coin"));
        assertNull(CoinKind.ofItemId("minecraft:gold_nugget"));
        Path wave = Path.of("src/main/java/com/newuniverse/nusmp/relics/wave/Wave16Tune.java");
        if (Files.exists(wave)) {
            assertTrue(Files.readString(wave).contains("GOLD_COIN_GP = 100"), "Wave16's rule (a gold coin is a hundred silver) changed: CoinKind must follow");
        }
    }

    @Test
    void valueAddsAHoldingUpAndSaturates() {
        assertEquals(0L, CoinMath.value(have(0, 0, 0, 0)));
        assertEquals(1_010_203L, CoinMath.value(have(3, 2, 1, 1)));
        assertEquals(5L, CoinMath.value(new long[] {5}), "a short array counts the missing kinds as none");
        assertEquals(7L, CoinMath.value(new long[] {7, -5, -1, -1}), "negative counts are zero");
        assertEquals(CoinMath.CAP, CoinMath.value(have(0, 0, 0, Long.MAX_VALUE)), "saturates instead of wrapping");
        assertEquals(CoinMath.CAP, CoinMath.value(have(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE)));
    }

    @Test
    void theBestMixUsesTheBiggestCoinsFirst() {
        assertEquals("[0, 0, 0, 0]", java.util.Arrays.toString(CoinMath.mix(0)));
        assertEquals("[99, 0, 0, 0]", java.util.Arrays.toString(CoinMath.mix(99)));
        assertEquals("[0, 1, 0, 0]", java.util.Arrays.toString(CoinMath.mix(100)));
        assertEquals("[3, 2, 1, 1]", java.util.Arrays.toString(CoinMath.mix(1_010_203)));
        assertEquals("[0, 0, 0, 5]", java.util.Arrays.toString(CoinMath.mix(5_000_000)));
        assertEquals("[0, 0, 0, 0]", java.util.Arrays.toString(CoinMath.mix(-5)));
        assertEquals(7L, CoinMath.coinCount(1_010_203), "1 stellar + 1 gold + 2 silver + 3 bronze = 7 coins");
        assertEquals("1 stellar gold, 1 gold, 2 silver, 3 bronze", CoinMath.describe(1_010_203));
        assertEquals("0 bronze", CoinMath.describe(0));
    }

    @Test
    void aPriceIsTakenSmallCoinsFirstAndTheOvershootComesBackAsChange() {
        CoinMath.Charge c = CoinMath.plan(250, have(0, 5, 1, 0));            // 5 silver + 1 gold, price 2 silver 50 bronze
        assertNotNull(c);
        assertEquals("[0, 3, 0, 0]", java.util.Arrays.toString(c.take()), "three silver cover 250 with 50 change; the gold coin is kept");
        assertEquals(50L, c.change());
        c = CoinMath.plan(250, have(0, 1, 1, 0));                            // only 1 silver + 1 gold: the gold crosses the line
        assertEquals("[0, 1, 1, 0]", java.util.Arrays.toString(c.take()));
        assertEquals(10_000L + 100L - 250L, c.change());
        c = CoinMath.plan(30, have(30, 0, 0, 1));                            // exact change in the small coin
        assertEquals("[30, 0, 0, 0]", java.util.Arrays.toString(c.take()));
        assertEquals(0L, c.change());
        assertTrue(CoinMath.plan(0, have(0, 0, 0, 0)).isEmpty(), "a free thing costs nothing");
    }

    @Test
    void aPriceThePlayerCannotPayIsRefusedAndABadPriceIsToo() {
        assertNull(CoinMath.plan(101, have(0, 1, 0, 0)));
        assertNull(CoinMath.plan(1, have(0, 0, 0, 0)));
        assertNull(CoinMath.plan(-1, have(5, 5, 5, 5)));
        assertNull(CoinMath.plan(Long.MAX_VALUE, have(5, 5, 5, 5)));
        assertFalse(CoinMath.plan(5, have(5, 0, 0, 0)).isEmpty());
    }

    @Test
    void noCoinIsEverLostOrMintedByAPurchase() {
        Random r = new Random(7);
        for (int i = 0; i < 20_000; i++) {
            long[] h = have(r.nextInt(300), r.nextInt(300), r.nextInt(30), r.nextInt(4));
            long total = CoinMath.value(h);
            long price = total == 0 ? 0 : Math.floorMod(r.nextLong(), total + 1);
            CoinMath.Charge c = CoinMath.plan(price, h);
            assertNotNull(c, "affordable price " + price + " of " + total);
            long taken = 0;
            for (int k = 0; k < CoinKind.COUNT; k++) {
                assertTrue(c.take()[k] >= 0 && c.take()[k] <= h[k], "takes no more coins than owned");
                taken += c.take()[k] * CoinKind.values()[k].value();
            }
            assertEquals(price + c.change(), taken, "what is taken is the price plus the change");
            long after = total - taken + CoinMath.value(CoinMath.mix(c.change()));
            assertEquals(total - price, after, "the player ends with exactly total - price");
        }
    }

    @Test
    void mixAndValueAreInverses() {
        Random r = new Random(11);
        for (int i = 0; i < 5_000; i++) {
            long v = Math.floorMod(r.nextLong(), 50_000_000L);
            assertEquals(v, CoinMath.value(CoinMath.mix(v)));
        }
        assertEquals(CoinMath.CAP, CoinMath.value(CoinMath.mix(Long.MAX_VALUE)), "an absurd amount is capped, not wrapped");
    }
}
