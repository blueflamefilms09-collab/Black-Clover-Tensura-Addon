package com.newuniverse.nusmp.elite.races.runtime;

import com.newuniverse.nusmp.elite.races.state.TransformMachine;
import com.newuniverse.nusmp.elite.races.state.Zenkai;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: what the elite races remember about ONE player, in one place: the state machine (as its short saved string), the kill counters the evolution rules read, the Zenkai stacks,
 * the template the player was last seen as (so a change of race resets the state) and the tick at which an evolution last took its items (so the two evolution paths never
 * charge twice). It lives in the player's persistent data under one compound ({@code nusmp_elite_races}, inside {@code Player.PERSISTED_NBT_TAG}, like the other per-player
 * state of this mod), is copied on {@code PlayerEvent.Clone} by {@link RaceRuntime}, and the machine is kept in a cache per player while they are online (written back on every
 * change, so a crash loses at most the last few ticks of a running state). Server side only.
 */
public final class RaceLedger {
    private RaceLedger() {}

    public static final String KEY = "nusmp_elite_races";
    private static final Map<UUID, TransformMachine> MACHINES = new ConcurrentHashMap<>();

    /** The player's compound (created when missing). */
    public static CompoundTag data(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, 10)) pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!root.contains(KEY, 10)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }

    /** The player's compound if there is one, else a detached empty one: reading must not write an empty ledger into every player who is not an elite race. */
    private static CompoundTag peek(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, 10)) return new CompoundTag();
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        return root.contains(KEY, 10) ? root.getCompound(KEY) : new CompoundTag();
    }

    private static final String FRESH = new TransformMachine().encode();

    // ---------------------------------------------------------------- the state machine
    public static TransformMachine machine(Player p) {
        return MACHINES.computeIfAbsent(p.getUUID(), id -> TransformMachine.decode(peek(p).getString("Machine")));
    }

    /** Writes the machine into the player's data; a machine that never left its fresh state writes nothing. */
    public static void saveMachine(Player p) {
        TransformMachine m = MACHINES.get(p.getUUID());
        if (m == null) return;
        String enc = m.encode();
        if (enc.equals(FRESH) && !peek(p).contains("Machine")) return;
        data(p).putString("Machine", enc);
    }

    /** Forget the cache (logout, clone): the next read decodes the saved string again. */
    public static void forget(Player p) {
        saveMachine(p);
        MACHINES.remove(p.getUUID());
    }

    public static void drop(UUID id) { MACHINES.remove(id); }

    // ---------------------------------------------------------------- the race the machine belongs to
    public static String template(Player p) { return peek(p).getString("Template"); }

    public static void setTemplate(Player p, String id) { data(p).putString("Template", id == null ? "" : id); }

    // ---------------------------------------------------------------- kills
    public static int kills(Player p, String key) {
        return peek(p).getCompound("Kills").getInt(key);
    }

    public static void addKill(Player p, String key) {
        CompoundTag d = data(p);
        if (!d.contains("Kills", 10)) d.put("Kills", new CompoundTag());
        CompoundTag k = d.getCompound("Kills");
        k.putInt(key, (int) Math.min(Integer.MAX_VALUE, (long) k.getInt(key) + 1L));
    }

    public static void setKills(Player p, String key, int n) {
        CompoundTag d = data(p);
        if (!d.contains("Kills", 10)) d.put("Kills", new CompoundTag());
        d.getCompound("Kills").putInt(key, Math.max(0, n));
    }

    // ---------------------------------------------------------------- Zenkai
    public static Zenkai.State zenkai(Player p) {
        CompoundTag d = peek(p);
        if (!d.contains("ZenkaiStacks")) return Zenkai.State.NONE;
        return new Zenkai.State(d.getInt("ZenkaiStacks"), d.getLong("ZenkaiAt"));
    }

    public static void setZenkai(Player p, Zenkai.State s) {
        CompoundTag d = data(p);
        d.putInt("ZenkaiStacks", s.stacks());
        d.putLong("ZenkaiAt", s.lastAt());
    }

    // ---------------------------------------------------------------- evolution bookkeeping
    public static long lastConsume(Player p) {
        CompoundTag d = peek(p);
        return d.contains("LastConsume") ? d.getLong("LastConsume") : -1L;
    }

    public static void markConsumed(Player p, long gameTime) { data(p).putLong("LastConsume", gameTime); }

    /** Copies the ledger from the old player body to the new one (respawn, End exit). The machine itself is reset by the caller on death. */
    public static void copy(Player from, Player to) {
        saveMachine(from);
        CompoundTag src = from.getPersistentData().contains(Player.PERSISTED_NBT_TAG, 10) ? from.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG) : null;
        if (src == null || !src.contains(KEY, 10)) return;
        CompoundTag dstRoot = to.getPersistentData();
        if (!dstRoot.contains(Player.PERSISTED_NBT_TAG, 10)) dstRoot.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        dstRoot.getCompound(Player.PERSISTED_NBT_TAG).put(KEY, src.getCompound(KEY).copy());
        MACHINES.remove(to.getUUID());
    }
}
