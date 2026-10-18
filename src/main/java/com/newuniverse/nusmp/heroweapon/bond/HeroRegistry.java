package com.newuniverse.nusmp.heroweapon.bond;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: "one holder per Holy Weapon" must hold across logouts: the holder may be offline, and a second player must still be refused the same weapon. The world
 * keeps a small register, the file {@code nusmp_hero_registry.dat} in the Overworld's data storage, of who holds which weapon (weapon id to holder uuids), claimed
 * when a bond is made and freed when it is released. The rules of the register are the pure {@link HeroRegistryData}; this class is the saved-data wrapper and
 * the doors the rest of the mod uses: the bind gate asks {@link #refusal}, the other systems (the gem's {@code holders} command, the status command) ask
 * {@link #holdersOf}. A claim by a player who is online and no longer holds the weapon (their data was reset by hand) is stale and is swept when it is
 * met; a claim by a player who never comes back is cleared by an operator with {@link #clear} ({@code /multiverse hero registry clear}). Every bonded player
 * re-registers at login, so a world made before the register existed fills it as its players return. Server thread only.
 */
public final class HeroRegistry extends SavedData {
    public static final String NAME = "nusmp_hero_registry";
    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    private final HeroRegistryData data = new HeroRegistryData();

    /** The register of this world (created empty on first use). */
    public static HeroRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(HeroRegistry::new, HeroRegistry::load, null), NAME);
    }

    static HeroRegistry load(CompoundTag tag, HolderLookup.Provider provider) {
        HeroRegistry r = new HeroRegistry();
        CompoundTag holders = tag.getCompound("holders");
        for (String weapon : holders.getAllKeys()) {
            ListTag l = holders.getList(weapon, Tag.TAG_STRING);
            for (int i = 0; i < l.size(); i++) {
                String id = l.getString(i);
                if (parse(id) != null) r.data.claim(weapon, id);
            }
        }
        return r;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        CompoundTag holders = new CompoundTag();
        for (Map.Entry<String, List<String>> e : data.toMap().entrySet()) {
            ListTag l = new ListTag();
            for (String id : e.getValue()) l.add(StringTag.valueOf(id));
            holders.put(e.getKey(), l);
        }
        tag.put("holders", holders);
        return tag;
    }

    private static UUID parse(String text) {
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException notAUuid) {
            return null;
        }
    }

    // ================================================================ reading
    /** The first holder of a weapon, or null when nobody holds it. */
    public static UUID holderOf(MinecraftServer server, HeroKind kind) {
        String id = get(server).data.firstHolder(kind.itemId());
        return id == null ? null : parse(id);
    }

    /** Every holder of a weapon, in the order they claimed it (a Holy Weapon has at most one while {@code holyUnique} is on). */
    public static List<UUID> holdersOf(MinecraftServer server, HeroKind kind) {
        List<UUID> out = new ArrayList<>();
        for (String id : get(server).data.holdersOf(kind.itemId())) {
            UUID u = parse(id);
            if (u != null) out.add(u);
        }
        return out;
    }

    /** The name to show for a holder: the online player's, else the profile cache's, else the uuid. */
    public static String nameOf(MinecraftServer server, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) return online.getGameProfile().getName();
        try {
            if (server.getProfileCache() != null) return server.getProfileCache().get(id).map(p -> p.getName()).orElse(id.toString());
        } catch (RuntimeException unknown) {
            // a profile cache that cannot answer: the uuid will do
        }
        return id.toString();
    }

    // ================================================================ the bind gate and the bond listeners
    /**
     * The reason {@code p} may not take {@code kind} because another player holds it, or null. A claim by an online player who no longer holds the weapon is
     * stale: it is swept and does not refuse.
     */
    static String refusal(ServerPlayer p, HeroKind kind) {
        MinecraftServer server = p.getServer();
        if (server == null) return null;
        HeroRegistry r = get(server);
        String me = p.getUUID().toString();
        String other;
        while ((other = r.data.otherHolder(kind.itemId(), me)) != null) {
            UUID u = parse(other);
            ServerPlayer online = u == null ? null : server.getPlayerList().getPlayer(u);
            if (u == null || online != null && HeroBonds.kindOf(online) != kind) {
                r.data.release(kind.itemId(), other);
                r.setDirty();
                continue;
            }
            return "The " + kind.display() + " already has a wielder (" + (u == null ? other : nameOf(server, u)) + ")";
        }
        return null;
    }

    /** Records that {@code p} holds {@code kind} (and frees any other weapon they were recorded as holding). */
    static void claim(ServerPlayer p, HeroKind kind) { agree(p, kind); }

    /** Frees {@code p}'s claim of {@code kind}. */
    static void release(ServerPlayer p, HeroKind kind) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        HeroRegistry r = get(server);
        if (r.data.release(kind.itemId(), p.getUUID().toString())) r.setDirty();
    }

    /** At login (and respawn): makes the register agree with the player's own bond, in both directions. */
    static void reconcile(ServerPlayer p) { agree(p, HeroBonds.kindOf(p)); }

    /** Makes the register say that {@code p} holds exactly {@code kind} (nothing when null). */
    private static void agree(ServerPlayer p, HeroKind kind) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        HeroRegistry r = get(server);
        String me = p.getUUID().toString();
        boolean changed = r.data.releaseAllExcept(me, kind == null ? null : kind.itemId());
        if (kind != null) changed |= r.data.claim(kind.itemId(), me);
        if (changed) r.setDirty();
    }

    /** An operator's cleanup: frees every claim of one weapon (its holder's data is gone for good). Returns how many holders were dropped. */
    public static int clear(MinecraftServer server, HeroKind kind) {
        HeroRegistry r = get(server);
        int n = r.data.clearWeapon(kind.itemId());
        if (n > 0) {
            r.setDirty();
            LOG.warn("[nusmp] hero registry: the claims of {} were cleared by an operator ({} holder(s))", kind.itemId(), n);
        }
        return n;
    }
}
