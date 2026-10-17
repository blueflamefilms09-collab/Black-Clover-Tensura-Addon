package com.newuniverse.nusmp.labyrinth.mega.gen;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: regenerating a sector from its saved seed ({@code /multiverse mega regen}, and the repair after a crash) must give EXACTLY the sector that was
 * placed, or the new rooms would not meet the tunnel and the neighbour that already hang from the old ones. A sector's plan depends on the other sectors in
 * one way that is easy to get wrong: it keeps out of their regions and tunnels, and its Way Onward is chosen by how well the next region would fit among them.
 * When the sector was first planned only the sectors BEFORE it existed, so a replay must see exactly those and no later one: the later sectors, above all the
 * next one whose tunnel starts at this sector's own seal, would make the original seal look blocked and the replay would pick another. This pure class makes
 * that rule one function, so the game-side generator (which reads the sectors from the saved data) and the simulator test (which replays whole simulated
 * dungeons and compares every sector with the original) use the same one.
 */
public final class WorldObstacles {
    private WorldObstacles() {}

    /**
     * What the obstacle rule needs to know about one saved sector: its id, its region, the seal its tunnel starts at (null for sector 0 or a sector that hangs
     * from nothing) and the cell of its Landing door.
     */
    public record Known(int id, Box region, Vec prevSeal, Facing prevFacing, Vec entryJigsaw) {}

    /**
     * The regions and tunnels the sector numbered {@code before} must keep clear of: those of the sectors with a LOWER id (every one of them existed when it was
     * first planned). {@code skipRegion} leaves one more region out (the planner of the NEXT sector adds the region of the sector it hangs from itself); -1 for
     * none. The tunnel INTO the sector being planned is never in the list: the request builds that one.
     */
    public static List<SectorPlanner.Obstacle> of(List<Known> sectors, int before, int skipRegion) {
        List<SectorPlanner.Obstacle> out = new ArrayList<>();
        for (Known k : sectors) {
            if (k.id() >= before) continue;
            if (k.id() != skipRegion) out.add(SectorPlanner.Obstacle.region(k.region()));
            if (k.prevSeal() == null || k.prevFacing() == null) continue;
            int modules = SectorPlanner.linkModulesBetween(k.prevSeal(), k.prevFacing(), k.entryJigsaw());
            if (modules > 0) out.add(SectorPlanner.Obstacle.tunnel(SectorPlanner.tunnelBox(k.prevSeal(), k.prevFacing(), modules)));
        }
        return out;
    }
}
