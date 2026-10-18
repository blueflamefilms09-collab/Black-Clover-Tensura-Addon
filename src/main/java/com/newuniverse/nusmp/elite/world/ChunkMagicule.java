package com.newuniverse.nusmp.elite.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.elite.world.rules.OreConversion;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: what the world engine remembers about ONE chunk, stored as a NeoForge chunk data attachment (it is saved with the chunk and needs no file of its own). Mutable on purpose (the engine
 * updates it in place and calls {@code chunk.setUnsaved(true)}). Everything is small: the stored density, the event bump, the crystal score, the positions of the Mother Rocks, the ore budget
 * and the time of the last update. A chunk that never had a player near it has no attachment at all (the engine only creates one when it updates the chunk).
 */
public final class ChunkMagicule {
    /** Stored density 0..1 (see MagiculeModel). Valid only when {@link #lastTick} is not 0. */
    public float density;
    /** The signed event bump (decays to 0). */
    public float event;
    /** The weighted count of buds and clusters of the chunk (kept by the blocks' onPlace/onRemove). */
    public int crystalScore;
    /** The Mother Rocks of the chunk, as packed block positions. A small set; the engine drops positions that no longer hold a Mother Rock. */
    public final Set<Long> mothers = new LinkedHashSet<>();
    /** The game time of the last update; 0 = never updated. */
    public long lastTick;
    /** The ore conversion budget (window start, used in the window, total). */
    public final OreConversion.Budget ore = new OreConversion.Budget();

    /** Cached for this session only (not saved): the biome factor of the chunk. NaN = not read yet. */
    public transient double biomeFactor = Double.NaN;

    public ChunkMagicule() {}

    private ChunkMagicule(float density, float event, int crystalScore, List<Long> mothers, long lastTick, long oreWindow, int oreUsed, int oreTotal) {
        this.density = density;
        this.event = event;
        this.crystalScore = Math.max(0, crystalScore);
        this.mothers.addAll(mothers);
        this.lastTick = lastTick;
        this.ore.windowStart = oreWindow;
        this.ore.usedInWindow = oreUsed;
        this.ore.total = oreTotal;
    }

    public boolean initialised() { return lastTick != 0L; }

    public static final Codec<ChunkMagicule> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("density", 0f).forGetter(c -> c.density),
            Codec.FLOAT.optionalFieldOf("event", 0f).forGetter(c -> c.event),
            Codec.INT.optionalFieldOf("crystals", 0).forGetter(c -> c.crystalScore),
            Codec.LONG.listOf().optionalFieldOf("mothers", List.of()).forGetter(c -> new ArrayList<>(c.mothers)),
            Codec.LONG.optionalFieldOf("last", 0L).forGetter(c -> c.lastTick),
            Codec.LONG.optionalFieldOf("oreWindow", 0L).forGetter(c -> c.ore.windowStart),
            Codec.INT.optionalFieldOf("oreUsed", 0).forGetter(c -> c.ore.usedInWindow),
            Codec.INT.optionalFieldOf("oreTotal", 0).forGetter(c -> c.ore.total)
    ).apply(i, ChunkMagicule::new));
}
