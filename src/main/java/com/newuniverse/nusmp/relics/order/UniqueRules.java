package com.newuniverse.nusmp.relics.order;

import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.TreeMap;

/**
 * Why: which relics may exist only once in a world used to be fixed in code (every relic but a consumable), and the owner wants the God of Order to have a command list that sets it up per
 * world. So the list is saved data: a map from relic id to "one world only" (true) or "may exist many times" (false), holding ONLY what an operator changed, so a relic nobody touched keeps the
 * code's own default and a later update that adds relics needs no migration. It lives in the Overworld's data storage next to the Order's ledger (the same lifetime: it is the Order's rulebook) as
 * {@code nusmp_relic_unique_rules}, and while a server runs it is plugged into {@code RelicDef.isUnique} through a one-function hook, so every system that asks "is this relic unique" (the ledger, the
 * scanner, the loot roll, the commands, the vaults) obeys the list with no change of its own. The commands that edit it are in {@link RelicOrderCommand}. A relic made one-world-only AFTER copies
 * exist is not purged: the first copy the scan finds becomes the valid one (the ledger adopts it) and the Order takes extra copies only once it enforces.
 */
public final class UniqueRules extends SavedData {
    public static final String NAME = "nusmp_relic_unique_rules";

    private final Map<String, Boolean> overrides = new TreeMap<>();

    public static UniqueRules get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(UniqueRules::new, UniqueRules::load, null), NAME);
    }

    /** Plugs the list into {@link RelicDef#isUnique} for this server (called when it has started). */
    public static void bind(MinecraftServer server) {
        UniqueRules rules = get(server);
        RelicDefHook.set(id -> rules.overrides.get(id));
    }

    /** Takes the list out again (the server is stopping). */
    public static void unbind() { RelicDefHook.set(id -> null); }

    /** The operator's setting for a relic: true, false, or null when it has none. */
    public Boolean override(String id) { return overrides.get(id); }

    /** Sets a relic: {@code oneWorld} true = only one may exist, false = any number. */
    public void set(String id, boolean oneWorld) {
        overrides.put(id, oneWorld);
        setDirty();
    }

    /** Gives a relic its default back. False when it had no setting. */
    public boolean reset(String id) {
        boolean had = overrides.remove(id) != null;
        if (had) setDirty();
        return had;
    }

    public int resetAll() {
        int n = overrides.size();
        overrides.clear();
        if (n > 0) setDirty();
        return n;
    }

    public Map<String, Boolean> all() { return java.util.Collections.unmodifiableMap(overrides); }

    /** The effective answer for a relic (its setting, else its default). */
    public static boolean effective(RelicDef d) { return d.isUnique(); }

    /** How many relics are one-world-only now, and how many may exist many times. */
    public static int[] counts() {
        int one = 0, many = 0;
        for (RelicDef d : Relics.all()) {
            if (d.isPairLeft()) continue;
            if (d.isUnique()) one++;
            else many++;
        }
        return new int[] {one, many};
    }

    static UniqueRules load(CompoundTag tag, HolderLookup.Provider provider) {
        UniqueRules r = new UniqueRules();
        ListTag one = tag.getList("One", Tag.TAG_STRING), many = tag.getList("Many", Tag.TAG_STRING);
        for (int i = 0; i < one.size(); i++) r.overrides.put(one.getString(i), true);
        for (int i = 0; i < many.size(); i++) r.overrides.put(many.getString(i), false);
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag one = new ListTag(), many = new ListTag();
        for (Map.Entry<String, Boolean> e : overrides.entrySet()) (e.getValue() ? one : many).add(StringTag.valueOf(e.getKey()));
        tag.put("One", one);
        tag.put("Many", many);
        return tag;
    }
}
