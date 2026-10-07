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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Eyeball Magic: floating eyes that scan, slow and petrify, shoot eye bolts and share their sight. */
public final class EyeballArts {
    private EyeballArts() {}

    private static final VfxShape CAST = VfxShape.EYEBALL_FX1, ZONE = VfxShape.EYEBALL_FX2, BURST = VfxShape.EYEBALL_FX3;

    private static Vec3 hand(ServerPlayer p) { return p.getEyePosition().add(0, -0.3, 0); }
    private static Vec3 mid(LivingEntity t) { return t.getBoundingBox().getCenter(); }

    private static boolean valid(LivingEntity t) {
        return t.isAlive() && !t.isSpectator() && !(t instanceof Player pl && pl.isCreative());
    }

    /** Bosses keep only a quarter of a status duration. */
    private static int shorten(LivingEntity t, int ticks) { return BalanceLaw.isBoss(t) ? Math.max(10, ticks / 4) : ticks; }

    private static void slow(LivingEntity t, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, shorten(t, ticks), amp));
    }

    private static void glow(LivingEntity t, int ticks) {
        t.removeEffect(MobEffects.INVISIBILITY);
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0));
    }

    /** The eyes read the muscles: a target that is walking, falling or jumping cannot dodge the shot. */
    private static boolean moving(LivingEntity t) {
        Vec3 v = t.getDeltaMovement();
        return v.horizontalDistanceSqr() > 0.004 || Math.abs(v.y) > 0.15;
    }

    /** True when nothing solid lies between the two points. */
    private static boolean sees(ServerPlayer p, Vec3 a, Vec3 b) {
        return p.level().clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS;
    }

    private static List<LivingEntity> nearest(ServerPlayer p, Vec3 c, double r, int cap) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : GrimoireBook.around(p, c, r)) if (valid(t)) out.add(t);
        out.sort(Comparator.comparingDouble(t -> t.distanceToSqr(c)));
        return out.size() > cap ? new ArrayList<>(out.subList(0, cap)) : out;
    }

    // ---------------------------------------------------------------- starter

    /** Sniper Eye: an instant shot at the first target in 40 blocks; a moving target is read and takes 50% more. */
    static boolean sniper(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 40);
        if (t == null || !valid(t)) { GrimoireBook.fail(p, "No target in sight."); return false; }
        float raw = moving(t) ? 12f : 8f;
        b.castCircle(p, 0.6f);
        b.vfx(p, CAST, hand(p), mid(t), 8, 1f * GrimoireBook.size(i, p));
        b.hurt(i, p, t, mode, raw);
        slow(t, 40, 0);
        b.vfx(p, BURST, mid(t), mid(t), 20, 0.9f);
        return true;
    }

    // ---------------------------------------------------------------- mid

    /** Petrifying Gaze: the target stiffens like stone for up to 3 s (hard control, diminishing, shortened on bosses). */
    static boolean petrify(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 22);
        if (t == null || !valid(t)) { GrimoireBook.fail(p, "Nothing to petrify."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int hold = BalanceLaw.controlTicks(t, 70);
        b.castCircle(p, 0.8f);
        b.vfx(p, CAST, hand(p), mid(t), 12, 1.2f);
        b.hurt(i, p, t, mode, 3f);
        EnergyBridge.effect(t, "paralysis", hold, 0);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, hold + 40, 9));
        t.addEffect(new MobEffectInstance(MobEffects.JUMP, hold + 40, 250));
        t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, hold + 40, 3));
        t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
        t.hurtMarked = true;
        b.vfx(p, BURST, mid(t), mid(t), hold, Math.max(0.8f, t.getBbHeight() / 1.8f));
        return true;
    }

    /** Magicule Scan: eyes sweep 20 blocks around you; every foe glows through walls and loses a little of its magicules. */
    static boolean scan(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 20 * GrimoireBook.size(i, p);
        List<LivingEntity> found = nearest(p, p.position(), r, 16);
        if (found.isEmpty()) { GrimoireBook.fail(p, "The eyes find no one."); return false; }
        b.castCircle(p, 1f);
        b.vfx(p, ZONE, p.position(), p.position(), 30, (float) r);
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0));
        for (LivingEntity t : found) {
            glow(t, shorten(t, 200));
            Nullification.bleed(t, BalanceLaw.isBoss(t) ? 0.005 : 0.03);
            b.vfx(p, CAST, hand(p), mid(t), 8, 0.5f);
        }
        return true;
    }

    /** Eye Swap: you and the target trade places; it is dazzled for 2 s and slowed. Bosses cannot be swapped. */
    static boolean swap(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 20);
        if (t == null || !valid(t)) { GrimoireBook.fail(p, "No one to swap with."); return false; }
        if (BalanceLaw.isBoss(t)) { GrimoireBook.fail(p, "That presence cannot be swapped."); return false; }
        Vec3 a = p.position(), c = t.position();
        if (!p.level().noCollision(p, p.getBoundingBox().move(c.subtract(a))) || !t.level().noCollision(t, t.getBoundingBox().move(a.subtract(c)))) {
            GrimoireBook.fail(p, "No room to swap.");
            return false;
        }
        b.castCircle(p, 0.7f);
        b.vfx(p, CAST, a.add(0, 1.2, 0), c.add(0, 1.2, 0), 12, 1f);
        p.teleportTo(c.x, c.y, c.z);
        t.teleportTo(a.x, a.y, a.z);
        p.fallDistance = 0;
        t.fallDistance = 0;
        b.hurt(i, p, t, mode, 2f);
        t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
        slow(t, 60, 1);
        if (t instanceof Mob m) m.setTarget(null);
        b.vfx(p, BURST, c.add(0, 1, 0), c.add(0, 1, 0), 20, 0.9f);
        b.vfx(p, BURST, a.add(0, 1, 0), a.add(0, 1, 0), 20, 0.9f);
        return true;
    }

    /** Sight Share: allies within 16 blocks see through your eyes (night vision 60 s, speed 30 s); foes within 30 glow for 10 s. */
    static boolean sightShare(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1f);
        List<ServerPlayer> allies = p.serverLevel().getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(16),
                a -> a == p || (a.isAlive() && a.isAlliedTo(p)));
        int n = 0;
        for (ServerPlayer a : allies) {
            if (++n > 8) break;
            a.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0));
            a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600, 0));
            VfxSpawn.sendFollowing(p.serverLevel(), BURST, a, a.position().add(0, 1.6, 0), b.color, 30, 0.8f);
        }
        for (LivingEntity t : nearest(p, p.position(), 30, 16)) glow(t, 200);
        return true;
    }

    /** Guardian Eyes: for 10 s eyes circle you; enemy shots within 4 blocks are shot down, foes near you are slowed and you take 20% less. */
    static boolean guardians(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 200;
        double r = 4 * GrimoireBook.size(i, p);
        ServerLevel sl = p.serverLevel();
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(sl, ZONE, p, p.position(), b.color, ticks, (float) r);
        int[] left = {24};
        SpellRuntime.zone(sl, ticks, 2, age -> {
            if (!p.isAlive()) return;
            Vec3 c = p.position().add(0, 1, 0);
            for (Projectile pr : WikiSpells.enemyShots(p, p.getBoundingBox().inflate(r))) {
                if (left[0] <= 0 || pr.position().distanceToSqr(c) > r * r) continue;
                left[0]--;
                VfxSpawn.send(sl, CAST, c, pr.position(), b.color, 6, 0.5f);
                VfxSpawn.send(sl, BURST, pr.position(), pr.position(), b.color, 14, 0.4f);
                pr.discard();
            }
            if (age % 10 == 0) {
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 24, 0));
                for (LivingEntity t : nearest(p, p.position(), r, 8)) slow(t, 30, 1);
            }
        });
        return true;
    }

    /** Scouting Eyes: eyes are left at the aim point for 25 s; foes within 14 blocks of them glow and are weakened, allies there see in the dark. */
    static boolean scouts(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 30);
        double r = 14 * GrimoireBook.size(i, p);
        int ticks = 500;
        b.castCircle(p, 0.9f);
        b.vfx(p, CAST, hand(p), c, 14, 1f);
        b.vfx(p, ZONE, c, c, ticks, (float) r);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            if (!p.isAlive()) return;
            for (LivingEntity t : nearest(p, c, r, 12)) {
                glow(t, 30);
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, shorten(t, 30), 0));
            }
            for (ServerPlayer a : p.serverLevel().getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(c, c).inflate(r),
                    x -> x == p || (x.isAlive() && x.isAlliedTo(p)))) a.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0));
        });
        return true;
    }

    // ---------------------------------------------------------------- zone

    /** Jamming Gaze Field: eyes watch a circle for 10 s; everyone inside is slowed and hurt, and whoever the eyes can SEE is magic-jammed (break line of sight to avoid it). */
    static boolean jammingField(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 24);
        double r = 6 * GrimoireBook.size(i, p);
        int ticks = 200;
        b.castCircle(p, 1f);
        b.vfx(p, ZONE, c, c, ticks, (float) r);
        Vec3 eyes = c.add(0, 2.5, 0);
        SpellRuntime.zone(p.serverLevel(), ticks, 10, age -> {
            if (!p.isAlive()) return;
            for (LivingEntity t : nearest(p, c, r, 12)) {
                b.hurt(i, p, t, mode, 2f);
                slow(t, 30, 2);
                if (!sees(p, eyes, mid(t))) continue;
                EnergyBridge.effect(t, "silence", shorten(t, 30), 0);
                Nullification.interfere(t, 2, 0.35f, 0.25f);
                b.vfx(p, CAST, eyes, mid(t), 6, 0.5f);
            }
        });
        return true;
    }

    // ---------------------------------------------------------------- signature

    /** Catoblepas's Evil Eye: eyes spread over 20 blocks for 10 s; every second the 5 nearest foes are revealed, read and sniped, and on the last pulse they are petrified. */
    static boolean evilEye(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 20 * GrimoireBook.size(i, p);
        if (nearest(p, p.position(), r, 1).isEmpty()) { GrimoireBook.fail(p, "No one for the eyes to watch."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int pulses = 10;
        ServerLevel sl = p.serverLevel();
        b.castCircle(p, 1.4f);
        b.vfx(p, ZONE, p.position(), p.position(), pulses * 20 + 10, (float) r);
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, pulses * 20 + 40, 0));
        int[] n = {0};
        SpellRuntime.zone(sl, pulses * 20, 20, age -> {
            if (!p.isAlive() || n[0] >= pulses) return;
            n[0]++;
            boolean last = n[0] == pulses;
            for (LivingEntity t : nearest(p, p.position(), r, 5)) {
                glow(t, 60);
                b.vfx(p, CAST, hand(p), mid(t), 6, 0.8f);
                b.hurt(i, p, t, mode, moving(t) ? 7.5f : 5f);
                slow(t, 40, 1);
                Nullification.bleed(t, BalanceLaw.isBoss(t) ? 0.002 : 0.01);
                if (last) {
                    int hold = BalanceLaw.controlTicks(t, 50);
                    EnergyBridge.effect(t, "paralysis", hold, 0);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, hold, 9));
                    b.vfx(p, BURST, mid(t), mid(t), 28, Math.max(1f, t.getBbHeight() / 1.5f));
                } else {
                    b.vfx(p, BURST, mid(t), mid(t), 14, 0.6f);
                }
            }
        });
        return true;
    }

    // ---------------------------------------------------------------- daily

    /** Swarm of Eyes: for 12 s a swarm of small eyes hunts around you; every fifth of a second one picks a foe within 22 blocks (3 damage, slow). */
    static boolean swarm(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        int ticks = 240;
        double r = 22 * GrimoireBook.size(i, p);
        if (nearest(p, p.position(), r, 1).isEmpty()) { GrimoireBook.fail(p, "No prey for the swarm."); return false; }
        b.castCircle(p, 1.4f);
        VfxSpawn.sendFollowing(p.serverLevel(), ZONE, p, p.position(), b.color, ticks, 5f);
        SpellRuntime.zone(p.serverLevel(), ticks, 4, age -> {
            if (!p.isAlive()) return;
            List<LivingEntity> prey = nearest(p, p.position(), r, 10);
            if (prey.isEmpty()) return;
            LivingEntity t = prey.get(p.getRandom().nextInt(prey.size()));
            Vec3 from = p.position().add(p.getRandom().nextDouble() * 4 - 2, 1.5 + p.getRandom().nextDouble(), p.getRandom().nextDouble() * 4 - 2);
            VfxSpawn.send(p.serverLevel(), CAST, from, mid(t), b.color, 8, 0.5f);
            b.hurt(i, p, t, mode, 3f);
            slow(t, 30, 0);
            VfxSpawn.send(p.serverLevel(), BURST, mid(t), mid(t), b.color, 12, 0.35f);
        });
        return true;
    }
}
