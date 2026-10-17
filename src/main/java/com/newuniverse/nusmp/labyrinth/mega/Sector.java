package com.newuniverse.nusmp.labyrinth.mega;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: a Sector is one Region of the Mega-Dungeon, the unit the whole design turns on: it has a theme (what it looks and feels like), a
 * threat level (how hard it bites), a region box (its exclusive territory: no other sector may build inside it), the rooms and doors that
 * were placed in it, the markers read from its templates, the handful of landmarks of its boss chain (the Guardian's spawn cell, the
 * Gatekeeper door, the vault, the Way Onward's rubble seal) and the state of its Crucible. It is plain data with hand-written NBT
 * (no codec): the saved-data class holds a list of them, every builder reads them through {@link MegaDungeon}, and the generator fills
 * them while it assembles. Field names are public and mutable because they are written by exactly one builder each (documented per field)
 * and read by many; the state machine is {@link State}.
 */
public final class Sector {
    /**
     * GENERATING: pieces are being placed (a server stop in this state re-generates from the saved seed: the plan is deterministic).
     * ACTIVE: playable; the Guardian lives. GUARDIAN_DEAD: its Mana Core exists, the Gatekeeper door is still sealed.
     * UNSEALED: the door was opened with the core; the next sector was queued. COMPLETE: the Crucible is resolved too. FAILED: generation gave up.
     */
    public enum State { GENERATING, ACTIVE, GUARDIAN_DEAD, UNSEALED, COMPLETE, FAILED }

    public final int id;
    /**
     * 1.55.0: which road of the bidirectional dungeon this sector belongs to. Sectors saved by 1.54.0 have no road and read as HOLY (their one chain is the
     * Holy one, so nothing changes for an old world). Set by the generator at creation.
     */
    public Polarity polarity = Polarity.HOLY;
    /** 1.55.0: the 0-based index of this sector within its polarity's chain; for a 1.54.0 save (one chain) it equals {@link #id}. Set by the generator at creation. */
    public int chainIndex;
    /** 1.55.0: how many floors this sector spans ({@link FloorMap#DEFAULT_PER_REGION}); saved so a config change never renumbers a sector that already exists. */
    public int perRegion = FloorMap.DEFAULT_PER_REGION;
    /** Set by the generator at creation. */
    public RegionTheme theme = RegionTheme.GRAVITY;
    /** 1 for sector 0, then 1 + id; the encounter director, factions and the guardian scale with it. */
    public int threat = 1;
    public State state = State.GENERATING;
    /** The seed the whole plan derives from (re-running the generator with it re-creates the same pieces). */
    public long seed;
    /** The exclusive territory (generator). */
    public BoundingBox region = new BoundingBox(0, 0, 0, 0, 0, 0);
    /** The union of the placed pieces (generator, after assembling). */
    public BoundingBox bounds = new BoundingBox(0, 0, 0, 0, 0, 0);
    /** The cell of the Landing's entry socket, and the direction the sector grows from it (generator). Sector 0: the origin of its Landing. */
    public BlockPos entry = BlockPos.ZERO;
    public Direction forward = Direction.NORTH;
    /** The Landing's ARRIVAL marker cell (generator, from the marker). */
    public BlockPos arrival;
    public final List<Room> rooms = new ArrayList<>();
    /** Room-id pairs that are joined by a doorway (a jigsaw join, or two aligned open sockets: a loop). */
    public final List<int[]> edges = new ArrayList<>();
    /** Every marker read from the pieces, in world positions (generator). */
    public final List<Marker> markers = new ArrayList<>();

    // ---- the boss chain (generator fills them from the markers; the guard builder uses them)
    public BlockPos guardianSpawn, gatekeeper, vaultCenter;
    /** The rubble seal of the Way Onward: its bottom-centre cell of the OUTER layer (the end socket's jigsaw cell, marker.below()), the direction the next sector attaches, and the seal's size (width, height, depth: it runs depth layers inward). */
    public BlockPos expansionPos;
    public Direction expansionFacing;
    public int sealW = 3, sealH = 4, sealD = 2;
    /** How many relics the Crucible offers (1..3; always 3 since the owner fixed it at three), decided by the planner from the theme (generator). */
    public int relicCount = 1;
    /** The pedestal blocks (generator, from PEDESTAL markers: marker.below()). */
    public final List<BlockPos> pedestals = new ArrayList<>();
    /** The id of the sector that was generated off this one's Way Onward, or -1. */
    public int nextId = -1;
    /**
     * 1.55.0: the Gatekeeper was opened but the next sector has not been requested yet. It waits until the Crucible is resolved or a player reaches the Way Onward
     * ({@code mega.expandAfterVault}), so that the relic taken can decide the next theme. Saved, so a restart keeps waiting.
     */
    public boolean expansionPending;

    // ---- the stairs of the floor model (1.58.0, generator fills them from the STAIR_EXIT and ARRIVAL markers of the two stairwells)
    /**
     * The cell above the landing pad at the end of the ARRIVAL stairwell's staircase (a {@code stair_exit} marker): walking onto it takes the player BACK, to the
     * forward stair of the floor before, or to Floor 0 on the first floor. Null for a sector of the old banded model, which has no stairwells.
     */
    public BlockPos stairBack;
    /** The same for the FORWARD stairwell: walking onto it takes the player on, to the arrival stair of the next floor. Null on a sector without one. */
    public BlockPos stairForward;
    /** Where a player stands when they come back to this floor from the next one: the {@code arrival} marker of the forward stairwell. Null on a sector without one. */
    public BlockPos stairForwardPad;

    public final Crucible crucible = new Crucible();

    public Sector(int id) {
        this.id = id;
        this.chainIndex = id;
    }

    /** The signed number of this sector's first floor (the Landing's floor). */
    public int firstFloor() { return FloorMap.firstFloor(polarity, chainIndex, perRegion); }

    /** The signed number of this sector's last floor (the Guardian's arena, the decennial floor). */
    public int lastFloor() { return FloorMap.lastFloor(polarity, chainIndex, perRegion); }

    /** Is this sector one floor (the floor model), with stairs, rather than a band of {@link #perRegion} floors? */
    public boolean isFloor() { return FloorMap.isFloorModel(perRegion); }

    /** Does this sector hold a boss: a Guardian, a Gatekeeper, a Crucible? Every sector of the banded model does; of the floor model, every tenth floor. */
    public boolean isBossFloor() { return FloorMap.isDecennial(lastFloor(), perRegion); }

    /** The Crucible of Choice's saved state (the crucible builder owns the meaning; the data lives here so there is one saved file). */
    public static final class Crucible {
        /** UNOPENED: nobody entered the vault; ROLLED: the offered relics were drawn; CHOSEN: one was taken and the catastrophe fired; RESOLVED: the vault is done. */
        public enum Phase { UNOPENED, ROLLED, CHOSEN, RESOLVED }

        public Phase phase = Phase.UNOPENED;
        /** The relic ids shown on the pedestals, in slot order (an entry is "" once its pedestal crumbled). */
        public final List<String> offered = new ArrayList<>();
        public String chosen = "";
        public UUID chooser;
        public String catastrophe = "";
        public long since;

        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putString("phase", phase.name());
            ListTag l = new ListTag();
            for (String s : offered) l.add(net.minecraft.nbt.StringTag.valueOf(s));
            t.put("offered", l);
            t.putString("chosen", chosen);
            if (chooser != null) t.putUUID("chooser", chooser);
            t.putString("cat", catastrophe);
            t.putLong("since", since);
            return t;
        }

        public void load(CompoundTag t) {
            try { phase = Phase.valueOf(t.getString("phase")); } catch (IllegalArgumentException e) { phase = Phase.UNOPENED; }
            offered.clear();
            ListTag l = t.getList("offered", Tag.TAG_STRING);
            for (int i = 0; i < l.size(); i++) offered.add(l.getString(i));
            chosen = t.getString("chosen");
            chooser = t.hasUUID("chooser") ? t.getUUID("chooser") : null;
            catastrophe = t.getString("cat");
            since = t.getLong("since");
        }
    }

    // ---------------------------------------------------------------- queries
    /** Is the position inside this sector's exclusive region (all three axes)? */
    public boolean containsRegion(BlockPos p) { return region.isInside(p); }

    /** The room whose box holds the position, preferring the smallest box (a corridor inside a hall's footprint never happens, but a cap-free tie must be stable). Null when none. */
    public Room roomAt(BlockPos p) {
        Room best = null;
        long bestVolume = Long.MAX_VALUE;
        for (Room r : rooms) {
            if (!r.contains(p)) continue;
            long v = (long) r.box.getXSpan() * r.box.getYSpan() * r.box.getZSpan();
            if (v < bestVolume) { best = r; bestVolume = v; }
        }
        return best;
    }

    public Room room(int roomId) {
        for (Room r : rooms) if (r.id == roomId) return r;
        return null;
    }

    /** All markers of one kind. */
    public List<Marker> markers(MarkerKind kind) {
        List<Marker> out = new ArrayList<>();
        for (Marker m : markers) if (m.kind() == kind) out.add(m);
        return out;
    }

    /** The ids of the rooms joined to {@code roomId} by a doorway. */
    public List<Integer> neighbours(int roomId) {
        List<Integer> out = new ArrayList<>();
        for (int[] e : edges) {
            if (e[0] == roomId) out.add(e[1]);
            else if (e[1] == roomId) out.add(e[0]);
        }
        return out;
    }

    // ---------------------------------------------------------------- NBT
    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("id", id);
        t.putString("theme", theme.id());
        t.putInt("threat", threat);
        t.putString("pol", polarity.id());
        t.putInt("chain", chainIndex);
        t.putInt("fpr", perRegion);
        t.putString("state", state.name());
        t.putLong("seed", seed);
        t.putIntArray("region", box(region));
        t.putIntArray("bounds", box(bounds));
        t.putLong("entry", entry.asLong());
        t.putString("fwd", forward.getName());
        if (arrival != null) t.putLong("arrival", arrival.asLong());
        ListTag rl = new ListTag();
        for (Room r : rooms) rl.add(r.save());
        t.put("rooms", rl);
        int[] e = new int[edges.size() * 2];
        for (int i = 0; i < edges.size(); i++) { e[i * 2] = edges.get(i)[0]; e[i * 2 + 1] = edges.get(i)[1]; }
        t.putIntArray("edges", e);
        ListTag ml = new ListTag();
        for (Marker m : markers) ml.add(m.save());
        t.put("markers", ml);
        if (guardianSpawn != null) t.putLong("guardian", guardianSpawn.asLong());
        if (gatekeeper != null) t.putLong("gate", gatekeeper.asLong());
        if (vaultCenter != null) t.putLong("vault", vaultCenter.asLong());
        if (expansionPos != null) t.putLong("exp", expansionPos.asLong());
        if (expansionFacing != null) t.putString("expf", expansionFacing.getName());
        t.putIntArray("seal", new int[] {sealW, sealH, sealD});
        t.putInt("relics", relicCount);
        long[] pl = new long[pedestals.size()];
        for (int i = 0; i < pl.length; i++) pl[i] = pedestals.get(i).asLong();
        t.putLongArray("peds", pl);
        t.putInt("next", nextId);
        if (expansionPending) t.putBoolean("xpend", true);
        if (stairBack != null) t.putLong("sbk", stairBack.asLong());
        if (stairForward != null) t.putLong("sfw", stairForward.asLong());
        if (stairForwardPad != null) t.putLong("sfp", stairForwardPad.asLong());
        t.put("crucible", crucible.save());
        return t;
    }

    public static Sector load(CompoundTag t) {
        Sector s = new Sector(t.getInt("id"));
        s.theme = RegionTheme.fromId(t.getString("theme"));
        s.threat = Math.max(1, t.getInt("threat"));
        Polarity pol = Polarity.fromId(t.getString("pol"));
        s.polarity = pol == null ? Polarity.HOLY : pol;
        s.chainIndex = t.contains("chain") ? Math.max(0, t.getInt("chain")) : s.id;
        s.perRegion = t.contains("fpr") ? Math.max(1, t.getInt("fpr")) : FloorMap.DEFAULT_PER_REGION;
        try { s.state = State.valueOf(t.getString("state")); } catch (IllegalArgumentException ex) { s.state = State.ACTIVE; }
        s.seed = t.getLong("seed");
        s.region = box(t.getIntArray("region"));
        s.bounds = box(t.getIntArray("bounds"));
        s.entry = BlockPos.of(t.getLong("entry"));
        Direction f = Direction.byName(t.getString("fwd"));
        s.forward = f != null && f.getAxis().isHorizontal() ? f : Direction.NORTH;
        if (t.contains("arrival")) s.arrival = BlockPos.of(t.getLong("arrival"));
        ListTag rl = t.getList("rooms", Tag.TAG_COMPOUND);
        for (int i = 0; i < rl.size(); i++) s.rooms.add(Room.load(rl.getCompound(i)));
        int[] e = t.getIntArray("edges");
        for (int i = 0; i + 1 < e.length; i += 2) s.edges.add(new int[] {e[i], e[i + 1]});
        ListTag ml = t.getList("markers", Tag.TAG_COMPOUND);
        for (int i = 0; i < ml.size(); i++) {
            Marker m = Marker.load(ml.getCompound(i));
            if (m != null) s.markers.add(m);
        }
        if (t.contains("guardian")) s.guardianSpawn = BlockPos.of(t.getLong("guardian"));
        if (t.contains("gate")) s.gatekeeper = BlockPos.of(t.getLong("gate"));
        if (t.contains("vault")) s.vaultCenter = BlockPos.of(t.getLong("vault"));
        if (t.contains("exp")) s.expansionPos = BlockPos.of(t.getLong("exp"));
        if (t.contains("expf")) {
            Direction d = Direction.byName(t.getString("expf"));
            s.expansionFacing = d != null && d.getAxis().isHorizontal() ? d : null;
        }
        int[] seal = t.getIntArray("seal");
        if (seal.length == 3) { s.sealW = seal[0]; s.sealH = seal[1]; s.sealD = seal[2]; }
        s.relicCount = Math.max(1, Math.min(3, t.getInt("relics")));
        for (long p : t.getLongArray("peds")) s.pedestals.add(BlockPos.of(p));
        s.nextId = t.contains("next") ? t.getInt("next") : -1;
        s.expansionPending = t.getBoolean("xpend");
        if (t.contains("sbk")) s.stairBack = BlockPos.of(t.getLong("sbk"));
        if (t.contains("sfw")) s.stairForward = BlockPos.of(t.getLong("sfw"));
        if (t.contains("sfp")) s.stairForwardPad = BlockPos.of(t.getLong("sfp"));
        if (t.contains("crucible")) s.crucible.load(t.getCompound("crucible"));
        return s;
    }

    private static int[] box(BoundingBox b) { return new int[] {b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()}; }

    private static BoundingBox box(int[] a) { return a.length == 6 ? new BoundingBox(a[0], a[1], a[2], a[3], a[4], a[5]) : new BoundingBox(0, 0, 0, 0, 0, 0); }
}
