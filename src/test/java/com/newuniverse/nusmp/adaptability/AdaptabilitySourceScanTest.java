package com.newuniverse.nusmp.adaptability;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: Adaptability (3.70.0) runs on every player hit, so its server rules (rule 9) and its bounds (rule 11) are pinned by reading the repo: no client class, no static player, no entity spawn or world scan, wired in. */
class AdaptabilitySourceScanTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final String MAIN = "src/main/java/com/newuniverse/nusmp/";

    private static String read(String rel) throws IOException { return Files.readString(ROOT.resolve(rel), StandardCharsets.UTF_8); }

    @Test
    void nothingInThePackageTouchesClientCodeOrAStaticPlayer() throws IOException {
        try (Stream<Path> w = Files.list(ROOT.resolve(MAIN + "adaptability"))) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String t = Files.readString(p, StandardCharsets.UTF_8);
                String n = p.getFileName().toString();
                assertFalse(t.contains("net.minecraft.client") || t.contains("com.newuniverse.nusmp.client") || t.contains("com.mojang.blaze3d") || t.contains("Minecraft.getInstance"), n + ": client code");
                assertFalse(t.contains("static ServerPlayer") || t.contains("static Player "), n + ": a static current player");
                assertFalse(t.contains("getEntitiesOfClass") || t.contains("getEntities(") || t.contains("spawn") || t.contains("addFreshEntity") || t.contains("getChunk"), n + ": no world scan, no spawn");
            }
        }
    }

    @Test
    void theHooksOnlyActForAToggledSkillHolderAndTheRecordIsBounded() throws IOException {
        String e = read(MAIN + "adaptability/AdaptabilityEvents.java");
        assertTrue(e.contains("AdaptabilitySkill.active(p)"), "both hooks go through the toggled-holder check");
        assertTrue(e.contains("instanceof ServerPlayer"), "server players only");
        assertTrue(e.contains("AdaptabilityRules.MAX_TYPES"), "the record read is capped");
        String r = read(MAIN + "adaptability/AdaptabilityRules.java");
        assertTrue(r.contains("MAX_TYPES = 16"), "sixteen kinds at most");
        String s = read(MAIN + "adaptability/AdaptabilitySkill.java");
        assertTrue(s.contains("AdaptabilityConfig.maxSkills()"), "the mastery pass is capped per call");
        assertFalse(e.contains("markDirty"), "a hit never sends a sync packet (the tick does, every five seconds)");
    }

    @Test
    void tensuraIsReachedByNameOnly() throws IOException {
        for (String f : new String[] {"AdaptabilityTensura", "AdaptabilityEvents", "AdaptabilitySkill"}) {
            String t = read(MAIN + "adaptability/" + f + ".java");
            assertFalse(t.contains("import io.github.manasmods.tensura.damage") || t.contains("import io.github.manasmods.tensura.event") || t.contains("import io.github.manasmods.tensura.storage"), f + ": links a Tensura class it only needs by name");
        }
    }

    @Test
    void itIsWiredIn() throws IOException {
        assertTrue(read(MAIN + "NUSMP.java").contains("AdaptabilityBoot.init(modEventBus, modContainer)"), "boot");
        assertTrue(read(MAIN + "skill/NUSkills.java").contains("SKILLS.register(\"adaptability\""), "registered");
        assertTrue(read(MAIN + "multiverse/MultiverseCommands.java").contains("AdaptabilityCommand.node()"), "command");
        assertTrue(read(MAIN + "adaptability/AdaptabilityBoot.java").contains("\"nusmp-adaptability-server.toml\"") && read(MAIN + "adaptability/AdaptabilityBoot.java").contains("ModConfig.Type.SERVER"), "a SERVER config");
        assertTrue(Files.exists(ROOT.resolve("src/main/resources/assets/nusmp/textures/skill/adaptability.png")), "icon");
        assertTrue(read("src/main/resources/assets/nusmp/lang/en_us.json").contains("\"nusmp.skill.adaptability\""), "lang");
    }
}
