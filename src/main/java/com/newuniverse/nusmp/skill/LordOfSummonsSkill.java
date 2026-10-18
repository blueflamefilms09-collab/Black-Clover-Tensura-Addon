package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.SummonRules.Card;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Lord of Summons: the Ultimate evolution of {@link SummonerSkill} (unlocked automatically by mastering it). Not an alias: it keeps Beast
 * Card, Stone Card and Recall but runs them on the ULTIMATE tier (twice the cap, four times the Beast Card's health, three wolves per
 * Beast Card, a colossal Stone Card, summons that live forty minutes), and adds two cards the Unique does not have: Legion (a pack of
 * five wolves in one cast) and Wraith Card (four flying vexes for two minutes). All of it is data in
 * {@link com.newuniverse.nusmp.skill.rules.SummonRules}; the two new modes are appended after Recall so stored mode indexes keep meaning.
 */
public class LordOfSummonsSkill extends SummonerSkill {
    static final SkillText LORD_TEXT = SkillText.of("lord_of_summons", "Lord of Summons",
            "Ultimate. Evolved from Summoner. Twice the summons and far stronger cards: Beast Card calls three wolves, Stone Card is a colossus, Legion calls a pack of five and the Wraith Card a swarm of flying spirits.");

    public LordOfSummonsSkill() { super(SkillType.ULTIMATE, LORD_TEXT, true); }

    @Override public int getModes(ManasSkillInstance instance) { return 5; }

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
