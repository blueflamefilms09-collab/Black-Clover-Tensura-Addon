package com.newuniverse.nusmp.entity.veldanava;

import io.github.manasmods.tensura.entity.TensuraProjectile;
import io.github.manasmods.tensura.entity.magic.beam.BeamProjectile;
import io.github.manasmods.tensura.entity.magic.lightning.TensuraLightningBolt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 3.31.0: Veldanava casts the base Tensura Reincarnated versions of his six Tensura moves (Absolute Severance, Black Lightning, Black Lightning Blast, Death Tornado,
 * Disintegration, Hell Flare, Megiddo): the real Tensura entities, with Tensura's own models, animations and sounds, found by registry id at run time (the mod never
 * links to a class that may be missing). They are only the look and the sound of the move: every one is spawned with its own damage, knockback, burn and effects turned
 * off, and the damage stays in the boss's capped, budgeted path ({@link VeldanavaBoss#strike}), so the DPS budget, the Anti-Totem and the damage cap apply as before.
 * The server setting {@code tensuraMoves} turns them off and brings back the 3.30.0 effects exactly; a missing id or a failed spawn does the same for that move.
 *
 * <p>Worst-case cost (rule 11): one instance per boss; at most {@link #MAX_LIVE} Tensura entities alive at once (the oldest are discarded first), each with a short life
 * (at most {@link #MAX_LIFE} ticks) and all discarded when the boss goes; no entity scan (it keeps its own list); no block change of its own (fire, explosions and
 * block launching are turned off on every entity that has such a switch).
 */
public final class VeldanavaTensura {
    public static final String SEVERANCE = "severance_cutter", BOLT = "black_lightning_bolt", BLAST = "black_lightning_blast", TORNADO = "death_tornado",
            DISINTEGRATION = "disintegration", HELL_FLARE = "hell_flare", MEGIDDO = "megiddo_bubble";
    /** Every id the moves use, in the order of the moves. */
    public static final String[] IDS = {SEVERANCE, BOLT, BLAST, TORNADO, DISINTEGRATION, HELL_FLARE, MEGIDDO};
    public static final int MAX_LIVE = 20, MAX_LIFE = 120, MAX_BEAMS = 6;
    /** How fast the Absolute Severance cutter flies, in blocks per tick. */
    public static final float CUTTER_SPEED = 3.0f;

    private final List<Entity> live = new ArrayList<>();
    private Entity mover;

    /** True when the server wants the Tensura versions (never in the 3.11.0 classic mode). */
    public static boolean on() { return VeldanavaConfig.tensuraMoves() && !VeldanavaConfig.classic(); }

    /** The Tensura entity type with this path, or null when the Tensura version in use does not have it. */
    public static EntityType<?> typeOf(String path) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.fromNamespaceAndPath("tensura", path)).orElse(null);
    }

    /** True when the move should use the Tensura entity: the setting is on and the id exists. */
    public static boolean use(String path) { return on() && typeOf(path) != null; }

    /**
     * Creates the entity, turns its own harm off, places it and adds it to the level. Returns it, or null when the id is missing or anything went wrong (the caller then
     * plays the 3.30.0 effect). At most {@link #MAX_LIVE} stay alive; the oldest go first. 'size' above 0 sets the entity's size (radius of a field or a funnel); 'owner' is
     * the boss (Tensura's own disintegration removes itself without one, and a field never hurts its owner).
     */
    public Entity spawn(ServerLevel sl, String path, Vec3 at, float yawDeg, float pitchDeg, int life, float size, Entity owner) {
        EntityType<?> type = typeOf(path);
        if (type == null) return null;
        prune();
        while (live.size() >= MAX_LIVE) live.remove(0).discard();
        Entity e = null;
        try {
            e = type.create(sl);
            if (e == null) return null;
            e.moveTo(at.x, at.y, at.z, yawDeg, pitchDeg);
            quiet(e);
            if (owner != null) {
                if (e instanceof Projectile pr) pr.setOwner(owner);
                else if (e instanceof TensuraLightningBolt lb) lb.setOwner(owner);
            }
            if (e instanceof TensuraProjectile tp) {
                tp.setLife(clampLife(life));
                if (size > 0f) tp.setSize(size);
            }
            if (!sl.addFreshEntity(e)) return null;
            live.add(e);
            return e;
        } catch (Throwable t) {
            if (e != null) e.discard();
            return null;
        }
    }

    // ---------------------------------------------------------------- the seven moves' entities
    /** The cutter of Absolute Severance, flying along the aim. */
    public Entity cutter(ServerLevel sl, Vec3 at, float mcYawDeg, Entity owner) {
        Entity e = spawn(sl, SEVERANCE, at, mcYawDeg, 0f, 24, 0f, owner);
        if (e instanceof Projectile pr) {
            double r = Math.toRadians(mcYawDeg);
            pr.shoot(-Math.sin(r), 0.0, Math.cos(r), CUTTER_SPEED, 0f);
        }
        return e;
    }

    /** One bolt of Black Lightning striking the point. */
    public Entity bolt(ServerLevel sl, Vec3 at, Entity owner) { return spawn(sl, BOLT, at, 0f, 0f, 1, 0f, owner); }

    /** 'beams' black lightning beams from the point, evenly round, each 'range' blocks long and alive for 'life' ticks. Returns how many were made. */
    public int blast(ServerLevel sl, Vec3 at, int beams, int life, float range, Entity owner) {
        int made = 0;
        for (int i = 0; i < Math.min(beams, MAX_BEAMS); i++) {
            float yaw = beamStep(i, beams);
            Entity e = spawn(sl, BLAST, at, yaw, 0f, life, 0f, owner);
            if (e == null) break;
            if (e instanceof BeamProjectile bp) {
                bp.setFollowingOwner(false);
                bp.setYaw(beamYaw(yaw));
                bp.setPitch(beamPitch(0f));
                bp.setRange(range);
            }
            made++;
        }
        return made;
    }

    /** The bubble of Megiddo at the point. */
    public Entity megiddo(ServerLevel sl, Vec3 at, int life, Entity owner) { return spawn(sl, MEGIDDO, at, 0f, 0f, life, 0f, owner); }

    /** The Death Tornado: the one the moves move along ({@link #follow}). */
    public Entity tornado(ServerLevel sl, Vec3 at, float size, int life, Entity owner) {
        mover = spawn(sl, TORNADO, at, 0f, 0f, life, size, owner);
        return mover;
    }

    /** A field: the Disintegration zone (the one the moves keep) or the Hell Flare. */
    public Entity zone(ServerLevel sl, String path, Vec3 at, float size, int life, Entity owner) {
        Entity e = spawn(sl, path, at, 0f, 0f, life, size, owner);
        if (DISINTEGRATION.equals(path)) mover = e;
        return e;
    }

    /** Moves the tornado (or the zone) to the point, if it is still there. */
    public void follow(Vec3 p) {
        if (mover != null && !mover.isRemoved()) mover.setPos(p.x, p.y, p.z);
    }

    /** True while the tornado or the zone of this cast is alive. */
    public boolean hasMover() { return mover != null && !mover.isRemoved(); }

    // ---------------------------------------------------------------- pure numbers
    /** Tensura's beam angle (radians) for a Minecraft yaw in degrees: its beams point along +X at 0, Minecraft's yaw 0 looks along +Z. */
    public static float beamYaw(float mcYawDeg) { return (float) Math.toRadians(mcYawDeg + 90.0); }

    /** Tensura's beam pitch (radians) for a Minecraft pitch in degrees (down is positive in Minecraft). */
    public static float beamPitch(float mcPitchDeg) { return (float) Math.toRadians(-mcPitchDeg); }

    /** The yaw of beam 'i' of 'n' spread evenly round the circle, in degrees. */
    public static float beamStep(int i, int n) { return n <= 0 ? 0f : 360f * i / n; }

    /** A life in ticks, kept between one tick and {@link #MAX_LIFE}. */
    public static int clampLife(int ticks) { return Math.max(1, Math.min(MAX_LIFE, ticks)); }

    /** Turns off everything a Tensura entity does to what it touches, so it is only a look and a sound. */
    public static void quiet(Entity e) {
        if (e instanceof TensuraProjectile tp) {
            tp.setDamage(0f);
            tp.setSecondaryDamage(0f);
            tp.setKnockForce(0f);
            tp.setKnockResistNegate(0f);
            tp.setBurnTicks(0);
            tp.setElementalAttack(false);
            try { tp.setMobEffect(null); } catch (Throwable ignored) { }
        }
        if (e instanceof TensuraLightningBolt lb) {
            lb.setTensuraDamage(0f);
            lb.setSecondaryDamage(0f);
            lb.setShouldPlaceFire(false);
            lb.setElementalAttack(false);
        }
        if (e instanceof BeamProjectile bp) bp.setExplosionRadius(0f);
    }

    /** Drops the ones that are gone. */
    public void prune() { live.removeIf(Entity::isRemoved); }

    /** How many are alive. */
    public int alive() { prune(); return live.size(); }

    /** Discards every one (the boss fell, was removed or the fight started over). */
    public void clear() {
        for (Entity e : live) if (!e.isRemoved()) e.discard();
        live.clear();
        mover = null;
    }
}
