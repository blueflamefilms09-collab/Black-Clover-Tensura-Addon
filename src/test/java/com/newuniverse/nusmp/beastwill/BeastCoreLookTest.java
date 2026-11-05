package com.newuniverse.nusmp.beastwill;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why (docs/solid_cores.md): the client tints a Beast Will Core by the will id on its stack, so the table must equal the {@code tint} of every shipped will file and stay distinct and valid. */
class BeastCoreLookTest {
    @Test
    void everyShippedWillHasItsOwnFileTint() throws Exception {
        File[] files = Path.of("src/main/resources/data/nusmp/beast_will").toFile().listFiles((d, n) -> n.endsWith(".json"));
        assertTrue(files != null && files.length >= 27);
        Set<Integer> seen = new HashSet<>();
        for (File f : files) {
            String id = f.getName().replace(".json", "");
            int tint = JsonParser.parseString(Files.readString(f.toPath())).getAsJsonObject().get("tint").getAsInt();
            assertEquals(tint, BeastCoreLook.tint(id), id + ": BeastCoreLook is out of step with the data file");
            assertTrue(seen.add(tint), id + ": two wills share a colour");
        }
    }

    @Test
    void unknownWillsGetAStableOwnColourAndABareCoreTheOldViolet() {
        assertEquals(BeastCoreLook.DEFAULT_TINT, BeastCoreLook.tint(""));
        assertEquals(BeastCoreLook.DEFAULT_TINT, BeastCoreLook.tint(null));
        assertEquals(BeastCoreLook.tint("moon_hare_will"), BeastCoreLook.tint("moon_hare_will"));
        assertNotEquals(BeastCoreLook.tint("moon_hare_will"), BeastCoreLook.tint("sun_crow_will"));
        for (String id : new String[]{"a", "moon_hare_will", "zzzzzzzz", "dragon_will", ""}) {
            assertEquals(0xFF, BeastCoreLook.bodyArgb(id) >>> 24);
            assertEquals(0xFF, BeastCoreLook.iconArgb(id) >>> 24);
            assertEquals(0, BeastCoreLook.tint(id) >>> 24, "0xRRGGBB only");
        }
        assertEquals(0xFFFFFF, BeastCoreLook.towardWhite(0x123456, 0f));
        assertEquals(0x123456, BeastCoreLook.towardWhite(0x123456, 1f));
    }
}
