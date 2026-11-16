package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.multiverse.Race;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the real Tensura Saiyan race (anime_multiverse:saiyan) is linked to the Saiyan flag by pure decisions, and its numbers are the stage 1 buff of {@link SaiyanRaceRules}. Pure (no game); the wiring is pinned
 * by reading the source, because nothing here can run Tensura.
 */
class SaiyanTensuraRaceTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final String DIR = "src/main/java/com/newuniverse/nusmp/saiyan/";

    private static String read(String rel) throws IOException { return Files.readString(ROOT.resolve(rel), StandardCharsets.UTF_8); }

    @Test
    void theRaceNumbersAreTheStageOneBuff() {
        SaiyanRaceRules.Bonus one = SaiyanRaceRules.bonus(SaiyanRaceRules.SAIYAN, 1.0);
        assertEquals(one.health(), SaiyanRaceRules.raceHealth(), 1e-9);
        assertEquals(one.attack(), SaiyanRaceRules.raceAttack(), 1e-9);
        assertEquals(one.knockback(), SaiyanRaceRules.raceKnockback(), 1e-9);
        assertEquals(one.speed() * 0.1, SaiyanRaceRules.raceMovement(), 1e-9);
        assertTrue(SaiyanRaceRules.RACE_MIN_AURA < SaiyanRaceRules.RACE_MAX_AURA && SaiyanRaceRules.RACE_MIN_MAGICULE < SaiyanRaceRules.RACE_MAX_MAGICULE);
        assertEquals("anime_multiverse:saiyan", SaiyanRaceRules.RACE_ID);
    }

    @Test
    void theFlagFollowsTheRaceAndTheRaceFollowsTheFlagOnlyWhenTheSettingIsOn() {
        for (boolean flag : new boolean[]{false, true})
            for (boolean holds : new boolean[]{false, true}) {
                assertEquals(SaiyanRaceRules.Link.NONE, SaiyanRaceRules.afterFlag(false, flag, holds));
                assertEquals(SaiyanRaceRules.Link.NONE, SaiyanRaceRules.atLogin(false, flag, holds));
                assertEquals(SaiyanRaceRules.Link.NONE, SaiyanRaceRules.onSetRace(false, flag, holds, !holds));
            }
        assertEquals(SaiyanRaceRules.Link.GIVE_RACE, SaiyanRaceRules.afterFlag(true, true, false));
        assertEquals(SaiyanRaceRules.Link.TAKE_RACE, SaiyanRaceRules.afterFlag(true, false, true));
        assertEquals(SaiyanRaceRules.Link.NONE, SaiyanRaceRules.afterFlag(true, true, true));
        assertEquals(SaiyanRaceRules.Link.NONE, SaiyanRaceRules.afterFlag(true, false, false));
        assertEquals(SaiyanRaceRules.Link.GIVE_RACE, SaiyanRaceRules.atLogin(true, true, false));
        assertEquals(SaiyanRaceRules.Link.SET_FLAG, SaiyanRaceRules.atLogin(true, false, true));
        assertEquals(SaiyanRaceRules.Link.SET_FLAG, SaiyanRaceRules.onSetRace(true, false, false, true));      // picked in Tensura's menu
        assertEquals(SaiyanRaceRules.Link.CLEAR_FLAG, SaiyanRaceRules.onSetRace(true, true, true, false));     // taken out of the race by Tensura
        assertEquals(SaiyanRaceRules.Link.NONE, SaiyanRaceRules.onSetRace(true, true, false, false));          // any other race change: the flag is left alone
        assertEquals(SaiyanRaceRules.Link.NONE, SaiyanRaceRules.onSetRace(true, true, false, true));
    }

    @Test
    void theMenuListGetsTheSaiyanOnceAndIsNeverShrunk() {
        List<String> base = List.of("tensura:human", "tensura:elf");
        List<String> now = SaiyanRaceRules.withMenuEntry(base, SaiyanRaceRules.RACE_ID);
        assertEquals(List.of("tensura:human", "tensura:elf", "anime_multiverse:saiyan"), now);
        assertEquals(List.of("tensura:human", "tensura:elf"), base);
        assertSame(now, SaiyanRaceRules.withMenuEntry(now, SaiyanRaceRules.RACE_ID));
        assertEquals(List.of("anime_multiverse:saiyan"), SaiyanRaceRules.withMenuEntry(null, SaiyanRaceRules.RACE_ID));
        assertEquals(new ArrayList<>(List.of("a", "anime_multiverse:saiyan")), SaiyanRaceRules.withMenuEntry(List.of("a"), SaiyanRaceRules.RACE_ID));
    }

    @Test
    void theStageBuffIsNotCountedTwiceWithTheRealRace() {
        SaiyanRaceRules.Bonus one = SaiyanRaceRules.bonus(SaiyanRaceRules.SAIYAN, 1.0);
        SaiyanRaceRules.Bonus zero = SaiyanRaceRules.minus(one, one);
        assertEquals(0.0, zero.attack() + zero.speed() + zero.health() + zero.armor() + zero.knockback(), 1e-12);
        SaiyanRaceRules.Bonus four = SaiyanRaceRules.bonus(SaiyanRaceRules.AUTONOMOUS, 1.0);
        SaiyanRaceRules.Bonus rest = SaiyanRaceRules.minus(four, one);
        assertEquals(four.health() - one.health(), rest.health(), 1e-9);
        SaiyanRaceRules.Bonus none = SaiyanRaceRules.minus(SaiyanRaceRules.bonus(SaiyanRaceRules.SAIYAN, 0.0), one);
        assertEquals(0.0, none.health(), 1e-12);          // a scale of 0 never goes below zero
    }

    @Test
    void aSaiyanIsAnUnknownFamilyToTheMultiverseRaceBuffsSoItFallsBackToHuman() {
        assertNull(Race.ofTensuraRace("saiyan"));          // MultiverseProfile.race then reads Human; no exhaustive switch gets a new constant
    }

    @Test
    void theRaceIsRegisteredAlwaysAndLinkedBehindTwoServerSettingsThatAreOnByDefault() throws IOException {
        String races = read(DIR + "SaiyanRaces.java");
        assertTrue(races.contains("DeferredRegister.create(RaceAPI.getRaceRegistryKey(), NUSMP.MODID)") && races.contains("RACES.register(\"saiyan\""), "registered in ManasCore's race registry like the skills are");
        assertFalse(Pattern.compile("import io\\.github\\.manasmods\\.tensura").matcher(races).find(), "the holder names no Tensura class, so the constructor does not load Tensura's races early");
        String race = read(DIR + "SaiyanTensuraRace.java");
        assertTrue(race.contains("extends DefaultRace") && race.contains("applyDefaultAttributeModifiers()") && race.contains("getDefaultConfig()"));
        String cfg = read(DIR + "SaiyanPhaseConfig.java");
        assertTrue(cfg.contains(".define(\"tensuraRace\", true)") && cfg.contains(".define(\"listInRaceMenu\", true)"), "on by default");
        assertFalse(cfg.contains("ModConfig.Type.CLIENT"));
        String boot = read(DIR + "SaiyanBoot.java");
        assertTrue(boot.contains("SaiyanRaces.RACES.register(modBus)") && boot.contains("RaceEvents.SET_RACE.register(SaiyanRaceLink::onSetRace)") && boot.contains("SaiyanRaceSelfTests.register()"));
        String mgr = read(DIR + "SaiyanManager.java");
        assertTrue(mgr.contains("SaiyanRaceLink.afterFlag(p, true)") && mgr.contains("SaiyanRaceLink.afterFlag(p, false)"), "setSaiyan keeps the race in step");
        String ev = read(DIR + "SaiyanEvents.java");
        assertTrue(ev.contains("SaiyanRaceLink.onLogin(p)") && ev.contains("SaiyanRaceLink.listInMenu()"));
        String link = read(DIR + "SaiyanRaceLink.java");
        assertTrue(link.contains("linking") && link.contains("EventResult.pass()") && !link.contains("interruptFalse") && !link.contains("EventResult.interrupt"), "the SET_RACE listener never cancels a race change");
        assertTrue(link.contains("if (!enabled()") || link.contains("|| !enabled()"), "every entry point checks the setting");
        assertFalse(Pattern.compile("net\\.minecraft\\.client\\.|com\\.mojang\\.blaze3d\\.|com\\.newuniverse\\.nusmp\\.client\\.|Minecraft\\.getInstance\\(").matcher(link + race + races).find());
        String lang = read("tools/lang/saiyan_race.json");
        assertTrue(lang.contains("\"anime_multiverse.race.saiyan\"") && lang.contains("\"anime_multiverse.race.saiyan.description\""), "the race name and description keys ManasCore reads (namespace.race.path)");
    }
}
