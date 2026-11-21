package com.newuniverse.nusmp.sinweapons;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why (4.30.1): the owner asked for a way to remove the Berserk weapon skills. {@code /multiverse sin remove <sloth|envy|vanity|chakrams|holy|all>} takes the skill (or all five) away from the caller and keeps it
 * from coming back by itself; {@code /multiverse sin restore <...>} undoes that (the next inventory look gives the skill again to the carrier of a weapon). An operator may add a player: {@code remove <...> <player>}.
 * All server side, validated by the command tree. {@code remove} and {@code restore} delete nothing; {@code purge} (4.77.0) is {@code remove} plus deleting that weapon's stacks.
 */
public final class SinCommand {
    private SinCommand() {}

    private static final String[] WORDS = {"sloth", "envy", "vanity", "chakrams", "holy", "all"};

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("sin")
                .then(action("remove", true))
                .then(action("restore", false))
                .then(action("purge", true));       // 4.77.0: remove plus the weapon stacks
    }

    private static LiteralArgumentBuilder<CommandSourceStack> action(String name, boolean remove) {
        return Commands.literal(name).then(Commands.argument("weapon", StringArgumentType.word())
                .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(WORDS, b))
                .executes(c -> run(c, c.getSource().getPlayerOrException(), remove, name.equals("purge")))
                .then(Commands.argument("player", EntityArgument.player()).requires(s -> s.hasPermission(2))
                        .executes(c -> run(c, EntityArgument.getPlayer(c, "player"), remove, name.equals("purge")))));
    }

    private static List<SinRules.Kind> kinds(String w) {
        List<SinRules.Kind> out = new ArrayList<>();
        for (SinRules.Kind k : SinRules.Kind.values()) if (w.equalsIgnoreCase("all") || w.equalsIgnoreCase(k.name())) out.add(k);
        return out;
    }

    private static int run(CommandContext<CommandSourceStack> c, ServerPlayer target, boolean remove, boolean purge) throws CommandSyntaxException {
        List<SinRules.Kind> ks = kinds(StringArgumentType.getString(c, "weapon"));
        if (ks.isEmpty()) {
            c.getSource().sendFailure(Component.literal("Name a weapon: sloth, envy, vanity, chakrams, holy or all."));
            return 0;
        }
        int changed = 0;
        for (SinRules.Kind k : ks) if (remove ? SinSkillService.remove(target, k) : SinSkillService.restore(target, k)) changed++;
        int stacks = 0;
        if (purge) for (SinRules.Kind k : ks) stacks += SinSkillService.purgeItems(target, k);
        final int n = changed, gone = stacks;
        c.getSource().sendSuccess(() -> Component.literal(remove ? n + " Berserk weapon skill(s) removed from " + target.getGameProfile().getName() + (purge ? " and " + gone + " weapon stack(s) deleted." : ".") : n + " Berserk weapon skill(s) allowed again for " + target.getGameProfile().getName() + "."), true);
        return n;
    }
}
