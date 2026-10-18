package com.newuniverse.nusmp.heroweapon.weapon.axe;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.entity.zogratis.ZogratisVfx;
import com.newuniverse.nusmp.heroweapon.Sin;
import com.newuniverse.nusmp.heroweapon.curse.HeroCurse;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.weapon.strike.Budget;
import com.newuniverse.nusmp.heroweapon.weapon.strike.SkillSpec;
import com.newuniverse.nusmp.heroweapon.weapon.strike.Strike;
import com.newuniverse.nusmp.heroweapon.weapon.strike.StrikeFx;
import com.newuniverse.nusmp.heroweapon.weapon.strike.StrikeSkill;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.djinn.weapon.WeaponGeo;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the nine techniques of the Vassal Axe. Eight of them carry the names of canon axe skills (Crescent Break, Power Break, Tornado Axe, Mountain Break, Accelerator Smash,
 * Guillotine, Shine Axe Burst, and the cursed penetration of the Curse Axe); the thrown axe is the owner's. They are the cleaving kit: a half-circle sweep, a debilitating chop, a
 * spin, a long cut through a crowd, a charge and a smash, an axe that comes back, an execution and a burst of light. Every blow that lands feeds Brawn ({@link AxeBrawn}), and
 * Brawn adds up to 16 percent to a technique's raw damage, but a hit never passes the cap of its kind ({@link ExecuteMath#raw} does the clamping for every hit here, so the owner's
 * budget holds whatever has been stacked). The heavy blows warn first (a ring, a line, a column, a pillar) and land a few ticks later; the thrown axe is resolved at the cast but
 * lands when it arrives and misses a foe that stepped out of its line. Nothing here changes a block. The numbers are {@link AxeNumbers}'s.
 */
final class AxeSkills {
    private AxeSkills() {}

    private static final int GREEN = AxeBrawn.GREEN;

    static List<HeroSkill> all() {
        return List.of(crescentBreak(), powerBreak(), tornadoAxe(), mountainBreak(), acceleratorSmash(), axeThrow(), guillotine(), shineAxeBurst(), cursedPenetration());
    }

    /** The raw damage of a hit of {@code spec}: {@code base} with the wielder's Brawn on it, clamped to the cap of the technique's kind. */
    private static float raw(HeroCtx c, SkillSpec spec, double base) {
        return ExecuteMath.raw((float) base, BrawnMath.skillMult(AxeBrawn.stacks(c)), false, false, (float) Budget.cap(spec.kind()));
    }

    // ================================================================ Crescent Break (canon)
    private static StrikeSkill crescentBreak() {
        return StrikeSkill.of(AxeNumbers.CRESCENT_BREAK, "Crescent Break",
                "Sweeps the axe in a half circle in front of you: a green arc on the ground shows what it will cover, then every foe inside is cut and thrown back. Hits up to twelve.",
                c -> {
                    ServerPlayer p = c.player;
                    StrikeFx.arc(c.level, p.position(), Strike.flatLook(p), AxeNumbers.CRESCENT_RANGE, AxeNumbers.CRESCENT_HALF_ANGLE, GREEN);
                    StrikeFx.sound(c.level, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 0.7f);
                    Strike.later(p, AxeNumbers.CRESCENT_DELAY, AxeSkills::crescent);
                    return true;
                });
    }

    private static void crescent(HeroCtx c) {
        float damage = raw(c, AxeNumbers.CRESCENT_BREAK, AxeNumbers.CRESCENT_BREAK.raw());
        int n = 0;
        for (LivingEntity t : Strike.foesCone(c.player, AxeNumbers.CRESCENT_RANGE, AxeNumbers.CRESCENT_HALF_ANGLE, AxeNumbers.CRESCENT_BREAK.maxTargets())) {
            if (!Strike.hit(c, t, damage, false)) continue;
            n++;
            Strike.shove(c, t, c.player.position(), 0.5);
        }
        Vec3 eye = c.player.getEyePosition().add(0, -0.5, 0), flat = Strike.flatLook(c.player);
        StrikeFx.shape(c.level, VfxShape.WIND_EMPEROR, eye, eye.add(flat.scale(AxeNumbers.CRESCENT_RANGE * 1.2)), GREEN, 12, 1.3f);
        StrikeFx.sound(c.level, c.player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.9f);
        if (n > 0) AxeBrawn.gain(c, 1);
    }

    // ================================================================ Power Break (canon)
    private static StrikeSkill powerBreak() {
        return StrikeSkill.of(AxeNumbers.POWER_BREAK, "Power Break",
                "A short overhead chop on the foe you face that breaks its strength: heavy damage, five seconds of weakness and three of slowness. A player behind a raised shield has the shield knocked down.",
                c -> {
                    LivingEntity t = Strike.pick(c.player, AxeNumbers.POWER_RANGE, 40);
                    if (t == null) {
                        c.say("No foe within reach.");
                        return false;
                    }
                    StrikeFx.ring(c.level, t.position(), 1.2, GREEN);
                    StrikeFx.gather(c.level, Strike.center(t), 0.7, GREEN, 8);
                    StrikeFx.sound(c.level, c.player.position(), SoundEvents.ANVIL_USE, 0.8f, 0.7f);
                    Strike.effect(c.player, "minecraft:slowness", AxeNumbers.POWER_DELAY, 1);
                    Strike.later(c.player, AxeNumbers.POWER_DELAY, cx -> breakPower(cx, t));
                    return true;
                });
    }

    private static void breakPower(HeroCtx c, LivingEntity t) {
        if (!t.isAlive() || t.level() != c.level || t.distanceTo(c.player) > AxeNumbers.POWER_RANGE + 1.5) {
            c.say("The foe slipped away.");
            return;
        }
        if (t instanceof Player pl && pl.isBlocking()) pl.disableShield();                  // the guard goes down first, so the chop itself is not swallowed by it
        if (!Strike.hit(c, t, raw(c, AxeNumbers.POWER_BREAK, AxeNumbers.POWER_BREAK.raw()), false)) return;
        Strike.effect(t, "minecraft:weakness", AxeNumbers.POWER_WEAK_TICKS, 1);
        Strike.effect(t, "minecraft:slowness", AxeNumbers.POWER_SLOW_TICKS, 1);
        StrikeFx.impact(c.level, t.position(), 1.6, GREEN);
        StrikeFx.sound(c.level, Strike.center(t), SoundEvents.SHIELD_BREAK, 1.0f, 0.8f);
        AxeBrawn.gain(c, 1);
    }

    // ================================================================ Tornado Axe (canon)
    private static StrikeSkill tornadoAxe() {
        return StrikeSkill.of(AxeNumbers.TORNADO_AXE, "Tornado Axe",
                "Spins with the axe held out for a second: a green tornado grows round you and four times in a row everything within four blocks is cut and shoved, the last time hardest. You are slowed while it turns.",
                c -> {
                    ServerPlayer p = c.player;
                    Strike.effect(p, "minecraft:slowness", AxeNumbers.TORNADO_TICKS + 4, 1);
                    StrikeFx.follow(c.level, VfxShape.WIND_TORNADO, p, p.position(), GREEN, AxeNumbers.TORNADO_TICKS + 4, 3.2f);
                    StrikeFx.sound(c.level, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.6f);
                    Strike.later(p, 4, cx -> Strike.zone(cx.player, AxeNumbers.TORNADO_TICKS, AxeNumbers.TORNADO_GAP, AxeSkills::spin));
                    return true;
                });
    }

    private static void spin(HeroCtx c, int age) {
        boolean last = age + AxeNumbers.TORNADO_GAP >= AxeNumbers.TORNADO_TICKS;
        float damage = raw(c, AxeNumbers.TORNADO_AXE, AxeNumbers.TORNADO_AXE.raw());
        int n = 0;
        for (LivingEntity t : Strike.foesAround(c.player, AxeNumbers.TORNADO_RADIUS, AxeNumbers.TORNADO_AXE.maxTargets())) {
            if (!Strike.hit(c, t, damage, true)) continue;
            n++;
            Strike.shove(c, t, c.player.position(), last ? 0.9 : 0.3);
        }
        StrikeFx.sound(c.level, c.player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, last ? 0.7f : 1.1f);
        if (n > 0 && age == 0) AxeBrawn.gain(c, 1);
    }

    // ================================================================ Mountain Break (canon)
    private static StrikeSkill mountainBreak() {
        return StrikeSkill.of(AxeNumbers.MOUNTAIN_BREAK, "Mountain Break",
                "Brings the axe down and cuts a line twelve blocks long straight ahead. A dotted line shows the path first. Heavy damage to everything along it, on the ground or in the air.",
                c -> {
                    ServerPlayer p = c.player;
                    Vec3 flat = Strike.flatLook(p);
                    Vec3 base = p.position().add(0, 0.9, 0);
                    Vec3 far = Strike.stopAtWall(p, base, base.add(flat.scale(AxeNumbers.MOUNTAIN_LENGTH)));
                    Vec3 from = p.position().add(flat.scale(1.0));
                    Vec3 to = new Vec3(far.x, p.getY(), far.z);
                    if (from.distanceTo(to) < 3.0) {
                        c.say("There is no room for the cut.");
                        return false;
                    }
                    StrikeFx.tellCrack(c.level, from, to, GREEN, AxeNumbers.MOUNTAIN_DELAY + 4);
                    StrikeFx.line(c.level, from, to, GREEN);
                    StrikeFx.sound(c.level, p.position(), SoundEvents.ANVIL_USE, 0.9f, 0.6f);
                    Strike.effect(p, "minecraft:slowness", AxeNumbers.MOUNTAIN_DELAY, 2);
                    Strike.later(p, AxeNumbers.MOUNTAIN_DELAY, cx -> mountain(cx, from, to));
                    return true;
                });
    }

    private static void mountain(HeroCtx c, Vec3 from, Vec3 to) {
        float damage = raw(c, AxeNumbers.MOUNTAIN_BREAK, AxeNumbers.MOUNTAIN_BREAK.raw());
        int n = 0;
        for (LivingEntity t : Strike.foesLine(c.player, from.add(0, 0.9, 0), to.add(0, 0.9, 0), AxeNumbers.MOUNTAIN_HALF_WIDTH, AxeNumbers.MOUNTAIN_BREAK.maxTargets())) {
            if (!Strike.hit(c, t, damage, false)) continue;
            n++;
            Strike.shove(c, t, from, 0.5);
            Strike.lift(t, 0.15);
        }
        StrikeFx.strikeCrack(c.level, from, to, GREEN, 36);
        StrikeFx.sound(c.level, from, SoundEvents.MACE_SMASH_GROUND, 1.0f, 1.0f);
        if (n > 0) AxeBrawn.gain(c, 1);
    }

    // ================================================================ Accelerator Smash (canon, web novel)
    private static StrikeSkill acceleratorSmash() {
        return StrikeSkill.of(AxeNumbers.ACCELERATOR_SMASH, "Accelerator Smash",
                "Charges six blocks forward with the shoulder, hitting what is in the way, then smashes the ground where you stop. The smash hits everything within three blocks. Closes distance fast.",
                c -> {
                    ServerPlayer p = c.player;
                    Vec3 flat = Strike.flatLook(p);
                    Vec3 a = p.position().add(0, 0.9, 0);
                    Vec3 far = Strike.stopAtWall(p, a, a.add(flat.scale(AxeNumbers.SMASH_RUN)));
                    Vec3 b = new Vec3(far.x, a.y, far.z);
                    float charge = raw(c, AxeNumbers.ACCELERATOR_SMASH, AxeNumbers.SMASH_CHARGE_RAW);
                    for (LivingEntity t : Strike.foesLine(p, a, b, AxeNumbers.SMASH_HALF_WIDTH, AxeNumbers.ACCELERATOR_SMASH.maxTargets())) {
                        Strike.later(p, 2, cx -> {
                            if (t.isAlive() && Strike.hit(cx, t, charge, true)) Strike.shove(cx, t, cx.player.position(), 0.6);
                        });
                    }
                    Fx.dash(p, Math.max(2.0, a.distanceTo(b)));
                    StrikeFx.tellRing(c.level, new Vec3(b.x, p.getY(), b.z), AxeNumbers.SMASH_RADIUS, GREEN, AxeNumbers.SMASH_DELAY + 4);     // where the smash will land
                    StrikeFx.shape(c.level, VfxShape.WIND_GALE, a, b, GREEN, 10, 1.0f);
                    StrikeFx.sound(c.level, p.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.7f);
                    Strike.later(p, AxeNumbers.SMASH_DELAY, AxeSkills::smashDown);
                    return true;
                });
    }

    private static void smashDown(HeroCtx c) {
        Vec3 at = c.player.position();
        float damage = raw(c, AxeNumbers.ACCELERATOR_SMASH, AxeNumbers.ACCELERATOR_SMASH.raw());
        int n = 0;
        for (LivingEntity t : Strike.foesNear(c.player, at, AxeNumbers.SMASH_RADIUS, AxeNumbers.ACCELERATOR_SMASH.maxTargets())) {
            if (!Strike.hit(c, t, damage, false)) continue;
            n++;
            Strike.shove(c, t, at, 0.7);
            Strike.lift(t, 0.25);
        }
        StrikeFx.strikeRing(c.level, at, AxeNumbers.SMASH_RADIUS, GREEN);
        StrikeFx.impact(c.level, at, AxeNumbers.SMASH_RADIUS * 0.8, GREEN);
        StrikeFx.sound(c.level, at, SoundEvents.MACE_SMASH_GROUND, 1.0f, 1.0f);
        if (n > 0) AxeBrawn.gain(c, 1);
    }

    // ================================================================ Axe Throw
    private static StrikeSkill axeThrow() {
        return StrikeSkill.of(AxeNumbers.AXE_THROW, "Axe Throw",
                "Hurls the axe fourteen blocks along your look; it cuts up to four foes on the way out, turns at the end and cuts again on the way back to your hand. A foe that steps out of the line in time is missed.",
                c -> {
                    ServerPlayer p = c.player;
                    Vec3 look = p.getLookAngle();
                    Vec3 a = p.getEyePosition().add(look.scale(0.5)).add(0, -0.3, 0);
                    Vec3 b = Strike.stopAtWall(p, a, a.add(look.scale(AxeNumbers.THROW_RANGE)));
                    int out = Math.max(2, (int) Math.round(a.distanceTo(b) / AxeNumbers.THROW_SPEED));
                    float outRaw = raw(c, AxeNumbers.AXE_THROW, AxeNumbers.THROW_OUT_RAW), backRaw = raw(c, AxeNumbers.AXE_THROW, AxeNumbers.THROW_BACK_RAW);
                    boolean[] gained = {false};
                    fly(p, a, b, outRaw, gained);
                    StrikeFx.shape(c.level, VfxShape.SLASH_WAVE, a, b, GREEN, out, 1.1f);
                    StrikeFx.sound(c.level, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.6f);
                    Strike.later(p, out + 1, cx -> {
                        Vec3 home = cx.player.getEyePosition().add(0, -0.4, 0);
                        fly(cx.player, b, home, backRaw, gained);
                        StrikeFx.shape(cx.level, VfxShape.SLASH_WAVE, b, home, GREEN, Math.max(2, (int) Math.round(b.distanceTo(home) / AxeNumbers.THROW_SPEED)), 1.1f);
                        StrikeFx.sound(cx.level, cx.player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.2f);
                    });
                    return true;
                });
    }

    /** One leg of the thrown axe: every foe in the line is scheduled to be cut when the axe reaches it. */
    private static void fly(ServerPlayer p, Vec3 from, Vec3 to, float raw, boolean[] gained) {
        for (LivingEntity t : Strike.foesLine(p, from, to, AxeNumbers.THROW_HALF_WIDTH, AxeNumbers.AXE_THROW.maxTargets())) {
            int delay = Math.max(1, (int) Math.round(from.distanceTo(Strike.center(t)) / AxeNumbers.THROW_SPEED));
            Strike.later(p, delay, cx -> lands(cx, t, from, to, raw, gained));
        }
    }

    private static void lands(HeroCtx c, LivingEntity t, Vec3 a, Vec3 b, float raw, boolean[] gained) {
        if (!t.isAlive()) return;
        Vec3 m = Strike.center(t);
        double miss = WeaponGeo.segmentDistance(a.x, a.y, a.z, b.x, b.y, b.z, m.x, m.y, m.z);
        if (miss > AxeNumbers.THROW_HALF_WIDTH + 0.7 + t.getBbWidth() * 0.5) return;                       // it stepped out of the way
        if (!Strike.hit(c, t, raw, false)) return;
        Strike.shove(c, t, a, 0.4);
        StrikeFx.burst(c.level, m, GREEN, 4);
        if (!gained[0]) {
            gained[0] = true;
            AxeBrawn.gain(c, 1);
        }
    }

    // ================================================================ Guillotine (canon, curse list)
    private static StrikeSkill guillotine() {
        return StrikeSkill.of(AxeNumbers.GUILLOTINE, "Guillotine",
                "A falling cut on the foe you face, with a red column over it as the warning. A foe at a quarter of its health or less (a player at a fifth) takes more than double; a boss is never executed. Kills feed Brawn.",
                c -> {
                    LivingEntity t = Strike.pick(c.player, AxeNumbers.GUILLOTINE_RANGE, 40);
                    if (t == null) {
                        c.say("No foe within reach.");
                        return false;
                    }
                    StrikeFx.column(c.level, t.position(), 3.0, 0xD01830);
                    StrikeFx.ring(c.level, t.position(), 1.0, 0xD01830);
                    StrikeFx.sound(c.level, c.player.position(), SoundEvents.ANVIL_USE, 0.8f, 0.5f);
                    Strike.later(c.player, AxeNumbers.GUILLOTINE_DELAY, cx -> fall(cx, t));
                    return true;
                });
    }

    private static void fall(HeroCtx c, LivingEntity t) {
        if (!t.isAlive() || t.level() != c.level || t.distanceTo(c.player) > AxeNumbers.GUILLOTINE_RANGE + 1.5) {
            c.say("The foe slipped away.");
            return;
        }
        boolean player = t instanceof Player;
        boolean executing = ExecuteMath.eligible(t.getHealth(), t.getMaxHealth(), BalanceLaw.isBoss(t), player);
        float damage = ExecuteMath.raw(AxeNumbers.GUILLOTINE_BASE, BrawnMath.skillMult(AxeBrawn.stacks(c)), executing, player, (float) Budget.cap(AxeNumbers.GUILLOTINE.kind()));
        if (!Strike.hit(c, t, damage, false)) return;
        Vec3 top = t.position().add(0, t.getBbHeight() + 1.5, 0);
        StrikeFx.shape(c.level, VfxShape.DARK_IAI, top, t.position().add(0, 0.2, 0), 0xD01830, 12, 1.4f);
        StrikeFx.sound(c.level, Strike.center(t), executing ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, executing ? 0.7f : 1.0f);
        if (executing) c.say("Executed.");
        AxeBrawn.gain(c, 1);
    }

    // ================================================================ Shine Axe Burst (canon, ultimate)
    private static StrikeSkill shineAxeBurst() {
        return StrikeSkill.of(AxeNumbers.SHINE_AXE_BURST, "Shine Axe Burst",
                "Needs five Brawn and spends all of it. The axe blazes: a pillar of green light rises over you, then everything within eight blocks is cut. Fourteen raw plus 2.2 for each Brawn spent, 31.6 at eight. You are held still while it gathers.",
                c -> {
                    int stacks = AxeBrawn.stacks(c);
                    if (!BrawnMath.canShine(stacks)) {
                        c.say("Shine Axe Burst needs " + BrawnMath.SHINE_MIN + " Brawn; you have " + stacks + ".");
                        return false;
                    }
                    ServerPlayer p = c.player;
                    float damage = BrawnMath.shineRaw(stacks);
                    AxeBrawn.clear(c);
                    Vec3 centre = p.position();
                    ZogratisVfx.pillar(c.level, centre, 3.0, AxeNumbers.SHINE_DELAY + 4, GREEN, true);
                    StrikeFx.ring(c.level, centre, AxeNumbers.SHINE_RADIUS, GREEN);
                    StrikeFx.gather(c.level, centre.add(0, 1.2, 0), 1.5, GREEN, 24);
                    StrikeFx.sound(c.level, centre, SoundEvents.BELL_BLOCK, 1.2f, 0.6f);
                    Strike.effect(p, "minecraft:slowness", AxeNumbers.SHINE_DELAY, 3);
                    Strike.later(p, AxeNumbers.SHINE_DELAY, cx -> shine(cx, centre, damage));
                    return true;
                });
    }

    private static void shine(HeroCtx c, Vec3 centre, float damage) {
        int n = 0;
        for (LivingEntity t : Strike.foesNear(c.player, centre, AxeNumbers.SHINE_RADIUS, AxeNumbers.SHINE_AXE_BURST.maxTargets())) {
            if (!Strike.hit(c, t, damage, false)) continue;
            n++;
            Strike.shove(c, t, centre, 1.0);
            Strike.lift(t, 0.35);
        }
        ZogratisVfx.pillar(c.level, centre, 5.0, 20, GREEN, false);
        StrikeFx.shape(c.level, VfxShape.LIGHT_FLARE, centre.add(0, 1.5, 0), centre.add(0, 1.5, 0), GREEN, 20, 3.0f);
        StrikeFx.strikeRing(c.level, centre, AxeNumbers.SHINE_RADIUS, GREEN);
        StrikeFx.sound(c.level, centre, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.9f, 1.4f);
        StrikeFx.sound(c.level, centre, SoundEvents.ANVIL_LAND, 1.0f, 0.6f);
        c.say(n == 0 ? "Shine Axe Burst: nothing in reach." : "Shine Axe Burst: " + n + " foes cut.");
    }

    // ================================================================ Cursed Penetration (the curse form)
    private static StrikeSkill cursedPenetration() {
        return StrikeSkill.of(AxeNumbers.CURSED_PENETRATION, "Cursed Penetration",
                "The Curse Axe drives through the foe you face: a raised shield is knocked down and a heavy cut lands, and a moment later the wound opens, a second cut. It costs you corruption.",
                c -> {
                    LivingEntity t = Strike.pick(c.player, AxeNumbers.PENETRATION_RANGE, 40);
                    if (t == null) {
                        c.say("No foe within reach.");
                        return false;
                    }
                    int gold = Sin.GREED.rgb();
                    StrikeFx.ring(c.level, t.position(), 1.2, gold);
                    StrikeFx.gather(c.level, Strike.center(t), 0.8, gold, 10);
                    StrikeFx.sound(c.level, c.player.position(), SoundEvents.ANVIL_USE, 0.8f, 0.4f);
                    HeroCurse.addCorruption(c.player, AxeNumbers.PENETRATION_CORRUPTION);
                    Strike.later(c.player, AxeNumbers.PENETRATION_DELAY, cx -> penetrate(cx, t));
                    return true;
                });
    }

    private static void penetrate(HeroCtx c, LivingEntity t) {
        if (!t.isAlive() || t.level() != c.level || t.distanceTo(c.player) > AxeNumbers.PENETRATION_RANGE + 1.5) {
            c.say("The foe slipped away.");
            return;
        }
        if (t instanceof Player pl && pl.isBlocking()) pl.disableShield();                  // it goes through the guard: the shield is knocked down, then the cut lands
        if (!Strike.hit(c, t, raw(c, AxeNumbers.CURSED_PENETRATION, AxeNumbers.CURSED_PENETRATION.raw()), false)) return;
        StrikeFx.impact(c.level, t.position(), 1.6, Sin.GREED.rgb());
        StrikeFx.sound(c.level, Strike.center(t), SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 0.6f);
        Strike.later(c.player, AxeNumbers.WOUND_DELAY - AxeNumbers.PENETRATION_DELAY, cx -> {
            if (!t.isAlive() || t.level() != cx.level) return;
            if (Strike.hitStill(cx, t, raw(cx, AxeNumbers.CURSED_PENETRATION, AxeNumbers.WOUND_RAW))) {
                StrikeFx.burst(cx.level, Strike.center(t), Sin.GREED.rgb(), 8);
                StrikeFx.sound(cx.level, Strike.center(t), SoundEvents.PLAYER_ATTACK_CRIT, 0.7f, 0.5f);
            }
        });
        AxeBrawn.gain(c, 1);
    }
}
