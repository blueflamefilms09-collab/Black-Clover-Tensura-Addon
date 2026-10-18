package com.newuniverse.nusmp.heroweapon.weapon.mirror;

import com.google.gson.JsonObject;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.FormsFile;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.ShippedForms;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a forms file that breaks a rule of design.md section 6 is skipped at start-up, so the shipped Vassal Mirror file must pass the checker and name only skills of this weapon. */
class MirrorFormsTest {
    private static Set<String> ids() {
        Set<String> ids = new HashSet<>();
        for (Budget.Row r : MirrorNumbers.ALL) ids.add(r.id());
        return ids;
    }

    @Test
    void theFileObeysTheFormsRules() {
        JsonObject file = ShippedForms.load("vassal_mirror");
        List<String> bad = FormsFile.check(file, new FormsFile.Expect("vassal_mirror", ids(), MirrorNumbers.ULTIMATES, Set.of("envy"), 3, 5, 2, 3));
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void everyListedSkillBelongsToTheMirror() {
        for (String id : FormsFile.skillIds(ShippedForms.load("vassal_mirror"))) assertTrue(ids().contains(id), id);
    }

    @Test
    void theCurseSeriesIsEnvyOnly() {
        JsonObject file = ShippedForms.load("vassal_mirror");
        int curse = 0;
        for (var f : file.getAsJsonArray("forms")) {
            JsonObject o = f.getAsJsonObject();
            if (o.get("curse").getAsBoolean()) {
                curse++;
                assertEquals("envy", o.get("sin").getAsString());
            }
        }
        assertEquals(3, curse);
    }
}
