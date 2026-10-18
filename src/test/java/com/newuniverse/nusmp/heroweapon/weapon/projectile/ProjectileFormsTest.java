package com.newuniverse.nusmp.heroweapon.weapon.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.weapon.kit.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.kit.FormsFileCheck;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the projectile's forms file decides which techniques a player can ever use, so it is read from the source tree and judged against what the projectile's
 * code promises: every form is well formed and fair, every skill id it names is a technique of the projectile, every technique and every passive the projectile
 * built appears in some form. The texts of the techniques and passives are checked to exist for each one and to stay plain ASCII.
 */
class ProjectileFormsTest {
    private static final Path FILE = Path.of("src/main/resources/data/nusmp/hero/forms/vassal_projectile.json");

    @Test
    void theShippedFileIsSoundAndUsesEverythingTheProjectileBuilt() throws Exception {
        Set<String> skills = new HashSet<>(), ultimates = new HashSet<>();
        for (Budget.Spec s : ProjectileNumbers.ALL) {
            skills.add(s.id());
            if (s.ultimate()) ultimates.add(s.id());
        }
        List<String> bad = FormsFileCheck.check(Files.readString(FILE), new FormsFileCheck.Rules("vassal_projectile", skills, ultimates, ProjectileText.PASSIVES.keySet()));
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void theTextsExistForEveryTechniqueAndPassiveAndStayPlain() {
        for (Budget.Spec s : ProjectileNumbers.ALL) {
            String[] t = ProjectileText.SKILLS.get(s.id());
            assertTrue(t != null && t[0].length() >= 3 && t[1].length() >= 20, "text for " + s.id());
            plain(s.id(), t[0] + t[1]);
        }
        assertEquals(ProjectileNumbers.ALL.size(), ProjectileText.SKILLS.size(), "no text for a technique that does not exist");
        for (String id : ProjectileText.PASSIVES.keySet()) plain(id, ProjectileText.PASSIVES.get(id)[0] + ProjectileText.PASSIVES.get(id)[1]);
        assertEquals(5, ProjectileText.PASSIVES.size());
    }

    private static void plain(String what, String text) {
        for (char ch : text.toCharArray()) assertTrue(ch >= 32 && ch < 127, what + " has a non-ASCII character");
    }
}
