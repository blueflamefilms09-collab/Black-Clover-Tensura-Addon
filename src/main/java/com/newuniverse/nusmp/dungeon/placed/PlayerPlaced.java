package com.newuniverse.nusmp.dungeon.placed;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Why: this is the one door to "which blocks of the Dungeon did a player place", so the Dungeon Guard, the backpack and any later system ask and tell the
 * same place and nobody reads the saved data by hand. The Dungeon Guard keeps holding every block of the dungeon; this lets it make ONE exception, the
 * blocks a person put down themselves (a torch, a crafting table, a sleeping bag, a backpack, a plank bridge). The mark is made when a player places a
 * block ({@link PlacedEvents}) or by {@link #mark}; it is dropped when the block is removed, when the dungeon's own builder writes over the position (inside
 * {@link DungeonGuard#internal}), when a chunk or a box is cleared ({@link #clearChunk}, {@link #clearBox}: the hooks for a floor reset), and, oldest
 * first, when a cap is reached. Server side it reads the level's saved data; client side it reads the {@link PlacedMirror} the server fills. Every method is
 * cheap when nothing is marked in the level (one map lookup).
 */
public final class PlayerPlaced {
    private PlayerPlaced() {}

    private static final String NOTICE_STAMP = "nusmp_placed_notice";
    private static final long NOTICE_INTERVAL = 200L;

    /** A level's data, or the fact that it has none yet (so a level nobody built in is looked up once). */
    private static final class Slot { PlacedBlocksData data; }

    private static final Map<Level, Slot> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    // ---------------------------------------------------------------- the switch
    /** Is the exemption in force in this level (the Labyrinth's dimensions, the switch on). Cheap on both sides. */
    public static boolean active(Level level) {
        return level != null && PlacedRules.active(PlacedConfig.get(PlacedConfig.EXEMPT), Labyrinth.in(level));
    }

    private static Slot slot(ServerLevel level) {
        Slot s = CACHE.get(level);
        if (s == null) {
            s = new Slot();
            s.data = PlacedBlocksData.peek(level);
            CACHE.put(level, s);
        }
        return s;
    }

    private static PlacedBlocksData create(ServerLevel level) {
        Slot s = slot(level);
        if (s.data == null) s.data = PlacedBlocksData.get(level);
        return s.data;
    }

    /** Forgets every cached handle (a world was closed: the next level objects are different ones). */
    public static void clearCache() { CACHE.clear(); }

    // ---------------------------------------------------------------- asking
    /** Is this position a block a player placed? True on the server by the saved list, on a client by what the server told it. */
    public static boolean isPlaced(Level level, BlockPos pos) {
        if (level == null || pos == null || !active(level)) return false;
        if (level.isClientSide()) {
            if (!PlacedConfig.get(PlacedConfig.SYNC)) return false;
            return PlacedRules.clientMayCrack(true, PlacedMirror.contains(level.dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ()));
        }
        if (!(level instanceof ServerLevel sl)) return false;
        PlacedBlocksData d = slot(sl).data;
        return d != null && d.set().containsKey(pos.asLong());
    }

    /** The rule registered with {@link DungeonGuard#addBreakRule}: the guard's break, break-speed and left-click handlers let a player-placed block through. */
    public static boolean breakRule(Level level, BlockPos pos) { return isPlaced(level, pos); }

    public static int size(ServerLevel level) {
        PlacedBlocksData d = slot(level).data;
        return d == null ? 0 : d.set().size();
    }

    // ---------------------------------------------------------------- telling
    /** Marks a position as player-placed (server side). Returns true when it was newly marked. */
    public static boolean mark(Level level, BlockPos pos) { return mark(level, pos, null); }

    /** Like {@link #mark(Level, BlockPos)}; {@code who} (may be null) is told, when the cap made the dungeon reclaim their oldest block. */
    public static boolean mark(Level level, BlockPos pos, ServerPlayer who) {
        if (!(level instanceof ServerLevel sl) || pos == null || !active(sl)) return false;
        PlacedBlocksData d = create(sl);
        int[] dropped = {0};
        int r = d.set().addKey(pos.asLong(), old -> {
            dropped[0]++;
            PlacedNet.sendDelta(sl, old, false);
        });
        if (r < 0) return false;
        d.setDirty();
        if (PlacedConfig.get(PlacedConfig.SYNC)) PlacedNet.sendDelta(sl, pos.asLong(), true);
        if (dropped[0] > 0 && who != null && PlacedConfig.get(PlacedConfig.NOTICE)) tellPruned(who);
        return true;
    }

    /** Drops the mark of a position (server side). True when there was one. */
    public static boolean unmark(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl) || pos == null) return false;
        PlacedBlocksData d = slot(sl).data;
        if (d == null || !d.set().removeKey(pos.asLong())) return false;
        d.setDirty();
        PlacedNet.sendDelta(sl, pos.asLong(), false);
        return true;
    }

    /** The block at this position has just been taken away (air, a fluid, fire) by a write that went through: its mark goes with it. */
    public static void released(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) return;
        PlacedBlocksData d = slot(sl).data;
        if (d == null || d.set().isEmpty() || !d.set().containsKey(pos.asLong())) return;
        if (active(sl)) unmark(sl, pos);
    }

    /** The dungeon's own builder is writing here (inside an internal span): the position is the dungeon's again, so a mark on it is dropped. */
    public static void forgetOnBuild(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) return;
        PlacedBlocksData d = slot(sl).data;
        if (d == null || d.set().isEmpty() || !d.set().containsKey(pos.asLong())) return;
        if (active(sl)) unmark(sl, pos);
    }

    // ---------------------------------------------------------------- floor and chunk reset
    /** Forgets every mark of one chunk: the call for a builder that rebuilds a chunk or a floor's chunk. Returns how many. */
    public static int clearChunk(ServerLevel level, int cx, int cz) {
        PlacedBlocksData d = slot(level).data;
        if (d == null) return 0;
        int n = d.set().removeChunk(cx, cz);
        if (n > 0) {
            d.setDirty();
            tellChunkCleared(level, cx, cz);
        }
        return n;
    }

    /** Forgets every mark inside a box (both corners inclusive): the call for a builder that rebuilds a room, a floor's volume or a sector. Returns how many. */
    public static int clearBox(ServerLevel level, BlockPos a, BlockPos b) {
        PlacedBlocksData d = slot(level).data;
        if (d == null) return 0;
        int cx0 = Math.min(a.getX(), b.getX()) >> 4, cx1 = Math.max(a.getX(), b.getX()) >> 4, cz0 = Math.min(a.getZ(), b.getZ()) >> 4, cz1 = Math.max(a.getZ(), b.getZ()) >> 4;
        int n = d.set().removeBox(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
        if (n > 0) {
            d.setDirty();
            for (int cx = cx0; cx <= cx1; cx++) for (int cz = cz0; cz <= cz1; cz++) tellChunkCleared(level, cx, cz);
        }
        return n;
    }

    /** After a bulk change, every player tracking the chunk gets the chunk's marks as they are now. */
    private static void tellChunkCleared(ServerLevel level, int cx, int cz) {
        PlacedBlocksData d = slot(level).data;
        PlacedNet.sendChunkToTrackers(level, cx, cz, d == null ? new long[0] : d.set().chunkKeys(cx, cz));
    }

    private static void tellPruned(ServerPlayer who) {
        CompoundTag data = who.getPersistentData();
        long now = who.level().getGameTime();
        if (!PlacedRules.pruneNoticeDue(now, data.getLong(NOTICE_STAMP), NOTICE_INTERVAL)) return;
        data.putLong(NOTICE_STAMP, now);
        who.displayClientMessage(Component.translatableWithFallback("message.nusmp.guard_exempt.pruned", "The dungeon takes back the oldest block you placed here; it has become part of its stone.").withStyle(ChatFormatting.GRAY), true);
    }
}
