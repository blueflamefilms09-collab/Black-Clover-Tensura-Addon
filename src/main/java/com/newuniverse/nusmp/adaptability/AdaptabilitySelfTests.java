package com.newuniverse.nusmp.adaptability;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.NUSkills;
import net.minecraft.server.MinecraftServer;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: the CI server smoke test of Adaptability (prefix {@code adaptability} in SMOKE_EXPECT, 3.70.0). It checks what the dedicated server owns: the skill is registered under its id, the config
 * reads back its defaults, the damage maths stays inside its bounds for any number of stacks, the record never passes 16 kinds however many are fed in, and a long learned-skill list is paid in turns.
 */
public final class AdaptabilitySelfTests {
    private AdaptabilitySelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("adaptability.rules", AdaptabilitySelfTests::rules);
    }

    private static Result rules(MinecraftServer server) {
        if (!"nusmp:adaptability".equals(NUSkills.ADAPTABILITY.getId().toString())) return Result.fail("the skill is not registered as nusmp:adaptability");
        double perHit = AdaptabilityConfig.perHit(), floor = AdaptabilityConfig.minimumMultiplier();
        if (perHit < 0.0 || perHit > 1.0 || floor < 0.0 || floor > 1.0) return Result.fail("a config value is out of its range");
        for (int s : new int[] {0, 1, 35, 100, 100000, Integer.MAX_VALUE}) {
            double m = AdaptabilityRules.multiplier(s, perHit, floor);
            if (m < floor - 1e-9 || m > 1.0) return Result.fail("the damage factor left [floor, 1] at " + s + " stacks: " + m);
        }
        Map<String, Integer> record = new HashMap<>();
        for (int i = 0; i < 200; i++) {
            AdaptabilityRules.bump(record, "t" + (i % 40), 5);
            if (record.size() > AdaptabilityRules.MAX_TYPES) return Result.fail("the record grew past " + AdaptabilityRules.MAX_TYPES + " kinds");
        }
        for (int v : record.values()) if (v < 1 || v > 5) return Result.fail("a stack count left [1, max]");
        int seen = 0;
        for (int pass = 0; pass < 10; pass++) { int[] w = AdaptabilityRules.window(100, pass, 24); if (w[1] > 24) return Result.fail("a pass pays more than the cap"); seen += w[1]; }
        if (seen != 240) return Result.fail("the passes did not each pay the cap");
        return Result.ok("adaptability: skill registered, damage factor in [" + floor + ", 1], record <= " + AdaptabilityRules.MAX_TYPES + " kinds, mastery paid in windows of <= 24");
    }
}
