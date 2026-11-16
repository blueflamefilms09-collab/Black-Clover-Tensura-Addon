package com.newuniverse.nusmp.boss.rework3;

import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcSpawner;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialFamilies;
import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialRules;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: boss rework wave 3 (docs/bosses/rework_wave3.md) appends one move to 16 arc bosses and gives each of the ten Decennial tier families a signature, and nobody can watch a
 * fight here, so the CI server smoke test does: key {@code boss.rework3}. It builds (never adds to the world) the 16 bosses and checks on the real entities that the appended move
 * is last in the list with a telegraph and code when {@code bossRework3Moves} is on and absent when it is off, that every old move kept its index (the first move of each is still
 * where it was), that every row names a real {@code VfxShape}, and that all 200 Decennial levels have a family whose numbers are inside the bounds. Server safe; nothing is spawned.
 */
public final class Rework3SelfTests {
    private Rework3SelfTests() {}

    public static void register() { SelfTests.register("boss.rework3", Rework3SelfTests::run); }

    private static Result run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        boolean on = Rework3Moves.on();
        List<String> bad = new ArrayList<>();
        int built = 0;
        for (Rework3Rows.Row row : Rework3Rows.ROWS) {
            String problem = Rework3Rows.problem(row);
            if (problem != null) bad.add(problem);
            try {
                VfxShape.valueOf(row.shape());
            } catch (IllegalArgumentException unknown) {
                bad.add(row.boss() + " names the unknown shape " + row.shape());
            }
            EntityType<? extends ArcBoss> type = ArcSpawner.typeOf(row.boss());
            if (type == null) {
                bad.add(row.boss() + " has no entity type");
                continue;
            }
            ArcBoss b = type.create(level);
            if (b == null) {
                bad.add(row.boss() + " could not be built");
                continue;
            }
            int at = b.moveIndex(row.move().id());
            if (on && at < 1) bad.add(row.boss() + " lacks " + row.move().id() + " (index " + at + ")");
            else if (!on && at >= 0) bad.add(row.boss() + " has " + row.move().id() + " although bossRework3Moves is off");
            else if (on) {
                var m = b.moveAt(at);
                if (m == null || m.tell() == null || m.run() == null) bad.add(row.boss() + "/" + row.move().id() + " has no telegraph or no code");
                else if (b.moveAt(at + 1) != null) bad.add(row.boss() + "/" + row.move().id() + " is not the last move");
                else built++;
            } else built++;
        }
        int families = 0;
        for (int l = -DecennialRules.LEVELS_PER_SIDE; l <= DecennialRules.LEVELS_PER_SIDE; l++) {
            if (l == 0) continue;
            DecennialFamilies.Family f = DecennialFamilies.of(l);
            if (f == null) {
                bad.add("level " + l + " has no tier family");
                continue;
            }
            try {
                VfxShape.valueOf(f.shape());
            } catch (IllegalArgumentException unknown) {
                bad.add(f.id() + " names the unknown shape " + f.shape());
            }
            if (f.share() > DecennialFamilies.MAX_SHARE || f.radius() > DecennialFamilies.MAX_RADIUS) bad.add(f.id() + " is out of bounds");
            families++;
        }
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(6, bad.size()))));
        return Result.ok("moves " + (on ? "on" : "off") + ": " + built + " arc bosses keep their old indices" + (on ? " and carry the appended move last" : "") + "; " + families
                + " decennial levels have a tier family");
    }
}
