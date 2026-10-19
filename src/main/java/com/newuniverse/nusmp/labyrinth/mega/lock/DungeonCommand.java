package com.newuniverse.nusmp.labyrinth.mega.lock;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.gen.StairReach;
import com.newuniverse.nusmp.labyrinth.mega.system.FloorCard;
import com.newuniverse.nusmp.labyrinth.mega.system.FloorInfo;
import com.newuniverse.nusmp.labyrinth.mega.system.FloorInfoConfig;
import com.newuniverse.nusmp.labyrinth.mega.system.FloorInfoRules;
import com.newuniverse.nusmp.labyrinth.mega.system.FloorInfoText;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Why: two commands the owner asked for in one request, both for EVERY player (no permission level): {@code /dungeon floor} re-shows the System's floor report whenever the
 * player wants it (also in the chat, for players whose window is off), and {@code /dungeon unstuck [party]} is the way out of any place the dungeon closed. Around them,
 * {@code /dungeon status} says whether the player is sealed in and when it opens, {@code /dungeon release} opens the seal the player stands in once its rune is ready (for a
 * player who cannot see the rune), and {@code /dungeon help} lists them. The operator's side lives under the existing tree, {@code /multiverse mega lock ...}
 * (permission 2), with the same conventions as the other branches of {@code MegaCommands}: {@code list}, {@code release [player]}, {@code unstuck <player>}, {@code cases} and
 * {@code reach [sector]}. Every reply is a translatable component with an English fallback.
 */
public final class DungeonCommand {
    private DungeonCommand() {}

    /** The player's command, registered as a top-level {@code /dungeon}. */
    public static LiteralArgumentBuilder<CommandSourceStack> player() {
        return Commands.literal("dungeon")
                .then(Commands.literal("floor").executes(ctx -> floor(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("release").executes(ctx -> LockBook.releaseFor(ctx.getSource().getPlayerOrException()) > 0 ? 1 : 0))
                .then(Commands.literal("unstuck").executes(ctx -> UnstuckService.unstuck(ctx.getSource().getPlayerOrException(), false))
                        .then(Commands.literal("party").executes(ctx -> UnstuckService.unstuck(ctx.getSource().getPlayerOrException(), true))))
                .then(Commands.literal("help").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> LockMsg.text(LockText.HELP), false);
                    return 1;
                }));
    }

    /** The operator's branch for {@code /multiverse mega}: {@code lock list | release [player] | unstuck <player> | cases | reach [sector]}. */
    public static LiteralArgumentBuilder<CommandSourceStack> admin() {
        return Commands.literal("lock").requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("release")
                        .executes(ctx -> release(ctx.getSource(), null))
                        .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> release(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("unstuck").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> force(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("cases").executes(ctx -> cases(ctx.getSource())))
                .then(Commands.literal("reach").executes(ctx -> reach(ctx.getSource(), -1))
                        .then(Commands.argument("sector", IntegerArgumentType.integer(0)).executes(ctx -> reach(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "sector")))));
    }

    // ================================================================ /dungeon
    static int floor(ServerPlayer p) {
        if (!FloorInfoConfig.enabled()) {
            p.sendSystemMessage(Component.translatableWithFallback(FloorInfoText.CMD_OFF, FloorInfoText.englishOrNull(FloorInfoText.CMD_OFF)));
            return 0;
        }
        int floor = FloorInfo.floorOf(p);
        if (floor == 0) {
            p.sendSystemMessage(Component.translatableWithFallback(FloorInfoText.CMD_NOT_HERE, FloorInfoText.englishOrNull(FloorInfoText.CMD_NOT_HERE)));
            return 0;
        }
        FloorInfo.show(p, floor, 0, FloorInfoRules.Reason.REQUEST);                 // the window (a repeat within half a second is dropped by the throttle)
        FloorCard card = FloorInfo.build(p, floor, 0, FloorInfoRules.Reason.REQUEST);
        FloorInfo.sendLines(p, card);                                                // and the chat, always
        return 1;
    }

    static int status(ServerPlayer p) {
        for (Component c : LockBook.statusFor(p)) p.sendSystemMessage(c);
        return 1;
    }

    // ================================================================ /multiverse mega lock
    private static int say(CommandSourceStack s, String text) {
        s.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    static int list(CommandSourceStack src) {
        say(src, "locks in the book: " + LockBook.liveCount() + ", runes standing: " + LockRunes.count() + ", escape rules " + (LockConfig.enabled() ? "on" : "OFF"));
        for (String k : LockBook.liveKeys()) {
            LockBook.Entry e = LockBook.entry(k);
            if (e == null) continue;
            EscapeRules.Facts f = e.facts();
            say(src, " " + k + (e.lock.soft() ? " (obstacle)" : "") + (f == null ? "" : ": sealed " + f.sealedTicks() / 20 + "s, inside " + f.playersInside() + ", hostile " + f.hostiles()
                    + ", beatable " + f.solvable() + ", " + e.decision().action() + "/" + e.decision().why() + (e.runeReady() ? ", rune ready" : "")));
        }
        return 1;
    }

    static int release(CommandSourceStack src, ServerPlayer p) {
        ServerLevel level = p != null ? p.serverLevel() : MegaDungeon.level(src.getServer());
        if (level == null) return say(src, "the Mega-Dungeon is not loaded");
        int n = LockBook.forceOpen(level, p);
        return say(src, "opened " + n + " lock(s)");
    }

    static int force(CommandSourceStack src, ServerPlayer p) {
        UnstuckService.Target t = UnstuckService.destination(p.serverLevel(), p);
        UnstuckService.moveTo(p, t);
        return say(src, p.getGameProfile().getName() + " moved to " + t.kind() + (t.pos() == null ? "" : " " + t.pos().toShortString()));
    }

    static int cases(CommandSourceStack src) {
        for (LockCase c : LockCase.values()) {
            src.sendSuccess(() -> Component.literal(c.id() + (c.canHold() ? " [can hold]" : " [cannot hold]") + " " + c.guarantees() + ": " + c.what())
                    .withStyle(c.covered() ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        }
        return LockCase.values().length;
    }

    static int reach(CommandSourceStack src, int id) {
        MegaData data = MegaData.get(src.getServer());
        List<Sector> which = new java.util.ArrayList<>();
        if (id >= 0) {
            Sector s = data.sector(id);
            if (s == null) return say(src, "no sector " + id);
            which.add(s);
        } else which.addAll(data.sectors());
        int bad = 0;
        for (Sector s : which) {
            StairReach.Report r = SectorReach.of(s);
            List<String> problems = SectorReach.problems(s);
            bad += problems.size();
            say(src, "sector " + s.id + ": " + r.rooms() + " rooms, farthest " + r.farthest() + " doorways from the arrival, " + (problems.isEmpty() ? "every room has a path to the stairs" : String.join("; ", problems)));
        }
        return bad == 0 ? 1 : 0;
    }
}
