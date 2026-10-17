package com.newuniverse.nusmp.labyrinth.mega.eco;

import com.newuniverse.nusmp.dungeon.sentient.Dice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: every faction test needs a few small {@link FactionDef}s to build graphs from, and writing the thirteen-argument constructor in each
 * test would hide what the test is about. These factories name the only things a test cares about (the relations, the flags, the weights) and
 * give everything else a neutral value.
 */
final class Fx {
    private Fx() {}

    /**
     * A dice stream for one seed. {@code new Random(i)} for consecutive i gives nearly identical FIRST draws (the weakness {@link Mix} exists to avoid),
     * and these tests roll once per seed, so every stream here is built on the splitmix finalizer and is statistically independent per seed.
     */
    static Dice seeded(long seed) {
        long[] k = {0};
        return () -> Mix.unit(seed * 0x9E3779B97F4A7C15L + k[0]++);
    }

    /** A faction with the given relations, no aggro numbers of its own (so the server config applies) and no theme weights. */
    static FactionDef def(String id, List<String> rivals, List<String> allies) {
        return new FactionDef(id, id, 0xFFFFFF, "nusmp:mega/" + id, rivals, allies, FactionDef.UNSET, FactionDef.UNSET, false, false, Map.of(), "raw_monster_meat", 0.5);
    }

    static FactionDef oneWay(String id, List<String> rivals) {
        return new FactionDef(id, id, 0xFFFFFF, "nusmp:mega/" + id, rivals, List.of(), FactionDef.UNSET, FactionDef.UNSET, false, true, Map.of(), "raw_monster_meat", 0.5);
    }

    static FactionDef territorial(String id, List<String> rivals, int range, int bonus) {
        return new FactionDef(id, id, 0xFFFFFF, "nusmp:mega/" + id, rivals, List.of(), range, bonus, true, false, Map.of(), "raw_armor_flank", 0.25);
    }

    static FactionDef withRange(String id, List<String> rivals, int range, int bonus) {
        return new FactionDef(id, id, 0xFFFFFF, "nusmp:mega/" + id, rivals, List.of(), range, bonus, false, false, Map.of(), "raw_monster_meat", 0.5);
    }

    /** Alternating key/weight pairs: {@code weights("gravity", 3, "blood", 1)}. */
    static FactionDef weighted(String id, Object... themeAndWeight) {
        Map<String, Double> w = new LinkedHashMap<>();
        for (int i = 0; i < themeAndWeight.length; i += 2) w.put((String) themeAndWeight[i], ((Number) themeAndWeight[i + 1]).doubleValue());
        return new FactionDef(id, id, 0xFFFFFF, "nusmp:mega/" + id, List.of(), List.of(), FactionDef.UNSET, FactionDef.UNSET, false, false, w, "raw_monster_meat", 0.5);
    }

    static FactionDef dropper(String id, String cut, double chance) {
        return new FactionDef(id, id, 0xFFFFFF, "nusmp:mega/" + id, List.of(), List.of(), FactionDef.UNSET, FactionDef.UNSET, false, false, Map.of(), cut, chance);
    }
}
