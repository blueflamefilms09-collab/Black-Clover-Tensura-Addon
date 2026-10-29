package com.newuniverse.nusmp.arc.tier;

import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.ArcSpawner;
import com.newuniverse.nusmp.boss.BossRegistry;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Tier List bosses cannot be played by their author, so the CI server smoke test checks them in the real game ({@code /multiverse mega selftest}, keys
 * {@code tier.*}): every row is in the roster and the boss registry, has an entity type with attributes, obeys the house rules, builds its moves; and one of them is
 * really summoned, sized, named, dressed in its moves and removed again. Cost: one entity, made and discarded in the same tick.
 */
public final class TierSelfTests {
    private TierSelfTests() {}

    public static void register() {
        SelfTests.register("tier.roster", TierSelfTests::roster);
        SelfTests.register("tier.moves", TierSelfTests::moves);
        SelfTests.register("tier.spawn", TierSelfTests::spawn);
    }

    private static SelfTests.Result roster(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        for (TierRow row : TierBosses.all()) {
            ArcCharacter c = ArcRoster.byId(row.id());
            if (c == null) { bad.add(row.id() + " is not in the roster"); continue; }
            if (c.saga() != ArcSaga.TIER_LIST) bad.add(row.id() + " is not of the Tier List saga");
            if (!BossRegistry.known(row.id())) bad.add(row.id() + " is not in the boss registry");
            var type = ArcSpawner.typeOf(row.id());
            if (type == null) bad.add(row.id() + " has no entity type");
            else if (DefaultAttributes.getSupplier((net.minecraft.world.entity.EntityType) type) == null) bad.add(row.id() + " has no attributes");
            bad.addAll(TierRules.problems(row));
        }
        return bad.isEmpty() ? SelfTests.Result.ok(TierBosses.all().size() + " light bosses are consistent")
                : SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(6, bad.size()))));
    }

    private static SelfTests.Result moves(MinecraftServer server) {
        int total = 0;
        for (TierRow row : TierBosses.all()) {
            List<Move> built = TierMoves.build(row);
            if (built.size() != row.moves().size()) return SelfTests.Result.fail(row.id() + " builds " + built.size() + " of " + row.moves().size() + " moves");
            total += built.size();
        }
        return SelfTests.Result.ok(total + " moves build");
    }

    private static SelfTests.Result spawn(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return SelfTests.Result.ok("skipped: the spawn chunk is not loaded");
        TierRow row = TierBosses.all().get(0);
        ArcBoss boss = null;
        try {
            ArcSpawner.Result r = ArcSpawner.summon(level, row.character(), Vec3.atBottomCenterOf(at.above(2)).add(6, 0, 0), 0f, List.of(), true);
            if (r.outcome() != ArcSpawner.Outcome.SPAWNED) return SelfTests.Result.fail("summoning " + row.id() + " gave " + r.outcome());
            boss = r.boss();
            if (!(boss instanceof TierBoss tb)) return SelfTests.Result.fail(row.id() + " is not a TierBoss");
            int built = tb.moves().size();
            if (built != row.moves().size()) return SelfTests.Result.fail(row.id() + " has " + built + " moves");
            if (!row.character().name().equals(boss.bossName())) return SelfTests.Result.fail("the name is " + boss.bossName());
            if (boss.getMaxHealth() < row.letter().health() * 0.5) return SelfTests.Result.fail("max health " + boss.getMaxHealth());
            return SelfTests.Result.ok(row.id() + " summoned with " + built + " moves and " + (int) boss.getMaxHealth() + " health");
        } finally {
            if (boss != null) boss.discard();
        }
    }
}
