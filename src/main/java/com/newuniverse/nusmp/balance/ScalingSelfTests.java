package com.newuniverse.nusmp.balance;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;

/**
 * Why: the late-game scaling and the grimoire buff (3.90) change every addon hit and nobody can watch that in a game here, so the CI server smoke test checks them: key {@code scaling.rules} (the curve
 * on the live SERVER settings: x1 at the baseline, rising, capped) and {@code scaling.live} (two stand-in zombies that never enter the world: a mob caster is not scaled (players only), a balance-law hit is finite and positive, the grimoire multiplier is the setting). Server safe, no client class, nothing is left in the world.
 */
public final class ScalingSelfTests {
    private ScalingSelfTests() {}

    public static void register() {
        SelfTests.register("scaling.rules", s -> rules());
        SelfTests.register("scaling.live", ScalingSelfTests::live);
        SelfTests.register("scaling.leaf", s -> leaf());
    }

    private static Result rules() {
        boolean on = NUConfig.LATE_GAME_SCALING.get();
        double base = NUConfig.LATE_GAME_BASELINE.get(), cap = NUConfig.LATE_GAME_CAP.get();
        if (LateGameScaling.factor(on, base, base, cap) != 1.0) return Result.fail("the baseline is not x1");
        if (LateGameScaling.factor(false, base * 1e6, base, cap) != 1.0) return Result.fail("off is not x1");
        double a = LateGameScaling.factor(on, base * 10, base, cap), b = LateGameScaling.factor(on, base * 1e9, base, cap);
        if (a < 1.0 || b < a || b > cap) return Result.fail("the curve is not rising and capped: " + a + " / " + b);
        return Result.ok("lateGameScaling=" + on + ", x" + a + " at baseline x10, x" + b + " at the top (cap " + cap + ")");
    }

    /** 4.83.0 leaf power: the ladder on the live SERVER settings is rising (three-leaf x1, four, five, black, god) and never below x1. */
    private static Result leaf() {
        double three = com.newuniverse.nusmp.grimoire.GrimoireLeafPower.factor(com.newuniverse.nusmp.blackclover.GrimoireCover.THREE_LEAF);
        double four = com.newuniverse.nusmp.grimoire.GrimoireLeafPower.factor(com.newuniverse.nusmp.blackclover.GrimoireCover.FOUR_LEAF);
        double five = com.newuniverse.nusmp.grimoire.GrimoireLeafPower.factor(com.newuniverse.nusmp.blackclover.GrimoireCover.FIVE_LEAF);
        double black = com.newuniverse.nusmp.grimoire.GrimoireLeafPower.factor(com.newuniverse.nusmp.blackclover.GrimoireCover.BLACK_MAGIC);
        double god = com.newuniverse.nusmp.grimoire.GrimoireLeafPower.factor(com.newuniverse.nusmp.blackclover.GrimoireCover.GOD_TIER);
        if (three != 1.0) return Result.fail("the three-leaf is not x1: " + three);
        if (!(four >= 1.0 && five >= four && black >= five && god >= black)) return Result.fail("the ladder does not rise: " + four + " / " + five + " / " + black + " / " + god);
        return Result.ok("leafPower=" + NUConfig.LEAF_POWER.get() + ": x" + three + " / x" + four + " / x" + five + " / x" + black + " / x" + god);
    }

    private static Result live(MinecraftServer server) {
        Zombie caster = EntityType.ZOMBIE.create(server.overworld()), target = EntityType.ZOMBIE.create(server.overworld());
        if (caster == null || target == null) return Result.fail("no stand-in zombie");
        try {
            double f = BalanceLaw.lateGameScale(caster);
            if (f != 1.0) return Result.fail("a mob caster is scaled: x" + f);
            float hit = BalanceLaw.damage(caster, target, 10f, 0.5);
            if (!Float.isFinite(hit) || hit <= 0f) return Result.fail("a balance-law hit is " + hit);
            if (Math.abs(BalanceLaw.grimoireMult() - NUConfig.GRIMOIRE_DAMAGE_MULT.get()) > 1e-6) return Result.fail("the grimoire multiplier is not the setting");
            return Result.ok("a mob caster stays x1, raw 10 hits for " + hit + ", grimoire x" + BalanceLaw.grimoireMult());
        } finally {
            caster.discard();
            target.discard();
        }
    }
}
