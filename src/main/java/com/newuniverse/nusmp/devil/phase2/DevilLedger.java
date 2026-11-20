package com.newuniverse.nusmp.devil.phase2;

import com.newuniverse.nusmp.devil.phase2.Phase2Rules.Kind;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: phase 2 needs the world to remember, per player UUID, which devils they hold and how (submission, even, dominance), which devils they were severed from for good (the ban list) and until when their
 * sabotage debuff lasts; and, per devil, how many contracts it has, so that the screen and the power split never scan anybody. This is that memory: world saved data {@code nusmp_devil_phase2} (not the grimoire
 * pact table, which is {@code nusmp_devil_pacts}). The per-devil list of holders is derived data kept in step with the rows. Read or written only when somebody signs, is severed, logs in, or asks; never per tick.
 * Bounds: at most {@link #MAX_PLAYERS} rows, {@link #MAX_PACTS} pacts and {@link #MAX_BANS} bans per player; every bound is applied again when the file is read, so a damaged file can not grow it.
 */
public final class DevilLedger extends SavedData {
    public static final int MAX_PLAYERS = 4096, MAX_PACTS = 16, MAX_BANS = 64;

    /** One contract of one player. */
    public record Pact(String devil, Kind kind, long since) {}

    private static final class Row {
        final List<Pact> pacts = new ArrayList<>();
        final Set<String> banned = new LinkedHashSet<>();
        long debuffUntil;

        boolean empty() { return pacts.isEmpty() && banned.isEmpty() && debuffUntil <= 0; }
    }

    private final Map<UUID, Row> rows = new LinkedHashMap<>();
    private final Map<String, List<UUID>> byDevil = new LinkedHashMap<>();

    public static DevilLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(DevilLedger::new, DevilLedger::load, null), "nusmp_devil_phase2");
    }

    public static DevilLedger load(CompoundTag t, HolderLookup.Provider p) {
        DevilLedger l = new DevilLedger();
        ListTag list = t.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && l.rows.size() < MAX_PLAYERS; i++) {
            CompoundTag c = list.getCompound(i);
            if (!c.hasUUID("id")) continue;
            UUID id = c.getUUID("id");
            Row r = new Row();
            ListTag pacts = c.getList("pacts", Tag.TAG_COMPOUND);
            for (int k = 0; k < pacts.size() && r.pacts.size() < MAX_PACTS; k++) {
                CompoundTag pc = pacts.getCompound(k);
                String devil = pc.getString("devil");
                Kind kind = Kind.byName(pc.getString("kind"));
                if (devil.isEmpty() || devil.length() > 64 || kind == null || has(r, devil)) continue;
                r.pacts.add(new Pact(devil, kind, Math.max(0L, pc.getLong("since"))));
            }
            ListTag bans = c.getList("bans", Tag.TAG_STRING);
            for (int k = 0; k < bans.size() && r.banned.size() < MAX_BANS; k++) if (!bans.getString(k).isEmpty() && bans.getString(k).length() <= 64) r.banned.add(bans.getString(k));
            r.debuffUntil = Math.max(0L, c.getLong("debuff"));
            if (r.empty()) continue;
            l.rows.put(id, r);
            for (Pact pa : r.pacts) l.index(pa.devil(), id);
        }
        return l;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider p) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Row> e : rows.entrySet()) {
            Row r = e.getValue();
            CompoundTag c = new CompoundTag();
            c.putUUID("id", e.getKey());
            ListTag pacts = new ListTag();
            for (Pact pa : r.pacts) {
                CompoundTag pc = new CompoundTag();
                pc.putString("devil", pa.devil());
                pc.putString("kind", pa.kind().name());
                pc.putLong("since", pa.since());
                pacts.add(pc);
            }
            c.put("pacts", pacts);
            ListTag bans = new ListTag();
            for (String b : r.banned) bans.add(StringTag.valueOf(b));
            c.put("bans", bans);
            c.putLong("debuff", r.debuffUntil);
            list.add(c);
        }
        t.put("players", list);
        return t;
    }

    private static boolean has(Row r, String devil) {
        for (Pact p : r.pacts) if (p.devil().equals(devil)) return true;
        return false;
    }

    private void index(String devil, UUID id) {
        List<UUID> l = byDevil.computeIfAbsent(devil, k -> new ArrayList<>());
        if (!l.contains(id)) l.add(id);
    }

    private void unindex(String devil, UUID id) {
        List<UUID> l = byDevil.get(devil);
        if (l == null) return;
        l.remove(id);
        if (l.isEmpty()) byDevil.remove(devil);
    }

    private Row row(UUID id, boolean create) {
        Row r = rows.get(id);
        if (r == null && create && rows.size() < MAX_PLAYERS) { r = new Row(); rows.put(id, r); }
        return r;
    }

    private void trim(UUID id) {
        Row r = rows.get(id);
        if (r != null && r.empty()) rows.remove(id);
        setDirty();
    }

    // ---------------------------------------------------------------- reading
    /** How many contracts a devil has. */
    public int count(String devil) {
        List<UUID> l = devil == null ? null : byDevil.get(devil);
        return l == null ? 0 : l.size();
    }

    /** The players who hold a devil (a copy; at most the devil's cap, 16). */
    public List<UUID> holders(String devil) {
        List<UUID> l = devil == null ? null : byDevil.get(devil);
        return l == null ? List.of() : List.copyOf(l);
    }

    /** The contracts of a player (a copy; at most 16). */
    public List<Pact> pacts(UUID id) {
        Row r = id == null ? null : rows.get(id);
        return r == null ? List.of() : List.copyOf(r.pacts);
    }

    public Pact pact(UUID id, String devil) {
        Row r = id == null ? null : rows.get(id);
        if (r != null && devil != null) for (Pact p : r.pacts) if (p.devil().equals(devil)) return p;
        return null;
    }

    public boolean banned(UUID id, String devil) {
        Row r = id == null ? null : rows.get(id);
        return r != null && devil != null && r.banned.contains(devil);
    }

    public List<String> bans(UUID id) {
        Row r = id == null ? null : rows.get(id);
        return r == null ? List.of() : List.copyOf(r.banned);
    }

    /** Do these two players hold a devil in common that the filter calls shared? No allocation: the fast path of the friendly-fire check. */
    public boolean commonShared(UUID a, UUID b, java.util.function.Predicate<String> shared) {
        Row ra = a == null ? null : rows.get(a), rb = b == null ? null : rows.get(b);
        if (ra == null || rb == null) return false;
        for (Pact p : ra.pacts) if (has(rb, p.devil()) && shared.test(p.devil())) return true;
        return false;
    }

    public long debuffUntil(UUID id) {
        Row r = id == null ? null : rows.get(id);
        return r == null ? 0L : r.debuffUntil;
    }

    public int playerRows() { return rows.size(); }

    // ---------------------------------------------------------------- writing
    /** The player now holds {@code devil} as {@code kind}. False (nothing written) when they already do, hold the most they can, or the table is full. */
    public boolean join(UUID id, String devil, Kind kind, long now) {
        if (id == null || devil == null || devil.isEmpty() || devil.length() > 64 || kind == null) return false;
        Row r = row(id, true);
        if (r == null || r.pacts.size() >= MAX_PACTS || has(r, devil)) return false;
        r.pacts.add(new Pact(devil, kind, Math.max(0L, now)));
        index(devil, id);
        setDirty();
        return true;
    }

    /** The contract ends (no ban). The pact that was removed, or null. */
    public Pact leave(UUID id, String devil) {
        Row r = id == null ? null : rows.get(id);
        if (r == null || devil == null) return null;
        for (int i = 0; i < r.pacts.size(); i++) {
            if (r.pacts.get(i).devil().equals(devil)) {
                Pact gone = r.pacts.remove(i);
                unindex(devil, id);
                trim(id);
                return gone;
            }
        }
        return null;
    }

    /** Every contract of the player ends (an operator cleared them). Returns the devils that were let go. */
    public List<String> leaveAll(UUID id) {
        List<String> out = new ArrayList<>();
        for (Pact p : pacts(id)) {
            leave(id, p.devil());
            out.add(p.devil());
        }
        return out;
    }

    /** The player may never take this devil again (severed). */
    public void ban(UUID id, String devil) {
        Row r = row(id, true);
        if (r == null || devil == null || devil.isEmpty() || devil.length() > 64 || r.banned.size() >= MAX_BANS) return;
        if (r.banned.add(devil)) setDirty();
    }

    /** An operator lifts a ban. */
    public boolean unban(UUID id, String devil) {
        Row r = id == null ? null : rows.get(id);
        if (r == null || !r.banned.remove(devil)) return false;
        trim(id);
        return true;
    }

    public void setDebuff(UUID id, long until) {
        Row r = row(id, true);
        if (r == null) return;
        r.debuffUntil = Math.max(0L, until);
        trim(id);
    }
}
