package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the casts of the pages that {@link CharacterPagesBulls} appends for the Black Bulls (docs/magic/character_spells_bulls.md): the spells these members used in the anime that the shared
 * element books had no page for. Every cast is a plain {@link BookPage.Cast}: the server decides everything (targets, damage, effects) with the same helpers every other page uses, and the
 * picture is a LAYER of existing {@link VfxShape}s (3 to 5 per cast: core, trail, impact, ground or air accent) sent to nearby players only through {@link VfxSpawn}.
 *
 * <p>Worst case per cast (the doc repeats it): at most 5 effect packets, one zone per cast of at most 12 s that runs every 5 to 20 ticks, every hit loop capped at {@link #MAX_HITS} (8) live
 * targets, no entity is spawned, no block is placed, no chunk is loaded (a teleport tests {@code hasChunkAt} first). Cooldowns are 12 to 120 s. Effects the sources did not describe are
 * marked "inferred" in the doc: the name and intent are the series', the numbers are this mod's. The anime-faithful casting gestures come from the page's release clip (CastAnim) and, for
 * Magna, from the existing Soul Bat casting (firecast), which wraps every family page of the Fire book.
 */
public final class CharacterArtsBulls {
    private CharacterArtsBulls() {}

    private static final int MAX_HITS = 8;

    private static boolean gone(ServerPlayer p) { return p.isRemoved() || !p.isAlive(); }

    private static void fx(ServerPlayer p, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) {
        VfxSpawn.send(p.serverLevel(), shape, from, to, 0, ticks, power);
    }

    private static void push(LivingEntity t, Vec3 dir, double h, double up) {
        t.setDeltaMovement(dir.x * h, up, dir.z * h);
        t.hurtMarked = true;
    }

    /** The caster and his allies within r of c (at most 8), one box query. */
    private static List<LivingEntity> friends(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r), e -> e.isAlive() && !e.isSpectator() && (e == p || e.isAlliedTo(p)) && e.distanceToSqr(c) <= r * r)) {
            if (out.size() >= MAX_HITS) break;
            out.add(e);
        }
        return out;
    }

    // ================================================================ Charmy Pappitson: Cotton Magic
    /** Cotton Magic: Fluffy Sheep Cushion (anime): a huge sheep appears and its wool catches everyone nearby; allies float down softly and are padded, foes are bounced off without a scratch. */
    public static boolean fluffySheepCushion(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 6f * GrimoireBook.size(i, p);
        Vec3 c = p.position();
        b.castCircle(p, 1.2f);
        for (LivingEntity e : friends(p, c, r)) {
            e.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
            e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0));
            e.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0));
            e.fallDistance = 0;
        }
        int n = 0;
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            if (n++ >= MAX_HITS) break;
            Vec3 away = t.position().subtract(c).multiply(1, 0, 1);
            push(t, away.lengthSqr() < 1e-4 ? new Vec3(0, 0, 0) : away.normalize(), 0.7, 0.5);
        }
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.COTTON_SHEEP, p, c.add(0, 0.2, 0), 0, 70, 1.8f * (r / 6f));
        fx(p, VfxShape.SPIRIT_AURA, c.add(0, 0.5, 0), c.add(0, 1.5, 0), 40, 1.4f);
        b.impact(p, c.add(0, 0.2, 0), 1.2f);
        return true;
    }

    // ================================================================ Luck Voltia: Lightning Magic
    /** Thunderclap Crumbling Orb (anime): lightning gathered in the claws is thrown as a ball that bursts on the first thing it meets. */
    public static boolean thunderclapOrb(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.9f);
        b.vfx(p, VfxShape.LIGHTNINGFX_CAST, eye.add(0, -0.3, 0), eye.add(dir.scale(22)), 14, 1.0f * s);
        fx(p, VfxShape.FX_LIGHTNING_ARC, eye.add(dir.scale(0.8)), eye.add(dir.scale(6)), 8, 0.8f * s);
        SpellRuntime.bolt(p, eye, dir.scale(2.2), 0.9 * s, 12, false, null, (bolt, t) -> { }, (bolt, at) -> {
            if (gone(p)) return;
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, at, 3.2 * s)) {
                if (n++ >= MAX_HITS) break;
                b.hurt(i, p, t, mode, 13f);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            }
            fx(p, VfxShape.LIGHTNINGFX_BURST, at, at.add(0, 1, 0), 18, 1.3f * s);
        });
        return true;
    }

    /** Thunder God's Gloves (anime): lightning gauntlets on both hands for 20 s: haste, strength, and a shock that arcs to the nearest foe within reach once a second. */
    public static boolean thunderGodsGloves(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1f);
        p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 400, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 0));
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.LIGHTNINGFX_ZONE, p, p.position().add(0, 1, 0), 0, 400, 0.7f);
        fx(p, VfxShape.FX_LIGHTNING_ARC, p.position().add(0, 1.4, 0), p.position().add(0, 2.4, 0), 14, 1.0f);
        SpellRuntime.zone(p.serverLevel(), 400, 20, age -> {
            if (gone(p)) return;
            LivingEntity near = null;
            double best = 9;
            for (LivingEntity t : GrimoireBook.around(p, p.position(), 3.0)) {
                double d = t.distanceToSqr(p);
                if (d < best) { best = d; near = t; }
            }
            if (near == null) return;
            b.hurt(i, p, near, mode, 3f);
            fx(p, VfxShape.FX_LIGHTNING_ARC, p.position().add(0, 1.3, 0), near.getBoundingBox().getCenter(), 6, 0.8f);
        });
        return true;
    }

    /** Keraunos (anime): the true lightning of the Heart Kingdom training. The caster is fired as a bolt of lightning up to 20 blocks, striking everyone on the line. Needs room. */
    public static boolean keraunos(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position(), dir = p.getViewVector(1f).multiply(1, 0.25, 1).normalize(), dest = null;
        float s = GrimoireBook.size(i, p);
        for (double d = 20 * s; d >= 3; d -= 1) {
            Vec3 c = from.add(dir.scale(d));
            if (p.level().noCollision(p, p.getBoundingBox().move(c.subtract(from)))) { dest = c; break; }
        }
        if (dest == null) { GrimoireBook.fail(p, "No room for the lightning to run."); return false; }
        b.castCircle(p, 1.2f);
        int n = 0;
        for (LivingEntity t : GrimoireBook.along(p, from.add(0, 1, 0), dest.add(0, 1, 0), 1.8)) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 15f);
            push(t, dir, 0.5, 0.35);
        }
        b.vfx(p, VfxShape.LIGHTNINGFX_CAST, from.add(0, 1, 0), dest.add(0, 1, 0), 14, 1.6f * s);
        fx(p, VfxShape.LIGHTNING_SPEAR, from.add(0, 1, 0), dest.add(0, 1, 0), 12, 1.2f * s);
        fx(p, VfxShape.LIGHTNINGFX_BURST, from, from.add(0, 1, 0), 16, 1.2f);
        fx(p, VfxShape.LIGHTNINGFX_BURST, dest, dest.add(0, 1, 0), 18, 1.6f * s);
        p.teleportTo(dest.x, dest.y, dest.z);
        p.fallDistance = 0;
        return true;
    }

    // ================================================================ Magna Swing: Flame Magic (the Soul Bat swing wraps these casts: firecast.MagnaSwing)
    /** Exploding Buckshot (anime): a single throw that splits into a spray of small fireballs: a short cone that burns everything in it. */
    public static boolean explodingBuckshot(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), end = eye.add(look.scale(11 * s));
        b.castCircle(p, 1f);
        CanonSpells.cutSpells(p, new AABB(eye, end).inflate(2));
        int n = 0;
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 11 * s)) {
            if (n >= MAX_HITS) break;
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            if (to.lengthSqr() < 1e-4 || to.normalize().dot(look) < 0.72) continue;
            n++;
            b.hurt(i, p, t, mode, 9f);
            t.setRemainingFireTicks(60);
        }
        b.vfx(p, VfxShape.FLAMEFX_CAST, eye, end, 16, 1.2f * s);
        for (int k = -1; k <= 1; k += 2) {
            Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize().scale(2.2 * k * s);
            fx(p, VfxShape.FLAME_TRAIL, eye, end.add(side), 14, 0.8f * s);
        }
        fx(p, VfxShape.FLAMEFX_BURST, end, end.add(0, 1, 0), 18, 1.2f * s);
        return true;
    }

    /** Grand Slam (anime): the bat of flame is swung and a trio of fireballs is knocked out in a fan; each one bursts on the first foe it meets. */
    public static boolean grandSlam(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), side = look.cross(new Vec3(0, 1, 0)).normalize();
        b.castCircle(p, 1.1f);
        b.vfx(p, VfxShape.FLAMEFX_CAST, eye, eye.add(look.scale(20)), 14, 1.2f * s);
        for (int k = -1; k <= 1; k++) {
            Vec3 dir = look.add(side.scale(0.12 * k)).normalize();
            SpellRuntime.bolt(p, eye, dir.scale(2.0), 0.8 * s, 14, false, null, (bolt, t) -> { }, (bolt, at) -> {
                if (gone(p)) return;
                int n = 0;
                for (LivingEntity t : GrimoireBook.around(p, at, 2.6 * s)) {
                    if (n++ >= MAX_HITS) break;
                    b.hurt(i, p, t, mode, 8f);
                    t.setRemainingFireTicks(60);
                }
            });
        }
        fx(p, VfxShape.FLAME_TRAIL, eye, eye.add(look.scale(18)), 14, 1.0f * s);
        fx(p, VfxShape.FLAMEFX_BURST, eye.add(look.scale(18)), eye.add(look.scale(18)).add(0, 1, 0), 18, 1.4f * s);
        return true;
    }

    /** Soul Chain Tag Deathmatch (anime, Magna's hidden spell): a fireball on the foe and a chain of souls binds the two together for 8 s: the foe cannot get farther than 7 blocks away, hits weaker, while Magna hits harder. */
    public static boolean soulChainDeathmatch(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null) { GrimoireBook.fail(p, "Nobody in reach to chain."); return false; }
        b.castCircle(p, 1.4f);
        b.hurt(i, p, t, mode, 10f);
        t.setRemainingFireTicks(60);
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 1));
        Vec3 here = p.getEyePosition(), there = t.getBoundingBox().getCenter();
        b.vfx(p, VfxShape.FLAMEFX_CAST, here, there, 12, 1.2f);
        fx(p, VfxShape.CHAIN_FX1, here, there, 160, 1.0f);
        fx(p, VfxShape.FLAMEFX_ZONE, t.position(), t.position().add(0, 1, 0), 40, 1.3f);
        fx(p, VfxShape.SPIRIT_AURA, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 40, 1.0f);
        SpellRuntime.zone(p.serverLevel(), 160, 5, age -> {
            if (gone(p) || !t.isAlive()) return;
            Vec3 d = t.position().subtract(p.position());
            double dist = d.length();
            if (dist > 7.0 && dist < 64) {
                Vec3 back = d.normalize().scale(-0.45);
                t.setDeltaMovement(back.x, t.getDeltaMovement().y, back.z);
                t.hurtMarked = true;
            }
        });
        return true;
    }

    // ================================================================ Gauche Adlai: Mirror Magic
    /** Double Reflect Refrain (anime): after a Real Double, twice the mirrors: the light bounces down a lane of 18 blocks twice and cuts through everything between the mirrors. */
    public static boolean doubleReflectRefrain(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 a = p.getEyePosition().add(0, -0.3, 0), dir = p.getViewVector(1f), end = a.add(dir.scale(18 * s));
        b.castCircle(p, 1.3f);
        int n = 0;
        for (LivingEntity t : GrimoireBook.along(p, a, end, 2.4)) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 10f);
            b.hurt(i, p, t, mode, 6f);
        }
        Vec3 side = dir.cross(new Vec3(0, 1, 0)).normalize().scale(2.0);
        b.vfx(p, VfxShape.MIRROR_ARRAY, a.add(dir.scale(4)), end, 22, 1.5f * s);
        fx(p, VfxShape.MIRROR_RAY, a, end.add(side), 14, 1.2f * s);
        fx(p, VfxShape.MIRROR_RAY, a, end.subtract(side), 14, 1.2f * s);
        fx(p, VfxShape.MIRROR_SHATTER, end, end.add(0, 1, 0), 16, 1.2f * s);
        return true;
    }

    /** Mirrors Brigade (anime): a wall of large square mirrors around the caster for 8 s; spells that touch it are turned back on their casters. */
    public static boolean mirrorsBrigade(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 5f * GrimoireBook.size(i, p);
        b.castCircle(p, 1.4f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MIRROR_FRAME, p, p.position().add(0, 1, 0), 0, 160, 1.4f * (r / 5f));
        fx(p, VfxShape.MIRROR_ARRAY, p.position().add(0, 0.3, 0), p.position().add(0, 1.5, 0), 40, r);
        SpellRuntime.zone(p.serverLevel(), 160, 5, age -> {
            if (gone(p)) return;
            int n = 0;
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(r))) {
                if (n >= 6) break;
                if (pr.getOwner() == p || (pr.getOwner() instanceof LivingEntity o && o.isAlliedTo(p))) continue;
                n++;
                pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.0));
                pr.setOwner(p);
                pr.hurtMarked = true;
            }
            if (n > 0 && age % 20 == 0) fx(p, VfxShape.MIRROR_SHATTER, p.position().add(0, 1, 0), p.position().add(0, 2, 0), 12, 1.0f);
        });
        return true;
    }

    // ================================================================ Gordon Agrippa: Poison Magic
    /** Verbotenes Obst / Forbidden Fruit (anime): a big mass of liquid poison between the hands is thrown; it bursts into a poison cloud where it lands. */
    public static boolean verbotenesObst(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.POISON_BREATH, eye.add(0, -0.3, 0), eye.add(dir.scale(20)), 16, 1.0f * s);
        SpellRuntime.bolt(p, eye, dir.scale(1.5), 1.1 * s, 18, false, null, (bolt, t) -> { }, (bolt, at) -> {
            if (gone(p)) return;
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, at, 3.6 * s)) {
                if (n++ >= MAX_HITS) break;
                b.hurt(i, p, t, mode, 14f);
                t.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
            }
            fx(p, VfxShape.POISON_CURTAIN, at, at.add(0, 1, 0), 30, 1.4f * s);
            fx(p, VfxShape.POISON_BREATH, at, at.add(0, 2, 0), 22, 1.2f * s);
        });
        return true;
    }

    /** Poison Cloud's Abode (anime): a cloud of poison spreads where you look and lingers 8 s: 8 blocks across, a little damage and poison to everything that stays inside. */
    public static boolean poisonCloudsAbode(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 6f * GrimoireBook.size(i, p);
        Vec3 c = GrimoireBook.aim(p, 22);
        b.castCircle(p, 1.3f);
        fx(p, VfxShape.POISON_CURTAIN, c, c.add(0, 1, 0), 160, r);
        fx(p, VfxShape.POISON_BREATH, c, c.add(0, 2, 0), 60, r * 0.7f);
        fx(p, VfxShape.CURSE_FX1, c, c.add(0, 1, 0), 40, r * 0.5f);
        SpellRuntime.zone(p.serverLevel(), 160, 20, age -> {
            if (gone(p)) return;
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (n++ >= MAX_HITS) break;
                b.hurt(i, p, t, mode, 3f);
                t.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            }
        });
        return true;
    }

    // ================================================================ Zora Ideale: Ash Magic (Trap Magic)
    /** Ash Snare Trap (anime, a Trap Magic): an ash circle drawn at the aim point holds the first foe that steps on it for 4 s. It waits 8 s. */
    public static boolean ashSnareTrap(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 2.6f * GrimoireBook.size(i, p);
        Vec3 c = GrimoireBook.aim(p, 20);
        b.castCircle(p, 0.9f);
        fx(p, VfxShape.ASH_FORMATION, c, c.add(0, 0.2, 0), 160, r);
        fx(p, VfxShape.ASH_FX2, c, c.add(0, 1, 0), 30, r * 0.6f);
        boolean[] sprung = {false};
        SpellRuntime.zone(p.serverLevel(), 160, 5, age -> {
            if (sprung[0] || gone(p)) return;
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (n++ >= MAX_HITS) break;
                sprung[0] = true;
                b.hurt(i, p, t, mode, 6f);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 6));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            }
            if (sprung[0]) fx(p, VfxShape.ASH_FX3, c, c.add(0, 1, 0), 24, r);
        });
        return true;
    }

    /** Counter Trap Magic (anime, Zora's best trap): for 10 s a circle under your feet swallows the spells that enter it; each one comes back at the nearest foe, doubled. */
    public static boolean counterTrapMagic(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 4.5f * GrimoireBook.size(i, p);
        b.castCircle(p, 1.5f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.ASH_FORMATION, p, p.position().add(0, 0.1, 0), 0, 200, r * 0.45f);
        fx(p, VfxShape.ASH_FX2, p.position(), p.position().add(0, 1, 0), 40, r * 0.5f);
        int[] left = {3};
        SpellRuntime.zone(p.serverLevel(), 200, 5, age -> {
            if (gone(p) || left[0] <= 0) return;
            Vec3 c = p.position();
            int taken = 0;
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(r))) {
                if (taken >= left[0]) break;
                if (pr.getOwner() == p || (pr.getOwner() instanceof LivingEntity o && o.isAlliedTo(p))) continue;
                pr.discard();
                taken++;
            }
            if (taken < left[0]) taken += SpellRuntime.dissolveBolts(p.serverLevel(), c, r, p, left[0] - taken);
            if (taken <= 0) return;
            left[0] -= taken;
            LivingEntity foe = null;
            double best = 18 * 18;
            for (LivingEntity t : GrimoireBook.around(p, c, 18)) {
                double d = t.distanceToSqr(p);
                if (d < best) { best = d; foe = t; }
            }
            if (foe != null) {
                b.hurt(i, p, foe, mode, 14f * taken);
                fx(p, VfxShape.ASH_FX3, foe.position(), foe.position().add(0, 1, 0), 22, 1.2f);
                fx(p, VfxShape.FX_SPIRIT_NOVA, c.add(0, 1, 0), foe.getBoundingBox().getCenter(), 14, 0.8f);
            }
        });
        return true;
    }

    // ================================================================ Grey: Transformation Magic
    /** Transformation Magic: Borrowed Form (anime): Grey takes the shape of something else, so nobody hostile can pick her out: 15 s of invisibility and speed, and the mobs hunting her lose her scent. */
    public static boolean transformationGuise(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.1f);
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 300, 0));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 0));
        int n = 0;
        for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16), m -> m.getTarget() == p)) {
            if (n++ >= MAX_HITS) break;
            m.setTarget(null);
        }
        Vec3 c = p.position().add(0, 1, 0);
        fx(p, VfxShape.ALCHEMY_CIRCLE, p.position(), c, 30, 1.4f);
        fx(p, VfxShape.TRANSMUTATION_FX1, c, c.add(0, 1, 0), 26, 1.3f);
        b.impact(p, c, 1.0f);
        return true;
    }

    // ================================================================ Henry Legolant: Recombination Magic
    /** Mana Rocket Punch (anime): the fist of the Black Bull hideout is torn off and flies on mana: a big piercing bolt that hits hard and throws everything aside. */
    public static boolean manaRocketPunch(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.RECOMBINE_CONSTRUCT, eye, eye.add(dir.scale(24)), 16, 1.3f * s);
        fx(p, VfxShape.RECOMBINATION_FX1, eye, eye.add(dir.scale(24)), 16, 1.0f * s);
        SpellRuntime.bolt(p, eye, dir.scale(2.2), 1.5 * s, 14, true, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 14f);
            t.knockback(1.4, -bolt.vel.x, -bolt.vel.z);
        }, (bolt, at) -> fx(p, VfxShape.RECOMBINATION_FX1, at, at.add(0, 1, 0), 18, 1.4f * s));
        return true;
    }

    /** The Mini Bull (anime): a small bull cut from the hideout charges ahead 12 blocks and tramples what it meets. */
    public static boolean miniBull(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 a = p.position().add(0, 0.6, 0), dir = p.getViewVector(1f).multiply(1, 0, 1).normalize(), end = a.add(dir.scale(12 * s));
        b.castCircle(p, 1f);
        int n = 0;
        for (LivingEntity t : GrimoireBook.along(p, a, end, 1.6)) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 10f);
            push(t, dir, 1.3, 0.4);
        }
        b.vfx(p, VfxShape.RECOMBINE_CONSTRUCT, a, end, 20, 0.9f * s);
        fx(p, VfxShape.RECOMBINATION_FX1, a, end, 18, 0.8f * s);
        fx(p, VfxShape.EARTH_SPIKES, end, end.add(0, 1, 0), 16, 0.9f * s);
        return true;
    }

    // ================================================================ Finral Roulacase: Spatial Magic
    /** Fallen Angel Wingbeat (anime, learned after his training): a sphere of space strikes the foe and throws it to another place, 12 to 20 blocks away. */
    public static boolean fallenAngelWingbeat(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 18);
        if (t == null || t.isAlliedTo(p)) { GrimoireBook.fail(p, "No foe in reach for the wingbeat."); return false; }
        Vec3 start = t.position();
        Vec3 dest = null;
        var rnd = p.getRandom();
        for (int k = 0; k < 8 && dest == null; k++) {
            double ang = rnd.nextDouble() * Math.PI * 2, dist = 12 + rnd.nextDouble() * 8;
            Vec3 c = new Vec3(start.x + Math.cos(ang) * dist, start.y, start.z + Math.sin(ang) * dist);
            BlockPos bp = BlockPos.containing(c);
            if (!p.serverLevel().hasChunkAt(bp)) continue;
            if (p.level().noCollision(t, t.getBoundingBox().move(c.subtract(start))) && !p.level().noCollision(t, t.getBoundingBox().move(c.subtract(start)).move(0, -3, 0))) dest = c;
        }
        if (dest == null) { GrimoireBook.fail(p, "The wingbeat found no place to send it."); return false; }
        b.castCircle(p, 1.1f);
        Vec3 hand = p.getEyePosition();
        b.vfx(p, VfxShape.SPACE_PORTAL, hand, t.getBoundingBox().getCenter(), 14, 1.0f);
        fx(p, VfxShape.SPACE_RINGGATE, start, start.add(0, 1, 0), 24, 1.4f);
        fx(p, VfxShape.SPACE_CRACK, start.add(0, 1, 0), dest.add(0, 1, 0), 18, 1.0f);
        b.hurt(i, p, t, mode, 6f);
        t.randomTeleport(dest.x, dest.y, dest.z, true);
        fx(p, VfxShape.SPACE_RINGGATE, dest, dest.add(0, 1, 0), 24, 1.4f);
        return true;
    }

    // ================================================================ Nacht Faust: Shadow Magic
    /** Dark Hunting Ground (anime, the Canis form): a great shadow wraps everyone within 8 blocks, blinds them and strikes from all four sides for 6 s. */
    public static boolean darkHuntingGround(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 8f * GrimoireBook.size(i, p);
        Vec3 c = p.position();
        b.castCircle(p, 1.4f);
        fx(p, VfxShape.SHADOW_POOL, c, c.add(0, 0.2, 0), 120, r);
        fx(p, VfxShape.DARKFX_ZONE, c, c.add(0, 1, 0), 120, r * 0.8f);
        fx(p, VfxShape.SHADOW_UNITE, c.add(0, 1, 0), c.add(0, 3, 0), 40, 1.6f);
        SpellRuntime.zone(p.serverLevel(), 120, 20, age -> {
            if (gone(p)) return;
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (n++ >= MAX_HITS) break;
                b.hurt(i, p, t, mode, 5f);
                t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            }
        });
        return true;
    }

    /** Atonement Monument (anime): the foes within 6 blocks are drawn down into the shadows and buried in an empty world: they are held 4 s and take the full weight of it. */
    public static boolean atonementMonument(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float r = 6f * GrimoireBook.size(i, p);
        Vec3 c = p.position();
        b.castCircle(p, 1.6f);
        int n = 0;
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 22f);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 6));
            t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
            fx(p, VfxShape.SHADOW_POOL, t.position(), t.position().add(0, 0.5, 0), 60, 1.1f);
            if (n >= 4) break;
        }
        fx(p, VfxShape.DARKFX_BURST, c, c.add(0, 1, 0), 30, r);
        fx(p, VfxShape.SHADOW_UNITE, c.add(0, 1, 0), c.add(0, 3, 0), 50, 1.8f);
        return true;
    }

    // ================================================================ Secre Swallowtail: Sealing Magic
    /** Seal Magic: Inverse Release (anime): the open hand forces what is shut to open: allies within 8 blocks (and you) are freed of binds, curses on the mind and a sealed grimoire. */
    public static boolean inverseRelease(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.3f);
        Vec3 c = p.position();
        int shown = 0;
        for (LivingEntity e : friends(p, c, 8)) {
            e.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            e.removeEffect(MobEffects.WEAKNESS);
            e.removeEffect(MobEffects.BLINDNESS);
            e.removeEffect(MobEffects.DARKNESS);
            e.removeEffect(MobEffects.DIG_SLOWDOWN);
            e.removeEffect(MobEffects.LEVITATION);
            e.removeEffect(MobEffects.CONFUSION);
            e.getPersistentData().putLong("nusmp_sealed_until", 0L);
            if (shown++ < 3) fx(p, VfxShape.SEAL_ORBIT, e.position(), e.position().add(0, 1, 0), 26, 1.0f);
        }
        b.vfx(p, VfxShape.SEAL_BRANCH, p.getEyePosition(), c.add(0, 1, 0), 20, 1.4f);
        fx(p, VfxShape.SEAL_BASIC, c, c.add(0, 1, 0), 30, 1.6f);
        return true;
    }
}
