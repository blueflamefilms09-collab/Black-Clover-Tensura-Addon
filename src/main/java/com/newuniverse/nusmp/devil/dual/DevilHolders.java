package com.newuniverse.nusmp.devil.dual;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: One of One needs the world to remember who holds each devil. This is that memory, world saved data ({@code nusmp_devil_holders}), keyed by devil id with the holder's UUID in it, so it works for a
 * holder who is offline and survives a restart. It is only ever read or written when somebody calls a devil, signs, dies (a holder), logs in or out (a holder), or opens a duel: there is no tick work and no scan.
 * At most one row per devil of the roster (the table is cut at {@link #MAX_ROWS} when read, so a damaged file cannot grow it), and a player holds at most one row (a player has one contract). The open duels are
 * live only (never saved, at most {@link #MAX_DUELS}). Pure data and bookkeeping: the decisions are {@link HolderRules}.
 */
public final class DevilHolders extends SavedData {
    public static final int MAX_ROWS = 256, MAX_DUELS = 8;

    /** One row of the table. {@code holder} is null while the devil is only reserved for the victor of a duel. */
    private static final class Row {
        UUID holder, reservedFor;
        long since, diedAt, seenMs, reservedUntil;

        HolderRules.Slot slot() { return new HolderRules.Slot(holder, diedAt, seenMs, reservedFor, reservedUntil); }
    }

    /** An open bound duel: the challenger may take the devil from the holder by killing them before {@code until}. */
    public record Duel(UUID challenger, UUID holder, String devil, long until) {}

    private final Map<String, Row> rows = new LinkedHashMap<>();
    private final List<Duel> duels = new ArrayList<>();

    public static DevilHolders get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(DevilHolders::new, DevilHolders::load, null), "nusmp_devil_holders");
    }

    static DevilHolders load(CompoundTag t, HolderLookup.Provider p) {
        DevilHolders h = new DevilHolders();
        ListTag list = t.getList("rows", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && h.rows.size() < MAX_ROWS; i++) {
            CompoundTag c = list.getCompound(i);
            String devil = c.getString("devil");
            if (devil.isEmpty() || devil.length() > 64) continue;
            Row r = new Row();
            if (c.hasUUID("holder")) r.holder = c.getUUID("holder");
            if (c.hasUUID("reservedFor")) r.reservedFor = c.getUUID("reservedFor");
            r.since = Math.max(0L, c.getLong("since"));
            r.diedAt = Math.max(0L, c.getLong("diedAt"));
            r.seenMs = Math.max(0L, c.getLong("seenMs"));
            r.reservedUntil = Math.max(0L, c.getLong("reservedUntil"));
            if (r.holder != null || r.reservedFor != null) h.rows.put(devil, r);
        }
        return h;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider p) {
        ListTag list = new ListTag();
        for (Map.Entry<String, Row> e : rows.entrySet()) {
            Row r = e.getValue();
            CompoundTag c = new CompoundTag();
            c.putString("devil", e.getKey());
            if (r.holder != null) c.putUUID("holder", r.holder);
            if (r.reservedFor != null) c.putUUID("reservedFor", r.reservedFor);
            c.putLong("since", r.since);
            c.putLong("diedAt", r.diedAt);
            c.putLong("seenMs", r.seenMs);
            c.putLong("reservedUntil", r.reservedUntil);
            list.add(c);
        }
        t.put("rows", list);
        return t;
    }

    // ---------------------------------------------------------------- reading
    /** The slot of a devil, or null when nobody holds or reserves it. */
    public HolderRules.Slot slot(String devil) {
        Row r = devil == null ? null : rows.get(devil);
        return r == null ? null : r.slot();
    }

    /** The devil this player holds, or "". */
    public String devilOf(UUID id) {
        if (id != null) for (Map.Entry<String, Row> e : rows.entrySet()) if (id.equals(e.getValue().holder)) return e.getKey();
        return "";
    }

    public UUID holderOf(String devil) {
        Row r = devil == null ? null : rows.get(devil);
        return r == null ? null : r.holder;
    }

    public int rowCount() { return rows.size(); }

    // ---------------------------------------------------------------- writing
    /** {@code id} now holds {@code devil}: a clean row (no death, no absence, no reservation). Any other devil this player held is freed (a player has one contract). */
    public void claim(String devil, UUID id, long now) { claim(devil, id, now, true); }

    /** Phase 2 (a player may hold several Supreme devils): the same claim, but the player's other devils stay theirs. */
    public void claimKeep(String devil, UUID id, long now) { claim(devil, id, now, false); }

    private void claim(String devil, UUID id, long now, boolean freeOthers) {
        if (devil == null || devil.isEmpty() || id == null) return;
        if (freeOthers) for (Iterator<Map.Entry<String, Row>> it = rows.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Row> e = it.next();
            if (!e.getKey().equals(devil) && id.equals(e.getValue().holder)) it.remove();
        }
        if (rows.size() >= MAX_ROWS && !rows.containsKey(devil)) return;
        Row r = new Row();
        r.holder = id;
        r.since = now;
        rows.put(devil, r);
        setDirty();
    }

    /** The devil has no holder and no reservation. */
    public void free(String devil) {
        if (devil != null && rows.remove(devil) != null) setDirty();
    }

    /** The devil is reserved for {@code id} until the game time {@code until}; it has no holder meanwhile. */
    public void reserve(String devil, UUID id, long until) {
        if (devil == null || devil.isEmpty() || id == null) return;
        if (rows.size() >= MAX_ROWS && !rows.containsKey(devil)) return;
        Row r = new Row();
        r.reservedFor = id;
        r.reservedUntil = until;
        rows.put(devil, r);
        setDirty();
    }

    /** The holder died with the contract: the grace clock starts (once; a second death does not restart it). */
    public void died(UUID id, long now) {
        for (Row r : rowsOf(id)) if (r.diedAt == 0) { r.diedAt = Math.max(1L, now); setDirty(); }
    }

    /** The holder let the devil in again: the death lapse is off. */
    public void renewed(UUID id) {
        for (Row r : rowsOf(id)) if (r.diedAt != 0) { r.diedAt = 0; setDirty(); }
    }

    /** The holder logged out at real time {@code ms}. */
    public void seen(UUID id, long ms) {
        for (Row r : rowsOf(id)) { r.seenMs = Math.max(1L, ms); setDirty(); }
    }

    /** The holder is online: the absence lapse is off. */
    public void online(UUID id) {
        for (Row r : rowsOf(id)) if (r.seenMs != 0) { r.seenMs = 0; setDirty(); }
    }

    /** Every row this player holds (one, except for a player with several Supreme devils; at most 8). */
    private java.util.List<Row> rowsOf(UUID id) {
        java.util.List<Row> out = new java.util.ArrayList<>(1);
        if (id != null) for (Row r : rows.values()) if (id.equals(r.holder)) out.add(r);
        return out;
    }

    private Row rowOf(UUID id) {
        if (id != null) for (Row r : rows.values()) if (id.equals(r.holder)) return r;
        return null;
    }

    // ---------------------------------------------------------------- duels (live only)
    public int duelCount() { return duels.size(); }

    public Duel duelOfHolder(UUID holder) {
        for (Duel d : duels) if (d.holder().equals(holder)) return d;
        return null;
    }

    public Duel duelOfChallenger(UUID challenger) {
        for (Duel d : duels) if (d.challenger().equals(challenger)) return d;
        return null;
    }

    /** Opens a duel unless the table is full or either man is already in one. */
    public boolean openDuel(Duel d) {
        if (duels.size() >= MAX_DUELS || duelOfHolder(d.holder()) != null || duelOfChallenger(d.challenger()) != null) return false;
        duels.add(d);
        return true;
    }

    /** Drops every duel this player is in, and every duel that ran out at {@code now}. */
    public void dropDuels(UUID id, long now) {
        duels.removeIf(d -> d.until() < now || d.challenger().equals(id) || d.holder().equals(id));
    }
}
