package com.newuniverse.nusmp.labyrinth.props;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the owner asked for the armour stands back in the dungeon, but "please don't make it a set decoration, it should be a rare one". The shipped templates carried stands as scenery (84 of
 * them held one or more, up to twelve in a hall) and the weapon racks replaced them; now the stand is a RARE prop: of the rooms where a stand makes sense (armoury, barracks, guard post,
 * training hall, treasure and vault rooms: {@link #eligibleTemplate}) a few percent get ONE, at most {@code floorCap} to a floor, never two in rooms that touch, never in a corridor, a
 * junction or a stair. This class is the whole decision, pure and deterministic: for the pieces of a plan (the template of each, its footprint) it answers which pieces get the stand, from the
 * plan's seed alone, so a regeneration or a repair puts the stands in the same rooms. It runs once per floor at generation time; nothing here runs per tick and no entity exists until the
 * placer spawns the one stand ({@code ArmourStandGate}).
 * <p>
 * How the numbers work: each eligible room rolls a stable hash of (seed, piece index, template) against {@code percent} (so the expected share of eligible rooms is {@code percent}, less what the
 * two rules below take away); rooms are then taken in plan order, a room that touches an already chosen room is dropped, and once {@code floorCap} rooms are chosen the rest are dropped. With the
 * default 6 percent and cap 2, a floor of ten eligible rooms has a stand in about half of its floors, and more than two never.
 */
public final class ArmourStandRarity {
    private ArmourStandRarity() {}

    /** The configured chance is a percent between 0 (no stand ever) and 100. */
    public static final double MAX_PERCENT = 100.0;
    /** The most stands a floor may hold, whatever the config says. */
    public static final int HARD_CAP = 8;

    /** One piece of a plan: its index, its shipped template and its footprint (inclusive block coordinates, x and z only: touching is decided in the plane). */
    public record Piece(int index, String template, int minX, int minZ, int maxX, int maxZ) {
        /** Do the footprints touch or overlap (the walls of two neighbouring rooms stand side by side)? */
        public boolean touches(Piece o) {
            return minX <= o.maxX + 1 && o.minX <= maxX + 1 && minZ <= o.maxZ + 1 && o.minZ <= maxZ + 1;
        }
    }

    private static final String[] STEMS = {"armory", "barracks", "guard_post", "training", "hoard", "counting", "reliquary", "gem_vault", "sarcophagus_vault", "burial_vault", "closet", "pantry"};

    /**
     * May a room of this template hold the rare stand? Halls and treasure rooms whose name says armoury, barracks, guard post, training hall, hoard, counting house, reliquary, gem vault, burial or
     * sarcophagus vault, closet or pantry. Never a corridor, junction, link, stair, trap, puzzle, trial, arena, antechamber, landing, relic vault or Safe Zone. Accepts a shipped id
     * ({@code nusmp:mega/hall/armory_forge_a}) or a rebuilt one ({@code mega_r/hall/armory_forge_a_v2}).
     */
    public static boolean eligibleTemplate(String id) {
        if (id == null) return false;
        String p = id.indexOf(':') >= 0 ? id.substring(id.indexOf(':') + 1) : id;
        boolean room = p.startsWith("mega/hall/") || p.startsWith("mega/treasure/") || p.startsWith("mega_r/hall/") || p.startsWith("mega_r/treasure/");
        if (!room) return false;
        String stem = p.substring(p.lastIndexOf('/') + 1);
        for (String s : STEMS) if (stem.contains(s)) return true;
        return false;
    }

    /** SplitMix64 finalizer over {@code z + golden}: the same mixing the generator's own random numbers use, so nearby seeds do not give nearby rolls. */
    static long mix(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** The stable roll of one room: true with probability {@code percent / 100}, the same answer for the same (seed, index, template) on every machine. */
    public static boolean roll(long seed, int index, String template, double percent) {
        if (!(percent > 0.0)) return false;
        if (percent >= MAX_PERCENT) return true;
        double u = (mix(mix(seed) ^ (index * 0xD1B54A32D192ED03L + template.hashCode())) >>> 11) / (double) (1L << 53);
        return u < percent / MAX_PERCENT;
    }

    /** The indices of the pieces that get the stand, in plan order. Empty when {@code percent} is 0 or {@code floorCap} is 0. */
    public static List<Integer> plan(List<Piece> pieces, long seed, double percent, int floorCap) {
        List<Integer> out = new ArrayList<>();
        int cap = Math.min(Math.max(0, floorCap), HARD_CAP);
        if (!(percent > 0.0) || cap == 0 || pieces == null) return out;
        List<Piece> chosen = new ArrayList<>();
        for (Piece p : pieces) {
            if (chosen.size() >= cap) break;
            if (!eligibleTemplate(p.template()) || !roll(seed, p.index(), p.template(), percent)) continue;
            boolean next = false;
            for (Piece c : chosen) if (c.touches(p)) { next = true; break; }
            if (next) continue;
            chosen.add(p);
            out.add(p.index());
        }
        return out;
    }

    /** The configured percent made safe: NaN and negatives are 0, anything above 100 is 100. */
    public static double clampPercent(double percent) {
        if (!(percent > 0.0)) return 0.0;
        return Math.min(MAX_PERCENT, percent);
    }
}
