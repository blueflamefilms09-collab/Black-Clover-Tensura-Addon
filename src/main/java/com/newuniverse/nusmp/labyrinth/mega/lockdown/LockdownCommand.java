package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * Why: the owner cannot attach a debugger to a running game, so "why did this room not lock?" and "why did nothing ambush me?" must be answerable from the chat. The
 * operator's branch (permission 2) of {@code /multiverse mega}: {@code lockdown status} (the room the operator stands in: its rule, the director's last verdict, whether it
 * is cleared, locked, cooling or refused, and the operator's ambush ledger), {@code lockdown rules} (the room data in force), {@code lockdown release} (opens every locked
 * room, with no penalty), {@code lockdown forget} (the cleared marks of the dungeon, so rooms can lock again) and {@code lockdown ambush} (places the ambush for the
 * operator now, skipping the heat and the cooldown but not the exclusions). Every reply is a translatable component with an English fallback.
 */
final class LockdownCommand {
    private LockdownCommand() {}

    static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("lockdown").requires(s -> s.hasPermission(2))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("rules").executes(ctx -> rules(ctx.getSource())))
                .then(Commands.literal("release").executes(ctx -> release(ctx.getSource())))
                .then(Commands.literal("forget").executes(ctx -> forget(ctx.getSource())))
                .then(Commands.literal("ambush").executes(ctx -> ambush(ctx.getSource())));
    }

    private static int status(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        if (!Labyrinth.inMega(level)) {
            src.sendFailure(LockdownMsg.text(LockdownText.STATUS_NONE));
            return 0;
        }
        LockdownData data = LockdownData.get(src.getServer());
        src.sendSuccess(() -> LockdownMsg.text(LockdownText.STATUS_HEAD, LockdownDirector.lockedCount(), data.clearedCount(), DynamicAmbushDirector.firedTotal()).withStyle(ChatFormatting.AQUA), false);
        ServerPlayer p = src.getPlayer();
        if (p == null) return 1;
        Sector s = MegaDungeon.sectorAt(level, p.blockPosition());
        Room r = s == null ? null : s.roomAt(p.blockPosition());
        if (r == null) {
            src.sendSuccess(() -> LockdownMsg.text(LockdownText.STATUS_NONE).withStyle(ChatFormatting.GRAY), false);
        } else {
            LockdownRule rule = LockdownRooms.ruleFor(r.template, r.kind);
            String last = LockdownDirector.lastVerdict(s.id, r.id), refused = LockdownDirector.rejection(s.id, r.id);
            String line = (rule.lock() ? "locks (" + rule.band().label() + ", " + rule.waves() + " wave(s), from floor " + rule.minFloor() + ", chance " + String.format(Locale.ROOT, "%.0f%%", rule.chance() * 100.0) + ")" : "does not lock")
                    + "; cleared " + data.isCleared(s.id, r.id) + ", locked " + LockdownDirector.isLocked(level, s, r)
                    + (refused == null ? "" : ", refused: " + refused) + (last == null ? "" : ", last verdict " + last);
            src.sendSuccess(() -> LockdownMsg.text(LockdownText.STATUS_ROOM, r.template + " #" + r.id, r.kind.name().toLowerCase(Locale.ROOT), line).withStyle(ChatFormatting.YELLOW), false);
        }
        long now = level.getGameTime();
        AmbushRules.Config cfg = LockdownConfig.ambush();
        AmbushLedger ledger = DynamicAmbushDirector.ledger(p.getUUID());
        src.sendSuccess(() -> LockdownMsg.text(LockdownText.STATUS_AMBUSH, String.format(Locale.ROOT, "%.1f", ledger.heat(now, cfg)), String.format(Locale.ROOT, "%.1f", cfg.threshold()),
                ledger.cooldownLeft(now) / 20L, ledger.firedInWindow(now, cfg), DynamicAmbushDirector.lastText().isEmpty() ? "-" : DynamicAmbushDirector.lastText()).withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int rules(CommandSourceStack src) {
        LockdownTable t = LockdownRooms.table();
        src.sendSuccess(() -> net.minecraft.network.chat.Component.literal("Lockdown rules in force: " + t.kinds().size() + " kinds, " + t.templates().size() + " templates, " + t.patterns().size() + " patterns.").withStyle(ChatFormatting.AQUA), false);
        for (var e : t.kinds().entrySet()) {
            LockdownRule r = e.getValue();
            src.sendSuccess(() -> net.minecraft.network.chat.Component.literal("  " + e.getKey() + ": " + (r.lock() ? "locks, " + r.band().label() + " x" + r.waves() + ", chance " + r.chance() + ", floor >= " + r.minFloor() + ", area >= " + r.minArea() : "never locks")
                    + (r.note().isEmpty() ? "" : " (" + r.note() + ")")).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int release(CommandSourceStack src) {
        int n = LockdownDirector.releaseAll(src.getLevel());
        src.sendSuccess(() -> LockdownMsg.text(LockdownText.RELEASED, n).withStyle(ChatFormatting.GREEN), true);
        return n;
    }

    private static int forget(CommandSourceStack src) {
        int n = LockdownData.get(src.getServer()).forgetAll();
        LockdownDirector.forgetRefusals();
        src.sendSuccess(() -> LockdownMsg.text(LockdownText.FORGOT, n).withStyle(ChatFormatting.GREEN), true);
        return n;
    }

    private static int ambush(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        if (p == null || !Labyrinth.inMega(src.getLevel())) {
            src.sendFailure(LockdownMsg.text(LockdownText.STATUS_NONE));
            return 0;
        }
        long now = src.getLevel().getGameTime();
        DynamicAmbushDirector.Result r = DynamicAmbushDirector.attempt(src.getLevel(), p, DynamicAmbushDirector.ledger(p.getUUID()), LockdownConfig.ambush(), now, 0.0, 0.0, true);
        src.sendSuccess(() -> net.minecraft.network.chat.Component.literal("Ambush: " + r.why() + (r.fired() ? ", " + r.mobs() + " monsters (" + r.band().label() + ")" : "") + " - " + r.text()).withStyle(r.fired() ? ChatFormatting.RED : ChatFormatting.GRAY), false);
        return r.fired() ? 1 : 0;
    }
}
