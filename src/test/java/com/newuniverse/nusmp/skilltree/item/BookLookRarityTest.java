package com.newuniverse.nusmp.skilltree.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the 3.26.0 look reads a book's rarity from three places that must agree: the shipped table's optional {@code rarity} field of a line, the stack's optional component, and the
 * model overrides' thresholds. This pins the pure parts: the rank of every rarity id and of junk, the property value {@code (rank + 1) / 6} (strictly increasing, below 1, so an override can
 * tell neighbours apart), the table parsing (a book without a field is common, an unknown name is common, a prefix and capitals are tolerated, the first line of a book wins), the component
 * value rule, the legacy-look holder, and the drop shimmer's caps (24 books, 4 particles a tick, 24 blocks, one particle per book per 6 ticks, no world scan in the client class, no client
 * import in the common classes).
 */
class BookLookRarityTest {
    private static JsonObject table(String lines) {
        return JsonParser.parseString("{\"franchises\":[\"tensura\",\"magi\"],\"prop_steps\":20,\"lines\":{" + lines + "}}").getAsJsonObject();
    }

    private static String line(String id, String book, String extra) {
        return "\"" + id + "\":{\"book\":\"nusmp:" + book + "\",\"name\":\"" + id + "\",\"book_name\":\"" + id + " book\",\"color\":\"#112233\",\"franchise\":\"magi\"" + extra + "}";
    }

    @Test
    void ranksFollowTheContractOrderAndJunkIsCommon() {
        assertEquals(java.util.List.of("common", "uncommon", "rare", "epic", "mythic"), BookLook.RARITY_IDS);
        for (int i = 0; i < 5; i++) assertEquals(i, BookLook.rankOfId(BookLook.RARITY_IDS.get(i)));
        assertEquals(4, BookLook.rankOfId("MYTHIC"));
        assertEquals(3, BookLook.rankOfId(" nusmp:Epic "));
        assertEquals(0, BookLook.rankOfId("legendary"));
        assertEquals(0, BookLook.rankOfId(""));
        assertEquals(0, BookLook.rankOfId(null));
    }

    @Test
    void thePropertyValueIsRankPlusOneOverSixAndStrictlyIncreasing() {
        float last = 0;
        for (int rank = 0; rank < 5; rank++) {
            float v = BookLook.rarityStep(rank);
            assertEquals((rank + 1) / 6f, v, 1e-6);
            assertTrue(v > last && v < 1f);
            last = v;
        }
        assertEquals(BookLook.rarityStep(0), BookLook.rarityStep(-3), "a rank below 0 is common");
        assertEquals(BookLook.rarityStep(4), BookLook.rarityStep(99), "a rank above 4 is mythic");
    }

    @Test
    void theTableGivesTheRankOfABookAndCommonWhenItHasNone() {
        BookLook.Index idx = BookLook.parse(table(line("a", "a_book", ",\"rarity\":\"epic\"") + "," + line("b", "b_book", "") + "," + line("c", "c_book", ",\"rarity\":\"Mythic\"") + "," + line("d", "d_book", ",\"rarity\":\"nope\"")));
        assertEquals(3, idx.rankOf("nusmp:a_book"));
        assertEquals(0, idx.rankOf("nusmp:b_book"), "no field: common");
        assertEquals(4, idx.rankOf("nusmp:c_book"));
        assertEquals(0, idx.rankOf("nusmp:d_book"), "an unknown name: common");
        assertEquals(0, idx.rankOf("nusmp:unknown_book"));
        assertEquals(0, idx.rankOf(null));
        assertEquals(BookLook.rarityStep(3), idx.rarityValue("nusmp:a_book"), 0f);
        assertEquals(1 / 6f, idx.rarityValue("nusmp:unknown_book"), 1e-6, "an unknown book looks common");
        assertNotNull(idx.byBook("nusmp:a_book"), "the entry itself still parses");
    }

    @Test
    void theStaticReaderUsesTheCurrentIndex() {
        BookLook.Index before = BookLook.index();
        try {
            BookLook.set(BookLook.parse(table(line("a", "a_book", ",\"rarity\":\"rare\""))));
            assertEquals(3 / 6f, BookLook.rarityValue("nusmp:a_book"), 1e-6);
            BookLook.set(BookLook.Index.EMPTY);
            assertEquals(0f, BookLook.rarityValue("nusmp:a_book"), 1e-6, "a book the table does not know has no rarity value (the base look)");
        } finally {
            BookLook.set(before);
        }
    }

    @Test
    void theComponentValueRule() {
        assertEquals(-1, BookLook.rankOfValue(null), "no component: fall back to the table");
        assertEquals(2, BookLook.rankOfValue("rare"));
        assertEquals(4, BookLook.rankOfValue("nusmp:mythic"));
        assertEquals(0, BookLook.rankOfValue("what"));
        assertEquals(3, BookLook.rankOfValue(3));
        assertEquals(4, BookLook.rankOfValue(40));
        assertEquals(0, BookLook.rankOfValue(-2));
    }

    @Test
    void theLegacyHolderStartsOnTheNewLookAndFollowsTheFlag() {
        boolean before = LegacyLook.legacy;
        try {
            LegacyLook.legacy = false;
            assertFalse(LegacyLook.isLegacy());
            assertEquals(0f, LegacyLook.lookValue());
            LegacyLook.legacy = true;
            assertTrue(LegacyLook.isLegacy());
            assertEquals(1f, LegacyLook.lookValue());
        } finally {
            LegacyLook.legacy = before;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------ the drop shimmer
    @Test
    void theShimmerCapsAreTheContractNumbers() {
        assertEquals(24, BookShimmerRules.MAX_TRACKED);
        assertEquals(24.0, BookShimmerRules.RANGE);
        assertEquals(6, BookShimmerRules.PERIOD_TICKS);
        assertEquals(4, BookShimmerRules.MAX_PER_TICK);
        assertEquals(2, BookShimmerRules.MIN_RANK, "rare and better");
        assertFalse(BookShimmerRules.shimmers(0));
        assertFalse(BookShimmerRules.shimmers(1));
        assertTrue(BookShimmerRules.shimmers(2) && BookShimmerRules.shimmers(3) && BookShimmerRules.shimmers(4));
    }

    @Test
    void noTickEverSpawnsMoreThanFourParticlesAndEachBookGetsOneEverySixTicks() {
        for (int tracked = 0; tracked <= 500; tracked++) assertTrue(BookShimmerRules.particlesThisTick(tracked) <= 4, "tracked " + tracked);
        assertEquals(4, BookShimmerRules.particlesThisTick(24));
        assertEquals(1, BookShimmerRules.particlesThisTick(1));
        assertEquals(0, BookShimmerRules.particlesThisTick(0));
        for (long start : new long[]{0, 1, 5, 6, 1_000_003L}) {
            int[] perTick = new int[BookShimmerRules.PERIOD_TICKS];
            int[] perBook = new int[BookShimmerRules.MAX_TRACKED];
            for (int t = 0; t < BookShimmerRules.PERIOD_TICKS; t++) {
                for (int slot = 0; slot < BookShimmerRules.MAX_TRACKED; slot++) {
                    if (BookShimmerRules.due(start + t, slot)) {
                        perTick[t]++;
                        perBook[slot]++;
                    }
                }
            }
            for (int n : perTick) assertEquals(4, n, "24 books spread evenly: four a tick, start " + start);
            for (int n : perBook) assertEquals(1, n, "one particle per book per period");
        }
        assertTrue(BookShimmerRules.due(-6, 0), "a negative tick must not break the stagger");
    }

    @Test
    void rangeAndColours() {
        assertTrue(BookShimmerRules.inRange(24, 0, 0));
        assertTrue(BookShimmerRules.inRange(13, 13, 13), "22.5 blocks away diagonally is in range");
        assertFalse(BookShimmerRules.inRange(14, 14, 14), "24.25 blocks away diagonally is out of range");
        assertFalse(BookShimmerRules.inRange(0, 24.5, 0));
        assertEquals(0x55AAFF, BookShimmerRules.colorOf(2));
        assertEquals(0xC855FF, BookShimmerRules.colorOf(3));
        assertEquals(0xFFAA2B, BookShimmerRules.colorOf(4));
        assertEquals(0xFFFFFF, BookShimmerRules.colorOf(0));
    }

    @Test
    void theClientClassUsesTheCapsAndNeverScansTheWorld() throws Exception {
        String src = Files.readString(Paths.get("src/main/java/com/newuniverse/nusmp/client/skilltree/BookDropFx.java"));
        String code = stripComments(src);
        for (String must : new String[]{"BookShimmerRules.MAX_TRACKED", "BookShimmerRules.MAX_PER_TICK", "BookShimmerRules.due(", "BookShimmerRules.inRange(", "BookShimmerRules.shimmers(", "EntityJoinLevelEvent", "EntityLeaveLevelEvent", "LoggingOut"}) {
            assertTrue(code.contains(must), "BookDropFx must use " + must);
        }
        for (String never : new String[]{"getEntitiesOfClass", "getEntities(", "getAllEntities", "entitiesForRendering", "getChunk", "getBlockState"}) {
            assertFalse(code.contains(never), "BookDropFx must not scan the world: " + never);
        }
        // the particle options are built once (in the static block), never per tick
        Matcher m = Pattern.compile("new DustParticleOptions\\(").matcher(code);
        int count = 0;
        while (m.find()) count++;
        assertEquals(1, count, "one options construction, in the static initialiser");
        assertTrue(code.indexOf("new DustParticleOptions(") < code.indexOf("public static synchronized void init"), "built before init, in the static block");
    }

    @Test
    void theCommonClassesOfTheLookImportNoClientClass() throws Exception {
        for (String f : new String[]{"BookLook", "BookLookStack", "BookLookFiles", "BookLookSelfTests", "BookShimmerRules", "LegacyLook"}) {
            String code = stripComments(Files.readString(Paths.get("src/main/java/com/newuniverse/nusmp/skilltree/item/" + f + ".java")));
            assertFalse(code.contains("net.minecraft.client"), f + " is common code: no net.minecraft.client");
            assertFalse(code.contains("com.newuniverse.nusmp.client"), f + " is common code: no mod client class");
            assertFalse(code.contains("com.mojang.blaze3d"), f + " is common code: no blaze3d");
            assertFalse(code.contains("Minecraft.getInstance"), f + " is common code: no Minecraft.getInstance()");
        }
    }

    private static String stripComments(String src) {
        return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }
}
