package com.newuniverse.nusmp.heroweapon.weapon.chain;

import com.google.gson.JsonObject;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.FormsFile;
import com.newuniverse.nusmp.heroweapon.weapon.vassalkit.ShippedForms;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a forms file that breaks a rule of design.md section 6 is skipped at start-up and leaves the weapon with no forms, so the shipped Vassal Chain file must pass the checker, and the Sloth techniques must be only in the curse forms. */
class ChainFormsTest {
    private static Set<String> ids() {
        Set<String> ids = new HashSet<>();
        for (Budget.Row r : ChainNumbers.ALL) ids.add(r.id());
        return ids;
    }

    private static final Set<String> SLOTH_ONLY = Set.of("chain.sloth_weight", "chain.sloth_snap", "chain.debt_comes_due");

    @Test
    void theFileObeysTheFormsRules() {
        JsonObject file = ShippedForms.load("vassal_chain");
        List<String> bad = FormsFile.check(file, new FormsFile.Expect("vassal_chain", ids(), ChainNumbers.ULTIMATES, Set.of("sloth"), 3, 5, 2, 3));
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void everyListedSkillBelongsToTheChain() {
        for (String id : FormsFile.skillIds(ShippedForms.load("vassal_chain"))) assertTrue(ids().contains(id), id);
    }

    @Test
    void theSlothTechniquesAreOnlyInSlothForms() {
        for (var f : ShippedForms.load("vassal_chain").getAsJsonArray("forms")) {
            JsonObject form = f.getAsJsonObject();
            boolean curse = form.get("curse").getAsBoolean();
            for (var s : form.getAsJsonObject("skills").entrySet()) {
                if (SLOTH_ONLY.contains(s.getValue().getAsString())) assertTrue(curse, form.get("id").getAsString() + " lists " + s.getValue().getAsString());
            }
        }
    }

    @Test
    void theSlothFormsAreHeavierAndSlowerThanTheOrdinaryOnes() {
        double slowest = 0;
        for (var f : ShippedForms.load("vassal_chain").getAsJsonArray("forms")) {
            JsonObject form = f.getAsJsonObject();
            JsonObject stats = form.getAsJsonObject("stats");
            if (form.get("curse").getAsBoolean()) assertTrue(stats.has("attack_speed") && stats.get("attack_speed").getAsDouble() < 0, form.get("id").getAsString());
            else assertFalse(stats.has("attack_speed") && stats.get("attack_speed").getAsDouble() < 0, form.get("id").getAsString());
            if (form.get("curse").getAsBoolean()) slowest = Math.min(slowest, stats.get("attack_speed").getAsDouble());
        }
        assertTrue(slowest <= -0.7 + 1e-9);
    }
}
