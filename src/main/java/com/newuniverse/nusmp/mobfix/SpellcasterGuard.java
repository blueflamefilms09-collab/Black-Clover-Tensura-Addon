package com.newuniverse.nusmp.mobfix;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Why: an Illusioner in a dungeon "cast blindness on thin air" and the server crashed with a NullPointerException (3.65.1). In vanilla the blindness spell ({@code IllusionerBlindnessSpellGoal}) and the
 * Evoker's fang spell read {@code getTarget()} at the moment they cast. A goal that has to tick every tick is only asked "do you still have a target?" on every second tick, so a target taken away in
 * between (this mod has many spells that make mobs forget the caster) reaches the cast as null. This puts a thin wrapper round those two goals when the spellcaster enters the world: the wrapper asks
 * for a living target before the goal starts, goes on or ticks, and stops the goal instead of letting it cast at nothing; a NullPointerException from inside the goal is also caught, once logged, and
 * ends the cast. Nothing else about the mob changes (same priority, same flags, same sounds, same spell). Cost: two comparisons per tick for an Illusioner or Evoker that is casting; nothing for any other
 * mob; the goal list is read once, when the spellcaster joins. The goal list of a {@link Mob} is protected and the mod has no access transformer, so it is read by reflection once (like the Paladin
 * goals); if that ever fails the self-test {@code mobfix.spellcaster} says so.
 */
public final class SpellcasterGuard {
    private SpellcasterGuard() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final Field GOALS = field("goalSelector");
    private static boolean warned;

    private static Field field(String name) {
        try {
            Field f = Mob.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unreadable) {
            return null;
        }
    }

    /** The single entry point: the join event. */
    public static void register(IEventBus gameBus) {
        gameBus.addListener(SpellcasterGuard::onJoin);
        SpellcasterGuardSelfTests.register();
    }

    private static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof SpellcasterIllager caster)) return;
        install(caster);
    }

    static GoalSelector goals(Mob m) {
        if (GOALS == null) return null;
        try { return (GoalSelector) GOALS.get(m); } catch (ReflectiveOperationException | RuntimeException bad) { return null; }
    }

    /** Wraps the target-reading spell goals of this mob (safe to call twice). Returns how many goals were wrapped. */
    static int install(Mob m) {
        GoalSelector g = goals(m);
        if (g == null) return 0;
        List<WrappedGoal> todo = new ArrayList<>();
        for (WrappedGoal w : g.getAvailableGoals()) if (!(w.getGoal() instanceof Guarded) && SpellcasterGuardRules.needsTarget(w.getGoal().getClass().getName())) todo.add(w);
        for (WrappedGoal w : todo) {
            g.removeGoal(w.getGoal());
            g.addGoal(w.getPriority(), new Guarded(m, w.getGoal()));
        }
        return todo.size();
    }

    /** The vanilla goal, asked for a living target first. */
    static final class Guarded extends Goal {
        private final Mob mob;
        final Goal inner;

        Guarded(Mob mob, Goal inner) {
            this.mob = mob;
            this.inner = inner;
            setFlags(inner.getFlags());
        }

        private boolean hasTarget() {
            LivingEntity t = mob.getTarget();
            return SpellcasterGuardRules.mayCast(t != null, t != null && t.isAlive());
        }

        private boolean safe(BooleanSupplier ask) {
            try { return ask.getAsBoolean(); } catch (NullPointerException gone) { note(gone); return false; }
        }

        @Override public boolean canUse() { return hasTarget() && safe(inner::canUse); }

        @Override public boolean canContinueToUse() { return hasTarget() && safe(inner::canContinueToUse); }

        @Override public boolean isInterruptable() { return inner.isInterruptable(); }

        @Override public boolean requiresUpdateEveryTick() { return inner.requiresUpdateEveryTick(); }

        @Override public void start() { inner.start(); }

        @Override public void stop() { inner.stop(); }

        @Override public void tick() {
            if (!hasTarget()) {
                inner.stop();
                return;
            }
            try {
                inner.tick();
            } catch (NullPointerException gone) {
                note(gone);
                inner.stop();
            }
        }
    }

    private static void note(NullPointerException gone) {
        if (warned) return;
        warned = true;
        LOG.warn("[nusmp] A spellcaster's spell goal lost its target while casting; the cast was stopped instead of crashing the server (logged once)", gone);
    }
}
