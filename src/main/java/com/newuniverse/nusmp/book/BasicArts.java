package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the casts of the generic pages that {@link BasicPages} appends to the basic grimoires (docs/magic/basic_grimoires.md): spells that ordinary mages and minor named characters used in the anime
 * (Fanzell's Slicing Wind Emperor family, Rhya's Aqua Curtain, Heath's Heavenly Ice Fang, Vetto's Bear Claw ...), none of them a spell of the characters who have their own spell set. Each cast is a
 * plain {@link BookPage.Cast}: the server decides everything with the helpers every other page uses, and the picture is a LAYER of two or three existing {@link VfxShape}s, one layer less than the
 * character pages (the standard tier: decent, but clearly less than a character's signature).
 *
 * <p>Worst case per cast (the doc repeats this): at most 6 effect packets (sent to players near the caster only, through {@link VfxSpawn}); at most 4 bolts and 4 delayed starts; one zone of at most
 * 4 seconds that runs every 4 to 5 ticks; every hit loop is capped at {@link #MAX_HITS} targets and every discard loop at {@link #MAX_DISCARD} projectiles; no entity is spawned, no block is
 * placed, no chunk is loaded, nothing is scanned beyond the box of the spell. The cooldowns are 10 to 25 seconds.
 */
public final class BasicArts {
    private BasicArts() {}

    static final int MAX_HITS = 8;
    static final int MAX_DISCARD = 6;

    private static boolean gone(ServerPlayer p) { return p.isRemoved() || !p.isAlive(); }

    private static void fx(ServerPlayer p, VfxShape shape, Vec3 from, Vec3 to, int ticks, float power) {
        VfxSpawn.send(p.serverLevel(), shape, from, to, 0, ticks, power);
    }

    private static Vec3 flat(ServerPlayer p) {
        Vec3 f = p.getViewVector(1f).multiply(1, 0, 1);
        return f.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : f.normalize();
    }

    private static int discardForeign(ServerPlayer p, Vec3 centre, double r) {
        int n = 0;
        for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(r))) {
            if (n >= MAX_DISCARD) break;
            if (pr.getOwner() == p || (pr.getOwner() instanceof LivingEntity o && o.isAlliedTo(p)) || pr.position().distanceToSqr(centre) > r * r) continue;
            pr.discard();
            n++;
        }
        return n;
    }

    // ================================================================ Fire Magic
    /** Shining Flame Cross (Theresa Rapual): a cluster of small blasts that together form a large cross where you aim. */
    public static boolean shiningFlameCross(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = GrimoireBook.aim(p, 22), fwd = flat(p), side = new Vec3(-fwd.z, 0, fwd.x);
        double arm = 3.2 * s;
        Vec3 a1 = c.subtract(fwd.scale(arm)), a2 = c.add(fwd.scale(arm)), b1 = c.subtract(side.scale(arm)), b2 = c.add(side.scale(arm));
        b.castCircle(p, 1f);
        Set<LivingEntity> hit = new LinkedHashSet<>();
        hit.addAll(GrimoireBook.along(p, a1, a2, 1.0));
        hit.addAll(GrimoireBook.along(p, b1, b2, 1.0));
        int n = 0;
        for (LivingEntity t : hit) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 8f);
            t.igniteForSeconds(3);
        }
        fx(p, VfxShape.FIRE_BURST, c, c, 18, 1.4f * s);
        fx(p, VfxShape.FLAME_TRAIL, a1, a2, 16, 0.9f);
        fx(p, VfxShape.FLAME_TRAIL, b1, b2, 16, 0.9f);
        return true;
    }

    /** Combo Eruption (Dmitri Brint): the arms spread wide and a series of fireballs is launched at the target; each swells and explodes on contact. */
    public static boolean comboEruption(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f), side = new Vec3(-dir.z, 0, dir.x).normalize();
        b.castCircle(p, 0.9f);
        fx(p, VfxShape.FLAME_TRAIL, eye, eye.add(dir.scale(20)), 14, 0.9f * s);
        for (int k = 0; k < 4; k++) {
            Vec3 d = dir.add(side.scale((k - 1.5) * 0.1)).normalize();
            SpellRuntime.later(p.serverLevel(), k * 3, () -> {
                if (gone(p)) return;
                SpellRuntime.bolt(p, p.getEyePosition(), d.scale(1.4), 0.5 * s, 22, false, null, (bolt, t) -> {}, (bolt, at) -> {
                    int n = 0;
                    for (LivingEntity t : GrimoireBook.around(p, at, 2.4 * s)) {
                        if (n++ >= MAX_HITS) break;
                        b.hurt(i, p, t, mode, 6f);
                        t.igniteForSeconds(3);
                    }
                    fx(p, VfxShape.FIRE_BURST, at, at, 14, 0.9f * s);
                });
            });
        }
        return true;
    }

    // ================================================================ Water Magic
    /** Aqua Curtain (Rhya): a large volume of water covers you: it puts out the fire on your body and turns aside what flies at you for four seconds. */
    public static boolean aquaCurtain(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        b.castCircle(p, 1f);
        p.clearFire();
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 300, 0));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 1));
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.WATER_CRADLE, p, p.position().add(0, 1, 0), b.colorNow(), 80, 1.1f * s);
        fx(p, VfxShape.WATER_RING, p.position().add(0, 0.2, 0), p.position().add(0, 1, 0), 20, 1.2f);
        SpellRuntime.zone(p.serverLevel(), 80, 5, age -> {
            if (gone(p)) return;
            p.clearFire();
            discardForeign(p, p.position(), 3.5);
        });
        return true;
    }

    /** Cascade Destroyer (Selena Silva): several balls of water are launched at the opponents, homing on the nearest ones. */
    public static boolean cascadeDestroyer(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f), side = new Vec3(-dir.z, 0, dir.x).normalize();
        List<LivingEntity> targets = GrimoireBook.around(p, p.position(), 18);
        b.castCircle(p, 0.9f);
        fx(p, VfxShape.WATER_JAVELIN, eye, eye.add(dir.scale(18)), 14, 0.6f * s);
        for (int k = 0; k < 4; k++) {
            Vec3 d = dir.add(side.scale((k - 1.5) * 0.12)).normalize();
            LivingEntity home = targets.isEmpty() ? null : targets.get(k % targets.size());
            SpellRuntime.later(p.serverLevel(), k * 2, () -> {
                if (gone(p)) return;
                SpellRuntime.bolt(p, p.getEyePosition(), d.scale(1.5), 0.6 * s, 26, false, home, (bolt, t) -> b.hurt(i, p, t, mode, 6f),
                        (bolt, at) -> fx(p, VfxShape.WATER_SPLASH, at, at, 14, 0.6f * s));
            });
        }
        return true;
    }

    /** Holy Water Assassination Bullet (Solid Silva): one bullet of holy water that goes through everything in a line; the undead take much more. */
    public static boolean holyWaterBullet(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f);
        int[] hits = {0};
        b.castCircle(p, 1f);
        fx(p, VfxShape.WATER_JAVELIN, eye, eye.add(dir.scale(34)), 16, 0.7f * s);
        SpellRuntime.bolt(p, eye, dir.scale(2.4), 0.6 * s, 15, true, null, (bolt, t) -> {
            if (hits[0]++ >= MAX_HITS) return;
            b.hurt(i, p, t, mode, t.isInvertedHealAndHarm() ? 22f : 13f);
        }, (bolt, at) -> fx(p, VfxShape.WATER_BURST, at, at, 16, 0.8f * s));
        return true;
    }

    // ================================================================ Wind Magic
    /** Slicing Wind Emperor: Wintry Wind (Fanzell Kruger): several swords of wind are made and sent at the opponents. */
    public static boolean wintryWind(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f), side = new Vec3(-dir.z, 0, dir.x).normalize();
        List<LivingEntity> targets = GrimoireBook.around(p, p.position(), 16);
        b.castCircle(p, 1f);
        for (int k = 0; k < 4; k++) {
            Vec3 d = dir.add(side.scale((k - 1.5) * 0.14)).normalize();
            LivingEntity home = targets.isEmpty() ? null : targets.get(k % targets.size());
            fx(p, VfxShape.WIND_SLASH, eye, home != null ? home.getBoundingBox().getCenter() : eye.add(d.scale(20)), 14, 0.7f * s);
            SpellRuntime.bolt(p, eye, d.scale(1.7), 0.5 * s, 20, false, home, (bolt, t) -> {
                b.hurt(i, p, t, mode, 6f);
                t.knockback(0.3, -bolt.vel.x, -bolt.vel.z);
            }, null);
        }
        return true;
    }

    private static final BookPage.Cast GALE_DASH = ElementBook.dash(9f, 9, false, VfxShape.WIND_GALE, ElementBook.knock(0.5));

    /** Slicing Wind Emperor: Gale (Fanzell Kruger): a plunge at the opponent with great force, surrounded by wind. */
    public static boolean galeDash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position();
        if (!GALE_DASH.cast(b, i, p, mode)) return false;
        fx(p, VfxShape.WIND_RING, from.add(0, 0.2, 0), from.add(0, 1, 0), 16, 1.1f);
        return true;
    }

    private static final BookPage.Cast MOUNTAIN_CONE = ElementBook.cone(4f, 8, 0.45, VfxShape.WIND_GALE, ElementBook.knock(2.4));

    /** Slicing Wind Emperor: Mountain Retreat (Fanzell Kruger): a very strong gust sends the opponents flying a great distance. */
    public static boolean mountainRetreat(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        if (!MOUNTAIN_CONE.cast(b, i, p, mode)) return false;
        fx(p, VfxShape.FX_WIND_GUST, eye, eye.add(look.scale(8)), 18, 1.3f);
        fx(p, VfxShape.WIND_RING, p.position().add(0, 0.2, 0), p.position().add(0, 1, 0), 16, 1.0f);
        return true;
    }

    private static final BookPage.Cast WHIRLWIND_BIND = ElementBook.bind(4f, 14, false, VfxShape.WIND_TORNADO, (t, p) -> {
        t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0));
        fx(p, VfxShape.WIND_RING, t.position(), t.position().add(0, 1, 0), 20, 1.0f);
    });

    /** Slicing Wind Emperor: Whirlwind (Fanzell Kruger): the sword is swung and whirlwinds bind the opponent's limbs and hold it in the air (Wind Restraining Magic; shares the control lock). */
    public static boolean whirlwind(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        return WHIRLWIND_BIND.cast(b, i, p, mode);
    }

    private static final BookPage.Cast KAKARA_DASH = ElementBook.dash(8f, 10, false, VfxShape.WIND_SLASH, ElementBook.NONE);

    /** Killing Kakara Blade (Jozou Hanegatsuji): you fly past the opponent and wind blades slash it from several angles. */
    public static boolean killingKakaraBlade(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position();
        if (!KAKARA_DASH.cast(b, i, p, mode)) return false;
        Vec3 to = p.position();
        fx(p, VfxShape.WIND_GALE, from.add(0, 1, 0), to.add(0, 1, 0), 12, 1.0f);
        fx(p, VfxShape.WIND_RING, to.add(0, 0.2, 0), to.add(0, 1, 0), 14, 0.9f);
        return true;
    }

    private static final BookPage.Cast NEEDLE_LINE = ElementBook.line(10f, 20, 0.7, VfxShape.WIND_GALE, ElementBook.NONE);

    /** Piercing Tornado Needle (George): a thin needle of wind goes through everything in a line; a small tornado stands where it ends. */
    public static boolean piercingTornadoNeedle(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 end = p.getEyePosition().add(p.getViewVector(1f).scale(20 * GrimoireBook.size(i, p)));
        if (!NEEDLE_LINE.cast(b, i, p, mode)) return false;
        fx(p, VfxShape.WIND_TORNADO, end, end.add(0, 1, 0), 20, 0.6f);
        return true;
    }

    // ================================================================ Dark Magic
    /** Black Star (Ichika Yami): a small black sphere where you aim pulls everything near it in for a second and unsettles it. */
    public static boolean blackStar(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 c = GrimoireBook.aim(p, 18);
        double r = 4.5 * s;
        b.castCircle(p, 0.9f);
        fx(p, VfxShape.DARK_BLACK_HOLE, c, c.add(0, 1, 0), 24, 0.6f * s);
        fx(p, VfxShape.DARKFX_BURST, c, c.add(0, 1, 0), 20, 0.8f * s);
        int n = 0;
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            if (n++ >= MAX_HITS) break;
            b.hurt(i, p, t, mode, 5.5f);
        }
        SpellRuntime.zone(p.serverLevel(), 20, 4, age -> {
            if (gone(p)) return;
            int m = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (m++ >= MAX_HITS) break;
                Vec3 pull = c.subtract(t.position()).multiply(0.22, 0.08, 0.22);
                t.setDeltaMovement(pull);
                t.hurtMarked = true;
            }
        });
        return true;
    }

    // ================================================================ Ice Magic
    /** Heavenly Ice Fang (Heath Graice): a large icicle rises from the ground under the target and strikes it (more blunt and cutting than piercing). */
    public static boolean heavenlyIceFang(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 24);
        if (t == null) { GrimoireBook.fail(p, "No target."); return false; }
        float s = GrimoireBook.size(i, p);
        Vec3 g = t.position();
        b.castCircle(p, 1f);
        b.hurt(i, p, t, mode, 11f);
        t.setTicksFrozen(t.getTicksRequiredToFreeze() + 40);
        t.setDeltaMovement(t.getDeltaMovement().add(0, 0.5, 0));
        t.hurtMarked = true;
        fx(p, VfxShape.ICE_FX1, g, g.add(0, 4, 0), 16, 1.3f * s);
        fx(p, VfxShape.ICE_FX3, g, g, 18, 0.9f * s);
        return true;
    }

    private static final BookPage.Cast BRIDGE_LINE = ElementBook.line(7f, 16, 1.0, VfxShape.ICE_FX1,
            ElementBook.all(ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2)), (t, p) -> t.setTicksFrozen(t.getTicksRequiredToFreeze() + 40)));

    /** Icicle Bridge: a large chunk of jagged ice is made and, once released, extends towards the target. */
    public static boolean icicleBridge(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 end = p.getEyePosition().add(p.getViewVector(1f).scale(16 * GrimoireBook.size(i, p)));
        if (!BRIDGE_LINE.cast(b, i, p, mode)) return false;
        fx(p, VfxShape.ICE_FX3, end, end, 16, 0.8f);
        return true;
    }

    /** Dissolve (Heath Graice): you raise a hand and melt any ice at once, yours or an enemy's, and spells of ice that fly at you come apart. */
    public static boolean dissolve(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = p.position().add(0, 1, 0);
        b.castCircle(p, 1f);
        p.setTicksFrozen(0);
        p.clearFire();
        int n = 0;
        for (LivingEntity ally : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(6), e -> e != p && e.isAlive() && e.isAlliedTo(p))) {
            if (n++ >= MAX_HITS) break;
            ally.setTicksFrozen(0);
        }
        SpellRuntime.dissolveBolts(p.serverLevel(), c, 7, p, MAX_DISCARD);
        discardForeign(p, c, 6);
        fx(p, VfxShape.ICE_FX3, p.position(), c, 18, 1.2f);
        fx(p, VfxShape.WATER_RING, p.position().add(0, 0.2, 0), c, 18, 1.1f);
        return true;
    }

    private static final BookPage.Cast ICE_SHIELD = ElementBook.empower(200, 0f, false, VfxShape.ICE_FX2,
            () -> new MobEffectInstance(MobEffects.ABSORPTION, 200, 2), () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));

    /** Crystal Ice Shield (Heath Graice): a shield of crystal ice stands round you for ten seconds. */
    public static boolean crystalIceShield(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!ICE_SHIELD.cast(b, i, p, mode)) return false;
        fx(p, VfxShape.ICE_FX3, p.position(), p.position().add(0, 1, 0), 16, 1.0f);
        return true;
    }

    // ================================================================ Beast Magic
    private static final BookPage.Cast CLAW_CONE = ElementBook.cone(13f, 3.6, 0.45, VfxShape.BEAST_FX1, ElementBook.knock(1.1));

    /** Bear Claw (Vetto): the arms are wrapped in an aura shaped like a bear's paws that greatly raises the strength of the punch. */
    public static boolean bearClaw(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 at = p.getEyePosition().add(p.getViewVector(1f).scale(2.6));
        if (!CLAW_CONE.cast(b, i, p, mode)) return false;
        fx(p, VfxShape.BEAST_FX3, at, at, 14, 0.9f);
        return true;
    }

    private static final BookPage.Cast CHEETAH_DASH = ElementBook.dash(8f, 11, false, VfxShape.BEAST_FX2, ElementBook.NONE);

    /** Cheetah Charge (Vetto): the legs are wrapped in an aura shaped like a cheetah's: a close-range burst of speed that runs through whoever is in the way. */
    public static boolean cheetahCharge(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position();
        if (!CHEETAH_DASH.cast(b, i, p, mode)) return false;
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 2));
        fx(p, VfxShape.BEAST_FX1, from.add(0, 1, 0), p.position().add(0, 1, 0), 12, 0.9f);
        return true;
    }

    private static final BookPage.Cast RHINO_SHIELD = ElementBook.empower(240, 0f, false, VfxShape.BEAST_FX3,
            () -> new MobEffectInstance(MobEffects.ABSORPTION, 240, 2), () -> new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 240, 0));

    /** Rhinoceros Armor (Vetto): an aura shaped like a rhinoceros that stops powerful impacts, for twelve seconds. */
    public static boolean rhinocerosArmor(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!RHINO_SHIELD.cast(b, i, p, mode)) return false;
        fx(p, VfxShape.BEAST_FX1, p.position(), p.position().add(0, 1, 0), 16, 1.0f);
        return true;
    }
}
