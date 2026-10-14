package com.newuniverse.nusmp.relics.scaling;

import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.relics.scaling.AuraScaling.Awakening;
import com.newuniverse.nusmp.relics.scaling.AuraScaling.Aura;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a player's aura is, read from Tensura once a second and kept: a hit then costs a map read and a tick comparison. Both versions of the aura
 * (ordinary target, boss target) are computed together, so the hit only picks one. Server side only.
 */
public final class AuraCache {
    private AuraCache() {}

    /** Ticks a reading stays good. */
    public static final int TTL = 20;

    public record Snapshot(long tick, Awakening awakening, boolean zone, double pool, Aura normal, Aura boss) {}

    private static final Map<UUID, Snapshot> CACHE = new ConcurrentHashMap<>();

    public static Snapshot of(ServerPlayer p) {
        long now = p.level().getGameTime();
        Snapshot s = CACHE.get(p.getUUID());
        if (s != null && now >= s.tick() && now - s.tick() < TTL) return s;
        s = read(p, now);
        if (CACHE.size() > 400) CACHE.clear();                                    // a long-running server cannot make this grow without end
        CACHE.put(p.getUUID(), s);
        return s;
    }

    public static void forget(UUID id) { CACHE.remove(id); }

    public static void clear() { CACHE.clear(); }

    private static Snapshot read(ServerPlayer p, long now) {
        double pool = 0;
        try { pool = EnergyHelper.getMaxMagicule(p); } catch (Throwable ignored) { /* no Tensura data: no pool bonus */ }
        Awakening aw = Awakening.NONE;
        try {
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex != null) {
                if (ex.isTrueDemonLord() || ex.isTrueHero()) aw = Awakening.AWAKENED;
                else if (ex.isDemonLordSeed() || ex.isHeroEgg()) aw = Awakening.SEEDED;
            }
        } catch (Throwable ignored) { /* same */ }
        if (crownAwakened(p)) aw = Awakening.AWAKENED;                              // the Wizard King's crown is a third path to the same state
        boolean zone = false;
        try { zone = ManaLaw.zoneActive(p); } catch (Throwable ignored) { /* same */ }
        return new Snapshot(now, aw, zone, pool, AuraScaling.compute(aw, zone, pool, false), AuraScaling.compute(aw, zone, pool, true));
    }

    /** CrownAwakening keeps {@code Mult} above 1 in the player's persisted data while the crown awakening is on. */
    private static boolean crownAwakened(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, 10)) return false;
        CompoundTag root = pd.getCompound(Player.PERSISTED_NBT_TAG);
        return root.contains("nusmp_crown_awakening", 10) && root.getCompound("nusmp_crown_awakening").getDouble("Mult") > 1.0;
    }
}
