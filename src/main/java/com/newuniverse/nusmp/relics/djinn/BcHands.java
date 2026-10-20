package com.newuniverse.nusmp.relics.djinn;

/**
 * 1.101.0: a pure model of what Better Combat does to the off hand, so the failing path can be shown without the mod (pure, generic over the stack type).
 *
 * <p>Better Combat 2.4.0, {@code net.bettercombat.mixin.player.PlayerEntityMixin#getEquippedStack_Pre} (a head inject on {@code getEquippedStack}, which is
 * Mojang's {@code Player.getItemBySlot}): when the requested slot is the OFF hand and the main-hand stack or the off-hand stack has a weapon profile with
 * {@code two_handed}, it returns EMPTY. {@code LivingEntity.getItemInHand(OFF_HAND)}, {@code getOffhandItem()} and the client's {@code startUseItem} all
 * read through that method, so to the game (and to every mod that asks it) the off hand is empty. The raw inventory ({@code Inventory.offhand.get(0)})
 * is not touched. {@link #seenOffhand} is that rule; {@link #legacyOther} is how the ritual used to find "the other hand"; {@link #rawOther} is how it does now.
 */
public final class BcHands {
    private BcHands() {}

    /** What {@code getItemBySlot(OFFHAND)} answers with Better Combat installed. */
    public static <T> T seenOffhand(T rawOff, T empty, boolean mainTwoHanded, boolean offTwoHanded) {
        return mainTwoHanded || offTwoHanded ? empty : rawOff;
    }

    /** The "other hand" the Seal used to read ({@code player.getItemInHand(the other hand)}): the off hand goes through the lie, the main hand does not. */
    public static <T> T legacyOther(boolean sealInMain, T rawMain, T rawOff, T empty, boolean mainTwoHanded, boolean offTwoHanded) {
        return sealInMain ? seenOffhand(rawOff, empty, mainTwoHanded, offTwoHanded) : rawMain;
    }

    /** The "other hand" read from the raw inventory (what the ritual does now). */
    public static <T> T rawOther(boolean sealInMain, T rawMain, T rawOff) {
        return sealInMain ? rawOff : rawMain;
    }

    /**
     * Can a right-click on a stack in this hand reach the item at all? The client skips an empty hand, and the server's
     * {@code handleUseItem} reads the same lie, so a Seal in the off hand behind a two-handed main-hand weapon is never used.
     */
    public static boolean offHandUsable(boolean mainTwoHanded, boolean offTwoHanded) { return !mainTwoHanded && !offTwoHanded; }
}
