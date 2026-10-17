package com.newuniverse.nusmp.dungeon.floor.goliath;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /multiverse boss goliath} (permission 2): a Goliath stands where the executor does, its arena there. {@code goliath rise} lets it climb out of
 * the floor under the executor instead (three seconds of cracking, then the climb). The multiverse command calls {@link #addTo} with its "boss" node.
 * (Until 1.58.0 a Goliath's Pit in the Living Dungeon woke one the same way; the pit was removed, the boss and this way to call it stayed.)
 */
public final class GoliathCommand {
    private GoliathCommand() {}

    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> bossNode) {
        bossNode.then(Commands.literal("goliath").requires(s -> s.hasPermission(2)).executes(ctx -> spawn(ctx, false))
                .then(Commands.literal("rise").requires(s -> s.hasPermission(2)).executes(ctx -> spawn(ctx, true))));
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx, boolean rise) {
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 here = ctx.getSource().getPosition();
        GoliathBoss boss;
        if (rise) {
            Vec3 floor = ZogratisBoss.ground(level, here);
            boss = GoliathBoss.spawnRising(level, BlockPos.containing(floor).below(), 0f);
        } else {
            boss = GoliathBoss.spawnStanding(level, ZogratisBoss.ground(level, here), 0f);
        }
        if (boss == null) {
            ctx.getSource().sendFailure(Component.literal("The Goliath could not be made here."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(rise ? "The bedrock cracks: a Goliath is climbing out of the floor. The arena is here."
                : "A Goliath stands here. The arena is here."), true);
        return 1;
    }
}
