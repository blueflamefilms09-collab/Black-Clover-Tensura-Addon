package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Why: nothing of the Saiyan system can be seen by its author in a game, so what a person can not check is checked by the real game: the CI server smoke test boots the mod and runs these two
 * (keys {@code saiyan.rules.bounds}, {@code saiyan.player.roundtrip}).
 * <ul>
 * <li>{@code rules}: the numbers the doc promises (the six rank thresholds, the growth scale, the bonus caps at the hard Battle Power cap, the anti-farm weights, the daily room, the save format) and the
 * config answers inside its bounds.</li>
 * <li>{@code player}: on a stand-in player: becoming a Saiyan puts the modifiers on and taking the race away removes every one; a weak creature gives no Battle Power, a strong one does, the daily cap
 * holds across two hundred kills; the same creature type is remembered; Ki is the magicule (paying takes it, an empty pool refuses); a Ki Blast and a Rapid Ki Barrage take effect, start their cooldown and
 * spawn no entity; death cleans up; everything is removed at the end.</li>
 * </ul>
 * Server safe: no client class is touched. The stand-in has no connection, so no packet is sent.
 */
public final class SaiyanSelfTests {
    private SaiyanSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("saiyan.rules.bounds", SaiyanSelfTests::rules);
        SelfTests.register("saiyan.player.roundtrip", SaiyanSelfTests::player);
    }

    private static boolean near(double a, double b) { return Math.abs(a - b) < 1.0e-6; }

    private static Result rules(MinecraftServer server) {
        if (SaiyanRules.rankOf(100) != SaiyanRules.Rank.LOW_CLASS || SaiyanRules.rankOf(999) != SaiyanRules.Rank.LOW_CLASS || SaiyanRules.rankOf(1_000) != SaiyanRules.Rank.ELITE
                || SaiyanRules.rankOf(9_999) != SaiyanRules.Rank.ELITE || SaiyanRules.rankOf(10_000) != SaiyanRules.Rank.SUPER_ELITE || SaiyanRules.rankOf(100_000) != SaiyanRules.Rank.LEGENDARY
                || SaiyanRules.rankOf(1_000_000) != SaiyanRules.Rank.DIVINE || SaiyanRules.rankOf(10_000_000) != SaiyanRules.Rank.TRANSCENDENT) return Result.fail("the rank thresholds moved");
        if (!near(SaiyanRules.growth(100), 0.0) || !near(SaiyanRules.growth(10_000_000), 1.0) || !near(SaiyanRules.growth(1.0e12), 1.0) || !near(SaiyanRules.growth(Double.NaN), 0.0)) return Result.fail("the growth scale moved");
        if (SaiyanRules.bonus(0.35, 1.0, 1.5, SaiyanRules.BP_HARD_CAP) > 1.5 + 1e-9 || SaiyanRules.bonus(5.0, 10.0, 0.5, SaiyanRules.BP_HARD_CAP) > 0.5 + 1e-9) return Result.fail("a bonus passed its cap");
        if (SaiyanRules.difficultyWeight(1.0, 100.0, 0.25) != 0.0 || !near(SaiyanRules.difficultyWeight(100.0, 100.0, 0.25), 1.0) || SaiyanRules.difficultyWeight(1.0e9, 100.0, 0.25) > 1.5 + 1e-9) return Result.fail("the victim weight moved");
        if (!(SaiyanRules.repeatFactor(10, 5) < SaiyanRules.repeatFactor(1, 5)) || SaiyanRules.repeatFactor(0, 5) != 1.0) return Result.fail("the repeat factor does not fall");
        if (SaiyanRules.killGain(1000, 0.01, 1.0, 1.0, 0.2, 3.0) > 3.0 + 1e-9 || SaiyanRules.dailyRoom(1000, 0.1, 20, 100) != 0.0) return Result.fail("a kill gain or the daily room passed its limit");
        if (SaiyanRules.takenFactor(0.3) >= 1.0 || SaiyanRules.damageFactor(0.35) <= 1.0) return Result.fail("strength or durability does nothing");

        SaiyanRules.Table caps = SaiyanConfig.caps(), top = SaiyanConfig.bonuses(SaiyanRules.BP_HARD_CAP);
        if (top.strength() > caps.strength() + 1e-9 || top.speed() > caps.speed() + 1e-9 || top.durability() > caps.durability() + 1e-9 || top.attackSpeed() > caps.attackSpeed() + 1e-9
                || top.jump() > caps.jump() + 1e-9 || top.stamina() > caps.stamina() + 1e-9 || top.kiCapacity() > caps.kiCapacity() + 1e-9 || top.knockback() > caps.knockback() + 1e-9
                || top.combatXp() > caps.combatXp() + 1e-9) return Result.fail("the bonuses at the highest Battle Power pass the caps");
        if (SaiyanConfig.barrageRays() > 8 || SaiyanConfig.blastRange() > 64.0 || SaiyanConfig.kiBase() < 0.0) return Result.fail("a config bound moved");

        SaiyanState s = new SaiyanState();
        s.saiyan = true;
        s.awakened = true;
        s.stamina = 77.0;
        s.setBattlePower(12_345.0);
        s.noteKill("minecraft:zombie", 500, 12000);
        s.startCooldown("blast", 500, 10);
        SaiyanState back = SaiyanTag.fromTag(SaiyanTag.toTag(s));
        if (!back.saiyan || !back.awakened || !near(back.stamina, 77.0) || !near(back.battlePower, 12_345.0) || back.recentKills("minecraft:zombie", 600, 12000) != 1 || back.ready("blast", 505)) return Result.fail("the save format did not round-trip");
        return Result.ok("ranks, growth scale, caps at the hard Battle Power cap, anti-farm weights, daily room, config bounds and the save format hold");
    }

    private static Result player(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "saiyan", at.x, at.y, at.z);
        try {
            SaiyanManager.setSaiyan(p, true);
            SaiyanState s = SaiyanStore.of(p);
            if (!SaiyanStore.isSaiyan(p) || !SaiyanAttributes.any(p)) return Result.fail("becoming a Saiyan did not set the flag and the modifiers");
            s.awakened = true;
            s.stamina = -1.0;
            s.battlePower = 1000.0;
            s.bpDay = -1;
            s.bpToday = 0.0;
            s.recent.clear();
            s.readyAt.clear();
            long now = level.getGameTime();

            // Battle Power: a weak creature teaches nothing, a strong one does, the day's cap holds, a repeat is remembered
            if (SaiyanManager.credit(p, s, 1.0, "selftest:weak", now) != 0.0) return Result.fail("a creature far below the Saiyan gave Battle Power");
            double first = SaiyanManager.credit(p, s, 5000.0, "selftest:strong", now);
            if (!(first > 0.0)) return Result.fail("a strong creature gave no Battle Power");
            if (s.recentKills("selftest:strong", now, SaiyanConfig.repeatWindow()) < 1) return Result.fail("the kill was not remembered");
            for (int i = 0; i < 200; i++) SaiyanManager.credit(p, s, 1.0e7, "selftest:farm" + (i % 7), now);
            double cap = Math.max(SaiyanConfig.dailyFloor(), s.battlePower * SaiyanConfig.dailyFraction());
            if (s.bpToday > cap + 1.0e-6) return Result.fail("the daily cap was passed: " + s.bpToday + " > " + cap);
            if (s.battlePower > SaiyanConfig.maxBp() + 1.0e-6) return Result.fail("Battle Power passed the configured maximum");

            // Ki is the magicule
            var ex = TensuraStorages.getExistenceFrom(p);
            double max = SaiyanKi.max(p);
            String ki;
            if (ex != null && max > 1.0) {
                ex.setMagicule(max);
                if (!SaiyanKi.pay(p, 10.0) || !(SaiyanKi.current(p) < max)) return Result.fail("paying Ki did not take magicule");
                ex.setMagicule(0.0);
                if (SaiyanKi.pay(p, 10.0)) return Result.fail("an empty Ki pool paid");

                // the two attacks: they take effect, start a cooldown and spawn nothing
                ex.setMagicule(max);
                s.stamina = SaiyanManager.staminaMax(s);
                AABB box = new AABB(at, at).inflate(48);
                int before = level.getEntities((Entity) null, box, e -> true).size();
                if (!SaiyanAbilities.blast(p, s, now) || s.ready("blast", now)) return Result.fail("the Ki Blast did not take effect and start its cooldown");
                if (SaiyanAbilities.blast(p, s, now)) return Result.fail("the Ki Blast ignored its cooldown");
                ex.setMagicule(max);
                s.stamina = SaiyanManager.staminaMax(s);
                if (!SaiyanAbilities.barrage(p, s, now) || s.ready("barrage", now)) return Result.fail("the Rapid Ki Barrage did not take effect and start its cooldown");
                int after = level.getEntities((Entity) null, box, e -> true).size();
                if (after > before) return Result.fail("a Ki attack spawned " + (after - before) + " entities");
                ki = "Ki payment, Ki Blast and Rapid Ki Barrage (cooldowns, no entity spawned)";
            } else {
                ki = "Ki payment and the two attacks NOT exercised (the stand-in has no Tensura magicule data)";
            }

            // death cleans up; taking the race away removes every modifier
            SaiyanManager.onDeath(p);
            if (SaiyanAttributes.any(p) || s.stamina >= 0.0) return Result.fail("death left a modifier or the stamina behind");
            SaiyanManager.setSaiyan(p, true);
            SaiyanManager.setSaiyan(p, false);
            if (SaiyanStore.isSaiyan(p) || SaiyanAttributes.any(p)) return Result.fail("taking the race away left the flag or a modifier behind");
            return Result.ok("racial modifiers on and off, the weak/strong kill weights, the daily cap, the repeat memory, death cleanup; " + ki);
        } finally {
            SaiyanState s = SaiyanStore.peek(p);
            if (s != null) {
                s.saiyan = false;
                s.awakened = false;
                s.battlePower = SaiyanRules.START_BP;
                s.bpToday = 0.0;
                s.recent.clear();
                s.readyAt.clear();
                s.stamina = -1.0;
            }
            SaiyanManager.onLogout(p);
            SaiyanAttributes.clear(p);
        }
    }
}
