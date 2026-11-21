package com.newuniverse.nusmp.boss.ai;

import com.newuniverse.nusmp.boss.rules.BossRulesConfig;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Why: the engage rule of {@link BossEngage} on a mob. A target goal (flag TARGET, priority 0) that does nothing while the boss already has a living, fair target;
 * otherwise, at most once per {@link BossEngage#SCAN_INTERVAL} ticks, it takes ONE box query of the players within {@code bossRules.aggroRange} and sets the boss's
 * target to the one {@link BossEngage#choose} names. It never holds the flag (the vanilla goals and the hurt-by goal keep working beside it). Server only; nothing
 * is allocated between scans. Off ({@code bossRules.aggroOnSight} false) or in the Sanctuary Hub it never starts: the old behaviour exactly.
 * Worst case per boss per 10 ticks: one box query, at most 10 players read, one line-of-sight ray each, at most one path search per 40 ticks, no packets.
 */
public final class BossEngageGoal extends Goal {
    private final Mob mob;
    private final BooleanSupplier allowed;
    private final Predicate<Player> extra;
    private long nextScan, nextPath;
    private Player pick;

    private BossEngageGoal(Mob mob, BooleanSupplier allowed, Predicate<Player> extra) {
        this.mob = mob;
        this.allowed = allowed;
        this.extra = extra;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    /** The engage goal; the boss adds it first in its target selector ({@code targetSelector.addGoal(0, ...)}). {@code allowed}: the boss's own "may it pick a target now" (a rising boss says no); {@code extra}: a further filter on players (its leash). */
    public static Goal create(Mob mob, BooleanSupplier allowed, Predicate<Player> extra) { return new BossEngageGoal(mob, allowed, extra); }

    public static Goal create(Mob mob) { return create(mob, () -> true, p -> true); }

    /** The boss is not busy fighting someone: the look-at and look-around goals may run (they never run over a fight when the engage rule is on). */
    public static boolean idle(Mob mob) { return !BossRulesConfig.aggroOnSight() || mob.getTarget() == null; }

    /** A look-at-player goal that stands down while the boss has a target (the attack and chase goals own the head then). */
    public static LookAtPlayerGoal lookAt(Mob mob, float range) {
        return new LookAtPlayerGoal(mob, Player.class, range) {
            @Override public boolean canUse() { return idle(mob) && super.canUse(); }
        };
    }

    public static RandomLookAroundGoal lookAround(Mob mob) {
        return new RandomLookAroundGoal(mob) {
            @Override public boolean canUse() { return idle(mob) && super.canUse(); }
        };
    }

    @Override
    public boolean canUse() {
        if (mob.level().isClientSide || !BossRulesConfig.aggroOnSight() || !allowed.getAsBoolean() || mob.isDeadOrDying()) return false;
        LivingEntity cur = mob.getTarget();
        if (cur != null && cur.isAlive() && !(cur instanceof Player p && (p.isCreative() || p.isSpectator()))) return false;
        long now = mob.tickCount;
        if (!BossEngage.due(now, nextScan)) return false;
        nextScan = now + BossEngage.SCAN_INTERVAL;
        if (Labyrinth.inSanctuary(mob.level())) return false;                      // the Hub is a safe zone: nothing there is engaged
        double range = BossRulesConfig.aggroRange();
        List<Player> found = mob.level().getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(range), p -> p.isAlive() && mob.canAttack(p) && extra.test(p));
        if (found.isEmpty()) return false;
        List<BossEngage.Candidate> cs = new ArrayList<>(found.size());
        for (Player p : found)
            cs.add(new BossEngage.Candidate(p.getId(), mob.distanceToSqr(p), p.isAlive(), p.isSpectator(), p.isCreative(), mob.getBoundingBox().inflate(3.0).intersects(p.getBoundingBox())));
        boolean path = now >= nextPath;
        int id = BossEngage.choose(cs, range, path, e -> mob.level().getEntity(e) instanceof LivingEntity le && mob.hasLineOfSight(le),
                e -> {
                    nextPath = now + BossEngage.PATH_RETRY;
                    return mob.level().getEntity(e) instanceof LivingEntity le && mob.getNavigation().createPath(le, 0) != null;
                });
        pick = null;
        if (id < 0) return false;
        for (Player p : found) if (p.getId() == id) pick = p;
        return pick != null;
    }

    @Override public void start() { if (pick != null) mob.setTarget(pick); pick = null; }

    @Override public boolean canContinueToUse() { return false; }
}
