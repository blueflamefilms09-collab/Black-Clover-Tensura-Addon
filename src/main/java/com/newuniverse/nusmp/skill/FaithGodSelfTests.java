package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.rules.FaithGodRules;
import com.newuniverse.nusmp.skill.rules.FaithGodRules.God;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.server.MinecraftServer;

import java.util.HashSet;
import java.util.Set;

/**
 * Why: the eight Faith gods (docs/faith_gods.md) add 16 skills, a ritual command and server settings that nobody can watch in a game here, so the CI server smoke test checks them (prefix {@code gods}):
 * {@code gods.rules} (the live SERVER numbers are inside their clamps, the pure table and the ritual answer sanely with them) and {@code gods.skills} (every one of the 16 ids is registered as a
 * {@link FaithGodSkill} with the mode count its class table gives, mode ids are unique, the two ritual commands and the admin command exist, the old Faith and Goddess ids are untouched). Registry reads and
 * stand-in instances that never belong to a player: no world, no tick work.
 */
public final class FaithGodSelfTests {
    private FaithGodSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("gods.rules", s -> rules());
        SelfTests.register("gods.skills", FaithGodSelfTests::skills);
    }

    private static Result rules() {
        if (FaithGodRules.GODS.size() != 8) return Result.fail("expected 8 gods, found " + FaithGodRules.GODS.size());
        Set<String> franchises = new HashSet<>(), ids = new HashSet<>();
        for (God g : FaithGodRules.GODS) {
            if (!franchises.add(g.franchise())) return Result.fail("two gods from one franchise: " + g.franchise());
            if (!ids.add(g.uniqueId()) || !ids.add(g.ultimateId())) return Result.fail("a god id repeats: " + g.id());
            if (g.moves().size() != FaithGodRules.POOL_ACTIVES || g.passives().size() != FaithGodRules.POOL_PASSIVES) return Result.fail(g.id() + " has the wrong number of abilities");
            if (FaithGodRules.activeCount(g, true) < FaithGodRules.activeCount(g, false) || FaithGodRules.passiveCount(g, true) < FaithGodRules.passiveCount(g, false)) return Result.fail(g.id() + ": the god form has fewer abilities than the Unique");
        }
        if (NUConfig.faithGodMaxLines() < 1 || NUConfig.faithGodMaxLines() > 8) return Result.fail("maxGodLines out of its clamp");
        if (NUConfig.faithGodPowerPercent() < 10 || NUConfig.faithGodPowerPercent() > 300) return Result.fail("powerPercent out of its clamp");
        if (NUConfig.faithGodDevotionPercent() < 0 || NUConfig.faithGodDevotionPercent() > 20) return Result.fail("devotionPercent out of its clamp");
        if (FaithGodRules.devotion(1_000_000, 20) > 1.0 + FaithGodRules.DEVOTION_CAP) return Result.fail("devotion is not capped");
        if (FaithGodRules.ritual(false, true, false, 0, 1, true, true, true, 4, 99, 1) != FaithGodRules.Ritual.DISABLED) return Result.fail("the ritual runs while the gods are switched off");
        if (FaithGodRules.ritual(true, true, false, 0, 1, true, true, true, 2, 99, 1) != FaithGodRules.Ritual.FEW_PROOFS) return Result.fail("the ritual accepts two proofs");
        return Result.ok("8 gods from 8 franchises, enabled=" + NUConfig.faithGodsEnabled() + ", maxLines=" + NUConfig.faithGodMaxLines() + ", epGate%=" + NUConfig.faithGodEpGatePercent());
    }

    private static Result skills(MinecraftServer server) {
        int checked = 0;
        for (God g : FaithGodRules.GODS) {
            for (boolean ult : new boolean[]{false, true}) {
                ManasSkill sk = SkillAPI.getSkillRegistry().get((ult ? FaithGods.ULTIMATE : FaithGods.UNIQUE).get(g.id()).getId());
                if (!(sk instanceof FaithGodSkill fs)) return Result.fail((ult ? g.ultimateId() : g.uniqueId()) + " is not registered as a FaithGodSkill");
                ManasSkillInstance inst = fs.createDefaultInstance();
                int want = FaithGodRules.activeCount(g, ult);
                if (fs.getModes(inst) != want) return Result.fail(g.id() + (ult ? " god form" : " Unique") + " has " + fs.getModes(inst) + " modes, expected " + want);
                Set<String> seen = new HashSet<>();
                for (int m = 0; m < want; m++) if (!seen.add(fs.getModeId(inst, m))) return Result.fail(g.id() + " repeats a mode id: " + fs.getModeId(inst, m));
                if (ult && fs.ultimateClass() != g.cls()) return Result.fail(g.id() + ": the god form is not " + g.cls());
                if (!ult && fs.ultimateClass() != null) return Result.fail(g.id() + ": the Unique must be unclassified");
                checked++;
            }
        }
        if (SkillAPI.getSkillRegistry().get(NUSkills.FAITH.getId()) == null || SkillAPI.getSkillRegistry().get(NUSkills.GODDESS_OF_FAITH.getId()) == null) return Result.fail("faith or goddess_of_faith is gone");
        var root = server.getCommands().getDispatcher().getRoot();
        var faith = root.getChild("faith");
        if (faith == null || faith.getChild("gods") == null || faith.getChild("ascend") == null || faith.getChild("status") == null) return Result.fail("/faith gods or /faith ascend is missing (or the worship commands were lost in the merge)");
        var admin = root.getChild("anime_multiverse");
        if (admin == null || admin.getChild("faithgod") == null || admin.getChild("faith") == null) return Result.fail("/anime_multiverse faithgod (or the old faith admin command) is missing");
        if (FaithGods.openState() > FaithGods.MAX_CLOCKS) return Result.fail("the ritual clocks are not bounded");
        return Result.ok(checked + " god skills registered with their class mode counts, commands ok, clocks " + FaithGods.openState());
    }
}
