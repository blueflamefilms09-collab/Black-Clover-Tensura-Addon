package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.MoveSpec;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongToIntFunction;

/**
 * A played-out fight for the move tables of Meliodas, Escanor and Ban (test helper, not a test): a decision every 5 ticks through the framework's
 * real {@link MoveScheduler}, the target wandering between 2 and 36 blocks, each move's wind-up (scaled by the power, never under the framework's 8)
 * and recovery respected. The power the scheduler sees may change with time (Escanor's sun holds it down), so it is a function of the tick.
 */
final class SdsaFightSim {
    private SdsaFightSim() {}

    /** Plays {@code ticks} ticks and returns how often each move was started. Fails when an ordinary move is started more than the framework's repeat limit in a row. */
    static Map<String, Integer> play(List<? extends MoveSpec> specs, long seed, int tuningPower, LongToIntFunction powerAt, int ticks) {
        MoveScheduler sch = new MoveScheduler(seed);
        sch.reset(0);
        Map<String, Integer> used = new HashMap<>();
        PowerCurve.Tuning tu = PowerCurve.tuning(tuningPower);
        long busyUntil = 0;
        String prev = "";
        int run = 0;
        for (long t = 20; t < ticks; t += 5) {
            if (t < busyUntil) continue;
            double dist = 2 + 17 * (1 + Math.sin(t / 97.0)) + 2 * Math.sin(t / 13.0);
            MoveScheduler.Situation s = new MoveScheduler.Situation(dist, 0.6f, powerAt.applyAsInt(t), 1, t, Double.POSITIVE_INFINITY, tu.ultimateGapTicks());
            int idx = sch.pick(specs, s);
            if (idx < 0) continue;
            MoveSpec m = specs.get(idx);
            sch.commit(m, s, (int) Math.max(10, Math.round(m.cooldownTicks() * tu.cooldownMultiplier())));
            used.merge(m.id(), 1, Integer::sum);
            busyUntil = t + Math.max(8, Math.round(m.windUpTicks() * tu.windUpMultiplier())) + (m.ultimate() ? 60 : 10);
            if (m.id().equals(prev)) run++;
            else { prev = m.id(); run = 1; }
            if (!m.ultimate() && run > MoveScheduler.REPEAT_LIMIT) throw new AssertionError("move spammed: " + m.id());
        }
        return used;
    }

    /** The same at one fixed power. */
    static Map<String, Integer> play(List<? extends MoveSpec> specs, long seed, int power, int ticks) {
        return play(specs, seed, power, t -> power, ticks);
    }
}
