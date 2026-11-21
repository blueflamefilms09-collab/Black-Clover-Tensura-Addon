package com.newuniverse.nusmp.rebirth;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: the rites cannot be played by the author, so the CI server smoke test runs this in the real game (key {@code rebirth.rules}): the pure rules (checks, cooldown, confirm window, marks, the second
 * book's magic), the confirm-window map (one entry per player, cleared), and that both sigils and both recipes exist in the booted registries. It needs no player and touches no client class.
 */
public final class RebirthSelfTests {
    private RebirthSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("rebirth.rules", RebirthSelfTests::check);
    }

    private static Result check(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        rules(bad);
        state(bad);
        for (String id : new String[]{"sigil_of_rebirth", "sigil_of_the_second_book"}) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("anime_multiverse", id);
            if (BuiltInRegistries.ITEM.getOptional(rl).isEmpty()) bad.add("no item " + id);
            if (server.getRecipeManager().byKey(rl).isEmpty()) bad.add("no recipe " + id);
        }
        if (!bad.isEmpty()) return Result.fail(String.join("; ", bad.subList(0, Math.min(8, bad.size()))));
        return Result.ok("checks + cooldown + confirm window + marks + second-book magic (pure), confirm map bounded, both sigils and recipes registered; master switch default " + RebirthConfig.ENABLED.getDefault());
    }

    private static void rules(List<String> bad) {
        var R = RebirthRules.Rite.REBIRTH;
        var S = RebirthRules.Rite.SECOND_BOOK;
        long day = RebirthRules.TICKS_PER_DAY;
        if (RebirthRules.verdict(R, true, 10 * day, -1, 7, 1, 3, 4, -1, 3, true, 0, 0, false) != RebirthRules.Verdict.OK) bad.add("rebirth should pass");
        if (RebirthRules.verdict(R, false, 0, -1, 7, 1, 3, 4, -1, 3, true, 0, 0, false) != RebirthRules.Verdict.DISABLED) bad.add("disabled");
        if (RebirthRules.verdict(R, true, 0, -1, 7, 0, 0, 4, -1, 3, true, 0, 0, false) != RebirthRules.Verdict.NO_GRIMOIRE) bad.add("no grimoire");
        if (RebirthRules.verdict(R, true, 3 * day, 0, 7, 1, 3, 4, -1, 3, true, 0, 0, false) != RebirthRules.Verdict.COOLDOWN) bad.add("cooldown");
        if (RebirthRules.verdict(S, true, 99 * day, -1, 7, 1, 3, 4, -1, 3, true, 0, 0, false) != RebirthRules.Verdict.TIER_TOO_LOW) bad.add("tier");
        if (RebirthRules.verdict(S, true, 99 * day, -1, 7, 1, 4, 4, -1, 3, true, 0, 0, false) != RebirthRules.Verdict.OK) bad.add("second book with a missing resolve is fine");
        if (RebirthRules.verdict(S, true, 99 * day, -1, 7, 1, 4, 4, 1, 3, true, 0, 0, false) != RebirthRules.Verdict.RESOLVE_TOO_LOW) bad.add("resolve");
        if (RebirthRules.verdict(S, true, 99 * day, -1, 7, 2, 5, 4, 5, 3, true, 0, 0, false) != RebirthRules.Verdict.ALREADY_TWO) bad.add("already two");
        if (RebirthRules.cooldownLeft(5, 10, 7) != 7 * day || RebirthRules.cooldownLeft(100, -1, 7) != 0 || RebirthRules.daysLeft(1) != 1) bad.add("cooldown maths");
        if (!RebirthRules.confirmed(100, 200, 300, true) || RebirthRules.confirmed(100, 200, 300, false) || RebirthRules.confirmed(100, 500, 300, true) || RebirthRules.confirmed(-1, 200, 300, true)) bad.add("confirm window");
        if (RebirthRules.afterUse(R) != RebirthRules.Mode.RESET || RebirthRules.afterUse(S) != RebirthRules.Mode.SECOND || RebirthRules.afterQuestionnaire(RebirthRules.Mode.RESET) != RebirthRules.Mode.DONE) bad.add("marks");
        if (!"B".equals(RebirthRules.secondMagic("A", "A", "B", "A")) || !"C".equals(RebirthRules.secondMagic("C", null, null, "A")) || RebirthRules.secondMagic("A", "A", null, "A") != null) bad.add("second magic");
        if (RebirthRules.poolWithout(List.of("A", "B"), "A").contains("A") || RebirthRules.poolWithout(List.of("A"), "A").size() != 1) bad.add("pool");
        if (RebirthRules.twoBooksOnFusion(false, true, 5, false) || !RebirthRules.twoBooksOnFusion(true, true, 4, false) || RebirthRules.twoBooksOnFusion(true, true, 3, false)) bad.add("twin rule");
    }

    private static void state(List<String> bad) {
        int before = RebirthState.size();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        RebirthState.warn(a, RebirthRules.Rite.REBIRTH, 10);
        RebirthState.warn(a, RebirthRules.Rite.REBIRTH, 20);
        RebirthState.warn(b, RebirthRules.Rite.SECOND_BOOK, 30);
        if (RebirthState.size() != before + 2) bad.add("one entry per player");
        if (RebirthState.warnedAt(a, RebirthRules.Rite.SECOND_BOOK) != -1 || RebirthState.warnedAt(a, RebirthRules.Rite.REBIRTH) != 20) bad.add("warning is per rite");
        RebirthState.forget(a);
        RebirthState.forget(b);
        if (RebirthState.size() != before) bad.add("forget");
    }
}
