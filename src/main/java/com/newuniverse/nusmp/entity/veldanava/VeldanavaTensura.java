package com.newuniverse.nusmp.entity.veldanava;

import io.github.manasmods.tensura.entity.TensuraProjectile;
import io.github.manasmods.tensura.entity.magic.beam.BeamProjectile;
import io.github.manasmods.tensura.entity.magic.lightning.TensuraLightningBolt;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
    public static final int MAX_LIVE = 20, MAX_LIFE = 120;

    private final List<Entity> live = new ArrayList<>();

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
     * plays the 3.30.0 effect). At most {@link #MAX_LIVE} stay alive; the oldest go first.
     */
    public Entity spawn(ServerLevel sl, String path, Vec3 at, float yawDeg, float pitchDeg, int life) {
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
            if (e instanceof TensuraProjectile tp) tp.setLife(Math.max(1, Math.min(MAX_LIFE, life)));
            if (!sl.addFreshEntity(e)) return null;
            live.add(e);
            return e;
        } catch (Throwable t) {
            if (e != null) e.discard();
            return null;
        }
    }

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
    }
}
