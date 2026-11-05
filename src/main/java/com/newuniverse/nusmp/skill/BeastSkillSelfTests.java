package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.rules.BeastSkillRules;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.server.MinecraftServer;

/** The CI self-test of 3.58.0 (id {@code skill.beast}): all 54 Beast Will skills are in the registry with the right type, each wheel matches its table, and each finisher's effect shape exists. Registry reads only. */
public final class BeastSkillSelfTests {
    private BeastSkillSelfTests() {}

    public static void register() { SelfTests.register("skill.beast", BeastSkillSelfTests::check); }

    private static Result check(MinecraftServer server) {
        int n = 0;
        for (BeastSkillRules.Spec s : BeastSkillRules.SPECS) {
            ManasSkill u = SkillAPI.getSkillRegistry().get(BeastSkills.UNIQUE.get(s.key()).getId());
            ManasSkill a = SkillAPI.getSkillRegistry().get(BeastSkills.ULTIMATE.get(s.key()).getId());
            if (!(u instanceof BeastSkill bu) || !(a instanceof BeastSkill ba)) return Result.fail(s.id() + " is not registered as a Beast Will skill");
            if (bu.getType() != Skill.SkillType.UNIQUE || ba.getType() != Skill.SkillType.ULTIMATE) return Result.fail(s.id() + ": wrong skill type");
            if (bu.wheel().size() != 2 || ba.wheel().size() != s.actives().size()) return Result.fail(s.id() + ": the wheel does not match the table");
            try { VfxShape.valueOf(s.fx().substring(2)); } catch (IllegalArgumentException e) { return Result.fail(s.id() + ": unknown finisher shape " + s.fx()); }
            n += 2;
        }
        return Result.ok(n + " Beast Will skills registered (27 Unique, 27 Ultimate); server setting beastSkillsEnabled=" + com.newuniverse.nusmp.NUConfig.BEAST_SKILLS_ENABLED.get());
    }
}
