package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.ability.skill.Skill;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;

/**
 * Liaris Freese (Bell Cranel): passive rapid growth.
 * Every kill has a chance to add mastery to ALL of the owner's skills.
 */
public class LiarisFreeseSkill extends Skill {
    private static final ResourceLocation ICON =
            ResourceLocation.withDefaultNamespace("textures/item/nether_star.png");

    public LiarisFreeseSkill() { super(SkillType.UNIQUE); }

    @Override public ResourceLocation getSkillIcon() { return ICON; }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int slot, int mode) {
        if (entity instanceof ServerPlayer player) {
            SkillUtil.actionbar(player, Component.literal("Liaris Freese: your skills grow as you fight.")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getEntity() == player) return;
        Skills skills = SkillAPI.getSkillsFrom(player);
        if (skills.getSkill(NUSkills.LIARIS_FREESE.get().getRegistryName()).isEmpty()) return;
        if (player.getRandom().nextDouble() >= NUConfig.LIARIS_CHANCE.get()) return;

        int amount = NUConfig.LIARIS_MASTERY_PER_KILL.get();
        if (amount <= 0) return;
        for (ManasSkillInstance instance : new ArrayList<>(skills.getLearnedSkills())) {
            if (instance.isMastered(player)) continue;
            instance.addMasteryPoint(player, amount);
            instance.markDirty();
        }
    }
}
