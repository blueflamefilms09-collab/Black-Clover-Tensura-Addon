package com.newuniverse.nusmp.client.particle.rukh;

import com.newuniverse.nusmp.particle.rukh.RukhBudget;
import com.newuniverse.nusmp.particle.rukh.RukhFx.Alignment;
import com.newuniverse.nusmp.particle.rukh.RukhMath;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Why: the client-side twin of {@code RukhFx} for call sites that already run on the client (renderers, layers, client ticks) and used to call
 * {@code level.addParticle}. Same method names, same caps (12 / 6 / 12 / 2) and the same {@link RukhMath} geometry, with its own small budget
 * (64 per tick: the client also receives server-sent Rukh, so it keeps headroom). It only references common classes ({@link Level},
 * the particle types), so it is safe to CALL from common code behind {@code level.isClientSide()}; the client-only particle classes are
 * never touched here, the engine resolves the type through the provider registered in {@code RukhClient}.
 */
public final class RukhClientFx {
    private RukhClientFx() {}

    private static final RukhBudget BUDGET = new RukhBudget(64);
    private static final Random RND = new Random();

    private static int grant(Level l, int want) {
        return BUDGET.take(l.getGameTime(), want);
    }

    private static Vec3 centre(Entity e) {
        return new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ());
    }

    private static double g() {
        return RND.nextGaussian();
    }

    public static void burst(Level l, Vec3 pos, Alignment a, int count) {
        burst(l, pos, a, count, 0.35);
    }

    public static void burst(Level l, double x, double y, double z, Alignment a, int count) {
        burst(l, new Vec3(x, y, z), a, count, 0.35);
    }

    public static void burst(Level l, Entity at, Alignment a, int count) {
        if (at != null) burst(l, centre(at), a, count, Math.max(0.25, at.getBbWidth() * 0.5));
    }

    public static void burst(Level l, Vec3 pos, Alignment a, int count, double spread) {
        if (l == null || a == null || pos == null) return;
        int n = grant(l, RukhMath.clamp(count, RukhMath.MAX_BURST));
        double s = Math.max(0, spread);
        var type = a.type();
        for (int i = 0; i < n; i++) {
            l.addParticle(type, pos.x + g() * s, pos.y + g() * s * 0.8, pos.z + g() * s, g() * 0.03, g() * 0.03, g() * 0.03);
        }
    }

    public static void stream(Level l, Vec3 from, Vec3 to, Alignment a, int perTick) {
        if (l == null || a == null || from == null || to == null) return;
        int n = grant(l, RukhMath.clamp(perTick, RukhMath.MAX_STREAM));
        if (n <= 0) return;
        double[] v = RukhMath.toward(to.x - from.x, to.y - from.y, to.z - from.z, RukhMath.FLIGHT_TICKS);
        var type = a.type();
        for (int i = 0; i < n; i++) {
            double j = RND.nextDouble() * 0.18;
            l.addParticle(type, from.x + (to.x - from.x) * j + (RND.nextDouble() - 0.5) * 0.3, from.y + (to.y - from.y) * j + (RND.nextDouble() - 0.5) * 0.3,
                    from.z + (to.z - from.z) * j + (RND.nextDouble() - 0.5) * 0.3, v[0], v[1], v[2]);
        }
    }

    public static void stream(Level l, Entity from, Entity to, Alignment a, int perTick) {
        if (from != null && to != null) stream(l, centre(from), centre(to), a, perTick);
    }

    public static void gather(Level l, LivingEntity target, Alignment a, int count) {
        if (l == null || a == null || target == null) return;
        int n = grant(l, RukhMath.clamp(count, RukhMath.MAX_GATHER));
        Vec3 c = centre(target);
        var type = a.type();
        for (int i = 0; i < n; i++) {
            double[] r = RukhMath.ring(RND.nextDouble(), RukhMath.slotAngle(i, n, RND.nextDouble()));
            double px = c.x + r[0], py = c.y + (RND.nextDouble() - 0.5) * 1.6, pz = c.z + r[1];
            double[] v = RukhMath.toward(c.x - px, c.y - py, c.z - pz, RukhMath.FLIGHT_TICKS);
            l.addParticle(type, px, py, pz, v[0], v[1], v[2]);
        }
    }

    public static void aura(Level l, Entity e, Alignment a) {
        if (l == null || a == null || e == null) return;
        int n = grant(l, RukhMath.MAX_AURA);
        Vec3 c = centre(e);
        var type = a.type();
        for (int i = 0; i < n; i++) {
            l.addParticle(type, c.x + g() * e.getBbWidth() * 0.4, c.y + g() * e.getBbHeight() * 0.25, c.z + g() * e.getBbWidth() * 0.4, 0, 0.01, 0);
        }
    }

    /** Drop-in for {@code level.addParticle(type, x, y, z, vx, vy, vz)}: one budgeted Rukh with the given velocity. */
    public static void spray(Level l, Alignment a, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        if (l == null || a == null) return;
        int n = grant(l, Math.max(1, RukhMath.clamp(count, RukhMath.MAX_BURST)));
        var type = a.type();
        for (int i = 0; i < n; i++) {
            l.addParticle(type, x, y, z, RukhMath.clampSpeed(dx * speed), RukhMath.clampSpeed(dy * speed), RukhMath.clampSpeed(dz * speed));
        }
    }
}
