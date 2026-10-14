package com.newuniverse.nusmp.boss;

import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.entity.zogratis.LuciusBoss;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lucius's three cast spells (see {@link LuciusSpellTable}): the code and the telegraph of each, looked up by move id from LuciusBoss.
 * <ul>
 *   <li><b>Megiddo</b>: rings are drawn under up to six foes for the whole wind-up; then sun blasts fall on those spots one after
 *       another, four ticks apart. A foe who left the ring is not hit; nobody takes more than two blasts.</li>
 *   <li><b>Dimension Cutter</b>: a lane of torn space is drawn from him toward the target and locked when the wind-up starts; it
 *       cuts everyone inside once, and again, softer, a third of a second later. Step out of the lane.</li>
 *   <li><b>True Darkness</b>: a dark ring grows to 18 blocks over the telegraph; at the end every foe inside the ring that he can see
 *       is blinded for three seconds and loses ten points of spiritual health (never to zero, it regenerates) and takes a small hit.
 *       Leave the ring or break his line of sight.</li>
 * </ul>
 */
public final class LuciusSpells {
    private LuciusSpells() {}

    private static final int SUN = 0xFFB020, SUN_ARGB = 0xFFFFB020, DARK = 0x4B1F7A, DARK_ARGB = 0xFF4B1F7A, RIFT = 0xB58CFF, RIFT_ARGB = 0xFFB58CFF, WARN = 0xFF5A4A;

    /** True for the first call of a wind-up (the framework calls the tell every third tick). */
    private static boolean first(int left, int total) { return left >= total - 3; }

    private static void safely(String what, Runnable r) {
        try { r.run(); } catch (RuntimeException e) { com.mojang.logging.LogUtils.getLogger().warn("[nusmp] Lucius's {} failed: {}", what, e.toString()); }
    }

    // ================================================================ code
    /** The code of a spell move, or a no-op for an id that is not one of the three. */
    public static Move.MoveRun run(LuciusBoss b, String id) {
        return switch (id) {
            case LuciusNumbers.MEGIDDO -> (boss, t) -> safely("Megiddo", () -> megiddo(b, t));
            case LuciusNumbers.DIMENSION_CUTTER -> (boss, t) -> safely("Dimension Cutter", () -> cutter(b, t));
            case LuciusNumbers.TRUE_DARKNESS -> (boss, t) -> safely("True Darkness", () -> darkness(b));
            default -> (boss, t) -> { };
        };
    }

    /** The telegraph of a spell move, or null for an id that is not one of the three. */
    public static Move.MoveTell tell(LuciusBoss b, String id) {
        return switch (id) {
            case LuciusNumbers.MEGIDDO -> (boss, t, left, total) -> safely("Megiddo tell", () -> tellMegiddo(b, t, left, total));
            case LuciusNumbers.DIMENSION_CUTTER -> (boss, t, left, total) -> safely("Dimension Cutter tell", () -> tellCutter(b, t, left, total));
            case LuciusNumbers.TRUE_DARKNESS -> (boss, t, left, total) -> safely("True Darkness tell", () -> tellDarkness(b, left, total));
            default -> null;
        };
    }

    // ================================================================ Megiddo
    private static List<Vec3> pickMarks(LuciusBoss b, LivingEntity target) {
        int n = Math.min(LuciusNumbers.MEGIDDO_MAX_MARKS, 2 + b.tuning().castSlots());
        List<Vec3> out = new ArrayList<>();
        for (LivingEntity f : b.foes(48)) {
            if (out.size() >= n) break;
            out.add(LuciusBoss.ground(b.level(), f.position()));
        }
        if (out.isEmpty() && target != null) out.add(LuciusBoss.ground(b.level(), target.position()));
        return out;
    }

    private static void tellMegiddo(LuciusBoss b, LivingEntity target, int left, int total) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        LuciusKit k = LuciusProfile.kit(b);
        if (first(left, total) || k.blastMarks.isEmpty()) {
            k.blastMarks = pickMarks(b, target);
            b.announce("Megiddo: suns fall on every ringed spot, one after another. Leave the ring.");
        }
        for (Vec3 m : k.blastMarks) {
            Tells.ring(sl, m, LuciusNumbers.MEGIDDO_RADIUS, WARN, 1.3f, 40);
            Tells.ring(sl, m, Math.max(0.6, LuciusNumbers.MEGIDDO_RADIUS * left / Math.max(1, total)), SUN, 1.1f, 24);
            Tells.column(sl, m, 5.0, SUN);
        }
        Tells.gather(sl, LuciusProfile.chest(b), 1.0, SUN, 8);
    }

    private static void megiddo(LuciusBoss b, LivingEntity target) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        LuciusKit k = LuciusProfile.kit(b);
        List<Vec3> marks = k.blastMarks.isEmpty() ? pickMarks(b, target) : k.blastMarks;
        k.blastMarks = new ArrayList<>();
        SpellMath.HitBudget<UUID> budget = new SpellMath.HitBudget<>(LuciusNumbers.MEGIDDO_MAX_HITS_PER_FOE);
        int i = 0;
        for (Vec3 m : marks) {
            final Vec3 at = m;
            b.later(i * 4, () -> {
                VfxSpawn.send(sl, VfxShape.FIRE_PILLAR, at, at.add(0, 3, 0), SUN_ARGB, 14, (float) LuciusNumbers.MEGIDDO_RADIUS);
                sl.sendParticles(ParticleTypes.FLAME, at.x, at.y + 1, at.z, 30, 1.2, 1.5, 1.2, 0.05);
                sl.playSound(null, net.minecraft.core.BlockPos.containing(at), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 1.2f, 1.4f);
                for (LivingEntity f : b.foesAround(at, LuciusNumbers.MEGIDDO_RADIUS)) {
                    if (budget.take(f.getUUID())) b.strike(f, LuciusNumbers.MEGIDDO_BLAST, true);
                }
            });
            i++;
        }
    }

    // ================================================================ Dimension Cutter
    private static void lockLane(LuciusBoss b, LivingEntity target) {
        LuciusKit k = LuciusProfile.kit(b);
        Vec3 from = b.position().add(0, 0.1, 0);
        Vec3 d = target == null ? b.getViewVector(1f) : target.position().subtract(b.position());
        d = new Vec3(d.x, 0, d.z);
        if (d.lengthSqr() < 1e-4) d = new Vec3(1, 0, 0);
        k.cutterFrom = from;
        k.cutterDir = d.normalize();
    }

    private static void tellCutter(LuciusBoss b, LivingEntity target, int left, int total) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        LuciusKit k = LuciusProfile.kit(b);
        if (first(left, total) || k.cutterDir == null) {
            lockLane(b, target);
            b.announce("Dimension Cutter: space is torn along the marked lane. Step out of it.");
        }
        Vec3 from = k.cutterFrom, dir = k.cutterDir, side = new Vec3(-dir.z, 0, dir.x).scale(LuciusNumbers.CUTTER_HALF_WIDTH);
        Vec3 end = from.add(dir.scale(LuciusNumbers.CUTTER_LENGTH));
        Tells.line(sl, from.add(side), end.add(side), RIFT);
        Tells.line(sl, from.subtract(side), end.subtract(side), RIFT);
        Tells.line(sl, from.add(0, 1, 0), end.add(0, 1, 0), WARN);
        Tells.gather(sl, LuciusProfile.chest(b), 0.8, RIFT, 8);
    }

    private static void cutter(LuciusBoss b, LivingEntity target) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        LuciusKit k = LuciusProfile.kit(b);
        if (k.cutterDir == null) lockLane(b, target);
        final Vec3 from = k.cutterFrom, dir = k.cutterDir;
        k.cutterFrom = null;
        k.cutterDir = null;
        Vec3 end = from.add(dir.scale(LuciusNumbers.CUTTER_LENGTH));
        VfxSpawn.send(sl, VfxShape.THREAD_LINE, from.add(0, 1.2, 0), end.add(0, 1.2, 0), RIFT_ARGB, 10, 1.4f);
        for (int s = 4; s <= LuciusNumbers.CUTTER_LENGTH; s += 7) {
            Vec3 at = from.add(dir.scale(s)).add(0, 1.0, 0);
            VfxSpawn.send(sl, VfxShape.SPATIAL_RIFT, at, at.add(0, 0.5, 0), RIFT_ARGB, 10, 0.9f);
        }
        sl.playSound(null, b.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.6f, 0.5f);
        SpellMath.HitBudget<UUID> budget = new SpellMath.HitBudget<>(2);
        cutLane(b, sl, from, dir, 1.0f, budget);
        b.later(7, () -> {
            Tells.line(sl, from.add(0, 1.0, 0), end.add(0, 1.0, 0), RIFT);
            cutLane(b, sl, from, dir, 0.6f, budget);
        });
    }

    private static void cutLane(LuciusBoss b, ServerLevel sl, Vec3 from, Vec3 dir, float share, SpellMath.HitBudget<UUID> budget) {
        for (LivingEntity f : b.foesAround(from.add(dir.scale(LuciusNumbers.CUTTER_LENGTH / 2)), LuciusNumbers.CUTTER_LENGTH / 2 + 2)) {
            if (Math.abs(f.getY() - from.y) > 4) continue;
            if (!SpellMath.inLane(from.x, from.z, dir.x, dir.z, f.getX(), f.getZ(), LuciusNumbers.CUTTER_LENGTH, LuciusNumbers.CUTTER_HALF_WIDTH)) continue;
            if (budget.take(f.getUUID())) b.strike(f, LuciusNumbers.CUTTER_BLADE * share, true);
        }
    }

    // ================================================================ True Darkness
    private static void tellDarkness(LuciusBoss b, int left, int total) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, b.position().add(0, 0.05, 0), b.position().add(0, 1, 0), DARK_ARGB, total, 5f);
            b.announce("True Darkness: the ring is the reach of the dark. Leave it, or break his line of sight.");
        }
        double r = SpellMath.cloudRadius(left, total, LuciusNumbers.DARKNESS_RADIUS);
        Tells.ring(sl, b.position(), r, DARK, 1.5f, 72);
        if (left % 6 == 0) Tells.ring(sl, b.position(), LuciusNumbers.DARKNESS_RADIUS, WARN, 1.2f, 96);
        Tells.gather(sl, b.position().add(0, 1.2, 0), 1.2, DARK, 10);
    }

    private static void darkness(LuciusBoss b) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Tells.burst(sl, b.position().add(0, 1, 0), DARK, 80);
        sl.playSound(null, b.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.2f, 0.5f);
        for (LivingEntity f : b.foes(LuciusNumbers.DARKNESS_RADIUS)) {
            if (!b.hasLineOfSight(f)) continue;
            f.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, LuciusNumbers.DARKNESS_BLIND_TICKS, 0));
            EnergyBridge.spirit(f, LuciusNumbers.DARKNESS_SPIRIT);
            b.strike(f, LuciusNumbers.DARKNESS_TOUCH, true);
            Tells.burst(sl, f.position().add(0, 1, 0), DARK, 14);
        }
    }
}
