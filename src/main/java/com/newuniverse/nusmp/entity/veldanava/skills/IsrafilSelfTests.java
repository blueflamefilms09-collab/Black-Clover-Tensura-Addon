package com.newuniverse.nusmp.entity.veldanava.skills;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * The CI server smoke test of Israfil (prefix {@code israfil} in SMOKE_EXPECT): {@code israfil.rules} (the mode table, the numbers, the caps, the mind-effect words, the shared cooldown table),
 * {@code israfil.skill} (registered, learned once, ten modes with the right names, the mask carries Israfil's code and only Israfil's passives read it, icon and lang are in the jar) and
 * {@code israfil.conditions} (the stored impact and its gifts, the guards, the two new effects, a cage and a Nihility field are fields with a kind that obey the caps). They run on the throw-away world and clean up after themselves.
 */
public final class IsrafilSelfTests {
    private IsrafilSelfTests() {}

    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("israfil.rules", IsrafilSelfTests::rules);
        SelfTests.register("israfil.skill", IsrafilSelfTests::skill);
        SelfTests.register("israfil.conditions", IsrafilSelfTests::conditions);
        SelfTests.register("israfil.pocket", IsrafilSelfTests::pocket);
    }

    private static Result rules(MinecraftServer server) {
        JudgmentRules.Spec s = IsrafilRules.ISRAFIL;
        if (s.count() != JudgmentRules.MODES) return Result.fail("expected " + JudgmentRules.MODES + " modes, found " + s.count());
        int toggles = 0, actives = 0, held = 0;
        for (JudgmentRules.Mode m : s.modes()) {
            if (m.index() != s.modes().indexOf(m)) return Result.fail("mode " + m.id() + " has the wrong index");
            if (m.toggle()) toggles++;
            else if (m.kind() == JudgmentRules.Kind.HELD) held++;
            else actives++;
            if (!m.toggle() && m.masteredCost() > m.cost()) return Result.fail(m.id() + " costs more once mastered");
            if (!m.toggle() && (m.cost() <= 0 || m.cooldownSeconds() <= 0)) return Result.fail(m.id() + " is free or has no cooldown");
            if (!m.toggle() && m.masteredCooldownSeconds() > m.cooldownSeconds()) return Result.fail(m.id() + " cools down slower once mastered");
        }
        if (toggles != 4 || held != 1 || actives != 5) return Result.fail("the shape is " + toggles + "/" + actives + "/" + held + " (expected 4/5/1 with Rift Walk held)");
        if (s.mode(8).kind() != JudgmentRules.Kind.HELD || !s.mode(8).perSecond()) return Result.fail("Rift Walk is not a held mode paid per second");
        if (s.mode(9).kind() != JudgmentRules.Kind.PRESS || !s.mode(9).masteryOnly()) return Result.fail("the Trumpet is not the mastery press");
        if (JudgmentRules.code("israfil") != 2 || JudgmentRules.spec("israfil") != s) return Result.fail("Israfil is not code 2 of the spec list");
        if (IsrafilRules.stored(100.0, 0.0) != 35.0 || IsrafilRules.stored(100.0, 110.0) != 10.0 || IsrafilRules.stored(-4.0, 0.0) != 0.0 || IsrafilRules.stored(100.0, 500.0) != 0.0) return Result.fail("the stored impact is wrong");
        if (IsrafilRules.storeHealth(1000.0) != IsrafilRules.STORE_HP_MAX || IsrafilRules.storeToughness(1000.0) != IsrafilRules.STORE_TOUGHNESS_MAX || IsrafilRules.storeHealth(-3.0) != 0.0) return Result.fail("the store gifts are not capped");
        if (IsrafilRules.drained(10.0, 2.0) != 6.0 || IsrafilRules.drained(1.0, 100.0) != 0.0) return Result.fail("the drain is wrong");
        if (IsrafilRules.debtOnAttacker(0.0) != 0 || IsrafilRules.debtOnAttacker(1.0) != 1 || IsrafilRules.debtOnAttacker(1.0e9) != IsrafilRules.STORE_DEBT_MAX) return Result.fail("the debt on the attacker is wrong");
        if (IsrafilRules.sinsDamage(0, 0) != IsrafilRules.SINS_BASE_DAMAGE || IsrafilRules.sinsDamage(5, 0) != 100.0 || IsrafilRules.sinsDamage(0, 100000) != IsrafilRules.SINS_BASE_DAMAGE + IsrafilRules.SINS_KILL_CAP) return Result.fail("Weight of Sins damage is wrong");
        if (IsrafilRules.riftSeconds(false) != 4 || IsrafilRules.riftSeconds(true) != 8) return Result.fail("Rift Walk lasts the wrong time");
        if (IsrafilRules.trumpetCooldown(0) != 60 || IsrafilRules.trumpetCooldown(200) != 200) return Result.fail("the Trumpet cooldown floor is wrong");
        if (!IsrafilRules.mindEffect("charm") || !IsrafilRules.mindEffect("Fear_Of_Death") || IsrafilRules.mindEffect("speed") || IsrafilRules.mindEffect(null)) return Result.fail("the mind-effect words are wrong");
        if (IsrafilRules.TRUMPET_MAX_TARGETS > 64 || IsrafilRules.COLLAPSE_MAX_TARGETS > 32 || IsrafilRules.MAX_WEIGHT_TARGETS > 32 || IsrafilRules.SHIFT_MAX_AGGRO > 48) return Result.fail("a per-press cap is not small");
        return Result.ok("10 modes (4/5/1), costs, cooldowns, the store, the debt, the damage and the caps hold");
    }

    private static Result skill(MinecraftServer server) throws Exception {
        ServerLevel sl = server.overworld();
        BlockPos at = sl.getSharedSpawnPos().above(3);
        FakePlayer p = SelfTests.standIn(sl, "israfil_skill", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        VeldanavaSkills.revoke(p);
        try {
            var holder = VeldanavaSkills.holder("israfil");
            if (holder == null || !(holder.get() instanceof IsrafilSkill)) return Result.fail("Israfil is not registered");
            if (!VeldanavaSkills.grant(p, "israfil")) return Result.fail("Israfil could not be granted");
            Optional<ManasSkillInstance> inst = SkillAPI.getSkillsFrom(p).getSkill(holder.getId());
            if (inst.isEmpty()) return Result.fail("the granted skill is not on the player");
            ManasSkillInstance i = inst.get();
            if (!(i.getSkill() instanceof IsrafilSkill sk)) return Result.fail("the instance is not an Israfil skill");
            if (sk.getModes(i) != JudgmentRules.MODES) return Result.fail("the skill reports " + sk.getModes(i) + " modes");
            for (JudgmentRules.Mode m : IsrafilRules.ISRAFIL.modes())
                if (!m.id().equals(sk.getModeId(i, m.index()))) return Result.fail("mode " + m.index() + " is '" + sk.getModeId(i, m.index()) + "' not '" + m.id() + "'");
            if (VeldanavaSkills.grant(p, "israfil")) return Result.fail("a second grant went through");
            if (VeldanavaSkills.grant(p, "sandalphon")) return Result.fail("a second skill was granted to the same player");

            JudgmentState.write(p, JudgmentState.compose(2, 0b0101, false));
            if (!JudgmentState.onFor(p, 2, 0) || JudgmentState.onFor(p, 2, 1) || !JudgmentState.onFor(p, 2, 2)) return Result.fail("Israfil's mask reads wrong");
            if (JudgmentState.on(p, 0)) return Result.fail("Israfil's toggles switched Sandalphon's passives on");
            if (!VeldanavaGuard.spatiallyFree(p) || !VeldanavaGuard.mindImmune(p) || !VeldanavaGuard.displacementImmune(p) || !VeldanavaGuard.entrapmentImmune(p)) return Result.fail("the guards do not read the mask");
            JudgmentState.write(p, JudgmentState.compose(1, 0b0101, false));
            if (VeldanavaGuard.spatiallyFree(p) || VeldanavaGuard.mindImmune(p)) return Result.fail("Sandalphon's toggles switched Israfil's guards on");
            JudgmentState.write(p, 0);

            String icon = "assets/nusmp/textures/skill/veldanava/israfil.png";
            if (IsrafilSelfTests.class.getClassLoader().getResource(icon) == null) return Result.fail("missing " + icon);
            JsonObject lang;
            try (InputStream in = IsrafilSelfTests.class.getResourceAsStream("/assets/nusmp/lang/en_us.json")) {
                if (in == null) return Result.fail("no en_us.json");
                lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            String[] need = {"nusmp.skill.israfil", "nusmp.skill.israfil.description", "nusmp.skill.israfil.mastered", "effect.nusmp.gravity_crush", "effect.nusmp.nihility_collapse"};
            for (String k : need) if (!lang.has(k)) return Result.fail("missing lang key " + k);
            for (JudgmentRules.Mode m : IsrafilRules.ISRAFIL.modes())
                if (!lang.has("nusmp.skill.mode.israfil." + m.id())) return Result.fail("missing lang key for mode " + m.id());
            return Result.ok("registered, learned once, 10 modes named, the mask keeps the two skills apart, icon and lang are in the jar");
        } finally {
            JudgmentState.write(p, 0);
            VeldanavaSkills.revoke(p);
        }
    }

    /** Dimensional Labyrinth's pocket dimension: the dimension is loaded, a maze is painted, shifted thirty times and taken down block-exact, and the setting and the old cage are still there. */
    private static Result pocket(MinecraftServer server) {
        if (server.getLevel(com.newuniverse.nusmp.entity.veldanava.pocket.PocketLabyrinth.DIMENSION) == null) return Result.fail("the nusmp:trial dimension is not loaded");
        if (!JudgmentConfig.pocketDimension()) return Result.fail("pocketDimension is not on by default");
        if (com.newuniverse.nusmp.entity.veldanava.pocket.PocketLabyrinth.sessions() != 0) return Result.fail("a labyrinth is open on a fresh world");
        String bad = com.newuniverse.nusmp.entity.veldanava.pocket.PocketLabyrinth.check(server);
        if (bad != null) return Result.fail(bad);
        if (new IsrafilSkill.Cage(java.util.UUID.randomUUID(), 20).period() < 1) return Result.fail("the old cage is gone");
        return Result.ok("the trial dimension is loaded; a mastered maze painted, shifted 30 times and taken down block-exact; the old cage is kept");
    }

    private static Result conditions(MinecraftServer server) {
        ServerLevel sl = server.overworld();
        BlockPos at = sl.getSharedSpawnPos().above(3);
        FakePlayer p = SelfTests.standIn(sl, "israfil_cond", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        Mob z = EntityType.ZOMBIE.create(sl);
        if (z == null) return Result.fail("could not make a test creature");
        z.moveTo(at.getX() + 3.5, at.getY(), at.getZ() + 0.5);
        sl.addFreshEntity(z);
        Vec3 center = new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        double baseHealth = p.getAttributeBaseValue(Attributes.MAX_HEALTH);
        try {
            // the stored impact and what it gives
            IsrafilState.setStore(p, 40.0);
            if (IsrafilState.store(p) != 40.0) return Result.fail("the store was not kept");
            IsrafilState.setStore(p, 100000.0);
            if (IsrafilState.store(p) != IsrafilRules.STORE_MAX) return Result.fail("the store is not capped");
            IsrafilState.applyStore(p, 40.0);
            double expected = baseHealth + IsrafilRules.storeHealth(40.0);
            if (Math.abs(p.getMaxHealth() - expected) > 1.0E-6) return Result.fail("the store gave " + p.getMaxHealth() + " health, expected " + expected);
            IsrafilState.clear(p);
            if (IsrafilState.store(p) != 0.0 || Math.abs(p.getMaxHealth() - baseHealth) > 1.0E-6) return Result.fail("clearing the skill left the store or its health behind");

            // movement and the guards: a mob can be pulled, a spatially free player cannot
            if (!IsrafilState.movable(z)) return Result.fail("an ordinary mob cannot be moved");
            JudgmentState.write(p, JudgmentState.compose(2, 0b0001, false));
            if (IsrafilState.movable(p)) return Result.fail("a player with Spatial Domination can be moved");
            JudgmentState.write(p, 0);
            if (!IsrafilState.movable(p)) return Result.fail("a plain player cannot be moved");

            // the two new effects
            z.addEffect(new net.minecraft.world.effect.MobEffectInstance(JudgmentEffects.NIHILITY_COLLAPSE, 40, 0));
            if (!JudgmentEffects.has(z, JudgmentEffects.NIHILITY_COLLAPSE)) return Result.fail("Nihility Collapse did not land");
            z.addEffect(new net.minecraft.world.effect.MobEffectInstance(JudgmentEffects.GRAVITY_CRUSH, 40, 0));
            if (!JudgmentEffects.has(z, JudgmentEffects.GRAVITY_CRUSH)) return Result.fail("Gravity Crush did not land");

            // a cage and a Nihility field are fields with a kind; the per-player cap holds, an edict ends them, a logout drops them
            JudgmentFields.clearAll();
            int per = Math.max(1, JudgmentConfig.fieldsPerPlayer());
            for (int k = 0; k < per + 2; k++) JudgmentFields.add(p, sl, null, center, 4.5, 100, new IsrafilSkill.Cage(java.util.UUID.randomUUID(), 20), "labyrinth");
            if (JudgmentFields.countOf(p.getUUID()) != per) return Result.fail("the player holds " + JudgmentFields.countOf(p.getUUID()) + " fields, the cap is " + per);
            if (JudgmentFields.find(p.getUUID(), "labyrinth") == null) return Result.fail("the cage has no kind");
            if (JudgmentFields.clearKind(server, p.getUUID(), "labyrinth") != per || JudgmentFields.countOf(p.getUUID()) != 0) return Result.fail("clearing a kind did not clear it");
            JudgmentFields.add(p, sl, null, center, 5.0, 100, IsrafilSkill.NIHILITY, "nihility");
            if (JudgmentFields.find(p.getUUID(), "nihility") == null) return Result.fail("the Nihility field was not kept");
            JudgmentFields.onLogout(p.getUUID());
            if (JudgmentFields.countOf(p.getUUID()) != 0) return Result.fail("fields stayed after the owner left");
            JudgmentFields.add(p, sl, null, center, 1.0, 100000, IsrafilSkill.NIHILITY, "nihility");
            JudgmentFields.Field f = JudgmentFields.find(p.getUUID(), "nihility");
            if (f == null || f.endsAt - sl.getGameTime() > JudgmentFields.MAX_TICKS + 1) return Result.fail("a field can outlast the cap");

            return Result.ok("store and gifts, guards, effects, field kinds and caps hold");
        } finally {
            JudgmentFields.clearAll();
            RiftWalk.clearAll();
            IsrafilState.clear(p);
            JudgmentState.write(p, 0);
            z.discard();
        }
    }
}
