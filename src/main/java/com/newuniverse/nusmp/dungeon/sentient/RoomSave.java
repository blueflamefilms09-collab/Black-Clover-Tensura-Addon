package com.newuniverse.nusmp.dungeon.sentient;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The Dungeon's memory of its rooms, saved with the world in the Overworld's data storage as "nusmp_rooms" (like the Dungeon's other
 * data): which rooms were rolled, whether they are populated, and when they last birthed. The bookkeeping is {@link RoomBook}; this puts it
 * into NBT as four arrays.
 */
public final class RoomSave extends SavedData {
    public static final String NAME = "nusmp_rooms";

    private RoomBook book = new RoomBook();

    public static RoomSave get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(RoomSave::new, RoomSave::load, null), NAME);
    }

    private static RoomSave load(CompoundTag tag, HolderLookup.Provider provider) {
        RoomSave s = new RoomSave();
        s.book = RoomBook.fromArrays(tag.getLongArray("Keys"), tag.getLongArray("Born"), tag.getLongArray("Seen"), tag.getByteArray("State"));
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        RoomBook.Saved s = book.toArrays();
        tag.putLongArray("Keys", s.keys());
        tag.putLongArray("Born", s.births());
        tag.putLongArray("Seen", s.seen());
        tag.putByteArray("State", s.states());
        return tag;
    }

    /** How many rooms the Dungeon remembers. */
    public int size() { return book.size(); }

    public int count(byte state) { return book.count(state); }

    /** The record of a room, or null if it was never rolled; looking marks it seen. */
    public RoomBook.Room lookup(long key, long now) { return book.lookup(key, now); }

    /** Writes down what a room rolled. */
    public RoomBook.Room record(long key, byte state, long now) {
        RoomBook.Room r = book.record(key, state, now);
        setDirty();
        return r;
    }

    /** Forgets every room whose chunk column is within {@code chunkRadius} chunks of the given chunk; returns how many. */
    public int forgetAround(int chunkX, int chunkZ, int chunkRadius) {
        int n = book.forget(k -> Math.abs(RoomKey.chunkX(k) - chunkX) <= chunkRadius && Math.abs(RoomKey.chunkZ(k) - chunkZ) <= chunkRadius);
        if (n > 0) setDirty();
        return n;
    }
}
