package com.newuniverse.nusmp.charter;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.UUID;

/**
 * The world's drop lock for the Charter Grimoires, saved with the world in the Overworld's data storage as {@code nusmp_charter_drops}.
 * The tag holds one boolean per grimoire under exactly the keys {@code has_dropped_charter_grimoire_<bossId>}, and a {@code claims} compound
 * with who got it first (UUID, name) and on which day. Once a flag is true it never becomes false through the code in this class: the lock is
 * a {@link DropLedger}, which has no public way to unset a claim; the only door is the package-private {@link #adminClear}, used by the
 * operator command {@code /multiverse charter reset} (and logged as a warning).
 */
public final class ModWorldDropData extends SavedData {
    public static final String NAME = "nusmp_charter_drops";
    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    private final DropLedger ledger = new DropLedger();
    /** The in-game day (from 1) as of the last {@link #get}; the day a claim made through the three-argument {@code markDropped} is written with. */
    private long day = 1L;

    public static ModWorldDropData get(MinecraftServer server) {
        ModWorldDropData d = server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ModWorldDropData::new, ModWorldDropData::load, null), NAME);
        d.day = server.overworld().getGameTime() / 24000L + 1L;
        return d;
    }

    // ---------------------------------------------------------------- reading
    public boolean hasDropped(String bossId) { return ledger.hasDropped(bossId); }

    public DropLedger.Claim claim(String bossId) { return ledger.claim(bossId); }

    /** Every claim, in the order they were made. */
    public Map<String, DropLedger.Claim> all() { return ledger.all(); }

    // ---------------------------------------------------------------- writing
    /** Sets the lock for a boss. True only for the call that set it: the first player wins, every later call answers false and changes nothing. */
    public boolean markDropped(String bossId, UUID player, String name) { return markDropped(bossId, player, name, day); }

    public boolean markDropped(String bossId, UUID player, String name, long dayFrom1) {
        if (!ledger.markDropped(bossId, player, name, dayFrom1)) return false;
        setDirty();
        return true;
    }

    /** The operator escape hatch (a grimoire was lost for good). Returns the claim that was removed, or null if there was none. */
    DropLedger.Claim adminClear(String bossId, String by) {
        DropLedger.Claim old = ledger.adminClear(bossId);
        if (old != null) {
            setDirty();
            LOG.warn("[nusmp] Canon Grimoire lock of '{}' cleared by {} (it had been claimed by {} on day {})", bossId, by, old.name(), old.day());
        }
        return old;
    }

    // ---------------------------------------------------------------- saving
    static ModWorldDropData load(CompoundTag tag, HolderLookup.Provider provider) {
        ModWorldDropData d = new ModWorldDropData();
        CompoundTag claims = tag.getCompound("claims");
        for (String key : tag.getAllKeys()) {
            if (!key.startsWith(CharterTable.FLAG_PREFIX) || !tag.getBoolean(key)) continue;
            String boss = key.substring(CharterTable.FLAG_PREFIX.length());
            CompoundTag c = claims.getCompound(boss);
            UUID who = c.hasUUID("Uuid") ? c.getUUID("Uuid") : new UUID(0L, 0L);
            d.ledger.restore(boss, who, c.getString("Name"), c.contains("Day", Tag.TAG_ANY_NUMERIC) ? c.getLong("Day") : 0L);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        for (CharterTable.Boss b : CharterTable.all()) tag.putBoolean(b.flag(), ledger.hasDropped(b.id()));
        CompoundTag claims = new CompoundTag();
        for (Map.Entry<String, DropLedger.Claim> e : ledger.all().entrySet()) {
            tag.putBoolean(CharterTable.FLAG_PREFIX + e.getKey(), true);                     // also for an id the table no longer knows: a claim is never dropped on save
            CompoundTag c = new CompoundTag();
            if (e.getValue().id() != null) c.putUUID("Uuid", e.getValue().id());
            c.putString("Name", e.getValue().name());
            c.putLong("Day", e.getValue().day());
            claims.put(e.getKey(), c);
        }
        tag.put("claims", claims);
        return tag;
    }
}
