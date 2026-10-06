package com.newuniverse.nusmp.vfx.fx;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every effect, at both detail levels, many seeds, lengths and powers, must pass every rule. */
class EffectRulesTest {
    @Test
    void allEffectsFollowTheRules() {
        List<String> failures = new ArrayList<>();
        for (String id : EffectLib.IDS)
          for (int v = 0; v < (id.startsWith("spirit_") ? EffectLib.SPIRIT_VARIANTS : 1); v++)
            for (EffectLib.Detail d : EffectLib.Detail.values())
                for (long seed = 0; seed < 60; seed++)
                    for (double len : new double[]{0, 3, 8, 16})
                        for (double power : new double[]{0.5, 1, 2}) {
                            var spec = EffectLib.build(id, d, seed, len, power, v);
                            for (String f : EffectChecks.check(spec, d)) if (!failures.contains(f)) failures.add(f);
                        }
        assertTrue(failures.isEmpty(), "Effect rule violations:\n" + String.join("\n", failures));
    }

    @Test
    void reducedDetailKeepsEveryPhase() {
        for (String id : EffectLib.IDS) {
            var full = EffectLib.build(id, EffectLib.Detail.FULL, 1, 8, 1, 2);
            var reduced = EffectLib.build(id, EffectLib.Detail.REDUCED, 1, 8, 1, 2);
            assertTrue(reduced.phases().size() == full.phases().size(), id + " dropped a phase at reduced detail");
            assertTrue(reduced.vertices() < full.vertices(), id + " reduced detail is not thinner: " + reduced.vertices() + " vs " + full.vertices());
        }
    }

    @Test
    void builtMeshesMatchTheBudget() {
        for (String id : EffectLib.IDS)
            for (EffectLib.Detail d : EffectLib.Detail.values())
                for (long seed = 0; seed < 20; seed++)
                    for (Piece p : EffectLib.build(id, d, seed, 8, 1, (int) (seed % 4)).pieces()) {
                        float[] m = com.newuniverse.nusmp.vfx.client.FxMeshes.build(p);
                        assertTrue(m.length / 5 == p.vertices(), id + "/" + p.group() + ": mesh has " + m.length / 5 + " vertices, budget says " + p.vertices());
                        for (float f : m) assertTrue(!Float.isNaN(f), id + "/" + p.group() + ": NaN vertex");
                    }
    }
}
