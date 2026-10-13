package com.newuniverse.nusmp.dungeon.sentient;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The wall birth: the Dungeon's way of bringing a mob into the world. A wall block beside a standing place cracks (the vanilla crack
 * stages grow over the delay, block dust falls, the stone grunts), then gives way in the real break effect and the mob steps out. Not one
 * block ever changes: the Dungeon is unbreakable and the crack is only a picture. The births wait in one list, handled by one tick handler
 * (at most 64 at a time; the rest are dropped with a log line). A birth whose level, wall or chunk has gone is dropped without a sound, and
 * so is one that finds a player within three blocks of its spot or no player within 80 blocks at all.
 */
public final class WallBirth {
    private WallBirth() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final int MAX_PENDING = 64, MAX_PROBES = 16_000, SCAN_HEIGHT = 6;
    private static final double SPACING = 3.0, TOO_CLOSE = 3.0, WITNESS = 80.0;
    /** How far from the wall a mob that does not fit against it is moved, step by step. */
    private static final double[] SHIFTS = {0.0, 0.3, 0.6, 0.9};
    /** A birth with at least this long a delay is a loud one (a Juggernaut's): it is heard far and wide. */
    private static final int LOUD_DELAY = 60;

    /** Makes the mob of one birth: positioned at the spot, finalized, NOT added to the level. Null skips the birth. */
    public interface Factory {
        Mob create(ServerLevel level, BlockPos spot);
    }

    private static final class Pending {
        final ServerLevel level;
        final BlockPos spot, wall;
        final int id, delay;
        final long start;
        final Factory factory;
        final Consumer<Mob> onBorn;

        Pending(ServerLevel level, BlockPos spot, BlockPos wall, int id, long start, int delay, Factory factory, Consumer<Mob> onBorn) {
            this.level = level;
            this.spot = spot;
            this.wall = wall;
            this.id = id;
            this.start = start;
            this.delay = delay;
            this.factory = factory;
            this.onBorn = onBorn;
        }

        boolean loud() { return delay >= LOUD_DELAY; }
    }

    private static final List<Pending> PENDING = new ArrayList<>();
    /** The ids the crack of each birth is drawn with: negative, so they never meet an entity's id (the client keeps one crack per id). */
    private static int nextId = -1;
    private static boolean overflowLogged;
    private static final Set<String> WARNED = new HashSet<>();

    // ---------------------------------------------------------------- scheduling
    /**
     * Looks for up to {@code count} standing places (water to swim in, if the position {@code around} is under water) between minDist and
     * maxDist blocks of {@code around}, in loaded chunks only, each beside a wall and at least three blocks from one another, and
     * schedules a birth at each. Returns how many were scheduled.
     */
    public static int birth(ServerLevel level, BlockPos around, int count, int minDist, int maxDist, int delayTicks, Factory factory, Consumer<Mob> onBorn) {
        if (level == null || around == null) return 0;
        BlockState here = Spots.stateAt(level, around.getX(), around.getY(), around.getZ());
        boolean wet = here != null && here.getFluidState().is(FluidTags.WATER);
        return birthAt(level, findSpots(level, around, count, minDist, maxDist, wet), delayTicks, factory, onBorn);
    }

    /** The same for spots the caller already chose. */
    public static int birthAt(ServerLevel level, List<BlockPos> spots, int delayTicks, Factory factory, Consumer<Mob> onBorn) {
        if (level == null || spots == null || factory == null) return 0;
        long now = level.getServer().getTickCount();
        int delay = BirthTiming.clamp(delayTicks);
        int scheduled = 0;
        for (BlockPos spot : spots) {
            if (PENDING.size() >= MAX_PENDING) {
                if (!overflowLogged) {
                    overflowLogged = true;
                    LOG.warn("[nusmp] Too many wall births waiting ({}): the rest are dropped", MAX_PENDING);
                }
                break;
            }
            BlockPos wall = wallFor(level, spot);
            if (wall == null) continue;
            PENDING.add(new Pending(level, spot.immutable(), wall, nextId(), now, delay, factory, onBorn));
            scheduled++;
        }
        return scheduled;
    }

    private static int nextId() {
        int id = nextId--;
        if (nextId < -2_000_000_000) nextId = -1;
        return id;
    }

    /**
     * Standing places (or, with {@code wet}, swimming places) for births, as {@link #birth} finds them: a bounded scan of the loaded chunks
     * round {@code around} (every other block, six blocks up and down, at most 16 000 probes), each place beside a wall and between the two
     * distances, shuffled and thinned to at least three blocks apart, at most {@code count} of them.
     */
    public static List<BlockPos> findSpots(ServerLevel level, BlockPos around, int count, int minDist, int maxDist, boolean wet) {
        List<BlockPos> out = new ArrayList<>();
        if (level == null || around == null || count <= 0) return out;
        int max = Math.max(1, maxDist), min = Math.max(0, Math.min(minDist, max));
        double min2 = (double) min * min, max2 = (double) max * max;
        double ox = around.getX() + 0.5, oy = around.getY(), oz = around.getZ() + 0.5;
        List<BlockPos> found = new ArrayList<>();
        int probes = 0;
        scan:
        for (int x = around.getX() - max; x <= around.getX() + max; x += 2) {
            for (int z = around.getZ() - max; z <= around.getZ() + max; z += 2) {
                if (!Spots.loaded(level, x, z)) continue;
                for (int y = around.getY() - SCAN_HEIGHT; y <= around.getY() + SCAN_HEIGHT; y++) {
                    if (++probes > MAX_PROBES) break scan;
                    double dx = x + 0.5 - ox, dy = y - oy, dz = z + 0.5 - oz;
                    double d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 < min2 || d2 > max2) continue;
                    boolean fits = wet ? Spots.swimmable(level, x, y, z) : Spots.standable(level, x, y, z);
                    if (fits && Spots.wallBeside(level, x, y, z) != null) found.add(new BlockPos(x, y, z));
                }
            }
        }
        shuffle(found, level.getRandom());
        for (BlockPos p : found) {
            boolean apart = true;
            for (BlockPos q : out) {
                if (p.distSqr(q) < SPACING * SPACING) {
                    apart = false;
                    break;
                }
            }
            if (!apart) continue;
            out.add(p);
            if (out.size() >= count) break;
        }
        return out;
    }

    private static void shuffle(List<BlockPos> list, RandomSource rnd) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            BlockPos t = list.get(i);
            list.set(i, list.get(j));
            list.set(j, t);
        }
    }

    /** The wall that cracks for a spot: a solid block beside it, else the one over its head, else the floor; null if there is none. */
    private static BlockPos wallFor(ServerLevel level, BlockPos spot) {
        Direction d = Spots.wallBeside(level, spot.getX(), spot.getY(), spot.getZ());
        if (d != null) return spot.relative(d);
        for (BlockPos p : new BlockPos[]{spot.above(2), spot.below()}) {
            BlockState s = Spots.stateAt(level, p.getX(), p.getY(), p.getZ());
            if (s != null && !s.isAir() && !DungeonGuard.isFixture(s) && s.getFluidState().isEmpty()) return p;
        }
        return null;
    }

    /** Would a creature of this type fit standing at the spot, moved away from its wall as a birth does it? (A big creature needs more room than a small one.) */
    public static boolean fits(ServerLevel level, BlockPos spot, EntityType<?> type) {
        Direction d = Spots.wallBeside(level, spot.getX(), spot.getY(), spot.getZ());
        int sx = d == null ? 0 : d.getStepX(), sz = d == null ? 0 : d.getStepZ();
        double cx = spot.getX() + 0.5, cz = spot.getZ() + 0.5, cy = spot.getY();
        for (double shift : SHIFTS) {
            if (level.noCollision(type.getDimensions().makeBoundingBox(cx - sx * shift, cy, cz - sz * shift))) return true;
        }
        return false;
    }

    /** How many births are waiting within the radius of the position (they count against a player's cap like the mobs already born). */
    static int pendingNear(ServerLevel level, BlockPos pos, double radius) {
        int n = 0;
        double r2 = radius * radius;
        for (Pending p : PENDING) if (p.level == level && p.spot.distSqr(pos) <= r2) n++;
        return n;
    }

    /** How many births are waiting. */
    public static int pendingCount() { return PENDING.size(); }

    // ---------------------------------------------------------------- the tick
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) {
            overflowLogged = false;
            return;
        }
        long now = event.getServer().getTickCount();
        for (int i = 0; i < PENDING.size(); ) {                              // by index: a callback of a birth may schedule more, which join the end of the list
            Pending p = PENDING.get(i);
            boolean keep;
            try {
                keep = step(p, now);
            } catch (RuntimeException | LinkageError failure) {
                keep = false;
                warn("a wall birth", failure);
            }
            if (keep) i++;
            else PENDING.remove(i);
        }
    }

    /** The births are forgotten with the world. */
    public static void onServerStopping(ServerStoppingEvent event) {
        PENDING.clear();
        overflowLogged = false;
        nextId = -1;
    }

    /** One look at one birth; false when it is over (done or dropped). */
    private static boolean step(Pending p, long now) {
        ServerLevel level = p.level;
        MinecraftServer server = level.getServer();
        if (server == null || server.getLevel(level.dimension()) != level || !Spots.loaded(level, p.spot.getX(), p.spot.getZ()) || !Spots.loaded(level, p.wall.getX(), p.wall.getZ())) {
            return false;                                                    // the level, the wall or the chunk is gone: nothing was shown that needs undoing the client cannot do itself
        }
        int age = (int) (now - p.start);
        if (BirthTiming.done(age, p.delay)) {
            finish(p);
            return false;
        }
        if (BirthTiming.crackDue(age)) crack(p, age);
        return true;
    }

    private static void crack(Pending p, int age) {
        ServerLevel level = p.level;
        BlockState wall = Spots.stateAt(level, p.wall.getX(), p.wall.getY(), p.wall.getZ());
        if (wall == null || wall.isAir()) return;
        int stage = BirthTiming.stage(age, p.delay);
        level.destroyBlockProgress(p.id, p.wall, stage);
        double fx = p.wall.getX() + 0.5 + 0.5 * Integer.signum(p.spot.getX() - p.wall.getX());
        double fy = p.wall.getY() + 0.5 + 0.5 * Integer.signum(p.spot.getY() - p.wall.getY());
        double fz = p.wall.getZ() + 0.5 + 0.5 * Integer.signum(p.spot.getZ() - p.wall.getZ());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, wall), fx, fy, fz, 4, 0.2, 0.2, 0.2, 0.05);
        if (age % 8 == 0) level.sendParticles(ParticleTypes.ASH, fx, fy, fz, 2, 0.25, 0.25, 0.25, 0.01);
        else level.sendParticles(ParticleTypes.SMOKE, fx, fy, fz, 1, 0.15, 0.15, 0.15, 0.01);
        level.playSound(null, p.wall, wall.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.5f, 0.55f + 0.05f * stage);
    }

    private static void finish(Pending p) {
        ServerLevel level = p.level;
        level.destroyBlockProgress(p.id, p.wall, -1);
        BlockState wall = Spots.stateAt(level, p.wall.getX(), p.wall.getY(), p.wall.getZ());
        if (wall != null && !wall.isAir()) level.levelEvent(null, 2001, p.wall, Block.getId(wall));          // the real break effect (dust and sound); the block stays
        level.playSound(null, p.spot, SoundEvents.WARDEN_DIG, SoundSource.HOSTILE, p.loud() ? 1.3f : 0.7f, 0.7f);
        if (p.loud()) level.playSound(null, p.spot, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.0f, 0.8f);
        if (!witnessed(level, p.spot) || playerNear(level, p.spot, TOO_CLOSE)) return;
        Mob mob = p.factory.create(level, p.spot);
        if (mob == null) return;
        if (!place(level, mob, p) || !level.addFreshEntity(mob)) {
            mob.discard();
            return;
        }
        mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 3, false, false));            // a second of grace so the first thing a player sees is the mob, not its death
        level.sendParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), p.loud() ? 16 : 6, 0.3, 0.4, 0.3, 0.02);
        if (p.onBorn != null) p.onBorn.accept(mob);
        if (mob.isAlive() && !mob.isRemoved() && FloorRoster.isBorn(mob)) BornDriver.track(mob);          // a callback may have let it go again
    }

    /** Puts the mob at its spot, moved away from the wall step by step until its box is clear; false if it does not fit anywhere along the way. */
    private static boolean place(ServerLevel level, Mob mob, Pending p) {
        int sx = Integer.signum(p.wall.getX() - p.spot.getX()), sz = Integer.signum(p.wall.getZ() - p.spot.getZ());
        double cx = p.spot.getX() + 0.5, cz = p.spot.getZ() + 0.5, cy = p.spot.getY();
        float yaw = mob.getYRot();
        for (double shift : SHIFTS) {
            mob.moveTo(cx - sx * shift, cy, cz - sz * shift, yaw, 0f);
            if (level.noCollision(mob.getBoundingBox())) return true;
        }
        return false;
    }

    private static boolean witnessed(ServerLevel level, BlockPos spot) {
        double r2 = WITNESS * WITNESS;
        for (ServerPlayer pl : level.players()) if (!pl.isSpectator() && pl.distanceToSqr(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5) <= r2) return true;
        return false;
    }

    private static boolean playerNear(ServerLevel level, BlockPos spot, double radius) {
        double r2 = radius * radius;
        for (ServerPlayer pl : level.players()) if (!pl.isSpectator() && pl.distanceToSqr(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5) <= r2) return true;
        return false;
    }

    private static void warn(String what, Throwable failure) {
        if (WARNED.add(what + failure.getClass().getName())) LOG.warn("[nusmp] Trouble with {} (logged once)", what, failure);
    }
}
