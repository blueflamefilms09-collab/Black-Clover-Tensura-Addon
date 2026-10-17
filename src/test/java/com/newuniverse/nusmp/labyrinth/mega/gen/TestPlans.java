package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: every assembler test starts from "plan sector 0 of a dungeon with this seed", and the plan must be built exactly as the game builds it (through
 * {@link SectorRequests}), or the tests would prove things about a request nobody makes. This is that one call, plus a few helpers the tests share: a
 * one-line-per-piece fingerprint of a result (to compare two runs for equality), and an independent, deliberately naive structural check (pairwise overlaps,
 * region, the geometry of every join) written without looking at {@link SectorAudit}, so a bug in the audit cannot hide a bug in the assembler.
 */
final class TestPlans {
    private TestPlans() {}

    static Request first(PoolSource src, long dungeonSeed) { return first(src, dungeonSeed, new Recipe(), 450); }

    static Request first(PoolSource src, long dungeonSeed, Recipe recipe, int maxPieces) {
        SectorPlanner.Plan plan = SectorPlanner.first(dungeonSeed);
        int relics = RegionTheme.GRAVITY.relicCount(dungeonSeed, 0);
        SectorRequests.Prepared p = SectorRequests.prepare(new SectorRequests.Inputs(src, recipe, maxPieces, 40, plan.seed(), plan.entry(), plan.forward(), plan.region(),
                null, null, null, List.of(), relics));
        if (!p.ok()) throw new IllegalStateException(p.failure());
        return p.request();
    }

    /**
     * The request of one FLOOR of the floor model (1.58.0), built the way the game builds it: the plan comes from {@link SectorPlanner#floor}, the room count and the
     * stairs from {@link FloorPlan#spec}, the theme from {@link SectorDraft} (so a decade shares its theme). {@code previousTheme} is the theme of the floor before (null
     * for the first floor of a road).
     */
    static Request floor(PoolSource src, long dungeonSeed, Polarity road, int chainIndex, int configuredRooms, Recipe recipe, TemplateThemes themes, RegionTheme previousTheme) {
        Request.Floor fl = FloorPlan.spec(road, chainIndex, configuredRooms);
        SectorDraft draft = SectorDraft.of(dungeonSeed, chainIndex * 2 + (road == Polarity.ABYSSAL ? 1 : 0), road, chainIndex, 1, previousTheme, null);
        SectorPlanner.Plan plan = SectorPlanner.floor(dungeonSeed, draft.id(), road, chainIndex, fl.rooms(), fl.boss());
        SectorRequests.Prepared p = SectorRequests.prepare(new SectorRequests.Inputs(src, recipe, 450, 40, plan.seed(), plan.entry(), plan.forward(), plan.region(),
                null, null, null, List.of(), draft.relicCount(), draft.theme().id(), themes, fl));
        if (!p.ok()) throw new IllegalStateException(p.failure());
        return p.request();
    }

    static Request floor(PoolSource src, long dungeonSeed, Polarity road, int chainIndex) {
        return floor(src, dungeonSeed, road, chainIndex, 28, Recipe.forFloors(), TemplateThemes.none(), null);
    }

    /** One line per piece: enough to tell two plans apart and nothing that depends on object identity. */
    static List<String> fingerprint(Result r) {
        List<String> out = new ArrayList<>();
        for (Placed p : r.pieces) out.add(p.index + " " + p.shape.id + " r" + p.rot + " " + p.origin + " d" + p.depth + " p" + p.parent + ":" + p.parentSocket + ">" + p.ownSocket);
        return out;
    }

    /** Naive structural checks; returns the problems found (empty = sound). */
    static List<String> brute(Result r, Request q) {
        List<String> bad = new ArrayList<>();
        List<Placed> ps = r.pieces;
        for (int i = 0; i < ps.size(); i++) {
            Placed p = ps.get(i);
            if (p.index != i) bad.add("piece " + i + " has index " + p.index);
            boolean cap = p.shape.isCap();
            if (!p.link && !p.box.inside(q.region, cap ? q.capMargin : 0)) bad.add(p + " leaves the region");
            if (!p.link) for (Box ex : q.exclusions) if (ex.overlaps(p.box)) bad.add(p + " overlaps an exclusion");
            for (int j = 0; j < i; j++) if (ps.get(j).box.overlaps(p.box)) bad.add(p + " overlaps " + ps.get(j));
            if (p.parent >= 0) {
                if (p.parent >= i) {
                    bad.add(p + " has a later parent");
                    continue;
                }
                Placed par = ps.get(p.parent);
                Socket from = par.shape.sockets.get(p.parentSocket), to = p.shape.sockets.get(p.ownSocket);
                if (!par.socketFront(p.parentSocket).equals(p.socketCell(p.ownSocket))) bad.add(p + " is not on the front cell of " + par + " socket " + p.parentSocket);
                if (par.socketFacing(p.parentSocket) != p.socketFacing(p.ownSocket).opposite()) bad.add(p + " does not face " + par);
                if (!Socket.canAttach(from.target(), par.socketFacing(p.parentSocket), to.name(), p.socketFacing(p.ownSocket))) bad.add(p + " cannot attach to " + par + " by name");
                if (!par.box.inflateXZ(1).overlaps(p.box)) bad.add(p + " does not touch " + par);
                if (!par.isUsed(p.parentSocket) || !p.isUsed(p.ownSocket)) bad.add(p + " join is not marked used");
                if (!p.link && !par.link && p.depth != par.depth + 1) bad.add(p + " has depth " + p.depth + " under depth " + par.depth);
            }
        }
        return bad;
    }
}
