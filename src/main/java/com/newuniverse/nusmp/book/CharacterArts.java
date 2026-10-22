package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the casts of the pages that {@link CharacterPages} appends for the pilot characters (docs/magic/character_spells.md). Each one is a plain {@link BookPage.Cast}: the server decides
 * everything (targets, damage, effects) with the same helpers every other page uses, and the picture is a LAYERED effect of the existing {@link VfxShape}s (two to four shapes per cast, so a
 * character's page reads richer than a basic one) sent to nearby players only through {@link VfxSpawn}, which is inside the performance packet budget.
 *
 * <p>Worst case per cast (the doc repeats this): at most 4 effect packets (a rain of blades: 6), one zone per cast that lasts at most 15 s and runs every 5 to 20 ticks, at most 8 live targets
 * hurt per tick of a zone (the same {@code around} / {@code along} queries every page uses), no entity is spawned, no chunk is loaded, no block is placed. The cooldowns are 10 to 45 s.
 * Effects that were not described precisely by the sources are marked "inferred" in the doc: the numbers are this mod's, the names and the intent are the series'.
 */
public final class CharacterArts {
    private CharacterArts() {}

    private static final int MAX_HITS = 8;

    private static boolean gone(ServerPlayer p) { return p.isRemoved() || !p.isAlive(); }

    private static boolean diving(ManasSkillInstance i, ServerPlayer p) { return p.level().getGameTime() < i.getOrCreateTag().getLong("DiveUntil"); }

    private static void fx(ServerPlayer p, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) {
        VfxSpawn.send(p.serverLevel(), shape, from, to, 0, ticks, power);
    }

    private static void push(LivingEntity t, Vec3 dir, double h, double up) {
        t.setDeltaMovement(dir.x * h, up, dir.z * h);
        t.hurtMarked = true;
    }

    // ================================================================ Yuno: Wind Magic and Wind Spirit Magic
    /** Tornado Fang: the Towering Tornado laid on its side, a rolling drill of wind that drags everything along a lane of 16 blocks. */
    public static boolean tornadoFang(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 a = p.position().add(0, 1, 0), dir = p.getViewVector(1f).multiply(1, 0.15, 1).normalize(), end = a.add(dir.scale(16 * s));
        b.castCircle(p, 1.2f);
        CanonSpells.cutSpells(p, new AABB(a, end).inflate(2));
        int n = 0;
        for (LivingEntity t : GrimoireBook.along(p, a, end, 2.0)) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 11f);
            push(t, dir, 1.2, 0.45);
        }
        b.vfx(p, VfxShape.WIND_GALE, a, end, 20, 1.4f * s);
        for (int k = 1; k <= 3; k++) {
            Vec3 c = a.add(dir.scale(16 * s * k / 4.0));
            fx(p, VfxShape.WIND_TORNADO, c, c.add(0, 1, 0), 24, 1.5f * s);
        }
        return true;
    }

    /** Sylph's Breath: the spirit exhales a huge volume of air: a wide cone of tornado-strength wind that throws everything back. */
    public static boolean sylphsBreath(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), end = eye.add(look.scale(14 * s));
        b.castCircle(p, 1.4f);
        CanonSpells.cutSpells(p, new AABB(eye, end).inflate(4));
        int n = 0;
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 14 * s)) {
            if (n >= MAX_HITS) break;
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            if (to.lengthSqr() < 1e-4 || to.normalize().dot(look) < 0.55) continue;
            n++;
            b.hurt(i, p, t, mode, 12f);
            push(t, to.normalize(), 1.8, 0.35);
        }
        b.vfx(p, VfxShape.WIND_GALE, eye, end, 22, 2.0f * s);
        fx(p, VfxShape.FX_WIND_GUST, eye, end, 22, 1.6f * s);
        fx(p, VfxShape.WIND_RING, eye, end, 20, 1.5f * s);
        return true;
    }

    /** Spirit's Hushed Dance: a Mana Zone read of the mana around you: a few seconds of near-weightless dodging that turns aside spells (longer and quicker inside a Spirit Dive). */
    public static boolean hushedDance(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        boolean dive = diving(i, p);
        int ticks = dive ? 300 : 200;
        b.castCircle(p, 1.2f);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, dive ? 3 : 2));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 1));
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, ticks, 0));
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.WIND_ZEPHYR, p, p.position().add(0, 1, 0), 0, ticks, 0.8f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.SPIRIT_AURA, p, p.position().add(0, 1, 0), b.colorNow(), ticks, 0.8f);
        SpellRuntime.zone(p.serverLevel(), ticks, 5, age -> {
            if (gone(p)) return;
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(3.5))) {
                if (pr.getOwner() == p || (pr.getOwner() instanceof LivingEntity o && o.isAlliedTo(p))) continue;
                pr.setDeltaMovement(pr.position().subtract(p.position()).normalize().scale(1.2));      // read in advance: the spell is turned aside
                pr.hurtMarked = true;
            }
            if (age % 40 == 0) fx(p, VfxShape.WIND_RING, p.position().add(0, 0.2, 0), p.position().add(0, 1, 0), 16, 1.2f);
        });
        return true;
    }

    /** Liable Tempest (anime): a tempest that will not let go of an area for 5 s, pulling in and tearing at everything inside (effect inferred from the name). */
    public static boolean liableTempest(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 6f * GrimoireBook.size(i, p);
        Vec3 c = GrimoireBook.aim(p, 24);
        b.castCircle(p, 1.4f);
        fx(p, VfxShape.WIND_TORNADO, c, c.add(0, 1, 0), 100, r);
        fx(p, VfxShape.WIND_RING, c, c.add(0, 1, 0), 40, r);
        SpellRuntime.zone(p.serverLevel(), 100, 5, age -> {
            if (gone(p)) return;
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (n++ >= MAX_HITS) break;
                Vec3 to = t.position().subtract(c);
                Vec3 swirl = new Vec3(-to.z, 0, to.x).normalize().scale(0.35), pull = to.multiply(-0.1, 0, -0.1);
                t.setDeltaMovement(swirl.add(pull).add(0, 0.25, 0));
                t.hurtMarked = true;
                if (age % 20 == 0) b.hurt(i, p, t, mode, 5f);
            }
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(r, r, r))) if (pr.getOwner() != p) pr.discard();
        });
        return true;
    }

    /** Gale White Bow: a great bow of wind is drawn and the Sylph's breath looses a white arrow that pierces everything in 40 blocks. */
    public static boolean galeWhiteBow(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.WIND_SLASH, eye, eye.add(dir.scale(40)), 18, 1.0f * s);
        fx(p, VfxShape.WIND_GALE, eye, eye.add(dir.scale(40)), 18, 0.9f * s);
        SpellRuntime.bolt(p, eye, dir.scale(3.2), 1.0 * s, 13, true, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 16f);
            t.knockback(0.8, -bolt.vel.x, -bolt.vel.z);
        }, null);
        return true;
    }

    /** Swift White Hawk: a hawk of wind carries you up to 16 blocks in one dive, cutting through everyone on the way. */
    public static boolean swiftWhiteHawk(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position(), dir = p.getViewVector(1f).multiply(1, 0.3, 1).normalize(), dest = null;
        for (double d = 16; d >= 2; d -= 1) {
            Vec3 c = from.add(dir.scale(d));
            if (p.level().noCollision(p, p.getBoundingBox().move(c.subtract(from)))) { dest = c; break; }
        }
        if (dest == null) { GrimoireBook.fail(p, "No room for the hawk to fly."); return false; }
        b.castCircle(p, 0.8f);
        int n = 0;
        for (LivingEntity t : GrimoireBook.along(p, from.add(0, 1, 0), dest.add(0, 1, 0), 1.6)) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 9f);
            push(t, dir, 0.6, 0.4);
        }
        b.vfx(p, VfxShape.WIND_GALE, from.add(0, 1, 0), dest.add(0, 1, 0), 16, 1.3f);
        fx(p, VfxShape.WIND_SLASH, from.add(0, 1, 0), dest.add(0, 1, 0), 14, 1.1f);
        p.teleportTo(dest.x, dest.y, dest.z);
        p.fallDistance = 0;
        return true;
    }

    /** Heavenly Wind Ark: a ship of wind lifts you and your allies within 8 blocks: slow fall, speed, and a push forward. */
    public static boolean heavenlyWindArk(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 look = p.getViewVector(1f).multiply(1, 0, 1).normalize();
        b.castCircle(p, 1.6f);
        int n = 0;
        for (ServerPlayer o : p.serverLevel().players()) {
            if (n >= 8) break;
            if (o != p && !(o.isAlliedTo(p) && o.distanceToSqr(p) <= 64)) continue;
            n++;
            o.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 300, 0));
            o.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1));
            push(o, look, 1.0, 0.7);
            o.fallDistance = 0;
        }
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.WIND_TORNADO, p, p.position().add(0, 0.2, 0), 0, 60, 3.5f);
        fx(p, VfxShape.WIND_RING, p.position().add(0, 0.3, 0), p.position().add(0, 1, 0), 30, 4f);
        fx(p, VfxShape.WIND_ZEPHYR, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 60, 1.6f);
        return true;
    }

    /** Wind Blades Shower: a rain of wind blades on the spot you look at: four volleys over 1.6 s. */
    public static boolean windBladesShower(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 5f * GrimoireBook.size(i, p);
        Vec3 c = GrimoireBook.aim(p, 26);
        b.castCircle(p, 1f);
        fx(p, VfxShape.WIND_RING, c, c.add(0, 1, 0), 40, r);
        for (int k = 0; k < 4; k++) {
            final int step = k;
            SpellRuntime.later(p.serverLevel(), k * 8, () -> {
                if (gone(p)) return;
                var rnd = p.getRandom();
                Vec3 top = c.add(rnd.nextGaussian() * r * 0.4, 9, rnd.nextGaussian() * r * 0.4);
                fx(p, VfxShape.WIND_SLASH, top, c.add(rnd.nextGaussian() * r * 0.3, 0, rnd.nextGaussian() * r * 0.3), 12, 1.2f);
                int n = 0;
                for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                    if (n++ >= MAX_HITS) break;
                    b.hurt(i, p, t, mode, 5f + step);
                }
            });
        }
        return true;
    }

    /** Spirit of Boreas (Saint Stage): the north wind in a close-range Creation strike: a freezing cyclone around you that bites and chills everything within 6 blocks. */
    public static boolean spiritOfBoreas(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 6f * GrimoireBook.size(i, p);
        Vec3 c = p.position();
        b.castCircle(p, 1.8f);
        int n = 0;
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 22f);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 40, t.getTicksFrozen() + 80));
            Vec3 away = t.position().subtract(c).normalize();
            push(t, away, 1.2, 0.3);
        }
        b.vfx(p, VfxShape.FX_SPIRIT_NOVA, c, c, 24, 1.6f);
        fx(p, VfxShape.WIND_RING, c.add(0, 0.2, 0), c.add(0, 1, 0), 24, r);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.WIND_TORNADO, p, c.add(0, 1, 0), 0, 30, 2.2f);
        return true;
    }

    // ================================================================ Noelle: Water Magic
    /** Point-Blank Sea Dragon's Roar: the Roar fired from the hand at the target's chest: no flight, all of it lands at once. */
    public static boolean pointBlankRoar(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity target = GrimoireBook.target(p, 7);
        Vec3 c = target != null ? target.position() : GrimoireBook.aim(p, 4);
        Vec3 eye = p.getEyePosition();
        b.castCircle(p, 1.3f);
        int n = 0;
        for (LivingEntity t : GrimoireBook.around(p, c, 3.5)) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 24f);
            Vec3 away = t.position().subtract(p.position()).normalize();
            t.knockback(2.0, -away.x, -away.z);
        }
        b.vfx(p, VfxShape.WATER_DRAGON, eye, c.add(0, 1, 0), 8, 1.2f);
        fx(p, VfxShape.FX_WATER_CRASH, c, c, 22, 1.5f);
        fx(p, VfxShape.WATER_BURST, c, c.add(0, 1, 0), 22, 1.6f);
        return true;
    }

    // ================================================================ Fuegoleon: Flame Magic and Salamander
    /** Leo Parma: a giant paw of flame closes on one target within 18 blocks and holds it for 3 s. */
    public static boolean leoParma(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 18);
        if (t == null) { GrimoireBook.fail(p, "No one in the lion's reach."); return false; }
        Vec3 at = t.position();
        b.castCircle(p, 1.1f);
        b.hurt(i, p, t, mode, 8f);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 6));
        t.setRemainingFireTicks(80);
        b.vfx(p, VfxShape.FIRE_LION, p.getEyePosition(), t.getBoundingBox().getCenter(), 14, 0.8f);
        fx(p, VfxShape.FIRE_PILLAR, at, at.add(0, 1, 0), 40, 1.6f);
        fx(p, VfxShape.FIRE_BURST, at, at.add(0, 1, 0), 20, 1.2f);
        SpellRuntime.zone(p.serverLevel(), 60, 20, age -> {
            if (gone(p) || !t.isAlive()) return;
            b.hurt(i, p, t, mode, 4f);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 6));
        });
        return true;
    }

    /** Salamander's Breath: the fire spirit's breath, a huge flame spat in a wide cone (stronger inside a Spirit Dive). */
    public static boolean salamandersBreath(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), end = eye.add(look.scale(14 * s));
        float dmg = diving(i, p) ? 20f : 16f;
        b.castCircle(p, 1.3f);
        int n = 0;
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 14 * s)) {
            if (n >= MAX_HITS) break;
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            if (to.lengthSqr() < 1e-4 || to.normalize().dot(look) < 0.6) continue;
            n++;
            b.hurt(i, p, t, mode, dmg);
            t.setRemainingFireTicks(100);
        }
        b.vfx(p, VfxShape.FIRE_SPIRAL, eye, end, 20, 1.8f * s);
        b.vfx(p, VfxShape.FLAME_TRAIL, eye, end, 20, 1.2f * s);
        fx(p, VfxShape.FIRE_BURST, end, end.add(0, 1, 0), 20, 1.6f * s);
        return true;
    }

    // ================================================================ Yami: Dark Magic
    private static final ResourceLocation DEEP_BLADE = ResourceLocation.fromNamespaceAndPath("nusmp", "deep_black_blade_reach");

    /** Dark Cloaked Deep Black Blade: the Black Blade grown deeper: 4 blocks of extra reach and Strength II for 20 s (effect inferred from the name). */
    public static boolean deepBlackBlade(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        var reach = p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (reach != null) {
            reach.removeModifier(DEEP_BLADE);
            reach.addTransientModifier(new AttributeModifier(DEEP_BLADE, 4.0, AttributeModifier.Operation.ADD_VALUE));
        }
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1));
        SpellRuntime.later(p.serverLevel(), 400, () -> { var r = p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE); if (r != null) r.removeModifier(DEEP_BLADE); });
        b.castCircle(p, 0.8f);
        Vec3 eye = p.getEyePosition();
        b.vfx(p, VfxShape.DARK_SLASH_BLADE, eye.add(0, -0.4, 0), eye.add(p.getViewVector(1f).scale(5)), 40, 1.2f);
        fx(p, VfxShape.DARK_SLASH_AVIDYA, eye, eye.add(p.getViewVector(1f).scale(4)), 16, 0.8f);
        return true;
    }

    /** Black Crescent Moon: a Mana Zone slash in the shape of a crescent: a wide arc that cuts 22 blocks (half as much again while Black Moon is up). */
    public static boolean blackCrescentMoon(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        boolean moon = CanonSpells.blackMoonActive(p);
        CanonSpells.longCut(b, i, p, mode, 22, 3.0, moon ? 36 : 24, VfxShape.DARK_SLASH_AVIDYA, false, false);
        Vec3 eye = p.getEyePosition(), end = eye.add(p.getViewVector(1f).scale(22));
        fx(p, VfxShape.DARK_SLASH_DIMENSION, eye, end, 20, 2.2f);
        fx(p, VfxShape.DARK_BLACK_MOON, end, end.add(0, 1, 0), 30, 2.4f);
        return true;
    }
}
