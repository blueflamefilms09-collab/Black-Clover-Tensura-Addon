package com.newuniverse.nusmp.arc;

import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.perf.PerfBudgets;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Why: the one place the redesigned Goku and Sinbad abilities send particles from (docs/boss_redesign_goku_sinbad.md). Server side only (ordinary particle packets to the players near the point; no client
 * class), every call takes its count from the shared per-tick particle budget ({@code PerfBudgets.particles}) so many players and several effects at once cannot flood the network (rule 11), and no call
 * ever sends more than {@link BossShapes#MAX_POINTS} points. Colours are 0xRRGGBB. Worst case of one call: 32 particle packets, fewer when the budget is spent.
 */
public final class BossFx {
    private BossFx() {}

    private static DustParticleOptions dustOf(int rgb, float size) {
        return new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), size);
    }

    /** How many of {@code wanted} points may go out this tick (0 when the budget is spent). */
    public static int take(ServerLevel sl, int wanted) {
        return PerfBudgets.particles(sl.getGameTime(), Math.max(0, Math.min(BossShapes.MAX_POINTS, wanted)));
    }

    /** The points of a {@link BossShapes} array (x, y, z triples relative to {@code centre}) as dust of one colour; as many as the budget allows. Returns how many were sent. */
    public static int points(ServerLevel sl, Vec3 centre, double[] rel, int rgb, float size) {
        int n = take(sl, rel.length / 3);
        if (n <= 0) return 0;
        DustParticleOptions d = dustOf(rgb, size);
        for (int i = 0; i < n; i++) sl.sendParticles(d, centre.x + rel[i * 3], centre.y + rel[i * 3 + 1], centre.z + rel[i * 3 + 2], 1, 0, 0, 0, 0);
        return n;
    }

    /** The same points, but each one in the colour of {@code palette[i % palette.length]} (a ring of seven djinn colours). */
    public static int pointsMulti(ServerLevel sl, Vec3 centre, double[] rel, int[] palette, float size) {
        int n = take(sl, rel.length / 3);
        if (n <= 0 || palette.length == 0) return 0;
        for (int i = 0; i < n; i++)
            sl.sendParticles(dustOf(palette[i % palette.length], size), centre.x + rel[i * 3], centre.y + rel[i * 3 + 1], centre.z + rel[i * 3 + 2], 1, 0, 0, 0, 0);
        return n;
    }

    /** One dust mote. */
    public static void dust(ServerLevel sl, Vec3 p, int rgb, float size) {
        if (take(sl, 1) > 0) sl.sendParticles(dustOf(rgb, size), p.x, p.y, p.z, 1, 0, 0, 0, 0);
    }

    /** A column of {@code n} dust motes (at most 12) standing on {@code base}. */
    public static void column(ServerLevel sl, Vec3 base, double height, int n, int rgb, float size) {
        int m = take(sl, Math.min(12, n));
        if (m <= 0) return;
        DustParticleOptions d = dustOf(rgb, size);
        for (int i = 0; i < m; i++) sl.sendParticles(d, base.x, base.y + height * i / Math.max(1, m - 1), base.z, 1, 0.08, 0, 0.08, 0);
    }

    /** A small spray of the theme's own particle (an element's spark): one packet, at most 6 particles, drawn from the same budget. */
    public static void spark(ServerLevel sl, FxOld old, Vec3 p, int count, double spread, double speed) {
        int n = take(sl, Math.min(6, count));
        if (n > 0) Fx.spray(sl, FxThemes.WORLD, old, p.x, p.y, p.z, n, spread, spread, spread, speed);
    }
}
