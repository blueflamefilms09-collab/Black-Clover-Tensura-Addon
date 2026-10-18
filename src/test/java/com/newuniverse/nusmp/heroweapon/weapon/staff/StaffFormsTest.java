package com.newuniverse.nusmp.heroweapon.weapon.staff;

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
 * Why: the staff's forms file decides which techniques a player can ever use, so it is read from the source tree and judged against what the staff's code
 * promises: every form is well formed and fair, every skill id it names is a technique of the staff, every technique and every passive the staff built appears
 * in some form, and the tiers climb from the Apprentice Staff to the Archmage Staff.
 */
class StaffFormsTest {
    private static final Path FILE = Path.of("src/main/resources/data/nusmp/hero/forms/vassal_staff.json");

    @Test
    void theShippedFileIsSoundAndUsesEverythingTheStaffBuilt() throws Exception {
        Set<String> skills = new HashSet<>(), ultimates = new HashSet<>();
        for (Budget.Spec s : StaffNumbers.ALL) {
            skills.add(s.id());
            if (s.ultimate()) ultimates.add(s.id());
        }
        List<String> bad = FormsFileCheck.check(Files.readString(FILE), new FormsFileCheck.Rules("vassal_staff", skills, ultimates, StaffText.PASSIVES.keySet()));
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void theTextsExistForEveryTechniqueAndPassiveAndStayPlain() {
        for (Budget.Spec s : StaffNumbers.ALL) {
            String[] t = StaffText.SKILLS.get(s.id());
            assertTrue(t != null && t[0].length() >= 3 && t[1].length() >= 20, "text for " + s.id());
            plain(s.id(), t[0] + t[1]);
        }
        assertEquals(StaffNumbers.ALL.size(), StaffText.SKILLS.size(), "no text for a technique that does not exist");
        for (String id : StaffText.PASSIVES.keySet()) plain(id, StaffText.PASSIVES.get(id)[0] + StaffText.PASSIVES.get(id)[1]);
        assertEquals(5, StaffText.PASSIVES.size());
    }

    private static void plain(String what, String text) {
        for (char ch : text.toCharArray()) assertTrue(ch >= 32 && ch < 127, what + " has a non-ASCII character");
    }
}
