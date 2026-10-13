package com.newuniverse.nusmp.entity.avatar;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /multiverse boss avatar [lucifero | megicula]} (permission 2): spawns the Devil Avatar at the executor with its arena there, a
 * random one when no variant is named. The multiverse command calls {@link #addTo} with its "boss" node, next to the Zogratis bosses.
 */
public final class AvatarCommand {
    private AvatarCommand() {}

    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> bossNode) {
        bossNode.then(Commands.literal("avatar").requires(s -> s.hasPermission(2))
                .executes(ctx -> summon(ctx, -1))
                .then(Commands.literal("lucifero").executes(ctx -> summon(ctx, AvatarTable.LUCIFERO)))
                .then(Commands.literal("megicula").executes(ctx -> summon(ctx, AvatarTable.MEGICULA))));
    }

    private static int summon(CommandContext<CommandSourceStack> ctx, int variant) {
        ServerLevel level = ctx.getSource().getLevel();
        Vec3 at = ctx.getSource().getPosition();
        int v = variant < 0 ? (level.random.nextBoolean() ? AvatarTable.MEGICULA : AvatarTable.LUCIFERO) : variant;
        DevilAvatarBoss boss = AvatarEntities.spawn(level, at, 0f, v, null, null, true);
        if (boss == null) {
            ctx.getSource().sendFailure(Component.literal("The Devil Avatar could not be summoned here."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(boss.bossName() + " descends, a high devil of the Underworld. The arena is here."), true);
        return 1;
    }
}
