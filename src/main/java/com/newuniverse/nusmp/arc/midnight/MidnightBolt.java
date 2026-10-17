package com.newuniverse.nusmp.arc.midnight;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.VolleyBudget;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Why: Patolli's light swords, Fana's fireballs, Rades's cursed bullets and Rhya's copied bolts are all the same thing: a point that flies along a ray a
 * few blocks a tick, stops at the first wall or the first foe, and is drawn as a trail of dust and one effect packet. A point and not an entity keeps a
 * volley of eight cheap (one zone step per tick each, no tracking, no client class) and lets a bolt be tested exactly (the segment it crosses in a tick is
 * swept for foes, so a fast sword cannot jump over someone). One more thing makes this class more than a loop: a bolt can be TURNED BACK. In the story
 * Asta's blade sent Salamander's fireball back and Gauche's mirror sent Patolli's light back; a bolt that is marked {@code reflectable} is caught by a foe
 * whose mirror, Full Reflection or anti-magic swing ({@link ReflectRules}) is up within a few blocks, flies back at the boss and strikes it for a share of
 * its health. The boss decides what a hit or an end means through the callbacks; this class only flies, sweeps and turns.
 */
final class MidnightBolt {
    /** A bolt found a foe. */
    interface Hit { void on(MidnightBolt bolt, LivingEntity victim); }

    /** A bolt ended (a foe, a wall, its life, or it struck its own caster after being turned): where, and whether it struck a foe. */
    interface Done { void on(MidnightBolt bolt, Vec3 at, boolean struck); }

    /** A turned bolt struck the boss: by whom and with what. */
    interface Turned { void on(MidnightBolt bolt, ServerPlayer by, ReflectRules.Kind kind); }

    private final ZogratisBoss boss;
    private final ServerLevel sl;
    private final double radius;
    private final int rgb;
    private final boolean reflectable;
    private final VolleyBudget budget;
    private final Hit onHit;
    private final Done onDone;
    private final Turned onTurned;
    private Vec3 pos, vel;
    private int life;
    private boolean done, reversed, struck;
    private ServerPlayer turnedBy;
    private ReflectRules.Kind turnedKind = ReflectRules.Kind.NONE;

    private MidnightBolt(ZogratisBoss boss, ServerLevel sl, Vec3 from, Vec3 vel, int life, double radius, int rgb, boolean reflectable, VolleyBudget budget,
                         Hit onHit, Done onDone, Turned onTurned) {
        this.boss = boss;
        this.sl = sl;
        this.pos = from;
        this.vel = vel;
        this.life = life;
        this.radius = radius;
        this.rgb = rgb;
        this.reflectable = reflectable;
        this.budget = budget;
        this.onHit = onHit;
        this.onDone = onDone;
        this.onTurned = onTurned;
    }

    /**
     * Launches a bolt from {@code from} with velocity {@code vel} (blocks per tick) for {@code life} ticks. {@code budget} (may be null) limits how many times
     * one volley hits one foe. Any of the callbacks may be null.
     */
    static void launch(ZogratisBoss boss, Vec3 from, Vec3 vel, int life, double radius, int rgb, boolean reflectable, VolleyBudget budget,
                       Hit onHit, Done onDone, Turned onTurned) {
        if (!(boss.level() instanceof ServerLevel sl) || vel.lengthSqr() < 1e-6) return;
        MidnightBolt b = new MidnightBolt(boss, sl, from, vel, Math.max(1, life), radius, rgb, reflectable, budget, onHit, onDone, onTurned);
        SpellRuntime.zone(sl, Math.max(1, life) * 2 + 24, 1, MidnightFx.safe("bolt", age -> b.step()));
    }

    Vec3 pos() { return pos; }

    Vec3 velocity() { return vel; }

    boolean reversed() { return reversed; }

    private void step() {
        if (done) return;
        if (!boss.isAlive() || boss.isRemoved()) {
            done = true;
            return;
        }
        Vec3 next = pos.add(vel);
        BlockHitResult wall = sl.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
        boolean blocked = wall.getType() != HitResult.Type.MISS;
        if (blocked) next = wall.getLocation();
        AABB sweep = new AABB(pos, next).inflate(radius);
        if (reversed) {
            if (boss.getBoundingBox().intersects(sweep)) {
                backfire(next);
                return;
            }
        } else {
            if (reflectable) {
                ServerPlayer catcher = catcher(next);
                if (catcher != null) {
                    turn(catcher);
                    return;
                }
            }
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, sweep, boss::isFoe)) {
                if (budget != null && !budget.tryHit(e.getId())) continue;
                struck = true;
                if (onHit != null) onHit.on(this, e);
                finish(next);
                return;
            }
        }
        Vec3 from = pos;
        pos = next;
        Tells.line(sl, from, next, rgb);
        Tells.gather(sl, next, 0.06, rgb, 1);
        if (blocked) finish(next);
        else if (--life <= 0) finish(pos);
    }

    /** The player whose mirror or blade is close enough to catch the bolt right now, or null. */
    private ServerPlayer catcher(Vec3 at) {
        for (LivingEntity e : boss.foesAround(at, ReflectRules.CATCH_RADIUS)) {
            if (!(e instanceof ServerPlayer p)) continue;
            if (p.getBoundingBox().getCenter().distanceTo(at) > ReflectRules.CATCH_RADIUS) continue;
            ReflectRules.Kind k = MidnightFx.reflectKind(p, true);
            if (k != ReflectRules.Kind.NONE) {
                turnedKind = k;
                return p;
            }
        }
        return null;
    }

    /** The bolt is turned: it flies back at the boss, faster, and belongs to the player now. */
    private void turn(ServerPlayer by) {
        reversed = true;
        turnedBy = by;
        MidnightFx.markTurned(by);
        Vec3 to = boss.getBoundingBox().getCenter().subtract(pos);
        double speed = Math.max(1.0, vel.length()) * 1.4;
        vel = to.lengthSqr() < 1e-6 ? vel.scale(-1) : to.normalize().scale(speed);
        life = 48;
        by.displayClientMessage(Component.literal("You turn it back on " + boss.bossName() + "!"), true);
        MidnightFx.sound(sl, pos, SoundEvents.AMETHYST_BLOCK_CHIME, 1.6f, 1.4f);
        Tells.burst(sl, pos, rgb, 10);
    }

    /** The turned bolt reaches the boss: it takes a share of its health from the player's hand. */
    private void backfire(Vec3 at) {
        float amount = ReflectRules.backfireShare(turnedKind) * boss.getMaxHealth();
        if (turnedBy != null && amount > 0f) boss.hurt(boss.damageSources().indirectMagic(turnedBy, turnedBy), amount);
        if (onTurned != null && turnedBy != null) onTurned.on(this, turnedBy, turnedKind);
        finish(at);
    }

    private void finish(Vec3 at) {
        if (done) return;
        done = true;
        if (onDone != null) onDone.on(this, at, struck);
    }
}
