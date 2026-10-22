package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.prop.MagicPropEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why (the "no lag on a public server" rule): the props of Light and Bone Magic are real entities, so their number is bounded twice: one caster may have at most
 * {@link LightBoneJa#MAX_PROPS_PER_CASTER} alive (a new cast past that ends the caster's oldest first) and the whole server at most {@link LightBoneJa#MAX_PROPS_GLOBAL}
 * (past it a new prop is simply not made: the spell still hits, only its model is missing). The decision is the pure {@link LightBoneJa#admit}. The props of a player are ended when the
 * player dies, logs out or changes dimension (a follower also ends by itself when its owner is gone). State is per player UUID, on the server thread only; entries of players with no live props
 * are dropped, so nothing grows. Nothing here reads a client class.
 */
public final class LightBoneJaBudget {
    private LightBoneJaBudget() {}

    private static final Map<UUID, List<MagicPropEntity>> LIVE = new HashMap<>();
    private static boolean wired;

    /** Hooks the cleanup events once (called from the props' init, so no shared registry has to change). */
    public static void wire() {
        if (wired) return;
        wired = true;
        NeoForge.EVENT_BUS.addListener(LightBoneJaBudget::onDeath);
        NeoForge.EVENT_BUS.addListener(LightBoneJaBudget::onLogout);
        NeoForge.EVENT_BUS.addListener(LightBoneJaBudget::onDimension);
    }

    private static void onDeath(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) endAll(p.getUUID()); }
    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { endAll(e.getEntity().getUUID()); }
    private static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { endAll(e.getEntity().getUUID()); }

    /** Ends every live prop of the player and forgets them. */
    static void endAll(UUID id) {
        List<MagicPropEntity> l = LIVE.remove(id);
        if (l == null) return;
        for (MagicPropEntity e : new ArrayList<>(l)) if (!e.isRemoved()) e.expire();
    }

    private static int prune() {
        int total = 0;
        for (Iterator<Map.Entry<UUID, List<MagicPropEntity>>> it = LIVE.entrySet().iterator(); it.hasNext(); ) {
            List<MagicPropEntity> l = it.next().getValue();
            l.removeIf(MagicPropEntity::isRemoved);
            if (l.isEmpty()) it.remove();
            else total += l.size();
        }
        return total;
    }

    /** May this caster make one more prop? Ends the caster's oldest one when the decision says so. */
    static boolean admit(ServerPlayer p) {
        int global = prune();
        List<MagicPropEntity> mine = LIVE.get(p.getUUID());
        int decision = LightBoneJa.admit(mine == null ? 0 : mine.size(), global);
        if (decision == LightBoneJa.REFUSE) return false;
        if (decision == LightBoneJa.ADMIT_AFTER_ENDING_OLDEST && mine != null && !mine.isEmpty()) {
            MagicPropEntity oldest = mine.remove(0);
            if (!oldest.isRemoved()) oldest.expire();
        }
        return true;
    }

    static void track(ServerPlayer p, MagicPropEntity e) { LIVE.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(e); }

    /** How many props the player has alive (for the self test and the report). */
    public static int live(UUID id) {
        prune();
        List<MagicPropEntity> l = LIVE.get(id);
        return l == null ? 0 : l.size();
    }

    /** How many props of both magics are alive on the whole server. */
    public static int liveTotal() { return prune(); }
}
