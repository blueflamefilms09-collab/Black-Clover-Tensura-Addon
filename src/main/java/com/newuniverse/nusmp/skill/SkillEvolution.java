package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Unique -> Ultimate evolution. Every 5 seconds each player is checked: if they have a
 * MASTERED base skill (and enough EP), it evolves into its Ultimate. Saved data such as
 * Faith points carries over to the new skill.
 */
public final class SkillEvolution {
    private SkillEvolution() {}

    private static Map<Supplier<? extends ManasSkill>, Supplier<? extends ManasSkill>> paths() {
        Map<Supplier<? extends ManasSkill>, Supplier<? extends ManasSkill>> m = new LinkedHashMap<>();
        m.put(NUSkills.SUMMONER, NUSkills.LORD_OF_SUMMONS);
        m.put(NUSkills.FAITH, NUSkills.GODDESS_OF_FAITH);
        m.put(NUSkills.ARGONAUT, NUSkills.HEROS_ARGONAUT);
        m.put(NUSkills.HELL_WALKER, NUSkills.HELL_CONQUEROR);
        m.put(NUSkills.UNBREAKABLE_WILL, NUSkills.IMMORTAL_WILL);
        m.put(NUSkills.DEVILS_FOOTPRINTS, NUSkills.ADOLLA_BURST);
        m.put(NUSkills.BOOK_MAKER, NUSkills.ALL_FICTION);
        m.put(NUSkills.SIX_EYES, NUSkills.LIMITLESS);
        m.put(NUSkills.GOLD_EXPERIENCE, NUSkills.GOLD_EXPERIENCE_REQUIEM);
        m.put(NUSkills.VECTOR_MANIPULATION, NUSkills.BLACK_WINGS);
        return m;
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 100 != 0) return;
        if (!NUConfig.EVOLUTION_ENABLED.get()) return;
        Skills skills = SkillAPI.getSkillsFrom(player);

        for (var path : paths().entrySet()) {
            ManasSkill base = path.getKey().get();
            ManasSkill ult = path.getValue().get();
            Optional<ManasSkillInstance> owned = skills.getSkill(base.getRegistryName());
            if (owned.isEmpty() || !owned.get().isMastered(player)) continue;
            if (skills.getSkill(ult.getRegistryName()).isPresent()) continue;

            double needEP = NUConfig.EVOLUTION_MIN_EP.get();
            IExistence existence = TensuraStorages.getExistenceFrom(player);
            if (needEP > 0 && (existence == null || existence.getEP() < needEP)) {
                if (player.tickCount % 6000 == 0) {
                    player.displayClientMessage(Component.literal("Your mastered " + base.getName().getString()
                            + " wants to evolve, but your soul needs " + (long) needEP + " EP.").withStyle(ChatFormatting.GRAY), false);
                }
                continue;
            }
            evolve(player, skills, owned.get(), ult);
            return; // one evolution per check
        }
    }

    private static void evolve(ServerPlayer player, Skills skills, ManasSkillInstance old, ManasSkill ult) {
        ManasSkillInstance evolved = ult.createDefaultInstance();
        evolved.getOrCreateTag().merge(old.getOrCreateTag().copy()); // keep Faith points etc.

        boolean learned = skills.learnSkill(evolved, Component.literal("Your skill has evolved!").withStyle(ChatFormatting.GOLD));
        if (!learned) return;
        if (NUConfig.EVOLUTION_REPLACES_BASE.get()) skills.forgetSkill(old.getSkillId());

        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 0.8F);
        player.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("[Voice of the World] ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal("Confirmed. " + player.getName().getString() + "'s Unique Skill ").withStyle(ChatFormatting.WHITE))
                        .append(old.getDisplayName().copy().withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" has evolved into the Ultimate Skill ").withStyle(ChatFormatting.WHITE))
                        .append(ult.getName().copy().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                        .append(Component.literal(".").withStyle(ChatFormatting.WHITE)), false);
    }
}
