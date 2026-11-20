package com.newuniverse.nusmp.meteor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads the repo: every wired ability has a caller that asks for its spec, every meteor-like name that was left alone has a reason, every style has its textures and the geo models exist,
 * and the shared meteor code stays server-safe and cheap (no client class, no block change, no fire, no per-tick world scan).
 */
class MeteorWiringTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Path ASSETS = Path.of("src/main/resources/assets/anime_multiverse");

    private static String read(Path p) throws IOException { return Files.readString(p); }

    @Test
    void everyWiredAbilityHasACallerThatDropsAMeteor() throws IOException {
        for (MeteorWiring.Entry e : MeteorWiring.WIRED.values()) {
            Path f = SRC.resolve(e.file());
            assertTrue(Files.exists(f), e.id() + ": " + f + " is missing");
            String s = read(f);
            assertTrue(s.contains("MeteorWiring.spec(\"" + e.id() + "\")"), e.id() + " is not asked for in " + e.file());
            assertTrue(s.contains("MeteorFall."), e.id() + " never calls MeteorFall in " + e.file());
        }
    }

    @Test
    void everyLeftAloneNameHasAReasonAndIsNotAlsoWired() {
        assertTrue(MeteorWiring.LEFT_ALONE.size() >= 15);
        for (var e : MeteorWiring.LEFT_ALONE.entrySet()) {
            assertTrue(e.getValue().trim().length() > 20, e.getKey() + " has no reason");
            assertFalse(MeteorWiring.WIRED.containsKey(e.getKey()), e.getKey() + " is both wired and left alone");
        }
    }

    @Test
    void stylesHaveTexturesAndModelsExist() {
        for (MeteorStyle s : MeteorStyle.values()) {
            for (String t : new String[]{"meteor_rock_" + s.key + ".png", "meteor_rock_" + s.key + "_glow.png", "meteor_tail_" + s.key + ".png"})
                assertTrue(Files.exists(ASSETS.resolve("textures/entity/" + t)), t + " is missing");
        }
        assertTrue(Files.exists(ASSETS.resolve("geo/entity/meteor_rock.geo.json")));
        assertTrue(Files.exists(ASSETS.resolve("geo/entity/meteor_tail.geo.json")));
    }

    @Test
    void theSharedCodeIsServerSafeAndCheap() throws IOException {
        try (var files = Files.list(SRC.resolve("meteor"))) {
            for (Path p : files.filter(x -> x.toString().endsWith(".java")).toList()) {
                String s = read(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
                String n = p.getFileName().toString();
                assertFalse(s.contains("net.minecraft.client") || s.contains("com.mojang.blaze3d") || s.contains("Minecraft.getInstance"), n + " touches a client class");
                assertFalse(s.contains("setBlock(") || s.contains("destroyBlock(") || s.contains("Level.ExplosionInteraction") || s.contains("LightningBolt"), n + " changes blocks or lights fires");
                assertFalse(s.contains("getPlayer()") && s.contains("static Player"), n + " keeps a static player");
            }
        }
        String entity = read(SRC.resolve("meteor/MeteorEntity.java"));
        assertFalse(entity.contains("getEntitiesOfClass") || entity.contains("getEntities("), "the meteor scans the world every tick");
        assertTrue(entity.contains("PerfBudgets.clientParticles"), "the trail is not on the client budget");
        String fall = read(SRC.resolve("meteor/MeteorFall.java"));
        assertTrue(fall.contains("MeteorRules.MAX_TARGETS") && fall.contains("canLaunch("), "the landing or the cap lost its bound");
        for (String h : Set.of("onLogout", "onDimension", "onDeath", "onServerStopped")) assertTrue(fall.contains(h), "the cleanup " + h + " is gone");
    }
}
