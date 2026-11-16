package com.newuniverse.nusmp.dungeon.rework;

import com.newuniverse.nusmp.dungeon.DirewolfEntity;
import com.newuniverse.nusmp.dungeon.DungeonCreature;
import com.newuniverse.nusmp.dungeon.LivingArmorEntity;
import com.newuniverse.nusmp.dungeon.rework.DungeonReworkRules.BeastKind;
import com.newuniverse.nusmp.dungeon.rework.DungeonReworkRules.Move;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the dungeon's three creatures (Direwolf, Basilisk, Living Armor) only bit before this rework; each gets one or two signature moves with a readable wind-up so a fight against a pack or a
 * sleeping den is something to read, not to tank: the Direwolf crouches (a ring on the floor) and then pounces, and howls to quicken the pack; the Basilisk charges a gaze that only bites a player who
 * looks at it, spits a poison bolt and sweeps its tail at anyone in reach; the Living Armor shield-bashes, and at half health rattles to harden the armours around it. The numbers are
 * {@link DungeonReworkRules#BEASTS}; this class is the Minecraft-facing half. SERVER ONLY, ONE PER CREATURE, NO STATIC STATE (everything is a field and goes with the entity).
 * <p>
 * A move starts only against a PLAYER target (a hunt of prey, a slime or an armour stays the old bite, so the food web is untouched) and only one move runs at a time with a one second pause after
 * it. Cost: nothing while it has no player target; during a move one bounded player query ({@link DungeonStrikes#victims}, 8 players) per tick or less, a few facade effects every few ticks, and for
 * the two "call the pack" moves one entity query of at most {@link #CALL_CAP} creatures once per 30 seconds. No entity is spawned.
 */
public final class BeastBrain {
    public static final int CALL_CAP = 6;
    private static final int GAP = 20;

    private final BeastKind kind;
    private final int[] ids;
    private final List<Move> book = new ArrayList<>(3);
    private final long[] readyAt;
    private int move = -1, t;
    private long gapUntil;
    private boolean landed;

    public BeastBrain(BeastKind kind) {
        this.kind = kind;
        this.ids = DungeonReworkRules.beastMoves(kind);
        for (int id : ids) book.add(DungeonReworkRules.BEASTS.get(id));
        this.readyAt = new long[ids.length];
    }

    /** Is a move winding up or landing (the creature's melee goal stands still)? */
    public boolean busy() { return move >= 0; }

    public BeastKind kind() { return kind; }

    /** The global index (of {@link DungeonReworkRules#BEASTS}) of the running move, or -1. */
    public int activeMove() { return move < 0 ? -1 : ids[move]; }

    public void reset() {
        move = -1;
        t = 0;
        landed = false;
    }

    public void tick(DungeonCreature e, ServerLevel sl) {
        if (!e.isAlive()) return;
        long now = sl.getGameTime();
        LivingEntity target = e.getTarget();
        if (move >= 0) {
            if (target == null || !target.isAlive()) {                                  // the prey is gone: drop the move, no payoff
                reset();
                return;
            }
            advance(e, sl, target, now);
            return;
        }
        if (!(target instanceof Player) || !target.isAlive() || now < gapUntil) return;
        double share = e.getMaxHealth() <= 0f ? 1.0 : e.getHealth() / (double) e.getMaxHealth();
        int phase = Math.min(2, DungeonReworkRules.phase(share));
        int i = DungeonReworkRules.choose(book, Math.sqrt(e.distanceToSqr(target)), phase, now, readyAt, e.getRandom().nextInt());
        if (i < 0) return;
        Move m = book.get(i);
        move = i;
        t = 0;
        landed = false;
        readyAt[i] = now + m.windup() + DungeonReworkRules.cooldown(m.cooldown(), phase, false);
        begin(e, sl, ids[i]);
    }

    private void begin(DungeonCreature e, ServerLevel sl, int global) {
        switch (global) {
            case DungeonReworkRules.B_POUNCE -> DungeonStrikes.sound(sl, e, SoundEvents.WOLF_GROWL, 1.2f, 0.7f);
            case DungeonReworkRules.B_HOWL -> DungeonStrikes.sound(sl, e, SoundEvents.WOLF_HOWL, 1.6f, 0.8f);
            case DungeonReworkRules.B_GAZE -> DungeonStrikes.sound(sl, e, SoundEvents.GUARDIAN_ATTACK, 1.0f, 0.6f);
            case DungeonReworkRules.B_SPIT -> DungeonStrikes.sound(sl, e, SoundEvents.SPIDER_AMBIENT, 1.0f, 1.6f);
            case DungeonReworkRules.B_SWEEP -> DungeonStrikes.sound(sl, e, SoundEvents.SPIDER_AMBIENT, 1.0f, 0.5f);
            case DungeonReworkRules.B_BASH -> DungeonStrikes.sound(sl, e, SoundEvents.SHIELD_BLOCK, 0.9f, 0.7f);
            default -> DungeonStrikes.sound(sl, e, SoundEvents.CHAIN_HIT, 1.2f, 0.6f);
        }
    }

    private static int resolveTicks(int global) { return global == DungeonReworkRules.B_POUNCE ? 10 : 1; }

    private void advance(DungeonCreature e, ServerLevel sl, LivingEntity target, long now) {
        int global = ids[move];
        Move m = book.get(move);
        t++;
        if (t < m.windup()) {
            e.getNavigation().stop();
            e.getLookControl().setLookAt(target, 40f, 40f);
            telegraph(e, sl, target, global);
        } else if (t == m.windup()) {
            land(e, sl, target, global);
        } else if (global == DungeonReworkRules.B_POUNCE && !landed) {
            for (Player p : DungeonStrikes.victims(sl, e, 2.2)) {
                if (DungeonStrikes.strike(e, p, m.damage(), false)) DungeonStrikes.knock(e, p, 0.6);
                landed = true;
            }
            if (landed) DungeonStrikes.sound(sl, e, SoundEvents.WOLF_GROWL, 1.0f, 0.5f);
        }
        if (t >= m.windup() + resolveTicks(global) || (landed && global == DungeonReworkRules.B_POUNCE)) {
            gapUntil = now + GAP;
            reset();
        }
    }

    private void telegraph(DungeonCreature e, ServerLevel sl, LivingEntity target, int global) {
        switch (global) {
            case DungeonReworkRules.B_POUNCE -> {
                if (t % 4 == 1) DungeonStrikes.ring(sl, e.getX(), e.getY() + 0.1, e.getZ(), 1.4, 12, false);
            }
            case DungeonReworkRules.B_HOWL -> {
                if (t % 6 == 1) DungeonStrikes.ring(sl, e.getX(), e.getY() + 0.1, e.getZ(), 1.0 + t * 0.4, 16, false);
            }
            case DungeonReworkRules.B_GAZE, DungeonReworkRules.B_SPIT -> {
                if (t % 3 == 1) DungeonStrikes.line(sl, e.getEyePosition(), target.getEyePosition(), global == DungeonReworkRules.B_GAZE, global == DungeonReworkRules.B_GAZE ? 2 : 1);
            }
            case DungeonReworkRules.B_SWEEP -> {
                if (t % 3 == 1) DungeonStrikes.ring(sl, e.getX(), e.getY() + 0.1, e.getZ(), 3.6, 20, false);
            }
            case DungeonReworkRules.B_BASH -> {
                if (t % 4 == 1) DungeonStrikes.burst(sl, e.getX(), e.getY() + 1.0, e.getZ(), 2, 0.3, false);
            }
            default -> {
                if (t % 5 == 1) DungeonStrikes.ring(sl, e.getX(), e.getY() + 0.1, e.getZ(), 4.0, 20, true);
            }
        }
    }

    private void land(DungeonCreature e, ServerLevel sl, LivingEntity target, int global) {
        Move m = DungeonReworkRules.BEASTS.get(global);
        switch (global) {
            case DungeonReworkRules.B_POUNCE -> {
                Vec3 d = target.position().subtract(e.position());
                Vec3 flat = new Vec3(d.x, 0, d.z);
                if (flat.lengthSqr() < 1e-4) return;
                flat = flat.normalize().scale(1.05);
                e.setDeltaMovement(flat.x, 0.42, flat.z);
                DungeonStrikes.burst(sl, e.getX(), e.getY() + 0.3, e.getZ(), 6, 0.3, false);
            }
            case DungeonReworkRules.B_HOWL -> {
                int called = 0;
                for (DirewolfEntity w : sl.getEntitiesOfClass(DirewolfEntity.class, e.getBoundingBox().inflate(24.0, 8.0, 24.0), x -> x != e && x.isAlive())) {
                    if (called++ >= CALL_CAP) break;
                    DungeonStrikes.effect(w, MobEffects.MOVEMENT_SPEED, 100, 0);
                    if (w.getTarget() == null) w.setTarget(target);
                }
                DungeonStrikes.effect(e, MobEffects.MOVEMENT_SPEED, 100, 0);
                DungeonStrikes.ring(sl, e.getX(), e.getY() + 0.1, e.getZ(), 10.0, 32, false);
                DungeonStrikes.sound(sl, e, SoundEvents.WOLF_HOWL, 2.0f, 0.7f);
            }
            case DungeonReworkRules.B_GAZE -> {
                for (Player p : DungeonStrikes.victims(sl, e, 17.0)) {
                    Vec3 toMe = e.getEyePosition().subtract(p.getEyePosition());
                    if (toMe.lengthSqr() < 1e-4 || p.getViewVector(1.0f).dot(toMe.normalize()) < 0.6) continue;      // looking away is the answer
                    if (DungeonStrikes.strike(e, p, m.damage(), true)) DungeonStrikes.slow(e, p, 40, 2);
                    DungeonStrikes.line(sl, e.getEyePosition(), p.getEyePosition(), true, 6);
                }
                DungeonStrikes.sound(sl, e, SoundEvents.GUARDIAN_ATTACK, 1.4f, 1.2f);
            }
            case DungeonReworkRules.B_SPIT -> {
                Vec3 from = e.getEyePosition(), to = target.getEyePosition();
                DungeonStrikes.line(sl, from, to, false, 8);
                if (target instanceof Player p && p.isAlive() && e.hasLineOfSight(p)) {
                    if (DungeonStrikes.strike(e, p, m.damage(), false)) DungeonStrikes.effect(p, MobEffects.POISON, 100, 0);
                }
                DungeonStrikes.sound(sl, e, SoundEvents.SPIDER_AMBIENT, 1.2f, 1.8f);
            }
            case DungeonReworkRules.B_SWEEP -> {
                for (Player p : DungeonStrikes.victims(sl, e, 3.6)) {
                    if (DungeonStrikes.strike(e, p, m.damage(), false)) DungeonStrikes.knock(e, p, 1.0);
                }
                DungeonStrikes.ring(sl, e.getX(), e.getY() + 0.1, e.getZ(), 3.6, 28, false);
                DungeonStrikes.sound(sl, e, SoundEvents.IRON_GOLEM_ATTACK, 1.0f, 0.6f);
            }
            case DungeonReworkRules.B_BASH -> {
                for (Player p : DungeonStrikes.victims(sl, e, 3.2)) {
                    if (DungeonStrikes.strike(e, p, m.damage(), false)) DungeonStrikes.knock(e, p, 1.1);
                }
                DungeonStrikes.flash(sl, e.getX(), e.getY() + 1.0, e.getZ());
                DungeonStrikes.sound(sl, e, SoundEvents.SHIELD_BLOCK, 1.2f, 0.5f);
            }
            default -> {
                int called = 0;
                for (LivingArmorEntity a : sl.getEntitiesOfClass(LivingArmorEntity.class, e.getBoundingBox().inflate(24.0, 8.0, 24.0), x -> x != e && x.isAlive())) {
                    if (called++ >= CALL_CAP) break;
                    DungeonStrikes.effect(a, MobEffects.DAMAGE_RESISTANCE, 100, 0);
                    if (a.getTarget() == null) a.setTarget(target);
                }
                DungeonStrikes.effect(e, MobEffects.DAMAGE_RESISTANCE, 100, 0);
                DungeonStrikes.ring(sl, e.getX(), e.getY() + 0.1, e.getZ(), 6.0, 28, true);
                DungeonStrikes.sound(sl, e, SoundEvents.CHAIN_HIT, 1.6f, 0.5f);
            }
        }
    }
}
