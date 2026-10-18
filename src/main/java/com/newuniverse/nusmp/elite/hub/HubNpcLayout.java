package com.newuniverse.nusmp.elite.hub;

import com.newuniverse.nusmp.labyrinth.mega.HubLayout;
import com.newuniverse.nusmp.labyrinth.mega.hub.HubPlazaLayout;

/**
 * Why: the two Hub NPCs must stand somewhere that makes sense on the plaza of Floor 0, and the plaza is a stamped template whose stalls and notice pavilion sit at known coordinates
 * ({@code tools/labyrinth/gen_mega_hub.py}: the pavilion's board wall is at z = 15 with its front at z = 14, four market stalls on the diagonals at (+-10, +-8) with the counter one block on the
 * open side). So the spots are numbers here, derived from {@link HubLayout} (the arrival dais is at the spawn x/z, the plaza is centred on {@code PLAZA_X}/{@code PLAZA_Z}) and pure, so a test can
 * prove that both fall inside the plaza's protected box, away from the home gate and on opposite sides of the arrival. The Quest Giver stands in front of the notice board, straight ahead of a
 * newcomer who arrives facing south, and looks back at them (yaw 180 = north); the Merchant stands behind the counter of the red stall, looking out at the avenue (yaw 0 = south). The y is not
 * stored: the spawner finds the walkable ground at the column at run time, so the NPCs also stand right when the fallback plaza was built instead of the template.
 */
public final class HubNpcLayout {
    private HubNpcLayout() {}

    /** A standing spot: block-centre x and z, and the yaw the NPC faces when placed. */
    public record Spot(double x, double z, float yaw) {}

    /** In front of the notice pavilion, facing the arrival dais. */
    public static Spot questGiver() { return new Spot(HubLayout.PLAZA_X + 0.5, HubLayout.PLAZA_Z + 12.5, 180.0f); }

    /** Behind the counter of the red stall (north-east diagonal), facing the avenue. */
    public static Spot merchant() { return new Spot(HubLayout.PLAZA_X + 10.5, HubLayout.PLAZA_Z - 7.5, 0.0f); }

    /** Is the spot inside the plaza template's box (the protected square)? */
    public static boolean onPlaza(Spot s) {
        int x = (int) Math.floor(s.x()), z = (int) Math.floor(s.z());
        return HubPlazaLayout.inBox(x, HubLayout.SURFACE_Y, z);
    }

    /** The search box for "is the NPC already there": the whole plaza column, generous in height. */
    public static double[] searchBox() {
        return new double[] {HubPlazaLayout.originX(), HubLayout.SURFACE_Y - 4, HubPlazaLayout.originZ(), HubPlazaLayout.maxX() + 1, HubLayout.SURFACE_Y + 12, HubPlazaLayout.maxZ() + 1};
    }
}
