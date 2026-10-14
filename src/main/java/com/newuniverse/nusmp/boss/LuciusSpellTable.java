package com.newuniverse.nusmp.boss;

import com.newuniverse.nusmp.entity.zogratis.LuciusTable;

import java.util.List;

/**
 * Lucius's three cast spells as move rows (pure, unit tested), appended after {@link LuciusTable#specs()} by LuciusBoss so the
 * existing table, its tests and the order of his move list never change. Tensura's own Megiddo, True Darkness and Dimension Cutter
 * hit with Tensura's unbounded numbers, which the boss framework's damage law cannot cap, so these are the mod's versions: named
 * after the spells, with the mod's effects, a telegraph of 30 ticks or more and capped hits through the framework's strike.
 */
public final class LuciusSpellTable {
    private LuciusSpellTable() {}

    private static final List<LuciusTable.Spec> SPECS = List.of(
            new LuciusTable.Spec(LuciusNumbers.MEGIDDO, "Megiddo", 40, 40, 520, 0, 44, 3, false, true, 12),
            new LuciusTable.Spec(LuciusNumbers.DIMENSION_CUTTER, "Dimension Cutter", 40, 30, 300, 4, 36, 4, false, false, 10),
            new LuciusTable.Spec(LuciusNumbers.TRUE_DARKNESS, "True Darkness", 60, 48, 780, 0, 30, 2, false, true, 14),
            // 1.35.0: two late-phase moves, appended after the spells (the client reads move names by index)
            new LuciusTable.Spec(LuciusNumbers.JUDGMENT, "Judgment of Light", 60, 36, 360, 0, 40, 4, false, true, 12),
            new LuciusTable.Spec(LuciusNumbers.CHRONO_RINGS, "Chrono Rings", 80, 30, 320, 0, 16, 4, false, true, 14));

    public static List<LuciusTable.Spec> specs() { return SPECS; }

    public static boolean isSpell(String id) {
        for (LuciusTable.Spec s : SPECS) if (s.id().equals(id)) return true;
        return false;
    }

    /** His whole move list: the original table, then the three spells, then the two late-phase moves. */
    public static List<LuciusTable.Spec> allSpecs() {
        List<LuciusTable.Spec> all = new java.util.ArrayList<>(LuciusTable.specs());
        all.addAll(SPECS);
        return List.copyOf(all);
    }
}
