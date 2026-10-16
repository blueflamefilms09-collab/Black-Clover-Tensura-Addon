package com.newuniverse.nusmp.avatar.outcome;

import com.newuniverse.nusmp.avatar.AvatarChatterEngine;
import com.newuniverse.nusmp.avatar.AvatarConfig;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarManager;
import com.newuniverse.nusmp.avatar.ChatEvent;
import com.newuniverse.nusmp.avatar.naming.TensuraNamingEngine;
import com.newuniverse.nusmp.quests.NusmpEvents;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: this is the owner's three-day outcome engine. {@link AvatarEntity#aiStep} calls {@link #tick} every tick; when the 72000-tick timer reaches zero the
 * pure {@link OutcomeMath} decides (Betrayal / Soul Awakening / Ordinary, skipped for a named Avatar and for one with a Unique or Ultimate skill) and
 * {@link #apply} carries the result out on the entity: goals rebuilt, perks applied, the creator told, the quest key fired. The same {@code apply} is what
 * the {@code /nusmp avatar outcome} command uses to force a result for testing.
 *
 * <p>What each result does:
 * <ul>
 *   <li><b>Named (Soul-Bond Lock)</b> and <b>Unique/Ultimate skill</b>: no roll. The Avatar stays loyal, the timer is cancelled ({@code SKIPPED_NAMED} /
 *       {@code SKIPPED_UNIQUE}). Naming cancels the timer the moment it happens (see {@link TensuraNamingEngine}).</li>
 *   <li><b>Betrayal</b>: the allied goals are cleared and {@code AvatarHuntCreatorGoal} is attached; it tracks the creator across dimensions (the
 *       BetrayalDimensionHandler), with a 10 second grace after every teleport and every blow capped at 25% of the victim's maximum health.</li>
 *   <li><b>Soul Awakening</b>: a generated {@link AvatarUniqueSkill.Perk} (attribute perks and a signature move, no new Tensura skill), the Unique flag, the
 *       tax lifted, and full independence: it leaves its creator's command and wanders as a neutral named NPC that still never attacks the creator.</li>
 *   <li><b>Ordinary Existence</b>: a standard high-tier NPC: a small permanent lift, it keeps following while its creator is within range, the tax stays.</li>
 * </ul>
 */
public final class AvatarOutcomes {
    private AvatarOutcomes() {}

    /** The per-tick work of an Avatar on the server. */
    public static void tick(AvatarEntity a, ServerLevel sl) {
        if (a.creatorUuid() == null) return;
        int t = a.tickCount;
        if (t == 2 || t % 100 == 7) AvatarManager.get(sl.getServer()).update(a);
        if (t % 20 == 3) a.perkRegen();
        if (t % 200 == 11) {
            pollUniqueSkill(a);
            ServerPlayer owner = a.creatorPlayer();
            if (owner != null && a.permanentTax() > 0) TensuraNamingEngine.refreshTax(owner);
        }
        if (a.outcomeState() == Outcome.PENDING && a.outcomeTimer() > 0) {
            int left = a.outcomeTimer() - 1;
            a.setOutcomeTimer(left);
            if (left <= 0) resolveTimer(a);
        }
    }

    /** The timer ran out: decide and apply. */
    public static void resolveTimer(AvatarEntity a) {
        OutcomeMath.Weights w = weights(a.storedLuck());
        apply(a, OutcomeMath.decide(a.isNamed(), a.hasUniqueOrUltimate(), w, a.getRandom().nextDouble()));
    }

    /** The weights from the configured bases and the creator's stored luck. */
    public static OutcomeMath.Weights weights(double luck) {
        return OutcomeMath.weights(luck, AvatarConfig.betrayalBase(), AvatarConfig.soulBase(), AvatarConfig.luckShiftPerPoint(), AvatarConfig.luckShiftMax());
    }

    /** Carries an outcome out on the entity. Safe to call for a forced result (a command), whatever the state was. */
    public static void apply(AvatarEntity a, Outcome o) {
        ServerPlayer owner = a.creatorPlayer();
        a.setOutcomeTimer(-1);
        switch (o) {
            case BETRAYAL -> {
                a.setOutcomeState(Outcome.BETRAYAL);
                a.setFrenzied(false);
                a.setTarget(null);
                a.rebuildGoals();
                AvatarChatterEngine.say(a, ChatEvent.BETRAYAL);
                if (owner != null) {
                    owner.sendSystemMessage(Component.literal(a.avatarName() + " has turned on you. It will find you, in any dimension. Name it to win it back.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                    NusmpEvents.fire(owner, "avatar.betrayed");
                }
            }
            case SOUL_AWAKENING -> {
                a.setPerk(AvatarUniqueSkill.generate(a.seeded()));
                a.setHasUniqueOrUltimate(true);
                a.setOutcomeState(Outcome.SOUL_AWAKENING);
                a.setPermanentTax(0.0);
                a.setFrenzied(false);
                a.setTarget(null);
                a.rebuildGoals();
                if (owner != null) {
                    owner.sendSystemMessage(Component.literal(a.avatarName() + "'s soul has awakened: " + a.perk().describe() + ". It walks its own road now, and will never raise a hand against you.").withStyle(ChatFormatting.LIGHT_PURPLE));
                    TensuraNamingEngine.refreshTax(owner);
                    NusmpEvents.fire(owner, "avatar.awakened");
                }
            }
            case ORDINARY -> {
                a.setOutcomeState(Outcome.ORDINARY);
                a.applyOrdinaryTier();
                a.rebuildGoals();
                if (owner != null) owner.sendSystemMessage(Component.literal(a.avatarName() + " settles into an ordinary life, but stays near you while it can.").withStyle(ChatFormatting.GRAY));
            }
            case SKIPPED_NAMED, SKIPPED_UNIQUE -> {
                a.setOutcomeState(o);
                a.rebuildGoals();
                if (owner != null) owner.sendSystemMessage(Component.literal(a.avatarName() + "'s bond holds. It stays at your side.").withStyle(ChatFormatting.AQUA));
            }
            case PENDING -> {
                // a reset: back to a fresh timer, loyal
                a.setOutcomeState(Outcome.PENDING);
                a.setOutcomeTimer(AvatarConfig.outcomeTicks());
                a.rebuildGoals();
            }
        }
        if (a.level() instanceof ServerLevel sl) AvatarManager.get(sl.getServer()).update(a);
    }

    /**
     * Once in a while: does the Avatar hold a Unique or Ultimate Tensura skill (granted by some other system)? Then the roll is skipped, as the owner wants.
     * The mod's own grimoire books are Unique-type skills but are not what the rule means, so only skills in Tensura's own namespace count.
     */
    static void pollUniqueSkill(AvatarEntity a) {
        if (a.hasUniqueOrUltimate()) return;
        try {
            for (ManasSkillInstance i : SkillAPI.getSkillsFrom(a).getLearnedSkills()) {
                var id = i.getSkill().getRegistryName();
                if (id == null || !id.getNamespace().equals("tensura")) continue;
                if (i.getSkill() instanceof Skill s && (s.getType() == Skill.SkillType.UNIQUE || s.getType() == Skill.SkillType.ULTIMATE)) {
                    a.setHasUniqueOrUltimate(true);
                    if (a.outcomeState() == Outcome.PENDING) { a.setOutcomeState(Outcome.SKIPPED_UNIQUE); a.setOutcomeTimer(-1); }
                    return;
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
            // an entity without a skill storage simply has no Unique skill
        }
    }
}
