package com.newuniverse.nusmp.client.relics.worn.lib;

import com.newuniverse.nusmp.client.relics.worn.WornAssets;
import com.newuniverse.nusmp.client.relics.worn.WornPlan;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The whole client decision, run for every relic the wave sources define, with the files of the jar: for each relic worn on the body, in every context a wearer is drawn in (wide and slim
 * arms, own body and another's, models on and off, a player's boxes and a kit character's charms), {@link WornPlan} answers with a way to draw it, and the way it names can be carried out
 * (a library model that loads, plain boxes, an own renderer, a charm). No relic is skipped for a reason a wearer cannot see.
 */
class WornPlanCoverageTest {
    @Test
    void everyWornRelicIsDrawnInEveryContext() throws IOException {
        List<WornCoverage.Entry> relics = WornCoverageTest.relicsFromSources();
        Map<WornPlan.Kind, Integer> total = new EnumMap<>(WornPlan.Kind.class);
        int worn = 0;
        List<String> bad = new ArrayList<>();
        for (boolean slim : new boolean[]{false, true})
            for (WornPlan.View view : new WornPlan.View[]{WornPlan.View.OTHER, WornPlan.View.SELF_THIRD})
                for (boolean models : new boolean[]{true, false})
                    for (WornPlan.Fallback fb : WornPlan.Fallback.values()) {
                        List<WornPlan.Entry> entries = new ArrayList<>();
                        for (WornCoverage.Entry r : relics) {
                            if (!WornCoverage.isWornSlot(r.slot())) continue;
                            boolean has = WornAssets.hasModel(r.id(), r.slot(), r.look());
                            WornPlan.Source src = "HEAD".equals(r.slot()) || "CHEST".equals(r.slot()) || "FEET".equals(r.slot()) ? WornPlan.Source.ARMOUR : WornPlan.Source.CURIOS;
                            entries.add(new WornPlan.Entry(r.id(), r.slot(), r.look(), src, Boolean.TRUE, WornAssets.CUSTOM_RENDERERS.contains(r.id()), has, 3));
                        }
                        worn = entries.size();
                        WornPlan.Plan plan = WornPlan.decide(new WornPlan.Ctx(slim, view, false, false, false, models, false, fb), entries);
                        for (WornPlan.Item it : plan.all()) {
                            if (it.kind() == WornPlan.Kind.SKIP && it.why() != WornPlan.Why.VANILLA_ARMOUR) bad.add(it.describe());
                            if (it.kind() == WornPlan.Kind.LIBRARY_MODEL && !it.entry().hasModel()) bad.add(it.entry().id() + ": library model without a loadable file");
                            total.merge(it.kind(), 1, Integer::sum);
                        }
                    }
        assertTrue(worn > 300, "worn relics: " + worn);
        assertTrue(bad.isEmpty(), bad.size() + " problems, first: " + (bad.isEmpty() ? "-" : bad.get(0)));
        assertTrue(total.getOrDefault(WornPlan.Kind.LIBRARY_MODEL, 0) > 0 && total.getOrDefault(WornPlan.Kind.PLAIN_BOX, 0) > 0, total.toString());
    }

    @Test
    void everyArmourRelicHasAModelSoNoneDependsOnThePaintedBoxAlone() throws IOException {
        int armour = 0;
        List<String> none = new ArrayList<>();
        for (WornCoverage.Entry r : WornCoverageTest.relicsFromSources()) {
            if (!"HEAD".equals(r.slot()) && !"CHEST".equals(r.slot()) && !"FEET".equals(r.slot())) continue;
            armour++;
            if (!WornAssets.hasModel(r.id(), r.slot(), r.look())) none.add(r.id());
        }
        assertTrue(armour > 40, "armour relics: " + armour);
        assertTrue(none.isEmpty(), "armour relics the library cannot dress (their painted box is all there is): " + none);
    }
}
