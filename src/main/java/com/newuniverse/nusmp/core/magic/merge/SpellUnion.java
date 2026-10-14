package com.newuniverse.nusmp.core.magic.merge;

import java.util.ArrayList;
import java.util.List;

/**
 * The curated spell list of a merged magic (pure). From each parent's page list (in the parent's own order, starter first, signature
 * last) it takes a fixed number of pages: 4 per parent for two parents, 3 for three, 2 for four, spread evenly so that the first
 * (starter) and the last (signature) page are always among them. The picks are interleaved by rank (every parent's starter, then every
 * parent's second page ...), so a merged book opens with a spell of each parent and grows the same way.
 *
 * <p>Cost, cooldown and mastery need of a page are the LARGEST of the parents' pages of the same rank, times
 * {@link MergeLimits#COST_FACTOR}: a merged spell is never cheaper than the dearest parent's.</p>
 */
public final class SpellUnion {
    private SpellUnion() {}

    /** What the union needs to know of a parent's page. */
    public record PageInfo(String id, double costPercent, double costFloor, int cooldown) {}

    /** A chosen page: which parent (index in canonical order), its page id and the merged numbers. */
    public record Pick(int parent, String pageId, double costPercent, double costFloor, int cooldown) {}

    /** Pages taken from each parent for a number of parents. */
    public static int perParent(int parents) { return parents <= 2 ? 4 : parents == 3 ? 3 : 2; }

    /** Indices spread evenly over 0..count-1, always including the first and the last when take >= 2. */
    public static int[] spread(int count, int take) {
        int n = Math.max(0, Math.min(count, take));
        int[] out = new int[n];
        if (n == 0) return out;
        if (n == 1) { out[0] = 0; return out; }
        for (int k = 0; k < n; k++) out[k] = (int) Math.round(k * (count - 1) / (double) (n - 1));
        return out;
    }

    /** The union over the parents' pages (outer list = parent in canonical order). Never null; a parent with no pages adds none. */
    public static List<Pick> union(List<List<PageInfo>> parentPages) {
        List<Pick> out = new ArrayList<>();
        if (parentPages == null || parentPages.isEmpty()) return out;
        int take = perParent(parentPages.size());
        List<int[]> chosen = new ArrayList<>();
        int ranks = 0;
        for (List<PageInfo> pages : parentPages) {
            int[] idx = spread(pages == null ? 0 : pages.size(), take);
            chosen.add(idx);
            ranks = Math.max(ranks, idx.length);
        }
        for (int rank = 0; rank < ranks; rank++) {
            // the dearest page of this rank over all the parents that have one
            double cost = 0, floor = 0;
            int cd = 0;
            for (int p = 0; p < parentPages.size(); p++) {
                int[] idx = chosen.get(p);
                if (rank >= idx.length) continue;
                PageInfo info = parentPages.get(p).get(idx[rank]);
                cost = Math.max(cost, info.costPercent());
                floor = Math.max(floor, info.costFloor());
                cd = Math.max(cd, info.cooldown());
            }
            double mc = Math.min(MergeLimits.COST_CAP_PERCENT, cost * MergeLimits.COST_FACTOR);
            double mf = floor * MergeLimits.COST_FACTOR;
            int mcd = (int) Math.round(cd * MergeLimits.COST_FACTOR);
            for (int p = 0; p < parentPages.size(); p++) {
                int[] idx = chosen.get(p);
                if (rank >= idx.length) continue;
                out.add(new Pick(p, parentPages.get(p).get(idx[rank]).id(), mc, mf, mcd));
            }
        }
        return out;
    }
}
