package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.particle.rukh.RukhFx;

import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.Zone;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.ZoneBook;
import com.newuniverse.nusmp.relics.wave.Wave14Rules.ZoneKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Wave 14: what stays in the world a little while (mud, a dust devil, a wind wall) and the short timers of the rituals (heel clicks, a
 * conjuring circle). One server tick handler drives both; with nothing pending it returns at once. Everything is bounded: 24 zones, 24 timers,
 * 24 foes a pass.
 */
final class Wave14Zones {
    private Wave14Zones() {}

    static final int KIND_CLICK = 0, KIND_PLANE = 1, KIND_CONJURE = 2;
    private static final int MAX_PENDING = 24;

    private static final ZoneBook BOOK = new ZoneBook();
    private static final List<Pending> PENDING = new ArrayList<>();

    private record Pending(long due, UUID player, int kind, int arg) {}

    // ---------------------------------------------------------------- adding
    static void addZone(ServerPlayer owner, ZoneKind kind, Vec3 at, double hx, double hz, double radius, int ticks) {
        long now = owner.level().getGameTime();
        BOOK.add(new Zone(kind, owner.level().dimension().location().toString(), at.x, at.y, at.z, hx, hz, radius, now + ticks, owner.getUUID()));
    }

    /** Runs {@code kind} for this player after {@code ticks} (at most 24 timers are kept; the oldest goes). */
    static void later(ServerPlayer p, int ticks, int kind, int arg) {
        PENDING.add(new Pending(p.level().getGameTime() + ticks, p.getUUID(), kind, arg));
        while (PENDING.size() > MAX_PENDING) PENDING.remove(0);
    }

    static void clear() {
        BOOK.clear();
        PENDING.clear();
    }

    // ---------------------------------------------------------------- the tick
    static void tick(MinecraftServer server) {
        if (BOOK.isEmpty() && PENDING.isEmpty()) return;
        long now = server.overworld().getGameTime();
        runTimers(server, now);
        if (BOOK.isEmpty()) return;
        int t = server.getTickCount();
        if (t % Wave14Rules.ZONE_STEP == 0) BOOK.prune(now);
        boolean step = t % Wave14Rules.ZONE_STEP == 0;
        for (Zone z : BOOK.snapshot()) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, Wave14Util.rl(z.dim())));
            if (level == null || !z.live(now)) continue;
            ServerPlayer owner = server.getPlayerList().getPlayer(z.owner());
            if (z.kind() == ZoneKind.WALL) wall(level, z, owner, step);
            else if (step) {
                if (z.kind() == ZoneKind.GREASE) grease(level, z, owner);
                else devil(level, z, owner);
            }
        }
    }

    private static void runTimers(MinecraftServer server, long now) {
        if (PENDING.isEmpty()) return;
        Iterator<Pending> it = PENDING.iterator();
        List<Pending> due = new ArrayList<>();
        while (it.hasNext()) {
            Pending p = it.next();
            if (p.due() <= now) {
                due.add(p);
                it.remove();
            }
        }
        for (Pending p : due) {
            ServerPlayer player = server.getPlayerList().getPlayer(p.player());
            if (player == null || !player.isAlive()) continue;
            try {
                if (p.kind() == KIND_CLICK) Wave14Feet.heelClick(player);
                else if (p.kind() == KIND_PLANE) Wave14Feet.finishPlane(player, p.arg());
                else if (p.kind() == KIND_CONJURE) Wave14Head.finishConjure(player, p.arg());
            } catch (RuntimeException | LinkageError failed) {
                org.slf4j.LoggerFactory.getLogger("nusmp").error("[nusmp] wave 14 timer {} failed", p.kind(), failed);
            }
        }
    }

    // ---------------------------------------------------------------- foes of a zone
    /** A creature a zone of this owner may bother: a foe of the owner when the owner is there, else any hostile mob. */
    private static boolean foe(ServerPlayer owner, LivingEntity e) {
        if (owner != null) return Wave14Util.target(owner, e) && !Wave14Util.friend(owner, e);
        return e instanceof Enemy;
    }

    // ---------------------------------------------------------------- mud
    private static void grease(ServerLevel level, Zone z, ServerPlayer owner) {
        Vec3 c = new Vec3(z.x(), z.y(), z.z());
        RukhFx.spray(level, Wave14Util.align(0x7A5232), z.x(), z.y() + 0.15, z.z(), 6, z.radius() * 0.5, 0.05, z.radius() * 0.5, 0.0);
        for (LivingEntity e : Wave14Util.near(level, c, z.radius(), x -> foe(owner, x) && Math.abs(x.getY() - z.y()) < 2.0)) {
            Wave14Util.give(e, 30, 1, "minecraft:slowness");
            if (e.getRandom().nextInt(4) == 0) {                                      // the grease takes the feet from under it
                Wave14Util.give(e, 20, 4, "minecraft:slowness");
                RukhFx.spray(level, RukhFx.Alignment.LIGHT, e.getX(), e.getY() + 0.2, e.getZ(), 6, 0.3, 0.1, 0.3, 0.02);
            }
        }
    }

    // ---------------------------------------------------------------- dust devil
    private static void devil(ServerLevel level, Zone z, ServerPlayer owner) {
        Zone m = z.moved(0.5);
        BOOK.replace(z, m);
        Vec3 c = new Vec3(m.x(), m.y(), m.z());
        for (int i = 0; i < 10; i++) {
            double a = i * 0.9 + level.getGameTime() * 0.4, h = i * 0.25;
            double r = 0.4 + i * 0.12;
            RukhFx.spray(level, Wave14Util.align(0xC8B27A), m.x() + Math.cos(a) * r, m.y() + h, m.z() + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
        RukhFx.spray(level, RukhFx.Alignment.LIGHT, m.x(), m.y() + 0.3, m.z(), 3, 0.3, 0.2, 0.3, 0.02);
        for (LivingEntity e : Wave14Util.near(level, c, m.radius(), x -> foe(owner, x) && Math.abs(x.getY() - m.y()) < 3.0)) {
            if (owner != null) Fx.damage(owner, e, 2.5f, "minecraft:magic");
            Wave14Util.give(e, 30, 0, "minecraft:blindness");
            e.push(0.0, 0.35, 0.0);
            e.hurtMarked = true;
        }
    }

    // ---------------------------------------------------------------- wind wall
    private static void wall(ServerLevel level, Zone z, ServerPlayer owner, boolean step) {
        if (step) {                                                                    // the wall drawn in the air, twice a second
            for (int i = -5; i <= 5; i++) {
                double ax = z.x() + (-z.hz()) * i, az = z.z() + z.hx() * i;
                for (int h = 0; h < 3; h++) RukhFx.spray(level, RukhFx.Alignment.LIGHT, ax, z.y() + 0.4 + h, az, 1, 0.05, 0.1, 0.05, 0.0);
            }
        }
        double r = z.radius() + 3.0;
        AABB box = new AABB(z.x() - r, z.y() - 0.5, z.z() - r, z.x() + r, z.y() + 3.0, z.z() + r);
        for (Projectile pr : level.getEntitiesOfClass(Projectile.class, box)) {
            if (owner != null && pr.getOwner() == owner) continue;                     // the caster's own shots pass
            if (!Wave14Rules.inWall(z, pr.getX(), pr.getY(), pr.getZ())) continue;
            RukhFx.spray(level, RukhFx.Alignment.LIGHT, pr.getX(), pr.getY(), pr.getZ(), 4, 0.1, 0.1, 0.1, 0.02);
            level.playSound(null, pr.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.5f, 1.4f);
            pr.discard();
        }
    }
}
