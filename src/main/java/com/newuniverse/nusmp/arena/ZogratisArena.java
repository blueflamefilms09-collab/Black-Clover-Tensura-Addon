package com.newuniverse.nusmp.arena;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Why: Lucius (1.46.0) fights in one place only, a dimension of his own, {@code nusmp:zogratis_arena}, and every other class of the
 * arena package (the builder, the director, the travel, the spawn gate, the guard) compares against the same ids and the same centre. This
 * class is that one place, in the shape of {@link com.newuniverse.nusmp.labyrinth.Labyrinth}: the dimension key, {@code in(Level)} and
 * {@code in(Entity)} (null safe, both sides), and the three points of the courtyard that the other classes need (the floor centre, where
 * Lucius stands on the dais, where the boss's own arena is anchored). The layout around them is {@link ArenaLayout}.
 */
public final class ZogratisArena {
    private ZogratisArena() {}

    /** The dimension of the fight (data/nusmp/dimension/zogratis_arena.json). */
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "zogratis_arena"));

    /** Is this world the arena? Null-safe, both sides (the key is compared, not the level instance). */
    public static boolean in(Level level) { return level != null && level.dimension() == DIMENSION; }

    public static boolean in(Entity e) { return e != null && in(e.level()); }

    /** The block in the middle of the floor (the dais base). */
    public static BlockPos floorCentre() { return new BlockPos(0, ArenaLayout.FLOOR_Y, 0); }

    /** Where the boss stands: on the dais, feet on its top face. */
    public static Vec3 daisStand() { return new Vec3(0.5, ArenaLayout.FLOOR_Y + 2, 0.5); }

    /** The anchor of the boss's own arena (his leash and ring are measured from it). */
    public static BlockPos bossAnchor() { return new BlockPos(0, ArenaLayout.FLOOR_Y + 2, 0); }

    /** The ring of the dais that counts for the five-second start: a player within this horizontal distance of the centre. */
    public static final double DAIS_RING = 12.0;
}
