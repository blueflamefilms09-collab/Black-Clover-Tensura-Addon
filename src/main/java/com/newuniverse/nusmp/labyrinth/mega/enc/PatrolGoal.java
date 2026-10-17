package com.newuniverse.nusmp.labyrinth.mega.enc;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Why: the thinking of a wandering boss is done by the Director every half second ({@link WandererMind}); this goal is only the muscle that
 * keeps the walk alive between two passes and, more importantly, claims the MOVE flag so the mob's own idle goals (a random stroll) do not wander
 * off with it. It runs exactly when the boss has an order to walk and no target: the moment the boss is given a target (a chase it perceives),
 * the goal lets go and the mob's own melee goals, which need the same flag, take over, which is why it keeps the mob's own attack and target
 * goals and replaces none of them. It re-issues the walk when the navigation has ended short of the goal. Goals only exist on mobs that use a goal
 * list: a Tensura brain ignores this, and the Director's pass steers it directly instead (same walk, same orders).
 */
final class PatrolGoal extends Goal {
    private final Mob mob;
    private final Wanderer boss;

    PatrolGoal(Mob mob, Wanderer boss) {
        this.mob = mob;
        this.boss = boss;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private boolean wanted() {
        WandererMind.Order o = boss.order;
        return o != null && o.move() && o.chase() == null && mob.getTarget() == null && mob.isAlive();
    }

    @Override
    public boolean canUse() { return wanted(); }

    @Override
    public boolean canContinueToUse() { return wanted(); }

    @Override
    public boolean isInterruptable() { return true; }

    @Override
    public void start() { WandererDirector.walk(mob, boss, true); }

    @Override
    public void tick() {
        if (mob.tickCount % 10 == 0) WandererDirector.walk(mob, boss, false);
    }
}
