package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonConfig;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.DungeonCollapseEvent;
import com.newuniverse.nusmp.dungeon.nexus.NexusInfo;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The Treasury Collapse (docs/living_dungeon.md section 8): when the central relic leaves its pedestal or the floor boss of a zone dies, a
 * {@link DungeonCollapseEvent} is posted and this class answers it (an admin can start one too: {@link CollapseCommand}). The Dungeon wails
 * and has three minutes to come down: its air goes, its stone falls in bounded waves from the far side toward the exits, a frenzied horde
 * is born from its walls round the players and the Root Gates that open at the exits, space surges under the players' feet and throws them
 * across the floor (the Band of the Spatial Anchor holds), and whoever is still inside at the end is thrown out (see {@link CollapseExpel}),
 * not killed. Any zone's Nexus can fall: nothing here names a zone, the Nexus is the one {@code NexusZones.containing} finds.
 *
 * <p>Three game-bus handlers drive it: {@link #onCollapse} (the event), {@link #onServerTick} (every tick, bounded work) and
 * {@link #onEntityJoin} (a creature of the horde that outlives its collapse is removed when its chunk loads); {@link #onServerStopping}
 * lets go of the statics. A collapse is saved ({@link CollapseSave}) and carries on after a restart. A Nexus is marked COLLAPSED in the
 * Nexus save the moment its collapse starts, so it is never rebuilt and breeds nothing; a collapse outside any standing Nexus is a "pocket":
 * a show of falling rubble that takes nothing from the Dungeon, with the same clock, horde, surges and expulsion.
 */
public final class CollapseManager {
    private CollapseManager() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final List<CollapseRun> ACTIVE = new ArrayList<>();
    /** The Dungeon's level the saved collapses were loaded for; a different one (another world) clears everything. */
    private static ServerLevel seen;

    /** What came of a request to start a collapse. */
    public enum Result {
        STARTED("The Nexus is collapsing."),
        NOT_DUNGEON("A collapse only happens in the Dungeon."),
        DISABLED("The Living Dungeon is switched off."),
        ALREADY("A collapse is already under way within " + CollapseRules.AREA + " blocks of there."),
        NOT_READY("That Nexus is still being built; nothing stands there to bring down."),
        GONE("That Nexus has collapsed already.");

        public final String message;

        Result(String message) { this.message = message; }
    }

    // ---------------------------------------------------------------- the event
    /** The game-bus handler of {@link DungeonCollapseEvent}: starts a collapse at its centre unless one is under way within 96 blocks. */
    public static void onCollapse(DungeonCollapseEvent e) {
        try {
            Result r = start(e.level(), e.centre(), e.zone(), e.cause(), e.bossDefeated());
            if (r != Result.STARTED) LOG.debug("[nusmp] A collapse at {} was not started: {}", e.centre().toShortString(), r);
        } catch (RuntimeException | LinkageError failure) {
            LOG.error("[nusmp] Could not start a Treasury Collapse at {}", e.centre().toShortString(), failure);
        }
    }

    /**
     * Starts a collapse centred on `centre`. The centre is a block of the Dungeon: inside a standing Nexus it brings that whole Nexus down
     * (the footprint, floor and roof the Nexus save recorded); anywhere else it is a pocket round the spot. Refused (and nothing done) if one
     * is under way within 96 blocks, if the Nexus is still being built, or if it has collapsed already and the cause is its boss.
     * `zone` only names a pocket; a Nexus has its own. `cause` is the player behind it, or null.
     */
    public static Result start(ServerLevel level, BlockPos centre, NexusZone zone, ServerPlayer cause, boolean bossDefeated) {
        if (!LivingDungeon.in(level)) return Result.NOT_DUNGEON;
        if (!DungeonConfig.get(DungeonConfig.ENABLED)) return Result.DISABLED;
        MinecraftServer server = level.getServer();
        ensureLoaded(server);
        if (seen != level) return Result.NOT_DUNGEON;
        NexusInfo info = CollapseHooks.containing(level, centre);
        CollapseRules.Mode mode = CollapseRules.modeFor(info == null ? null : info.state, bossDefeated);
        if (mode == CollapseRules.Mode.NOT_READY) return Result.NOT_READY;
        if (mode == CollapseRules.Mode.GONE) return Result.GONE;
        boolean nexus = mode == CollapseRules.Mode.NEXUS;
        if (nexus && !CollapseRules.sane(info.footprint, info.floorY, info.topY)) {
            LOG.warn("[nusmp] The {} at {} has ground no collapse can work on (footprint {}, floor {}, roof {}); it is treated as a pocket", info.zone.label, centre.toShortString(), info.footprint, info.floorY, info.topY);
            nexus = false;
        }
        BlockPos at = nexus ? info.centre() : centre;
        for (CollapseRun r : ACTIVE) {
            if (!r.done() && r.level == level && CollapseRules.sameArea(at.getX(), at.getZ(), r.s.centreX, r.s.centreZ)) return Result.ALREADY;
        }
        CollapseSave save = CollapseSave.get(server);
        CollapseState s = new CollapseState();
        s.id = save.takeId();
        s.startTick = level.getGameTime();
        s.nexus = nexus;
        s.centreX = at.getX();
        s.centreY = at.getY();
        s.centreZ = at.getZ();
        if (nexus) {
            s.zone = info.zone;
            s.cellX = info.cellX;
            s.cellZ = info.cellZ;
            s.fp = info.footprint;
            s.floorY = info.floorY;
            s.topY = info.topY;
        } else {
            CollapseRules.Ground g = CollapseRules.pocket(at.getX(), at.getY(), at.getZ());
            s.zone = zone == null ? NexusZone.at(at.getX(), at.getZ()) : zone;
            s.fp = g.fp();
            s.floorY = g.floorY();
            s.topY = g.topY();
        }
        save.add(s);
        ACTIVE.add(new CollapseRun(level, s, save));
        if (nexus) {
            CollapseHooks.markCollapsed(level, at);                                     // at once: no descent into it, no breeding, no rebuild
            if (CollapseHooks.isCentralRelic(info, centre)) CollapseHooks.markCentreRelicTaken(level, at);
        }
        LOG.info("[nusmp] A Treasury Collapse #{} was triggered at {} ({}{}{})", s.id, centre.toShortString(), s.zone.label, nexus ? "" : ", a pocket", cause == null ? "" : " by " + cause.getName().getString());
        return Result.STARTED;
    }

    // ---------------------------------------------------------------- the server tick
    /** Advances every collapse once a tick (each does bounded work: see {@link CollapseRunner}). */
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ensureLoaded(server);
        if (ACTIVE.isEmpty() || seen == null || !DungeonConfig.get(DungeonConfig.ENABLED)) return;
        long now = seen.getGameTime();
        for (CollapseRun r : new ArrayList<>(ACTIVE)) {
            try {
                r.tick(now);
            } catch (RuntimeException | LinkageError failure) {
                LOG.error("[nusmp] A Treasury Collapse failed outside its own guard", failure);
            }
        }
        ACTIVE.removeIf(CollapseRun::done);
    }

    /** The world is closing: nothing of it may stay behind in the statics (the collapses themselves are saved with the world). */
    public static void onServerStopping(ServerStoppingEvent event) { clear(); }

    private static void clear() {
        ACTIVE.clear();
        seen = null;
    }

    /** Loads the saved collapses of this world the first time the Dungeon is there (and forgets those of another world). Cheap after that. */
    private static void ensureLoaded(MinecraftServer server) {
        ServerLevel level = server.getLevel(LivingDungeon.DIMENSION);
        if (level == seen) return;
        clear();
        if (level == null) return;
        seen = level;
        CollapseSave save = CollapseSave.get(server);
        for (CollapseState s : save.all()) {
            ACTIVE.add(new CollapseRun(level, s, save));
            LOG.info("[nusmp] A Treasury Collapse #{} at ({}, {}) carries on after the restart", s.id, s.centreX, s.centreZ);
        }
    }

    // ---------------------------------------------------------------- horde that outlives its collapse
    /** A creature of the horde coming into the world (loaded from disk): one of a collapse that is over is removed, one of a collapse under way is taken back into its count. */
    public static void onEntityJoin(EntityJoinLevelEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !(e.getEntity() instanceof Mob m) || !m.getPersistentData().contains(CollapseGuards.KEY)) return;
        if (!LivingDungeon.in(level)) return;
        try {
            ensureLoaded(level.getServer());
            long id = m.getPersistentData().getLong(CollapseGuards.KEY);
            CollapseRun owner = null;
            for (CollapseRun r : ACTIVE) if (r.s.id == id && !r.done()) owner = r;
            if (owner == null) e.setCanceled(true);
            else owner.guards.adopt(m.getUUID());
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] Could not place a creature of the collapse's horde that came into the world", failure);
        }
    }

    /**
     * The Dungeon's guard ({@code DungeonGuard.onEntityJoin}, highest priority) cancels the join of every plain falling block, to keep sand
     * and gravity from copying blocks it refused to remove. The ones this collapse starts are made on purpose, after it has taken their
     * block away itself: while {@link CollapseWorld#spawning} says so, the cancel is taken back. Register this one at the LOWEST priority,
     * receiving cancelled events, so that it runs after the guard's.
     */
    public static void onFallingBlockJoin(EntityJoinLevelEvent e) {
        if (CollapseWorld.spawning && e.isCanceled() && e.getEntity() instanceof FallingBlockEntity && !e.loadedFromDisk()) e.setCanceled(false);
    }

    // ---------------------------------------------------------------- questions from outside
    /** Is there a collapse under way at this block (inside its ground or a short way round it)? */
    public static boolean active(ServerLevel level, BlockPos pos) {
        for (CollapseRun r : ACTIVE) if (!r.done() && r.level == level && r.covers(pos)) return true;
        return false;
    }

    /**
     * Must the Dungeon's ecosystem breed nothing in this chunk because a collapse is under way on it? (A Nexus that collapsed is also closed
     * to life by {@code NexusZones.lifeAllowed}; this covers a pocket.) Cheap: the common case is an empty list.
     */
    public static boolean suppressesLife(ServerLevel level, ChunkPos chunk) {
        if (ACTIVE.isEmpty()) return false;
        for (CollapseRun r : ACTIVE) if (!r.done() && r.level == level && r.s.fp.touchesChunk(chunk.x, chunk.z)) return true;
        return false;
    }

    /** Ends the collapse nearest to `from` (or every one) early and restores nothing; returns how many were ended. */
    public static int stop(ServerLevel level, BlockPos from, boolean all) {
        ensureLoaded(level.getServer());
        CollapseRun best = null;
        double bestD = Double.MAX_VALUE;
        int n = 0;
        for (CollapseRun r : new ArrayList<>(ACTIVE)) {
            if (r.done() || r.level != level) continue;
            if (all) {
                r.abort("an admin stopped it");
                n++;
                continue;
            }
            double dx = r.s.centreX - from.getX(), dz = r.s.centreZ - from.getZ(), d = dx * dx + dz * dz;
            if (d < bestD) {
                bestD = d;
                best = r;
            }
        }
        if (best != null) {
            best.abort("an admin stopped it");
            n++;
        }
        ACTIVE.removeIf(CollapseRun::done);
        return n;
    }

    /** One line for every collapse under way in this level, nearest to `from` first. */
    public static List<String> status(ServerLevel level, BlockPos from) {
        ensureLoaded(level.getServer());
        List<CollapseRun> mine = new ArrayList<>();
        for (CollapseRun r : ACTIVE) if (!r.done() && r.level == level) mine.add(r);
        mine.sort((a, b) -> Double.compare(flat(a, from), flat(b, from)));
        List<String> out = new ArrayList<>();
        for (CollapseRun r : mine) out.add(r.describe() + ", " + Math.round(Math.sqrt(flat(r, from))) + " blocks from you");
        return out;
    }

    private static double flat(CollapseRun r, BlockPos from) {
        double dx = r.s.centreX - from.getX(), dz = r.s.centreZ - from.getZ();
        return dx * dx + dz * dz;
    }
}
