package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.SummonRules;
import com.newuniverse.nusmp.skill.rules.SummonRules.Card;
import com.newuniverse.nusmp.skill.rules.Tier;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;

/**
 * Summoner (Allen, Hell Mode): a Unique skill that calls summon cards. Modes: Beast Card (a tamed fighting wolf), Stone Card (a guardian
 * golem), Recall (dismiss all of your summons). Summons are capped (config: 3, mastered 6), last twelve minutes (twenty-four when mastered),
 * follow and defend their owner, and vanish when the owner dies or leaves. Lord of Summons (the Ultimate) extends this class with two more
 * cards and doubled numbers. The numbers are in {@link SummonRules}; the creatures and their upkeep are in {@link Summons}.
 */
public class SummonerSkill extends NUSkill {
    public static final String SUMMON_TAG = "nusmp_summon";
    public static final String OWNER_KEY = "nusmp_owner";

    static final SkillText TEXT = SkillText.of("summoner", "Summoner",
            "Unique. Call summon cards: a Beast Card wolf or a Stone Card golem fight beside you, Recall dismisses them. Mastery raises how many can answer.");

    /** Mode indexes (append-only: Lord of Summons adds LEGION and WRAITH after Recall). */
    static final int BEAST = 0, STONE = 1, RECALL = 2, LEGION = 3, WRAITH = 4;

    public SummonerSkill() { this(SkillType.UNIQUE, TEXT, false); }

    protected SummonerSkill(SkillType type, SkillText text, boolean ultimate) { super(type, text, ultimate); }

    @Override public int getModes(ManasSkillInstance instance) { return 3; }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        int count = getModes(instance);
        return reverse ? (mode + count - 1) % count : (mode + 1) % count;
    }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) {
        return "summoner." + switch (mode) {
            case BEAST -> "beast";
            case STONE -> "stone";
            default -> "recall";
        };
    }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        return switch (mode) {
            case BEAST -> SkillMsg.mode("summoner.beast", "Beast Card");
            case STONE -> SkillMsg.mode("summoner.stone", "Stone Card");
            default -> SkillMsg.mode("summoner.recall", "Recall");
        };
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (!(entity instanceof ServerPlayer player) || !player.isAlive()) return;
        switch (mode) {
            case RECALL -> recall(player);
            case BEAST -> castCard(instance, player, Card.BEAST, mode);
            case STONE -> castCard(instance, player, Card.STONE, mode);
            default -> { }
        }
    }

    /** Recall: every summon of the caster goes away (never anybody else's). */
    protected final void recall(ServerPlayer player) {
        int removed = Summons.recall(player);
        SkillUtil.actionbar(player, Summons.countMessage(removed));
        SkillFx.sound(player.serverLevel(), player.position(), SoundEvents.EVOKER_CAST_SPELL, 0.8F, 1.4F);
    }

    /** The shared flow of a card: cooldown, cap, cost, creatures, effects, cooldown, mastery. */
    protected final void castCard(ManasSkillInstance instance, ServerPlayer player, Card card, int mode) {
        Tier tier = tier(instance, player);
        if (!SummonRules.available(card, tier)) {
            SkillUtil.fail(player, SkillMsg.t("summoner.lord_only", "Only the Lord of Summons can call this card."));
            return;
        }
        if (instance.onCoolDown(mode)) {
            SkillUtil.fail(player, SkillMsg.t("summoner.cooldown", "Summoner is on cooldown."));
            return;
        }

        int max = SummonRules.maxSummons(NUConfig.SUMMON_MAX.get(), NUConfig.SUMMON_MAX_MASTERED.get(), tier);
        int want = SummonRules.mobCount(card, tier);
        int count = SummonRules.allowed(Summons.owned(player).size(), max, want);
        if (count <= 0) {
            SkillUtil.fail(player, SkillMsg.t("summoner.cap", "You can only have %s summons at once.", max));
            return;
        }
        double price = SummonRules.cost(card, NUConfig.SUMMON_MAGICULE_COST.get()) * count / want;   // a partial cast costs a partial price
        if (!SkillUtil.spendMagicules(player, price)) return;

        int made = Summons.spawn(player, card, tier, count);
        if (made <= 0) {
            SkillUtil.refund(player, price);
            SkillUtil.fail(player, SkillMsg.t("summoner.failed", "The card did not answer."));
            return;
        }
        SkillFx.sound(player.serverLevel(), player.position(), SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F, card == Card.STONE ? 0.8F : 1.2F);
        SkillFx.ring(player.serverLevel(), player.position(), 2.5, card == Card.WRAITH ? 0xB070FF : 0xFFC860);
        cooldown(instance, mode, SummonRules.cooldown(card, NUConfig.SUMMON_COOLDOWN.get()));
        SkillUtil.castVfx(player, 0xFFB8A0FF);
        grow(instance, player, SummonRules.masteryGain(card));
    }

    /** The cap that applies to this player's upkeep: their strongest Summoner skill's, or no trimming if they have neither. */
    static int capFor(ServerPlayer player) {
        var skills = io.github.manasmods.manascore.skill.api.SkillAPI.getSkillsFrom(player);
        var lord = skills.getSkill(NUSkills.LORD_OF_SUMMONS.get().getRegistryName());
        if (lord.isPresent()) return SummonRules.maxSummons(NUConfig.SUMMON_MAX.get(), NUConfig.SUMMON_MAX_MASTERED.get(), Tier.ULTIMATE);
        var base = skills.getSkill(NUSkills.SUMMONER.get().getRegistryName());
        if (base.isPresent()) {
            return SummonRules.maxSummons(NUConfig.SUMMON_MAX.get(), NUConfig.SUMMON_MAX_MASTERED.get(),
                    Tier.of(base.get().isMastered(player), false));
        }
        return SummonRules.HARD_CAP;
    }
}
