package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Butoh Magic: the dance spells. Water is the sea god's element (slowing, pulling, soaking), rhythm is the buff and the jam:
 * every pulse of a beat can buff allies and silence enemies. All damage goes through the balance law as water damage.
 */
final class ButohArts {
    private ButohArts() {}

    private static final ResourceKey<DamageType> WATER = TensuraDamageTypes.WATER_ELEMENTAL;
    static final String TRANCE = "ButohTranceUntil", GUARD = "ButohGuardUntil";

    // ------------------------------------------------------------------ helpers
    private static void hit(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, LivingEntity t, int mode, float raw) {
        b.hurtAs(i, p, t, mode, raw, WATER);
    }

    private static void slow(LivingEntity t, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.isBoss(t) ? ticks / 2 : ticks, amp));
    }

    /** Magic Jamming: the beat breaks the enemy's casting rhythm (bosses 1 s at most). */
    private static void jam(LivingEntity t, int ticks) {
        EnergyBridge.effect(t, "silence", BalanceLaw.isBoss(t) ? Math.min(ticks, 20) : ticks, 0);
    }

    private static void knock(Vec3 from, LivingEntity t, double k) {
        Vec3 d = t.position().subtract(from);
        double len = Math.sqrt(d.x * d.x + d.z * d.z);
        if (len < 1e-3) return;
        t.knockback(k, -d.x / len, -d.z / len);
    }

    private static void lift(LivingEntity t, double y) {
        t.setDeltaMovement(t.getDeltaMovement().add(0, y, 0));
        t.hurtMarked = true;
    }

    private static List<ServerPlayer> allies(ServerPlayer p, Vec3 c, double r) {
        return p.serverLevel().getEntitiesOfClass(ServerPlayer.class, new AABB(c, c).inflate(r),
                a -> a.isAlive() && !a.isSpectator() && (a == p || a.isAlliedTo(p)) && a.distanceToSqr(c) <= r * r);
    }

    // ------------------------------------------------------------------ Flowing Water Blade
    /** Two sweeping arcs of water in a row: the dancer turns and the blade follows. Slows and shoves. */
    static boolean flowingBlade(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        double r = 5.5 * s;
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.BUTOH_FX1, eye, eye.add(look.scale(r)), 16, 1.0f);
        arc(b, i, p, mode, eye, look, r, 7f, 0.4);
        SpellRuntime.later(p.serverLevel(), 7, () -> {
            if (!p.isAlive()) return;
            Vec3 back = p.getEyePosition(), l2 = p.getViewVector(1f);
            b.vfx(p, VfxShape.BUTOH_FX1, back, back.add(l2.scale(r)), 16, 1.0f);
            arc(b, i, p, mode, back, l2, r, 5f, 0.8);
        });
        return true;
    }

    private static void arc(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, Vec3 eye, Vec3 look, double r, float dmg, double push) {
        for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
            if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.3) continue;
            hit(b, i, p, t, mode, dmg);
            slow(t, 50, 1);
            knock(p.position(), t, push);
        }
    }

    // ------------------------------------------------------------------ Rhythm Wave
    /** Three beats around the dancer: each shoves, the last jams the casters it caught; allies in the beat run faster. */
    static boolean rhythmWave(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 6 * GrimoireBook.size(i, p);
        b.castCircle(p, 1f);
        SpellRuntime.zone(p.serverLevel(), 23, 8, age -> {
            if (!p.isAlive()) return;
            int beat = age / 8;
            Vec3 c = p.position();
            b.vfx(p, VfxShape.BUTOH_FX3, c.add(0, 0.2, 0), c.add(0, 1, 0), 16, 1.0f + 0.2f * beat);
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                hit(b, i, p, t, mode, 3.5f);
                knock(c, t, 0.5);
                if (beat == 2) { jam(t, 80); slow(t, 40, 1); }
            }
            for (ServerPlayer a : allies(p, c, r)) a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0));
        });
        return true;
    }

    // ------------------------------------------------------------------ Union Magic: Sea God Slash
    /** Canon: the song enhances the dance, the swords carry a great aura, every swing throws a shockwave and flying slashes. */
    static boolean seaGodSlash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = GrimoireBook.size(i, p);
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        Set<LivingEntity> seen = new HashSet<>();
        b.castCircle(p, 1f);
        Vec3 near = p.position().add(look.x * 2, 1, look.z * 2);
        b.vfx(p, VfxShape.BUTOH_FX3, near, near.add(0, 0.5, 0), 16, 1.0f);
        for (LivingEntity t : GrimoireBook.around(p, near, 3.5 * s)) {
            hit(b, i, p, t, mode, 5f);
            knock(p.position(), t, 0.9);
            seen.add(t);
        }
        for (int k = -1; k <= 1; k++) {
            Vec3 d = look.yRot((float) (k * 0.21));
            b.vfx(p, VfxShape.BUTOH_FX1, eye, eye.add(d.scale(26 * s)), 18, 1.0f);
            SpellRuntime.bolt(p, eye, d.scale(1.8), 0.9 * s, 15, true, null, (bolt, t) -> {
                if (!seen.add(t)) return;
                hit(b, i, p, t, mode, 10f);
                slow(t, 40, 1);
                knock(bolt.pos, t, 0.4);
            }, (bolt, at) -> b.vfx(p, VfxShape.BUTOH_FX3, at, at.add(0, 0.5, 0), 14, 0.7f));
        }
        return true;
    }

    // ------------------------------------------------------------------ Ebb Step
    /** An evasion step: slip backwards out of the fight, leave a wet afterimage that hunters chase, shove what stood close. */
    static boolean ebbStep(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position();
        Vec3 back = p.getViewVector(1f).multiply(1, 0, 1);
        if (back.lengthSqr() < 1e-4) back = new Vec3(0, 0, 1);
        back = back.normalize().scale(-1);
        Vec3 dest = null;
        for (double d = 7; d >= 2; d -= 0.5) {
            Vec3 c = from.add(back.scale(d));
            if (p.level().noCollision(p, p.getBoundingBox().move(c.subtract(from)))) { dest = c; break; }
        }
        if (dest == null) { GrimoireBook.fail(p, "No room to slip away."); return false; }
        for (LivingEntity t : GrimoireBook.around(p, from, 2.8)) { slow(t, 40, 1); knock(from, t, 0.7); }
        for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16))) if (m.getTarget() == p) m.setTarget(null);
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.BUTOH_FX3, from.add(0, 0.2, 0), from.add(0, 1, 0), 18, 1.0f);
        p.teleportTo(dest.x, dest.y, dest.z);
        p.fallDistance = 0;
        b.vfx(p, VfxShape.BUTOH_FX1, from.add(0, 1, 0), dest.add(0, 1, 0), 14, 1.0f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 2));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
        return true;
    }

    // ------------------------------------------------------------------ Trance Dance
    /** The trance: faster, harder-hitting, and now and then the dancer is simply not where the blow lands. Allies catch the beat. */
    static boolean tranceDance(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 600;
        long until = p.level().getGameTime() + ticks;
        var tag = i.getOrCreateTag();
        tag.putLong("EmpowerUntil", until);
        tag.putFloat("EmpowerBonus", 3f);
        tag.putLong(TRANCE, until);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 1));
        for (ServerPlayer a : allies(p, p.position(), 10)) {
            if (a == p) continue;
            a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0));
            a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 0));
        }
        b.castCircle(p, 1.2f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BUTOH_FX2, p, p.position().add(0, 1, 0), b.color, 200, 3.5f);
        return true;
    }

    // ------------------------------------------------------------------ Tidal Veil
    /** A veil of water: damage softened, a few hearts of absorption, blows reflected, arrows and spells washed away. */
    static boolean tidalVeil(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 160;
        i.getOrCreateTag().putLong(GUARD, p.level().getGameTime() + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 1));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 1));
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 3.5)) { knock(p.position(), t, 0.8); slow(t, 30, 0); }
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.BUTOH_FX2, p, p.position().add(0, 1, 0), b.color, ticks, 3f);
        SpellRuntime.zone(p.serverLevel(), ticks, 4, age -> {
            if (!p.isAlive()) return;
            for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(3.5))) if (pr.getOwner() != p) pr.discard();
        });
        return true;
    }

    // ------------------------------------------------------------------ Undertow
    /** A whirlpool at the aim point: it drags everything in toward the middle, soaks it slow, and ends in a surge. */
    static boolean undertow(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 24);
        double r = 5 * GrimoireBook.size(i, p);
        int ticks = 120;
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.BUTOH_FX2, c.add(0, 0.1, 0), c.add(0, 1, 0), ticks, (float) r);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            if (!p.isAlive()) return;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (age == 100) {
                    hit(b, i, p, t, mode, 9f);
                    lift(t, 0.7);
                    knock(c, t, 0.9);
                } else {
                    hit(b, i, p, t, mode, 2.5f);
                    if (!BalanceLaw.isBoss(t)) {
                        Vec3 pull = new Vec3(c.x - t.getX(), 0, c.z - t.getZ());
                        if (pull.lengthSqr() > 0.25) {
                            Vec3 v = pull.normalize().scale(0.35);
                            t.setDeltaMovement(v.x, t.getDeltaMovement().y, v.z);
                            t.hurtMarked = true;
                        }
                    }
                }
                slow(t, 50, 2);
            }
            if (age == 100) b.vfx(p, VfxShape.BUTOH_FX3, c.add(0, 0.2, 0), c.add(0, 1, 0), 18, 1.3f);
            else if (age % 20 == 0) b.vfx(p, VfxShape.BUTOH_FX3, c.add(0, 0.2, 0), c.add(0, 1, 0), 14, 0.6f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Metronome Field
    /** A stage of rhythm: allies inside move and hit faster, enemies lose their casting beat, their weapons and some magicules. */
    static boolean metronomeField(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = p.position();
        double r = 7 * GrimoireBook.size(i, p);
        int ticks = 200;
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.BUTOH_FX2, c.add(0, 0.1, 0), c.add(0, 1, 0), ticks, (float) r);
        SpellRuntime.zone(p.serverLevel(), ticks, 20, age -> {
            if (!p.isAlive()) return;
            for (ServerPlayer a : allies(p, c, r)) {
                a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 0));
                a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 50, 0));
            }
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                hit(b, i, p, t, mode, 2f);
                jam(t, 40);
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0));
                slow(t, 50, 0);
                Nullification.bleed(t, 0.01);
            }
            b.vfx(p, VfxShape.BUTOH_FX3, c.add(0, 0.2, 0), c.add(0, 1, 0), 12, 0.6f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Sea God's Descent
    /** The sea god comes down on the aim point: a marked ring, then a crushing wave, then the echo that sweeps wider. */
    static boolean seaGodDescent(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 28);
        double r = 7 * GrimoireBook.size(i, p);
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.BUTOH_FX2, c.add(0, 0.1, 0), c.add(0, 1, 0), 28, (float) r);
        SpellRuntime.later(p.serverLevel(), 24, () -> {
            if (!p.isAlive()) return;
            b.vfx(p, VfxShape.BUTOH_FX3, c.add(0, 0.2, 0), c.add(0, 1, 0), 26, 1.4f);
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                hit(b, i, p, t, mode, 22f);
                lift(t, 0.9);
                knock(c, t, 1.0);
                slow(t, 80, 3);
            }
            SpellRuntime.later(p.serverLevel(), 8, () -> {
                if (!p.isAlive()) return;
                b.vfx(p, VfxShape.BUTOH_FX3, c.add(0, 0.2, 0), c.add(0, 1, 0), 20, 1.0f);
                for (LivingEntity t : GrimoireBook.around(p, c, r * 1.4)) {
                    hit(b, i, p, t, mode, 8f);
                    knock(c, t, 1.2);
                    slow(t, 60, 1);
                }
            });
        });
        return true;
    }

    // ------------------------------------------------------------------ Seabed Hymn
    /** Daily: the worship dance of the Seabed Temple. Allies are healed and washed clean; enemies are swept back and silenced. */
    static boolean seabedHymn(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = p.position();
        double r = 10 * GrimoireBook.size(i, p);
        b.castCircle(p, 1.6f);
        b.vfx(p, VfxShape.BUTOH_FX2, c.add(0, 0.1, 0), c.add(0, 1, 0), 60, (float) r);
        b.vfx(p, VfxShape.BUTOH_FX3, c.add(0, 0.2, 0), c.add(0, 1, 0), 24, 1.2f);
        for (ServerPlayer a : allies(p, c, r)) {
            BalanceLaw.heal(a, a.getMaxHealth() * 0.35f);
            for (MobEffectInstance e : new ArrayList<>(a.getActiveEffects())) if (!e.getEffect().value().isBeneficial()) a.removeEffect(e.getEffect());
            a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
        }
        for (LivingEntity t : GrimoireBook.around(p, c, r)) {
            hit(b, i, p, t, mode, 10f);
            knock(c, t, 1.0);
            slow(t, 100, 1);
            jam(t, 100);
        }
        return true;
    }
}
