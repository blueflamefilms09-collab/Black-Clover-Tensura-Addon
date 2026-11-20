package com.newuniverse.nusmp.boss.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Leon study fix 9: parse, validate and clamp one arc_bosses file; and no shipped file changes a number. */
class BossDataTableTest {
    private static JsonObject json(String s) { return JsonParser.parseString(s).getAsJsonObject(); }

    @Test
    void aFullFileIsRead() {
        BossDataTable.Override o = BossDataTable.parse(json("{\"health\":4200,\"attack\":30,\"speed\":0.3,\"armor\":18,\"move_cooldown_scale\":0.9}"));
        assertEquals(4200.0, o.health());
        assertEquals(30.0, o.attack());
        assertEquals(0.3, o.speed());
        assertEquals(18.0, o.armor());
        assertEquals(0.9, o.moveCooldownScale());
    }

    @Test
    void aKeyIsOptionalButOneMustBeThere() {
        BossDataTable.Override o = BossDataTable.parse(json("{\"_comment\":\"only the bar\",\"health\":100}"));
        assertEquals(100.0, o.health());
        assertNull(o.attack());
        assertNull(o.moveCooldownScale());
        assertThrows(IllegalArgumentException.class, () -> BossDataTable.parse(json("{}")));
        assertThrows(IllegalArgumentException.class, () -> BossDataTable.parse(json("{\"_comment\":\"nothing\"}")));
    }

    @Test
    void numbersAreClampedNotRejected() {
        BossDataTable.Override o = BossDataTable.parse(json("{\"health\":99999999999,\"attack\":-5,\"speed\":7,\"armor\":500,\"move_cooldown_scale\":0}"));
        assertEquals(BossDataTable.HEALTH_MAX, o.health());
        assertEquals(0.0, o.attack());
        assertEquals(BossDataTable.SPEED_MAX, o.speed());
        assertEquals(BossDataTable.ARMOR_MAX, o.armor());
        assertEquals(BossDataTable.COOLDOWN_MIN, o.moveCooldownScale());
        assertEquals(BossDataTable.HEALTH_MIN, BossDataTable.parse(json("{\"health\":0}")).health());
        assertEquals(BossDataTable.COOLDOWN_MAX, BossDataTable.parse(json("{\"move_cooldown_scale\":100}")).moveCooldownScale());
    }

    @Test
    void anInvalidFileIsRefusedWhole() {
        for (String bad : new String[]{"{\"health\":\"big\"}", "{\"health\":null}", "{\"health\":true}", "{\"health\":[1]}", "{\"health\":{}}",
                "{\"health\":100,\"mana\":5}", "{\"health\":100,\"attack\":\"x\"}"})
            assertThrows(IllegalArgumentException.class, () -> BossDataTable.parse(json(bad)), bad);
        assertThrows(IllegalArgumentException.class, () -> BossDataTable.parse(null));
    }

    @Test
    void nonFiniteNumbersAreRefused() {
        JsonObject o = new JsonObject();
        o.addProperty("health", Double.NaN);
        assertThrows(IllegalArgumentException.class, () -> BossDataTable.parse(o));
        JsonObject i = new JsonObject();
        i.addProperty("attack", Double.POSITIVE_INFINITY);
        assertThrows(IllegalArgumentException.class, () -> BossDataTable.parse(i));
    }

    @Test
    void theFactorTurnsTheJavaDefaultIntoTheFileNumber() {
        assertEquals(2.0, BossDataTable.factor(3000.0, 1500.0));
        assertEquals(0.5, BossDataTable.factor(5.0, 10.0));
        assertEquals(1.0, BossDataTable.factor(null, 1500.0), "not set: the Java number stands");
        assertEquals(1.0, BossDataTable.factor(100.0, 0.0), "a zero default cannot be scaled");
        assertEquals(1.0, BossDataTable.factor(100.0, Double.NaN));
    }

    @Test
    void idsAreFileNamesOfBosses() {
        assertTrue(BossDataTable.validId("meliodas"));
        assertTrue(BossDataTable.validId("heath_grice"));
        assertFalse(BossDataTable.validId("sub/folder"));
        assertFalse(BossDataTable.validId("Meliodas"));
        assertFalse(BossDataTable.validId(""));
        assertFalse(BossDataTable.validId(null));
    }

    @Test
    void noShippedFileChangesANumber() throws IOException {
        Path dir = Path.of("src/main/resources/data/anime_multiverse/arc_bosses");
        if (!Files.exists(dir)) return;
        try (Stream<Path> s = Files.walk(dir)) {
            assertEquals(0, s.filter(Files::isRegularFile).count(), "the mod ships no arc_bosses override; the example lives under docs/");
        }
    }

    @Test
    void theExampleUnderDocsIsValidAndInert() throws IOException {
        Path ex = Path.of("docs/boss_data_example.json");
        assertTrue(Files.exists(ex), "the example file");
        BossDataTable.Override o = BossDataTable.parse(json(Files.readString(ex)));
        assertTrue(o.health() != null, "it shows the keys");
    }
}
