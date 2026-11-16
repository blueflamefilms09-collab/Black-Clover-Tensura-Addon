package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.rules.ArgonautMergeRules;
import com.newuniverse.nusmp.skill.rules.ChargeRules;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.server.MinecraftServer;

import java.util.HashSet;
import java.util.Set;

/**
 * Why: Firebolt was folded into Argonaut (docs/argonaut_rework.md): a mode table, a migration and a server setting that nobody can watch in a game here, so the CI server smoke
 * test checks them: key {@code argonaut.modes} (the three skills are registered, Firebolt still is, the mode tables hold and every old mode index kept its meaning, mode ids are
 * unique, a stale saved mode falls back to the Strike) and {@code argonaut.rules} (the live setting and the pure rules agree: ignite seconds, mastery, migration answers). Registry
 * reads and stand-in instances that never belong to a player: no world, no tick work.
 */
public final class ArgonautSelfTests {
    private ArgonautSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("argonaut.modes", ArgonautSelfTests::modes);
        SelfTests.register("argonaut.rules", s -> rules());
    }

    private static Result modes(MinecraftServer server) {
        ManasSkill fire = SkillAPI.getSkillRegistry().get(NUSkills.FIREBOLT.getId());
        ManasSkill arg = SkillAPI.getSkillRegistry().get(NUSkills.ARGONAUT.getId());
        ManasSkill hero = SkillAPI.getSkillRegistry().get(NUSkills.HEROS_ARGONAUT.getId());
        if (!(fire instanceof FireboltSkill)) return Result.fail("firebolt is no longer registered as a FireboltSkill");
        if (!(arg instanceof ArgonautSkill a) || arg instanceof HerosArgonautSkill) return Result.fail("argonaut is not registered as the plain ArgonautSkill");
        if (!(hero instanceof HerosArgonautSkill h)) return Result.fail("heros_argonaut is not registered as a HerosArgonautSkill");
        ManasSkillInstance ai = a.createDefaultInstance(), hi = h.createDefaultInstance();
        boolean merged = ArgonautMerge.enabled();
        if (a.getModes(ai) != (merged ? 2 : 1)) return Result.fail("Argonaut has " + a.getModes(ai) + " modes with merge=" + merged);
        if (h.getModes(hi) != (merged ? 3 : 2)) return Result.fail("Hero's Argonaut has " + h.getModes(hi) + " modes with merge=" + merged);
        if (!"argonaut.strike".equals(h.getModeId(hi, 0)) || !"argonaut.beam".equals(h.getModeId(hi, 1))) return Result.fail("an old Hero's Argonaut mode moved");
        if (merged && !"argonaut.firebolt".equals(h.getModeId(hi, 2))) return Result.fail("Hero's Argonaut mode 2 is not Firebolt");
        if (merged && !"argonaut.firebolt".equals(a.getModeId(ai, 1))) return Result.fail("Argonaut mode 1 is not Firebolt");
        Set<String> seen = new HashSet<>();
        for (int m = 0; m < h.getModes(hi); m++) if (!seen.add(h.getModeId(hi, m))) return Result.fail("Hero's Argonaut repeats a mode id: " + h.getModeId(hi, m));
        seen.clear();
        for (int m = 0; m < a.getModes(ai); m++) if (!seen.add(a.getModeId(ai, m))) return Result.fail("Argonaut repeats a mode id: " + a.getModeId(ai, m));
        // a saved mode from a time the setting was on is the plain Strike once it is off, and the mode cycle never leaves the live range
        if (ArgonautMergeRules.kind(2, true, false) != ChargeRules.Strike.STRIKE || ArgonautMergeRules.kind(1, false, false) != ChargeRules.Strike.STRIKE)
            return Result.fail("a stale saved mode is not the Strike");
        for (int m = 0; m < 3; m++) {
            int n = a.nextMode(null, ai, m, false), r = h.nextMode(null, hi, m, true);
            if (n < 0 || n >= a.getModes(ai) || r < 0 || r >= h.getModes(hi)) return Result.fail("the mode cycle left the live range at " + m);
        }
        return Result.ok("merge=" + merged + ", Argonaut modes " + a.getModes(ai) + ", Hero modes " + h.getModes(hi));
    }

    private static Result rules() {
        for (int c = 0; c <= 10; c++) {
            int s = ArgonautMergeRules.chargedIgniteSeconds(c);
            if (s < 0 || s > ArgonautMergeRules.MAX_IGNITE_SECONDS) return Result.fail("ignite seconds out of range at charge " + c + ": " + s);
        }
        if (ArgonautMergeRules.chargedIgniteSeconds(1) != 0 || ArgonautMergeRules.chargedIgniteSeconds(ArgonautMergeRules.FIRE_MIN_CHARGE) <= 0) return Result.fail("the fire threshold moved");
        if (ArgonautMergeRules.fireboltMastery(false) != 0.0 || ArgonautMergeRules.fireboltMastery(true) <= 0.0) return Result.fail("Firebolt mastery is not hit-only");
        if (!ArgonautMergeRules.migrates(true, true) || ArgonautMergeRules.migrates(false, true) || ArgonautMergeRules.migrates(true, false)) return Result.fail("the migration answer is wrong");
        if (ArgonautMergeRules.grantsArgonaut(true, false) || ArgonautMergeRules.grantsArgonaut(false, true) || !ArgonautMergeRules.grantsArgonaut(false, false)) return Result.fail("the grant answer is wrong");
        if (!ArgonautMergeRules.restores(false, true, false) || ArgonautMergeRules.restores(true, true, false) || ArgonautMergeRules.restores(false, true, true)) return Result.fail("the restore answer is wrong");
        if (NUConfig.FIREBOLT_COOLDOWN.get() < 0 || NUConfig.FIREBOLT_MAGICULE_COST.get() < 0) return Result.fail("Firebolt's own price or cooldown is negative");
        return Result.ok("merge=" + ArgonautMerge.enabled() + ", fireboltCooldown=" + NUConfig.FIREBOLT_COOLDOWN.get() + ", fireboltCost=" + NUConfig.FIREBOLT_MAGICULE_COST.get());
    }
}
