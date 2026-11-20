package com.newuniverse.nusmp.lords;

import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: what each Lord weapon DOES, on the server only: the two new forms' arts (the right click of form 1 and form 2; form 0's right click is the old technique of {@code sinweapons}) and the four Lord's decrees.
 * Every art is bounded by construction: ONE box query ({@code relics.Fx.foesAround}, at most 24, hostile and PvP-legal only), a cut to {@link LordRules#MAX_ART_TARGETS} (the decrees {@link LordRules#MAX_TARGETS}),
 * one effect packet plus at most {@link LordFx#MAX_HIT_FLASHES} hit flashes, damage only through the balance law. Nothing is scheduled and nothing is kept: the cooldown is the caller's. Each method returns what it
 * did so the caller can charge the burden; {@link Result#fired} false means nobody was there and nothing is charged (a slam at nothing is free).
 */
final class LordArts {
    private LordArts() {}

    /** What an art did: whether it ran, how many foes it hurt and how many of them were undead (Seifort's Penance is eased by smiting the undead). */
    record Result(boolean fired, int hit, int undead) {
        static final Result NONE = new Result(false, 0, 0);
    }

    // ---------------------------------------------------------------- the geometry
    private record Aim(Vec3 eye, Vec3 look, Vec3 right, Vec3 up) {
        static Aim of(ServerPlayer p) {
            Vec3 look = p.getLookAngle(), r = look.cross(new Vec3(0, 1, 0));
            r = r.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : r.normalize();
            return new Aim(p.getEyePosition(), look, r, r.cross(look).normalize());
        }

        double along(LivingEntity t) { return t.getBoundingBox().getCenter().subtract(eye).dot(look); }

        /** Distance of the creature's centre from the look line. */
        double off(LivingEntity t) {
            Vec3 c = t.getBoundingBox().getCenter().subtract(eye);
            return c.subtract(look.scale(c.dot(look))).length();
        }
    }

    private static List<LivingEntity> line(ServerPlayer p, Aim a, double range, double radius, int cap) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : Fx.foesAround(p, range + 1.0)) {
            if (out.size() >= cap) break;
            double al = a.along(t), hw = t.getBbWidth() * 0.5;
            if (al >= 0.0 && al <= range + 0.5 && a.off(t) <= radius + hw) out.add(t);
        }
        return out;
    }

    private static List<LivingEntity> around(ServerPlayer p, double radius, int cap) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : Fx.foesAround(p, radius)) {
            if (out.size() >= cap) break;
            out.add(t);
        }
        return out;
    }

    private static List<LivingEntity> cone(ServerPlayer p, Aim a, double range, double cosHalf, int cap) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : Fx.foesAround(p, range)) {
            if (out.size() >= cap) break;
            Vec3 c = t.getBoundingBox().getCenter().subtract(a.eye);
            double len = c.length();
            if (len < 1.0e-3 || c.dot(a.look) / len >= cosHalf) out.add(t);
        }
        return out;
    }

    /** The damage of one hit: the listed number, the server setting, and the mastery's 2% a level; undead take 1.5 times from the holy weapon. */
    private static float dmg(float listed, int level, boolean holyVsUndead) {
        double d = listed * LordConfig.damage() * (1.0 + 0.02 * Math.max(0, Math.min(LordRules.MAX_LEVEL, level))) * (holyVsUndead ? 1.5 : 1.0);
        return (float) Math.max(0.0, Math.min(d, 400.0));
    }

    private static boolean undead(LivingEntity t) { return t.getType().is(EntityTypeTags.UNDEAD); }

    private static void hurt(ServerPlayer p, LivingEntity t, float amount) { Fx.damage(p, t, amount, "minecraft:player_attack"); }

    private static void shove(LivingEntity t, double x, double y, double z) {
        t.push(x, y, z);
        t.hurtMarked = true;                                                          // the client is told its new motion, as for a knock-back
    }

    /** Takes up to {@code room} beneficial effects from {@code t} and lends them to the wielder (each at most 10 s, amplifier at most 4). Returns how many. */
    static int steal(ServerPlayer p, LivingEntity t, int room) {
        int n = 0;
        for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) {
            if (n >= room) break;
            if (!e.getEffect().value().isBeneficial()) continue;
            t.removeEffect(e.getEffect());
            p.addEffect(new MobEffectInstance(e.getEffect(), Math.max(1, Math.min(e.getDuration(), 200)), Math.min(e.getAmplifier(), 4)));
            n++;
        }
        return n;
    }

    private static void cleanse(ServerPlayer p, int max) {
        int n = 0;
        for (MobEffectInstance e : new ArrayList<>(p.getActiveEffects())) {
            if (n >= max) break;
            if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                p.removeEffect(e.getEffect());
                n++;
            }
        }
    }

    private static void flashes(ServerLevel level, LordRules.Sin sin, List<LivingEntity> hit) {
        int n = 0;
        for (LivingEntity t : hit) {
            if (n++ >= LordFx.MAX_HIT_FLASHES) break;
            Vec3 at = LordFx.chest(t);
            LordFx.send(level, sin, LordVfxCodes.HIT, at, at, 0.0f);
        }
    }

    // ---------------------------------------------------------------- the arts of form 1 and form 2
    static Result art(ServerPlayer p, LordRules.Sin sin, int form, int level) {
        return switch (sin) {
            case SLOTH -> form == 1 ? titansVerdict(p, level) : drowsingChains(p, level);
            case ENVY -> form == 1 ? jealousVolley(p, level) : covetingCoil(p, level);
            case VANITY -> form == 1 ? reflectedGaze(p, level) : fanOfVanity(p, level);
            case HOLY -> form == 1 ? pillarOfJudgement(p, level) : sendForth(p, level);
            default -> Result.NONE;
        };
    }

    /** Sloth form 1: a heavy overhead blow down a 7 block, 2 block wide lane: damage, a crushing slow, the foes are driven into the ground. */
    private static Result titansVerdict(ServerPlayer p, int level) {
        Aim a = Aim.of(p);
        List<LivingEntity> hit = line(p, a, 7.0, 2.0, LordRules.MAX_ART_TARGETS);
        ServerLevel lv = p.serverLevel();
        LordFx.send(lv, LordRules.Sin.SLOTH, LordVfxCodes.ART1, LordFx.hand(p), a.eye.add(a.look.scale(7.0)), 0.0f);
        if (hit.isEmpty()) return new Result(true, 0, 0);
        for (LivingEntity t : hit) {
            hurt(p, t, dmg(18.0f, level, false));
            Fx.addEffect(t, "minecraft:slowness", 60, 2);
            shove(t, 0.0, -0.6, 0.0);
        }
        flashes(lv, LordRules.Sin.SLOTH, hit);
        return new Result(true, hit.size(), 0);
    }

    /** Sloth form 2: chains drag every foe within 9 blocks (at most 6) towards the wielder and bind them: slowness and weakness. */
    private static Result drowsingChains(ServerPlayer p, int level) {
        List<LivingEntity> hit = around(p, 9.0, 6);
        ServerLevel lv = p.serverLevel();
        LordFx.send(lv, LordRules.Sin.SLOTH, LordVfxCodes.ART2, p.position().add(0, 0.05, 0), p.position().add(0, 0.05, 0), 9.0f);
        if (hit.isEmpty()) return new Result(false, 0, 0);
        for (LivingEntity t : hit) {
            hurt(p, t, dmg(6.0f, level, false));
            Fx.addEffect(t, "minecraft:slowness", 40, 3);
            Fx.addEffect(t, "minecraft:weakness", 80, 0);
            Vec3 to = p.position().subtract(t.position());
            Vec3 pull = to.lengthSqr() < 1.0e-6 ? Vec3.ZERO : to.normalize().scale(0.9);
            shove(t, pull.x, 0.1, pull.z);
        }
        flashes(lv, LordRules.Sin.SLOTH, hit);
        return new Result(true, hit.size(), 0);
    }

    /** Envy form 1: up to three green shots in a fan (the three nearest foes within 22 blocks and 25 degrees of the look line), each takes one beneficial effect. */
    private static Result jealousVolley(ServerPlayer p, int level) {
        Aim a = Aim.of(p);
        List<LivingEntity> hit = cone(p, a, 22.0, 0.906, 3);
        ServerLevel lv = p.serverLevel();
        LordFx.send(lv, LordRules.Sin.ENVY, LordVfxCodes.ART1, LordFx.hand(p), a.eye.add(a.look.scale(20.0)), 0.0f);
        if (hit.isEmpty()) return new Result(true, 0, 0);
        for (LivingEntity t : hit) {
            hurt(p, t, dmg(9.0f, level, false));
            steal(p, t, 1);
        }
        flashes(lv, LordRules.Sin.ENVY, hit);
        return new Result(true, hit.size(), 0);
    }

    /** Envy form 2: a coil lashes the first foe on a 12 block line: damage, up to two stolen effects, and it is pulled in. */
    private static Result covetingCoil(ServerPlayer p, int level) {
        Aim a = Aim.of(p);
        List<LivingEntity> hit = line(p, a, 12.0, 1.2, 1);
        ServerLevel lv = p.serverLevel();
        Vec3 end = hit.isEmpty() ? a.eye.add(a.look.scale(12.0)) : LordFx.chest(hit.get(0));
        LordFx.send(lv, LordRules.Sin.ENVY, LordVfxCodes.ART2, LordFx.hand(p), end, 0.0f);
        if (hit.isEmpty()) return new Result(true, 0, 0);
        LivingEntity t = hit.get(0);
        hurt(p, t, dmg(8.0f, level, false));
        steal(p, t, 2);
        Vec3 to = p.position().subtract(t.position());
        Vec3 pull = to.lengthSqr() < 1.0e-6 ? Vec3.ZERO : to.normalize().scale(1.1);
        shove(t, pull.x, 0.2, pull.z);
        return new Result(true, 1, 0);
    }

    /** Vanity form 1: the mirror stares back: foes in an 8 block cone are weakened and made to glow, and the wielder is shielded. */
    private static Result reflectedGaze(ServerPlayer p, int level) {
        Aim a = Aim.of(p);
        List<LivingEntity> hit = cone(p, a, 8.0, 0.766, LordRules.MAX_ART_TARGETS);
        ServerLevel lv = p.serverLevel();
        LordFx.send(lv, LordRules.Sin.VANITY, LordVfxCodes.ART1, LordFx.hand(p), a.eye.add(a.look.scale(8.0)), 0.0f);
        Fx.shield(p, 6.0f, 100);
        if (hit.isEmpty()) return new Result(true, 0, 0);
        for (LivingEntity t : hit) {
            hurt(p, t, dmg(8.0f, level, false));
            Fx.addEffect(t, "minecraft:weakness", 80, 1);
            Fx.addEffect(t, "minecraft:glowing", 100, 0);
        }
        flashes(lv, LordRules.Sin.VANITY, hit);
        return new Result(true, hit.size(), 0);
    }

    /** Vanity form 2: three lines of cutting light (straight, and 25 degrees to each side), 10 blocks long. */
    private static Result fanOfVanity(ServerPlayer p, int level) {
        Aim a = Aim.of(p);
        List<LivingEntity> hit = new ArrayList<>();
        for (LivingEntity t : Fx.foesAround(p, 11.0)) {
            if (hit.size() >= LordRules.MAX_ART_TARGETS) break;
            Vec3 c = t.getBoundingBox().getCenter().subtract(a.eye);
            double len = c.length();
            if (len < 1.0e-3 || len > 10.5) continue;
            double ang = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, c.dot(a.look) / len))));
            double side = c.dot(a.right);
            double lateralAngle = Math.toDegrees(Math.atan2(side, c.dot(a.look)));
            boolean onLine = Math.abs(lateralAngle) <= 6.0 || Math.abs(Math.abs(lateralAngle) - 25.0) <= 6.0;
            if (ang <= 40.0 && onLine) hit.add(t);
        }
        ServerLevel lv = p.serverLevel();
        LordFx.send(lv, LordRules.Sin.VANITY, LordVfxCodes.ART2, LordFx.hand(p), a.eye.add(a.look.scale(10.0)), 0.0f);
        if (hit.isEmpty()) return new Result(true, 0, 0);
        for (LivingEntity t : hit) hurt(p, t, dmg(10.0f, level, false));
        flashes(lv, LordRules.Sin.VANITY, hit);
        return new Result(true, hit.size(), 0);
    }

    /** Holy form 1: a pillar of light falls where the wielder looks (the first foe within 14 blocks, else 10 blocks ahead): foes within 3 blocks of it are judged; the wielder is mended a little. */
    private static Result pillarOfJudgement(ServerPlayer p, int level) {
        Aim a = Aim.of(p);
        List<LivingEntity> first = line(p, a, 14.0, 1.5, 1);
        Vec3 c = first.isEmpty() ? a.eye.add(a.look.scale(10.0)) : first.get(0).position();
        ServerLevel lv = p.serverLevel();
        LordFx.send(lv, LordRules.Sin.HOLY, LordVfxCodes.ART1, c, c, 3.0f);
        List<LivingEntity> hit = new ArrayList<>();
        for (LivingEntity t : Fx.foesAround(p, 17.0)) {
            if (hit.size() >= LordRules.MAX_ART_TARGETS) break;
            if (t.position().distanceToSqr(c) <= 9.0) hit.add(t);
        }
        if (hit.isEmpty()) return new Result(true, 0, 0);
        int dead = 0;
        for (LivingEntity t : hit) {
            boolean u = undead(t);
            if (u) { dead++; t.setRemainingFireTicks(80); }
            hurt(p, t, dmg(14.0f, level, u));
        }
        Fx.heal(p, 2.0f);
        flashes(lv, LordRules.Sin.HOLY, hit);
        return new Result(true, hit.size(), dead);
    }

    /** Holy form 2: the sword is sent forth along a 20 block line, passing through up to six foes. */
    private static Result sendForth(ServerPlayer p, int level) {
        Aim a = Aim.of(p);
        List<LivingEntity> hit = line(p, a, 20.0, 1.4, 6);
        ServerLevel lv = p.serverLevel();
        LordFx.send(lv, LordRules.Sin.HOLY, LordVfxCodes.ART2, LordFx.hand(p), a.eye.add(a.look.scale(20.0)), 0.0f);
        if (hit.isEmpty()) return new Result(true, 0, 0);
        int dead = 0;
        for (LivingEntity t : hit) {
            boolean u = undead(t);
            if (u) dead++;
            hurt(p, t, dmg(15.0f, level, u));
        }
        flashes(lv, LordRules.Sin.HOLY, hit);
        return new Result(true, hit.size(), dead);
    }

    // ---------------------------------------------------------------- the Lord's decrees (mastery level 8)
    static Result decree(ServerPlayer p, LordRules.Sin sin, int level) {
        ServerLevel lv = p.serverLevel();
        Vec3 feet = p.position().add(0, 0.05, 0);
        switch (sin) {
            case SLOTH -> {                                                                     // Abyssal Stillness: a field of crushing weight, radius 12
                List<LivingEntity> hit = around(p, 12.0, LordRules.MAX_TARGETS);
                LordFx.send(lv, sin, LordVfxCodes.DECREE, feet, feet, 12.0f);
                for (LivingEntity t : hit) {
                    hurt(p, t, dmg(16.0f, level, false));
                    Fx.addEffect(t, "minecraft:slowness", 120, 4);
                    Fx.addEffect(t, "minecraft:mining_fatigue", 120, 2);
                    shove(t, 0.0, -0.8, 0.0);
                }
                flashes(lv, sin, hit);
                return new Result(true, hit.size(), 0);
            }
            case ENVY -> {                                                                      // Covet All: every foe within 16 blocks is robbed of up to three effects (six in all)
                List<LivingEntity> hit = around(p, 16.0, 6);
                LordFx.send(lv, sin, LordVfxCodes.DECREE, feet, feet, 16.0f);
                int room = 6;
                for (LivingEntity t : hit) {
                    hurt(p, t, dmg(12.0f, level, false));
                    room -= steal(p, t, Math.min(3, Math.max(0, room)));
                }
                flashes(lv, sin, hit);
                return new Result(true, hit.size(), 0);
            }
            case VANITY -> {                                                                    // Hall of Mirrors: a ring of mirrors, radius 10
                List<LivingEntity> hit = around(p, 10.0, LordRules.MAX_ART_TARGETS);
                LordFx.send(lv, sin, LordVfxCodes.DECREE, feet, feet, 10.0f);
                Fx.shield(p, 10.0f, 200);
                for (LivingEntity t : hit) {
                    hurt(p, t, dmg(12.0f, level, false));
                    Fx.addEffect(t, "minecraft:slowness", 100, 1);
                    Fx.addEffect(t, "minecraft:weakness", 100, 1);
                }
                flashes(lv, sin, hit);
                return new Result(true, hit.size(), 0);
            }
            case HOLY -> {                                                                      // Holy Judgement: a cross of light over a 14 block circle
                List<LivingEntity> hit = around(p, 14.0, LordRules.MAX_TARGETS);
                LordFx.send(lv, sin, LordVfxCodes.DECREE, feet, feet, 14.0f);
                int dead = 0;
                for (LivingEntity t : hit) {
                    boolean u = undead(t);
                    if (u) { dead++; t.setRemainingFireTicks(100); }
                    hurt(p, t, dmg(20.0f, level, u));
                }
                cleanse(p, 3);
                Fx.heal(p, 8.0f);
                flashes(lv, sin, hit);
                return new Result(true, hit.size(), dead);
            }
            default -> {
                return Result.NONE;
            }
        }
    }
}
