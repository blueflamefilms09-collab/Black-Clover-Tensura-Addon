package com.newuniverse.nusmp.antimagic;

import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * 0.48: the Nihility zone Black Meteorite — Void Severance leaves where it strikes (item.DemonSlayerSwordItem). For its length,
 * twice a second: every barrier, jail, area spell and spell projectile inside shatters, and foes are magic-jammed (Tensura
 * silence). Once a second every active skill of a foe inside is held on a short cooldown and one buff is stripped.
 * All of it ends with the zone (nothing lasting for players).
 */
public final class NihilityZone {
    private NihilityZone() {}

    public static final int CRIMSON = 0xFFC0102A;

    private static final class Zone {
        final ServerLevel level; final UUID owner; final Vec3 c; final double r; final long until;
        long next;
        Zone(ServerLevel level, UUID owner, Vec3 c, double r, long until) { this.level = level; this.owner = owner; this.c = c; this.r = r; this.until = until; }
    }

    private static final List<Zone> ZONES = new ArrayList<>();

    /** Opens a zone of radius r at c for 'ticks' ticks. */
    public static void start(ServerLevel level, LivingEntity owner, Vec3 c, double r, int ticks) {
        ZONES.add(new Zone(level, owner == null ? null : owner.getUUID(), c, r, level.getGameTime() + ticks));
        VfxSpawn.send(level, VfxShape.NIHILITY_ZONE, c, c.add(0, 1, 0), CRIMSON, ticks, (float) r);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2f, 0.5f);
    }

    /** True if 'pos' is inside a live Nihility zone of this level. */
    public static boolean inside(ServerLevel level, Vec3 pos) {
        for (Zone z : ZONES) if (z.level == level && z.c.distanceToSqr(pos) <= z.r * z.r) return true;
        return false;
    }

    public static void onServerTick(ServerTickEvent.Post e) {
        if (ZONES.isEmpty()) return;
        Iterator<Zone> it = ZONES.iterator();
        while (it.hasNext()) {
            Zone z = it.next();
            long now = z.level.getGameTime();
            if (now >= z.until || z.level.getServer() == null) { it.remove(); continue; }
            if (now < z.next) continue;
            z.next = now + 10;
            Entity o = z.owner == null ? null : z.level.getEntity(z.owner);
            LivingEntity owner = o instanceof LivingEntity l ? l : null;
            int broke = Nullification.shatterBarriers(z.level, z.c, z.r, owner);
            if (broke > 0) z.level.playSound(null, z.c.x, z.c.y, z.c.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5f, 0.5f);
            boolean second = ((z.until - now) / 10) % 2 == 0;
            for (LivingEntity t : z.level.getEntitiesOfClass(LivingEntity.class, new AABB(z.c, z.c).inflate(z.r),
                    x -> x.isAlive() && !x.isSpectator() && x.distanceToSqr(z.c) <= z.r * z.r)) {
                if (owner != null && (t == owner || t.isAlliedTo(owner))) continue;
                if (t instanceof Player pl && pl.isCreative()) continue;
                EnergyBridge.effect(t, "silence", 30, 0);
                if (second) {
                    Nullification.jamAll(t, 3, 0.1f);
                    for (MobEffectInstance m : new ArrayList<>(t.getActiveEffects()))
                        if (m.getEffect().value().isBeneficial()) { t.removeEffect(m.getEffect()); break; }
                }
            }
        }
    }

    public static void clear() { ZONES.clear(); }
}
