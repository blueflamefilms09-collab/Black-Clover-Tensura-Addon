package com.newuniverse.nusmp.dragonballs;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Tab completion of {@code /dragonwish <wish> <name>} while the overpowered mode is on: the wish ids, then the names of the live race, skill (at the ceiling of the summoner's dragon, namespaced ids when two
 * mods share a path), item, relic or rank list. Server side, answers only a player who has a running ritual, at most {@value #MAX} suggestions, no scan beyond one pass over a cached list.
 */
final class DbWishSuggest {
    private DbWishSuggest() {}

    static final int MAX = 100;

    static CompletableFuture<Suggestions> wishes(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        ServerPlayer p = c.getSource().getPlayer();
        DbSession s = p == null ? null : DbSession.byPlayer(p.getUUID());
        if (s == null) return b.buildFuture();
        return SharedSuggestionProvider.suggest(WishRules.forSet(s.set, DbConfig.overpowered()).stream().map(WishRules.Wish::id), b);
    }

    static CompletableFuture<Suggestions> words(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) {
        ServerPlayer p = c.getSource().getPlayer();
        DbSession s = p == null ? null : DbSession.byPlayer(p.getUUID());
        if (s == null || !DbConfig.overpowered()) return b.buildFuture();
        List<WishNames.Entry> list = switch (StringArgumentType.getString(c, "wish").toLowerCase(Locale.ROOT)) {
            case "race" -> WishCatalog.races();
            case "skill" -> WishCatalog.skillsFor(s.set);
            case "item" -> WishCatalog.items();
            case "weapon" -> WishCatalog.relics(DbConfig.genesisRelics());
            case "rank" -> WishCatalog.ranks();
            default -> List.of();
        };
        List<String> names = WishCatalog.commandNames(list);
        String typed = b.getRemaining().toLowerCase(Locale.ROOT);
        int n = 0;
        for (String name : names) {
            if (!SharedSuggestionProvider.matchesSubStr(typed, name.toLowerCase(Locale.ROOT))) continue;
            b.suggest(name);
            if (++n >= MAX) break;
        }
        return b.buildFuture();
    }
}
