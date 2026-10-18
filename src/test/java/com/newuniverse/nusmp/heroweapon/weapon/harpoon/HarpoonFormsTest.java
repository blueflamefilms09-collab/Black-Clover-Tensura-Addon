package com.newuniverse.nusmp.heroweapon.weapon.harpoon;

import com.google.gson.JsonObject;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.FormsFile;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.ShippedForms;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a forms file that breaks a rule of design.md section 6 is skipped at start-up, so the shipped Vassal Harpoon file must pass the checker, have no curse form and name only its own skills. */
class HarpoonFormsTest {
    private static Set<String> ids() {
        Set<String> ids = new HashSet<>();
        for (Budget.Row r : HarpoonNumbers.ALL) ids.add(r.id());
        return ids;
    }

    @Test
    void theFileObeysTheFormsRules() {
        JsonObject file = ShippedForms.load("vassal_harpoon");
        List<String> bad = FormsFile.check(file, new FormsFile.Expect("vassal_harpoon", ids(), HarpoonNumbers.ULTIMATES, Set.of(), 3, 5, 0, 0));
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void everyListedSkillBelongsToTheHarpoon() {
        for (String id : FormsFile.skillIds(ShippedForms.load("vassal_harpoon"))) assertTrue(ids().contains(id), id);
    }

    @Test
    void everyFormThatHasATetherTechniqueAlsoHasTheThrowThatMakesTheTether() {
        for (var f : ShippedForms.load("vassal_harpoon").getAsJsonArray("forms")) {
            JsonObject skills = f.getAsJsonObject().getAsJsonObject("skills");
            assertTrue(skills.has("use") && "harpoon.harpoon_throw".equals(skills.get("use").getAsString()), f.getAsJsonObject().get("id").getAsString());
        }
    }
}
