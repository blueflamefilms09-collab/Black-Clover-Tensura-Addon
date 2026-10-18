package com.newuniverse.nusmp.heroweapon.weapon.hammer;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.weapon.strike.Strike;
import com.newuniverse.nusmp.heroweapon.weapon.strike.StrikeFx;
import com.newuniverse.nusmp.heroweapon.weapon.strike.StrikeSkill;
import com.newuniverse.nusmp.song.SongTargets;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the seven techniques of the Vassal Hammer, written so a player can learn them: every heavy blow draws a ring or a line on the ground at the moment of the cast and lands a
 * few ticks later ({@link Strike#later}), so the foe, and the wielder's friends, see exactly where and when. The wielder is slowed while a blow winds up (the weapon is heavy),
 * the ground blows (Overhead Smash, Shockwave, Quake Stomp) miss what is in the air, and every damaging technique feeds the foe's poise gauge ({@link HammerPoise}). All damage
 * goes through {@link Strike#hit} (the balance law), the support technique heals nothing and changes no block. The numbers are {@link HammerNumbers}'s.
 */
final class HammerSkills {
    private HammerSkills() {}

    private static final int AMBER = HammerPoise.AMBER;

    static List<HeroSkill> all() {
        return List.of(overheadSmash(), shockwave(), armorCrack(), quakeStomp(), skywardSmash(), forgeAegis(), titanFall());
    }

    private static float raw(com.newuniverse.nusmp.heroweapon.weapon.strike.SkillSpec s) { return (float) s.raw(); }

    /** A ground point {@code dist} blocks ahead of the wielder, pulled back from a wall if one is in the way. */
    private static Vec3 ahead(ServerPlayer p, double dist) {
        Vec3 base = p.position().add(0, 0.6, 0);
        Vec3 flat = Strike.flatLook(p);
        Vec3 end = base.add(flat.scale(dist));
        Vec3 hit = Strike.stopAtWall(p, base, end);
        if (hit.distanceToSqr(end) > 1.0E-6) hit = hit.subtract(flat.scale(0.4));
        return new Vec3(hit.x, p.getY(), hit.z);
    }

    /** The shared look of a ground blow landing: a strike ring, the crushing impact and the sound of a mace. */
    private static void impact(ServerLevel l, Vec3 at, double radius, boolean heavy) {
        StrikeFx.strikeRing(l, at, radius, AMBER);
        StrikeFx.impact(l, at, radius * 0.8, AMBER);
        StrikeFx.sound(l, at, heavy ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND, 1.0f, 0.9f);
    }

    // ================================================================ Overhead Smash
    private static StrikeSkill overheadSmash() {
        return StrikeSkill.of(HammerNumbers.OVERHEAD_SMASH, "Overhead Smash",
                "Raises the hammer and brings it down on the ground ahead; a ring shows where it lands. Heavy damage, a shove and a lot of poise to everything standing in it. Step out of the ring, or jump.",
                c -> {
                    ServerPlayer p = c.player;
                    Vec3 spot = ahead(p, HammerNumbers.SMASH_REACH);
                    StrikeFx.tellRing(c.level, spot, HammerNumbers.SMASH_RADIUS, AMBER, HammerNumbers.SMASH_DELAY + 4);
                    StrikeFx.ring(c.level, spot, HammerNumbers.SMASH_RADIUS, AMBER);
                    StrikeFx.sound(c.level, p.position(), SoundEvents.ANVIL_USE, 0.9f, 0.6f);
                    Strike.effect(p, "minecraft:slowness", HammerNumbers.SMASH_DELAY, 2);
                    Strike.later(p, HammerNumbers.SMASH_DELAY, cx -> smash(cx, spot));
                    return true;
                });
    }

    private static void smash(HeroCtx c, Vec3 spot) {
        int n = 0;
        for (LivingEntity t : Strike.foesNear(c.player, spot, HammerNumbers.SMASH_RADIUS, 12)) {
            if (!Strike.grounded(t) || !Strike.hit(c, t, raw(HammerNumbers.OVERHEAD_SMASH), false)) continue;
            n++;
            HammerPoise.add(c, t, HammerNumbers.SMASH_POISE);
            Strike.shove(c, t, spot, 0.55);
            Strike.lift(t, 0.22);
        }
        impact(c.level, spot, HammerNumbers.SMASH_RADIUS, n > 0);
    }

    // ================================================================ Shockwave
    private static StrikeSkill shockwave() {
        return StrikeSkill.of(HammerNumbers.SHOCKWAVE, "Shockwave",
                "Strikes the ground and sends a crack running ahead along it. A dotted line shows the path first. Hits everything on the ground along the line; whatever is in the air passes over it.",
                c -> {
                    ServerPlayer p = c.player;
                    Vec3 flat = Strike.flatLook(p);
                    Vec3 base = p.position().add(0, 0.6, 0);
                    Vec3 far = Strike.stopAtWall(p, base, base.add(flat.scale(HammerNumbers.SHOCK_LENGTH)));
                    Vec3 from = p.position().add(flat.scale(1.2));
                    Vec3 to = new Vec3(far.x, p.getY(), far.z);
                    if (from.distanceTo(to) < 2.5) {
                        c.say("There is no room for a shockwave.");
                        return false;
                    }
                    StrikeFx.tellCrack(c.level, from, to, AMBER, HammerNumbers.SHOCK_DELAY + 4);
                    StrikeFx.line(c.level, from, to, AMBER);
                    StrikeFx.sound(c.level, p.position(), SoundEvents.ANVIL_USE, 0.8f, 0.8f);
                    Strike.later(p, HammerNumbers.SHOCK_DELAY, cx -> wave(cx, from, to));
                    return true;
                });
    }

    private static void wave(HeroCtx c, Vec3 from, Vec3 to) {
        Vec3 a = from.add(0, 0.6, 0), b = to.add(0, 0.6, 0);
        for (LivingEntity t : Strike.foesLine(c.player, a, b, HammerNumbers.SHOCK_HALF_WIDTH, 12)) {
            if (!Strike.grounded(t) || !Strike.hit(c, t, raw(HammerNumbers.SHOCKWAVE), false)) continue;
            HammerPoise.add(c, t, HammerNumbers.SHOCK_POISE);
            Strike.shove(c, t, from, 0.5);
            Strike.lift(t, 0.18);
        }
        StrikeFx.strikeCrack(c.level, from, to, AMBER, 30);
        StrikeFx.sound(c.level, from, SoundEvents.MACE_SMASH_GROUND, 0.9f, 1.1f);
    }

    // ================================================================ Armor Crack
    private static StrikeSkill armorCrack() {
        return StrikeSkill.of(HammerNumbers.ARMOR_CRACK, "Armor Crack",
                "A short, hard blow on the foe you face. A ring marks it for a moment, then the hammer lands: heavy damage and a lot of poise, and the foe's armour is cracked for eight seconds.",
                c -> {
                    LivingEntity t = Strike.pick(c.player, HammerNumbers.CRACK_RANGE, 40);
                    if (t == null) {
                        c.say("No foe within reach.");
                        return false;
                    }
                    StrikeFx.ring(c.level, t.position(), 1.2, AMBER);
                    StrikeFx.gather(c.level, Strike.center(t), 0.7, AMBER, 8);
                    StrikeFx.sound(c.level, c.player.position(), SoundEvents.ANVIL_USE, 0.9f, 0.5f);
                    Strike.later(c.player, HammerNumbers.CRACK_DELAY, cx -> crack(cx, t));
                    return true;
                });
    }

    private static void crack(HeroCtx c, LivingEntity t) {
        if (!t.isAlive() || t.level() != c.level || t.distanceTo(c.player) > HammerNumbers.CRACK_RANGE + 1.5) {
            c.say("The foe slipped away.");
            return;
        }
        if (!Strike.hit(c, t, raw(HammerNumbers.ARMOR_CRACK), false)) return;
        HammerPoise.add(c, t, HammerNumbers.CRACK_POISE);
        HammerPoise.crackArmor(t, HammerNumbers.CRACK_ARMOR, HammerNumbers.CRACK_TOUGHNESS, HammerNumbers.CRACK_TICKS);
        StrikeFx.impact(c.level, t.position(), 1.6, AMBER);
        StrikeFx.sound(c.level, Strike.center(t), SoundEvents.SHIELD_BREAK, 1.0f, 0.7f);
    }

    // ================================================================ Quake Stomp
    private static StrikeSkill quakeStomp() {
        return StrikeSkill.of(HammerNumbers.QUAKE_STOMP, "Quake Stomp",
                "Plants the hammer and stamps: the ground heaves in a wide ring round you. A ring closes in first. Everything standing on the ground inside is hurt, thrown outward and loses poise; jump to avoid it.",
                c -> {
                    ServerPlayer p = c.player;
                    Vec3 spot = p.position();
                    StrikeFx.tellRing(c.level, spot, HammerNumbers.QUAKE_RADIUS, AMBER, HammerNumbers.QUAKE_DELAY + 4);
                    StrikeFx.ring(c.level, spot, HammerNumbers.QUAKE_RADIUS, AMBER);
                    StrikeFx.sound(c.level, spot, SoundEvents.ANVIL_USE, 1.0f, 0.5f);
                    Strike.effect(p, "minecraft:slowness", HammerNumbers.QUAKE_DELAY, 3);
                    Strike.later(p, HammerNumbers.QUAKE_DELAY, cx -> quake(cx, spot));
                    return true;
                });
    }

    private static void quake(HeroCtx c, Vec3 spot) {
        for (LivingEntity t : Strike.foesNear(c.player, spot, HammerNumbers.QUAKE_RADIUS, 12)) {
            if (!Strike.grounded(t) || !Strike.hit(c, t, raw(HammerNumbers.QUAKE_STOMP), false)) continue;
            HammerPoise.add(c, t, HammerNumbers.QUAKE_POISE);
            Strike.shove(c, t, spot, 0.9);
            Strike.lift(t, 0.3);
        }
        impact(c.level, spot, HammerNumbers.QUAKE_RADIUS, true);
    }

    // ================================================================ Skyward Smash
    private static StrikeSkill skywardSmash() {
        return StrikeSkill.of(HammerNumbers.SKYWARD_SMASH, "Skyward Smash",
                "An upward blow on the foe you face that throws it into the air, out of reach of the ground blows, so a friend can strike it or you can break off. A boss is only nudged.",
                c -> {
                    LivingEntity t = Strike.pick(c.player, HammerNumbers.SKYWARD_RANGE, 45);
                    if (t == null) {
                        c.say("No foe within reach.");
                        return false;
                    }
                    StrikeFx.ring(c.level, t.position(), 1.0, AMBER);
                    StrikeFx.sound(c.level, c.player.position(), SoundEvents.ANVIL_USE, 0.7f, 0.9f);
                    Strike.later(c.player, HammerNumbers.SKYWARD_DELAY, cx -> skyward(cx, t));
                    return true;
                });
    }

    private static void skyward(HeroCtx c, LivingEntity t) {
        if (!t.isAlive() || t.level() != c.level || t.distanceTo(c.player) > HammerNumbers.SKYWARD_RANGE + 1.5) {
            c.say("The foe slipped away.");
            return;
        }
        if (!Strike.hit(c, t, raw(HammerNumbers.SKYWARD_SMASH), false)) return;
        HammerPoise.add(c, t, HammerNumbers.SKYWARD_POISE);
        Strike.lift(t, BalanceLaw.isBoss(t) ? 0.25 : 0.95);
        Strike.shove(c, t, c.player.position(), 0.15);
        StrikeFx.impact(c.level, t.position(), 1.4, AMBER);
        StrikeFx.sound(c.level, Strike.center(t), SoundEvents.MACE_SMASH_AIR, 1.0f, 1.0f);
    }

    // ================================================================ Forge Aegis
    private static StrikeSkill forgeAegis() {
        return StrikeSkill.of(HammerNumbers.FORGE_AEGIS, "Forge Aegis",
                "Strikes the anvil of the world: you and your allies within seven blocks are tempered for eight seconds, taking less damage and gaining two hearts of absorption. It heals nothing.",
                c -> {
                    ServerPlayer p = c.player;
                    int n = 0;
                    for (LivingEntity a : SongTargets.allies(p, p.position(), HammerNumbers.AEGIS_RADIUS)) {
                        if (n >= HammerNumbers.FORGE_AEGIS.maxTargets()) break;
                        Strike.effect(a, "minecraft:resistance", HammerNumbers.AEGIS_TICKS, 0);
                        Strike.effect(a, "minecraft:absorption", HammerNumbers.AEGIS_TICKS, 0);
                        StrikeFx.burst(c.level, Strike.center(a), AMBER, 8);
                        n++;
                    }
                    StrikeFx.ring(c.level, p.position(), HammerNumbers.AEGIS_RADIUS, AMBER);
                    StrikeFx.follow(c.level, VfxShape.MAGIC_CIRCLE, p, p.position().add(0, 1, 0), AMBER, 30, 2.4f);
                    StrikeFx.sound(c.level, p.position(), SoundEvents.ANVIL_USE, 1.0f, 1.2f);
                    StrikeFx.sound(c.level, p.position(), SoundEvents.SMITHING_TABLE_USE, 0.8f, 1.0f);
                    c.say("Forge Aegis: " + n + " tempered.");
                    return true;
                });
    }

    // ================================================================ Titan's Fall
    private static StrikeSkill titanFall() {
        return StrikeSkill.of(HammerNumbers.TITAN_FALL, "Titan's Fall",
                "Leaps high with the hammer and comes down like a falling mountain. A ring follows you down to show where it will land. Huge damage and poise to everything in it, enough to break a golem. You take no fall damage.",
                c -> {
                    ServerPlayer p = c.player;
                    if (!p.onGround()) {
                        c.say("Titan's Fall needs solid ground to leap from.");
                        return false;
                    }
                    Strike.leap(p, Strike.flatLook(p), 0.35, 1.05);
                    StrikeFx.column(c.level, p.position(), 4.0, AMBER);
                    StrikeFx.sound(c.level, p.position(), SoundEvents.MACE_SMASH_AIR, 1.0f, 0.7f);
                    boolean[] landed = {false};
                    Strike.zone(p, HammerNumbers.TITAN_MAX_AIR, 2, (cx, age) -> {
                        if (landed[0]) return;
                        ServerPlayer w = cx.player;
                        w.fallDistance = 0f;
                        boolean down = age >= HammerNumbers.TITAN_MIN_AIR && (w.onGround() || w.isInWater());
                        boolean last = age >= HammerNumbers.TITAN_MAX_AIR - 2;
                        if (!down && !last) {
                            if (age % 6 == 0) StrikeFx.ring(cx.level, Strike.groundUnder(w, w.position()), HammerNumbers.TITAN_RADIUS, AMBER);
                            return;
                        }
                        landed[0] = true;
                        slam(cx);
                    });
                    return true;
                });
    }

    private static void slam(HeroCtx c) {
        Vec3 spot = c.player.position();
        c.player.fallDistance = 0f;
        int n = 0;
        for (LivingEntity t : Strike.foesNear(c.player, spot, HammerNumbers.TITAN_RADIUS, 12)) {
            if (!Strike.hit(c, t, raw(HammerNumbers.TITAN_FALL), false)) continue;
            n++;
            HammerPoise.add(c, t, HammerNumbers.TITAN_POISE);
            Strike.shove(c, t, spot, 1.2);
            Strike.lift(t, 0.5);
        }
        impact(c.level, spot, HammerNumbers.TITAN_RADIUS, true);
        StrikeFx.column(c.level, spot, 5.0, AMBER);
        StrikeFx.sound(c.level, spot, SoundEvents.ANVIL_LAND, 1.0f, 0.6f);
        c.say(n == 0 ? "Titan's Fall lands on nothing." : "Titan's Fall: " + n + " foes crushed.");
    }
}
