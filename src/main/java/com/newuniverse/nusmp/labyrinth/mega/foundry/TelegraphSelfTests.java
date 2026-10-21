package com.newuniverse.nusmp.labyrinth.mega.foundry;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.poise.PoiseData;
import com.newuniverse.nusmp.labyrinth.mega.poise.PoiseState;
import com.newuniverse.nusmp.labyrinth.mega.poise.PoiseSystem;
import com.newuniverse.nusmp.labyrinth.mega.poise.TelegraphDef;
import com.newuniverse.nusmp.labyrinth.mega.poise.TelegraphLibrary;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the telegraphed attacks are the foundry goal of the poise layer, and their promises need a real foundry mob to check, so these tests live in the foundry package (the goal is
 * package-private). {@code dungeon.poise.telegraph}: a foundry mob carries the telegraphing goal exactly when the config says so (and is still an ability goal either way, which is what the older
 * foundry test looks for); every cleave, slam, leap, dash, dive and bash that a mob of the roster uses has a warning in the table. {@code dungeon.poise.cast}: for a mob of the elite tier of each kind
 * of ability the table warns about, one whole cast runs against a stand-in player: the wind-up is at least the table's minimum and at least the ability's own, the cast ends, the cooldown starts, the
 * super armor is up during the wind-up when the row says so; and a cast that is cut short by a broken bar starts the whole cooldown and leaves no cast behind.
 */
public final class TelegraphSelfTests {
    private TelegraphSelfTests() {}

    private static final Set<FoundryAbility.Kind> MUST_WARN = EnumSet.of(FoundryAbility.Kind.CLEAVE, FoundryAbility.Kind.SLAM, FoundryAbility.Kind.LEAP, FoundryAbility.Kind.DASH, FoundryAbility.Kind.DIVE,
            FoundryAbility.Kind.BASH);

    public static void register() {
        SelfTests.register("dungeon.poise.telegraph", TelegraphSelfTests::telegraph);
        SelfTests.register("dungeon.poise.cast", TelegraphSelfTests::cast);
    }

    private static SelfTests.Result ok(String d) { return SelfTests.Result.ok(d); }

    private static SelfTests.Result fail(String d) { return SelfTests.Result.fail(d); }

    private static TelegraphedAttackGoal goalOf(FoundryMobEntity m) {
        for (WrappedGoal g : m.goals()) if (g.getGoal() instanceof TelegraphedAttackGoal t) return t;
        return null;
    }

    private static SelfTests.Result telegraph(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return fail("the Mega-Dungeon dimension is not loaded");
        TelegraphLibrary lib = PoiseSystem.telegraphs();
        if (lib.kinds().isEmpty()) return fail("the telegraph table is empty");
        int checked = 0;
        for (FoundryDef d : FoundryData.roster().all()) {
            for (FoundryAbility a : FoundryData.abilitiesOf(d)) {
                if (MUST_WARN.contains(a.kind()) && lib.def(a) == null) return fail(d.id() + " uses " + a.id() + " (" + a.kind() + ") which has no warning in the table");
                if (lib.def(a) != null) checked++;
            }
        }
        FoundryDef any = FoundryData.roster().all().isEmpty() ? null : FoundryData.roster().all().get(0);
        if (any == null) return fail("the foundry roster is empty");
        FoundryMobEntity m = FoundryRegistry.MOB.get().create(level);
        if (m == null) return fail("the entity type does not create");
        try {
            m.setFoundry(any, 0);
            boolean want = FoundryConfig.get(FoundryConfig.TELEGRAPHS);
            boolean has = goalOf(m) != null;
            boolean plain = false;
            for (WrappedGoal g : m.goals()) if (g.getGoal() instanceof FoundryGoals.AbilityGoal) plain = true;
            if (!plain) return fail("the mob has no ability goal at all");
            if (has != want) return fail("telegraphedAttacks is " + want + " but the mob " + (has ? "has" : "lacks") + " the telegraphing goal");
        } finally {
            m.discard();
        }
        return ok(checked + " ability uses across the roster have a warning; every cleave, slam, leap, dash, dive and bash does; the mob carries the " + (FoundryConfig.get(FoundryConfig.TELEGRAPHS) ? "telegraphing" : "plain") + " ability goal");
    }

    private static SelfTests.Result cast(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return fail("the Mega-Dungeon dimension is not loaded");
        if (!FoundryConfig.get(FoundryConfig.TELEGRAPHS)) return ok("skipped: foundry.poise.telegraphedAttacks is false in the config");
        if (!FoundryConfig.get(FoundryConfig.ABILITIES)) return ok("skipped: foundry.abilities is false in the config");
        TelegraphLibrary lib = PoiseSystem.telegraphs();
        FakePlayer p = SelfTests.player(level, "poise_cast", 3.5, 200.0, 0.5);
        Set<FoundryAbility.Kind> done = EnumSet.noneOf(FoundryAbility.Kind.class);
        List<String> report = new ArrayList<>();
        boolean cutShort = false;
        for (Polarity road : Polarity.values()) {
            for (FoundryDef d : FoundryData.roster().of(road, 5)) {
                FoundryMobEntity m = null;
                try {
                    List<FoundryAbility> abs = FoundryData.abilitiesOf(d);
                    int index = -1;
                    for (int i = 0; i < abs.size(); i++) {
                        FoundryAbility a = abs.get(i);
                        if (lib.def(a) != null && MUST_WARN.contains(a.kind()) && !done.contains(a.kind())) {
                            index = i;
                            break;
                        }
                    }
                    if (index < 0) continue;
                    m = FoundryRegistry.MOB.get().create(level);
                    if (m == null) return fail("the entity type does not create");
                    m.moveTo(0.5, 200.0, 0.5, 0f, 0f);
                    m.setFoundry(d, road.sign() * 801);
                    TelegraphedAttackGoal g = goalOf(m);
                    if (g == null) return fail(d.id() + " has no telegraphing goal");
                    FoundryAbility a = m.brain().abilities().get(index);
                    TelegraphDef def = lib.def(a);
                    done.add(a.kind());
                    int ticks = g.drill(index, p);
                    int floorWind = Math.max(a.wind(), def.minWind());
                    if (ticks < floorWind) return fail(d.id() + "/" + a.id() + ": the wind-up was " + ticks + " ticks, the table asks for at least " + floorWind);
                    if (ticks != g.lastWind()) return fail(d.id() + "/" + a.id() + ": the cast took " + ticks + " ticks but the wind-up was " + g.lastWind());
                    if (m.casting()) return fail(d.id() + "/" + a.id() + ": still casting after the cast ended");
                    if (m.brain().cooldownLeft(index, m.tickCount) <= 0) return fail(d.id() + "/" + a.id() + ": the cooldown did not start");
                    PoiseData bar = PoiseSystem.data(m);
                    if (bar != null && def.armor()) {
                        long now = m.level().getGameTime();
                        m.tickCount = 0;
                        m.brain().reset(0);
                        g.begin(index, p);
                        if (!PoiseSystem.armored(m, now)) return fail(d.id() + "/" + a.id() + ": no super armor during the wind-up of an armored attack");
                        PoiseSystem.Outcome o = PoiseSystem.hit(m, (float) (bar.state.max() * 4.0), false);
                        if (o.hit() != PoiseState.Hit.BROKEN) return fail(d.id() + "/" + a.id() + ": a huge blow on the winding-up mob was " + o.hit());
                        if (m.casting() || g.advance()) return fail(d.id() + "/" + a.id() + ": the cast went on after the bar broke");
                        g.stop();
                        if (m.brain().cooldownLeft(index, m.tickCount) <= 0) return fail(d.id() + "/" + a.id() + ": a cast cut short did not start its cooldown");
                        cutShort = true;
                    }
                    report.add(a.kind() + ":" + a.id() + " wind " + ticks);
                } finally {
                    if (m != null) m.discard();
                }
            }
        }
        if (done.isEmpty()) return fail("no elite-tier mob of the roster uses a telegraphed ability");
        return ok("casts run clean for " + report + (cutShort ? "; a broken bar cut a cast short and started its cooldown" : "; (no armored attack on an elite was available for the cut-short check)"));
    }
}
