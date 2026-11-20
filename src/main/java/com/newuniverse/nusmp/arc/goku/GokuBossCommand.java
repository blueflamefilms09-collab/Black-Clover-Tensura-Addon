package com.newuniverse.nusmp.arc.goku;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import com.newuniverse.nusmp.arc.ArcSpawner;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the operator's door to the Goku boss ({@code /multiverse goku_boss ...}, permission 2): {@code spawn [pos]} puts one at the spot (at your feet by default; the same checks as every other
 * door: the {@code enabled} setting and the global cap of {@code globalCap}), {@code status} lists the live ones with their form, scaling reading and health, {@code clear} removes them all.
 * The normal arc doors ({@code /multiverse arc summon goku_boss}, the spawn egg) go through the same checks.
 */
public final class GokuBossCommand {
    private GokuBossCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("goku_boss").requires(s -> s.hasPermission(2))
                .then(Commands.literal("spawn").executes(ctx -> spawn(ctx, null))
                        .then(Commands.argument("pos", Vec3Argument.vec3()).executes(ctx -> spawn(ctx, Vec3Argument.getVec3(ctx, "pos")))))
                .then(Commands.literal("status").executes(GokuBossCommand::status))
                .then(Commands.literal("clear").executes(GokuBossCommand::clear));
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
        if (at == null && ctx.getSource().getPlayer() != null && com.newuniverse.nusmp.arc.realm.BossRealm.open(ctx.getSource().getPlayer(), GokuBoss.CHARACTER)) return 1;   // the boss realm; an explicit position still spawns in place
        Vec3 pos = at != null ? at : ctx.getSource().getPosition();
        ArcSpawner.Result r = ArcSpawner.summon(sl, GokuBoss.CHARACTER, pos, ctx.getSource().getRotation().y, List.of(), true);
        return switch (r.outcome()) {
            case SPAWNED -> ok(ctx, "Son Goku appears. He is read at the strongest players near him (Existence Points first).");
            case DISABLED -> fail(ctx, "The Goku boss is switched off (nusmp-goku-boss-server.toml: enabled).");
            case ALREADY_PRESENT -> fail(ctx, "Another Goku already stands within " + (int) GokuBosses.ARENA_CLEARANCE + " blocks of there (one boss per arena).");
            case FAILED -> fail(ctx, "Goku could not be summoned: " + GokuBossConfig.globalCap() + " of him already stand, or there is no room here.");
            default -> fail(ctx, "Goku could not be summoned (" + r.outcome() + ").");
        };
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        List<GokuBoss> all = GokuBoss.live();
        if (all.isEmpty()) return ok(ctx, "No Goku boss stands in the world.");
        for (GokuBoss b : all) {
            ok(ctx, "Goku at " + (int) b.getX() + " " + (int) b.getY() + " " + (int) b.getZ() + ": " + GokuBoss.formName(b.formIdx()) + ", health " + (int) b.getHealth() + "/" + (int) b.getMaxHealth()
                    + ", read " + Math.round(b.reading().ep()) + " EP (" + b.reading().source().name().toLowerCase(java.util.Locale.ROOT) + "), " + b.countedPlayers() + " player(s), health x"
                    + String.format(java.util.Locale.ROOT, "%.2f", b.healthFactor()) + ", damage x" + String.format(java.util.Locale.ROOT, "%.2f", b.damageFactorValue()));
        }
        return all.size();
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        int n = 0;
        for (GokuBoss b : GokuBoss.live()) {
            b.discard();
            n++;
        }
        return ok(ctx, "Removed " + n + " Goku boss(es).") ;
    }
}
