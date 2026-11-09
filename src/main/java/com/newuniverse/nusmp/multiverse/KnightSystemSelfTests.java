package com.newuniverse.nusmp.multiverse;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Why: the CI server smoke test of the Magic Knight system (3.68.0, prefix {@code knights} in SMOKE_EXPECT). {@code knights.rules}: the nine squad styles, the Mission Board numbers and the Star Awards numbers.
 * {@code knights.player}: on a stand-in player the board offers three missions, one can be taken, the kills pay its gold stars and end it, an abandoned mission is dropped; a killed villager costs a black star once
 * per cool-down. Nothing is spawned into the world (the villager is created and never added).
 */
public final class KnightSystemSelfTests {
    private KnightSystemSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("knights.rules", KnightSystemSelfTests::rules);
        SelfTests.register("knights.player", KnightSystemSelfTests::player);
    }

    private static Result rules(MinecraftServer server) {
        if (SquadStyle.values().length != 9 || SquadStyle.byId("black_bull") != SquadStyle.BLACK_BULL || SquadStyle.byId("Golden Dawn") != SquadStyle.GOLDEN_DAWN) return Result.fail("the squad styles moved");
        var kinds = MissionBoardRules.offerKinds(MissionBoardRules.seed(1L, 2L, 3));
        if (kinds.length != MissionBoardRules.OFFERS || kinds[0] == kinds[1] || kinds[1] == kinds[2] || kinds[0] == kinds[2]) return Result.fail("the board offers are not three different kinds");
        for (var k : MissionBoardRules.Kind.values()) {
            int n = MissionBoardRules.need(k, 14), r = MissionBoardRules.reward(k, n, 100);
            if (n > MissionBoardRules.MAX_NEED || n <= MissionBoardRules.need(k, 0) || r < 1 || r > MissionBoardRules.MAX_REWARD) return Result.fail("a mission number passed its bound for " + k);
        }
        if (StarAwardRules.reward(1, 5, 3, 1) != 5 || StarAwardRules.reward(4, 5, 3, 1) != 0 || !StarAwardRules.due(1, 2) || StarAwardRules.due(-1, 2)) return Result.fail("the Star Awards numbers moved");
        return Result.ok("nine squad styles, the board offers and bounds, and the Star Awards numbers hold");
    }

    private static Result player(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "knight1", at.x, at.y, at.z);
        try {
            if (!MultiverseConfig.get(MultiverseConfig.BOARD_ENABLED)) return Result.ok("the board is switched off in this config; not exercised");
            List<MissionBoard.Offer> offers = MissionBoard.offers(p);
            if (offers.size() != MissionBoardRules.OFFERS) return Result.fail("the board did not offer three missions");
            MissionBoard.abandon(p);
            MissionBoard.Offer o = offers.get(0);
            MissionBoard.take(p, 1);
            if (MissionBoard.status(p).get(0).getString().startsWith("You have no mission")) return Result.fail("a taken mission is not active");
            if (!MissionBoard.take(p, 2).getString().startsWith("Finish or abandon")) return Result.fail("a second mission could be taken at once");
            if (MissionBoard.credit(p, EnumSet.noneOf(MissionBoardRules.Kind.class)) != 0) return Result.fail("a kill of no kind counted");
            int g0 = MultiverseProfile.goldStars(p), paid = 0;
            for (int i = 0; i < o.need() && paid == 0; i++) paid = MissionBoard.credit(p, EnumSet.of(o.kind()));
            if (paid <= 0 || MultiverseProfile.goldStars(p) != g0 + paid) return Result.fail("finishing the mission paid no gold stars");
            if (MissionBoard.credit(p, EnumSet.of(o.kind())) != 0) return Result.fail("a finished mission still counted kills");
            MissionBoard.take(p, 2);
            MissionBoard.abandon(p);
            if (!MissionBoard.status(p).get(0).getString().startsWith("You have no mission")) return Result.fail("an abandoned mission is still active");

            LivingEntity villager = EntityType.VILLAGER.create(level);
            if (villager == null) return Result.fail("could not make a test villager");
            int b0 = MultiverseProfile.blackStars(p);
            int first = KnightConduct.apply(p, villager), second = KnightConduct.apply(p, villager);
            if (MultiverseConfig.get(MultiverseConfig.CONDUCT_VILLAGER) > 0) {
                if (first <= 0 || MultiverseProfile.blackStars(p) != b0 + first) return Result.fail("a killed villager cost no black star");
                if (MultiverseConfig.get(MultiverseConfig.CONDUCT_COOLDOWN) > 0 && second != 0) return Result.fail("a second villager kill inside the cool-down cost a black star again");
            }
            return Result.ok("board: three offers, take, kills pay " + paid + " gold stars, abandon; conduct: villager " + first + " black star(s), then none in the cool-down");
        } catch (RuntimeException ex) {
            return Result.fail("the knight system threw " + ex);
        }
    }
}
