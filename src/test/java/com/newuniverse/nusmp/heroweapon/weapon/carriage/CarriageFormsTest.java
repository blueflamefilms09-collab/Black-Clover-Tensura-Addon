package com.newuniverse.nusmp.heroweapon.weapon.carriage;

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
 * Why: the carriage's forms file decides which techniques a player can ever use, so it is read from the source tree and judged against what the carriage's
 * code promises: every form is well formed and fair, every skill id it names is a technique of the carriage, every technique and every passive the carriage
 * built appears in some form. The texts of the techniques and passives are checked to exist for each one and to stay plain ASCII.
 */
class CarriageFormsTest {
    private static final Path FILE = Path.of("src/main/resources/data/nusmp/hero/forms/vassal_carriage.json");

    @Test
    void theShippedFileIsSoundAndUsesEverythingTheCarriageBuilt() throws Exception {
        Set<String> skills = new HashSet<>(), ultimates = new HashSet<>();
        for (Budget.Spec s : CarriageNumbers.ALL) {
            skills.add(s.id());
            if (s.ultimate()) ultimates.add(s.id());
        }
        List<String> bad = FormsFileCheck.check(Files.readString(FILE), new FormsFileCheck.Rules("vassal_carriage", skills, ultimates, CarriageText.PASSIVES.keySet()));
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void theTextsExistForEveryTechniqueAndPassiveAndStayPlain() {
        for (Budget.Spec s : CarriageNumbers.ALL) {
            String[] t = CarriageText.SKILLS.get(s.id());
            assertTrue(t != null && t[0].length() >= 3 && t[1].length() >= 20, "text for " + s.id());
            plain(s.id(), t[0] + t[1]);
        }
        assertEquals(CarriageNumbers.ALL.size(), CarriageText.SKILLS.size(), "no text for a technique that does not exist");
        for (String id : CarriageText.PASSIVES.keySet()) plain(id, CarriageText.PASSIVES.get(id)[0] + CarriageText.PASSIVES.get(id)[1]);
        assertEquals(4, CarriageText.PASSIVES.size());
    }

    private static void plain(String what, String text) {
        for (char ch : text.toCharArray()) assertTrue(ch >= 32 && ch < 127, what + " has a non-ASCII character");
    }
}
