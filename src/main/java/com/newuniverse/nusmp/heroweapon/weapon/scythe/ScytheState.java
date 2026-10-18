package com.newuniverse.nusmp.heroweapon.weapon.scythe;

import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Expiry;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.PerPlayer;

import java.util.UUID;

/**
 * Why: the Scythe keeps three small things in memory that live only as long as the server does: the souls each wielder has gathered, whether a wielder's Floating
 * Scythe is out (so a second cannot be conjured on top of it), and which foes were just stripped of a blessing by the Null Edge (so one foe is not stripped every
 * tick). They are bounded by the shared holders and dropped when the wielder logs out, puts the weapon away or the server stops, so nothing outlives the server
 * it belongs to (the owner's server rule).
 */
final class ScytheState {
    private ScytheState() {}

    static final PerPlayer<SoulLedger> SOULS = new PerPlayer<>(SoulLedger::new, 256);

    /** The game tick until which a wielder's Floating Scythe is out. */
    static final Expiry<UUID> FLOATING = new Expiry<>(256);

    /** The game tick before which a foe is not stripped again by the Null Edge. */
    static final Expiry<UUID> STRIPPED = new Expiry<>(512);

    static void forget(UUID wielder) {
        SOULS.forget(wielder);
        FLOATING.clear(wielder);
    }

    static void clearAll() {
        SOULS.clear();
        FLOATING.clearAll();
        STRIPPED.clearAll();
    }
}
