package com.newuniverse.nusmp.labyrinth;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Why: the Labyrinth (1.35.0, docs/labyrinth.md) is a NEW dimension, {@code nusmp:labyrinth}, and not a rewrite of the Living Dungeon's
 * generator. A world that already exists keeps the dimension generators written into its level.dat, so editing {@code living_dungeon}'s
 * noise settings would leave every old world on the old caves while new worlds got the spire: two worlds with the same dimension id and
 * different terrain. A fresh id with a flat void generator ({@code layers: []}) and {@code structure_overrides} gives Jigsaw total control of
 * what stands there, and the old dimension stays registered untouched (rule 2: nothing is deleted, old worlds load). This class is the one
 * place that spells the ids every other Labyrinth package compares against: the dimension and biome keys, the structure tags that
 * {@code findNearestMapStructure} takes, and the {@code engine} switch that decides whether a Maw leads here or to the legacy caves.
 * Shape copied from {@link com.newuniverse.nusmp.dungeon.LivingDungeon} ({@code DIMENSION}, {@code in(Level)}, {@code in(Entity)}) so
 * code that already tests "is this the Dungeon" reads the same for the Labyrinth.
 */
public final class Labyrinth {
    private Labyrinth() {}

    /** The void sky dimension the Spire is built in (data/nusmp/dimension/labyrinth.json). */
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "labyrinth"));
    /** The one biome of that dimension: no spawners, no features, so only the Jigsaw pieces and what the Sealed Archways call stand there. */
    public static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "labyrinth_sky"));
    /** Structure tag holding the Spire: what {@code ServerLevel.findNearestMapStructure} is asked for from inside the dimension. */
    public static final TagKey<Structure> SPIRE_TAG = TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "labyrinth_spire"));
    /** Structure tag holding the Overworld Gate arch (the way in). */
    public static final TagKey<Structure> GATE_TAG = TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "dungeon_gate"));

    public static final String ENGINE_LABYRINTH = "labyrinth", ENGINE_LEGACY = "legacy";

    /** Is this world the Labyrinth dimension? Null-safe, works on both sides (the key is compared, not the level instance). */
    public static boolean in(Level level) { return level != null && level.dimension() == DIMENSION; }

    /** Is this entity standing in the Labyrinth dimension? */
    public static boolean in(Entity e) { return e != null && in(e.level()); }

    /**
     * Does the {@code labyrinth.engine} switch send Maws, Gates and {@code /multiverse dungeon enter} into the Labyrinth? {@code false} means
     * "legacy": the Living Dungeon caves of 1.17-1.34, which stay registered and reachable. Read through {@link LabyrinthConfig#get} so it is
     * safe to call before the server config is loaded (then the default, the Labyrinth, answers).
     */
    public static boolean engineIsLabyrinth() { return ENGINE_LABYRINTH.equals(LabyrinthConfig.get(LabyrinthConfig.ENGINE)); }
}
