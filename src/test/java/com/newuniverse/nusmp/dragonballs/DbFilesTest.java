package com.newuniverse.nusmp.dragonballs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reads the repo's own files: every ball has a texture and a model (live + stone), the radar and Shenron assets exist, the lang file is flat and has every key the code asks for, and the code stays
 * server safe and bounded (no client class, no Minecraft.getInstance, only loaded chunks, throttled scans). Run from the project root, like the other file tests.
 */
class DbFilesTest {
    private static final Path RES = Path.of("src/main/resources");
    private static final Path ASSETS = RES.resolve("assets/anime_multiverse");
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path DB = SRC.resolve("dragonballs");

    @Test
    void everyBallHasATextureAndAModelThatSwitchesToStone() throws IOException {
        for (BallSet s : BallSet.values()) {
            for (int n = 1; n <= DbRules.MAX_COUNT; n++) {
                String id = s.itemId(n);
                assertTrue(Files.exists(ASSETS.resolve("textures/item/" + id + ".png")), id + " texture");
                assertTrue(Files.exists(ASSETS.resolve("textures/item/" + id + "_stone.png")), id + " stone texture");
                JsonObject live = JsonParser.parseString(Files.readString(ASSETS.resolve("models/item/" + id + ".json"))).getAsJsonObject();
                assertEquals("anime_multiverse:item/" + id, live.getAsJsonObject("textures").get("layer0").getAsString());
                assertEquals("anime_multiverse:item/" + id + "_stone", live.getAsJsonArray("overrides").get(0).getAsJsonObject().get("model").getAsString());
                assertEquals(1, live.getAsJsonArray("overrides").get(0).getAsJsonObject().getAsJsonObject("predicate").get("custom_model_data").getAsInt());
                assertTrue(Files.exists(ASSETS.resolve("models/item/" + id + "_stone.json")), id + " stone model");
            }
        }
    }

    @Test
    void radarShenronAndRecipeExist() throws IOException {
        for (String f : List.of("textures/item/dragon_radar.png", "models/item/dragon_radar.json", "geo/entity/dragonballs_shenron.geo.json", "animations/entity/dragonballs_shenron.animation.json",
                "textures/entity/dragonballs_shenron.png", "textures/entity/dragonballs_shenron_glow.png", "textures/entity/dragonballs_shenron_super.png", "textures/entity/dragonballs_shenron_super_glow.png"))
            assertTrue(Files.exists(ASSETS.resolve(f)), f);
        assertTrue(Files.readString(RES.resolve("data/anime_multiverse/recipe/dragon_radar.json")).contains("anime_multiverse:dragon_radar"));
        String anim = Files.readString(ASSETS.resolve("animations/entity/dragonballs_shenron.animation.json"));
        assertTrue(anim.contains("\"idle\""), "Shenron needs an idle clip");
        String geo = Files.readString(ASSETS.resolve("geo/entity/dragonballs_shenron.geo.json"));
        assertTrue(geo.contains("\"seg25\"") && geo.contains("\"head\"") && geo.contains("\"jaw\"") && geo.contains("\"eye_l\""), "serpentine chain + head + jaw + eyes");
    }

    @Test
    void langFileIsFlatAndHasEveryKeyTheCodeAsksFor() throws IOException {
        JsonObject lang = JsonParser.parseString(Files.readString(Path.of("tools/lang/dragonballs.json"))).getAsJsonObject();
        for (var e : JsonParser.parseString(Files.readString(Path.of("tools/lang/dbwish.json"))).getAsJsonObject().entrySet()) lang.add(e.getKey(), e.getValue());   // the overpowered wishes' fragment
        for (var e : lang.entrySet()) {
            assertTrue(e.getKey().startsWith("anime_multiverse.") || e.getKey().startsWith("item.anime_multiverse.") || e.getKey().startsWith("entity.anime_multiverse."), e.getKey());
            assertTrue(e.getValue().isJsonPrimitive(), "flat: " + e.getKey());
        }
        for (BallSet s : BallSet.values()) {
            for (int n = 1; n <= DbRules.MAX_COUNT; n++) assertTrue(lang.has("item.anime_multiverse." + s.itemId(n)), s.itemId(n));
            for (WishRules.Wish w : WishRules.forSet(s)) assertTrue(lang.has("anime_multiverse.dragonballs.wish." + w.id() + "." + s.id()), "wish hover " + w.id() + " " + s.id());
            assertTrue(lang.has("anime_multiverse.dragonballs.tip.set." + s.id()));
        }
        assertTrue(lang.has("item.anime_multiverse.dragon_radar") && lang.has("entity.anime_multiverse.shenron"));
        // every literal key passed to DbMsg.say/info/bad or used as a P + "key" in the package
        Pattern use = Pattern.compile("(?:say\\(\\s*\\w+,\\s*\"|info\\(\"|bad\\(\"|no\\(set,\\s*\"|yes\\(set,\\s*\"|P \\+ \"|dragonballs\\.)([a-z_.]+)\"");
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(DB)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String src = Files.readString(f);
                Matcher m = use.matcher(src);
                while (m.find()) {
                    String key = m.group(1);
                    if (key.endsWith(".")) continue;
                    if (!lang.has("anime_multiverse.dragonballs." + key) && !lang.has(key)) missing.add(f.getFileName() + " -> " + key);
                }
            }
        }
        // revive_none / skill_pick etc. are covered above; the self-test prefix "dragonballs." is not a lang key, so only report keys that look like messages
        missing.removeIf(s -> s.contains("DbSelfTests") || s.contains("DbBoot"));
        assertTrue(missing.isEmpty(), "lang keys used in code but missing from tools/lang/dragonballs.json: " + missing);
    }

    // ---------------------------------------------------------------- server safety and bounds, by reading the source
    private static List<Path> dbSources() throws IOException {
        try (Stream<Path> s = Files.walk(DB)) { return s.filter(p -> p.toString().endsWith(".java")).sorted().toList(); }
    }

    @Test
    void commonCodeNeverNamesAClientClass() throws IOException {
        for (Path f : dbSources()) {
            String src = Files.readString(f);
            assertFalse(src.contains("net.minecraft.client"), f + " names a client class");
            assertFalse(src.contains("com.mojang.blaze3d"), f + " names a client class");
            assertFalse(src.contains("com.newuniverse.nusmp.client"), f + " names a client class");
            assertFalse(src.contains("Minecraft.getInstance"), f + " uses Minecraft.getInstance");
        }
    }

    @Test
    void theServerNeverLoadsChunksOrScansTheWholeWorld() throws IOException {
        for (Path f : dbSources()) {
            if (f.getFileName().toString().equals("DbSelfTests.java") || f.getFileName().toString().equals("DbWishSelfTests.java")) continue;      // a self-test may load a chunk of its own scratch world
            String code = Files.readString(f).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
            assertFalse(code.contains(".getChunk("), f + " may load a chunk (use getChunkNow)");
            assertFalse(code.contains("getAllEntities") || code.contains("EntityTypeTest.forClass(Entity.class)"), f + " scans every entity");
            assertFalse(code.contains("loadAllChunks") || code.contains("forceLoad") || code.contains("setChunkForced"), f + " forces chunks");
            assertFalse(code.contains("Thread.sleep"), f.toString());
        }
        String mgr = Files.readString(DB.resolve("DbManager.java"));
        assertTrue(mgr.contains("getChunkNow"), "the pending-ball spawn only uses already loaded chunks");
        assertTrue(mgr.contains("spawned >= 4"), "the sweep has a spawn cap");
    }

    @Test
    void everyPacketIsClientBoundOptionalAndValidated() throws IOException {
        String net = Files.readString(DB.resolve("DbNet.java"));
        assertTrue(net.contains("playToClient"), "the sky packet is server -> client only");
        assertFalse(net.contains("playToServer"), "no packet is accepted from a client");
        assertTrue(net.contains(".optional()"), "a client without the mod must not be kicked");
        assertTrue(net.contains("valid("), "the received values are range checked");
        assertTrue(net.contains("sendToPlayersNear"), "sent to nearby players only");
    }

    @Test
    void gameplaySettingsAreServerConfigsAndLooksAreClientConfigs() throws IOException {
        assertTrue(Files.readString(DB.resolve("DbBoot.java")).contains("ModConfig.Type.SERVER"));
        assertTrue(Files.readString(SRC.resolve("client/dragonballs/DbClient.java")).contains("ModConfig.Type.CLIENT"));
    }

    @Test
    void theRitualIsPerPlayerAndKeyedByUuid() throws IOException {
        String chat = Files.readString(DB.resolve("DbChat.java"));
        assertTrue(chat.contains("UUID"), "the phrase window is per player UUID");
        assertTrue(chat.contains("ServerChatEvent"));
        assertTrue(chat.contains("length() >") || chat.contains("length()>"), "the chat line is length capped before it is worked on");
    }

    @Test
    void noHungerEffectIsEverApplied() throws IOException {
        for (Path f : dbSources()) {
            String code = Files.readString(f);
            assertFalse(code.contains("MobEffects.HUNGER"), f.toString());
            assertFalse(code.contains("causeFoodExhaustion"), f.toString());
            assertFalse(code.contains("getFoodData().setFoodLevel"), f.toString());
        }
    }
}
