package com.newuniverse.nusmp.prop;

import com.google.gson.JsonParser;
import com.newuniverse.nusmp.client.geo.GeoModelData;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 1.93.0 (the rebuild of Shakudo, Scale, Spike, Nail and Hair Magic), headless: the synced word of the constructs round-trips, every armour model has a
 * root bone for every piece its painter draws (the painters' piece lists are read from the source), the staggers stay inside 0 to 12 ticks of the Djinn's
 * equip animation, and every model, glow map and particle texture the rebuild names is there.
 */
class ShakudoRebuildTest {
    private static final File ASSETS = new File("src/main/resources/assets/nusmp");
    private static final File AURA_SRC = new File("src/main/java/com/newuniverse/nusmp/client/aura");
    private static final List<String> KEYS = List.of("shakudo", "scale", "spike", "nail", "hair");
    private static final List<String> PROPS = List.of("shakudo_golem", "shakudo_bulwark", "scale_serpent", "scale_shield", "spike_pillar", "spike_urchin",
            "nail_bolt", "nail_hammer", "hair_tendril", "hair_cocoon");

    @Test
    void theSyncedWordRoundTrips() {
        for (int variant = 0; variant < 16; variant += 5) {
            for (int state = 0; state < 4; state++) {
                long now = 123456L;
                int p = ShakudoPropState.pack(variant, state, now);
                assertEquals(state, ShakudoPropState.state(p));
                assertEquals(variant, ShakudoPropState.variant(p));
                assertEquals(0, ShakudoPropState.since(p, now));
                assertEquals(17, ShakudoPropState.since(p, now + 17));
            }
        }
    }

    @Test
    void theClockWrapsInsteadOfBreaking() {
        long start = 65535L;
        int p = ShakudoPropState.pack(2, ShakudoPropState.ACT, start);
        assertEquals(5, ShakudoPropState.since(p, start + 5));          // 65535 + 5 wraps past the 16 bit clock
        assertEquals(ShakudoPropState.ACT, ShakudoPropState.state(p));
        assertEquals(2, ShakudoPropState.variant(p));
    }

    @Test
    void everyPieceOfEveryArmourIsARootBoneOfItsModel() throws Exception {
        Pattern piece = Pattern.compile("ShakudoPieceKit\\.(?:piece|head|detail)\\(\"([a-z_]+)\", Anchor\\.[A-Z_]+, (\\d+),");
        for (String key : KEYS) {
            String pascal = Character.toUpperCase(key.charAt(0)) + key.substring(1);
            String src = Files.readString(new File(AURA_SRC, pascal + "Aura.java").toPath());
            Matcher m = piece.matcher(src);
            int n = 0;
            File geo = new File(ASSETS, "geo/entity/" + key + "_armor.geo.json");
            assertTrue(geo.isFile(), key + ": no armour model");
            GeoModelData data;
            try (Reader r = new FileReader(geo)) { data = GeoModelData.parse(geo.getName(), JsonParser.parseReader(r).getAsJsonObject()); }
            while (m.find()) {
                n++;
                GeoModelData.Bone b = data.bones.get(m.group(1));
                assertNotNull(b, key + ": the piece " + m.group(1) + " is not a bone of the model");
                assertNull(b.parent, key + ": the piece " + m.group(1) + " must be a root bone");
                int delay = Integer.parseInt(m.group(2));
                assertTrue(delay >= 0 && delay <= 12, key + ": the piece " + m.group(1) + " has the stagger " + delay + " outside 0 to 12");
            }
            assertTrue(n >= 11, key + ": fewer than 11 pieces (" + n + ")");
            for (String must : List.of("helm", "chest", "pauldron_r", "pauldron_l", "vambrace_r", "vambrace_l", "gauntlet_r", "gauntlet_l", "greave_r", "greave_l", "back"))
                assertTrue(src.contains("\"" + must + "\""), key + ": no piece " + must);
        }
    }

    @Test
    void modelsGlowMapsAndAnimationsExist() {
        for (String key : KEYS) assertAssets(key + "_armor");
        for (String name : PROPS) assertAssets(name);
    }

    private static void assertAssets(String id) {
        assertTrue(new File(ASSETS, "geo/entity/" + id + ".geo.json").isFile(), id + ": no model");
        assertTrue(new File(ASSETS, "animations/entity/" + id + ".animation.json").isFile(), id + ": no animations");
        assertTrue(new File(ASSETS, "textures/entity/" + id + ".png").isFile(), id + ": no texture");
        assertTrue(new File(ASSETS, "textures/entity/" + id + "_glow.png").isFile(), id + ": no glow map");
    }

    @Test
    void theParticleSpritesAreThere() {
        for (String key : KEYS)
            for (String s : List.of("sigil", "ring", "flare", "streak", "glow", "crack", "mote"))
                assertTrue(new File(ASSETS, "textures/particle/" + key + "_" + s + ".png").isFile(), key + "_" + s);
        for (String s : List.of("shakudo_plate", "shakudo_flake", "shakudo_slab", "shakudo_chip", "scale_scale", "scale_ripple", "scale_fin", "scale_gleam",
                "spike_spike", "spike_spike2", "spike_shard", "spike_burst", "nail_nail", "nail_nail_small", "nail_hammer", "nail_spark",
                "hair_lock", "hair_lock2", "hair_strand", "hair_tuft"))
            assertTrue(new File(ASSETS, "textures/particle/" + s + ".png").isFile(), s);
    }
}
