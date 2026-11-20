package com.newuniverse.nusmp.boss;

import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.ArcSpawner;
import com.newuniverse.nusmp.boss.budget.BossBudget;
import com.newuniverse.nusmp.boss.budget.BossBudgetConfig;
import com.newuniverse.nusmp.boss.budget.BudgetPolicy;
import com.newuniverse.nusmp.boss.data.BossData;
import com.newuniverse.nusmp.boss.data.BossDataTable;
import com.newuniverse.nusmp.boss.god.ArenaScaling;
import com.newuniverse.nusmp.boss.rules.BossRulesConfig;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

/**
 * Why: the Leon study fixes cannot be watched in a game here, so the CI server smoke test does. Key {@code boss.leon1} (fixes 1 and 5): the damage cap of a Black Clover boss is taken from its base health, and a
 * framework boss is scaled by 1 and by 4 fighters and put back (the table, the kept bar, the combined cap). Key {@code boss.leon3} (fix 9): a data override of a boss's health is applied, replaced and taken off again,
 * and a bad file is refused. Every boss is summoned through the normal door and removed at the end; the loaded data files are swapped back. Server safe, no client class.
 */
final class BossLeonSelfTests {
    private BossLeonSelfTests() {}

    static void register() {
        SelfTests.register("boss.leon1", BossLeonSelfTests::leon1);
        SelfTests.register("boss.leon3", BossLeonSelfTests::leon3);
    }

    /** A summonable roster boss: Black Clover or not; the others skip the Tier List (a data row that keeps its numbers). */
    private static ArcCharacter pick(boolean blackClover) {
        for (ArcCharacter c : ArcRoster.bosses())
            if (BossStatMath.applies(c.source(), true) == blackClover && (blackClover || c.saga() != ArcSaga.TIER_LIST) && ArcSpawner.typeOf(c.id()) != null) return c;
        return null;
    }

    private static ArcBoss summon(ServerLevel level, ArcCharacter c, int dx) {
        BlockPos at = level.getSharedSpawnPos();
        ArcSpawner.Result r = ArcSpawner.summon(level, c, Vec3.atBottomCenterOf(at.above(2)).add(dx, 0, 0), 0f, List.of(), true);
        return r.outcome() == ArcSpawner.Outcome.SPAWNED ? r.boss() : null;
    }

    private static boolean near(double a, double b) { return Math.abs(a - b) < 0.5; }

    private static Result leon1(MinecraftServer server) {
        // the pure numbers first
        if (BudgetPolicy.capBasis(300f, 3.0, true) != 100f || BudgetPolicy.capBasis(300f, 3.0, false) != 300f) return Result.fail("capBasis is wrong");
        if (Math.abs(ArenaScaling.healthCombined(4, 7.5) - 8.0 / 7.5) > 1e-9 || ArenaScaling.healthCombined(1, 7.5) != 0.70) return Result.fail("the combined cap is wrong");
        ServerLevel level = server.overworld();
        if (!level.hasChunkAt(level.getSharedSpawnPos())) return Result.ok("pure checks passed; skipped the bosses: the spawn chunk is not loaded");
        ArcCharacter bc = pick(true), other = pick(false);
        ArcBoss a = null, b = null;
        try {
            String note = "";
            double mult = BossStatMath.clamp(BossRulesConfig.blackCloverStatMultiplier());
            if (bc != null && (a = summon(level, bc, 6)) != null) {
                float max = a.getMaxHealth();
                double fac = BossStatBuff.healthFactor(a);
                if (mult > 1.0 && Math.abs(fac - mult) > 1e-9) return Result.fail(bc.id() + " health factor " + fac + " is not x" + mult);
                double scale = BudgetPolicy.phaseScale(a.budgetStage(), a.budgetFinalStage(), BossBudgetConfig.finalScale());
                float want = BudgetPolicy.capPerSecond(BossBudgetConfig.baseHealth() ? (float) (max / fac) : max, BossBudgetConfig.fraction(), BossBudgetConfig.minimum(), scale);
                float got = BossBudget.capNow(a, a);
                if (Math.abs(got - want) > 0.01f) return Result.fail(bc.id() + " cap " + got + " is not " + want);
                note = bc.id() + " cap " + (int) got + "/s on " + (int) max + " health";
            }
            if (other != null && (b = summon(level, other, -6)) != null) {
                float max0 = b.getMaxHealth();
                boolean on = BossRulesConfig.partyScaling();
                b.applyPartyScale(1);
                if (!near(b.getMaxHealth(), on ? max0 * ArenaScaling.health(1) : max0)) return Result.fail(other.id() + " solo health " + b.getMaxHealth() + " of " + max0);
                if (!near(b.getHealth(), b.getMaxHealth())) return Result.fail(other.id() + " lost its full bar when scaled: " + b.getHealth());
                b.applyPartyScale(4);
                if (!near(b.getMaxHealth(), on ? max0 * ArenaScaling.health(4) : max0)) return Result.fail(other.id() + " squad health " + b.getMaxHealth() + " of " + max0);
                b.clearPartyScale();
                if (!near(b.getMaxHealth(), max0) || b.partyDamage() != 1.0) return Result.fail(other.id() + " was not put back");
                note += (note.isEmpty() ? "" : "; ") + other.id() + " x" + ArenaScaling.health(1) + "/x" + ArenaScaling.health(4) + (on ? "" : " (setting off: untouched)");
            }
            return Result.ok(note.isEmpty() ? "pure checks passed, no boss could be summoned" : note);
        } finally {
            if (a != null) a.discard();
            if (b != null) b.discard();
        }
    }

    private static Result leon3(MinecraftServer server) {
        if (BossDataTable.clamp(5e9, BossDataTable.HEALTH_MIN, BossDataTable.HEALTH_MAX) != BossDataTable.HEALTH_MAX) return Result.fail("clamp is wrong");
        try {
            BossDataTable.parse(com.google.gson.JsonParser.parseString("{\"health\":\"big\"}").getAsJsonObject());
            return Result.fail("a text health was accepted");
        } catch (IllegalArgumentException expected) { /* refused, as it must be */ }
        ServerLevel level = server.overworld();
        if (!level.hasChunkAt(level.getSharedSpawnPos())) return Result.ok("pure checks passed; skipped the boss: the spawn chunk is not loaded");
        ArcCharacter c = pick(false);
        if (c == null) return Result.ok("pure checks passed; no boss can be summoned");
        ArcBoss a = null;
        Map<String, BossDataTable.Override> old = null;
        try {
            a = summon(level, c, 6);
            if (a == null) return Result.ok("pure checks passed; " + c.id() + " was not summoned");
            String id = BossEntities.bossId(a);
            float max0 = a.getMaxHealth();
            old = BossData.install(Map.of(id, new BossDataTable.Override(max0 * 2.0, null, null, null, 0.5)));
            BossData.apply(a, id);
            boolean on = BossRulesConfig.bossData();
            if (!near(a.getMaxHealth(), on ? max0 * 2.0 : max0)) return Result.fail(id + " health " + a.getMaxHealth() + " after the override of " + max0 * 2.0);
            if (!near(a.getHealth(), a.getMaxHealth())) return Result.fail(id + " lost its full bar: " + a.getHealth());
            if (on && a.moveCooldownScale() != 0.5) return Result.fail(id + " cooldown scale " + a.moveCooldownScale());
            BossData.install(Map.of());
            BossData.apply(a, id);
            if (!near(a.getMaxHealth(), max0) || a.moveCooldownScale() != 1.0) return Result.fail(id + " did not return to its Java numbers: " + a.getMaxHealth());
            return Result.ok(id + " " + (int) max0 + " -> " + (on ? (int) (max0 * 2) : (int) max0) + " -> " + (int) max0 + (on ? "" : " (setting off: files ignored)"));
        } finally {
            if (old != null) BossData.install(old);
            if (a != null) a.discard();
        }
    }
}
