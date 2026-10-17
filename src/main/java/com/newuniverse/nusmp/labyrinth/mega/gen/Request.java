package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;

import java.util.List;
import java.util.Map;

/**
 * Why: everything the assembler needs to plan ONE sector, in one value, so a plan is a pure function of its request: the same request gives the
 * same pieces on every machine, which is what makes the restart repair and {@code /multiverse mega regen} safe. It names where the sector starts
 * (the Landing hangs from a world socket: for sector 0 a fixed one, for the others the end of the tunnel behind the previous sector's seal), the
 * region the sector may fill, the boxes it must keep out of (other sectors' regions and tunnels), the limits, the Sector Recipe, the pools that are
 * forced for the boss chain (the vault by relic count), the policy for the Way Onward and the seed. The fields are public and mutable because this is
 * a parameter bag built once by the caller; {@link #withSeed} gives the solver an independent copy per try.
 */
public final class Request {
    /**
     * Where the sector starts. The Landing is drawn from {@code pool} and turned so that one of its sockets named {@code socketName} lands on
     * {@code frontCell} and points back against {@code growth} (the direction the sector grows in). {@code parent} / {@code parentSocket} say which
     * piece of {@link #pre} the Landing hangs from (the last tunnel piece), or -1 when it hangs from nothing (sector 0).
     */
    public record Start(Vec frontCell, Facing growth, String pool, String socketName, int parent, int parentSocket) {}

    /**
     * 1.58.0, the floor model: what makes this request plan ONE floor (a level of its own) instead of a ten-floor sector. The Landing is replaced by the ARRIVAL
     * stairwell (drawn from {@code arrivalPool}, with its only door facing into the region), the Way Onward and the tunnel to the next sector by a FORWARD stairwell
     * (drawn from {@code forwardPool}): on an ordinary floor it hangs from the door of the room that is farthest from the arrival by walking distance, on a boss
     * floor it stands behind the Way Onward of the boss chain. {@code rooms} is the number of rooms the floor aims at (stairwells, boss chain and Safe Zone
     * included), {@code minRooms} the least the audit accepts.
     *
     * @param boss         is this a boss floor (the boss chain exists and the forward stairwell is behind it)
     * @param arrivalPool  the pool of the arrival stairwell (stairs that lead BACK: down on the Holy road, up on the Abyssal one)
     * @param forwardPool  the pool of the forward stairwell (up on the Holy road, down on the Abyssal one; the grand ones on a boss floor)
     * @param rooms        the room count aimed at
     * @param minRooms     the least number of rooms a plan may have
     */
    public record Floor(boolean boss, String arrivalPool, String forwardPool, int rooms, int minRooms) {}

    public PoolSource source;
    public long seed;
    public Start start;
    /** The box the sector may fill. Pieces must lie inside it; door caps may protrude by {@link #capMargin}. */
    public Box region;
    /** Boxes no piece or cap may overlap (the other sectors' regions, grown by what must stay clear around them). */
    public List<Box> exclusions = List.of();
    public int maxPieces = 450;
    public int capMargin = MegaGrid.CAP_MARGIN;
    public Recipe recipe = new Recipe();
    /** Pool overrides by socket name, e.g. {@code nusmp:mega_vault} to {@code nusmp:mega/vault_2}. */
    public Map<String, String> forced = Map.of();
    /** How the Way Onward is chosen and checked; null means "use the pool written in the template and accept any seal". */
    public OnwardPolicy onward;
    /** Pieces that already stand before the assembler starts (the tunnel from the previous sector), with final indices 0..n-1. */
    public List<Placed> pre = List.of();
    /** How many relics the Crucible offers: the vault must have this many PEDESTAL markers. */
    public int relicCount = 1;
    /** The fallback layout: the Landing and the boss chain only, with the distance rules off. */
    public boolean fallback;
    /** 1.58.0: set for a floor of the floor model (see {@link Floor}); null for a sector of the old model. */
    public Floor floor;
    /** 1.58.0: the theme id of the sector ({@code holy_church}); templates reserved for other themes are not drawn ({@link TemplateThemes}). Null = no theme: reserved templates are skipped. */
    public String themeId;
    /** 1.58.0: which templates belong to which theme. {@link TemplateThemes#none()} = every template is free for every sector (the 1.54.0 behaviour). */
    public TemplateThemes themes = TemplateThemes.none();

    /** A shallow copy with another seed (the lists and the recipe are shared; the assembler never writes to them). */
    public Request withSeed(long newSeed, boolean asFallback) {
        Request r = new Request();
        r.source = source;
        r.seed = newSeed;
        r.start = start;
        r.region = region;
        r.exclusions = exclusions;
        r.maxPieces = maxPieces;
        r.capMargin = capMargin;
        r.recipe = recipe;
        r.forced = forced;
        r.onward = onward;
        r.pre = pre;
        r.relicCount = relicCount;
        r.fallback = asFallback;
        r.floor = floor;
        r.themeId = themeId;
        r.themes = themes;
        return r;
    }
}
