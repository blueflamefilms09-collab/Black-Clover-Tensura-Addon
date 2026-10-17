package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.sds.kit.FourSinsKit;
import com.newuniverse.nusmp.arc.sds.kit.FourSinsMath;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.DanteMoves;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * Why: the parts of Gowther's moves that touch the world and need nothing of the boss's own state (a fan of arrows, the rings of an illusion drawn and struck, the
 * string pull, the Rewrite lane, the rush), kept apart from {@link GowtherBoss} so that class can stay about the fight's rules (the reading, the dome, the echo
 * trail). Two rules run through all of it. First, the CODE OF AN ILLUSION is the same in every move that uses one, so a player can learn it once: a ring that is
 * REAL is gold, steady and has a column of light over it; an ILLUSION is violet, flickers and has no column, and when the moment comes it pops harmlessly. Second,
 * nothing here takes a player's body, aim or controls: the only effects that touch a player's hands are the grimoire silence of Rewrite Light and the short pull
 * of the strings, both through {@code BalanceLaw.beginControl / controlTicks} (three seconds at the most, less on a repeat, never creative, spectators or a holder
 * of anti-magic), and both avoidable by a clear rule (leave the lane; break the line of sight). Damage goes through {@code boss.strike} (the Zogratis damage law).
 * Nothing changes a block or spawns an entity.
 */
final class GowtherMoves {
    private GowtherMoves() {}

    /** The code of an illusion: a real ring is gold and steady, an illusion is violet and flickers. */
    static final int REAL = 0xFFD34D, FAKE = 0xA070FF;
    private static final int RGB = GowtherSheet.RGB, ACCENT = GowtherSheet.ACCENT;
    private static final int PINK = 0xFF000000 | RGB, PALE = 0xFF000000 | ACCENT;
    private static final String WHO = "Gowther";
    /** The arrows' flight, in blocks per tick, and their lifetime. */
    static final double ARROW_SPEED = 1.35;
    static final int ARROW_LIFE = 30;

    /** One ring of an illusion: where it lies on the ground and whether it will strike. */
    record Ring(Vec3 at, boolean real) {}

    static IntConsumer safe(String what, IntConsumer step) { return FourSinsKit.safe(WHO, what, step); }

    static Runnable safe(String what, Runnable task) { return FourSinsKit.safe(WHO, what, task); }

    private static boolean holdsAntiMagic(LivingEntity e) { return e instanceof Player p && DanteMoves.antiMagicHeld(p); }

    // ================================================================ Herritt: the twin bows
    /**
     * {@code count} arrows in a fan round {@code aim}, alternately from his two bows, two at a time and three ticks apart. They fly straight (a bow's arrow does not
     * bend) and hurt the first foe they touch; the fan is drawn in the telegraph, so the way out is sideways.
     */
    static void arrows(GowtherBoss b, Vec3 aim, int count, float damage) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 axis = aim == null || aim.lengthSqr() < 1e-4 ? b.getViewVector(1f) : aim.normalize();
        int n = Math.max(1, count);
        double[] fan = FourSinsMath.fan(n, Math.min(1.0, 0.13 * n));
        for (int i = 0; i < n; i++) {
            final double offset = fan[i];
            final int side = i % 2 == 0 ? 1 : -1;
            Runnable launch = () -> {
                Vec3 dir = FourSinsKit.rotateY(axis, offset);
                Vec3 right = new Vec3(-dir.z, 0, dir.x);
                right = right.lengthSqr() < 1e-6 ? Vec3.ZERO : right.normalize();
                Vec3 origin = b.position().add(0, b.getBbHeight() * 0.7, 0).add(right.scale(0.55 * side));
                fly(b, sl, origin.add(dir.scale(0.6)), dir, ARROW_LIFE, damage);
                FourSinsKit.sound(sl, b.position(), SoundEvents.CROSSBOW_SHOOT, 0.9f, 1.3f + 0.1f * side);
            };
            if (i < 2) launch.run();
            else b.later((i / 2) * 3, safe("arrow", launch));
        }
    }

    /** One arrow of Herritt: a straight bolt of pale light that hurts the first foe it touches and knocks it back a little. */
    private static void fly(GowtherBoss b, ServerLevel sl, Vec3 start, Vec3 dir, int life, float damage) {
        new FourSinsKit.Bolt(b, start, dir, ARROW_SPEED, life, 0.7, 0.0, null, ACCENT, e -> {
            if (b.strike(e, damage, false)) e.knockback(0.25, b.getX() - e.getX(), b.getZ() - e.getZ());
        }).launch(sl);
    }

    /** The two starting points of a Cross Lock, left and right of him, 3.5 blocks out (so the two lanes meet exactly on the mark). */
    static Vec3[] crossStarts(GowtherBoss b, Vec3 mark) {
        Vec3 toMark = FourSinsKit.flatDir(b.position(), mark, b.getViewVector(1f));
        Vec3 side = new Vec3(-toMark.z, 0, toMark.x).scale(3.5);
        Vec3 base = b.position().add(0, b.getBbHeight() * 0.7, 0);
        return new Vec3[]{base.add(side), base.subtract(side)};
    }

    /** The lane of one arrow of a Cross Lock: from its start through the mark (and nine blocks past it). Returns the direction; the length is in {@code lengthOut[0]}. */
    static Vec3 crossLane(Vec3 start, Vec3 mark, double[] lengthOut) {
        Vec3 aim = mark.add(0, 1.0, 0).subtract(start);
        double d = aim.length();
        if (lengthOut != null && lengthOut.length > 0) lengthOut[0] = d + 9;
        return d < 1e-4 ? new Vec3(0, 0, 1) : aim.scale(1.0 / d);
    }

    /** Two arrows that cross on the mark: each flies its own lane and hurts the first foe on it; a foe standing on the mark is on both. */
    static void crossLock(GowtherBoss b, Vec3 mark, float damage) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        for (Vec3 start : crossStarts(b, mark)) {
            double[] len = new double[1];
            Vec3 dir = crossLane(start, mark, len);
            fly(b, sl, start.add(dir.scale(0.6)), dir, (int) Math.ceil(len[0] / ARROW_SPEED) + 2, damage);
        }
        FourSinsKit.sound(sl, b.position(), SoundEvents.CROSSBOW_SHOOT, 1.1f, 0.9f);
        Tells.burst(sl, mark.add(0, 0.2, 0), ACCENT, 10);
    }

    // ================================================================ rings: the garden and the dream
    /**
     * Plans the rings of an illusion over {@code foes}: {@code realPerFoe} real rings for every foe (the first on their feet) and two illusions for every real ring (see
     * {@link MarkMix}). Rings outside {@code limitRadius} of {@code limitCentre} (when given) are dropped, and so is any spot with no floor under it.
     */
    static List<Ring> plan(GowtherBoss b, List<LivingEntity> foes, int realPerFoe, Vec3 limitCentre, double limitRadius) {
        List<Ring> out = new ArrayList<>();
        if (foes.isEmpty()) return out;
        double[][] pos = new double[foes.size()][];
        for (int i = 0; i < foes.size(); i++) pos[i] = new double[]{foes.get(i).getX(), foes.get(i).getZ()};
        List<MarkMix.Mark> marks = MarkMix.plan(pos, realPerFoe, GowtherTable.falsePerReal(), 4.0, 1.7, GowtherTable.MAX_MARKS, new Random(b.getRandom().nextLong()));
        for (int i = 0; i < marks.size(); i++) {
            MarkMix.Mark m = marks.get(i);
            if (limitCentre != null && Math.hypot(m.x() - limitCentre.x, m.z() - limitCentre.z) > limitRadius - 1.0) continue;
            LivingEntity near = foes.get(0);
            double best = Double.MAX_VALUE;
            for (LivingEntity f : foes) {
                double d = Math.hypot(f.getX() - m.x(), f.getZ() - m.z());
                if (d < best) {
                    best = d;
                    near = f;
                }
            }
            Vec3 want = new Vec3(m.x(), near.getY(), m.z());
            Vec3 floor = ZogratisBoss.groundOrNull(b.level(), want);
            if (floor == null) {
                if (i >= foes.size()) continue;                        // an extra ring over a pit is dropped; a foe's own ring stays where the foe is
                floor = want;
            }
            out.add(new Ring(floor, m.real()));
        }
        return out;
    }

    /**
     * Draws the rings of a telegraph. A real ring is steady: its rim, a ring closing in as the time runs out, and a column of light. An illusion has none of that: a
     * thin violet rim drawn on alternate calls only, so it flickers. {@code flickerOn} says which half of the flicker this call is.
     */
    static void drawRings(ServerLevel sl, List<Ring> rings, double radius, int left, int total, boolean flickerOn) {
        int real = 0;
        for (Ring r : rings) {
            if (r.real()) {
                if (real++ < 6) {
                    FourSinsKit.markTell(sl, r.at(), radius, left, total, REAL, REAL);
                } else {                                                      // a crowd of real rings (four foes at the Gates) is drawn without the closing ring, to keep the dust bounded
                    Tells.ring(sl, r.at(), radius, REAL, 1.4f, 20);
                    Tells.column(sl, r.at(), 4, REAL);
                }
            } else if (flickerOn) {
                Tells.ring(sl, r.at(), radius, FAKE, 1.0f, 12);
            }
        }
    }

    /**
     * The rings land. Every foe inside a real ring is struck (once in all, however many rings it stands in); an illusion pops in a puff of violet and does nothing.
     * Returns how many foes were struck.
     */
    static int blast(GowtherBoss b, List<Ring> rings, double radius, float damage) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        Set<Integer> struck = new HashSet<>();
        int shown = 0;
        for (Ring r : rings) {
            Vec3 at = r.at();
            if (!r.real()) {
                Tells.burst(sl, at.add(0, 0.5, 0), FAKE, 6);
                continue;
            }
            if (shown++ < 8) VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, at.add(0, 0.05, 0), at.add(0, 1, 0), PINK, 14, (float) (radius / 1.4));
            Tells.burst(sl, at.add(0, 0.8, 0), REAL, 12);
            for (LivingEntity e : FourSinsKit.inDisc(b, at, radius, 2, 4)) {
                if (!struck.add(e.getId())) continue;
                if (b.strike(e, damage, true)) e.knockback(0.5, at.x - e.getX(), at.z - e.getZ());
            }
        }
        if (!rings.isEmpty()) FourSinsKit.sound(sl, b.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.8f, 0.8f);
        return struck.size();
    }

    // ================================================================ Echo of Memory
    /** One echo lands: a pale ring where the foe stood, and everyone inside is struck. */
    static void echoAt(GowtherBoss b, Vec3 at) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, at.add(0, 0.05, 0), at.add(0, 1, 0), PALE, 12, (float) (GowtherTable.ECHO_RADIUS / 1.4));
        Tells.burst(sl, at.add(0, 0.8, 0), ACCENT, 10);
        FourSinsKit.sound(sl, at, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 0.6f);
        for (LivingEntity e : FourSinsKit.inDisc(b, at, GowtherTable.ECHO_RADIUS, 2, 4)) b.strike(e, GowtherTable.ECHO, true);
    }

    /** The ring of an echo in a telegraph: its rim and a pale ghost standing in it (an echo is a memory, so it has neither the gold of a real ring nor the flicker of an illusion). */
    static void drawEcho(ServerLevel sl, Vec3 at, int left, int total) {
        Tells.ring(sl, at, GowtherTable.ECHO_RADIUS, ACCENT, 1.2f, 18);
        Tells.ring(sl, at, Math.max(0.5, GowtherTable.ECHO_RADIUS * left / Math.max(1, total)), RGB, 1.0f, 12);
        Tells.gather(sl, at.add(0, 1.0, 0), 0.35, ACCENT, 3);
    }

    // ================================================================ Marionette Strings
    /** Can his hand see the foe's chest: no wall in between. */
    static boolean seen(GowtherBoss b, LivingEntity e) {
        if (!(b.level() instanceof ServerLevel sl)) return false;
        Vec3 hand = b.position().add(0, b.getBbHeight() * 0.7, 0);
        Vec3 chest = e.position().add(0, e.getBbHeight() * 0.7, 0);
        return FourSinsKit.clipWall(sl, b, hand, chest).distanceToSqr(chest) < 0.25;
    }

    /** The foes the strings can reach now: within range, in sight. */
    static List<LivingEntity> reachable(GowtherBoss b) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : b.foes(GowtherTable.STRING_RANGE)) {
            if (out.size() >= 4) break;
            if (StringPull.reaches(Math.hypot(e.getX() - b.getX(), e.getZ() - b.getZ()), GowtherTable.STRING_RANGE) && seen(b, e)) out.add(e);
        }
        return out;
    }

    /** The threads of the strings from his hand to a foe's head, redrawn every few ticks so they follow. */
    static void drawThread(GowtherBoss b, LivingEntity e, int ticks) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        Vec3 hand = b.position().add(0, b.getBbHeight() * 0.7, 0);
        VfxSpawn.send(sl, VfxShape.THREAD_STRINGS, hand, e.position().add(0, e.getBbHeight() * 0.9, 0), PINK, ticks, 1f);
    }

    /**
     * The strings pull. Each foe still in sight is dragged along the ground towards him, a block a tick at the most, for at most twelve ticks, and the foe who arrives
     * next to him is struck once. A foe he can no longer see is not pulled (the thread snaps on the wall); a summon, a creative player, a spectator or a holder of
     * anti-magic is struck where it stands for half instead. One shared control cool-down is paid for the whole move; if it is running nobody is pulled. Returns how
     * many were pulled.
     */
    static int pull(GowtherBoss b, List<LivingEntity> targets) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        boolean allowed = BalanceLaw.beginControl(b);
        int pulled = 0;
        for (LivingEntity e : targets) {
            if (e == null || !e.isAlive()) continue;
            if (!seen(b, e)) {
                FourSinsKit.note(e, "The thread snaps on the wall.", ChatFormatting.GRAY);
                continue;
            }
            int ticks = 0;
            if (allowed && e instanceof ServerPlayer && FourSinsKit.fair(e) && !holdsAntiMagic(e))
                ticks = Math.min(GowtherTable.STRINGS_TICKS, BalanceLaw.controlTicks(e, GowtherTable.STRINGS_TICKS));
            if (ticks <= 0) {
                b.strike(e, GowtherTable.STRING_STRIKE * 0.5f, false);
                continue;
            }
            pulled++;
            final LivingEntity foe = e;
            final int length = ticks;
            FourSinsKit.note(e, "Marionette Strings: Gowther pulls you in.", ChatFormatting.LIGHT_PURPLE);
            SpellRuntime.zone(sl, length, 1, safe("string", age -> pullStep(b, foe, age)));
            b.later(length + 1, safe("string slam", () -> slam(b, foe)));
        }
        FourSinsKit.sound(sl, b.position(), SoundEvents.CHAIN_PLACE, 1.5f, 1.5f);
        return pulled;
    }

    private static void pullStep(GowtherBoss b, LivingEntity e, int age) {
        if (!e.isAlive() || !b.isAlive()) return;
        double dx = b.getX() - e.getX(), dz = b.getZ() - e.getZ(), d = Math.hypot(dx, dz);
        double v = StringPull.speed(d, GowtherTable.STRING_STOP);
        if (v <= 0 || d < 1e-6) return;
        Vec3 old = e.getDeltaMovement();
        e.setDeltaMovement(dx / d * v, old.y, dz / d * v);
        e.hurtMarked = true;
        if (age % 4 == 0) drawThread(b, e, 6);
    }

    private static void slam(GowtherBoss b, LivingEntity e) {
        if (!e.isAlive() || !b.isAlive() || !(b.level() instanceof ServerLevel sl)) return;
        double d = Math.hypot(b.getX() - e.getX(), b.getZ() - e.getZ());
        if (d > GowtherTable.STRING_STOP + 2.0) return;                       // not pulled all the way: the strings go slack
        Tells.burst(sl, e.position().add(0, 1, 0), RGB, 14);
        FourSinsKit.sound(sl, e.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.8f);
        if (b.strike(e, GowtherTable.STRING_STRIKE, false)) e.knockback(0.7, b.getX() - e.getX(), b.getZ() - e.getZ());
    }

    // ================================================================ Rewrite Light
    /**
     * The lane of rewriting light: every foe in it is struck, and a player's grimoire is shut for a moment, their Mana Zone switched off and the item in hand let go
     * of (nothing is taken or spent), under the balance law. Returns how many were rewritten.
     */
    static int rewrite(GowtherBoss b, Vec3 from, Vec3 dir) {
        if (!(b.level() instanceof ServerLevel sl)) return 0;
        Vec3 end = FourSinsKit.clipWall(sl, b, from, from.add(dir.scale(GowtherTable.REWRITE_LENGTH)));
        VfxSpawn.send(sl, VfxShape.MIRROR_RAY, from, end, PALE, 10, 1.2f);
        FourSinsKit.sound(sl, from, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.8f, 1.6f);
        List<LivingEntity> hit = FourSinsKit.inLine(b, from, end, GowtherTable.REWRITE_WIDTH);
        if (hit.isEmpty()) return 0;
        boolean allowed = BalanceLaw.beginControl(b);
        int rewritten = 0;
        for (LivingEntity e : hit) {
            b.strike(e, GowtherTable.REWRITE, true);
            if (allowed && FourSinsKit.forget(e, 40) > 0) {
                rewritten++;
                FourSinsKit.note(e, "Rewrite Light: your hands forget what they held.", ChatFormatting.LIGHT_PURPLE);
            }
        }
        return rewritten;
    }

    // ================================================================ Puppet Rush
    /** Two quick swipes of the doll's arm, nine ticks apart, each striking everything in a cone in front of him. */
    static void rush(GowtherBoss b, LivingEntity target) {
        swipe(b, target);
        b.later(9, safe("rush second", () -> swipe(b, target)));
    }

    private static void swipe(GowtherBoss b, LivingEntity target) {
        if (!(b.level() instanceof ServerLevel sl) || !b.isAlive()) return;
        Vec3 dir = FourSinsKit.flatDir(b.position(), target != null && target.isAlive() ? target.position() : b.position().add(b.getViewVector(1f)), b.getViewVector(1f));
        Vec3 tip = b.position();
        b.getLookControl().setLookAt(tip.x + dir.x * 4, tip.y + 1.4, tip.z + dir.z * 4);
        FourSinsKit.sound(sl, tip, SoundEvents.PLAYER_ATTACK_SWEEP, 1.3f, 0.7f);
        Tells.burst(sl, tip.add(dir.scale(2.0)).add(0, 1.2, 0), ACCENT, 14);
        for (LivingEntity e : FourSinsKit.inCone(b, tip, dir, GowtherTable.RUSH_RADIUS, 0.85)) {
            if (b.strike(e, GowtherTable.RUSH, false)) e.knockback(0.8, -dir.x, -dir.z);
        }
    }
}
