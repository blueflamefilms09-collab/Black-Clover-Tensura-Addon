package com.newuniverse.nusmp.arc.kakashi;

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
 * Why: the operator's door to the Kakashi boss ({@code /multiverse kakashi_boss ...}, permission 2): {@code spawn [pos]} puts one at the spot (at your feet by default; the same checks as every
 * other door: the {@code enabled} setting and the global cap), {@code status} lists the live ones with phase and health, {@code clear} removes them all. The normal arc doors
 * ({@code /multiverse arc summon kakashi_hatake}, the spawn egg) go through the same checks.
 */
public final class KakashiCommand {
    private KakashiCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("kakashi_boss").requires(s -> s.hasPermission(2))
                .then(Commands.literal("spawn").executes(ctx -> spawn(ctx, null))
                        .then(Commands.argument("pos", Vec3Argument.vec3()).executes(ctx -> spawn(ctx, Vec3Argument.getVec3(ctx, "pos")))))
                .then(Commands.literal("status").executes(KakashiCommand::status))
                .then(Commands.literal("clear").executes(KakashiCommand::clear));
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
        ArcSpawner.Result r = ArcSpawner.summon(sl, KakashiBoss.CHARACTER, pos, ctx.getSource().getRotation().y, List.of(), true);
        return switch (r.outcome()) {
            case SPAWNED -> ok(ctx, "Kakashi Hatake appears.");
            case DISABLED -> fail(ctx, "The Kakashi boss is switched off (nusmp-kakashi-boss-server.toml: enabled).");
            case ALREADY_PRESENT -> fail(ctx, "Another Kakashi already stands within " + (int) KakashiBosses.ARENA_CLEARANCE + " blocks of there (one boss per arena).");
            case FAILED -> fail(ctx, "Kakashi could not be summoned: " + KakashiConfig.globalCap() + " of him already stand, or there is no room here.");
            default -> fail(ctx, "Kakashi could not be summoned (" + r.outcome() + ").");
        };
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        List<KakashiBoss> all = KakashiBoss.live();
        if (all.isEmpty()) return ok(ctx, "No Kakashi boss stands in the world.");
        for (KakashiBoss b : all) {
            ok(ctx, "Kakashi at " + (int) b.getX() + " " + (int) b.getY() + " " + (int) b.getZ() + ": phase " + (b.phaseIdx() + 1) + " (" + KakashiMath.PHASE_NAMES[b.phaseIdx()] + "), health "
                    + (int) b.getHealth() + "/" + (int) b.getMaxHealth() + ", copy ledger " + b.ledger.size());
        }
        return all.size();
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        int n = 0;
        for (KakashiBoss b : KakashiBoss.live()) {
            b.discard();
            n++;
        }
        return ok(ctx, "Removed " + n + " Kakashi boss(es).");
    }
}
