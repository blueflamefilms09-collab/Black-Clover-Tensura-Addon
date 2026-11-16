package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.UltimateClass;
import com.newuniverse.nusmp.skill.rules.SummonRules.Card;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Summoner of Beginnings (registry id and class name stay {@code lord_of_summons} / LordOfSummonsSkill so old worlds load): the Ultimate evolution of
 * {@link SummonerSkill} (unlocked automatically by mastering it). Not an alias: it keeps every mode of the Summoner (Beast Card, Stone Card, Recall and, with
 * the Summoner rework on, the Bird Card, Spirit Card, Strengthening and Synthesis, all open from the start, Synthesis up to rank three, the stat
 * reflection at full strength) but runs them on the ULTIMATE tier (twice the cap, four times the Beast Card's health, three wolves per Beast Card, a colossal
 * Stone Card, summons that live forty minutes), and adds two cards the Unique does not have: Legion (a pack of five wolves in one cast) and Wraith Card
 * (four flying vexes for two minutes). All of it is data in {@link com.newuniverse.nusmp.skill.rules.SummonRules} and
 * {@link com.newuniverse.nusmp.skill.rules.SummonerRules}; the two ultimate cards are appended after Recall so stored mode indexes keep meaning, and the mode count
 * comes from {@link SummonerSkill#getModes}.
 */
public class LordOfSummonsSkill extends SummonerSkill {
    static final SkillText LORD_TEXT = SkillText.of("lord_of_summons", "Summoner of Beginnings",
            "Ultimate. Evolved from Summoner (formerly the Lord of Summons). Every card and every gift of Allen's class is open: twice the summons and far stronger cards, Beast Card calls three wolves, Stone Card is a colossus, Legion calls a pack of five and the Wraith Card a swarm of flying spirits, with the Bird Card's sight, the Spirit Card's healing, Strengthening, Synthesis up to rank three and the full stat reflection.");

    public LordOfSummonsSkill() { super(SkillType.ULTIMATE, LORD_TEXT, true); }
    @Override public UltimateClass ultimateClass() { return UltimateClass.KING; }      // docs/ultimate_classes.md

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) {
        return switch (mode) {
            case LEGION -> "summoner.legion";
            case WRAITH -> "summoner.wraith";
            default -> super.getModeId(instance, mode);
        };
    }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        return switch (mode) {
            case LEGION -> SkillMsg.mode("summoner.legion", "Legion");
            case WRAITH -> SkillMsg.mode("summoner.wraith", "Wraith Card");
            default -> super.getModeName(instance, mode);
        };
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (entity instanceof ServerPlayer player && player.isAlive()) {
            if (mode == LEGION) { castCard(instance, player, Card.LEGION, mode); return; }
            if (mode == WRAITH) { castCard(instance, player, Card.WRAITH, mode); return; }
        }
        super.onPressed(instance, entity, slot, mode);
    }
}
