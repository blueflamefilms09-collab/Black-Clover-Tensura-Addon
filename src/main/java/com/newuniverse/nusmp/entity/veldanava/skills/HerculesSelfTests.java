package com.newuniverse.nusmp.entity.veldanava.skills;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.entity.veldanava.VeldanavaItem;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * The CI server smoke test of Hercules (prefix {@code hercules} in SMOKE_EXPECT): {@code hercules.rules} (the mode table, the numbers, the pure helpers and the caps), {@code hercules.skill} (registered,
 * learned once, ten modes with the right names, the mask carries Hercules' code and only Hercules' passives read it, icon and lang are in the jar) and {@code hercules.conditions} (Adamantine Body's reductions,
 * fire and Stutter immunity, God of Strength's blow, Invictus' floor, the leap and its landing, the terrain budget, the throw's field, the Castle Guard spent by the Strike, and the clean-up). They run on the
 * throw-away world and clean up after themselves.
 */
public final class HerculesSelfTests {
    private HerculesSelfTests() {}

    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("hercules.rules", HerculesSelfTests::rules);
        SelfTests.register("hercules.skill", HerculesSelfTests::skill);
        SelfTests.register("hercules.conditions", HerculesSelfTests::conditions);
    }

    private static Result rules(MinecraftServer server) {
        JudgmentRules.Spec s = HerculesRules.HERCULES;
        if (s.count() != JudgmentRules.MODES) return Result.fail("expected " + JudgmentRules.MODES + " modes, found " + s.count());
        int toggles = 0, actives = 0;
        for (JudgmentRules.Mode m : s.modes()) {
            if (m.index() != s.modes().indexOf(m)) return Result.fail("mode " + m.id() + " has the wrong index");
            if (m.toggle()) toggles++;
            else actives++;
            if (!m.toggle() && m.masteredCost() > m.cost()) return Result.fail(m.id() + " costs more once mastered");
            if (!m.toggle() && (m.cost() <= 0 || m.cooldownSeconds() <= 0)) return Result.fail(m.id() + " is free or has no cooldown");
            if (!m.toggle() && m.masteredCooldownSeconds() > m.cooldownSeconds()) return Result.fail(m.id() + " cools down slower once mastered");
        }
        if (toggles != 4 || actives != 6) return Result.fail("the shape is " + toggles + "/" + actives + " (expected 4/6)");
        int[][] costs = {{4, 3000, 2000}, {5, 8000, 5500}, {6, 2500, 1500}, {7, 4000, 2500}, {8, 5000, 3200}, {9, 15000, 15000}};
        for (int[] c : costs)
            if (s.mode(c[0]).cost() != c[1] || s.mode(c[0]).masteredCost() != c[2]) return Result.fail(s.mode(c[0]).id() + " does not cost " + c[1] + "/" + c[2]);
        if (s.mode(5).kind() != JudgmentRules.Kind.CROUCH_PRESS || s.mode(7).kind() != JudgmentRules.Kind.CROUCH_PRESS) return Result.fail("Invictus and Devastating Throw are not crouch + press");
        if (s.mode(9).cooldownSeconds() != 180 || s.mode(9).kind() != JudgmentRules.Kind.PRESS) return Result.fail("World-Breaking Strike is not a press with a 180 s cooldown");
        if (JudgmentRules.code("hercules") != HerculesRules.CODE || JudgmentRules.spec("hercules") != s || !JudgmentRules.claimable("hercules")) return Result.fail("Hercules is not claimable code 5 of the spec list");

        // pure helpers
        if (Math.abs(HerculesRules.adamantine(100f, false) - 10f) > 1.0E-4f || Math.abs(HerculesRules.adamantine(100f, true) - 50f) > 1.0E-4f || HerculesRules.adamantine(-4f, false) != 0f) return Result.fail("Adamantine Body's reductions are wrong");
        if (!HerculesRules.fire("in_fire") || !HerculesRules.fire("lava") || !HerculesRules.fire("on_fire") || HerculesRules.fire("mob_attack") || HerculesRules.fire("fall") || HerculesRules.fire(null)) return Result.fail("the fire words are wrong");
        if (HerculesRules.strength(10f, false) != 30f || HerculesRules.strength(10f, true) != 45f || HerculesRules.strength(-1f, false) != 0f) return Result.fail("God of Strength's multiplier is wrong");
        if (!HerculesRules.instantBreak(1.5f) || !HerculesRules.instantBreak(50f) || HerculesRules.instantBreak(-1f) || HerculesRules.instantBreak(51f)) return Result.fail("the one-hit block rule is wrong");
        if (HerculesRules.instantSpeed(0f) <= 100f || HerculesRules.instantSpeed(50f) <= 5000f) return Result.fail("the one-hit speed does not beat the block");
        if (HerculesRules.momentumSeconds(5, 0.0) != 0 || HerculesRules.momentumSeconds(5, 10.0) != 6 || HerculesRules.momentumSeconds(HerculesRules.SECONDS_MAX, 10.0) != HerculesRules.SECONDS_MAX) return Result.fail("the momentum seconds are wrong");
        if (HerculesRules.momentumStage(2) != 0 || HerculesRules.momentumStage(3) != 1 || HerculesRules.momentumStage(6) != 2 || HerculesRules.momentumStage(9) != 3 || HerculesRules.momentumStage(99) != HerculesRules.STAGE_MAX) return Result.fail("the momentum stages are wrong");
        if (HerculesRules.armourPierce(10f, 4f, 0, false) != 0f || HerculesRules.armourPierce(10f, 12f, 3, false) != 0f || HerculesRules.armourPierce(10f, 4f, 3, true) > 6f) return Result.fail("the armour pierce is wrong");
        if (!HerculesRules.calm(500L, 100L, 200L) || HerculesRules.calm(250L, 100L, 200L)) return Result.fail("the calm test is wrong");
        if (HerculesRules.lifesteal(1000f, 20f, false) > 20f * HerculesRules.LIFESTEAL_CAP + 1.0E-4f || HerculesRules.lifesteal(0f, 20f, true) != 0f) return Result.fail("the life steal is not capped");
        if (HerculesRules.tremorFalloff(0.0, 14.0) != 1.0 || Math.abs(HerculesRules.tremorFalloff(14.0, 14.0) - 0.4) > 1.0E-9 || HerculesRules.tremorFalloff(99.0, 14.0) < 0.4 - 1.0E-9) return Result.fail("the tremor falloff is wrong");
        if (HerculesRules.invictusFloor(50f, 20f, 0f) != 19f || HerculesRules.invictusFloor(5f, 20f, 0f) != 5f || HerculesRules.invictusFloor(50f, 1f, 0f) != 0f || HerculesRules.invictusFloor(50f, 10f, 5f) != 14f) return Result.fail("Invictus' floor is wrong");
        if (HerculesRules.invictusMultiplier(20f, 20f, false) != 1.0 || Math.abs(HerculesRules.invictusMultiplier(0f, 20f, false) - 3.0) > 1.0E-9 || Math.abs(HerculesRules.invictusMultiplier(0f, 20f, true) - 4.0) > 1.0E-9) return Result.fail("Invictus' multiplier is wrong");
        if (HerculesRules.throwPath(9999) != HerculesRules.THROW_SPEED * HerculesRules.THROW_MAX_TICKS || HerculesRules.throwPath(-3) != 0.0) return Result.fail("the throw path is not bounded");
        if (HerculesRules.armorBreakTicks(false, false) != HerculesRules.ARMOR_BREAK_TICKS || HerculesRules.armorBreakTicks(true, false) >= HerculesRules.ARMOR_BREAK_TICKS || HerculesRules.armorBreakTicks(false, true) >= HerculesRules.ARMOR_BREAK_TICKS) return Result.fail("the broken armour times are wrong");
        if (HerculesRules.strikeDamage(0) != HerculesRules.STRIKE_DAMAGE || HerculesRules.strikeDamage(1) != HerculesRules.STRIKE_DAMAGE * HerculesRules.STRIKE_SIDE_SHARE) return Result.fail("the Strike's damage ranks are wrong");
        if (HerculesRules.TREMOR_MAX_CREATURES > 24 || HerculesRules.TREMOR_MAX_BLOCKS > 240 || HerculesRules.THROW_MAX_HITS > 8 || HerculesRules.THROW_BLAST_MAX > 12 || HerculesRules.THROW_CRATER_BLOCKS > 60
                || HerculesRules.CHUNK_BLOCKS > 12 || HerculesRules.ROAR_MAX > 32 || HerculesRules.STRIKE_MAX > 8 || HerculesRules.SHOCK_MAX > 6 || HerculesRules.INVICTUS_TICKS > JudgmentFields.MAX_TICKS
                || HerculesRules.THROW_MAX_TICKS > JudgmentFields.MAX_TICKS || HerculesRules.ROAR_CLEANSE_MAX > 24) return Result.fail("a per-press cap is not small");
        return Result.ok("10 modes (4/6), costs, cooldowns, the body, strength, momentum, vigor, tremor, Invictus, throw, roar and strike numbers and the caps hold");
    }

    private static Result skill(MinecraftServer server) throws Exception {
        ServerLevel sl = server.overworld();
        BlockPos at = sl.getSharedSpawnPos().above(3);
        FakePlayer p = SelfTests.standIn(sl, "hercules_skill", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        VeldanavaSkills.revoke(p);
        try {
            var holder = VeldanavaSkills.holder("hercules");
            if (holder == null || !(holder.get() instanceof HerculesSkill)) return Result.fail("Hercules is not registered");
            if (!VeldanavaSkills.grant(p, "hercules")) return Result.fail("Hercules could not be granted");
            Optional<ManasSkillInstance> inst = SkillAPI.getSkillsFrom(p).getSkill(holder.getId());
            if (inst.isEmpty()) return Result.fail("the granted skill is not on the player");
            ManasSkillInstance i = inst.get();
            if (!(i.getSkill() instanceof HerculesSkill sk)) return Result.fail("the instance is not a Hercules skill");
            if (sk.getModes(i) != JudgmentRules.MODES) return Result.fail("the skill reports " + sk.getModes(i) + " modes");
            for (JudgmentRules.Mode m : HerculesRules.HERCULES.modes())
                if (!m.id().equals(sk.getModeId(i, m.index()))) return Result.fail("mode " + m.index() + " is '" + sk.getModeId(i, m.index()) + "' not '" + m.id() + "'");
            if (VeldanavaSkills.grant(p, "hercules")) return Result.fail("a second grant went through");
            if (VeldanavaSkills.grant(p, "solomon")) return Result.fail("a second skill was granted to the same player");

            JudgmentState.write(p, JudgmentState.compose(5, 0b1001, false));
            if (!JudgmentState.onFor(p, 5, 0) || JudgmentState.onFor(p, 5, 1) || !JudgmentState.onFor(p, 5, 3)) return Result.fail("Hercules' mask reads wrong");
            if (JudgmentState.on(p, 0) || JudgmentState.onFor(p, 4, 0) || JudgmentState.onFor(p, 3, 3)) return Result.fail("Hercules' toggles switched another skill's passives on");
            if (VeldanavaGuard.spatiallyFree(p) || VeldanavaGuard.mindImmune(p)) return Result.fail("Hercules' toggles switched Israfil's guards on");
            JudgmentState.write(p, 0);

            String icon = "assets/nusmp/textures/skill/veldanava/hercules.png";
            if (HerculesSelfTests.class.getClassLoader().getResource(icon) == null) return Result.fail("missing " + icon);
            JsonObject lang;
            try (InputStream in = HerculesSelfTests.class.getResourceAsStream("/assets/nusmp/lang/en_us.json")) {
                if (in == null) return Result.fail("no en_us.json");
                lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            for (String k : new String[]{"nusmp.skill.hercules", "nusmp.skill.hercules.description", "nusmp.skill.hercules.mastered", "effect.nusmp.armor_break"}) if (!lang.has(k)) return Result.fail("missing lang key " + k);
            for (JudgmentRules.Mode m : HerculesRules.HERCULES.modes())
                if (!lang.has("nusmp.skill.mode.hercules." + m.id())) return Result.fail("missing lang key for mode " + m.id());
            return Result.ok("registered, learned once, 10 modes named, the mask keeps the skills apart, icon and lang are in the jar");
        } finally {
            JudgmentState.write(p, 0);
            VeldanavaSkills.revoke(p);
        }
    }

    private static Result conditions(MinecraftServer server) {
        ServerLevel sl = server.overworld();
        BlockPos at = sl.getSharedSpawnPos().above(3);
        FakePlayer p = SelfTests.standIn(sl, "hercules_cond", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        Mob z = EntityType.ZOMBIE.create(sl);
        Mob far = EntityType.ZOMBIE.create(sl);
        if (z == null || far == null) return Result.fail("could not make a test creature");
        z.moveTo(at.getX() + 3.5, at.getY(), at.getZ() + 0.5);
        sl.addFreshEntity(z);
        Vec3 center = new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        boolean griefing = sl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        BlockPos platform = new BlockPos(at.getX(), sl.getMaxBuildHeight() - 24, at.getZ());
        try {
            sl.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, server);

            // Adamantine Body: a tenth of a physical blow, half of a magical one, no flame, no knockback, no fall damage
            JudgmentState.write(p, JudgmentState.compose(5, 0b0001, false));
            var hit = new LivingIncomingDamageEvent(p, new DamageContainer(sl.damageSources().mobAttack(z), 20f));
            HerculesEvents.onIncoming(hit);
            if (hit.isCanceled() || Math.abs(hit.getAmount() - 2f) > 1.0E-4f) return Result.fail("Adamantine Body let " + hit.getAmount() + " of a 20 point blow through (expected 2)");
            var spell = new LivingIncomingDamageEvent(p, new DamageContainer(sl.damageSources().indirectMagic(z, z), 20f));
            HerculesEvents.onIncoming(spell);
            if (Math.abs(spell.getAmount() - 10f) > 1.0E-4f) return Result.fail("Adamantine Body let " + spell.getAmount() + " of a 20 point spell through (expected 10)");
            var fire = new LivingIncomingDamageEvent(p, new DamageContainer(sl.damageSources().inFire(), 4f));
            HerculesEvents.onIncoming(fire);
            if (!fire.isCanceled()) return Result.fail("Adamantine Body let a flame through");
            var fall = new LivingFallEvent(p, 20f, 1f);
            HerculesEvents.onFall(fall);
            if (fall.getDamageMultiplier() != 0f) return Result.fail("Adamantine Body took fall damage");
            p.addEffect(new MobEffectInstance(JudgmentEffects.TEMPORAL_STUTTER, 100, 0));
            boolean stuttered = p.hasEffect(JudgmentEffects.TEMPORAL_STUTTER);
            p.removeAllEffects();
            if (stuttered) return Result.fail("Temporal Stutter took hold of the owner of Adamantine Body");
            JudgmentState.write(p, 0);
            var bare = new LivingIncomingDamageEvent(p, new DamageContainer(sl.damageSources().mobAttack(z), 20f));
            HerculesEvents.onIncoming(bare);
            if (bare.getAmount() != 20f) return Result.fail("someone without Adamantine Body had a blow reduced");

            // God of Strength: the owner's melee blow grows, a blow of someone without the skill does not
            JudgmentState.write(p, JudgmentState.compose(5, 0b0010, false));
            var mine = new LivingDamageEvent.Pre(z, new DamageContainer(sl.damageSources().playerAttack(p), 4f));
            HerculesEvents.onDamagePre(mine);
            if (mine.getNewDamage() <= 4f) return Result.fail("God of Strength did not grow the blow (" + mine.getNewDamage() + ")");
            JudgmentState.write(p, 0);
            var plain = new LivingDamageEvent.Pre(z, new DamageContainer(sl.damageSources().playerAttack(p), 4f));
            HerculesEvents.onDamagePre(plain);
            if (plain.getNewDamage() != 4f) return Result.fail("a blow of someone without the skill was grown");

            // Invictus: a blow that would kill leaves one Hit Point, only while it lasts, and its end clears the mark
            JudgmentState.write(p, JudgmentState.compose(5, 0, false));
            p.setHealth(10f);
            HerculesState.startInvictus(p);
            var lethal = new LivingDamageEvent.Pre(p, new DamageContainer(sl.damageSources().mobAttack(z), 500f));
            HerculesEvents.onDamagePre(lethal);
            if (Math.abs(lethal.getNewDamage() - 9f) > 1.0E-4f) return Result.fail("Invictus let " + lethal.getNewDamage() + " of a lethal blow through (expected 9)");
            HerculesState.endInvictus(p);
            var after = new LivingDamageEvent.Pre(p, new DamageContainer(sl.damageSources().mobAttack(z), 500f));
            HerculesEvents.onDamagePre(after);
            if (after.getNewDamage() != 500f || HerculesState.invictus(p)) return Result.fail("Invictus outlived its end");
            p.setHealth(p.getMaxHealth());
            JudgmentState.write(p, 0);

            // the leap and its landing: the fall is free, the foe within the radius is hurt and thrown off its feet, and the leap is used up
            JudgmentState.write(p, JudgmentState.compose(5, 0, false));
            z.setHealth(z.getMaxHealth());
            float before = z.getHealth();
            HerculesState.leap(p);
            if (!HerculesState.leaping(p.getUUID())) return Result.fail("the leap was not recorded");
            var land = new LivingFallEvent(p, 12f, 1f);
            HerculesEvents.onFall(land);
            if (land.getDamageMultiplier() != 0f || HerculesState.leaping(p.getUUID())) return Result.fail("the landing was not taken");
            if (z.getHealth() >= before && z.isAlive()) return Result.fail("the tremor did no harm");
            if (!z.hasEffect(JudgmentEffects.MOVEMENT_INTERFERENCE) && z.isAlive()) return Result.fail("the tremor did not unbalance the foe");
            var again = new LivingFallEvent(p, 12f, 1f);
            HerculesEvents.onFall(again);
            if (again.getDamageMultiplier() != 1f) return Result.fail("a second landing was a tremor");
            HerculesState.leap(p);
            HerculesState.expireLeap(p);
            if (HerculesState.leapCount() != 1) return Result.fail("a leap inside its window was dropped");
            JudgmentState.write(p, 0);
            HerculesState.leave(p);
            if (HerculesState.leapCount() != 0 || HerculesState.kept() != 0) return Result.fail("leaving left the leap behind");

            // the terrain: a platform in the sky is dug within the budget, never when mobGriefing is off
            for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) for (int dy = 0; dy >= -3; dy--) sl.setBlock(platform.offset(dx, dy, dz), Blocks.STONE.defaultBlockState(), 2);
            if (HerculesTerrain.bowl(sl, platform, HerculesRules.TREMOR_BOWL_RADIUS, HerculesRules.TREMOR_BOWL_DEPTH, 50) != 0 || HerculesTerrain.allowed(sl)) return Result.fail("terrain was dug with mobGriefing off");
            sl.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, server);
            int dug = HerculesTerrain.bowl(sl, platform, HerculesRules.TREMOR_BOWL_RADIUS, HerculesRules.TREMOR_BOWL_DEPTH, 50);
            if (dug <= 0 || dug > 50) return Result.fail("a bowl removed " + dug + " blocks (budget 50)");
            if (HerculesTerrain.bowl(sl, platform.offset(0, -20, 0), 3.0, 2, 50) != 0) return Result.fail("air was counted as dug");
            sl.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, server);

            // the throw: the field of a chunk (nothing thrown) ends on its own inside the bound
            JudgmentFields.clearAll();
            JudgmentFields.add(p, sl, null, center, HerculesRules.THROW_BLAST_RADIUS, HerculesRules.THROW_MAX_TICKS + 4, new HerculesThrow(null, center.add(0.0, 40.0, 0.0), new Vec3(1.0, 0.0, 0.0)), HerculesThrow.KIND);
            JudgmentFields.Field tf = JudgmentFields.find(p.getUUID(), HerculesThrow.KIND);
            if (tf == null) return Result.fail("the throw is not a field");
            HerculesThrow behavior = (HerculesThrow) tf.behavior;
            int pulses = 0;
            while (tf.endsAt != 0L && pulses++ < HerculesRules.THROW_MAX_TICKS) behavior.pulse(p, sl, tf);
            if (tf.endsAt != 0L) return Result.fail("a throw did not land within its bound");
            JudgmentFields.clearAll();

            // the Strike: Michael's Castle Guard of a player is spent once, and the armour of a creature breaks
            p.getPersistentData().putBoolean(VeldanavaItem.TAG_MICHAEL, true);
            p.getPersistentData().remove(VeldanavaItem.TAG_MICHAEL_DAY);
            if (!HerculesSkill.spendCastleGuard(p) || HerculesSkill.spendCastleGuard(p)) return Result.fail("the Castle Guard was not spent exactly once");
            p.getPersistentData().remove(VeldanavaItem.TAG_MICHAEL);
            if (HerculesSkill.spendCastleGuard(p)) return Result.fail("a Castle Guard that was never raised was spent");
            far.addEffect(new MobEffectInstance(JudgmentEffects.ARMOR_BREAK, 40, 0));
            if (!far.hasEffect(JudgmentEffects.ARMOR_BREAK)) return Result.fail("the armour break did not take");

            // forgetting: the modifiers, the run, the leap and the mark go
            HerculesState.leap(p);
            HerculesState.startInvictus(p);
            HerculesState.clear(p);
            if (HerculesState.kept() != 0 || HerculesState.invictus(p) || HerculesState.hasSpeedModifier(p)) return Result.fail("clearing the skill left something behind");
            return Result.ok("body, fire, stutter, fall, strength, Invictus, leap and landing, terrain, throw, Castle Guard, armour break and clean-up hold");
        } finally {
            JudgmentFields.clearAll();
            HerculesState.clear(p);
            p.removeAllEffects();
            JudgmentState.write(p, 0);
            p.getPersistentData().remove(VeldanavaItem.TAG_MICHAEL);
            p.getPersistentData().remove(VeldanavaItem.TAG_MICHAEL_DAY);
            sl.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(griefing, server);
            for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) for (int dy = 0; dy >= -3; dy--) sl.setBlock(platform.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
            z.discard();
            far.discard();
            VeldanavaSkills.revoke(p);
        }
    }
}
