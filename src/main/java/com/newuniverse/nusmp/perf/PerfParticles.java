package com.newuniverse.nusmp.perf;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;

/**
 * Why: a projectile's trail, an ambient puff and a rest's sparkle each send a few particles EVERY tick (or every pulse for every player), and a volley of forty projectiles over a crowd is a few
 * hundred particles per tick that every client in range has to receive and spawn. {@code sendParticles} has no limit of its own. This is the one call those continuous emitters use instead: it asks
 * the per-tick particle budget ({@link PerfBudgets#particles}, {@code maxParticlesPerTick} in nusmp-performance-common.toml, 2000 by default, 0 = no limit) how many of the wanted particles may go
 * out in this tick and sends that many (all of them while the budget lasts, fewer or none in a storm). One-shot bursts (a boss move, an impact) are not budgeted: they are rare and are the
 * point of the move.
 */
public final class PerfParticles {
    private PerfParticles() {}

    /** The same arguments as {@code ServerLevel#sendParticles(type, x, y, z, count, dx, dy, dz, speed)}; returns the count that was sent (0 when the budget of the tick is spent). */
    public static <T extends ParticleOptions> int send(ServerLevel level, T type, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        int n = PerfBudgets.particles(level.getGameTime(), count);
        if (n > 0) level.sendParticles(type, x, y, z, n, dx, dy, dz, speed);
        return n;
    }
}
