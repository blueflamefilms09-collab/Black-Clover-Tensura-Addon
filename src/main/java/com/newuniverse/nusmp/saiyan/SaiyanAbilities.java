package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.avatar.FtbTeamHelper;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.perf.PerfParticles;
import com.newuniverse.nusmp.song.SongTargets;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Why: the first two Ki attacks of the Saiyan system (3.63.0, Stage 1), written so they cost nothing to spawn and nothing to clean up: a Ki Blast is ONE raycast (blocks stop it) and ONE entity query along
 * the line, the first foe on it takes the hit, and the beam is a line of at most {@link #MAX_TRAIL} budgeted particles. A Rapid Ki Barrage is up to eight of those, with the damage of each target added up
 * and dealt as ONE hit (so the hurt cooldown does not eat the volley). No entity is spawned and no block is touched, so there is nothing to protect or leave behind. Damage goes through the mod's balance law
 * (Existence Points of the caster, the PvP hit cap, the mob multiplier) as magic damage (an energy attack, not a blow); targets go through the same foe rule as the Beast Wills (not allies, not team mates,
 * players only if the server allows and the place can be interacted with). Costs: Ki is the magicule pool ({@link SaiyanKi}), stamina is the state's.
 */
final class SaiyanAbilities {
    private SaiyanAbilities() {}

    static final int MAX_TRAIL = 16, MAX_CANDIDATES = 64;

    /** May this creature be hit by the Saiyan's Ki at all (foes only, teams and the players setting respected, protected places respected)? */
    static boolean allowed(ServerPlayer p, LivingEntity t) {
        if (t == p || !t.isAlive() || t.isSpectator() || !SongTargets.isFoe(p, t)) return false;
        if (t instanceof Player o) {
            if (!SaiyanConfig.hitPlayers()) return false;
            if (FtbTeamHelper.sameTeam(p.getUUID(), o.getUUID())) return false;
            if (!p.serverLevel().mayInteract(p, o.blockPosition())) return false;
        }
        return true;
    }

    private static boolean pre(ServerPlayer p, SaiyanState s, long now, String kind) {
        if (!s.awakened) {
            say(p, "not_awakened");
            return false;
        }
        return s.ready(kind, now) && !p.isSpectator();
    }

    private static void say(ServerPlayer p, String key) { p.displayClientMessage(Component.translatable("nusmp.saiyan.msg." + key), true); }

    /** Pays the stamina and the Ki of an attack, or says why not (nothing is taken then). */
    private static boolean pay(ServerPlayer p, SaiyanState s, double stamina, double kiPercent) {
        if (!p.isCreative() && s.stamina < stamina) {
            say(p, "no_stamina");
            return false;
        }
        if (!SaiyanKi.pay(p, kiPercent)) {
            say(p, "no_ki");
            return false;
        }
        if (!p.isCreative()) s.stamina = SaiyanRules.stamina(s.stamina, -stamina, SaiyanManager.staminaMax(s));
        return true;
    }

    /** Where a ray ended and who it hit (null = nobody). */
    record Hit(LivingEntity target, Vec3 at) {}

    /** One ray: stopped by blocks, first foe on the line takes it. */
    static Hit ray(ServerPlayer p, Vec3 dir, double range) {
        ServerLevel level = p.serverLevel();
        Vec3 from = p.getEyePosition();
        Vec3 end = from.add(dir.scale(range));
        var block = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        LivingEntity best = null;
        Vec3 bestAt = stop;
        double bestD = Double.MAX_VALUE;
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, new AABB(from, stop).inflate(1.0), e -> e != p && e.isAlive());
        int seen = 0;
        for (LivingEntity t : near) {
            if (++seen > MAX_CANDIDATES) break;
            Optional<Vec3> hit = t.getBoundingBox().inflate(0.3).clip(from, stop);
            if (hit.isEmpty()) continue;
            double d = from.distanceToSqr(hit.get());
            if (d < bestD && allowed(p, t)) {
                bestD = d;
                best = t;
                bestAt = hit.get();
            }
        }
        trail(level, from.add(dir.scale(0.8)).add(0, -0.25, 0), bestAt);
        return new Hit(best, bestAt);
    }

    /** The beam: a short line of budgeted particles, and a small burst where it ended. */
    private static void trail(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        int n = (int) Math.min(MAX_TRAIL, Math.max(2, len / 1.5));
        for (int i = 1; i <= n; i++) {
            Vec3 q = from.add(d.scale(i / (double) n));
            PerfParticles.send(level, ParticleTypes.END_ROD, q.x, q.y, q.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
        PerfParticles.send(level, ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 5, 0.2, 0.2, 0.2, 0.05);
    }

    /** Deals {@code raw} through the balance law as magic damage; returns what was dealt. */
    private static float hurt(ServerPlayer p, LivingEntity t, double raw, double mastery) {
        float dmg = BalanceLaw.damage(p, t, (float) Math.max(0.0, raw), mastery);
        if (dmg <= 0.01f) return 0f;
        t.hurt(p.damageSources().indirectMagic(p, p), dmg);
        return dmg;
    }

    private static void sound(ServerPlayer p, float pitch) {
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.7f, pitch);
    }

    // ------------------------------------------------------------------ Ki Blast
    static boolean blast(ServerPlayer p, SaiyanState s, long now) {
        if (!pre(p, s, now, "blast") || !pay(p, s, SaiyanConfig.blastStamina(), SaiyanConfig.blastCost())) return false;
        s.startCooldown("blast", now, SaiyanConfig.blastCooldown());
        double mastery = SaiyanRules.growth(s.battlePower);
        Hit h = ray(p, p.getLookAngle(), SaiyanConfig.blastRange());
        if (h.target() != null) {
            double raw = SaiyanRules.kiDamage(SaiyanConfig.blastDamage(), SaiyanConfig.kiDamageGrowth(), s.battlePower);
            if (hurt(p, h.target(), raw, mastery) > 0f && !BalanceLaw.isBoss(h.target())) h.target().knockback(0.3, -p.getLookAngle().x, -p.getLookAngle().z);
        }
        sound(p, 1.6f);
        return true;
    }

    // ------------------------------------------------------------------ Rapid Ki Barrage
    static boolean barrage(ServerPlayer p, SaiyanState s, long now) {
        if (!pre(p, s, now, "barrage") || !pay(p, s, SaiyanConfig.barrageStamina(), SaiyanConfig.barrageCost())) return false;
        s.startCooldown("barrage", now, SaiyanConfig.barrageCooldown());
        double mastery = SaiyanRules.growth(s.battlePower);
        double each = SaiyanRules.kiDamage(SaiyanConfig.blastDamage(), SaiyanConfig.kiDamageGrowth(), s.battlePower) * SaiyanConfig.barrageShare();
        Vec3 look = p.getLookAngle();
        Map<LivingEntity, Double> sums = new LinkedHashMap<>();
        for (int i = 0; i < SaiyanConfig.barrageRays(); i++) {
            Vec3 dir = look.add(p.getRandom().nextGaussian() * 0.07, p.getRandom().nextGaussian() * 0.05, p.getRandom().nextGaussian() * 0.07).normalize();
            Hit h = ray(p, dir, SaiyanConfig.blastRange());
            if (h.target() != null) sums.merge(h.target(), each, Double::sum);
        }
        for (Map.Entry<LivingEntity, Double> e : new ArrayList<>(sums.entrySet())) hurt(p, e.getKey(), e.getValue(), mastery);
        sound(p, 1.9f);
        return true;
    }
}
