package com.newuniverse.nusmp.heroweapon.weapon.glassworld;

import java.util.UUID;

/**
 * Why: a weapon logic keeps small runtime state per wielder (residue, souls, tempo, read pages). When the wielder logs out, or the self-test lets go of its
 * stand-in player, that state must be dropped at once, and the test harness must be able to do it without knowing which weapon it is looking at. A logic that
 * keeps such state implements this one method.
 */
public interface Forgetful {
    /** Drops everything the logic remembers about this wielder. */
    void forget(UUID wielder);
}
