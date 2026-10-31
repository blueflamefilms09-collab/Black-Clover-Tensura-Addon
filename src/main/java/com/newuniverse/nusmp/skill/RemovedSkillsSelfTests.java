package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skilltree.unique.LegacyIds;
import com.newuniverse.nusmp.skilltree.unique.LegacySkill;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/**
 * The CI self-test of the 3.44.0 removals (id {@code skill.removed_ultimates}): on the booted server each of the six removed skill ids ({@link LegacyIds#REMOVED_SKILLS}) must still be in the skill
 * registry (an old world that saved one must load) and must be an inert {@link LegacySkill}, never a live skill; and Black Wings, which used to be reached through Vector Manipulation, must still be a
 * live skill. Registry reads only: no player, no world, no tick work.
 */
public final class RemovedSkillsSelfTests {
    private RemovedSkillsSelfTests() {}

    public static void register() {
        SelfTests.register("skill.removed_ultimates", RemovedSkillsSelfTests::check);
    }

    private static Result check(MinecraftServer server) {
        int stubs = 0;
        for (String path : LegacyIds.REMOVED_SKILLS) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path);
            ManasSkill skill = SkillAPI.getSkillRegistry().get(id);
            if (skill == null) return Result.fail(id + " is no longer registered: a world that saved it may fail to load");
            if (!(skill instanceof LegacySkill)) return Result.fail(id + " is still a live skill (" + skill.getClass().getSimpleName() + "), it must be an inert stub");
            stubs++;
        }
        ManasSkill wings = SkillAPI.getSkillRegistry().get(NUSkills.BLACK_WINGS.getId());
        if (wings == null || wings instanceof LegacySkill) return Result.fail("Black Wings must stay a live skill");
        return Result.ok(stubs + " removed skills are inert stubs, Black Wings is live");
    }
}
