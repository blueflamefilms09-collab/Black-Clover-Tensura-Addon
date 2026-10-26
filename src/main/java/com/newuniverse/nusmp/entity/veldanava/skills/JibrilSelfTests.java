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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * The CI server smoke test of Jibril (prefix {@code jibril} in SMOKE_EXPECT): {@code jibril.rules} (the mode table, the numbers, the helpers, the caps), {@code jibril.skill} (registered, learned once, ten
 * modes with the right names, the mask carries Jibril's code and only Jibril's passives read it, icon and lang are in the jar) and {@code jibril.conditions} (the wings give and take back flight, Divine Rigor
 * stops a debuff, the parry window negates and returns a blow, the holy hit, the hotbar lock and the sanctuary registry are bounded and clean up, Strict Governance stretches a cooldown once a second, the
 * Kingdom is a field with a kind that obeys the caps). They run on the throw-away world and clean up after themselves.
 */
public final class JibrilSelfTests {
    private JibrilSelfTests() {}

    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("jibril.rules", JibrilSelfTests::rules);
        SelfTests.register("jibril.skill", JibrilSelfTests::skill);
        SelfTests.register("jibril.conditions", JibrilSelfTests::conditions);
    }

    private static Result rules(MinecraftServer server) {
        JudgmentRules.Spec s = JibrilRules.JIBRIL;
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
        if (toggles != 4 || held != 1 || actives != 5) return Result.fail("the shape is " + toggles + "/" + actives + "/" + held + " (expected 4/5/1 with Kingdom of God held)");
        int[][] costs = {{4, 5000, 3500}, {5, 8500, 6000}, {6, 7000, 5000}, {7, 4500, 3000}, {8, 6000, 4000}, {9, 4000, 2500}};
        for (int[] c : costs)
            if (s.mode(c[0]).cost() != c[1] || s.mode(c[0]).masteredCost() != c[2]) return Result.fail(s.mode(c[0]).id() + " does not cost " + c[1] + "/" + c[2]);
        if (s.mode(5).kind() != JudgmentRules.Kind.CROUCH_PRESS || s.mode(7).kind() != JudgmentRules.Kind.CROUCH_PRESS) return Result.fail("Judgment of Heaven and Holy Shackles are not crouch + press");
        if (s.mode(9).kind() != JudgmentRules.Kind.HELD || !s.mode(9).perSecond() || !s.mode(9).masteryOnly()) return Result.fail("Kingdom of God is not the held mastery mode paid per second");
        if (JudgmentRules.code("jibril") != JibrilRules.CODE || JudgmentRules.spec("jibril") != s) return Result.fail("Jibril is not code 3 of the spec list");
        if (JibrilRules.rigorAllyDuration(100) != 50 || JibrilRules.rigorAllyDuration(-1) != -1 || JibrilRules.rigorAllyDuration(1) != 1) return Result.fail("the ally share of Divine Rigor is wrong");
        if (!JibrilRules.demonic("demon_lord") || !JibrilRules.demonic("Greater_Demon") || !JibrilRules.demonic("imp") || JibrilRules.demonic("chimp") || JibrilRules.demonic("simple_golem") || JibrilRules.demonic(null)) return Result.fail("the demon words are wrong");
        if (JibrilRules.presenceDamage(0) != JibrilRules.PRESENCE_DAMAGE || JibrilRules.presenceDamage(100000) != JibrilRules.PRESENCE_DAMAGE + JibrilRules.PRESENCE_DEBT_CAP) return Result.fail("the holy burn is not capped");
        if (!JibrilRules.governanceSecond(0L) || JibrilRules.governanceSecond(20L) || !JibrilRules.governanceSecond(40L)) return Result.fail("Strict Governance does not stretch every other second");
        if (!JibrilRules.dodges(0, false) || !JibrilRules.dodges(69, false) || JibrilRules.dodges(70, false) || !JibrilRules.dodges(84, true) || JibrilRules.dodges(85, true) || JibrilRules.dodges(-1, true)) return Result.fail("the dodge chance is wrong");
        if (JibrilRules.parryTicks(false) != 12 || JibrilRules.parryTicks(true) != 20 || JibrilRules.reflected(10.0) != 30.0 || JibrilRules.reflected(-4.0) != 0.0) return Result.fail("the parry numbers are wrong");
        if (JibrilRules.kingdomLimited(100.0, 20.0, 0.0) != 19.0 || JibrilRules.kingdomLimited(5.0, 20.0, 0.0) != 5.0 || JibrilRules.kingdomLimited(100.0, 1.0, 0.0) != 0.0
                || JibrilRules.kingdomLimited(100.0, 1.0, 4.0) != 4.0 || JibrilRules.kingdomLimited(-3.0, 20.0, 0.0) != 0.0) return Result.fail("the Kingdom's floor of 1 HP is wrong");
        if (JibrilRules.stolenTicks(-1) != JibrilRules.SHACKLE_BUFF_MAX_TICKS || JibrilRules.stolenTicks(100) != 100 || JibrilRules.stolenTicks(1000000) != JibrilRules.SHACKLE_BUFF_MAX_TICKS || JibrilRules.stolenTicks(0) != 1) return Result.fail("a stolen effect is not capped");
        if (JibrilRules.hotbarTicks(500) != JibrilRules.HOTBAR_MAX_TICKS || JibrilRules.hotbarTicks(-4) != 0 || JibrilRules.hotbarTicks(100) != 100) return Result.fail("the hotbar lock length is wrong");
        if (JibrilRules.PRESENCE_MAX_TARGETS > 32 || JibrilRules.GOVERNANCE_MAX_PLAYERS > 32 || JibrilRules.PURGE_MAX_CLOUDS > 128 || JibrilRules.KINGDOM_MAX_ENEMIES > 48 || JibrilRules.JUDGMENT_MAX_SPLASH > 12
                || JibrilState.MAX_HOTBAR_LOCKS > 128 || JibrilRules.KINGDOM_MAX_TICKS > JudgmentFields.MAX_TICKS) return Result.fail("a per-press cap is not small");
        return Result.ok("10 modes (4/5/1), costs, cooldowns, the parry, the dodge, the Kingdom's floor, the debuff share and the caps hold");
    }

    private static Result skill(MinecraftServer server) throws Exception {
        ServerLevel sl = server.overworld();
        BlockPos at = sl.getSharedSpawnPos().above(3);
        FakePlayer p = SelfTests.standIn(sl, "jibril_skill", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        VeldanavaSkills.revoke(p);
        try {
            var holder = VeldanavaSkills.holder("jibril");
            if (holder == null || !(holder.get() instanceof JibrilSkill)) return Result.fail("Jibril is not registered");
            if (!VeldanavaSkills.grant(p, "jibril")) return Result.fail("Jibril could not be granted");
            Optional<ManasSkillInstance> inst = SkillAPI.getSkillsFrom(p).getSkill(holder.getId());
            if (inst.isEmpty()) return Result.fail("the granted skill is not on the player");
            ManasSkillInstance i = inst.get();
            if (!(i.getSkill() instanceof JibrilSkill sk)) return Result.fail("the instance is not a Jibril skill");
            if (sk.getModes(i) != JudgmentRules.MODES) return Result.fail("the skill reports " + sk.getModes(i) + " modes");
            for (JudgmentRules.Mode m : JibrilRules.JIBRIL.modes())
                if (!m.id().equals(sk.getModeId(i, m.index()))) return Result.fail("mode " + m.index() + " is '" + sk.getModeId(i, m.index()) + "' not '" + m.id() + "'");
            if (VeldanavaSkills.grant(p, "jibril")) return Result.fail("a second grant went through");
            if (VeldanavaSkills.grant(p, "israfil")) return Result.fail("a second skill was granted to the same player");

            JudgmentState.write(p, JudgmentState.compose(3, 0b1001, false));
            if (!JudgmentState.onFor(p, 3, 0) || JudgmentState.onFor(p, 3, 1) || !JudgmentState.onFor(p, 3, 3)) return Result.fail("Jibril's mask reads wrong");
            if (JudgmentState.on(p, 0)) return Result.fail("Jibril's toggles switched Sandalphon's passives on");
            if (VeldanavaGuard.spatiallyFree(p) || VeldanavaGuard.mindImmune(p)) return Result.fail("Jibril's toggles switched Israfil's guards on");
            JudgmentState.write(p, JudgmentState.compose(2, 0b1001, false));
            if (JudgmentState.onFor(p, 3, 0) || JudgmentState.onFor(p, 3, 3)) return Result.fail("Israfil's toggles switched Jibril's passives on");
            JudgmentState.write(p, 0);

            String icon = "assets/nusmp/textures/skill/veldanava/jibril.png";
            if (JibrilSelfTests.class.getClassLoader().getResource(icon) == null) return Result.fail("missing " + icon);
            JsonObject lang;
            try (InputStream in = JibrilSelfTests.class.getResourceAsStream("/assets/nusmp/lang/en_us.json")) {
                if (in == null) return Result.fail("no en_us.json");
                lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            for (String k : new String[]{"nusmp.skill.jibril", "nusmp.skill.jibril.description", "nusmp.skill.jibril.mastered"}) if (!lang.has(k)) return Result.fail("missing lang key " + k);
            for (JudgmentRules.Mode m : JibrilRules.JIBRIL.modes())
                if (!lang.has("nusmp.skill.mode.jibril." + m.id())) return Result.fail("missing lang key for mode " + m.id());
            return Result.ok("registered, learned once, 10 modes named, the mask keeps the three skills apart, icon and lang are in the jar");
        } finally {
            JudgmentState.write(p, 0);
            VeldanavaSkills.revoke(p);
        }
    }

    private static Result conditions(MinecraftServer server) {
        ServerLevel sl = server.overworld();
        BlockPos at = sl.getSharedSpawnPos().above(3);
        FakePlayer p = SelfTests.standIn(sl, "jibril_cond", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        Mob z = EntityType.ZOMBIE.create(sl);
        Mob pig = EntityType.PIG.create(sl);
        if (z == null || pig == null) return Result.fail("could not make a test creature");
        z.moveTo(at.getX() + 3.5, at.getY(), at.getZ() + 0.5);
        sl.addFreshEntity(z);
        Vec3 center = new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        boolean mayfly = p.getAbilities().mayfly;
        try {
            // the wings give flight and take it back, and never touch a flight that is not theirs
            JibrilState.applyWings(p, true);
            if (!p.getAbilities().mayfly || !JibrilState.wingsGiven(p)) return Result.fail("the wings gave no flight");
            JibrilState.applyWings(p, false);
            if (p.getAbilities().mayfly != mayfly || JibrilState.wingsGiven(p)) return Result.fail("the wings did not give the flight back");
            p.getAbilities().mayfly = true;
            JibrilState.applyWings(p, false);
            if (!p.getAbilities().mayfly) return Result.fail("the wings took a flight that was not theirs");
            p.getAbilities().mayfly = mayfly;

            // Divine Rigor: a debuff does not land on its owner, and does land on anyone else
            JudgmentState.write(p, JudgmentState.compose(3, 0b0001, false));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            p.addEffect(new MobEffectInstance(JudgmentEffects.TEMPORAL_STUTTER, 100, 0));
            boolean landed = p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) || JudgmentEffects.has(p, JudgmentEffects.TEMPORAL_STUTTER);
            JudgmentState.write(p, 0);
            if (landed) return Result.fail("a debuff landed on an owner of Divine Rigor");
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            boolean without = p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN);
            p.removeAllEffects();
            if (!without) return Result.fail("Divine Rigor blocks a debuff without being on");
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            JudgmentState.write(p, JudgmentState.compose(3, 0b0001, false));
            if (JibrilState.cleanse(p, JibrilRules.RIGOR_CLEANSE_MAX) != 1 || p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) return Result.fail("cleansing did not lift the debuff");
            JudgmentState.write(p, 0);

            // Word of Law: the blow is negated and comes back (the handler is driven directly: a stand-in player is invulnerable to a real hit)
            float zBefore = z.getHealth();
            JudgmentState.write(p, JudgmentState.compose(3, 0, false));
            var plain = new LivingIncomingDamageEvent(p, new DamageContainer(sl.damageSources().mobAttack(z), 6f));
            JibrilEvents.onIncoming(plain);
            if (plain.isCanceled()) return Result.fail("a blow was parried without a parry window");
            JibrilState.openParry(p, 40);
            if (!JibrilState.parrying(p)) return Result.fail("the parry window did not open");
            var parried = new LivingIncomingDamageEvent(p, new DamageContainer(sl.damageSources().mobAttack(z), 6f));
            JibrilEvents.onIncoming(parried);
            boolean closed = !JibrilState.parrying(p);
            float zAfter = z.getHealth();
            JudgmentState.write(p, 0);
            JibrilState.closeParry(p);
            if (!parried.isCanceled() || !closed) return Result.fail("the parry did not negate the blow");
            if (zAfter >= zBefore) return Result.fail("the parry returned nothing to the attacker");

            // the holy hit, and what Heavenly Presence burns
            float zNow = z.getHealth();
            if (!JibrilState.holy(p, z, 20f, 1.0) || z.getHealth() >= zNow) return Result.fail("the holy hit did nothing");
            if (!JibrilSkill.burns(z) || JibrilSkill.burns(pig)) return Result.fail("Heavenly Presence burns the wrong creatures");

            // stealing the good effects
            p.removeAllEffects();
            z.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 1));
            if (JibrilState.steal(z, p, JibrilRules.SHACKLE_STEAL_MAX) != 1 || z.hasEffect(MobEffects.DAMAGE_BOOST) || !p.hasEffect(MobEffects.DAMAGE_BOOST)) return Result.fail("Holy Shackles did not move a good effect");
            p.removeAllEffects();

            // the hotbar lock: held for a player who is online only, dropped for the rest, gone on request
            JibrilState.lockHotbar(p, 40);
            if (!JibrilState.hotbarLocked(p.getUUID()) || JibrilState.lockCount() < 1) return Result.fail("the hotbar was not locked");
            JibrilState.tickHotbar(server);
            if (JibrilState.hotbarLocked(p.getUUID())) return Result.fail("the lock of a player who is not online was kept");
            JibrilState.lockHotbar(p, 40);
            JibrilState.unlockHotbar(p.getUUID());
            if (JibrilState.hotbarLocked(p.getUUID())) return Result.fail("the hotbar lock was not lifted");

            // the sanctuary registry and the rigor aura clean up
            JibrilState.kingdom(p);
            JibrilState.aura(p, true);
            if (JibrilState.kingdomCount() != 1 || JibrilState.auraCount() != 1) return Result.fail("the sanctuary or the aura was not recorded");
            JibrilState.clear(p);
            if (JibrilState.kingdomCount() != 0 || JibrilState.auraCount() != 0) return Result.fail("clearing the skill left the sanctuary or the aura behind");

            // Strict Governance: a running cooldown is put back by one second, once per second, on every other second
            var skills = SkillAPI.getSkillsFrom(p);
            VeldanavaSkills.revoke(p);
            if (!VeldanavaSkills.grant(p, "sandalphon")) return Result.fail("could not give the test player a skill with a cooldown");
            var inst = skills.getSkill(com.newuniverse.nusmp.skill.NUSkills.SANDALPHON.getId());
            if (inst.isEmpty()) return Result.fail("the test skill is not on the player");
            inst.get().setCoolDown(10, 4);
            boolean second = JibrilRules.governanceSecond(sl.getGameTime());
            int changed = JibrilState.stretchCooldowns(p), again = JibrilState.stretchCooldowns(p);
            int cd = inst.get().getCoolDown(4);
            if (second && (changed != 1 || cd != 11)) return Result.fail("the cooldown was not stretched (" + changed + ", " + cd + ")");
            if (!second && (changed != 0 || cd != 10)) return Result.fail("the cooldown was stretched on a second that is not stretched (" + changed + ", " + cd + ")");
            if (again != 0) return Result.fail("the cooldown was stretched twice in one second");

            // the Kingdom is a field of its own kind: capped, ended by a logout, and its ending drops the sanctuary
            JudgmentFields.clearAll();
            JudgmentFields.add(p, sl, p, center, JibrilRules.KINGDOM_RADIUS, 100000, new JibrilSkill.Kingdom(0.0), JibrilSkill.KINGDOM_KIND);
            JudgmentFields.Field f = JudgmentFields.find(p.getUUID(), JibrilSkill.KINGDOM_KIND);
            if (f == null) return Result.fail("the Kingdom is not a field");
            if (f.endsAt - sl.getGameTime() > JudgmentFields.MAX_TICKS + 1) return Result.fail("the Kingdom can outlast the cap");
            JibrilState.kingdom(p);
            if (JudgmentFields.clearKind(server, p.getUUID(), JibrilSkill.KINGDOM_KIND) != 1 || JibrilState.kingdomCount() != 0) return Result.fail("ending the Kingdom left the sanctuary standing");

            return Result.ok("wings, rigor, parry, holy hit, steal, hotbar lock, sanctuary, governance and the Kingdom field hold");
        } finally {
            JudgmentFields.clearAll();
            JibrilState.clearAll();
            JibrilState.clear(p);
            p.getAbilities().mayfly = mayfly;
            p.removeAllEffects();
            JibrilState.closeParry(p);
            JudgmentState.write(p, 0);
            VeldanavaSkills.revoke(p);
            z.discard();
        }
    }
}
