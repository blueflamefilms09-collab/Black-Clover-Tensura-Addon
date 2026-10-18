package com.newuniverse.nusmp.arc;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.arc.dialogue.DialogueLoader;
import com.newuniverse.nusmp.arc.story.ArcStory;
import com.newuniverse.nusmp.arc.story.StoryData;
import com.newuniverse.nusmp.charter.CharterTable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: the owner and the testers need to see, call and reset the characters of the arcs without playing the whole story: list who exists, read one
 * character's record, summon a boss for a test fight, place an NPC, and read or edit a player's story. {@link #node()} is the {@code arc} node the lead
 * attaches under {@code /multiverse}:
 * <ul>
 *   <li>{@code list [saga]}: the roster (anyone); {@code info <id>}: one character's record, its entity, grimoire, dialogue file and gaps (anyone);</li>
 *   <li>{@code summon <id> [pos]} (permission 2): a boss at the position (default: where you stand), ignoring the story gate;</li>
 *   <li>{@code spawn_npc <id> [pos]} (permission 2): a friendly character with its post there;</li>
 *   <li>{@code story}: your own story (anyone); {@code story <player> [list|unlock <id>|reset|quest <npc> <stage>]} (permission 2).</li>
 * </ul>
 */
public final class ArcCommand {
    private ArcCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("arc")
                .then(Commands.literal("list").executes(ctx -> list(ctx, null))
                        .then(Commands.argument("saga", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(sagaIds(), b))
                                .executes(ctx -> list(ctx, StringArgumentType.getString(ctx, "saga")))))
                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(ids(null), b))
                                .executes(ArcCommand::info)))
                .then(Commands.literal("summon").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(ids(ArcRole.BOSS), b))
                                .executes(ctx -> summon(ctx, null))
                                .then(Commands.argument("pos", Vec3Argument.vec3()).executes(ctx -> summon(ctx, Vec3Argument.getVec3(ctx, "pos"))))))
                .then(Commands.literal("spawn_npc").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(ids(ArcRole.NPC), b))
                                .executes(ctx -> spawnNpc(ctx, null))
                                .then(Commands.argument("pos", Vec3Argument.vec3()).executes(ctx -> spawnNpc(ctx, Vec3Argument.getVec3(ctx, "pos"))))))
                .then(Commands.literal("story").executes(ArcCommand::storyOwn)
                        .then(Commands.argument("player", EntityArgument.player()).requires(s -> s.hasPermission(2))
                                .executes(ctx -> storyList(ctx, EntityArgument.getPlayer(ctx, "player")))
                                .then(Commands.literal("list").executes(ctx -> storyList(ctx, EntityArgument.getPlayer(ctx, "player"))))
                                .then(Commands.literal("reset").executes(ArcCommand::storyReset))
                                .then(Commands.literal("unlock")
                                        .then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(ids(null), b))
                                                .executes(ArcCommand::storyUnlock)))
                                .then(Commands.literal("quest")
                                        .then(Commands.argument("npc", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(ids(ArcRole.NPC), b))
                                                .then(Commands.argument("stage", IntegerArgumentType.integer(0, StoryData.QUEST_MAX)).executes(ArcCommand::storyQuest))))));
    }

    // ================================================================ helpers
    private static List<String> ids(ArcRole role) {
        List<String> out = new ArrayList<>();
        for (ArcCharacter c : ArcRoster.all()) if (role == null || c.role() == role) out.add(c.id());
        return out;
    }

    private static List<String> sagaIds() {
        List<String> out = new ArrayList<>();
        for (ArcSaga s : ArcSaga.values()) out.add(s.id());
        return out;
    }

    private static void say(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
    }

    private static void say(CommandContext<CommandSourceStack> ctx, Component text) {
        ctx.getSource().sendSuccess(() -> text, false);
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendFailure(Component.literal(text));
        return 0;
    }

    private static ArcCharacter find(CommandContext<CommandSourceStack> ctx, String arg) {
        ArcCharacter c = ArcRoster.byId(StringArgumentType.getString(ctx, arg));
        if (c == null) fail(ctx, "No such character '" + StringArgumentType.getString(ctx, arg) + "'. Known: " + (ArcRoster.all().isEmpty() ? "(none registered yet)" : String.join(", ", ids(null))));
        return c;
    }

    // ================================================================ list / info
    private static int list(CommandContext<CommandSourceStack> ctx, String sagaId) {
        ArcSaga only = null;
        if (sagaId != null) {
            for (ArcSaga s : ArcSaga.values()) if (s.id().equalsIgnoreCase(sagaId)) only = s;
            if (only == null) return fail(ctx, "No such saga '" + sagaId + "'. Known: " + String.join(", ", sagaIds()));
        }
        int n = 0;
        for (ArcSaga saga : ArcSaga.values()) {
            if (only != null && saga != only) continue;
            List<ArcCharacter> in = ArcRoster.ofSaga(saga);
            if (in.isEmpty()) continue;
            say(ctx, Component.literal(saga.title() + ":").withStyle(ChatFormatting.GOLD));
            for (ArcCharacter c : in) {
                say(ctx, " " + c.id() + "  " + c.fullName() + "  [" + c.role().name().toLowerCase(Locale.ROOT) + (c.isBoss() ? ", tier " + c.tier() : "") + "]");
                n++;
            }
        }
        if (n == 0) say(ctx, "No characters are registered" + (only == null ? " yet." : " in that saga."));
        return n;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) {
        ArcCharacter c = find(ctx, "id");
        if (c == null) return 0;
        say(ctx, Component.literal(c.fullName()).withStyle(ChatFormatting.GOLD));
        say(ctx, " id " + c.id() + ", " + c.role().name().toLowerCase(Locale.ROOT) + ", from " + (c.source().isEmpty() ? "-" : c.source()) + ", " + c.saga().title());
        if (c.isBoss()) {
            say(ctx, " tier " + c.tier() + ", health " + (int) c.health() + ", attack " + c.attack() + ", speed " + c.speed() + ", armor " + c.armor() + (c.magic().isEmpty() ? "" : ", magic " + c.magic()));
            say(ctx, " floors " + (c.floorMax() <= 0 ? "(never a floor boss)" : c.floorMin() + " to " + c.floorMax() + (c.road() == null ? " on either road" : " on the " + c.road().name().toLowerCase(Locale.ROOT) + " road"))
                    + (c.themes().isEmpty() ? "" : ", themes " + String.join(", ", c.themes())) + (c.arena().isEmpty() ? "" : ", arena " + c.arena()));
            say(ctx, " entity " + (com.newuniverse.nusmp.arc.world.ExistingBosses.owns(c.id()) ? "nusmp:" + com.newuniverse.nusmp.arc.world.ExistingBosses.entityPath(c.id()) + " (an older boss)"      // 1.61.0
                    : ArcSpawner.typeOf(c.id()) != null ? "nusmp:" + c.id() : "NOT REGISTERED"));
            CharterTable.Boss g = CharterTable.byId(c.charterId());
            say(ctx, " grimoire " + (!c.hasCharter() ? "none" : g == null ? "declared but not in the Canon table" : g.skills().size() + " skills, first-kill chance " + g.chance()));
        }
        say(ctx, " follows " + (c.prerequisites().isEmpty() ? "nobody (open from the start)" : String.join(", ", c.prerequisites())));
        List<String> problems = DialogueLoader.problems().get(c.id());
        say(ctx, " dialogue " + (c.isNpc() ? (DialogueLoader.has(c.id()) ? "file loaded" : "NO FILE (it says the generic lines)") : "(bosses speak the lines of their record)")
                + (problems == null ? "" : ", problems: " + String.join("; ", problems)));
        return 1;
    }

    // ================================================================ summon / spawn
    private static Vec3 spot(CommandContext<CommandSourceStack> ctx, Vec3 given) {
        return given != null ? given : com.newuniverse.nusmp.entity.zogratis.ZogratisBoss.ground(ctx.getSource().getLevel(), ctx.getSource().getPosition());
    }

    private static int summon(CommandContext<CommandSourceStack> ctx, Vec3 pos) {
        ArcCharacter c = find(ctx, "id");
        if (c == null) return 0;
        ServerLevel level = ctx.getSource().getLevel();
        ArcSpawner.Result r = ArcSpawner.summon(level, c, spot(ctx, pos), 0f, List.of(), true);
        switch (r.outcome()) {
            case SPAWNED -> {
                ctx.getSource().sendSuccess(() -> Component.literal(c.fullName() + " is summoned here. The arena is here."), true);
                return 1;
            }
            case DISABLED -> { return fail(ctx, "The characters of the arcs are switched off (arc.enabled)."); }
            case NOT_A_BOSS -> { return fail(ctx, c.name() + " is not a boss. Use /multiverse arc spawn_npc " + c.id() + "."); }
            case NO_ENTITY -> { return fail(ctx, c.name() + " has no entity type registered (the boss class is not loaded)."); }
            default -> { return fail(ctx, c.name() + " could not be summoned here."); }
        }
    }

    private static int spawnNpc(CommandContext<CommandSourceStack> ctx, Vec3 pos) {
        ArcCharacter c = find(ctx, "id");
        if (c == null) return 0;
        if (!c.isNpc()) return fail(ctx, c.name() + " is a boss. Use /multiverse arc summon " + c.id() + ".");
        ServerLevel level = ctx.getSource().getLevel();
        int already = ArcSpawner.npcs(level, c.id()).size();
        ArcNpcEntity npc = ArcSpawner.spawnNpc(level, c, spot(ctx, pos), 0f);
        if (npc == null) return fail(ctx, c.name() + " could not be placed here (is arc.enabled on?).");
        ctx.getSource().sendSuccess(() -> Component.literal(c.fullName() + " stands here; this is their post." + (already > 0 ? " (" + already + " more of them are already loaded in this dimension.)" : "")), true);
        return 1;
    }

    // ================================================================ story
    private static int storyOwn(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p == null) return fail(ctx, "Name a player: /multiverse arc story <player>.");
        return storyList(ctx, p);
    }

    private static int storyList(CommandContext<CommandSourceStack> ctx, ServerPlayer p) {
        StoryData d = ArcStory.data(p);
        say(ctx, Component.literal("Story of " + p.getGameProfile().getName() + ": stage " + ArcStory.stageOf(p) + ", " + ArcStory.sagaOf(p).title()).withStyle(ChatFormatting.GOLD));
        say(ctx, " defeated: " + (d.defeatedList().isEmpty() ? "(nobody)" : String.join(", ", d.defeatedList())));
        say(ctx, " met: " + (d.metList().isEmpty() ? "(nobody)" : String.join(", ", d.metList())));
        if (!d.questMap().isEmpty()) say(ctx, " quests: " + d.questMap());
        List<String> open = new ArrayList<>();
        for (ArcCharacter c : ArcStory.available(p)) open.add(c.id());
        say(ctx, " open to fight: " + (open.isEmpty() ? "(nobody)" : String.join(", ", open)));
        return 1;
    }

    private static int storyReset(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
            ArcStory.reset(p);
            ctx.getSource().sendSuccess(() -> Component.literal("The story of " + p.getGameProfile().getName() + " is reset."), true);
            return 1;
        } catch (CommandSyntaxException e) {
            return fail(ctx, "No such player.");
        }
    }

    private static int storyUnlock(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
            ArcCharacter c = find(ctx, "id");
            if (c == null) return 0;
            List<String> done = ArcStory.unlock(p, c);
            ctx.getSource().sendSuccess(() -> Component.literal(c.fullName() + " is open for " + p.getGameProfile().getName()
                    + (done.isEmpty() ? " (nothing new was credited)." : "; credited as defeated: " + String.join(", ", done) + ".")), true);
            return 1;
        } catch (CommandSyntaxException e) {
            return fail(ctx, "No such player.");
        }
    }

    private static int storyQuest(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
            ArcCharacter c = find(ctx, "npc");
            if (c == null) return 0;
            int stage = IntegerArgumentType.getInteger(ctx, "stage");
            ArcStory.setQuestStage(p, c.id(), stage);
            ctx.getSource().sendSuccess(() -> Component.literal("Quest stage of " + c.name() + " with " + p.getGameProfile().getName() + " is " + stage + "."), true);
            return 1;
        } catch (CommandSyntaxException e) {
            return fail(ctx, "No such player.");
        }
    }
}
