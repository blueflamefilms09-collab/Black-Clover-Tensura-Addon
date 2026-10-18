package com.newuniverse.nusmp.heroweapon.bond;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Why: putting the weapon in the hand and taking it away again (the loan of the projection) is done by the core, but the acquisition ritual, the commands, the forms
 * and the curse all need to ask for it. This is that small door; the machine behind it is {@link HeroLoan}. The answers are plain booleans so a caller does not
 * have to know the machine's outcomes; the keys and the commands that want to tell the player WHY something was refused ask {@code HeroLoan} directly.
 */
public final class HeroSummon {
    private HeroSummon() {}

    /** Summons the bonded weapon into the selected hotbar slot; false when the player has no bond, it is already summoned, or the player is in creative / spectator. */
    public static boolean summon(ServerPlayer player) { return HeroLoan.summon(player, false) == HeroLoan.Outcome.OK; }

    /** Puts the weapon away and gives back what it displaced; false when it is not summoned or the curse holds the wielder ({@code HeroCurse.locked}). */
    public static boolean dismiss(ServerPlayer player) { return HeroLoan.dismiss(player, false) == HeroLoan.Outcome.OK; }

    /** True while the weapon is summoned. */
    public static boolean isSummoned(ServerPlayer player) { return HeroBonds.of(player).summoned(); }

    /** The summoned weapon itself (the projection in the player's inventory), or an empty stack when it is stowed. */
    public static ItemStack stack(ServerPlayer player) { return HeroLoan.stackOf(player); }
}
