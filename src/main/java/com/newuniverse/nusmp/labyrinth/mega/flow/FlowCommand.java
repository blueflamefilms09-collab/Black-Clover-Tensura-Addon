package com.newuniverse.nusmp.labyrinth.mega.flow;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.order.UniqueLedger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Why: the owner's hands on the Rukh flow, attached by the lead under {@code /multiverse mega} (permission 2). Nothing about the flow can be tried on a
 * quiet server without knowing what it believes, so the node answers the questions that come up first. {@code flow status [player]} says what flow the place
 * under the player has, what the player cast last, how long ago, and what a hit would be worth right now: the first thing to check when "my Light spell did
 * nothing" (usually: the hook is not wired, the player is in a gap between sectors, or the cast is older than the window). {@code flow classify <id>...} says
 * which alignment the table gives a cast and WHICH RULE decided (ids separated by spaces, the most specific first, such as {@code devil_union light}), the
 * way to tune the data file. {@code flow vessel list} lists the relics that can be handed out as the Metal Vessel and which of them the world already has,
 * and which sectors have paid; {@code flow vessel give <player>} runs the reward logic by hand for the sector the sender stands in (or none), skipping the
 * once-per-sector memory and the switch, for tests and for making good a lost vessel. All text goes through lang keys {@code message.nusmp.mega.mfw.cmd.*}
 * with an English fallback, so the command reads fine before the lang fragment is merged.
 */
public final class FlowCommand {
    private FlowCommand() {}

    private static final int LIST_SHOWN = 30;

    /** The {@code flow} node: {@code status [player] | classify <ids> | vessel list | vessel give <player>}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("flow").requires(s -> s.hasPermission(2))
                .then(Commands.literal("status")
                        .executes(ctx -> status(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> status(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("classify").then(Commands.argument("ids", StringArgumentType.greedyString())
                        .executes(ctx -> classify(ctx.getSource(), StringArgumentType.getString(ctx, "ids")))))
                .then(Commands.literal("vessel")
                        .then(Commands.literal("list").executes(ctx -> vesselList(ctx.getSource())))
                        .then(Commands.literal("give").then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> vesselGive(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))));
    }

    private static MutableComponent text(String key, String english, Object... args) {
        return Component.translatableWithFallback("message.nusmp.mega.mfw.cmd." + key, english, args);
    }

    private static int ok(CommandSourceStack s, MutableComponent c) {
        s.sendSuccess(() -> c, false);
        return 1;
    }

    private static int fail(CommandSourceStack s, MutableComponent c) {
        s.sendFailure(c);
        return 0;
    }

    private static String num(double d) { return String.format(Locale.ROOT, "%.2f", d).replaceAll("\\.?0+$", ""); }

    // ---------------------------------------------------------------- status
    private static int status(CommandSourceStack src, ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        boolean on = FlowConfig.get(FlowConfig.ENABLED);
        double mult = FlowRules.clampMultiplier(FlowConfig.get(FlowConfig.HOLY_MULTIPLIER));
        int window = FlowConfig.get(FlowConfig.CAST_WINDOW_TICKS);
        ok(src, text("status.config", "Rukh flow %s: Holy road flow %s, Abyssal road flow %s, match x%s, a hit counts for %s ticks after a cast, rejection %s percent of max health (at least %s), %s.",
                on ? "ON" : "OFF", FlowConfig.holyFlow().id(), FlowConfig.abyssalFlow().id(), num(mult), window, num(FlowConfig.get(FlowConfig.REJECT_PERCENT)),
                num(FlowConfig.get(FlowConfig.REJECT_MIN_HEALTH)), FlowConfig.get(FlowConfig.NON_LETHAL) ? "never lethal" : "can kill"));
        Sector s = MegaDungeon.sectorAt(level, FlowEvents.posOf(p));
        Alignment flow = Alignment.NEUTRAL;
        if (s == null) {
            ok(src, text("status.place.none", "%s is not inside a sector of the Mega-Dungeon: no flow here.", p.getName()));
        } else {
            flow = FlowEvents.flowOf(s.polarity);
            ok(src, text("status.place", "%s stands in sector %s (%s road, floor %s): the flow here is %s.", p.getName(), s.id, s.polarity.id(),
                    FloorMap.label(MegaDungeon.floorAt(level, FlowEvents.posOf(p))), flow.id()));
        }
        FlowTracker.Cast c = FlowTracker.shared().lastCast(p.getUUID());
        if (c == null) {
            ok(src, text("status.cast.none", "No cast heard from %s since they logged in (if they cast a grimoire page, the two FlowCasts lines in GrimoireBook.castPage are not wired).", p.getName()));
        } else {
            long age = level.getGameTime() - c.tick();
            boolean recent = age >= 0 && age <= window;
            FlowRules.Outcome o = FlowRules.outcome(flow, c.alignment());
            ok(src, text("status.cast", "Last cast: %s, %s ticks ago (%s the %s-tick window). Here that is a %s%s.", c.alignment().id(), age, recent ? "inside" : "outside", window,
                    o.name().toLowerCase(Locale.ROOT), o == FlowRules.Outcome.BOOST ? " (x" + num(mult) + " while it lasts)" : ""));
        }
        ok(src, text("status.table", "Table: %s light words, %s dark words, %s exact entries; the cast hook has seen %s player(s) cast.",
                AlignmentRegistry.current().lightKeywords().size(), AlignmentRegistry.current().darkKeywords().size(), AlignmentRegistry.current().exactEntries().size(),
                FlowTracker.shared().size()));
        return 1;
    }

    // ---------------------------------------------------------------- classify
    private static int classify(CommandSourceStack src, String raw) {
        List<String> ids = new ArrayList<>();
        for (String part : raw.trim().split("[\\s,]+")) if (!part.isBlank()) ids.add(part);
        if (ids.isEmpty()) return fail(src, text("classify.empty", "Give at least one id, most specific first, for example: devil_union light"));
        MagicAlignments table = AlignmentRegistry.current();
        if (table.isEmpty()) return fail(src, text("classify.noTable", "The classification table is empty (alignments.json was not loaded): every cast reads as neutral."));
        MagicAlignments.Verdict v = table.explain(ids.toArray(new String[0]));
        return ok(src, text("classify.result", "%s -> %s: %s.", String.join(" ", ids), v.alignment().id().toUpperCase(Locale.ROOT), v.rule()));
    }

    // ---------------------------------------------------------------- vessels
    private static int vesselList(CommandSourceStack src) {
        MinecraftServer server = src.getServer();
        List<RelicDef> all = DecennialReward.candidates();
        Set<String> alive = UniqueLedger.liveIds(server);
        Map<String, Integer> byGrade = new TreeMap<>();
        List<String> inWorld = new ArrayList<>(), free = new ArrayList<>();
        for (RelicDef d : all) {
            byGrade.merge(d.grade().shortName(), 1, Integer::sum);
            (alive.contains(d.id()) ? inWorld : free).add(d.id());
        }
        ok(src, text("vessel.head", "%s relics can be handed out as a Metal Vessel (%s): %s are free, %s are already in the world. The switch vesselEnabled is %s.",
                all.size(), byGrade, free.size(), inWorld.size(), FlowConfig.get(FlowConfig.VESSEL_ENABLED) ? "on" : "off"));
        if (!inWorld.isEmpty()) ok(src, text("vessel.world", "In the world: %s", String.join(", ", inWorld)));
        ok(src, text("vessel.free", "Free: %s%s", String.join(", ", free.subList(0, Math.min(LIST_SHOWN, free.size()))),
                free.size() > LIST_SHOWN ? " ... and " + (free.size() - LIST_SHOWN) + " more" : ""));
        RewardLog log = FlowData.get(server).rewards();
        if (log.size() == 0) {
            ok(src, text("vessel.paid.none", "No sector has paid its vessel yet."));
        } else {
            List<String> paid = new ArrayList<>();
            for (Map.Entry<Integer, String> e : log.entries().entrySet()) paid.add("#" + e.getKey() + " " + (e.getValue().isEmpty() ? "?" : e.getValue()));
            ok(src, text("vessel.paid", "Sectors that have paid: %s", String.join(", ", paid)));
        }
        return 1;
    }

    private static int vesselGive(CommandSourceStack src, ServerPlayer target) {
        ServerLevel level = src.getLevel();
        ServerPlayer sender = src.getPlayer();
        Sector s = sender == null ? null : MegaDungeon.sectorAt(sender.level(), FlowEvents.posOf(sender));
        DecennialReward.Grant g = DecennialReward.grant(level, s, true);
        if (g.status() != DecennialReward.Status.GIVEN) {
            return fail(src, text("vessel.none", "No Metal Vessel is left to give: every relic that can carry a Djinn is already in the world."));
        }
        if (!target.getInventory().add(g.stack())) target.drop(g.stack(), false);
        DecennialReward.announce(level, target, s, g, null, false);
        return ok(src, text("vessel.gave", "Gave %s to %s%s. (An admin hand-over: the sector's own reward is unchanged.)", g.stack().getHoverName(), target.getName(),
                s == null ? "" : " as the Guardian of floor " + FloorMap.label(s.lastFloor())));
    }
}
