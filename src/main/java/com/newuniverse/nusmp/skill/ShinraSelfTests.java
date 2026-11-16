package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.rules.ShinraRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.server.MinecraftServer;

/**
 * Why: the Shinra rework (docs/shinra_rework.md) changes what four registered skills do and nobody can watch a game here, so the CI server smoke test checks it: {@code shinra.rules} (the pure numbers
 * hold: both wheels, thresholds, the Ultimate out-classes the Unique, nothing unbounded) and {@code shinra.skills} (all four ids are registered, Hell Walker, Hell Conqueror and Adolla Burst are
 * Shinra skills with the wheel the setting asks for, Devil's Footprints stays registered). Registry reads and stand-in instances that never belong to a player: no world, no tick work.
 */
public final class ShinraSelfTests {
    private ShinraSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("shinra.rules", s -> rules());
        SelfTests.register("shinra.skills", ShinraSelfTests::skills);
    }

    private static Result rules() {
        if (ShinraRules.wheel(false).length != 3 || ShinraRules.wheel(true).length != 4) return Result.fail("wheel sizes");
        if (ShinraRules.move(false, 2) != ShinraRules.RAPID || ShinraRules.move(true, 2) != ShinraRules.BURST || ShinraRules.move(true, -1) != ShinraRules.RAPID) return Result.fail("wheel order");
        if (!ShinraRules.grin(30, 100) || ShinraRules.grin(40, 100) || ShinraRules.grin(0, 100)) return Result.fail("grin threshold");
        if (!ShinraRules.link(20, 100, Tier.MASTERED) || ShinraRules.link(20, 100, Tier.BASE) || ShinraRules.link(30, 100, Tier.ULTIMATE)) return Result.fail("link threshold");
        if (ShinraRules.speedAmp(20, 100, Tier.ULTIMATE) != 1 || ShinraRules.strengthAmp(20, 100, Tier.ULTIMATE) != 1 || ShinraRules.speedAmp(100, 100, Tier.ULTIMATE) != -1) return Result.fail("passive amplifiers");
        if (!(ShinraRules.rapidDamage(Tier.ULTIMATE) > ShinraRules.rapidDamage(Tier.MASTERED) && ShinraRules.rapidDamage(Tier.MASTERED) > ShinraRules.rapidDamage(Tier.BASE))) return Result.fail("rapid damage ladder");
        if (ShinraRules.maxTargets() > 16 || ShinraRules.rapidRange(Tier.ULTIMATE) > 16 || ShinraRules.cooldownFactor(10, 100, Tier.ULTIMATE) >= 1.0) return Result.fail("bounds");
        return Result.ok("wheels 3 / 4, grin < 35 %, link < 25 %, at most " + ShinraRules.maxTargets() + " foes a move");
    }

    private static Result skills(MinecraftServer server) {
        boolean on = NUConfig.shinraRework();
        ManasSkill walker = SkillAPI.getSkillRegistry().get(NUSkills.HELL_WALKER.getId());
        ManasSkill conq = SkillAPI.getSkillRegistry().get(NUSkills.HELL_CONQUEROR.getId());
        ManasSkill adolla = SkillAPI.getSkillRegistry().get(NUSkills.ADOLLA_BURST.getId());
        ManasSkill footprints = SkillAPI.getSkillRegistry().get(NUSkills.DEVILS_FOOTPRINTS.getId());
        if (!(walker instanceof HellWalkerSkill w) || !(conq instanceof HellWalkerSkill c) || !(adolla instanceof AdollaBurstSkill a)) return Result.fail("a Shinra skill is not a HellWalkerSkill");
        if (!(footprints instanceof DevilsFootprintsSkill)) return Result.fail("devils_footprints is no longer registered");
        ManasSkillInstance wi = w.createDefaultInstance(), ci = c.createDefaultInstance(), ai = a.createDefaultInstance();
        int want = on ? 3 : 1;
        if (w.getModes(wi) != want || c.getModes(ci) != want) return Result.fail("Hell Walker / Hell Conqueror wheel " + w.getModes(wi) + " / " + c.getModes(ci) + ", expected " + want);
        if (a.getModes(ai) != (on ? 4 : 3)) return Result.fail("Adolla Burst wheel " + a.getModes(ai));
        if (on && !"shinra.rapid".equals(w.getModeId(wi, 2))) return Result.fail("Hell Walker's third move is " + w.getModeId(wi, 2));
        if (!"fireforce.light_speed".equals(a.getModeId(ai, 0)) || !"fireforce.burst".equals(a.getModeId(ai, 2))) return Result.fail("Adolla Burst's wheel ids changed");
        return Result.ok("shinraRework=" + on + ", Hell Walker " + w.getModes(wi) + " moves, Adolla Burst " + a.getModes(ai) + " moves");
    }
}
