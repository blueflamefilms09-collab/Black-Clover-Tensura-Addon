package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the CI server smoke test of the 3.71.0 techniques (prefix {@code saiyan.tech3} in SMOKE_EXPECT).
 * <ul>
 * <li>{@code saiyan.tech3.rules}: the numbers the doc promises (dragon unlock and step cap, bind time with the boss and player share, deflection speed cap, energy received and its cap) and the hard bounds of the settings.</li>
 * <li>{@code saiyan.tech3.player}: on a stand-in Saiyan: God Bind is refused without the Son Goku Ultimate; Ki Deflection opens, starts its cool-down and closes; Dragon Fist is refused while the martial training is
 * below the unlock and, once unlocked and with a zombie in front, runs its steps and ends; God Bind holds the zombie still and lets it go; Energy Transfer refuses with no ally; and none of it spawns an entity.</li>
 * </ul>
 */
public final class SaiyanTech3SelfTests {
    private SaiyanTech3SelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("saiyan.tech3.rules", SaiyanTech3SelfTests::rules);
        SelfTests.register("saiyan.tech3.player", SaiyanTech3SelfTests::player);
    }

    private static boolean near(double a, double b) { return Math.abs(a - b) < 1.0e-6; }

    private static Result rules(MinecraftServer server) {
        if (SaiyanTech3Rules.dragonUnlocked(5.0, 15.0) || !SaiyanTech3Rules.dragonUnlocked(15.0, 15.0) || SaiyanTech3Rules.dragonSteps(1000) != SaiyanTech3Rules.MAX_DRAGON_STEPS || SaiyanTech3Rules.dragonSteps(0) != 2
                || !near(SaiyanTech3Rules.dragonStepFactor(5, 6), 3.0) || !near(SaiyanTech3Rules.dragonStepFactor(0, 6), 0.0)) return Result.fail("the Dragon Fist numbers moved");
        if (SaiyanTech3Rules.bindTicks(6.0, false, false, 0.25, 0.5) != 120 || SaiyanTech3Rules.bindTicks(6.0, true, false, 0.25, 0.5) != 30 || SaiyanTech3Rules.bindTicks(6.0, true, true, 0.25, 0.5) != 30
                || SaiyanTech3Rules.bindTicks(6.0, false, true, 0.25, 0.5) != 60 || SaiyanTech3Rules.bindTicks(1.0e6, false, false, 1.0, 1.0) != SaiyanTech3Rules.MAX_BIND_TICKS) return Result.fail("the God Bind time numbers moved");
        if (SaiyanTech3Rules.deflectSpeed(100.0, 3.0) > SaiyanTech3Rules.MAX_DEFLECT_SPEED + 1.0e-9 || !(SaiyanTech3Rules.deflectSpeed(0.0, 1.0) >= 0.3)) return Result.fail("the Ki Deflection speed bound moved");
        if (!near(SaiyanTech3Rules.receivedPercent(1000.0, 20.0, 0.8, 1000.0, 30.0), 16.0) || !near(SaiyanTech3Rules.receivedPercent(1000.0, 100.0, 1.0, 100.0, 30.0), 30.0) || SaiyanTech3Rules.receivedPercent(1000.0, 20.0, 0.8, 0.0, 30.0) != 0.0)
            return Result.fail("the Energy Transfer numbers moved");
        if (SaiyanConfig.dragonSteps() > SaiyanTech3Rules.MAX_DRAGON_STEPS || SaiyanConfig.bindSeconds() * 20.0 > SaiyanTech3Rules.MAX_BIND_TICKS + 1.0e-9 || SaiyanConfig.deflectRadius() > 8.0 + 1.0e-9 || SaiyanConfig.energyLoss() > 0.9 + 1.0e-9)
            return Result.fail("a technique setting passed its hard bound");
        return Result.ok("Dragon Fist unlock and step cap, God Bind time with boss and player share, Ki Deflection speed cap, Energy Transfer loss and cap, and the hard bounds of the settings hold");
    }

    private static Result player(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "saiyan5", at.x, at.y, at.z);
        Zombie foe = null;
        try {
            SaiyanManager.setSaiyan(p, true);
            SaiyanState s = SaiyanStore.of(p);
            SaiyanManager.Run r = SaiyanManager.runOf(p);
            long now = level.getGameTime();
            s.awakened = true;
            s.battlePower = 5000.0;
            var ex = TensuraStorages.getExistenceFrom(p);
            double max = SaiyanKi.max(p);
            if (ex == null || !(max > 1.0)) return Result.ok("the techniques were NOT exercised (the stand-in has no Tensura magicule data)");
            ex.setMagicule(max);
            s.stamina = SaiyanManager.staminaMax(s);
            s.readyAt.clear();

            // Ki Deflection: opens, cools down, closes with its time
            if (!SaiyanTechniques3.deflect(p, s, r, now) || r.tech3.deflectUntil <= now) return Result.fail("the Ki Deflection did not open");
            if (SaiyanTechniques3.deflect(p, s, r, now) || s.ready("deflect", now)) return Result.fail("the Ki Deflection ignored its cool-down");
            SaiyanTechniques3.tick(p, s, r, r.tech3.deflectUntil);
            if (r.tech3.deflectUntil != 0) return Result.fail("the Ki Deflection never closed");

            // Energy Transfer: nobody near, nothing changes
            s.readyAt.clear();
            double kiBefore = SaiyanKi.current(p);
            if (SaiyanTechniques3.energy(p, s, r, now) || SaiyanKi.current(p) < kiBefore - 1.0e-6 || !s.ready("energy", now)) return Result.fail("an Energy Transfer with no ally took Ki or started a cool-down");

            // God Bind needs the Ultimate
            s.readyAt.clear();
            if (SaiyanTechniques3.bind(p, s, r, now)) return Result.fail("God Bind worked without the Son Goku Ultimate");
            s.ultimate = SaiyanUltimates.GOKU;

            AABB box = new AABB(at, at).inflate(48);
            foe = EntityType.ZOMBIE.create(level);
            if (foe == null) return Result.fail("could not make a test zombie");
            foe.setPos(p.getX(), p.getY(), p.getZ() + 2.5);
            foe.setNoAi(true);
            foe.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000.0);
            foe.setHealth(2000f);
            level.addFreshEntity(foe);
            int before = level.getEntities((Entity) null, box, e -> true).size();
            p.setYRot(0f);
            p.setXRot(0f);

            // God Bind: holds the zombie, lets it go
            ex.setMagicule(max);
            String bind;
            if (SaiyanTechniques3.bind(p, s, r, now)) {
                if (r.tech3.bindTarget == null || s.ready("bind", now) || s.uses("bind") != 1) return Result.fail("God Bind did not hold the foe or start its cool-down");
                foe.setDeltaMovement(1.0, 0.0, 1.0);
                SaiyanTechniques3.tick(p, s, r, now + 1);
                if (Math.abs(foe.getDeltaMovement().x) > 1.0e-6) return Result.fail("God Bind did not hold the foe still");
                SaiyanTechniques3.tick(p, s, r, r.tech3.bindUntil);
                if (r.tech3.bindTarget != null) return Result.fail("God Bind never let go");
                bind = "God Bind (hold, release, cool-down)";
            } else {
                bind = "God Bind refused (no foe lock in this world)";
            }

            // Dragon Fist: refused while untrained, runs and ends once unlocked
            s.readyAt.clear();
            ex.setMagicule(max);
            s.stamina = SaiyanManager.staminaMax(s);
            s.training.clear();
            if (SaiyanTechniques3.dragon(p, s, r, now)) return Result.fail("Dragon Fist worked without the martial training");
            s.training.put(SaiyanTraining.Cat.MARTIAL.id(), SaiyanConfig.dragonUnlock() + 1.0);
            String dragon;
            if (SaiyanTechniques3.dragon(p, s, r, now)) {
                float hp = foe.getHealth();
                long t = now;
                for (int i = 0; i < 40 && r.tech3.dragonTarget != null && foe.isAlive(); i++) SaiyanTechniques3.tick(p, s, r, t += 2);
                if (r.tech3.dragonTarget != null && foe.isAlive()) return Result.fail("Dragon Fist did not run out its steps");
                if (!(foe.getHealth() < hp) && foe.isAlive()) return Result.fail("Dragon Fist struck for nothing");
                if (s.ready("dragon", now) || s.uses("dragon") != 1) return Result.fail("Dragon Fist did not start its cool-down");
                dragon = "Dragon Fist (unlock, steps, blow, cool-down)";
            } else {
                dragon = "Dragon Fist refused (no foe lock in this world)";
            }
            int after = level.getEntities((Entity) null, box, e -> true).size();
            if (after > before) return Result.fail("a technique spawned " + (after - before) + " entities");
            return Result.ok("Ki Deflection (open, cool-down, close), Energy Transfer (no ally), " + bind + ", " + dragon + "; no entity spawned");
        } finally {
            if (foe != null) foe.discard();
            SaiyanState s = SaiyanStore.peek(p);
            if (s != null) {
                s.saiyan = false;
                s.awakened = false;
                s.battlePower = SaiyanRules.START_BP;
                s.uses.clear();
                s.training.clear();
                s.ultimate = "";
                s.readyAt.clear();
                s.stamina = -1.0;
            }
            SaiyanManager.onLogout(p);
            SaiyanAttributes.clear(p);
        }
    }
}
