package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxSlot;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.song.SongTargets;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Why: four new Saiyan techniques (3.66.0), next to everything else (nothing existing is changed): KI BARRIER (any awakened Saiyan: a shield that soaks a pool of damage for a few seconds),
 * INSTANT TRANSMISSION (Son Goku: teleport to the creature you look at, or blink to where you look), SPIRIT BOMB (Son Goku: hold to gather, living friends nearby feed it, throw it where you look and it
 * lands after a travel time and blasts every foe in a radius) and METEOR COMBINATION (Son Goku: a rush of timed strikes on one foe that ends in a heavy blow). Everything is decided here on the server; the
 * client only asks (see {@link SaiyanNet}). State lives in the per-player {@link State} of the player's run (dropped on death, logout and change of dimension). Cost: no entity is ever spawned; a Spirit Bomb
 * gathers with one entity query a second (at most {@link SaiyanTechRules#MAX_SOURCES} counted) and 1 particle burst per half second, flies as at most 3 particles every 2 ticks and blasts with one query
 * (at most {@link SaiyanTechRules#MAX_BLAST_TARGETS} looked at, 30 particles); a transmission is at most 12 box checks; a Meteor Combination is at most 16 strikes 3 ticks apart (one UUID lookup each);
 * a barrier is two comparisons per blow. A teleport never goes to a player who is not an ally, never loads a chunk and always ends the fall damage of the arrival.
 */
final class SaiyanTechniques {
    private SaiyanTechniques() {}

    /** Per-player technique state (not saved). */
    static final class State {
        double barrier;
        long barrierUntil;
        boolean gathering;
        long gatherStart;
        int sources;
        double rateSum;
        int rateN;
        boolean bombFlying;
        Vec3 from, to;
        long launchAt, landAt;
        double bombDamage, bombRadius;
        ResourceKey<Level> dim;
        UUID meteorTarget;
        int meteorLeft, meteorTotal;
        long meteorNext;

        void clear() {
            barrier = 0.0;
            gathering = false;
            bombFlying = false;
            meteorLeft = 0;
            meteorTarget = null;
        }
    }

    private static final int SCAN_STEPS = 12;

    // ================================================================== the gate
    private static boolean open(ServerPlayer p, SaiyanState s, boolean goku) {
        if (!SaiyanConfig.techEnabled() || p.isSpectator() || !p.isAlive()) return false;
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

    /** Stops everything that is running (the system was switched off, the race was taken away). */
    static void cancel(SaiyanManager.Run r) { r.tech.clear(); r.dirty = true; }

    /** The gather / throw progress in percent for the HUD (the Kamehameha charge field is shared: only one of them can be held). */
    static int chargePercent(SaiyanManager.Run r, long now) {
        State t = r.tech;
        return t.gathering ? (int) Math.round(SaiyanTechRules.charge(now - t.gatherStart, SaiyanConfig.sbChargeSeconds(), avgRate(t)) * 100.0) : 0;
    }

    private static double avgRate(State t) { return t.rateN <= 0 ? 1.0 : t.rateSum / t.rateN; }

    // ================================================================== Ki Barrier
    static boolean barrier(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        if (!open(p, s, false) || !s.ready("barrier", now)) return false;
        if (!SaiyanKi.pay(p, SaiyanConfig.barrierCost())) {
            SaiyanAbilities.say(p, "no_ki");
            return false;
        }
        State t = r.tech;
        t.barrier = SaiyanTechRules.barrierPool(SaiyanConfig.barrierPool(), SaiyanRules.growth(s.battlePower));
        t.barrierUntil = now + 20L * SaiyanConfig.barrierSeconds();
        s.startCooldown("barrier", now, SaiyanConfig.barrierCooldown());
        s.use("barrier", 1);
        ServerLevel level = p.serverLevel();
        Fx.sprayTinted(level, FxThemes.WORLD, FxSlot.MOTE, 0x80D0FF, 1.4f, p.getX(), p.getY() + 1.0, p.getZ(), 14, 0.7, 0.9, 0.7, 0.0);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 0.7f);
        SaiyanAbilities.say(p, "barrier_on");
        return true;
    }

    /** A blow of {@code damage} reaches the Saiyan: what the barrier does not soak is returned. Called from the damage event, after the race's own resistances. */
    static float absorb(ServerPlayer v, SaiyanManager.Run r, float damage, long now) {
        State t = r.tech;
        if (t.barrier <= 0.0 || damage <= 0f) return damage;
        if (!SaiyanTechRules.barrierUp(t.barrier, now, t.barrierUntil)) {
            t.barrier = 0.0;
            return damage;
        }
        double through = SaiyanTechRules.throughBarrier(t.barrier, damage);
        t.barrier = SaiyanTechRules.poolAfter(t.barrier, damage);
        Fx.sprayTinted(v.serverLevel(), FxThemes.WORLD, FxSlot.MOTE, 0x80D0FF, 1.0f, v.getX(), v.getY() + 1.0, v.getZ(), 6, 0.5, 0.8, 0.5, 0.02);
        if (t.barrier <= 0.0) SaiyanAbilities.say(v, "barrier_break");
        return (float) through;
    }

    // ================================================================== Instant Transmission
    static boolean transmit(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        if (!open(p, s, true) || !s.ready("transmit", now)) return false;
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        double range = SaiyanConfig.itRange();
        Vec3 far = eye.add(look.scale(range));
        var block = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 end = block.getType() == HitResult.Type.MISS ? far : block.getLocation();
        LivingEntity target = lockOn(p, eye, end);
        Vec3 dest = target != null ? landNear(p, target) : blink(p, eye, look, end, SaiyanTechRules.blinkRange(range, SaiyanConfig.itBlink()));
        if (dest == null) {
            SaiyanAbilities.say(p, "it_blocked");
            return false;
        }
        if (!SaiyanKi.pay(p, SaiyanConfig.itCost())) {
            SaiyanAbilities.say(p, "no_ki");
            return false;
        }
        flash(level, p.position());
        moveTo(p, dest, r, now);
        flash(level, dest);
        s.startCooldown("transmit", now, SaiyanConfig.itCooldown());
        s.use("transmit", 1);
        SaiyanAbilities.say(p, target != null ? "it_lock" : "it_blink");
        return true;
    }

    private static void flash(ServerLevel level, Vec3 at) {
        Fx.sprayTinted(level, FxThemes.WORLD, FxSlot.SPARK, 0x80D0FF, 1.4f, at.x, at.y + 1.0, at.z, 10, 0.3, 0.8, 0.3, 0.1);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.6f);
    }

    /** Puts the Saiyan at {@code dest}: no fall damage for five seconds, the flight's last position moved with it (the move is not a speeding flight). */
    private static void moveTo(ServerPlayer p, Vec3 dest, SaiyanManager.Run r, long now) {
        if (p.connection != null) p.connection.teleport(dest.x, dest.y, dest.z, p.getYRot(), p.getXRot());
        else p.setPos(dest.x, dest.y, dest.z);
        p.fallDistance = 0f;
        p.setDeltaMovement(Vec3.ZERO);
        p.hurtMarked = true;
        r.lx = dest.x;
        r.ly = dest.y;
        r.lz = dest.z;
        r.safeFallUntil = now + 100;
        r.dirty = true;
    }

    /** The first creature the Saiyan looks at, along the line to {@code end}: a player only if an ally. One query, at most {@link SaiyanTechRules#MAX_SOURCES} * 3 looked at. */
    private static LivingEntity lockOn(ServerPlayer p, Vec3 eye, Vec3 end) {
        List<LivingEntity> near = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1.5), e -> e != p && e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand));
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        int seen = 0;
        for (LivingEntity t : near) {
            if (++seen > GokuAbilities.MAX_CANDIDATES) break;
            if (t instanceof Player o && !SongTargets.isAlly(p, o)) continue;
            Optional<Vec3> hit = t.getBoundingBox().inflate(0.6).clip(eye, end);
            if (hit.isEmpty()) continue;
            double d = hit.get().distanceToSqr(eye);
            if (d < bestD) {
                bestD = d;
                best = t;
            }
        }
        return best;
    }

    /** A free spot next to the target, on the Saiyan's side of it first, then round it (six places, one box check each); null if none is free. */
    private static Vec3 landNear(ServerPlayer p, LivingEntity t) {
        Vec3 d = p.position().subtract(t.position());
        d = new Vec3(d.x, 0.0, d.z);
        if (d.lengthSqr() < 1.0e-4) d = p.getLookAngle().scale(-1.0);
        d = new Vec3(d.x, 0.0, d.z).normalize();
        double dist = t.getBbWidth() * 0.5 + p.getBbWidth() * 0.5 + 0.8;
        double[] turns = {0.0, 1.05, -1.05, 2.1, -2.1, Math.PI};
        for (double a : turns) {
            double c = Math.cos(a), sn = Math.sin(a);
            Vec3 at = new Vec3(t.getX() + (d.x * c - d.z * sn) * dist, t.getY(), t.getZ() + (d.x * sn + d.z * c) * dist);
            if (fits(p, at)) return at;
        }
        return null;
    }

    /** Where the blink lands: as far along the line as the blocks and the range allow, backing off in two-block steps until the body fits. */
    private static Vec3 blink(ServerPlayer p, Vec3 eye, Vec3 look, Vec3 end, double maxDist) {
        double dist = Math.min(maxDist, eye.distanceTo(end) - 1.0);
        for (int i = 0; i < SCAN_STEPS && dist >= 2.0; i++, dist -= 2.0) {
            Vec3 at = eye.add(look.scale(dist)).add(0.0, -p.getEyeHeight(), 0.0);
            if (fits(p, at)) return at;
        }
        return null;
    }

    /** True if the Saiyan's body fits at {@code at}: the chunk is loaded (nothing is ever loaded for a teleport), no block in the way and no lava or fire under or in it. */
    private static boolean fits(ServerPlayer p, Vec3 at) {
        ServerLevel level = p.serverLevel();
        BlockPos pos = BlockPos.containing(at);
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) return false;
        AABB box = p.getBoundingBox().move(at.x - p.getX(), at.y - p.getY(), at.z - p.getZ());
        if (!level.noCollision(p, box)) return false;
        return level.getFluidState(pos).isEmpty() && level.getFluidState(pos.below()).isEmpty() && !level.getBlockState(pos).is(net.minecraft.tags.BlockTags.FIRE);
    }

    // ================================================================== Spirit Bomb
    static boolean bombStart(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech;
        if (t.gathering || t.bombFlying || !open(p, s, true) || !s.ready("spirit", now)) return false;
        if (!SaiyanKi.canPay(p, SaiyanConfig.sbChargeCost())) {
            SaiyanAbilities.say(p, "no_ki");
            return false;
        }
        t.gathering = true;
        t.gatherStart = now;
        t.sources = 0;
        t.rateSum = 0.0;
        t.rateN = 0;
        r.dirty = true;
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7f, 0.6f);
        SaiyanAbilities.say(p, "sb_start");
        return true;
    }

    /** Every tick while gathering: Ki per second, one friend count a second, the glow above the head, and a safety release five seconds after a full gather. */
    static void gatherTick(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech;
        if (!t.gathering) return;
        if (!SaiyanConfig.enabled() || !SaiyanConfig.techEnabled() || !s.awakened || p.isSpectator() || !p.isAlive()) {
            cancel(r);
            return;
        }
        long held = now - t.gatherStart;
        if (held > 0 && held % 20 == 0) {
            if (!SaiyanKi.pay(p, SaiyanConfig.sbChargeCost())) {
                cancel(r);
                SaiyanAbilities.say(p, "no_ki");
                return;
            }
            t.sources = SaiyanTechRules.sources(countFriends(p));
            t.rateSum += SaiyanTechRules.gatherRate(t.sources);
            t.rateN++;
        }
        if (held % 10 == 0) {
            double c = SaiyanTechRules.charge(held, SaiyanConfig.sbChargeSeconds(), avgRate(t));
            ServerLevel level = p.serverLevel();
            Fx.sprayTinted(level, FxThemes.WORLD, FxSlot.MOTE, 0x80C8FF, (float) (1.0 + 2.0 * c), p.getX(), p.getY() + 2.6, p.getZ(), 2, 0.25, 0.25, 0.25, 0.0);
            if (t.sources > 0) Fx.sprayTinted(level, FxThemes.WORLD, FxSlot.MOTE, 0xC0F0FF, 0.8f, p.getX(), p.getY() + 1.5, p.getZ(), 2, 3.0, 1.0, 3.0, 0.02);
        }
        if (held >= (long) (SaiyanConfig.sbChargeSeconds() * 20.0) + 100L) bombRelease(p, s, r, now);
    }

    /** One query: the living friends within the feed radius (players only if allies; hostile creatures, bosses and armour stands never feed it). Bounded by {@link SaiyanTechRules#MAX_SOURCES}. */
    static int countFriends(ServerPlayer p) {
        double rad = SaiyanConfig.sbFeedRadius();
        List<LivingEntity> near = p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(rad, rad * 0.5, rad),
                e -> e != p && e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand) && !(e instanceof Enemy) && !BalanceLaw.isBoss(e)
                        && (!(e instanceof Player o) || SongTargets.isAlly(p, o)));
        return Math.min(near.size(), SaiyanTechRules.MAX_SOURCES);
    }

    /** The throw: what was gathered flies to where the Saiyan looks and lands after its travel time (a throw that came too soon fizzles). */
    static boolean bombRelease(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech;
        if (!t.gathering) return false;
        t.gathering = false;
        r.dirty = true;
        long held = now - t.gatherStart;
        if (held < SaiyanConfig.sbMinTicks()) {
            SaiyanAbilities.say(p, "sb_fizzle");
            return true;
        }
        double charge = SaiyanTechRules.charge(held, SaiyanConfig.sbChargeSeconds(), avgRate(t));
        if (!SaiyanKi.pay(p, GokuRules.kiPercent(SaiyanConfig.sbCost(), charge))) {
            SaiyanAbilities.say(p, "no_ki");
            return true;
        }
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        double range = SaiyanConfig.sbRange();
        Vec3 far = eye.add(look.scale(range));
        var block = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 to = block.getType() == HitResult.Type.MISS ? far : block.getLocation();
        t.from = p.position().add(0.0, p.getBbHeight() + 0.8, 0.0);
        t.to = to;
        t.launchAt = now;
        t.landAt = now + SaiyanTechRules.travelTicks(t.from.distanceTo(to), SaiyanConfig.sbTravelMax());
        double growth = SaiyanRules.growth(s.battlePower);
        t.bombDamage = SaiyanTechRules.bombDamage(SaiyanConfig.sbDamage(), charge, t.sources) * (1.0 + SaiyanConfig.kiDamageGrowth() * growth);
        t.bombRadius = SaiyanTechRules.bombRadius(SaiyanConfig.sbRadius(), charge);
        t.dim = level.dimension();
        t.bombFlying = true;
        s.startCooldown("spirit", now, SaiyanConfig.sbCooldown());
        s.use("spirit", 1);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 0.5f);
        SaiyanAbilities.say(p, "sb_throw");
        return true;
    }

    /** Every tick while a bomb is in the air: a few particles every second tick, and the blast when it lands. A bomb whose thrower died or left the dimension is gone. */
    static void flightTick(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech;
        if (!t.bombFlying) return;
        ServerLevel level = p.serverLevel();
        if (!p.isAlive() || t.dim == null || !t.dim.equals(level.dimension()) || t.from == null || t.to == null) {
            t.bombFlying = false;
            return;
        }
        double span = Math.max(1, t.landAt - t.launchAt), u = Math.min(1.0, (now - t.launchAt) / span);
        Vec3 at = t.from.lerp(t.to, u);
        if (((now - t.launchAt) & 1L) == 0L) Fx.sprayTinted(level, FxThemes.WORLD, FxSlot.MOTE, 0x80C8FF, 3.0f, at.x, at.y, at.z, 3, 0.35, 0.35, 0.35, 0.0);
        if (now >= t.landAt) {
            t.bombFlying = false;
            explode(p, s, r, t, now);
        }
    }

    private static void explode(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, State t, long now) {
        ServerLevel level = p.serverLevel();
        Vec3 c = t.to;
        double rad = t.bombRadius, growth = SaiyanRules.growth(s.battlePower);
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(rad), e -> e != p && e.isAlive());
        int seen = 0;
        for (LivingEntity e : near) {
            if (++seen > SaiyanTechRules.MAX_BLAST_TARGETS) break;
            double d = e.getBoundingBox().getCenter().distanceTo(c);
            if (d > rad + e.getBbWidth() * 0.5 || !SaiyanAbilities.allowed(p, e)) continue;
            double falloff = 1.0 - 0.5 * Math.min(1.0, d / rad);
            if (SaiyanAbilities.hurt(p, e, t.bombDamage * falloff, growth) > 0f && !BalanceLaw.isBoss(e)) {
                Vec3 away = e.position().subtract(c);
                if (away.lengthSqr() > 1.0e-6) e.knockback(0.8 * falloff, -away.x, -away.z);
            }
            SaiyanCombat.trainOn(s, e, SaiyanTraining.Cat.KI, now);
        }
        Fx.spray(level, FxThemes.WORLD, FxOld.FLASH, c.x, c.y, c.z, 1, 0.0, 0.0, 0.0, 0.0);
        Fx.sprayTinted(level, FxThemes.WORLD, FxSlot.MOTE, 0x80C8FF, 3.0f, c.x, c.y + 0.5, c.z, 20, rad * 0.4, rad * 0.3, rad * 0.4, 0.05);
        Fx.spray(level, FxThemes.WORLD, FxOld.ELECTRIC_SPARK, c.x, c.y + 0.5, c.z, 10, rad * 0.3, rad * 0.2, rad * 0.3, 0.1);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5f, 0.6f);
    }

    // ================================================================== Meteor Combination
    static boolean meteor(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech;
        if (t.meteorLeft > 0 || !open(p, s, true) || !s.ready("meteor", now)) return false;
        LivingEntity target = lockFoe(p, SaiyanConfig.meteorRange());
        if (target == null) {
            SaiyanAbilities.say(p, "meteor_none");
            return false;
        }
        if (!SaiyanAbilities.pay(p, s, SaiyanConfig.meteorStamina(), SaiyanConfig.meteorCost())) return false;
        t.meteorTarget = target.getUUID();
        t.meteorTotal = SaiyanTechRules.meteorHits(SaiyanConfig.meteorHits());
        t.meteorLeft = t.meteorTotal;
        t.meteorNext = now;
        s.startCooldown("meteor", now, SaiyanConfig.meteorCooldown());
        s.use("meteor", 1);
        r.lastCombat = now;
        r.dirty = true;
        SaiyanAbilities.say(p, "meteor_start");
        return true;
    }

    /** The nearest foe in front of the Saiyan inside the range (one query, at most {@link GokuAbilities#MAX_CANDIDATES} looked at). */
    private static LivingEntity lockFoe(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        List<LivingEntity> near = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye.add(look.scale(range))).inflate(3.0), e -> e != p && e.isAlive());
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        int seen = 0;
        for (LivingEntity t : near) {
            if (++seen > GokuAbilities.MAX_CANDIDATES) break;
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

    /** Every tick while the rush runs: the next strike when its time has come (the Saiyan flashes next to a foe that moved away), the last one a heavy blow. */
    static void meteorTick(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech;
        if (t.meteorLeft <= 0 || now < t.meteorNext) return;
        ServerLevel level = p.serverLevel();
        if (!SaiyanConfig.enabled() || !SaiyanConfig.techEnabled() || !p.isAlive() || p.isSpectator() || !(level.getEntity(t.meteorTarget) instanceof LivingEntity e) || !e.isAlive() || !SaiyanAbilities.allowed(p, e)) {
            t.meteorLeft = 0;
            t.meteorTarget = null;
            return;
        }
        if (!SaiyanTechRules.inReach(p.distanceTo(e), 4.0)) {
            Vec3 at = landNear(p, e);
            if (at == null || p.distanceTo(e) > SaiyanConfig.meteorRange() + 8.0) {
                t.meteorLeft = 0;
                t.meteorTarget = null;
                return;
            }
            flash(level, p.position());
            moveTo(p, at, r, now);
        }
        int i = t.meteorTotal - t.meteorLeft;
        double growth = SaiyanRules.growth(s.battlePower);
        double raw = SaiyanConfig.meteorDamage() * SaiyanTechRules.meteorHitFactor(i, t.meteorTotal)
                * SaiyanRules.damageFactor(SaiyanConfig.bonuses(s.battlePower).strength()) * SaiyanFormEngine.damageFactor(s, r);
        boolean last = t.meteorLeft == 1;
        Vec3 dir = e.position().subtract(p.position());
        if (SaiyanAbilities.hurtBlow(p, e, raw, growth) > 0f && !BalanceLaw.isBoss(e) && dir.lengthSqr() > 1.0e-6) e.knockback(last ? 1.2 : 0.15, -dir.x, -dir.z);
        SaiyanCombat.trainOn(s, e, SaiyanTraining.Cat.MARTIAL, now);
        Fx.spray(level, FxThemes.WORLD, last ? FxOld.SONIC_BOOM : FxOld.CRIT, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), last ? 1 : 4, 0.3, 0.3, 0.3, 0.1);
        level.playSound(null, e.getX(), e.getY(), e.getZ(), last ? SoundEvents.GENERIC_EXPLODE.value() : SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, last ? 0.9f : 0.5f, 0.9f + 0.05f * i);
        r.lastCombat = now;
        t.meteorLeft--;
        t.meteorNext = now + SaiyanConfig.meteorInterval();
        if (t.meteorLeft <= 0) t.meteorTarget = null;
    }

    // ================================================================== once a second
    /** Forgets a barrier whose time is up and says so. */
    static void second(ServerPlayer p, SaiyanManager.Run r, long now) {
        State t = r.tech;
        if (t.barrier > 0.0 && now >= t.barrierUntil) {
            t.barrier = 0.0;
            SaiyanAbilities.say(p, "barrier_down");
        }
    }
}
