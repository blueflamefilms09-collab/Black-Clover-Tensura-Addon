package com.newuniverse.nusmp.arc.magi;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.newuniverse.nusmp.arc.ArcSpawner;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the operator's door to the Sinbad boss ({@code /multiverse sinbad_boss ...}, permission 2): {@code spawn [pos]} puts one at the spot (at your feet by default; the same checks as every
 * other door: the {@code enabled} setting, the global cap and one boss per arena), {@code status} lists the live ones with their djinn and health, {@code clear} removes them all. The normal
 * arc doors ({@code /multiverse arc summon sinbad_boss}, the spawn egg) go through the same checks.
 */
public final class SinbadBossCommand {
    private SinbadBossCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("sinbad_boss").requires(s -> s.hasPermission(2))
                .then(Commands.literal("spawn").executes(ctx -> spawn(ctx, null))
                        .then(Commands.argument("pos", Vec3Argument.vec3()).executes(ctx -> spawn(ctx, Vec3Argument.getVec3(ctx, "pos")))))
                .then(Commands.literal("status").executes(SinbadBossCommand::status))
                .then(Commands.literal("clear").executes(SinbadBossCommand::clear));
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendFailure(Component.literal(text));
        return 0;
    }

    private static int ok(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx, Vec3 at) {
        ServerLevel sl = ctx.getSource().getLevel();
        Vec3 pos = at != null ? at : ctx.getSource().getPosition();
        ArcSpawner.Result r = ArcSpawner.summon(sl, SinbadBoss.CHARACTER, pos, ctx.getSource().getRotation().y, List.of(), true);
        return switch (r.outcome()) {
            case SPAWNED -> ok(ctx, "Sinbad appears.");
            case DISABLED -> fail(ctx, "The Sinbad boss is switched off (nusmp-sinbad-boss-server.toml: enabled).");
            case ALREADY_PRESENT -> fail(ctx, "Another Sinbad already stands within " + (int) SinbadBosses.ARENA_CLEARANCE + " blocks of there (one boss per arena).");
            case FAILED -> fail(ctx, "Sinbad could not be summoned: " + SinbadBossConfig.globalCap() + " of him already stand, or there is no room here.");
            default -> fail(ctx, "Sinbad could not be summoned (" + r.outcome() + ").");
        };
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        List<SinbadBoss> all = SinbadBoss.live();
        if (all.isEmpty()) return ok(ctx, "No Sinbad boss stands in the world.");
        for (SinbadBoss b : all) {
            ok(ctx, "Sinbad at " + (int) b.getX() + " " + (int) b.getY() + " " + (int) b.getZ() + ": djinn " + b.equipId() + ", power " + b.power() + "%, health " + (int) b.getHealth() + "/" + (int) b.getMaxHealth());
        }
        return all.size();
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        int n = 0;
        for (SinbadBoss b : SinbadBoss.live()) {
            b.discard();
            n++;
        }
        return ok(ctx, "Removed " + n + " Sinbad boss(es).");
    }
}
