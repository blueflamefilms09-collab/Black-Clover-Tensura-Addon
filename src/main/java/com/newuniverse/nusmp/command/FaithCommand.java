package com.newuniverse.nusmp.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.newuniverse.nusmp.skill.FaithWorship;
import com.newuniverse.nusmp.skill.rules.FaithWorshipRules;
import net.minecraft.commands.arguments.ResourceLocationArgument;
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
 * /anime_multiverse faith get <player>
 * /anime_multiverse faith set <player> <amount>
 * /anime_multiverse faith add <player> <amount>
 * and, for every player (permission 0, the clickable worship menus send these; docs/faith_fix.md):
 * /faith status | accept <god> | reject <god> | leave | offering | offer <1|5|10> | gift <player> <skill>
 */
public final class FaithCommand {
    private FaithCommand() {}

    public static void register(RegisterCommandsEvent event) {
        registerWorship(event);
        event.getDispatcher().register(Commands.literal("anime_multiverse")
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

    /** The worship commands: the sender must be a player; every argument is checked again by {@link FaithWorship} / the skill. */
    private static void registerWorship(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("faith")
                .then(Commands.literal("status").executes(ctx -> { FaithWorship.status(ctx.getSource().getPlayerOrException()); return 1; }))
                .then(Commands.literal("leave").executes(ctx -> FaithWorship.leave(ctx.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("offering").executes(ctx -> { FaithWorship.openOffering(ctx.getSource().getPlayerOrException()); return 1; }))
                .then(Commands.literal("accept").then(Commands.argument("god", EntityArgument.player())
                        .executes(ctx -> FaithWorship.accept(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "god")) ? 1 : 0)))
                .then(Commands.literal("reject").then(Commands.argument("god", EntityArgument.player())
                        .executes(ctx -> FaithWorship.reject(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "god")) ? 1 : 0)))
                .then(Commands.literal("offer").then(Commands.argument("units", IntegerArgumentType.integer(1, FaithWorshipRules.OFFER_STEPS[FaithWorshipRules.OFFER_STEPS.length - 1]))
                        .executes(ctx -> FaithWorship.offer(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "units")) ? 1 : 0)))
                .then(Commands.literal("gift").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("skill", ResourceLocationArgument.id())
                                .executes(ctx -> FaithWorship.gift(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"),
                                        ResourceLocationArgument.getId(ctx, "skill"))
                                        ? 1 : 0)))));
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
            int value = add ? (int) Math.max(0L, Math.min(Integer.MAX_VALUE, (long) FaithSkill.getFaith(faith.get()) + amount)) : amount;      // long sum: a huge add cannot wrap negative
            FaithSkill.setFaith(faith.get(), value);
        }
        int now = FaithSkill.getFaith(faith.get());
        ctx.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " has " + now + " Faith."), true);
        return now;
    }
}
