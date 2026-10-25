package com.newuniverse.nusmp.corpse;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why (owner report 3.7.0, "Rades's Number Summons are just magic circles", docs/magic/rades_cast_fix.md): the game cannot be played here, so the shape that made the numbered undead show up is read from
 * the sources: the new looks are registered INSIDE the dungeon kit's own listener (so they come after its puppets), the old LOWEST listener and the kit's own registration stay, the renderer never draws
 * nothing without the owner's switch, a refused cast says why, every undead of Soul Corpse Magic is sun-safe, and the real-grimoire self-test drives all five numbered pages and checks the body 40 ticks on.
 */
class RadesCastSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String code(String rel) throws IOException {
        return Files.readString(SRC.resolve(rel), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }

    @Test
    void theNewLooksAreRegisteredInsideTheKitsListenerAndTheOldListenerStays() throws IOException {
        String main = code("NUSMP.java");
        int init = main.indexOf("PlayerModelRegistry.init(modEventBus)");
        int extend = main.indexOf("PlayerModelRegistry.extend(com.newuniverse.nusmp.client.corpse.CorpseClient::registerLooks)");
        assertTrue(init > 0, "the player-model kit is no longer initialised");
        assertTrue(extend > 0, "CorpseClient.registerLooks is not handed to PlayerModelRegistry.extend: the kit's puppets of Jimmy, Alfredo and Carl would win again");
        assertTrue(main.contains("EventPriority.LOWEST, com.newuniverse.nusmp.client.corpse.CorpseClient::registerLooks"), "the old LOWEST listener was removed (rule 2: keep the old)");
        String registry = code("client/playermodel/PlayerModelRegistry.java");
        assertTrue(registry.contains("new ArrayList<>(EXTENSIONS)"), "the registry no longer runs extensions");
    }

    @Test
    void theExtensionsRunAfterTheDungeonRegistrations() throws IOException {
        String registry = code("client/playermodel/PlayerModelRegistry.java");
        int dungeon = registry.indexOf("DungeonRenderers.register(e)");
        int ext = registry.indexOf("new ArrayList<>(EXTENSIONS)");
        assertTrue(dungeon > 0 && ext > dungeon, "the extension consumers must run after the dungeon registrations in the kit's listener");
    }

    @Test
    void theNewLookRegistersAllThreePuppetsUnlessClassicLooks() throws IOException {
        String c = code("client/corpse/CorpseClient.java");
        int at = c.indexOf("static void registerLooks");
        assertTrue(at > 0);
        String body = c.substring(at, c.indexOf("}", c.indexOf("RadesUndeadRenderer::new", at + 200)) + 1);
        assertTrue(body.contains("classicLooks()"), "corpse.classicLooks no longer restores the old looks");
        for (String k : new String[]{"JIMMY", "ALFREDO", "CARL"}) assertTrue(c.contains("CorpseEntities." + k + ".get(), RadesUndeadRenderer::new"), k + " is not given the new look");
        assertTrue(c.contains("CorpseEntities.DAVID.get(), RadesUndeadRenderer::new") && c.contains("CorpseEntities.MICHAEL_CAESAL.get(), RadesUndeadRenderer::new"));
    }

    @Test
    void theRendererNeverDrawsNothingWithoutTheOwnersSwitch() throws IOException {
        String r = code("client/corpse/RadesUndeadRenderer.java");
        assertTrue(r.contains("CorpseLookRules.draw("), "the renderer does not ask the look rule what to draw");
        assertTrue(r.contains("CorpseClientConfig.fallbackLook()"));
        assertTrue(r.contains("Draw.FALLBACK") && r.contains("standIn("), "the zombie stand-in is gone");
        String cfg = code("client/corpse/CorpseClientConfig.java");
        assertTrue(cfg.contains("\"fallbackLook\", true"), "corpse.fallbackLook must default to true");
    }

    @Test
    void aRefusedNamedCastSaysWhy() throws IOException {
        String s = code("corpse/SoulCorpseSpells.java");
        assertTrue(s.contains("static boolean named(GrimoireBook"), "the named() signature changed (SoulCorpsePagesSourceScanTest reads it)");
        assertTrue(s.contains("RadesCastDiagnosis.Refusal.SWITCHED_OFF") && s.contains("RadesCastDiagnosis.Refusal.ELITE_STANDS") && s.contains("summonNamedChecked"));
        String c = code("corpse/Corpses.java");
        for (String why : new String[]{"SERVER_CAP", "NO_ENTITY", "WORLD_REFUSED"}) assertTrue(c.contains("Refusal." + why), "no log line for " + why);
        assertTrue(c.contains("CorpseConfig.diagnose()") && c.contains("RadesCastDiagnosis.mayLog("), "the diagnosis is not behind the setting and the rate limit");
        assertTrue(c.contains("static NamedCorpseEntity summonNamed(") || c.contains("NamedCorpseEntity summonNamed("), "summonNamed (the old entry point) was removed");
    }

    @Test
    void everyUndeadOfSoulCorpseMagicIsSunSafe() throws IOException {
        assertFalse(SoulZombieRules.SUN_BURNS, "soul zombies must not burn in daylight");
        String named = code("corpse/NamedCorpseEntity.java");
        assertTrue(named.matches("(?s).*boolean isSunBurnTick\\(\\)\\s*\\{\\s*return false;\\s*\\}.*"), "NamedCorpseEntity.isSunBurnTick must answer false");
        assertTrue(named.matches("(?s).*boolean burnsInDaylight\\(\\)\\s*\\{\\s*return false;\\s*\\}.*"), "NamedCorpseEntity.burnsInDaylight must answer false");
        String zombie = code("corpse/SoulZombieEntity.java");
        assertTrue(zombie.contains("isSunSensitive() { return SoulZombieRules.SUN_BURNS; }"));
        assertTrue(zombie.matches("(?s).*boolean isSunBurnTick\\(\\)\\s*\\{\\s*return false;\\s*\\}.*"));
        String corpses = code("corpse/Corpses.java");
        assertTrue(corpses.contains("MobEffects.FIRE_RESISTANCE"), "the raised (vanilla) undead lost their fire resistance");
        assertTrue(corpses.contains("CorpseConfig.noSunBurn()"), "the sweep no longer re-extinguishes raised undead behind the corpse.noSunBurn setting");
        String cfg = code("corpse/CorpseConfig.java");
        assertTrue(cfg.contains("\"noSunBurn\"") && cfg.contains("define(\"noSunBurn\", true)") && cfg.contains("define(\"diagnose\", true)"), "diagnose / noSunBurn must default to true");
    }

    @Test
    void theSelfTestDrivesTheRealGrimoireForAllFiveNumberedPages() throws IOException {
        String t = code("corpse/SoulCorpseCastSelfTests.java");
        assertTrue(t.contains("numberedStand(") && t.contains("STAND_TICKS = 40") && t.contains("Corpses.sweepAll("), "the self-test no longer checks the body 40 ticks on");
        for (String id : new String[]{"JIMMY", "DAVID", "ALFREDO", "CARL", "MICHAEL_CAESAL"}) assertTrue(t.contains("NamedCorpses." + id), "the self-test does not cast " + id);
        assertTrue(t.contains("castPage") || t.contains("onRelease") || t.contains("usable"), "the self-test does not go through the grimoire's page cast");
        assertTrue(t.contains("isInvisible()") && t.contains("isAlive()"), "the self-test does not check that the body is alive and visible");
        String rades = code("corpse/RadesUndeadSelfTests.java");
        assertTrue(rades.contains("e.burnsInDaylight() || e.isSunBurnTick()"), "the numbered undead sun rule is not in the self-test");
    }
}
