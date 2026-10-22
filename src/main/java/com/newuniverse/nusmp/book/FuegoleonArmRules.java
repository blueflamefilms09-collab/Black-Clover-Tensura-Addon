package com.newuniverse.nusmp.book;

/**
 * Spirit Dive remake: the rules of Fuegoleon's flaming right arm, pure (unit tested by FuegoleonArmRulesTest). In the series Fuegoleon Vermillion was struck down and the fire spirit Salamander
 * chose him: he has a living flame for a right arm (the official character profile: he "has a flaming right arm"), a molten arm from the shoulder with a fire mane streaming off it. Here the arm
 * belongs to the player whose bound grimoire is Fuegoleon's ({@code CharacterSpells.characterOf} = FUEGOLEON) from the moment the Salamander is theirs:
 * <ul>
 *   <li>the contract is held (the Fire spirit bond: the Spirit Lord skill or the bond kind "Salamander"), or</li>
 *   <li>the small Salamander (or its big form) is out ({@code FireCasting.salamanderOut}), or</li>
 *   <li>the Fire Spirit Dive runs.</li>
 * </ul>
 * and it goes when none of these holds (the spirit was released or the contract is gone), when the grimoire is not Fuegoleon's any more, on death and on logout. The SERVER decides; it sends the
 * state as the {@code Aura.FUEGOLEON_ARM} lease (one small packet to the players that track the wearer, at most every {@link #RENEW_TICKS} ticks); the client only draws it.
 */
public final class FuegoleonArmRules {
    private FuegoleonArmRules() {}

    /** The lease the server sends: the arm stays at most this long without a renewal (so it vanishes by itself if the server stops renewing, e.g. when the wearer disconnects). */
    public static final int LEASE_TICKS = 100;
    /** How often a wearer is checked and the lease renewed (staggered by entity id). */
    public static final int RENEW_TICKS = 40;
    /** The style of the lease: 1 = the arm at rest (the contract is held), 2 = the Salamander is out or the Fire dive runs (the arm burns higher). Other numbers are ignored by the painter. */
    public static final int STYLE = 1, STYLE_LINKED = 2;

    /** Why the arm is on, for the self-test and the debug line. */
    public enum Reason { NONE, CONTRACT, LINK, DIVE }

    /**
     * Whether the arm is on, and why.
     *
     * @param feature   the server switch ({@code spiritDive.fuegoleonArm})
     * @param fuegoleon the bound grimoire is Fuegoleon's
     * @param contract  the Salamander contract is held (the Fire spirit bond)
     * @param linked    the Salamander (small or big) is out
     * @param fireDive  the Fire Spirit Dive runs
     * @param alive     the wearer is alive and not a spectator
     */
    public static Reason reason(boolean feature, boolean fuegoleon, boolean contract, boolean linked, boolean fireDive, boolean alive) {
        if (!feature || !fuegoleon || !alive) return Reason.NONE;
        if (fireDive) return Reason.DIVE;
        if (linked) return Reason.LINK;
        if (contract) return Reason.CONTRACT;
        return Reason.NONE;
    }

    /** The style the server sends for a reason (the change of style is the only thing that restarts the arm's fade-in, and it only happens when the Salamander comes out or goes in). */
    public static int styleOf(Reason r) { return r == Reason.LINK || r == Reason.DIVE ? STYLE_LINKED : STYLE; }

    /** Whether a style number is one the painter draws. */
    public static boolean valid(int style) { return style == STYLE || style == STYLE_LINKED; }

    public static boolean on(boolean feature, boolean fuegoleon, boolean contract, boolean linked, boolean fireDive, boolean alive) {
        return reason(feature, fuegoleon, contract, linked, fireDive, alive) != Reason.NONE;
    }

    /** Whether the bond kind named by the spirit systems ("Salamander", any case) is the Fire spirit. */
    public static boolean salamander(String bondKind) { return bondKind != null && bondKind.equalsIgnoreCase("Salamander"); }

    /** Is it this wearer's turn to be checked (every {@link #RENEW_TICKS} ticks, staggered by entity id so a crowd does not all work in the same tick). */
    public static boolean due(long tick, int entityId) { return Math.floorMod(tick, (long) RENEW_TICKS) == Math.floorMod(entityId, RENEW_TICKS); }

    /** Packets per second the arm can cost per wearer at most (one renewal per check). */
    public static double packetsPerSecond() { return 20.0 / RENEW_TICKS; }

    // ------------------------------------------------------------------ the client's draw rules
    /** What the client shows of the arm: the setting of the viewer (a CLIENT config). */
    public enum View { OFF, FLARE, ALWAYS }

    /**
     * The strength 0..1 the arm is drawn with: nothing when the viewer turned it off; in FLARE mode only while the wearer casts, dives or has the Salamander out; ALWAYS shows it at rest too (a
     * quieter flame). {@code casting} is the cast animation of the wearer, {@code diving} the Fire dive, {@code salamanderOut} the link.
     */
    public static float strength(View view, boolean casting, boolean diving, boolean salamanderOut) {
        if (view == View.OFF) return 0f;
        float active = diving ? 1.0f : casting ? 0.9f : salamanderOut ? 0.6f : 0f;
        if (view == View.FLARE) return active;
        return Math.max(0.55f, active);
    }

    /** The flare factor 0..1 on top of the base strength: a cast or the dive makes the flame leap. */
    public static float flare(boolean casting, boolean diving) { return diving ? 1f : casting ? 0.8f : 0f; }

    /** Sprites the arm draws at most (per paint, both flame sheets and embers): the worst case of one flaming arm. */
    public static final int MAX_SHEETS = 8, MAX_EMBERS = 8, MAX_STRANDS = 4;

    /** The most quads one flaming arm draws near the camera (sheets cross two planes, an ember and a strand segment are one quad). */
    public static int worstQuads() { return MAX_SHEETS * 2 + MAX_EMBERS + MAX_STRANDS * 4 + 2; }
}
