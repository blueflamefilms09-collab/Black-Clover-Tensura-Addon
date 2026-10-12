package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.city.CityCursor;
import com.newuniverse.nusmp.dungeon.city.CityNexusBridge;
import com.newuniverse.nusmp.dungeon.city.CityPlan;
import com.newuniverse.nusmp.dungeon.nexus.NexusCursor;
import com.newuniverse.nusmp.dungeon.nexus.NexusSink;
import com.newuniverse.nusmp.dungeon.nexus.StageSequence;
import com.newuniverse.nusmp.dungeon.nexus.ZonePlan;
import com.newuniverse.nusmp.dungeon.nexus.ZoneStage;
import net.minecraft.server.level.ServerLevel;

/**
 * The two stages that follow the garden's own ops (stage 0 of the Nexus build): the old Golden City, built by the city's own cursor and
 * ops (dungeon/city/CityPlan, written through {@link CityNexusBridge}), and then the Treasury cut under its plaza ({@link GoldenTreasury}).
 * The order matters: the city's carve, floor and walls overwrite whatever the garden put in its oval, and the Treasury is cut into the
 * city's floor. Both are idempotent op by op, so a restart simply replays a few ops.
 */
final class GoldenStages {
    private GoldenStages() {}

    /** The old city as stage 1. */
    static ZoneStage city(int cellX, int cellZ) {
        return new ZoneStage() {
            @Override
            public String name() { return "the old Golden City"; }

            @Override
            public StageSequence.Stage begin(ServerLevel level, int savedOp, long savedCell) {
                CityPlan plan = CityNexusBridge.plan(cellX, cellZ);
                CityCursor cursor = savedOp <= 0 && savedCell <= 0L ? new CityCursor(plan, 0, 0L) : CityCursor.resume(plan, savedOp);
                CityCursor.Sink sink = CityNexusBridge.sink(level);
                return new StageSequence.Stage() {
                    @Override
                    public long run(long budget) { return cursor.run(sink, budget); }

                    @Override
                    public boolean done() { return cursor.done(); }

                    @Override
                    public int op() { return cursor.op(); }

                    @Override
                    public long cell() { return cursor.cell(); }
                };
            }
        };
    }

    /** The Treasury as stage 2. */
    static ZoneStage treasury(int cellX, int cellZ) {
        return new ZoneStage() {
            @Override
            public String name() { return "the Treasury under the plaza"; }

            @Override
            public StageSequence.Stage begin(ServerLevel level, int savedOp, long savedCell) {
                ZonePlan plan = GoldenCityPlan.treasury(cellX, cellZ);
                NexusCursor cursor = savedOp <= 0 && savedCell <= 0L ? new NexusCursor(plan, 0, 0L) : NexusCursor.resume(plan, savedOp);
                NexusSink sink = new NexusSink(level);
                return new StageSequence.Stage() {
                    @Override
                    public long run(long budget) { return cursor.run(sink, budget); }

                    @Override
                    public boolean done() { return cursor.done(); }

                    @Override
                    public int op() { return cursor.op(); }

                    @Override
                    public long cell() { return cursor.cell(); }
                };
            }
        };
    }
}
