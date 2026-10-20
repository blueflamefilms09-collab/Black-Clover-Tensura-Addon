package com.newuniverse.nusmp.labyrinth.mega.admin;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.floors.AlignmentAttachments;
import com.newuniverse.nusmp.labyrinth.mega.floors.FloorTracker;
import com.newuniverse.nusmp.labyrinth.mega.rift.Waypoint;
import com.newuniverse.nusmp.labyrinth.mega.rift.WaypointService;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.function.IntSupplier;

/**
 * Why: the executors of the {@code /dungeon} admin tree, one static method per command, all server side. Every one of them starts the same way: find the PLAYER the command acts on
 * ({@link #player}); a console, a command block or a function has none, and then the answer is a red line that says how to run the command (as a player, or {@code /execute as
 * <player> run dungeon ...}), never an exception. Every one of them is also wrapped by {@link #guarded}, so a bug or a missing system below (a hook that throws, a skill list that
 * is not ready) ends as a red line and a log entry and never as an exception out of the command, which the game would print as an opaque error and which could leave half a job
 * behind. They do not do the heavy work themselves: a teleport to a floor that does not exist becomes a ticket on a clock ({@link FloorTeleport}), granting 250 skills becomes a job on
 * a clock ({@link SkillAdmin}), and the bosses and rifts go through the two small hooks ({@link DungeonAdminHooks}). The arithmetic is {@link DungeonAdminMath}.
 */
final class AdminCommands {
    private AdminCommands() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** How far the crosshair is followed for a rift, in blocks. */
    static final double RIFT_REACH = 32.0;
    /** How far in front of the player a rift opens when the crosshair hits nothing. */
    static final int RIFT_FALLBACK = 3;

    // ================================================================ plumbing
    /** The player the command acts on, or null (after telling the sender in red) when the source is not a player. */
    static ServerPlayer player(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        if (p == null) src.sendFailure(AdminMsg.red(AdminText.NEED_PLAYER));
        return p;
    }

    /** Runs a command body; a runtime failure becomes a red line, a log entry and a 0. */
    static int guarded(CommandSourceStack src, String what, IntSupplier body) {
        try {
            return body.getAsInt();
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("[nusmp] /dungeon {} failed: {}", what, e.toString(), e);
            return AdminMsg.fail(src, AdminMsg.red(AdminText.FAILED, e.getClass().getSimpleName()));
        }
    }

    /** The signed floor the player stands on: 0 in the Hub, the floor under their feet in the dungeon, else the floor of their waypoint (where they left from), else 0. */
    static int currentFloor(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        if (Labyrinth.inSanctuary(level)) return 0;
        if (MegaDungeon.in(level)) {
            int f = FloorTracker.floorAt(level, p.blockPosition());
            if (f == 0) f = FloorTracker.currentFloor(p);
            if (f == 0) {
                Sector s = MegaDungeon.sectorAt(level, p.blockPosition());
                if (s != null) f = s.firstFloor();
            }
            return f;
        }
        Waypoint w = WaypointService.get(p);
        return w == null ? 0 : w.floor();
    }

    /** Which way "forward" is for a player who stands on Floor 0: the road they are bound to, else the road of their waypoint, else the Holy road. */
    static int hubSide(ServerPlayer p) {
        Polarity chosen = AlignmentAttachments.get(p).chosen();
        if (chosen != null) return chosen.sign();
        Waypoint w = WaypointService.get(p);
        return w != null && w.floor() != 0 ? Integer.signum(w.floor()) : 1;
    }

    // ================================================================ tp and skip
    static int tp(CommandContext<CommandSourceStack> ctx, int floor) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer p = player(src);
        if (p == null) return 0;
        return guarded(src, "tp", () -> FloorTeleport.request(p, floor));
    }

    static int skip(CommandContext<CommandSourceStack> ctx, int floors) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer p = player(src);
        if (p == null) return 0;
        return guarded(src, "skip", () -> {
            if (floors == 0) return AdminMsg.fail(src, AdminMsg.red(AdminText.SKIP_ZERO));
            int current = currentFloor(p);
            if (current == 0 && floors < 0) return AdminMsg.fail(src, AdminMsg.red(AdminText.SKIP_HUB_BACK));
            DungeonAdminMath.Skip skip = DungeonAdminMath.skip(current, floors, hubSide(p));
            if (skip.target() == current) return AdminMsg.fail(src, AdminMsg.red(AdminText.SKIP_CAPPED, FloorMap.label(skip.target())));
            Component way = AdminMsg.text(floors > 0 ? AdminText.SKIP_FORWARD : AdminText.SKIP_BACK);
            AdminMsg.ok(src, AdminMsg.green(AdminText.SKIP_GO, Math.abs(floors), way, FloorMap.label(current), FloorMap.label(skip.target())));
            if (skip.capped()) AdminMsg.tell(p, AdminMsg.yellow(AdminText.SKIP_CAPPED, FloorMap.label(skip.target())));
            return FloorTeleport.request(p, skip.target());
        });
    }

    // ================================================================ bosses
    static int bossSpawn(CommandContext<CommandSourceStack> ctx, int level) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer p = player(src);
        if (p == null) return 0;
        if (!DungeonAdminMath.validLevel(level)) return AdminMsg.fail(src, AdminMsg.red(AdminText.BOSS_LEVEL, level));
        return guarded(src, "boss spawn", () -> {
            BlockPos at = p.blockPosition();
            boolean made = DungeonAdminHooks.bossHook().spawnBoss(p.serverLevel(), level, at);
            if (!made) return AdminMsg.fail(src, AdminMsg.red(AdminText.BOSS_SPAWN_FAIL, level));
            return AdminMsg.ok(src, AdminMsg.green(AdminText.BOSS_SPAWN_OK, level, FloorMap.label(DungeonAdminMath.bossFloorOfLevel(level)), at.toShortString()));
        });
    }

    static int bossKill(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer p = player(src);
        if (p == null) return 0;
        return guarded(src, "boss kill", () -> {
            int slain = DungeonAdminHooks.bossHook().killBosses(p.serverLevel(), p);
            if (slain <= 0) return AdminMsg.fail(src, AdminMsg.red(AdminText.BOSS_KILL_NONE));
            return AdminMsg.ok(src, AdminMsg.green(AdminText.BOSS_KILL_OK, slain));
        });
    }

    // ================================================================ rifts
    static int riftSpawn(CommandContext<CommandSourceStack> ctx, boolean pocket) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer p = player(src);
        if (p == null) return 0;
        return guarded(src, "rift spawn", () -> {
            ServerLevel level = p.serverLevel();
            BlockPos aim = crosshair(p), front = front(p);
            BlockPos at = aim;
            boolean made = DungeonAdminHooks.riftHook().spawnRift(level, aim, pocket);
            if (!made && !aim.equals(front)) {                                           // the cell at the crosshair was not free: the cell in front of the player
                at = front;
                made = DungeonAdminHooks.riftHook().spawnRift(level, front, pocket);
            }
            Component kind = AdminMsg.text(pocket ? AdminText.RIFT_KIND_POCKET : AdminText.RIFT_KIND_SHORTCUT);
            if (!made) return AdminMsg.fail(src, AdminMsg.red(AdminText.RIFT_FAIL, aim.toShortString()));
            return AdminMsg.ok(src, AdminMsg.green(AdminText.RIFT_OK, kind, at.toShortString()));
        });
    }

    /** The air cell in front of the block under the crosshair (32 blocks), or the cell three blocks in front of the player when the crosshair hits nothing. */
    static BlockPos crosshair(ServerPlayer p) {
        HitResult hit = p.pick(RIFT_REACH, 1.0f, false);
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult b) return b.getBlockPos().relative(b.getDirection());
        return front(p);
    }

    static BlockPos front(ServerPlayer p) { return p.blockPosition().relative(p.getDirection(), RIFT_FALLBACK); }

    // ================================================================ skills and resets
    static int skillUnlock(CommandContext<CommandSourceStack> ctx, String id) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer p = player(src);
        if (p == null) return 0;
        return guarded(src, "skill unlock", () -> SkillAdmin.unlock(p, id));
    }

    static int skillGiveSp(CommandContext<CommandSourceStack> ctx, int amount) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer p = player(src);
        if (p == null) return 0;
        return guarded(src, "skill give_sp", () -> SkillAdmin.giveMastery(p, amount));
    }

    enum Reset { ALIGNMENT, SKILLS, WAYPOINT, ALL }

    static int reset(CommandContext<CommandSourceStack> ctx, Reset what) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer p = player(src);
        if (p == null) return 0;
        return guarded(src, "reset " + what.name().toLowerCase(Locale.ROOT), () -> switch (what) {
            case ALIGNMENT -> ResetAdmin.doAlignment(p);
            case WAYPOINT -> ResetAdmin.doWaypoint(p);
            case SKILLS -> SkillAdmin.forgetAll(p, false);
            case ALL -> ResetAdmin.doAll(p);
        });
    }

    // ================================================================ debug and help
    static int debugWfc(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer p = player(src);
        if (p == null) return 0;
        return guarded(src, "debug wfc", () -> WfcDebugService.toggle(p));
    }

    static int debugInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer p = player(src);
        if (p == null) return 0;
        return guarded(src, "debug info", () -> {
            ServerLevel level = p.serverLevel();
            if (Labyrinth.inSanctuary(level)) return AdminMsg.ok(src, AdminMsg.green(AdminText.DEBUG_INFO_HUB));
            if (!MegaDungeon.in(level)) {
                Waypoint w = WaypointService.get(p);
                AdminMsg.fail(src, AdminMsg.red(AdminText.NOT_IN_DUNGEON, level.dimension().location().toString()));
                Component where = w == null ? AdminMsg.text(AdminText.DEBUG_INFO_NO_WAYPOINT) : AdminMsg.text(AdminText.DEBUG_INFO_WAYPOINT_AT, FloorMap.label(w.floor()), w.sector());
                src.sendSuccess(() -> AdminMsg.gray(AdminText.DEBUG_INFO_WAYPOINT, where), false);
                return 0;
            }
            Sector s = MegaDungeon.sectorAt(level, p.blockPosition());
            if (s == null) return AdminMsg.fail(src, AdminMsg.red(AdminText.NO_SECTOR));
            int floor = currentFloor(p);
            AdminMsg.ok(src, AdminMsg.green(AdminText.DEBUG_INFO, FloorMap.label(floor), DungeonAdminMath.levelOfFloor(floor), s.theme.id(), Long.toString(s.seed)));
            Component boss = DungeonAdminMath.isBossFloor(floor) ? AdminMsg.text(AdminText.DEBUG_INFO_BOSS) : Component.empty();
            src.sendSuccess(() -> AdminMsg.gray(AdminText.DEBUG_INFO_MORE, s.id, s.polarity.id(), s.party, s.state.name().toLowerCase(Locale.ROOT), s.rooms.size(), boss), false);
            return 1;
        });
    }

    static int help(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> AdminMsg.text(AdminText.HELP_HEAD).withStyle(ChatFormatting.GOLD), false);
        for (AdminSpec.Entry e : AdminSpec.ENTRIES) {
            src.sendSuccess(() -> Component.literal("/" + AdminSpec.ROOT + " " + e.usage() + "  ").withStyle(ChatFormatting.YELLOW).append(AdminMsg.text(e.helpKey()).withStyle(ChatFormatting.GRAY)), false);
        }
        return AdminSpec.ENTRIES.size();
    }
}
