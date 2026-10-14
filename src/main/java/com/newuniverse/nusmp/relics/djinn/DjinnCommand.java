package com.newuniverse.nusmp.relics.djinn;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.newuniverse.nusmp.relics.djinn.dark.DarkDjinn;

import java.util.UUID;

/**
 * {@code /multiverse djinn ...}. Players: {@code status}, {@code release}, {@code share <player>}, {@code unshare <player>}, {@code accept},
 * {@code leave}. Admins (permission 2): {@code status <player>}, {@code ledger}, {@code grant <player> <djinn> [bind]}, {@code reset <djinn>},
 * {@code phase <0-4>} (the phase for yourself, no price), {@code dark <on|off> [player]} (the Dark Djinn, no price). Attach with {@code DjinnCommand.addTo(root)} on the {@code multiverse} node.
 */
public final class DjinnCommand {
    private DjinnCommand() {}

    private static boolean admin(CommandSourceStack s) { return s.hasPermission(2); }

    private static int ok(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendSuccess(() -> Component.literal(msg), true);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String msg) {
        ctx.getSource().sendFailure(Component.literal(msg));
        return 0;
    }

    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("djinn")
                .then(Commands.literal("status").executes(ctx -> status(ctx, ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(DjinnCommand::admin)
                                .executes(ctx -> status(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("release").executes(ctx -> release(ctx, ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("share").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    DjinnHousehold.share(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"));
                    return 1;
                })))
                .then(Commands.literal("unshare").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    DjinnHousehold.unshare(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"));
                    return 1;
                })))
                .then(Commands.literal("accept").executes(ctx -> { DjinnHousehold.accept(ctx.getSource().getPlayerOrException()); return 1; }))
                .then(Commands.literal("leave").executes(ctx -> { DjinnHousehold.leave(ctx.getSource().getPlayerOrException()); return 1; }))
                .then(Commands.literal("ledger").requires(DjinnCommand::admin).executes(DjinnCommand::ledger))
                .then(Commands.literal("grant").requires(DjinnCommand::admin).then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("djinn", StringArgumentType.word())
                                .suggests((c, b) -> { for (Djinn d : Djinn.values()) b.suggest(d.id()); return b.buildFuture(); })
                                .executes(ctx -> grant(ctx, false))
                                .then(Commands.literal("bind").executes(ctx -> grant(ctx, true))))))
                .then(Commands.literal("reset").requires(DjinnCommand::admin).then(Commands.argument("djinn", StringArgumentType.word())
                        .suggests((c, b) -> { for (Djinn d : Djinn.values()) b.suggest(d.id()); return b.buildFuture(); })
                        .executes(ctx -> {
                            Djinn d = Djinn.byName(StringArgumentType.getString(ctx, "djinn"));
                            if (d == null) return fail(ctx, "Unknown Djinn.");
                            DjinnLedger.get(ctx.getSource().getServer()).reset(d);
                            return ok(ctx, d.displayName() + " sleeps again. Players who held it lose it within four seconds.");
                        })))
                .then(Commands.literal("dark").requires(DjinnCommand::admin)
                        .then(Commands.literal("on").executes(ctx -> dark(ctx, ctx.getSource().getPlayerOrException(), true))
                                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> dark(ctx, EntityArgument.getPlayer(ctx, "player"), true))))
                        .then(Commands.literal("off").executes(ctx -> dark(ctx, ctx.getSource().getPlayerOrException(), false))
                                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> dark(ctx, EntityArgument.getPlayer(ctx, "player"), false)))))
                .then(Commands.literal("phase").requires(DjinnCommand::admin).then(Commands.argument("phase", IntegerArgumentType.integer(0, 4)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    int n = IntegerArgumentType.getInteger(ctx, "phase");
                    boolean done = DjinnEngine.go(p, n, true);
                    return done ? ok(ctx, "Phase " + n + ".") : fail(ctx, "Not possible (no Djinn or vessel, or the step is out of order).");
                }))));
    }

    // ---------------------------------------------------------------- status
    private static int status(CommandContext<CommandSourceStack> ctx, ServerPlayer p) {
        DjinnState s = DjinnState.of(p);
        Djinn d = s.active();
        String who = p.getName().getString();
        if (d == null) return ok(ctx, who + " holds no Djinn. Bonds held: " + Bonds.held(p) + (Bonds.multiBond(p) ? " (the multi-bond relic is worn)" : "") + ".");
        int[] sum = DjinnEngine.summary(p);
        StringBuilder b = new StringBuilder(who).append(s.lent() ? " shares " : " holds ").append(d.fullName()).append(" (").append(d.affinity().label).append(")")
                .append(DarkDjinn.active(s) ? " | DARK (the black rukh)" : "").append(" | phase ").append(sum[0]).append(" | trust ").append(sum[1]).append("/100 | assimilation ").append(sum[2] / 10.0).append("%");
        if (sum[3] > 0) b.append(" | Maso closed for ").append((sum[3] + 19) / 20).append(" s");
        if (sum[4] > 0) b.append(" | Extreme Magic ready in ").append((sum[4] + 19) / 20).append(" s");
        if (!s.lent()) {
            ItemStack v = DjinnEngine.vesselOf(p);
            b.append(" | vessel: ").append(v.isEmpty() ? "NOT on them" : v.getHoverName().getString());
            MinecraftServer server = ctx.getSource().getServer();
            int n = DjinnLedger.get(server).household(d).size();
            if (n > 0) b.append(" | household: ").append(n).append("/").append(DjinnPhaseRules.HOUSEHOLD_MAX);
        }
        b.append(" | bonds: ").append(Bonds.held(p));
        return ok(ctx, b.toString());
    }

    private static int ledger(CommandContext<CommandSourceStack> ctx) {
        DjinnLedger l = DjinnLedger.get(ctx.getSource().getServer());
        StringBuilder b = new StringBuilder("Djinn ledger:");
        for (Djinn d : Djinn.values()) {
            DjinnLedger.State st = l.state(d);
            b.append("\n ").append(d.displayName()).append(" [").append(d.affinity().label).append("] ").append(st);
            if (st == DjinnLedger.State.BOUND) b.append(" by ").append(l.ownerName(d)).append(", household ").append(l.household(d).size());
            if (st == DjinnLedger.State.SEALED) b.append(", Seal last at ").append(l.sealPlace(d));
        }
        return ok(ctx, b.toString());
    }

    // ---------------------------------------------------------------- the Dark Djinn
    /** Admin: turns a player's own Djinn dark or cleanses it, no price and no shard. A dark Djinn cannot have a household, so that is refused first. */
    private static int dark(CommandContext<CommandSourceStack> ctx, ServerPlayer p, boolean on) {
        DjinnState s = DjinnState.of(p);
        String who = p.getName().getString();
        if (!s.owns()) return fail(ctx, who + " owns no Djinn (only an owner's Djinn can be dark)." + (s.lent() ? " They share one." : ""));
        if (on) {
            if (s.dark()) return fail(ctx, who + "'s Djinn is already dark.");
            if (!DjinnLedger.get(ctx.getSource().getServer()).household(s.djinn()).isEmpty())
                return fail(ctx, "A dark Djinn cannot be lent: /multiverse djinn unshare its household members first.");
            DarkDjinn.darken(p, s);
            return ok(ctx, who + "'s " + s.djinn().displayName() + " has drunk the black rukh.");
        }
        if (!s.dark()) return fail(ctx, who + "'s Djinn is not dark.");
        DarkDjinn.purify(p, s);
        return ok(ctx, who + "'s " + s.djinn().displayName() + " is cleansed.");
    }

    // ---------------------------------------------------------------- release
    private static int release(CommandContext<CommandSourceStack> ctx, ServerPlayer p) {
        DjinnState s = DjinnState.of(p);
        if (!s.owns()) return fail(ctx, "You own no Djinn." + (s.lent() ? " (You share one: /multiverse djinn leave.)" : ""));
        Djinn d = s.djinn();
        ItemStack seal = DjinnBinding.releaseOwned(p);
        if (seal.isEmpty()) return fail(ctx, "The Djinn would not let go.");
        p.getInventory().placeItemBackInInventory(seal);
        return ok(ctx, d.fullName() + " leaves you and becomes a Seal in your hands. Its household is called back, the vessel forgets it.");
    }

    // ---------------------------------------------------------------- grant
    private static int grant(CommandContext<CommandSourceStack> ctx, boolean bind) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        Djinn d = Djinn.byName(StringArgumentType.getString(ctx, "djinn"));
        if (d == null) return fail(ctx, "Unknown Djinn. Try one of the suggestions.");
        MinecraftServer server = ctx.getSource().getServer();
        DjinnLedger ledger = DjinnLedger.get(server);
        if (ledger.state(d) == DjinnLedger.State.BOUND) return fail(ctx, d.displayName() + " is held by " + ledger.ownerName(d) + ". Release it, or /multiverse djinn reset " + d.id() + ".");
        if (!bind) {
            ledger.sealIssued(d, p.level().dimension().location().toString(), p.blockPosition(), server.overworld().getGameTime());
            p.getInventory().placeItemBackInInventory(DjinnItems.sealStack(d));
            return ok(ctx, "Gave " + p.getName().getString() + " the Seal of " + d.displayName() + ".");
        }
        DjinnState st = DjinnState.of(p);
        ItemStack vessel = VesselAccess.isVessel(p.getMainHandItem()) ? p.getMainHandItem() : p.getOffhandItem();
        if (!VesselAccess.isVessel(vessel) || VesselAccess.boundDjinn(vessel) != null) return fail(ctx, p.getName().getString() + " must hold a free Legend-grade vessel.");
        if (st.owns() || st.lent()) return fail(ctx, p.getName().getString() + " already carries a Djinn.");
        UUID serial = UUID.randomUUID();
        if (!ledger.bind(d, p.getUUID(), p.getName().getString(), serial)) return fail(ctx, "The ledger refused.");
        VesselAccess.stamp(vessel, d, serial);
        st.bind(d, serial, 60);
        DjinnEngine.restore(p);
        return ok(ctx, d.fullName() + " is bound to " + p.getName().getString() + "'s " + vessel.getHoverName().getString() + " (admin: no price, trust 60).");
    }
}
