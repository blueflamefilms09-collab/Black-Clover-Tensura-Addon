package com.newuniverse.nusmp.boss.rework;

import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcSpawner;
import com.newuniverse.nusmp.arc.tier.TierSignature;
import com.newuniverse.nusmp.dungeon.floor.charybdis.CharybdisEntities;
import com.newuniverse.nusmp.dungeon.floor.charybdis.CharybdisTable;
import com.newuniverse.nusmp.dungeon.floor.goliath.GoliathEntities;
import com.newuniverse.nusmp.dungeon.floor.goliath.GoliathTable;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.relics.order.OrderEntities;
import com.newuniverse.nusmp.relics.order.OrderTable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: boss rework wave 1 (docs/bosses/rework_wave1.md) appended moves to four kinds of boss, and nobody can watch a fight here, so the CI server smoke test does: key
 * {@code boss.rework1}. It builds (never adds to the world) one Goliath, one Charybdis Spawn, one God of Order and the seven Tier List bosses, and checks on the real
 * entities that every old move still sits at its old index (the client reads by index), that each appended move is there with a telegraph and code when the setting
 * {@code bossRework1Moves} is on and absent when it is off. Server safe; nothing is spawned, so nothing needs removing.
 */
public final class BossReworkSelfTests {
    private BossReworkSelfTests() {}

    public static void register() { SelfTests.register("boss.rework1", BossReworkSelfTests::run); }

    /** Problems of one boss's move book against the ids its table lists ({@code old} of them before the rework). */
    private static void check(String who, ZogratisBoss b, List<String> ids, int old, boolean on, List<String> bad) {
        if (b == null) {
            bad.add(who + " could not be built");
            return;
        }
        for (int i = 0; i < ids.size(); i++) {
            int at = b.moveIndex(ids.get(i));
            boolean wanted = i < old || on;
            if (wanted && at != i) bad.add(who + "/" + ids.get(i) + " is at " + at + ", not " + i);
            if (!wanted && at >= 0) bad.add(who + "/" + ids.get(i) + " is in the list although bossRework1Moves is off");
            Move m = at >= 0 ? b.moveAt(at) : null;
            if (m != null && i >= old && (m.tell() == null || m.run() == null)) bad.add(who + "/" + ids.get(i) + " has no telegraph or no code");
        }
    }

    private static Result run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        boolean on = ReworkFx.moves();
        List<String> bad = new ArrayList<>();
        check("goliath", GoliathEntities.GOLIATH.get().create(level), GoliathTable.specs().stream().map(GoliathTable.Spec::id).toList(), GoliathTable.OLD_MOVES, on, bad);
        check("charybdis_spawn", CharybdisEntities.CHARYBDIS_SPAWN.get().create(level), CharybdisTable.specs().stream().map(CharybdisTable.Spec::id).toList(),
                CharybdisTable.OLD_MOVES, on, bad);
        check("god_of_order", OrderEntities.GOD_OF_ORDER.get().create(level), OrderTable.specs().stream().map(OrderTable.Spec::id).toList(), OrderTable.OLD_MOVES, on, bad);
        int tiers = 0;
        for (String[] row : TierSignature.ROWS) {
            EntityType<? extends ArcBoss> type = ArcSpawner.typeOf(row[0]);
            if (type == null) {
                bad.add(row[0] + " has no entity type");
                continue;
            }
            ArcBoss b = type.create(level);
            int at = b == null ? -2 : b.moveIndex(row[1]);
            if (b == null) bad.add(row[0] + " could not be built");
            else if (on && at < 0) bad.add(row[0] + " lacks its signature move " + row[1]);
            else if (!on && at >= 0) bad.add(row[0] + " has " + row[1] + " although bossRework1Moves is off");
            else tiers++;
        }
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(6, bad.size()))));
        return Result.ok("moves " + (on ? "on" : "off") + ": goliath, charybdis_spawn, god_of_order and " + tiers + " tier bosses keep their old indices"
                + (on ? " and carry the appended moves" : ""));
    }
}
