package com.newuniverse.nusmp.multiverse;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the CI server smoke test of the mission stars (prefix {@code missions} in SMOKE_EXPECT). {@code missions.rules}: the stars per type, the bound, the filter. {@code missions.player}: on a stand-in
 * player a mission pays its stars once and a second finish of the same mission pays nothing, a goal pays at least a task, and the paid list is kept.
 */
public final class MissionStarsSelfTests {
    private MissionStarsSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("missions.rules", MissionStarsSelfTests::rules);
        SelfTests.register("missions.player", MissionStarsSelfTests::player);
    }

    private static Result rules(MinecraftServer server) {
        if (MissionStarRules.stars("TASK", 1, 2, 5) != 1 || MissionStarRules.stars("goal", 1, 2, 5) != 2 || MissionStarRules.stars("CHALLENGE", 1, 2, 5) != 5 || MissionStarRules.stars("weird", 1, 2, 5) != 1
                || MissionStarRules.stars("challenge", 1, 2, 5000) != MissionStarRules.MAX_STARS || MissionStarRules.stars("task", -3, 2, 5) != 0) return Result.fail("the mission star numbers moved");
        List<String> ex = List.of("root", "magic/");
        if (!MissionStarRules.counts("nusmp", "bosses/x", true, ex) || MissionStarRules.counts("minecraft", "story/root", true, ex) || MissionStarRules.counts("nusmp", "root", true, ex)
                || MissionStarRules.counts("nusmp", "magic/cast", true, ex) || MissionStarRules.counts("nusmp", "bosses/x", false, ex)) return Result.fail("the mission filter moved");
        if (MultiverseConfig.get(MultiverseConfig.MISSION_CHALLENGE) > MissionStarRules.MAX_STARS) return Result.fail("a mission setting passed its hard bound");
        return Result.ok("stars per type, the bound and the filter hold");
    }

    private static Result player(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "mission1", at.x, at.y, at.z);
        try {
            if (!MultiverseConfig.get(MultiverseConfig.MISSION_STARS)) return Result.ok("mission stars are switched off in this config; not exercised");
            String a = "nusmp:selftest/a" + server.getTickCount(), b = "nusmp:selftest/b" + server.getTickCount();
            int g0 = MultiverseProfile.goldStars(p);
            int first = MissionStars.award(p, a, "task", Component.literal("test"));
            if (MultiverseConfig.get(MultiverseConfig.MISSION_TASK) > 0 && (first <= 0 || MultiverseProfile.goldStars(p) != g0 + first)) return Result.fail("a finished mission paid no gold stars");
            if (!MultiverseProfile.missionDone(p, a)) return Result.fail("a paid mission was not remembered");
            int g1 = MultiverseProfile.goldStars(p);
            if (MissionStars.award(p, a, "task", null) != 0 || MultiverseProfile.goldStars(p) != g1) return Result.fail("the same mission paid twice");
            int goal = MissionStars.award(p, b, "goal", null);
            if (MultiverseConfig.get(MultiverseConfig.MISSION_GOAL) >= MultiverseConfig.get(MultiverseConfig.MISSION_TASK) && goal < first) return Result.fail("a goal paid less than a task");
            return Result.ok("a mission pays once (" + first + " for a task, " + goal + " for a goal) and is remembered");
        } catch (RuntimeException ex) {
            return Result.fail("mission stars threw " + ex);
        }
    }
}
