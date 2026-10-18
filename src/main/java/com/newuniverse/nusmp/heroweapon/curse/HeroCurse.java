package com.newuniverse.nusmp.heroweapon.curse;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Why: the Curse Series (a weapon's answer to despair, tied to the seven sins) touches other systems at a few points only: the core asks whether a curse form is
 * active (the look, the HUD), the weapon logic and the forms ask for the corruption, and the events that make a person despair (an ally's death, a betrayal) report
 * to it. This is that small door; the curse builder replaces the whole class. The stub answers "no curse, no corruption" and ignores reports, which is exactly
 * what a player who never despaired gets.
 */
public final class HeroCurse {
    private HeroCurse() {}

    /** True while the player's active form is a Curse Series form. */
    public static boolean active(Player player) { return false; }

    /** True while the weapon holds its wielder (an Overrun): the weapon cannot be dismissed and the form cannot be changed. */
    public static boolean locked(Player player) { return false; }

    /** The corruption of the player, 0 to 1000 (1000 is an Overrun). */
    public static int corruption(Player player) { return 0; }

    /** Despair of the player, 0 to 1000; at the unlock threshold the weapon offers the Curse Series. */
    public static int despair(Player player) { return 0; }

    /** A cause of despair happened to this player (an ally died, a betrayal, a near-death). {@code amount} is in despair points; {@code why} a short key for the log. */
    public static void addDespair(ServerPlayer player, int amount, String why) {}

    /** Corruption from outside the curse forms themselves (a skill that costs its wielder a piece of their mind). */
    public static void addCorruption(ServerPlayer player, int amount) {}
}
