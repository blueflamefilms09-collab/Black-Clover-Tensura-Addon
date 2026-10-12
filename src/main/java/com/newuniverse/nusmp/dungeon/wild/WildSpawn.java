package com.newuniverse.nusmp.dungeon.wild;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonCreature;
import com.newuniverse.nusmp.dungeon.DungeonEcology;
import com.newuniverse.nusmp.dungeon.DungeonEntities;
import com.newuniverse.nusmp.dungeon.DungeonTier;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

/**
 * How the Living Dungeon's ecosystem brings a creature in (docs/living_dungeon.md section 1): the births of DungeonEcology.population are
 * drawn from the roster of Wildlife instead of the custom classes. A creature is made with {@code type.create} + {@code finalizeSpawn(EVENT)}
 * + {@code addFreshEntity}: a direct finalizeSpawn call does not fire the FinalizeSpawnEvent, so the Dungeon's own cancel hook (which stops
 * the world's natural spawning) does not stop it.
 */
public final class WildSpawn {
    private WildSpawn() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** The share of births that go to the water when the tier has creatures that live in it. */
    private static final float WATER_SHARE = 0.25f;
    private static final Set<String> warned = new HashSet<>();

    // ---------------------------------------------------------------- an ordinary birth
    /** Brings one creature of this tier (1-3) into the chunk, out of sight of the players. Returns true if one came. */
    public static boolean birth(ServerLevel d, LevelChunk c, int tier) {
        Roster roster = Wildlife.roster();
        if (roster.count(tier) == 0) return custom(d, c, tier);                // neither Tensura nor Ice and Fire gave this tier anything
        RandomSource r = d.getRandom();
        boolean water = roster.hasAquatic(tier) && r.nextFloat() < WATER_SHARE;
        int floor = playerFloor(d, c, r);                                       // the height the players near this chunk are at
        for (int t = 0; t < 4; t++) {
            int x = c.getPos().getMinBlockX() + r.nextInt(16), z = c.getPos().getMinBlockZ() + r.nextInt(16);
            BlockPos p = floor == Integer.MIN_VALUE ? (water ? waterNear(d, x, z, d.getMaxBuildHeight()) : LivingDungeon.column(d, x, z))
                    : water ? waterNear(d, x, z, floor) : standingNear(d, x, z, floor);
            if (p == null) {
                water = false;                                                  // no water here: try the floor
                continue;
            }
            if (d.getNearestPlayer(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 10, false) != null) continue;
            Roster.Entry e = roster.pick(tier, DungeonTier.at(p.getX(), p.getY(), p.getZ()).ordinal(), water, r.nextDouble());
            if (e == null) {
                water = false;
                continue;
            }
            if (spawnId(d, e.id(), p, r) != null) return true;
        }
        return false;
    }

    /** Brings one dragon-class creature into the chunk, in the tier of the Dungeon that suits it, far from the players and from any other. */
    public static boolean birthLegend(ServerLevel d, LevelChunk c) {
        Roster roster = Wildlife.roster();
        RandomSource r = d.getRandom();
        Roster.Entry e = roster.pickLegend(r.nextDouble());
        if (e == null) return false;
        ChunkPos cp = c.getPos();
        AABB around = new AABB(cp.getMiddleBlockX() - 128, d.getMinBuildHeight(), cp.getMiddleBlockZ() - 128, cp.getMiddleBlockX() + 128, d.getMaxBuildHeight(), cp.getMiddleBlockZ() + 128);
        if (!d.getEntitiesOfClass(Mob.class, around, m -> Wildlife.isLegendary(m)).isEmpty()) return false;     // one at a time
        int home = Roster.legendHome(e.path());
        for (int t = 0; t < 6; t++) {
            int x = cp.getMinBlockX() + r.nextInt(16), z = cp.getMinBlockZ() + r.nextInt(16);
            BlockPos p = standing(d, x, z, home);
            if (p == null || d.getNearestPlayer(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 24, false) != null) continue;
            if (spawnId(d, e.id(), p, r) != null) return true;
        }
        return false;
    }

    /** With neither Tensura nor Ice and Fire present the Dungeon would stay empty: the 1.18.0 creatures fill in. */
    private static boolean custom(ServerLevel d, LevelChunk c, int tier) {
        EntityType<? extends DungeonCreature> type;
        if (tier == 1) type = DungeonEntities.SLIME.get();
        else if (tier == 2) type = DungeonEntities.LIVING_ARMOR.get();
        else if (d.getRandom().nextBoolean()) type = DungeonEntities.DIREWOLF.get();
        else type = DungeonEntities.BASILISK.get();
        return DungeonEcology.spawn(d, c, type);
    }

    // ---------------------------------------------------------------- the guard
    /** A tier-2 creature standing at the given block: the Armorsaurus if the roster has it, else another by weight. */
    public static boolean guard(ServerLevel d, BlockPos at) {
        Roster roster = Wildlife.roster();
        RandomSource r = d.getRandom();
        String id = null;
        if (roster.contains(2, "tensura:armorsaurus")) id = "tensura:armorsaurus";
        else {
            Roster.Entry e = roster.pick(2, DungeonTier.at(at.getX(), at.getY(), at.getZ()).ordinal(), false, r.nextDouble());
            if (e != null) id = e.id();
        }
        if (id != null && (spawnId(d, id, at, r) != null || spawnId(d, id, at.above(), r) != null)) return true;
        if (id != null) return false;
        return spawnType(d, DungeonEntities.LIVING_ARMOR.get(), at, r, MobSpawnType.EVENT, false) != null;           // no roster at all: the old Living Armor
    }

    // ---------------------------------------------------------------- making one
    /** Spawns the creature with this id at a standing place; null if the type is not registered, has no room, or would not be made. */
    public static Mob spawnId(ServerLevel d, String id, BlockPos at, RandomSource r) {
        EntityType<?> type = Wildlife.typeOf(id);
        return type == null ? null : spawnType(d, type, at, r, MobSpawnType.EVENT, true);
    }

    /**
     * The one way a creature is made here: create, place, finalize (as an event spawn, so the Dungeon's spawn hook leaves it alone), mark it
     * as staying, add. Null if the type is not a mob, there is no room for it (when asked), or the mob's own setup fails.
     */
    public static Mob spawnType(ServerLevel d, EntityType<?> type, BlockPos at, RandomSource r, MobSpawnType how, boolean needRoom) {
        try {
            Entity made = type.create(d);
            if (!(made instanceof Mob m)) {
                if (made != null) made.discard();
                return null;
            }
            m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, r.nextFloat() * 360f, 0f);
            if (needRoom && !d.noCollision(m.getBoundingBox())) {
                m.discard();
                return null;
            }
            m.finalizeSpawn(d, d.getCurrentDifficultyAt(at), how, null);
            m.setPersistenceRequired();                                          // the ecosystem decides how long it lives, not the despawner
            d.addFreshEntity(m);
            return m;
        } catch (RuntimeException | LinkageError ex) {
            String key = Wildlife.idOf(type) + ":" + ex.getClass().getName();
            if (warned.add(key)) LOG.warn("Living Dungeon: could not spawn {}", Wildlife.idOf(type), ex);
            return null;
        }
    }

    // ---------------------------------------------------------------- finding a place
    private static boolean standable(ServerLevel d, BlockPos p) {
        return d.getBlockState(p).isAir() && d.getBlockState(p.above()).isAir() && d.getFluidState(p).isEmpty() && d.getFluidState(p.below()).isEmpty()
                && d.getBlockState(p.below()).isFaceSturdy(d, p.below(), Direction.UP);
    }

    /** A standing place in this column, from the roof down, that lies in the given tier of the Dungeon (Roster.UPPER..CITY); null if there is none. */
    private static BlockPos standing(ServerLevel d, int x, int z, int wantedTier) {
        int top = d.getMaxBuildHeight() - 8;
        if (wantedTier == Roster.CITY) top = Math.min(top, DungeonTier.CITY_Y - 1);
        for (int y = top; y > d.getMinBuildHeight() + 4; y--) {
            BlockPos p = new BlockPos(x, y, z);
            if (standable(d, p) && DungeonTier.at(x, y, z).ordinal() == wantedTier) return p;
        }
        return null;
    }

    /** Water at least three blocks deep in this column, the spot nearest to the height y0 (so just under a surface); null if there is none. */
    private static BlockPos waterNear(ServerLevel d, int x, int z, int y0) {
        BlockPos best = null;
        int bestGap = Integer.MAX_VALUE;
        for (int y = d.getMaxBuildHeight() - 8; y > d.getMinBuildHeight() + 4; y--) {
            int gap = Math.abs(y - y0);
            if (best != null && gap >= bestGap) {
                if (y <= y0) break;
                continue;
            }
            BlockPos p = new BlockPos(x, y, z);
            if (d.getFluidState(p).is(FluidTags.WATER) && d.getFluidState(p.above()).is(FluidTags.WATER) && d.getFluidState(p.below()).is(FluidTags.WATER)) {
                best = p;
                bestGap = gap;
            }
        }
        return best;
    }

    /** A standing place in this column, the one nearest to the height y0, so life appears on the floor the players are on and not only under the roof; null if there is none. */
    private static BlockPos standingNear(ServerLevel d, int x, int z, int y0) {
        BlockPos best = null;
        int bestGap = Integer.MAX_VALUE;
        for (int y = d.getMaxBuildHeight() - 8; y > d.getMinBuildHeight() + 4; y--) {
            int gap = Math.abs(y - y0);
            if (best != null && gap >= bestGap) {
                if (y <= y0) break;
                continue;
            }
            BlockPos p = new BlockPos(x, y, z);
            if (standable(d, p)) {
                best = p;
                bestGap = gap;
            }
        }
        return best;
    }

    /** The height of the player nearest to the chunk, with a little scatter so the neighbouring floors fill too; Integer.MIN_VALUE if nobody is near. */
    private static int playerFloor(ServerLevel d, LevelChunk c, RandomSource r) {
        ChunkPos cp = c.getPos();
        ServerPlayer near = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer p : d.players()) {
            if (p.isSpectator()) continue;
            double dx = p.getX() - cp.getMiddleBlockX(), dz = p.getZ() - cp.getMiddleBlockZ(), dist = dx * dx + dz * dz;
            if (dist < best) {
                best = dist;
                near = p;
            }
        }
        return near == null ? Integer.MIN_VALUE : near.blockPosition().getY() + r.nextInt(25) - 12;
    }
}
