package com.newuniverse.nusmp.companion.bond;

import com.newuniverse.nusmp.companion.CompanionRules;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Why: the contract of an entity that takes part in the shared bond system: gifts with favourites and hates, the high five, a stance, the pouch. She says who she is ({@link #bondKey}, {@link #langPrefix}), holds
 * her side of the bond ({@link #bond}), reports her rapport with a player and takes more of it, says whom she walks with ({@link #bondOwner}), and keeps the high five in synched data so clients can pose her. What a
 * stance DOES is her own job (her target goal asks {@code Bond.wants} / {@code Bond.foe}). Anything implementing this is never attacked by another companion ({@code Bond.foe}). All of it is read and written on the
 * server thread.
 *
 * <p>Her own lines are lang keys under {@link #langPrefix()}: {@code act.five}, {@code use} (the item), {@code gift.love} / {@code gift.ok} / {@code gift.hate} / {@code gift.no} (the item), {@code likes.love} /
 * {@code likes.hate} (the items) and {@code stance.guard} / {@code stance.aggressive} / {@code stance.passive}. The common wording (buttons, why a choice is closed) is the mod's own, {@code nusmp.companion.*}.
 */
public interface Companion {
    /** The high five kinds: none, and the high five. */
    int NONE = CompanionRules.NONE, FIVE = CompanionRules.FIVE;

    /** Unique, lower case letters, digits and underscores; names what is kept on the player (use the mod id as a prefix). */
    String bondKey();

    /** Where her own lines are in the lang file, with the trailing dot ({@code "nusmp.companion.charmy."}). */
    String langPrefix();

    /** Her side of the bond: stance, pouch, the use-it switch. Saved with her ({@link PersonState#save}). */
    PersonState bond();

    /** The player's rapport with her, 0..99. */
    int bondRapport(ServerPlayer player);

    /** Adds rapport (which may be negative). */
    void bondAddRapport(ServerPlayer player, int n);

    /** The player she walks with, or null when she is nobody's. */
    UUID bondOwner();

    /** Does she fight? False: no stance buttons. */
    default boolean fighter() { return true; }

    /** Says a line (a speech bubble over her head, usually). */
    default void bondSpeak(Component line) {}

    /** Shows a mood: glad (an accepted gift) or eating, for a number of ticks. */
    default void bondFeel(boolean eating, int ticks) {}

    /** The synched social pose: {@link #NONE} or {@link #FIVE}. */
    int socialKind();

    /** Ticks since the pose began (with the partial tick), or -1 when there is none. */
    float socialAge(float partialTick);

    /** Starts a social pose for {@code ticks} ticks ({@code <0}: until ended), or ends it with {@link #NONE}. */
    void social(int kind, int ticks);

    /** False for a companion that cannot keep things (one that is only with the player for a while): no pouch. */
    default boolean canCarry() { return true; }

    /** True when she has gifts of her own: the shared gift buttons and favourites then do not apply to her. */
    default boolean customGifts() { return false; }

    /** The tier of the player's rapport with her: 0 stranger, 1 acquaintance, 2 friend, 3 close. */
    default int bondTier(ServerPlayer player) { return CompanionRules.tier(bondRapport(player)); }
}
