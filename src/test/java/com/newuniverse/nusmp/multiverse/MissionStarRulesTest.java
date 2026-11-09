package com.newuniverse.nusmp.multiverse;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionStarRulesTest {
    @Test
    void starsFollowTheTypeAndStayBounded() {
        assertEquals(1, MissionStarRules.stars("task", 1, 2, 5));
        assertEquals(2, MissionStarRules.stars("GOAL", 1, 2, 5));
        assertEquals(5, MissionStarRules.stars("Challenge", 1, 2, 5));
        assertEquals(1, MissionStarRules.stars(null, 1, 2, 5));
        assertEquals(MissionStarRules.MAX_STARS, MissionStarRules.stars("challenge", 1, 2, 9999));
        assertEquals(0, MissionStarRules.stars("task", -4, 2, 5));
    }

    @Test
    void onlyOurShownAdvancementsOutsideTheExcludeListCount() {
        List<String> ex = List.of("root", "magic/");
        assertTrue(MissionStarRules.counts("nusmp", "bosses/zogratis", true, ex));
        assertFalse(MissionStarRules.counts("minecraft", "story/mine_stone", true, ex));
        assertFalse(MissionStarRules.counts("nusmp", "root", true, ex));
        assertFalse(MissionStarRules.counts("nusmp", "magic/cast_cotton", true, ex));
        assertFalse(MissionStarRules.counts("nusmp", "bosses/zogratis", false, ex));
        assertTrue(MissionStarRules.counts("nusmp", "rootless/x", true, ex));
    }

    @Test
    void theHookIsRegisteredTheSelfTestIsInTheSmokeListAndNothingTicks() throws Exception {
        String mod = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUSMP.java"));
        assertTrue(mod.contains("MissionStars::onAdvancement") && mod.contains("MissionStarsSelfTests.register()"));
        String smoke = Files.readString(Path.of(".github/workflows/smoke.yml"));
        assertTrue(smoke.contains("missions.rules,missions.player") || (smoke.contains("missions.rules") && smoke.contains("missions.player")));
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/multiverse/MissionStars.java"));
        assertFalse(src.contains("PlayerTickEvent") || src.contains("ServerTickEvent"));
    }
}
