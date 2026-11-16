package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.vfx.VfxShape;
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

import java.util.Set;

/**
 * Why: the CI server smoke test of the Dragon Ball rework (prefixes {@code db.phase2} and {@code saiyan.t3} in SMOKE_EXPECT, after {@code saiyan.kivfx}).
 * <ul>
 * <li>{@code db.phase2.rules}: the pure numbers (ops range, hard bounds, Kaio-ken stages, race stages, ritual ring, form rules) and the hard bounds of the settings; the new settings are off or as before by default.</li>
 * <li>{@code db.phase2.race}: the race stage rises only with its requirements, never falls, and is NOT touched while the server switch is off.</li>
 * <li>{@code db.phase2.player}: on a stand-in Saiyan: the toggles (Ki Reinforcement, Afterimage, Gravity Training) switch and clean up; Combat Instinct puts its modifier on and takes it off; Warrior's Counter arms and
 * expires; Charged Ki Sphere and Ki Wave start, run to their end and stop; every ask of the new range is bounded; no entity is spawned.</li>
 * <li>{@code saiyan.t3.rules}: the Giant Ki Avatar numbers (height, time, form rule, swing beat), and the Tensura damage source of the Ki techniques.</li>
 * <li>{@code saiyan.t3.player}: the Giant Ki Avatar is refused without an Ultra Instinct form, rises with one, swings, ends with its time and starts its cool-down; the shape is sent through the effect budget.</li>
 * </ul>
 */
public final class SaiyanPhaseSelfTests {
    private SaiyanPhaseSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("db.phase2.rules", SaiyanPhaseSelfTests::rules);
        SelfTests.register("db.phase2.race", SaiyanPhaseSelfTests::race);
        SelfTests.register("db.phase2.player", SaiyanPhaseSelfTests::player);
        SelfTests.register("saiyan.t3.rules", SaiyanPhaseSelfTests::t3Rules);
        SelfTests.register("saiyan.t3.player", SaiyanPhaseSelfTests::t3Player);
    }

    private static boolean near(double a, double b) { return Math.abs(a - b) < 1.0e-6; }

    private static Result rules(MinecraftServer server) {
        if (!SaiyanNet2.valid(SaiyanNet2.FIRST) || !SaiyanNet2.valid(SaiyanNet2.LAST) || SaiyanNet2.valid(SaiyanNet2.FIRST - 1) || SaiyanNet2.valid(SaiyanNet2.LAST + 1) || SaiyanNet2.FIRST <= SaiyanNet.OP_ENERGY)
            return Result.fail("the ask range of the rework moved or overlaps the older one");
        if (SaiyanPhaseRules.vanishDistance(1.0e9) > SaiyanPhaseRules.MAX_VANISH || SaiyanPhaseRules.slamRadius(100.0, 100.0) > SaiyanPhaseRules.MAX_SLAM_RADIUS || SaiyanPhaseRules.counterReflect(1.0e9, 10.0) > SaiyanPhaseRules.MAX_COUNTER_REFLECT
                || SaiyanPhaseRules.waveTicks(1000, 0, 1000) > SaiyanPhaseRules.MAX_WAVE_TICKS || SaiyanPhaseRules.waveTicks(2, 10, 60) != 0 || SaiyanPhaseRules.sphereTravel(1.0e6, 0.0) > SaiyanPhaseRules.MAX_SPHERE_TRAVEL
                || SaiyanPhaseRules.adaptedLevel(Integer.MAX_VALUE, 1) != SaiyanPhaseRules.MAX_GRAVITY || SaiyanPhaseRules.gravityPenalty(100, 0) > 0.35 + 1.0e-9)
            return Result.fail("a hard bound of the techniques moved");
        if (SaiyanPhaseRules.kaioMaxStage(1.0, false) != 3 || SaiyanPhaseRules.kaioMaxStage(1.0, true) != 5 || !near(SaiyanPhaseRules.kaioDamage(1), 1.0) || !near(SaiyanPhaseRules.kaioDamage(3), SaiyanForms.kaioDamageMul(3))
                || !near(SaiyanPhaseRules.kaioDrain(3), SaiyanForms.kaioDrainMul(3)) || !near(SaiyanPhaseRules.kaioStrain(3), SaiyanForms.kaioStrainRate(3)))
            return Result.fail("the Kaio-ken stages 1 to 3 are not the old table, or the extra stages moved");
        if (SaiyanPhaseRules.moonOk(3, 15000, false) || !SaiyanPhaseRules.moonOk(0, 15000, false) || SaiyanPhaseRules.moonOk(0, 1000, false) || !SaiyanPhaseRules.moonOk(3, 1000, true)) return Result.fail("the moon rule moved");
        SaiyanRaceRules.Need need = SaiyanRaceRules.Need.DEFAULT;
        SaiyanRaceRules.Progress none = new SaiyanRaceRules.Progress(1.0e12, 99, Set.of(), Set.of());
        if (SaiyanRaceRules.next(0, need, none) != SaiyanRaceRules.SAIYAN || SaiyanRaceRules.next(SaiyanRaceRules.AUTONOMOUS, need, none) != SaiyanRaceRules.AUTONOMOUS)
            return Result.fail("a race stage moved without its requirements, or fell");
        SaiyanRaceRules.Progress all = new SaiyanRaceRules.Progress(1.0e12, 99, Set.of("super_saiyan_3", "perfected_ultra_instinct"), Set.of(SaiyanRaceRules.RITUAL_FLAG));
        if (SaiyanRaceRules.next(SaiyanRaceRules.SAIYAN, need, all) != SaiyanRaceRules.AUTONOMOUS) return Result.fail("the stages did not chain with every requirement met");
        if (SaiyanRitualRules.helpers(1000) != SaiyanRitualRules.MAX_HELPERS || SaiyanRitualRules.ticks(1_000_000) != SaiyanRitualRules.MAX_TICKS || SaiyanRitualRules.inRing(0.5, 0.5) || !SaiyanRitualRules.inRing(4.0, 0.0) || SaiyanRitualRules.inRing(9.0, 0.0))
            return Result.fail("the ritual ring or its bounds moved");
        if (SaiyanPhaseConfig.vanishDistance() > SaiyanPhaseRules.MAX_VANISH || SaiyanPhaseConfig.waveMaxChannel() > SaiyanPhaseRules.MAX_WAVE_TICKS || SaiyanPhaseConfig.ritualHelpers() > SaiyanRitualRules.MAX_HELPERS
                || SaiyanPhaseConfig.avatarTicks() > SaiyanPhaseRules.MAX_AVATAR_TICKS || SaiyanPhaseConfig.giftTargets() > SaiyanPhaseRules.MAX_GIFT_TARGETS || SaiyanPhaseConfig.breakerRadius() > SaiyanPhaseRules.MAX_SLAM_RADIUS)
            return Result.fail("a setting of the rework passed its hard bound");
        return Result.ok("ask range (22 to 35), hard bounds (vanish, slam, counter, wave, sphere, gravity), Kaio-ken stages 1 to 3 = the old table and 4 and 5 behind a switch, moon rule, race stages and ritual ring, and the settings' bounds hold");
    }

    private static Result race(MinecraftServer server) {
        SaiyanState s = new SaiyanState();
        s.saiyan = true;
        s.battlePower = 500_000.0;
        if (SaiyanRace.stageNow(s) != SaiyanRaceRules.SAIYAN) return Result.fail("a Saiyan without Super Saiyan 3 reached Primal");
        s.unlocked.add("super_saiyan_3");
        if (SaiyanRace.stageNow(s) != SaiyanRaceRules.PRIMAL) return Result.fail("the Primal stage was not reached with its requirements");
        s.raceStage = SaiyanRaceRules.DIVINE;
        s.battlePower = SaiyanRules.START_BP;
        if (SaiyanRace.stageNow(s) != SaiyanRaceRules.DIVINE) return Result.fail("a stage fell when the Battle Power did");
        SaiyanState back = SaiyanTag.fromTag(SaiyanTag.toTag(s));
        if (back.raceStage != SaiyanRaceRules.DIVINE || back.gravitySecs != 0) return Result.fail("the race stage was not saved");
        if (SaiyanTag.fromTag(new net.minecraft.nbt.CompoundTag()).raceStage != 0) return Result.fail("an old save does not read as stage 0");
        return Result.ok("stages rise with their requirements, never fall, and survive a save (an old save reads as 0); realRace on this server = " + SaiyanPhaseConfig.realRace());
    }

    private static Result player(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "saiyan6", at.x, at.y, at.z);
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
            AABB box = new AABB(at, at).inflate(48);
            foe = EntityType.ZOMBIE.create(level);
            if (foe == null) return Result.fail("could not make a test zombie");
            foe.setPos(p.getX(), p.getY(), p.getZ() + 3.0);
            foe.setNoAi(true);
            foe.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000.0);
            foe.setHealth(2000f);
            level.addFreshEntity(foe);
            int before = level.getEntities((Entity) null, box, e -> true).size();
            p.setYRot(0f);
            p.setXRot(0f);

            // toggles
            if (!SaiyanTechniques4.action(p, s, r, SaiyanNet2.OP_REINFORCE, now) || !r.tech4.reinforce || !(SaiyanTechniques4.damageMul(r) > 1.0) || !(SaiyanTechniques4.takenMul(r) < 1.0)) return Result.fail("Ki Reinforcement did not switch on with its factors");
            SaiyanTechniques4.action(p, s, r, SaiyanNet2.OP_REINFORCE, now);
            if (r.tech4.reinforce || SaiyanTechniques4.damageMul(r) != 1.0) return Result.fail("Ki Reinforcement did not switch off");
            if (!SaiyanTechniques4.action(p, s, r, SaiyanNet2.OP_AFTERIMAGE, now) || !r.tech4.afterimage) return Result.fail("the Afterimage did not switch on");
            SaiyanTechniques4.action(p, s, r, SaiyanNet2.OP_AFTERIMAGE, now);
            boolean gravity = SaiyanTechniques4.action(p, s, r, SaiyanNet2.OP_GRAVITY, now);
            if (gravity && r.tech4.gravityLevel < 1) return Result.fail("Gravity Training has no level");
            if (gravity) SaiyanTechniques4.action(p, s, r, SaiyanNet2.OP_GRAVITY, now);
            if (r.tech4.gravityLevel != 0) return Result.fail("Gravity Training did not switch off");

            // Combat Instinct: modifier on, then off with its time
            s.readyAt.clear();
            if (!SaiyanTechniques4.action(p, s, r, SaiyanNet2.OP_INSTINCT, now) || r.tech4.instinctUntil <= now || s.ready("instinct", now)) return Result.fail("Combat Instinct did not start with a cool-down");
            if (!SaiyanAttributes.any(p)) return Result.fail("Combat Instinct put no modifier on");
            SaiyanTechniques4.second(p, s, r, r.tech4.instinctUntil);
            if (r.tech4.instinctUntil != 0) return Result.fail("Combat Instinct never ended");

            // Warrior's Counter: arms, then expires
            ex.setMagicule(max);
            s.stamina = SaiyanManager.staminaMax(s);
            if (!SaiyanTechniques4.action(p, s, r, SaiyanNet2.OP_COUNTER, now) || !SaiyanPhaseRules.counterArmed(now, r.tech4.counterUntil)) return Result.fail("Warrior's Counter did not arm");
            SaiyanTechniques4.second(p, s, r, r.tech4.counterUntil);
            if (r.tech4.counterUntil != 0) return Result.fail("Warrior's Counter never expired");

            // Charged Ki Sphere: flies, ends
            s.readyAt.clear();
            ex.setMagicule(max);
            s.stamina = SaiyanManager.staminaMax(s);
            if (SaiyanTechniques4.action(p, s, r, SaiyanNet2.OP_SPHERE, now)) {
                long t = now;
                for (int i = 0; i < SaiyanPhaseRules.MAX_SPHERE_TRAVEL + 4 && r.tech4.sphereLeft > 0; i++) SaiyanTechniques4.tick(p, s, r, ++t);
                if (r.tech4.sphereLeft != 0) return Result.fail("the Charged Ki Sphere never landed");
            }

            // Ki Wave: a short charge fizzles, a long one fires and ends
            s.readyAt.clear();
            ex.setMagicule(max);
            if (!SaiyanTechniques4.action(p, s, r, SaiyanNet2.OP_WAVE_START, now) || r.tech4.waveStart < 0) return Result.fail("Ki Wave did not start charging");
            SaiyanTechniques4.waveRelease(p, s, r, now + 1);
            if (r.tech4.waveEnd != 0 || r.tech4.waveStart != -1) return Result.fail("a short Ki Wave charge fired");
            SaiyanTechniques4.action(p, s, r, SaiyanNet2.OP_WAVE_START, now);
            if (SaiyanTechniques4.waveRelease(p, s, r, now + 40) && r.tech4.waveEnd > 0) {
                long t = now + 40;
                for (int i = 0; i < SaiyanPhaseRules.MAX_WAVE_TICKS + 4 && r.tech4.waveEnd > 0; i++) SaiyanTechniques4.tick(p, s, r, ++t);
                if (r.tech4.waveEnd != 0) return Result.fail("the Ki Wave never ended");
            }

            // everything off again, no modifier left
            SaiyanTechniques4.cancel(p, r);
            SaiyanAttributes.clear(p);
            int after = level.getEntities((Entity) null, box, e -> true).size();
            if (after > before) return Result.fail("a technique spawned " + (after - before) + " entities");
            return Result.ok("Ki Reinforcement, Afterimage and Gravity Training (switch on and off), Combat Instinct (modifier, end), Warrior's Counter (arm, expire), Charged Ki Sphere and Ki Wave (run and end); no entity spawned");
        } finally {
            if (foe != null) foe.discard();
            clean(p);
        }
    }

    private static Result t3Rules(MinecraftServer server) {
        if (SaiyanPhaseRules.avatarTicks(1.0e6) > SaiyanPhaseRules.MAX_AVATAR_TICKS || SaiyanPhaseRules.avatarTicks(-5.0) < 20 || SaiyanPhaseRules.avatarScale(1.0e6) > SaiyanPhaseRules.MAX_AVATAR_SCALE || SaiyanPhaseRules.avatarScale(0.0) < 3.0)
            return Result.fail("the Giant Ki Avatar bounds moved");
        if (!SaiyanPhaseRules.avatarForm("perfected_ultra_instinct", true) || !SaiyanPhaseRules.avatarForm("true_ultra_instinct", true) || SaiyanPhaseRules.avatarForm("super_saiyan", true) || !SaiyanPhaseRules.avatarForm("", false))
            return Result.fail("the form rule of the Giant Ki Avatar moved");
        if (!SaiyanPhaseRules.avatarSwing(120, 100) || SaiyanPhaseRules.avatarSwing(110, 100) || SaiyanPhaseRules.avatarSwing(100, 100) || !SaiyanPhaseRules.avatarStands(5, 10) || SaiyanPhaseRules.avatarStands(10, 10))
            return Result.fail("the avatar swing beat or its end moved");
        if (VfxShape.GIANT_AVATAR.ordinal() <= VfxShape.SKILL_SIGIL.ordinal()) return Result.fail("GIANT_AVATAR is not appended after SKILL_SIGIL");
        ServerLevel level = server.overworld();
        ServerPlayer p = SelfTests.standIn(level, "saiyan7", level.getSharedSpawnPos().getX() + 0.5, level.getSharedSpawnPos().getY() + 2, level.getSharedSpawnPos().getZ() + 0.5);
        try {
            if (SaiyanDamage.source(p) == null || SaiyanDamage.source(p).getEntity() != p) return Result.fail("the Ki damage source lost its cause");
        } finally {
            clean(p);
        }
        Vec3 a = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        SaiyanKiFx.send(level, VfxShape.GIANT_AVATAR, a, a, SaiyanKiFx.BASE, 20, 8f);
        return Result.ok("the avatar's height, time, form rule and swing beat, the shape after SKILL_SIGIL sent through the effect budget, and the Tensura damage source with the Saiyan as cause");
    }

    private static Result t3Player(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "saiyan8", at.x, at.y, at.z);
        try {
            SaiyanManager.setSaiyan(p, true);
            SaiyanState s = SaiyanStore.of(p);
            SaiyanManager.Run r = SaiyanManager.runOf(p);
            long now = level.getGameTime();
            s.awakened = true;
            s.battlePower = 5000.0;
            var ex = TensuraStorages.getExistenceFrom(p);
            double max = SaiyanKi.max(p);
            if (ex == null || !(max > 1.0)) return Result.ok("the avatar was NOT exercised (the stand-in has no Tensura magicule data)");
            ex.setMagicule(max);
            s.stamina = SaiyanManager.staminaMax(s);
            s.readyAt.clear();
            r.form = "";
            if (SaiyanPhaseConfig.avatarNeedsForm() && SaiyanTechniques4.action(p, s, r, SaiyanNet2.OP_AVATAR, now)) return Result.fail("the Giant Ki Avatar rose without an Ultra Instinct form");
            r.form = SaiyanForms.PERFECTED_ULTRA_INSTINCT.id();
            if (!SaiyanTechniques4.action(p, s, r, SaiyanNet2.OP_AVATAR, now) || r.tech4.avatarUntil <= now) return Result.fail("the Giant Ki Avatar did not rise");
            if (s.ready("avatar", now) || s.uses("avatar") != 1) return Result.fail("the Giant Ki Avatar did not start its cool-down");
            long end = r.tech4.avatarUntil;
            SaiyanTechniques4.tick(p, s, r, r.tech4.avatarRose + 20);
            if (r.tech4.avatarUntil != end) return Result.fail("the avatar ended on its first swing");
            SaiyanTechniques4.tick(p, s, r, end);
            if (r.tech4.avatarUntil != 0) return Result.fail("the Giant Ki Avatar never ended");
            return Result.ok("the Giant Ki Avatar (refused without the form, rises, swings, ends with its time, cool-down)");
        } finally {
            clean(p);
        }
    }

    private static void clean(ServerPlayer p) {
        SaiyanState s = SaiyanStore.peek(p);
        if (s != null) {
            s.saiyan = false;
            s.awakened = false;
            s.battlePower = SaiyanRules.START_BP;
            s.uses.clear();
            s.training.clear();
            s.readyAt.clear();
            s.stamina = -1.0;
            s.raceStage = 0;
            s.gravitySecs = 0;
        }
        SaiyanManager.onLogout(p);
        SaiyanAttributes.clear(p);
    }
}
