package com.newuniverse.nusmp.book;

import net.minecraft.nbt.CompoundTag;

/**
 * Why: the page bit masks of a grimoire instance ("Unlocked": which pages are open, "GateTold": which pages the player was told are waiting for a gate) are an int tag for modes 0-31 plus a second
 * int tag (the key + "2") for modes 32-63, see {@link UnlockMask}. Every reader and writer goes through here so no place can alias bit 32 onto bit 0. The second tag is only written when it is not zero
 * (or already exists), so an item of a book with 31 pages or fewer is byte for byte what it was before. Common code (a NBT tag is not a client class).
 */
public final class PageBits {
    private PageBits() {}

    public static final String UNLOCKED = "Unlocked", GATE_TOLD = "GateTold";

    public static long read(CompoundTag t, String key) { return UnlockMask.join(t.getInt(key), t.getInt(key + "2")); }

    public static void write(CompoundTag t, String key, long mask) {
        t.putInt(key, UnlockMask.lo(mask));
        int hi = UnlockMask.hi(mask);
        if (hi != 0 || t.contains(key + "2")) t.putInt(key + "2", hi);
    }

    public static boolean has(CompoundTag t, String key, int mode) { return UnlockMask.has(read(t, key), mode); }

    public static void set(CompoundTag t, String key, int mode) { if (UnlockMask.in(mode)) write(t, key, UnlockMask.with(read(t, key), mode)); }

    public static void clear(CompoundTag t, String key, int mode) { if (UnlockMask.in(mode)) write(t, key, UnlockMask.without(read(t, key), mode)); }

    /** Forgets the whole mask (both ints). */
    public static void remove(CompoundTag t, String key) { t.remove(key); t.remove(key + "2"); }
}
