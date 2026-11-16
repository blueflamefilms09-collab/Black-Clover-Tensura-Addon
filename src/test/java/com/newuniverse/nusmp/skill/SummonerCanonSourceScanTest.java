package com.newuniverse.nusmp.skill;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the Summoner canon pass (docs/summoner_rework.md) must stay server-only, bounded, switchable, append-only and wired into the lang fragment and the smoke test. */
class SummonerCanonSourceScanTest {
    private static final Path MAIN = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) throws IOException { return Files.readString(MAIN.resolve(rel), StandardCharsets.UTF_8); }

    @Test
    void theCanonPassIsServerOnlySwitchableAndKeepsTheOldModesFirst() throws IOException {
        String canon = read("skill/SummonerCanon.java");
        assertFalse(canon.contains("net.minecraft.client") || canon.contains("Minecraft.getInstance") || canon.contains("com.mojang.blaze3d"));
        String config = read("NUConfig.java");
        assertTrue(config.contains("\"summonerCanon\"") && config.contains("live(SUMMONER_CANON) && live(SUMMONER_REWORK)"), "the setting exists and needs the rework");
        String skill = read("skill/SummonerSkill.java");
        assertTrue(skill.contains("NUConfig.summonerCanon() ? SummonerRules.MODES_CANON : NUConfig.summonerRework() ? SummonerRules.MODES"), "the old mode counts are the fallback");
        assertTrue(skill.contains("canon || SummonerRules.reflects("), "the old reflection gate is the fallback");
        String rules = read("skill/rules/SummonerRules.java");
        assertFalse(rules.contains("net.minecraft"), "the rules stay Minecraft-free");
        assertTrue(rules.contains("BEAST = 0, STONE = 1, RECALL = 2, LEGION = 3, WRAITH = 4, BIRD = 5, SPIRIT = 6, STRENGTHEN = 7, SYNTHESIS = 8, MODES = 9"), "the old mode indexes are untouched");
        assertTrue(rules.contains("INSECT = 9, FISH = 10, SPIRIT_CAST = 11, DRAGON = 12, AWAKENING = 13, DEPUTIZE = 14, STORAGE = 15, ANGEL = 16, MODES_CANON = 17"), "new modes are appended");
    }

    @Test
    void everyCastIsBounded() throws IOException {
        String canon = read("skill/SummonerCanon.java");
        assertFalse(canon.contains("getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(64.0)") && !canon.contains("i < 48"), "the Farsight scan is capped");
        assertTrue(canon.contains("SummonerRules.ANGEL_TARGETS"), "the Angel's lightning is capped");
        assertTrue(canon.contains("setVisualOnly(true)"), "a bolt never sets fire");
        assertFalse(canon.contains("static final java.util.Map") || canon.contains("new java.util.HashMap"), "no state is kept");
        String summons = read("skill/Summons.java");
        assertTrue(summons.contains("addOrReplacePermanentModifier") && summons.contains("ResourceLocation.fromNamespaceAndPath(\"anime_multiverse\", \"summoner_strength_hp\")"), "the boosts are permanent modifiers built once");
        assertFalse(read("skill/SummonerSkill.java").contains("addFreshEntity"), "the skill class spawns nothing");
        assertTrue(read("skill/SummonerSkill.java").contains("SummonRules.allowed(Summons.owned(player).size(), max, want)"), "cards still go through the cap");
    }

    @Test
    void langFragmentCarriesTheNewKeys() throws IOException {
        String frag = Files.readString(Path.of("tools/lang/summoner_canon.json"), StandardCharsets.UTF_8);
        for (String mode : new String[]{"grass", "insect", "fish", "spirit_card", "dragon", "awakening", "deputize", "storage", "angel"}) {
            assertTrue(frag.contains("\"anime_multiverse.skill.mode.summoner." + mode + "\""), "mode name " + mode);
        }
        for (String msg : new String[]{"locked_canon", "canon_nothing", "awakened", "deputize_look", "deputize_limit", "deputized", "general", "king", "plain"}) {
            assertTrue(frag.contains("\"anime_multiverse.skill.msg.summoner." + msg + "\""), "message " + msg);
        }
        String skill = read("skill/SummonerSkill.java");
        for (String mode : new String[]{"grass", "insect", "fish", "spirit_card", "dragon", "awakening", "deputize", "storage", "angel"}) {
            assertTrue(skill.contains("SkillMsg.mode(\"summoner." + mode + "\""), "mode " + mode + " is named in the skill");
        }
        assertTrue(read("skill/SummonerSelfTests.java").contains("\"summoner.canon\""), "the self-test is registered (prefix summoner is in the smoke list)");
    }
}
