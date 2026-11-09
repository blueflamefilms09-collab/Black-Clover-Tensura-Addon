package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxSlot;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.song.SongTargets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Why: four more Saiyan techniques (3.71.0), next to everything else (nothing existing is changed): DRAGON FIST (an awakened Saiyan with enough martial training: a golden dragon rises round a foe in front and
 * the last blow throws it up), GOD BIND (Son Goku: holds one foe still for a few seconds, a boss or a player for less), KI DEFLECTION (an awakened Saiyan: for a few seconds hostile projectiles that come near are turned
 * back) and ENERGY TRANSFER (an awakened Saiyan: gives Ki to an allied player nearby, with a loss and a cap). Everything is decided here on the server; the client only asks (see {@link SaiyanNet}) and no new packet
 * exists. State lives in the {@link State} of the player's run (dropped on death, logout and change of dimension). Cost: no entity is ever spawned; a Dragon Fist is at most {@link SaiyanTech3Rules#MAX_DRAGON_STEPS} steps two
 * ticks apart (about 10 particles each, one UUID lookup each, one blow); a bind is one UUID lookup and two velocity writes a tick on ONE foe, and one particle ring every half second; a deflection is one projectile query every
 * second tick while its window is open, at most {@link SaiyanTech3Rules#MAX_DEFLECTS} handled; an Energy Transfer is one player query once per use.
 */
final class SaiyanTechniques3 {
    private SaiyanTechniques3() {}

    private static final String TAG_TURNED = "nusmp_ki_turned";
    private static final int GOLD = 0xFFC832;

    /** Per-player technique state (not saved). */
    static final class State {
        long deflectUntil;
        UUID bindTarget;
        long bindUntil;
        UUID dragonTarget;
        int dragonStep, dragonTotal;
        long dragonNext;

        /** True while any of the timed parts runs (so the per-tick call is one comparison when nothing does). */
        boolean busy() { return deflectUntil > 0 || bindTarget != null || dragonTarget != null; }

        void clear() {
            deflectUntil = 0;
            bindTarget = null;
            dragonTarget = null;
            dragonStep = 0;
        }
    }

    /** Stops everything that is running (the system was switched off, the race was taken away). */
    static void cancel(SaiyanManager.Run r) { r.tech3.clear(); r.dirty = true; }

    private static boolean open(ServerPlayer p, SaiyanState s, boolean goku) {
        if (!SaiyanConfig.tech3Enabled() || p.isSpectator() || !p.isAlive()) return false;
        if (!s.awakened) {
            SaiyanAbilities.say(p, "not_awakened");
            return false;
        }
        if (goku && (!SaiyanConfig.gokuEnabled() || !GokuAbilities.isGoku(s))) {
            SaiyanAbilities.say(p, "goku_locked");
            return false;
        }
        return true;
    }

    /** The nearest foe in front of the Saiyan inside {@code range}: one query, at most {@link SaiyanAbilities#MAX_CANDIDATES} looked at. */
    private static LivingEntity lockFoe(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        List<LivingEntity> near = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye.add(look.scale(range))).inflate(2.5), e -> e != p && e.isAlive() && !(e instanceof ArmorStand));
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        int seen = 0;
        for (LivingEntity t : near) {
            if (++seen > SaiyanAbilities.MAX_CANDIDATES) break;
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist > range || (dist > 1.0e-6 && to.scale(1.0 / dist).dot(look) < 0.7) || !SaiyanAbilities.allowed(p, t)) continue;
            if (dist < bestD) {
                bestD = dist;
                best = t;
            }
        }
        return best;
    }

    // ================================================================== Dragon Fist
    static boolean dragon(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech3;
        if (t.dragonTarget != null || !open(p, s, false) || !s.ready("dragon", now)) return false;
        if (!SaiyanTech3Rules.dragonUnlocked(s.trained(SaiyanTraining.Cat.MARTIAL), SaiyanConfig.dragonUnlock())) {
            SaiyanAbilities.say(p, "dragon_locked");
            return false;
        }
        LivingEntity target = lockFoe(p, SaiyanConfig.dragonRange());
        if (target == null) {
            SaiyanAbilities.say(p, "dragon_none");
            return false;
        }
        if (!SaiyanAbilities.pay(p, s, SaiyanConfig.dragonStamina(), SaiyanConfig.dragonCost())) return false;
        t.dragonTarget = target.getUUID();
        t.dragonTotal = SaiyanTech3Rules.dragonSteps(SaiyanConfig.dragonSteps());
        t.dragonStep = 0;
        t.dragonNext = now;
        s.startCooldown("dragon", now, SaiyanConfig.dragonCooldown());
        s.use("dragon", 1);
        r.lastCombat = now;
        r.dirty = true;
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.6f, 1.4f);
        SaiyanAbilities.say(p, "dragon_start");
        return true;
    }

    /** One step: a ring of the dragon's body round the foe, higher each time, the foe lifted a little; the last step is the blow. */
    private static void dragonTick(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech3;
        if (now < t.dragonNext) return;
        ServerLevel level = p.serverLevel();
        if (!SaiyanConfig.enabled() || !SaiyanConfig.tech3Enabled() || !p.isAlive() || p.isSpectator() || !(level.getEntity(t.dragonTarget) instanceof LivingEntity e) || !e.isAlive()
                || !SaiyanAbilities.allowed(p, e) || p.distanceTo(e) > SaiyanConfig.dragonRange() + 3.0) {
            t.dragonTarget = null;
            return;
        }
        int i = t.dragonStep, n = t.dragonTotal;
        double h = SaiyanTech3Rules.dragonHeight(i, n, e.getBbHeight() + 1.5);
        double a = i * 1.7;                                                             // the body winds round the foe
        for (int k = 0; k < 5; k++) {
            double ang = a + k * 0.5, y = h - k * 0.35;
            if (y < 0.0) continue;
            Fx.sprayTinted(level, FxThemes.WORLD, FxSlot.MOTE, GOLD, k == 0 ? 2.6f : 1.6f, e.getX() + Math.cos(ang) * 0.9, e.getY() + y, e.getZ() + Math.sin(ang) * 0.9, 2, 0.05, 0.05, 0.05, 0.0);
        }
        Vec3 dm = e.getDeltaMovement();
        if (!BalanceLaw.isBoss(e)) {
            e.setDeltaMovement(dm.x * 0.3, Math.max(dm.y, 0.12), dm.z * 0.3);
            if (e instanceof ServerPlayer) e.hurtMarked = true;
        }
        if (i >= n - 1) {
            double growth = SaiyanRules.growth(s.battlePower);
            double raw = SaiyanConfig.dragonDamage() * SaiyanTech3Rules.dragonStepFactor(i, n)
                    * SaiyanRules.damageFactor(SaiyanConfig.bonuses(s.battlePower).strength()) * SaiyanFormEngine.damageFactor(s, r);
            if (SaiyanAbilities.hurtBlow(p, e, raw, growth) > 0f && !BalanceLaw.isBoss(e)) {
                e.setDeltaMovement(e.getDeltaMovement().add(0.0, 0.9, 0.0));
                if (e instanceof ServerPlayer) e.hurtMarked = true;
            }
            SaiyanCombat.trainOn(s, e, SaiyanTraining.Cat.MARTIAL, now);
            Fx.sprayTinted(level, FxThemes.WORLD, FxSlot.SPARK, GOLD, 2.2f, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 24, 0.6, 0.8, 0.6, 0.25);
            Fx.spray(level, FxThemes.WORLD, FxOld.SONIC_BOOM, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9f, 1.1f);
            r.lastCombat = now;
            t.dragonTarget = null;
            return;
        }
        t.dragonStep++;
        t.dragonNext = now + 2;
    }

    // ================================================================== God Bind
    static boolean bind(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech3;
        if (!open(p, s, true) || !s.ready("bind", now)) return false;
        LivingEntity target = lockFoe(p, SaiyanConfig.bindRange());
        if (target == null) {
            SaiyanAbilities.say(p, "bind_none");
            return false;
        }
        int ticks = SaiyanTech3Rules.bindTicks(SaiyanConfig.bindSeconds(), BalanceLaw.isBoss(target), target instanceof Player, SaiyanConfig.bindBossShare(), SaiyanConfig.bindPlayerShare());
        if (ticks <= 0) {
            SaiyanAbilities.say(p, "bind_free");
            return false;
        }
        if (!SaiyanKi.pay(p, SaiyanConfig.bindCost())) {
            SaiyanAbilities.say(p, "no_ki");
            return false;
        }
        t.bindTarget = target.getUUID();
        t.bindUntil = now + ticks;
        s.startCooldown("bind", now, SaiyanConfig.bindCooldown());
        s.use("bind", 1);
        r.lastCombat = now;
        r.dirty = true;
        ring(p.serverLevel(), target);
        p.serverLevel().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.8f, 0.6f);
        SaiyanAbilities.say(p, "bind_on");
        return true;
    }

    private static void ring(ServerLevel level, LivingEntity e) {
        for (int k = 0; k < 4; k++) {
            double ang = k * Math.PI / 2.0;
            Fx.sprayTinted(level, FxThemes.WORLD, FxSlot.MOTE, GOLD, 1.6f, e.getX() + Math.cos(ang) * 0.7, e.getY() + e.getBbHeight() * 0.5, e.getZ() + Math.sin(ang) * 0.7, 1, 0.05, 0.4, 0.05, 0.0);
        }
    }

    /** One tick of the hold on the one bound foe (a velocity write, and the slowness refreshed twice a second); ends with the time, the foe's death, or the binder's. */
    private static void bindTick(ServerPlayer p, SaiyanManager.Run r, long now) {
        State t = r.tech3;
        ServerLevel level = p.serverLevel();
        Entity ent = level.getEntity(t.bindTarget);
        if (!SaiyanConfig.tech3Enabled() || !p.isAlive() || !(ent instanceof LivingEntity e) || !e.isAlive() || !SaiyanTech3Rules.bindHolds(now, t.bindUntil)) {
            if (ent instanceof LivingEntity e && e.isAlive() && p.isAlive()) SaiyanAbilities.say(p, "bind_free");
            t.bindTarget = null;
            return;
        }
        Vec3 dm = e.getDeltaMovement();
        e.setDeltaMovement(0.0, Math.min(dm.y, 0.0), 0.0);
        if (e instanceof ServerPlayer) e.hurtMarked = true;
        if ((now % 10L) == 0L) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 9, false, false, false));
        if ((now % 10L) == 5L) ring(level, e);
    }

    // ================================================================== Ki Deflection
    static boolean deflect(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech3;
        if (t.deflectUntil > now || !open(p, s, false) || !s.ready("deflect", now)) return false;
        if (!SaiyanKi.pay(p, SaiyanConfig.deflectCost())) {
            SaiyanAbilities.say(p, "no_ki");
            return false;
        }
        t.deflectUntil = now + 20L * SaiyanConfig.deflectSeconds();
        s.startCooldown("deflect", now, SaiyanConfig.deflectCooldown());
        s.use("deflect", 1);
        r.dirty = true;
        ServerLevel level = p.serverLevel();
        Fx.sprayTinted(level, FxThemes.WORLD, FxSlot.MOTE, 0xA0E0FF, 1.6f, p.getX(), p.getY() + 1.0, p.getZ(), 10, 0.6, 0.8, 0.6, 0.02);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.4f);
        SaiyanAbilities.say(p, "deflect_on");
        return true;
    }

    /** Every second tick while the window is open: one projectile query, at most {@link SaiyanTech3Rules#MAX_DEFLECTS} turned. A projectile is turned once (it is marked), only if it is fast, and only if its
     *  shooter may be hit by the Saiyan at all (a player's arrow is not turned against that player where the server forbids it); it is sent back at the shooter, or along the Saiyan's look if the shooter is gone. */
    private static void deflectTick(ServerPlayer p, SaiyanManager.Run r, long now) {
        State t = r.tech3;
        if (now >= t.deflectUntil) {
            t.deflectUntil = 0;
            SaiyanAbilities.say(p, "deflect_off");
            return;
        }
        if (!SaiyanTech3Rules.pulseTick(now)) return;
        double rad = SaiyanConfig.deflectRadius();
        ServerLevel level = p.serverLevel();
        List<Projectile> near = level.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(rad), e -> e.isAlive() && e.getOwner() != p && e.getDeltaMovement().lengthSqr() > 0.04 && !e.getPersistentData().getBoolean(TAG_TURNED));
        int done = 0, seen = 0;
        for (Projectile pr : near) {
            if (++seen > SaiyanAbilities.MAX_CANDIDATES || done >= SaiyanTech3Rules.MAX_DEFLECTS) break;
            Entity owner = pr.getOwner();
            Vec3 dir;
            if (owner instanceof LivingEntity o && o.isAlive()) {
                if (o instanceof Player op && SongTargets.isAlly(p, op)) continue;
                if (!SaiyanAbilities.allowed(p, o)) continue;
                dir = o.getBoundingBox().getCenter().subtract(pr.position());
                if (dir.lengthSqr() < 1.0e-6) dir = p.getLookAngle();
            } else {
                dir = p.getLookAngle();
            }
            double speed = SaiyanTech3Rules.deflectSpeed(pr.getDeltaMovement().length(), SaiyanConfig.deflectBoost());
            pr.setDeltaMovement(dir.normalize().scale(speed));
            pr.setOwner(p);
            pr.getPersistentData().putBoolean(TAG_TURNED, true);
            pr.hasImpulse = true;
            pr.hurtMarked = true;
            Fx.spray(level, FxThemes.WORLD, FxOld.ELECTRIC_SPARK, pr.getX(), pr.getY(), pr.getZ(), 3, 0.1, 0.1, 0.1, 0.1);
            done++;
        }
        if (done > 0) level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 1.5f);
    }

    // ================================================================== Energy Transfer
    static boolean energy(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        if (!open(p, s, false) || !s.ready("energy", now)) return false;
        Player ally = nearestAlly(p, SaiyanConfig.energyRange());
        if (!(ally instanceof ServerPlayer to)) {
            SaiyanAbilities.say(p, "energy_none");
            return false;
        }
        double give = SaiyanTech3Rules.receivedPercent(SaiyanKi.max(p), SaiyanConfig.energyCost(), 1.0 - SaiyanConfig.energyLoss(), SaiyanKi.max(to), SaiyanConfig.energyCap());
        if (!(give > 0.0)) {
            SaiyanAbilities.say(p, "energy_none");
            return false;
        }
        if (!SaiyanKi.pay(p, SaiyanConfig.energyCost())) {
            SaiyanAbilities.say(p, "no_ki");
            return false;
        }
        SaiyanKi.regen(to, give);
        s.startCooldown("energy", now, SaiyanConfig.energyCooldown());
        s.use("energy", 1);
        ServerLevel level = p.serverLevel();
        Vec3 a = p.position().add(0.0, 1.2, 0.0), b = to.position().add(0.0, 1.2, 0.0);
        for (int k = 1; k <= 6; k++) {
            Vec3 q = a.lerp(b, k / 7.0);
            Fx.sprayTinted(level, FxThemes.WORLD, FxSlot.MOTE, 0x80E0FF, 1.4f, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        level.playSound(null, to.getX(), to.getY(), to.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 1.6f);
        SaiyanAbilities.say(p, "energy_sent");
        SaiyanAbilities.say(to, "energy_got");
        return true;
    }

    /** The nearest allied player inside {@code range} (one query, at most {@link SaiyanAbilities#MAX_CANDIDATES} looked at); null if none. */
    private static net.minecraft.world.entity.player.Player nearestAlly(ServerPlayer p, double range) {
        List<Player> near = p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(range), e -> e != p && e.isAlive() && !e.isSpectator() && SongTargets.isAlly(p, e));
        Player best = null;
        double bestD = Double.MAX_VALUE;
        int seen = 0;
        for (Player o : near) {
            if (++seen > SaiyanAbilities.MAX_CANDIDATES) break;
            double d = o.distanceToSqr(p);
            if (d <= range * range && d < bestD) {
                bestD = d;
                best = o;
            }
        }
        return best;
    }

    // ================================================================== every tick while something runs
    static void tick(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech3;
        if (t.deflectUntil > 0) deflectTick(p, r, now);
        if (t.bindTarget != null) bindTick(p, r, now);
        if (t.dragonTarget != null) dragonTick(p, s, r, now);
    }
}
