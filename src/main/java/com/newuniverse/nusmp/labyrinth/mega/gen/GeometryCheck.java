package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Why: the whole assembler rests on one assumption that no unit test can prove, because it is about the game and not about the code: that the position and
 * rotation maths of {@link Rot} agree with what Minecraft does when it turns a template and puts it in the world. If they disagreed by a quarter turn every room
 * would be built where the plan says there is none, and the first hint would be a dungeon full of walls in the wrong places. This class asks the game itself,
 * on a plan that has been built into real pieces, and compares. For every piece it checks, WITHOUT placing anything, that vanilla computes the same bounding
 * box, finds the same jigsaw cells pointing the same way and the same data-marker cells as the plan does (so the plan's rotation is vanilla's rotation); and,
 * for a spread of the pieces of a sector that is already built, it reads the world: a sample of the solid blocks of the template must stand at the cells the
 * plan says (so the placement call put the template where the plan expects, with the pivot and the offset it was given), and no jigsaw block may be left at a
 * planned socket cell (so the jigsaw replacement ran). Nothing here changes the world. It is what {@code /multiverse mega verify} and the {@code core.geometry}
 * self-test run, and the first thing to run on a real server.
 */
final class GeometryCheck {
    /** How many solid template blocks are looked up in the world per checked piece. */
    static final int SAMPLES_PER_PIECE = 48;
    /** The share of sampled blocks that must be found non-air (processors may legitimately swap a few for air-like blocks or the wall may be a neighbour's). */
    static final double MIN_FOUND = 0.9;
    private static final int MAX_LISTED = 6;
    private static final Set<String> NOT_SOLID = Set.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:structure_void", "minecraft:jigsaw",
            "minecraft:structure_block", "minecraft:light");

    /** What the check looked at and what it found. */
    record Report(int pieces, int boxes, int jigsaws, int markers, int worldPieces, int sampled, int found, int jigsawsLeft, List<String> failures) {
        boolean ok() { return failures.isEmpty(); }

        /** The one-line answer {@code /multiverse mega verify} prints. */
        String line(int sectorId) {
            if (!ok()) return "verify " + sectorId + " FAIL: " + String.join("; ", failures);
            return "verify " + sectorId + " OK pieces=" + pieces + " boxes=" + boxes + " jigsaws=" + jigsaws + " markers=" + markers
                    + " world=" + found + "/" + sampled + " worldPieces=" + worldPieces;
        }
    }

    private GeometryCheck() {}

    /**
     * Checks the plan against the game. {@code worldPieces} is how many pieces to look up in the world (0 = the plan-only checks, for a sector that is not
     * placed yet); the pieces are chosen to cover every kind of room, a door cap and the tunnel, then spread over the rest.
     */
    static Report run(ServerLevel level, SectorGenerator.Built built, int worldPieces) { return run(level, built, worldPieces, false); }

    /**
     * {@link #run(ServerLevel, SectorGenerator.Built, int)} with the rule {@code clearance7} (1.107.0): when {@code clearance7} is true (the sector was finished with
     * {@code gen.clearance7} on, see {@link GenData.Stat#clear7}) every doorway, corridor, chamber and staircase of the pieces read from the world must measure at least
     * 7 wide and 7 high ({@link Clearance7#check}); a transition below that is a failure line like any other.
     */
    static Report run(ServerLevel level, SectorGenerator.Built built, int worldPieces, boolean clearance7) {
        StructureTemplateManager mgr = level.getStructureManager();
        Result r = built.planned().result();
        List<String> fails = new ArrayList<>();
        int boxes = 0, jigsaws = 0, markers = 0;
        int omitted = 0;
        for (int i = 0; i < r.pieces.size(); i++) {
            Placed p = r.pieces.get(i);
            PoolElementStructurePiece piece = built.pieces().get(i);
            List<String> mine = new ArrayList<>();
            try {
                boxes += box(mgr, p, piece, mine);
                jigsaws += sockets(mgr, p, piece, mine);
                markers += markers(mgr, p, piece, mine);
            } catch (Throwable t) {
                mine.add(label(p) + ": the game could not be asked (" + t + ")");
            }
            for (String m : mine) {
                if (fails.size() < MAX_LISTED) fails.add(m);
                else omitted++;
            }
        }
        for (String m : rebuiltRooms(mgr, r, built)) {                                  // rooms rebuilt: a piece built from a rebuilt room must be a variant of its own shipped template, same size
            if (fails.size() < MAX_LISTED) fails.add(m);
            else omitted++;
        }
        int sampled = 0, found = 0, left = 0, checked = 0;
        final boolean[][] connectedSockets = clearance7 ? Clearance7.connected(r) : null;
        if (worldPieces > 0) {
            for (int i : pick(r, worldPieces)) {
                Placed p = r.pieces.get(i);
                int[] w;
                try {
                    w = world(level, mgr, p, i < built.templates().size() ? built.templates().get(i) : p.shape.id, skipBoxes(clearance7 ? Clearance7.cuts(p, connectedSockets[p.index]) : List.of()));
                } catch (Throwable t) {
                    if (fails.size() < MAX_LISTED) fails.add(label(p) + ": the world could not be read (" + t + ")");
                    else omitted++;
                    continue;
                }
                checked++;
                sampled += w[0];
                found += w[1];
                left += w[2];
                if (w[0] > 0 && w[1] < Math.ceil(w[0] * MIN_FOUND)) {
                    String msg = label(p) + ": only " + w[1] + " of " + w[0] + " sampled solid template blocks stand at the planned cells";
                    if (fails.size() < MAX_LISTED) fails.add(msg);
                    else omitted++;
                }
            }
            if (left > 0) {
                String msg = left + " jigsaw blocks are still standing at planned socket cells (the placement did not replace them, or the cells are wrong)";
                if (fails.size() < MAX_LISTED) fails.add(msg);
                else omitted++;
            }
            if (clearance7) {
                java.util.Set<Integer> read = new HashSet<>(pick(r, worldPieces));
                for (String m : clearance7(level, r, read::contains)) {
                    if (fails.size() < MAX_LISTED) fails.add(m);
                    else omitted++;
                }
            }
        }
        if (omitted > 0) fails.add("and " + omitted + " more");
        return new Report(r.pieces.size(), boxes, jigsaws, markers, checked, sampled, found, left, fails);
    }

    /**
     * The rule {@code clearance7}: the transitions of the chosen pieces read from the world, as {@link Clearance7#check} sees them (collision-free cells), plus the head
     * room of the stairwells among them. Pure apart from the world reads; empty = the rule holds.
     */
    static List<String> clearance7(ServerLevel level, Result r, java.util.function.IntPredicate pieces) {
        Clearance7.Space space = ClearanceCarver.space(level);
        List<String> bad = new ArrayList<>(Clearance7.check(r, space, pieces, 4));
        for (Placed p : r.pieces) {
            if (p.shape.kind != RoomKind.STAIRWELL || !pieces.test(p.index)) continue;
            List<Vec> stands = ClearanceCarver.standsOf(level, p.box);
            int short7 = Clearance7.shortHeadroom(space, stands);
            if (short7 > 0) bad.add("piece " + p.index + " (" + p.shape.id + "): " + short7 + " of " + stands.size() + " stair blocks have less than 7 free blocks above them");
        }
        return bad;
    }

    /**
     * The rule {@code rooms_rebuilt} (docs/labyrinth/rooms_rebuilt.md): every piece that was built from another template than its shape names must have been built from a rebuilt
     * variant of exactly that shape's template ({@link RoomVariants}) whose size is the shape's size. The sockets and markers of such a piece are then judged by the checks above (the
     * game's own boxes, jigsaw cells and marker cells against the plan), which is what proves a rebuilt room is the shipped room's size, doors and encounters. Empty = the rule holds.
     */
    static List<String> rebuiltRooms(StructureTemplateManager mgr, Result r, SectorGenerator.Built built) {
        List<String> bad = new ArrayList<>();
        for (int i = 0; i < r.pieces.size() && i < built.templates().size(); i++) {
            Placed p = r.pieces.get(i);
            String id = built.templates().get(i);
            if (id.equals(p.shape.id)) continue;
            if (!RoomVariants.isVariantId(id) || !RoomVariants.shippedOf(id).equals(p.shape.id)) {
                bad.add(label(p) + " is built from " + id + ", which is not a rebuilt room of " + p.shape.id);
                continue;
            }
            ResourceLocation rl = ResourceLocation.tryParse(id);
            Optional<StructureTemplate> t = rl == null ? Optional.empty() : mgr.get(rl);
            if (t.isEmpty()) bad.add(label(p) + ": the rebuilt room " + id + " cannot be read");
            else if (t.get().getSize().getX() != p.shape.sx || t.get().getSize().getY() != p.shape.sy || t.get().getSize().getZ() != p.shape.sz) {
                bad.add(label(p) + ": the rebuilt room " + id + " is " + t.get().getSize().getX() + "x" + t.get().getSize().getY() + "x" + t.get().getSize().getZ() + ", the shape is " + p.shape.sx + "x" + p.shape.sy + "x" + p.shape.sz);
            }
        }
        return bad;
    }

    private static List<Box> skipBoxes(List<Clearance7.Cut> cuts) {
        List<Box> out = new ArrayList<>(cuts.size());
        for (Clearance7.Cut c : cuts) out.add(c.box());
        return out;
    }

    private static boolean inAny(List<Box> boxes, Vec v) {
        for (Box b : boxes) if (b.contains(v)) return true;
        return false;
    }

    private static String label(Placed p) { return "piece " + p.index + " (" + p.shape.id + " r" + p.rot + " at " + p.origin + ")"; }

    private static String show(BoundingBox b) { return "[" + b.minX() + "," + b.minY() + "," + b.minZ() + " .. " + b.maxX() + "," + b.maxY() + "," + b.maxZ() + "]"; }

    // ------------------------------------------------------------------------------------------------ no placement needed

    /** The game's bounding box of the piece must equal the plan's. Returns 1 when it does. */
    private static int box(StructureTemplateManager mgr, Placed p, PoolElementStructurePiece piece, List<String> bad) {
        BoundingBox vanilla = piece.getElement().getBoundingBox(mgr, piece.getPosition(), piece.getRotation());
        BoundingBox ours = SectorGenerator.bb(p.box);
        if (!vanilla.equals(ours)) {
            bad.add(label(p) + ": the game's box " + show(vanilla) + " differs from the plan's " + show(ours));
            return 0;
        }
        return 1;
    }

    /** The jigsaw cells and their directions the game finds must be the plan's sockets. Returns how many matched. */
    private static int sockets(StructureTemplateManager mgr, Placed p, PoolElementStructurePiece piece, List<String> bad) {
        StructurePoolElement el = piece.getElement();
        Set<String> vanilla = new LinkedHashSet<>();
        for (StructureTemplate.StructureBlockInfo info : el.getShuffledJigsawBlocks(mgr, piece.getPosition(), piece.getRotation(), RandomSource.create(1L))) {
            Direction front = JigsawBlock.getFrontFacing(info.state());
            vanilla.add(info.pos().getX() + "," + info.pos().getY() + "," + info.pos().getZ() + " " + front.getName());
        }
        Set<String> ours = new LinkedHashSet<>();
        for (int i = 0; i < p.shape.sockets.size(); i++) {
            Vec c = p.socketCell(i);
            ours.add(c.x() + "," + c.y() + "," + c.z() + " " + p.socketFacing(i).lower());
        }
        if (!vanilla.equals(ours)) {
            Set<String> onlyVanilla = new HashSet<>(vanilla), onlyOurs = new HashSet<>(ours);
            onlyVanilla.removeAll(ours);
            onlyOurs.removeAll(vanilla);
            bad.add(label(p) + ": the game puts jigsaws at " + sample(onlyVanilla) + " where the plan has " + sample(onlyOurs));
            return 0;
        }
        return ours.size();
    }

    /** Every marker the plan computed must be a data marker cell the game finds (the game may know more: markers of other namespaces). Returns how many matched. */
    private static int markers(StructureTemplateManager mgr, Placed p, PoolElementStructurePiece piece, List<String> bad) {
        if (p.shape.markers.isEmpty()) return 0;
        StructurePoolElement el = piece.getElement();
        if (!(el instanceof SinglePoolElement single)) return 0;
        Set<String> relative = cells(single.getDataMarkers(mgr, piece.getPosition(), piece.getRotation(), true));
        Set<String> raw = cells(single.getDataMarkers(mgr, piece.getPosition(), piece.getRotation(), false));
        int ok = 0;
        List<String> missing = new ArrayList<>();
        for (ShapeMarker m : p.shape.markers) {
            Vec w = p.world(m.cell());
            String key = w.x() + "," + w.y() + "," + w.z();
            if (relative.contains(key)) ok++;
            else missing.add(key);
        }
        if (!missing.isEmpty()) {
            bad.add(label(p) + ": " + missing.size() + " planned markers (first " + sample(new HashSet<>(missing)) + ") are not where the game puts data markers. The game lists "
                    + sample(relative) + " with relative positions on and " + sample(raw) + " with them off");
            return 0;
        }
        return ok;
    }

    private static Set<String> cells(List<StructureTemplate.StructureBlockInfo> infos) {
        Set<String> out = new LinkedHashSet<>();
        for (StructureTemplate.StructureBlockInfo i : infos) out.add(i.pos().getX() + "," + i.pos().getY() + "," + i.pos().getZ());
        return out;
    }

    private static String sample(Set<String> s) {
        if (s.isEmpty()) return "none";
        List<String> l = new ArrayList<>(s);
        l.sort(String::compareTo);
        StringBuilder b = new StringBuilder("(");
        for (int i = 0; i < l.size() && i < 3; i++) b.append(i > 0 ? " | " : "").append(l.get(i));
        if (l.size() > 3) b.append(" ...");
        return b.append(')').toString();
    }

    // ------------------------------------------------------------------------------------------------ the placed world

    /** Which pieces to read from the world: the first of every room kind (the Landing, the arena ...), the first door cap, the first tunnel piece, then evenly spread. */
    static List<Integer> pick(Result r, int max) {
        Set<Integer> out = new LinkedHashSet<>();
        Set<RoomKind> seen = new HashSet<>();
        for (Placed p : r.pieces) {
            if (out.size() >= max) break;
            if (seen.add(p.shape.kind)) out.add(p.index);
        }
        int n = r.pieces.size();
        if (n > 0) {
            int step = Math.max(1, n / Math.max(1, max));
            for (int i = 0; i < n && out.size() < max; i += step) out.add(i);
        }
        return new ArrayList<>(out);
    }

    /** {sampled, found non-air, jigsaws left} for one piece. */
    private static int[] world(ServerLevel level, StructureTemplateManager mgr, Placed p, String templateId, List<Box> skip) {          // the template the piece was really built from (a rebuilt room has other blocks than the shipped one)
        ResourceLocation id = ResourceLocation.tryParse(templateId);
        Optional<StructureTemplate> t = id == null ? Optional.empty() : mgr.get(id);
        if (t.isEmpty()) return new int[] {0, 0, 0};
        CompoundTag nbt = t.get().save(new CompoundTag());
        ListTag palette = nbt.getList("palette", Tag.TAG_COMPOUND);
        if (palette.isEmpty() && nbt.contains("palettes", Tag.TAG_LIST)) {
            ListTag all = nbt.getList("palettes", Tag.TAG_LIST);
            if (!all.isEmpty()) palette = all.getList(0);
        }
        boolean[] solidState = new boolean[palette.size()];
        for (int i = 0; i < solidState.length; i++) solidState[i] = !NOT_SOLID.contains(palette.getCompound(i).getString("Name"));
        ListTag blocks = nbt.getList("blocks", Tag.TAG_COMPOUND);
        List<int[]> solid = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag b = blocks.getCompound(i);
            int state = b.getInt("state");
            if (state < 0 || state >= solidState.length || !solidState[state]) continue;
            ListTag pos = b.getList("pos", Tag.TAG_INT);
            if (pos.size() != 3) continue;
            if (!skip.isEmpty() && inAny(skip, p.world(new Vec(pos.getInt(0), pos.getInt(1), pos.getInt(2))))) continue;     // 1.107.0: a block the 7 x 7 cuts took away is not expected to stand
            solid.add(new int[] {pos.getInt(0), pos.getInt(1), pos.getInt(2)});
        }
        int sampled = 0, found = 0;
        int step = Math.max(1, solid.size() / SAMPLES_PER_PIECE);
        for (int i = 0; i < solid.size() && sampled < SAMPLES_PER_PIECE; i += step) {
            int[] c = solid.get(i);
            Vec w = p.world(new Vec(c[0], c[1], c[2]));
            sampled++;
            if (!level.getBlockState(new BlockPos(w.x(), w.y(), w.z())).isAir()) found++;
        }
        int left = 0;
        for (int i = 0; i < p.shape.sockets.size(); i++) {
            Vec c = p.socketCell(i);
            if (level.getBlockState(new BlockPos(c.x(), c.y(), c.z())).is(Blocks.JIGSAW)) left++;
        }
        return new int[] {sampled, found, left};
    }
}
