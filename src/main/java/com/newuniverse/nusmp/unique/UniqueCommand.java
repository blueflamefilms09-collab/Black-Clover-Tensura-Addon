package com.newuniverse.nusmp.unique;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: {@code /multiverse unique list | status <id> | release <id>}, for operators only (permission 2), all validated and server side, no packets. {@code list} shows every tracked 1-of-1 (capped at 30 lines), {@code status}
 * shows one with where it comes from once it is back in the pool, and {@code release} forces a unique back into the loot pool now (no clocks, no flap guard), the same door the guardian uses.
 */
public final class UniqueCommand {
    private UniqueCommand() {}

    private static final int MAX_LINES = 30;

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("unique").requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").executes(UniqueCommand::list))
                .then(Commands.literal("status").then(Commands.argument("id", StringArgumentType.greedyString())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(c.getSource().getServer()), b)).executes(UniqueCommand::status)))
                .then(Commands.literal("release").then(Commands.argument("id", StringArgumentType.greedyString())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(c.getSource().getServer()), b)).executes(UniqueCommand::release)));
    }

    private static List<String> ids(MinecraftServer sv) {
        List<String> out = new ArrayList<>();
        UniqueSaved sd = UniqueGuardian.saved();
        if (sd != null) for (UniqueRecord r : sd.data().all()) out.add(r.id);
        out.addAll(HeroUnique.ids());
        return out;
    }

    private static int fail(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendFailure(Component.literal(text));
        return 0;
    }

    private static int say(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static String line(MinecraftServer sv, UniqueRecord r, long tick) {
        UniqueKind k = UniqueKinds.kindOfId(r.id);
        StringBuilder b = new StringBuilder(r.id).append(" (").append(k == null ? "?" : k.label(r.id)).append("): ").append(r.state);
        if (r.state != UniqueState.POOL) {
            b.append(", ").append(r.state == UniqueState.HELD ? "held by " : "last held by ").append(UniqueGuardian.holderName(sv, r.holder));
            b.append(" at ").append(r.dim).append(' ').append((int) r.x).append(' ').append((int) r.y).append(' ').append((int) r.z);
            if (r.missing()) b.append(", NOT ACCOUNTED FOR for ").append(Math.max(0L, (tick - r.missingSince) / 20L)).append(" s").append(r.confirmed ? " (destroyed)" : "");
        }
        b.append(", returned to the pool ").append(r.releases).append("x (epoch ").append(r.epoch).append(')');
        if (!r.lastReason.isEmpty()) b.append(", last: ").append(r.lastReason);
        return b.toString();
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        MinecraftServer sv = c.getSource().getServer();
        UniqueSaved sd = UniqueGuardian.saved();
        if (sd == null) return fail(c, "The Unique Guardian is not running.");
        long tick = sv.overworld().getGameTime();
        int missing = 0, pool = 0;
        for (UniqueRecord r : sd.data().all()) {
            if (r.missing()) missing++;
            if (r.state == UniqueState.POOL) pool++;
        }
        say(c, "Unique Guardian (" + (UniqueConfig.enabled() ? "on" : "OFF") + "): " + sd.data().size() + " tracked, " + missing + " not accounted for, " + pool + " in the pool. Grace " + UniqueConfig.graceSeconds() + " s, absence "
                + UniqueConfig.absenceDays() + " days.");
        int n = 0;
        for (UniqueRecord r : sd.data().all()) {
            if (n++ >= MAX_LINES) {
                say(c, "... " + (sd.data().size() - MAX_LINES) + " more (use status <id>).");
                break;
            }
            say(c, " " + line(sv, r, tick));
        }
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        MinecraftServer sv = c.getSource().getServer();
        UniqueSaved sd = UniqueGuardian.saved();
        if (sd == null) return fail(c, "The Unique Guardian is not running.");
        String id = StringArgumentType.getString(c, "id").strip();
        if (!UniqueRules.validId(id)) return fail(c, "'" + id + "' is not a unique id (for example greed, shishou, relic:the_mind_stone, charter:dante, dragonball:earth:3, hero:legendary_sword).");
        if (HeroUnique.owns(id)) {
            say(c, id + ": a Holy Weapon, tracked by its holders: " + HeroUnique.holders(sv, id) + ". It returns to its normal source (the acquisition ritual) when its holder stays away " + UniqueConfig.absenceDays() + " days.");
            return 1;
        }
        UniqueKind k = UniqueKinds.kindOfId(id);
        if (k == null) return fail(c, "Unknown unique '" + id + "'.");
        UniqueRecord r = sd.data().get(id);
        say(c, r == null ? id + " (" + k.label(id) + "): not seen yet; no valid copy has been met by the guardian." : line(sv, r, sv.overworld().getGameTime()));
        say(c, " The owning system believes a valid copy " + (k.exists(sv, id) ? "exists." : "does not exist (it is in the loot pool)."));
        say(c, " Back in the pool: " + k.pool(id));
        return 1;
    }

    private static int release(CommandContext<CommandSourceStack> c) {
        MinecraftServer sv = c.getSource().getServer();
        String id = StringArgumentType.getString(c, "id").strip();
        String why = UniqueGuardian.force(sv, id, c.getSource().getTextName());
        return why == null ? say(c, id + " is back in the loot pool.") : fail(c, why);
    }
}
