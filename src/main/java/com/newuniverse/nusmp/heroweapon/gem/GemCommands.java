package com.newuniverse.nusmp.heroweapon.gem;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: the acquisition system's part of {@code /multiverse hero ...}: the admin routes to a gem (the survival route is the recipes, and an operator who tests or
 * rewards a player must not have to craft a nether star's worth of materials). {@code gem give <player> holy} hands over the Holy Calling Gem,
 * {@code gem give <player> seal <weapon>} the Vassal Seal of one of the seventeen Seven Star and Vassal Weapons, {@code gem give <player> fragment [count]} the
 * Gem Fragments the Seals are crafted from, and {@code gem holders} lists who holds which weapon. Binding and releasing a weapon directly are the core's own
 * commands ({@code bind}, {@code release}). Permission level 2 for all of them. The code only parses and reports: what a gem does is {@link GemChoosing}'s.
 */
public final class GemCommands {
    private GemCommands() {}

    private static final SuggestionProvider<CommandSourceStack> SEAL_WEAPONS = (ctx, builder) -> {
        List<String> words = new ArrayList<>();
        for (HeroKind k : GemCatalog.sealKinds()) words.add(k.name().toLowerCase(Locale.ROOT));
        return SharedSuggestionProvider.suggest(words, builder);
    };

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("gem")
                .requires(s -> s.hasPermission(2))
                .executes(ctx -> usage(ctx.getSource()))
                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("holy").executes(ctx -> give(ctx, GemItems.holyStack(), GemMessages.en(GemMessages.ITEM_HOLY))))
                                .then(Commands.literal("seal")
                                        .then(Commands.argument("weapon", StringArgumentType.word()).suggests(SEAL_WEAPONS).executes(GemCommands::giveSeal)))
                                .then(Commands.literal("fragment")
                                        .executes(ctx -> give(ctx, GemItems.fragmentStack(1), GemMessages.en(GemMessages.ITEM_FRAGMENT)))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> give(ctx, GemItems.fragmentStack(IntegerArgumentType.getInteger(ctx, "count")), GemMessages.en(GemMessages.ITEM_FRAGMENT)))))))
                .then(Commands.literal("holders").executes(ctx -> holders(ctx.getSource())));
    }

    private static int usage(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("hero gem: give <player> holy | seal <weapon> | fragment [count], holders").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int giveSeal(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String word = StringArgumentType.getString(ctx, "weapon");
        HeroKind kind = HeroKind.parse(word);
        if (kind == null) {
            ctx.getSource().sendFailure(GemText.msg(GemMessages.UNKNOWN_WEAPON, word));
            return 0;
        }
        if (!GemCatalog.hasSeal(kind)) {
            ctx.getSource().sendFailure(GemText.msg(GemMessages.NO_SEAL, kind.display()));
            return 0;
        }
        return give(ctx, GemItems.sealStack(kind), GemCatalog.sealName(kind));
    }

    /** Puts the stack in the player's inventory, or drops it at their feet when the inventory is full (as {@code /give} does). */
    private static int give(CommandContext<CommandSourceStack> ctx, ItemStack stack, String what) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        if (!target.getInventory().add(stack) && !stack.isEmpty()) target.drop(stack, false);        // what did not fit (add leaves the remainder in the stack)
        ctx.getSource().sendSuccess(() -> GemText.msg(GemMessages.GAVE, target.getGameProfile().getName(), what), true);
        return 1;
    }

    private static int holders(CommandSourceStack src) {
        List<GemHolders.Holder> all = new ArrayList<>(GemHolders.online(src.getServer()));
        all.addAll(GemHolders.offline(src.getServer()));
        if (all.isEmpty()) {
            src.sendSuccess(() -> GemText.msg(GemMessages.HOLDERS_NONE), false);
            return 0;
        }
        src.sendSuccess(() -> GemText.msg(GemMessages.HOLDERS_HEAD, String.valueOf(all.size())).withStyle(ChatFormatting.GOLD), false);
        for (GemHolders.Holder h : all) {
            String line = h.online()
                    ? "  " + h.name() + ": " + h.kind().display() + ", level " + h.level() + ", form " + h.form()
                    : "  " + h.name() + " (offline): " + h.kind().display();
            src.sendSuccess(() -> Component.literal(line).withStyle(h.online() ? ChatFormatting.WHITE : ChatFormatting.GRAY), false);
        }
        return all.size();
    }
}
