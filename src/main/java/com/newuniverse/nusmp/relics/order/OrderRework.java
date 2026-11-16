package com.newuniverse.nusmp.relics.order;

import com.newuniverse.nusmp.boss.rework.ReworkFx;
import com.newuniverse.nusmp.boss.vfx.BossMoveVfx;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Boss rework wave 1 (docs/bosses/rework_wave1.md): the four moves appended to the God of Order ({@link OrderTable#OLD_MOVES} onward), so the judge fights with eight:
 * the Seal of Silence (a locked ring that tears timed blessings away), the Scales of Order (a circle under the foe and its mirror across the God, struck together), the Chains
 * of Law (a corridor that holds and stings) and the Verdict Rain (a circle under every foe, at most four, struck from the sky). All are drawn on the ground in gold for the
 * whole wind-up, land where the wind-up drew them, go through the framework's damage law (one hit is capped at the pvp share), and destroy no block. The God stays out of
 * every buff (BossRegistry leaves it out); nothing here changes that. Worst case per cast: 24 foes looked at per circle, at most 4 circles, 4 boss effects.
 */
final class OrderRework {
    private OrderRework() {}

    private static final int MAX_VICTIMS = 24;

    private static boolean first(int left, int total) { return left >= total - 3; }

    static Move.MoveRun runOf(GodOfOrder god, String id) {
        return switch (id) {
            case OrderTable.SEAL_OF_SILENCE -> (boss, target) -> { seal(god, target); god.quip(id); };
            case OrderTable.SCALES_OF_ORDER -> (boss, target) -> { scales(god, target); god.quip(id); };
            case OrderTable.CHAINS_OF_LAW -> (boss, target) -> { chains(god, target); god.quip(id); };
            case OrderTable.VERDICT_RAIN -> (boss, target) -> { verdicts(god); god.quip(id); };
            default -> (boss, target) -> { };
        };
    }

    static Move.MoveTell tellOf(GodOfOrder god, String id) {
        return switch (id) {
            case OrderTable.SEAL_OF_SILENCE -> (boss, target, left, total) -> tellSeal(god, target, left, total);
            case OrderTable.SCALES_OF_ORDER -> (boss, target, left, total) -> tellScales(god, target, left, total);
            case OrderTable.CHAINS_OF_LAW -> (boss, target, left, total) -> tellChains(god, target, left, total);
            case OrderTable.VERDICT_RAIN -> (boss, target, left, total) -> tellVerdicts(god, left, total);
            default -> null;
        };
    }

    /** Strikes everyone standing within {@code r} of {@code at} (a few blocks up or down), once each across one cast; returns the struck. */
    private static List<LivingEntity> circle(GodOfOrder god, Vec3 at, double r, float damage, Set<Integer> hit) {
        List<LivingEntity> out = new ArrayList<>();
        int seen = 0;
        for (LivingEntity e : god.foesAround(at, r + 1.0)) {
            if (++seen > MAX_VICTIMS || hit.contains(e.getId())) continue;
            if (Math.abs(e.getY() - at.y) > 3.5 || Math.hypot(e.getX() - at.x, e.getZ() - at.z) > r + e.getBbWidth() / 2) continue;
            hit.add(e.getId());
            if (god.strike(e, damage, true)) out.add(e);
        }
        return out;
    }

    // ================================================================ Seal of Silence
    private static void tellSeal(GodOfOrder god, LivingEntity target, int left, int total) {
        if (!(god.level() instanceof ServerLevel sl) || target == null) return;
        if (god.mark == null || first(left, total)) {
            god.mark = ZogratisBoss.ground(sl, target.position());
            BossMoveVfx.telegraph(sl, VfxShape.BOSS_ORDER_SIGIL, god.mark, god.mark.add(0, 1, 0), OrderVfx.tint(god), total, OrderTable.SEAL_RADIUS);
        }
        Vec3 m = god.mark;
        Tells.ring(sl, m, OrderTable.SEAL_RADIUS, OrderFx.GOLD, 1.3f, 40);
        Tells.ring(sl, m, OrderTable.SEAL_RADIUS * (0.3 + 0.7 * left / (double) Math.max(1, total)), OrderFx.PALE, 1.0f, 24);
        Tells.gather(sl, god.position().add(0, 2.0, 0), 0.8, OrderFx.WHITE_GOLD, 4);
    }

    /** The seal closes: whoever is inside is struck lightly, loses up to two timed blessings and is slowed. */
    private static void seal(GodOfOrder god, LivingEntity target) {
        if (!(god.level() instanceof ServerLevel sl)) return;
        Vec3 at = god.mark != null ? god.mark : target.position();
        god.mark = null;
        god.swing(InteractionHand.MAIN_HAND);
        BossMoveVfx.strike(sl, VfxShape.BOSS_ORDER_SIGIL, at, at.add(0, 1, 0), OrderVfx.tint(god), OrderTable.SEAL_RADIUS);
        for (LivingEntity e : circle(god, at, OrderTable.SEAL_RADIUS, OrderTable.SEAL_DAMAGE, new HashSet<>())) {
            ReworkFx.cancelMagic(e, OrderTable.SEAL_STRIPS);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
        sl.playSound(null, BlockPos.containing(at), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.8f, 0.7f);
    }

    // ================================================================ Scales of Order
    /** The two pans: under the foe, and the same distance across the God (snapped to the floor). */
    private static List<Vec3> pans(GodOfOrder god, LivingEntity target) {
        Vec3 a = ZogratisBoss.ground(god.level(), target.position());
        Vec3 off = a.subtract(god.position()).multiply(1, 0, 1);
        Vec3 b = ZogratisBoss.ground(god.level(), new Vec3(god.getX() - off.x, target.getY(), god.getZ() - off.z));
        return List.of(a, b);
    }

    private static void tellScales(GodOfOrder god, LivingEntity target, int left, int total) {
        if (!(god.level() instanceof ServerLevel sl) || target == null) return;
        if (god.reworkMarks.size() != 2 || first(left, total)) {
            god.reworkMarks = pans(god, target);
            for (Vec3 p : god.reworkMarks) BossMoveVfx.telegraph(sl, VfxShape.BOSS_ORDER_SIGIL, p, p.add(0, 1, 0), OrderVfx.tint(god), total, OrderTable.SCALES_RADIUS);
        }
        float p = 1f - left / (float) Math.max(1, total);
        for (Vec3 m : god.reworkMarks) {
            Tells.ring(sl, m, OrderTable.SCALES_RADIUS, OrderFx.GOLD, 1.4f, 36);
            Tells.column(sl, m, 1.5 + 3.0 * p, OrderFx.WHITE_GOLD);
        }
        Tells.line(sl, god.reworkMarks.get(0).add(0, 0.2, 0), god.reworkMarks.get(1).add(0, 0.2, 0), OrderFx.PALE);
    }

    /** Both pans are struck at once by a beam from the sky; a foe in both is struck once. */
    private static void scales(GodOfOrder god, LivingEntity target) {
        if (!(god.level() instanceof ServerLevel sl)) return;
        List<Vec3> pans = god.reworkMarks.size() == 2 ? god.reworkMarks : pans(god, target);
        god.reworkMarks = List.of();
        god.swing(InteractionHand.MAIN_HAND);
        Set<Integer> hit = new HashSet<>();
        for (Vec3 m : pans) {
            BossMoveVfx.strikeFor(sl, VfxShape.BOSS_ORDER_BEAM, m.add(0, 16, 0), m, OrderVfx.tint(god), 14, OrderTable.SCALES_RADIUS * 0.8);
            circle(god, m, OrderTable.SCALES_RADIUS, OrderTable.SCALES_DAMAGE, hit);
            sl.playSound(null, BlockPos.containing(m), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 1.6f, 0.6f);
        }
    }

    // ================================================================ Chains of Law
    private static void tellChains(GodOfOrder god, LivingEntity target, int left, int total) {
        if (!(god.level() instanceof ServerLevel sl) || target == null) return;
        if (god.markDir == null || first(left, total)) {
            Vec3 d = target.position().subtract(god.position()).multiply(1, 0, 1);
            god.markDir = d.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : d.normalize();
            god.mark = god.position().add(0, 0.15, 0);
            Vec3 a = god.mark.add(0, 1.0, 0);
            BossMoveVfx.telegraph(sl, VfxShape.BOSS_ORDER_BEAM, a, a.add(god.markDir.scale(OrderTable.CHAINS_LENGTH)), OrderVfx.tint(god), total, OrderTable.CHAINS_HALF_WIDTH * 1.4);
        }
        Vec3 o = god.mark, d = god.markDir, side = new Vec3(-d.z, 0, d.x).scale(OrderTable.CHAINS_HALF_WIDTH), end = o.add(d.scale(OrderTable.CHAINS_LENGTH));
        Tells.line(sl, o.add(side), end.add(side), OrderFx.GOLD);
        Tells.line(sl, o.subtract(side), end.subtract(side), OrderFx.GOLD);
    }

    /** The chains run down the corridor: whoever stands in it is stung and held (slowness III for two seconds; not a hard hold). */
    private static void chains(GodOfOrder god, LivingEntity target) {
        if (!(god.level() instanceof ServerLevel sl)) return;
        Vec3 o = god.mark != null ? god.mark : god.position();
        Vec3 d = god.markDir != null ? god.markDir : new Vec3(0, 0, 1);
        god.mark = null;
        god.markDir = null;
        god.swing(InteractionHand.MAIN_HAND);
        Vec3 a = o.add(0, 1.0, 0);
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_ORDER_BEAM, a, a.add(d.scale(OrderTable.CHAINS_LENGTH)), OrderVfx.tint(god), 16, OrderTable.CHAINS_HALF_WIDTH * 1.4);
        int seen = 0;
        for (LivingEntity e : god.foesAround(o, OrderTable.CHAINS_LENGTH + 2.0)) {
            if (++seen > MAX_VICTIMS) break;
            if (!OrderGeometry.inCorridor(e.getX(), e.getY(), e.getZ(), o.x, o.y, o.z, d.x, d.z, OrderTable.CHAINS_LENGTH,
                    OrderTable.CHAINS_HALF_WIDTH + e.getBbWidth() / 2, 3.5)) continue;
            if (god.strike(e, OrderTable.CHAINS_DAMAGE, true)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, OrderTable.CHAINS_TICKS, OrderTable.CHAINS_AMP));
        }
        sl.playSound(null, BlockPos.containing(o), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 2.0f, 0.5f);
    }

    // ================================================================ Verdict Rain
    private static void tellVerdicts(GodOfOrder god, int left, int total) {
        if (!(god.level() instanceof ServerLevel sl)) return;
        if (god.reworkMarks.isEmpty() || first(left, total)) {
            List<Vec3> spots = new ArrayList<>(OrderTable.VERDICT_SPOTS);
            for (LivingEntity e : god.foes(36)) {
                spots.add(ZogratisBoss.ground(sl, e.position()));
                if (spots.size() >= OrderTable.VERDICT_SPOTS) break;
            }
            god.reworkMarks = List.copyOf(spots);
            for (Vec3 p : spots) BossMoveVfx.telegraph(sl, VfxShape.BOSS_ORDER_SIGIL, p, p.add(0, 1, 0), OrderVfx.tint(god), total, OrderTable.VERDICT_RADIUS);
        }
        for (Vec3 m : god.reworkMarks) {
            Tells.ring(sl, m, OrderTable.VERDICT_RADIUS, OrderFx.GOLD, 1.3f, 28);
            Tells.ring(sl, m, Math.max(0.6, OrderTable.VERDICT_RADIUS * left / Math.max(1, total)), OrderFx.WHITE_GOLD, 1.0f, 20);
        }
    }

    /** A beam from the sky on every circle; a foe is judged once whichever circles they stand in. */
    private static void verdicts(GodOfOrder god) {
        if (!(god.level() instanceof ServerLevel sl)) return;
        List<Vec3> spots = god.reworkMarks;
        god.reworkMarks = List.of();
        god.swing(InteractionHand.MAIN_HAND);
        Set<Integer> hit = new HashSet<>();
        for (Vec3 m : spots) {
            BossMoveVfx.strikeFor(sl, VfxShape.BOSS_ORDER_BEAM, m.add(0, 16, 0), m, OrderVfx.tint(god), 12, OrderTable.VERDICT_RADIUS * 0.8);
            circle(god, m, OrderTable.VERDICT_RADIUS, OrderTable.VERDICT_DAMAGE, hit);
        }
        if (!spots.isEmpty()) sl.playSound(null, BlockPos.containing(spots.get(0)), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 0.8f, 1.6f);
    }
}
