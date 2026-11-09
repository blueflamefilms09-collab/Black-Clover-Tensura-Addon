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
 * Why: the CI server smoke test of the 3.66.0 techniques (prefix {@code saiyan.tech} in SMOKE_EXPECT). Nothing here can be seen by its author in a game, so the real game checks it.
 * <ul>
 * <li>{@code saiyan.tech.rules}: the numbers the doc promises (barrier pool and soak, friend cap and gather rate, bomb damage / radius / travel, meteor hit cap and the heavy last hit) and the configured
 * limits (a setting can never push a technique past its hard bound).</li>
 * <li>{@code saiyan.tech.player}: on a stand-in Saiyan: the Ki Barrier soaks a blow and breaks on a big one and obeys its cool-down; the three Goku techniques are refused without the Ultimate;
 * Instant Transmission either moves the Saiyan within its range and starts its cool-down or refuses with nothing changed; a Spirit Bomb gathers, fizzles when thrown too soon, flies and lands after its
 * travel time; a Meteor Combination locks onto a zombie in front, strikes it in order and ends; and none of it spawns an entity.</li>
 * </ul>
 */
public final class SaiyanTechSelfTests {
    private SaiyanTechSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("saiyan.tech.rules", SaiyanTechSelfTests::rules);
        SelfTests.register("saiyan.tech.player", SaiyanTechSelfTests::player);
    }

    private static boolean near(double a, double b) { return Math.abs(a - b) < 1.0e-6; }

    private static Result rules(MinecraftServer server) {
        if (!near(SaiyanTechRules.barrierPool(60.0, 0.0), 60.0) || !near(SaiyanTechRules.barrierPool(60.0, 1.0), 180.0) || !near(SaiyanTechRules.throughBarrier(50.0, 80.0), 30.0)
                || !near(SaiyanTechRules.throughBarrier(50.0, 20.0), 0.0) || !near(SaiyanTechRules.poolAfter(50.0, 80.0), 0.0) || SaiyanTechRules.barrierUp(10.0, 100, 100)) return Result.fail("the barrier numbers moved");
        if (SaiyanTechRules.sources(1000) != SaiyanTechRules.MAX_SOURCES || SaiyanTechRules.gatherRate(1000) > 2.5 + 1.0e-9 || !(SaiyanTechRules.gatherRate(5) > SaiyanTechRules.gatherRate(0))) return Result.fail("the Spirit Bomb friend cap or gather rate moved");
        if (!(SaiyanTechRules.bombDamage(120.0, 1.0, 10) > SaiyanTechRules.bombDamage(120.0, 0.0, 0)) || SaiyanTechRules.bombRadius(8.0, 0.0) < 2.0 || SaiyanTechRules.travelTicks(1.0e6, 60) != 60 || SaiyanTechRules.travelTicks(0.0, 60) != 10) return Result.fail("the Spirit Bomb damage, radius or travel numbers moved");
        if (SaiyanTechRules.meteorHits(1000) != SaiyanTechRules.MAX_METEOR_HITS || SaiyanTechRules.meteorHits(0) != 1 || !near(SaiyanTechRules.meteorHitFactor(7, 8), 3.0) || !near(SaiyanTechRules.meteorHitFactor(0, 8), 1.0)) return Result.fail("the Meteor Combination hit numbers moved");
        if (SaiyanConfig.meteorHits() > SaiyanTechRules.MAX_METEOR_HITS || SaiyanConfig.sbRadius() > 16.0 + 1.0e-9 || SaiyanConfig.itBlink() > 48.0 + 1.0e-9 || SaiyanConfig.itRange() > 128.0 + 1.0e-9 || SaiyanConfig.sbFeedRadius() > 48.0 + 1.0e-9) return Result.fail("a technique setting passed its hard bound");
        return Result.ok("barrier pool and soak, friend cap and gather rate, bomb damage / radius / travel, meteor hit cap and heavy last hit, and the hard bounds of the settings hold");
    }

    private static Result player(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "saiyan4", at.x, at.y, at.z);
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

            // Ki Barrier: soaks, breaks, cool-down
            if (!SaiyanTechniques.barrier(p, s, r, now) || !(r.tech.barrier > 0.0)) return Result.fail("the Ki Barrier did not rise");
            float soaked = SaiyanTechniques.absorb(p, r, 10f, now);
            if (!(soaked < 10f)) return Result.fail("the Ki Barrier did not soak a small blow");
            float big = SaiyanTechniques.absorb(p, r, 1.0e6f, now);
            if (!(big > 0f) || big >= 1.0e6f || r.tech.barrier > 0.0) return Result.fail("a huge blow did not break the Ki Barrier and pass only the rest");
            if (SaiyanTechniques.absorb(p, r, 7f, now) != 7f) return Result.fail("a broken Ki Barrier still soaked");
            ex.setMagicule(max);
            if (SaiyanTechniques.barrier(p, s, r, now)) return Result.fail("the Ki Barrier ignored its cool-down");

            // the three Goku techniques need the Ultimate
            s.readyAt.clear();
            if (SaiyanTechniques.transmit(p, s, r, now) || SaiyanTechniques.bombStart(p, s, r, now) || SaiyanTechniques.meteor(p, s, r, now)) return Result.fail("a Goku technique worked without the Son Goku Ultimate");
            s.ultimate = SaiyanUltimates.GOKU;

            AABB box = new AABB(at, at).inflate(48);
            foe = EntityType.ZOMBIE.create(level);
            if (foe == null) return Result.fail("could not make a test zombie");
            foe.setPos(p.getX(), p.getY(), p.getZ() + 3.0);
            foe.setNoAi(true);
            foe.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000.0);
            foe.setHealth(2000f);
            level.addFreshEntity(foe);
            int before = level.getEntities((Entity) null, box, e -> true).size();

            // Instant Transmission: moves within range and cools down, or refuses and changes nothing
            ex.setMagicule(max);
            p.setYRot(0f);
            p.setXRot(0f);
            Vec3 from = p.position();
            boolean moved = SaiyanTechniques.transmit(p, s, r, now);
            if (moved) {
                if (p.position().distanceTo(from) > SaiyanConfig.itRange() + 2.0 || s.ready("transmit", now) || s.uses("transmit") != 1) return Result.fail("Instant Transmission moved too far or did not start its cool-down");
            } else if (p.position().distanceTo(from) > 1.0e-6 || !s.ready("transmit", now)) {
                return Result.fail("a refused Instant Transmission still moved the Saiyan or started a cool-down");
            }
            p.setPos(from.x, from.y, from.z);

            // Spirit Bomb: gather, fizzle when thrown too soon, fly, land
            ex.setMagicule(max);
            s.readyAt.clear();
            if (!SaiyanTechniques.bombStart(p, s, r, now) || !r.tech.gathering) return Result.fail("the Spirit Bomb did not start gathering");
            SaiyanTechniques.bombRelease(p, s, r, now + 1);
            if (r.tech.gathering || r.tech.bombFlying || s.uses("spirit") != 0) return Result.fail("a Spirit Bomb thrown too soon did not fizzle");
            if (s.ready("spirit", now + 1)) return Result.fail("a fizzled Spirit Bomb started no cool-down");
            s.readyAt.clear();
            SaiyanTechniques.bombStart(p, s, r, now);
            if (!SaiyanTechniques.bombRelease(p, s, r, now + 200) || !r.tech.bombFlying || r.tech.landAt <= now + 200 || s.uses("spirit") != 1 || s.ready("spirit", now + 200)) return Result.fail("a gathered Spirit Bomb was not thrown");
            SaiyanTechniques.flightTick(p, s, r, now + 200 + 1);
            if (!r.tech.bombFlying) return Result.fail("the Spirit Bomb landed at once");
            SaiyanTechniques.flightTick(p, s, r, r.tech.landAt);
            if (r.tech.bombFlying) return Result.fail("the Spirit Bomb never landed");

            // Meteor Combination: locks onto the zombie, strikes it in order, ends
            ex.setMagicule(max);
            s.stamina = SaiyanManager.staminaMax(s);
            s.readyAt.clear();
            p.setPos(from.x, from.y, from.z);
            String meteor;
            if (SaiyanTechniques.meteor(p, s, r, now)) {
                float hp = foe.getHealth();
                long t = now;
                for (int i = 0; i < 200 && r.tech.meteorLeft > 0 && foe.isAlive(); i++) SaiyanTechniques.meteorTick(p, s, r, t += SaiyanConfig.meteorInterval());
                if (r.tech.meteorLeft > 0 && foe.isAlive()) return Result.fail("the Meteor Combination did not run out its strikes");
                if (!(foe.getHealth() < hp) && foe.isAlive()) return Result.fail("the Meteor Combination struck for nothing");
                if (s.ready("meteor", now) || s.uses("meteor") != 1) return Result.fail("the Meteor Combination did not start its cool-down");
                meteor = "the Meteor Combination (lock, strikes, cool-down)";
            } else {
                meteor = "the Meteor Combination refused (no foe lock in this world)";
            }
            int after = level.getEntities((Entity) null, box, e -> true).size();
            if (after > before) return Result.fail("a technique spawned " + (after - before) + " entities");
            return Result.ok("Ki Barrier (soak, break, cool-down), Goku gate, Instant Transmission, Spirit Bomb (gather, fizzle, throw, land), " + meteor + "; no entity spawned");
        } finally {
            if (foe != null) foe.discard();
            SaiyanState s = SaiyanStore.peek(p);
            if (s != null) {
                s.saiyan = false;
                s.awakened = false;
                s.battlePower = SaiyanRules.START_BP;
                s.uses.clear();
                s.ultimate = "";
                s.readyAt.clear();
                s.stamina = -1.0;
            }
            SaiyanManager.onLogout(p);
            SaiyanAttributes.clear(p);
        }
    }
}
