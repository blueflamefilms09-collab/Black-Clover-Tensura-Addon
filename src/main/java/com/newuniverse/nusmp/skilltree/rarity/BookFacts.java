package com.newuniverse.nusmp.skilltree.rarity;

import com.newuniverse.nusmp.skilltree.unique.LineIndex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: three doors of the loot, the shop and the swap of an old book all ask the same questions about a line: how rare is it, which franchise is it from, is that franchise blocked, which background theme does
 * it wear. The answers are in the jar's own table of books ({@link LineIndex}, the shipped {@code skill_books.json}), which the registry already reads on both sides, so this is a thin, allocation-free
 * lookup over that table: a hash map by bare id built ONCE (the table is immutable for the life of the game), and nothing here touches a world, a player or a config. A line the table does not know (a
 * route of a data pack, a test route) is common, has no franchise and is never blocked. The blacklist itself is {@link BlockedFranchises}; this class only joins the two.
 */
public final class BookFacts {
    private BookFacts() {}

    private static volatile Map<String, LineIndex.Line> byBare;

    private static Map<String, LineIndex.Line> table() {
        Map<String, LineIndex.Line> m = byBare;
        if (m == null) {
            m = new HashMap<>();
            for (LineIndex.Line l : LineIndex.shipped()) m.put(l.bare(), l);
            byBare = m;
        }
        return m;
    }

    /** The line of a bare id or route id in the shipped table, or null. */
    public static LineIndex.Line line(String bareOrRouteId) {
        if (bareOrRouteId == null) return null;
        return table().get(LineIndex.bare(bareOrRouteId));
    }

    /** The rarity of a line ({@code lady_luck} or {@code nusmp:lady_luck}); common when the table does not know it. */
    public static BookRarity rarityOf(String bareOrRouteId) {
        LineIndex.Line l = line(bareOrRouteId);
        return l == null ? BookRarity.COMMON : l.rarity();
    }

    /** The franchise key of a line, or "" when unknown. */
    public static String franchiseOf(String bareOrRouteId) {
        LineIndex.Line l = line(bareOrRouteId);
        return l == null ? "" : l.franchise();
    }

    /** Is this line of a franchise on the blacklist in force? (Always false for a line the table does not know.) */
    public static boolean isBlocked(String bareOrRouteId) { return BlockedFranchises.isBlocked(franchiseOf(bareOrRouteId)); }

    /** Is this franchise of the table blocked by an explicit setting string (no facade; for tests and the self-test)? */
    public static boolean isBlocked(String bareOrRouteId, String setting) { return BlockedFranchises.blocked(BlockedFranchises.parse(setting), franchiseOf(bareOrRouteId)); }

    /** Every line of the shipped table as a candidate of the Guardian pick, with the blacklist of {@code setting} applied. One small record per line, built per Guardian death. */
    public static List<BossThemes.Cand> candidates(String setting) {
        var blocked = BlockedFranchises.parse(setting);
        List<LineIndex.Line> lines = LineIndex.shipped();
        List<BossThemes.Cand> out = new ArrayList<>(lines.size());
        for (LineIndex.Line l : lines) out.add(new BossThemes.Cand(l.bare(), l.rarity(), l.theme(), BlockedFranchises.blocked(blocked, l.franchise())));
        return out;
    }
}
