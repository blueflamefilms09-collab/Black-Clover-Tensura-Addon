package com.newuniverse.nusmp.skill;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the Summoner rework (docs/summoner_rework.md) must keep the old ids registered, stay server-only, be switchable, and be wired into the smoke test and the lang fragment. */
class SummonerReworkSourceScanTest {
    private static final Path MAIN = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) throws IOException { return Files.readString(MAIN.resolve(rel), StandardCharsets.UTF_8); }

    @Test
    void oldIdsStayRegisteredAndTheUltimateKeepsItsId() throws IOException {
        String skills = read("skill/NUSkills.java");
        for (String id : new String[]{"\"summoner\"", "\"hawk_eye\"", "\"spirit_card\"", "\"lord_of_summons\""}) assertTrue(skills.contains("register(" + id), id);
        assertTrue(read("skill/SkillEvolution.java").contains("m.put(NUSkills.SUMMONER, NUSkills.LORD_OF_SUMMONS);"), "the evolution path");
        assertTrue(read("skill/LordOfSummonsSkill.java").contains("\"Summoner of Beginnings\""), "the new name");
    }

    @Test
    void theReworkIsServerOnlyAndSwitchable() throws IOException {
        for (String f : new String[]{"skill/SummonerSkill.java", "skill/SummonerMigration.java", "skill/SummonerSelfTests.java", "skill/rules/SummonerRules.java"}) {
            String s = read(f);
            assertFalse(s.contains("net.minecraft.client") || s.contains("Minecraft.getInstance") || s.contains("com.mojang.blaze3d"), f + " touches a client class");
        }
        String rules = read("skill/rules/SummonerRules.java");
        assertFalse(rules.contains("net.minecraft"), "the rules class is Minecraft-free");
        assertTrue(read("NUConfig.java").contains("\"summonerRework\""));
        assertTrue(read("skill/HawkEyeSkill.java").contains("summonerRework()") && read("skill/SpiritCardSkill.java").contains("summonerRework()"), "the old skills go inert only with the setting on");
        assertTrue(read("skill/SummonerMigration.java").contains("if (!NUConfig.summonerRework()) return;"));
    }

    @Test
    void theWorkIsBounded() throws IOException {
        String summons = read("skill/Summons.java");
        assertTrue(summons.contains("static int strengthen(") && summons.contains("static Mob merge("), "synthesis and strengthening live in Summons");
        String skill = read("skill/SummonerSkill.java");
        assertTrue(skill.contains("SummonRules.allowed(Summons.owned(player).size(), max, want)"), "a card still goes through the cap");
        assertTrue(skill.contains("player.tickCount % 200 == 0") || skill.contains("now % 200 == 0"), "mastery from the toggles is a slow trickle");
    }

    @Test
    void smokeAndLangAreWired() throws IOException {
        assertTrue(read("NUSMP.java").contains("SummonerSelfTests.register()"));
        String smoke = Files.readString(Path.of(".github/workflows/smoke.yml"), StandardCharsets.UTF_8);
        assertTrue(smoke.contains(",summoner,") && smoke.contains(",scaling,boss.rework1,boss.rework3,boss.stats,tier\""), "the smoke list keeps its tail and has the summoner prefix");
        String frag = Files.readString(Path.of("tools/lang/summoner_rework.json"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("tools/lang/summoner_canon.json"), StandardCharsets.UTF_8);   // the canon pass adds keys and overrides some
        assertTrue(frag.contains("\"anime_multiverse.skill.lord_of_summons\": \"Summoner of Beginnings\""));
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("SkillMsg\\.t\\(\"(summoner\\.[a-z_]+)\"").matcher(read("skill/SummonerSkill.java") + read("skill/SummonerMigration.java"));
        int keys = 0;
        while (m.find()) { keys++; assertTrue(frag.contains("\"anime_multiverse.skill.msg." + m.group(1) + "\"") || isOld(m.group(1)), "lang fragment lacks " + m.group(1)); }
        assertTrue(keys > 5);
    }

    /** Keys the first release already had in en_us.json. */
    private static boolean isOld(String key) { return key.equals("summoner.cooldown") || key.equals("summoner.cap") || key.equals("summoner.failed"); }
}
