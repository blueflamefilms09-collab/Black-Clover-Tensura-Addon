package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Why: the Mega-Dungeon is built from the same DATA the vanilla Jigsaw system uses (template pools, jigsaw blocks, structure templates) but the
 * vanilla placement LOOP cannot do what this dungeon needs: it cannot keep out of other sectors, cannot promise that the Guardian's hall exists,
 * cannot report loops and unused doorways, cannot be sliced over ticks, and cannot run in a unit test with thousands of seeds. This is the loop,
 * replaced, in pure Java over {@link Shape}s and {@link PoolDef}s (the game-side adapter and the test catalog both feed it), so the whole algorithm of
 * {@code docs/labyrinth/mega.md} section 5 is testable and deterministic: the same {@link Request} gives the same {@link Result} on every machine.
 *
 * <p>The steps, in order, and why each is where it is:
 * <ol>
 * <li><b>Start.</b> The Landing is drawn from the start pool and turned so that one of its doors lands on the start cell (for sector 0 a fixed cell, for the
 *     others the end of the tunnel behind the previous sector's seal). The caller's tunnel pieces, if any, are installed first.</li>
 * <li><b>First fill</b> (breadth first, so rooms grow outward from the Landing like a map being drawn). Each door of a piece, highest selection priority first,
 *     takes the first pool candidate (weighted-random order) that fits: inside the region, overlapping nothing in more than touching, keeping out of every
 *     exclusion. A door that finds nothing stays UNUSED. Door caps are deliberately NOT placed yet, and a full-width band at the far side of the region is kept
 *     free (the reserve): the caps of every door that faces the band, and the strip in front of the seal, can only be decided once the boss chain has been placed.</li>
 * <li><b>Ensure</b> (the Sector Recipe). The boss chain is forced: the arena hangs from an unused door far enough from the Landing, then the antechamber, then
 *     the vault (the pool for the sector's relic count) and the Way Onward (the variant the policy prefers, with its seal facing free space). It is a
 *     backtracking search with an undo journal, so a dead end costs a few placements, not the seed. Then the extra safe room and the minimum numbers of trial
 *     halls and treasure closets are forced the same way.</li>
 * <li><b>Second fill.</b> The reserve is released and every door still unused gets another go, except that nothing may touch the keep-clear strip in front of
 *     the seal: that corridor is where the tunnel to the next sector will run.</li>
 * <li><b>Caps.</b> Every door that still faces free space gets a cap from its pool's fallback (a niche with a lantern, a cell ...): an uncapped doorway would
 *     open onto the void.</li>
 * <li><b>Loops.</b> Two unused doors that face each other across the wall between two touching rooms are a doorway nobody had to build: an edge of the map.</li>
 * </ol>
 * The class is single-use: {@link #assemble} builds one instance per request.
 */
public final class MegaAssembler {
    private enum Mode { FILL, CAPS }

    private static final int MODULE = MegaGrid.MODULE;

    private final Request req;
    private final Recipe rc;
    private final PoolSource src;
    private final Rng rng;
    private final Box region;

    private final ArrayList<Placed> placed = new ArrayList<>();
    private final BoxIndex index = new BoxIndex();
    private final int[] kindCount = new int[RoomKind.values().length];
    private final HashMap<String, Integer> shapeCount = new HashMap<>();
    /** Boxes no room may overlap / no door cap may overlap (the reserved band while the first fill runs; later the keep-clear strip in front of the seal). */
    private final List<Box> roomBlockers = new ArrayList<>();
    private final List<Box> capBlockers = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final HashSet<String> warned = new HashSet<>();
    private final Map<String, Integer> stats = new LinkedHashMap<>();

    /** Number of unused, non-sealed sockets over all placed pieces: the most door caps still to come. */
    private int unresolved;
    private long steps;
    /** The step budget is a runaway guard for the backtracking searches only (the boss chain and the extras); the fills are bounded by their own sockets. */
    private boolean budgeted;
    private boolean overBudget;
    private Box zone;
    private Box strip;
    private Result.Expansion expansion;
    private int landing = -1;
    private double landingCx, landingCz;

    private MegaAssembler(Request req) {
        this.req = req;
        this.rc = req.recipe == null ? new Recipe() : req.recipe;
        this.src = req.source;
        this.region = req.region;
        this.rng = new Rng(Rng.mix(req.seed, 0x4D454741L));
    }

    /** Plans one sector. Never throws for a bad data set: missing pools and shapes become warnings and a failed {@link Result}. */
    public static Result assemble(Request req) {
        if (req == null || req.source == null || req.region == null || req.start == null) throw new IllegalArgumentException("request needs a source, a region and a start");
        return new MegaAssembler(req).run();
    }

    // ================================================================================================================ the run

    private Result run() {
        for (Placed p : req.pre) {
            Placed c = p.copy();
            if (c.index != placed.size()) throw new IllegalArgumentException("tunnel piece indices must be 0..n-1 in order");
            pushRaw(c);
        }
        if (!placeStart()) return fail("no Landing fits at the start cell (pool " + req.start.pool() + ")");
        Placed land = placed.get(landing);
        landingCx = land.box.centerX();
        landingCz = land.box.centerZ();

        if (!req.fallback) {
            reserveZone(land);
            fill(Mode.FILL, Math.max(placed.size() + 1, req.maxPieces - rc.capReserve), false);
            roomBlockers.clear();
            capBlockers.clear();
        }
        budgeted = true;
        if (!ensureChain()) return fail(chainFailure());
        if (!req.fallback) {
            String extra = ensureExtras();
            if (extra != null) return fail(extra);
        }
        budgeted = false;
        if (!req.fallback) fill(Mode.FILL, req.maxPieces, true);
        fill(Mode.CAPS, req.maxPieces, true);
        List<Result.Loop> loops = findLoops();
        return new Result(region, placed, loops, expansion, null, warnings, req.seed, req.fallback, landing, stats);
    }

    private Result fail(String why) {
        return Result.failed(region, why, warnings, req.seed, req.fallback, stats);
    }

    // ================================================================================================================ start

    private boolean placeStart() {
        Request.Start st = req.start;
        PoolDef pool = src.pool(st.pool());
        if (pool == null || pool.isEmpty()) {
            warn("start pool " + st.pool() + " is missing or empty");
            return false;
        }
        WeightedDraw d = new WeightedDraw(pool, true, rng);
        PoolEntry e;
        while ((e = d.next()) != null) {
            Shape sh = src.shape(e.shape());
            if (sh == null) {
                warn("pool " + st.pool() + " names the missing template " + e.shape());
                continue;
            }
            for (int k : order(sh)) {
                Socket ks = sh.sockets.get(k);
                if (!ks.name().equals(st.socketName())) continue;
                int r = Rot.turnsFor(ks.facing(), st.growth().opposite());
                Vec o = st.frontCell().sub(Rot.cell(r, ks.cell()));
                Box box = sh.box(o, r);
                if (!box.inside(region, 0) || hitsExclusion(box) || index.overlapsAny(box)) continue;
                Placed c = new Placed(placed.size(), sh, e.processors(), r, o, 0, st.parent(), st.parentSocket(), k, false);
                push(c);
                landing = c.index;
                return true;
            }
        }
        return false;
    }

    /**
     * Keeps a full-width band at the far side of the region free of the first fill: the boss chain and the Way Onward are built there afterwards. The band is
     * as deep as the chain needs (the recipe's {@code reserveDepth}, or when that is 0 the sum of the largest arena, antechamber and Way Onward measured from
     * the real shapes), because a chain that does not fit in a line cannot be placed however long the search runs.
     */
    private void reserveZone(Placed land) {
        int modules = rc.reserveDepth > 0 ? rc.reserveDepth : autoReserve();
        int depth = modules * MODULE;
        if (depth <= 0) return;
        Facing g = req.start.growth();
        Box z;
        switch (g) {
            case EAST -> z = new Box(region.maxX() - depth + 1, region.minY(), region.minZ(), region.maxX(), region.maxY(), region.maxZ());
            case WEST -> z = new Box(region.minX(), region.minY(), region.minZ(), region.minX() + depth - 1, region.maxY(), region.maxZ());
            case SOUTH -> z = new Box(region.minX(), region.minY(), region.maxZ() - depth + 1, region.maxX(), region.maxY(), region.maxZ());
            default -> z = new Box(region.minX(), region.minY(), region.minZ(), region.maxX(), region.maxY(), region.minZ() + depth - 1);
        }
        if (z.overlaps(land.box)) return;                    // a region too small to reserve anything: let the fill use it all
        zone = z;
        roomBlockers.add(z);
        capBlockers.add(z);
    }

    /** The depth of the band, in modules, that the chain pools need: the longest line arena + antechamber + Way Onward in the worst orientation. */
    private int autoReserve() {
        int onward = extent(MegaGrid.P_ONWARD);
        if (req.onward != null) for (String v : req.onward.variants()) onward = Math.max(onward, extent(v));
        int need = extent(MegaGrid.P_ARENA) + extent(MegaGrid.P_ANTECHAMBER) + onward;
        return Math.max(4, Math.min(need, MegaGrid.REGION_MODULES - 6));
    }

    /** The longest edge, in modules, of any template of a pool (0 for a missing pool). */
    private int extent(String poolId) {
        PoolDef pool = src.pool(poolId);
        if (pool == null) return 0;
        int best = 0;
        for (PoolEntry e : pool.entries()) {
            Shape sh = src.shape(e.shape());
            if (sh != null) best = Math.max(best, (Math.max(sh.sx, sh.sz) + MODULE - 1) / MODULE);
        }
        return best;
    }

    // ================================================================================================================ fill and caps

    /**
     * Breadth-first over the doors. {@code everything} starts from every placed piece (second fill, caps) instead of only the Landing (first fill).
     * {@code limit} is the most pieces the plan may hold, counting the door caps still owed to every unused door.
     */
    private void fill(Mode mode, int limit, boolean everything) {
        ArrayDeque<Placed> queue = new ArrayDeque<>();
        if (everything) queue.addAll(placed);
        else queue.add(placed.get(landing));
        while (!queue.isEmpty()) {
            Placed p = queue.poll();
            if (p.shape.isCap()) continue;
            if (p.link && mode == Mode.FILL) continue;
            List<Placed> children = new ArrayList<>();
            for (int si : order(p.shape)) {
                if (p.isUsed(si)) continue;
                Socket s = p.shape.sockets.get(si);
                if (s.sealed()) continue;
                if (mode == Mode.FILL && s.isChain()) continue;
                Placed c = mode == Mode.FILL ? fillSocket(p, si, limit) : capSocket(p, si, limit);
                if (c == null) continue;
                push(c);
                children.add(c);
            }
            children.sort((a, b) -> Integer.compare(p.shape.sockets.get(b.parentSocket).placement(), p.shape.sockets.get(a.parentSocket).placement()));
            queue.addAll(children);
        }
    }

    private Placed fillSocket(Placed p, int si, int limit) {
        if (p.depth >= rc.maxDepth) return null;
        Socket s = p.shape.sockets.get(si);
        PoolDef pool = src.pool(s.pool());
        if (pool == null) {
            warn("door pool " + s.pool() + " does not exist (template " + p.shape.id + ")");
            return null;
        }
        WeightedDraw d = new WeightedDraw(pool, false, rng);
        PoolEntry e;
        while ((e = d.next()) != null) {
            Shape sh = shapeOf(e, pool.id());
            if (sh == null) continue;
            for (int k : order(sh)) {
                Placed c = tryOne(p, si, sh, e, k, limit, null);
                if (c != null) return c;
            }
        }
        return null;
    }

    private Placed capSocket(Placed p, int si, int limit) {
        Socket s = p.shape.sockets.get(si);
        PoolDef own = src.pool(s.pool());
        String fb = own == null ? MegaGrid.P_CAP : own.fallback();
        if (MegaGrid.P_EMPTY.equals(fb)) return null;
        PoolDef pool = src.pool(fb);
        if (pool == null || pool.isEmpty()) return null;
        WeightedDraw d = new WeightedDraw(pool, false, rng);
        PoolEntry e;
        while ((e = d.next()) != null) {
            Shape sh = shapeOf(e, pool.id());
            if (sh == null) continue;
            for (int k : order(sh)) {
                Placed c = tryOne(p, si, sh, e, k, limit, null);
                if (c != null) return c;
            }
        }
        return null;
    }

    // ================================================================================================================ the boss chain

    /** The cost of a Way Onward whose seal has no room beyond it: no real candidate costs this much, so any live continuation beats it. */
    public static final int DEAD_END = 1_000_000;

    private record DoorStart(Placed piece, int socket, boolean inZone, int tie) {}

    /** One complete boss chain found by the search: its four pieces (arena, antechamber, vault, Way Onward), the policy's cost for the seal and the seal itself. */
    private record Chain(List<Placed> pieces, int cost, Box strip, Vec seal, Facing facing) {}

    private Chain bestChain;
    private int leaves;
    private int arenaLeafCap = Integer.MAX_VALUE;
    private boolean stopChain;

    /**
     * Finds the boss chain: a backtracking search over doors for the arena, then its antechamber, the vault and the Way Onward. The search does not stop at the
     * first chain that works: it looks at up to {@code chainLeaves} complete chains (at most two per arena, so they differ in where they stand and which way
     * they face) and keeps the one whose seal the policy rates best, because the first chain found may point at a wall of older sectors while another one a few
     * doors along points into open ground. Every placement of the search is taken back off before the winner is installed.
     */
    private boolean ensureChain() {
        PoolDef arenaPool = src.pool(MegaGrid.P_ARENA);
        if (arenaPool == null || arenaPool.isEmpty()) {
            warn("the arena pool " + MegaGrid.P_ARENA + " is missing or empty");
            return false;
        }
        List<DoorStart> starts = arenaStarts();
        bump("chain.doors", starts.size());
        int tried = 0;
        for (DoorStart s : starts) {
            if (tried++ >= rc.chainSockets || overBudget || stopChain) break;
            bump("chain.doorsTried", 1);
            attachEach(s.piece(), s.socket(), arenaPool, req.maxPieces, null, this::completeChain, () -> stopChain || overBudget);
        }
        if (bestChain == null) return false;
        if (bestChain.pieces().get(0).index != placed.size()) throw new IllegalStateException("the chain search did not take its placements back");
        for (Placed p : bestChain.pieces()) push(p);
        Placed onward = bestChain.pieces().get(3);
        Shape sh = onward.shape;
        expansion = new Result.Expansion(onward.index, bestChain.seal(), bestChain.facing(), sh.sealW, sh.sealH, sh.sealD, bestChain.strip(), bestChain.cost());
        strip = bestChain.strip();
        roomBlockers.clear();
        capBlockers.clear();
        roomBlockers.add(strip);
        capBlockers.add(inner(strip, bestChain.facing()));
        bump("chain.leaves", leaves);
        return true;
    }

    /** The doors the arena may hang from: unused ordinary doors far enough from the Landing and deep enough in the tree; those facing the reserved band first, each group shuffled. */
    private List<DoorStart> arenaStarts() {
        List<DoorStart> out = new ArrayList<>();
        for (Placed p : placed) {
            if (p.link || p.shape.isCap()) continue;
            if (!req.fallback && p.depth + 1 < rc.arenaMinDepth) continue;
            for (int si = 0; si < p.shape.sockets.size(); si++) {
                Socket s = p.shape.sockets.get(si);
                if (p.isUsed(si) || s.sealed() || !s.isDoor()) continue;
                Vec f = p.socketFront(si);
                if (!req.fallback && moduleDistance(f.x(), f.z()) < rc.arenaMinDistance) continue;
                out.add(new DoorStart(p, si, zone != null && zone.contains(f), rng.nextInt(1 << 20)));
            }
        }
        out.sort((a, b) -> a.inZone() != b.inZone() ? (a.inZone() ? -1 : 1) : Integer.compare(a.tie(), b.tie()));
        return out;
    }

    /** The arena is placed: its chain door must lead to an antechamber that can hold the vault and the Way Onward. Always false: the chains it finds are recorded, not kept. */
    private boolean completeChain(Placed arena) {
        bump("chain.arenaFits", 1);
        arenaLeafCap = leaves + 2;
        for (int ci : arena.shape.socketsNamed(MegaGrid.CHAIN)) {
            if (arena.isUsed(ci)) continue;
            PoolDef pool = poolFor(arena.shape.sockets.get(ci), MegaGrid.P_ANTECHAMBER);
            if (pool == null) continue;
            attachEach(arena, ci, pool, req.maxPieces, null, this::completeAntechamber, () -> stopChain || overBudget || leaves >= arenaLeafCap);
        }
        arenaLeafCap = Integer.MAX_VALUE;
        return false;
    }

    private boolean completeAntechamber(Placed ante) {
        bump("chain.anteFits", 1);
        List<Integer> vaults = ante.shape.socketsNamed(MegaGrid.VAULT);
        List<Integer> onwards = ante.shape.socketsNamed(MegaGrid.ONWARD);
        if (vaults.isEmpty() || onwards.isEmpty()) {
            warn("antechamber " + ante.shape.id + " lacks a " + (vaults.isEmpty() ? "mega_vault" : "mega_onward") + " socket");
            return false;
        }
        int vi = vaults.get(0), oi = onwards.get(0);
        PoolDef vaultPool = poolFor(ante.shape.sockets.get(vi), MegaGrid.P_VAULT);
        if (vaultPool == null) return false;
        return attachEach(ante, vi, vaultPool, req.maxPieces, null, v -> {
            bump("chain.vaultFits", 1);
            int peds = v.shape.countMarkers(MarkerKind.PEDESTAL);
            if (peds != req.relicCount) {
                bump("chain.vaultWrongRelics", 1);
                warn("vault " + v.shape.id + " has " + peds + " pedestal markers but this sector needs " + req.relicCount);
                return false;
            }
            return placeOnward(ante, oi);
        }, () -> stopChain || overBudget || leaves >= arenaLeafCap);
    }

    private record OnwardChoice(Placed piece, int cost, Box strip, Vec seal, Facing facing) {}

    /**
     * The leaf of the chain search: picks the best Way Onward for the antechamber on the stack (the first fitting candidate of each variant whose seal faces
     * free space; the policy's lowest cost wins) and records the whole chain if it beats the best so far. Returns false so the search takes the chain back off.
     */
    private boolean placeOnward(Placed ante, int oi) {
        List<String> variants = new ArrayList<>();
        if (req.onward != null) variants.addAll(req.onward.variants());
        if (variants.isEmpty()) variants.add(ante.shape.sockets.get(oi).pool());
        OnwardChoice best = null;
        for (String vid : variants) {
            PoolDef pool = src.pool(vid);
            if (pool == null || pool.isEmpty()) continue;
            OnwardChoice c = firstOnward(ante, oi, pool);
            if (c != null && (best == null || c.cost() < best.cost())) best = c;
            if (overBudget) break;
        }
        if (best == null) {
            bump("chain.onwardNone", 1);
            return false;
        }
        leaves++;
        int n = placed.size();
        Chain c = new Chain(List.of(placed.get(n - 3), placed.get(n - 2), placed.get(n - 1), best.piece()), best.cost(), best.strip(), best.seal(), best.facing());
        if (bestChain == null || c.cost() < bestChain.cost()) bestChain = c;
        if (leaves >= rc.chainLeaves) stopChain = true;
        return false;
    }

    /** The first Way Onward of a variant that fits and faces free space; a candidate with no room beyond its seal is remembered as a dead end and used only if nothing live is found. */
    private OnwardChoice firstOnward(Placed ante, int oi, PoolDef pool) {
        OnwardChoice deadEnd = null;
        WeightedDraw d = new WeightedDraw(pool, true, rng);
        PoolEntry e;
        while ((e = d.next()) != null) {
            Shape sh = shapeOf(e, pool.id());
            if (sh == null) continue;
            if (sh.expansionSocket < 0) {
                warn("Way Onward " + sh.id + " has no readable seal (an EXPANSION_SOCKET marker above a socket)");
                continue;
            }
            for (int k : order(sh)) {
                if (k == sh.expansionSocket) continue;
                Placed c = tryOne(ante, oi, sh, e, k, req.maxPieces, null);
                if (c == null) continue;
                bump("chain.onwardFits", 1);
                Vec seal = c.socketCell(sh.expansionSocket);
                Facing f = c.socketFacing(sh.expansionSocket);
                Box st = stripFor(seal, f);
                if (index.overlapsAny(st) || hitsExclusion(st)) {
                    bump("chain.onwardBlocked", 1);
                    continue;
                }
                int cost = req.onward == null ? 0 : req.onward.cost(seal, f);
                if (cost < 0) {
                    bump("chain.onwardNoRoom", 1);
                    if (deadEnd == null) deadEnd = new OnwardChoice(c, DEAD_END, st, seal, f);
                    continue;
                }
                return new OnwardChoice(c, cost, st, seal, f);
            }
            if (overBudget) return deadEnd;
        }
        return deadEnd;
    }

    /** The keep-clear corridor in front of a seal: one module wide, from the cell in front of the seal out past the region edge and the reach of a door cap. */
    private Box stripFor(Vec seal, Facing f) {
        Vec front = seal.step(f);
        int half = MODULE / 2;
        int y1 = region.minY(), y2 = region.maxY();
        int margin = req.capMargin;
        return switch (f) {
            case EAST -> new Box(front.x(), y1, seal.z() - half, Math.max(front.x(), region.maxX() + margin), y2, seal.z() + half);
            case WEST -> new Box(Math.min(front.x(), region.minX() - margin), y1, seal.z() - half, front.x(), y2, seal.z() + half);
            case SOUTH -> new Box(seal.x() - half, y1, front.z(), seal.x() + half, y2, Math.max(front.z(), region.maxZ() + margin));
            case NORTH -> new Box(seal.x() - half, y1, Math.min(front.z(), region.minZ() - margin), seal.x() + half, y2, front.z());
        };
    }

    /**
     * The part of the keep-clear strip that door caps must stay out of: the strip without its two outermost columns on each side. A cap that enters only those
     * columns is at most 2 deep, and the tunnel that is built through the strip later has walls 2 thick there, so it overwrites such a cap completely; a deeper
     * cap would stand inside the tunnel. This is what lets a doorway beside the strip have a cap instead of being a hole.
     */
    static Box inner(Box strip, Facing facing) {
        int w = MegaGrid.WALL;
        return facing.alongX()
                ? new Box(strip.minX(), strip.minY(), strip.minZ() + w, strip.maxX(), strip.maxY(), strip.maxZ() - w)
                : new Box(strip.minX() + w, strip.minY(), strip.minZ(), strip.maxX() - w, strip.maxY(), strip.maxZ());
    }

    private String chainFailure() {
        StringBuilder b = new StringBuilder("no boss chain: ");
        boolean first = true;
        for (Map.Entry<String, Integer> e : stats.entrySet()) {
            if (!e.getKey().startsWith("chain.")) continue;
            if (!first) b.append(", ");
            b.append(e.getKey().substring(6)).append('=').append(e.getValue());
            first = false;
        }
        if (overBudget) b.append(" (step budget exhausted)");
        return b.toString();
    }

    // ================================================================================================================ extras

    /** Forces the extra safe room and the minimum trial / treasure rooms; returns a failure message or null. */
    private String ensureExtras() {
        int haveSafe = 0;
        for (Placed p : placed) if (p.shape.kind == RoomKind.SAFE && inSafeRange(p.box)) haveSafe++;
        for (int i = haveSafe; i < rc.extraSafe; i++) {
            if (!placeExtra(MegaGrid.P_SAFE, RoomKind.SAFE, box -> inSafeRange(box))) return "no extra safe room fits " + rc.safeMinDistance + ".." + rc.safeMaxDistance + " modules from the Landing";
        }
        for (RoomKind k : new RoomKind[] {RoomKind.TRIAL, RoomKind.TREASURE}) {
            for (int i = kindCount[k.ordinal()]; i < rc.min(k); i++) {
                if (!placeExtra(null, k, null)) return "no " + k.folder() + " room fits (the recipe wants " + rc.min(k) + ")";
            }
        }
        return null;
    }

    private boolean inSafeRange(Box box) {
        double d = moduleDistance(box.centerX(), box.centerZ());
        return d >= rc.safeMinDistance && d <= rc.safeMaxDistance;
    }

    /**
     * Hangs one room of {@code kind} on some unused door. {@code poolId} forces the pool (null: the door's own pool, filtered to the kind); {@code where}
     * restricts where the new room's box may stand.
     */
    private boolean placeExtra(String poolId, RoomKind kind, Predicate<Box> where) {
        List<int[]> doors = new ArrayList<>();
        for (Placed p : placed) {
            if (p.link || p.shape.isCap()) continue;
            for (int si = 0; si < p.shape.sockets.size(); si++) {
                Socket s = p.shape.sockets.get(si);
                if (p.isUsed(si) || s.sealed() || !s.isDoor()) continue;
                doors.add(new int[] {p.index, si});
            }
        }
        rng.shuffle(doors);
        PoolDef forced = poolId == null ? null : src.pool(poolId);
        if (poolId != null && (forced == null || forced.isEmpty())) {
            warn("pool " + poolId + " is missing or empty");
            return false;
        }
        for (int[] ds : doors) {
            if (overBudget) return false;
            Placed p = placed.get(ds[0]);
            int si = ds[1];
            PoolDef pool = forced != null ? forced : src.pool(p.shape.sockets.get(si).pool());
            if (pool == null) continue;
            WeightedDraw d = new WeightedDraw(pool, true, rng);
            PoolEntry e;
            while ((e = d.next()) != null) {
                Shape sh = shapeOf(e, pool.id());
                if (sh == null || sh.kind != kind) continue;
                for (int k : order(sh)) {
                    Placed c = tryOne(p, si, sh, e, k, req.maxPieces, where);
                    if (c == null) continue;
                    push(c);
                    bump("extra." + kind.folder(), 1);
                    return true;
                }
            }
        }
        return false;
    }

    // ================================================================================================================ placement primitives

    /**
     * Places each fitting candidate of {@code pool} on the door {@code (parent, ps)} in turn and keeps the first one for which {@code next} says yes.
     * A candidate that is rejected is taken back off (the undo journal), so a chain search can try the next candidate from the same state; {@code abort}
     * ends the search early (a step budget, or enough chains found).
     */
    private boolean attachEach(Placed parent, int ps, PoolDef pool, int limit, Predicate<Box> where, Predicate<Placed> next, BooleanSupplier abort) {
        WeightedDraw d = new WeightedDraw(pool, true, rng);
        PoolEntry e;
        while ((e = d.next()) != null) {
            Shape sh = shapeOf(e, pool.id());
            if (sh == null) continue;
            for (int k : order(sh)) {
                if (abort.getAsBoolean()) return false;
                Placed c = tryOne(parent, ps, sh, e, k, limit, where);
                if (c == null) continue;
                int mark = placed.size();
                push(c);
                if (next.test(c)) return true;
                rollbackTo(mark);
            }
        }
        return false;
    }

    /** The pool a door draws from: the forced override for its name, else its own, else {@code dflt}. Null when none exists. */
    private PoolDef poolFor(Socket s, String dflt) {
        String forced = req.forced.get(s.name());
        PoolDef pool = forced != null ? src.pool(forced) : null;
        if (pool == null || pool.isEmpty()) pool = src.pool(s.pool());
        if (pool == null || pool.isEmpty()) pool = src.pool(dflt);
        if (pool == null || pool.isEmpty()) {
            warn("no pool for the door " + s.name() + " (tried " + (forced == null ? "" : forced + ", ") + s.pool() + ", " + dflt + ")");
            return null;
        }
        return pool;
    }

    /** The candidate placement of shape {@code sh} (its door {@code k}) on the door {@code (parent, ps)}, or null when the names or the space do not allow it. */
    private Placed tryOne(Placed parent, int ps, Shape sh, PoolEntry e, int k, int limit, Predicate<Box> where) {
        Socket from = parent.shape.sockets.get(ps);
        Socket to = sh.sockets.get(k);
        if (!from.target().equals(to.name())) return null;
        Facing fp = parent.socketFacing(ps);
        int r = Rot.turnsFor(to.facing(), fp.opposite());
        Vec o = parent.socketFront(ps).sub(Rot.cell(r, to.cell()));
        if (budgeted && ++steps > rc.stepBudget) {
            overBudget = true;
            return null;
        }
        Box box = sh.box(o, r);
        boolean cap = sh.isCap();
        if (!box.inside(region, cap ? req.capMargin : 0)) return null;
        if (hitsExclusion(box)) return null;
        for (Box b : cap ? capBlockers : roomBlockers) if (b.overlaps(box)) return null;
        if (index.overlapsAny(box)) return null;
        if (kindCount[sh.kind.ordinal()] >= rc.max(sh.kind)) return null;
        if (sh.max > 0 && shapeCount.getOrDefault(sh.id, 0) >= sh.max) return null;
        int sm = rc.shapeMax(sh.id);
        if (sm > 0 && shapeCount.getOrDefault(sh.id, 0) >= sm) return null;
        int projected = placed.size() + 1 + (unresolved - 1) + (sh.openSockets() - (to.sealed() ? 0 : 1));
        if (projected > limit) return null;
        if (where != null && !where.test(box)) return null;
        return new Placed(placed.size(), sh, e.processors(), r, o, parent.depth + 1, parent.index, ps, k, false);
    }

    private boolean hitsExclusion(Box box) {
        for (Box ex : req.exclusions) if (ex.overlaps(box)) return true;
        return false;
    }

    private void pushRaw(Placed c) {
        placed.add(c);
        index.add(c.box);
        kindCount[c.shape.kind.ordinal()]++;
        shapeCount.merge(c.shape.id, 1, Integer::sum);
        unresolved += openUnused(c);
    }

    private void push(Placed c) {
        pushRaw(c);
        if (c.parent >= 0) {
            Placed par = placed.get(c.parent);
            if (!par.isUsed(c.parentSocket)) {
                par.markUsed(c.parentSocket, true);
                if (!par.shape.sockets.get(c.parentSocket).sealed()) unresolved--;
            }
        }
    }

    private void rollbackTo(int mark) {
        while (placed.size() > mark) {
            Placed c = placed.remove(placed.size() - 1);
            index.removeLast();
            kindCount[c.shape.kind.ordinal()]--;
            shapeCount.merge(c.shape.id, -1, Integer::sum);
            unresolved -= openUnused(c);
            if (c.parent >= 0 && c.parent < placed.size()) {
                Placed par = placed.get(c.parent);
                if (par.isUsed(c.parentSocket)) {
                    par.markUsed(c.parentSocket, false);
                    if (!par.shape.sockets.get(c.parentSocket).sealed()) unresolved++;
                }
            }
        }
    }

    private static int openUnused(Placed p) {
        int n = 0;
        for (int i = 0; i < p.shape.sockets.size(); i++) if (!p.isUsed(i) && !p.shape.sockets.get(i).sealed()) n++;
        return n;
    }

    // ================================================================================================================ loops

    private List<Result.Loop> findLoops() {
        HashMap<Vec, int[]> open = new HashMap<>();
        for (Placed p : placed) {
            if (p.shape.isCap()) continue;
            for (int si = 0; si < p.shape.sockets.size(); si++) {
                if (p.isUsed(si) || p.shape.sockets.get(si).sealed()) continue;
                open.put(p.socketCell(si), new int[] {p.index, si});
            }
        }
        List<Result.Loop> loops = new ArrayList<>();
        for (Placed p : placed) {
            if (p.shape.isCap()) continue;
            for (int si = 0; si < p.shape.sockets.size(); si++) {
                if (p.isUsed(si) || p.shape.sockets.get(si).sealed()) continue;
                Facing f = p.socketFacing(si);
                int[] o = open.get(p.socketFront(si));
                if (o == null || o[0] <= p.index) continue;
                Placed q = placed.get(o[0]);
                int sj = o[1];
                if (q.socketFacing(sj) != f.opposite() || !q.socketFront(sj).equals(p.socketCell(si))) continue;
                Socket a = p.shape.sockets.get(si), b = q.shape.sockets.get(sj);
                if (!a.target().equals(b.name()) || !b.target().equals(a.name())) continue;
                loops.add(new Result.Loop(p.index, si, q.index, sj, p.socketCell(si), f));
            }
        }
        for (Result.Loop l : loops) {
            placed.get(l.pieceA()).markUsed(l.socketA(), true);
            placed.get(l.pieceB()).markUsed(l.socketB(), true);
        }
        return loops;
    }

    // ================================================================================================================ small helpers

    private double moduleDistance(double x, double z) {
        return Math.hypot(x - landingCx, z - landingCz) / MODULE;
    }

    private Shape shapeOf(PoolEntry e, String poolId) {
        Shape sh = src.shape(e.shape());
        if (sh == null) warn("pool " + poolId + " names the missing template " + e.shape());
        return sh;
    }

    private void warn(String w) {
        if (warned.add(w)) warnings.add(w);
    }

    private void bump(String key, int by) {
        stats.merge(key, by, Integer::sum);
    }

    /** The sockets of a shape in the order to try them: higher selection priority first, equal priorities in random order. */
    private int[] order(Shape sh) {
        int n = sh.sockets.size();
        int[] idx = new int[n];
        long[] key = new long[n];
        for (int i = 0; i < n; i++) {
            idx[i] = i;
            key[i] = ((long) -sh.sockets.get(i).selection() << 32) | rng.nextInt(1 << 30);
        }
        for (int i = 1; i < n; i++) {
            int vi = idx[i];
            long vk = key[i];
            int j = i - 1;
            while (j >= 0 && key[j] > vk) {
                idx[j + 1] = idx[j];
                key[j + 1] = key[j];
                j--;
            }
            idx[j + 1] = vi;
            key[j + 1] = vk;
        }
        return idx;
    }
}
