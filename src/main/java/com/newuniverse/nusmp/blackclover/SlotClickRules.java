package com.newuniverse.nusmp.blackclover;

import java.util.UUID;

/**
 * The pure rules of the Grimoire Slot (no Minecraft types, unit tested): what a click on the slot does, how the scan of the inventory is staggered
 * between players, the change signal that replaces the per-tick deep stack compare, and the click rate limit. {@link GrimoireSlot} applies them.
 */
public final class SlotClickRules {
    private SlotClickRules() {}

    public enum Action { NONE, PLACE, TAKE, SWAP, TO_INVENTORY, FROM_INVENTORY }

    /**
     * What a click on the slot does. {@code cursorOwned}: the carried stack is one of the player's own grimoires. A shift-click moves the book out of
     * the slot, or (slot empty) an owned grimoire from the inventory into it; it does nothing while the cursor carries something. Only button 0
     * (left) and 1 (right) count.
     */
    public static Action decide(boolean cursorOwned, boolean cursorEmpty, boolean slotEmpty, boolean shift, int button) {
        if (button != 0 && button != 1) return Action.NONE;
        if (shift) return !cursorEmpty ? Action.NONE : slotEmpty ? Action.FROM_INVENTORY : Action.TO_INVENTORY;
        if (cursorEmpty) return slotEmpty ? Action.NONE : Action.TAKE;
        if (!cursorOwned) return Action.NONE;                       // only the player's own bound grimoire may be placed
        return slotEmpty ? Action.PLACE : Action.SWAP;
    }

    // ---------------------------------------------------------------- scan stagger
    /** The inventory is looked at once per this many ticks per player. */
    public static final int SCAN_EVERY = 20;

    /** The tick (mod {@link #SCAN_EVERY}) a player's scan runs on: spread by UUID so the players are never all scanned in the same tick. */
    public static int phase(UUID id) { return Math.floorMod(id.hashCode(), SCAN_EVERY); }

    public static boolean scanDue(long tick, int phase) { return Math.floorMod(tick + phase, (long) SCAN_EVERY) == 0; }

    // ---------------------------------------------------------------- change signal
    /** A version counter: the slot bumps it on every write, the tick sends the hip book only while it differs from the version last sent. */
    public static final class Signal {
        private int version;
        private int sent = -1;
        public void bump() { version++; }
        public boolean dirty() { return version != sent; }
        public void markSent() { sent = version; }
        /** Forces a send (a new client entity after respawn or dimension change). */
        public void reset() { sent = -1; }
    }

    // ---------------------------------------------------------------- rate limit
    public static final int CLICKS_PER_WINDOW = 8, WINDOW_TICKS = 20;

    /** At most {@link #CLICKS_PER_WINDOW} accepted per {@link #WINDOW_TICKS} ticks; a clock that goes backwards starts a new window. */
    public static final class Limiter {
        private long start = Long.MIN_VALUE / 2;
        private int count;
        public boolean allow(long tick) {
            if (tick - start >= WINDOW_TICKS || tick < start) { start = tick; count = 0; }
            if (count >= CLICKS_PER_WINDOW) return false;
            count++;
            return true;
        }
    }
}
