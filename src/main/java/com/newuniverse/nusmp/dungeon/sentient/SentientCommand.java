package com.newuniverse.nusmp.dungeon.sentient;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.dungeon.DungeonConfig;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * /multiverse dungeon sentient (permission level 2): status | birth [count] | threat &lt;amount&gt; | juggernaut | rooms forget. A look at the
 * Sentient Dungeon where you stand, and the admin's hands on it: bring mobs out of the walls now, load the immune system, send a Juggernaut,
 * make the Dungeon forget the rooms round you so they are rolled again. Attached under the dungeon node by {@link #addTo}.
 */
public final class SentientCommand {
    private SentientCommand() {}

    private static final int FORGET_RADIUS = 4, DEFAULT_BIRTHS = 5;

    private static int ok(CommandSourceStack s, String msg) {
        s.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int fail(CommandSourceStack s, String msg) {
        s.sendFailure(Component.literal(msg));
        return 0;
    }

    /** Adds {@code sentient status | birth | threat | juggernaut | rooms forget} under the given dungeon node. */
    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> dungeonNode) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("sentient").requires(s -> s.hasPermission(2));
        root.then(Commands.literal("status").executes(ctx -> status(ctx.getSource())));
        root.then(Commands.literal("birth").executes(ctx -> birth(ctx.getSource(), DEFAULT_BIRTHS))
                .then(Commands.argument("count", IntegerArgumentType.integer(1, 12)).executes(ctx -> birth(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count")))));
        root.then(Commands.literal("threat")
                .then(Commands.argument("amount", FloatArgumentType.floatArg(0f, 1000f)).executes(ctx -> threat(ctx.getSource(), FloatArgumentType.getFloat(ctx, "amount")))));
        root.then(Commands.literal("juggernaut").executes(ctx -> juggernaut(ctx.getSource())));
        root.then(Commands.literal("rooms").then(Commands.literal("forget").executes(ctx -> forget(ctx.getSource()))));
        dungeonNode.then(root);
    }

    private static int status(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel level = p.serverLevel();
        boolean on = SentientMode.active();
        ok(src, "Sentient Dungeon: " + (on ? "ON" : "OFF") + " (dungeon.sentient = " + DungeonConfig.get(DungeonConfig.SENTIENT) + ", dungeon.enabled = " + DungeonConfig.get(DungeonConfig.ENABLED) + ")");
        ok(src, "Juggernauts alive: " + JuggernautSpawner.alive() + " of " + JuggernautPlan.MAX_ALIVE + ", on their way: " + JuggernautSpawner.pending(level.getGameTime()));
        if (!LivingDungeon.in(p)) return 1;
        FloorKind kind = FloorKind.at(level, p.blockPosition());
        ok(src, "You are on: " + kind.label() + (kind.hasRoomBirths() ? "" : " (no room births here)"));
        for (String line : DungeonRoomManager.describe(level, p)) ok(src, line);
        double threshold = DungeonConfig.get(DungeonConfig.JUGGERNAUT_THREAT);
        long left = ImmuneSystem.cooldownUntil(p) - level.getGameTime();
        ok(src, String.format(Locale.ROOT, "Your threat: %.1f of %.0f%s", ImmuneSystem.threat(p), threshold, left > 0 ? " (cooldown: " + (left / 20) + " s)" : ""));
        String why = ImmuneSystem.whyNot(p, false);
        if (why != null) ok(src, "The immune system ignores you now: " + why + ".");
        return 1;
    }

    private static int birth(CommandSourceStack src, int count) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (!LivingDungeon.in(p)) return fail(src, "Only inside the Dungeon.");
        int n = DungeonRoomManager.force(p, count);
        return n > 0 ? ok(src, n + " birth(s) are cracking the walls round you.") : fail(src, "No wall with a place to stand beside it was found round you (or nothing of this floor's list exists in this game).");
    }

    private static int threat(CommandSourceStack src, float amount) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        String why = ImmuneSystem.whyNot(p, false);
        if (why != null) return fail(src, "The immune system does not answer for you: " + why + ".");
        ImmuneSystem.addThreat(p, amount, "command");
        return ok(src, String.format(Locale.ROOT, "Threat is now %.1f of %.0f.", ImmuneSystem.threat(p), DungeonConfig.get(DungeonConfig.JUGGERNAUT_THREAT)));
    }

    private static int juggernaut(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        String why = ImmuneSystem.whyNot(p, true);
        if (why != null) return fail(src, "No Juggernaut: " + why + ".");
        return ImmuneSystem.aggro(p, "command", true) ? ok(src, "A Juggernaut is being born from the walls near you.") : fail(src, "None could be born: three are alive already, or no wall with a place to stand was found near you.");
    }

    private static int forget(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (!LivingDungeon.in(p)) return fail(src, "Only inside the Dungeon.");
        int n = RoomSave.get(src.getServer()).forgetAround(p.blockPosition().getX() >> 4, p.blockPosition().getZ() >> 4, FORGET_RADIUS);
        return ok(src, "The Dungeon forgot " + n + " room(s) within " + FORGET_RADIUS + " chunks; they are rolled again when someone walks in.");
    }
}
