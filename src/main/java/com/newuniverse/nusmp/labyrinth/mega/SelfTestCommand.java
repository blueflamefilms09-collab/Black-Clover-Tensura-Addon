package com.newuniverse.nusmp.labyrinth.mega;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Why: {@code /multiverse mega selftest [key]} runs the registered {@link SelfTests} and prints ONE line per test, {@code selftest <key> OK <detail>} or
 * {@code selftest <key> FAIL: <detail>}, then {@code selftest summary ok=<n> fail=<m>}: the fixed format tools/ci/server_smoke.py parses (it is also a
 * handy admin check on a real server: run it after an update and every system says whether it still works). Permission 2, usable from the console.
 * The lead attaches it under {@code mega}.
 */
public final class SelfTestCommand {
    private SelfTestCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("selftest").requires(s -> s.hasPermission(2))
                .executes(ctx -> run(ctx.getSource(), null))
                .then(Commands.argument("key", StringArgumentType.word()).executes(ctx -> run(ctx.getSource(), StringArgumentType.getString(ctx, "key"))));
    }

    private static int run(CommandSourceStack src, String only) {
        List<String> keys = only == null ? SelfTests.keys() : List.of(only);
        int ok = 0, bad = 0;
        for (String k : keys) {
            SelfTests.Result r = SelfTests.run(k, src.getServer());
            String detail = r.detail() == null ? "" : r.detail().replace('\n', ' ');
            String line = r.ok() ? "selftest " + k + " OK " + detail : "selftest " + k + " FAIL: " + detail;
            src.sendSuccess(() -> Component.literal(line), false);
            if (r.ok()) ok++; else bad++;
        }
        final int fo = ok, fb = bad;
        src.sendSuccess(() -> Component.literal("selftest summary ok=" + fo + " fail=" + fb), false);
        return fb == 0 ? 1 : 0;
    }
}
