package com.newuniverse.nusmp.relics.order;

import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The four moves of the God of Order. All of them are telegraphed on the ground first (a ring, a line or a circle of gold that can be left), none
 * destroys a block, and every hit goes through the framework's damage law ({@code ZogratisBoss.strike}), which caps one hit at the pvp share of a
 * player's health. Their numbers are in {@link OrderTable}; the marks they lock in the wind-up live on the God.
 */
final class OrderMoves {
    private OrderMoves() {}

    /** The first call of a wind-up (the tell runs every third tick, so the first is within three ticks of the start). */
    private static boolean first(int left, int total) { return left >= total - 3; }

    static List<Move> build(GodOfOrder god) {
        List<Move> out = new ArrayList<>();
        for (OrderTable.Spec s : OrderTable.specs()) {
            Move.Builder b = Move.builder(s.id(), s.label()).minPower(s.minPower()).windUp(s.windUp()).cooldown(s.cooldown())
                    .range(s.minRange(), s.maxRange()).weight(s.weight()).recovery(s.recovery());
            if (s.ultimate()) b.ultimate();
            Move.MoveTell tell = tellOf(god, s.id());
            if (tell != null) b.tell(tell);
            out.add(b.run(runOf(god, s.id())));
        }
        return out;
    }

    private static Move.MoveRun runOf(GodOfOrder god, String id) {
        return switch (id) {
            case OrderTable.DECREE -> (boss, target) -> {
                decree(god, target);
                god.quip(id);
            };
            case OrderTable.WRIT -> (boss, target) -> {
                writ(god, target);
                god.quip(id);
            };
            case OrderTable.JUDGEMENT -> (boss, target) -> {
                judgement(god, target);
                god.quip(id);
            };
            case OrderTable.ERASURE -> (boss, target) -> erasure(god);
            default -> (boss, target) -> { };
        };
    }

    private static Move.MoveTell tellOf(GodOfOrder god, String id) {
        return switch (id) {
            case OrderTable.DECREE -> (boss, target, left, total) -> tellDecree(god, target, left, total);
            case OrderTable.WRIT -> (boss, target, left, total) -> tellWrit(god, target, left, total);
            case OrderTable.JUDGEMENT -> (boss, target, left, total) -> tellJudgement(god, target, left, total);
            case OrderTable.ERASURE -> (boss, target, left, total) -> tellErasure(god, left, total);
            default -> null;
        };
    }

    // ================================================================ Decree of Binding
    private static void tellDecree(GodOfOrder god, LivingEntity target, int left, int total) {
        if (!(god.level() instanceof ServerLevel sl) || target == null) return;
        if (god.mark == null || first(left, total)) god.mark = ZogratisBoss.ground(sl, target.position());
        Vec3 m = god.mark;
        double r = OrderTable.decreeRadius(god.gatesOpen());
        if (first(left, total)) OrderVfx.decreeTell(god, m, r, total);
        Tells.ring(sl, m, r, OrderFx.GOLD, 1.3f, 40);
        Tells.ring(sl, m, r * (0.3 + 0.7 * left / (double) Math.max(1, total)), OrderFx.WHITE_GOLD, 1.0f, 24);
        sl.sendParticles(ParticleTypes.ENCHANT, m.x, m.y + 0.8, m.z, 8, r * 0.5, 0.5, r * 0.5, 0.3);
    }

    /** A ring of binding glyphs closes on the locked spot: whoever is inside is struck lightly and slowed for four seconds. */
    private static void decree(GodOfOrder god, LivingEntity target) {
        if (!(god.level() instanceof ServerLevel sl)) return;
        Vec3 at = god.mark != null ? god.mark : target.position();
        god.mark = null;
        double r = OrderTable.decreeRadius(god.gatesOpen());
        OrderVfx.decreeStrike(god, at, r);
        for (LivingEntity e : god.foesAround(at, r + 1.0)) {
            if (Math.abs(e.getY() - at.y) > 3.5 || Math.hypot(e.getX() - at.x, e.getZ() - at.z) > r + e.getBbWidth() / 2) continue;
            if (god.strike(e, OrderTable.DECREE_DAMAGE, true))
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, OrderTable.DECREE_SLOW_TICKS, OrderTable.DECREE_SLOW_AMP));
        }
        god.swing(InteractionHand.MAIN_HAND);
        Tells.ring(sl, at, r, OrderFx.PALE, 1.6f, 48);
        for (int k = 1; k <= 3; k++) {                                                  // the glyphs close like a shutting book
            final double rr = r * (1.0 - k * 0.25);
            god.later(k * 4, () -> Tells.ring(sl, at, rr, OrderFx.GOLD, 1.4f, 32));
        }
        sl.playSound(null, BlockPos.containing(at), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 1.6f, 0.6f);
    }

    // ================================================================ Writ of Light
    private static void tellWrit(GodOfOrder god, LivingEntity target, int left, int total) {
        if (!(god.level() instanceof ServerLevel sl) || target == null) return;
        if (god.markDir == null || first(left, total)) {
            Vec3 d = target.position().subtract(god.position()).multiply(1, 0, 1);
            god.markDir = d.lengthSqr() < 1e-4 ? god.getLookAngle().multiply(1, 0, 1).normalize() : d.normalize();
            god.mark = god.position().add(0, 0.15, 0);
        }
        Vec3 o = god.mark;
        for (Vec3 d : writDirs(god, god.markDir)) {
            Vec3 side = new Vec3(-d.z, 0, d.x).scale(OrderTable.WRIT_HALF_WIDTH);
            Vec3 end = o.add(d.scale(OrderTable.WRIT_LENGTH));
            if (first(left, total)) OrderVfx.writTell(god, o, d, OrderTable.WRIT_LENGTH, total);
            Tells.line(sl, o.add(side), end.add(side), OrderFx.GOLD);
            Tells.line(sl, o.subtract(side), end.subtract(side), OrderFx.GOLD);
            sl.sendParticles(ParticleTypes.ENCHANT, o.x + d.x * 1.5, o.y + 1.8, o.z + d.z * 1.5, 6, 0.4, 0.4, 0.4, 0.3);
        }
    }

    /** One line of light, and at the Open Ledger (the Gates) a second one across it, a quarter turn to the side: the wind-up draws both. */
    private static Vec3[] writDirs(GodOfOrder god, Vec3 d) {
        return god.gatesOpen() ? new Vec3[]{d, new Vec3(-d.z, 0, d.x)} : new Vec3[]{d};
    }

    /** A line of light 20 blocks long and under three wide leaves the staff: whoever stands in it is struck and thrown to the side. */
    private static void writ(GodOfOrder god, LivingEntity target) {
        if (!(god.level() instanceof ServerLevel sl)) return;
        Vec3 o = god.mark != null ? god.mark : god.position();
        Vec3 d0 = god.markDir;
        if (d0 == null) {
            Vec3 f = god.getLookAngle().multiply(1, 0, 1);
            d0 = f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
        }
        god.mark = null;
        god.markDir = null;
        god.swing(InteractionHand.MAIN_HAND);
        java.util.Set<Integer> hit = new java.util.HashSet<>();
        for (Vec3 d : writDirs(god, d0)) {
            Vec3 end = o.add(d.scale(OrderTable.WRIT_LENGTH));
            OrderVfx.writStrike(god, o, d, OrderTable.WRIT_LENGTH);
            Tells.line(sl, o.add(0, 1.0, 0), end.add(0, 1.0, 0), OrderFx.WHITE_GOLD);
            for (int i = 1; i <= 12; i++) {
                Vec3 p = o.add(d.scale(OrderTable.WRIT_LENGTH * i / 12.0));
                sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 1.0, p.z, 3, 0.15, 0.4, 0.15, 0.02);
            }
            for (LivingEntity e : god.foesAround(o, OrderTable.WRIT_LENGTH + 2.0)) {
                if (hit.contains(e.getId())) continue;
                if (!OrderGeometry.inCorridor(e.getX(), e.getY(), e.getZ(), o.x, o.y, o.z, d.x, d.z, OrderTable.WRIT_LENGTH,
                        OrderTable.WRIT_HALF_WIDTH + e.getBbWidth() / 2, 3.5)) continue;
                hit.add(e.getId());
                if (god.strike(e, OrderTable.WRIT_DAMAGE, true)) {
                    double side = Math.signum((e.getX() - o.x) * -d.z + (e.getZ() - o.z) * d.x);
                    Vec3 push = new Vec3(-d.z * (side == 0 ? 1 : side), 0, d.x * (side == 0 ? 1 : side));
                    e.knockback(0.7, -push.x, -push.z);
                    e.hurtMarked = true;
                }
            }
        }
        sl.playSound(null, BlockPos.containing(o), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.8f, 1.6f);
    }

    // ================================================================ Circle of Judgement
    private static void tellJudgement(GodOfOrder god, LivingEntity target, int left, int total) {
        if (!(god.level() instanceof ServerLevel sl) || target == null) return;
        if (god.mark == null || first(left, total)) god.mark = ZogratisBoss.ground(sl, target.position());
        Vec3 m = god.mark;
        double r = OrderTable.judgementRadius(god.gatesOpen());
        if (first(left, total)) OrderVfx.judgementTell(god, m, r, total);
        float p = 1f - left / (float) Math.max(1, total);
        Tells.ring(sl, m, r, OrderFx.GOLD, 1.5f, 48);
        if (p > 0.5f) Tells.ring(sl, m, r * 0.55, OrderFx.WHITE_GOLD, 1.2f, 28);
        Tells.column(sl, m, 1.5 + 4.0 * p, OrderFx.WHITE_GOLD);
        sl.sendParticles(ParticleTypes.ENCHANT, m.x, m.y + 1.2, m.z, 10, r * 0.5, 1.0, r * 0.5, 0.4);
    }

    /** The circle is struck: one heavy hit on everyone in it. */
    private static void judgement(GodOfOrder god, LivingEntity target) {
        if (!(god.level() instanceof ServerLevel sl)) return;
        Vec3 at = god.mark != null ? god.mark : target.position();
        god.mark = null;
        double r = OrderTable.judgementRadius(god.gatesOpen());
        OrderVfx.judgementStrike(god, at, r);
        god.swing(InteractionHand.MAIN_HAND);
        for (LivingEntity e : god.foesAround(at, r + 1.0)) {
            if (Math.abs(e.getY() - at.y) > 3.5 || Math.hypot(e.getX() - at.x, e.getZ() - at.z) > r + e.getBbWidth() / 2) continue;
            god.strike(e, OrderTable.JUDGEMENT_DAMAGE, true);
        }
        Tells.burst(sl, at.add(0, 0.8, 0), OrderFx.GOLD, 60);
        for (int i = 0; i < 10; i++) sl.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.4 + i * 0.5, at.z, 3, 0.25, 0.1, 0.25, 0.03);
        sl.playSound(null, BlockPos.containing(at), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 0.9f, 1.5f);
        sl.playSound(null, BlockPos.containing(at), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 2.0f, 0.5f);
    }

    // ================================================================ Erasure
    /** A great ring opens round the God and a column of glyphs rises: it will end the matter. */
    static void tellErasure(GodOfOrder god, int left, int total) {
        if (!(god.level() instanceof ServerLevel sl)) return;
        double r = 3.0 + (total - left) * 0.12;
        if (first(left, total)) OrderVfx.erasureTell(god, total);
        Tells.ring(sl, god.position(), r, OrderFx.GOLD, 1.4f, 56);
        Tells.column(sl, god.position(), 5.0, OrderFx.WHITE_GOLD);
        sl.sendParticles(ParticleTypes.ENCHANT, god.getX(), god.getY() + 2.0, god.getZ(), 16, 1.2, 1.4, 1.2, 0.6);
    }

    /**
     * It takes the duplicates of everyone who struck it (during the census it only counts), and goes. A God that has done this once does not do it
     * twice, whichever way it was started.
     */
    static void erasure(GodOfOrder god) {
        if (god.erased || !(god.level() instanceof ServerLevel sl) || sl.getServer() == null) return;
        god.erased = true;
        OrderVfx.erasureStrike(god);
        List<ServerPlayer> fighters = new ArrayList<>();
        double reach2 = OrderTable.ERASURE_REACH * OrderTable.ERASURE_REACH;
        for (UUID id : god.attackers()) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
            if (p != null && p.isAlive() && p.level() == sl && p.distanceToSqr(god) <= reach2) fighters.add(p);
        }
        Tells.burst(sl, god.position().add(0, 1.6, 0), OrderFx.WHITE_GOLD, 60);
        OrderDirector.erased(sl.getServer(), god, fighters);
    }
}
