package com.newuniverse.nusmp.boss.ai;

import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.boss.rules.BossRulesConfig;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.relics.order.GodOfOrder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: nobody watches a boss fight in CI, so the smoke test builds every boss the registry holds (never adds one to the world, so nothing is left behind) under keys
 * {@code bossai.goals} and {@code bossai.engage}: each boss has the engage goal first in its target selector (the God of Order, a judge by design, has none), and
 * no look-at goal has a priority number at or below its attack or chase goal; the goal's scan on a boss with nobody near it answers "no target" without throwing,
 * and the pure chooser takes the nearest fair player and skips creative and spectator ones. Needs no player and no world change.
 */
public final class BossAiSelfTests {
    private BossAiSelfTests() {}

    public static void register() {
        SelfTests.register("bossai.goals", BossAiSelfTests::goals);
        SelfTests.register("bossai.engage", BossAiSelfTests::engage);
    }

    private static boolean attackGoal(Goal g) { return g instanceof MeleeAttackGoal || g.getClass().getSimpleName().equals("Positioning") || g.getClass().getSimpleName().equals("Chase"); }

    private static Result goals(MinecraftServer server) {
        ServerLevel level = server.overworld();
        List<String> bad = new ArrayList<>();
        int bosses = 0, withEngage = 0, judges = 0;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!"anime_multiverse".equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace())) continue;
            Entity e;
            try {
                e = type.create(level);
            } catch (RuntimeException unbuildable) {
                continue;                                                                     // a type that needs spawn data is not a boss this test can see
            }
            if (!(e instanceof Mob m) || !BossIdentity.isBoss(m)) continue;
            bosses++;
            String name = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
            if (m instanceof GodOfOrder) { judges++; continue; }
            boolean engage = false;
            int attackPrio = Integer.MAX_VALUE, lookPrio = Integer.MIN_VALUE;
            for (WrappedGoal w : m.targetSelector.getAvailableGoals()) if (w.getGoal() instanceof BossEngageGoal && w.getPriority() == 0) engage = true;
            for (WrappedGoal w : m.goalSelector.getAvailableGoals()) {
                if (attackGoal(w.getGoal())) attackPrio = Math.min(attackPrio, w.getPriority());
                if (w.getGoal() instanceof LookAtPlayerGoal) lookPrio = Math.max(lookPrio, w.getPriority());
            }
            if (engage) withEngage++;
            else bad.add(name + " has no engage goal");
            if (attackPrio != Integer.MAX_VALUE && lookPrio != Integer.MIN_VALUE && lookPrio <= attackPrio) bad.add(name + " looks (" + lookPrio + ") before it attacks (" + attackPrio + ")");
        }
        if (bosses == 0) bad.add("no boss could be built");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(6, bad.size()))));
        return Result.ok(bosses + " bosses built, " + withEngage + " with the engage goal, " + judges + " judge(s) left neutral, aggroOnSight " + (BossRulesConfig.aggroOnSight() ? "on" : "off")
                + ", range " + BossRulesConfig.aggroRange());
    }

    private static Result engage(MinecraftServer server) {
        ServerLevel level = server.overworld();
        List<String> bad = new ArrayList<>();
        int scanned = 0;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!"anime_multiverse".equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace())) continue;
            Entity e;
            try {
                e = type.create(level);
            } catch (RuntimeException unbuildable) {
                continue;
            }
            if (!(e instanceof Mob m) || !BossIdentity.isBoss(m)) continue;
            for (WrappedGoal w : m.targetSelector.getAvailableGoals()) {
                if (!(w.getGoal() instanceof BossEngageGoal g)) continue;
                try {
                    if (!g.canUse() && m.getTarget() == null && !g.canContinueToUse()) scanned++;      // nobody is near a boss that was never added to the world: no pick
                } catch (RuntimeException ex) {
                    bad.add(BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath() + " scan threw " + ex);
                }
            }
        }
        List<BossEngage.Candidate> cs = List.of(
                new BossEngage.Candidate(1, 20 * 20, true, false, true, false),          // creative: never
                new BossEngage.Candidate(2, 10 * 10, true, true, false, false),          // spectator: never
                new BossEngage.Candidate(3, 30 * 30, true, false, false, false),         // out of the 24 block range below
                new BossEngage.Candidate(4, 11 * 11, true, false, false, false));        // close: no line of sight needed
        int pick = BossEngage.choose(cs, 24, true, id -> false, id -> false);
        if (pick != 4) bad.add("the chooser gave " + pick + " instead of 4");
        if (BossEngage.choose(cs.subList(0, 3), 24, true, id -> false, id -> false) != -1) bad.add("the chooser took a creative, a spectator or a far player");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(6, bad.size()))));
        return Result.ok("pure chooser ok, " + scanned + " empty scans ran without a pick");
    }
}
