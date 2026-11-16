package com.newuniverse.nusmp.dungeon.rework;

import com.newuniverse.nusmp.boss.rules.BossRulesConfig;
import com.newuniverse.nusmp.dungeon.BasiliskEntity;
import com.newuniverse.nusmp.dungeon.DirewolfEntity;
import com.newuniverse.nusmp.dungeon.DungeonEntities;
import com.newuniverse.nusmp.dungeon.LivingArmorEntity;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.labyrinth.seal.FloorWardenEntity;
import com.newuniverse.nusmp.labyrinth.seal.SealRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the dungeon-boss rework (docs/dungeon_bosses_rework.md) changes the Floor Warden and three creatures, and nobody watches a fight in CI, so the server smoke test builds them (never adds them to
 * the world, so nothing needs removing) under key {@code dboss}: the Warden is built and its gate, its budget and its stage numbers match the setting; a drop of health goes through the unsized budget
 * whole (the hooks are wired and null safe); the three creatures build; and the move books are consistent (every index in range, every windup positive, the Warden's worst-case base damage under the
 * ceiling the rules test also holds). Server safe and cheap.
 */
public final class DungeonBossSelfTests {
    private DungeonBossSelfTests() {}

    public static void register() { SelfTests.register("dboss", DungeonBossSelfTests::run); }

    private static Result run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        List<String> bad = new ArrayList<>();
        boolean on = BossRulesConfig.dungeonWarden();
        FloorWardenEntity w = SealRegistry.FLOOR_WARDEN.get().create(level);
        if (w == null) {
            bad.add("the Floor Warden could not be built");
        } else {
            if (w.reworkOn() != on) bad.add("reworkOn() is " + w.reworkOn() + " but dungeonBosses.warden is " + on);
            if (w.dpsBudget() == null) bad.add("the Warden has no damage budget");
            if (w.budgetFinalStage() != (on ? 2 : 0)) bad.add("budgetFinalStage is " + w.budgetFinalStage());
            float before = w.getMaxHealth();
            w.setHealth(before * 0.4f);
            if (Math.abs(w.getHealth() - before * 0.4f) > 0.5f) bad.add("setHealth through the budget gave " + w.getHealth() + " of " + before);
        }
        DirewolfEntity wolf = DungeonEntities.DIREWOLF.get().create(level);
        BasiliskEntity basilisk = DungeonEntities.BASILISK.get().create(level);
        LivingArmorEntity armor = DungeonEntities.LIVING_ARMOR.get().create(level);
        if (wolf == null || basilisk == null || armor == null) bad.add("a creature could not be built");
        for (int kind = 0; kind < DungeonReworkRules.BeastKind.values().length; kind++) {
            for (int id : DungeonReworkRules.beastMoves(DungeonReworkRules.BeastKind.values()[kind])) {
                if (id < 0 || id >= DungeonReworkRules.BEASTS.size()) bad.add("beast move index " + id + " is out of range");
            }
        }
        for (DungeonReworkRules.Move m : DungeonReworkRules.WARDEN) if (m.windup() <= 0 || m.cooldown() < DungeonReworkRules.MIN_COOLDOWN) bad.add(m.id() + " has no wind-up or too short a cooldown");
        double worst = DungeonReworkRules.worstCaseBasePerSecond(DungeonReworkRules.WARDEN, 3, true);
        if (worst > 14.0) bad.add("the Warden's worst-case base damage is " + worst + " per second");
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(6, bad.size()))));
        return Result.ok("warden rework " + (on ? "on" : "off") + ", " + DungeonReworkRules.WARDEN.size() + " warden moves, creatures " + (BossRulesConfig.dungeonCreatures() ? "on" : "off")
                + ", worst case " + String.format(java.util.Locale.ROOT, "%.1f", worst) + " base damage per second");
    }
}
