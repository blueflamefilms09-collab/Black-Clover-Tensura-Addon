package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Why: two small command trees. The ADMIN tree {@code /multiverse mega puzzle ...} (permission 2, attached under {@code mega} by the lead like every builder's branch): {@code list} (the puzzles and
 * their weights), {@code info <id>} (name and hint), {@code apply <id> here} (arm one where the sender stands, refused in a safe room or where it does not fit), {@code status} (the live ones),
 * {@code plan} (what the planner chose for the sender's sector), {@code clear}, {@code reset} (let cleared and given-up puzzles start again) and {@code verify} (the run-time check). The PLAYER
 * command {@code /nusmp_puzzle pick <puzzle number> <question number> <answer>} (permission 0) is what the answer buttons in chat run: it names the puzzle and the question, so a button of
 * an old question can never answer a new one, and a player who is not inside the puzzle's room is refused. All text goes through lang keys with English fallbacks.
 */
public final class PuzzleCommand {
    private PuzzleCommand() {}

    private static MutableComponent text(String key, Object... args) { return Component.translatableWithFallback(PuzzleText.langKey("cmd." + key), PuzzleText.en("cmd." + key), args); }

    private static int ok(CommandSourceStack s, MutableComponent c) {
        s.sendSuccess(() -> c, false);
        return 1;
    }

    private static int fail(CommandSourceStack s, MutableComponent c) {
        s.sendFailure(c);
        return 0;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> admin() {
        return Commands.literal("puzzle").requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("info").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(PuzzleCatalog.ids(), b))
                        .executes(ctx -> info(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("apply").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(PuzzleCatalog.ids(), b))
                        .then(Commands.literal("here").executes(ctx -> apply(ctx.getSource(), StringArgumentType.getString(ctx, "id"), 0L)))))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("plan").executes(ctx -> plan(ctx.getSource())))
                .then(Commands.literal("clear").executes(ctx -> ok(ctx.getSource(), text("clear", PuzzleDirector.clearAll()))))
                .then(Commands.literal("reset").executes(ctx -> {
                    PuzzleDirector.resetSuppression();
                    return ok(ctx.getSource(), text("reset"));
                }))
                .then(Commands.literal("verify").executes(ctx -> verify(ctx.getSource())));
    }

    /** The command the answer buttons run. */
    public static LiteralArgumentBuilder<CommandSourceStack> player() {
        return Commands.literal("nusmp_puzzle").requires(s -> s.getEntity() instanceof ServerPlayer)
                .then(Commands.literal("pick").then(Commands.argument("puzzle", IntegerArgumentType.integer(0)).then(Commands.argument("question", IntegerArgumentType.integer(0))
                        .then(Commands.argument("answer", IntegerArgumentType.integer(0, 9)).executes(ctx -> pick(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "puzzle"),
                                IntegerArgumentType.getInteger(ctx, "question"), IntegerArgumentType.getInteger(ctx, "answer")))))));
    }

    private static int pick(CommandSourceStack src, int puzzle, int question, int answer) {
        if (!(src.getEntity() instanceof ServerPlayer p)) return 0;
        PuzzleInstance in = PuzzleDirector.byId(puzzle);
        if (in == null || in.state != PuzzleInstance.State.ARMED || p.level() != in.level || !in.area.contains(p)) {
            p.displayClientMessage(Component.translatableWithFallback(PuzzleText.langKey("common.pickNone"), PuzzleText.en("common.pickNone")), true);
            return 0;
        }
        String why = in.choose(p, question, answer, p.level().getGameTime());
        if (why != null) {
            p.displayClientMessage(Component.translatableWithFallback(PuzzleText.langKey("common." + (why.equals("stale") ? "stale" : "pickNone")), PuzzleText.en("common." + (why.equals("stale") ? "stale" : "pickNone"))), true);
            return 0;
        }
        return 1;
    }

    private static int list(CommandSourceStack src) {
        PuzzleWeights w = PuzzleWeights.get();
        ok(src, text("list.head", PuzzleCatalog.ids().size(), w.maxPerFloor, w.enabled ? "on" : "off"));
        for (PuzzleCatalog.Entry e : PuzzleCatalog.all()) {
            PuzzleWeights.TypeW t = w.types.get(e.id());
            ok(src, text("list.line", e.id(), e.family(), t == null ? "-" : t.weight(), t == null ? "-" : t.tierLo(), t == null ? "-" : t.tierHi()));
        }
        return PuzzleCatalog.ids().size();
    }

    private static int info(CommandSourceStack src, String id) {
        if (!PuzzleCatalog.has(id)) return fail(src, text("nosuch", id));
        ok(src, Component.translatableWithFallback(PuzzleText.langKey(id + ".name"), PuzzleText.en(id + ".name")));
        ok(src, Component.translatableWithFallback(PuzzleText.langKey(id + ".hint"), PuzzleText.en(id + ".hint")));
        PuzzleRule r = PuzzleCatalog.create(id, new Params(1, 1, 1));
        return ok(src, text("info", PuzzleCatalog.family(id), r.layout().total()));
    }

    private static int apply(CommandSourceStack src, String id, long seed) {
        if (!(src.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return fail(src, text("player"));
        String[] why = {""};
        PuzzleInstance in = PuzzleDirector.applyAt(level, id, p.blockPosition(), seed == 0L ? level.getRandom().nextLong() : seed, List.of(p), level.getGameTime(), why);
        if (in == null) return fail(src, text("apply.no", why[0]));
        return ok(src, text("apply.ok", id, in.id));
    }

    private static int status(CommandSourceStack src) {
        List<PuzzleInstance> live = PuzzleDirector.active();
        ok(src, text("status.head", live.size()));
        for (PuzzleInstance in : live) ok(src, Component.literal("  " + in.describe()));
        return live.size();
    }

    private static int plan(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return fail(src, text("player"));
        Sector s = MegaDungeon.sectorAt(level, p.blockPosition());
        if (s == null) return fail(src, text("plan.none"));
        List<PuzzlePlanner.Slot> slots = PuzzleDirector.planFor(s);
        ok(src, text("plan.head", s.id, s.theme.id(), slots.size()));
        for (PuzzlePlanner.Slot slot : slots) ok(src, Component.literal("  room " + slot.roomId() + ": " + slot.type()));
        return slots.size();
    }

    private static int verify(CommandSourceStack src) {
        List<String> bad = PuzzleVerify.run();
        if (bad.isEmpty()) return ok(src, text("verify.ok", PuzzleCatalog.ids().size()));
        for (String b : bad) fail(src, Component.literal(b));
        return 0;
    }
}
