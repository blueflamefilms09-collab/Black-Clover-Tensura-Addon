package com.newuniverse.nusmp.vfx;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Test command (ops): /nusmp vfx <shape> [power]
 * Plays the effect from you toward the block you're looking at, so you can preview VFX
 * without any skill code.
 */
public final class VfxCommand {
    private VfxCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nusmp").requires(s -> s.hasPermission(2))
                .then(Commands.literal("vfx")
                        .then(Commands.argument("shape", StringArgumentType.word())
                                .suggests((ctx, b) -> { for (VfxShape s : VfxShape.values()) b.suggest(s.name().toLowerCase()); return b.buildFuture(); })
                                .executes(ctx -> play(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "shape"), 1f))
                                .then(Commands.argument("power", FloatArgumentType.floatArg(0.1f, 5f))
                                        .executes(ctx -> play(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "shape"),
                                                FloatArgumentType.getFloat(ctx, "power")))))));
    }

    private static int play(ServerPlayer player, String name, float power) {
        VfxShape shape;
        try { shape = VfxShape.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { player.sendSystemMessage(Component.literal("Unknown effect: " + name)); return 0; }
        Vec3 eye = player.getEyePosition();
        Vec3 target = player.pick(24, 1f, false).getLocation();
        boolean atFeet = shape == VfxShape.WIND_RING || shape == VfxShape.ELF_CIRCLE || shape == VfxShape.DEVIL_CIRCLE;
        Vec3 from = atFeet ? player.position().add(0, 0.05, 0) : eye.add(player.getLookAngle().scale(1.5));
        if (atFeet || shape == VfxShape.MAGIC_CIRCLE || shape == VfxShape.MAGIC_CIRCLE_EXPLOSION) {
            VfxSpawn.sendFollowing(player.serverLevel(), shape, player, atFeet ? from.add(0, 1, 0) : target, 0, 0, power);
        } else {
            VfxSpawn.send(player.serverLevel(), shape, from, target, 0, 0, power);
        }
        return 1;
    }
}
