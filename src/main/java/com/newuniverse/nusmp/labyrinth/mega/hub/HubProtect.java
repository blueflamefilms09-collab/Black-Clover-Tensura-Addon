package com.newuniverse.nusmp.labyrinth.mega.hub;

import com.newuniverse.nusmp.labyrinth.mega.HubLayout;

/**
 * Why: Floor 0 is a place to BUILD a base, so the Dungeon Guard (which makes every block of a Labyrinth dimension unbreakable) deliberately does not
 * hold it. What must still survive a careless or hostile player is small and fixed: the paved square with the way home (without it nobody can leave
 * the Hub) and the two platforms that carry the Holy and Abyssal gates (without them nobody can start a road). Those are the PROTECTED volumes: the
 * plaza template's box, and a sphere of {@link #GATE_SPHERE_RADIUS} blocks round each polarity gate. Everything else is buildable. The question
 * "is this block protected" is a pure function of three integers and {@link HubLayout}, so the break, place and explosion handlers in {@link HubRules}
 * stay one-liners and the boundaries are unit tested (a box that is one block too small shows up as a missing stair in game and nowhere else).
 * The creative-mode bypass is the CALLER's job: this class answers where, not who.
 */
public final class HubProtect {
    private HubProtect() {}

    /** Radius of the protected sphere round each polarity gate: it covers the gate plane (3 x 4), its frame and the platform step in front of it. */
    public static final int GATE_SPHERE_RADIUS = 8;

    /** Is the cell inside the plaza template's box (the paved square, the way home, the signposts)? */
    public static boolean inPlazaBox(int x, int y, int z) { return HubPlazaLayout.inBox(x, y, z); }

    /** Is the cell within {@link #GATE_SPHERE_RADIUS} blocks (inclusive, spherical) of the centre of the Holy gate or the Abyssal gate? */
    public static boolean nearPolarityGate(int x, int y, int z) {
        return within(x, y, z, HubLayout.HOLY_GATE_X, HubLayout.HOLY_GATE_Y, HubLayout.HOLY_GATE_Z, GATE_SPHERE_RADIUS)
                || within(x, y, z, HubLayout.ABYSS_GATE_X, HubLayout.ABYSS_GATE_Y, HubLayout.ABYSS_GATE_Z, GATE_SPHERE_RADIUS);
    }

    /** Is the cell in any protected volume? */
    public static boolean isProtected(int x, int y, int z) { return inPlazaBox(x, y, z) || nearPolarityGate(x, y, z); }

    /** How far from a protected volume a bucket is refused: water flows 7 blocks over flat ground, so a bucket emptied further out cannot reach the paving. */
    public static final int BUCKET_MARGIN = 8;
    /** How far from a protected volume a flint and steel or a fire charge is refused: fire jumps to flammable neighbours (the awnings, the hedge, the pavilion). */
    public static final int FIRE_MARGIN = 3;

    /**
     * Is the cell within {@code margin} blocks of a protected volume: the plaza box grown by the margin on every side, or a sphere of
     * {@link #GATE_SPHERE_RADIUS} + margin blocks round a polarity gate? This is the question for a bucket or a flame, which do their damage AFTER the
     * click, a few blocks away from where it was made. {@code margin == 0} is {@link #isProtected}.
     */
    public static boolean near(int x, int y, int z, int margin) {
        boolean box = x >= HubPlazaLayout.originX() - margin && x <= HubPlazaLayout.maxX() + margin
                && y >= HubPlazaLayout.originY() - margin && y <= HubPlazaLayout.maxY() + margin
                && z >= HubPlazaLayout.originZ() - margin && z <= HubPlazaLayout.maxZ() + margin;
        return box || within(x, y, z, HubLayout.HOLY_GATE_X, HubLayout.HOLY_GATE_Y, HubLayout.HOLY_GATE_Z, GATE_SPHERE_RADIUS + margin)
                || within(x, y, z, HubLayout.ABYSS_GATE_X, HubLayout.ABYSS_GATE_Y, HubLayout.ABYSS_GATE_Z, GATE_SPHERE_RADIUS + margin);
    }

    /** {@link #refuses} for an action whose effect reaches {@code margin} blocks: refused when the cell is within the margin of a protected volume (everywhere when building is off). */
    public static boolean refusesNear(boolean buildEnabled, boolean protectPlaza, int x, int y, int z, int margin) {
        if (!buildEnabled) return true;
        return protectPlaza && near(x, y, z, margin);
    }

    /**
     * Must a non-creative player be refused a change at this cell? {@code buildEnabled = false} makes the whole dimension read-only; otherwise only
     * the protected volumes are refused, and only while {@code protectPlaza} is on.
     */
    public static boolean refuses(boolean buildEnabled, boolean protectPlaza, int x, int y, int z) {
        if (!buildEnabled) return true;
        return protectPlaza && isProtected(x, y, z);
    }

    /** Ticks between two refusals a player is told about (a held mouse button fires the event again every tick). */
    public static final int MESSAGE_INTERVAL = 40;

    /** Is it time to tell the player again why they were refused? A stamp from the future (a restored backup, a clock that went back) counts as due. */
    public static boolean messageDue(long now, long last) { return now - last >= MESSAGE_INTERVAL || now < last; }

    private static boolean within(int x, int y, int z, int cx, int cy, int cz, int radius) {
        long dx = x - cx, dy = y - cy, dz = z - cz;
        return dx * dx + dy * dy + dz * dz <= (long) radius * radius;
    }
}
