package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lucius's Time Magic, the contract with the Supreme Time Devil Astaroth, as static functions on {@link LuciusBoss}: steps beyond the
 * speed of light, the Chrono Stasis, the Time Stop and its combo, the Time Reversal and Chrono Acceleration. (Precognition is his
 * passive, in {@link Precognition} and {@code LuciusBoss#filterDamage}.) The existing Time Magic effects are called, not changed: the
 * stasis sphere, the clock, the rewind dial and the acceleration ribbons.
 * <p>
 * The rules that keep it fair (all numbers in {@link ChronoMath}):
 * <ul>
 *   <li>a stop holds for three seconds at most, does no damage by itself, only touches those who can see him, and never those who hold
 *       anti-magic; an ally's blow of 8 health breaks it (the existing {@link TimeStop}); after it nobody can be stopped for six seconds;</li>
 *   <li>every blow that follows a stop is marked on the ground fourteen ticks before it lands, and the first one gives eighteen ticks;</li>
 *   <li>a step beyond light draws its line and its width fourteen ticks before he moves.</li>
 * </ul>
 */
final class ChronoMoves {
    private ChronoMoves() {}

    private static final ResourceLocation ACCEL_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "lucius_acceleration");

    // ================================================================ steps beyond light
    /** Where a step towards 'target' will end (the floor 2.2 blocks past it, no farther than 32 blocks from the boss). */
    static Vec3 planStep(LuciusBoss b, LivingEntity target) {
        double[] e = ChronoMath.stepEnd(b.getX(), b.getZ(), target.getX(), target.getZ(), 2.2);
        return ZogratisBoss.ground(b.level(), new Vec3(e[0], target.getY(), e[1]));
    }

    /** The telegraph of a step: its line, both edges of its strip, and a ring where it ends. */
    static void tellStep(LuciusBoss b, ServerLevel sl, Vec3 end) {
        Vec3 from = b.position().add(0, 0.25, 0), to = end.add(0, 0.25, 0);
        Vec3 d = LuciusFx.flatDir(from, to, b.getViewVector(1f));
        Vec3 side = new Vec3(-d.z, 0, d.x).scale(ChronoMath.STEP_WIDTH);
        Tells.line(sl, from, to, LuciusFx.GOLD);
        Tells.line(sl, from.add(side), to.add(side), LuciusFx.WARN);
        Tells.line(sl, from.subtract(side), to.subtract(side), LuciusFx.WARN);
        Tells.ring(sl, end, 1.3, LuciusFx.WARN, 1.2f, 20);
    }

    /**
     * Steps to 'want' faster than anything can follow: the foes in the strip between here and there are hit for 'damage' (a plain blow:
     * armour helps) and shoved along it, afterimages hang on the line. A wall shortens the step; no room at the end cancels it.
     */
    static void lightStep(LuciusBoss b, Vec3 want, float damage) {
        if (!(b.level() instanceof ServerLevel sl) || want == null || !b.isAlive()) return;
        Vec3 from = b.position();
        Vec3 flat = want.subtract(from).multiply(1, 0, 1);
        if (flat.lengthSqr() < 1.0) return;
        double len = Math.min(ChronoMath.STEP_MAX, flat.length());
        Vec3 dir = flat.normalize();
        Vec3 eye = from.add(0, 1.0, 0), far = eye.add(dir.scale(len));
        Vec3 wall = LuciusFx.clipWall(sl, b, eye, far);
        double reach = wall.distanceToSqr(far) < 1e-3 ? len : Math.max(0, eye.distanceTo(wall) - 0.8);
        Vec3 spot = LuciusFx.standable(sl, b, from.add(dir.scale(reach)));
        if (spot == null || spot.distanceToSqr(from) < 1.0) return;
        List<LivingEntity> hit = new ArrayList<>();
        for (LivingEntity v : b.foesAround(from.add(spot.subtract(from).scale(0.5)), from.distanceTo(spot) / 2 + 3)) {
            if (ChronoMath.inLine(v.getX(), v.getZ(), from.x, from.z, spot.x, spot.z, v.getBbWidth() / 2) && Math.abs(v.getY() - from.y) <= 3.0) hit.add(v);
        }
        VfxSpawn.send(sl, VfxShape.LIGHT_BLADE, from.add(0, 1.1, 0), spot.add(0, 1.1, 0), LuciusFx.GOLD_ARGB, 10, 1.3f);
        for (double f : new double[]{0.2, 0.4, 0.6, 0.8}) {
            Vec3 p = from.add(spot.subtract(from).scale(f)).add(0, 1.0, 0);
            LuciusFx.dust(sl, p, LuciusFx.GOLD, 1.6f, 12, 0.35);
            LuciusFx.sparkle(sl, p, 4, 0.3);
        }
        LuciusFx.chime(sl, from, 1.9f);
        b.teleportTo(spot.x, spot.y, spot.z);
        b.getNavigation().stop();
        LuciusFx.sound(sl, spot, SoundEvents.ENDERMAN_TELEPORT, 1.2f, 1.9f);
        for (LivingEntity v : hit) if (b.strike(v, damage, false)) v.knockback(0.5, -dir.x, -dir.z);
    }

    /** The rest of a Beyond Light barrage: 'left' more steps, each through the next foe in turn, each shown 14 ticks before it happens. */
    static void comboNext(LuciusBoss b, LivingEntity first, int left, float damage) {
        if (left <= 0 || !(b.level() instanceof ServerLevel sl) || !b.isAlive()) return;
        List<LivingEntity> foes = b.foes(36);
        if (foes.isEmpty()) return;
        LivingEntity next = foes.get(b.getRandom().nextInt(foes.size()));
        if (foes.size() > 1 && next == first) next = foes.get((foes.indexOf(next) + 1) % foes.size());
        final LivingEntity who = next;
        final Vec3 end = planStep(b, who);
        SpellRuntime.zone(sl, ChronoMath.STEP_LEAD, 3, LuciusFx.safe("light combo mark", age -> {
            if (b.isAlive()) tellStep(b, sl, end);
        }));
        b.later(ChronoMath.STEP_LEAD, LuciusFx.safe("light combo", () -> {
            lightStep(b, end, damage);
            comboNext(b, who, left - 1, damage);
        }));
    }

    // ================================================================ time stop
    /**
     * Holds one foe in stopped time for up to 'base' ticks (the balance law's control caps and diminishing returns apply). Returns the
     * ticks held, or 0 if the foe cannot be held: anti-magic in hand, a stop's grace, no line of sight to him, or already held.
     */
    static int freeze(LuciusBoss b, LivingEntity foe, int base) {
        if (!(foe.level() instanceof ServerLevel sl) || b.juliusActive()) return 0;
        if (TimeStop.isImmune(foe) || TimeStop.isFrozen(foe) || foe instanceof Player p && LuciusFx.antiMagicHeld(p) || !b.hasLineOfSight(foe)) return 0;
        int t = Math.min(ChronoMath.HARD_STOP_CAP, BalanceLaw.controlTicks(foe, base));
        TimeStop.freeze(foe, t);
        b.noteHeld(foe);
        float radius = Math.max(foe.getBbHeight(), foe.getBbWidth()) * 0.6f + 0.25f;
        Vec3 c = foe.position().add(0, foe.getBbHeight() / 2, 0);
        VfxSpawn.send(sl, new VfxPayload(VfxShape.TIME_STASIS.ordinal(), c, c, 0, t, radius / 0.6f, foe.getId(), sl.random.nextLong()));
        return t;
    }

    /** Ends the stop on 'foe' and gives it the grace: nobody can be stopped again for six seconds. */
    static void release(LivingEntity foe) { TimeStop.immune(foe, ChronoMath.GRACE); }

    /**
     * Chrono Stasis: one foe is stopped, he steps in front of it, and a mark closes on the spot where it stands; when the stop ends, the
     * mark strikes everything inside it. If the foe cannot be stopped the mark is simply drawn and the blow comes anyway (so it can be
     * walked out of). Either way the foe can leave the ring once it can move.
     */
    static void timeHalt(LuciusBoss b, LivingEntity target, Vec3 spot, float blow) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        final Vec3 c = spot != null ? spot : ZogratisBoss.ground(sl, target.position());
        int held = freeze(b, target, ChronoMath.haltTicks(b.power()));
        final int wait = Math.max(held > 0 ? held : 26, ChronoMath.MARK_LEAD + 6);     // a stop cut short by the diminishing returns still leaves time to walk out of the mark
        Vec3 dir = LuciusFx.flatDir(c, b.position(), b.getViewVector(1f));
        b.later(4, LuciusFx.safe("stasis step", () -> {
            Vec3 at = LuciusFx.standable(sl, b, c.add(dir.scale(2.4)));
            if (at != null) b.blinkTo(at);
        }));
        SpellRuntime.zone(sl, wait, 3, LuciusFx.safe("stasis mark", age -> {
            if (!b.isAlive()) return;
            boolean late = age >= wait - ChronoMath.MARK_LEAD;
            Tells.ring(sl, c, ChronoMath.BLOW_RADIUS, late ? LuciusFx.WARN : LuciusFx.GOLD, late ? 1.5f : 1.0f, 36);
            if (late) Tells.column(sl, c, 3.0, LuciusFx.WARN);
        }));
        b.later(wait, LuciusFx.safe("stasis blow", () -> {
            if (held > 0) release(target);
            VfxSpawn.send(sl, VfxShape.LIGHTNING_SPEAR, c.add(0, 8, 0), c, 0xFFFFD66E, 10, 1.0f);
            LuciusFx.sound(sl, c, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.7f, 1.7f);
            for (LivingEntity v : b.foesAround(c, ChronoMath.BLOW_RADIUS + 1)) {
                double dx = v.getX() - c.x, dz = v.getZ() - c.z;
                if (dx * dx + dz * dz > ChronoMath.BLOW_RADIUS * ChronoMath.BLOW_RADIUS || Math.abs(v.getY() - c.y) > 3.5) continue;
                if (b.strike(v, blow, false)) v.knockback(0.6, b.getX() - v.getX(), b.getZ() - v.getZ());
            }
        }));
    }

    /**
     * The Time Stop: everyone who can see him and holds no anti-magic is stopped for up to three seconds. Nothing hurts them while they
     * are held. When it ends they get the grace, and then the stopped time's blows arrive: {@link ChronoMath#comboBlows} of them,
     * each marked on the ground under every foe fourteen ticks ahead, the first after eighteen ticks.
     */
    static void timeStop(LuciusBoss b, LivingEntity target) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        int power = b.power();
        List<LivingEntity> foes = b.foes(44);
        List<LivingEntity> held = new ArrayList<>();
        int longest = 0;
        for (LivingEntity f : foes) {
            if (held.size() >= ChronoMath.MAX_HELD) break;
            int t = freeze(b, f, ChronoMath.stopTicks(power));
            if (t > 0) { held.add(f); longest = Math.max(longest, t); }
        }
        Vec3 centre = target.position();
        if (!foes.isEmpty()) {
            double x = 0, z = 0;
            for (LivingEntity f : foes) { x += f.getX(); z += f.getZ(); }
            centre = ZogratisBoss.ground(sl, new Vec3(x / foes.size(), target.getY(), z / foes.size()));
        }
        VfxSpawn.send(sl, VfxShape.TIME_CLOCK, centre, centre.add(0, 7, 0), 0, 70, 6f);
        LuciusFx.sound(sl, b.position(), SoundEvents.BELL_RESONATE, 2.5f, 0.5f);
        if (held.isEmpty()) b.announce("Time does not stop for anyone: they see through it (anti-magic, a grace, or out of his sight).");
        else {
            b.announce("Time stops for " + held.size() + (held.size() == 1 ? " fighter" : " fighters") + ": " + (longest / 20) + " seconds. Nothing hurts them while it lasts; "
                    + "the blows come after. Anti-magic and cover break it.");
            for (LivingEntity f : held) if (f instanceof ServerPlayer p) p.displayClientMessage(Component.literal("Time has stopped.").withStyle(net.minecraft.ChatFormatting.GOLD), true);
        }
        final int wait = held.isEmpty() ? 20 : longest;
        final List<LivingEntity> frozen = held;
        b.later(8, LuciusFx.safe("stop step", () -> {
            if (frozen.isEmpty()) return;
            Vec3 at = LuciusFx.standable(sl, b, frozen.get(0).position().add(LuciusFx.flatDir(frozen.get(0).position(), b.position(), b.getViewVector(1f)).scale(3.0)));
            if (at != null) b.blinkTo(at);
        }));
        b.later(wait, LuciusFx.safe("stop release", () -> {
            for (LivingEntity f : frozen) release(f);
            combo(b, sl, ChronoMath.comboBlows(b.power()));
        }));
    }

    /** The blows of the stopped time: each marked 14 ticks ahead under every foe where it stands at that moment. */
    private static void combo(LuciusBoss b, ServerLevel sl, int blows) {
        for (int k = 0; k < blows; k++) {
            final List<Vec3> marks = new ArrayList<>();
            b.later(ChronoMath.markDelay(k), LuciusFx.safe("stop mark", () -> {
                for (LivingEntity f : b.foes(48)) marks.add(ZogratisBoss.ground(sl, f.position()));
                SpellRuntime.zone(sl, ChronoMath.MARK_LEAD, 3, LuciusFx.safe("stop mark ring", age -> {
                    if (!b.isAlive()) return;
                    for (Vec3 m : marks) {
                        Tells.ring(sl, m, ChronoMath.BLOW_RADIUS, LuciusFx.WARN, 1.4f, 32);
                        Tells.column(sl, m, 3.0, LuciusFx.GOLD);
                    }
                }));
            }));
            b.later(ChronoMath.blowDelay(k), LuciusFx.safe("stop blow", () -> {
                Set<LivingEntity> struck = new HashSet<>();
                for (Vec3 m : marks) {
                    VfxSpawn.send(sl, VfxShape.LIGHTNING_SPEAR, m.add(0, 8, 0), m, 0xFFFFD66E, 10, 1.0f);
                    for (LivingEntity v : b.foesAround(m, ChronoMath.BLOW_RADIUS + 1)) {
                        double dx = v.getX() - m.x, dz = v.getZ() - m.z;
                        if (struck.contains(v) || dx * dx + dz * dz > ChronoMath.BLOW_RADIUS * ChronoMath.BLOW_RADIUS || Math.abs(v.getY() - m.y) > 3.5) continue;
                        if (b.strike(v, LuciusTable.STOP_BLOW, false)) struck.add(v);
                    }
                }
                if (!marks.isEmpty()) LuciusFx.sound(sl, marks.get(0), SoundEvents.LIGHTNING_BOLT_IMPACT, 0.9f, 1.6f);
            }));
        }
    }

    // ================================================================ time reversal
    /** Where Time Reversal would put him now: the position remembered 3 to 5 seconds ago, or null. */
    static ChronoRing.Snap rewindTarget(LuciusBoss b, long now) { return b.ring().rewind(now, ChronoMath.rewindTicks(b.power())); }

    /**
     * Time Reversal: he goes back to where he was a few seconds ago and takes back some of his wounds (never more than
     * {@link ChronoMath#rewindHealCap}), and every harmful effect on him is unmade. Anti-magic struck during the wind-up refuses it. The
     * wind-up shows where he will reappear.
     */
    static void timeReversal(LuciusBoss b, boolean refused) {
        if (!(b.level() instanceof ServerLevel sl) || !b.isAlive()) return;
        long now = sl.getGameTime();
        Vec3 here = b.position();
        VfxSpawn.send(sl, VfxShape.TIME_REWIND, here.add(0, 0.05, 0), here.add(0, 1, 0), 0, 40, 1.6f);
        LuciusFx.sound(sl, here, SoundEvents.BELL_RESONATE, 2f, 0.6f);
        if (refused) {
            b.announce("Anti-magic cuts the thread of time: Lucius cannot rewind.");
            return;
        }
        ChronoRing.Snap s = rewindTarget(b, now);
        if (s == null) return;
        for (MobEffectInstance i : new ArrayList<>(b.getActiveEffects())) {
            Holder<MobEffect> h = i.getEffect();
            if (h.value().getCategory() == MobEffectCategory.HARMFUL) b.removeEffect(h);
        }
        float before = b.getHealth();
        float after = ChronoRing.rewoundHealth(before, s.health(), b.getMaxHealth(), ChronoMath.rewindHealCap(b.power()));
        if (after > before && com.newuniverse.nusmp.boss.rules.BossHealGuard.mayHeal(b)) b.setHealth(Math.min(b.getMaxHealth(), after));       // 1.37.0: the rewind returns his place and cleanses, it no longer heals in a fight
        Vec3 to = new Vec3(s.x(), s.y(), s.z());
        Vec3 spot = to.distanceToSqr(b.arenaCenter()) < (double) ZogratisBoss.ARENA_RADIUS * ZogratisBoss.ARENA_RADIUS ? LuciusFx.standable(sl, b, to) : null;
        if (spot != null) {
            VfxSpawn.send(sl, VfxShape.TIME_REWIND, spot.add(0, 0.05, 0), spot.add(0, 1, 0), 0, 30, 1.2f);
            b.teleportTo(spot.x, spot.y, spot.z);
            b.getNavigation().stop();
        }
        b.ring().truncateAfter(s.tick());
        b.precog().suspend(now + 40);
        b.announce("Time turns back " + (ChronoMath.rewindTicks(b.power()) / 20) + " seconds for Lucius: " + Math.round(Math.max(0, b.getHealth() - before)) + " health restored, every ailment undone.");
    }

    // ================================================================ acceleration
    /** Chrono Acceleration: eight seconds of speed, faster blows, and one precognition charge back. */
    static void accelerate(LuciusBoss b) {
        if (!(b.level() instanceof ServerLevel sl) || !b.isAlive()) return;
        AttributeInstance speed = b.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.addOrUpdateTransientModifier(new AttributeModifier(ACCEL_ID, ChronoMath.ACCEL_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        b.accelerateUntil(sl.getGameTime() + ChronoMath.ACCEL_TICKS);
        b.precog().grant(b.power());
        VfxSpawn.sendFollowing(sl, VfxShape.TIME_ACCEL, b, b.position().add(0, 1, 0), 0, ChronoMath.ACCEL_TICKS, 1.15f);
        LuciusFx.chime(sl, b.position(), 1.7f);
    }

    /** Ends the acceleration. */
    static void decelerate(LuciusBoss b) {
        AttributeInstance speed = b.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(ACCEL_ID);
    }

    /** True if the entity is a foe that a stop could take now (for the tell). */
    static boolean holdable(LuciusBoss b, LivingEntity foe) {
        return !TimeStop.isImmune(foe) && !(foe instanceof Player p && LuciusFx.antiMagicHeld(p)) && b.hasLineOfSight(foe);
    }

}
