package com.newuniverse.nusmp.avatar.ai;

/**
 * Why: a mob's goals are asked "can you start?" only every OTHER tick, and which of the two parities that is depends on the entity (its id and the tick it was
 * made). So a start check like {@code tickCount % 20 == 0} is true on an asked tick for half of the Avatars and never for the other half, and the behaviour behind it
 * (a Cowardly retreat, a Gluttonous snack, a Heroic rescue, a naming) silently never happens for them. Dividing the count by two first fixes it: the answer is yes on
 * exactly one asked tick in {@code period}, whichever parity the Avatar has. Pure arithmetic with a test that walks both parities.
 */
public final class TickSlot {
    private TickSlot() {}

    /** Is this tick the Avatar's turn in a rhythm of {@code period} ticks (an even period of at least 4; smaller values count as 4)? */
    public static boolean on(int tickCount, int period) {
        int half = Math.max(2, period / 2);
        return ((tickCount >> 1) % half) == 0;
    }
}
