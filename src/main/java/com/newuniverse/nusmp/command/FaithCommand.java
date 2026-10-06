package com.newuniverse.nusmp.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.skill.FaithSkill;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Optional;

/**
 * /nusmp faith get <player>
 * /nusmp faith set <player> <amount>
 * /nusmp faith add <player> <amount>
 */
public final class FaithCommand {
    private FaithCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nusmp")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("faith")
                        .then(Commands.literal("get")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> run(ctx, 0, false))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(ctx -> run(ctx, IntegerArgumentType.getInteger(ctx, "amount"), false)))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(ctx -> run(ctx, IntegerArgumentType.getInteger(ctx, "amount"), true)))))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, int amount, boolean add) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        var skills = SkillAPI.getSkillsFrom(target);
        Optional<ManasSkillInstance> faith = skills.getSkill(NUSkills.FAITH.get().getRegistryName());
        if (faith.isEmpty()) faith = skills.getSkill(NUSkills.GODDESS_OF_FAITH.get().getRegistryName());
        if (faith.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal(target.getName().getString() + " does not have Faith or Goddess of Faith."));
            return 0;
        }
        String sub = ctx.getNodes().get(2).getNode().getName();
        if (!sub.equals("get")) {
            int value = add ? FaithSkill.getFaith(faith.get()) + amount : amount;
            FaithSkill.setFaith(faith.get(), value);
        }
        int now = FaithSkill.getFaith(faith.get());
        ctx.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " has " + now + " Faith."), true);
        return now;
    }
}
