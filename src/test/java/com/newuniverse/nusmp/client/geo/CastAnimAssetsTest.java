package com.newuniverse.nusmp.client.geo;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The casting body animations (assets/nusmp/animations/player/cast.animation.json): every clip the server can name exists and only drives the player model's bones. */
class CastAnimAssetsTest {
    private static final Set<String> BONES = Set.of("body", "head", "left_arm", "right_arm", "left_leg", "right_leg");

    @Test
    void everyCastClipExistsAndDrivesRealBones() throws Exception {
        GeoAnim a;
        try (Reader r = new FileReader(new File("src/main/resources/assets/nusmp/animations/player/cast.animation.json"))) {
            a = GeoAnim.parse(JsonParser.parseReader(r).getAsJsonObject());
        }
        for (String name : new String[]{"cast_chant", "cast_mana_zone", "cast_release_thrust", "cast_release_sweep", "cast_release_side",
                "cast_release_up", "cast_release_slam", "cast_signature", "cast_fail"}) {
            GeoAnim.Clip c = a.clip(name);
            assertNotNull(c, name);
            assertTrue(c.length > 0f, name);
            for (String bone : c.bones.keySet()) assertTrue(BONES.contains(bone), name + " bone " + bone);
        }
        assertTrue(a.clip("cast_chant").loop);
        assertFalse(a.clip("cast_release_up").loop);
    }
}
