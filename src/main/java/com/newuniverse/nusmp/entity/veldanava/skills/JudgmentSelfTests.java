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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * The CI server smoke test of the Veldanava ultimate skills (prefix {@code sandalphon} in SMOKE_EXPECT): {@code sandalphon.rules} (the mode table, the cards, the numbers and the caps),
 * {@code sandalphon.skill} (the skill is registered, can be learned, has ten modes with the right names, its toggles survive a save, and the icon and lang keys are in the jar) and
 * {@code sandalphon.conditions} (Temporal Debt stacks, is capped and detonates, the no-heal mark, the Judgment Sigil, the field limits, the execution settings). They run on the throw-away
 * world and clean up after themselves.
 */
public final class JudgmentSelfTests {
    private JudgmentSelfTests() {}

    private static boolean registered;

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("sandalphon.rules", JudgmentSelfTests::rules);
        SelfTests.register("sandalphon.skill", JudgmentSelfTests::skill);
        SelfTests.register("sandalphon.conditions", JudgmentSelfTests::conditions);
    }

    // ---------------------------------------------------------------- rules (pure)
    private static Result rules(MinecraftServer server) {
        JudgmentRules.Spec s = JudgmentRules.SANDALPHON;
        if (s.count() != JudgmentRules.MODES) return Result.fail("expected " + JudgmentRules.MODES + " modes, found " + s.count());
        int toggles = 0, actives = 0, held = 0;
        for (JudgmentRules.Mode m : s.modes()) {
            if (m.index() != s.modes().indexOf(m)) return Result.fail("mode " + m.id() + " has the wrong index");
            if (m.toggle()) toggles++;
            else if (m.kind() == JudgmentRules.Kind.HELD) held++;
            else actives++;
            if (!m.toggle() && m.masteredCost() > m.cost()) return Result.fail(m.id() + " costs more once mastered");
            if (!m.toggle() && m.cost() <= 0) return Result.fail(m.id() + " is free");
        }
        if (toggles != 4 || actives != 5 || held != 1) return Result.fail("the shape is " + toggles + " toggles, " + actives + " actives, " + held + " held (expected 4/5/1)");
        if (s.mode(9).cooldownSeconds() != 120) return Result.fail("Final Judgment does not cool down for 120 seconds");
        if (JudgmentRules.cost(s.mode(4), false, 1.0) != 2500 || JudgmentRules.cost(s.mode(4), true, 1.0) != 1800) return Result.fail("the Bullet cost is not 2.5K/1.8K");
        if (JudgmentRules.cost(s.mode(4), false, 0.0) != 0 || JudgmentRules.cost(s.mode(4), false, -3.0) != 0) return Result.fail("a zero or negative cost scale gives a cost");
        if (JudgmentRules.CARDS.size() != 5) return Result.fail("the reward screen does not have five cards");
        if (!JudgmentRules.claimable("sandalphon")) return Result.fail("Sandalphon cannot be claimed");
        if (!JudgmentRules.claimable("israfil")) return Result.fail("Israfil cannot be claimed");
        for (String sealed : new String[]{"jibril", "solomon", "hercules", "", "nonsense", null})
            if (JudgmentRules.claimable(sealed)) return Result.fail("'" + sealed + "' should not be claimable in this release");
        for (JudgmentRules.Spec sp : JudgmentRules.SPECS)
            for (JudgmentRules.Mode m : sp.modes()) {
                if (m.toggle()) continue;
                if (m.cooldownSeconds() <= 0) return Result.fail(sp.id() + " " + m.id() + " has no cooldown");
                if (m.masteredCooldownSeconds() > m.cooldownSeconds()) return Result.fail(sp.id() + " " + m.id() + " cools down slower once mastered");
            }
        if (JudgmentRules.demiseMultiplier(1.0) != 1.0 || JudgmentRules.demiseMultiplier(0.0) != 2.0 || JudgmentRules.demiseMultiplier(-4) != 2.0 || JudgmentRules.demiseMultiplier(9) != 1.0) return Result.fail("the Demise multiplier is wrong");
        if (JudgmentRules.magicVulnerability(0) != 1.0 || JudgmentRules.magicVulnerability(1000) != 1.5) return Result.fail("the magic vulnerability is not capped at +50%");
        if (JudgmentRules.armourPierce(1000) != JudgmentRules.ARMOUR_PIERCE_MAX || JudgmentRules.armourPierce(-5) != 0) return Result.fail("the armour pierce is not clamped");
        if (JudgmentRules.detonation(10, 20, 5, 200, 50000) != 1000.0 || JudgmentRules.detonation(0, 20, 5, 200, 50000) != 200.0 || JudgmentRules.detonation(1000, 20, 5, 200, 50000) != 50000.0) return Result.fail("the detonation numbers are wrong");
        if (!JudgmentRules.atOrBelow(20, 100, 0.2) || JudgmentRules.atOrBelow(21, 100, 0.2) || JudgmentRules.atOrBelow(1, 0, 0.2)) return Result.fail("the execute threshold is wrong");
        if (!JudgmentRules.looksAt(0, 0, 1, 0, 0, 5, JudgmentRules.GAZE_COSINE) || JudgmentRules.looksAt(0, 0, 1, 5, 0, 1, JudgmentRules.GAZE_COSINE)) return Result.fail("the gaze cone is wrong");
        if (!JudgmentRules.inCone(0, 1, 0, 3, 5.5, 65) || JudgmentRules.inCone(0, 1, 0, -3, 5.5, 65) || JudgmentRules.inCone(0, 1, 0, 9, 5.5, 65)) return Result.fail("the cleave cone is wrong");
        int mask = 0;
        mask = JudgmentRules.flip(mask, 1);
        mask = JudgmentRules.flip(mask, 3);
        if (!JudgmentRules.on(mask, 1) || !JudgmentRules.on(mask, 3) || JudgmentRules.on(mask, 0) || JudgmentRules.flip(JudgmentRules.flip(mask, 1), 1) != mask) return Result.fail("the toggle bits are wrong");
        if (JudgmentRules.bit(4) != 0 || JudgmentRules.bit(-1) != 0) return Result.fail("an active mode has a toggle bit");
        return Result.ok("10 modes (4/5/1), 5 cards (2 open), cooldowns, numbers and caps hold");
    }

    // ---------------------------------------------------------------- the skill
    private static Result skill(MinecraftServer server) throws Exception {
        ServerLevel sl = server.overworld();
        BlockPos at = sl.getSharedSpawnPos().above(3);
        FakePlayer p = SelfTests.standIn(sl, "sandalphon_skill", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        VeldanavaSkills.revoke(p);
        try {
            var holder = VeldanavaSkills.holder("sandalphon");
            if (holder == null || !(holder.get() instanceof SandalphonSkill)) return Result.fail("Sandalphon is not registered");
            if (!VeldanavaSkills.grant(p, "sandalphon")) return Result.fail("Sandalphon could not be granted");
            Optional<ManasSkillInstance> inst = SkillAPI.getSkillsFrom(p).getSkill(VeldanavaSkills.holder("sandalphon").getId());
            if (inst.isEmpty()) return Result.fail("the granted skill is not on the player");
            ManasSkillInstance i = inst.get();
            if (!(i.getSkill() instanceof SandalphonSkill sk)) return Result.fail("the instance is not a Sandalphon skill");
            if (sk.getModes(i) != JudgmentRules.MODES) return Result.fail("the skill reports " + sk.getModes(i) + " modes");
            for (JudgmentRules.Mode m : JudgmentRules.SANDALPHON.modes())
                if (!m.id().equals(sk.getModeId(i, m.index()))) return Result.fail("mode " + m.index() + " is '" + sk.getModeId(i, m.index()) + "' not '" + m.id() + "'");
            if (VeldanavaSkills.grant(p, "sandalphon")) return Result.fail("a second grant went through");
            if (VeldanavaSkills.grant(p, "jibril")) return Result.fail("a sealed skill was granted");
            JudgmentState.setToggles(i, 0b1010);
            if (JudgmentState.toggles(i) != 0b1010) return Result.fail("the toggles were not kept in the skill");
            JudgmentState.setToggles(i, 0xFFFF);
            if (JudgmentState.toggles(i) != JudgmentState.TOGGLES) return Result.fail("an out-of-range toggle bit was kept");
            JudgmentState.write(p, JudgmentState.compose(1, 0b0101, true));
            if (!JudgmentState.on(p, 0) || JudgmentState.on(p, 1) || !JudgmentState.on(p, 2) || !JudgmentState.mastered(p)) return Result.fail("the player mask reads wrong");
            JudgmentState.write(p, 0);
            if (JudgmentState.mask(p) != 0) return Result.fail("a zero mask was kept");
            Mob mob = EntityType.ZOMBIE.create(sl);
            if (mob == null || JudgmentState.mask(mob) != 0) return Result.fail("a mob has a toggle mask");

            String icon = "assets/nusmp/textures/skill/veldanava/sandalphon.png";
            if (JudgmentSelfTests.class.getClassLoader().getResource(icon) == null) return Result.fail("missing " + icon);
            JsonObject lang;
            try (InputStream in = JudgmentSelfTests.class.getResourceAsStream("/assets/nusmp/lang/en_us.json")) {
                if (in == null) return Result.fail("no en_us.json");
                lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            String[] need = {"nusmp.skill.sandalphon", "nusmp.skill.sandalphon.description", "effect.nusmp.temporal_debt", "effect.nusmp.action_lock", "nusmp.veldanava.skills.title"};
            for (String k : need) if (!lang.has(k)) return Result.fail("missing lang key " + k);
            for (JudgmentRules.Mode m : JudgmentRules.SANDALPHON.modes())
                if (!lang.has("nusmp.skill.mode.sandalphon." + m.id())) return Result.fail("missing lang key for mode " + m.id());
            for (JudgmentRules.Card c : JudgmentRules.CARDS) if (c.title().isBlank() || c.epithet().isBlank()) return Result.fail("card " + c.id() + " has no title");
            return Result.ok("registered, learned once, 10 modes named, toggles and mask hold, icon and lang are in the jar");
        } finally {
            JudgmentState.write(p, 0);
            VeldanavaSkills.revoke(p);
        }
    }

    // ---------------------------------------------------------------- the conditions on a real server
    private static Result conditions(MinecraftServer server) {
        ServerLevel sl = server.overworld();
        BlockPos at = sl.getSharedSpawnPos().above(3);
        FakePlayer p = SelfTests.standIn(sl, "sandalphon_cond", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        Mob z = EntityType.ZOMBIE.create(sl);
        if (z == null) return Result.fail("could not make a test creature");
        z.moveTo(at.getX() + 3.5, at.getY(), at.getZ() + 0.5);
        sl.addFreshEntity(z);
        try {
            if (TemporalDebt.get(z) != 0) return Result.fail("a fresh creature carries debt");
            if (TemporalDebt.add(z, 5) != 5 || TemporalDebt.get(z) != 5) return Result.fail("5 stacks were not kept");
            if (TemporalDebt.add(z, 3) != 8) return Result.fail("stacks did not add up");
            if (TemporalDebt.add(z, 100000) != Math.min(JudgmentConfig.debtMax(), 100008)) return Result.fail("the debt is not capped");
            TemporalDebt.clear(z);
            TemporalDebt.add(z, 10);
            double dmg = TemporalDebt.detonate(z, 5.0, 200.0, 50000.0);
            double want = Math.min(50000.0, Math.max(200.0, 10 * JudgmentConfig.debtDamage() * 5.0));
            if (Math.abs(dmg - want) > 1.0E-6) return Result.fail("detonation " + dmg + " is not " + want);
            if (TemporalDebt.get(z) != 0) return Result.fail("the debt stayed after the detonation");
            if (TemporalDebt.add(z, 0) != 0 || TemporalDebt.add(z, -4) != 0) return Result.fail("a non-positive add changed the debt");

            if (JudgmentDamage.noHeal(z)) return Result.fail("a fresh creature cannot heal");
            JudgmentDamage.markNoHeal(z, 40);
            if (!JudgmentDamage.noHeal(z)) return Result.fail("the no-heal mark was not kept");

            JudgmentEffects.silence(z, 20);
            if (!JudgmentEffects.has(z, JudgmentEffects.EVENT_SILENCE)) return Result.fail("silence did not land");
            if (!z.getPersistentData().contains("nusmp_sealed_until")) return Result.fail("silence did not set the seal flag the casts read");

            JudgmentDamage.mark(p, z);
            if (!JudgmentDamage.hasSigil(z)) return Result.fail("the sigil was not kept");
            JudgmentDamage.clearSigil(z);
            if (JudgmentDamage.hasSigil(z)) return Result.fail("the sigil was not cleared");

            if (JudgmentDamage.trueActive()) return Result.fail("the true-damage flag is stuck on");
            if (!JudgmentDamage.mayExecute(z)) return Result.fail("an ordinary mob cannot be executed");
            if (JudgmentDamage.mayExecute(p) != JudgmentConfig.executePlayers()) return Result.fail("the player execution setting is ignored");

            // field limits: a player never keeps more than the per-player cap, the server never more than the total
            JudgmentFields.clearAll();
            int per = Math.max(1, JudgmentConfig.fieldsPerPlayer());
            for (int k = 0; k < per + 3; k++) JudgmentFields.add(p, sl, null, new Vec3(at.getX(), at.getY(), at.getZ()), 4.0, 100);
            if (JudgmentFields.countOf(p.getUUID()) != per) return Result.fail("the player holds " + JudgmentFields.countOf(p.getUUID()) + " fields, the cap is " + per);
            JudgmentFields.onLogout(p.getUUID());
            if (JudgmentFields.countOf(p.getUUID()) != 0) return Result.fail("fields stayed after the owner left");
            JudgmentFields.add(p, sl, null, new Vec3(at.getX(), at.getY(), at.getZ()), 4.0, 100);
            if (JudgmentFields.clearAround(sl, new Vec3(at.getX(), at.getY(), at.getZ()), 5.0, p.getUUID()) != 0) return Result.fail("an edict ended the caster's own field");
            if (JudgmentFields.clearAround(sl, new Vec3(at.getX(), at.getY(), at.getZ()), 5.0) != 1) return Result.fail("an edict did not end a field in its sphere");
            return Result.ok("debt stacks, caps and detonates; no-heal, silence, sigil, execution settings and field caps hold");
        } finally {
            JudgmentFields.clearAll();
            JudgmentDamage.clearSigil(z);
            TemporalDebt.clear(z);
            z.discard();
        }
    }
}
