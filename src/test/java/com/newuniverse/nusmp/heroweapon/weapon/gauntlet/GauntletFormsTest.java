package com.newuniverse.nusmp.heroweapon.weapon.gauntlet;

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
 * Why: the gauntlet's forms file decides which techniques a player can ever use, so it is read from the source tree and judged against what the gauntlet's
 * code promises: every form is well formed and fair, every skill id it names is a technique of the gauntlet, every technique and every passive the gauntlet
 * built appears in some form. The texts of the techniques and passives are checked to exist for each one and to stay plain ASCII.
 */
class GauntletFormsTest {
    private static final Path FILE = Path.of("src/main/resources/data/nusmp/hero/forms/vassal_gauntlet.json");

    @Test
    void theShippedFileIsSoundAndUsesEverythingTheGauntletBuilt() throws Exception {
        Set<String> skills = new HashSet<>(), ultimates = new HashSet<>();
        for (Budget.Spec s : GauntletNumbers.ALL) {
            skills.add(s.id());
            if (s.ultimate()) ultimates.add(s.id());
        }
        List<String> bad = FormsFileCheck.check(Files.readString(FILE), new FormsFileCheck.Rules("vassal_gauntlet", skills, ultimates, GauntletText.PASSIVES.keySet()));
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void theTextsExistForEveryTechniqueAndPassiveAndStayPlain() {
        for (Budget.Spec s : GauntletNumbers.ALL) {
            String[] t = GauntletText.SKILLS.get(s.id());
            assertTrue(t != null && t[0].length() >= 3 && t[1].length() >= 20, "text for " + s.id());
            plain(s.id(), t[0] + t[1]);
        }
        assertEquals(GauntletNumbers.ALL.size(), GauntletText.SKILLS.size(), "no text for a technique that does not exist");
        for (String id : GauntletText.PASSIVES.keySet()) plain(id, GauntletText.PASSIVES.get(id)[0] + GauntletText.PASSIVES.get(id)[1]);
        assertEquals(5, GauntletText.PASSIVES.size());
    }

    private static void plain(String what, String text) {
        for (char ch : text.toCharArray()) assertTrue(ch >= 32 && ch < 127, what + " has a non-ASCII character");
    }
}
