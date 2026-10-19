package com.newuniverse.nusmp.compat.create;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Why: draws the six traps, once, in the frame described by {@link Blueprint}, and proves a drawing safe ({@link #problems}). Nothing here touches Minecraft or Create, so a
 * plain test can build every family at every length and prove that it never stands a block in the walkway, that a lever sits at both ends, that a band never leaves a
 * corridor without a place to wait, and that the numbers are the ones the rules allow.
 *
 * <p>What the drawings share. The machinery lives INSIDE the two walls (columns x = +-2 and +-3, floor row to knee height), never in the lane, so a corridor stays as
 * wide as it was. A power line runs along a wall: a creative motor, then a clutch (a redstone block beside it, swapped by the runtime, cuts the line: that is the
 * "off" of the trap), then the working parts with shafts, gearboxes and chain drives between them. Every band is a full-width slab that is quiet most of the time, so a
 * player who waits for the sound can always cross. Two levers per end, on the lane's walls at eye height, disarm the trap: any lever up means disarmed.
 */
public final class TrapBlueprints {
    private TrapBlueprints() {}

    /** The working z of the bands for a lane of length {@code n}: odd cells from 3 to n-3, so a free cell lies between two bands and one at each end. */
    public static List<Integer> workZs(int n) {
        List<Integer> out = new ArrayList<>();
        for (int z = 3; z <= n - 3; z += 2) out.add(z);
        return out;
    }

    /** The trigger of a family is drawn by the rules; this builds the blueprint for it. {@code core} is 6..8, {@code headroom} 4..8. */
    public static Blueprint build(TrapFamily family, int core, int headroom, TriggerKind trigger, long seed) {
        int c = Math.max(TrapRules.MIN_CORE, Math.min(TrapRules.MAX_CORE, core));
        int h = Math.max(TrapRules.MIN_HEADROOM, Math.min(TrapRules.MAX_HEADROOM, headroom));
        int n = c + 2;
        SplittableRandom rng = new SplittableRandom(seed ^ (family.ordinal() * 0x9E3779B97F4A7C15L));
        List<Blueprint.Placed> parts = new ArrayList<>();
        List<Blueprint.PowerCell> power = new ArrayList<>();
        List<Blueprint.Band> bands = new ArrayList<>();
        List<int[]> rams = new ArrayList<>();
        Blueprint.Conveyor conveyor = null;

        addLevers(parts, n);
        addTrigger(parts, trigger, n);
        List<Integer> zs = workZs(n);
        boolean reverse = rng.nextBoolean();                                      // the wave of the sequence and the order of the staggering run either way down the lane
        switch (family) {
            case SAW_RUN -> {
                for (int s : new int[] {-1, 1}) {
                    wallLine(parts, power, s, 2, 1, zs, Part.SAW, rng, true, 1);
                    for (int z = 4; z <= n - 3; z += 2) parts.add(new Blueprint.Placed(3 * s, 2, z, Part.COGWHEEL, null, Dir6.FWD, null));          // cogs on the wall behind the saws: dressing
                }
                addBands(bands, family, zs, 2, -1, 1, reverse, 1.0, false);
            }
            case PRESS -> {
                for (int s : new int[] {-1, 1}) {
                    wallLine(parts, power, s, 2, 1, zs, Part.PISTON, rng, true, 1);
                    for (int z : zs) {
                        parts.add(new Blueprint.Placed(3 * s, 1, z, Part.PISTON_POLE, into(s), null, null));
                        parts.add(new Blueprint.Placed(2 * s, 2, z, Part.DEPLOYER, into(s), null, null));
                        parts.add(new Blueprint.Placed(3 * s, 2, z, Part.LARGE_COGWHEEL, null, Dir6.FWD, null));
                    }
                }
                for (int z : zs) for (int s : new int[] {-1, 1}) for (int y = 1; y <= 2; y++) rams.add(new int[] {s, y, z});
                addBands(bands, family, zs, 2, -1, 1, reverse, 1.0, false);
            }
            case DRILL_WALL -> {
                for (int s : new int[] {-1, 1}) tappedLine(parts, power, s, zs, rng);
                addBands(bands, family, zs, 2, -1, 1, reverse, 1.0, false);
            }
            case TREADMILL -> {
                // the belt runs down the middle of the lane from z = 2 to the cell before the press; the motor and clutch feed it from the left wall, below the floor row's top
                int end = n - 4;
                parts.add(new Blueprint.Placed(-2, 0, 2, Part.MOTOR, Dir6.RIGHT, null, null));
                parts.add(new Blueprint.Placed(-1, 0, 2, Part.CLUTCH, null, Dir6.RIGHT, null));
                power.add(new Blueprint.PowerCell(-1, -1, 2, Blueprint.Role.CLUTCH, 0));
                for (int z = 2; z <= end; z++) parts.add(new Blueprint.Placed(0, 0, z, Part.BELT, Dir6.FWD, null, z == 2 ? "start" : z == end ? "end" : "middle"));
                conveyor = new Blueprint.Conveyor(2, end, TrapRules.BELT_STRENGTH);
                int pz = n - 3;                                                     // the press stands at the end of the belt
                List<Integer> pressZ = List.of(pz);
                for (int s : new int[] {-1, 1}) {
                    wallLine(parts, power, s, 2, 1, pressZ, Part.PISTON, rng, true, 1);
                    parts.add(new Blueprint.Placed(3 * s, 1, pz, Part.PISTON_POLE, into(s), null, null));
                    parts.add(new Blueprint.Placed(2 * s, 2, pz, Part.STORAGE_INTERFACE, into(s), null, null));
                    for (int y = 1; y <= 2; y++) rams.add(new int[] {s, y, pz});
                }
                addBands(bands, family, pressZ, 2, -1, 1, false, 1.0, false);
            }
            case POUR -> {
                // spouts hang from the top of the lane under a fluid pipe in the ceiling; each pours one column (a pair of columns per z, the third column free)
                int top = h;
                int[][] columns = {{-1, 0}, {0, 1}, {-1, 1}};
                for (int i = 0; i < zs.size(); i++) {
                    int z = zs.get(i);
                    int[] cols = columns[i % columns.length];
                    for (int x : cols) {
                        parts.add(new Blueprint.Placed(x, top, z, Part.SPOUT, null, null, null));
                        if (x != 0) parts.add(new Blueprint.Placed(x, top + 1, z, Part.PIPE, null, null, null));          // a pipe over every spout, joined to the line down the middle
                        bands.add(new Blueprint.Band(x, x, top - 1, z, z, bandOffset(family, i, zs.size(), reverse), family.period(), family.telegraph(), family.active(),
                                family.kind(), 1.0, -1));
                    }
                }
                for (int z = 1; z <= n - 2; z++) parts.add(new Blueprint.Placed(0, top + 1, z, Part.PIPE, null, null, null));
                parts.add(new Blueprint.Placed(0, top + 1, n - 1, Part.TANK, null, null, null));
                for (int s : new int[] {-1, 1}) {
                    parts.add(new Blueprint.Placed(2 * s, 2, n - 2, Part.WHISTLE, into(s), null, null));
                    parts.add(new Blueprint.Placed(2 * s, 1, n - 2, Part.STEAM_ENGINE, into(s), null, null));
                }
            }
            case SEQUENCE -> {
                for (int s : new int[] {-1, 1}) wallLine(parts, power, s, 2, 1, zs, Part.CHAIN_DRIVE, rng, true, 1);
                for (int i = 0; i < zs.size(); i++) {
                    int z = zs.get(i);
                    for (int s : new int[] {-1, 1}) {
                        parts.add(new Blueprint.Placed(2 * s, 2, z, Part.NIXIE, into(s), null, null));
                        power.add(new Blueprint.PowerCell(3 * s, 2, z, Blueprint.Role.LIGHT, i));
                    }
                }
                for (int s : new int[] {-1, 1}) {
                    parts.add(new Blueprint.Placed(2 * s, 2, 1, Part.LINK, into(s), null, null));
                    parts.add(new Blueprint.Placed(2 * s, 2, n - 2, Part.LINK, into(s), null, null));
                }
                addBands(bands, family, zs, 2, -1, 1, reverse, 1.0, true);
            }
        }
        return new Blueprint(family, trigger, n, h, parts, bands, power, rams, conveyor, seed);
    }

    /** The direction from the wall at side {@code s} (-1 left, +1 right) into the lane. */
    static Dir6 into(int s) { return s < 0 ? Dir6.RIGHT : Dir6.LEFT; }

    private static int bandOffset(TrapFamily f, int index, int count, boolean reverse) {
        int i = reverse ? count - 1 - index : index;
        return Math.max(0, i * f.stagger());
    }

    private static void addBands(List<Blueprint.Band> out, TrapFamily f, List<Integer> zs, int yMax, int xMin, int xMax, boolean reverse, double factor, boolean lights) {
        for (int i = 0; i < zs.size(); i++) {
            int z = zs.get(i);
            out.add(new Blueprint.Band(xMin, xMax, yMax, z, z, bandOffset(f, i, zs.size(), reverse), f.period(), f.telegraph(), f.active(), f.kind(), factor, lights ? i : -1));
        }
    }

    private static void addLevers(List<Blueprint.Placed> parts, int n) {
        for (int z : new int[] {0, n - 1}) {
            parts.add(new Blueprint.Placed(-1, 2, z, Part.LEVER, Dir6.RIGHT, null, null));
            parts.add(new Blueprint.Placed(1, 2, z, Part.LEVER, Dir6.LEFT, null, null));
        }
    }

    private static void addTrigger(List<Blueprint.Placed> parts, TriggerKind trigger, int n) {
        switch (trigger) {
            case PLATE -> {
                parts.add(new Blueprint.Placed(0, 1, 1, Part.PLATE, null, null, null));
                parts.add(new Blueprint.Placed(0, 1, n - 2, Part.PLATE, null, null, null));
            }
            case TRIPWIRE -> {
                for (int z : new int[] {1, n - 2}) {
                    parts.add(new Blueprint.Placed(-1, 1, z, Part.HOOK, Dir6.RIGHT, null, null));
                    parts.add(new Blueprint.Placed(0, 1, z, Part.STRING, null, null, null));
                    parts.add(new Blueprint.Placed(1, 1, z, Part.HOOK, Dir6.LEFT, null, null));
                }
            }
            case PROXIMITY -> { }
        }
    }

    /**
     * A power line along the wall column {@code 2 * s} at height {@code y}: motor, clutch (with its redstone block behind it, column 3), then from the first working z
     * to the last a working part at each work z with a filler between (shaft, gearbox or chain drive). All of it rotates about the lane's own axis.
     */
    private static void wallLine(List<Blueprint.Placed> parts, List<Blueprint.PowerCell> power, int s, int column, int y, List<Integer> zs, Part work, SplittableRandom rng,
                                 boolean clutch, int groupIndex) {
        if (zs.isEmpty()) return;
        int x = column * s, first = zs.get(0), last = zs.get(zs.size() - 1);
        parts.add(new Blueprint.Placed(x, y, first - 2, Part.MOTOR, Dir6.FWD, null, null));
        if (clutch) {
            parts.add(new Blueprint.Placed(x, y, first - 1, Part.CLUTCH, null, Dir6.FWD, null));
            power.add(new Blueprint.PowerCell(3 * s, y, first - 1, Blueprint.Role.CLUTCH, 0));
        } else {
            parts.add(new Blueprint.Placed(x, y, first - 1, Part.SHAFT, null, Dir6.FWD, null));
        }
        Part[] fillers = {Part.SHAFT, Part.GEARBOX, Part.CHAIN_DRIVE};
        for (int z = first; z <= last; z++) {
            if (zs.contains(z)) {
                // a saw or a piston faces into the lane and turns about the lane's axis
                parts.add(new Blueprint.Placed(x, y, z, work, into(s), Dir6.FWD, null));
            } else {
                Part f = fillers[rng.nextInt(fillers.length)];
                Dir6 axis = f == Part.GEARBOX ? Dir6.UP : Dir6.FWD;               // a gearbox is drawn standing, so its four side faces take the line and the next block
                parts.add(new Blueprint.Placed(x, y, z, f, null, axis, null));
            }
        }
    }

    /**
     * The drill wall's line: a line of the second wall column (x = +-3) with a gearbox at every work z, and a drill in the first column in front of each gearbox, facing the
     * lane. Motor, clutch and the clutch's redstone block (UNDER the clutch, in the floor row) as for the other lines.
     */
    private static void tappedLine(List<Blueprint.Placed> parts, List<Blueprint.PowerCell> power, int s, List<Integer> zs, SplittableRandom rng) {
        if (zs.isEmpty()) return;
        int x = 3 * s, first = zs.get(0), last = zs.get(zs.size() - 1);
        parts.add(new Blueprint.Placed(x, 1, first - 2, Part.MOTOR, Dir6.FWD, null, null));
        parts.add(new Blueprint.Placed(x, 1, first - 1, Part.CLUTCH, null, Dir6.FWD, null));
        power.add(new Blueprint.PowerCell(x, 0, first - 1, Blueprint.Role.CLUTCH, 0));
        for (int z = first; z <= last; z++) {
            if (zs.contains(z)) {
                parts.add(new Blueprint.Placed(x, 1, z, Part.GEARBOX, null, Dir6.UP, null));
                parts.add(new Blueprint.Placed(2 * s, 1, z, Part.DRILL, into(s), null, null));
                parts.add(new Blueprint.Placed(2 * s, 2, z, Part.DEPLOYER, into(s), null, null));
            } else {
                parts.add(new Blueprint.Placed(x, 1, z, rng.nextBoolean() ? Part.SHAFT : Part.CHAIN_DRIVE, null, Dir6.FWD, null));
            }
        }
    }

    // ---------------------------------------------------------------- the proof

    /**
     * Everything wrong with a drawing, as sentences (empty = safe). Proves: no two blocks share a cell; no solid part stands in the lane volume (a plate, lever, hook or string
     * may); at most a belt, a motor or a clutch stands in the floor row of the lane; a lever sits at eye height at both ends on both walls; the trigger exists; bands lie inside
     * the lane, keep a free cell between them, never hurt for longer than {@link TrapRules#MAX_DUTY} of a cycle, never hurt in the last
     * {@link TrapRules#MIN_QUIET} ticks of a cycle (a player who waits always has a window), and have a warning of at least {@link TrapRules#MIN_TELEGRAPH} ticks; the belt is
     * slow enough to walk against; rams never fill the middle column; nothing is placed past the walls' depth (columns beyond +-3).
     */
    public static List<String> problems(Blueprint b) {
        List<String> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int n = b.length();
        for (Blueprint.Placed p : b.parts()) {
            if (!seen.add(p.key())) out.add("two parts share the cell " + p.key());
            if (b.inLane(p.dx(), p.dy(), p.dz()) && !p.part().passable()) {
                // the top cell of the lane may hold a spout or a pipe (above head height)
                boolean overhead = p.dy() >= b.headroom() && (p.part() == Part.SPOUT || p.part() == Part.PIPE || p.part() == Part.TANK);
                if (!overhead) out.add(p.part() + " stands in the walkway at " + p.key());
            }
            if (p.part() == Part.SPOUT && p.dy() < b.headroom()) out.add("a spout hangs below the top of the lane at " + p.key());
            if (Math.abs(p.dx()) > 3) out.add(p.part() + " is past the wall depth at " + p.key());
            if (p.dy() < -1 || p.dy() > b.headroom() + 1) out.add(p.part() + " is out of the height range at " + p.key());
            if (p.dz() < 0 || p.dz() > n - 1) out.add(p.part() + " is out of the lane's length at " + p.key());
            if (Math.abs(p.dx()) <= 1 && p.dy() <= 0 && !(p.part() == Part.BELT || p.part() == Part.CLUTCH || p.part() == Part.MOTOR))
                out.add(p.part() + " replaces the lane's floor at " + p.key());
        }
        for (Blueprint.PowerCell c : b.power()) {
            if (!seen.add(c.key())) out.add("a power cell shares the cell " + c.key());
            if (Math.abs(c.dx()) <= 1 && c.dy() >= 1 && c.dy() <= b.headroom()) out.add("a power cell stands in the walkway at " + c.key());
            if (Math.abs(c.dx()) > 3) out.add("a power cell is past the wall depth at " + c.key());
        }
        // levers
        int leftNear = 0, rightNear = 0, leftFar = 0, rightFar = 0;
        for (Blueprint.Placed l : b.levers()) {
            if (l.dy() < 1 || l.dy() > 3) out.add("a lever is not within reach at " + l.key());
            if (l.dz() == 0) { if (l.dx() < 0) leftNear++; else rightNear++; }
            if (l.dz() == n - 1) { if (l.dx() < 0) leftFar++; else rightFar++; }
        }
        if (leftNear != 1 || rightNear != 1 || leftFar != 1 || rightFar != 1) out.add("the levers are not one per wall at each end");
        // the trigger
        int plates = b.of(Part.PLATE).size(), strings = b.of(Part.STRING).size();
        switch (b.trigger()) {
            case PLATE -> { if (plates != 2) out.add("a plate trap needs a plate at each end"); }
            case TRIPWIRE -> { if (strings != 2 || b.of(Part.HOOK).size() != 4) out.add("a tripwire trap needs two wires and four hooks"); }
            case PROXIMITY -> { if (plates + strings != 0) out.add("a proximity trap has a plate or a wire"); }
        }
        if (b.bands().isEmpty()) out.add("no band");
        // bands
        List<int[]> zr = new ArrayList<>();
        for (Blueprint.Band k : b.bands()) {
            if (k.xMin() < -1 || k.xMax() > 1 || k.xMin() > k.xMax()) out.add("a band leaves the lane sideways");
            if (k.zMin() < 2 || k.zMax() > n - 3) out.add("a band is too close to an end of the lane: z " + k.zMin() + ".." + k.zMax());
            if (k.yMax() < 1 || k.yMax() > b.headroom()) out.add("a band's height is out of range");
            if (k.active() * 100 > k.period() * TrapRules.MAX_DUTY_PERCENT) out.add("a band hurts for more than " + TrapRules.MAX_DUTY_PERCENT + "% of its cycle");
            if (k.period() - k.telegraph() - k.active() < TrapRules.MIN_QUIET) out.add("a band leaves less than " + TrapRules.MIN_QUIET + " quiet ticks per cycle");
            if (k.telegraph() < TrapRules.MIN_TELEGRAPH) out.add("a band's warning is shorter than " + TrapRules.MIN_TELEGRAPH + " ticks");
            if (k.offset() < 0 || k.offset() >= k.period()) out.add("a band's offset is out of its cycle");
            zr.add(new int[] {k.zMin(), k.zMax(), k.xMin(), k.xMax()});
        }
        for (int i = 0; i < zr.size(); i++) {
            for (int j = i + 1; j < zr.size(); j++) {
                int[] a = zr.get(i), c = zr.get(j);
                boolean sameColumns = !(a[3] < c[2] || c[3] < a[2]);
                boolean near = !(a[1] + 1 < c[0] || c[1] + 1 < a[0]);                   // overlapping or touching along z
                if (sameColumns && near) out.add("two bands touch at z " + a[0] + " and " + c[0]);
            }
        }
        if (b.conveyor() != null) {
            Blueprint.Conveyor v = b.conveyor();
            if (v.strength() > TrapRules.MAX_BELT_STRENGTH || v.strength() <= 0) out.add("the belt is too strong to walk against");
            if (v.zMin() < 1 || v.zMax() > n - 2) out.add("the belt leaves the lane");
        }
        for (int[] r : b.rams()) {
            if (Math.abs(r[0]) != 1) out.add("a ram fills the middle column");
            if (r[1] < 1 || r[1] > 2) out.add("a ram is out of height");
        }
        if (b.parts().size() > TrapRules.MAX_PARTS) out.add("more than " + TrapRules.MAX_PARTS + " parts");
        return out;
    }

    /** The number of distinct Create-using parts (what a server gets on top of the vanilla cells). */
    public static int createPartCount(Blueprint b) {
        int n = 0;
        for (Blueprint.Placed p : b.parts()) if (!p.part().vanilla()) n++;
        return n;
    }
}
