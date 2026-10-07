package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Food Magic: the spells of FoodBook that need their own logic (cutlery strikes, pies, spoiled food, the banquet table, the feast). */
public final class FoodArts {
    private FoodArts() {}

    /** Set on the skill tag by Full Stomach: until then, projectiles that hit you are eaten (see FoodBook#onTakenDamage). */
    static final String EAT_UNTIL = "FoodEatUntil";

    // ------------------------------------------------------------------ small helpers
    private static float k(ManasSkillInstance i, ServerPlayer p) {
        return GrimoireBook.size(i, p) * EnergyBridge.scale(p);
    }

    /** The caster and its friends within r of c (at most 12). */
    static List<LivingEntity> friends(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> out = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                e -> e.isAlive() && !e.isSpectator() && (e == p || e.isAlliedTo(p)) && e.distanceToSqr(c) <= r * r);
        return out.size() > 12 ? out.subList(0, 12) : out;
    }

    /** A meal for one friend: healing (with the healing decay), food and saturation for players, a little regeneration. */
    static void feed(LivingEntity f, float frac, int food, int regenTicks) {
        BalanceLaw.heal(f, f.getMaxHealth() * frac);
        if (f instanceof ServerPlayer sp) sp.getFoodData().eat(food, 0.6f);
        if (regenTicks > 0) f.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenTicks, 0));
    }

    /** A good meal washes the bad food out: hunger, poison and nausea come off. */
    static void cleanse(LivingEntity f) {
        f.removeEffect(MobEffects.HUNGER);
        f.removeEffect(MobEffects.POISON);
        f.removeEffect(MobEffects.CONFUSION);
    }

    /** Spoiled food on a target: hunger, poison, weakness (and nausea, except on bosses). */
    static void spoil(LivingEntity t, int ticks) {
        t.addEffect(new MobEffectInstance(MobEffects.HUNGER, ticks * 2, 2));
        t.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, 1));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0));
        if (!BalanceLaw.isBoss(t)) t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, Math.min(ticks, 80), 0));
    }

    // ------------------------------------------------------------------ Giant Fork: a skewering bolt that bleeds
    static boolean forkSkewer(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        int[] eaten = {0};
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.FOOD_FX1, start, start.add(dir.scale(29)), 16, 1.0f);
        SpellRuntime.bolt(p, start, dir.scale(1.8), 0.6 * s, 16, true, null, (bolt, t) -> {
            b.hurt(i, p, t, mode, 9f);
            Nullification.bleed(t, 0.05);
            if (eaten[0]++ < 3) BalanceLaw.heal(p, 1.5f);
        }, (bolt, at) -> b.vfx(p, VfxShape.FOOD_FX3, at, at, 20, 0.8f));
        return true;
    }

    // ------------------------------------------------------------------ Carving Knife: a wide cut that weakens
    static boolean carvingCleave(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        double r = 5.0 * s;
        for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
            if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.35) continue;
            b.hurt(i, p, t, mode, 12f);
            Nullification.bleed(t, 0.04);
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            t.knockback(0.5, -look.x, -look.z);
        }
        b.castCircle(p, 0.6f);
        Vec3 mid = eye.add(look.scale(r * 0.6));
        b.vfx(p, VfxShape.FOOD_FX1, eye.add(look.scale(0.5)), mid, 12, 1.2f);
        b.vfx(p, VfxShape.FOOD_FX3, mid, mid.add(look), 22, 1.0f);
        return true;
    }

    // ------------------------------------------------------------------ Pie Barrage: five homing pies, cream in the face
    static boolean pieBarrage(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        List<LivingEntity> ts = GrimoireBook.around(p, p.position(), 18);
        b.castCircle(p, 0.9f);
        for (int n = 0; n < 5; n++) {
            int k = n;
            SpellRuntime.later(p.serverLevel(), n * 2, () -> {
                if (!p.isAlive()) return;
                Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f), side = new Vec3(-dir.z, 0, dir.x).normalize();
                double off = (k - 2) * 0.14;
                Vec3 d = dir.add(side.scale(off)).add(0, 0.05, 0).normalize();
                LivingEntity home = ts.isEmpty() ? null : ts.get(k % ts.size());
                b.vfx(p, VfxShape.FOOD_FX1, start, home != null ? home.getBoundingBox().getCenter() : start.add(d.scale(20)), 14, 0.6f);
                SpellRuntime.bolt(p, start, d.scale(1.4), 0.6, 30, false, home, (bolt, t) -> {
                    b.hurt(i, p, t, mode, 5f);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    if (!BalanceLaw.isBoss(t)) t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0));
                }, (bolt, at) -> b.vfx(p, VfxShape.FOOD_FX3, at, at, 16, 0.6f));
            });
        }
        return true;
    }

    // ------------------------------------------------------------------ Rotten Feast: a spoiled dish that bursts into a stinking cloud
    static boolean rottenFeast(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.FOOD_FX1, start, start.add(dir.scale(28)), 20, 1.0f);
        SpellRuntime.bolt(p, start, dir.scale(1.4), 0.5 * s, 20, false, null, (bolt, t) -> b.hurt(i, p, t, mode, 6f), (bolt, at) -> {
            double r = 3.2 * s;
            b.vfx(p, VfxShape.FOOD_FX3, at, at, 24, 1.0f);
            b.vfx(p, VfxShape.FOOD_FX2, at, at.add(0, 1, 0), 80, (float) r);
            SpellRuntime.zone(p.serverLevel(), 80, 10, age -> {
                for (LivingEntity t : GrimoireBook.around(p, at, r)) {
                    b.hurt(i, p, t, mode, 2.5f);
                    spoil(t, 60);
                }
            });
        });
        return true;
    }

    // ------------------------------------------------------------------ Starving Rush: dash and bite through the line
    // (the dash is ElementBook.dash with a leech rider, see FoodBook)

    // ------------------------------------------------------------------ Hearty Meal: a plate for every friend
    static boolean heartyMeal(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 8 * k(i, p);
        List<LivingEntity> fs = friends(p, p.position(), r);
        for (LivingEntity f : fs) {
            feed(f, 0.25f, 8, 100);
            WikiSpells.giveManaFrac(f, 0.04);
            b.vfx(p, VfxShape.FOOD_FX3, f.position().add(0, 0.5, 0), f.position().add(0, 1.5, 0), 20, 0.7f);
        }
        b.castCircle(p, 1.1f);
        b.vfx(p, VfxShape.FOOD_FX2, p.position(), p.position().add(0, 1, 0), 40, (float) r);
        return true;
    }

    // ------------------------------------------------------------------ Full Stomach: a stuffed body that eats projectiles
    static boolean fullStomach(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 600;
        i.getOrCreateTag().putLong(EAT_UNTIL, p.level().getGameTime() + ticks);
        for (LivingEntity f : friends(p, p.position(), 5)) {
            boolean self = f == p;
            f.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, self ? ticks : ticks / 2, 0));
            f.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, self ? ticks : ticks / 2, 0));
            f.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, self ? ticks : ticks / 2, self ? 2 : 1));
            if (f instanceof ServerPlayer sp) sp.getFoodData().eat(20, 1f);
        }
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.FOOD_FX3, p, p.position().add(0, 1, 0), b.color, 30, 1.0f);
        return true;
    }

    // ------------------------------------------------------------------ Silver Platter: a ward that eats what is thrown at you
    static boolean silverPlatter(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 3.4 * k(i, p);
        int ticks = 100;
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 1));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.FOOD_FX2, p, p.position().add(0, 1, 0), b.color, ticks, (float) r);
        SpellRuntime.zone(p.serverLevel(), ticks, 2, age -> {
            if (!p.isAlive()) return;
            Vec3 c = p.position().add(0, 1, 0);
            int n = 0;
            for (Projectile pr : WikiSpells.enemyShots(p, new AABB(c, c).inflate(r))) {
                if (n++ >= 6) break;
                b.vfx(p, VfxShape.FOOD_FX3, pr.position(), c, 14, 0.5f);
                pr.discard();
                BalanceLaw.heal(p, 2f);
                WikiSpells.giveManaFrac(p, 0.03);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Banquet Table: a laid table that feeds friends and stuffs foes
    static boolean banquetTable(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 5 * k(i, p);
        Vec3 a = GrimoireBook.aim(p, 16);
        Vec3 c = a.distanceTo(p.getEyePosition()) > 15.5 ? p.position() : a;
        int ticks = 160;
        b.castCircle(p, 1.1f);
        b.vfx(p, VfxShape.FOOD_FX3, c, c.add(0, 1, 0), 24, 1.0f);
        b.vfx(p, VfxShape.FOOD_FX2, c, c.add(0, 1, 0), ticks, (float) r);
        SpellRuntime.zone(p.serverLevel(), ticks, 20, age -> {
            for (LivingEntity f : friends(p, c, r)) {
                feed(f, 0.04f, 2, 40);
                WikiSpells.giveManaFrac(f, 0.015);
            }
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Glutton's Banquet (wiki): the wolf with the fork and knife
    static boolean gluttonsBanquet(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = k(i, p);
        double r = 5.5 * s;
        Vec3 look = p.getViewVector(1f), c = p.getEyePosition().add(look.scale(4));
        Vec3 g = new Vec3(c.x, p.getY(), c.z);
        int[] eaten = {0};
        boolean[] bit = {false};
        b.castCircle(p, 1.3f);
        b.vfx(p, VfxShape.FOOD_MAW, c, c.add(look), 60, 1.4f);
        b.vfx(p, VfxShape.FOOD_FX2, g, g.add(0, 1, 0), 60, (float) r);
        SpellRuntime.zone(p.serverLevel(), 56, 2, age -> {
            if (!p.isAlive()) return;
            int n = 0;
            for (Projectile pr : WikiSpells.enemyShots(p, new AABB(c, c).inflate(r))) {
                if (n++ >= 6) break;
                b.vfx(p, VfxShape.FOOD_FX3, pr.position(), c, 14, 0.5f);
                pr.discard();
                eaten[0]++;
                WikiSpells.giveManaFrac(p, 0.05);
                BalanceLaw.heal(p, 2f);
            }
            if (age % 10 == 0) {
                int broke = Nullification.shatterBarriers(p.serverLevel(), c, r, p);
                if (broke > 0) { eaten[0] += broke; WikiSpells.giveManaFrac(p, 0.04 * Math.min(broke, 3)); }
            }
            if (age >= 48 && !bit[0]) {
                bit[0] = true;
                for (LivingEntity t : GrimoireBook.around(p, c, Math.min(r, 4.5))) {
                    b.hurt(i, p, t, mode, 14f);
                    WikiSpells.giveMana(p, WikiSpells.drainMana(t, 0.12));
                    BalanceLaw.heal(p, 3f);
                }
                if (eaten[0] > 0) p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, Math.min(3, eaten[0] / 2)));
                b.vfx(p, VfxShape.FOOD_FX3, c, c.add(look), 30, 1.3f);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Midnight Feast (daily): the whole table eats its fill
    static boolean midnightFeast(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 12 * k(i, p);
        Vec3 c = p.position();
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.FOOD_FX2, c, c.add(0, 1, 0), 100, (float) r);
        b.vfx(p, VfxShape.FOOD_FX3, c.add(0, 0.5, 0), c.add(0, 1.5, 0), 40, 1.6f);
        for (LivingEntity f : friends(p, c, r)) {
            cleanse(f);
            feed(f, 0.5f, 20, 200);
            WikiSpells.giveManaFrac(f, 0.25);
            f.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 2));
            f.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 0));
        }
        SpellRuntime.later(p.serverLevel(), 60, () -> {
            if (!p.isAlive()) return;
            b.vfx(p, VfxShape.FOOD_FX3, p.position().add(0, 0.5, 0), p.position().add(0, 1.5, 0), 28, 1.2f);
            for (LivingEntity f : friends(p, p.position(), r)) feed(f, 0.2f, 6, 0);
        });
        return true;
    }
}
