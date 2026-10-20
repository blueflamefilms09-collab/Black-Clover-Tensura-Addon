package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.newuniverse.nusmp.labyrinth.mega.rift.PocketRealm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: everything about a live exclusive tomb that is DERIVED, not saved. The saved record of a tomb ({@link PocketRealm.Pocket}) is a few numbers: which tomb, its seed, its clock, what has been done
 * (rooms woken, seal opened, relic taken). The floor plan, the palette, the world position of every seal block, pedestal and chest, the timers of the traps and the build and clear jobs are all
 * functions of that record plus the definition, so they are rebuilt on demand after a restart instead of being saved: a datapack that tunes a tomb takes effect for the next tomb, and a record can
 * never disagree with a stale copy of its own plan. One runtime per slot, kept in a map keyed by slot and checked against the tomb's id and seed (a slot is reused by the next tomb).
 */
public final class PocketRuntime {
    private static final Map<Integer, PocketRuntime> LIVE = new HashMap<>();

    public final int slot;
    public final String pocketId;
    public final long seed;
    public final PocketDef def;
    public final PocketLayout layout;
    public final PocketPlan plan;
    public final PocketPalette palette;
    /** The world positions (packed) of the seal's blocks, the two pedestals, and the exit rift. */
    public final Set<Long> sealBlocks = new HashSet<>();
    public BlockPos primary, secondary, arrival, exit, bossSpawn, keySpawn;
    public final List<BlockPos> chests = new ArrayList<>();
    /** 1.101.0: the release lever of the sealed treasury (on a wall of the key room), the chest of the Sigil, the second exit rift in the treasury, and the signs. */
    public BlockPos switchPos, sigilChest, shortcutExit;
    public final List<BlockPos> signs = new ArrayList<>();

    /** Job state (server thread only). */
    public PocketBuild build;
    /** 1.101.0: the watchdog's repair pass while one runs, the game time of the last probe, and how many repairs this tomb has needed. */
    public PocketBuild repairJob;
    public long lastProbe;
    public int repairs;
    public boolean leverTold;
    /** 1.101.0: delvers waiting at the tear for the tomb to finish forming (see {@code ExclusivePockets.join}). */
    public final java.util.Map<UUID, Waiter> waiting = new java.util.LinkedHashMap<>();
    public long waitingSince = -1;

    /** A delver held at the tear while the tomb forms, and the tear they came by (null when they were sent by a command). */
    public record Waiter(net.minecraft.server.level.ServerPlayer player, BlockPos rift) {}
    public PocketClear clear;
    public PocketCollapse collapse;
    /** Rooms that fell in the collapse (a bit each), traps that fired (player, tile index) -> game time, mages already written to, delvers rescued lately. */
    public long fallen;
    public final Map<String, Long> trapCooldown = new HashMap<>();
    public final Set<UUID> resonated = new HashSet<>();
    public final Map<UUID, Long> rescued = new HashMap<>();
    public final Map<UUID, Long> sealWarned = new HashMap<>();
    /** The boss: when it was last put in the hall, how many times it had to be, and when its absence was first noticed. */
    public long bossSpawnedAt = -1, bossMissingSince = -1;
    public int bossRespawns;
    public long lastQuiet;
    /** The part of a point of force the seal has been given and not yet counted (the hold of a button adds fractions), and the crack stage last shown on its blocks. */
    public double sealCarry;
    public int crackShown = -1;

    private PocketRuntime(PocketRealm.Pocket p, PocketDef def) {
        this.slot = p.slot;
        this.pocketId = p.pocketId;
        this.seed = p.seed;
        this.def = def;
        this.layout = LayoutGen.generate(p.seed, LayoutGen.Params.of(def));
        this.plan = PocketPlan.of(layout);
        this.palette = PocketPalette.of(def);
        for (PocketPlan.Feature f : plan.features()) {
            BlockPos w = PocketCell.world(slot, f.x(), f.y(), f.z());
            switch (f.type()) {
                case SEAL_BLOCK -> sealBlocks.add(w.asLong());
                case PEDESTAL_PRIMARY -> primary = w;
                case PEDESTAL_SECONDARY -> secondary = w;
                case ARRIVAL -> arrival = w;
                case EXIT_RIFT -> exit = w;
                case BOSS_SPAWN -> bossSpawn = w;
                case KEY_SPAWN -> keySpawn = w;
                case CHEST_MINOR, CHEST_TREASURE -> chests.add(w);
                case SWITCH -> switchPos = w;
                case SIGIL_CHEST -> sigilChest = w;
                case EXIT_SHORTCUT -> shortcutExit = w;
                case SIGN -> signs.add(w);
                default -> { }
            }
        }
    }

    /** The runtime of a pocket record, made on first use; null when the definition is gone (an orphan tomb: the caller collapses it). */
    public static PocketRuntime of(PocketRealm.Pocket p) {
        if (p == null || !p.exclusive()) return null;
        PocketRuntime r = LIVE.get(p.slot);
        if (r != null && r.pocketId.equals(p.pocketId) && r.seed == p.seed) return r;
        PocketDef d = PocketDefs.get(p.pocketId);
        if (d == null) return null;
        r = new PocketRuntime(p, d);
        LIVE.put(p.slot, r);
        return r;
    }

    /** The runtime of a slot if one exists already (never makes one). */
    public static PocketRuntime peek(int slot) { return LIVE.get(slot); }

    public static void forget(int slot) { LIVE.remove(slot); }

    public static void forgetAll() { LIVE.clear(); }

    public static int liveCount() { return LIVE.size(); }

    // ---------------------------------------------------------------- places
    /** The local x and z of a world point of this tomb. */
    public double lx(double x) { return PocketCell.localX(slot, x); }

    public double lz(double z) { return PocketCell.localZ(z); }

    /** The room whose footprint holds a world point, or -1 (a corridor or outside). */
    public int roomAt(double x, double z) { return layout.roomAt(lx(x), lz(z)); }

    /** The gravity zone (INVERTED, NEGATED or null) at a world point. */
    public PocketLayout.Kind zoneAt(double x, double y, double z) { return plan.gravityAt(lx(x), y, lz(z)); }

    /** The world box of a room's interior (floor to ceiling). */
    public AABB roomBox(int roomId) {
        PocketPlan.RoomBox b = plan.box(roomId);
        return new AABB(PocketCell.centreX(slot) + b.x0(), b.y0(), PocketCell.centreZ() + b.z0(), PocketCell.centreX(slot) + b.x1() + 1, b.y1() + 1, PocketCell.centreZ() + b.z1() + 1);
    }

    /** The world box of a room with its walls, floor and ceiling. */
    public AABB roomShell(int roomId) {
        PocketLayout.Room r = layout.room(roomId);
        int g = layout.grid(), cx = r.cx(g), cz = r.cz(g), w = r.half() + 1;
        return new AABB(PocketCell.centreX(slot) + cx - w, PocketPlan.Y0 - 3, PocketCell.centreZ() + cz - w, PocketCell.centreX(slot) + cx + w + 1, PocketPlan.Y0 + r.height() + 4, PocketCell.centreZ() + cz + w + 1);
    }

    /** Is a world block one of the seal's? */
    public boolean isSeal(BlockPos p) { return sealBlocks.contains(p.asLong()); }

    /** Did the build finish (every fill placed, every feature set)? */
    public boolean built() { return build == null || build.done(); }

    /** The chunks (as section positions' chunk coordinates) this tomb touches, as packed longs, for the tickets. */
    public Set<Long> chunkKeys() {
        Set<Long> out = new HashSet<>();
        int x0 = PocketCell.centreX(slot) + plan.minX(), x1 = PocketCell.centreX(slot) + plan.maxX(), z0 = PocketCell.centreZ() + plan.minZ(), z1 = PocketCell.centreZ() + plan.maxZ();
        for (int cz = SectionPos.blockToSectionCoord(z0); cz <= SectionPos.blockToSectionCoord(z1); cz++) for (int cx = SectionPos.blockToSectionCoord(x0); cx <= SectionPos.blockToSectionCoord(x1); cx++) out.add(net.minecraft.world.level.ChunkPos.asLong(cx, cz));
        return out;
    }

    /** A cooldown key for a trap tile and a player. */
    public static String trapKey(UUID player, int tile) { return player + "#" + tile; }

    /** Marks a room as woken in the saved record; true when it was not woken before. */
    public static boolean wake(PocketRealm.Pocket p, int roomId) {
        long bit = 1L << roomId;
        if ((p.activated & bit) != 0) return false;
        p.activated |= bit;
        return true;
    }

    public static boolean woken(PocketRealm.Pocket p, int roomId) { return (p.activated & (1L << roomId)) != 0; }

    /** Used by the self-test to read a record without a world: the record as NBT is the saved state. */
    public static CompoundTag describe(PocketRealm.Pocket p) {
        CompoundTag t = new CompoundTag();
        t.putString("Pocket", p.pocketId);
        t.putInt("Phase", p.phase);
        t.putBoolean("SealOpen", p.sealOpen);
        t.putBoolean("RelicTaken", p.relicTaken);
        t.putLong("Activated", p.activated);
        return t;
    }
}
