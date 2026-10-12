package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import com.newuniverse.nusmp.dungeon.nexus.NexusState;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;

/**
 * The small rules of the Treasury Collapse that are not its clock ({@link CollapsePlan}): when two collapses are the same one, what
 * ground a collapse outside a standing Nexus covers, who counts as inside, and how hard the expulsion at the end may hit. Pure, so every
 * one of them is checked in tests.
 */
public final class CollapseRules {
    private CollapseRules() {}

    /** One collapse per area: a new one is refused while another is active with its centre within this many blocks (flat). */
    public static final int AREA = 96;
    /** The ground of a collapse outside a standing Nexus (a "pocket"): an oval of this radius round the spot, this many blocks high above its floor. */
    public static final int POCKET_RADIUS = 22, POCKET_HEIGHT = 15;
    /** A player this close outside the footprint's edge still counts as inside it (the same two blocks the Nexus's own lookups use). */
    public static final int EDGE = 2;

    /** The ground of a collapse: where it stands and how high. */
    public record Ground(Footprint fp, int floorY, int topY) {}

    /** The pocket round a spot where a collapse starts outside a standing Nexus: its floor is the block under the feet. */
    public static Ground pocket(int x, int y, int z) {
        int floor = y - 1;
        return new Ground(Footprint.oval(x, z, POCKET_RADIUS, POCKET_RADIUS), floor, floor + POCKET_HEIGHT);
    }

    /** Are two collapses centred on these columns the same one (the second would start within AREA blocks of the first)? */
    public static boolean sameArea(int ax, int az, int bx, int bz) {
        long dx = Math.abs((long) ax - bx), dz = Math.abs((long) az - bz);
        if (dx > AREA || dz > AREA) return false;                                       // also keeps the squares below from overflowing
        return dx * dx + dz * dz <= (long) AREA * AREA;
    }

    /** Is this block inside the volume of a collapse: the footprint (with its edge) from a little below the floor to a little above the roof? */
    public static boolean inside(Footprint fp, int floorY, int topY, int x, int y, int z) {
        return y >= floorY - ZonePlan.BELOW && y <= topY + ZonePlan.ABOVE && fp.contains(x, z, EDGE);
    }

    // ---------------------------------------------------------------- what a collapse is
    /** What a collapse centred on a block is: the fall of a standing Nexus, a pocket round the spot, or nothing (still being built; already gone). */
    public enum Mode { NEXUS, POCKET, NOT_READY, GONE }

    /**
     * `state` is the state of the Nexus whose volume holds the block (null if the block is in none). A standing Nexus falls; one that is
     * still being built is not brought down (the builder is writing it); one that has collapsed already is gone, and only a pocket can be
     * made in its ruin, and not by the death of its boss.
     */
    public static Mode modeFor(NexusState state, boolean bossDefeated) {
        if (state == null) return Mode.POCKET;
        return switch (state) {
            case DONE -> Mode.NEXUS;
            case PLAN, BUILDING -> Mode.NOT_READY;
            case COLLAPSED -> bossDefeated ? Mode.GONE : Mode.POCKET;
            case UNBUILT -> Mode.POCKET;
        };
    }

    /** What the collapse of a Nexus does once a second about the Nexus's saved state: nothing (it is marked), mark it (not yet, or the mark was lost), or end (it was marked and has been reset or rebuilt since). */
    public enum Watch { KEEP, MARK, END }

    public static Watch watch(boolean sawCollapsed, NexusState now) {
        if (now == NexusState.COLLAPSED) return Watch.KEEP;
        return sawCollapsed ? Watch.END : Watch.MARK;
    }

    /** Is the rolling front about to come down on the column at `index` of the order: past the cursor, inside what it reaches in the next few seconds, and not while it only cracks? */
    public static boolean frontAhead(CollapsePlan.Stage stage, int index, int cursor, int limit) {
        return stage != CollapsePlan.Stage.CRACKS && stage != CollapsePlan.Stage.WIPE && index >= 0 && index >= cursor && index < limit;
    }

    // ---------------------------------------------------------------- the expulsion at the end
    /** The share of a player's maximum health the expulsion takes: heavy. */
    public static final float EXPEL_SHARE = 0.75f;
    /** Magicule Poisoning after the expulsion: how long (two minutes) and the level (the third: three tenths of the maximum health). */
    public static final int POISON_TICKS = 2400, POISON_LEVEL = 2;
    /** The least health an expelled player is left with when the expulsion is not lethal: half a heart. */
    public static final float SPARED_HEALTH = 1.0f;

    /**
     * The damage the expulsion deals to someone with `health` of `maxHealth`: three quarters of the maximum, but when it must not kill
     * (`lethal` false) never more than leaves SPARED_HEALTH. NaN or empty values give 0, so a broken player value can never hurt.
     */
    public static float expelDamage(float health, float maxHealth, boolean lethal) {
        if (!(maxHealth > 0f) || !(health > 0f)) return 0f;
        float heavy = maxHealth * EXPEL_SHARE;
        if (lethal) return heavy;
        return Math.max(0f, Math.min(heavy, health - SPARED_HEALTH));
    }

    // ---------------------------------------------------------------- the falling blocks
    /** What a block that lands on someone does, for the part of the collapse that dropped it: DRIP_DAMAGE for a crack or a corridor block, FRONT_HIT for the rolling front. */
    public static int hit(boolean drip) { return drip ? CollapsePlan.DRIP_DAMAGE : CollapsePlan.FRONT_HIT; }
}
