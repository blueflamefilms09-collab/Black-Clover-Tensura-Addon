package com.newuniverse.nusmp.mana;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** /multiverse mana ...: your stage and affinity (open to everyone about themselves), and the admin switches. */
public final class ManaCommands {
    private ManaCommands() {}

    private static boolean admin(CommandSourceStack s) { return s.hasPermission(2); }

    /** 1.99.0: the tree with the Mana Zone's gate command ({@code /multiverse mana zone}) added after the original commands. */
    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return base().then(com.newuniverse.nusmp.mana.zone.ManaZoneCommands.build());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> base() {
        return Commands.literal("mana")
                .then(Commands.literal("info").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    var s = ManaLaw.stage(p);
                    var a = ManaLaw.affinity(p);
                    send(ctx.getSource(), s.label() + " | " + a.label + " | spells cost " + Math.round(s.cost() * a.cost * 100) + "%, hit for " + Math.round(s.power() * 100) + "%");
                    send(ctx.getSource(), "Mana density here: " + String.format("%.1f", ManaLaw.density(p.level(), p.blockPosition()))
                            + (ManaLaw.isGrandZone(p.level(), p.blockPosition()) ? " (dense mana)" : "") + " | corruption " + (int) ManaLaw.corruption(p) + "/100");
                    return 1;
                }))
                .then(Commands.literal("affinity").then(Commands.argument("kind", StringArgumentType.word())
                        .suggests((c, b) -> { for (ManaLaw.Affinity a : ManaLaw.Affinity.values()) b.suggest(a.name().toLowerCase()); return b.buildFuture(); })
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            var a = ManaLaw.Affinity.byName(StringArgumentType.getString(ctx, "kind"));
                            if (a == null) { ctx.getSource().sendFailure(Component.literal("loved, normal or hated.")); return 0; }
                            ManaLaw.setAffinity(p, a);
                            send(ctx.getSource(), a.label + ": " + a.blurb);
                            return 1;
                        })))
                .then(Commands.literal("stage").requires(ManaCommands::admin).then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("level", StringArgumentType.word())
                                .suggests((c, b) -> { for (String s : new String[]{"arcane", "saint", "clear"}) b.suggest(s); return b.buildFuture(); })
                                .executes(ctx -> {
                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                    String l = StringArgumentType.getString(ctx, "level");
                                    ManaLaw.setArcane(p, l.equals("arcane") || l.equals("saint"));
                                    ManaLaw.setSaint(p, l.equals("saint"));
                                    send(ctx.getSource(), p.getName().getString() + " is now " + ManaLaw.stage(p).label() + ".");
                                    return 1;
                                }))))
                .then(Commands.literal("stone").requires(ManaCommands::admin).then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    p.getInventory().add(new ItemStack(NUItems.MAGIC_STONE.get()));
                    send(ctx.getSource(), "A Magic Stone for " + p.getName().getString() + ".");
                    return 1;
                })))
                .then(Commands.literal("corruption").requires(ManaCommands::admin).then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(-150, 150)).executes(ctx -> {
                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                            ManaLaw.addCorruption(p, IntegerArgumentType.getInteger(ctx, "amount"));
                            send(ctx.getSource(), p.getName().getString() + " corruption " + (int) ManaLaw.corruption(p) + "/100.");
                            return 1;
                        }))));
    }

    private static void send(CommandSourceStack s, String msg) { s.sendSuccess(() -> Component.literal(msg).withStyle(ChatFormatting.LIGHT_PURPLE), false); }
}
