package com.newuniverse.nusmp.shishou;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Why: {@code /multiverse shishou ...}, attached to the root by {@code MultiverseCommands} with one line. For operators (permission 2): {@code spawn} (a wild Shishou where the operator stands, refused when one
 * exists), {@code status}, {@code reset} (every body and stack of him becomes a ghost and dissolves, so the world has none and {@code spawn} works again; his level, skills and Stomach are KEPT),
 * {@code recover} (the recovery rule: to his wielder, or a dormant sword at their home / the spawn) and {@code level set <n>}. For the wielder (any player who holds him): {@code skills} (what he knows, with the
 * Japanese names) and {@code broadcast on|off} (Thought Communication to players within 32 blocks, off by default) and {@code stomach auto on|off}. All validated and server side.
 */
public final class ShishouCommand {
    private ShishouCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("shishou")
                .then(Commands.literal("spawn").requires(s -> s.hasPermission(2)).executes(ShishouCommand::spawn))
                .then(Commands.literal("status").requires(s -> s.hasPermission(2)).executes(ShishouCommand::status))
                .then(Commands.literal("reset").requires(s -> s.hasPermission(2)).executes(ShishouCommand::reset))
                .then(Commands.literal("recover").requires(s -> s.hasPermission(2)).executes(ShishouCommand::recover))
                .then(Commands.literal("level").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("set").then(Commands.argument("n", IntegerArgumentType.integer(1, ShishouLevels.MAX_LEVEL)).executes(ShishouCommand::setLevel))))
                .then(Commands.literal("skills").executes(ShishouCommand::skills))
                .then(Commands.literal("broadcast").then(Commands.argument("on", BoolArgumentType.bool()).executes(ShishouCommand::broadcast)))
                .then(Commands.literal("stomach").then(Commands.literal("auto").then(Commands.argument("on", BoolArgumentType.bool()).executes(ShishouCommand::stomachAuto))));
    }

    private static int say(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendFailure(Component.literal(text));
        return 0;
    }

    private static int spawn(CommandContext<CommandSourceStack> c) {
        if (!ShishouService.enabled()) return fail(c, "Shishou is disabled (relic.shishouEnabled = false) or the server has not started.");
        ServerLevel l = c.getSource().getLevel();
        Vec3 at = c.getSource().getPosition().add(0, 2, 0);
        ShishouEntity e = ShishouService.spawnWild(l, at);
        return e == null ? fail(c, "There is already a Shishou in this world (/multiverse shishou status). Use reset first.") : say(c, "Shishou wakes and starts to fly.");
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        for (String s : ShishouService.status()) say(c, s);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> c) {
        if (!ShishouData.bound()) return fail(c, "The server has not started.");
        ShishouData d = ShishouData.get();
        java.util.UUID body = d.cap().entity();
        d.cap().reset();
        d.touch();
        ShishouLive.clearAll();
        if (body != null) for (ServerLevel l : c.getSource().getServer().getAllLevels()) if (l.getEntity(body) instanceof ShishouEntity se) {
            se.expectRemoval();
            se.discard();
        }
        return say(c, "Shishou was reset: every body and stack of him dissolves. His level, skills and Stomach are kept. /multiverse shishou spawn wakes him again.");
    }

    private static int recover(CommandContext<CommandSourceStack> c) {
        if (!ShishouService.enabled()) return fail(c, "Shishou is disabled or the server has not started.");
        ShishouService.recover(ShishouRecovery.Cause.OPERATOR, c.getSource().getLevel(), c.getSource().getPosition());
        return say(c, "Shishou was recovered by the recovery rule.");
    }

    private static int setLevel(CommandContext<CommandSourceStack> c) {
        if (!ShishouData.bound()) return fail(c, "The server has not started.");
        int n = IntegerArgumentType.getInteger(c, "n");
        ShishouData d = ShishouData.get();
        d.record().setLevel(n);
        d.touch();
        return say(c, "Shishou is now level " + n + ".");
    }

    private static ServerPlayer wielder(CommandContext<CommandSourceStack> c) {
        ServerPlayer p = c.getSource().getPlayer();
        return p != null && ShishouService.enabled() && ShishouService.holding(p) ? p : null;
    }

    private static int skills(CommandContext<CommandSourceStack> c) {
        ServerPlayer p = wielder(c);
        if (p == null) return fail(c, "Only his wielder, holding him, may ask what he knows.");
        ShishouRecord r = ShishouData.get().record();
        say(c, "Shishou, level " + r.level() + ", tier " + ShishouSkill.tierOf(r.level()) + ", absorbed skills " + r.absorbedCount());
        for (ShishouSkill s : ShishouSkill.values()) {
            boolean on = r.has(s);
            String state = on ? (ShishouConfig.skillOn(s) ? "ready" : "switched off by the server") : s.absorbed() ? "not eaten yet" : "from level " + s.unlockLevel();
            say(c, "  " + (on ? "+ " : "- ") + s.english() + " (" + s.romaji() + "): " + state);
        }
        return 1;
    }

    private static int broadcast(CommandContext<CommandSourceStack> c) {
        ServerPlayer p = wielder(c);
        if (p == null) return fail(c, "Only his wielder, holding him, may change this.");
        if (!ShishouConfig.allowBroadcast()) return fail(c, "The server does not allow Shishou to speak to others (telepathy.allowBroadcast).");
        boolean on = BoolArgumentType.getBool(c, "on");
        ShishouData d = ShishouData.get();
        d.record().setBroadcast(on);
        d.touch();
        return say(c, "Thought Communication to players within 32 blocks: " + (on ? "on" : "off"));
    }

    private static int stomachAuto(CommandContext<CommandSourceStack> c) {
        ServerPlayer p = wielder(c);
        if (p == null) return fail(c, "Only his wielder, holding him, may change this.");
        boolean on = BoolArgumentType.getBool(c, "on");
        ShishouData d = ShishouData.get();
        d.record().setAutoStomach(on);
        d.touch();
        return say(c, "Stomach takes in loot near you once a second: " + (on ? "on" : "off"));
    }
}
