package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Why: a lair action is the HALL acting on its own clock, and the brief's rules for it are strict: it is telegraphed before it lands (never under 0.8 s), it is a bounded share of the boss's own base
 * attack after the player's hit cap, it has an answer (move out of the circle, jump over the ring, stay clear of the line, break the pillar first), it uses only the closed list of effects, and
 * it never pays hunger. This class is the implementation of the eight patterns the lair table can name ({@code DecennialVocab.Pattern}); the table says which pattern a lair action is and every
 * number of it. A lair action becomes one or more PENDING shapes (a circle, a line, a ring, a zone, a pull, a toppling pillar) that draw their warning with the table's particle until they fire.
 * The engine holds no reference to the world between ticks except through the boss it is given, and a boss with no hall (a rift boss, a boss with its arena switched off) still works: the shapes
 * simply have no hazard spots to prefer and no pillar to topple.
 */
final class LairEngine {
    private enum Shape { CIRCLE, LINE, RING, ZONE, PULL, TOPPLE }

    private static final class Pending {
        final DecennialVocab.Lair lair;
        final Shape shape;
        final long start;
        long fireAt, endAt;
        double x, y, z, x2, z2, r, dmg;
        int element = -1;
        boolean fired;
        final Set<UUID> hit = new HashSet<>();

        Pending(DecennialVocab.Lair lair, Shape shape, long start, long fireAt) {
            this.lair = lair;
            this.shape = shape;
            this.start = start;
            this.fireAt = fireAt;
        }
    }

    private final List<Pending> pending = new ArrayList<>();

    int pendingCount() { return pending.size(); }

    void clear() { pending.clear(); }

    // ================================================================ a lair action begins
    /**
     * Starts lair action {@code lair} with the power factor {@code power} (which already carries the phase's damage factor). {@code hall} may be null. Returns how many shapes it laid out (at least one).
     */
    int start(LabyrinthGuardianEntity g, ServerLevel sl, DecennialArenas.Live hall, DecennialVocab.Lair lair, double power, long now, List<Player> players) {
        double dmg = DecennialFx.baseAttack(g) * lair.share() * power;
        long fireAt = now + lair.telegraph();
        int before = pending.size();
        double[] mid = hall != null ? hall.middle() : new double[] {g.getX(), g.getY(), g.getZ()};
        double reach = hall != null ? hall.plan.size / 2.0 - 4.0 : 18.0;
        switch (lair.pattern()) {
            case AT_PLAYERS -> circlesAtPlayers(lair, players, sl, now, fireAt, dmg, lair.count());
            case RANDOM_SPOTS -> {
                List<double[]> spots = new ArrayList<>(hall == null ? List.of() : hall.zoneSpots(lair.zone()));
                java.util.Collections.shuffle(spots, new java.util.Random(sl.random.nextLong()));
                int n = 0;
                for (double[] s : spots) {
                    if (n++ >= lair.count()) break;
                    circle(lair, now, fireAt, s[0], s[1], s[2], lair.radius(), dmg);
                }
                while (n++ < lair.count()) {
                    double a = sl.random.nextDouble() * Math.PI * 2, d = 3.0 + sl.random.nextDouble() * Math.max(2.0, reach - 3.0);
                    circle(lair, now, fireAt, mid[0] + Math.cos(a) * d, mid[1], mid[2] + Math.sin(a) * d, lair.radius(), dmg);
                }
            }
            case SPOTS -> {
                List<double[]> spots = new ArrayList<>(hall == null ? List.of() : hall.zoneSpots(lair.zone()));
                if (spots.isEmpty()) {
                    circlesAtPlayers(lair, players, sl, now, fireAt, dmg, lair.count());
                } else {
                    java.util.Collections.shuffle(spots, new java.util.Random(sl.random.nextLong()));
                    for (int i = 0; i < Math.min(lair.count(), spots.size()); i++) circle(lair, now, fireAt, spots.get(i)[0], spots.get(i)[1], spots.get(i)[2], Math.max(lair.radius(), 1.6), dmg);
                }
            }
            case LINE_TO_PLAYER -> {
                List<Player> chosen = pick(players, lair.count(), sl);
                for (Player p : chosen) {
                    Pending q = new Pending(lair, Shape.LINE, now, fireAt);
                    q.x = g.getX();
                    q.y = g.getY();
                    q.z = g.getZ();
                    double dx = p.getX() - g.getX(), dz = p.getZ() - g.getZ(), d = Math.max(1.0, Math.hypot(dx, dz));
                    q.x2 = g.getX() + dx / d * 14.0;
                    q.z2 = g.getZ() + dz / d * 14.0;
                    q.r = lair.radius();
                    q.dmg = dmg;
                    pending.add(q);
                }
            }
            case RING_PULSE -> {
                Pending q = new Pending(lair, Shape.RING, now, fireAt);
                q.x = g.getX();
                q.y = g.getY();
                q.z = g.getZ();
                q.r = lair.radius();
                q.endAt = fireAt + 16;
                q.dmg = dmg;
                pending.add(q);
            }
            case ZONE -> {
                List<double[]> spots = new ArrayList<>(hall == null ? List.of() : hall.zoneSpots(lair.zone()));
                java.util.Collections.shuffle(spots, new java.util.Random(sl.random.nextLong()));
                int n = 0;
                for (double[] s : spots) {
                    if (n++ >= lair.count()) break;
                    zone(lair, now, fireAt, s[0], s[1], s[2], dmg);
                }
                if (n < lair.count() && !players.isEmpty()) {
                    for (Player p : pick(players, lair.count() - n, sl)) zone(lair, now, fireAt, p.getX() + (sl.random.nextDouble() - 0.5) * 4.0, p.getY(), p.getZ() + (sl.random.nextDouble() - 0.5) * 4.0, dmg);
                } else if (n == 0) {
                    zone(lair, now, fireAt, mid[0], mid[1], mid[2], dmg);
                }
            }
            case PULL -> {
                Pending q = new Pending(lair, Shape.PULL, now, fireAt);
                q.x = hall != null ? mid[0] : g.getX();
                q.y = hall != null ? mid[1] : g.getY();
                q.z = hall != null ? mid[2] : g.getZ();
                q.r = lair.radius();
                q.endAt = fireAt + 14;
                q.dmg = dmg;
                pending.add(q);
            }
            case TOPPLE -> topple(g, hall, lair, players, sl, now, fireAt, dmg);
        }
        g.noteCast(1);
        Component text = Component.translatableWithFallback("decennial.lair." + lair.id(), lair.text()).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
        for (Player p : players) p.displayClientMessage(text, true);
        return pending.size() - before;
    }

    private static List<Player> pick(List<Player> players, int n, ServerLevel sl) {
        List<Player> pool = new ArrayList<>(players);
        List<Player> out = new ArrayList<>();
        while (!pool.isEmpty() && out.size() < n) out.add(pool.remove(sl.random.nextInt(pool.size())));
        return out;
    }

    private void circlesAtPlayers(DecennialVocab.Lair lair, List<Player> players, ServerLevel sl, long now, long fireAt, double dmg, int n) {
        for (Player p : pick(players, n, sl)) circle(lair, now, fireAt, p.getX(), p.getY(), p.getZ(), lair.radius(), dmg);
    }

    private void circle(DecennialVocab.Lair lair, long now, long fireAt, double x, double y, double z, double r, double dmg) {
        Pending q = new Pending(lair, Shape.CIRCLE, now, fireAt);
        q.x = x;
        q.y = y;
        q.z = z;
        q.r = r;
        q.dmg = dmg;
        pending.add(q);
    }

    private void zone(DecennialVocab.Lair lair, long now, long fireAt, double x, double y, double z, double dmg) {
        Pending q = new Pending(lair, Shape.ZONE, now, fireAt);
        q.x = x;
        q.y = y;
        q.z = z;
        q.r = lair.radius();
        q.endAt = fireAt + Math.max(40, lair.duration());
        q.dmg = dmg;
        pending.add(q);
    }

    private void topple(LabyrinthGuardianEntity g, DecennialArenas.Live hall, DecennialVocab.Lair lair, List<Player> players, ServerLevel sl, long now, long fireAt, double dmg) {
        List<BossArenaStructure.Element> pillars = hall == null ? List.of() : hall.pillars();
        // the pillars nearest a person fall first: the warning is a rectangle on the floor that reaches the party
        List<BossArenaStructure.Element> order = new ArrayList<>(pillars);
        order.sort((a, b) -> Double.compare(nearest(hall, a, players), nearest(hall, b, players)));
        int n = 0;
        for (BossArenaStructure.Element e : order) {
            if (n >= lair.count()) break;
            double[] s = hall.strip(e);
            Pending q = new Pending(lair, Shape.TOPPLE, now, fireAt);
            q.element = e.id;
            q.x = s[0];
            q.z = s[1];
            q.x2 = s[2];
            q.z2 = s[3];
            q.y = hall.frame.y0() + 1.0;
            q.dmg = dmg;
            pending.add(q);
            n++;
        }
        if (n == 0) circlesAtPlayers(lair, players, sl, now, fireAt, dmg, Math.max(1, lair.count()));      // no pillar left standing: the hall drops a circle on the party instead
    }

    private static double nearest(DecennialArenas.Live hall, BossArenaStructure.Element e, List<Player> players) {
        double[] s = hall.strip(e);
        double cx = (s[0] + s[2]) / 2, cz = (s[1] + s[3]) / 2, best = 1e9;
        for (Player p : players) best = Math.min(best, Math.hypot(p.getX() - cx, p.getZ() - cz));
        return best;
    }

    // ================================================================ the tick: warnings, then the blows
    void tick(LabyrinthGuardianEntity g, ServerLevel sl, DecennialArenas.Live hall, long now, List<Player> players) {
        Iterator<Pending> it = pending.iterator();
        while (it.hasNext()) {
            Pending q = it.next();
            ParticleOptions fx = DecennialFx.particle(q.lair.particle());
            if (now < q.fireAt) {
                if ((q.fireAt - now) % 4 == 0) warn(sl, q, fx);
                continue;
            }
            boolean done = switch (q.shape) {
                case CIRCLE -> fireCircle(g, sl, q, players, fx);
                case LINE -> fireLine(g, sl, q, players, fx);
                case RING -> fireRing(g, sl, q, now, players, fx);
                case ZONE -> fireZone(g, sl, q, now, players, fx);
                case PULL -> firePull(g, sl, q, now, players, fx);
                case TOPPLE -> fireTopple(g, sl, hall, q);
            };
            if (done) it.remove();
        }
    }

    private void warn(ServerLevel sl, Pending q, ParticleOptions fx) {
        switch (q.shape) {
            case CIRCLE, ZONE, PULL -> DecennialFx.ring(sl, fx, q.x, q.y + 0.15, q.z, q.r, (int) (q.r * 6));
            case RING -> DecennialFx.ring(sl, fx, q.x, q.y + 0.15, q.z, q.r, 48);
            case LINE -> DecennialFx.line(sl, fx, q.x, q.y + 0.2, q.z, q.x2, q.y + 0.2, q.z2, 28);
            case TOPPLE -> {
                DecennialFx.line(sl, fx, q.x, q.y + 0.2, q.z, q.x2, q.y + 0.2, q.z, 12);
                DecennialFx.line(sl, fx, q.x, q.y + 0.2, q.z2, q.x2, q.y + 0.2, q.z2, 12);
                DecennialFx.line(sl, fx, q.x, q.y + 0.2, q.z, q.x, q.y + 0.2, q.z2, 12);
                DecennialFx.line(sl, fx, q.x2, q.y + 0.2, q.z, q.x2, q.y + 0.2, q.z2, 12);
            }
        }
    }

    private static boolean inCircle(Player p, Pending q, double r) {
        double dx = p.getX() - q.x, dz = p.getZ() - q.z;
        return dx * dx + dz * dz <= r * r && Math.abs(p.getY() - q.y) < 3.5;
    }

    private static boolean magic(DecennialVocab.Lair l) {
        return l.particle().equals("end_rod") || l.particle().equals("electric_spark") || l.particle().equals("witch") || l.particle().equals("enchant") || l.particle().equals("portal");
    }

    private boolean fireCircle(LabyrinthGuardianEntity g, ServerLevel sl, Pending q, List<Player> players, ParticleOptions fx) {
        sl.sendParticles(fx, q.x, q.y + 0.3, q.z, 26, q.r * 0.4, 0.3, q.r * 0.4, 0.04);
        sl.playSound(null, net.minecraft.core.BlockPos.containing(q.x, q.y, q.z), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.7f, 1.2f);
        for (Player p : players) {
            if (!inCircle(p, q, q.r)) continue;
            DecennialFx.hurt(g, p, q.dmg, magic(q.lair));
            DecennialFx.effect(g, sl, p, q.lair.effect(), q.lair.effectTicks(), q.x, q.z);
        }
        return true;
    }

    private boolean fireLine(LabyrinthGuardianEntity g, ServerLevel sl, Pending q, List<Player> players, ParticleOptions fx) {
        DecennialFx.line(sl, fx, q.x, q.y + 1.0, q.z, q.x2, q.y + 1.0, q.z2, 36);
        sl.playSound(null, g.blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 1.0f, 0.8f);
        double vx = q.x2 - q.x, vz = q.z2 - q.z, len2 = Math.max(1e-6, vx * vx + vz * vz);
        for (Player p : players) {
            double t = Math.max(0.0, Math.min(1.0, ((p.getX() - q.x) * vx + (p.getZ() - q.z) * vz) / len2));
            double px = q.x + vx * t, pz = q.z + vz * t;
            if (Math.hypot(p.getX() - px, p.getZ() - pz) > q.r + 0.4 || Math.abs(p.getY() - q.y) > 3.5) continue;
            DecennialFx.hurt(g, p, q.dmg, magic(q.lair));
            DecennialFx.effect(g, sl, p, q.lair.effect(), q.lair.effectTicks(), q.x, q.z);
        }
        return true;
    }

    /** A ring that grows from the boss; a person who is in the air as it passes is not touched (jump it). */
    private boolean fireRing(LabyrinthGuardianEntity g, ServerLevel sl, Pending q, long now, List<Player> players, ParticleOptions fx) {
        double t = Math.min(1.0, (now - q.fireAt) / 16.0), r = q.r * t, prev = q.r * Math.max(0.0, (now - 1 - q.fireAt) / 16.0);
        DecennialFx.ring(sl, fx, q.x, q.y + 0.2, q.z, r, 40);
        if (now == q.fireAt) sl.playSound(null, g.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.2f, 1.2f);
        for (Player p : players) {
            if (q.hit.contains(p.getUUID()) || !p.onGround()) continue;
            double d = Math.hypot(p.getX() - q.x, p.getZ() - q.z);
            if (d < prev - 0.8 || d > r + 0.8) continue;
            q.hit.add(p.getUUID());
            DecennialFx.hurt(g, p, q.dmg, magic(q.lair));
            DecennialFx.effect(g, sl, p, q.lair.effect(), q.lair.effectTicks(), q.x, q.z);
        }
        return now >= q.endAt;
    }

    /** A zone acts once a second for its duration. */
    private boolean fireZone(LabyrinthGuardianEntity g, ServerLevel sl, Pending q, long now, List<Player> players, ParticleOptions fx) {
        if (now % 10 == 0) sl.sendParticles(fx, q.x, q.y + 0.4, q.z, 10, q.r * 0.45, 0.35, q.r * 0.45, 0.01);
        if (now % 20 == 0) {
            for (Player p : players) {
                if (!inCircle(p, q, q.r)) continue;
                DecennialFx.hurt(g, p, q.dmg, magic(q.lair));
                DecennialFx.effect(g, sl, p, q.lair.effect(), Math.max(30, q.lair.effectTicks()), q.x, q.z);
            }
        }
        return now >= q.endAt;
    }

    /** Everyone in reach is dragged towards the point for a moment, and the ones who end up close are struck. */
    private boolean firePull(LabyrinthGuardianEntity g, ServerLevel sl, Pending q, long now, List<Player> players, ParticleOptions fx) {
        if (now % 3 == 0) DecennialFx.ring(sl, fx, q.x, q.y + 0.4, q.z, Math.max(2.0, q.r * (1.0 - (now - q.fireAt) / 14.0)), 24);
        if (now < q.endAt) {
            for (Player p : players) {
                double dx = q.x - p.getX(), dz = q.z - p.getZ(), d = Math.hypot(dx, dz);
                if (d > q.r || d < 1.5) continue;
                p.setDeltaMovement(p.getDeltaMovement().add(dx / d * 0.16, 0.0, dz / d * 0.16));
                p.hurtMarked = true;
            }
            return false;
        }
        sl.playSound(null, net.minecraft.core.BlockPos.containing(q.x, q.y, q.z), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0f, 0.6f);
        for (Player p : players) {
            if (Math.hypot(p.getX() - q.x, p.getZ() - q.z) > 3.8) continue;
            DecennialFx.hurt(g, p, q.dmg, magic(q.lair));
            DecennialFx.effect(g, sl, p, q.lair.effect(), q.lair.effectTicks(), q.x, q.z);
        }
        return true;
    }

    private boolean fireTopple(LabyrinthGuardianEntity g, ServerLevel sl, DecennialArenas.Live hall, Pending q) {
        if (hall == null) return true;
        BossArenaStructure.Element e = hall.element(q.element);
        if (e != null && !hall.site.broken.contains(e.id)) DecennialArenas.topple(sl, hall, e, q.dmg);
        return true;
    }
}
