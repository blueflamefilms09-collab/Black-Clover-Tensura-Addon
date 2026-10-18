package com.newuniverse.nusmp.elite.races.runtime;

import com.newuniverse.nusmp.elite.races.EliteRaces;
import com.newuniverse.nusmp.elite.races.EliteRacesConfig;
import com.newuniverse.nusmp.elite.races.data.RaceTemplate;
import com.newuniverse.nusmp.elite.races.data.RaceTemplates;
import com.newuniverse.nusmp.elite.races.race.EliteRace;
import com.newuniverse.nusmp.elite.races.rule.RaceCtx;
import com.newuniverse.nusmp.elite.races.rule.RaceEvolutionRule;
import com.newuniverse.nusmp.elite.races.rule.RaceRuleSet;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.race.RaceHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the one place that moves a player along a line. There are TWO ways in and both end here. (1) Tensura's own evolution screen: the race lists the rules as
 * requirements, Tensura changes the race when the bar is full, and the race's {@code onRaceEvolution} calls {@link #onEvolved} so the items of the rules are taken. (2) The
 * command {@code /multiverse elite race evolve} and the Primordial Rite, which check the whole set, change the race and take the items themselves. The two never charge twice: the
 * tick at which items were taken is remembered, and {@link #onEvolved} takes them only if the set really was met (a forced evolution by an administrator keeps the player's items).
 */
public final class RaceEvolution {
    private RaceEvolution() {}

    /** The result of an attempt, with a sentence for the player. */
    public record Outcome(boolean ok, Component message) {}

    private static Outcome fail(String key, String english, Object... args) { return new Outcome(false, RaceMessages.msg(key, english, args)); }

    // ---------------------------------------------------------------- along the line
    public static Outcome evolve(ServerPlayer p, boolean force) {
        if (!EliteRacesConfig.enabled()) return fail("off", "The elite races are switched off on this server.");
        EliteRace race = RaceTransforms.raceOf(p);
        RaceTemplate t = race == null ? null : race.template();
        if (t == null) return fail("norace", "You are not one of the elite races (Saiyan or Primordial).");
        if (t.evolvesTo().isEmpty()) return fail("top", "%s is the top of its line.", race.getName());
        ManasRace next = EliteRaces.race(t.evolvesTo());
        if (next == null) return fail("nonext", "The next race is not registered.");
        RaceCtx ctx = PlayerRaceCtx.of(p);
        RaceRuleSet set = t.evolution();
        if (!force && !set.allMet(ctx)) return missing(set, ctx);
        RaceLedger.markConsumed(p, p.serverLevel().getGameTime());
        boolean changed;
        try {
            changed = RaceHelper.evolveRace(p, next, true, true);   // the call this mod already uses for the Wizard King's awakening (multiverse/CrownAwakening)
        } catch (RuntimeException e) {
            return fail("error", "The change of race failed (%s).", e.getClass().getSimpleName());
        }
        if (!changed) return fail("refused", "Tensura refused the change of race.");
        if (!force) set.commitOnly(ctx);
        return new Outcome(true, RaceMessages.msg("evolved", "You evolve into %s!", next.getName()));
    }

    /** The Primordial Rite (or any race that has {@code entry} rules): become {@code targetId} from outside its line. */
    public static Outcome rite(ServerPlayer p, String targetId, boolean force) {
        if (!EliteRacesConfig.enabled()) return fail("off", "The elite races are switched off on this server.");
        RaceTemplate t = RaceTemplates.get(targetId);
        ManasRace target = EliteRaces.race(targetId);
        if (t == null || target == null) return fail("norace.target", "There is no such race: %s.", String.valueOf(targetId));
        if (!force && t.entry().isEmpty()) return fail("norite", "%s cannot be entered by a rite.", target.getName());
        EliteRace now = RaceTransforms.raceOf(p);
        if (now != null && now.templateId().equals(targetId)) return fail("already", "You already are %s.", target.getName());
        RaceCtx ctx = PlayerRaceCtx.of(p);
        if (!force && !t.entry().allMet(ctx)) return missing(t.entry(), ctx);
        boolean changed;
        try {
            changed = RaceAPI.getRaceFrom(p).setRace(target, true);
        } catch (RuntimeException e) {
            return fail("error", "The change of race failed (%s).", e.getClass().getSimpleName());
        }
        if (!changed) return fail("refused", "Tensura refused the change of race.");
        if (!force) t.entry().commitOnly(ctx);
        return new Outcome(true, RaceMessages.msg("rite", "The rite is done: you are now %s.", target.getName()));
    }

    private static Outcome missing(RaceRuleSet set, RaceCtx ctx) {
        List<String> words = new ArrayList<>();
        for (RaceEvolutionRule r : set.unmet(ctx)) words.add(r.describe());
        return fail("missing", "Not yet: %s.", String.join("; ", words));
    }

    // ---------------------------------------------------------------- the Tensura screen path
    /** Called by the OLD race's {@code onRaceEvolution}: takes the items of its evolution rules, once, and only when the rules were all met. */
    public static void onEvolved(ServerPlayer p, EliteRace from) {
        RaceTemplate t = from.template();
        if (t == null || t.evolution().isEmpty()) return;
        long now = p.serverLevel().getGameTime();
        if (RaceLedger.lastConsume(p) == now) return;
        RaceCtx ctx = PlayerRaceCtx.of(p);
        if (!t.evolution().allMet(ctx)) return;
        RaceLedger.markConsumed(p, now);
        t.evolution().commitOnly(ctx);
    }

    // ---------------------------------------------------------------- for info
    /** One line per rule: a tick or a cross, the rule in words and its progress. */
    public static List<String> progressLines(RaceRuleSet set, RaceCtx ctx) {
        List<String> out = new ArrayList<>();
        for (RaceEvolutionRule r : set.rules()) {
            int pct = (int) Math.round(Math.max(0.0, Math.min(1.0, r.progress(ctx))) * 100);
            out.add((r.met(ctx) ? "[x] " : "[ ] ") + r.describe() + " (" + pct + "%, share " + Math.round(set.sharePercent(r)) + "%)");
        }
        return out;
    }
}
