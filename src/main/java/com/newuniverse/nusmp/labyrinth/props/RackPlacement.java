package com.newuniverse.nusmp.labyrinth.props;

/**
 * Why: the decisions of turning a template's armour stand into a rack, kept apart from the world so a unit test can pin them. (1) {@link #shouldConvert}: only the mod's own building converts
 * (a stand a player places with the item, or one that is loaded from disk, is never touched), and only while the flag {@code props.rackInsteadOfArmourStand} is on. (2) {@link #target}: where the
 * rack goes. The generator stands an armour stand either on the floor (the block at its feet is air: the rack takes that block) or on a pedestal slab (a bottom slab at its feet: the rack replaces
 * the slab and stands on the block under it) or on something else (the rack goes on top if that is free; if it is not, the stand is left alone: nothing is lost). (3) {@link #facing}: the
 * stand's yaw as one of the four horizontal directions. (4) {@link #lootVariant}: which racks are the loot-filled kind, a stable pick from the position so a repair of a sector gives the same
 * racks. (5) {@link #pack}: which of the stand's six equipment places go to which of the rack's five slots.
 */
public final class RackPlacement {
    private RackPlacement() {}

    /** What is in the block at the stand's feet. */
    public enum Feet { AIR, BOTTOM_SLAB, OTHER }

    /** Where the rack goes: at the stand's own block, replacing the slab there, on the block above, or nowhere (keep the stand). */
    public enum Spot { AT, REPLACE_SLAB, ABOVE, NONE }

    public static boolean shouldConvert(boolean flagOn, boolean dungeonWorld, boolean ownBuilder, boolean loadedFromDisk) {
        return flagOn && dungeonWorld && ownBuilder && !loadedFromDisk;
    }

    public static Spot target(Feet feet, boolean aboveIsAir) {
        switch (feet) {
            case AIR: return Spot.AT;
            case BOTTOM_SLAB: return Spot.REPLACE_SLAB;
            default: return aboveIsAir ? Spot.ABOVE : Spot.NONE;
        }
    }

    /** The stand's yaw as 0 = south, 1 = west, 2 = north, 3 = east (the stand faces south at yaw 0, like every entity). */
    public static int facing(float yawDegrees) {
        return Math.floorMod((int) Math.floor(yawDegrees / 90.0 + 0.5), 4);
    }

    /** Is the rack at this position key one of the loot-filled variants? A stable hash of the key against the chance (0 = none, 1 = all). */
    public static boolean lootVariant(long key, double chance) {
        if (chance <= 0.0) return false;
        if (chance >= 1.0) return true;
        long z = key * 0x9E3779B97F4A7C15L + 0x2545F4914F6CDD1DL;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        double u = (z >>> 11) / (double) (1L << 53);
        return u < chance;
    }

    /**
     * The slots a stand's equipment places take on a rack of five: the stand lists main hand, off hand, head, chest, legs, feet; the present ones go to slots 0, 1, 2, ... in that order and
     * what does not fit (the boots, when the stand is fully dressed and armed) is dropped. Returns, for each place, the slot or -1.
     */
    public static int[] pack(boolean[] present) {
        int[] out = new int[present.length];
        int next = 0;
        for (int i = 0; i < present.length; i++) {
            if (present[i] && next < RackLayout.SLOTS) out[i] = next++;
            else out[i] = -1;
        }
        return out;
    }
}
