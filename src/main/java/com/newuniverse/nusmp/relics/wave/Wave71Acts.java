package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.relics.ActBody;
import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the active abilities of the 35 weapons of wave 71. Each method returns an {@link ActBody} of one SHAPE (a cone cleave, a piercing line, a nova, a blink strike, an
 * execution, a chain, a ward, a rally, a drain, a pull, a volley, a rush, a storm); a weapon's flavour is the shape, its numbers and its effect. Every body runs inside
 * {@link SafeCast#tick} (a fault is logged once and never reaches the server tick), takes its foes only from {@link Fx#foesAround} and {@code Wave20Util}
 * (hostile, never allies, pets, summons, creative or spectator players), deals every blow through {@link Fx} (the balance law and the cap on players), clamps
 * every radius, count and duration, and draws its effect with one of the mod's {@link VfxShape}s plus a few Rukh particles. Nothing here keeps state.
 */
final class Wave71Acts {
    private Wave71Acts() {}

    static final int MAX_HITS = 12;

    /** Runs the body under the shared guard, once per use. */
    private static ActBody guard(String key, ActBody body) {
        return ctx -> {
            if (ctx.player() == null || !ctx.player().isAlive()) return;
            SafeCast.tick("relic.w71." + key, () -> body.run(ctx));
        };
    }

    // ================================================================ small helpers
    private static void fx(ServerLevel l, VfxShape s, Vec3 from, Vec3 to, int rgb, int ticks, float power) {
        if (l == null || s == null) return;
        VfxSpawn.send(l, s, from, to, 0xFF000000 | (rgb & 0xFFFFFF), Mth.clamp(ticks, 4, 60), power);
    }

    private static Vec3 chest(ServerPlayer p) { return p.position().add(0, p.getBbHeight() * 0.7, 0); }

    private static Vec3 body(LivingEntity e) { return e.position().add(0, e.getBbHeight() * 0.55, 0); }

    private static void spark(ServerLevel l, Vec3 at, int rgb, int n) {
        if (l == null) return;
        RukhFx.spray(l, Fx.align(rgb), at.x, at.y, at.z, Mth.clamp(n, 1, 24), 0.35, 0.35, 0.35, 0.05);
    }

    private static void hit(ServerPlayer p, LivingEntity t, float dmg, String type, boolean heavy, String eff, int secs, int amp) {
        if (heavy) Fx.damageHeavy(p, t, dmg, type); else Fx.damage(p, t, dmg, type);
        if (eff != null && !eff.isEmpty() && secs > 0) Fx.addEffect(t, eff, secs * 20, amp);
    }

    /** The foes within range that stand inside a cone of {@code arcDeg} degrees round the look direction (or within one block, so a close foe is never missed). */
    private static List<LivingEntity> inCone(ServerPlayer p, double range, double arcDeg) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        double min = Math.cos(Math.toRadians(Mth.clamp(arcDeg, 10, 360) / 2.0));
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : Fx.foesAround(p, range)) {
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            double len = to.length();
            if (len < 1.2 || to.scale(1.0 / len).dot(look) >= min) out.add(t);
            if (out.size() >= MAX_HITS) break;
        }
        return out;
    }

    private static int glow(ActCtx c) { return c.def().paletteGlow(); }

    // ================================================================ the shapes
    /** A sweeping cut: every foe in a cone in front takes the blow and the status. */
    static ActBody cone(double range, double arcDeg, float dmg, String type, String eff, int secs, int amp, VfxShape shape) {
        return guard("cone", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            Vec3 look = p.getLookAngle();
            fx(l, shape, chest(p), chest(p).add(look.scale(range)), glow(c), 14, (float) (range * 0.5));
            for (LivingEntity t : inCone(p, range, arcDeg)) {
                hit(p, t, dmg, type, false, eff, secs, amp);
                spark(l, body(t), glow(c), 6);
            }
        });
    }

    /** A thrust or a beam: every foe along the look line (up to the first wall) takes the blow. */
    static ActBody line(double range, double width, float dmg, String type, String eff, int secs, int amp, VfxShape shape) {
        return guard("line", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            double len = Wave20Util.lineLength(p, Mth.clamp(range, 2, 40));
            Vec3 from = p.getEyePosition().add(0, -0.3, 0), to = from.add(p.getLookAngle().scale(len));
            fx(l, shape, from, to, glow(c), 12, (float) Math.max(0.8, width));
            for (LivingEntity t : Wave20Util.foesOnLine(p, range, width, MAX_HITS)) {
                hit(p, t, dmg, type, false, eff, secs, amp);
                spark(l, body(t), glow(c), 5);
            }
        });
    }

    /** A burst round the wielder: damage, a status and a shove outward. */
    static ActBody nova(double radius, float dmg, String type, String eff, int secs, int amp, double knock, VfxShape shape) {
        return guard("nova", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            fx(l, shape, p.position(), p.position().add(0, 1, 0), glow(c), 16, (float) radius);
            Fx.ring(l, p.position(), Math.min(radius, 12.0), glow(c));
            for (LivingEntity t : Fx.foesAround(p, radius)) {
                hit(p, t, dmg, type, false, eff, secs, amp);
                if (knock > 0) {
                    Vec3 d = t.position().subtract(p.position());
                    double len = Math.max(0.5, Math.hypot(d.x, d.z));
                    t.setDeltaMovement(t.getDeltaMovement().add(d.x / len * knock, 0.25 + knock * 0.1, d.z / len * knock));
                    t.hurtMarked = true;
                }
            }
        });
    }

    /** A step along the look line, then a burst where the wielder lands. */
    static ActBody blinkStrike(double range, double radius, float dmg, String type, String eff, int secs, int amp, VfxShape shape) {
        return guard("blink", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            Vec3 start = chest(p);
            boolean moved = Fx.teleportLook(p, range);
            if (!moved) Fx.say(p, "There is no room to step into.");
            fx(l, shape, start, chest(p), glow(c), 14, (float) radius);
            Fx.ring(l, p.position(), Math.min(radius, 12.0), glow(c));
            for (LivingEntity t : Fx.foesAround(p, radius)) hit(p, t, dmg, type, false, eff, secs, amp);
        });
    }

    /** A dash that cuts everything on its path. */
    static ActBody rush(double dist, double width, float dmg, String type, String eff, int secs, int amp, VfxShape shape) {
        return guard("rush", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            Vec3 from = chest(p);
            for (LivingEntity t : Wave20Util.foesOnLine(p, dist, width, MAX_HITS)) hit(p, t, dmg, type, false, eff, secs, amp);
            fx(l, shape, from, from.add(p.getLookAngle().scale(dist)), glow(c), 12, (float) Math.max(1.0, width));
            Fx.dash(p, dist);
        });
    }

    /** One great blow at the foe in sight, multiplied below a share of its health (a boss is only ever hit one and a half times as hard). */
    static ActBody execute(double range, float dmg, double belowFrac, double mult, VfxShape shape) {
        return guard("execute", c -> {
            ServerPlayer p = c.player();
            LivingEntity t = Wave20Util.lookTarget(p, range);
            if (t == null) {
                Fx.say(p, "Nothing in sight to strike.");
                return;
            }
            boolean low = t.getHealth() <= t.getMaxHealth() * belowFrac;
            double m = low ? (BalanceLaw.isBoss(t) ? Math.min(mult, 1.5) : mult) : 1.0;
            fx(p.serverLevel(), shape, chest(p), body(t), glow(c), 12, 1.2f);
            Fx.damageHeavy(p, t, (float) (dmg * m), "minecraft:player_attack");
            spark(p.serverLevel(), body(t), glow(c), low ? 14 : 6);
            if (low && !t.isAlive()) Fx.say(p, "It is done in one stroke.");
        });
    }

    /** A bolt that leaps from foe to foe, a little weaker at every jump. */
    static ActBody chain(int jumps, double range, float dmg, String type, VfxShape shape) {
        return guard("chain", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            LivingEntity cur = Wave20Util.lookTarget(p, range);
            if (cur == null) {
                List<LivingEntity> near = Fx.foesAround(p, range);
                if (near.isEmpty()) {
                    Fx.say(p, "Nothing in reach.");
                    return;
                }
                cur = near.get(0);
            }
            Set<LivingEntity> done = new HashSet<>();
            Vec3 from = chest(p);
            float d = dmg;
            for (int i = 0; i < Mth.clamp(jumps, 1, 10) && cur != null; i++) {
                done.add(cur);
                fx(l, shape, from, body(cur), glow(c), 8, 1.0f);
                Fx.damage(p, cur, d, type);
                from = body(cur);
                d *= 0.85f;
                LivingEntity next = null;
                double best = 1.0e9;
                for (LivingEntity t : Wave20Util.foesNear(p, cur.position(), 8.0)) {
                    if (done.contains(t)) continue;
                    double dd = t.distanceToSqr(cur);
                    if (dd < best) {
                        best = dd;
                        next = t;
                    }
                }
                cur = next;
            }
        });
    }

    /** A guard: an absorbing shield, a status on the wielder and a shove that clears the foes round. */
    static ActBody ward(float absorb, int secs, String eff, int effSecs, int effAmp, double repelRadius, double repel) {
        return guard("ward", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            Fx.shield(p, absorb, secs * 20);
            if (eff != null && !eff.isEmpty()) Fx.addEffect(p, eff, effSecs * 20, effAmp);
            Fx.ring(l, p.position(), Math.min(Math.max(repelRadius, 2.0), 12.0), glow(c));
            if (repelRadius > 0 && repel > 0) {
                for (LivingEntity t : Fx.foesAround(p, repelRadius)) {
                    Vec3 d = t.position().subtract(p.position());
                    double len = Math.max(0.5, Math.hypot(d.x, d.z));
                    t.setDeltaMovement(t.getDeltaMovement().add(d.x / len * repel, 0.3, d.z / len * repel));
                    t.hurtMarked = true;
                }
            }
        });
    }

    /** A rallying cry: two statuses on every ally round, and a little life for the wielder. */
    static ActBody rally(double radius, String effA, String effB, int secs, int amp, float heal) {
        return guard("rally", c -> {
            ServerPlayer p = c.player();
            Fx.ring(p.serverLevel(), p.position(), Math.min(radius, 12.0), glow(c));
            if (effA != null && !effA.isEmpty()) Wave31Acts.allies(c, radius, effA, secs, amp);
            if (effB != null && !effB.isEmpty()) Wave31Acts.allies(c, radius, effB, secs, amp);
            if (heal > 0f) Fx.heal(p, heal);
        });
    }

    /** Life drawn out of every foe round: damage to each and a share of it back to the wielder. */
    static ActBody drain(double radius, float dmg, double healShare, String type, VfxShape shape) {
        return guard("drain", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            int n = 0;
            fx(l, shape, p.position(), p.position().add(0, 1, 0), glow(c), 18, (float) radius);
            for (LivingEntity t : Fx.foesAround(p, radius)) {
                Fx.damage(p, t, dmg, type);
                spark(l, body(t), glow(c), 4);
                n++;
            }
            if (n > 0) Fx.heal(p, (float) Math.min(60.0, dmg * healShare * n));
        });
    }

    /** Foes dragged in to the wielder, then struck. */
    static ActBody pullBind(double radius, double strength, float dmg, String type, String eff, int secs, int amp, VfxShape shape) {
        return guard("pull", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            fx(l, shape, p.position().add(0, 1, 0), p.position().add(0, 1.1, 0), glow(c), 18, (float) radius);
            Wave31Acts.pull(c, radius, strength);
            for (LivingEntity t : Fx.foesAround(p, radius)) hit(p, t, dmg, type, false, eff, secs, amp);
        });
    }

    /** Shots at the nearest foes in front: one blow each, up to {@code shots}. */
    static ActBody volley(int shots, double range, float dmg, String type, String eff, int secs, int amp, VfxShape shape) {
        return guard("volley", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            int n = 0;
            for (LivingEntity t : inCone(p, range, 70)) {
                if (n >= Mth.clamp(shots, 1, MAX_HITS)) break;
                fx(l, shape, chest(p), body(t), glow(c), 8, 1.0f);
                hit(p, t, dmg, type, false, eff, secs, amp);
                n++;
            }
            if (n == 0) Fx.say(p, "No foe in front.");
        });
    }

    /** Bolts out of a clear sky on the nearest foes round the wielder. */
    static ActBody storm(double radius, int strikes, float dmg, String type, String eff, int secs, int amp, VfxShape shape) {
        return guard("storm", c -> {
            ServerPlayer p = c.player();
            ServerLevel l = p.serverLevel();
            int n = 0;
            for (LivingEntity t : Fx.foesAround(p, radius)) {
                if (n++ >= Mth.clamp(strikes, 1, MAX_HITS)) break;
                fx(l, shape, t.position().add(0, 10, 0), t.position(), glow(c), 12, 2.0f);
                hit(p, t, dmg, type, false, eff, secs, amp);
            }
            if (n == 0) Fx.say(p, "No foe near.");
        });
    }

    /** Brands every foe round with the glowing mark and the weakness the wielder chooses (the mark shows them through walls). */
    static ActBody brand(double radius, int markSecs, float dmg, String type, String eff, int secs, int amp) {
        return guard("brand", c -> {
            Wave31Acts.mark(c, radius, markSecs);
            for (LivingEntity t : Fx.foesAround(c.player(), radius)) hit(c.player(), t, dmg, type, false, eff, secs, amp);
        });
    }

    /** A self buff: up to three effects on the wielder, a shield and some life. */
    static ActBody bolster(String effA, int ampA, String effB, int ampB, int secs, float absorb, float heal) {
        return guard("bolster", c -> {
            ServerPlayer p = c.player();
            if (effA != null && !effA.isEmpty()) Fx.addEffect(p, effA, secs * 20, ampA);
            if (effB != null && !effB.isEmpty()) Fx.addEffect(p, effB, secs * 20, ampB);
            if (absorb > 0f) Fx.shield(p, absorb, secs * 20);
            if (heal > 0f) Fx.heal(p, heal);
            Fx.ring(p.serverLevel(), p.position(), 2.5, glow(c));
        });
    }
}
