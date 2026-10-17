package com.newuniverse.nusmp.avatar.inventory;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarManager;
import com.newuniverse.nusmp.avatar.party.AvatarPartyManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: the buttons of the inventory window only work within a few blocks of the Avatar, but an Avatar that went into the Labyrinth cannot be walked up to, and a
 * creator should be able to call it home. {@code /avatar} is the creator's own command (no operator rights needed) for the same orders as the buttons:
 * <pre>
 *   /avatar follow|stay|delve|recall|share [name]     give the order to your Avatar of that name (all your loaded Avatars without a name)
 *   /avatar party                                       list the named monsters of your Avatars
 *   /avatar release [name]                              free the party of your Avatar of that name
 * </pre>
 * It acts only on the caller's own Avatars, and only on those that are loaded.
 */
public final class AvatarOrdersCommand {
    private AvatarOrdersCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        var root = Commands.literal("avatar");
        for (AvatarOrders.Order order : AvatarOrders.Order.values()) {
            String word = order.name().toLowerCase(Locale.ROOT);
            root.then(Commands.literal(word)
                    .executes(c -> give(c, order, null))
                    .then(Commands.argument("name", StringArgumentType.greedyString()).executes(c -> give(c, order, StringArgumentType.getString(c, "name")))));
        }
        root.then(Commands.literal("party").executes(AvatarOrdersCommand::party));
        root.then(Commands.literal("release")
                .executes(c -> release(c, null))
                .then(Commands.argument("name", StringArgumentType.greedyString()).executes(c -> release(c, StringArgumentType.getString(c, "name")))));
        d.register(root);
    }

    /** The caller's loaded Avatars, optionally only the one with this name (case-insensitive). */
    private static List<AvatarEntity> mine(ServerPlayer p, String name) {
        MinecraftServer server = p.getServer();
        List<AvatarEntity> out = new ArrayList<>();
        if (server == null) return out;
        for (AvatarManager.Row row : AvatarManager.get(server).of(p.getUUID())) {
            if (name != null && !row.name.equalsIgnoreCase(name.trim())) continue;
            for (ServerLevel level : server.getAllLevels()) {
                Entity e = level.getEntity(row.id);
                if (e instanceof AvatarEntity a && a.isAlive()) { out.add(a); break; }
            }
        }
        return out;
    }

    private static int give(CommandContext<CommandSourceStack> c, AvatarOrders.Order order, String name) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        List<AvatarEntity> list = mine(p, name);
        if (list.isEmpty()) {
            c.getSource().sendFailure(Component.literal(name == null ? "None of your Avatars is loaded." : "No loaded Avatar of yours is called " + name + "."));
            return 0;
        }
        int done = 0;
        for (AvatarEntity a : list) {
            String line = AvatarOrders.give(p, a, order);
            c.getSource().sendSuccess(() -> Component.literal(line), false);
            if (!line.isEmpty()) done++;
        }
        return done;
    }

    private static int party(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        AvatarPartyManager mgr = AvatarPartyManager.get(p.getServer());
        int total = 0;
        for (AvatarManager.Row row : AvatarManager.get(p.getServer()).of(p.getUUID())) {
            List<AvatarPartyManager.Member> members = mgr.members(row.id);
            if (members.isEmpty()) continue;
            StringBuilder sb = new StringBuilder(row.name.isEmpty() ? "(unnamed)" : row.name).append(": ");
            for (int i = 0; i < members.size(); i++) sb.append(i == 0 ? "" : ", ").append(members.get(i).name);
            c.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
            total += members.size();
        }
        if (total == 0) c.getSource().sendSuccess(() -> Component.literal("Your Avatars have no named monsters."), false);
        return total;
    }

    private static int release(CommandContext<CommandSourceStack> c, String name) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        int n = 0;
        for (AvatarManager.Row row : AvatarManager.get(p.getServer()).of(p.getUUID())) {
            if (name != null && !row.name.equalsIgnoreCase(name.trim())) continue;
            int had = AvatarPartyManager.get(p.getServer()).count(row.id);
            if (had == 0) continue;
            AvatarPartyManager.release(p.getServer(), row.id);
            n += had;
        }
        final int released = n;
        c.getSource().sendSuccess(() -> Component.literal(released == 0 ? "There was nobody to release." : "Released " + released + (released == 1 ? " monster." : " monsters.")), false);
        return n;
    }
}
