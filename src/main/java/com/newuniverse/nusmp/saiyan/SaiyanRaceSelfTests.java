package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import io.github.manasmods.manascore.config.ConfigRegistry;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.config.ReincarnationConfig;
import io.github.manasmods.tensura.config.race.RaceConfig;
import io.github.manasmods.tensura.race.template.DefaultRace;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the CI server smoke test of the real Tensura Saiyan race (prefix {@code db.phase2.tensura}, inside the existing {@code db.phase2} smoke prefix).
 * <ul>
 * <li>{@code db.phase2.tensura.rules}: {@code anime_multiverse:saiyan} is in ManasCore's race registry, is a Tensura {@code DefaultRace} with the numbers of {@link SaiyanRaceRules}, has no evolution, survives
 * a save and a load of its instance, and (setting on) is in Tensura's starting-race list after {@link SaiyanRaceLink#listInMenu}.</li>
 * <li>{@code db.phase2.tensura.player}: on a stand-in: the flag on sets the race; Tensura setting Human clears the flag; Tensura setting the Saiyan race sets the flag; the flag off takes the race away.
 * Not exercised (said so) when the stand-in has no ManasCore race storage. Nothing is spawned.</li>
 * </ul>
 */
public final class SaiyanRaceSelfTests {
    private SaiyanRaceSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("db.phase2.tensura.rules", SaiyanRaceSelfTests::rules);
        SelfTests.register("db.phase2.tensura.player", SaiyanRaceSelfTests::player);
    }

    private static boolean near(double a, double b) { return Math.abs(a - b) < 1.0e-9; }

    private static Result rules(MinecraftServer server) {
        ManasRace race = RaceAPI.getRaceRegistry().get(SaiyanRaces.id());
        if (race == null) return Result.fail("anime_multiverse:saiyan is not in ManasCore's race registry");
        if (!(race instanceof DefaultRace d)) return Result.fail("the Saiyan race is not a Tensura DefaultRace");
        RaceConfig.Default c = d.getDefaultConfig();
        if (!near(c.getMaxHealth(), SaiyanRaceRules.raceHealth()) || !near(c.getAttack(), SaiyanRaceRules.raceAttack()) || !near(c.getMovementSpeed(), SaiyanRaceRules.raceMovement())
                || !near(c.getKnockbackResistance(), SaiyanRaceRules.raceKnockback()) || !near(c.getMinAura(), SaiyanRaceRules.RACE_MIN_AURA) || !near(c.getMaxMagicule(), SaiyanRaceRules.RACE_MAX_MAGICULE))
            return Result.fail("the race's numbers are not SaiyanRaceRules'");
        if (!(c.getMinAura() < c.getMaxAura()) || !(c.getMinMagicule() < c.getMaxMagicule())) return Result.fail("an aura or magicule range is empty");
        ManasRaceInstance inst = race.createDefaultInstance();
        if (!SaiyanRaces.id().equals(inst.getRaceId())) return Result.fail("the instance names another race");
        ManasRaceInstance back = ManasRaceInstance.fromNBT(inst.toNBT());
        if (!SaiyanRaces.id().equals(back.getRaceId())) return Result.fail("the race did not survive a save and a load");
        String menu;
        if (SaiyanRaceLink.enabled() && SaiyanPhaseConfig.listInRaceMenu()) {
            SaiyanRaceLink.listInMenu();
            ReincarnationConfig cfg = ConfigRegistry.getConfig(ReincarnationConfig.class);
            if (cfg == null || !cfg.Races.startingRaces.contains(SaiyanRaceRules.RACE_ID)) return Result.fail("the Saiyan is not in Tensura's starting-race list");
            menu = "listed in Tensura's race menu";
        } else {
            menu = "menu listing is off on this server";
        }
        return Result.ok("anime_multiverse:saiyan is registered as a Tensura DefaultRace with the Saiyan numbers, survives a save, " + menu + "; phase.race.tensuraRace = " + SaiyanRaceLink.enabled());
    }

    private static Result player(MinecraftServer server) {
        if (!SaiyanRaceLink.enabled()) return Result.ok("phase.race.tensuraRace is off on this server: the old flag-only path, nothing to link");
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "saiyan_race", at.x, at.y, at.z);
        try {
            SaiyanManager.setSaiyan(p, true);
            if (!SaiyanStore.isSaiyan(p)) return Result.fail("the flag did not go on");
            if (SaiyanRaceLink.current(p) == null) return Result.ok("the link was NOT exercised (the stand-in has no ManasCore race storage)");
            if (!SaiyanRaceLink.holdsRace(p)) return Result.fail("the flag on did not set the Saiyan race");
            RaceAPI.getRaceFrom(p).setRace(ResourceLocation.fromNamespaceAndPath("tensura", "human"), false);
            if (SaiyanRaceLink.holdsRace(p) || SaiyanStore.isSaiyan(p)) return Result.fail("Tensura setting Human left the Saiyan flag on");
            RaceAPI.getRaceFrom(p).setRace(SaiyanRaces.id(), false);
            if (!SaiyanRaceLink.holdsRace(p) || !SaiyanStore.isSaiyan(p)) return Result.fail("Tensura setting the Saiyan race did not set the flag");
            SaiyanManager.setSaiyan(p, false);
            if (SaiyanRaceLink.holdsRace(p) || SaiyanStore.isSaiyan(p)) return Result.fail("the flag off left the Saiyan race on");
            return Result.ok("flag on sets the race, Tensura setting Human clears the flag, Tensura setting the Saiyan race sets it, flag off takes the race away");
        } finally {
            SaiyanState s = SaiyanStore.peek(p);
            if (s != null) {
                s.saiyan = false;
                s.awakened = false;
                s.battlePower = SaiyanRules.START_BP;
                s.stamina = -1.0;
                s.readyAt.clear();
            }
            p.getPersistentData().remove(SaiyanRaceLink.PREV);
            SaiyanManager.onLogout(p);
            SaiyanAttributes.clear(p);
        }
    }
}
