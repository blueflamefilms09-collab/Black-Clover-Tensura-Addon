package com.newuniverse.nusmp.relics.tier;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Locale;

/**
 * {@code /multiverse relic tier <player> up} and {@code ... set <grade>} (permission 2): raise the relic in the player's main hand one grade, or set its
 * tier to a grade (a grade at or under its base removes the tier). Hang it below the same "relic" word as the other relic commands:
 * {@code TierCommands.addTo(root)} (Brigadier merges the two "relic" literals).
 */
public final class TierCommands {
    private TierCommands() {}

    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("relic")
                .then(Commands.literal("tier").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("up")
                                        .executes(ctx -> up(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("grade", StringArgumentType.word())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                        Arrays.stream(Grade.values()).map(g -> g.shortName().toLowerCase(Locale.ROOT)), b))
                                                .executes(ctx -> set(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "grade"))))))));
    }

    private static int fail(CommandSourceStack s, String key) { s.sendFailure(Component.translatable(key)); return 0; }

    private static int up(CommandSourceStack src, ServerPlayer p) {
        ItemStack held = p.getMainHandItem();
        RelicDef d = Relics.defOf(held);
        if (d == null) return fail(src, "nusmp.tier.cmd.no_relic");
        int next = TierRules.nextTarget(Tiers.effectiveGrade(held, d).ordinal());
        if (next < 0) return fail(src, "nusmp.tier.top");
        return apply(src, p, held, d, next);
    }

    private static int set(CommandSourceStack src, ServerPlayer p, String gradeName) {
        ItemStack held = p.getMainHandItem();
        RelicDef d = Relics.defOf(held);
        if (d == null) return fail(src, "nusmp.tier.cmd.no_relic");
        Grade g = Grade.byName(gradeName);
        if (g == null) return fail(src, "nusmp.tier.cmd.bad_grade");
        return apply(src, p, held, d, g.ordinal());
    }

    private static int apply(CommandSourceStack src, ServerPlayer p, ItemStack held, RelicDef d, int tier) {
        Tiers.setTier(held, tier);
        Relics.invalidate(p);
        Grade now = Tiers.effectiveGrade(held, d);
        src.sendSuccess(() -> Component.translatable("nusmp.tier.cmd.done", held.getHoverName(), now.label()), true);
        return 1;
    }
}
