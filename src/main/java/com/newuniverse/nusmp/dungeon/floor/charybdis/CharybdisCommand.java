package com.newuniverse.nusmp.dungeon.floor.charybdis;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /multiverse boss charybdis} (permission 2): spawns one Charybdis Spawn at the executor, with the arena there (it floats in the air and
 * steers the same way). The multiverse command calls {@link #addTo} with its "boss" node. (Until 1.58.0 inside a Charybdis Basin of the Living
 * Dungeon it appeared over the dais; the basin was removed, the boss and this way to call it stayed.)
 */
public final class CharybdisCommand {
    private CharybdisCommand() {}

    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> bossNode) {
        bossNode.then(Commands.literal("charybdis").requires(s -> s.hasPermission(2)).executes(CharybdisCommand::spawn));
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 at = ctx.getSource().getPosition();
        CharybdisSpawnBoss boss = CharybdisEntities.CHARYBDIS_SPAWN.get().create(level);
        if (boss == null) {
            ctx.getSource().sendFailure(Component.literal("The Charybdis Spawn could not be summoned here."));
            return 0;
        }
        boss.moveTo(at.x, at.y, at.z, 0f, 0f);
        boss.setArena(BlockPos.containing(at));
        level.addFreshEntity(boss);
        ctx.getSource().sendSuccess(() -> Component.literal("The Charybdis Spawn appears. The arena is here (it floats in the air, and it pays the Djinn Seal of its fall only inside the Labyrinth)."), true);
        return 1;
    }
}
