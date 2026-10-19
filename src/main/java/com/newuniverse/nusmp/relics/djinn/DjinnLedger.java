package com.newuniverse.nusmp.relics.djinn;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The world's book of Djinn (SavedData "nusmp_djinn_ledger", in the overworld's data storage like the spirit slots): for each of the thirty-six (a world saved with twelve reads the other twenty-four as asleep), is it
 * still asleep in the Dungeon (SLEEPING), does a Seal exist in the world (SEALED: where it was last put, when it was last seen), or does a
 * player hold it (BOUND: who, which vessel serial, which two allies share it). It enforces the owner's rule that one of each exists and one
 * player possesses a given Djinn, and keeps which floor bosses have already paid their Seal. A Seal that nobody has seen for
 * {@link HouseholdRules#SEAL_LOST_AFTER} ticks of running time goes back to sleep, so a Djinn can never be lost for good.
 */
public final class DjinnLedger extends SavedData {
    public enum State { SLEEPING, SEALED, BOUND }

    private static final class Entry {
        State state = State.SLEEPING;
        UUID owner, serial;
        String ownerName = "";
        final List<UUID> household = new ArrayList<>();
        String sealDim = "";
        int sx, sy, sz;
        long lastSeen;
    }

    private final Map<Djinn, Entry> entries = new EnumMap<>(Djinn.class);
    private final Set<String> rewarded = new HashSet<>();

    private DjinnLedger() {
        for (Djinn d : Djinn.values()) entries.put(d, new Entry());
    }

    public static DjinnLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(DjinnLedger::new, DjinnLedger::load, null), "nusmp_djinn_ledger");
    }

    private static DjinnLedger load(CompoundTag tag, HolderLookup.Provider provider) {
        DjinnLedger l = new DjinnLedger();
        for (Djinn d : Djinn.values()) {
            if (!tag.contains(d.id(), 10)) continue;
            CompoundTag c = tag.getCompound(d.id());
            Entry e = l.entries.get(d);
            try { e.state = State.valueOf(c.getString("state")); } catch (IllegalArgumentException ignored) { e.state = State.SLEEPING; }
            if (c.hasUUID("owner")) e.owner = c.getUUID("owner");
            if (c.hasUUID("serial")) e.serial = c.getUUID("serial");
            e.ownerName = c.getString("ownerName");
            ListTag h = c.getList("household", 11);
            for (int i = 0; i < h.size() && e.household.size() < DjinnPhaseRules.HOUSEHOLD_MAX; i++) e.household.add(net.minecraft.nbt.NbtUtils.loadUUID(h.get(i)));
            e.sealDim = c.getString("sealDim");
            e.sx = c.getInt("sx");
            e.sy = c.getInt("sy");
            e.sz = c.getInt("sz");
            e.lastSeen = c.getLong("lastSeen");
            if (e.state == State.BOUND && e.owner == null) e.state = State.SEALED;          // a broken record: the Djinn is not lost, its Seal is
        }
        ListTag r = tag.getList("rewarded", 8);
        for (int i = 0; i < r.size(); i++) l.rewarded.add(r.getString(i));
        return l;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        for (Djinn d : Djinn.values()) {
            Entry e = entries.get(d);
            CompoundTag c = new CompoundTag();
            c.putString("state", e.state.name());
            if (e.owner != null) c.putUUID("owner", e.owner);
            if (e.serial != null) c.putUUID("serial", e.serial);
            c.putString("ownerName", e.ownerName);
            ListTag h = new ListTag();
            for (UUID u : e.household) h.add(net.minecraft.nbt.NbtUtils.createUUID(u));
            c.put("household", h);
            c.putString("sealDim", e.sealDim);
            c.putInt("sx", e.sx);
            c.putInt("sy", e.sy);
            c.putInt("sz", e.sz);
            c.putLong("lastSeen", e.lastSeen);
            tag.put(d.id(), c);
        }
        ListTag r = new ListTag();
        for (String z : rewarded) r.add(StringTag.valueOf(z));
        tag.put("rewarded", r);
        return tag;
    }

    // ---------------------------------------------------------------- reading
    public State state(Djinn d) { return entries.get(d).state; }

    /** Who holds this Djinn (null unless BOUND). */
    public UUID owner(Djinn d) { return entries.get(d).state == State.BOUND ? entries.get(d).owner : null; }

    public String ownerName(Djinn d) { return entries.get(d).ownerName; }

    public UUID serial(Djinn d) { return entries.get(d).serial; }

    /** The Djinn this player owns, or null. */
    public Djinn heldBy(UUID player) {
        for (Djinn d : Djinn.values()) if (player.equals(owner(d))) return d;
        return null;
    }

    public List<UUID> household(Djinn d) { return List.copyOf(entries.get(d).household); }

    /** The Djinn lent to this player, or null. */
    public Djinn lentTo(UUID player) {
        for (Djinn d : Djinn.values()) if (entries.get(d).state == State.BOUND && entries.get(d).household.contains(player)) return d;
        return null;
    }

    public boolean isMember(Djinn d, UUID player) { return entries.get(d).state == State.BOUND && entries.get(d).household.contains(player); }

    /** Djinn nobody holds and no Seal exists for: what a boss or a vault may hand out. */
    public List<Djinn> sleeping() {
        List<Djinn> out = new ArrayList<>();
        for (Djinn d : Djinn.values()) if (entries.get(d).state == State.SLEEPING) out.add(d);
        return out;
    }

    public String sealPlace(Djinn d) {
        Entry e = entries.get(d);
        return e.sealDim.isEmpty() ? "unknown" : e.sealDim + " " + e.sx + " " + e.sy + " " + e.sz;
    }

    public long lastSeen(Djinn d) { return entries.get(d).lastSeen; }

    public boolean zoneRewarded(String zone) { return rewarded.contains(zone); }

    // ---------------------------------------------------------------- changing
    /** A player takes the Djinn. False if another player already holds it. */
    public boolean bind(Djinn d, UUID owner, String ownerName, UUID serial) {
        Entry e = entries.get(d);
        if (e.state == State.BOUND && !owner.equals(e.owner)) return false;
        e.state = State.BOUND;
        e.owner = owner;
        e.ownerName = ownerName == null ? "" : ownerName;
        e.serial = serial;
        e.household.clear();
        setDirty();
        return true;
    }

    /** The owner lets go (the release command; a death does not): the Djinn becomes a Seal at this place, its household is dismissed. */
    public void release(Djinn d, String dim, BlockPos at, long now) {
        Entry e = entries.get(d);
        e.state = State.SEALED;
        e.owner = null;
        e.ownerName = "";
        e.serial = null;
        e.household.clear();
        e.sealDim = dim == null ? "" : dim;
        if (at != null) { e.sx = at.getX(); e.sy = at.getY(); e.sz = at.getZ(); }
        e.lastSeen = now;
        setDirty();
    }

    /** A Seal for a sleeping Djinn has entered the world (a boss, a vault or an admin). */
    public void sealIssued(Djinn d, String dim, BlockPos at, long now) {
        Entry e = entries.get(d);
        if (e.state == State.BOUND) return;
        e.state = State.SEALED;
        e.sealDim = dim == null ? "" : dim;
        if (at != null) { e.sx = at.getX(); e.sy = at.getY(); e.sz = at.getZ(); }
        e.lastSeen = now;
        setDirty();
    }

    /** The Seal was seen (in an inventory, as an item entity, in an opened chest). Cheap: only dirties on a real change. */
    public void seen(Djinn d, long now, String dim, BlockPos at) {
        Entry e = entries.get(d);
        if (e.state != State.SEALED || now - e.lastSeen < 200) return;
        e.lastSeen = now;
        if (dim != null && at != null) { e.sealDim = dim; e.sx = at.getX(); e.sy = at.getY(); e.sz = at.getZ(); }
        setDirty();
    }

    public boolean addMember(Djinn d, UUID member) {
        Entry e = entries.get(d);
        if (e.state != State.BOUND || e.household.contains(member) || e.household.size() >= DjinnPhaseRules.HOUSEHOLD_MAX) return false;
        e.household.add(member);
        setDirty();
        return true;
    }

    public boolean removeMember(Djinn d, UUID member) {
        boolean had = entries.get(d).household.remove(member);
        if (had) setDirty();
        return had;
    }

    public void markZone(String zone) {
        if (rewarded.add(zone)) setDirty();
    }

    /** SEALED Djinn whose Seal has not been seen for too long sleep again. Returns how many woke up asleep. */
    public int sweepLost(long now) {
        int n = 0;
        for (Djinn d : Djinn.values()) {
            Entry e = entries.get(d);
            if (e.state == State.SEALED && now - e.lastSeen > HouseholdRules.SEAL_LOST_AFTER) {
                e.state = State.SLEEPING;
                e.sealDim = "";
                n++;
            }
        }
        if (n > 0) setDirty();
        return n;
    }

    /** Admin: forget everything about a Djinn (it sleeps again). */
    public void reset(Djinn d) {
        entries.put(d, new Entry());
        setDirty();
    }
}
