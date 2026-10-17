package com.newuniverse.nusmp.labyrinth.mega.safe;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.Marker;
import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumMap;
import java.util.Map;

/**
 * Why: nothing here can be tried without playing, so the admin gets three small tools to see and fix the Safe-Zone systems from a chat window:
 * {@code status [player]} (whose rest state is what: short rests left, the long-rest cool-down, the rest in progress, whether the regeneration
 * throttle is on, the mana share, which compat hints are live), {@code refill <player>} (every short rest back, cool-down cleared, any rest ended:
 * for testing and for the owner who wants to forgive a player) and {@code anchors} (how many seats, hearths and beds the sector knows, how many
 * stand in this room, and which is nearest: the quickest check that a template's markers really arrived in the world). Permission 2 on the whole
 * tree, messages as literals (the pattern of {@code LabyrinthCommand}); the lead attaches it under {@code /multiverse mega}.
 */
public final class SafeCommand {
    private SafeCommand() {}

    private static int ok(CommandSourceStack s, String msg) { s.sendSuccess(() -> Component.literal(msg), false); return 1; }

    private static int fail(CommandSourceStack s, String msg) { s.sendFailure(Component.literal(msg)); return 0; }

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("rest").requires(s -> s.hasPermission(2))
                .then(Commands.literal("status")
                        .executes(ctx -> status(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> status(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("refill")
                        .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> refill(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("anchors").executes(ctx -> anchors(ctx.getSource())));
    }

    private static String clock(long ticks) {
        long s = Math.max(0L, ticks) / 20L;
        return String.format("%d:%02d", s / 60L, s % 60L);
    }

    private static int status(CommandSourceStack src, ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        long now = level.getGameTime();
        RestState st = p.getData(SafeRegistry.REST_STATE);
        int uses = SafeConfig.get(SafeConfig.SHORT_REST_USES);
        long cooldown = RestRules.longCooldownRemaining(now, st.lastLongRest, SafeConfig.get(SafeConfig.LONG_REST_COOLDOWN_TICKS));
        StringBuilder b = new StringBuilder(p.getGameProfile().getName()).append(": ");
        b.append("short rests left ").append(RestRules.shortRestsLeft(uses, st.shortRestsUsed)).append('/').append(uses);
        b.append(", long rest ").append(cooldown > 0 ? "ready in " + clock(cooldown) : "ready");
        if (st.lastRest >= 0) b.append(", last rest ").append(clock(now - st.lastRest)).append(" ago");
        if (st.channel != null) {
            long elapsed = now - st.channel.start;
            b.append(", RESTING (").append(st.channel.kind.id()).append(' ').append(RestRules.progressPercent(elapsed, st.channel.total)).append("%)");
        }
        boolean mega = Labyrinth.inMega(level);
        b.append(". ");
        if (mega) {
            Sector sector = MegaDungeon.sectorAt(level, p.blockPosition());
            b.append("In the Mega-Dungeon");
            b.append(sector == null ? ", between sectors" : ", sector " + sector.id + " (threat " + sector.threat + ")");
            b.append(MegaDungeon.inSafeZone(level, p.blockPosition()) ? ", in a safe zone" : ", not in a safe zone");
            b.append(", regen throttle ").append(RegenThrottle.throttled(p) ? "ON" : "off").append(" (scale ").append(SafeConfig.get(SafeConfig.PASSIVE_REGEN_SCALE)).append(')');
        } else {
            b.append("Not in the Mega-Dungeon");
        }
        double mana = RestMana.magiculeShare(p);
        if (mana >= 0) b.append(". Mana ").append(Math.round(mana * 100)).append('%');
        Map<String, Integer> hints = ModCompatProcessor.availableHints();
        b.append(". Compat hints: ").append(hints.isEmpty() ? "none (no compat mod loaded)" : hints.toString());
        return ok(src, b.toString());
    }

    private static int refill(CommandSourceStack src, ServerPlayer p) {
        p.getData(SafeRegistry.REST_STATE).refill();
        p.displayClientMessage(Component.translatable("message.nusmp.mega.safe.refilled"), true);
        return ok(src, p.getGameProfile().getName() + ": every short rest is back, the long-rest cool-down is cleared.");
    }

    private static int anchors(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel level = p.serverLevel();
        if (!Labyrinth.inMega(level)) return fail(src, "You are not in the Mega-Dungeon.");
        Sector sector = MegaDungeon.sectorAt(level, p.blockPosition());
        if (sector == null) return fail(src, "You stand between sectors: no rest anchors here.");
        Room room = sector.roomAt(p.blockPosition());
        Map<MarkerKind, Integer> inSector = new EnumMap<>(MarkerKind.class), inRoom = new EnumMap<>(MarkerKind.class);
        Map<MarkerKind, Marker> nearest = new EnumMap<>(MarkerKind.class);
        for (Marker m : RestAnchors.all(sector)) {
            inSector.merge(m.kind(), 1, Integer::sum);
            if (room != null && room.contains(m.pos())) inRoom.merge(m.kind(), 1, Integer::sum);
            Marker best = nearest.get(m.kind());
            if (best == null || m.pos().distSqr(p.blockPosition()) < best.pos().distSqr(p.blockPosition())) nearest.put(m.kind(), m);
        }
        StringBuilder b = new StringBuilder("Sector ").append(sector.id).append(" rest anchors");
        if (room != null) b.append(", this room is ").append(room.kind).append(" (").append(room.template).append(')');
        b.append(":");
        for (MarkerKind k : new MarkerKind[] {MarkerKind.SEAT, MarkerKind.HEARTH, MarkerKind.BED}) {
            b.append(' ').append(k.id()).append(' ').append(inSector.getOrDefault(k, 0)).append(" in the sector, ").append(inRoom.getOrDefault(k, 0)).append(" in this room");
            Marker near = nearest.get(k);
            if (near != null) {
                BlockPos at = near.pos();
                b.append(" (nearest ").append(Math.round(Math.sqrt(at.distSqr(p.blockPosition())))).append(" blocks at ").append(at.getX()).append(' ').append(at.getY()).append(' ').append(at.getZ()).append(')');
            }
            b.append(';');
        }
        return ok(src, b.toString());
    }
}
