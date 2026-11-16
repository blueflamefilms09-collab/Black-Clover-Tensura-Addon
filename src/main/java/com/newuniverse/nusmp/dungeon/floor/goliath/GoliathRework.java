package com.newuniverse.nusmp.dungeon.floor.goliath;

import com.newuniverse.nusmp.boss.rework.BossRework;
import com.newuniverse.nusmp.boss.rework.ReworkFx;
import com.newuniverse.nusmp.boss.vfx.BossMoveVfx;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Boss rework wave 1 (docs/bosses/rework_wave1.md): the five moves appended to the Goliath's table ({@link GoliathTable#OLD_MOVES} onward), DanMachi's Goliath of the
 * seventeenth floor: the Magebane Roar (a ring that tears timed blessings off the foes in it), the Boulder Hurl (a boulder torn up and thrown on a marked circle), the
 * Stone Skin (half of every hit turned aside for six seconds: a shield, never a heal), the Ground-Pound Shockwave (a fast jumpable wave) and the Giant Stomp Chain (four
 * marked circles walking toward the foe). Each is telegraphed for its whole wind-up with the existing {@link Tells} and one boss effect, lands where the wind-up drew it,
 * goes through the boss's damage law, and changes no block. Worst case per cast: 24 foes looked at, 4 delayed tasks, 4 boss effects (the per-tick budget is 12).
 */
final class GoliathRework {
    private GoliathRework() {}

    private static final int MAX_VICTIMS = 24;

    private static boolean first(int left, int total) { return left >= total - 3; }

    private static double reach(GoliathBoss g, double base) { return GoliathTable.reach(base, g.gatesOpen()); }

    /** The action-bar warning every foe within reach reads once, when a wind-up begins. */
    private static void warn(GoliathBoss g, String id) {
        String w = GoliathTable.warning(id);
        if (w.isEmpty()) return;
        Component c = Component.literal(w).withStyle(ChatFormatting.GOLD);
        for (LivingEntity e : g.foes(48)) if (e instanceof ServerPlayer p) p.displayClientMessage(c, true);
    }

    static Move.MoveRun runOf(GoliathBoss g, String id) {
        return switch (id) {
            case GoliathTable.MAGEBANE_ROAR -> (b, t) -> roar(g);
            case GoliathTable.BOULDER_HURL -> (b, t) -> hurl(g, t);
            case GoliathTable.STONE_SKIN -> (b, t) -> skin(g);
            case GoliathTable.POUND_SHOCKWAVE -> (b, t) -> pound(g);
            case GoliathTable.STOMP_CHAIN -> (b, t) -> stompChain(g, t);
            default -> (b, t) -> { };
        };
    }

    static Move.MoveTell tellOf(GoliathBoss g, String id) {
        return switch (id) {
            case GoliathTable.MAGEBANE_ROAR -> (b, t, left, total) -> tellRing(g, id, reach(g, GoliathTable.ROAR_REACH), left, total);
            case GoliathTable.POUND_SHOCKWAVE -> (b, t, left, total) -> tellRing(g, id, reach(g, GoliathTable.POUND_REACH), left, total);
            case GoliathTable.BOULDER_HURL -> (b, t, left, total) -> tellHurl(g, t, left, total);
            case GoliathTable.STONE_SKIN -> (b, t, left, total) -> tellSkin(g, left, total);
            case GoliathTable.STOMP_CHAIN -> (b, t, left, total) -> tellChain(g, t, left, total);
            default -> null;
        };
    }

    // ================================================================ telegraphs
    /** A ring that grows from the feet to the reach, the reach itself now and then, dust drawn into the fists; one stone ring effect for the whole wind-up. */
    private static void tellRing(GoliathBoss g, String id, double reach, int left, int total) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            warn(g, id);
            GoliathVfx.ringTell(g, reach, total);
            GoliathMoves.sound(sl, g.position(), GoliathTable.MAGEBANE_ROAR.equals(id) ? SoundEvents.WARDEN_SONIC_CHARGE : SoundEvents.STONE_HIT, 2.5f, 0.4f);
        }
        GoliathMoves.dustRing(sl, g.position(), GoliathMath.tellRadius(reach, left, total), GoliathMoves.DUST, 1.3f);
        if (left % 12 == 0) GoliathMoves.dustRing(sl, g.position(), reach, GoliathMoves.HOT, 1.1f);
        Tells.gather(sl, g.position().add(0, g.getBbHeight() * 0.7, 0), 1.4, GoliathMoves.AMBER, 4);
    }

    /** Where the boulder lands for this foe: on them, at most 40 blocks out. */
    private static Vec3 hurlMark(GoliathBoss g, LivingEntity target) {
        Vec3 flat = target.position().subtract(g.position()).multiply(1, 0, 1);
        double d = flat.length();
        Vec3 at = d > 40 ? g.position().add(flat.scale(40 / d)) : target.position();
        return ZogratisBoss.ground(g.level(), new Vec3(at.x, target.getY(), at.z));
    }

    private static Vec3 hand(GoliathBoss g) { return g.position().add(0, g.getBbHeight() * 0.85, 0); }

    /** The circle under the foe, locked when the wind-up begins, a closing ring in it, and the arc the boulder will fly along. */
    private static void tellHurl(GoliathBoss g, LivingEntity target, int left, int total) {
        if (!(g.level() instanceof ServerLevel sl) || target == null) return;
        double r = reach(g, GoliathTable.BOULDER_RADIUS);
        if (first(left, total) || g.reworkMarks.size() != 1) {
            warn(g, GoliathTable.BOULDER_HURL);
            g.reworkMarks = List.of(hurlMark(g, target));
            BossMoveVfx.telegraph(sl, VfxShape.BOSS_STONE_RING, g.reworkMarks.get(0), g.reworkMarks.get(0).add(0, 1, 0), GoliathVfx.tint(g), total, r);
            GoliathMoves.sound(sl, g.position(), SoundEvents.STONE_BREAK, 2.5f, 0.4f);
        }
        Vec3 m = g.reworkMarks.get(0);
        GoliathMoves.dustRing(sl, m, r, GoliathMoves.HOT, 1.4f);
        GoliathMoves.dustRing(sl, m, Math.max(0.6, r * left / Math.max(1, total)), GoliathMoves.DUST, 1.1f);
        Tells.line(sl, hand(g), m.add(0, 1.0, 0), GoliathMoves.AMBER);
        Tells.gather(sl, hand(g), 1.0, GoliathMoves.DUST, 4);
    }

    /** Stone crawls over the body: dust drawn in, a ring round the feet, one stone column for the wind-up. */
    private static void tellSkin(GoliathBoss g, int left, int total) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            warn(g, GoliathTable.STONE_SKIN);
            BossMoveVfx.telegraph(sl, VfxShape.BOSS_STONE_COLUMN, g.position(), g.position().add(0, 1, 0), GoliathVfx.tint(g), total, 2.0);
            GoliathMoves.sound(sl, g.position(), SoundEvents.DEEPSLATE_BREAK, 2.5f, 0.4f);
        }
        GoliathMoves.dustRing(sl, g.position(), 2.5, GoliathMoves.DUST, 1.2f);
        Tells.gather(sl, g.position().add(0, g.getBbHeight() * 0.5, 0), 2.0, GoliathMoves.DUST, 6);
    }

    /** The stomp circles, from the feet toward the foe, locked when the wind-up begins: one stone ring effect each. */
    private static void tellChain(GoliathBoss g, LivingEntity target, int left, int total) {
        if (!(g.level() instanceof ServerLevel sl) || target == null) return;
        double r = reach(g, GoliathTable.CHAIN_RADIUS);
        if (first(left, total) || g.reworkMarks.size() != GoliathTable.CHAIN_STOMPS) {
            warn(g, GoliathTable.STOMP_CHAIN);
            Vec3 look = g.getViewVector(1f);
            double[][] pts = BossRework.chain(g.getX(), g.getZ(), target.getX(), target.getZ(), look.x, look.z, GoliathTable.CHAIN_STOMPS,
                    GoliathTable.CHAIN_FIRST, reach(g, GoliathTable.CHAIN_STEP), 22.0);
            List<Vec3> marks = new ArrayList<>(pts.length);
            for (double[] p : pts) marks.add(ZogratisBoss.ground(sl, new Vec3(p[0], g.getY() + 0.5, p[1])));
            g.reworkMarks = List.copyOf(marks);
            for (Vec3 m : marks) BossMoveVfx.telegraph(sl, VfxShape.BOSS_STONE_RING, m, m.add(0, 1, 0), GoliathVfx.tint(g), total, r);
            GoliathMoves.sound(sl, g.position(), SoundEvents.STONE_HIT, 2.5f, 0.4f);
        }
        for (Vec3 m : g.reworkMarks) Tells.ring(sl, m, r, GoliathMoves.HOT, 1.2f, 20);
    }

    // ================================================================ the moves
    /** The Magebane Roar: everyone within reach (a few blocks up or down) is struck lightly and loses up to two timed blessings. */
    static void roar(GoliathBoss g) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        double reach = reach(g, GoliathTable.ROAR_REACH);
        Vec3 c = g.position();
        GoliathMoves.sound(sl, c, SoundEvents.WARDEN_ROAR, 4.0f, 0.45f);
        BossMoveVfx.strike(sl, VfxShape.BOSS_STONE_RING, c, c.add(0, 1, 0), GoliathVfx.HOT, reach);
        ReworkFx.plain(sl, VfxShape.ANTI_MAGIC_SLASH, c.add(0, g.getBbHeight() * 0.8, 0), c.add(g.getViewVector(1f).scale(6)).add(0, g.getBbHeight() * 0.8, 0), GoliathVfx.tint(g), 16, 3.0);
        GoliathMoves.Blow blow = new GoliathMoves.Blow(GoliathTable.ROAR_BLAST, 0.2, 0.6, 40, 1);
        int seen = 0;
        for (LivingEntity e : g.foes(reach + 1.0)) {
            if (++seen > MAX_VICTIMS) break;
            double dx = e.getX() - c.x, dz = e.getZ() - c.z, d = Math.sqrt(dx * dx + dz * dz);
            if (d > reach || Math.abs(e.getY() - c.y) > 5.0) continue;
            ReworkFx.cancelMagic(e, GoliathTable.ROAR_STRIPS);
            GoliathMoves.land(g, e, blow, dx / Math.max(1e-3, d), dz / Math.max(1e-3, d), null);
        }
    }

    /** The Boulder Hurl: the boulder flies for {@link GoliathTable#BOULDER_FLIGHT} ticks along the drawn arc, then whoever is in the circle takes the blow. */
    static void hurl(GoliathBoss g, LivingEntity target) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        Vec3 m = g.reworkMarks.size() == 1 ? g.reworkMarks.get(0) : target != null ? hurlMark(g, target) : g.position();
        g.reworkMarks = List.of();
        double r = reach(g, GoliathTable.BOULDER_RADIUS);
        g.swing(InteractionHand.MAIN_HAND);
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_STONE_TRAIL, hand(g), m, GoliathVfx.tint(g), GoliathTable.BOULDER_FLIGHT + 4, Math.max(1.0, r * 0.6));
        GoliathMoves.sound(sl, g.position(), SoundEvents.STONE_BREAK, 2.5f, 0.35f);
        g.later(GoliathTable.BOULDER_FLIGHT, GoliathMoves.safe("boulder landing", () -> {
            if (!(g.level() instanceof ServerLevel level)) return;
            GoliathMoves.burst(level, m, 50, r * 0.5);
            BossMoveVfx.strike(level, VfxShape.BOSS_STONE_COLUMN, m, m.add(0, 1, 0), GoliathVfx.tint(g), Math.max(1.0, r * 0.5));
            GoliathMoves.sound(level, m, SoundEvents.ANVIL_LAND, 3.0f, 0.35f);
            GoliathMoves.Blow blow = new GoliathMoves.Blow(GoliathTable.BOULDER, 0.55, 0.7, 40, 1);
            int seen = 0;
            for (LivingEntity e : g.foesAround(m, r + 3)) {
                if (++seen > MAX_VICTIMS) break;
                double dx = e.getX() - m.x, dz = e.getZ() - m.z, d = Math.sqrt(dx * dx + dz * dz);
                if (d > r + e.getBbWidth() / 2 || e.getY() - m.y < -2.0 || e.getY() - m.y > 5.0) continue;
                GoliathMoves.land(g, e, blow, dx / Math.max(1e-3, d), dz / Math.max(1e-3, d), null);
            }
        }));
    }

    /** The Stone Skin: for {@link GoliathTable#SKIN_TICKS} ticks half of every hit is turned aside ({@code GoliathBoss.filterDamage}); nothing is healed. */
    static void skin(GoliathBoss g) {
        if (!(g.level() instanceof ServerLevel sl)) return;
        g.skinUntil = g.tickCount + GoliathTable.SKIN_TICKS;
        Vec3 c = g.position();
        BossMoveVfx.strikeFor(sl, VfxShape.BOSS_STONE_COLUMN, c, c.add(0, 1, 0), GoliathVfx.tint(g), 24, 2.4);
        GoliathMoves.burst(sl, c.add(0, g.getBbHeight() * 0.5, 0), 40, 1.4);
        GoliathMoves.sound(sl, c, SoundEvents.DEEPSLATE_PLACE, 3.0f, 0.4f);
        g.announce("The Goliath's stone hardens. For six seconds every blow does half.");
    }

    /** The Ground-Pound Shockwave: one fast ring across sixteen blocks, thin enough to be jumped. */
    static void pound(GoliathBoss g) {
        g.swing(InteractionHand.MAIN_HAND);
        g.swing(InteractionHand.OFF_HAND);
        if (g.level() instanceof ServerLevel sl) GoliathMoves.sound(sl, g.position(), SoundEvents.ANVIL_LAND, 3.0f, 0.3f);
        GoliathMoves.ringWave(g, g.position(), reach(g, GoliathTable.POUND_REACH), GoliathTable.POUND_SPEED, new GoliathMoves.Blow(GoliathTable.POUND, 0.45, 0.9, 30, 1), null);
    }

    /** The Giant Stomp Chain: the drawn circles are stomped one after another, {@link GoliathTable#CHAIN_GAP} ticks apart, sharing one budget per victim. */
    static void stompChain(GoliathBoss g, LivingEntity target) {
        if (!(g.level() instanceof ServerLevel)) return;
        List<Vec3> marks = g.reworkMarks.size() == GoliathTable.CHAIN_STOMPS ? g.reworkMarks : List.of();
        g.reworkMarks = List.of();
        if (marks.isEmpty()) return;
        double r = reach(g, GoliathTable.CHAIN_RADIUS);
        GoliathMath.DamageBudget budget = new GoliathMath.DamageBudget(GoliathTable.CHAIN_BUDGET);
        GoliathMoves.Blow blow = new GoliathMoves.Blow(GoliathTable.CHAIN_STOMP, 0.45, 0.4, 20, 0);
        for (int i = 0; i < marks.size(); i++) {
            final Vec3 m = marks.get(i);
            final int k = i;
            Runnable stomp = GoliathMoves.safe("stomp chain", () -> {
                if (!(g.level() instanceof ServerLevel level) || !g.isAlive()) return;
                g.swing(k % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
                GoliathMoves.burst(level, m, 24, r * 0.4);
                BossMoveVfx.strike(level, VfxShape.BOSS_STONE_RING, m, m.add(0, 1, 0), GoliathVfx.tint(g), r + 0.5);
                GoliathMoves.sound(level, m, SoundEvents.IRON_GOLEM_STEP, 2.5f, 0.4f);
                int seen = 0;
                for (LivingEntity e : g.foesAround(m, r + 2)) {
                    if (++seen > MAX_VICTIMS) break;
                    double dx = e.getX() - m.x, dz = e.getZ() - m.z, d = Math.sqrt(dx * dx + dz * dz);
                    if (d > r + e.getBbWidth() / 2 || Math.abs(e.getY() - m.y) > 3.0) continue;
                    GoliathMoves.land(g, e, blow, dx / Math.max(1e-3, d), dz / Math.max(1e-3, d), budget);
                }
            });
            if (i == 0) stomp.run();
            else g.later(i * GoliathTable.CHAIN_GAP, stomp);
        }
    }
}
