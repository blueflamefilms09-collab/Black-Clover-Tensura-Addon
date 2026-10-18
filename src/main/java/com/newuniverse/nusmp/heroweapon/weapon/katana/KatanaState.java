package com.newuniverse.nusmp.heroweapon.weapon.katana;

import com.newuniverse.nusmp.heroweapon.weapon.glassworld.PerPlayer;

import java.util.UUID;

/**
 * Why: the Katana keeps one small session per wielder in memory only (the tempo of the duel, the parry and evade windows, the phantom blade, the riposte, the
 * time since the last action): a fresh server starts with none, which is what a freshly summoned katana should have, the holder is bounded, and the session is
 * dropped when the wielder logs out, puts the weapon away or the server stops, so nothing outlives the server it belongs to (the owner's server rule).
 */
final class KatanaState {
    private KatanaState() {}

    static final PerPlayer<KatanaSession> SESSIONS = new PerPlayer<>(KatanaSession::new, 256);

    static void forget(UUID wielder) { SESSIONS.forget(wielder); }

    static void clearAll() { SESSIONS.clear(); }
}
