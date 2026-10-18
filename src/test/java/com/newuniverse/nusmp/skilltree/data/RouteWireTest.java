package com.newuniverse.nusmp.skilltree.data;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the sync packet's byte layout is a contract between a server and a client that may be different builds of the mod, so it is pinned here without a game: a route survives a round trip
 * exactly (record equality, doubles included), the size the registry computes IS the number of bytes the writer produces, and the reader refuses every kind of oversized or out-of-range input
 * with an exception instead of building a record the screen would choke on. The tests bind the wire primitives to plain byte arrays that follow the Minecraft layout (little-endian base-128
 * integers, a string as its byte length plus UTF-8 bytes, big-endian doubles); the in-game self-test {@code tree.core.sync} repeats the round trip with the real {@code FriendlyByteBuf}.
 */
class RouteWireTest {
    // ------------------------------------------------------------------ byte-array stand-ins for the Minecraft primitives
    static class ByteSink implements RouteWire.Sink {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final boolean checkLengths;

        ByteSink(boolean checkLengths) { this.checkLengths = checkLengths; }

        @Override
        public void varInt(int v) {
            while ((v & -128) != 0) {
                out.write(v & 127 | 128);
                v >>>= 7;
            }
            out.write(v);
        }

        @Override
        public void utf(String s, int max) {
            if (checkLengths && s.length() > max) throw new IllegalArgumentException("too long");
            byte[] b = s.getBytes(StandardCharsets.UTF_8);
            varInt(b.length);
            out.write(b, 0, b.length);
        }

        @Override
        public void f64(double d) {
            long l = Double.doubleToLongBits(d);
            for (int i = 7; i >= 0; i--) out.write((int) (l >>> (8 * i)) & 0xFF);
        }

        byte[] bytes() { return out.toByteArray(); }
    }

    static class ByteSource implements RouteWire.Source {
        final byte[] data;
        int pos;

        ByteSource(byte[] data) { this.data = data; }

        @Override
        public int varInt() {
            int value = 0, shift = 0;
            while (true) {
                if (pos >= data.length) throw new IllegalStateException("the packet ended early");
                byte b = data[pos++];
                value |= (b & 127) << shift;
                if ((b & 128) == 0) return value;
                shift += 7;
                if (shift > 35) throw new IllegalStateException("VarInt too big");
            }
        }

        @Override
        public String utf(int max) {
            int len = varInt();
            if (len < 0 || len > max * 3 || pos + len > data.length) throw new IllegalStateException("bad string length " + len);
            String s = new String(data, pos, len, StandardCharsets.UTF_8);
            pos += len;
            if (s.length() > max) throw new IllegalStateException("string longer than " + max);
            return s;
        }

        @Override
        public double f64() {
            if (pos + 8 > data.length) throw new IllegalStateException("the packet ended early");
            long l = 0;
            for (int i = 0; i < 8; i++) l = (l << 8) | (data[pos++] & 0xFF);
            return Double.longBitsToDouble(l);
        }
    }

    // ------------------------------------------------------------------ helpers
    private static SkillRoute sample() {
        RouteSchema.Parsed p = RouteSchema.parse(RouteSchemaTest.ID, RouteSchemaTest.GOOD);
        assertTrue(p.clean(), "the sample must be clean: " + p.problems());
        return p.route();
    }

    private static SkillNode node(String id, int cost, int max, int x, int y) {
        return new SkillNode(id, "Node " + id, "", List.of(), cost, List.of(), max, "", List.of(), Map.of(), x, y, "");
    }

    private static SkillRoute routeOf(List<SkillNode> nodes) {
        return new SkillRoute("nusmp:t", "other", "T", "", "nusmp:t_book", "T Book", "", 0x123456, "minecraft:book", "t", 3, 1, nodes);
    }

    private static byte[] write(SkillRoute r, boolean check) {
        ByteSink s = new ByteSink(check);
        RouteWire.write(s, r);
        return s.bytes();
    }

    /** True when reading these bytes as a route throws. */
    private static boolean readFails(byte[] bytes) {
        try {
            RouteWire.read(new ByteSource(bytes));
            return false;
        } catch (RuntimeException expected) {
            return true;
        }
    }

    // ------------------------------------------------------------------ tests
    @Test
    void aRouteSurvivesTheRoundTripExactly() {
        SkillRoute r = sample();
        byte[] bytes = write(r, true);
        ByteSource in = new ByteSource(bytes);
        SkillRoute back = RouteWire.read(in);
        assertEquals(r, back, "every field, node, parent, price, table and double");
        assertEquals(bytes.length, in.pos, "the reader consumed exactly what the writer produced");
        assertEquals(0xE8541C, back.color());
        assertEquals(List.of(6.0, 7.0, 8.0, 9.0, 11.0), back.node("iron_fist").values().get("damage"));
    }

    @Test
    void doublesComeBackBitForBit() {
        Map<String, List<Double>> values = new LinkedHashMap<>();
        values.put("odd", List.of(0.1, 1.0 / 3.0, -0.0, 123456.789, Double.MIN_VALUE, 1.0e9));
        SkillNode n = new SkillNode("n", "N", "", List.of(), 1, List.of(), 5, "", List.of(), values, 0, 0, "");
        // the table has 6 entries: the reader's limit is 10, so this is fine
        SkillRoute back = RouteWire.read(new ByteSource(write(routeOf(List.of(n)), true)));
        assertEquals(values.get("odd"), back.node("n").values().get("odd"));
        assertTrue(Double.doubleToLongBits(back.node("n").values().get("odd").get(2)) == Double.doubleToLongBits(-0.0), "negative zero keeps its sign");
    }

    @Test
    void theComputedSizeIsTheNumberOfBytesWritten() {
        SkillRoute r = sample();
        assertEquals(write(r, true).length, RouteWire.size(r));
        SkillRoute plain = routeOf(List.of(node("a", 1, 1, 0, 0)));
        assertEquals(write(plain, true).length, RouteWire.size(plain));
        // multi-byte text changes the size by its UTF-8 length, not its character count
        SkillRoute accents = new SkillRoute("nusmp:t", "other", "Flämme 炙", "", "nusmp:t_book", "T Book", "", 1, "minecraft:book", "t", 3, 1, List.of(node("a", 1, 1, 0, 0)));
        assertEquals(write(accents, true).length, RouteWire.size(accents));
        assertTrue(RouteWire.size(accents) > RouteWire.size(plain) - 10);
    }

    @Test
    void variableLengthIntegersHaveTheMinecraftSizes() {
        int[][] cases = {{0, 1}, {127, 1}, {128, 2}, {16383, 2}, {16384, 3}, {2097151, 3}, {2097152, 4}, {268435455, 4}, {268435456, 5}, {-1, 5}, {Integer.MAX_VALUE, 5}};
        for (int[] c : cases) {
            assertEquals(c[1], RouteWire.varIntSize(c[0]), "size of " + c[0]);
            ByteSink s = new ByteSink(true);
            s.varInt(c[0]);
            assertEquals(c[1], s.bytes().length, "the test stand-in agrees for " + c[0]);
        }
    }

    @Test
    void aTextThatIsTooLongIsRefusedByTheSizeCheckJustLikeTheRealWriter() {
        SkillRoute r = new SkillRoute("nusmp:t", "other", "x".repeat(65), "", "nusmp:t_book", "T", "", 1, "minecraft:book", "t", 3, 1, List.of(node("a", 1, 1, 0, 0)));
        boolean threw = false;
        try {
            RouteWire.size(r);
        } catch (IllegalArgumentException expected) {
            threw = true;
            assertTrue(expected.getMessage().contains("wire limit of 64"), expected.getMessage());
        }
        assertTrue(threw, "65 characters do not fit a 64-character name");
        assertEquals(64, RouteSchema.LEN_NAME, "the writer's limit is the schema's limit");
    }

    @Test
    void manyRoutesRoundTripInOrderAndTheEmptyListToo() {
        List<SkillRoute> routes = new ArrayList<>();
        routes.add(sample());
        routes.add(routeOf(List.of(node("a", 1, 1, 0, 0), node("b", 2, 2, 1, 0))));
        routes.add(new SkillRoute("nusmp:u", "x", "U", "d", "nusmp:u_book", "U Book", "l", 0xFFFFFF, "minecraft:stick", "u", 0, 0, List.of(node("c", 50, 10, 12, 9))));
        ByteSink s = new ByteSink(true);
        RouteWire.writeAll(s, routes);
        assertEquals(routes, RouteWire.readAll(new ByteSource(s.bytes())));

        ByteSink e = new ByteSink(true);
        RouteWire.writeAll(e, List.of());
        assertEquals(1, e.bytes().length);
        assertTrue(RouteWire.readAll(new ByteSource(e.bytes())).isEmpty());
    }

    @Test
    void theReaderRefusesMoreRoutesThanTheLimit() {
        ByteSink s = new ByteSink(true);
        s.varInt(RouteWire.MAX_ROUTES + 1);
        boolean threw = false;
        try {
            RouteWire.readAll(new ByteSource(s.bytes()));
        } catch (RuntimeException expected) {
            threw = true;
        }
        assertTrue(threw);
        ByteSink ok = new ByteSink(true);
        ok.varInt(0);
        assertNotNull(RouteWire.readAll(new ByteSource(ok.bytes())), "zero routes is fine");
    }

    @Test
    void theReaderRefusesCountsAndNumbersOutsideTheSchemaRanges() {
        assertFalse(readFails(write(routeOf(List.of(node("a", 1, 1, 0, 0))), false)), "the control: a plain route reads fine");

        List<SkillNode> forty = new ArrayList<>(), fortyOne = new ArrayList<>();
        for (int i = 0; i < 41; i++) {
            if (i < 40) forty.add(node("n" + i, 1, 1, 0, 0));
            fortyOne.add(node("n" + i, 1, 1, 0, 0));
        }
        assertFalse(readFails(write(routeOf(forty), false)), "forty nodes is the limit");
        assertTrue(readFails(write(routeOf(fortyOne), false)), "41 nodes");
        assertTrue(readFails(write(routeOf(List.of()), false)), "no node at all");

        assertTrue(readFails(write(routeOf(List.of(node("a", 0, 1, 0, 0))), false)), "cost 0");
        assertTrue(readFails(write(routeOf(List.of(node("a", 51, 1, 0, 0))), false)), "cost 51");
        assertTrue(readFails(write(routeOf(List.of(node("a", 1, 0, 0, 0))), false)), "max level 0");
        assertTrue(readFails(write(routeOf(List.of(node("a", 1, 11, 0, 0))), false)), "max level 11");
        assertTrue(readFails(write(routeOf(List.of(node("a", 1, 1, 13, 0))), false)), "x 13");
        assertTrue(readFails(write(routeOf(List.of(node("a", 1, 1, 0, 10))), false)), "y 10");
        assertTrue(readFails(write(routeOf(List.of(node("a", 1, 1, -1, 0))), false)), "x -1 (a negative number is five bytes on the wire)");

        SkillNode manyPrices = new SkillNode("a", "A", "", List.of(), 1, List.of(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), 10, "", List.of(), Map.of(), 0, 0, "");
        assertTrue(readFails(write(routeOf(List.of(manyPrices)), false)), "11 prices");
        SkillNode badPrice = new SkillNode("a", "A", "", List.of(), 1, List.of(0), 10, "", List.of(), Map.of(), 0, 0, "");
        assertTrue(readFails(write(routeOf(List.of(badPrice)), false)), "a price of 0");
        SkillNode badParent = new SkillNode("a", "A", "", List.of(new SkillNode.Parent("b", 0)), 1, List.of(), 1, "", List.of(), Map.of(), 0, 0, "");
        assertTrue(readFails(write(routeOf(List.of(badParent)), false)), "a parent level of 0");
        List<SkillNode.Parent> seven = new ArrayList<>();
        for (int i = 0; i < 7; i++) seven.add(new SkillNode.Parent("p" + i, 1));
        assertTrue(readFails(write(routeOf(List.of(new SkillNode("a", "A", "", seven, 1, List.of(), 1, "", List.of(), Map.of(), 0, 0, ""))), false)), "7 parents");
        Map<String, List<Double>> tooLong = new LinkedHashMap<>();
        tooLong.put("t", List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0));
        assertTrue(readFails(write(routeOf(List.of(new SkillNode("a", "A", "", List.of(), 1, List.of(), 1, "", List.of(), tooLong, 0, 0, ""))), false)), "11 table entries");
        Map<String, List<Double>> seventeen = new LinkedHashMap<>();
        for (int i = 0; i < 17; i++) seventeen.put("t" + i, List.of(1.0));
        assertTrue(readFails(write(routeOf(List.of(new SkillNode("a", "A", "", List.of(), 1, List.of(), 1, "", List.of(), seventeen, 0, 0, ""))), false)), "17 tables");
        List<String> nine = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i");
        assertTrue(readFails(write(routeOf(List.of(new SkillNode("a", "A", "", List.of(), 1, List.of(), 1, "", nine, Map.of(), 0, 0, ""))), false)), "9 passives");

        SkillRoute loud = new SkillRoute("nusmp:t", "other", "T", "", "nusmp:t_book", "T", "", 1, "minecraft:book", "t", 101, 1, List.of(node("a", 1, 1, 0, 0)));
        assertTrue(readFails(write(loud, false)), "duplicate_sp 101");
        SkillRoute heavy = new SkillRoute("nusmp:t", "other", "T", "", "nusmp:t_book", "T", "", 1, "minecraft:book", "t", 3, 1001, List.of(node("a", 1, 1, 0, 0)));
        assertTrue(readFails(write(heavy, false)), "loot_weight 1001");
    }

    @Test
    void theReaderRefusesATextLongerThanItsLimitAndATruncatedPacket() {
        SkillRoute longName = new SkillRoute("nusmp:t", "other", "x".repeat(65), "", "nusmp:t_book", "T", "", 1, "minecraft:book", "t", 3, 1, List.of(node("a", 1, 1, 0, 0)));
        assertTrue(readFails(write(longName, false)), "a 65-character name written by a careless peer");
        byte[] whole = write(sample(), true);
        assertFalse(readFails(whole));
        for (int cut : new int[]{0, 1, whole.length / 2, whole.length - 1}) {
            byte[] part = new byte[cut];
            System.arraycopy(whole, 0, part, 0, cut);
            assertTrue(readFails(part), "cut at " + cut);
        }
    }
}
