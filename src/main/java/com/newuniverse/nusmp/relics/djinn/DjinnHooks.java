package com.newuniverse.nusmp.relics.djinn;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * What the rest of the relic system may ask about a player's Djinn (server side). The client twin, for rendering other players' vessels and the
 * HUD, is {@code client.relics.djinn.DjinnClientState}.
 */
public final class DjinnHooks {
    private DjinnHooks() {}

    /** The Djinn this player can call: the one they own, else the one lent to them. */
    public static Optional<Djinn> boundTo(ServerPlayer p) { return Optional.ofNullable(DjinnState.of(p).active()); }

    /** The Djinn this player owns (not a lent one). */
    public static Optional<Djinn> ownedBy(ServerPlayer p) { return Optional.ofNullable(DjinnState.of(p).djinn()); }

    /** 0 none, 1 Lesser Summon, 2 Half Equip, 3 Full Equip, 4 while an Extreme Magic is being cast. */
    public static int phase(ServerPlayer p) {
        int ph = DjinnState.of(p).phase();
        return ph == DjinnPhaseRules.FULL && DjinnEngine.charging(p) ? DjinnPhaseRules.EXTREME : ph;
    }

    /** True in a Full Djinn Equip (the Maso). */
    public static boolean fullEquip(ServerPlayer p) { return DjinnState.of(p).phase() >= DjinnPhaseRules.FULL; }

    /** The glow colour (0xRRGGBB) the vessel and its bearer's blows take in Half Equip or higher; 0 when the Djinn is not out. */
    public static int glowRgb(ServerPlayer p) {
        DjinnState s = DjinnState.of(p);
        Djinn d = s.djinn();
        return d != null && s.phase() >= DjinnPhaseRules.HALF ? d.glow() : 0;
    }

    /** The glow of a stack the player holds: its Djinn's glow when it is the bound vessel and the bearer is in Half Equip or higher. */
    public static int glowRgb(ServerPlayer holder, ItemStack stack) {
        Djinn bound = VesselAccess.boundDjinn(stack);
        if (bound == null) return 0;
        DjinnState s = DjinnState.of(holder);
        return bound == s.djinn() && s.phase() >= DjinnPhaseRules.HALF ? bound.glow() : 0;
    }

    /** The share of base EP a Full Equip adds (the engine applies it to Tensura's max magicule and aura): for the relics' EP display. */
    public static double epBonus(ServerPlayer p) { return DjinnState.of(p).phase() >= DjinnPhaseRules.FULL ? DjinnPhaseRules.MASO_EP : 0.0; }
}
