package com.newuniverse.nusmp.mobfix;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the crash (an Illusioner casting blindness with no target) can not be seen in a game by its author, so the CI server smoke test makes it happen on purpose: an Illusioner and an Evoker that are not
 * in the world get the guard, start their spell with a target, lose the target, and are ticked past the whole cast warm-up. Without the guard the vanilla goal throws; with it the cast just stops. Key
 * {@code mobfix.spellcaster}. Nothing is added to the world (no entity is spawned), so there is nothing to clean up.
 */
final class SpellcasterGuardSelfTests {
    private SpellcasterGuardSelfTests() {}

    private static boolean registered;

    static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("mobfix.spellcaster", SpellcasterGuardSelfTests::run);
    }

    private static Goal guarded(GoalSelector g) {
        for (WrappedGoal w : g.getAvailableGoals()) if (w.getGoal() instanceof SpellcasterGuard.Guarded x) return x;
        return null;
    }

    private static boolean rawLeft(GoalSelector g) {
        for (WrappedGoal w : g.getAvailableGoals()) if (SpellcasterGuardRules.needsTarget(w.getGoal().getClass().getName())) return true;
        return false;
    }

    private static Result run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer foe = SelfTests.standIn(level, "mobfix", at.x + 3, at.y, at.z);
        Illusioner ill = new Illusioner(EntityType.ILLUSIONER, level);
        Evoker evo = new Evoker(EntityType.EVOKER, level);
        ill.setPos(at.x, at.y, at.z);
        evo.setPos(at.x, at.y, at.z);
        for (SpellcasterIllager m : new SpellcasterIllager[] {ill, evo}) {
            String who = m.getClass().getSimpleName();
            GoalSelector g = SpellcasterGuard.goals(m);
            if (g == null) return Result.fail(who + ": the goal list could not be read (the field name changed?), so the guard would do nothing");
            if (!rawLeft(g)) return Result.fail(who + ": no target-reading spell goal found (the vanilla class names changed?)");
            int n = SpellcasterGuard.install(m);
            if (n < 1 || rawLeft(g)) return Result.fail(who + ": the spell goal was not wrapped (" + n + ")");
            if (SpellcasterGuard.install(m) != 0) return Result.fail(who + ": wrapping twice wrapped again");
            Goal guard = guarded(g);
            if (guard == null) return Result.fail(who + ": the wrapper is not in the goal list");
            m.setTarget(null);
            if (guard.canUse() || guard.canContinueToUse()) return Result.fail(who + ": a spell may start or go on with no target");

            // the crash: the spell starts with a target, the target is taken away, the cast warm-up runs out
            m.setTarget(foe);
            guard.start();
            m.setTarget(null);
            try {
                for (int i = 0; i < 60; i++) guard.tick();
            } catch (RuntimeException boom) {
                return Result.fail(who + ": ticking the spell with no target threw " + boom);
            } finally {
                guard.stop();
            }
        }
        return Result.ok("an Illusioner and an Evoker: the target-reading spell goals are wrapped once, refuse to start or go on without a living target, and a cast whose target is taken away stops instead of throwing");
    }
}
