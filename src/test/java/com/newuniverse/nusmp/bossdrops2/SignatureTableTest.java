package com.newuniverse.nusmp.bossdrops2;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why (docs/boss_drops_worthwhile.md): the signature table parser takes a good file and refuses every bad one with a message, and no file can ask for more than the caps. Pure. */
class SignatureTableTest {
    private static final String BOON = "[{\"effect\":\"minecraft:strength\",\"seconds\":900,\"amplifier\":1}]";

    private static JsonObject obj(String s) { return JsonParser.parseString(s).getAsJsonObject(); }

    private static String file(String sig, String extras, String unique) {
        return "{\"boss\":\"meliodas\",\"tier\":\"apex\",\"signature\":" + sig + (extras == null ? "" : ",\"extras\":" + extras) + (unique == null ? "" : ",\"one_of_one\":" + unique) + "}";
    }

    private static final String SIG = "{\"chance\":1.0,\"min\":1,\"max\":2,\"boon\":" + BOON + "}";

    @Test
    void aGoodFileIsRead() {
        SignatureTable.Table t = SignatureTable.parse(obj(file(SIG, "[{\"item\":\"minecraft:diamond\",\"chance\":0.5,\"min\":2,\"max\":5}]",
                "{\"relic\":\"hellblaze_the_demon_sin\",\"echo_chance\":0.2,\"echo_boon\":" + BOON + "}")));
        assertEquals("meliodas", t.boss());
        assertEquals(SignatureTable.Tier.APEX, t.tier());
        assertEquals(1, t.signature().min());
        assertEquals(2, t.signature().max());
        assertEquals(1, t.signature().boon().size());
        assertEquals(900, t.signature().boon().get(0).seconds());
        assertEquals("minecraft:diamond", t.extras().get(0).item());
        assertTrue(t.hasUnique());
        assertEquals("hellblaze_the_demon_sin", t.unique().relic());
        assertEquals(0.2, t.unique().echoChance());
    }

    @Test
    void extrasAndTheOneOfOneAreOptional() {
        SignatureTable.Table t = SignatureTable.parse(obj(file(SIG, null, null)));
        assertTrue(t.extras().isEmpty());
        assertFalse(t.hasUnique());
    }

    @Test
    void defaultsAreOneChanceOneCount() {
        SignatureTable.Table t = SignatureTable.parse(obj(file("{\"boon\":" + BOON + "}", null, null)));
        assertEquals(1.0, t.signature().chance());
        assertEquals(1, t.signature().min());
        assertEquals(1, t.signature().max());
    }

    private static void bad(String json, String mention) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> SignatureTable.parse(obj(json)), json);
        assertTrue(e.getMessage().contains(mention), "the message '" + e.getMessage() + "' should mention '" + mention + "'");
    }

    @Test
    void badFilesAreRefusedWithAReason() {
        bad("{}", "boss");
        bad("{\"boss\":\"Bad Id\",\"tier\":\"apex\",\"signature\":" + SIG + "}", "boss id");
        bad("{\"boss\":\"x\",\"tier\":\"god\",\"signature\":" + SIG + "}", "tier");
        bad("{\"boss\":\"x\",\"signature\":" + SIG + "}", "tier");
        bad("{\"boss\":\"x\",\"tier\":\"mid\"}", "signature");
        bad(file("{\"chance\":0.0,\"boon\":" + BOON + "}", null, null), "chance");
        bad(file("{\"chance\":1.5,\"boon\":" + BOON + "}", null, null), "chance");
        bad(file("{\"min\":0,\"boon\":" + BOON + "}", null, null), "min");
        bad(file("{\"min\":3,\"max\":2,\"boon\":" + BOON + "}", null, null), "min");
        bad(file("{\"max\":65,\"boon\":" + BOON + "}", null, null), "max");
        bad(file("{}", null, null), "boon");
        bad(file("{\"boon\":[]}", null, null), "boon");
        bad(file("{\"boon\":[{\"effect\":\"Strength\",\"seconds\":60}]}", null, null), "effect");
        bad(file("{\"boon\":[{\"effect\":\"minecraft:strength\",\"seconds\":5}]}", null, null), "seconds");
        bad(file("{\"boon\":[{\"effect\":\"minecraft:strength\",\"seconds\":86400}]}", null, null), "seconds");
        bad(file("{\"boon\":[{\"effect\":\"minecraft:strength\",\"seconds\":60,\"amplifier\":9}]}", null, null), "amplifier");
        bad(file(SIG, "[{\"item\":\"Diamond\"}]", null), "item");
        bad(file(SIG, "[{\"item\":\"minecraft:diamond\",\"max\":99}]", null), "max");
        bad(file(SIG, "\"nope\"", null), "extras");
        bad(file(SIG, null, "{\"relic\":\"Bad Relic\",\"echo_boon\":" + BOON + "}"), "relic");
        bad(file(SIG, null, "{\"relic\":\"x\",\"echo_chance\":2,\"echo_boon\":" + BOON + "}"), "echo_chance");
        bad(file(SIG, null, "{\"relic\":\"x\"}"), "echo_boon");
    }

    @Test
    void noFileCanAskForMoreThanTheCaps() {
        StringBuilder rows = new StringBuilder("[");
        for (int i = 0; i <= SignatureTable.MAX_EXTRAS; i++) rows.append(i == 0 ? "" : ",").append("{\"item\":\"minecraft:diamond\"}");
        bad(file(SIG, rows.append("]").toString(), null), "at most");
        StringBuilder boons = new StringBuilder("[");
        for (int i = 0; i <= SignatureTable.MAX_BOON; i++) boons.append(i == 0 ? "" : ",").append("{\"effect\":\"minecraft:strength\",\"seconds\":60}");
        bad(file("{\"boon\":" + boons.append("]") + "}", null, null), "boon");
        StringBuilder ok = new StringBuilder("[");
        for (int i = 0; i < SignatureTable.MAX_EXTRAS; i++) ok.append(i == 0 ? "" : ",").append("{\"item\":\"minecraft:diamond\",\"max\":64}");
        assertEquals(SignatureTable.MAX_EXTRAS, SignatureTable.parse(obj(file(SIG, ok.append("]").toString(), null))).extras().size());
    }
}
