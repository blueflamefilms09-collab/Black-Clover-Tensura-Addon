package com.newuniverse.nusmp.dungeon.nexus.zone;

import java.util.List;

/**
 * The slow cycle of one gravity zone of the Underworld Gate (docs/living_dungeon.md section 5, zone 2). Pure maths with no Minecraft
 * imports: a zone is a vertical cylinder, its state is a function of the game time alone, so the server (which applies it) and a test (which
 * checks it) agree without any saved data.
 *
 * <p>Every zone turns through the same kind of round: calm; a four second warning (the ground glows in the colour of what is coming, in the
 * final {@link #WARN} ticks of the calm); a heavy, light or reversed spell; calm again. Whatever happens, the calms between spells last at
 * least four seconds on top of the warning, a spell never lasts longer than 18 seconds, the push it gives is bounded ({@link Push}) and a
 * lift stops short of the top of the zone ({@link #CAP_MARGIN}). Nothing in here can hurt: falling is cushioned by the glue.
 */
public final class GravityCycle {
    private GravityCycle() {}

    /** What the gravity of a zone is doing: nothing, pressing down, lifting, or flipped for a moment. */
    public enum Mode { CALM, HEAVY, LIGHT, REVERSAL }

    /** The three rounds a zone can have: crushing wells, lifting columns, and tides that do both in turn. */
    public enum Kind { WELL, FLOAT, TIDE }

    /** Ticks of warning at the end of a calm that is followed by a spell, ticks a spell takes to rise to full strength and to fall away, the same for a burst. */
    public static final int WARN = 80, RAMP = 30, BURST_RAMP = 10;
    /** A lift stops this many blocks under the top of the zone. */
    public static final double CAP_MARGIN = 3.0;
    /** The most a heavy zone adds to gravity (a share of it) and takes from a jump (a share of its strength). */
    public static final double MAX_GRAVITY_BOOST = 0.65, MAX_JUMP_CUT = 0.30;

    /** One gravity zone: a cylinder (block coordinates of its middle column, a radius, a range of heights), the round it turns through and where in the round it starts. */
    public record Zone(int id, int x, int z, int radius, int yMin, int yMax, Kind kind, int phase) {
        public Zone {
            if (radius < 1) throw new IllegalArgumentException("a zone needs a radius of at least 1");
            if (yMax <= yMin) throw new IllegalArgumentException("a zone needs some height");
            if (kind == null) throw new IllegalArgumentException("a zone needs a kind");
        }

        /** Is this point inside the cylinder (the centre of the zone's middle block is its axis)? */
        public boolean contains(double px, double py, double pz) {
            double dx = px - (x + 0.5), dz = pz - (z + 0.5);
            return dx * dx + dz * dz <= (double) radius * radius && py >= yMin && py <= yMax + 1.0;
        }

        /** The horizontal distance from the axis, in blocks. */
        public double distance(double px, double pz) {
            double dx = px - (x + 0.5), dz = pz - (z + 0.5);
            return Math.sqrt(dx * dx + dz * dz);
        }
    }

    /**
     * What a zone is doing at one moment. {@code warning} is what is about to start while the ground is glowing a warning (CALM when nothing
     * is); {@code elapsed} and {@code remaining} count the ticks of the present stretch; {@code strength} is 0 to 1, rising and falling in a spell.
     */
    public record State(Mode mode, Mode warning, int elapsed, int remaining, double strength) {
        public boolean active() { return mode != Mode.CALM; }

        public boolean telegraphing() { return warning != Mode.CALM; }

        /** 0 as a warning begins, up to 1 as the spell starts. */
        public double warnProgress() { return telegraphing() ? 1.0 - (double) remaining / WARN : 0.0; }
    }

    /**
     * The bounded push a state gives a body inside the zone: a share added to gravity, a share taken from a jump, a Slowness amplifier (-1 for
     * none), a Levitation amplifier (-1 for none; always short of the zone's cap) and whether a slow fall goes with it.
     */
    public record Push(double gravityBoost, double jumpCut, int slowAmp, int levitateAmp, boolean slowFall) {
        public static final Push NONE = new Push(0.0, 0.0, -1, -1, false);

        public boolean any() { return gravityBoost > 0 || jumpCut > 0 || slowAmp >= 0 || levitateAmp >= 0 || slowFall; }
    }

    private record Seg(Mode mode, int ticks) {}

    private static final List<Seg> WELL = List.of(new Seg(Mode.CALM, 440), new Seg(Mode.HEAVY, 300), new Seg(Mode.CALM, 200), new Seg(Mode.REVERSAL, 40));
    private static final List<Seg> FLOAT = List.of(new Seg(Mode.CALM, 400), new Seg(Mode.LIGHT, 360), new Seg(Mode.CALM, 220), new Seg(Mode.REVERSAL, 40));
    private static final List<Seg> TIDE = List.of(new Seg(Mode.CALM, 240), new Seg(Mode.HEAVY, 260), new Seg(Mode.CALM, 160), new Seg(Mode.LIGHT, 280),
            new Seg(Mode.CALM, 160), new Seg(Mode.REVERSAL, 40));

    private static List<Seg> round(Kind kind) {
        return switch (kind) {
            case WELL -> WELL;
            case FLOAT -> FLOAT;
            case TIDE -> TIDE;
        };
    }

    /** The length of the round of a kind of zone, in ticks. */
    public static int period(Kind kind) {
        int sum = 0;
        for (Seg s : round(kind)) sum += s.ticks();
        return sum;
    }

    /** The modes of a kind's round, in order, with the length of each (as "MODE:ticks"), for the tests and the debug command. */
    public static List<String> describe(Kind kind) {
        List<String> out = new java.util.ArrayList<>();
        for (Seg s : round(kind)) out.add(s.mode() + ":" + s.ticks());
        return out;
    }

    // ---------------------------------------------------------------- the state at a time
    /** What the zone is doing at this game time. */
    public static State at(Zone zone, long time) {
        List<Seg> segs = round(zone.kind());
        long t = Math.floorMod(time + (long) zone.phase(), (long) period(zone.kind()));
        long start = 0;
        for (int i = 0; i < segs.size(); i++) {
            Seg s = segs.get(i);
            if (t < start + s.ticks()) {
                int elapsed = (int) (t - start), remaining = s.ticks() - elapsed;
                Mode warning = Mode.CALM;
                if (s.mode() == Mode.CALM) {
                    Mode next = segs.get((i + 1) % segs.size()).mode();
                    if (next != Mode.CALM && remaining <= WARN) warning = next;
                }
                double strength = s.mode() == Mode.CALM ? 0.0 : envelope(elapsed, remaining, s.mode() == Mode.REVERSAL ? BURST_RAMP : RAMP);
                return new State(s.mode(), warning, elapsed, remaining, strength);
            }
            start += s.ticks();
        }
        throw new IllegalStateException("time fell outside the round of " + zone.kind());
    }

    /** 0 to 1: a smooth rise over `ramp` ticks from the start of a spell and a smooth fall over `ramp` ticks to its end. */
    static double envelope(int elapsed, int remaining, int ramp) {
        double x = Math.min(1.0, Math.min(elapsed + 1, remaining) / (double) ramp);
        return x * x * (3.0 - 2.0 * x);
    }

    /** The share of a round a kind spends in a spell (not calm), 0 to 1. */
    public static double activeShare(Kind kind) {
        int active = 0;
        for (Seg s : round(kind)) if (s.mode() != Mode.CALM) active += s.ticks();
        return (double) active / period(kind);
    }

    // ---------------------------------------------------------------- the push
    /** What the state of a zone does to a body whose feet are at height `feetY` inside it. */
    public static Push push(State s, Zone z, double feetY) {
        double k = s.strength();
        boolean room = feetY < z.yMax() - CAP_MARGIN;
        switch (s.mode()) {
            case HEAVY: {
                int slow = k >= 0.75 ? 1 : k >= 0.35 ? 0 : -1;
                return new Push(MAX_GRAVITY_BOOST * k, MAX_JUMP_CUT * k, slow, -1, false);
            }
            case LIGHT: {
                int lift = room && k >= 0.2 ? (k >= 0.7 ? 1 : 0) : -1;
                return new Push(0.0, 0.0, -1, lift, true);
            }
            case REVERSAL: {
                int lift = room && k >= 0.2 ? 3 : -1;
                return new Push(0.0, 0.0, -1, lift, true);
            }
            default:
                return Push.NONE;
        }
    }

    // ---------------------------------------------------------------- how it looks
    /** The colour of the ground glow and the dust (0xRRGGBB): crimson for a press, blue for a lift, violet for a flip; a dim violet while calm. */
    public static int colour(Mode mode) {
        return switch (mode) {
            case HEAVY -> 0xD8321F;
            case LIGHT -> 0x4F9BFF;
            case REVERSAL -> 0xA040FF;
            case CALM -> 0x5A3A8C;
        };
    }
}
