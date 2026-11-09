package com.newuniverse.nusmp.adaptability;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Why: the way to get Adaptability in play: {@code /multiverse adaptability give <player>} (operators). The server decides; a player cannot grant it to themselves without the permission. */
public final class AdaptabilityCommand {
    private AdaptabilityCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("adaptability").requires(c -> c.hasPermission(2))
                .then(Commands.literal("give").then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> give(c.getSource(), EntityArgument.getPlayer(c, "player")))));
    }

    private static int give(CommandSourceStack src, ServerPlayer p) {
        Skills skills = SkillAPI.getSkillsFrom(p);
        if (skills.getSkill(NUSkills.ADAPTABILITY.getId()).isPresent()) { src.sendFailure(Component.literal(p.getName().getString() + " already has Adaptability.")); return 0; }
        boolean ok = skills.learnSkill(NUSkills.ADAPTABILITY.get().createDefaultInstance(), Component.literal("You are granted Adaptability.").withStyle(ChatFormatting.GOLD));
        if (!ok) { src.sendFailure(Component.literal("Could not give it.")); return 0; }
        src.sendSuccess(() -> Component.literal("Gave Adaptability to " + p.getName().getString() + "."), true);
        return 1;
    }
}
