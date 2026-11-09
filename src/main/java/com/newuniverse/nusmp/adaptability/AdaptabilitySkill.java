package com.newuniverse.nusmp.adaptability;

import com.newuniverse.nusmp.skill.NUSkill;
import com.newuniverse.nusmp.skill.NUSkills;
import com.newuniverse.nusmp.skill.SkillText;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Adaptability (Unique, TOGGLE, 3.70.0; {@code docs/adaptability.md}): a skill that evolves through punishment. While it is on, every kind of damage that hurts you is learned: each hit of one kind adds a
 * stack and takes a little more off the next hit of that kind (up to a floor), a body hit by Severance enough times becomes immune to it, resistance skills learn faster, and every skill you know slowly
 * gains mastery. Everything is decided on the server in {@link AdaptabilityEvents}; this class only owns the toggle, the state keys and the slow mastery pass. The record lives in the skill instance's own
 * data (so it saves with the player and is gone with the skill), 16 kinds at most. The numbers are {@link AdaptabilityConfig}; the maths is {@link AdaptabilityRules}.
 */
public class AdaptabilitySkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("adaptability", "Adaptability",
            "Unique. Toggle. A skill that evolves through punishment: every kind of damage you survive is learned, so the next hit of that kind hurts less; Severance is adapted to completely, resistances are learned faster, and your skills slowly gain mastery.");

    public static final String STACKS = "AdStacks", SEVERANCE_HITS = "AdSevHits", SEVERANCE_IMMUNE = "AdSevImmune";

    public AdaptabilitySkill() { super(SkillType.UNIQUE, TEXT); }

    @Override public boolean canBeToggled(ManasSkillInstance instance, LivingEntity entity) { return true; }

    @Override public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return true; }

    @Override
    public void onToggleOn(ManasSkillInstance instance, LivingEntity entity) {
        if (entity instanceof ServerPlayer p) p.displayClientMessage(Component.translatableWithFallback("nusmp.skill.msg.adaptability.on", "Adaptability: ON").withStyle(ChatFormatting.GREEN), true);
    }

    @Override
    public void onToggleOff(ManasSkillInstance instance, LivingEntity entity) {
        if (entity instanceof ServerPlayer p) p.displayClientMessage(Component.translatableWithFallback("nusmp.skill.msg.adaptability.off", "Adaptability: OFF").withStyle(ChatFormatting.GRAY), true);
    }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity living) {
        if (!(living instanceof ServerPlayer p) || !instance.isToggled() || !p.isAlive() || !AdaptabilityConfig.enabled()) return;
        if (p.tickCount % 100 == 0) instance.markDirty();                       // what the hits changed is sent to the client at most every five seconds
        int interval = AdaptabilityConfig.interval();
        if (AdaptabilityRules.globalDue(p.tickCount, interval)) payGlobalMastery(instance, p, p.tickCount / Math.max(20, interval));
    }

    /** Every learned, not yet mastered skill gets a little mastery; at most {@code maxSkillsPerPass} per pass, the window turning each pass so a long list is covered in turns. */
    private static void payGlobalMastery(ManasSkillInstance self, ServerPlayer p, int pass) {
        double bonus = AdaptabilityConfig.globalBonus(self.isMastered(p));
        if (bonus <= 0.0) return;
        List<ManasSkillInstance> all = new ArrayList<>(SkillAPI.getSkillsFrom(p).getLearnedSkills());
        int[] w = AdaptabilityRules.window(all.size(), pass, AdaptabilityConfig.maxSkills());
        for (int n = 0; n < w[1]; n++) {
            ManasSkillInstance i = all.get((w[0] + n) % all.size());
            if (i == self || i.isMastered(p)) continue;
            i.addMasteryPoint(p, bonus);
        }
    }

    /** The player's Adaptability instance, if they hold it and it is toggled on. */
    static Optional<ManasSkillInstance> active(ServerPlayer p) {
        Optional<ManasSkillInstance> i = SkillAPI.getSkillsFrom(p).getSkill(NUSkills.ADAPTABILITY.getId());
        return i.isPresent() && i.get().isToggled() ? i : Optional.empty();
    }
}
