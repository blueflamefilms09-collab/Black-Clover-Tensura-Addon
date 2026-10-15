package com.newuniverse.nusmp.particle.rukh;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Why: the owner's rule is that relics, equipment and active abilities spawn NO vanilla particles, only Rukh, and that the density is
 * capped (bursts of 8-12, 4-6 per tick while channelling). One static facade enforces both: every method clamps its count (12 / 6 / 12 / 2),
 * and a per-level per-tick budget ({@link RukhBudget}, default 96) drops the excess, so a hundred relic wearers cannot flood the packet
 * stream however the call sites are written. It goes through {@code ServerLevel.sendParticles}, i.e. the vanilla particle packet: the client
 * needs no custom networking and the particle classes stay purely visual. Directed motion uses {@code count = 0}, where the packet's
 * delta is the velocity (vanilla semantics), one packet per particle; that is affordable because the caps are small. Everything is a
 * no-op on bad input so an ability can call it blindly.
 */
public final class RukhFx {
    private RukhFx() {}

    /** Light = the white-gold Rukh, Dark = the black-violet Rukh. */
    public enum Alignment {
        LIGHT, DARK;

        public static Alignment of(boolean dark) {
            return dark ? DARK : LIGHT;
        }

        public net.minecraft.core.particles.SimpleParticleType type() {
            return RukhParticles.of(this);
        }
    }

    private static final Map<ServerLevel, RukhBudget> BUDGETS = new WeakHashMap<>();
    private static volatile int cap = RukhBudget.DEFAULT_CAP;

    /** Changes the per-level per-tick budget (tests, tuning). */
    public static void setBudgetCap(int newCap) {
        cap = Math.max(0, newCap);
        synchronized (BUDGETS) {
            for (RukhBudget b : BUDGETS.values()) b.setCap(cap);
        }
    }

    private static int grant(ServerLevel l, int want) {
        RukhBudget b;
        synchronized (BUDGETS) {
            b = BUDGETS.computeIfAbsent(l, k -> new RukhBudget(cap));
        }
        return b.take(l.getGameTime(), want);
    }

    private static boolean ok(ServerLevel l, Alignment a) {
        return l != null && a != null;
    }

    private static Vec3 centre(Entity e) {
        return new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ());
    }

    // ---- burst ----
    public static void burst(ServerLevel l, Vec3 pos, Alignment a, int count) {
        burst(l, pos, a, count, 0.35);
    }

    public static void burst(ServerLevel l, double x, double y, double z, Alignment a, int count) {
        burst(l, new Vec3(x, y, z), a, count, 0.35);
    }

    public static void burst(ServerLevel l, Entity at, Alignment a, int count) {
        if (at != null) burst(l, centre(at), a, count, Math.max(0.25, at.getBbWidth() * 0.5));
    }

    public static void burst(ServerLevel l, Vec3 pos, Alignment a, int count, double spread) {
        if (!ok(l, a) || pos == null) return;
        int n = grant(l, RukhMath.clamp(count, RukhMath.MAX_BURST));
        if (n <= 0) return;
        double s = Math.max(0, spread);
        l.sendParticles(a.type(), pos.x, pos.y, pos.z, n, s, s * 0.8, s, 0.03);
    }

    // ---- stream ----
    public static void stream(ServerLevel l, Vec3 from, Vec3 to, Alignment a, int perTick) {
        if (!ok(l, a) || from == null || to == null) return;
        int n = grant(l, RukhMath.clamp(perTick, RukhMath.MAX_STREAM));
        if (n <= 0) return;
        double[] v = RukhMath.toward(to.x - from.x, to.y - from.y, to.z - from.z, RukhMath.FLIGHT_TICKS);
        var type = a.type();
        for (int i = 0; i < n; i++) {
            double j = l.getRandom().nextDouble() * 0.6;   // start a little along the way so the stream is not one clump
            l.sendParticles(type, from.x + (to.x - from.x) * j * 0.3 + (l.getRandom().nextDouble() - 0.5) * 0.3,
                    from.y + (to.y - from.y) * j * 0.3 + (l.getRandom().nextDouble() - 0.5) * 0.3,
                    from.z + (to.z - from.z) * j * 0.3 + (l.getRandom().nextDouble() - 0.5) * 0.3, 0, v[0], v[1], v[2], 1.0);
        }
    }

    public static void stream(ServerLevel l, Entity from, Entity to, Alignment a, int perTick) {
        if (from != null && to != null) stream(l, centre(from), centre(to), a, perTick);
    }

    // ---- gather ----
    public static void gather(ServerLevel l, LivingEntity target, Alignment a, int count) {
        if (!ok(l, a) || target == null) return;
        int n = grant(l, RukhMath.clamp(count, RukhMath.MAX_GATHER));
        if (n <= 0) return;
        Vec3 c = centre(target);
        var type = a.type();
        for (int i = 0; i < n; i++) {
            double ang = RukhMath.slotAngle(i, n, l.getRandom().nextDouble());
            double[] r = RukhMath.ring(l.getRandom().nextDouble(), ang);
            double px = c.x + r[0], py = c.y + (l.getRandom().nextDouble() - 0.5) * 1.6, pz = c.z + r[1];
            double[] v = RukhMath.toward(c.x - px, c.y - py, c.z - pz, RukhMath.FLIGHT_TICKS);
            l.sendParticles(type, px, py, pz, 0, v[0], v[1], v[2], 1.0);
        }
    }

    // ---- aura ----
    public static void aura(ServerLevel l, Entity e, Alignment a) {
        if (!ok(l, a) || e == null) return;
        int n = grant(l, RukhMath.MAX_AURA);
        if (n <= 0) return;
        Vec3 c = centre(e);
        l.sendParticles(a.type(), c.x, c.y, c.z, n, e.getBbWidth() * 0.6, e.getBbHeight() * 0.4, e.getBbWidth() * 0.6, 0.01);
    }

    // ---- spray: drop-in for a raw sendParticles ----
    public static void spray(ServerLevel l, Alignment a, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        if (!ok(l, a)) return;
        if (count <= 0) {   // vanilla "count 0" form: a single directed particle (dx,dy,dz = velocity * speed)
            if (grant(l, 1) > 0) l.sendParticles(a.type(), x, y, z, 0, dx, dy, dz, speed);
            return;
        }
        int n = grant(l, RukhMath.clamp(count, RukhMath.MAX_BURST));
        if (n > 0) l.sendParticles(a.type(), x, y, z, n, dx, dy, dz, speed);
    }
}
