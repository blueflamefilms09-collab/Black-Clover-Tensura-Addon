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
        SelfTests.register("summoner.canon", s -> canon());
        SelfTests.register("summoner.creatures", SummonerSelfTests::creatures);                  // 4.106.0: the Hell Mode creatures (docs/summoner_creatures.md)
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

    /** The canon pass (docs/summoner_rework.md): the type table follows the wiki's levels and ranks, the named summons sit on their ranks, the command ratios and limits hold. */
    private static Result canon() {
        int prevLevel = 0;
        for (SummonerRules.Family f : SummonerRules.Family.values()) {
            if (f.level < prevLevel) return Result.fail("the summon types are not in unlock order at " + f);
            prevLevel = f.level;
            if (f.firstRank() < 0 || f.firstRank() >= SummonerRules.RANKS.length()) return Result.fail("a type's first rank is off the rank list: " + f);
        }
        if (SummonerRules.rankLetter(SummonerRules.Family.STONE.firstRank()) != 'E' || SummonerRules.rankLetter(SummonerRules.Family.SPIRIT.firstRank()) != 'C'
                || SummonerRules.rankLetter(SummonerRules.Family.DRAGON.firstRank()) != 'B') return Result.fail("Stone must start at rank E, Spirit at C, Dragon at B");
        if (!"Wally".equals(SummonerRules.cardLabel(SummonerRules.Family.STONE, 0, true)) || !"Beast H".equals(SummonerRules.cardLabel(SummonerRules.Family.BEAST, 0, false))) return Result.fail("a card label is wrong");
        for (int mode = 0; mode < SummonerRules.MODES_CANON; mode++) {
            if (SummonerRules.canonLevel(mode) < 1 || SummonerRules.canonLevel(mode) > SummonerRules.MAX_LEVEL) return Result.fail("mode " + mode + " needs a Summoning level outside 1.." + SummonerRules.MAX_LEVEL);
        }
        if (SummonerRules.unlockedCanon(SummonerRules.DEPUTIZE, 0.5, false, false) || !SummonerRules.unlockedCanon(SummonerRules.DEPUTIZE, 1.0, false, false)) return Result.fail("Deputize opens at the wrong mastery");
        if (SummonerRules.commandBonus(SummonerRules.CMD_GENERAL) != 2 * SummonerRules.commandBonus(SummonerRules.CMD_SOLDIER)
                || SummonerRules.commandBonus(SummonerRules.CMD_KING) != 4 * SummonerRules.commandBonus(SummonerRules.CMD_SOLDIER)) return Result.fail("the command tiers are not 1 : 2 : 4");
        if (SummonerRules.nextCommand(SummonerRules.CMD_NONE, SummonerRules.MAX_GENERALS, 0, true) != -1) return Result.fail("a fourth General was allowed");
        if (SummonerRules.awakeningCooldown(SummonerRules.CMD_KING) >= SummonerRules.awakeningCooldown(SummonerRules.CMD_GENERAL)) return Result.fail("a King must wake faster than a General");
        return Result.ok("canon=" + NUConfig.summonerCanon() + ", modes " + SummonerRules.MODES_CANON);
    }

    /** 4.106.0: the creature type is registered, every species has its model files in the jar, a creature takes its species' box, and a performer is harmless, invulnerable and short-lived. */
    private static Result creatures(MinecraftServer server) {
        var type = com.newuniverse.nusmp.skill.summon.SummonEntities.BEAST;
        if (!type.isBound()) return Result.fail("anime_multiverse:summon_beast is not registered");
        for (com.newuniverse.nusmp.skill.rules.SummonSpecies sp : com.newuniverse.nusmp.skill.rules.SummonSpecies.values()) {
            for (String f : new String[]{"geo/entity/summon_" + sp.id + ".geo.json", "animations/entity/summon_" + sp.id + ".animation.json", "textures/entity/summon_" + sp.id + ".png", "textures/entity/summon_" + sp.id + "_glowmask.png"}) {
                if (SummonerSelfTests.class.getResource("/assets/anime_multiverse/" + f) == null) return Result.fail("missing asset " + f);
            }
        }
        var level = server.overworld();
        var b = type.get().create(level);
        if (b == null) return Result.fail("the creature could not be made");
        b.setSpecies(com.newuniverse.nusmp.skill.rules.SummonSpecies.DRADRA);
        if (b.species() != com.newuniverse.nusmp.skill.rules.SummonSpecies.DRADRA) return Result.fail("the species did not stick");
        if (Math.abs(b.getBbWidth() - com.newuniverse.nusmp.skill.rules.SummonSpecies.DRADRA.width) > 0.01f) return Result.fail("the box did not follow the species: " + b.getBbWidth());
        b.makePerformer(java.util.UUID.randomUUID(), 60);
        if (!b.isPerformer() || !b.isInvulnerable()) return Result.fail("a performer must be invulnerable");
        if (b.hurt(level.damageSources().generic(), 5f)) return Result.fail("a performer was hurt");
        return Result.ok("creatures=" + NUConfig.summonerCreatures() + ", species " + com.newuniverse.nusmp.skill.rules.SummonSpecies.values().length);
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
            int expect = NUConfig.summonerCanon() ? SummonerRules.MODES_CANON : SummonerRules.MODES;
            if (baseModes != expect || lordModes != expect) return Result.fail("mode counts " + baseModes + " / " + lordModes);
            int m = SummonerSkill.RECALL;
            for (int i = 0; i < baseModes; i++) {
                m = b.nextMode(null, bi, m, false);
                if (m == SummonerSkill.LEGION || m == SummonerSkill.WRAITH || (NUConfig.summonerCanon() && m == SummonerRules.ANGEL)) return Result.fail("the base Summoner lists an Ultimate-only card");
            }
            m = SummonerSkill.RECALL;
            if (l.nextMode(null, li, m, false) != SummonerSkill.LEGION) return Result.fail("the Ultimate does not go from Recall to Legion");
        } else if (baseModes != 3 || lordModes != 5) {
            return Result.fail("with the rework off the mode counts are " + baseModes + " / " + lordModes + ", not 3 / 5");
        }
        for (int mode = 0; mode < (NUConfig.summonerCanon() ? SummonerRules.MODES_CANON : SummonerRules.MODES); mode++) {
            if (b.getModeId(bi, mode).isEmpty() || l.getModeName(li, mode).getString().isEmpty()) return Result.fail("mode " + mode + " has no name");
        }
        return Result.ok("modes " + baseModes + " / " + lordModes);
    }
}
