package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.rules.DbRules;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.server.MinecraftServer;

/** The CI self-test of 3.56.0 (id {@code skill.db}): all 30 Dragon Ball skills are in the registry with the right type, and each wheel matches its table. Registry reads only. */
public final class DbSkillSelfTests {
    private DbSkillSelfTests() {}

    public static void register() { SelfTests.register("skill.db", DbSkillSelfTests::check); }

    private static Result check(MinecraftServer server) {
        int n = 0;
        for (DbRules.Spec s : DbRules.SPECS) {
            ManasSkill u = SkillAPI.getSkillRegistry().get(DbSkills.UNIQUE.get(s.id()).getId());
            ManasSkill a = SkillAPI.getSkillRegistry().get(DbSkills.ULTIMATE.get(s.id()).getId());
            if (!(u instanceof DbSkill du) || !(a instanceof DbSkill da)) return Result.fail(s.id() + " is not registered as a Dragon Ball skill");
            if (du.getType() != Skill.SkillType.UNIQUE || da.getType() != Skill.SkillType.ULTIMATE) return Result.fail(s.id() + ": wrong skill type");
            if (du.wheel().size() != 2 || da.wheel().size() != s.actives().size()) return Result.fail(s.id() + ": the wheel does not match the table");
            n += 2;
        }
        return Result.ok(n + " Dragon Ball skills registered (15 Unique, 15 Ultimate); server setting dragonBallEnabled=" + com.newuniverse.nusmp.NUConfig.DB_ENABLED.get());
    }
}
