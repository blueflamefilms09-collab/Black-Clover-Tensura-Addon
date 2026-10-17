package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: turning "sector 7, behind this seal, with these neighbours" into the assembler's {@link Request} is more than filling in fields: the tunnel from the
 * old seal to the new Landing has to be built and lined up with the Landing's door, the exclusions are the neighbours grown by the reach of a door cap,
 * the vault pool is the one for this sector's relic count, and the Way Onward policy must know every neighbour so it can judge a candidate seal. The game-side
 * generator and the simulator both need exactly this, and if each had its own copy the simulator would stop proving anything about the game. So it is one
 * pure function over plain values: the generator feeds it numbers read from the saved sectors, the tests feed it numbers from a simulated world, and both
 * get the same request for the same sector.
 */
public final class SectorRequests {
    private SectorRequests() {}

    /**
     * Everything the request depends on. {@code prevSeal} / {@code prevFacing} / {@code prevRegion} describe the seal this sector hangs from (null for
     * sector 0); {@code others} are the regions and tunnels of every other sector EXCEPT the tunnel into this one (which is built here).
     */
    public record Inputs(PoolSource source, Recipe recipe, int maxPieces, int maxLinkModules, long seed, Vec entryJigsaw, Facing forward, Box region,
                         Vec prevSeal, Facing prevFacing, Box prevRegion, List<SectorPlanner.Obstacle> others, int relicCount) {}

    /**
     * The request, the tunnel pieces it starts with and, when something does not line up, why the request could not be built. {@code notes} are the things the
     * generator decided on its own that the owner should hear about (a promise of the recipe that the template pools cannot keep, see {@link #effective}).
     */
    public record Prepared(Request request, LinkBuilder.Tunnel tunnel, SectorPlanner.Obstacle tunnelObstacle, String failure, List<String> notes) {
        public Prepared { notes = List.copyOf(notes); }

        public Prepared(Request request, LinkBuilder.Tunnel tunnel, SectorPlanner.Obstacle tunnelObstacle, String failure) { this(request, tunnel, tunnelObstacle, failure, List.of()); }

        public boolean ok() { return failure == null; }
    }

    /**
     * The recipe this sector can really keep. A promise that needs a kind of room no template of the pools offers (the recipe wants two treasure closets, but nobody
     * has drawn one yet; a data pack that dropped the safe rooms) cannot be kept by any seed, and failing every try of every sector for it would leave the dungeon
     * with nothing but the 7-piece fallback layout, which is a far worse sector than one that lacks a closet. So such a promise is waived for this plan and said so in
     * {@code notes}; the pool commands and the log tell the owner which folder is empty. A promise whose kind IS offered is untouched, and the audit holds the plan
     * to the effective recipe, so what is waived here is not demanded there.
     */
    static Recipe effective(PoolSource source, Recipe recipe, List<String> notes) {
        Recipe r = recipe == null ? new Recipe() : recipe;
        PoolDef side = source.pool(MegaGrid.P_SIDE);
        if (side != null) {
            for (RoomKind k : RoomKind.values()) {
                if (k == RoomKind.CAP || r.min(k) <= 0 || offers(source, side, k)) continue;
                notes.add("no template of kind " + k.folder() + " is in the pool " + MegaGrid.P_SIDE + ", so the recipe's minimum of " + r.min(k) + " is waived for this sector");
                r = r == recipe ? recipe.copy() : r;
                r.setMin(k, 0);
            }
        }
        if (r.extraSafe > 0) {
            PoolDef safe = source.pool(MegaGrid.P_SAFE);
            if (safe != null && !offers(source, safe, RoomKind.SAFE)) {
                notes.add("the pool " + MegaGrid.P_SAFE + " has no template, so the recipe's extra safe room is waived for this sector");
                r = r == recipe ? recipe.copy() : r;
                r.extraSafe = 0;
            }
        }
        return r;
    }

    /** Does the pool hold at least one template that can be read and is of this kind? */
    private static boolean offers(PoolSource source, PoolDef pool, RoomKind kind) {
        for (PoolEntry e : pool.entries()) {
            Shape sh = source.shape(e.shape());
            if (sh != null && sh.kind == kind) return true;
        }
        return false;
    }

    public static Prepared prepare(Inputs in) {
        LinkBuilder.Tunnel tunnel = new LinkBuilder.Tunnel(List.of(), in.entryJigsaw(), -1, null);
        SectorPlanner.Obstacle tunnelObstacle = null;
        List<SectorPlanner.Obstacle> beyond = new ArrayList<>(in.others());
        if (in.prevSeal() != null) {
            int modules = SectorPlanner.linkModulesBetween(in.prevSeal(), in.prevFacing(), in.entryJigsaw());
            if (modules < 1) return new Prepared(null, tunnel, null, "the Landing door is not behind the old seal (" + modules + " tunnel segments)");
            final List<SectorPlanner.Obstacle> all = in.others();
            final Box prevReach = in.prevRegion() == null ? null : in.prevRegion().inflateXZ(MegaGrid.CAP_MARGIN);
            tunnel = LinkBuilder.build(in.source(), in.prevSeal(), in.prevFacing(), modules, in.seed(), box -> {
                if (prevReach != null && box.overlaps(prevReach)) return false;
                for (SectorPlanner.Obstacle o : all) if (box.overlaps(o.box().inflateXZ(MegaGrid.CAP_MARGIN))) return false;
                return true;
            });
            if (!tunnel.ok()) return new Prepared(null, tunnel, null, "tunnel: " + tunnel.failure());
            if (!tunnel.landingFront().equals(in.entryJigsaw())) return new Prepared(null, tunnel, null, "the tunnel ends at " + tunnel.landingFront() + " but the Landing door is at " + in.entryJigsaw());
            tunnelObstacle = SectorPlanner.Obstacle.tunnel(SectorPlanner.tunnelBox(in.prevSeal(), in.prevFacing(), modules));
            beyond.add(tunnelObstacle);
        }
        List<String> notes = new ArrayList<>();
        Request q = new Request();
        q.source = in.source();
        q.seed = in.seed();
        q.recipe = effective(in.source(), in.recipe(), notes);
        q.maxPieces = in.maxPieces();
        q.region = in.region();
        q.relicCount = in.relicCount();
        q.exclusions = SectorPlanner.exclusionsFor(in.region(), in.others());
        q.forced = Map.of(MegaGrid.VAULT, MegaGrid.P_VAULT + "_" + in.relicCount());
        q.onward = SectorPlanner.policy(in.seed(), in.region(), beyond, in.maxLinkModules());
        q.pre = tunnel.pieces();
        int parent = tunnel.pieces().isEmpty() ? -1 : tunnel.pieces().size() - 1;
        q.start = new Request.Start(in.entryJigsaw(), in.forward(), MegaGrid.P_LANDING, MegaGrid.DOOR, parent, tunnel.lastOutSocket());
        return new Prepared(q, tunnel, tunnelObstacle, null, notes);
    }
}
