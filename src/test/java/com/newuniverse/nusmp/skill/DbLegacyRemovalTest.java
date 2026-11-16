package com.newuniverse.nusmp.skill;

import static org.junit.jupiter.api.Assertions.*;

import com.newuniverse.nusmp.skill.rules.DbRules;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Dragon Ball rework, phase 1 (the owner's "remove all old Dragon Ball skills"): the removal is a server setting (default removed), the ids, classes, rules and icons stay (rule 2, old worlds load),
 * and every entry point (command, evolution, use, login) reads the one switch. Source scan, no game needed.
 */
class DbLegacyRemovalTest {
    private static String read(String path) throws Exception { return Files.readString(Path.of(path), StandardCharsets.UTF_8); }

    private static final String S = "src/main/java/com/newuniverse/nusmp/";

    @Test void theSwitchIsAServerSettingThatDefaultsToRemoved() throws Exception {
        String cfg = read(S + "NUConfig.java");
        assertTrue(cfg.contains("define(\"dragonBallLegacySkills\", false)"), "default false = removed");
        assertTrue(read(S + "NUSMP.java").contains("registerConfig(ModConfig.Type.SERVER, NUConfig.SPEC)"), "NUConfig is a SERVER config (rule 9)");
    }

    @Test void everyEntryPointReadsTheSwitch() throws Exception {
        String skills = read(S + "skill/DbSkills.java");
        assertTrue(skills.contains("requires(c -> c.hasPermission(2) && legacy())"), "the command node is hidden while removed");
        assertTrue(skills.contains("static void evolve(ServerPlayer player, Skills skills) {") && skills.contains("if (!active()) return;"), "no evolution while removed");
        assertTrue(skills.contains("public static void onLogin(PlayerEvent.PlayerLoggedInEvent e)") && skills.contains("legacy()) return;"), "login revokes only while removed");
        assertTrue(skills.contains("skills.forgetSkill(h.getId())"), "revoked through the normal forget path (clears the passives)");
        String skill = read(S + "skill/DbSkill.java");
        assertEquals(2, skill.split("DbSkills\\.active\\(\\)", -1).length - 1, "use and passives both ask active()");
        assertFalse(skill.contains("DB_ENABLED"), "DbSkill no longer reads dragonBallEnabled directly");
        assertTrue(read(S + "NUSMP.java").contains("DbSkills::onLogin"), "the login hook is registered");
    }

    @Test void theIdsClassesRulesAndIconsAreKept() throws Exception {
        assertTrue(read(S + "skill/NUSkills.java").contains("DbSkills.registerAll(SKILLS)"), "still registered (old worlds load)");
        assertTrue(read(S + "multiverse/MultiverseCommands.java").contains("DbSkills.node()"), "the node is still attached, only hidden");
        assertTrue(read(S + "skill/SkillEvolution.java").contains("DbSkills.evolve(player, skills)"), "the call stays, the method returns early");
        assertEquals(15, DbRules.SPECS.size());
        for (DbRules.Spec s : DbRules.SPECS)
            for (String id : new String[] {s.id(), s.uniqueId()})
                assertTrue(new File("src/main/resources/assets/anime_multiverse/textures/skill/" + id + ".png").isFile(), id + ": the icon stays (rule 2)");
    }
}
