package com.newuniverse.nusmp.boss.rules;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 4.95.1: the damage diagnostic names each new kind of hit once, never floods, and is wired read-only behind its setting. */
class DamageProbeTest {
    @Test
    void eachKindIsNamedOnceAndTheLogIsCapped() {
        DamageProbe.reset();
        assertTrue(DamageProbe.first("goku", "mob", "player", "player"));
        assertFalse(DamageProbe.first("goku", "mob", "player", "player"));
        assertTrue(DamageProbe.first("goku", "mob", "player", "none"));
        assertTrue(DamageProbe.first("kakashi", "mob", "player", "player"));
        for (int i = 0; i < 100; i++) DamageProbe.first("goku", "type" + i, "a", "b");
        assertFalse(DamageProbe.first("goku", "brand_new", "x", "y"), "the cap holds");
        DamageProbe.reset();
        assertTrue(DamageProbe.first("goku", "mob", "player", "player"));
    }

    @Test
    void theProbeOnlyReadsAndHasItsSetting() throws Exception {
        String hooks = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/boss/rules/BossRulesHooks.java"));
        int at = hooks.indexOf("private static void probe(");
        String body = hooks.substring(at, hooks.indexOf("static void onJoin", at));
        assertFalse(body.contains("setCanceled") || body.contains("setAmount"), "a diagnostic never changes the hit");
        String cfg = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/boss/rules/BossRulesConfig.java"));
        assertTrue(cfg.contains("\"logDamageBosses\", \"anime_multiverse:goku_boss,anime_multiverse:kakashi_hatake\""));
    }
}
