package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Eye Magic: sight that marks, blinds, weakens, reflects and paralyses. */
public final class EyeArts {
    private EyeArts() {}

    private static final VfxShape CAST = VfxShape.EYE_FX1, ZONE = VfxShape.EYE_FX2, BURST = VfxShape.EYE_FX3;
    static final String MARK_UNTIL = "nusmp_eye_mark_until", MARK_BY = "nusmp_eye_mark_by";

    private static Vec3 hand(ServerPlayer p) { return p.getEyePosition().add(0, -0.3, 0); }
    private static Vec3 mid(LivingEntity t) { return t.getBoundingBox().getCenter(); }

    /** Not a spectator, not a creative player. */
    static boolean valid(LivingEntity t) {
        return t.isAlive() && !t.isSpectator() && !(t instanceof Player pl && pl.isCreative());
    }

    /** Bosses keep only a quarter of a status duration. */
    static int shorten(LivingEntity t, int ticks) { return BalanceLaw.isBoss(t) ? Math.max(10, ticks / 4) : ticks; }

    /** Unhides the target (invisibility is stripped) and makes it glow through walls. */
    static void reveal(LivingEntity t, int ticks) {
        t.removeEffect(MobEffects.INVISIBILITY);
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0));
    }

    /** Marks the target for the caster and allies: they deal 25% more to it while the mark lasts. */
    static void mark(LivingEntity t, ServerPlayer p, int ticks) {
        t.getPersistentData().putLong(MARK_UNTIL, t.level().getGameTime() + ticks);
        t.getPersistentData().putString(MARK_BY, p.getUUID().toString());
        reveal(t, ticks);
    }

    /** Rider of the seeking eyes: a flash that blinds and a small bleed of magicules. */
    static ElementBook.Rider dazzle() {
        return (t, p) -> {
            t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, shorten(t, 40), 0));
            Nullification.bleed(t, 0.01);
        };
    }

    /** Hook (EyeProps): a marked target takes 25% more from the one who marked it and from their allies. */
    public static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity v = e.getEntity();
        var d = v.getPersistentData();
        if (!d.contains(MARK_UNTIL) || v.level().getGameTime() >= d.getLong(MARK_UNTIL)) return;
        Entity a = e.getSource().getEntity();
        if (a == null) return;
        String by = d.getString(MARK_BY);
        if (!a.getUUID().toString().equals(by)) {
            Player owner;
            try { owner = v.level().getPlayerByUUID(UUID.fromString(by)); } catch (IllegalArgumentException bad) { return; }
            if (owner == null || !a.isAlliedTo(owner)) return;
        }
        e.setAmount(e.getAmount() * 1.25f);
    }

    // ---------------------------------------------------------------- starter

    /** Piercing Glance: an instant gaze that hits the first thing in sight; the invisible are revealed and hurt more. */
    static boolean glance(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 28);
        if (t == null) { GrimoireBook.fail(p, "Nothing in sight."); return false; }
        float raw = t.hasEffect(MobEffects.INVISIBILITY) ? 10.5f : 7f;
        b.castCircle(p, 0.6f);
        b.vfx(p, CAST, hand(p), mid(t), 8, 1f * GrimoireBook.size(i, p));
        b.hurt(i, p, t, mode, raw);
        reveal(t, 100);
        b.vfx(p, BURST, mid(t), mid(t), 20, 0.9f);
        return true;
    }

    // ---------------------------------------------------------------- mid

    /** Mark of Sight: the target glows through walls and takes 25% more from you and your allies for 12 s. */
    static boolean markOfSight(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 32);
        if (t == null) { GrimoireBook.fail(p, "Nothing to mark."); return false; }
        b.castCircle(p, 0.7f);
        b.vfx(p, CAST, hand(p), mid(t), 10, 1f);
        mark(t, p, 240);
        b.hurt(i, p, t, mode, 2f);
        b.vfx(p, BURST, mid(t), mid(t), 28, Math.max(0.8f, t.getBbHeight() / 1.8f));
        return true;
    }

    /** Blinding Flash: a glare in front of you; it blinds, and monsters lose you. */
    static boolean flash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        double r = 9 * GrimoireBook.size(i, p);
        int hit = 0;
        for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
            if (!valid(t) || mid(t).subtract(eye).normalize().dot(look) < 0.35) continue;
            if (hit == 0) { b.castCircle(p, 0.9f); b.vfx(p, BURST, eye.add(look.scale(0.8)), eye.add(look.scale(r)), 24, 1.2f); }
            if (++hit > 16) break;
            b.hurt(i, p, t, mode, 4f);
            t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, shorten(t, 120), 0));
            if (t instanceof Mob m && !BalanceLaw.isBoss(t)) m.setTarget(null);
            b.vfx(p, CAST, eye, mid(t), 8, 0.6f);
        }
        if (hit == 0) { GrimoireBook.fail(p, "No one is looking at you."); return false; }
        return true;
    }

    /** Evil Eye: a gaze that weakens, saps strength and speed, shreds resistance and drinks magicules. */
    static boolean evilEye(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 24);
        if (t == null) { GrimoireBook.fail(p, "Nothing to curse with your gaze."); return false; }
        int w = shorten(t, 240);
        b.castCircle(p, 0.8f);
        b.vfx(p, CAST, hand(p), mid(t), 12, 1.2f);
        b.hurt(i, p, t, mode, 4f);
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, w, 1));
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, w / 2, 0));
        t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, w / 2, 0));
        EnergyBridge.effect(t, "fragility", shorten(t, 120), 0);
        EnergyBridge.drain(t, p, BalanceLaw.isBoss(t) ? 0.01 : 0.05);
        b.vfx(p, BURST, mid(t), mid(t), 26, 1.1f);
        return true;
    }

    /** Iris Ward: for 8 s enemy shots entering your guard are turned back at whoever fired them, and that caster is jammed. */
    static boolean ward(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 160;
        double r = 4.5 * GrimoireBook.size(i, p);
        ServerLevel sl = p.serverLevel();
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(sl, ZONE, p, p.position(), b.color, ticks, (float) r);
        int[] left = {14};
        SpellRuntime.zone(sl, ticks, 2, age -> {
            if (!p.isAlive() || left[0] <= 0) return;
            Vec3 c = p.position().add(0, 1, 0);
            for (Projectile pr : WikiSpells.enemyShots(p, new AABB(c, c).inflate(r))) {
                if (left[0] <= 0 || pr.position().distanceToSqr(c) > r * r) continue;
                Vec3 at = pr.position(), back;
                if (pr.getOwner() instanceof LivingEntity s && s.isAlive()) {
                    back = mid(s).subtract(at).normalize().scale(Math.max(1.2, pr.getDeltaMovement().length() * 1.2));
                    EnergyBridge.effect(s, "silence", shorten(s, 40), 0);
                } else {
                    back = pr.getDeltaMovement().scale(-1.2);
                }
                pr.setDeltaMovement(back);
                pr.setOwner(p);
                pr.hurtMarked = true;
                left[0]--;
                VfxSpawn.send(sl, BURST, at, at.add(back), b.color, 14, 0.5f);
            }
        });
        return true;
    }

    /** Sight Step: step to the spot you are looking at (up to 16 blocks, no walls), with a night-vision eye after; foes along the way are revealed. */
    static boolean sightStep(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), from = p.position();
        HitResult wall = p.level().clip(new ClipContext(eye, eye.add(look.scale(16)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double max = wall.getType() == HitResult.Type.MISS ? 16 : eye.distanceTo(wall.getLocation()) - 0.6;
        Vec3 dest = null;
        for (double d = max; d >= 2; d -= 0.5) {
            Vec3 c = from.add(look.scale(d));
            if (p.level().isLoaded(net.minecraft.core.BlockPos.containing(c)) && p.level().noCollision(p, p.getBoundingBox().move(c.subtract(from)))) { dest = c; break; }
        }
        if (dest == null) { GrimoireBook.fail(p, "No room to step."); return false; }
        int n = 0;
        for (LivingEntity t : GrimoireBook.along(p, from.add(0, 1, 0), dest.add(0, 1, 0), 2.5)) {
            if (!valid(t)) continue;
            reveal(t, 60);
            if (++n >= 8) break;
        }
        b.castCircle(p, 0.7f);
        p.teleportTo(dest.x, dest.y, dest.z);
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100, 0));
        b.vfx(p, CAST, from.add(0, 1.2, 0), dest.add(0, 1.2, 0), 12, 1.2f);
        b.vfx(p, BURST, dest.add(0, 1, 0), dest.add(0, 1, 0), 20, 0.8f);
        return true;
    }

    // ---------------------------------------------------------------- zone

    /** Field of Eyes: eyes open across the ground; everything in it glows, is weakened, bled and jammed for 8 s. */
    static boolean fieldOfEyes(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 24);
        double r = 6 * GrimoireBook.size(i, p);
        int ticks = 160;
        b.castCircle(p, 1f);
        b.vfx(p, ZONE, c, c, ticks, (float) r);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (!valid(t)) continue;
                if (++n > 12) break;
                b.hurt(i, p, t, mode, 1.5f);
                reveal(t, 30);
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 0));
                EnergyBridge.effect(t, "silence", 25, 0);
                Nullification.bleed(t, 0.005);
            }
        });
        return true;
    }

    // ---------------------------------------------------------------- signature

    /** Reflect Iris: an array of mirrors around you; whoever looks into it is paralysed with their magic blocked, then struck (Reflect Refrain). */
    static boolean reflectIris(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 8 * GrimoireBook.size(i, p);
        boolean any = false;
        for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) if (valid(t)) { any = true; break; }
        if (!any) { GrimoireBook.fail(p, "No one to catch in the mirrors."); return false; }
        if (!GrimoireBook.control(p)) return false;
        Vec3 c = p.position();
        int ticks = 120;
        ServerLevel sl = p.serverLevel();
        b.castCircle(p, 1.4f);
        b.vfx(p, ZONE, c, c, ticks, (float) r);
        Set<UUID> caught = new HashSet<>();
        SpellRuntime.zone(sl, ticks, 4, age -> {
            if (!p.isAlive() || caught.size() >= 8) return;
            Vec3 core = c.add(0, 1, 0);
            for (LivingEntity t : GrimoireBook.around(p, c, r)) {
                if (!valid(t) || caught.contains(t.getUUID())) continue;
                if (t.getViewVector(1f).dot(core.subtract(t.getEyePosition()).normalize()) < 0.55) continue;
                caught.add(t.getUUID());
                int hold = BalanceLaw.controlTicks(t, 50);
                EnergyBridge.effect(t, "paralysis", hold, 0);
                EnergyBridge.effect(t, "silence", hold + 60, 0);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, hold, 9));
                t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
                t.hurtMarked = true;
                b.vfx(p, VfxShape.MIRROR_PANE, t.getEyePosition().add(t.getViewVector(1f).scale(1.2)), t.getEyePosition(), hold, 1f);
                b.vfx(p, CAST, core, mid(t), 10, 1f);
                SpellRuntime.later(sl, hold, () -> {
                    if (!t.isAlive()) return;
                    b.hurt(i, p, t, mode, 14f);
                    b.vfx(p, BURST, mid(t), mid(t), 24, 1.3f);
                });
                if (caught.size() >= 8) break;
            }
        });
        return true;
    }

    // ---------------------------------------------------------------- daily

    /** All-Seeing Eye: for 30 s you see in the dark, everything within 40 blocks glows through walls (the invisible too) and the nearest foes stay marked. */
    static boolean allSeeing(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 600;
        double r = 40;
        b.castCircle(p, 1.6f);
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ticks, 0));
        VfxSpawn.sendFollowing(p.serverLevel(), BURST, p, p.position().add(0, 1.6, 0), b.color, 30, 1.6f);
        b.vfx(p, ZONE, p.position(), p.position(), 40, 12f);
        SpellRuntime.zone(p.serverLevel(), ticks, 20, age -> {
            if (!p.isAlive()) return;
            int n = 0;
            for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
                if (!valid(t)) continue;
                if (++n > 40) break;
                reveal(t, 50);
                if (t.distanceToSqr(p) < 400) mark(t, p, 40);
            }
        });
        return true;
    }
}
