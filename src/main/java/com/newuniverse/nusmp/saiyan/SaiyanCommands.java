package com.newuniverse.nusmp.saiyan;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: the commands of the Saiyan system (3.63.0), a root command of their own. For everybody: {@code /saiyan status} (your own Battle Power, rank and bonuses in words). For operators (permission 2):
 * {@code race <player> on|off} (make a player a Saiyan, or take the race away), {@code awaken <player>} (skip the meditation), {@code bp <player> set|add <value>} (clamped to the rules), {@code status <player>}.
 * Stage 1 has no other way to become a Saiyan; the questionnaire and the race pool are not touched. 3.65.0 adds, for everybody: {@code forms} (every form, what is unlocked and what is still missing) and
 * {@code form <id>} (choose the form of the toggle key); for operators: {@code unlock <player> <form|all>}, {@code flag <player> <flag>} (the Limit Breaker unlock is the flag {@code limit_breaker}),
 * {@code ultimate <player> goku|none} and {@code zenkai <player>}.
 */
public final class SaiyanCommands {
    private SaiyanCommands() {}

    public static void register(IEventBus bus) { bus.addListener(SaiyanCommands::onRegister); }

    private static final SuggestionProvider<CommandSourceStack> FORM_IDS = (c, b) -> {
        List<String> ids = new ArrayList<>();
        for (SaiyanForms.Form f : SaiyanForms.ALL) ids.add(f.id());
        return SharedSuggestionProvider.suggest(ids, b);
    };

    private static void onRegister(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("saiyan")
                .then(Commands.literal("status").executes(c -> status(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player()).requires(s -> s.hasPermission(2)).executes(c -> status(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("forms").executes(c -> forms(c, c.getSource().getPlayerOrException())))
                .then(Commands.literal("form").then(Commands.argument("id", StringArgumentType.word()).suggests(FORM_IDS)
                        .executes(c -> select(c, c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "id")))))
                .then(Commands.literal("unlock").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("id", StringArgumentType.word()).suggests(FORM_IDS)
                                .executes(c -> unlock(c, EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "id"))))))
                .then(Commands.literal("flag").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("flag", StringArgumentType.word())
                                .executes(c -> flag(c, EntityArgument.getPlayer(c, "player"), StringArgumentType.getString(c, "flag"))))))
                .then(Commands.literal("ultimate").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("goku").executes(c -> ultimate(c, EntityArgument.getPlayer(c, "player"), true)))
                                .then(Commands.literal("none").executes(c -> ultimate(c, EntityArgument.getPlayer(c, "player"), false)))))
                .then(Commands.literal("zenkai").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> zenkai(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("race").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("on").executes(c -> race(c, EntityArgument.getPlayer(c, "player"), true)))
                                .then(Commands.literal("off").executes(c -> race(c, EntityArgument.getPlayer(c, "player"), false)))))
                .then(Commands.literal("awaken").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player()).executes(c -> awaken(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("bp").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("set").then(Commands.argument("value", DoubleArgumentType.doubleArg(SaiyanRules.START_BP, SaiyanRules.BP_HARD_CAP))
                                        .executes(c -> bp(c, EntityArgument.getPlayer(c, "player"), DoubleArgumentType.getDouble(c, "value"), false))))
                                .then(Commands.literal("add").then(Commands.argument("value", DoubleArgumentType.doubleArg(-SaiyanRules.BP_HARD_CAP, SaiyanRules.BP_HARD_CAP))
                                        .executes(c -> bp(c, EntityArgument.getPlayer(c, "player"), DoubleArgumentType.getDouble(c, "value"), true)))))));
    }

    private static int say(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int no(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendFailure(Component.literal(text));
        return 0;
    }

    private static int race(CommandContext<CommandSourceStack> c, ServerPlayer p, boolean on) {
        SaiyanState s = SaiyanStore.peek(p);
        if (on == (s != null && s.saiyan)) return no(c, p.getName().getString() + (on ? " is already a Saiyan" : " is not a Saiyan"));
        SaiyanManager.setSaiyan(p, on);
        return say(c, p.getName().getString() + (on ? " is now a Saiyan" : " is no longer a Saiyan (Battle Power is kept)"));
    }

    private static int awaken(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        SaiyanState s = SaiyanStore.peek(p);
        if (s == null || !s.saiyan) return no(c, p.getName().getString() + " is not a Saiyan");
        if (s.awakened) return no(c, p.getName().getString() + " is already awakened");
        SaiyanManager.awaken(p, s, SaiyanManager.runOf(p));
        return say(c, "Awakened the Ki of " + p.getName().getString());
    }

    private static int bp(CommandContext<CommandSourceStack> c, ServerPlayer p, double v, boolean add) {
        SaiyanState s = SaiyanStore.peek(p);
        if (s == null || !s.saiyan) return no(c, p.getName().getString() + " is not a Saiyan");
        s.setBattlePower(Math.min(SaiyanConfig.maxBp(), add ? s.battlePower + v : v));
        SaiyanManager.markDirty(p);
        return say(c, p.getName().getString() + " now has " + Math.round(s.battlePower) + " Battle Power (" + s.rank().name().toLowerCase(Locale.ROOT) + ")");
    }

    private static int status(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        SaiyanState s = SaiyanStore.peek(p);
        if (s == null || !s.saiyan) return say(c, p.getName().getString() + " is not a Saiyan.");
        SaiyanRules.Table t = SaiyanConfig.bonuses(s.battlePower);
        long now = p.level().getGameTime();
        StringBuilder sb = new StringBuilder(p.getName().getString() + ": " + s.rank().name().toLowerCase(Locale.ROOT) + ", Battle Power " + Math.round(s.battlePower)
                + (s.awakened ? ", Ki awakened" : ", Ki not awakened (sneak still)"));
        sb.append("\n Ki ").append(Math.round(SaiyanKi.current(p))).append(" / ").append(Math.round(SaiyanKi.max(p)))
                .append(", stamina ").append(Math.round(Math.max(0.0, s.stamina < 0.0 ? SaiyanManager.staminaMax(s) : s.stamina))).append(" / ").append(Math.round(SaiyanManager.staminaMax(s)));
        sb.append("\n strength +").append(pct(t.strength())).append("%, durability +").append(pct(t.durability())).append("%, speed +").append(pct(t.speed())).append("%, attack speed +").append(pct(t.attackSpeed()))
                .append("%, jump +").append(pct(t.jump())).append("%, knockback resistance ").append(pct(t.knockback())).append("%, combat experience +").append(pct(t.combatXp())).append("%");
        double room = s.roomToday(now, SaiyanConfig.dailyFraction(), SaiyanConfig.dailyFloor());     // also rolls the day over
        sb.append("\n gained today ").append(Math.round(s.bpToday)).append(" of ").append(Math.round(s.bpToday + room));
        sb.append("\n Ki Flight ").append(SaiyanFlight.tier(s).name().toLowerCase(Locale.ROOT)).append(" (").append(s.uses("flight")).append(" s flown), senses ").append(SaiyanSenses.tier(s).name().toLowerCase(Locale.ROOT))
                .append(" (").append(s.uses("sense")).append(" scans), techniques mastered ").append(s.mastered()).append(" / ").append(SaiyanTraining.UNIVERSAL.size());
        sb.append("\n Ultimate: ").append(s.ultimate.isEmpty() ? "none" : s.ultimate).append(", Zenkai ").append(s.zenkai).append(", challenging martial victories ").append(s.hardMartialKills)
                .append(", trial victories ").append(s.trialKills);
        sb.append("\n training: physical ").append(Math.round(s.trained(SaiyanTraining.Cat.PHYSICAL))).append(", Ki ").append(Math.round(s.trained(SaiyanTraining.Cat.KI))).append(", martial ")
                .append(Math.round(s.trained(SaiyanTraining.Cat.MARTIAL))).append(", divine ").append(Math.round(s.trained(SaiyanTraining.Cat.DIVINE)));
        if (s.ultimate.isEmpty() && s.awakened) {
            List<String> need = SaiyanUltimates.missing(SaiyanConfig.gokuNeed(), GokuAbilities.progress(s));
            if (!need.isEmpty()) sb.append("\n Son Goku awakening still needs: ").append(String.join("; ", need));
        }
        return say(c, sb.toString());
    }

    private static SaiyanState saiyanOrSay(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        SaiyanState s = SaiyanStore.peek(p);
        if (s == null || !s.saiyan) {
            no(c, p.getName().getString() + " is not a Saiyan");
            return null;
        }
        return s;
    }

    private static int forms(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        SaiyanState s = saiyanOrSay(c, p);
        if (s == null) return 0;
        SaiyanForms.Progress pr = s.progress();
        SaiyanManager.Run r = SaiyanManager.runOf(p);
        StringBuilder sb = new StringBuilder("Forms (Base is always yours; active: " + (r.form.isEmpty() ? "Base" : r.form) + ", selected: " + (s.selectedForm.isEmpty() ? "none" : s.selectedForm) + ")");
        for (SaiyanForms.Form f : SaiyanForms.ALL) {
            sb.append("\n ").append(f.id()).append(" - ");
            if (s.unlocked.contains(f.id())) sb.append("unlocked, mastery ").append(pct(s.mastery(f.id()))).append("%");
            else sb.append("locked: ").append(String.join(", ", SaiyanForms.missing(f, SaiyanConfig.formMinBp(f), pr)));
        }
        return say(c, sb.toString());
    }

    private static int select(CommandContext<CommandSourceStack> c, ServerPlayer p, String id) {
        SaiyanState s = saiyanOrSay(c, p);
        if (s == null) return 0;
        SaiyanForms.Form f = SaiyanForms.byId(SaiyanForms.norm(id));
        if (f == null) return no(c, "No such form: " + id);
        if (!s.unlocked.contains(f.id())) return no(c, f.id() + " is not unlocked yet (see /saiyan forms)");
        s.selectedForm = f.id();
        SaiyanManager.markDirty(p);
        return say(c, "Selected " + f.id() + " for the form key");
    }

    private static int unlock(CommandContext<CommandSourceStack> c, ServerPlayer p, String id) {
        SaiyanState s = saiyanOrSay(c, p);
        if (s == null) return 0;
        String n = SaiyanForms.norm(id);
        int count = 0;
        if (n.equals("all")) {
            for (SaiyanForms.Form f : SaiyanForms.ALL) if (s.unlock(f.id())) count++;
        } else {
            SaiyanForms.Form f = SaiyanForms.byId(n);
            if (f == null) return no(c, "No such form: " + id);
            if (s.unlock(f.id())) count++;
        }
        SaiyanManager.markDirty(p);
        return say(c, "Unlocked " + count + " form(s) for " + p.getName().getString());
    }

    private static int flag(CommandContext<CommandSourceStack> c, ServerPlayer p, String flag) {
        SaiyanState s = saiyanOrSay(c, p);
        if (s == null) return 0;
        String f = SaiyanForms.norm(flag);
        if (f.isEmpty() || f.length() > 40) return no(c, "A flag is a short word");
        if (!s.grantFlag(f)) return no(c, "Could not add the flag (already there, or the list is full)");
        SaiyanManager.markDirty(p);
        return say(c, "Granted the flag " + f + " to " + p.getName().getString());
    }

    private static int ultimate(CommandContext<CommandSourceStack> c, ServerPlayer p, boolean give) {
        SaiyanState s = saiyanOrSay(c, p);
        if (s == null) return 0;
        if (!give) {
            s.ultimate = "";
            SaiyanManager.markDirty(p);
            return say(c, p.getName().getString() + " holds no Ultimate Skill now");
        }
        if (!SaiyanUltimates.mayTake(s.ultimate)) return no(c, p.getName().getString() + " already holds the Ultimate " + s.ultimate);
        GokuAbilities.awaken(p, s, SaiyanManager.runOf(p), SaiyanUltimates.byId(SaiyanUltimates.GOKU));
        return say(c, "Awakened Son Goku for " + p.getName().getString());
    }

    private static int zenkai(CommandContext<CommandSourceStack> c, ServerPlayer p) {
        SaiyanState s = saiyanOrSay(c, p);
        if (s == null) return 0;
        double gain = SaiyanCombat.grantZenkai(p, s, SaiyanManager.runOf(p), p.level().getGameTime());
        return say(c, "Zenkai for " + p.getName().getString() + ": +" + Math.round(gain) + " Battle Power (" + s.zenkai + " so far)");
    }

    private static long pct(double v) { return Math.round(v * 100.0); }
}
