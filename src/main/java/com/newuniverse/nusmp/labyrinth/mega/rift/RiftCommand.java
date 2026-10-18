package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Why: the owner's hands on the rifts and the waypoint, attached under {@code /multiverse mega} (permission 2): {@code rift open <shortcut|pocket|return>} opens one in front of the sender (the way to
 * look at the renderer and to test a destination), {@code rift list} lists the open ones, {@code rift closeall} closes them, {@code rift pocket list|collapse <slot>} shows and ends the instanced
 * realms, {@code rift waypoint show|save|clear|return [player]} reads and drives the waypoint (the first questions when "the crystal does not take me back"), and {@code rift status} shows the
 * switches. All text goes through lang keys {@code message.nusmp.mega.rift.cmd.*} with an English fallback.
 */
public final class RiftCommand {
    private RiftCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("rift").requires(s -> s.hasPermission(2))
                .then(Commands.literal("open")
                        .then(Commands.literal("shortcut").executes(c -> open(c.getSource(), RiftType.SHORTCUT)))
                        .then(Commands.literal("pocket").executes(c -> open(c.getSource(), RiftType.POCKET)))
                        .then(Commands.literal("return").executes(c -> open(c.getSource(), RiftType.RETURN))))
                .then(Commands.literal("list").executes(c -> list(c.getSource())))
                .then(Commands.literal("closeall").executes(c -> closeAll(c.getSource())))
                .then(Commands.literal("status").executes(c -> status(c.getSource())))
                .then(Commands.literal("pocket")
                        .then(Commands.literal("list").executes(c -> pockets(c.getSource())))
                        .then(Commands.literal("collapse").then(Commands.argument("slot", IntegerArgumentType.integer(0, RiftRules.MAX_SLOTS - 1))
                                .executes(c -> collapse(c.getSource(), IntegerArgumentType.getInteger(c, "slot"))))))
                .then(Commands.literal("waypoint")
                        .then(Commands.literal("show").executes(c -> show(c.getSource(), c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player()).executes(c -> show(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                        .then(Commands.literal("save").executes(c -> save(c.getSource(), c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player()).executes(c -> save(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                        .then(Commands.literal("clear").executes(c -> clear(c.getSource(), c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player()).executes(c -> clear(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                        .then(Commands.literal("return").executes(c -> back(c.getSource(), c.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player()).executes(c -> back(c.getSource(), EntityArgument.getPlayer(c, "player"))))));
    }

    private static MutableComponent text(String key, String english, Object... args) {
        return Component.translatableWithFallback("message.nusmp.mega.rift.cmd." + key, english, args);
    }

    private static int ok(CommandSourceStack s, MutableComponent c) {
        s.sendSuccess(() -> c, false);
        return 1;
    }

    private static int fail(CommandSourceStack s, MutableComponent c) {
        s.sendFailure(c);
        return 0;
    }

    private static int open(CommandSourceStack s, RiftType type) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = s.getPlayerOrException();
        ServerLevel level = p.serverLevel();
        BlockPos at = p.blockPosition().relative(p.getDirection(), 3);
        DimensionalRiftBlockEntity be = RiftService.open(level, at, type, level.getRandom().nextLong());
        if (be == null) return fail(s, text("nospace", "There is no room for a rift three blocks ahead."));
        if (type == RiftType.SHORTCUT) {
            Sector here = MegaData.get(level.getServer()).sectorAt(p.blockPosition());
            if (here == null) {
                be.close();
                return fail(s, text("nofloor", "A shortcut needs a floor of the Mega-Dungeon under you."));
            }
            RiftService.aimShortcut(MegaData.get(level.getServer()), here, be);
        }
        return ok(s, text("opened", "Opened a %s rift at %s.", type.id(), at.toShortString()));
    }

    private static int list(CommandSourceStack s) {
        var l = RiftService.openRifts();
        if (l.isEmpty()) return ok(s, text("none", "No rift is open."));
        for (BlockPos p : l) ok(s, text("row", "rift at %s", p.toShortString()));
        return l.size();
    }

    private static int closeAll(CommandSourceStack s) {
        int n = 0;
        for (ServerLevel level : s.getServer().getAllLevels())
            for (BlockPos p : RiftService.openRifts()) {
                if (!level.isLoaded(p)) continue;
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof DimensionalRiftBlockEntity r) {
                    r.close();
                    n++;
                }
            }
        return ok(s, text("closed", "Closed %s rifts.", n));
    }

    private static int status(CommandSourceStack s) {
        ServerLevel mega = MegaDungeon.level(s.getServer());
        PocketRealm realm = PocketRealm.get(s.getServer());
        return ok(s, text("status", "Rifts: enabled=%s pockets=%s waypoint=%s; open now %s; pockets alive %s; jump %s-%s floors; pocket %s min.", RiftConfig.get(RiftConfig.ENABLED),
                RiftConfig.get(RiftConfig.POCKETS), RiftConfig.get(RiftConfig.WAYPOINT), RiftService.openRifts().size(), realm.pockets().size(), RiftConfig.get(RiftConfig.JUMP_MIN),
                RiftConfig.get(RiftConfig.JUMP_MAX), RiftConfig.get(RiftConfig.POCKET_MINUTES)).append(mega == null ? text("nodim", " (the dungeon dimension is not loaded)") : Component.empty()));
    }

    private static int pockets(CommandSourceStack s) {
        PocketRealm realm = PocketRealm.get(s.getServer());
        if (realm.pockets().isEmpty()) return ok(s, text("nopockets", "No pocket realm is alive."));
        ServerLevel mega = MegaDungeon.level(s.getServer());
        for (PocketRealm.Pocket p : realm.pockets().values())
            ok(s, text("pocket", "slot %s: %s (floor %s), %s inside, %s s left%s", p.slot, p.variant, p.floor, mega == null ? 0 : PocketRealm.playersIn(mega, p.slot).size(),
                    mega == null ? 0 : Math.max(0, (p.expiresAt - mega.getGameTime()) / 20), p.bossDead || p.solved ? ", vault open" : ""));
        return realm.pockets().size();
    }

    private static int collapse(CommandSourceStack s, int slot) {
        ServerLevel mega = MegaDungeon.level(s.getServer());
        if (mega == null || !PocketRealm.get(s.getServer()).collapseSlot(mega, slot)) return fail(s, text("nosuch", "There is no pocket in slot %s.", slot));
        return ok(s, text("collapsed", "Collapsed the pocket in slot %s.", slot));
    }

    private static int show(CommandSourceStack s, ServerPlayer p) {
        Waypoint w = WaypointService.get(p);
        if (w == null) return fail(s, text("wp.none", "%s has no waypoint.", p.getName()));
        return ok(s, text("wp.show", "%s: floor %s (level %s, sector %s) at %s %s %s in %s, saved at tick %s.", p.getName(), w.floor(), w.level(), w.sector(), Math.round(w.x()), Math.round(w.y()),
                Math.round(w.z()), w.dimension(), w.savedAt()));
    }

    private static int save(CommandSourceStack s, ServerPlayer p) {
        return WaypointService.save(p) ? ok(s, text("wp.saved", "Saved a waypoint for %s.", p.getName())) : fail(s, text("wp.nosave", "%s is not standing anywhere a waypoint is kept.", p.getName()));
    }

    private static int clear(CommandSourceStack s, ServerPlayer p) {
        WaypointService.clear(p);
        return ok(s, text("wp.cleared", "Cleared the waypoint of %s.", p.getName()));
    }

    private static int back(CommandSourceStack s, ServerPlayer p) {
        return WaypointService.returnToWaypoint(p) ? ok(s, text("wp.back", "Sent %s to their waypoint.", p.getName())) : fail(s, text("wp.noback", "%s could not be sent to a waypoint.", p.getName()));
    }
}
