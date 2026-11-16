package com.newuniverse.nusmp.dungeon.rework;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dungeon-boss rework (docs/dungeon_bosses_rework.md): the source rules of rules 9 and 11 for the new package, and the wiring that keeps the old behaviour reachable. The new classes load no client
 * class, keep no static mutable state, spawn no entity, register no sound event, read no world per tick beyond the bounded queries; the entities gate on the settings; the self-test is registered and
 * in the smoke list in the right place; every translation key the brains use is in the lang fragment; the settings default to on.
 */
class DungeonBossSourceScanTest {
    private static final String SRC = "src/main/java/com/newuniverse/nusmp/";
    private static final String PKG = SRC + "dungeon/rework/";

    private static String read(String path) throws IOException { return Files.readString(Path.of(path)); }

    private static List<Path> reworkFiles() throws IOException {
        try (Stream<Path> s = Files.list(Path.of(PKG))) {
            return s.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
    }

    @Test
    void theNewPackageHasNoClientClass() throws IOException {
        for (Path p : reworkFiles()) {
            String src = Files.readString(p);
            assertFalse(src.contains("net.minecraft.client"), p + " touches the client");
            assertFalse(src.contains("com.mojang.blaze3d"), p + " touches blaze3d");
            assertFalse(src.contains("com.newuniverse.nusmp.client"), p + " touches the mod's client package");
            assertFalse(src.contains("Minecraft.getInstance"), p + " reads the client singleton");
        }
    }

    @Test
    void theNewPackageKeepsNoStaticMutableStateAndSpawnsNothing() throws IOException {
        Pattern mutableStatic = Pattern.compile("(?m)^\\s*(private |public |protected )?static (?!final)[A-Za-z<>\\[\\], ?]+ \\w+\\s*(=|;)");
        for (Path p : reworkFiles()) {
            String src = Files.readString(p);
            Matcher m = mutableStatic.matcher(src);
            assertFalse(m.find(), p + " has static mutable state");
            assertFalse(src.contains("addFreshEntity"), p + " spawns an entity");
            assertFalse(src.contains("DeferredRegister"), p + " registers something");
            assertFalse(src.contains("SoundEvent.createVariableRangeEvent"), p + " makes a sound event without an .ogg");
        }
    }

    @Test
    void everyQueryIsBoundedAndGoesThroughTheHelperOrACap() throws IOException {
        String strikes = read(PKG + "DungeonStrikes.java");
        assertTrue(strikes.contains("MAX_VICTIMS"), "the player query is capped");
        String warden = read(PKG + "WardenBrain.java");
        assertEquals(0, count(warden, "getEntitiesOfClass"), "the Warden's brain asks the helper for players only");
        String beast = read(PKG + "BeastBrain.java");
        assertTrue(beast.contains("CALL_CAP"), "the pack calls are capped");
        assertEquals(2, count(beast, "getEntitiesOfClass"), "only the two capped pack calls query entities directly");
        // every damage goes through the boss damage law, never a bare hurt()
        for (String name : List.of("WardenBrain.java", "BeastBrain.java")) {
            String src = read(PKG + name);
            assertFalse(Pattern.compile("\\.hurt\\(").matcher(src).find(), name + " must hit through DungeonStrikes.strike (ZogratisDamage)");
            assertFalse(src.contains("setHealth("), name + " must not touch health");
        }
        assertTrue(strikes.contains("ZogratisDamage.hit"), "damage goes through the boss damage law");
        assertTrue(strikes.contains("BalanceLaw.beginControl") && strikes.contains("controlTicks"), "control goes through the control law");
    }

    @Test
    void theEntitiesGateOnTheSettingsAndKeepTheOldFight() throws IOException {
        String warden = read(SRC + "labyrinth/seal/FloorWardenEntity.java");
        assertTrue(warden.contains("BossRulesConfig.dungeonWarden()"));
        assertTrue(warden.contains("getClass() == FloorWardenEntity.class"), "the Guardian keeps its own fight");
        assertTrue(warden.contains("private void groundSlam") && warden.contains("private boolean shadowStep"), "the old moves are kept");
        assertTrue(warden.contains("} else if (target != null && target.isAlive()) {"), "the old chooser stays behind the setting");
        assertTrue(warden.contains("wardenBudget.tick") && warden.contains("wardenBudget.hurt") && warden.contains("wardenBudget.setHealth"), "the three budget hooks");
        assertTrue(warden.contains("SHADOW_KEY"), "the drop is untouched");
        for (String c : List.of("DirewolfEntity", "BasiliskEntity", "LivingArmorEntity")) {
            String src = read(SRC + "dungeon/" + c + ".java");
            assertTrue(src.contains("BossRulesConfig.dungeonCreatures()"), c);
            assertTrue(src.contains("getClass() != " + c + ".class"), c + " leaves its subclasses alone");
        }
        String creature = read(SRC + "dungeon/DungeonCreature.java");
        assertTrue(creature.contains("protected void reworkTick") && creature.contains("protected boolean busy()"));
    }

    @Test
    void theSettingsExistAndDefaultToOn() throws IOException {
        String cfg = read(SRC + "boss/rules/BossRulesConfig.java");
        assertTrue(cfg.contains("b.push(\"dungeonBosses\")"));
        assertTrue(cfg.contains(".define(\"rework\", true)"));
        assertTrue(cfg.contains(".define(\"warden\", true)"));
        assertTrue(cfg.contains(".define(\"creatures\", true)"));
    }

    @Test
    void theSelfTestIsRegisteredAndInTheSmokeListBeforeScaling() throws IOException {
        assertTrue(read(SRC + "boss/BossHooks.java").contains("DungeonBossSelfTests.register()"));
        assertTrue(read(PKG + "DungeonBossSelfTests.java").contains("SelfTests.register(\"dboss\""));
        String smoke = read(".github/workflows/smoke.yml");
        assertTrue(smoke.contains(",dboss,scaling,boss.rework1,boss.rework3,boss.stats,tier\""), "dboss sits directly before scaling and the tail is unchanged");
        assertEquals(1, count(smoke, ",dboss,"));
    }

    @Test
    void everyTranslationKeyTheBrainsUseIsInTheLangFragment() throws IOException {
        String frag = read("tools/lang/dungeon_bosses.json");
        String warden = read(PKG + "WardenBrain.java");
        assertTrue(warden.contains("\"message.anime_multiverse.dboss.warden.\" + key"));
        Matcher m = Pattern.compile("announce\\(w, sl, (?:phase >= 3 \\? )?\"(\\w+)\"(?: : \"(\\w+)\")?\\)").matcher(warden);
        int found = 0;
        while (m.find()) {
            for (int g = 1; g <= 2; g++) {
                if (m.group(g) == null) continue;
                found++;
                assertTrue(frag.contains("\"message.anime_multiverse.dboss.warden." + m.group(g) + "\""), "lang fragment lacks " + m.group(g));
            }
        }
        assertTrue(found >= 4, "p2, p3, toll and enrage are announced: " + found);
    }

    @Test
    void noNewSoundEventIsRegistered() throws IOException {
        // the rework recombines vanilla sounds and the mod's own family voices: a new SoundEvent would need an .ogg
        for (Path p : reworkFiles()) {
            String src = Files.readString(p);
            assertFalse(src.contains("ModSounds.register"), p + " registers a sound");
            assertFalse(src.contains("createVariableRangeEvent"), p + " makes a sound event");
        }
    }

    private static int count(String s, String what) {
        int n = 0;
        for (int i = s.indexOf(what); i >= 0; i = s.indexOf(what, i + what.length())) n++;
        return n;
    }
}
