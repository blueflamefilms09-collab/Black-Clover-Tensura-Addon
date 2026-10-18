package com.newuniverse.nusmp.heroweapon.weapon.instrument;

import com.google.gson.JsonObject;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.FormsFile;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.ShippedForms;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a forms file that breaks a rule of design.md section 6 is skipped at start-up, so the shipped Vassal Musical Instrument file must pass the checker, have no curse form and name only its own skills. */
class InstrumentFormsTest {
    private static Set<String> ids() {
        Set<String> ids = new HashSet<>();
        for (Budget.Row r : InstrumentNumbers.ALL) ids.add(r.id());
        return ids;
    }

    @Test
    void theFileObeysTheFormsRules() {
        JsonObject file = ShippedForms.load("vassal_instrument");
        List<String> bad = FormsFile.check(file, new FormsFile.Expect("vassal_instrument", ids(), InstrumentNumbers.ULTIMATES, Set.of(), 3, 5, 0, 0));
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void everyListedSkillBelongsToTheInstrument() {
        for (String id : FormsFile.skillIds(ShippedForms.load("vassal_instrument"))) assertTrue(ids().contains(id), id);
    }

    @Test
    void theTopFormNeedsBothBranches() {
        JsonObject top = null;
        for (var f : ShippedForms.load("vassal_instrument").getAsJsonArray("forms")) if ("instrument_zero".equals(f.getAsJsonObject().get("id").getAsString())) top = f.getAsJsonObject();
        assertNotNull(top);
        assertEquals(2, top.getAsJsonArray("parents").size());
    }
}
