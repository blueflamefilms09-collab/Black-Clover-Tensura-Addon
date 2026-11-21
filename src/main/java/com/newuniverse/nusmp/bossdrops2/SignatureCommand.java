package com.newuniverse.nusmp.bossdrops2;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.newuniverse.nusmp.relics.order.UniqueLedger;
import com.newuniverse.nusmp.unique.UniqueGuardian;
import com.newuniverse.nusmp.unique.UniqueRecord;
import com.newuniverse.nusmp.unique.UniqueSaved;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.UUID;

/**
 * Why: {@code /multiverse bossdrops list | where <boss> | give <boss> [count]}, all validated and server side, no packets (one chat message per line, at most {@code BossSigCatalog.BOSSES.size()} lines).
 * <ul>
 *   <li>{@code list} (everybody): each boss's one-of-one and whether it is still unclaimed or claimed (by whom). It never shows a position.</li>
 *   <li>{@code where} (operators): the same plus where the Unique Guardian last saw the valid copy (dimension and block position, whether it is held, dropped or in a container).</li>
 *   <li>{@code give} (operators): a signature stack of a boss, for testing a boon or the look.</li>
 * </ul>
 */
public final class SignatureCommand {
    private SignatureCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("bossdrops")
                .then(Commands.literal("list").executes(SignatureCommand::list))
                .then(Commands.literal("where").requires(s -> s.hasPermission(2)).then(Commands.argument("boss", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(BossSigCatalog.BOSSES, b)).executes(SignatureCommand::where)))
                .then(Commands.literal("give").requires(s -> s.hasPermission(2)).then(Commands.argument("boss", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(BossSigCatalog.BOSSES, b)).executes(c -> give(c, 1))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 16)).executes(c -> give(c, IntegerArgumentType.getInteger(c, "count"))))));
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        MinecraftServer sv = c.getSource().getServer();
        Set<String> live = UniqueLedger.liveIds(sv);
        int n = 0;
        for (String boss : BossSigCatalog.BOSSES) {
            String relic = BossSigCatalog.relic(boss);
            if (relic.isEmpty()) continue;
            n++;
            boolean claimed = live.contains(relic);
            Component line = Component.literal(boss + ": ").append(Component.translatable("item.anime_multiverse." + relic))
                    .append(Component.literal(claimed ? " - claimed by " + holder(sv, relic) : " - unclaimed"));
            c.getSource().sendSuccess(() -> line, false);
        }
        int total = n;
        c.getSource().sendSuccess(() -> Component.literal(total + " one-of-one relics; a second kill while one exists pays an echo."), false);
        return n;
    }

    private static int where(CommandContext<CommandSourceStack> c) {
        MinecraftServer sv = c.getSource().getServer();
        String boss = StringArgumentType.getString(c, "boss");
        String relic = BossSigCatalog.relic(boss);
        if (relic.isEmpty()) return fail(c, Component.translatable("bossdrops2.cmd.unknown", boss));
        UniqueSaved saved = UniqueGuardian.saved();
        UniqueRecord r = saved == null ? null : saved.data().get("relic:" + relic);
        String text;
        if (!UniqueLedger.liveIds(sv).contains(relic)) text = boss + ": " + relic + " is unclaimed (no valid copy exists).";
        else if (r == null) text = boss + ": " + relic + " exists, but the Unique Guardian has not seen it yet.";
        else text = boss + ": " + relic + " is " + r.state + (r.holder == null ? "" : ", held by " + name(sv, r.holder)) + " at " + r.dim + " " + (int) r.x + " " + (int) r.y + " " + (int) r.z + ".";
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int give(CommandContext<CommandSourceStack> c, int count) {
        String boss = StringArgumentType.getString(c, "boss");
        if (!BossSigCatalog.known(boss)) return fail(c, Component.translatable("bossdrops2.cmd.unknown", boss));
        ServerPlayer p = c.getSource().getPlayer();
        if (p == null) return fail(c, Component.literal("Only a player can receive it."));
        ItemStack stack = BossSigItems.signature(boss, BossSigCatalog.tier(boss), count);
        if (!p.addItem(stack.copy())) p.drop(stack.copy(), false);
        c.getSource().sendSuccess(() -> Component.translatable("bossdrops2.cmd.given", stack.getHoverName()), true);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> c, Component why) {
        c.getSource().sendFailure(why);
        return 0;
    }

    private static String holder(MinecraftServer sv, String relic) {
        UniqueSaved saved = UniqueGuardian.saved();
        UniqueRecord r = saved == null ? null : saved.data().get("relic:" + relic);
        return r == null || r.holder == null ? "someone (not located yet)" : name(sv, r.holder);
    }

    private static String name(MinecraftServer sv, UUID id) {
        ServerPlayer on = sv.getPlayerList().getPlayer(id);
        if (on != null) return on.getGameProfile().getName();
        try {
            if (sv.getProfileCache() != null) return sv.getProfileCache().get(id).map(g -> g.getName()).orElse("an offline player");
        } catch (RuntimeException unknown) {
            // a profile cache that cannot answer: "an offline player" will do
        }
        return "an offline player";
    }
}
