package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Wave 6: the two standing areas of the wave, both short-lived and bounded: the Skycrystal Armor's Globe of Invulnerability (magic cannot hurt the
 * caster and allies inside, for 10 s) and the Shadow Robes' cloud of necrotic energy (left where the wearer stepped out, three pulses of 2 damage and
 * a short wither on foes standing in it). They store only keys and numbers, never entities, and are cleared when the server stops.
 */
public final class Wave6Zones {
    private Wave6Zones() {}

    private static final int CAP = 24;
    private static final int GLOBE_COLOUR = 0x9FD8FF, CLOUD_COLOUR = 0x2A1F3D;

    private enum Kind { GLOBE, CLOUD }

    private static final class Zone {
        final Kind kind; final ResourceKey<Level> dim; final double x, y, z; final long until; final UUID owner;
        long next; int pulses;
        Zone(Kind kind, ResourceKey<Level> dim, Vec3 at, long until, UUID owner) {
            this.kind = kind; this.dim = dim; this.x = at.x; this.y = at.y; this.z = at.z; this.until = until; this.owner = owner;
        }
    }

    private static final List<Zone> ZONES = new ArrayList<>();

    private static boolean add(Kind kind, ServerPlayer owner, Vec3 at, int ticks) {
        MinecraftServer s = owner.getServer();
        if (s == null || ZONES.size() >= CAP) return false;
        Zone z = new Zone(kind, owner.level().dimension(), at, s.getTickCount() + ticks, owner.getUUID());
        z.next = s.getTickCount() + (kind == Kind.CLOUD ? Wave6Tune.CLOUD_PERIOD : 20);
        ZONES.add(z);
        return true;
    }

    public static boolean addGlobe(ServerPlayer owner, Vec3 at, int ticks) { return add(Kind.GLOBE, owner, at, ticks); }

    public static boolean addCloud(ServerPlayer owner, Vec3 at) {
        return add(Kind.CLOUD, owner, at, Wave6Tune.CLOUD_PULSES * Wave6Tune.CLOUD_PERIOD + 2);
    }

    /** Is any globe standing at all (the damage hook asks this first, so the common case costs nothing). */
    public static boolean anyGlobe() {
        for (Zone z : ZONES) if (z.kind == Kind.GLOBE) return true;
        return false;
    }

    /** Does a live globe of this level cover this creature (the caster, a team mate, or an animal the caster owns)? */
    public static boolean globeCovers(LivingEntity e) {
        MinecraftServer s = e.getServer();
        if (s == null) return false;
        int now = s.getTickCount();
        double r2 = Wave6Tune.GLOBE_RADIUS * Wave6Tune.GLOBE_RADIUS;
        for (Zone z : ZONES) {
            if (z.kind != Kind.GLOBE || z.until <= now || z.dim != e.level().dimension() || e.distanceToSqr(z.x, z.y, z.z) > r2) continue;
            ServerPlayer owner = s.getPlayerList().getPlayer(z.owner);
            if (owner != null && Wave6Kit.isAlly(owner, e)) return true;
        }
        return false;
    }

    public static void tick(MinecraftServer s) {
        if (ZONES.isEmpty()) return;
        int now = s.getTickCount();
        Iterator<Zone> it = ZONES.iterator();
        while (it.hasNext()) {
            Zone z = it.next();
            ServerLevel level = s.getLevel(z.dim);
            if (z.until <= now || level == null) { it.remove(); continue; }
            if (now < z.next) continue;
            z.next = now + (z.kind == Kind.CLOUD ? Wave6Tune.CLOUD_PERIOD : 20);
            Vec3 at = new Vec3(z.x, z.y, z.z);
            if (z.kind == Kind.GLOBE) {
                Fx.ring(level, at.add(0, 0.1, 0), Wave6Tune.GLOBE_RADIUS, GLOBE_COLOUR);
            } else {
                pulse(s, level, z, at);
            }
        }
    }

    private static void pulse(MinecraftServer s, ServerLevel level, Zone z, Vec3 at) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.6, at.z, 14, Wave6Tune.CLOUD_RADIUS * 0.4, 0.4, Wave6Tune.CLOUD_RADIUS * 0.4, 0.01);
        Fx.ring(level, at.add(0, 0.1, 0), Wave6Tune.CLOUD_RADIUS, CLOUD_COLOUR);
        ServerPlayer owner = s.getPlayerList().getPlayer(z.owner);
        if (owner == null || z.pulses >= Wave6Tune.CLOUD_PULSES) return;
        z.pulses++;
        double r = Wave6Tune.CLOUD_RADIUS;
        List<LivingEntity> in = level.getEntitiesOfClass(LivingEntity.class, new AABB(at.x - r, at.y - 1, at.z - r, at.x + r, at.y + 2.5, at.z + r),
                e -> Wave6Kit.isFoe(owner, e) && e.distanceToSqr(at.x, at.y, at.z) <= r * r);
        String dark = Wave6Kit.damageType(level, Wave6Skills.DMG_DARKNESS, "minecraft:magic");     // necrotic: Tensura's darkness damage
        int n = 0;
        for (LivingEntity t : in) {
            if (n++ >= 8) break;
            Fx.damage(owner, t, Wave6Tune.CLOUD_PULSE_DAMAGE, dark);
            Fx.addEffect(t, "minecraft:wither", Wave6Tune.CLOUD_WITHER_TICKS, 0);
        }
    }

    public static void clear() { ZONES.clear(); }
}
