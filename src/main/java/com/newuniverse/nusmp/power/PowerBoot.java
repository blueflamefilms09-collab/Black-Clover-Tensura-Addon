package com.newuniverse.nusmp.power;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: one entry point for the 3.74.0 power raise (docs/power_raise.md): the server config {@code nusmp-power-server.toml}, the damage hook and the CI self-test {@code power.rules}.
 * Safe to call twice; names no client class.
 */
public final class PowerBoot {
    private PowerBoot() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.SERVER, PowerConfig.SPEC, "nusmp-power-server.toml");
        PowerEvents.register(NeoForge.EVENT_BUS);
        SelfTests.register("power.rules", server -> {
            if (PowerRules.factor(25, 25, 25) != 25.0) return Result.fail("three systems at once multiplied each other");
            if (PowerRules.factor(1, 1, 1) != 1.0 || PowerRules.factor(-3, Double.NaN, 0) != 1.0) return Result.fail("an inactive system changed the blow");
            if (PowerRules.dealt(10f, 25, false, 20f, 0.4) != 250f) return Result.fail("a mob did not take 25 times the blow");
            if (PowerRules.dealt(10f, 25, true, 20f, 0.4) != 10f) return Result.fail("a blow already over the PvP cap was changed");
            if (PowerRules.dealt(2f, 25, true, 20f, 0.4) != 8f) return Result.fail("a player took more than the PvP cap from a raised blow");
            if (PowerRules.taken(10f, 0.5) != 5f || PowerRules.taken(10f, 7) != 10f) return Result.fail("the damage a wielder takes left [0, as it was]");
            if (PowerRules.legendary(25, 0.2) < 25 * PowerRules.MIN_LEGENDARY_MARGIN - 1e-9 || PowerConfig.legendary() <= PowerConfig.vassal() && PowerConfig.vassal() > 1.0) return Result.fail("a Vassal Weapon could pass a Legendary one");
            return Result.ok("power: legendary x" + PowerConfig.legendary() + ", djinn x" + PowerConfig.djinn() + ", beast will x" + PowerConfig.beastWill() + ", vassal x" + PowerConfig.vassal() + ", largest wins, PvP capped, taken x" + PowerConfig.taken());
        });
    }
}
