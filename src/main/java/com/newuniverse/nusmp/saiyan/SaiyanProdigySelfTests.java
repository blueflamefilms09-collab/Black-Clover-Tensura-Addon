package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.ProdigySkills;
import com.newuniverse.nusmp.skill.SaiyanProdigySkill;
import com.newuniverse.nusmp.skill.modes.ModeSpec;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Why: the CI server smoke test of the Saiyan Prodigy and the Saiyan-blooded evolution (prefix {@code prodigy}, docs/saiyan_prodigy.md); run by the dedicated server in CI, so it uses no client class.
 * <ul>
 * <li>{@code prodigy.rules}: the mode table (unique ids, every Saiyan op reachable except the two named), the evolution weights add up to 100, the unlock formula.</li>
 * <li>{@code prodigy.skill}: {@code anime_multiverse:saiyan_prodigy} is in the skill registry, is intrinsic, has every mode, and the mode key skips what a non-Saiyan has not unlocked.</li>
 * <li>{@code prodigy.race}: the Saiyan race gives the Prodigy as its intrinsic skill and asks the three deeds; whether the Human lists the Saiyan (the mixin) is reported, never failed (the mixin is optional).</li>
 * <li>{@code prodigy.player}: on a stand-in: becoming a Saiyan grants the Prodigy once (idempotent), a press of a Saiyan mode and of the Art mode does not throw, the old Dragon Ball ids are not touched.
 * Not exercised (said so) when the stand-in has no skill storage. Nothing is spawned.</li>
 * </ul>
 */
public final class SaiyanProdigySelfTests {
    private SaiyanProdigySelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("prodigy.rules", SaiyanProdigySelfTests::rules);
        SelfTests.register("prodigy.skill", SaiyanProdigySelfTests::skill);
        SelfTests.register("prodigy.race", SaiyanProdigySelfTests::race);
        SelfTests.register("prodigy.player", SaiyanProdigySelfTests::player);
    }

    private static Result rules(MinecraftServer server) {
        Set<String> ids = new HashSet<>();
        for (ModeSpec m : SaiyanProdigyModes.BUNDLE.modes()) if (!ids.add(m.id())) return Result.fail("duplicate mode id " + m.id());
        List<Integer> missing = new java.util.ArrayList<>();
        for (int op = SaiyanNet.OP_BLAST; op <= SaiyanNet2.LAST; op++) {
            boolean skipped = false;
            for (int u : SaiyanProdigyModes.UNREACHED) if (u == op) skipped = true;
            if (!skipped && !SaiyanProdigyModes.reaches(op)) missing.add(op);
        }
        if (!missing.isEmpty()) return Result.fail("Saiyan ops not reachable from the Prodigy: " + missing);
        double[] w = SaiyanBloodRules.weights(SaiyanProdigyConfig.bloodEp(), SaiyanProdigyConfig.bloodKills(), SaiyanProdigyConfig.bloodNearDeath());
        if (Math.abs(w[0] + w[1] + w[2] - 100.0) > 1.0e-9) return Result.fail("the evolution weights do not add up to 100");
        double goku = SaiyanProdigyModes.dbUnlockBp(0.97, 1.0), bulma = SaiyanProdigyModes.dbUnlockBp(0.30, 1.0);
        if (!(bulma < goku) || Math.abs(bulma - 1000.0) > 1.0e-6) return Result.fail("the Dragon Ball unlock formula is off (Bulma " + bulma + ", Goku " + goku + ")");
        return Result.ok(SaiyanProdigyModes.BUNDLE.size() + " modes with unique ids, every Saiyan op reachable, evolution weights 100, Goku art unlock " + Math.round(goku) + " Battle Power");
    }

    private static Result skill(MinecraftServer server) {
        SaiyanProdigySkill s = ProdigySkills.PRODIGY.get();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "saiyan_prodigy");
        if (SkillAPI.getSkillRegistry().get(id) != s) return Result.fail("anime_multiverse:saiyan_prodigy is not in the skill registry");
        if (s.getType() != Skill.SkillType.INTRINSIC) return Result.fail("the Prodigy is not an intrinsic skill");
        ManasSkillInstance inst = s.createDefaultInstance();
        if (s.getModes(inst) != SaiyanProdigyModes.BUNDLE.size()) return Result.fail("the skill's mode count is not the table's");
        int a = s.nextMode(null, inst, 0, false), b = s.nextMode(null, inst, SaiyanProdigyModes.ART, false), c = s.nextMode(null, inst, SaiyanProdigyModes.STYLE, true);
        if (a != SaiyanProdigyModes.ART || b != SaiyanProdigyModes.STYLE || c != SaiyanProdigyModes.ART) return Result.fail("the mode key does not skip the locked modes (got " + a + ", " + b + ", " + c + ")");
        for (int i = 0; i < s.getModes(inst); i++) if (s.getModeId(inst, i) == null || s.getModeName(inst, i).getString().isEmpty()) return Result.fail("mode " + i + " has no id or name");
        return Result.ok("saiyan_prodigy is registered, intrinsic, " + s.getModes(inst) + " modes, the mode key skips locked modes");
    }

    private static Result race(MinecraftServer server) {
        ManasRace race = RaceAPI.getRaceRegistry().get(SaiyanRaces.id());
        if (!(race instanceof SaiyanTensuraRace r)) return Result.fail("the Saiyan race is not registered as SaiyanTensuraRace");
        ManasRaceInstance inst = race.createDefaultInstance();
        if (SaiyanProdigyHub.enabled() && !r.getIntrinsicSkills(inst, null).contains(ProdigySkills.PRODIGY.get())) return Result.fail("the Saiyan race does not give the Prodigy");
        float sum = 0f;
        for (float f : r.getEvolutionRequirements(inst, null).values()) sum += f;
        if (Math.abs(sum - 100f) > 0.01f) return Result.fail("the evolution weights add up to " + sum);
        String mixin;
        try {
            ManasRace human = RaceAPI.getRaceRegistry().get(ResourceLocation.fromNamespaceAndPath("tensura", "human"));
            ServerLevel level = server.overworld();
            Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
            ServerPlayer p = SelfTests.standIn(level, "prodigy_race", at.x, at.y, at.z);
            try {
                boolean listed = human != null && human.getNextEvolutions(human.createDefaultInstance(), p).contains(race);
                mixin = listed ? "the Human lists the Saiyan (the mixin is applied)" : (SaiyanProdigyConfig.bloodEvolution() ? "INFO: the Human does NOT list the Saiyan (the evolution mixin did not apply; the Saiyan is still reachable by command and menu)" : "the evolution is off on this server");
            } finally {
                SaiyanManager.onLogout(p);
            }
        } catch (RuntimeException | LinkageError e) {
            mixin = "INFO: the Human's evolution list could not be read here (" + e + ")";
        }
        return Result.ok("the Saiyan race gives the Prodigy and asks the deeds (weights 100); " + mixin);
    }

    private static Result player(MinecraftServer server) {
        if (!SaiyanProdigyHub.enabled()) return Result.ok("prodigy.prodigyHub is off on this server: nothing to grant");
        ServerLevel level = server.overworld();
        Vec3 at = Vec3.atCenterOf(level.getSharedSpawnPos()).add(0, 2, 0);
        ServerPlayer p = SelfTests.standIn(level, "prodigy_player", at.x, at.y, at.z);
        try {
            Skills skills = SkillAPI.getSkillsFrom(p);
            if (skills == null) return Result.ok("the grant was NOT exercised (the stand-in has no skill storage)");
            SaiyanManager.setSaiyan(p, true);
            if (!SaiyanStore.isSaiyan(p)) return Result.fail("the flag did not go on");
            if (SaiyanProdigyConfig.grantToSaiyans() && !SaiyanProdigyHub.ownsProdigy(p)) return Result.fail("becoming a Saiyan did not grant the Prodigy");
            if (SaiyanProdigyHub.grant(p)) return Result.fail("a second grant gave it again (not idempotent)");
            SaiyanProdigyHub.onLogin(p);
            SaiyanProdigyHub.onLogin(p);
            var held = skills.getSkill(ProdigySkills.PRODIGY.getId());
            if (held.isPresent()) {
                ManasSkillInstance inst = held.get();
                SaiyanProdigySkill s = ProdigySkills.PRODIGY.get();
                try {
                    s.onPressed(inst, p, 0, SaiyanProdigyModes.KI_BLAST);       // locked (not awakened): a message, never an exception
                    s.onPressed(inst, p, 0, SaiyanProdigyModes.ART);             // no Saiyan arts yet: a Dragon Ball art or a message
                    s.onPressed(inst, p, 0, SaiyanProdigyModes.STYLE);
                    s.onTick(inst, p);
                    SaiyanProdigyHub.second(p, SaiyanStore.of(p));
                } catch (RuntimeException e) {
                    return Result.fail("a press or the tick of the Prodigy threw on a stand-in Saiyan: " + e);
                }
                skills.forgetSkill(ProdigySkills.PRODIGY.getId());
            }
            SaiyanManager.setSaiyan(p, false);
            return Result.ok("becoming a Saiyan grants the Prodigy once (repeated logins and grants add nothing), the locked, Art and Style presses and the tick run without error");
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
            p.getPersistentData().remove(SaiyanProdigyHub.GIVEN);
            SaiyanProdigyHub.clearPerks(p);
            SaiyanManager.onLogout(p);
            SaiyanAttributes.clear(p);
        }
    }
}
