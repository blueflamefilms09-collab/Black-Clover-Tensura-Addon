package com.newuniverse.nusmp.mana.zone;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.mana.zone.ManaZoneGate.Check;
import com.newuniverse.nusmp.mana.zone.ManaZoneGate.Verdict;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /multiverse mana zone}: what the Mana Zone gate needs and how far you are (open to everyone, about themselves); admin: {@code attune <player> <seconds>}
 * sets the ritual's progress, {@code end <player>} closes a player's zone. There is no command that opens a Mana Zone.
 */
public final class ManaZoneCommands {
    private ManaZoneCommands() {}

    private static boolean admin(CommandSourceStack s) { return s.hasPermission(2); }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("zone")
                .executes(ctx -> info(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("attune").requires(ManaZoneCommands::admin).then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 1_000_000)).executes(ctx -> {
                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                            ManaZonePlayer.setAttune(p, IntegerArgumentType.getInteger(ctx, "seconds"));
                            ctx.getSource().sendSuccess(() -> Component.literal(p.getName().getString() + " is attuned for " + (int) ManaZonePlayer.attuneSeconds(p) + " s."), false);
                            return 1;
                        }))))
                .then(Commands.literal("end").requires(ManaZoneCommands::admin).then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    ManaZoneManager.end(p, ManaZoneManager.End.ADMIN);
                    return 1;
                })));
    }

    private static int info(CommandSourceStack src, ServerPlayer p) {
        Verdict v = ManaZoneManager.verdict(p);
        send(src, v.open() ? "Mana Zone: open to you." : "Mana Zone: not yet. It is a technique for the end of the road, after mana skin and reinforcement.", v.open() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY);
        for (Check c : v.checks())
            send(src, (c.met() ? " [x] " : " [ ] ") + ManaZoneGate.label(c.req()) + ": " + c.have() + " (needs " + c.need() + ")", c.met() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
        long now = p.level().getGameTime();
        if (ManaZoneManager.active(p)) send(src, "Your zone is open, radius " + String.format("%.1f", ManaZoneManager.radiusOf(p)) + ".", ChatFormatting.LIGHT_PURPLE);
        else if (now < ManaZonePlayer.cooldownUntil(p)) send(src, "The mana is settling: " + (ManaZonePlayer.cooldownUntil(p) - now + 19) / 20 + " s.", ChatFormatting.GRAY);
        if (GrandZones.inRegion(p)) send(src, "You stand in " + GrandZones.regionIdOf(p) + ": harm " + String.format("%.2f", GrandZones.harmOf(p)) + ", boon " + String.format("%.2f", GrandZones.boonOf(p)) + ".", ChatFormatting.AQUA);
        return 1;
    }

    private static void send(CommandSourceStack s, String msg, ChatFormatting f) { s.sendSuccess(() -> Component.literal(msg).withStyle(f), false); }
}
