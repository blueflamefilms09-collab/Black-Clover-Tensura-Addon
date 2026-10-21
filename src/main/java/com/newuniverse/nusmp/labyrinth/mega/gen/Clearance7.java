package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Why: the Mega-Dungeon was drawn for a player who is two blocks tall and one wide, and a doorway of 3 x 4 blocks, a corridor 5 wide and 6 high and a spiral
 * staircase one block wide are exactly that. The owner wants the scale of Tensura (an ogre, a dragonewt, a giant mount, a party of six side by side), where
 * every place a player is carried through (a doorway, a corridor, a hallway, a stair) must be at least 7 blocks wide and 7 blocks high. The templates are
 * static files that other builders drew on the 9-block module grid with walls two layers thick, and a 7-wide passage does not fit between two walls of a
 * 9-wide piece unless the walls give up a layer, so the passages are CUT after a piece is placed instead of redrawing 244 templates: the cut boxes below are
 * computed from the plan alone (which piece, which socket, how it is turned), they never leave the piece's own footprint (so the plan, the loop planner and
 * the dead-end pruner see the same pieces on the same grid and every module still connects), and a rule measures the result.
 * <p>
 * This class is the pure half and holds the whole decision, so JUnit can run it on every real template: {@link #connected} (which sockets are doors that lead
 * somewhere), {@link #cuts} (the boxes that must be free: the doorway through the wall layers and a 7-block apron inside the room, the interior of a corridor
 * or chamber, the stair head room), {@link #carvable} (which blocks may be removed: never a chest, a spawner, a gate or an altar), {@link #measure} and
 * {@link #check} (THE RULE {@code clearance7}: the free width and height of every cut, fails below 7 x 7) and the helpers the game side shares ({@link #needsCap},
 * {@link #lightCells}, {@link #sealCut}). Nothing here knows a Minecraft type.
 * <p>
 * Three transitions are NOT cut on purpose, and the doc lists them: the boss-chain doors (Guardian hall, antechamber, vault: they hold a Gatekeeper door block of
 * their own size), a door that ends in a door cap (an alcove, not a passage) and the spiral staircase's width (a spiral round a newel cannot be 7 wide inside a
 * 9-block shaft; its head room is cut, its width is reported, see {@link #stairWidth}).
 */
public final class Clearance7 {
    private Clearance7() {}

    /** The free width and the free height every transition must have, in blocks. */
    public static final int REQUIRED = 7;
    /** Blocks to each side of the centre line of a 7-wide passage. */
    public static final int HALF = REQUIRED / 2;
    /** How far a doorway cut reaches into the room behind it, beyond the wall layers (the "apron" the room's furniture must keep clear). */
    public static final int APRON = 3;
    /** The widest sideways search of {@link #measure}, so an open cavern does not make a measurement unbounded. */
    private static final int LOOK = 12;

    private static final int MODULE = MegaGrid.MODULE;
    private static final int WALL = MegaGrid.WALL;
    private static final int ROW = MegaGrid.DOOR_ROW;

    /** What kind of transition a cut belongs to. */
    public enum Kind { DOORWAY, CORRIDOR, CHAMBER, STAIR }

    /**
     * One transition: the box (in the same coordinates as the plan) that must be free and 7 x 7 in cross section, the direction a walker goes along it, the piece
     * it belongs to and a short label for messages. For {@link Kind#STAIR} the box is a one-column head-room box above one stretch of steps.
     */
    public record Cut(Kind kind, int piece, Box box, Facing axis, String what) {
        /** The size across the walking line (7 for every cut but a stair column). */
        public int width() { return axis.alongX() ? box.sizeZ() : box.sizeX(); }

        public int height() { return box.sizeY(); }

        /** The length along the walking line. */
        public int length() { return axis.alongX() ? box.sizeX() : box.sizeZ(); }
    }

    /** What a walker can pass through: is the cell free of anything that stops a player? */
    public interface Space {
        boolean free(int x, int y, int z);
    }

    /** The free width (sideways, round the centre line) and free height the narrowest place of a transition has. */
    public record Clearance(int width, int height) {
        public boolean ok() { return width >= REQUIRED && height >= REQUIRED; }

        @Override
        public String toString() { return width + "x" + height; }
    }

    // ---------------------------------------------------------------------------------------------------------------------------- which sockets are doors

    /**
     * Is this socket an ordinary doorway (a {@code mega_door}, or the plain doorway of the Way Onward corridor)? The boss-chain sockets of the Guardian hall and the vault hold
     * a Gatekeeper door block of their own size and are never cut. Whether a socket really leads somewhere is {@link #connected}'s business: a sealed end is never connected.
     */
    public static boolean isCuttableDoor(Socket s) { return s.isDoor() || MegaGrid.ONWARD.equals(s.name()); }

    /**
     * For every piece and socket: does the socket lead into another room (a parent-child join, a loop, a bridge piece's ends)? A socket that ends in a door cap is NOT
     * connected: an alcove is not a passage. Indexed {@code [piece][socket]}.
     */
    public static boolean[][] connected(Result r) {
        List<Placed> ps = r.pieces;
        boolean[][] c = new boolean[ps.size()][];
        for (int i = 0; i < c.length; i++) c[i] = new boolean[ps.get(i).shape.sockets.size()];
        for (Placed p : ps) {
            if (p.shape.isCap()) continue;
            if (p.link) {
                for (int j = 0; j < c[p.index].length; j++) if (isCuttableDoor(p.shape.sockets.get(j)) && !p.shape.sockets.get(j).sealed()) c[p.index][j] = true;
            }
            if (p.ownSocket >= 0 && p.ownSocket < c[p.index].length) c[p.index][p.ownSocket] = true;
            if (p.parent >= 0 && p.parent < c.length && !ps.get(p.parent).shape.isCap() && p.parentSocket >= 0 && p.parentSocket < c[p.parent].length) c[p.parent][p.parentSocket] = true;
        }
        for (Result.Loop l : r.loops) {
            if (l.pieceA() >= 0 && l.pieceA() < c.length && l.socketA() >= 0 && l.socketA() < c[l.pieceA()].length) c[l.pieceA()][l.socketA()] = true;
            if (l.pieceB() >= 0 && l.pieceB() < c.length && l.socketB() >= 0 && l.socketB() < c[l.pieceB()].length) c[l.pieceB()][l.socketB()] = true;
        }
        return c;
    }

    // ---------------------------------------------------------------------------------------------------------------------------- the cuts

    /** Every transition cut of a plan. */
    public static List<Cut> cuts(Result r) {
        boolean[][] conn = connected(r);
        List<Cut> out = new ArrayList<>();
        for (Placed p : r.pieces) out.addAll(cuts(p, conn[p.index]));
        return out;
    }

    /**
     * The cuts of one placed piece, given which of its sockets are connected: a 7 x 7 doorway through the wall layers of each connected door (and, for rooms other than
     * corridors, circulation pieces and stairwells, a further {@link #APRON} blocks into the room), and for a corridor / link / onward piece that is exactly one module
     * wide the whole interior (a 7-wide strip along it, or the 7 x 7 chamber of a bend, tee or cross). Door caps are never cut. The boxes lie inside the piece's own box.
     */
    public static List<Cut> cuts(Placed p, boolean[] connected) {
        List<Cut> out = new ArrayList<>();
        Shape sh = p.shape;
        if (sh.isCap()) return out;
        RoomKind k = sh.kind;
        boolean apron = !(k.isCirculation() || k == RoomKind.STAIRWELL);
        for (int i = 0; i < sh.sockets.size(); i++) {
            Socket s = sh.sockets.get(i);
            if (i >= connected.length || !connected[i] || !isCuttableDoor(s)) continue;
            Facing in = s.facing().opposite(), lat = s.facing().cw();
            Vec cell = s.cell();
            int depth = WALL - 1 + (apron ? APRON : 0);
            Vec a = clamp(sh, cell.step(lat, -HALF));
            Vec b = clamp(sh, cell.step(lat, HALF).step(in, depth).add(0, REQUIRED - 1, 0));
            Facing travel = s.facing();                                                 // out of the room (the line a walker follows is the same)
            Facing world = Rot.facing(p.rot, travel);
            out.add(new Cut(Kind.DOORWAY, p.index, Box.of(p.world(a), p.world(b)), world, "door " + i + " of " + sh.id));
        }
        if (k == RoomKind.ONWARD && sh.sx > MODULE && sh.sz > MODULE) {
            // the bent Way Onward corridors (27 x 27): a lane from every socket (the entrance and the one that ends in the rubble seal) to the far wall's inner face; the two end
            // walls of every lane stay (the seal is in one of them), the lane starts behind the door's own two wall layers
            for (int i = 0; i < sh.sockets.size(); i++) {
                Socket s = sh.sockets.get(i);
                Facing in = s.facing().opposite(), lat = s.facing().cw();
                int ext = in.alongX() ? sh.sx : sh.sz;
                Vec a = clamp(sh, s.cell().step(in, WALL).step(lat, -HALF));
                Vec b = clamp(sh, s.cell().step(in, ext - 1 - WALL).step(lat, HALF).add(0, REQUIRED - 1, 0));
                out.add(new Cut(Kind.CORRIDOR, p.index, Box.of(p.world(a), p.world(b)), Rot.facing(p.rot, in), "corridor lane " + i + " of " + sh.id));
            }
        }
        if (k == RoomKind.CORRIDOR || k == RoomKind.LINK || k == RoomKind.ONWARD) {
            boolean narrowX = sh.sx == MODULE, narrowZ = sh.sz == MODULE;
            if (narrowX && narrowZ) {
                // a bend, tee, cross or waystation: the whole 7 x 7 inside (the layer next to the outer wall goes, the outer layer stays)
                Vec a = new Vec(1, ROW, 1), b = new Vec(MODULE - 2, ROW + REQUIRED - 1, MODULE - 2);
                out.add(new Cut(Kind.CHAMBER, p.index, Box.of(p.world(a), p.world(b)), Rot.facing(p.rot, Facing.NORTH), "chamber of " + sh.id));
            } else if (narrowX || narrowZ) {
                // a straight corridor: a 7-wide strip along it. An Onward keeps both end walls two layers thick (its far end is the rubble seal).
                int end = k == RoomKind.ONWARD ? WALL : 1;
                Vec a, b;
                Facing along;
                if (narrowX) {
                    a = new Vec(1, ROW, end);
                    b = new Vec(MODULE - 2, ROW + REQUIRED - 1, sh.sz - 1 - end);
                    along = Facing.NORTH;
                } else {
                    a = new Vec(end, ROW, 1);
                    b = new Vec(sh.sx - 1 - end, ROW + REQUIRED - 1, MODULE - 2);
                    along = Facing.EAST;
                }
                out.add(new Cut(Kind.CORRIDOR, p.index, Box.of(p.world(a), p.world(b)), Rot.facing(p.rot, along), "corridor " + sh.id));
            }
        }
        return out;
    }

    /** A template-frame cell pulled inside the template, so a cut of a small piece never reaches outside it. */
    private static Vec clamp(Shape sh, Vec v) {
        return new Vec(Math.max(0, Math.min(sh.sx - 1, v.x())), Math.max(0, Math.min(sh.sy - 1, v.y())), Math.max(0, Math.min(sh.sz - 1, v.z())));
    }

    /**
     * The widening of a rubble seal: the 7 x 7 box in the wall layers around the seal cell of a Way Onward (lateral 3 each way, 7 high, {@code depth} layers inward, at
     * least the wall thickness), opened together with the seal itself so the bridge to the next sector is as wide as the rest of the dungeon.
     */
    public static Box sealCut(Vec seal, Facing facing, int depth) {
        Facing in = facing.opposite(), lat = facing.cw();
        int d = Math.max(WALL, Math.min(4, depth)) - 1;
        return Box.of(seal.step(lat, -HALF), seal.step(lat, HALF).step(in, d).add(0, REQUIRED - 1, 0));
    }

    /**
     * The head-room cuts of a staircase: for every stretch of steps (a cell a player stands on) the 7 cells above it must be free. Cells in the same column are merged
     * where their boxes overlap; the stand cells themselves are never part of a cut. One box per merged stretch, one cell wide.
     */
    public static List<Cut> stairCuts(int piece, Collection<Vec> stands, String label) {
        TreeMap<Long, TreeSet<Integer>> cols = new TreeMap<>();
        TreeMap<Long, Vec> where = new TreeMap<>();
        for (Vec v : stands) {
            long key = ((long) v.x() << 32) ^ (v.z() & 0xffffffffL);
            cols.computeIfAbsent(key, x -> new TreeSet<>()).add(v.y());
            where.putIfAbsent(key, v);
        }
        List<Cut> out = new ArrayList<>();
        for (java.util.Map.Entry<Long, TreeSet<Integer>> e : cols.entrySet()) {
            Vec at = where.get(e.getKey());
            int lo = Integer.MIN_VALUE, hi = Integer.MIN_VALUE;
            for (int y : e.getValue()) {
                if (lo == Integer.MIN_VALUE) {
                    lo = y + 1;
                    hi = y + REQUIRED;
                } else if (y + 1 <= hi + 1) {
                    hi = y + REQUIRED;
                } else {
                    out.add(column(piece, at, lo, hi, label));
                    lo = y + 1;
                    hi = y + REQUIRED;
                }
            }
            if (lo != Integer.MIN_VALUE) out.add(column(piece, at, lo, hi, label));
        }
        return out;
    }

    private static Cut column(int piece, Vec at, int lo, int hi, String label) {
        return new Cut(Kind.STAIR, piece, new Box(at.x(), lo, at.z(), at.x(), hi, at.z()), Facing.NORTH, "stair head room of " + label);
    }

    // ---------------------------------------------------------------------------------------------------------------------------- what may be removed

    /** Blocks that belong to a mechanism and are never cut away (the Gatekeeper, an altar, a gate, a sealed archway, a trap tile ...). */
    private static final Set<String> PROTECTED = Set.of("nusmp:gatekeeper_door", "nusmp:peak_altar", "nusmp:relic_pedestal", "nusmp:labyrinth_exit", "nusmp:labyrinth_gate",
            "nusmp:sealed_archway", "nusmp:magic_barrier", "nusmp:ward_rune", "nusmp:spawn_glyph", "nusmp:pitfall_tile", "nusmp:snuffer_glyph", "nusmp:sigil_plate",
            "nusmp:riddle_stone", "nusmp:guardian_altar", "minecraft:bedrock", "minecraft:barrier", "minecraft:structure_block", "minecraft:jigsaw", "minecraft:spawner",
            "minecraft:trial_spawner", "minecraft:vault", "minecraft:end_portal", "minecraft:end_portal_frame", "minecraft:nether_portal", "minecraft:end_gateway");

    /** Is the block a fluid (it has no collision, so it is not a clearance problem, and removing it would let the neighbours flow in)? */
    public static boolean isFluid(String id) {
        return id.equals("minecraft:water") || id.equals("minecraft:lava") || id.equals("minecraft:bubble_column");
    }

    /** Furniture that happens to be a block entity but holds nothing a player can take (the templates place them empty): a cut may remove them. */
    private static final Set<String> DECOR_ENTITIES = Set.of("minecraft:decorated_pot", "minecraft:chiseled_bookshelf", "minecraft:lectern", "minecraft:furnace", "minecraft:blast_furnace",
            "minecraft:smoker", "minecraft:brewing_stand", "minecraft:bell", "minecraft:campfire", "minecraft:soul_campfire", "minecraft:enchanting_table", "minecraft:jukebox",
            "minecraft:beehive", "minecraft:bee_nest", "minecraft:conduit", "minecraft:daylight_detector");

    /** Is the block entity one that is only decoration (a banner, a sign, a head), which a cut may remove? Containers and mechanisms are kept. */
    private static boolean decorationEntity(String id) {
        return id.endsWith("_banner") || id.endsWith("_sign") || id.endsWith("_hanging_sign") || id.endsWith("_skull") || id.endsWith("_head") || DECOR_ENTITIES.contains(id);
    }

    /**
     * May a cut remove this block? Not air, fluids, the blocks of mechanisms ({@link #PROTECTED}), and not a block entity that holds something (a chest, a barrel, a
     * furnace, a lectern ...): those stay, and the rule then reports the transition honestly if they block it.
     */
    public static boolean carvable(String id, boolean blockEntity) {
        if (isAir(id) || isFluid(id) || PROTECTED.contains(id)) return false;
        if (id.startsWith("nusmp:") && blockEntity) return false;
        return !blockEntity || decorationEntity(id);
    }

    public static boolean isAir(String id) { return id.equals("minecraft:air") || id.equals("minecraft:cave_air") || id.equals("minecraft:void_air"); }

    /** Blocks a player walks through (no collision): the rule counts them as free. */
    public static boolean passable(String id) {
        if (isAir(id) || isFluid(id) || id.equals("minecraft:light") || id.equals("minecraft:structure_void") || id.equals("minecraft:jigsaw") || id.equals("minecraft:structure_block")) return true;
        return id.endsWith("_wall_banner") || id.endsWith("_banner") || id.endsWith("_wall_sign") || id.endsWith("_sign") || id.endsWith("wall_torch") || id.endsWith("_torch")
                || id.equals("minecraft:cobweb") || id.equals("minecraft:vine") || id.equals("minecraft:glow_lichen") || id.equals("minecraft:lever") || id.endsWith("_button")
                || id.equals("minecraft:tripwire") || id.equals("minecraft:tripwire_hook") || id.equals("minecraft:short_grass") || id.equals("minecraft:fern") || id.equals("minecraft:seagrass")
                || id.equals("minecraft:tall_seagrass") || id.equals("minecraft:kelp") || id.equals("minecraft:kelp_plant") || id.equals("minecraft:hanging_roots") || id.equals("minecraft:fire")
                || id.equals("minecraft:soul_fire") || id.equals("minecraft:redstone_wire") || id.equals("minecraft:rail") || id.equals("minecraft:powered_rail");
    }

    /**
     * After a cut, does the column need a roof block above the box? Only when the column had a ceiling inside the box (the highest removed solid block was at least
     * 3 rows up, so it was not furniture) and the cell above the box is air, so the cut would otherwise open the room to the void above it.
     *
     * @param cleared  for the rows of the box, bottom to top: was a solid block removed there
     * @param aboveAir is the cell just above the box air
     */
    public static boolean needsCap(boolean[] cleared, boolean aboveAir) {
        if (!aboveAir) return false;
        int top = -1;
        for (int i = 0; i < cleared.length; i++) if (cleared[i]) top = i;
        return top >= 3;
    }

    /** The cells that get an invisible light block after a corridor or chamber was cut (the hanging lanterns of the old ceiling are gone): the top row, every 4th cell on the centre line. */
    public static List<Vec> lightCells(Cut c) {
        List<Vec> out = new ArrayList<>();
        if (c.kind() != Kind.CORRIDOR && c.kind() != Kind.CHAMBER) return out;
        Box b = c.box();
        int cx = (b.minX() + b.maxX()) / 2, cz = (b.minZ() + b.maxZ()) / 2;
        if (c.kind() == Kind.CHAMBER) {
            out.add(new Vec(cx, b.maxY(), cz));
            return out;
        }
        if (c.axis().alongX()) for (int x = b.minX() + 2; x <= b.maxX(); x += 4) out.add(new Vec(x, b.maxY(), cz));
        else for (int z = b.minZ() + 2; z <= b.maxZ(); z += 4) out.add(new Vec(cx, b.maxY(), z));
        return out;
    }

    // ---------------------------------------------------------------------------------------------------------------------------- the rule

    /**
     * Measures a transition: along its length, at every station, the number of free columns (free for the full required height) round the centre line, and the free
     * height of the centre column; the answer is the narrowest and lowest station. A stair column is measured by height only (its width is {@link #stairWidth}).
     */
    public static Clearance measure(Space s, Cut c) {
        Box b = c.box();
        int minW = Integer.MAX_VALUE, minH = Integer.MAX_VALUE;
        if (c.kind() == Kind.STAIR) {
            int h = 0;
            while (h < c.height() && s.free(b.minX(), b.minY() + h, b.minZ())) h++;
            return new Clearance(REQUIRED, h);
        }
        boolean alongX = c.axis().alongX();
        int lo = alongX ? b.minX() : b.minZ(), hi = alongX ? b.maxX() : b.maxZ();
        int centre = alongX ? (b.minZ() + b.maxZ()) / 2 : (b.minX() + b.maxX()) / 2;
        for (int st = lo; st <= hi; st++) {
            int h = 0;
            while (h < REQUIRED && free(s, alongX, st, centre, b.minY() + h)) h++;
            int w = 0;
            if (h >= REQUIRED) {
                w = 1;
                for (int d = 1; d <= LOOK && column(s, alongX, st, centre + d, b.minY()); d++) w++;
                for (int d = 1; d <= LOOK && column(s, alongX, st, centre - d, b.minY()); d++) w++;
            }
            minW = Math.min(minW, w);
            minH = Math.min(minH, h);
        }
        return new Clearance(minW == Integer.MAX_VALUE ? 0 : minW, minH == Integer.MAX_VALUE ? 0 : minH);
    }

    private static boolean free(Space s, boolean alongX, int along, int lateral, int y) { return alongX ? s.free(along, y, lateral) : s.free(lateral, y, along); }

    private static boolean column(Space s, boolean alongX, int along, int lateral, int y0) {
        for (int h = 0; h < REQUIRED; h++) if (!free(s, alongX, along, lateral, y0 + h)) return false;
        return true;
    }

    /**
     * THE RULE {@code clearance7}: every cut of the plan (of the pieces {@code piece} accepts) must measure at least 7 wide and 7 high in {@code space}. Returns one
     * line per failing transition, at most {@code max} of them (then a "and N more" line); empty = the rule holds.
     */
    public static List<String> check(Result r, Space space, java.util.function.IntPredicate piece, int max) {
        List<String> bad = new ArrayList<>();
        int more = 0;
        for (Cut c : cuts(r)) {
            if (!piece.test(c.piece())) continue;
            Clearance m = measure(space, c);
            if (m.ok()) continue;
            if (bad.size() < max) bad.add(describe(r, c, m));
            else more++;
        }
        if (more > 0) bad.add("and " + more + " more transitions below 7x7");
        return bad;
    }

    /** One failing transition as a sentence. */
    public static String describe(Result r, Cut c, Clearance m) {
        Placed p = r.pieces.get(c.piece());
        return "piece " + p.index + " (" + p.shape.id + " r" + p.rot + " at " + p.origin + ") " + c.kind().name().toLowerCase(Locale.ROOT) + " " + c.box() + " is " + m + " (needs 7x7)";
    }

    /** The fewest steps' head room below the requirement among the given stand cells (cells whose 7 above are not all free), counted. */
    public static int shortHeadroom(Space s, Collection<Vec> stands) {
        int bad = 0;
        for (Vec v : stands) {
            for (int h = 1; h <= REQUIRED; h++) {
                if (!s.free(v.x(), v.y() + h, v.z())) {
                    bad++;
                    break;
                }
            }
        }
        return bad;
    }

    /**
     * The width a spiral stair has at one step: the number of free cells sideways from the step across the shaft (not through the newel). It is reported, never required:
     * a spiral stair round a core inside a 9-block shaft cannot be 7 wide.
     */
    public static int stairWidth(Space s, Vec stand, Facing across) {
        int w = 1;
        for (int d = 1; d <= LOOK && s.free(stand.x() + across.dx * d, stand.y() + 1, stand.z() + across.dz * d); d++) w++;
        return w;
    }

    // ---------------------------------------------------------------------------------------------------------------------------- the plan's own consistency

    /**
     * Does the cutting keep every module connected? Returns the problems found (empty = fine): every cut lies inside its own piece's box (so no neighbour changes);
     * every doorway cut touches the outside of the piece only at its own door face and every interior cut stays clear of the outer wall layer; the two cuts of a
     * connected door line up (the same centre line, face to face); every ordinary join and loop of the plan has a cut on both sides; and the room graph restricted to
     * the joins that have cuts (plus the boss-chain joins, which are not cut) is still connected, exactly as the plan's graph is.
     */
    public static List<String> connectivity(Result r) {
        List<String> bad = new ArrayList<>();
        List<Cut> all = cuts(r);
        for (Cut c : all) {
            Placed p = r.pieces.get(c.piece());
            if (!insideBox(c.box(), p.box)) bad.add(p + ": the cut " + c.box() + " leaves the piece");
            if (c.kind() != Kind.DOORWAY && c.kind() != Kind.STAIR && touchesOuterLayer(c.box(), p.box)) bad.add(p + ": the interior cut " + c.box() + " reaches the outer wall layer");
        }
        int nonCap = 0;
        for (Placed p : r.pieces) if (!p.shape.isCap()) nonCap++;
        // every ordinary join has a doorway cut on both sides
        for (Placed p : r.pieces) {
            if (p.shape.isCap() || p.parent < 0) continue;
            Placed par = r.pieces.get(p.parent);
            if (par.shape.isCap()) continue;
            boolean ordinary = isCuttableDoor(par.shape.sockets.get(p.parentSocket)) && isCuttableDoor(p.shape.sockets.get(p.ownSocket));
            if (!ordinary) continue;
            Cut a = doorCut(all, par.index, par.socketCell(p.parentSocket), par.socketFacing(p.parentSocket)), b = doorCut(all, p.index, p.socketCell(p.ownSocket), p.socketFacing(p.ownSocket));
            if (a == null || b == null) {
                bad.add("the join " + par + " -> " + p + " has no doorway cut on " + (a == null ? "the parent" : "the child") + " side");
                continue;
            }
            if (!lined(a, b)) bad.add("the cuts of the join " + par + " -> " + p + " do not line up: " + a.box() + " and " + b.box());
        }
        for (Result.Loop l : r.loops) {
            Placed pa = r.pieces.get(l.pieceA()), pb = r.pieces.get(l.pieceB());
            if (!isCuttableDoor(pa.shape.sockets.get(l.socketA())) || !isCuttableDoor(pb.shape.sockets.get(l.socketB()))) continue;
            Cut a = doorCut(all, pa.index, pa.socketCell(l.socketA()), pa.socketFacing(l.socketA())), b = doorCut(all, pb.index, pb.socketCell(l.socketB()), pb.socketFacing(l.socketB()));
            if (a == null || b == null) bad.add("the loop " + pa + " <-> " + pb + " has no doorway cut on " + (a == null ? "the first" : "the second") + " side");
            else if (!lined(a, b)) bad.add("the cuts of the loop " + pa + " <-> " + pb + " do not line up: " + a.box() + " and " + b.box());
        }
        // the graph
        int[] ids = r.roomIds();
        int rooms = 0;
        for (int id : ids) rooms = Math.max(rooms, id + 1);
        List<int[]> edges = new ArrayList<>();
        for (Placed p : r.pieces) {
            if (p.parent < 0 || ids[p.index] < 0 || ids[p.parent] < 0) continue;
            Placed par = r.pieces.get(p.parent);
            boolean cutDoor = isCuttableDoor(par.shape.sockets.get(p.parentSocket)) && isCuttableDoor(p.shape.sockets.get(p.ownSocket));
            if (cutDoor && (doorCut(all, par.index, par.socketCell(p.parentSocket), par.socketFacing(p.parentSocket)) == null)) continue;      // would be a broken join
            edges.add(new int[] {ids[p.parent], ids[p.index]});
        }
        for (Result.Loop l : r.loops) {
            if (ids[l.pieceA()] < 0 || ids[l.pieceB()] < 0) continue;
            Placed pa = r.pieces.get(l.pieceA());
            boolean cutDoor = isCuttableDoor(pa.shape.sockets.get(l.socketA()));
            if (cutDoor && doorCut(all, pa.index, pa.socketCell(l.socketA()), pa.socketFacing(l.socketA())) == null) continue;
            edges.add(new int[] {ids[l.pieceA()], ids[l.pieceB()]});
        }
        if (rooms > 0) {
            int[] dist = WalkGraph.distances(rooms, edges, 0);
            int unreached = 0;
            for (int d : dist) if (d < 0) unreached++;
            if (unreached > 0) bad.add(unreached + " of " + rooms + " rooms cannot be reached through the cut doorways");
        }
        if (nonCap == 0) bad.add("the plan has no room");
        return bad;
    }

    private static Cut doorCut(List<Cut> all, int piece, Vec socketCell, Facing out) {
        for (Cut c : all) {
            if (c.kind() != Kind.DOORWAY || c.piece() != piece) continue;
            Facing lat = out.cw();
            // the cut is centred on the socket cell: the cell lies in the box and is at the centre of the lateral range
            if (!c.box().contains(socketCell)) continue;
            int mid = lat.alongX() ? (c.box().minX() + c.box().maxX()) / 2 : (c.box().minZ() + c.box().maxZ()) / 2;
            int at = lat.alongX() ? socketCell.x() : socketCell.z();
            if (mid == at) return c;
        }
        return null;
    }

    /** Do two doorway cuts of facing pieces stand face to face on the same centre line, touching or overlapping? */
    private static boolean lined(Cut a, Cut b) {
        if (a.axis() != b.axis().opposite()) return false;
        boolean alongX = a.axis().alongX();
        int ma = alongX ? (a.box().minZ() + a.box().maxZ()) / 2 : (a.box().minX() + a.box().maxX()) / 2;
        int mb = alongX ? (b.box().minZ() + b.box().maxZ()) / 2 : (b.box().minX() + b.box().maxX()) / 2;
        if (ma != mb || a.height() != b.height() || a.box().minY() != b.box().minY()) return false;
        return a.box().inflateXZ(1).overlaps(b.box());
    }

    private static boolean insideBox(Box in, Box out) {
        return in.minX() >= out.minX() && in.maxX() <= out.maxX() && in.minY() >= out.minY() && in.maxY() <= out.maxY() && in.minZ() >= out.minZ() && in.maxZ() <= out.maxZ();
    }

    private static boolean touchesOuterLayer(Box in, Box piece) {
        return in.minX() == piece.minX() || in.maxX() == piece.maxX() || in.minZ() == piece.minZ() || in.maxZ() == piece.maxZ();
    }

    /** A self-check of the rule itself on made-up spaces, for the in-game self-test and the unit test: returns the problems (empty = the rule behaves). */
    public static List<String> ruleSelfCheck() {
        List<String> bad = new ArrayList<>();
        Cut door = new Cut(Kind.DOORWAY, 0, new Box(0, 9, 0, 6, 15, 1), Facing.NORTH, "test");
        Space open = (x, y, z) -> x >= 0 && x <= 6 && y >= 9 && y <= 15 && z >= 0 && z <= 1;
        if (!measure(open, door).ok()) bad.add("a free 7x7 doorway does not pass the rule: " + measure(open, door));
        Space narrow = (x, y, z) -> x >= 1 && x <= 5 && y >= 9 && y <= 15 && z >= 0 && z <= 1;
        if (measure(narrow, door).ok()) bad.add("a 5-wide doorway passes the rule");
        Space low = (x, y, z) -> x >= 0 && x <= 6 && y >= 9 && y <= 14 && z >= 0 && z <= 1;
        if (measure(low, door).ok()) bad.add("a 6-high doorway passes the rule");
        Space pillar = (x, y, z) -> open.free(x, y, z) && !(x == 3 && z == 1);
        if (measure(pillar, door).ok()) bad.add("a doorway with a pillar in the middle passes the rule");
        if (carvable("minecraft:chest", true)) bad.add("a chest may be cut");
        if (!carvable("minecraft:deepslate_bricks", false)) bad.add("a wall may not be cut");
        return bad;
    }
}
