package com.newuniverse.nusmp.arc.arena;

import com.newuniverse.nusmp.arena.ArenaState;
import com.newuniverse.nusmp.arena.ReturnBook;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the story arenas must remember, across restarts, five things: which site of the grid each character was given (so an arena never moves under a party that
 * built in it), which sites are built and by which build version (so a changed formula is laid again by the repair, and an old arena is never laid twice), the
 * state of each site's fight ({@code ArenaState}, the Final Hour's own state machine, reused: ready, fighting or resting, and until when), which boss belongs to
 * each fight, and where each traveller must be returned ({@code ReturnBook}, also reused). It also keeps what each player has chosen on the lectern of the
 * Chronicle (a plain boss id per player, bounded). One saved-data object on the overworld's storage holds them, the way the other server-wide books of the mod do;
 * the rules are in the pure classes and this is only the wrapper that writes them into a tag. Garbage in a tag is made safe, never thrown.
 */
public final class ArcArenaSave extends SavedData {
    public static final String NAME = "nusmp_arc_arena";
    /** Bump when an arena's formula or palette changes in a way that a built arena should be told about (the repair lays the missing and changed blocks). */
    public static final int BUILD_VERSION = 1;
    /** The most chosen-boss entries kept (the oldest goes first), so a server that thousands once visited does not grow without end. */
    public static final int MAX_PICKS = 4096;

    private final Map<String, Integer> slots = new LinkedHashMap<>();
    private final Map<Integer, Integer> built = new LinkedHashMap<>();
    private final Map<Integer, ArenaState> states = new LinkedHashMap<>();
    private final Map<Integer, UUID> bosses = new LinkedHashMap<>();
    private final Map<UUID, String> picks = new LinkedHashMap<>();
    private final ReturnBook returns = new ReturnBook();

    public static ArcArenaSave get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ArcArenaSave::new, ArcArenaSave::load, null), NAME);
    }

    // ---------------------------------------------------------------- slots
    /** The slot of a character, or -1 when it has none yet. */
    public int slotOf(String characterId) { return slots.getOrDefault(characterId, -1); }

    /** The character that holds a slot, or null. */
    public String characterAt(int slot) {
        for (Map.Entry<String, Integer> e : slots.entrySet()) if (e.getValue() == slot) return e.getKey();
        return null;
    }

    /** The slot of a character; the first free one is given the first time. -1 when the grid is full. */
    public int slotFor(String characterId) {
        int have = slotOf(characterId);
        if (have >= 0) return have;
        int next = ArenaGrid.nextFree(slots);
        if (next < 0) return -1;
        slots.put(characterId, next);
        setDirty();
        return next;
    }

    public Map<String, Integer> slots() { return java.util.Collections.unmodifiableMap(slots); }

    // ---------------------------------------------------------------- built
    public int builtVersion(int slot) { return built.getOrDefault(slot, 0); }

    public void markBuilt(int slot, int version) {
        built.put(slot, version);
        setDirty();
    }

    // ---------------------------------------------------------------- fights
    /** The fight state of a slot (made on first use). The caller calls {@link #setDirty()} after it changes the state. */
    public ArenaState state(int slot) { return states.computeIfAbsent(slot, s -> new ArenaState()); }

    public boolean hasState(int slot) { return states.containsKey(slot); }

    public Map<Integer, ArenaState> allStates() { return java.util.Collections.unmodifiableMap(states); }

    public UUID bossId(int slot) { return bosses.get(slot); }

    public void bossId(int slot, UUID id) {
        if (id == null) bosses.remove(slot);
        else bosses.put(slot, id);
        setDirty();
    }

    // ---------------------------------------------------------------- picks and returns
    public String pick(UUID player) { return picks.get(player); }

    public void pick(UUID player, String characterId) {
        if (player == null) return;
        picks.remove(player);
        if (characterId != null && !characterId.isEmpty()) picks.put(player, characterId);
        while (picks.size() > MAX_PICKS) picks.remove(picks.keySet().iterator().next());
        setDirty();
    }

    public ReturnBook returns() { return returns; }

    // ---------------------------------------------------------------- the tag
    /** Reads the save from its tag (what the world's storage and the self-test call). */
    public static ArcArenaSave load(CompoundTag tag, HolderLookup.Provider provider) {
        ArcArenaSave s = new ArcArenaSave();
        CompoundTag sl = tag.getCompound("Slots");
        for (String k : sl.getAllKeys()) {
            int v = sl.getInt(k);
            if (v >= 0 && v < ArenaGrid.MAX_SLOTS && !s.slots.containsValue(v)) s.slots.put(k, v);
        }
        CompoundTag b = tag.getCompound("Built");
        for (String k : b.getAllKeys()) {
            Integer slot = parse(k);
            if (slot != null) s.built.put(slot, b.getInt(k));
        }
        CompoundTag st = tag.getCompound("States");
        for (String k : st.getAllKeys()) {
            Integer slot = parse(k);
            if (slot == null) continue;
            ArenaState a = new ArenaState();
            a.restore(st.getLongArray(k));
            s.states.put(slot, a);
        }
        CompoundTag bo = tag.getCompound("Bosses");
        for (String k : bo.getAllKeys()) {
            Integer slot = parse(k);
            if (slot != null && bo.hasUUID(k)) s.bosses.put(slot, bo.getUUID(k));
        }
        CompoundTag pk = tag.getCompound("Picks");
        for (String k : pk.getAllKeys()) {
            try {
                s.picks.put(UUID.fromString(k), pk.getString(k));
            } catch (IllegalArgumentException notAUuid) {
                // dropped
            }
        }
        CompoundTag r = tag.getCompound("Returns");
        Map<String, String> in = new LinkedHashMap<>();
        for (String k : r.getAllKeys()) in.put(k, r.getString(k));
        s.returns.fromStrings(in);
        return s;
    }

    private static Integer parse(String key) {
        try {
            int v = Integer.parseInt(key);
            return v >= 0 && v < ArenaGrid.MAX_SLOTS ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag sl = new CompoundTag();
        for (Map.Entry<String, Integer> e : slots.entrySet()) sl.putInt(e.getKey(), e.getValue());
        tag.put("Slots", sl);
        CompoundTag b = new CompoundTag();
        for (Map.Entry<Integer, Integer> e : built.entrySet()) b.putInt(Integer.toString(e.getKey()), e.getValue());
        tag.put("Built", b);
        CompoundTag st = new CompoundTag();
        for (Map.Entry<Integer, ArenaState> e : states.entrySet()) st.putLongArray(Integer.toString(e.getKey()), e.getValue().save());
        tag.put("States", st);
        CompoundTag bo = new CompoundTag();
        for (Map.Entry<Integer, UUID> e : bosses.entrySet()) bo.putUUID(Integer.toString(e.getKey()), e.getValue());
        tag.put("Bosses", bo);
        CompoundTag pk = new CompoundTag();
        for (Map.Entry<UUID, String> e : picks.entrySet()) pk.putString(e.getKey().toString(), e.getValue());
        tag.put("Picks", pk);
        CompoundTag r = new CompoundTag();
        for (Map.Entry<String, String> e : returns.toStrings().entrySet()) r.putString(e.getKey(), e.getValue());
        tag.put("Returns", r);
        return tag;
    }
}
