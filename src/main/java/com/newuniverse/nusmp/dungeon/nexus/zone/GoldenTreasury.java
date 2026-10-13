package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.city.CityPlan;
import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import com.newuniverse.nusmp.dungeon.nexus.NexusOps;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;

/**
 * The Treasury of the Golden City, planned (docs/living_dungeon.md section 7): a round hall under the plaza, directly beneath the Aurelian
 * Altar, reached by a stair that goes down from the north avenue and then runs in a tunnel under the plaza's north bridge. A mana-locked
 * gate stands in the tunnel. Pure: it is a list of ops in the frame of the city's middle (offsets from it, y absolute), run as a stage
 * after the city itself stands ({@link GoldenStages}), because the city's own floor lies where the hall is cut.
 *
 * <p>Everything keeps clear of the moat: the hall is 5 blocks across at most (the moat begins 6 out and its water is two blocks higher
 * than the hall's floor), the tunnel is three wide and the moat starts three out from the axis, so no water can reach it.
 */
final class GoldenTreasury {
    private GoldenTreasury() {}

    /** The hall's radius (air), the walking level under the plaza's floor, and the tunnel's last and first cells along the avenue. */
    static final int HALL_R = 5, STAIR_TOP = -17, STAIRS = 5, TUNNEL_FROM = -11, TUNNEL_TO = -6, GATE_B = -8;
    /** Offsets, from the city's middle, of the four minor pedestals. */
    static final int[][] MINOR = {{-3, -1}, {3, -1}, {-2, 3}, {2, 3}};

    private static final String BRICKS = "minecraft:polished_blackstone_bricks", GILDED = "minecraft:gilded_blackstone", GOLD = "minecraft:gold_block",
            STAIR = "minecraft:polished_blackstone_brick_stairs", WALL = "minecraft:polished_blackstone_brick_wall", LANTERN = "minecraft:lantern",
            SEA = "minecraft:sea_lantern";

    /** The pedestal block of the central relic (the altar stands directly above it). */
    static ZonePlan.Spot centralSpot(CityPlan c) { return new ZonePlan.Spot(c.centreX, c.floorY - 4, c.centreZ); }

    static ZonePlan.Spot[] minorSpots(CityPlan c) {
        ZonePlan.Spot[] out = new ZonePlan.Spot[MINOR.length];
        for (int i = 0; i < MINOR.length; i++) out[i] = new ZonePlan.Spot(c.centreX + MINOR[i][0], c.floorY - 4, c.centreZ + MINOR[i][1]);
        return out;
    }

    /** The lower cell of the doorway the mana gate fills (the cell above it is the doorway's second). */
    static ZonePlan.Spot gateSpot(CityPlan c) { return new ZonePlan.Spot(c.centreX, c.floorY - 4, c.centreZ + GATE_B); }

    /** The foot of the stair on the surface: where someone stands to go down. */
    static ZonePlan.Spot stairHead(CityPlan c) { return new ZonePlan.Spot(c.centreX, c.floorY + 1, c.centreZ + STAIR_TOP + 1); }

    /** The ops of the Treasury, in the frame of the city's middle: a carve, then the stonework. */
    static ZonePlan ops(CityPlan city, int cellX, int cellZ) {
        final int f = city.floorY;
        ZonePlan.Builder b = ZonePlan.builder(NexusZone.GOLDEN_CITY, cellX, cellZ, city.centreX, city.centreZ)
                .floorY(f).topY(f + 6).footprint(Footprint.Shape.OVAL, city.semiX, city.semiZ);
        NexusOps o = b.ops();

        // ---- carve: the hall, the tunnel and the stairwell
        o.carveDisc(HALL_R, HALL_R, f - 4, f - 1);
        o.carve(-1, f - 4, TUNNEL_FROM, 1, f - 1, TUNNEL_TO);
        for (int k = 1; k <= STAIRS; k++) o.carve(-1, f + 1 - k, STAIR_TOP + k, 1, f + 3, STAIR_TOP + k);
        o.phase();

        // ---- the hall: floor, walls, lights
        o.disc(BRICKS, "", 6, 6, f - 5, f - 5);
        o.disc(GILDED, "", 4, 4, f - 5, f - 5);
        o.disc(GOLD, "", 2, 2, f - 5, f - 5);
        o.discHollow(BRICKS, "", 6, 6, 1, f - 4, f - 1);
        o.air(-1, f - 4, TUNNEL_TO, 1, f - 1, TUNNEL_TO);                                   // the ring of the wall left the tunnel's last cells shut
        for (int[] m : MINOR) o.put(GOLD, m[0], f - 5, m[1]);
        int[][] lamps = {{5, 2}, {-5, 2}, {5, -2}, {-5, -2}, {2, 5}, {-2, 5}};
        for (int[] l : lamps) o.put(SEA, l[0], f - 2, l[1]);

        // ---- the tunnel under the north bridge, and the gate in it
        o.box(BRICKS, -1, f - 5, TUNNEL_FROM, 1, f - 5, TUNNEL_TO);
        o.box(BRICKS, -2, f - 4, TUNNEL_FROM, -2, f - 1, TUNNEL_TO);
        o.box(BRICKS, 2, f - 4, TUNNEL_FROM, 2, f - 1, TUNNEL_TO);
        o.put(SEA, -2, f - 2, TUNNEL_FROM + 1);
        o.put(SEA, 2, f - 2, TUNNEL_FROM + 1);
        o.put(SEA, -2, f - 2, TUNNEL_TO - 1);
        o.put(SEA, 2, f - 2, TUNNEL_TO - 1);
        o.box(GILDED, -1, f - 4, GATE_B, 1, f - 1, GATE_B);                                  // the gate wall, with a doorway two high
        o.air(0, f - 4, GATE_B, 0, f - 3, GATE_B);
        o.box(GOLD, -1, f - 2, GATE_B, 1, f - 2, GATE_B);

        // ---- the stair down from the avenue, and the arch over it
        for (int k = 1; k <= STAIRS; k++) o.box(STAIR, "facing=north,half=bottom", -1, f + 1 - k, STAIR_TOP + k, 1, f + 1 - k, STAIR_TOP + k);
        for (int k = 1; k <= STAIRS; k++) {
            o.put(WALL, -2, f + 1, STAIR_TOP + k);
            o.put(WALL, 2, f + 1, STAIR_TOP + k);
        }
        o.box(GILDED, -2, f + 1, STAIR_TOP, -2, f + 4, STAIR_TOP);
        o.box(GILDED, 2, f + 1, STAIR_TOP, 2, f + 4, STAIR_TOP);
        o.box(GOLD, -2, f + 5, STAIR_TOP, 2, f + 5, STAIR_TOP);
        o.put(LANTERN, "hanging=true", 0, f + 4, STAIR_TOP);
        o.put(LANTERN, "hanging=false", -2, f + 6, STAIR_TOP);
        o.put(LANTERN, "hanging=false", 2, f + 6, STAIR_TOP);
        return b.build();
    }
}
