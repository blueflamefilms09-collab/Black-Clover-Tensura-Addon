package com.newuniverse.nusmp.labyrinth.loot.backpack;

import java.util.Locale;

/**
 * Why: a backpack has five sizes and every part of the feature (the block state, the item component, the menu's rows, the model, the texture, the upgrade items, the drop table)
 * has to agree on which is which, so the tier is one pure enum with the numbers in it: leather 27 slots (three rows, a chest), iron 36, gold 45, diamond 54 (a double chest) and
 * netherite 63 (seven rows, the dungeon tier). A slot count is always a whole number of rows of nine, so the screen is a plain grid. The ordinal is the saved number (the block
 * state, the item component), so entries are only ever APPENDED at the end; an unknown number from a newer save is clamped to the nearest tier rather than failing. Pure.
 */
public enum BackpackTier {
    LEATHER("leather", 27),
    IRON("iron", 36),
    GOLD("gold", 45),
    DIAMOND("diamond", 54),
    NETHERITE("netherite", 63);

    /** The slots in a row. */
    public static final int COLUMNS = 9;

    /** The most slots any tier has (the size of the block entity's list). */
    public static final int MAX_SLOTS = 63;

    private final String id;
    private final int slots;

    BackpackTier(String id, int slots) {
        this.id = id;
        this.slots = slots;
    }

    public String id() { return id; }

    public int slots() { return slots; }

    public int rows() { return slots / COLUMNS; }

    /** The tier of a saved number; below 0 is leather, above the last is the last. */
    public static BackpackTier of(int index) {
        BackpackTier[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, index))];
    }

    public static BackpackTier fromId(String id) {
        if (id == null) return null;
        String s = id.trim().toLowerCase(Locale.ROOT);
        for (BackpackTier t : values()) if (t.id.equals(s)) return t;
        return null;
    }

    /** The biggest tier whose slots fit in a list of this size (leather when the list is smaller than leather): the tier a window over such a list must use. */
    public static BackpackTier fitting(int size) {
        BackpackTier best = LEATHER;
        for (BackpackTier t : values()) if (t.slots <= size) best = t;
        return best;
    }

    /** Is there a bigger tier? */
    public boolean hasNext() { return ordinal() < values().length - 1; }

    /** The next bigger tier, or this one when it is the biggest. */
    public BackpackTier next() { return hasNext() ? values()[ordinal() + 1] : this; }

    /** The registry path of the upgrade item that makes a backpack THIS tier; null for leather (the plain backpack). */
    public String upgradeItemId() { return this == LEATHER ? null : "backpack_upgrade_" + id; }

    /** The tier an upgrade item of this registry path makes, or null when the path is not an upgrade's. */
    public static BackpackTier ofUpgrade(String path) {
        for (BackpackTier t : values()) if (t.upgradeItemId() != null && t.upgradeItemId().equals(path)) return t;
        return null;
    }

    /**
     * What an upgrade of {@code target} does to a backpack of {@code current}: the tier the backpack becomes. An upgrade only ever makes a backpack BIGGER; one that is not bigger
     * than what the backpack already is changes nothing (returns {@code current}), so an upgrade can never shrink a backpack and drop its items.
     */
    public static BackpackTier upgraded(BackpackTier current, BackpackTier target) {
        return target != null && target.ordinal() > current.ordinal() ? target : current;
    }
}
