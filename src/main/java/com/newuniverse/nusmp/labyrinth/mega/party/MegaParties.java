package com.newuniverse.nusmp.labyrinth.mega.party;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the party index of a party (which lane its floors stand on) must outlive the server, the player and the team, or a restart would hand a party a different lane and
 * strand its floors. This is the saved book: {@link PartyIndex} (the pure rules) wrapped in a {@code SavedData} file {@code data/nusmp_mega_parties.dat} in the OVERWORLD's
 * data storage, next to {@code nusmp_mega.dat} that holds the sectors (the overworld is always loaded, and a lookup from a command or a gate never has to wake a dimension).
 * It is a separate file from the sectors on purpose: the sectors carry their own {@code party} number, so the floors are never lost with the book, and the book is tiny. The
 * mutating methods mark the file dirty, so nobody has to remember to. Server thread only.
 */
public final class MegaParties extends SavedData {
    public static final String NAME = "nusmp_mega_parties";

    private final PartyIndex book = new PartyIndex();

    public MegaParties() {}

    public static MegaParties get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(MegaParties::new, MegaParties::load, null), NAME);
    }

    /** The book, to READ (use the methods below to change it, so the file is saved). */
    public PartyIndex index() { return book; }

    /** {@link PartyIndex#allocate}, saved. */
    public PartyIndex.Allocation allocate(PartyKey key, String label, int maxParties, Set<Integer> reserved, List<UUID> inheritFrom) {
        PartyIndex.Allocation a = book.allocate(key, label, maxParties, reserved, inheritFrom);
        if (a.created()) setDirty();
        return a;
    }

    /** {@link PartyIndex#assign}, saved. Returns the index the party had before, or -1. */
    public int assign(PartyKey key, String label, int index) {
        int before = book.assign(key, label, index);
        setDirty();
        return before;
    }

    /** {@link PartyIndex#relabel}, saved. */
    public boolean relabel(UUID key, String label) {
        boolean changed = book.relabel(key, label);
        if (changed) setDirty();
        return changed;
    }

    /** {@link PartyIndex#forget}, saved. */
    public boolean forget(UUID key) {
        boolean was = book.forget(key);
        if (was) setDirty();
        return was;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (PartyIndex.Entry e : book.entries()) {
            CompoundTag t = new CompoundTag();
            t.putString("key", (e.team() ? PartyKey.team(e.key()) : PartyKey.solo(e.key())).text());
            t.putInt("index", e.index());
            t.putString("label", e.label());
            list.add(t);
        }
        tag.put("parties", list);
        return tag;
    }

    public static MegaParties load(CompoundTag tag, HolderLookup.Provider registries) {
        MegaParties out = new MegaParties();
        ListTag list = tag.getList("parties", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            PartyKey key = PartyKey.parse(t.getString("key"));
            if (key == null) continue;                                         // a damaged entry is dropped, the rest of the book is kept
            out.book.restore(new PartyIndex.Entry(key.id(), key.team(), t.getInt("index"), t.getString("label")));
        }
        return out;
    }
}
