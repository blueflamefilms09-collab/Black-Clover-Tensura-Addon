package com.newuniverse.nusmp.fairytail.storm;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Why: one live storm. It is not an entity (nothing is spawned, nothing is saved, nothing can be hit or leashed): it is a small record the {@link StormField} manager ticks on the
 * server, with everything a tick needs and nothing the client could change. It remembers who cast it (a UUID, never a player object), where it is going (a straight slow drift),
 * the seed the clients draw it from, and its book of hits ({@link StormHits}).
 */
final class StormCell {
    final int id;
    final UUID owner;
    final ResourceKey<Level> dim;
    final StormSpell spell;
    final long start;
    final int duration;
    final long seed;
    /** The ground centre at the start and at the end of the drift (a line spell: the start and the far end of the line). */
    final double[] from, to;
    /** The cell radius (a line spell: half width) and the unit direction the caster faced. */
    final double radius, dx, dz;
    /** The length of a line spell (a front, a surge). */
    final double length;
    final StormHits hits = new StormHits();
    /** Creatures a line spell has already struck (a front hits each once). */
    final Set<Integer> struck = new HashSet<>();
    int bolts;

    StormCell(int id, UUID owner, ResourceKey<Level> dim, StormSpell spell, long start, int duration, long seed, double[] from, double[] to, double radius, double dx, double dz, double length) {
        this.id = id;
        this.owner = owner;
        this.dim = dim;
        this.spell = spell;
        this.start = start;
        this.duration = duration;
        this.seed = seed;
        this.from = from;
        this.to = to;
        this.radius = radius;
        this.dx = dx;
        this.dz = dz;
        this.length = length;
    }

    String ownerKey() { return owner.toString(); }
}
