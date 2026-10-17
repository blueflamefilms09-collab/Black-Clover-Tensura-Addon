package com.newuniverse.nusmp.arc.hub;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.arena.ArcArenaBuilder;
import com.newuniverse.nusmp.arc.world.ArcWorldConfig;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.HubLayout;
import com.newuniverse.nusmp.labyrinth.mega.hub.HubPlazaLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.Map;

/**
 * Why: the Promenade and the Chronicle are new ground in the Hub, and the Hub is a world players have already walked and built beside, so laying them is done
 * with care. It runs from a quiet pass (every five seconds, only while a player is in the Hub near the plaza), only when EVERY chunk of the site is already loaded
 * (it never loads a chunk, so it cannot stall the game from an event), and only when the Hub's saved record says the current build is not in the world. It first
 * clears what grows there by nature (leaves, logs, grass, flowers, snow: the plains' trees reach the plaza's edge) from the paving up to the height of the site,
 * and never anything a player put there (a wall, a chest, a lamp is left standing, and the cell of the site it blocks is simply not laid). Then it lays every cell
 * of {@link ChronicleSite}, bottom to top, in the vanilla blocks of its palette. The paving row replaces whatever ground was there (that IS the site). The same
 * code is the admin's repair ({@code onlyMissing}): it puts back a column an explosion took, and never touches a cell that already holds the right block.
 * Nothing is built while {@code arc.chronicle} is off.
 */
public final class ChronicleBuilder {
    private ChronicleBuilder() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** Bump when the site's formula changes in a way an old world should be told about (an admin repairs; nothing re-lays by itself). */
    public static final int BUILD_VERSION = 1;
    /** The pass runs this often (ticks), offset from the NPC pass so they never share a tick. */
    public static final int EVERY = 100, OFFSET = 50;
    /** A player must be within this many blocks of the plaza's centre for the pass to look at all. */
    public static final double NEAR = 96.0;

    private static boolean registered;

    public static synchronized void register(IEventBus bus) {
        if (registered) return;
        registered = true;
        bus.addListener(ChronicleBuilder::onTick);
    }

    private static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if ((server.getTickCount() + OFFSET) % EVERY != 0 || !ArcWorldConfig.chronicle()) return;
        try {
            ServerLevel hub = server.getLevel(Labyrinth.SANCTUARY_DIMENSION);
            if (hub == null || !playerNear(hub)) return;
            ChronicleSave save = ChronicleSave.get(hub);
            if (save.version() >= BUILD_VERSION || !chunksLoaded(hub)) return;
            build(hub, false);
            save.version(BUILD_VERSION);
        } catch (RuntimeException ex) {
            LOG.error("[nusmp] the Chronicle could not be laid in the Hub: {}", ex.toString());
        }
    }

    private static boolean playerNear(ServerLevel hub) {
        double r2 = NEAR * NEAR;
        for (ServerPlayer p : hub.players()) {
            double dx = p.getX() - HubLayout.PLAZA_X, dz = p.getZ() - HubLayout.PLAZA_Z;
            if (dx * dx + dz * dz <= r2) return true;
        }
        return false;
    }

    /** Is every chunk the site touches loaded right now? (Nothing here ever loads one.) */
    public static boolean chunksLoaded(ServerLevel hub) {
        int r = ChronicleSite.MAX_RADIUS + 1;
        for (int cx = (-r) >> 4; cx <= r >> 4; cx++) for (int cz = (-r) >> 4; cz <= r >> 4; cz++) if (!hub.hasChunk(cx, cz)) return false;
        return true;
    }

    /** Is a block one that grows there by nature, so that clearing it takes nothing from a player? */
    static boolean natural(BlockState s) {
        return s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS)
                || s.is(Blocks.VINE) || s.is(Blocks.SNOW) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.LARGE_FERN) || s.is(Blocks.FERN) || s.is(Blocks.SWEET_BERRY_BUSH);
    }

    /**
     * Lays the site (or, with {@code onlyMissing}, puts back what is missing). Returns how many blocks were set. The chunks must be loaded: the caller checks
     * {@link #chunksLoaded} (the command loads them first, the pass waits for them).
     */
    public static int build(ServerLevel hub, boolean onlyMissing) {
        Map<SitePart, BlockState> states = new EnumMap<>(SitePart.class);
        for (Map.Entry<SitePart, String> e : ChronicleSite.palette().entrySet()) states.put(e.getKey(), ArcArenaBuilder.stateOf(e.getValue()));
        int[] set = {0};
        int r = ChronicleSite.MAX_RADIUS;
        BlockState air = Blocks.AIR.defaultBlockState();
        DungeonGuard.internal(() -> {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            if (!onlyMissing) {
                // the natural growth over the site goes first (over the guarded footprint, a block wider than the site itself, so no crown or vine is left hanging at its edge)
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        if (!ChronicleSite.nearSite(x, z, 0) || (ChronicleSite.onRoad(x, z) && !ChronicleSite.inRotunda(x, z))) continue;   // the roads are left alone
                        if (HubPlazaLayout.inBox(x, ChronicleSite.PAVE_Y + 1, z)) continue;                                                // and the plaza's own box (its hedge and awnings are leaves too)
                        int top = ChronicleSite.nearSiteRotunda(x, z, 1) ? ChronicleSite.OBELISK_TOP_DY + 2 : 8;
                        for (int dy = 1; dy <= top; dy++) {
                            pos.set(x, ChronicleSite.PAVE_Y + dy, z);
                            BlockState have = hub.getBlockState(pos);
                            if (!have.isAir() && natural(have)) {
                                hub.setBlock(pos, air, 2);
                                set[0]++;
                            }
                        }
                    }
                }
            }
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    for (ChronicleSite.Cell c : ChronicleSite.column(x, z)) {
                        pos.set(x, ChronicleSite.PAVE_Y + c.dy(), z);
                        BlockState want = states.get(c.part());
                        if (want == null) continue;
                        BlockState have = hub.getBlockState(pos);
                        if (have.is(want.getBlock())) continue;
                        if (c.dy() > 0 && !natural(have)) continue;                   // a player's block stands in the way: the cell is simply not laid
                        hub.setBlock(pos, want, 2);
                        set[0]++;
                    }
                }
            }
        });
        LOG.info("[nusmp] The Chronicle and the Promenade of the Hub {}: {} blocks set", onlyMissing ? "repaired" : "laid", set[0]);
        return set[0];
    }
}
