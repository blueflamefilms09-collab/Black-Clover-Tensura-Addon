package com.newuniverse.nusmp.relics.djinn;

import net.minecraft.server.level.ServerPlayer;

/**
 * Dual Djinn (owner notes: as one of the final Djinn, Belial's Metal Vessel can be used with another Life Magic Djinn, Zagan, without switching Djinn Equips). The mod holds ONE Djinn per
 * player (their own, or one lent to them) and one engine state, so the smallest safe version does not add a second equip: a player who owns the canon Belial can also be a member of Zagan's
 * household (the ledger, {@link HouseholdRules#dualLink}); while they are, Belial's weapon can cast Zagan's first act (look straight down + right-click) with its own cooldown and Magoi cost,
 * from Belial's Half or Full Equip, without ending it. Not done: Zagan's own Equip, vessel or Extreme Magic at the same time (they need a second engine state).
 * <p>Cost: one ledger lookup per right-click that looks down. Server thread only.
 */
public final class BelialDual {
    private BelialDual() {}

    /** Does this player own the canon Belial (the setting on)? */
    public static boolean ownsCanon(ServerPlayer p) { return BelialCanon.on() && DjinnState.of(p).djinn() == Djinn.BELIAL; }

    /** Is the dual link live for this player: the setting on, the canon Belial owned and Zagan shared with them? */
    public static boolean active(ServerPlayer p) {
        if (p == null || p.getServer() == null || !BelialConfig.dual() || !ownsCanon(p)) return false;
        return DjinnLedger.get(p.getServer()).isMember(Djinn.ZAGAN, p.getUUID());
    }
}
