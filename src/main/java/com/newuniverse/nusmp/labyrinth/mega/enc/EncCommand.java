package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

/**
 * Why: the Dungeon Master is a dice-driven system that nobody can playtest by waiting, so the admin needs to see and push it:
 * {@code /multiverse mega dm status} shows the party, the noise, the light, the modifiers, the cooldown, the last roll and the live wandering
 * bosses; {@code roll [band]} rolls for the party now (ignoring the cooldown, the crowd cap and the safe-zone filter; with a band the dice are
 * skipped and that size of trouble is spawned); {@code spawn <entry>} spawns one entry of the sector's table; {@code wanderer} brings a
 * wandering boss into the sector; {@code clear} removes every monster of the Director and every wandering boss of the dimension. The tree is
 * attached by the lead under {@code /multiverse mega}; the permission is 2. Messages are literals, like the other admin commands.
 */
public final class EncCommand {
    private EncCommand() {}

    private static final List<String> BANDS = List.of("minor", "standard", "hard", "ambush");
    private static final SimpleCommandExceptionType NOT_HERE = new SimpleCommandExceptionType(Component.literal("You are not in the Mega-Dungeon."));

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("dm").requires(s -> s.hasPermission(2))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("roll").executes(ctx -> roll(ctx.getSource(), null))
                        .then(Commands.argument("band", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(BANDS, b))
                                .executes(ctx -> roll(ctx.getSource(), StringArgumentType.getString(ctx, "band")))))
                .then(Commands.literal("spawn").then(Commands.argument("entry", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(entryIds(), b))
                        .executes(ctx -> spawn(ctx.getSource(), StringArgumentType.getString(ctx, "entry")))))
                .then(Commands.literal("wanderer").executes(ctx -> wanderer(ctx.getSource())))
                .then(Commands.literal("clear").executes(ctx -> clear(ctx.getSource())));
    }

    private static int ok(CommandSourceStack s, String msg) {
        s.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int fail(CommandSourceStack s, String msg) {
        s.sendFailure(Component.literal(msg));
        return 0;
    }

    private static List<String> entryIds() {
        TreeSet<String> ids = new TreeSet<>();
        for (String name : EncData.tableNames()) {
            EncounterTable t = EncData.byName(name);
            if (t != null) for (EncounterTable.Entry e : t.entries()) ids.add(e.id());
        }
        return new ArrayList<>(ids);
    }

    /** The player who ran the command, which must stand in the Mega-Dungeon. */
    private static ServerPlayer here(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (!Labyrinth.inMega(p.level())) throw NOT_HERE.create();
        return p;
    }

    /** The party of the player: everyone in the dimension who is connected to them by a chain of players within the party radius (the player counts even in creative). */
    private static List<ServerPlayer> party(ServerLevel level, ServerPlayer who) {
        List<PartyMath.Member> members = new ArrayList<>();
        boolean self = false;
        for (ServerPlayer p : level.players()) {
            if (p == who) self = true;
            else if (!p.isAlive() || p.isSpectator() || p.isCreative()) continue;
            members.add(new PartyMath.Member(p.getUUID(), p.getX(), p.getY(), p.getZ()));
        }
        if (!self) members.add(new PartyMath.Member(who.getUUID(), who.getX(), who.getY(), who.getZ()));
        List<ServerPlayer> out = new ArrayList<>();
        for (PartyMath.Party party : PartyMath.group(members, EncConfig.get(EncConfig.PARTY_RADIUS))) {
            if (!party.has(who.getUUID())) continue;
            for (PartyMath.Member m : party.members()) {
                if (m.id().equals(who.getUUID())) out.add(who);
                else if (level.getPlayerByUUID(m.id()) instanceof ServerPlayer sp) out.add(sp);
            }
        }
        if (out.isEmpty()) out.add(who);
        return out;
    }

    private static int status(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = here(src);
        ServerLevel level = p.serverLevel();
        for (String line : EncounterDirector.status(level, p, party(level, p))) ok(src, line);
        for (String line : WandererDirector.status(level)) ok(src, line);
        return 1;
    }

    private static int roll(CommandSourceStack src, String band) throws CommandSyntaxException {
        ServerPlayer p = here(src);
        ServerLevel level = p.serverLevel();
        EncounterDirector.Force force = EncounterDirector.Force.admin();
        if (band != null) {
            Band b = Band.byName(band);
            if (b == null || b == Band.NOTHING) return fail(src, "Bands: " + String.join(", ", BANDS) + ".");
            force = EncounterDirector.Force.band(b);
        }
        return report(src, EncounterDirector.roll(level, party(level, p), force));
    }

    private static int spawn(CommandSourceStack src, String entry) throws CommandSyntaxException {
        ServerPlayer p = here(src);
        ServerLevel level = p.serverLevel();
        return report(src, EncounterDirector.roll(level, party(level, p), EncounterDirector.Force.entry(entry)));
    }

    private static int report(CommandSourceStack src, EncounterDirector.Outcome o) {
        return o.spawned() || o.kind() == EncounterDirector.Kind.QUIET ? ok(src, o.text()) : fail(src, o.text());
    }

    private static int wanderer(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = here(src);
        ServerLevel level = p.serverLevel();
        Sector sector = MegaDungeon.sectorAt(level, p.blockPosition());
        if (sector == null) return fail(src, "You are not inside a sector.");
        WandererDirector.Spawned s = WandererDirector.spawn(level, sector, party(level, p), new Random(level.getRandom().nextLong()));
        return s.mob() != null ? ok(src, s.text()) : fail(src, s.text());
    }

    private static int clear(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = here(src);
        ServerLevel level = p.serverLevel();
        int monsters = EncounterDirector.clear(level), bosses = WandererDirector.clear(level);
        return ok(src, "Removed " + monsters + " monsters of the Dungeon Master and " + bosses + " wandering bosses.");
    }
}
