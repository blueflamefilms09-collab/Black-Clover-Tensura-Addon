package com.newuniverse.nusmp.heroweapon.weapon.sewingset;

import com.google.gson.JsonObject;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.FormsFile;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.ShippedForms;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a forms file that breaks a rule of design.md section 6 is skipped at start-up, so the shipped Vassal Sewing Set file must pass the checker, have no curse form and name only its own skills. */
class SewingFormsTest {
    private static Set<String> ids() {
        Set<String> ids = new HashSet<>();
        for (Budget.Row r : SewingNumbers.ALL) ids.add(r.id());
        return ids;
    }

    @Test
    void theFileObeysTheFormsRules() {
        JsonObject file = ShippedForms.load("vassal_sewing_set");
        List<String> bad = FormsFile.check(file, new FormsFile.Expect("vassal_sewing_set", ids(), SewingNumbers.ULTIMATES, Set.of(), 3, 5, 0, 0));
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void everyListedSkillBelongsToTheSewingSet() {
        for (String id : FormsFile.skillIds(ShippedForms.load("vassal_sewing_set"))) assertTrue(ids().contains(id), id);
    }

    @Test
    void everyFormHasTheNeedleShotThatMakesTheMarksTheOthersUse() {
        for (var f : ShippedForms.load("vassal_sewing_set").getAsJsonArray("forms")) {
            JsonObject skills = f.getAsJsonObject().getAsJsonObject("skills");
            assertEquals("sewing_set.needle_shot", skills.get("use").getAsString(), f.getAsJsonObject().get("id").getAsString());
        }
    }
}
