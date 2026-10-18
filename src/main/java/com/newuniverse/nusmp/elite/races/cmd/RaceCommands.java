package com.newuniverse.nusmp.elite.races.cmd;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.newuniverse.nusmp.elite.races.EliteRaces;
import com.newuniverse.nusmp.elite.races.EliteRacesConfig;
import com.newuniverse.nusmp.elite.races.data.RaceTemplate;
import com.newuniverse.nusmp.elite.races.data.RaceTemplates;
import com.newuniverse.nusmp.elite.races.race.EliteRace;
import com.newuniverse.nusmp.elite.races.rule.EvolutionRules;
import com.newuniverse.nusmp.elite.races.rule.RaceCtx;
import com.newuniverse.nusmp.elite.races.rule.RaceEvolutionRule;
import com.newuniverse.nusmp.elite.races.runtime.PlayerRaceCtx;
import com.newuniverse.nusmp.elite.races.runtime.PlayerTransformCtx;
import com.newuniverse.nusmp.elite.races.runtime.RaceEvolution;
import com.newuniverse.nusmp.elite.races.runtime.RaceLedger;
import com.newuniverse.nusmp.elite.races.runtime.RaceSpecs;
import com.newuniverse.nusmp.elite.races.runtime.RaceTransforms;
import com.newuniverse.nusmp.elite.races.state.TransformMachine;
import com.newuniverse.nusmp.elite.races.state.TransformSpec;
import com.newuniverse.nusmp.elite.races.state.TransformState;
import com.newuniverse.nusmp.elite.races.state.Zenkai;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.RaceAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Map;

/**
 * Why: the owner's commands, {@code /multiverse elite race <list|info|set|transform>} for operators, plus {@code evolve} (the operator's way along a line, with or without the
 * rules) and {@code rite} (the Primordial Rite, open to players when the config allows). They are registered as a SECOND branch of the existing {@code /multiverse} literal:
 * Brigadier merges two registrations of the same literal, so no existing command file is touched. Only {@code rite} is open to everyone; every other leaf requires permission level 2,
 * and every leaf answers that the races are off when the master switch is. They call exactly the code the hotkey and Tensura's screen use ({@link RaceTransforms},
 * {@link RaceEvolution}); nothing here decides a rule.
 */
public final class RaceCommands {
    private RaceCommands() {}

    private static boolean admin(CommandSourceStack s) { return s.hasPermission(2); }

    private static final SuggestionProvider<CommandSourceStack> RACES = (c, b) -> SharedSuggestionProvider.suggest(RaceTemplates.all().keySet(), b);
    private static final SuggestionProvider<CommandSourceStack> STATES = (c, b) -> {
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (TransformState s : TransformState.values()) ids.add(s.id);
        return SharedSuggestionProvider.suggest(ids, b);
    };

    public static void onRegister(RegisterCommandsEvent event) { register(event.getDispatcher()); }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("multiverse").then(Commands.literal("elite").then(Commands.literal("race")
                .then(Commands.literal("list").requires(RaceCommands::admin).executes(RaceCommands::list))
                .then(Commands.literal("info").requires(RaceCommands::admin)
                        .executes(c -> info(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> info(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("set").requires(RaceCommands::admin)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("race", StringArgumentType.word()).suggests(RACES).executes(RaceCommands::set))))
                .then(Commands.literal("transform").requires(RaceCommands::admin)
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("state", StringArgumentType.word()).suggests(STATES)
                                        .executes(c -> transform(c, false))
                                        .then(Commands.literal("force").executes(c -> transform(c, true))))))
                .then(Commands.literal("evolve").requires(RaceCommands::admin)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> evolve(c, false))
                                .then(Commands.literal("force").executes(c -> evolve(c, true)))))
                .then(Commands.literal("rite")
                        .executes(c -> rite(c, EliteRaces.PRIMORDIAL_ID))
                        .then(Commands.argument("race", StringArgumentType.word()).suggests(RACES).executes(c -> rite(c, StringArgumentType.getString(c, "race"))))))));
    }

    private static int ok(CommandContext<CommandSourceStack> c, Component msg) {
        c.getSource().sendSuccess(() -> msg, true);
        return 1;
    }

    private static int say(CommandContext<CommandSourceStack> c, String line) {
        c.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int fail(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendFailure(Component.literal(msg));
        return 0;
    }

    private static boolean off(CommandContext<CommandSourceStack> c) {
        if (EliteRacesConfig.enabled()) return false;
        fail(c, "The elite races are switched off on this server (nusmp-elite-races-server.toml, [races] enabled).");
        return true;
    }

    // ---------------------------------------------------------------- list
    private static int list(CommandContext<CommandSourceStack> c) {
        if (off(c)) return 0;
        say(c, "Elite races (" + RaceTemplates.all().size() + "):");
        for (RaceTemplate t : RaceTemplates.all().values()) {
            StringBuilder sb = new StringBuilder("  ").append(t.id()).append("  line ").append(t.line()).append(", tier ").append(t.tier()).append(", ").append(t.difficulty());
            sb.append(", aura ").append(num(t.stats().auraMin())).append("-").append(num(t.stats().auraMax()));
            sb.append(", magicule ").append(num(t.stats().magiculeMin())).append("-").append(num(t.stats().magiculeMax()));
            if (!t.evolvesTo().isEmpty()) sb.append(", evolves to ").append(t.evolvesTo()).append(gate(t));
            else if (!t.entry().isEmpty()) sb.append(", entered by the rite");
            sb.append(", forms:");
            for (TransformState s : TransformState.values()) if (t.spec(s) != null) sb.append(' ').append(s.id);
            if (EliteRaces.race(t.id()) == null) sb.append("  (NOT REGISTERED: restart needed)");
            say(c, sb.toString());
        }
        for (String p : RaceTemplates.problems()) say(c, "  problem: " + p);
        return RaceTemplates.all().size();
    }

    private static String gate(RaceTemplate t) {
        for (RaceEvolutionRule r : t.evolution().rules()) if (r instanceof EvolutionRules.EpGate g) return " (EP " + num(g.min() * EliteRacesConfig.epGateScale()) + ")";
        return "";
    }

    private static String num(double v) { return v == Math.rint(v) ? String.format(java.util.Locale.ROOT, "%,d", (long) v) : String.format(java.util.Locale.ROOT, "%.2f", v); }

    // ---------------------------------------------------------------- info
    private static int info(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        if (off(c)) return 0;
        EliteRace race = RaceTransforms.raceOf(p);
        String name = p.getGameProfile().getName();
        if (race == null) return say(c, name + " is not one of the elite races.");
        RaceTemplate t = race.template();
        RaceCtx rc = PlayerRaceCtx.of(p);
        PlayerTransformCtx tc = new PlayerTransformCtx(p, t.tier());
        TransformMachine m = RaceLedger.machine(p);
        long now = tc.now();
        say(c, name + ": " + race.getName().getString() + " (" + t.id() + ", tier " + t.tier() + ", " + t.line() + " line)");
        say(c, "  EP " + num(rc.ep()) + ", health " + num(p.getHealth()) + "/" + num(p.getMaxHealth()) + ", ki (aura) " + Math.round(tc.auraFraction() * 100) + "%");
        say(c, "  form: " + m.state().id + (m.active() ? ", " + (m.remaining(now) / 20) + "s left" : ""));
        Map<TransformState, TransformSpec> specs = RaceSpecs.effective(t);
        for (TransformState s : TransformState.values()) {
            TransformSpec spec = specs.get(s);
            if (spec == null) continue;
            TransformMachine.Result r = m.check(spec, tc);
            String rest = m.restLeft(s, now) > 0 ? ", resting " + (m.restLeft(s, now) / 20) + "s" : "";
            say(c, "    " + s.id + ": " + (r.ok() ? "can enter now" : (r.reason() == null ? "-" : r.reason().name().toLowerCase(java.util.Locale.ROOT))) + rest);
        }
        if (t.hasTrait("zenkai")) {
            Zenkai.State z = RaceLedger.zenkai(p);
            say(c, "  Zenkai " + z.stacks() + "/" + Zenkai.cap(t.tier()));
        }
        say(c, "  kills: any " + RaceLedger.kills(p, "any") + ", hostile " + RaceLedger.kills(p, "hostile") + ", boss " + RaceLedger.kills(p, "boss") + ", player " + RaceLedger.kills(p, "player"));
        if (!t.evolution().isEmpty()) {
            say(c, "  evolution to " + t.evolvesTo() + ": " + Math.round(t.evolution().percent(rc)) + "%");
            for (String line : RaceEvolution.progressLines(t.evolution(), rc)) say(c, "    " + line);
        } else if (!t.evolvesTo().isEmpty()) {
            say(c, "  evolution to " + t.evolvesTo() + ": no rules (free)");
        } else {
            say(c, "  top of its line");
        }
        return 1;
    }

    // ---------------------------------------------------------------- set
    private static int set(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        if (off(c)) return 0;
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        String id = StringArgumentType.getString(c, "race");
        ManasRace race = EliteRaces.race(id);
        if (race == null) return fail(c, "There is no elite race '" + id + "'. Use /multiverse elite race list.");
        boolean changed;
        try {
            changed = RaceAPI.getRaceFrom(p).setRace(race, true);
        } catch (RuntimeException e) {
            return fail(c, "Setting the race failed: " + e);
        }
        if (!changed) return fail(c, "Tensura refused the change of race.");
        return ok(c, Component.literal(p.getGameProfile().getName() + " is now " + race.getName().getString() + "."));
    }

    // ---------------------------------------------------------------- transform
    private static int transform(CommandContext<CommandSourceStack> c, boolean force) throws CommandSyntaxException {
        if (off(c)) return 0;
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        String word = StringArgumentType.getString(c, "state");
        RaceTransforms.Outcome o;
        if (word.equalsIgnoreCase("off")) {
            o = RaceTransforms.request(p, RaceTransforms.Mode.OFF, null, force);
        } else {
            TransformState s = TransformState.byId(word);
            if (s == null) return fail(c, "'" + word + "' is not a form (base, boosted, ascended, great_ape, divine, or off).");
            o = RaceTransforms.request(p, RaceTransforms.Mode.STATE, s, force);
        }
        if (!o.ok()) return fail(c, o.message().getString());
        return ok(c, Component.literal(p.getGameProfile().getName() + ": " + o.message().getString()));
    }

    // ---------------------------------------------------------------- evolve and rite
    private static int evolve(CommandContext<CommandSourceStack> c, boolean force) throws CommandSyntaxException {
        if (off(c)) return 0;
        ServerPlayer p = EntityArgument.getPlayer(c, "player");
        RaceEvolution.Outcome o = RaceEvolution.evolve(p, force);
        if (!o.ok()) return fail(c, o.message().getString());
        return ok(c, o.message());
    }

    private static int rite(CommandContext<CommandSourceStack> c, String raceId) throws CommandSyntaxException {
        if (off(c)) return 0;
        ServerPlayer p = c.getSource().getPlayerOrException();
        if (!admin(c.getSource()) && !EliteRacesConfig.primordialRite()) return fail(c, "The rite is closed on this server.");
        RaceEvolution.Outcome o = RaceEvolution.rite(p, raceId, false);
        if (!o.ok()) return fail(c, o.message().getString());
        return ok(c, o.message());
    }
}
