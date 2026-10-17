package com.newuniverse.nusmp.labyrinth.mega.eco;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Why: the two global events that make the turf wars work without touching one monster class. (1) {@code EntityJoinLevelEvent}: when a mob
 * with a faction enters the Mega-Dungeon, the {@link TurfWarGoal} is added to its target selector at priority 3. A goal, not a rewrite of the
 * mob's brain: every vanilla and most modded hostile mobs choose targets with goals, a goal at priority 3 sits below the "hit me, I hit back" goal
 * and the "nearest player" goal so a player (or a blow) always wins, and nothing the mob already does is removed. The selector field is protected
 * and the mod has no access transformer (the owner's file list for this work does not include the build files), so it is read once by reflection
 * under its official name, the same way {@code soul.PaladinGoals} does; {@code addGoal} itself is the ordinary public call. If the field cannot be
 * read, or a mod replaced the selector with something that refuses goals, one warning is logged and the mob is simply left as it was. (2)
 * {@code LivingChangeTargetEvent}: a wild mob about to target a wild mob of its own faction or an ally has that change cancelled, which stops a
 * skeleton's stray arrow from starting a war inside the undead; a player target is never touched, and neither is a servant or a boss on either side.
 */
public final class TurfWars {
    private TurfWars() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final Field TARGETS = field("targetSelector");
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    /** The priority of the turf-war goal: below the hurt-by goal (1) and the nearest-player goal (2) of the hostile mobs. */
    static final int PRIORITY = 3;

    private static Field field(String name) {
        try {
            Field f = Mob.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unreadable) {
            return null;
        }
    }

    private static GoalSelector targets(Mob m) {
        if (TARGETS == null) return null;
        try {
            return (GoalSelector) TARGETS.get(m);
        } catch (ReflectiveOperationException | RuntimeException bad) {
            return null;
        }
    }

    /** Logs the first failure of the turf wars (one line for the whole session; the rest would only repeat it). */
    static void warnOnce(String what, Throwable t) {
        if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] Mega-Dungeon turf wars: {} ({}); they are off for the mobs this affects", what, t == null ? "no detail" : t.toString());
    }

    /** Does the mob carry the turf-war goal? False too when its selector cannot be read. (For the admin command and the in-game self-test.) */
    static boolean hasGoal(Mob mob) {
        GoalSelector selector = targets(mob);
        if (selector == null) return false;
        for (WrappedGoal w : selector.getAvailableGoals()) if (w.getGoal() instanceof TurfWarGoal) return true;
        return false;
    }

    /** Adds the goal to a mob that has none yet; true when the mob has one afterwards. Safe to call twice. */
    static boolean install(Mob mob) {
        GoalSelector selector = targets(mob);
        if (selector == null) {
            warnOnce("the target selector of " + mob.getType().getDescriptionId() + " could not be read", null);
            return false;
        }
        for (WrappedGoal w : selector.getAvailableGoals()) if (w.getGoal() instanceof TurfWarGoal) return true;
        selector.addGoal(PRIORITY, new TurfWarGoal(mob));
        return true;
    }

    public static void onJoin(EntityJoinLevelEvent e) {
        try {
            Level level = e.getLevel();
            if (level.isClientSide() || !(e.getEntity() instanceof Mob mob) || !Labyrinth.inMega(level)) return;
            if (!EcoConfig.get(EcoConfig.TURF_WARS) || FactionRegistry.factionOf(mob.getType()) == null || !TurfEligibility.wild(mob)) return;
            install(mob);
        } catch (RuntimeException | LinkageError ex) {
            warnOnce("a mob could not be given the goal", ex);
        }
    }

    public static void onChangeTarget(LivingChangeTargetEvent e) {
        try {
            LivingEntity next = e.getNewAboutToBeSetTarget();
            if (next == null || next instanceof Player || !(e.getEntity() instanceof Mob mob) || !(next instanceof Mob other)) return;
            if (mob.level().isClientSide() || !Labyrinth.inMega(mob.level()) || !EcoConfig.get(EcoConfig.TURF_WARS)) return;
            String a = FactionRegistry.factionOf(mob.getType()), b = FactionRegistry.factionOf(other.getType());
            if (a == null || b == null || !FactionRegistry.graph().sameOrAllied(a, b)) return;
            if (TurfEligibility.wild(mob) && TurfEligibility.wild(other)) e.setCanceled(true);
        } catch (RuntimeException | LinkageError ex) {
            warnOnce("the target guard failed", ex);
        }
    }
}
