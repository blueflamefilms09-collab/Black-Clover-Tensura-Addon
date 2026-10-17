package com.newuniverse.nusmp.arc.world;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRole;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.arena.ArcArena;
import com.newuniverse.nusmp.arc.arena.ArcArenaBuilder;
import com.newuniverse.nusmp.arc.arena.ArcArenaDirector;
import com.newuniverse.nusmp.arc.arena.ArcArenaSave;
import com.newuniverse.nusmp.arc.arena.ArcArenaTravel;
import com.newuniverse.nusmp.arc.arena.ArenaGrid;
import com.newuniverse.nusmp.arc.arena.ArenaShape;
import com.newuniverse.nusmp.arc.arena.ArenaShapes;
import com.newuniverse.nusmp.arc.arena.ChronicleLectern;
import com.newuniverse.nusmp.arc.hub.ArcHubNpcs;
import com.newuniverse.nusmp.arc.hub.ChronicleBuilder;
import com.newuniverse.nusmp.arc.hub.ChronicleSave;
import com.newuniverse.nusmp.arc.hub.ChronicleSite;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: the places where the characters appear (the story arenas, the Chronicle, the Hub's friends, the roster file the advancement generator reads) need
 * commands the owner and the testers can use without playing the whole story, and they must be added WITHOUT editing the core's command file. This class returns
 * a second {@code arc} node; the lead attaches it next to the core's ({@code root.then(ArcWorldCommand.node())}), and Brigadier merges two literals of the same name
 * under one parent, so everything below appears beside {@code list}, {@code info}, {@code summon} and {@code story}:
 * <ul>
 *   <li>{@code dump} (permission 2): writes the roster as {@code id|name|saga|role} lines for {@code tools/quests/spec_arc.py};</li>
 *   <li>{@code chronicle list|page <n>|pick <id>} (anyone): the lectern's story pages and the choice of the boss the Chronicle Sigil carries you to;</li>
 *   <li>{@code arena leave} (anyone), {@code arena enter <id>}, {@code status}, {@code repair <id>}, {@code reset <id>}, {@code edit <on|off>} (permission 2);</li>
 *   <li>{@code hub status|build|repair|npcs|clear} (permission 2): the Promenade and the Chronicle in the Hub and the characters who stand on it.</li>
 * </ul>
 */
public final class ArcWorldCommand {
    private ArcWorldCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("arc")
                .then(Commands.literal("dump").requires(s -> s.hasPermission(2)).executes(ArcWorldCommand::dump))
                .then(Commands.literal("chronicle")
                        .then(Commands.literal("list").executes(ctx -> page(ctx, -1)))
                        .then(Commands.literal("page").then(Commands.argument("n", IntegerArgumentType.integer(0, 64)).executes(ctx -> page(ctx, IntegerArgumentType.getInteger(ctx, "n")))))
                        .then(Commands.literal("pick").then(Commands.argument("id", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(ArcRole.BOSS), b)).executes(ArcWorldCommand::pick))))
                .then(Commands.literal("arena")
                        .then(Commands.literal("leave").executes(ArcWorldCommand::leave))
                        .then(Commands.literal("status").requires(s -> s.hasPermission(2)).executes(ArcWorldCommand::arenaStatus))
                        .then(Commands.literal("enter").requires(s -> s.hasPermission(2)).then(Commands.argument("id", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(ArcRole.BOSS), b)).executes(ArcWorldCommand::enter)))
                        .then(Commands.literal("repair").requires(s -> s.hasPermission(2)).then(Commands.argument("id", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(ArcRole.BOSS), b)).executes(ArcWorldCommand::repair)))
                        .then(Commands.literal("reset").requires(s -> s.hasPermission(2)).then(Commands.argument("id", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(ids(ArcRole.BOSS), b)).executes(ArcWorldCommand::reset)))
                        .then(Commands.literal("edit").requires(s -> s.hasPermission(2)).then(Commands.argument("on", BoolArgumentType.bool()).executes(ArcWorldCommand::edit))))
                .then(Commands.literal("hub").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("status").executes(ArcWorldCommand::hubStatus))
                        .then(Commands.literal("build").executes(ctx -> hubBuild(ctx, false)))
                        .then(Commands.literal("repair").executes(ctx -> hubBuild(ctx, true)))
                        .then(Commands.literal("npcs").executes(ArcWorldCommand::hubNpcs))
                        .then(Commands.literal("clear").executes(ArcWorldCommand::hubClear)));
    }

    // ================================================================ helpers
    private static List<String> ids(ArcRole role) {
        List<String> out = new ArrayList<>();
        for (ArcCharacter c : ArcRoster.all()) if (role == null || c.role() == role) out.add(c.id());
        return out;
    }

    private static int say(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendFailure(Component.literal(text));
        return 0;
    }

    private static ArcCharacter boss(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        ArcCharacter c = ArcRoster.byId(id);
        if (c == null || !c.isBoss()) {
            fail(ctx, "No boss with the id '" + id + "' is registered.");
            return null;
        }
        return c;
    }

    // ================================================================ dump
    private static int dump(CommandContext<CommandSourceStack> ctx) {
        String text = RosterDump.text(ArcRoster.all());
        List<Path> wrote = new ArrayList<>();
        try {
            Path game = FMLPaths.GAMEDIR.get().resolve("arc_roster.txt");
            Files.writeString(game, text, StandardCharsets.UTF_8);
            wrote.add(game);
            for (Path repo : new Path[] {Path.of("tools", "quests"), Path.of("..", "tools", "quests")}) {
                if (Files.isDirectory(repo)) {
                    Path file = repo.resolve("arc_roster.txt");
                    Files.writeString(file, text, StandardCharsets.UTF_8);
                    wrote.add(file.toAbsolutePath().normalize());
                    break;
                }
            }
        } catch (IOException e) {
            return fail(ctx, "The roster could not be written: " + e.getMessage());
        }
        int n = RosterDump.lines(ArcRoster.all()).size();
        say(ctx, n + " characters written to " + wrote + ". Copy the file to tools/quests/arc_roster.txt and run tools/quests/gen_arc_adv.py.");
        return 1;
    }

    // ================================================================ the Chronicle
    private static int page(CommandContext<CommandSourceStack> ctx, int n) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return fail(ctx, "Only a player can read the Chronicle.");
        if (n < 0) ChronicleLectern.show(p, ChronicleLectern.pageOf(p, com.newuniverse.nusmp.arc.story.ArcStory.sagaOf(p)));
        else ChronicleLectern.show(p, n);
        return 1;
    }

    private static int pick(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return fail(ctx, "Only a player can choose.");
        return ChronicleLectern.choose(p, StringArgumentType.getString(ctx, "id")) != null ? 1 : 0;
    }

    // ================================================================ the arenas
    private static int leave(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return fail(ctx, "Only a player can leave an arena.");
        if (ArcArenaDirector.fightRunningAt(p.getServer(), p)) return fail(ctx, "The fight is underway. Hold the Chronicle Sigil for ten seconds to leave.");
        return ArcArenaTravel.leave(p) ? 1 : fail(ctx, "You are not in an arena.");
    }

    private static int enter(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return fail(ctx, "Only a player can be carried.");
        ArcCharacter c = boss(ctx);
        if (c == null) return 0;
        return ArcArenaTravel.enter(p, List.of(), c) ? 1 : 0;
    }

    private static int arenaStatus(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        ArcArenaSave save = ArcArenaSave.get(server);
        say(ctx, "Story arenas (dimension " + ArcArena.DIMENSION.location() + "), " + save.slots().size() + " of " + ArenaGrid.MAX_SLOTS + " sites in use, editing " + (ArcArena.editing() ? "ON" : "off"));
        for (Map.Entry<String, Integer> e : save.slots().entrySet()) {
            ArcCharacter c = ArcRoster.byId(e.getKey());
            ArenaShape shape = ArenaShapes.forCharacter(c);
            String phase = save.hasState(e.getValue()) ? save.state(e.getValue()).phase().name() : "READY";
            say(ctx, " site " + e.getValue() + " " + e.getKey() + ": " + shape.id() + ", build " + save.builtVersion(e.getValue()) + "/" + ArcArenaSave.BUILD_VERSION
                    + ", " + phase + (save.bossId(e.getValue()) != null ? ", boss " + save.bossId(e.getValue()) : "") + (c == null ? " (no longer in the roster)" : ""));
        }
        return 1;
    }

    private static int repair(CommandContext<CommandSourceStack> ctx) {
        ArcCharacter c = boss(ctx);
        if (c == null) return 0;
        MinecraftServer server = ctx.getSource().getServer();
        ServerLevel arena = server.getLevel(ArcArena.DIMENSION);
        if (arena == null) return fail(ctx, "The arena dimension is not loaded.");
        if (ArenaShapes.usesFinalHour(c)) return fail(ctx, c.name() + " fights in the Final Hour's courtyard: use /multiverse arena repair.");
        ArcArenaSave save = ArcArenaSave.get(server);
        int slot = save.slotFor(c.id());
        if (slot < 0) return fail(ctx, "Every site is taken.");
        ArenaShape shape = ArenaShapes.forCharacter(c);
        int n = ArcArenaBuilder.ensureBuilt(arena, save, shape, slot) ? -1 : ArcArenaBuilder.repair(arena, shape, slot);
        return say(ctx, n < 0 ? shape.id() + " was built for " + c.id() + " (site " + slot + ")." : shape.id() + " repaired for " + c.id() + " (site " + slot + "): " + n + " blocks set.");
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        ArcCharacter c = boss(ctx);
        if (c == null) return 0;
        MinecraftServer server = ctx.getSource().getServer();
        int slot = ArcArenaSave.get(server).slotOf(c.id());
        if (slot < 0) return fail(ctx, c.name() + " has no arena site yet.");
        ArcArenaDirector.reset(server, slot);
        return say(ctx, "The arena of " + c.name() + " (site " + slot + ") is reset: no boss, ready.");
    }

    private static int edit(CommandContext<CommandSourceStack> ctx) {
        boolean on = BoolArgumentType.getBool(ctx, "on");
        ArcArena.setEditing(on);
        return say(ctx, on ? "Editing is ON: the guard lets blocks change in the story arenas until the server stops or you turn it off. Run 'arena repair <id>' to put an arena back." : "Editing is off.");
    }

    // ================================================================ the Hub
    private static ServerLevel hub(CommandContext<CommandSourceStack> ctx) {
        ServerLevel hub = ctx.getSource().getServer().getLevel(Labyrinth.SANCTUARY_DIMENSION);
        if (hub == null) fail(ctx, "The Sanctuary dimension is not loaded.");
        return hub;
    }

    private static int hubStatus(CommandContext<CommandSourceStack> ctx) {
        ServerLevel hub = hub(ctx);
        if (hub == null) return 0;
        int npcs = 0;
        for (List<?> l : ArcHubNpcs.standing(hub).values()) npcs += l.size();
        say(ctx, "Chronicle site: build " + ChronicleSave.get(hub).version() + "/" + ChronicleBuilder.BUILD_VERSION + ", chunks " + (ChronicleBuilder.chunksLoaded(hub) ? "loaded" : "NOT all loaded")
                + ", " + ChronicleSite.posts().size() + " posts, " + npcs + " characters standing, " + ArcRoster.npcs().size() + " friendly characters in the roster, switches: chronicle "
                + ArcWorldConfig.chronicle() + ", hubNpcs " + ArcWorldConfig.hubNpcs() + ", always " + ArcWorldConfig.hubNpcsAlways());
        return 1;
    }

    private static int hubBuild(CommandContext<CommandSourceStack> ctx, boolean onlyMissing) {
        ServerLevel hub = hub(ctx);
        if (hub == null) return 0;
        int r = ChronicleSite.MAX_RADIUS + 1;
        for (int cx = (-r) >> 4; cx <= r >> 4; cx++) for (int cz = (-r) >> 4; cz <= r >> 4; cz++) hub.getChunk(cx, cz);       // an operator's command may load the chunks it needs
        int n = ChronicleBuilder.build(hub, onlyMissing);
        if (!onlyMissing) ChronicleSave.get(hub).version(ChronicleBuilder.BUILD_VERSION);
        return say(ctx, "The Chronicle and the Promenade were " + (onlyMissing ? "repaired" : "laid") + ": " + n + " blocks set.");
    }

    private static int hubNpcs(CommandContext<CommandSourceStack> ctx) {
        ServerLevel hub = hub(ctx);
        if (hub == null) return 0;
        ArcHubNpcs.Report r = ArcHubNpcs.pass(hub, ctx.getSource().getServer());
        return say(ctx, "Hub pass: " + r.spawned() + " placed, " + r.removed() + " removed, " + r.deduped() + " duplicates removed, " + r.kept() + " kept, " + r.unseen() + " not loaded"
                + (r.unplaced().isEmpty() ? "" : "; no post for " + r.unplaced()));
    }

    private static int hubClear(CommandContext<CommandSourceStack> ctx) {
        ServerLevel hub = hub(ctx);
        if (hub == null) return 0;
        return say(ctx, ArcHubNpcs.clear(hub) + " characters placed by the Hub pass were removed (the next pass places them again unless arc.hubNpcs is off).");
    }
}
