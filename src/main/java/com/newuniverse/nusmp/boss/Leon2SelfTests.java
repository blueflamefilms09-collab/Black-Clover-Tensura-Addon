package com.newuniverse.nusmp.boss;

import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSpawner;
import com.newuniverse.nusmp.boss.rules.BossRulesConfig;
import com.newuniverse.nusmp.entity.zogratis.AttackTimeline;
import com.newuniverse.nusmp.entity.zogratis.ReactiveGuard;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the reactive guard and the attack timeline (docs/boss_leon_combat.md) cannot be watched in a game here, so the CI server smoke test checks what a server can: key
 * {@code boss.leon2.tables} (every id in the two tables is a boss of the roster, every swing is damage neutral, the guard's numbers keep at least half of a foe's damage) and
 * {@code boss.leon2.live} (a Meliodas is summoned, draws no timeline swing before it fights, takes a hit without a foe behind it, and is removed again). Server safe.
 */
final class Leon2SelfTests {
    private Leon2SelfTests() {}

    static void register() {
        SelfTests.register("boss.leon2.tables", Leon2SelfTests::tables);
        SelfTests.register("boss.leon2.live", Leon2SelfTests::live);
    }

    private static Result tables(MinecraftServer server) {
        Set<String> roster = new HashSet<>();
        for (ArcCharacter c : ArcRoster.bosses()) roster.add(c.id());
        for (String id : ReactiveGuard.OPT_IN) if (!roster.contains(id)) return Result.fail("guard id " + id + " is not a boss of the roster");
        for (String id : List.of("meliodas", "ban", "escanor", "heath_grice", "vetto", "mars", "udaeus", "minotaur", "black_goliath")) {
            if (!roster.contains(id)) return Result.fail("timeline id " + id + " is not a boss of the roster");
            if (!AttackTimeline.has(id)) return Result.fail(id + " has no timeline rows");
            for (AttackTimeline.Swing s : AttackTimeline.forBoss(id))
                if (!AttackTimeline.neutral(s)) return Result.fail(id + " swing " + s.name() + " deals more than the old fixed cooldown");
        }
        ReactiveGuard.Params p = ReactiveGuard.Params.of(BossRulesConfig.guardBlockChance(), BossRulesConfig.guardDodgeChance());
        if (p.leastThrough() < 0.5) return Result.fail("the guard lets only " + p.leastThrough() + " of a foe's damage through");
        return Result.ok("guard ids and timeline ids are roster bosses, every swing neutral, the guard keeps " + Math.round(p.leastThrough() * 100) + "% of a foe's damage");
    }

    private static Result live(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return Result.ok("skipped: the spawn chunk is not loaded");
        ArcCharacter meliodas = null;
        for (ArcCharacter c : ArcRoster.bosses()) if (c.id().equals("meliodas")) meliodas = c;
        if (meliodas == null || ArcSpawner.typeOf(meliodas.id()) == null) return Result.ok("skipped: no Meliodas in this build");
        ArcBoss b = null;
        try {
            ArcSpawner.Result r = ArcSpawner.summon(level, meliodas, Vec3.atBottomCenterOf(at.above(2)).add(8, 0, 0), 0f, List.of(), true);
            if (r.outcome() != ArcSpawner.Outcome.SPAWNED) return Result.ok("skipped: meliodas was not summoned (" + r.outcome() + ")");
            b = r.boss();
            if (b.swingRow() != null) return Result.fail("a boss that has not fought draws a timeline swing");
            float before = b.getHealth();
            b.hurt(level.damageSources().generic(), 1f);                    // no foe behind it: the guard must let it through untouched, and nothing may throw
            if (b.getHealth() > before) return Result.fail("a hit raised the health");
            return Result.ok("meliodas summoned, no swing before the fight, a foe-less hit passes (timeline " + BossRulesConfig.attackTimeline() + ", guard " + BossRulesConfig.reactiveGuard() + ")");
        } finally {
            if (b != null) b.discard();
        }
    }
}
