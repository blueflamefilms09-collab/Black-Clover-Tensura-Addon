package com.newuniverse.nusmp.relics.wave;


import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * Wave 17: the Crystal Dragon's illusory dragon. It is not an entity: for eight seconds it is drawn in dust and sparks over the player, its head follows
 * where the player looks, and once a second it breathes a cone of shadow (damage on foes, fear on monsters). It is walked from the server tick every
 * five ticks, there is one per player and at most sixteen in the world, and a player foe can lose only 30% of their health to one dragon in all.
 */
final class Wave17Dragon {
    private Wave17Dragon() {}

    private static final int MAX_DRAGONS = 16, STEP = 5, TEAL = 0x3AD7C0, DEEP = 0x14786E;
    private static final double REACH = 8.0, CONE = 0.78;

    private static final class Dragon {
        final UUID owner;
        final long start;
        int breaths;
        final Map<UUID, Float> dealt = new HashMap<>();

        Dragon(UUID owner, long start) {
            this.owner = owner;
            this.start = start;
        }
    }

    private static final List<Dragon> ACTIVE = new ArrayList<>();

    /** Raises a dragon over the player; false if they have one already or the world has too many. */
    static boolean start(ServerPlayer p) {
        if (ACTIVE.size() >= MAX_DRAGONS) return false;
        for (Dragon d : ACTIVE) if (d.owner.equals(p.getUUID())) return false;
        ACTIVE.add(new Dragon(p.getUUID(), Wave17Kit.clock(p.serverLevel())));
        return true;
    }

    static void clear() { ACTIVE.clear(); }

    /** Called every server tick; works every fifth tick and only while a dragon lives. */
    static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        long now = server.overworld().getGameTime();
        if (now % STEP != 0) return;
        ACTIVE.removeIf(d -> {
            ServerPlayer p = server.getPlayerList().getPlayer(d.owner);
            long age = now - d.start;
            if (p == null || !p.isAlive() || age > Wave17Rules.DRAGON_TICKS) return true;
            draw(p, age);
            if (age / Wave17Rules.DRAGON_BREATH_EVERY > d.breaths && age >= Wave17Rules.DRAGON_BREATH_EVERY) {
                d.breaths++;
                breathe(p, d);
            }
            return false;
        });
    }

    private static Vec3 right(Vec3 f) {
        Vec3 r = f.cross(new Vec3(0, 1, 0));
        return r.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : r.normalize();
    }

    /** The dragon's silhouette: head ahead of the player, a winding spine behind it and two wings. */
    private static void draw(ServerPlayer p, long age) {
        ServerLevel level = p.serverLevel();
        Vec3 f = p.getLookAngle(), r = right(f), up = new Vec3(0, 1, 0);
        Vec3 head = p.getEyePosition().add(f.scale(5.0)).add(up.scale(0.5));
        var teal = Wave17Kit.align(TEAL);
        var deep = Wave17Kit.align(DEEP);
        com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_17, teal, head.x, head.y, head.z, 3, 0.25, 0.2, 0.25, 0.0);
        com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_17, FxTone.LIGHT, head.x + r.x * 0.3, head.y + 0.15, head.z + r.z * 0.3, 1, 0, 0, 0, 0.0);
        com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_17, FxTone.LIGHT, head.x - r.x * 0.3, head.y + 0.15, head.z - r.z * 0.3, 1, 0, 0, 0, 0.0);
        double wave = age * 0.3;
        Vec3 wingRoot = head;
        for (int i = 1; i <= 11; i++) {
            double t = i / 11.0;
            Vec3 at = head.subtract(f.scale(t * 9.0)).add(r.scale(Math.sin(t * 5.0 + wave) * 1.2)).add(up.scale(0.4 + Math.cos(t * 4.0 + wave) * 0.4 - t * 0.6));
            com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_17, i % 3 == 0 ? teal : deep, at.x, at.y, at.z, 2, 0.12, 0.12, 0.12, 0.0);
            if (i == 3) wingRoot = at;
        }
        double flap = Math.sin(age * 0.45) * 0.5;
        for (int side = -1; side <= 1; side += 2) {
            for (int j = 1; j <= 7; j++) {
                Vec3 at = wingRoot.add(r.scale(side * j * 0.75)).add(up.scale(j * (0.38 + flap * 0.4) - j * j * 0.035)).subtract(f.scale(j * 0.35));
                com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_17, deep, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }
    }

    /** The breath: a cone of shadow from the dragon's head, forwards along the player's look. */
    private static void breathe(ServerPlayer p, Dragon d) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition(), f = p.getLookAngle();
        Vec3 head = eye.add(f.scale(5.0)).add(0, 0.5, 0);
        var shade = Wave17Kit.align(DEEP);
        for (int k = 0; k < 28; k++) {
            double dist = 0.5 + p.getRandom().nextDouble() * REACH;
            Vec3 dir = f.add(p.getRandom().nextGaussian() * 0.16, p.getRandom().nextGaussian() * 0.16, p.getRandom().nextGaussian() * 0.16).normalize();
            Vec3 at = head.add(dir.scale(dist));
            com.newuniverse.nusmp.particle.fx.Fx.spray(level, FxThemes.RELIC_WAVE_17, k % 2 == 0 ? FxTone.DARK : shade, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        level.playSound(null, head.x, head.y, head.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.7f, 1.5f);
        for (LivingEntity t : Fx.foesAround(p, REACH + 3.0)) {
            Vec3 to = t.getBoundingBox().getCenter().subtract(eye);
            if (to.lengthSqr() < 1.0e-4 || to.normalize().dot(f) < CONE || to.length() > REACH + 2.0) continue;
            float dmg = Wave17Rules.DRAGON_BREATH_DAMAGE;
            if (t instanceof Player pl) {
                float room = (float) (pl.getMaxHealth() * Wave17Rules.DRAGON_PLAYER_BUDGET) - d.dealt.getOrDefault(pl.getUUID(), 0f);
                dmg = Math.min(dmg, Math.max(0f, room));
                d.dealt.merge(pl.getUUID(), dmg, Float::sum);
            } else {
                Wave17Kit.give(t, "tensura:fear", 80, 0);
            }
            if (dmg > 0f) Fx.damage(p, t, dmg, "minecraft:magic");
        }
    }
}
