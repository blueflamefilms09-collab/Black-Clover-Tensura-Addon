package com.newuniverse.nusmp.relics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 3.10.0: the relic_head / relic_feet Curios slots exist, are given to the player and list the head and feet relics. */
class HeadFeetCurioSlotsTest {
    static final Path RES = Path.of("src/main/resources");

    @Test
    void slotsExistAreAssignedAndTagged() throws IOException {
        for (String id : new String[]{"relic_head", "relic_feet"}) {
            JsonObject slot = JsonParser.parseString(Files.readString(RES.resolve("data/nusmp/curios/slots/" + id + ".json"))).getAsJsonObject();
            assertTrue(slot.get("size").getAsInt() >= 1, id);
            assertTrue(Files.readString(RES.resolve("data/nusmp/curios/entities/relics.json")).contains("\"" + id + "\""), id + " is not given to the player");
            assertTrue(RelicWear.OTHER_SLOTS.contains(id), id);
        }
        String head = Files.readString(RES.resolve("data/curios/tags/item/relic_head.json")), feet = Files.readString(RES.resolve("data/curios/tags/item/relic_feet.json"));
        assertTrue(head.contains("nusmp:mask_of_mind_melting"));
        assertTrue(feet.contains("nusmp:ruby_boots_of_planestriding"));
        assertFalse(head.contains("\"replace\": true") || feet.contains("\"replace\": true"));
    }
}
