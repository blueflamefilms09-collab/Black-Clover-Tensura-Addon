package com.newuniverse.nusmp.relics.wave;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Wave 18: the small glue the charms share (dust particles, a foe in front of the player, an entity by id). Statics only; server side. */
final class Wave18Kit {
    private Wave18Kit() {}

    static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), size);
    }

    /** The registry id of an entity type, for messages and checks. */
    static String typeId(LivingEntity e) {
        ResourceLocation k = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return k == null ? "" : k.toString();
    }

    /** The living thing with this id in this level, or null. */
    static LivingEntity living(ServerLevel level, java.util.UUID id) {
        net.minecraft.world.entity.Entity e = level.getEntity(id);
        return e instanceof LivingEntity l && l.isAlive() ? l : null;
    }

    /** The player with this id, or null when offline or dead. */
    static ServerPlayer player(ServerLevel level, java.util.UUID id) {
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
        return p != null && p.isAlive() ? p : null;
    }

    /** The unit horizontal direction the player faces. */
    static Vec3 flatLook(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : flat.normalize();
    }

    /** Nearest first, those {@code list} members roughly in front of the player (a half-plane test). */
    static List<LivingEntity> inFront(ServerPlayer p, List<LivingEntity> list) {
        Vec3 f = flatLook(p);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : list) {
            Vec3 d = e.position().subtract(p.position());
            if (d.x * f.x + d.z * f.z > -0.5 || d.lengthSqr() < 1.0) out.add(e);
        }
        return out;
    }

    static boolean isMob(LivingEntity e) { return e instanceof Mob; }
}
