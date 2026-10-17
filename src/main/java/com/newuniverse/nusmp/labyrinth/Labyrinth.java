package com.newuniverse.nusmp.labyrinth;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Why: this is the one place that spells the ids every Labyrinth package compares against: the dimension keys and the structure tags that
 * {@code findNearestMapStructure} takes. The Labyrinth (1.35.0, docs/labyrinth.md) began as a floating Spire in its own dimension,
 * {@code nusmp:labyrinth}; since 1.54.0 the Mega-Dungeon ({@code nusmp:mega_dungeon}) is the dungeon, and since 1.58.0 it is the only one: the
 * Spire's generator, its structure set and pools and the Living Dungeon's caves were removed (docs/labyrinth/mega_v2_prune.md). The two old
 * dimension keys stay spelled here ON PURPOSE: a world that ever loaded them has them in its level.dat, so their dimension, dimension type and
 * biome files stay registered as inert data (a missing dimension type there would make the save fail to decode), and the code must still
 * recognise them to hold them with the Dungeon Guard ({@link #in}) and to carry a player out of the removed caves ({@link #inRemovedDungeon}).
 * The shape (a key, {@code in(Level)}, {@code in(Entity)}) is the one the removed {@code LivingDungeon} had, so code that asks "is this the
 * Dungeon" reads the same.
 */
public final class Labyrinth {
    private Labyrinth() {}

    /** The void sky dimension the Spire was built in (data/nusmp/dimension/labyrinth.json, kept as inert data for old saves; nothing generates there since 1.58.0). */
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "labyrinth"));
    /** 1.54.0: the singular, expanding Mega-Dungeon (data/nusmp/dimension/mega_dungeon.json). A NEW dimension, for the same reason the Labyrinth was one: a world keeps the dimension generators written into its level.dat. */
    public static final ResourceKey<Level> MEGA_DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "mega_dungeon"));
    /** 1.55.0: Floor 0, the Sanctuary Hub (data/nusmp/dimension/sanctuary.json): a safe, buildable plains world with two landmarks, the Holy Spire and the Abyssal Chasm. Deliberately NOT a Labyrinth dimension ({@link #in} is false there), so the Dungeon Guard does not hold it and players can build. */
    public static final ResourceKey<Level> SANCTUARY_DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "sanctuary"));
    /** The cave dimension of the Living Dungeon (data/nusmp/dimension/living_dungeon.json), removed in 1.58.0 and kept only as inert data for old saves. Not a Labyrinth dimension. */
    public static final ResourceKey<Level> REMOVED_DUNGEON_DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "living_dungeon"));
    /** 1.54.0: structure tag holding the Gateway of the Mega-Dungeon (one per world); also listed in {@link #GATE_TAG}. */
    public static final TagKey<Structure> MEGA_GATEWAY_TAG = TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "mega_gateway"));
    /** Structure tag holding the Overworld Gate (the way in): the Mega-Dungeon's Gateway. */
    public static final TagKey<Structure> GATE_TAG = TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "dungeon_gate"));

    /**
     * Is this world a Labyrinth dimension: the Mega-Dungeon ({@link #MEGA_DIMENSION}) or, in an old world that remembers it, the Spire's sky
     * ({@link #DIMENSION})? Null-safe, works on both sides (the key is compared, not the level instance). Both count because everything that was
     * written for "the Labyrinth" (the Dungeon Guard, the economy, the Vital Strike, the gloom, the relic exclusivity, the void rescue) must hold in
     * the new dimension too without one of those classes being edited; code that needs only the Mega-Dungeon asks {@link #inMega}.
     */
    public static boolean in(Level level) { return level != null && (level.dimension() == DIMENSION || level.dimension() == MEGA_DIMENSION); }

    /** Is this world the Mega-Dungeon dimension (1.54.0)? */
    public static boolean inMega(Level level) { return level != null && level.dimension() == MEGA_DIMENSION; }

    /** Is this world Floor 0, the Sanctuary Hub (1.55.0)? Not a Labyrinth dimension: {@link #in} stays false there. */
    public static boolean inSanctuary(Level level) { return level != null && level.dimension() == SANCTUARY_DIMENSION; }

    /** Is this world the cave dimension of the removed Living Dungeon (kept registered as inert data)? A player found there is carried out. */
    public static boolean inRemovedDungeon(Level level) { return level != null && level.dimension() == REMOVED_DUNGEON_DIMENSION; }

    /** Is this entity standing in a Labyrinth dimension? */
    public static boolean in(Entity e) { return e != null && in(e.level()); }
}
