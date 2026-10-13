package com.newuniverse.nusmp.relics.order;

import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.order.LedgerRules.Known;
import com.newuniverse.nusmp.relics.order.LedgerRules.Mode;
import com.newuniverse.nusmp.relics.order.LedgerRules.Verdict;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Order's ledger: for every unique relic, which copy is the one valid copy of the world, where it was last seen and whether it is alive. Saved
 * with the world in the Overworld's data storage as {@code nusmp_relic_ledger}.
 * <ul>
 *   <li>A stack of a unique relic carries a serial (the data component {@code relic_serial}). The ledger's serial for the relic id is the canonical
 *       one; the decision table is {@link LedgerRules}.</li>
 *   <li>{@link #issue} is what loot and commands call when they make a relic: it stamps a fresh serial and records it, or refuses when a copy lives.</li>
 *   <li>A canonical copy whose item entity was destroyed is marked LOST (see {@link OrderEvents}) so the relic can be issued or found again.</li>
 *   <li>The census: the first time the ledger is made in a world that already has some age, the Order only counts for ten minutes after the first
 *       player is online; duplicates met then are reported, not taken.</li>
 * </ul>
 * All of it runs on the server thread.
 */
public final class UniqueLedger extends SavedData {
    public static final String NAME = "nusmp_relic_ledger";

    /** The server the static helpers answer for (set when it starts, cleared when it stops). */
    private static MinecraftServer current;

    /** What the ledger knows about one relic id. */
    public static final class Entry {
        final String relicId;
        UUID serial;
        boolean live = true;
        Spot.Kind kind = Spot.Kind.UNKNOWN;
        UUID holder;
        String holderName = "";
        String dim = "";
        long pos;
        long seenTick, seenMillis, issuedTick, writtenTick;
        boolean everSeen;

        Entry(String relicId) { this.relicId = relicId; }

        public String relicId() { return relicId; }

        public boolean live() { return live; }

        public UUID serial() { return serial; }

        Spot spot() { return new Spot(kind, holder, holderName, dim, pos); }
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final Map<String, Long> visits = new HashMap<>();
    private long censusEnd = -1L;
    private boolean censusPending, censusReported = true, fresh = true;
    private int forced = -1;
    private int adoptedInCensus;

    // ================================================================ access
    public static void bind(MinecraftServer server) { current = server; }

    public static void unbind() { current = null; }

    public static MinecraftServer server() { return current; }

    public static UniqueLedger get(MinecraftServer server) {
        UniqueLedger l = server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(UniqueLedger::new, UniqueLedger::load, null), NAME);
        if (l.fresh) {
            l.fresh = false;
            l.censusPending = LedgerRules.needsCensus(server.overworld().getGameTime());
            l.setDirty();
        }
        return l;
    }

    // ================================================================ the contract with the relic core
    /**
     * A relic is being made (a loot roll, a command, a shop): stamps a new serial onto 'stack' and records it. False if a copy of this relic lives
     * already (nothing is stamped then), or the stack could not be stamped. A consumable is not tracked: true, no serial.
     */
    public static boolean issue(ServerLevel any, String relicId, ItemStack stack) {
        if (any == null || relicId == null || stack == null || stack.isEmpty()) return false;
        MinecraftServer server = any.getServer();
        if (server == null) return false;
        RelicDef def = Relics.byId(relicId);
        if (def != null && !OrderRelics.unique(def)) return true;
        UniqueLedger l = get(server);
        Entry e = l.entries.get(relicId);
        if (e != null && e.live) return false;
        UUID serial = UUID.randomUUID();
        RelicSerial.set(stack, serial);
        if (!serial.equals(RelicSerial.get(stack))) return false;
        l.adopt(relicId, serial, Spot.NOWHERE, server.overworld().getGameTime(), false);
        return true;
    }

    /**
     * Is this stack the valid copy (or the first copy, which is about to become it)? True for everything that is not a unique relic. False for a
     * duplicate. Answers true when there is no server.
     */
    public static boolean isCanonical(ItemStack stack) {
        MinecraftServer server = current;
        String id = OrderRelics.uniqueId(stack);
        if (id == null || server == null) return true;
        Verdict v = LedgerRules.judge(true, get(server).known(id), RelicSerial.get(stack), false);
        return v != Verdict.DUPLICATE;
    }

    /** Looks at every online player's belongings at the next tick instead of waiting for their turn (after a command or a loot roll). */
    public static void scan(MinecraftServer server) { RelicScanner.requestAll(server); }

    /** The ids of the relics that have a live valid copy. */
    public static Set<String> liveIds(MinecraftServer server) {
        Set<String> out = new LinkedHashSet<>();
        for (Entry e : get(server).entries.values()) if (e.live) out.add(e.relicId);
        return out;
    }

    // ================================================================ reading
    public Known known(String relicId) {
        Entry e = entries.get(relicId);
        return e == null ? Known.NONE : new Known(true, e.live, e.serial);
    }

    public Entry entry(String relicId) { return entries.get(relicId); }

    public Map<String, Entry> entries() { return Collections.unmodifiableMap(entries); }

    public int liveCount() {
        int n = 0;
        for (Entry e : entries.values()) if (e.live) n++;
        return n;
    }

    public int lostCount() { return entries.size() - liveCount(); }

    public long lastVisit(String relicId) {
        Long v = visits.get(relicId);
        return v == null ? -1L : v;
    }

    // ================================================================ writing
    /** The valid copy of 'relicId' is this serial, seen at 'spot' now. */
    void adopt(String relicId, UUID serial, Spot spot, long tick, boolean seen) {
        Entry e = entries.computeIfAbsent(relicId, Entry::new);
        boolean wasLive = e.live && e.serial != null;
        e.serial = serial;
        e.live = true;
        if (seen) {
            place(e, spot, tick);
            e.everSeen = true;
        } else {
            e.issuedTick = tick;
            e.everSeen = false;
            place(e, spot, tick);
        }
        if (!wasLive && seen && mode(tick) == Mode.CENSUS) adoptedInCensus++;
        setDirty();
    }

    /** The valid copy was seen at 'spot'. Rewrites the record only when it moved or after a while. */
    void touch(String relicId, Spot spot, long tick) {
        Entry e = entries.get(relicId);
        if (e == null) return;
        boolean moved = !e.spot().sameAs(spot) || !e.everSeen;
        e.everSeen = true;
        if (LedgerRules.rewriteSeen(moved, e.writtenTick, tick)) {
            place(e, spot, tick);
            setDirty();
        }
    }

    private static void place(Entry e, Spot spot, long tick) {
        e.kind = spot.kind();
        e.holder = spot.holder();
        e.holderName = spot.holderName() == null ? "" : spot.holderName();
        e.dim = spot.dim() == null ? "" : spot.dim();
        e.pos = spot.pos();
        e.seenTick = tick;
        e.seenMillis = System.currentTimeMillis();
        e.writtenTick = tick;
    }

    /** The valid copy no longer exists (or nobody can tell): the relic can be issued or found again. */
    boolean lose(String relicId) {
        Entry e = entries.get(relicId);
        if (e == null || !e.live) return false;
        e.live = false;
        setDirty();
        return true;
    }

    void recordVisit(String relicId, long tick) {
        visits.put(relicId, tick);
        setDirty();
    }

    // ================================================================ the census
    public Mode mode(long now) {
        if (censusPending && forced != 1) return Mode.CENSUS;                  // an existing world waits for its first player: nothing is taken yet
        return LedgerRules.modeAt(forced == 0 ? Mode.CENSUS : forced == 1 ? Mode.ENFORCE : null, censusEnd, now);
    }

    public long censusEnd() { return censusEnd; }

    public boolean censusPending() { return censusPending; }

    public boolean censusHeld() { return forced == 0; }

    public int adoptedInCensus() { return adoptedInCensus; }

    /** Starts the ten minutes if the ledger was waiting for its first player. True the moment it starts. */
    boolean startCensusIfPending(long now) {
        if (!censusPending) return false;
        beginCensus(now);
        return true;
    }

    /** An admin starts a census now (or keeps one going forever with 'hold'). */
    void beginCensus(long now) {
        censusPending = false;
        censusEnd = now + LedgerRules.CENSUS_TICKS;
        censusReported = false;
        adoptedInCensus = 0;
        forced = -1;
        setDirty();
    }

    void holdCensus() {
        forced = 0;
        censusReported = true;
        setDirty();
    }

    /** Ends any census now: the Order enforces from this tick. */
    void enforceNow(long now) {
        censusPending = false;
        forced = -1;
        if (censusEnd >= now || censusEnd < 0) censusEnd = now;
        setDirty();
    }

    /** True once, the tick the census is over (so the summary is written once). */
    boolean censusJustEnded(long now) {
        if (censusReported || censusEnd < 0 || now < censusEnd || forced == 0) return false;
        censusReported = true;
        setDirty();
        return true;
    }

    // ================================================================ housekeeping
    /**
     * Once a minute: gives up on a copy that was issued and never seen for an hour, and on a copy of an online player that has not been seen for
     * fourteen days of real time. Returns the ids declared lost.
     */
    List<String> sweep(MinecraftServer server, long tick) {
        List<String> lost = new ArrayList<>();
        long nowMillis = System.currentTimeMillis();
        for (Entry e : entries.values()) {
            if (!e.live) continue;
            boolean online = e.kind == Spot.Kind.PLAYER && e.holder != null && server.getPlayerList().getPlayer(e.holder) != null;
            if (LedgerRules.unseenExpired(true, e.everSeen, tick - e.issuedTick)
                    || LedgerRules.staleLost(true, e.kind == Spot.Kind.PLAYER, online, nowMillis - e.seenMillis)) {
                e.live = false;
                lost.add(e.relicId);
            }
        }
        if (!lost.isEmpty()) setDirty();
        return lost;
    }

    // ================================================================ saving
    static UniqueLedger load(CompoundTag tag, HolderLookup.Provider provider) {
        UniqueLedger l = new UniqueLedger();
        l.fresh = false;
        l.censusEnd = tag.contains("CensusEnd") ? tag.getLong("CensusEnd") : -1L;
        l.censusPending = tag.getBoolean("CensusPending");
        l.censusReported = !tag.contains("CensusReported") || tag.getBoolean("CensusReported");
        l.forced = tag.contains("Forced") ? tag.getInt("Forced") : -1;
        ListTag list = tag.getList("Relics", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            String id = t.getString("Id");
            if (id.isEmpty()) continue;
            Entry e = new Entry(id);
            if (t.hasUUID("Serial")) e.serial = t.getUUID("Serial");
            e.live = t.getBoolean("Live");
            int k = t.getInt("Kind");
            e.kind = k >= 0 && k < Spot.Kind.values().length ? Spot.Kind.values()[k] : Spot.Kind.UNKNOWN;
            if (t.hasUUID("Holder")) e.holder = t.getUUID("Holder");
            e.holderName = t.getString("HolderName");
            e.dim = t.getString("Dim");
            e.pos = t.getLong("Pos");
            e.seenTick = t.getLong("SeenTick");
            e.seenMillis = t.getLong("SeenMillis");
            e.issuedTick = t.getLong("IssuedTick");
            e.writtenTick = e.seenTick;
            e.everSeen = t.getBoolean("Seen");
            l.entries.put(id, e);
        }
        ListTag visits = tag.getList("Visits", Tag.TAG_COMPOUND);
        for (int i = 0; i < visits.size(); i++) {
            CompoundTag t = visits.getCompound(i);
            l.visits.put(t.getString("Id"), t.getLong("At"));
        }
        return l;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLong("CensusEnd", censusEnd);
        tag.putBoolean("CensusPending", censusPending);
        tag.putBoolean("CensusReported", censusReported);
        tag.putInt("Forced", forced);
        ListTag list = new ListTag();
        for (Entry e : entries.values()) {
            CompoundTag t = new CompoundTag();
            t.putString("Id", e.relicId);
            if (e.serial != null) t.putUUID("Serial", e.serial);
            t.putBoolean("Live", e.live);
            t.putInt("Kind", e.kind.ordinal());
            if (e.holder != null) t.putUUID("Holder", e.holder);
            t.putString("HolderName", e.holderName);
            t.putString("Dim", e.dim);
            t.putLong("Pos", e.pos);
            t.putLong("SeenTick", e.seenTick);
            t.putLong("SeenMillis", e.seenMillis);
            t.putLong("IssuedTick", e.issuedTick);
            t.putBoolean("Seen", e.everSeen);
            list.add(t);
        }
        tag.put("Relics", list);
        ListTag v = new ListTag();
        for (Map.Entry<String, Long> me : visits.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("Id", me.getKey());
            t.putLong("At", me.getValue());
            v.add(t);
        }
        tag.put("Visits", v);
        return tag;
    }
}
