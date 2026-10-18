package com.newuniverse.nusmp.heroweapon.weapon.claw;

import com.newuniverse.nusmp.blackclover.TimedModifiers;
import com.newuniverse.nusmp.entity.zogratis.ZogratisVfx;
import com.newuniverse.nusmp.heroweapon.curse.HeroCurse;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.skill.SimpleSkill;
import com.newuniverse.nusmp.heroweapon.weapon.strike.SkillSpec;
import com.newuniverse.nusmp.heroweapon.weapon.strike.Strike;
import com.newuniverse.nusmp.heroweapon.weapon.strike.StrikeFx;
import com.newuniverse.nusmp.heroweapon.weapon.strike.StrikeSkill;
import com.newuniverse.nusmp.relics.djinn.weapon.WeaponGeo;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * Why: the eight techniques of the Vassal Claw, written for a fast, light weapon. Air Strike Slash, Second Slash and the Wahnsinn Claw are the three the series names; they and
 * the others either OPEN wounds ({@link ClawBleed}) or CASH THEM IN (Hemorrhage). Every damaging technique grows with use (canon Skill Mastery, {@link ClawMastery}): a used technique
 * hits up to 30 percent harder and comes back up to 20 percent sooner. The Air Strike Slash is resolved at the cast but lands when the crescent arrives, and misses a foe that
 * has stepped out of its line by then; the heavy Hemorrhage and the Wahnsinn Claw warn first. All damage goes through {@link Strike#hit} (the balance law); nothing here changes
 * a block, heals the wielder or holds a foe still. The numbers are {@link ClawNumbers}'s.
 */
final class ClawSkills {
    private ClawSkills() {}

    /** The amber of the gem (and of the beast effects), and the crimson of the cursed beam. */
    private static final int AMBER = 0xFFA030, CRIMSON = 0xD01830;
    private static final String COMBO = "nusmp_cl_combo", EXTRA = "claw";
    private static final ResourceLocation SHRED = ResourceLocation.fromNamespaceAndPath("nusmp", "hero_claw_shred");

    static List<HeroSkill> all() {
        return List.of(airStrikeSlash(), secondSlash(), hemorrhage(), pounce(), shreddingRush(), predatorInstinct(), dragonRend(), wahnsinnClaw());
    }

    // ------------------------------------------------------------------ skill mastery (canon power-up): uses per technique, kept in the bond
    static int uses(HeroCtx c, SkillSpec spec) { return c.bond.hasExtra(EXTRA) ? c.bond.extra(EXTRA).getInt(spec.shortName()) : 0; }

    static int level(HeroCtx c, SkillSpec spec) { return ClawMastery.level(uses(c, spec)); }

    private static void bump(HeroCtx c, SkillSpec spec) {
        CompoundTag t = c.bond.extra(EXTRA);
        int before = t.getInt(spec.shortName());
        t.putInt(spec.shortName(), before + 1);
        if (ClawMastery.leveledUp(before)) c.say(pretty(spec.shortName()) + " mastery " + ClawMastery.level(before + 1));
    }

    private static String pretty(String shortName) {
        String s = shortName.replace('_', ' ');
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    /** The raw damage of a hit: {@code base} times the technique's mastery factor (when the form has the Skill Mastery passive). */
    static float raw(HeroCtx c, SkillSpec spec, double base) {
        double m = c.form.has(ClawNumbers.SKILL_MASTERY) ? ClawMastery.damageMult(level(c, spec)) : 1.0;
        return (float) (base * m);
    }

    /** A technique that counts its uses and recovers faster as it is mastered. */
    private static StrikeSkill mastered(SkillSpec spec, String name, String desc, SimpleSkill.Body body) {
        return StrikeSkill.of(spec, name, desc, c -> {
            boolean ok = body.cast(c);
            if (ok && c.form.has(ClawNumbers.SKILL_MASTERY)) bump(c, spec);
            return ok;
        }).cooldownFactor(c -> c.form.has(ClawNumbers.SKILL_MASTERY) ? ClawMastery.cooldownMult(level(c, spec)) : 1.0);
    }

    // ================================================================ Air Strike Slash (canon)
    private static StrikeSkill airStrikeSlash() {
        return mastered(ClawNumbers.AIR_STRIKE_SLASH, "Air Strike Slash",
                "Slashes the air and sends a crescent flying ahead; it cuts up to three foes in its line and opens a wound on each. A foe that steps out of the line in time is missed. A Second Slash soon after is stronger.",
                c -> {
                    ServerPlayer p = c.player;
                    Vec3 look = p.getLookAngle();
                    Vec3 start = p.getEyePosition().add(look.scale(0.5)).add(0, -0.3, 0);
                    Vec3 end = Strike.stopAtWall(p, start, start.add(look.scale(ClawNumbers.SLASH_RANGE)));
                    float damage = raw(c, ClawNumbers.AIR_STRIKE_SLASH, ClawNumbers.AIR_STRIKE_SLASH.raw());
                    for (LivingEntity t : Strike.foesLine(p, start, end, ClawNumbers.SLASH_HALF_WIDTH, ClawNumbers.AIR_STRIKE_SLASH.maxTargets())) {
                        int delay = Math.max(1, (int) Math.round(start.distanceTo(Strike.center(t)) / ClawNumbers.SLASH_SPEED));
                        Strike.later(p, delay, cx -> slashLands(cx, t, start, end, damage));
                    }
                    StrikeFx.shape(c.level, VfxShape.WIND_SLASH, start, end, AMBER, 8, 1.2f);
                    StrikeFx.sound(c.level, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
                    return true;
                });
    }

    private static void slashLands(HeroCtx c, LivingEntity t, Vec3 a, Vec3 b, float damage) {
        if (!t.isAlive()) return;
        Vec3 m = Strike.center(t);
        double miss = WeaponGeo.segmentDistance(a.x, a.y, a.z, b.x, b.y, b.z, m.x, m.y, m.z);
        if (miss > ClawNumbers.SLASH_HALF_WIDTH + 0.7 + t.getBbWidth() * 0.5) return;                       // it stepped out of the way
        if (!Strike.hit(c, t, damage, false)) return;
        ClawBleed.add(c, t, 1);
        c.player.getPersistentData().putLong(COMBO, c.level.getGameTime());
        StrikeFx.burst(c.level, m, ClawBleed.BLOOD, 4);
    }

    // ================================================================ Second Slash (canon)
    private static StrikeSkill secondSlash() {
        return mastered(ClawNumbers.SECOND_SLASH, "Second Slash",
                "Two crossing cuts in a fan in front of you, a wound with each. Used within five seconds of an Air Strike Slash that found a foe it is the follow-up: it lunges, reaches farther and hits 40 percent harder.",
                c -> {
                    ServerPlayer p = c.player;
                    boolean combo = c.level.getGameTime() - p.getPersistentData().getLong(COMBO) <= ClawNumbers.COMBO_WINDOW;
                    double range = combo ? ClawNumbers.SECOND_RANGE_COMBO : ClawNumbers.SECOND_RANGE;
                    List<LivingEntity> foes = Strike.foesCone(p, range, ClawNumbers.SECOND_HALF_ANGLE, ClawNumbers.SECOND_SLASH.maxTargets());
                    if (foes.isEmpty()) {
                        c.say("Nothing in reach to cut.");
                        return false;
                    }
                    float damage = raw(c, ClawNumbers.SECOND_SLASH, ClawNumbers.SECOND_BASE * (combo ? ClawNumbers.COMBO_MULT : 1.0));
                    if (combo) {
                        Vec3 to = foes.get(0).position().subtract(p.position());
                        if (Math.sqrt(to.x * to.x + to.z * to.z) > 2.6) Strike.leap(p, to, 0.7, 0.15);
                    }
                    p.getPersistentData().putLong(COMBO, 0L);                                                    // the follow-up is spent
                    for (int k = 0; k < 2; k++) Strike.later(p, 2 + 3 * k, cx -> cut(cx, range, damage));
                    return true;
                });
    }

    private static void cut(HeroCtx c, double range, float damage) {
        for (LivingEntity t : Strike.foesCone(c.player, range, ClawNumbers.SECOND_HALF_ANGLE, ClawNumbers.SECOND_SLASH.maxTargets())) {
            if (!Strike.hit(c, t, damage, true)) continue;
            ClawBleed.add(c, t, 1);
            StrikeFx.burst(c.level, Strike.center(t), ClawBleed.BLOOD, 4);
        }
        Vec3 eye = c.player.getEyePosition(), flat = Strike.flatLook(c.player);
        StrikeFx.shape(c.level, VfxShape.WIND_SLASH, eye.add(0, -0.2, 0), eye.add(flat.scale(range)).add(0, -0.6, 0), AMBER, 6, 1.0f);
        StrikeFx.sound(c.level, c.player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.3f);
    }

    // ================================================================ Hemorrhage
    private static StrikeSkill hemorrhage() {
        return mastered(ClawNumbers.HEMORRHAGE, "Hemorrhage",
                "Tears open every wound on the foe you face at once. The burst is bigger the more wounds are open (up to five, seven with the Dragon Claws) and the wounds close. A foe that is not bleeding is not touched.",
                c -> {
                    LivingEntity t = Strike.pick(c.player, ClawNumbers.HEMORRHAGE_RANGE, 40);
                    if (t == null) {
                        c.say("No foe within reach.");
                        return false;
                    }
                    if (ClawBleed.stacks(t) <= 0) {
                        c.say("The foe is not bleeding.");
                        return false;
                    }
                    StrikeFx.gather(c.level, Strike.center(t), 0.7, ClawBleed.BLOOD, 12);
                    StrikeFx.sound(c.level, c.player.position(), SoundEvents.FOX_BITE, 1.0f, 0.7f);
                    Strike.later(c.player, ClawNumbers.HEMORRHAGE_DELAY, cx -> burst(cx, t));
                    return true;
                });
    }

    private static void burst(HeroCtx c, LivingEntity t) {
        if (!t.isAlive() || t.level() != c.level || t.distanceTo(c.player) > ClawNumbers.HEMORRHAGE_RANGE + 1.5) return;
        int stacks = ClawBleed.stacks(t);
        if (stacks <= 0) return;
        float damage = raw(c, ClawNumbers.HEMORRHAGE, BleedMath.burst(stacks));
        if (!Strike.hit(c, t, damage, false)) return;
        ClawBleed.clear(t);
        ZogratisVfx.downpour(c.level, t.position(), 1.6, 18, ClawBleed.BLOOD);
        StrikeFx.sound(c.level, Strike.center(t), SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
    }

    // ================================================================ Pounce
    private static StrikeSkill pounce() {
        return mastered(ClawNumbers.POUNCE, "Pounce",
                "Leaps far forward and lands with the claws out: foes at the landing are cut and bleed twice over. You take no fall damage. With the Rope Claws the leap homes in on the foe you face.",
                c -> {
                    ServerPlayer p = c.player;
                    Vec3 dir = Strike.flatLook(p);
                    double speed = ClawNumbers.POUNCE_SPEED;
                    if (c.form.has(ClawNumbers.ROPE_HOOK)) {
                        LivingEntity lock = Strike.pick(p, ClawNumbers.POUNCE_LOCK_RANGE, ClawNumbers.ROPE_ANGLE);
                        if (lock != null) {
                            Vec3 to = lock.position().subtract(p.position());
                            double dist = Math.sqrt(to.x * to.x + to.z * to.z);
                            if (dist > 1.0E-3) {
                                dir = new Vec3(to.x / dist, 0, to.z / dist);
                                speed = Math.max(0.5, Math.min(1.4, (dist - 1.2) / 7.5));
                            }
                        }
                    }
                    Strike.leap(p, dir, speed, ClawNumbers.POUNCE_LIFT);
                    Vec3 aim = p.position().add(dir.scale(speed * 7.5)).add(0, 0.8, 0);
                    StrikeFx.shape(c.level, VfxShape.BEAST_FX1, p.getEyePosition().add(0, -0.3, 0), aim, AMBER, 10, 1.2f);
                    StrikeFx.sound(c.level, p.position(), SoundEvents.WOLF_GROWL, 0.8f, 1.2f);
                    boolean[] done = {false};
                    Strike.zone(p, ClawNumbers.POUNCE_MAX_AIR, 1, (cx, age) -> {
                        if (done[0]) return;
                        cx.player.fallDistance = 0f;
                        boolean down = age >= ClawNumbers.POUNCE_MIN_AIR && (cx.player.onGround() || cx.player.isInWater());
                        if (!down && age < ClawNumbers.POUNCE_MAX_AIR - 1) return;
                        done[0] = true;
                        land(cx);
                    });
                    return true;
                });
    }

    private static void land(HeroCtx c) {
        Vec3 at = c.player.position();
        float damage = raw(c, ClawNumbers.POUNCE, ClawNumbers.POUNCE.raw());
        for (LivingEntity t : Strike.foesNear(c.player, at, ClawNumbers.POUNCE_RADIUS, ClawNumbers.POUNCE.maxTargets())) {
            if (!Strike.hit(c, t, damage, false)) continue;
            ClawBleed.add(c, t, 2);
            Strike.shove(c, t, at, 0.3);
        }
        StrikeFx.shape(c.level, VfxShape.BEAST_FX3, at.add(0, 0.3, 0), at.add(Strike.flatLook(c.player)), AMBER, 22, 0.9f);
        StrikeFx.sound(c.level, at, SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 1.1f);
    }

    // ================================================================ Shredding Rush
    private static StrikeSkill shreddingRush() {
        return mastered(ClawNumbers.SHREDDING_RUSH, "Shredding Rush",
                "Rushes the foe you face and rakes it four times in a flurry. Each cut opens a wound and strips more of its armour (7 percent a cut, 28 percent after the fourth) for five seconds. If it gets away from you the flurry stops.",
                c -> {
                    ServerPlayer p = c.player;
                    LivingEntity t = Strike.pick(p, ClawNumbers.RUSH_RANGE, 45);
                    if (t == null) {
                        c.say("No foe in range.");
                        return false;
                    }
                    Vec3 to = t.position().subtract(p.position());
                    double dist = Math.sqrt(to.x * to.x + to.z * to.z);
                    if (dist > 2.6) Strike.leap(p, to, Math.max(0.25, Math.min(1.4, (dist - 2.0) / 2.2)), 0.1);
                    StrikeFx.shape(c.level, VfxShape.BEAST_FX1, p.getEyePosition().add(0, -0.3, 0), Strike.center(t), AMBER, 8, 1.0f);
                    float damage = raw(c, ClawNumbers.SHREDDING_RUSH, ClawNumbers.SHREDDING_RUSH.raw());
                    for (int k = 0; k < ClawNumbers.RUSH_CUTS; k++) {
                        int cut = k;
                        Strike.later(p, ClawNumbers.RUSH_FIRST + ClawNumbers.RUSH_GAP * k, cx -> rushCut(cx, t, cut, damage));
                    }
                    return true;
                });
    }

    private static void rushCut(HeroCtx c, LivingEntity t, int cut, float damage) {
        if (!t.isAlive() || t.level() != c.level || c.player.distanceTo(t) > ClawNumbers.RUSH_REACH) return;
        if (!Strike.hit(c, t, damage, true)) return;
        ClawBleed.add(c, t, 1);
        TimedModifiers.apply(t, Attributes.ARMOR, SHRED, ClawNumbers.RUSH_ARMOR_STEP * (cut + 1), ClawNumbers.RUSH_ARMOR_TICKS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        StrikeFx.shape(c.level, VfxShape.WIND_SLASH, c.player.getEyePosition().add(0, -0.3, 0), Strike.center(t), AMBER, 6, 0.8f);
        StrikeFx.sound(c.level, c.player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.4f + 0.1f * cut);
    }

    // ================================================================ Predator's Instinct
    private static StrikeSkill predatorInstinct() {
        return StrikeSkill.of(ClawNumbers.PREDATOR_INSTINCT, "Predator's Instinct",
                "The beast in the gem wakes for eight seconds: you gain speed and haste, and every foe within twenty blocks that is bleeding glows, so you can find it through walls and in the dark.",
                c -> {
                    ServerPlayer p = c.player;
                    Strike.effect(p, "minecraft:speed", ClawNumbers.INSTINCT_TICKS, 1);
                    Strike.effect(p, "minecraft:haste", ClawNumbers.INSTINCT_TICKS, 1);
                    int seen = 0;
                    for (LivingEntity t : Strike.foesAround(p, ClawNumbers.INSTINCT_SIGHT, ClawNumbers.PREDATOR_INSTINCT.maxTargets())) {
                        if (ClawBleed.stacks(t) <= 0) continue;
                        Strike.effect(t, "minecraft:glowing", ClawNumbers.INSTINCT_TICKS, 0);
                        seen++;
                    }
                    StrikeFx.shape(c.level, VfxShape.BEAST_FX2, p.position(), p.position().add(0, 1, 0), AMBER, 60, 3.5f);
                    StrikeFx.sound(c.level, p.position(), SoundEvents.WOLF_GROWL, 1.0f, 0.7f);
                    c.say("Predator's Instinct: " + seen + " bleeding foes in sight.");
                    return true;
                });
    }

    // ================================================================ Dragon Rend (ultimate)
    private static StrikeSkill dragonRend() {
        return mastered(ClawNumbers.DRAGON_REND, "Dragon Rend",
                "Marks a twelve-block line in front of you; a moment later you tear through everything in it in one run, then rake each foe twice more as you pass: three cuts and three wounds on up to eight foes. Needs foes in the path when you start; a foe that leaves the line in time is missed.",
                c -> {
                    ServerPlayer p = c.player;
                    Vec3 dir = Strike.flatLook(p);
                    Vec3 end = rendEnd(p, dir);
                    if (Strike.foesLine(p, rendStart(p), end, ClawNumbers.REND_HALF_WIDTH, ClawNumbers.DRAGON_REND.maxTargets()).isEmpty()) {
                        c.say("Nothing in the path of the Dragon Claws.");
                        return false;
                    }
                    StrikeFx.tellCrack(c.level, p.position(), new Vec3(end.x, p.getY(), end.z), AMBER, ClawNumbers.REND_WARN + 4);
                    StrikeFx.gather(c.level, p.position().add(0, 1.0, 0), 1.2, AMBER, 12);
                    StrikeFx.sound(c.level, p.position(), SoundEvents.WOLF_GROWL, 1.0f, 0.5f);
                    Strike.effect(p, "minecraft:slowness", ClawNumbers.REND_WARN, 3);                  // held still while the line shows, so the line shown is the line run
                    Strike.later(p, ClawNumbers.REND_WARN, cx -> rush(cx, dir));
                    return true;
                });
    }

    /** Where the run of the Dragon Claws starts: the wielder's chest. */
    private static Vec3 rendStart(ServerPlayer p) { return p.position().add(0, 0.9, 0); }

    /** Where the run ends: twelve blocks along {@code dir}, or at the first wall, at chest height. */
    private static Vec3 rendEnd(ServerPlayer p, Vec3 dir) {
        Vec3 a = rendStart(p);
        Vec3 far = Strike.stopAtWall(p, a, a.add(dir.scale(ClawNumbers.REND_LENGTH)));
        return new Vec3(far.x, a.y, far.z);
    }

    /** The run itself: the foes still in the line are picked now, the wielder dashes along it and rakes them three times. */
    private static void rush(HeroCtx c, Vec3 dir) {
        ServerPlayer p = c.player;
        Vec3 a = rendStart(p), b = rendEnd(p, dir);
        List<LivingEntity> foes = Strike.foesLine(p, a, b, ClawNumbers.REND_HALF_WIDTH, ClawNumbers.DRAGON_REND.maxTargets());
        double run = Math.max(2.0, a.distanceTo(b) - 1.0);
        Strike.leap(p, dir, Math.min(1.5, run / 2.2), 0.12);
        StrikeFx.shape(c.level, VfxShape.BEAST_FX1, p.getEyePosition().add(0, -0.3, 0), b, AMBER, 12, 2.0f);
        StrikeFx.sound(c.level, p.position(), SoundEvents.WOLF_GROWL, 1.0f, 0.6f);
        float damage = raw(c, ClawNumbers.DRAGON_REND, ClawNumbers.DRAGON_REND.raw());
        for (int k = 0; k < ClawNumbers.REND_PASSES; k++) {
            int pass = k;
            Strike.later(p, ClawNumbers.REND_FIRST + ClawNumbers.REND_GAP * k, cx -> rend(cx, foes, pass, damage));
        }
    }

    private static void rend(HeroCtx c, List<LivingEntity> foes, int pass, float damage) {
        for (LivingEntity t : foes) {
            if (!t.isAlive() || t.level() != c.level || c.player.distanceTo(t) > 16.0) continue;
            if (!Strike.hit(c, t, damage, true)) continue;
            ClawBleed.add(c, t, 1);
            Vec3 m = Strike.center(t);
            StrikeFx.shape(c.level, VfxShape.BEAST_FX3, m, m.add(Strike.flatLook(c.player)), AMBER, 16, pass == ClawNumbers.REND_PASSES - 1 ? 1.2f : 0.8f);
        }
        StrikeFx.sound(c.level, c.player.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.8f + 0.15f * pass);
    }

    // ================================================================ Wahnsinn Claw (the curse form)
    private static StrikeSkill wahnsinnClaw() {
        return mastered(ClawNumbers.WAHNSINN_CLAW, "Wahnsinn Claw",
                "The cursed claw fires a beam of crimson light down a fourteen-block line after a short warning. Foes still in the line are hurt, cursed for eight seconds and cut with three wounds. It eats at your mind.",
                c -> {
                    ServerPlayer p = c.player;
                    Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
                    Vec3 from = eye.add(look.scale(0.6)).add(0, -0.3, 0);
                    Vec3 end = Strike.stopAtWall(p, eye, eye.add(look.scale(ClawNumbers.MADNESS_LENGTH)));
                    ZogratisVfx.ray(c.level, from, end, 0.8, ClawNumbers.MADNESS_DELAY + 2, CRIMSON, true);
                    StrikeFx.sound(c.level, p.position(), SoundEvents.ENDER_EYE_LAUNCH, 1.0f, 0.5f);
                    HeroCurse.addCorruption(p, ClawNumbers.MADNESS_CORRUPTION);
                    float damage = raw(c, ClawNumbers.WAHNSINN_CLAW, ClawNumbers.WAHNSINN_CLAW.raw());
                    Strike.later(p, ClawNumbers.MADNESS_DELAY, cx -> beam(cx, from, end, damage));
                    return true;
                });
    }

    private static void beam(HeroCtx c, Vec3 from, Vec3 end, float damage) {
        for (LivingEntity t : Strike.foesLine(c.player, from, end, ClawNumbers.MADNESS_HALF_WIDTH, ClawNumbers.WAHNSINN_CLAW.maxTargets())) {
            if (!Strike.hit(c, t, damage, false)) continue;
            Strike.effect(t, "tensura:curse", ClawNumbers.MADNESS_CURSE_TICKS, 0);
            ClawBleed.add(c, t, 3);
        }
        ZogratisVfx.ray(c.level, from, end, 1.6, 12, CRIMSON, false);
        StrikeFx.sound(c.level, end, SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 1.4f);
    }
}
