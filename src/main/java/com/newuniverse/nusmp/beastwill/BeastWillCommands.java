package com.newuniverse.nusmp.beastwill;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.avatar.TensuraBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Locale;

/**
 * Why: the operator and player commands of the Beast Will system (3.45.0), as a root command of their own so the shared multiverse command class does not change. For everybody: {@code /beastwill status}
 * (your own progress, in words). For operators (permission 2): {@code list}, {@code give} (bind a will, no checks), {@code core} (a core item of a will), {@code offer} (show the inheritance preview),
 * {@code set} (assimilation and mastery in percent of the active will), {@code strain}, {@code status <player>}, {@code clear} (forget every will of a player). Nothing here can touch anything outside the system.
 */
public final class BeastWillCommands {
    private BeastWillCommands() {}

    public static void register(IEventBus bus) { bus.addListener(BeastWillCommands::onRegister); }

    private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> WILLS = (c, b) -> SharedSuggestionProvider.suggest(BeastWillCatalog.ids(), b);

    private static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("beastwill")
                .then(Commands.literal("status").executes(c -> status(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(s -> s.hasPermission(2)).executes(c -> status(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("list").requires(s -> s.hasPermission(2)).executes(BeastWillCommands::list))
                .then(Commands.literal("give").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("will", StringArgumentType.word()).suggests(WILLS)
                                .executes(c -> give(c, EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "will"))))))
                .then(Commands.literal("core").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("will", StringArgumentType.word()).suggests(WILLS)
                                .executes(c -> core(c, EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "will"))))))
                .then(Commands.literal("offer").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("will", StringArgumentType.word()).suggests(WILLS)
                                .executes(c -> offer(c, EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "will"))))))
                .then(Commands.literal("set").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("assimilation", DoubleArgumentType.doubleArg(0, 100))
                                .then(Commands.argument("mastery", DoubleArgumentType.doubleArg(0, 100))
                                        .executes(c -> set(c, EntityArgument.getPlayer(c, "player"), DoubleArgumentType.getDouble(c, "assimilation"), DoubleArgumentType.getDouble(c, "mastery")))))))
                .then(Commands.literal("strain").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 100))
                                .executes(c -> strain(c, EntityArgument.getPlayer(c, "player"), DoubleArgumentType.getDouble(c, "value"))))))
                .then(Commands.literal("clear").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> clear(c, EntityArgument.getPlayer(c, "player"))))));
    }

    private static int say(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int no(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendFailure(Component.literal(text));
        return 0;
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        StringBuilder sb = new StringBuilder(BeastWillCatalog.size() + " wills:");
        for (BeastWillDef d : BeastWillCatalog.all()) sb.append("\n ").append(d.id()).append(" (").append(d.beast()).append(", rank ").append(d.minRank()).append("+, drop ").append(d.dropChance() * 100).append("%, needs ").append((long) d.requireEp()).append(" EP)");
        return say(c, sb.toString());
    }

    private static int give(CommandContext<CommandSourceStack> c, ServerPlayer p, String id) {
        BeastWillDef d = BeastWillCatalog.get(id);
        if (d == null) return no(c, "No such will: " + id);
        BeastWillState s = BeastWillStore.peek(p);
        if (s != null && s.has(id)) return no(c, p.getName().getString() + " already holds " + id);
        return BeastWillAcquire.grant(p, d, true) ? say(c, "Gave " + id + " to " + p.getName().getString()) : no(c, "That player already holds the most wills (" + BeastWillState.MAX_WILLS + ")");
    }

    private static int core(CommandContext<CommandSourceStack> c, ServerPlayer p, String id) {
        BeastWillDef d = BeastWillCatalog.get(id);
        if (d == null) return no(c, "No such will: " + id);
        ItemStack stack = BeastWillItems.core(d.id(), d.beast(), d.minRank());
        if (!p.getInventory().add(stack)) p.drop(stack, false);
        return say(c, "Gave a core of " + id + " to " + p.getName().getString());
    }

    private static int offer(CommandContext<CommandSourceStack> c, ServerPlayer p, String id) {
        return BeastWillApi.offer(p, id, null, null, "") ? say(c, "Showed the offer of " + id + " to " + p.getName().getString()) : no(c, "No such will: " + id);
    }

    private static int set(CommandContext<CommandSourceStack> c, ServerPlayer p, double assimilation, double mastery) {
        BeastWillState s = BeastWillStore.peek(p);
        BeastWillState.Progress pr = s == null ? null : s.activeProgress();
        if (pr == null) return no(c, p.getName().getString() + " has no active will");
        pr.assimilation = (float) (assimilation / 100.0);
        pr.mastery = (float) (mastery / 100.0);
        BeastWillManager.markDirty(p);
        return say(c, "Set " + s.active + " to " + (int) assimilation + "% assimilation, " + (int) mastery + "% mastery");
    }

    private static int strain(CommandContext<CommandSourceStack> c, ServerPlayer p, double v) {
        BeastWillState s = BeastWillStore.peek(p);
        if (s == null) return no(c, p.getName().getString() + " has no will");
        s.strain = BeastWillRules.clampStrain(v);
        BeastWillManager.markDirty(p);
        return say(c, "Strain set to " + (int) s.strain);
    }

    private static int clear(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        BeastWillState s = BeastWillStore.peek(p);
        if (s == null) return no(c, p.getName().getString() + " has no will");
        s.wills.clear();
        s.coreDay.clear();
        s.active = "";
        s.strain = 0;
        s.lockUntil = 0;
        s.swapLockUntil = 0;
        BeastWillManager.onEnter(p);
        BeastWillManager.markDirty(p);
        return say(c, "Cleared every will of " + p.getName().getString());
    }

    private static int status(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        BeastWillState s = BeastWillStore.peek(p);
        if (s == null || s.wills.isEmpty()) return say(c, p.getName().getString() + " holds no Beast Will.");
        long now = p.level().getGameTime();
        StringBuilder sb = new StringBuilder(p.getName().getString() + ": active " + (s.active.isEmpty() ? "none" : s.active)
                + ", form " + (s.phase == BeastWillRules.INTEGRATE ? "Integrate" : s.phase == BeastWillRules.ACQUIRE ? "Acquire" : "off")
                + ", strain " + Math.round(s.strain) + " (" + BeastWillRules.tier(s.strain).name().toLowerCase(Locale.ROOT) + ")");
        if (s.locked(now)) sb.append(", quiet for ").append((s.lockUntil - now) / 20).append(" s");
        if (s.swapLocked(now)) sb.append(", swap in ").append((s.swapLockUntil - now) / 20).append(" s");
        for (var e : s.wills.entrySet()) {
            BeastWillState.Progress pr = e.getValue();
            sb.append("\n ").append(e.getKey()).append(": assimilation ").append(Math.round(pr.assimilation * 100)).append("%, mastery ").append(Math.round(pr.mastery * 100)).append("%");
        }
        sb.append("\n max EP ").append((long) TensuraBridge.maxEp(p));
        return say(c, sb.toString());
    }
}
