package com.newuniverse.nusmp.labyrinth.mega.admin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

/**
 * Why: this is the command tree, and the reasons for its shape are the ones that make it safe next to what already exists. (1) It is registered under the SAME literal as the
 * player's {@code /dungeon} ({@code floor}, {@code unstuck}, {@code status}, {@code release}, {@code help}, from {@code lock.DungeonCommand}): Brigadier merges the children of two
 * registrations that share a literal, so one tree answers both, with no rename and no removal of anything that was there. (2) The root literal carries NO permission, because that
 * would take the player's subcommands away from everyone below level 2; the gate ({@code hasPermission(2)}) sits on each ADMIN child ({@code tp}, {@code skip}, {@code boss},
 * {@code rift}, {@code skill}, {@code reset}, {@code debug}, {@code admin}), and a gated node is also hidden from tab completion and from the client's command tree for players
 * below level 2. Everything under a gated child is reached only through it, so the children need no gate of their own. (3) Integer arguments carry their ranges, so a floor of
 * 1001 or a level of 101 never reaches the executor: Brigadier refuses it with its own red message and its own caret. (4) The skill id is a word with a suggestion provider fed
 * from the skill registry ({@link SkillAdmin#suggestions}), so tab completion always matches what exists. (5) Each executor is a one-line call into {@link AdminCommands}; no logic
 * lives here. The shape is mirrored as plain data in {@link AdminSpec}, which a unit test and the {@code dungeon.admin} self-test compare against this tree.
 */
public final class AdminTree {
    private AdminTree() {}

    /** The node to register: {@code dispatcher.register(AdminTree.node())}. */
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        int maxFloor = DungeonAdminMath.MAX_FLOOR, maxLevel = DungeonAdminMath.MAX_LEVEL;
        return Commands.literal(AdminSpec.ROOT)
                .then(gated("tp")
                        .then(Commands.argument("floor", IntegerArgumentType.integer(-maxFloor, maxFloor))
                                .executes(c -> AdminCommands.tp(c, IntegerArgumentType.getInteger(c, "floor")))))
                .then(gated("skip")
                        .executes(c -> AdminCommands.skip(c, 1))
                        .then(Commands.argument("floors", IntegerArgumentType.integer(-maxFloor, maxFloor))
                                .executes(c -> AdminCommands.skip(c, IntegerArgumentType.getInteger(c, "floors")))))
                .then(gated("boss")
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("level", IntegerArgumentType.integer(-maxLevel, maxLevel))
                                        .executes(c -> AdminCommands.bossSpawn(c, IntegerArgumentType.getInteger(c, "level")))))
                        .then(Commands.literal("kill").executes(AdminCommands::bossKill)))
                .then(gated("rift")
                        .then(Commands.literal("spawn")
                                .then(Commands.literal("pocket").executes(c -> AdminCommands.riftSpawn(c, true)))
                                .then(Commands.literal("shortcut").executes(c -> AdminCommands.riftSpawn(c, false)))))
                .then(gated("skill")
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("skill", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(SkillAdmin.suggestions(), b))
                                        .executes(c -> AdminCommands.skillUnlock(c, StringArgumentType.getString(c, "skill")))))
                        .then(Commands.literal("give_sp")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, AdminSpec.SP_MAX))
                                        .executes(c -> AdminCommands.skillGiveSp(c, IntegerArgumentType.getInteger(c, "amount"))))))
                .then(gated("reset")
                        .then(Commands.literal("alignment").executes(c -> AdminCommands.reset(c, AdminCommands.Reset.ALIGNMENT)))
                        .then(Commands.literal("skills").executes(c -> AdminCommands.reset(c, AdminCommands.Reset.SKILLS)))
                        .then(Commands.literal("waypoint").executes(c -> AdminCommands.reset(c, AdminCommands.Reset.WAYPOINT)))
                        .then(Commands.literal("all").executes(c -> AdminCommands.reset(c, AdminCommands.Reset.ALL))))
                .then(gated("debug")
                        .then(Commands.literal("wfc").executes(AdminCommands::debugWfc))
                        .then(Commands.literal("info").executes(AdminCommands::debugInfo)))
                .then(gated("admin")
                        .then(Commands.literal("help").executes(AdminCommands::help)));
    }

    /** A child of {@code /dungeon} that only operators (permission level {@link AdminSpec#PERMISSION}) can see or use. */
    private static LiteralArgumentBuilder<CommandSourceStack> gated(String name) {
        return Commands.literal(name).requires(s -> s.hasPermission(AdminSpec.PERMISSION));
    }
}
