package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.mana.method.TrueBlockRules.Kind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * The real blocks of True Magic: placement and the ledger that takes them back. A bolt that ends at a wall or a creature calls {@link #place}; the rules are
 * {@link TrueBlockRules}.
 *
 * <p>Safety: only loaded chunks (it never loads one), only into free cells (air, or a replaceable block with no fluid), only where the caster may build ({@code mayInteract}:
 * spawn protection and the world border are respected), never in the Labyrinth or any world the Dungeon Guard holds, fire only when {@code doFireTick} is on and the config allows
 * it. At most {@link TrueBlockRules#PER_CAST} blocks per cast, {@link TrueBlockRules#PER_CASTER} standing per caster, {@link TrueBlockRules#GLOBAL} in the world, a few block reads per
 * cell. Every block is removed after the configured time by {@link #tick} (and when the server stops), but only if it is still the block that was placed.
 *
 * <p>The ledger is memory only: a block placed just before a crash stays (water or stone in the world). That is the price of not touching saved data; the blocks are few.
 */
public final class TrueBlocks {
    private TrueBlocks() {}

    private record Entry(ResourceKey<Level> dim, BlockPos pos, Block block, UUID owner, long until) {}

    private static final List<Entry> LEDGER = new ArrayList<>();
    private static final int CHECK_EVERY = 10;
    /** A block in an unloaded chunk is retried this long after it expired, then forgotten. */
    private static final long RETRY_TICKS = 1200;

    public static int count() { return LEDGER.size(); }

    public static int liveOf(UUID owner) {
        int n = 0;
        for (Entry e : LEDGER) if (e.owner.equals(owner)) n++;
        return n;
    }

    /** The default permission: the chunk is loaded and the player may interact with the spot (spawn protection, world border). */
    public static BiPredicate<ServerLevel, BlockPos> mayBuild(ServerPlayer p) {
        return (l, pos) -> l.isLoaded(pos) && (p == null || l.mayInteract(p, pos));
    }

    /** True Magic never writes blocks in the Labyrinth or in a world the Dungeon Guard holds. */
    public static boolean levelAllows(ServerLevel level) { return !Labyrinth.in(level) && !DungeonGuard.on(level); }

    /**
     * Places the blocks of {@code element} round {@code at} (the impact) for {@code owner}; returns how many were placed (0 for wind and lightning, when the config is off, or when
     * nothing was free). {@code may} decides per cell whether the caster may build there.
     */
    public static int place(ServerLevel level, UUID owner, Vec3 at, RuneElement element, BiPredicate<ServerLevel, BlockPos> may, long now) {
        Kind kind = TrueBlockRules.kindOf(element);
        if (kind == Kind.NONE || !ManaMethodConfig.trueBlocks() || !levelAllows(level)) return 0;
        if (kind == Kind.FIRE && (!ManaMethodConfig.trueFire() || !level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK))) return 0;
        int want = TrueBlockRules.allowed(TrueBlockRules.blocksWanted(kind), liveOf(owner), LEDGER.size(), TrueBlockRules.PER_CASTER, TrueBlockRules.GLOBAL);
        if (want <= 0) return 0;
        ResourceLocation id = ResourceLocation.parse(TrueBlockRules.blockId(kind));
        Block block = BuiltInRegistries.BLOCK.get(id);
        long until = now + TrueBlockRules.seconds(ManaMethodConfig.blockSeconds()) * 20L;
        BlockPos base = BlockPos.containing(at);
        int placed = 0;
        for (int i = 0; i < TrueBlockRules.cellCount() && placed < want; i++) {
            int[] c = TrueBlockRules.cell(i);
            BlockPos cell = find(level, base.offset(c[0], 0, c[1]), kind, may);
            if (cell == null) continue;
            BlockState state = kind == Kind.FIRE ? BaseFireBlock.getState(level, cell) : block.defaultBlockState();
            if (kind == Kind.FIRE && !state.canSurvive(level, cell)) continue;
            if (!level.setBlock(cell, state, 3)) continue;
            LEDGER.add(new Entry(level.dimension(), cell.immutable(), state.getBlock(), owner, until));
            placed++;
        }
        return placed;
    }

    /** The free cell for this kind at or near {@code pos}: the cell itself, one up (a wall was hit), or down to three below for the kinds that need the ground. */
    private static BlockPos find(ServerLevel level, BlockPos pos, Kind kind, BiPredicate<ServerLevel, BlockPos> may) {
        boolean ground = TrueBlockRules.needsGround(kind);
        BlockPos up = pos.above();
        for (int dy = 0; dy <= 3; dy++) {
            BlockPos q = pos.below(dy);
            if (free(level, q, may) && (!ground || solidBelow(level, q))) return q;
            if (dy == 0 && free(level, up, may) && (!ground || solidBelow(level, up))) return up;
            if (!ground) break;                                                     // water is set at the cell or one up, never lowered
        }
        return null;
    }

    private static boolean free(ServerLevel level, BlockPos pos, BiPredicate<ServerLevel, BlockPos> may) {
        if (!level.isLoaded(pos) || !may.test(level, pos)) return false;
        BlockState s = level.getBlockState(pos);
        return s.isAir() || (s.canBeReplaced() && s.getFluidState().isEmpty());
    }

    private static boolean solidBelow(ServerLevel level, BlockPos pos) {
        BlockPos d = pos.below();
        return level.isLoaded(d) && level.getBlockState(d).isFaceSturdy(level, d, Direction.UP);
    }

    // ---------------------------------------------------------------- expiry
    /** Called every server tick: nothing to do while the ledger is empty; otherwise one pass every {@value #CHECK_EVERY} ticks. */
    public static void tick(MinecraftServer server, long now) {
        if (LEDGER.isEmpty() || now % CHECK_EVERY != 0) return;
        for (Iterator<Entry> it = LEDGER.iterator(); it.hasNext(); ) {
            Entry e = it.next();
            if (now < e.until) continue;
            ServerLevel l = server.getLevel(e.dim);
            if (l == null) { it.remove(); continue; }
            if (!l.isLoaded(e.pos)) {
                if (now > e.until + RETRY_TICKS) it.remove();
                continue;
            }
            takeBack(l, e);
            it.remove();
        }
    }

    private static void takeBack(ServerLevel l, Entry e) {
        if (l.getBlockState(e.pos).is(e.block)) DungeonGuard.internal(() -> l.removeBlock(e.pos, false));
    }

    /** Takes back every block still standing (the server is stopping, or a test ends). */
    public static void clearAll(MinecraftServer server) {
        for (Entry e : LEDGER) {
            ServerLevel l = server == null ? null : server.getLevel(e.dim);
            if (l != null && l.isLoaded(e.pos)) takeBack(l, e);
        }
        LEDGER.clear();
    }

    /** Test hook: expires everything standing now (as if {@code now} were long past). */
    public static void expireAllForTest(MinecraftServer server) {
        for (Iterator<Entry> it = LEDGER.iterator(); it.hasNext(); ) {
            Entry e = it.next();
            ServerLevel l = server.getLevel(e.dim);
            if (l != null && l.isLoaded(e.pos)) takeBack(l, e);
            it.remove();
        }
    }

    /** The positions standing for an owner (tests and the debug command). */
    public static List<BlockPos> positionsOf(UUID owner) {
        List<BlockPos> out = new ArrayList<>();
        for (Entry e : LEDGER) if (e.owner.equals(owner)) out.add(e.pos);
        return out;
    }
}
