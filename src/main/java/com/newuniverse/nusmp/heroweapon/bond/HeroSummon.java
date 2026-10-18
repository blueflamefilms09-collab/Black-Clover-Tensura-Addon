package com.newuniverse.nusmp.heroweapon.bond;

import net.minecraft.server.level.ServerPlayer;

/**
 * Why: putting the weapon in the hand and taking it away again (the loan of the projection) is done by the core, but the acquisition ritual, the commands, the forms
 * and the curse all need to ask for it. This is that small door. Owned by the core builder, who replaces this stub with the real loan; the stub refuses both.
 */
public final class HeroSummon {
    private HeroSummon() {}

    /** Summons the bonded weapon into the selected hotbar slot; false when the player has no bond, it is already summoned, or the player is in creative / spectator. */
    public static boolean summon(ServerPlayer player) { return false; }

    /** Puts the weapon away and gives back what it displaced; false when it is not summoned or the curse holds the wielder ({@code HeroCurse.locked}). */
    public static boolean dismiss(ServerPlayer player) { return false; }

    /** True while the weapon is summoned. */
    public static boolean isSummoned(ServerPlayer player) { return HeroBonds.of(player).summoned(); }
}
