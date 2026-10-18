package com.newuniverse.nusmp.elite.nations.world;

import com.newuniverse.nusmp.elite.nations.contract.Bounty;
import com.newuniverse.nusmp.elite.nations.contract.Contract;
import com.newuniverse.nusmp.elite.nations.core.Nation;
import com.newuniverse.nusmp.elite.nations.rep.ReputationBook;
import com.newuniverse.nusmp.elite.nations.war.AllianceBook;
import com.newuniverse.nusmp.elite.nations.war.War;
import com.newuniverse.nusmp.elite.nations.war.WarBook;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the one SavedData of the Nations & War system ({@code data/nusmp_elite_nations.dat} in the overworld's storage): nations, boards, bounties, reputation, wars with their history, alliances and the
 * clocks of the periodic jobs. The model classes inside are pure; {@link NationNbt} converts them. Reads go through the indexes below (player to nation, capital position to nation) so the per-event hooks
 * are O(1). Every mutation calls {@link #setDirty()} through the service so the game saves it, and {@link #touch} marks the nations whose screens must be refreshed.
 */
public final class NationsData extends SavedData {
    public static final String NAME = "nusmp_elite_nations";

    public final Map<String, Nation> nations = new LinkedHashMap<>();
    public final Map<String, List<Contract>> boards = new HashMap<>();
    public final List<Bounty> bounties = new ArrayList<>();
    public final ReputationBook reputation = new ReputationBook();
    public final AllianceBook alliances = new AllianceBook();
    public WarBook wars = new WarBook(100);
    public long nextUid = 1L;
    public long lastBoardRefresh, lastRepDecay;

    /** Player to nation id, rebuilt on load and kept in step by the service. */
    private final Map<UUID, String> memberIndex = new HashMap<>();
    /** Nations changed since the last sync flush (not saved). */
    public final java.util.Set<String> dirty = new java.util.HashSet<>();

    public static NationsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(NationsData::new, NationsData::load, null), NAME);
    }

    // ---------------------------------------------------------------- indexes
    public String nationIdOf(UUID player) { return player == null ? "" : memberIndex.getOrDefault(player, ""); }

    public Nation nationOf(UUID player) { return nations.get(nationIdOf(player)); }

    public void indexMember(UUID player, String nation) { memberIndex.put(player, nation); }

    public void unindexMember(UUID player) { memberIndex.remove(player); }

    public void reindex() {
        memberIndex.clear();
        for (Nation n : nations.values()) for (UUID m : n.members.keySet()) memberIndex.put(m, n.id);
    }

    public void touch(String nationId) { if (nationId != null && !nationId.isEmpty()) dirty.add(nationId); setDirty(); }

    public List<Contract> board(String nationId) { return boards.computeIfAbsent(nationId, k -> new ArrayList<>()); }

    public long newUid() { return nextUid++; }

    /** The nation whose capital block sits at this exact position, or null. */
    public Nation capitalAt(String dim, int x, int y, int z) {
        for (Nation n : nations.values()) if (n.hasCapital && n.cx == x && n.cy == y && n.cz == z && n.capitalDim.equals(dim)) return n;
        return null;
    }

    /** The nation whose capital area contains the point (horizontal radius, vertical slack), or null. */
    public Nation capitalZoneAt(String dim, int x, int y, int z, int radius, int vertical) {
        for (Nation n : nations.values()) {
            if (n.hasCapital && n.capitalDim.equals(dim) && com.newuniverse.nusmp.elite.nations.core.GuardRules.inZone(x, y, z, n.cx, n.cy, n.cz, radius, vertical)) return n;
        }
        return null;
    }

    // ---------------------------------------------------------------- save / load
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag ns = new ListTag();
        for (Nation n : nations.values()) ns.add(NationNbt.write(n));
        tag.put("Nations", ns);
        CompoundTag bs = new CompoundTag();
        boards.forEach((nid, list) -> {
            ListTag l = new ListTag();
            for (Contract c : list) l.add(NationNbt.write(c));
            bs.put(nid, l);
        });
        tag.put("Boards", bs);
        ListTag bl = new ListTag();
        for (Bounty b : bounties) bl.add(NationNbt.write(b));
        tag.put("Bounties", bl);
        CompoundTag rep = new CompoundTag();
        reputation.raw().forEach((player, map) -> {
            CompoundTag m = new CompoundTag();
            map.forEach(m::putInt);
            rep.put(player.toString(), m);
        });
        tag.put("Rep", rep);
        tag.put("Alliances", NationNbt.write(alliances));
        ListTag ws = new ListTag();
        for (War w : wars.wars.values()) ws.add(NationNbt.write(w));
        tag.put("Wars", ws);
        ListTag hs = new ListTag();
        for (WarBook.Record r : wars.history) hs.add(NationNbt.write(r));
        tag.put("History", hs);
        tag.put("Truce", NationNbt.writeLongMap(wars.truce));
        tag.put("DeclareCd", NationNbt.writeLongMap(wars.declareCooldown));
        tag.putLong("NextWar", wars.nextId);
        tag.putLong("NextUid", nextUid);
        tag.putLong("LastBoard", lastBoardRefresh);
        tag.putLong("LastRepDecay", lastRepDecay);
        return tag;
    }

    public static NationsData load(CompoundTag tag, HolderLookup.Provider provider) {
        NationsData d = new NationsData();
        d.wars = new WarBook(NationsConfig.get(NationsConfig.HISTORY_MAX));
        int ledgerCap = NationsConfig.get(NationsConfig.LEDGER_MAX_ENTRIES);
        for (Tag e : tag.getList("Nations", Tag.TAG_COMPOUND)) {
            Nation n = NationNbt.readNation((CompoundTag) e, ledgerCap);
            if (n != null) d.nations.put(n.id, n);
        }
        CompoundTag bs = tag.getCompound("Boards");
        for (String nid : bs.getAllKeys()) {
            if (!d.nations.containsKey(nid)) continue;
            List<Contract> list = new ArrayList<>();
            for (Tag e : bs.getList(nid, Tag.TAG_COMPOUND)) list.add(NationNbt.readContract((CompoundTag) e));
            d.boards.put(nid, list);
        }
        for (Tag e : tag.getList("Bounties", Tag.TAG_COMPOUND)) d.bounties.add(NationNbt.readBounty((CompoundTag) e));
        CompoundTag rep = tag.getCompound("Rep");
        for (String k : rep.getAllKeys()) {
            try {
                UUID p = UUID.fromString(k);
                CompoundTag m = rep.getCompound(k);
                for (String nid : m.getAllKeys()) d.reputation.set(p, nid, m.getInt(nid));
            } catch (IllegalArgumentException ignored) { /* a bad key from a hand edit */ }
        }
        NationNbt.read(tag.getCompound("Alliances"), d.alliances);
        for (Tag e : tag.getList("Wars", Tag.TAG_COMPOUND)) {
            War w = NationNbt.readWar((CompoundTag) e);
            if (w != null && w.open()) d.wars.wars.put(w.id, w);
        }
        for (Tag e : tag.getList("History", Tag.TAG_COMPOUND)) d.wars.history.add(NationNbt.readRecord((CompoundTag) e));
        NationNbt.readLongMap(tag.getCompound("Truce"), d.wars.truce);
        NationNbt.readLongMap(tag.getCompound("DeclareCd"), d.wars.declareCooldown);
        d.wars.nextId = Math.max(1L, tag.getLong("NextWar"));
        d.nextUid = Math.max(1L, tag.getLong("NextUid"));
        d.lastBoardRefresh = tag.getLong("LastBoard");
        d.lastRepDecay = tag.getLong("LastRepDecay");
        d.reindex();
        return d;
    }
}
