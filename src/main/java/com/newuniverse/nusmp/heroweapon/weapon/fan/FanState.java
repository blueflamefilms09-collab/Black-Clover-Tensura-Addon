package com.newuniverse.nusmp.heroweapon.weapon.fan;

import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Expiry;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.PerPlayer;

import java.util.UUID;

/**
 * Why: the Fan keeps three small things per wielder that live only as long as the server does: the magic residue it has gathered, the time its Mist Veil
 * holds and the time its glide holds. They are in memory only (a fresh server starts with none, which is exactly what a freshly summoned fan should have), bounded by the shared holders, and
 * dropped when the wielder logs out, puts the weapon away or the server stops, so nothing outlives the server it belongs to (the owner's server rule).
 */
final class FanState {
    private FanState() {}

    static final PerPlayer<ResidueLedger> RESIDUE = new PerPlayer<>(ResidueLedger::new, 256);

    /** The game tick until which a wielder's Mist Veil holds. */
    static final Expiry<UUID> VEIL = new Expiry<>(256);

    /** The game tick until which a wielder's glide (Featherfall) holds unless it is renewed. */
    static final Expiry<UUID> GLIDE = new Expiry<>(256);

    static void forget(UUID id) {
        RESIDUE.forget(id);
        VEIL.clear(id);
        GLIDE.clear(id);
    }

    static void clearAll() {
        RESIDUE.clear();
        VEIL.clearAll();
        GLIDE.clearAll();
    }
}
