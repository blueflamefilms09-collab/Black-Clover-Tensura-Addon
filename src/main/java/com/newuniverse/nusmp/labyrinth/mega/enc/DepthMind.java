package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.newuniverse.nusmp.boss.rules.BossIdentity;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import com.newuniverse.nusmp.labyrinth.mega.gear.GearDrops;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Why: "smarter" has to be something a player can feel in a fight, not a number. This is the mob's mind: ONE goal with no movement flags, so it runs beside whatever the mob already does (a
 * vanilla goal list, a Foundry style goal, or nothing at all for a Tensura mob that thinks with a brain), and every few ticks (a deeper mind thinks sooner, {@link DepthRules#thinkEvery})
 * it runs the behaviours its level has unlocked ({@link DepthRules.Behaviour}), in this order: REGROUP (a badly hurt mob backs off and recovers, once), MEMORY (it keeps hunting where it last
 * saw its target), ALERT (a hurt or alerted mob calls the monsters near it), FLANK (several mobs on one target come at it from different sides), CLOSE_DOWN (a target that kites is closed down
 * with a burst of speed), HUNTER (the softest player in reach, not the nearest). It never fights the mob's own brain: it steers the way the rest of the mod does ({@link MobSteer}: erase the
 * brain's walk target, aim in the plain slot and the memory) and it only ever aims at a living player who is not in creative mode, not a spectator and not in a safe room. Every behaviour sits
 * in its own try block, so one that fails on an odd mob costs that behaviour on that mob and nothing else. The decisions are pure ({@link DepthRules}) and tested without a world.
 */
final class DepthMind extends Goal {
    private final Mob mob;
    private final int mind;
    private final int every;
    private int phase;
    private final Random rnd = new Random();

    // memory
    private LivingEntity lastTarget;
    private Vec3 lastPos;
    private long lastSeen = Long.MIN_VALUE;
    // alert
    private long alertedAt = Long.MIN_VALUE;
    private LivingEntity alertedFor;
    // regroup
    private boolean regroupUsed, regrouping;
    private long regroupSince;
    // close down
    private double lastDistance = -1.0;
    private long boostedAt = Long.MIN_VALUE;
    // hunter
    private long switchedAt = Long.MIN_VALUE;

    DepthMind(Mob mob, int mind) {
        this.mob = mob;
        this.mind = mind;
        this.every = DepthRules.thinkEvery(mind);
        this.phase = new Random(mob.getUUID().getLeastSignificantBits()).nextInt(every);       // mobs of one pack do not all think on the same tick
    }

    @Override public boolean canUse() { return mob.isAlive() && !mob.level().isClientSide; }

    @Override public boolean canContinueToUse() { return canUse(); }

    @Override public boolean isInterruptable() { return true; }

    @Override
    public void tick() {
        if (++phase < every) return;
        phase = 0;
        if (!(mob.level() instanceof ServerLevel level) || mob.isNoAi()) return;
        long now = level.getGameTime();
        if (regrouping) {
            try { regroup(level, now); } catch (RuntimeException ignored) { regrouping = false; }
            return;
        }
        if (DepthRules.knows(mind, DepthRules.Behaviour.REGROUP)) guard(() -> maybeRegroup(level, now));
        if (regrouping) return;
        if (DepthRules.knows(mind, DepthRules.Behaviour.MEMORY)) guard(() -> memory(level, now));
        if (DepthRules.knows(mind, DepthRules.Behaviour.ALERT)) guard(() -> alert(level, now));
        if (DepthRules.knows(mind, DepthRules.Behaviour.FLANK)) guard(() -> flank(level));
        if (DepthRules.knows(mind, DepthRules.Behaviour.CLOSE_DOWN)) guard(() -> closeDown(level, now));
        if (DepthRules.knows(mind, DepthRules.Behaviour.HUNTER)) guard(() -> hunter(level, now));
    }

    private static void guard(Runnable r) {
        try { r.run(); } catch (RuntimeException | LinkageError oddMob) { /* this behaviour does not work on this mob: the rest still do */ }
    }

    // ---------------------------------------------------------------- who may be hunted
    /** A living player the dungeon may go after: not creative, not a spectator, not resting in a safe room. */
    static boolean fair(ServerLevel level, LivingEntity e) {
        if (e == null || !e.isAlive() || e.isRemoved()) return false;
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        return !MegaDungeon.inSafeZone(level, e.blockPosition());
    }

    private double followRange() {
        var a = mob.getAttribute(Attributes.FOLLOW_RANGE);
        return a == null ? 16.0 : Math.max(8.0, a.getValue());
    }

    // ---------------------------------------------------------------- 4 regroup
    private void maybeRegroup(ServerLevel level, long now) {
        double frac = mob.getHealth() / Math.max(1.0f, mob.getMaxHealth());
        if (!DepthRules.startRegroup(frac, regroupUsed)) return;
        LivingEntity foe = mob.getTarget();
        if (foe == null) return;                                   // nobody to back away from: it just heals the slow way
        regroupUsed = true;
        regrouping = true;
        regroupSince = now;
        lastTarget = foe;
        mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
        if (DepthRules.knows(mind, DepthRules.Behaviour.ALERT)) callFor(level, foe, now);       // it calls for help as it falls back
    }

    private void regroup(ServerLevel level, long now) {
        double frac = mob.getHealth() / Math.max(1.0f, mob.getMaxHealth());
        if (DepthRules.endRegroup(frac, (int) (now - regroupSince))) {
            regrouping = false;
            if (fair(level, lastTarget)) MobSteer.aim(mob, lastTarget);
            return;
        }
        LivingEntity foe = lastTarget != null && lastTarget.isAlive() ? lastTarget : null;
        MobSteer.release(mob);
        if (foe == null) return;
        Vec3 away = mob.position().subtract(foe.position());
        away = away.lengthSqr() < 1.0E-4 ? new Vec3(rnd.nextDouble() - 0.5, 0, rnd.nextDouble() - 0.5) : away.normalize();
        Vec3 to = mob.position().add(away.scale(10.0));
        MobSteer.walk(mob, to.x, to.y, to.z, 1.2);
    }

    // ---------------------------------------------------------------- 1 memory
    private void memory(ServerLevel level, long now) {
        LivingEntity t = mob.getTarget();
        if (t != null && t.isAlive()) {
            if (mob.hasLineOfSight(t)) {
                lastTarget = t;
                lastPos = t.position();
                lastSeen = now;
            }
            return;
        }
        if (lastTarget == null || !fair(level, lastTarget) || now - lastSeen > DepthRules.memoryTicks(mind)) return;
        if (mob.distanceToSqr(lastTarget) < followRange() * followRange() && mob.hasLineOfSight(lastTarget)) {
            MobSteer.aim(mob, lastTarget);                          // it sees them again: back on the hunt
            lastSeen = now;
            lastPos = lastTarget.position();
        } else if (lastPos != null && mob.position().distanceToSqr(lastPos) > 4.0) {
            MobSteer.walk(mob, lastPos.x, lastPos.y, lastPos.z, 1.1);   // it goes to where it last saw them
        }
    }

    // ---------------------------------------------------------------- 2 alert
    private void alert(ServerLevel level, long now) {
        LivingEntity t = mob.getTarget();
        if (t == null || !fair(level, t)) return;
        boolean hurt = mob.hurtTime > 0;
        boolean fresh = t != alertedFor;
        if ((!hurt && !fresh) || now - alertedAt < 100) return;
        callFor(level, t, now);
    }

    private void callFor(ServerLevel level, LivingEntity foe, long now) {
        alertedAt = now;
        alertedFor = foe;
        if (!fair(level, foe)) return;
        double r = DepthRules.alertRadius(mind);
        List<Mob> near = level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(r), m -> m != mob && GearDrops.hostile(m) && m.isAlive() && m.getTarget() == null
                && !BossIdentity.isBoss(m) && !m.getTags().contains(WandererDirector.TAG));
        near.sort(Comparator.comparingDouble(m -> m.distanceToSqr(mob)));
        for (int i = 0; i < Math.min(near.size(), 10); i++) MobSteer.aim(near.get(i), foe);
    }

    // ---------------------------------------------------------------- 3 flank
    private void flank(ServerLevel level) {
        LivingEntity t = mob.getTarget();
        if (t == null || mob instanceof RangedAttackMob || !fair(level, t)) return;
        double d = mob.distanceTo(t);
        if (d < 5.0 || d > 18.0) return;                              // close in: the mob's own attack takes over; far away: it is not a fight yet
        List<Mob> pack = level.getEntitiesOfClass(Mob.class, t.getBoundingBox().inflate(14.0), m -> GearDrops.hostile(m) && m.isAlive() && m.getTarget() == t);
        if (pack.size() < 2) return;
        pack.sort(Comparator.comparing(Mob::getUUID));
        int index = Math.max(0, pack.indexOf(mob));
        double a = DepthRules.flankAngle(index, pack.size());
        Vec3 p = t.position().add(Math.cos(a) * 2.4, 0, Math.sin(a) * 2.4);
        MobSteer.walk(mob, p.x, p.y, p.z, 1.05);
    }

    // ---------------------------------------------------------------- 5 close down
    private void closeDown(ServerLevel level, long now) {
        LivingEntity t = mob.getTarget();
        if (t == null || !fair(level, t)) {
            lastDistance = -1.0;
            return;
        }
        double d = mob.distanceTo(t);
        boolean closing = lastDistance >= 0.0 && d < lastDistance - 0.5;
        lastDistance = d;
        int since = mob.tickCount - mob.getLastHurtMobTimestamp();
        if (!DepthRules.isKited(d, since, closing) || now - boostedAt < 200) return;
        boostedAt = now;
        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
        MobSteer.follow(mob, t, 1.3);
    }

    // ---------------------------------------------------------------- 6 hunter
    private void hunter(ServerLevel level, long now) {
        if (now - switchedAt < 100) return;
        double r = followRange();
        List<Player> seen = new ArrayList<>(level.getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(r), p -> fair(level, p) && mob.hasLineOfSight(p)));
        if (seen.size() < 2) return;                                  // one player in sight: nothing to choose
        seen.sort(Comparator.comparingDouble(p -> p.distanceToSqr(mob)));
        List<Double> scores = new ArrayList<>();
        for (Player p : seen) scores.add(DepthRules.softness(p.getHealth(), p.getArmorValue()));
        int best = DepthRules.softest(scores);
        if (best < 0) return;
        Player pick = seen.get(best);
        LivingEntity cur = mob.getTarget();
        if (pick == cur) return;
        if (cur instanceof Player c && fair(level, c) && scores.get(best) > DepthRules.softness(c.getHealth(), c.getArmorValue()) * 0.8) return;   // not worth the switch
        switchedAt = now;
        MobSteer.aim(mob, pick);
    }
}
