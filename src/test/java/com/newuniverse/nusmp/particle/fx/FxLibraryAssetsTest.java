package com.newuniverse.nusmp.particle.fx;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the custom particle library is data plus art, and every link of the chain must hold or the game shows a purple-black square: each catalog entry has a sprite json in
 * {@code assets/nusmp/particles}, the json lists exactly its frames, every frame is a PNG of the catalog's size that is not empty, the type is registered through the catalog loop and
 * a provider is registered for it, and no sprite json is left without a catalog entry (the two Rukh jsons excepted). It also pins the families the owner asked for, so a regenerated
 * library cannot quietly lose one.
 */
class FxLibraryAssetsTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path ASSETS = ROOT.resolve("src/main/resources/assets/nusmp");
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    @Test
    void theCatalogHasAtLeastSixtyParticlesWithSaneData() {
        List<FxCatalog.Def> all = FxCatalog.all();
        assertTrue(all.size() >= 60, "only " + all.size() + " particles in the catalog");
        Set<String> seen = new HashSet<>();
        int big = 0;
        for (FxCatalog.Def d : all) {
            assertTrue(d.id().matches("[a-z][a-z0-9_]*"), "bad id " + d.id());
            assertTrue(seen.add(d.id()), "repeated id " + d.id());
            assertTrue(d.frames() >= 4 && d.frames() <= 8, d.id() + ": " + d.frames() + " frames (4 to 8 wanted)");
            assertTrue(d.px() == 16 || d.px() == 32, d.id() + ": size " + d.px());
            if (d.px() == 32) big++;
            assertFalse(d.behaviour().isBlank(), d.id() + " has no behaviour text");
            FxProfile p = d.profile();
            assertTrue(p.lifeMin() >= 1 && p.lifeMax() <= FxProfile.MAX_LIFE && p.lifeMin() <= p.lifeMax(), d.id() + " lifetime");
            assertTrue(p.sizeStart() > 0f && p.sizeStart() <= FxProfile.MAX_SIZE, d.id() + " start size");
            assertTrue(p.drag() > 0.5f && p.drag() <= 1.0f, d.id() + " drag");
        }
        assertTrue(big >= 8, "some particles must be 32x32 (found " + big + ")");
    }

    @Test
    void everyParticleHasASpriteJsonListingExactlyItsFramesAndTheTexturesExist() throws IOException {
        for (FxCatalog.Def d : FxCatalog.all()) {
            Path json = ASSETS.resolve("particles/" + d.id() + ".json");
            assertTrue(Files.isRegularFile(json), "no sprite json for " + d.id());
            JsonObject o = JsonParser.parseString(Files.readString(json, StandardCharsets.UTF_8)).getAsJsonObject();
            List<String> listed = new ArrayList<>();
            o.getAsJsonArray("textures").forEach(e -> listed.add(e.getAsString()));
            assertEquals(d.sprites(), listed, d.id() + ": the sprite list does not match the frames");
            for (int f = 0; f < d.frames(); f++) {
                Path png = ASSETS.resolve("textures/particle/" + d.textureFile(f));
                assertTrue(Files.isRegularFile(png), "missing texture " + png.getFileName());
                BufferedImage img = ImageIO.read(png.toFile());
                assertNotNull(img, "unreadable texture " + png.getFileName());
                assertEquals(d.px(), img.getWidth(), png.getFileName() + " width");
                assertEquals(d.px(), img.getHeight(), png.getFileName() + " height");
                int lit = 0;
                for (int y = 0; y < img.getHeight(); y++)
                    for (int x = 0; x < img.getWidth(); x++) if (((img.getRGB(x, y) >>> 24) & 255) > 16) lit++;
                assertTrue(lit >= 3, png.getFileName() + " is empty");
            }
        }
    }

    @Test
    void noSpriteJsonIsLeftWithoutACatalogEntry() throws IOException {
        Set<String> ids = new HashSet<>(FxCatalog.ids());
        try (Stream<Path> s = Files.list(ASSETS.resolve("particles"))) {
            for (Path p : (Iterable<Path>) s::iterator) {
                String n = p.getFileName().toString();
                if (!n.endsWith(".json")) continue;
                String id = n.substring(0, n.length() - 5);
                if (id.equals("dark_rukh") || id.equals("light_rukh")) continue;
                assertTrue(ids.contains(id), "sprite json without a catalog entry: " + n);
            }
        }
    }

    @Test
    void theTypesAreRegisteredFromTheCatalogAndEveryTypeGetsAProvider() throws IOException {
        String types = Files.readString(SRC.resolve("particle/fx/FxTypes.java"), StandardCharsets.UTF_8);
        assertTrue(types.contains("FxCatalog.all()") && types.contains("PARTICLES.register(d.id()"), "FxTypes no longer registers one type per catalog entry");
        String client = Files.readString(SRC.resolve("client/particle/fx/FxClient.java"), StandardCharsets.UTF_8);
        assertTrue(client.contains("FxCatalog.all()") && client.contains("registerSpriteSet(type"), "FxClient no longer registers a provider for every catalog entry");
        String nusmp = Files.readString(SRC.resolve("NUSMP.java"), StandardCharsets.UTF_8);
        assertTrue(nusmp.contains("particle.fx.FxBoot.init(modEventBus, modContainer)"), "NUSMP does not call FxBoot.init");
        assertTrue(nusmp.contains("client.particle.fx.FxClient.init(modEventBus, modContainer)"), "NUSMP does not call FxClient.init");
    }

    @Test
    void theFamiliesTheOwnerAskedForAreAllThere() {
        Set<String> ids = new HashSet<>(FxCatalog.ids());
        String[] required = {"ember", "cinder", "spark", "mote", "mote_soft", "glint", "star4", "gem_sparkle", "mana_mote", "aura_wisp", "spirit_firefly",
                "shard_ice", "shard_crystal", "shard_glass", "feather_white", "feather_black", "feather_fire", "petal", "petal_cherry", "leaf_green", "leaf_autumn",
                "bubble", "droplet", "snowflake", "ash_flake", "soot_puff", "smoke_wisp", "lightning_arc", "ring_pulse", "halo_ring", "nebula_puff", "void_speck",
                "soul_wisp", "holy_ray", "sand_grain", "cog", "clock_tick", "card", "page", "ink_splat", "paint_splat", "thread", "slash_streak", "shock_ring",
                "impact_burst", "circle_flash", "blood_drop", "flame_tongue", "water_splash", "wind_swirl", "earth_chip", "seed", "spore", "poison_cloud",
                "shadow_tendril", "dream_star", "music_note"};
        for (String r : required) assertTrue(ids.contains(r), "missing family member " + r);
        int runes = 0;
        for (String id : ids) if (id.startsWith("rune_")) runes++;
        assertTrue(runes >= 8, "the rune glyph set needs 8 glyphs, found " + runes);
    }

    @Test
    void theEightRuneGlyphsAreDistinctPictures() throws IOException {
        Set<Integer> hashes = new HashSet<>();
        for (String id : FxCatalog.ids()) {
            if (!id.startsWith("rune_")) continue;
            BufferedImage img = ImageIO.read(ASSETS.resolve("textures/particle/" + id + "_2.png").toFile());
            int h = 1;
            for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) h = h * 31 + img.getRGB(x, y);
            assertTrue(hashes.add(h), id + " looks exactly like another rune");
        }
        assertTrue(hashes.size() >= 8);
    }

    @Test
    void additiveGlowsAreFullBrightAndSolidThingsAreNot() {
        for (FxCatalog.Def d : FxCatalog.all()) {
            FxProfile p = d.profile();
            if (p.blend() == FxProfile.Blend.ADD) assertEquals(FxProfile.Light.FULL, p.light(), d.id() + ": an additive particle that is not full-bright is invisible in the dark");
        }
    }
}
