package com.newuniverse.nusmp.boss.rules;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 4.90.0: the list of bosses that take no anonymous damage, and the pins of Goku's farewell. */
class AnonymousDamageTest {
    @Test
    void listMatchesIdsIgnoringCaseAndSpaces() {
        assertTrue(BossEnvironment.listed("anime_multiverse:goku_boss", "anime_multiverse:goku_boss"));
        assertTrue(BossEnvironment.listed("anime_multiverse:goku_boss", " a:b , Anime_Multiverse:Goku_Boss "));
        assertFalse(BossEnvironment.listed("anime_multiverse:goku_boss", ""));
        assertFalse(BossEnvironment.listed("anime_multiverse:sinbad_boss", "anime_multiverse:goku_boss"));
        assertFalse(BossEnvironment.listed(null, "a:b"));
    }

    @Test
    void gokuFarewellIsWiredBehindItsSetting() throws Exception {
        String boss = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/arc/goku/GokuBoss.java"));
        assertTrue(boss.contains("GokuBossConfig.farewell()"), "die() reads the setting");
        assertTrue(boss.contains("finishFarewell"), "the framework's death runs when he leaves");
        assertTrue(boss.contains("farewellAt != 0L && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)"), "he cannot be hurt while leaving");
        String cfg = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/arc/goku/GokuBossConfig.java"));
        assertTrue(cfg.contains("\"farewell\", true"), "default on");
        String rules = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/boss/rules/BossRulesConfig.java"));
        assertTrue(rules.contains("\"anonymousDamageBosses\", \"anime_multiverse:goku_boss\""), "Goku is the default listed boss");
    }
}
