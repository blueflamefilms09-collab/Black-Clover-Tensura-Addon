package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.boss.arc.HoverMath;
import com.newuniverse.nusmp.boss.arc.PaladinTurn;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the chain data (boss/arc/LuciusChains) says <i>when</i> each step of a combo happens; this class says what a step <i>does</i> in
 * the world, once as a telegraph (the warning that lasts the step's telegraph ticks) and once as the effect. Every step reuses what the
 * boss already has instead of inventing new damage: the damage goes through {@link LuciusBoss#strike} (the balance law's per-hit cap,
 * and the arc's shared per-second pressure cap in {@code LuciusArcHooks}), the gravity zone is {@link DanteMoves#gravityField}, the
 * stasis is the existing Time Magic freeze rules ({@link TimeStop}, {@link BalanceLaw#controlTicks}: three seconds on a player at the
 * most, never on creative or spectator, never on a holder of anti-magic), the blink uses the same standable-spot search as the rest of
 * Lucius ({@link LuciusFx#standable}: solid floor, nothing inside the body). All visuals are the Zogratis layers
 * ({@link ZogratisVfx}) or the existing Time effects, never raw particles.
 * <p>
 * The clones call the same helpers with themselves as the caster, which is how a clone "has its own grimoire" without a second
 * implementation of the spells.
 */
final class ArcSteps {
    private ArcSteps() {}

    static final int VIOLET = 0x5B2A8C, SOUL = 0xB07CFF;
    static final float PIN_BASE = 9f, TOUCH_BASE = 26f, BOLT_BASE = 5f, SPATIAL_STRIKE_BASE = 12f;
    static final double PIN_RADIUS = 3.4, TOUCH_RING = 3.0, BUBBLE_RADIUS = 3.0, BEHIND = 2.5;
    static final int MAX_HELD = 4;

    // ================================================================ helpers
    static Vec3 groundUnder(LivingEntity e) { return ZogratisBoss.ground(e.level(), e.position()); }

    static boolean fair(LivingEntity e) {
        return e != null && e.isAlive() && !e.isSpectator() && !(e instanceof Player p && p.isCreative());
    }

    /** Turns 'self' to look at 'target' (body, head and the yaw the entity moves by). */
    static void faceTowards(LivingEntity self, LivingEntity target) {
        double dx = target.getX() - self.getX(), dz = target.getZ() - self.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        self.setYRot(yaw);
        self.setYHeadRot(yaw);
        self.setYBodyRot(yaw);
    }

    /**
     * The blind spot of 'target': the floor 2.5 blocks behind its look vector (flat), never inside blocks. Falls back to 1.8 and 3.2
     * blocks behind, then to either side; null if there is no room anywhere (the step is then skipped).
     */
    static Vec3 behindSpot(LivingEntity mover, LivingEntity target) {
        Vec3 look = Vec3.directionFromRotation(0f, target.getYRot());
        double[] reach = {BEHIND, 1.8, 3.2};
        for (double d : reach) {
            Vec3 s = LuciusFx.standable(mover.level(), mover, target.position().subtract(look.scale(d)));
            if (s != null && s.distanceToSqr(target.position()) > 1.0) return s;
        }
        for (double a : new double[]{Math.PI / 2, -Math.PI / 2}) {
            Vec3 s = LuciusFx.standable(mover.level(), mover, target.position().add(LuciusFx.rotateY(look, a).scale(BEHIND)));
            if (s != null && s.distanceToSqr(target.position()) > 1.0) return s;
        }
        return null;
    }

    // ================================================================ gravity pin
    static void pinTell(ServerLevel sl, Vec3 mark, int ticks) {
        ZogratisVfx.well(sl, mark, PIN_RADIUS, ticks + 2, VIOLET, true);
        ZogratisVfx.pillar(sl, mark, 1.4, ticks + 2, 0xB58CFF, true);
    }

    /** The pin: heavy downward velocity, Slowness IV for at most the balance law's control time, a crush; held to the floor for a second. */
    static void pinFire(LuciusBoss b, ServerLevel sl, Vec3 mark) {
        ZogratisVfx.gravityImpact(sl, mark, PIN_RADIUS, VIOLET);
        LuciusFx.sound(sl, mark, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.2f, 0.5f);
        List<LivingEntity> pinned = new ArrayList<>();
        for (LivingEntity v : b.foesAround(mark, PIN_RADIUS + 1.5)) {
            double dx = v.getX() - mark.x, dz = v.getZ() - mark.z;
            if (dx * dx + dz * dz > PIN_RADIUS * PIN_RADIUS || Math.abs(v.getY() - mark.y) > 4.5 || !fair(v)) continue;
            pinned.add(v);
            v.setDeltaMovement(v.getDeltaMovement().multiply(0.2, 0, 0.2).add(0, -1.4, 0));
            v.hurtMarked = true;
            v.fallDistance = 0;
            int slow = Math.min(50, BalanceLaw.controlTicks(v, 50));
            v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slow, 3));
            b.strike(v, PIN_BASE, true);
        }
        if (pinned.isEmpty()) return;
        SpellRuntime.zone(sl, 24, 2, LuciusFx.safe("gravity pin", age -> {
            for (LivingEntity v : pinned) {
                if (!v.isAlive()) continue;
                Vec3 d = v.getDeltaMovement();
                if (d.y > -0.3) { v.setDeltaMovement(d.x * 0.3, -0.45, d.z * 0.3); v.hurtMarked = true; }
                v.fallDistance = 0;
            }
        }));
    }

    // ================================================================ spatial blink
    static void blinkTell(ServerLevel sl, Vec3 dest, Vec3 facing, int ticks) {
        ZogratisVfx.gate(sl, dest.add(0, 1.0, 0), facing, 1.2, ticks + 2, FxMath.divine(100));
        ZogratisVfx.divineSigil(sl, dest, 1.6, ticks + 2, FxMath.divine(100), true);
    }

    /** The blink: he is at 'dest' (the spot behind the target), facing it. False if the spot was no longer free. */
    static boolean blinkFire(LuciusBoss b, Vec3 dest, LivingEntity target) {
        if (dest == null || target == null) return false;
        boolean ok = b.blinkTo(dest);
        if (ok && target.isAlive()) faceTowards(b, target);
        return ok;
    }

    // ================================================================ chrono stasis
    static void stasisTell(ServerLevel sl, LivingEntity target, int ticks) {
        Vec3 g = groundUnder(target);
        VfxSpawn.send(sl, VfxShape.TIME_CLOCK, g, g.add(0, 5, 0), 0, ticks + 4, 2.0f);
        ZogratisVfx.divineSigil(sl, g, BUBBLE_RADIUS, ticks + 2, FxMath.divine(100), true);
    }

    /**
     * The existing Time Magic freeze, bounded: holds 'foe' for the balance law's control time (three seconds at the most for a player, less
     * on repeats), never a creative or spectator player, never a holder of anti-magic, never without a line of sight from 'caster', never
     * while Julius holds the body. Returns the ticks held, or 0. The grace after it (nobody can be stopped again for six seconds) is the
     * existing {@link ChronoMoves#release}.
     */
    static int hold(LuciusBoss owner, LivingEntity caster, LivingEntity foe, int base) { return hold(owner, caster, foe, base, true); }

    /** The same, with the line of sight optional (1.45.0: a Time Stasis Trap on the ground holds whoever steps in it, whether or not he can see them). */
    static int hold(LuciusBoss owner, LivingEntity caster, LivingEntity foe, int base, boolean needSight) {
        if (!(foe.level() instanceof ServerLevel sl) || owner.juliusActive() || !fair(foe)) return 0;
        if (foe instanceof Player p && LuciusFx.antiMagicHeld(p)) return 0;
        if (TimeStop.isImmune(foe) || TimeStop.isFrozen(foe) || (needSight && !caster.hasLineOfSight(foe))) return 0;
        int t = Math.min(ChronoMath.HARD_STOP_CAP, BalanceLaw.controlTicks(foe, base));
        TimeStop.freeze(foe, t);
        owner.noteHeld(foe);
        float radius = Math.max(foe.getBbHeight(), foe.getBbWidth()) * 0.6f + 0.25f;
        Vec3 c = foe.position().add(0, foe.getBbHeight() / 2, 0);
        VfxSpawn.send(sl, new VfxPayload(VfxShape.TIME_STASIS.ordinal(), c, c, 0, t, radius / 0.6f, foe.getId(), sl.random.nextLong()));
        owner.later(t, LuciusFx.safe("stasis grace", () -> ChronoMoves.release(foe)));
        return t;
    }

    /** The bubble: the target and whoever stands within three blocks of it (four at most) are held. Returns how many. */
    static int bubble(LuciusBoss b, LivingEntity caster, LivingEntity target) {
        int held = 0;
        List<LivingEntity> around = new ArrayList<>(b.foesAround(target.position(), BUBBLE_RADIUS));
        around.remove(target);
        around.add(0, target);
        for (LivingEntity v : around) {
            if (held >= MAX_HELD) break;
            if (hold(b, caster, v, 60) > 0) {
                held++;
                if (v instanceof ServerPlayer p) p.displayClientMessage(Component.literal("Time has stopped.").withStyle(ChatFormatting.GOLD), true);
            }
        }
        if (held == 0 && target instanceof ServerPlayer p) p.displayClientMessage(Component.literal("Time cannot hold you.").withStyle(ChatFormatting.GRAY), true);
        LuciusFx.sound(sl(b), target.position(), SoundEvents.BELL_RESONATE, 1.6f, 0.6f);
        return held;
    }

    private static ServerLevel sl(LuciusBoss b) { return (ServerLevel) b.level(); }

    // ================================================================ chrono bolt
    static void boltTell(ServerLevel sl, LivingEntity caster, LivingEntity target, int ticks) {
        ZogratisVfx.ray(sl, caster.position().add(0, caster.getBbHeight() * 0.75, 0), target.getBoundingBox().getCenter(), 0.5, ticks + 2, FxMath.divine(100), true);
    }

    /** A slow bolt of time: it homes only weakly (a sidestep avoids it) and holds what it touches, bounded as above. */
    static void chronoBolt(LuciusBoss owner, LivingEntity caster, LivingEntity target) {
        if (!(caster.level() instanceof ServerLevel sl) || target == null) return;
        Vec3 start = caster.position().add(0, caster.getBbHeight() * 0.75, 0);
        Vec3 aim = target.getBoundingBox().getCenter().subtract(start);
        if (aim.lengthSqr() < 1e-4) aim = new Vec3(0, 0, 1);
        final double speed = 0.55;
        final Vec3[] pos = {start};
        final Vec3[] vel = {aim.normalize().scale(speed)};
        final int[] life = {80};
        final boolean[] done = {false};
        LuciusFx.sound(sl, start, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.3f, 1.8f);
        SpellRuntime.zone(sl, life[0] + 2, 1, LuciusFx.safe("chrono bolt", age -> {
            if (done[0] || owner.isRemoved() || caster.isRemoved()) { done[0] = true; return; }
            if (target.isAlive()) {
                Vec3 want = target.getBoundingBox().getCenter().subtract(pos[0]);
                if (want.lengthSqr() > 1e-4) {
                    Vec3 v = vel[0].scale(0.94).add(want.normalize().scale(speed * 0.06));
                    vel[0] = v.lengthSqr() < 1e-6 ? vel[0] : v.normalize().scale(speed);
                }
            }
            Vec3 next = pos[0].add(vel[0]);
            BlockHitResult wall = sl.clip(new ClipContext(pos[0], next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
            boolean blocked = wall.getType() != HitResult.Type.MISS;
            if (blocked) next = wall.getLocation();
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(pos[0], next).inflate(0.7), x -> owner.isFoe(x) && fair(x))) {
                owner.strike(e, BOLT_BASE, true);
                int t = hold(owner, caster, e, 60);
                VfxSpawn.send(sl, VfxShape.TIME_CLOCK, e.position(), e.position().add(0, 4, 0), 0, 24, 1.2f);
                if (t == 0 && e instanceof ServerPlayer p) p.displayClientMessage(Component.literal("The bolt of time slides off you.").withStyle(ChatFormatting.GRAY), true);
                done[0] = true;
                return;
            }
            pos[0] = next;
            if (age % 3 == 0) VfxSpawn.send(sl, VfxShape.TIME_STASIS, next, next, 0, 6, 0.7f);
            if (blocked || --life[0] <= 0) done[0] = true;
        }));
    }

    // ================================================================ tower volley
    static void towerTell(LuciusBoss b, ServerLevel sl, TowerSpells.Entry e, int ticks) {
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, b.position().add(0, 0.05, 0), b.position().add(0, 1, 0), e.colorArgb(), ticks + 2, 1.4f);
        TowerSpells.announce(b, e);
    }

    static void towerFire(LuciusBoss b, TowerSpells.Entry e, LivingEntity target, int salt) {
        TowerSpells.cast(b, e, target, salt);
    }

    // ================================================================ gravity zone
    static void zoneTell(ServerLevel sl, Vec3 mark, double radius, int ticks) {
        ZogratisVfx.well(sl, mark, radius, ticks + 2, VIOLET, true);
        ZogratisVfx.pillar(sl, mark, 1.2, ticks + 2, 0xB58CFF, true);
    }

    static double zoneRadius(LuciusBoss b) { return 7.0 + 0.5 * PowerCurve.stage(b.power()); }

    static void zoneFire(LuciusBoss b, Vec3 mark) {
        int stage = PowerCurve.stage(b.power());
        DanteMoves.gravityField(b, mark, zoneRadius(b), 80, 1.3f + 0.2f * stage);
    }

    // ================================================================ soul touch
    static void touchTell(LuciusBoss b, ServerLevel sl, Vec3 mark, LivingEntity target, int ticks) {
        ZogratisVfx.divineSigil(sl, mark, TOUCH_RING, ticks + 2, SOUL, true);
        ZogratisVfx.soulVortex(sl, mark, 1.6, ticks + 2, SOUL);
        ZogratisVfx.ray(sl, b.position().add(0, b.getBbHeight() * 0.75, 0), target.getBoundingBox().getCenter(), 0.5, ticks + 2, SOUL, true);
    }

    /**
     * The Soul Touch. The ring that was shown is the reach (three blocks): whoever is still inside it is touched, he steps in if he
     * has to; whoever left it is not (a missed touch is announced). A touch that lands does the capped damage, puts Soul Corruption on
     * the victim and turns the victim's companions. Returns true if it landed.
     */
    static boolean touchFire(LuciusBoss b, PaladinTurn<UUID> roster, Vec3 mark, LivingEntity target) {
        if (!(b.level() instanceof ServerLevel sl) || !fair(target)) return false;
        double dx = target.getX() - mark.x, dz = target.getZ() - mark.z;
        boolean inside = dx * dx + dz * dz <= TOUCH_RING * TOUCH_RING && Math.abs(target.getY() - mark.y) <= 3.5;
        if (!inside) {
            LuciusFx.sound(sl, b.position(), SoundEvents.AMETHYST_BLOCK_BREAK, 1.0f, 0.6f);
            b.announce("Lucius's hand closes on nothing.");
            return false;
        }
        if (b.distanceTo(target) > 2.0) {
            Vec3 dir = LuciusFx.flatDir(target.position(), b.position(), b.getViewVector(1f));
            Vec3 spot = LuciusFx.standable(sl, b, target.position().add(dir.scale(1.5)));
            if (spot != null) b.blinkTo(spot);
        }
        faceTowards(b, target);
        b.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        boolean landed = b.strike(target, TOUCH_BASE, true);
        ZogratisVfx.soulVortex(sl, target.position(), 2.0, 30, SOUL);
        ZogratisVfx.ray(sl, b.position().add(0, b.getBbHeight() * 0.75, 0), target.getBoundingBox().getCenter(), 0.9, 10, SOUL, false);
        LuciusFx.sound(sl, target.position(), SoundEvents.BEACON_DEACTIVATE, 1.4f, 0.6f);
        if (!landed) return false;
        LuciusEffects.corrupt(target);
        if (target instanceof ServerPlayer p) {
            p.displayClientMessage(Component.literal("Soul Corruption: your maximum health is cut for a minute.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), true);
            PaladinTurns.turnCompanionsOf(b, roster, p);
        }
        return true;
    }

    // ================================================================ retreat
    /** A spot 14 to 22 blocks away from 'from', inside the arena, on floor; null if there is none. */
    static Vec3 retreatSpot(LuciusBoss b, Vec3 from) {
        Vec3 away = LuciusFx.flatDir(from, b.position(), b.getViewVector(1f));
        Vec3 centre = b.arenaCenter();
        double limit = (ZogratisBoss.ARENA_RADIUS - 6.0) * (ZogratisBoss.ARENA_RADIUS - 6.0);
        for (double dist : new double[]{18, 22, 14}) {
            for (double a : new double[]{0, 0.6, -0.6, 1.2, -1.2}) {
                Vec3 want = b.position().add(LuciusFx.rotateY(away, a).scale(dist));
                if (want.distanceToSqr(centre) > limit) continue;
                Vec3 s = LuciusFx.standable(b.level(), b, want);
                if (s != null) return s;
            }
        }
        return null;
    }

    static void retreatTell(LuciusBoss b, ServerLevel sl, int ticks) {
        ZogratisVfx.divineSigil(sl, b.position(), 2.4, ticks + 2, FxMath.divine(b.power()), true);
        ZogratisVfx.pillar(sl, b.position(), 1.0, ticks + 2, FxMath.divine(b.power()), true);
    }

    /** He is elsewhere, three blocks off the ground if there is room (the hover brings him there anyway). */
    static boolean retreatFire(LuciusBoss b, Vec3 spot) {
        if (spot == null) return false;
        return b.blinkTo(spot.add(0, HoverMath.HEIGHT, 0)) || b.blinkTo(spot);
    }
}
