package com.newuniverse.nusmp.skilltree.shop;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: rules 9 and 11 for the new goods are pinned by reading the source (nothing here can be run on a server): a purchase carries only the slot id and the price (the item and its count are
 * re-derived on the server from the shelf, never read from the packet), the pools are resolved on the server in one place behind a cache that a reload clears, nothing of the shop runs per tick, the
 * shelf is bounded, and the loop guard runs on the real shelf.
 */
class ShopItemServerSafetyTest {
    private static final Path SHOP = Path.of("src/main/java/com/newuniverse/nusmp/skilltree/shop");

    private static String read(String file) throws IOException {
        return Files.readString(SHOP.resolve(file));
    }

    @Test
    void aPurchasePacketCarriesNoItemAndNoCount() throws IOException {
        String src = read("BuyPayload.java");
        assertTrue(src.contains("public record BuyPayload(String offer, long price)"), "the packet is the slot id and the price the player saw, nothing else");
        assertFalse(src.contains("ItemStack"), "no item in the buy packet");
        assertTrue(src.contains("readUtf(StockBoard.MAX_ID)"), "the slot id is length-bounded");
    }

    @Test
    void theServerRederivesTheItemFromItsOwnShelf() throws IOException {
        String svc = read("ShopService.java");
        int buy = svc.indexOf("public static Result buy(");
        String body = svc.substring(buy, svc.indexOf("// ====", buy));
        assertTrue(body.contains("StockBoard.find(shelf(server), offerId)"), "the offer is looked up on the server's shelf by id");
        assertTrue(body.contains("ShopGoods.stack(offer)"), "the stack handed over is built from the shelf's offer");
        assertTrue(body.contains("judgeBuy") && body.contains("expectedPrice"), "the price the player saw is compared with the price now");
        assertTrue(body.indexOf("giveItemToPlayer") < body.indexOf("MarketGold.take"), "item first, gold second: a failure never leaves the player poor and empty-handed");
    }

    @Test
    void poolsAreResolvedOnlyInOnePlaceBehindACacheAClientNeverTouches() throws IOException {
        String pools = read("ShopPools.java");
        assertTrue(pools.contains("MarketData.generation()") && pools.contains("POOLS.clear()") && pools.contains("SLOTS.clear()"), "the cache is rebuilt only when the data or the tags change");
        assertTrue(pools.contains("computeIfAbsent(shopId"), "slots are built once per shop and then read");
        assertTrue(pools.contains("MAX_POOL"), "a pool is bounded");
        assertTrue(pools.contains("LOGGED.add(key)"), "a bad line is logged once, not once per shelf");
        assertFalse(pools.contains("net.minecraft.client"), "server side only");
        assertFalse(pools.contains("Minecraft.getInstance"), "no client singleton");
        String svc = read("ShopService.java");
        assertFalse(svc.contains("BuiltInRegistries.ITEM.getTag"), "the shelf never walks a registry tag itself");
        assertTrue(svc.contains("ShopPools.slots(shopId)"), "the shelf takes its slots from the cache");
        assertTrue(svc.contains("ShopLoopGuard.loops("), "the loop guard runs on every shelf");
    }

    @Test
    void nothingOfTheShopRunsPerTickAndEveryRollIsOSlots() throws IOException {
        try (var files = Files.list(SHOP)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String src = Files.readString(f);
                assertFalse(src.contains("ServerTickEvent") || src.contains("LevelTickEvent") || src.contains("PlayerTickEvent") || src.contains("EntityTickEvent"), f.getFileName() + " must not tick");
                assertFalse(src.contains("ChunkSource") || src.contains("getChunk(") || src.contains("loadChunk"), f.getFileName() + " must not touch chunks");
            }
        }
        String board = read("StockBoard.java");
        assertTrue(board.contains("MAX_SLOTS = 32") && board.contains("out.size() >= MAX_SLOTS"), "the shelf is capped");
        assertEquals(1, board.split("StockRoll.rollItem\\(", -1).length - 1, "one item roll per slot");
    }

    @Test
    void theReloadListenerIsBoundedAndTheTagsRebindInvalidatesThePools() throws IOException {
        String data = read("MarketData.java");
        assertTrue(data.contains("MAX_SHOPS = 16") && data.contains("++n > MAX_SHOPS"), "a datapack cannot make the reload read an unbounded number of shop files");
        assertTrue(data.contains("GENERATION.incrementAndGet()"));
        assertTrue(read("ShopSystem.java").contains("TagsUpdatedEvent"), "a tag rebinding rebuilds the pools");
    }

    @Test
    void theWitchKeepsHerBareSlotIdsAndEveryOtherShopIsPrefixed() throws IOException {
        String pools = read("ShopPools.java");
        assertTrue(pools.contains("MarketData.WITCH_SHOP.equals(shopId) ? \"\" : shopId + \"/\""));
        assertEquals("witch", BlackMarketWitch.SHOP);
        List<String> kinds = java.util.Arrays.stream(StockEntry.Kind.values()).map(StockEntry.Kind::id).toList();
        assertEquals(List.of("route_book", "sp_book", "item"), kinds);
    }
}
