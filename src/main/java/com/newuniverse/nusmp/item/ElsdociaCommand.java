package com.newuniverse.nusmp.item;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.multiverse.MultiverseProfile;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 1.97.0: {@code /multiverse relic elsdocia reset | owner <player> | lineage} (permission 2). All three act on the Elsdocia in the main hand of the
 * command's player. {@code reset} wipes the lineage and the learned magics (a fresh sword at 400); {@code owner} makes the player the owner whatever the
 * no-return rule says (a new distinct Grand Magic rank player still adds +25); {@code lineage} lists the owners, the damage and the learned magics.
 * Attached by {@code RelicCommands}' sibling call in MultiverseCommands; Brigadier merges it under the existing "relic" word.
 */
public final class ElsdociaCommand {
    private ElsdociaCommand() {}

    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("relic").then(Commands.literal("elsdocia").requires(s -> s.hasPermission(2))
                .then(Commands.literal("reset").executes(ctx -> reset(ctx.getSource())))
                .then(Commands.literal("lineage").executes(ctx -> lineage(ctx.getSource())))
                .then(Commands.literal("owner").then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> owner(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))));
    }

    private static ItemStack held(CommandSourceStack src) {
        try {
            ServerPlayer p = src.getPlayerOrException();
            ItemStack s = p.getMainHandItem();
            if (ElsdociaRules.isLegacyException(s)) return s;
            s = p.getOffhandItem();
            if (ElsdociaRules.isLegacyException(s)) return s;
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ignored) { /* console */ }
        src.sendFailure(Component.literal("Hold Elsdocia in your hand."));
        return ItemStack.EMPTY;
    }

    private static int reset(CommandSourceStack src) {
        ItemStack s = held(src);
        if (s.isEmpty()) return 0;
        ElsdociaComponents.set(s, ElsdociaLineage.EMPTY);
        src.sendSuccess(() -> Component.literal("Elsdocia's lineage is wiped: base damage " + ElsdociaRules.BASE_DAMAGE + ", no owner, no learned magic."), true);
        return 1;
    }

    private static int owner(CommandSourceStack src, ServerPlayer target) {
        ItemStack s = held(src);
        if (s.isEmpty()) return 0;
        ElsdociaLineage l = ElsdociaComponents.get(s).force(target.getUUID(), target.getGameProfile().getName(), ElsdociaEvents.magicOf(target), MultiverseProfile.step(target));
        ElsdociaComponents.set(s, l);
        src.sendSuccess(() -> Component.literal("Elsdocia now answers to " + target.getGameProfile().getName() + " (base damage " + l.damage() + ")."), true);
        return 1;
    }

    private static int lineage(CommandSourceStack src) {
        ItemStack s = held(src);
        if (s.isEmpty()) return 0;
        ElsdociaLineage l = ElsdociaComponents.get(s);
        StringBuilder b = new StringBuilder("Elsdocia: base damage " + l.damage() + ", " + l.owners().size() + " owner(s), " + l.learned().size() + " magic(s) learned.");
        int n = 1;
        for (ElsdociaLineage.Owner o : l.owners())
            b.append("\n ").append(n++).append(". ").append(o.name()).append(" [").append(o.magic().isEmpty() ? "no magic" : o.magic()).append(", rank step ").append(o.rank())
                    .append(o.bonus() ? ", +" + ElsdociaRules.GRAND_STEP_DAMAGE : "").append(']');
        if (!l.learned().isEmpty()) b.append("\n Learned: ").append(String.join(", ", l.learned()));
        String text = b.toString();
        src.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
