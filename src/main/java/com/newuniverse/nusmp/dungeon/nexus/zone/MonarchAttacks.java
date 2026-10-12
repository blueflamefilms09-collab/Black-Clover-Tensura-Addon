package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.dungeon.nexus.zone.MonarchFight.Hazard;
import com.newuniverse.nusmp.dungeon.wild.Wildlife;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Monarch's five moves and its roar between phases, as things set going in the world (docs/living_dungeon.md section 5, zone 1). Each is
 * a {@link Hazard}: it draws its own warning in particles and a shape of the mod's VFX for the whole wind-up, then lands once, and every one has
 * a counter: the Domain Slam is a ring on the floor that a jump clears, the Shadow Rain is circles that erupt a moment after they are marked,
 * Soul Rend is a line drawn before it is cut, the Authority is a field that can be left (or held back by a Mana Zone), and the soldiers are
 * real daemons that can be killed. Damage is a bounded share of the victim's health ({@link MonarchRules#hit}), spares anyone struck in the
 * last {@link MonarchRules#HIT_GRACE} ticks, and is dealt as magic from the Monarch so a death is credited to it. The numbers are in
 * {@link MonarchRules}, the geometry in {@link MonarchShapes}; this class is only the glue to the world.
 */
final class MonarchAttacks {
    private MonarchAttacks() {}

    private static final int VIOLET = 0xFFB026FF;
    private static final DustParticleOptions DUST_VIOLET = new DustParticleOptions(new Vector3f(0.62f, 0.12f, 0.95f), 1.5f);
    private static final DustParticleOptions DUST_RED = new DustParticleOptions(new Vector3f(1.0f, 0.1f, 0.18f), 1.6f);
    private static final DustParticleOptions DUST_WHITE = new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.95f), 1.2f);

    // ---------------------------------------------------------------- helpers
    /** Players that can be struck: alive, and neither spectating nor in creative mode (the Monarch and its soldiers never take them for a foe). */
    static List<ServerPlayer> foes(ServerLevel level) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players()) if (p.isAlive() && !p.isSpectator() && !p.isCreative()) out.add(p);
        return out;
    }

    static List<ServerPlayer> foesNear(ServerLevel level, double x, double z, double radius) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : foes(level)) if (MonarchShapes.inside(p.getX(), p.getZ(), x, z, radius)) out.add(p);
        return out;
    }

    /** The players in the hall of this fight: inside its round, from just under the floor to the dome. */
    static List<ServerPlayer> hallFoes(ServerLevel level, MonarchFight f) {
        List<ServerPlayer> out = new ArrayList<>();
        double lo = f.centre.getY() - 6.0, hi = f.centre.getY() + MonarchVaultPlan.HEIGHT + 4.0;
        for (ServerPlayer p : foesNear(level, f.centre.getX() + 0.5, f.centre.getZ() + 0.5, MonarchRules.ARENA_RADIUS)) if (p.getY() >= lo && p.getY() <= hi) out.add(p);
        return out;
    }

    /** The y of the floor under (x, z), looking from 3 above y down to 12 below it; y itself if there is no floor (or the chunk is not loaded: reading it would load it). */
    static double groundY(ServerLevel level, double x, double y, double z) {
        BlockPos base = BlockPos.containing(x, y, z);
        if (!level.hasChunk(base.getX() >> 4, base.getZ() >> 4)) return y;
        for (int dy = 3; dy >= -12; dy--) {
            BlockPos b = base.offset(0, dy, 0);
            if (level.getBlockState(b).isAir() && level.getBlockState(b.below()).blocksMotion()) return b.getY();
        }
        return y;
    }

    static void ring(ServerLevel level, ParticleOptions particle, double cx, double y, double cz, double radius, double spacing, double start) {
        int n = Math.max(12, Math.min(64, (int) (Math.PI * 2 * radius / spacing)));
        for (double[] p : MonarchShapes.ring(cx, cz, radius, n, start)) level.sendParticles(particle, p[0], y, p[1], 1, 0, 0, 0, 0);
    }

    static void line(ServerLevel level, ParticleOptions particle, double ax, double ay, double az, double bx, double bz, double spacing) {
        double len = Math.hypot(bx - ax, bz - az);
        int n = Math.max(2, Math.min(60, (int) (len / spacing)));
        for (int i = 1; i <= n; i++) {
            double t = (double) i / n;
            level.sendParticles(particle, ax + (bx - ax) * t, ay, az + (bz - az) * t, 1, 0, 0, 0, 0);
        }
    }

    static void sound(ServerLevel level, double x, double y, double z, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, BlockPos.containing(x, y, z), sound, SoundSource.HOSTILE, volume, pitch);
    }

    /** Tells the players round the Monarch what it is doing, on the action bar. */
    static void warn(ServerLevel level, Mob boss, String text) {
        Component c = Component.literal(text).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
        for (ServerPlayer p : foesNear(level, boss.getX(), boss.getZ(), MonarchRules.ARENA_RADIUS)) p.displayClientMessage(c, true);
    }

    /** A push away from (fromX, fromZ), the way a blow shoves; no damage. */
    static void shove(ServerPlayer p, double fromX, double fromZ, double power, double lift) {
        double dx = p.getX() - fromX, dz = p.getZ() - fromZ, d = Math.max(0.1, Math.hypot(dx, dz));
        p.push(dx / d * power, lift, dz / d * power);
        p.hurtMarked = true;
    }

    /** Strikes one player with a move, if they were not struck a moment ago. The damage is a share of their health, capped by the balance law. */
    static boolean strike(ServerLevel level, Mob boss, MonarchFight f, ServerPlayer p, MonarchMove move, long now) {
        Long last = f.lastHit.get(p.getUUID());
        if (last != null && !MonarchRules.mayHit(now, last)) return false;
        float amount = BalanceLaw.cap(p, MonarchRules.hit(MonarchRules.hitShare(move, f.phase), p.getMaxHealth()));
        if (amount <= 0f) return false;
        f.lastHit.put(p.getUUID(), now);
        p.hurt(level.damageSources().indirectMagic(boss, boss), amount);
        return true;
    }

    /** The soldiers of this fight that are still alive; the dead and the unloaded are dropped from its list. */
    static int alive(ServerLevel level, MonarchFight f) {
        Iterator<UUID> it = f.soldiers.iterator();
        while (it.hasNext()) {
            Entity e = level.getEntity(it.next());
            if (!(e instanceof Mob m) || !m.isAlive() || m.isRemoved()) it.remove();
        }
        return f.soldiers.size();
    }

    // ---------------------------------------------------------------- Domain Slam
    /** A ring marked on the floor for a second and a bit, then a shockwave that spreads from the Monarch; anyone standing on the floor when it passes is hurt, anyone in the air is not. */
    static Hazard slam(ServerLevel level, Mob boss, MonarchFight f, long now) {
        final double cx = boss.getX(), cz = boss.getZ(), cy = boss.getY();
        final double radius = MonarchRules.slamRadius(f.phase);
        final long release = now + MonarchRules.SLAM_WINDUP;
        VfxSpawn.send(level, VfxShape.DEVIL_CIRCLE, new Vec3(cx, cy + 0.1, cz), new Vec3(cx, cy + 1.0, cz), VIOLET, MonarchRules.SLAM_WINDUP, (float) (radius / 4.0));
        sound(level, cx, cy, cz, SoundEvents.WARDEN_NEARBY_CLOSE, 3.0f, 0.6f);
        return new Hazard() {
            final Set<UUID> struck = new HashSet<>();
            double prev;
            boolean loosed;

            @Override
            public boolean tick(ServerLevel l, Mob b, MonarchFight fight, long t) {
                if (t < release) {
                    if ((release - t) % 4 == 0) {
                        double shrink = (double) (release - t) / MonarchRules.SLAM_WINDUP;
                        ring(l, DUST_RED, cx, cy + 0.15, cz, radius, 0.9, 0.0);
                        ring(l, DUST_VIOLET, cx, cy + 0.15, cz, Math.max(1.0, radius * shrink), 1.2, t * 0.2);
                    }
                    return false;
                }
                if (!loosed) {
                    loosed = true;
                    sound(l, cx, cy, cz, SoundEvents.WARDEN_ATTACK_IMPACT, 4.0f, 0.5f);
                    l.sendParticles(ParticleTypes.EXPLOSION, cx, cy + 0.3, cz, 6, 1.5, 0.1, 1.5, 0.0);
                }
                int ticks = (int) (t - release) + 1;
                double front = MonarchShapes.front(ticks, MonarchRules.SLAM_SPEED);
                if (ticks % 2 == 0) ring(l, DUST_VIOLET, cx, cy + 0.2, cz, front, 0.8, 0.0);
                for (ServerPlayer p : foesNear(l, cx, cz, radius + MonarchRules.SLAM_BAND + 2.0)) {
                    if (struck.contains(p.getUUID())) continue;
                    double dist = Math.hypot(p.getX() - cx, p.getZ() - cz);
                    if (!MonarchShapes.passes(dist, prev, front, MonarchRules.SLAM_BAND)) continue;
                    if (!MonarchShapes.ringHurts(p.onGround(), p.getY(), cy, MonarchRules.SLAM_REACH)) continue;
                    if (strike(l, b, fight, p, MonarchMove.SLAM, t)) {
                        struck.add(p.getUUID());
                        shove(p, cx, cz, 0.7, 0.35);
                    }
                }
                prev = front;
                return front - MonarchRules.SLAM_BAND > radius;
            }
        };
    }

    // ---------------------------------------------------------------- Shadow Rain
    /** Circles marked on the floor, under the foes and scattered round them; a moment later each erupts. */
    static Hazard rain(ServerLevel level, Mob boss, MonarchFight f, List<ServerPlayer> foes, long now) {
        List<double[]> at = new ArrayList<>();
        for (ServerPlayer p : foes) at.add(new double[]{p.getX(), p.getZ()});
        final Vec3 c = f.centreVec();
        final List<double[]> spots = MonarchShapes.rain(level.random.nextLong(), at, MonarchRules.rainCircles(f.phase, foes.size()), c.x, c.z, MonarchRules.RAIN_REACH, MonarchRules.RAIN_GAP);
        final double[] ys = new double[spots.size()];
        for (int i = 0; i < ys.length; i++) {
            ys[i] = groundY(level, spots.get(i)[0], boss.getY(), spots.get(i)[1]);
            VfxSpawn.send(level, VfxShape.SHADOW_POOL, new Vec3(spots.get(i)[0], ys[i] + 0.05, spots.get(i)[1]), new Vec3(spots.get(i)[0], ys[i] + 1.0, spots.get(i)[1]),
                    VIOLET, MonarchRules.RAIN_MARK, (float) MonarchRules.RAIN_RADIUS);
        }
        sound(level, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.SCULK_SHRIEKER_SHRIEK, 3.0f, 0.5f);
        final long erupt = now + MonarchRules.RAIN_MARK;
        return (l, b, fight, t) -> {
            if (t < erupt) {
                if ((erupt - t) % 4 == 0) {
                    boolean late = erupt - t <= 8;
                    for (int i = 0; i < spots.size(); i++) ring(l, late ? DUST_RED : DUST_VIOLET, spots.get(i)[0], ys[i] + 0.12, spots.get(i)[1], MonarchRules.RAIN_RADIUS, 0.7, t * 0.3);
                }
                return false;
            }
            for (int i = 0; i < spots.size(); i++) {
                double x = spots.get(i)[0], z = spots.get(i)[1];
                for (ServerPlayer p : foesNear(l, x, z, MonarchRules.RAIN_RADIUS)) {
                    if (Math.abs(p.getY() - ys[i]) <= 3.0 && strike(l, b, fight, p, MonarchMove.RAIN, t)) shove(p, x, z, 0.3, 0.45);
                }
                l.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, ys[i] + 0.2, z, 22, 0.9, 0.2, 0.9, 0.10);
                l.sendParticles(ParticleTypes.SQUID_INK, x, ys[i] + 0.5, z, 14, 0.8, 0.8, 0.8, 0.06);
            }
            if (!spots.isEmpty()) sound(l, spots.get(0)[0], ys[0], spots.get(0)[1], SoundEvents.AMETHYST_BLOCK_RESONATE, 4.0f, 0.5f);
            return true;
        };
    }

    // ---------------------------------------------------------------- Soul Rend
    /** Lines drawn across the hall from the Monarch toward its foe, a fan of them in the later phases; then each is cut, once. */
    static Hazard beam(ServerLevel level, Mob boss, MonarchFight f, ServerPlayer target, long now) {
        final double ax = boss.getX(), az = boss.getZ(), ay = boss.getY() + 1.0;
        final double base = MonarchShapes.angle(ax, az, target.getX(), target.getZ());
        final List<double[]> ends = new ArrayList<>();
        for (double a : MonarchShapes.fan(base, MonarchRules.beams(f.phase), MonarchRules.BEAM_FAN)) ends.add(MonarchShapes.end(ax, az, a, MonarchRules.BEAM_LENGTH));
        final long fire = now + MonarchRules.BEAM_WINDUP;
        sound(level, ax, ay, az, SoundEvents.ELDER_GUARDIAN_CURSE, 2.0f, 1.4f);
        return (l, b, fight, t) -> {
            if (t < fire) {
                if ((fire - t) % 2 == 0) {
                    ParticleOptions dust = fire - t <= 10 ? DUST_WHITE : fire - t <= 20 ? DUST_RED : DUST_VIOLET;
                    for (double[] e : ends) line(l, dust, ax, ay - 0.4, az, e[0], e[1], 1.5);
                }
                return false;
            }
            sound(l, ax, ay, az, SoundEvents.WARDEN_SONIC_BOOM, 4.0f, 0.8f);
            for (double[] e : ends) {
                VfxSpawn.send(l, VfxShape.DARK_THRUST, new Vec3(ax, ay, az), new Vec3(e[0], ay, e[1]), VIOLET, 12, 1.6f);
                line(l, ParticleTypes.SOUL_FIRE_FLAME, ax, ay - 0.5, az, e[0], e[1], 1.0);
            }
            for (ServerPlayer p : foesNear(l, ax, az, MonarchRules.BEAM_LENGTH + 2.0)) {
                for (double[] e : ends) {
                    if (MonarchShapes.beamHits(p.getX(), p.getY(), p.getZ(), ax, ay - 5.0, az, e[0], e[1], MonarchRules.BEAM_HALF, 9.0)) {
                        if (strike(l, b, fight, p, MonarchMove.BEAM, t)) shove(p, ax, az, 0.5, 0.3);
                        break;
                    }
                }
            }
            return true;
        };
    }

    // ---------------------------------------------------------------- Monarch's Authority
    /** A ring closing in on the Monarch for a second, then a field round it for four seconds that slows everyone in it; a Mana Zone holds it back. */
    static Hazard authority(ServerLevel level, Mob boss, MonarchFight f, long now) {
        final double cx = boss.getX(), cz = boss.getZ(), cy = boss.getY();
        final double radius = MonarchRules.AUTHORITY_RADIUS;
        final long begin = now + MonarchRules.AUTHORITY_WINDUP, end = begin + MonarchRules.AUTHORITY_TICKS;
        sound(level, cx, cy, cz, SoundEvents.WITHER_AMBIENT, 3.0f, 0.5f);
        return new Hazard() {
            boolean open;

            @Override
            public boolean tick(ServerLevel l, Mob b, MonarchFight fight, long t) {
                if (t < begin) {
                    if ((begin - t) % 4 == 0) {
                        ring(l, DUST_VIOLET, cx, cy + 0.15, cz, radius, 0.9, 0.0);
                        ring(l, DUST_RED, cx, cy + 0.15, cz, radius * (0.4 + 0.6 * (begin - t) / MonarchRules.AUTHORITY_WINDUP), 1.0, t * 0.1);
                    }
                    return false;
                }
                if (!open) {
                    open = true;
                    VfxSpawn.send(l, VfxShape.NIHILITY_ZONE, new Vec3(cx, cy, cz), new Vec3(cx, cy + 1.0, cz), VIOLET, MonarchRules.AUTHORITY_TICKS, (float) radius);
                    sound(l, cx, cy, cz, SoundEvents.ELDER_GUARDIAN_CURSE, 3.0f, 0.4f);
                }
                if ((t - begin) % MonarchRules.AUTHORITY_REFRESH == 0) {
                    ring(l, DUST_VIOLET, cx, cy + 0.15, cz, radius, 0.9, t * 0.05);
                    l.sendParticles(ParticleTypes.SQUID_INK, cx, cy + 4.0, cz, 18, radius * 0.5, 1.5, radius * 0.5, 0.01);
                    for (ServerPlayer p : foesNear(l, cx, cz, radius)) {
                        boolean zone = ManaLaw.zoneActive(p);
                        fight.slowed.add(p.getUUID());
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MonarchRules.authorityDuration(zone), MonarchRules.authoritySlow(fight.phase, zone), false, true));
                    }
                }
                return t >= end;
            }
        };
    }

    // ---------------------------------------------------------------- Summon Shadow Soldiers
    /** Shadow gathers at the Monarch's feet for two seconds; then daemons step out of it, as many as the phase allows and the cap leaves room for. */
    static Hazard summon(ServerLevel level, Mob boss, MonarchFight f, long now) {
        final long release = now + MonarchRules.SUMMON_CHANNEL;
        VfxSpawn.send(level, VfxShape.SHADOW_POOL, boss.position().add(0, 0.05, 0), boss.position().add(0, 1.0, 0), VIOLET, MonarchRules.SUMMON_CHANNEL, 5.0f);
        sound(level, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WITHER_SPAWN, 1.5f, 0.6f);
        return (l, b, fight, t) -> {
            if (t < release) {
                if ((release - t) % 3 == 0) {
                    ring(l, DUST_VIOLET, b.getX(), b.getY() + 0.15, b.getZ(), 3.5, 0.8, t * 0.2);
                    l.sendParticles(ParticleTypes.SOUL, b.getX(), b.getY() + 0.3, b.getZ(), 4, 1.5, 0.2, 1.5, 0.02);
                }
                return false;
            }
            spawnSoldiers(l, b, fight, MonarchRules.summonCount(fight.phase, alive(l, fight)));
            return true;
        };
    }

    /** Brings up to `count` shadow soldiers out of the Monarch's shadow, in a ring round it. Returns how many came. */
    static int spawnSoldiers(ServerLevel level, Mob boss, MonarchFight f, int count) {
        int made = 0, greater = 0;
        for (UUID id : f.soldiers) if (level.getEntity(id) instanceof Mob m && Wildlife.idOf(m.getType()).equals("tensura:greater_daemon")) greater++;
        for (int i = 0; i < count && alive(level, f) < MonarchRules.minionCap(f.phase); i++) {
            boolean big = greater < MonarchRules.greaterAllowed(f.phase) && i % 3 == 2;
            Mob m = null;
            for (int attempt = 0; attempt < 6 && m == null; attempt++) {
                double a = level.random.nextDouble() * Math.PI * 2, d = 3.5 + level.random.nextDouble() * 2.5;
                double x = boss.getX() + Math.cos(a) * d, z = boss.getZ() + Math.sin(a) * d;
                m = MonarchBoss.soldier(level, big, new Vec3(x, groundY(level, x, boss.getY(), z), z), f.boss, f.phase);
            }
            if (m == null) continue;
            f.soldiers.add(m.getUUID());
            if (big) greater++;
            made++;
            level.sendParticles(ParticleTypes.SQUID_INK, m.getX(), m.getY() + 0.5, m.getZ(), 20, 0.4, 0.6, 0.4, 0.08);
            LivingEntity target = nearest(foes(level), m);
            if (target != null) MonarchDriver.aim(m, target);
        }
        return made;
    }

    /** The player nearest to an entity, or null. */
    static ServerPlayer nearest(List<ServerPlayer> players, Entity to) {
        ServerPlayer best = null;
        double bestD = Double.MAX_VALUE;
        for (ServerPlayer p : players) {
            double d = p.distanceToSqr(to);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- the roar between phases
    /** The roar: a ring closing in for most of a second while it cannot be hurt, then a shove that clears the foes from it (no damage) and a fresh troop of soldiers. */
    static Hazard roar(ServerLevel level, Mob boss, MonarchFight f, int toPhase, long now) {
        final long end = now + MonarchRules.TRANSITION;
        sound(level, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WARDEN_ROAR, 5.0f, 0.7f);
        sound(level, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.ENDER_DRAGON_GROWL, 4.0f, 0.6f);
        VfxSpawn.sendFollowing(level, VfxShape.KOTO_AURA, boss, boss.position(), VIOLET, MonarchRules.TRANSITION + 20, 3.0f);
        String line = MonarchText.phase(toPhase);
        if (!line.isEmpty()) {
            Component c = Component.literal(line).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
            for (ServerPlayer p : foesNear(level, boss.getX(), boss.getZ(), MonarchRules.ARENA_RADIUS)) p.displayClientMessage(c, true);
        }
        return (l, b, fight, t) -> {
            if (t < end) {
                if ((end - t) % 3 == 0) {
                    double shrink = (double) (end - t) / MonarchRules.TRANSITION;
                    ring(l, DUST_RED, b.getX(), b.getY() + 0.2, b.getZ(), 2.0 + 10.0 * shrink, 0.9, t * 0.15);
                    l.sendParticles(ParticleTypes.SQUID_INK, b.getX(), b.getY() + 1.5, b.getZ(), 10, 1.0, 1.5, 1.0, 0.05);
                }
                return false;
            }
            for (ServerPlayer p : foesNear(l, b.getX(), b.getZ(), 14.0)) shove(p, b.getX(), b.getZ(), 1.1, 0.4);
            l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, b.getX(), b.getY() + 1.0, b.getZ(), 1, 0, 0, 0, 0);
            spawnSoldiers(l, b, fight, MonarchRules.summonCount(fight.phase, alive(l, fight)));
            return true;
        };
    }
}
