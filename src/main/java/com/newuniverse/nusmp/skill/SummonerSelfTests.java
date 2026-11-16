package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import com.newuniverse.nusmp.skill.rules.SummonerRules;
import com.newuniverse.nusmp.skill.rules.Tier;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.server.MinecraftServer;

/**
 * Why: the Summoner rework (docs/summoner_rework.md) folds Hawk Eye and Spirit Card into the Summoner, adds modes and a login-free migration, and renames the
 * Ultimate; nobody can watch that in a game here, so the CI server smoke test checks it: key {@code summoner.rules} (the live SERVER settings are inside their
 * clamps, the unlock order and the rank ceiling hold) and {@code summoner.skills} (the Summoner and the Summoner of Beginnings are registered with their ids, the
 * old mode numbers kept, the mode lists, the old Hawk Eye / Spirit Card ids still registered, the evolution path kept, the Ultimate's new name). Registry reads and a
 * stand-in instance that never belongs to a player: no world, no tick work.
 */
public final class SummonerSelfTests {
    private SummonerSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("summoner.rules", s -> rules());
        SelfTests.register("summoner.skills", SummonerSelfTests::skills);
    }

    private static Result rules() {
        double pct = NUConfig.summonerMasteryPercent();
        if (pct < 0.0 || pct > 5.0) return Result.fail("summonerMasteryPercent out of its clamp: " + pct);
        double prev = -1;
        for (int mode : new int[]{SummonerRules.BIRD, SummonerRules.STRENGTHEN, SummonerRules.SYNTHESIS, SummonerRules.SPIRIT}) {
            double at = SummonerRules.unlockAt(mode);
            if (at <= prev || at >= SummonerRules.REFLECT_AT) return Result.fail("the unlock order is broken at mode " + mode);
            prev = at;
        }
        if (SummonerRules.unlocked(SummonerRules.SPIRIT, 0.0, false, false)) return Result.fail("the Spirit Card is open at no mastery");
        if (!SummonerRules.unlocked(SummonerRules.SPIRIT, 0.0, true, false) || !SummonerRules.unlocked(SummonerRules.BIRD, 0.0, false, true)) return Result.fail("the Ultimate or a carried-over holder is locked out");
        if (SummonerRules.maxRank(Tier.ULTIMATE) <= SummonerRules.maxRank(Tier.BASE)) return Result.fail("the Ultimate's synthesis ceiling is not higher");
        if (SummonerRules.canSynthesize(SummonerRules.maxRank(Tier.BASE), Tier.BASE)) return Result.fail("synthesis passes its rank ceiling");
        double gain = SummonerRules.castGain(10_000, 1, pct, true);
        if (gain < 1.0 || gain > 10_000 * 0.05 + 1e-9) return Result.fail("a cast's mastery gain is out of bounds: " + gain);
        return Result.ok("rework=" + NUConfig.summonerRework() + ", masteryPercent=" + pct);
    }

    private static Result skills(MinecraftServer server) {
        ManasSkill base = SkillAPI.getSkillRegistry().get(NUSkills.SUMMONER.getId());
        ManasSkill lord = SkillAPI.getSkillRegistry().get(NUSkills.LORD_OF_SUMMONS.getId());
        if (!(base instanceof SummonerSkill b)) return Result.fail("summoner is not registered as a SummonerSkill");
        if (!(lord instanceof LordOfSummonsSkill l)) return Result.fail("lord_of_summons is not registered as a LordOfSummonsSkill");
        if (!"lord_of_summons".equals(NUSkills.LORD_OF_SUMMONS.getId().getPath())) return Result.fail("the Ultimate's registry id changed");
        if (!(SkillAPI.getSkillRegistry().get(NUSkills.HAWK_EYE.getId()) instanceof HawkEyeSkill)) return Result.fail("hawk_eye lost its registration");
        if (!(SkillAPI.getSkillRegistry().get(NUSkills.SPIRIT_CARD.getId()) instanceof SpiritCardSkill)) return Result.fail("spirit_card lost its registration");
        if (!"Summoner of Beginnings".equals(LordOfSummonsSkill.LORD_TEXT.name())) return Result.fail("the Ultimate is not called Summoner of Beginnings");
        if (SummonerSkill.BEAST != 0 || SummonerSkill.STONE != 1 || SummonerSkill.RECALL != 2 || SummonerSkill.LEGION != 3 || SummonerSkill.WRAITH != 4) return Result.fail("an old mode index moved");

        ManasSkillInstance bi = b.createDefaultInstance(), li = l.createDefaultInstance();
        int baseModes = b.getModes(bi), lordModes = l.getModes(li);
        if (NUConfig.summonerRework()) {
            if (baseModes != SummonerRules.MODES || lordModes != SummonerRules.MODES) return Result.fail("mode counts " + baseModes + " / " + lordModes);
            int m = SummonerSkill.RECALL;
            for (int i = 0; i < baseModes; i++) {
                m = b.nextMode(null, bi, m, false);
                if (m == SummonerSkill.LEGION || m == SummonerSkill.WRAITH) return Result.fail("the base Summoner lists an Ultimate-only card");
            }
            m = SummonerSkill.RECALL;
            if (l.nextMode(null, li, m, false) != SummonerSkill.LEGION) return Result.fail("the Ultimate does not go from Recall to Legion");
        } else if (baseModes != 3 || lordModes != 5) {
            return Result.fail("with the rework off the mode counts are " + baseModes + " / " + lordModes + ", not 3 / 5");
        }
        for (int mode = 0; mode < SummonerRules.MODES; mode++) {
            if (b.getModeId(bi, mode).isEmpty() || l.getModeName(li, mode).getString().isEmpty()) return Result.fail("mode " + mode + " has no name");
        }
        return Result.ok("modes " + baseModes + " / " + lordModes);
    }
}
