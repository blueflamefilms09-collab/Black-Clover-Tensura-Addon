package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.skilltree.data.EvolvedParams;
import com.newuniverse.nusmp.skilltree.data.RouteEvolution;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.unique.UniqueBooks;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why (3.26.0, the Evolution skills): the counters, the gate and the Debt are wiring on top of pure rules, and wiring is what a unit test cannot see. Two self-tests, run by the CI server smoke test with a
 * stand-in player (a hand-built route stands in for a line, so they need no data file). {@code evolution.counters}: a counter written through the one place that writes it is clamped to the count, never goes
 * down, is separate for two players, is persisted in the player's own data and is copied to a new body; the Ultimate gate says "not evolved", then "needs mastery", then "open" and leaves every other art open;
 * an evolved passive reads its {@code evolved} numbers at its top level and an ordinary one does not; and, when the shipped data has a line with an evolution, the real hooks (a kill, a blow dealt, a blow taken, a
 * cast, an ally buffed) count for the owned skill while it is on, do not count a creature that is no foe, and stop when the skill is switched off. {@code evolution.debt}: a blow stores Debt on a spawned zombie
 * up to the cap of its maximum health, the ledger holds at most 20 foes per player, a Debt blow does not store more, a burst hits the foe once for the base blow plus what it owed and takes the Debt, and the
 * Debt is forgotten when the foe dies, when the owner dies and when the server stops. Everything the tests change is put back.
 */
public final class EvolutionSelfTests {
    private EvolutionSelfTests() {}

    public static final String COUNTERS = "evolution.counters", DEBT = "evolution.debt";
    private static final String ROUTE_ID = "nusmp:evolution_selftest";

    public static void register() {
        SelfTests.register(COUNTERS, EvolutionSelfTests::counters);
        SelfTests.register(DEBT, EvolutionSelfTests::debt);
    }

    // ================================================================== a hand-built line
    private static SkillNode node(String id, String type, String kind, Map<String, List<Double>> values, boolean ultimate, EvolvedParams evolved, int maxLevel) {
        return new SkillNode(id, id, "", List.of(), 0, List.of(), maxLevel, "", List.of(), values, 0, 0, "", type, kind, Map.of(), ultimate, evolved);
    }

    private static SkillRoute route(RouteEvolution evolution, SkillNode... nodes) {
        return new SkillRoute(ROUTE_ID, "selftest", "Evolution Test", "", "evolution_selftest", "Book", "Lore", 0xFFAA00, "minecraft:book", "", 0, 0, List.of(nodes), "selftest", "", "", "magma", evolution);
    }

    private static Map<String, List<Double>> debtValues(double tickSeconds) {
        return Map.of("store_pct", List.of(0.5), "max_pct", List.of(0.5), "tick_seconds", List.of(tickSeconds), "hold_seconds", List.of(10.0));
    }

    // ================================================================== counters and gate
    private static SelfTests.Result counters(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        ServerLevel level = server.overworld();
        FakePlayer a = SelfTests.standIn(level, "evolution_counter_a", 0.5, 100, 0.5);
        FakePlayer b = SelfTests.standIn(level, "evolution_counter_b", 8.5, 100, 0.5);
        RouteEvolution evo = new RouteEvolution("kills", 3, "Kill three foes.", "Test Ultimate", "");
        SkillNode ultimate = node("ult", SkillNode.TYPE_ACTIVE, "debt_burst", Map.of(), true, EvolvedParams.NONE, 1);
        SkillNode art = node("art", SkillNode.TYPE_ACTIVE, "debt_burst", Map.of(), false, EvolvedParams.NONE, 1);
        SkillNode passive = node("pas", SkillNode.TYPE_PASSIVE, "debt_store", debtValues(0.0), false, new EvolvedParams(Map.of("store_pct", 0.6), Map.of()), 3);
        SkillRoute r = route(evo, art, ultimate, passive);
        StringBuilder said = new StringBuilder();
        try {
            LineState.remove(a, LineState.EVO + ROUTE_ID);
            LineState.remove(b, LineState.EVO + ROUTE_ID);

            // 1. the counter: clamped, never down, per player, persisted in the player's own data
            if (Evolution.progress(a, ROUTE_ID) != 0 || Evolution.isEvolved(a, r)) bad.add("a fresh player has progress");
            if (Evolution.bump(a, ROUTE_ID, 3, 1) != 1) bad.add("one kill is not progress 1");
            if (Evolution.bump(a, ROUTE_ID, 3, 1_000_000_000L) != 3) bad.add("a huge delta is not clamped to the count");
            if (Evolution.bump(a, ROUTE_ID, 3, -5) != 3) bad.add("a negative delta lowered the counter");
            if (!Evolution.isEvolved(a, r)) bad.add("progress 3 of 3 is not evolved");
            if (LineState.getInt(a, LineState.EVO + ROUTE_ID) != 3) bad.add("the counter is not in the player's own data");
            if (Evolution.progress(b, ROUTE_ID) != 0) bad.add("the counter of one player leaked to another");
            said.append("counter clamped, per player; ");

            // 2. the Ultimate gate
            LineState.remove(a, LineState.EVO + ROUTE_ID);
            if (Evolution.gate(a, r, ultimate, 1.0) != EvolutionRules.Gate.NOT_EVOLVED) bad.add("the Ultimate opened before the evolution");
            Evolution.bump(a, ROUTE_ID, 3, 3);
            if (Evolution.gate(a, r, ultimate, 0.5) != EvolutionRules.Gate.NOT_MASTERED) bad.add("an evolved but unmastered skill could cast the Ultimate");
            if (Evolution.gate(a, r, ultimate, 1.0) != EvolutionRules.Gate.OPEN) bad.add("an evolved and mastered skill could not cast the Ultimate");
            LineState.remove(a, LineState.EVO + ROUTE_ID);
            if (Evolution.gate(a, r, art, 0.0) != EvolutionRules.Gate.OPEN) bad.add("an ordinary art was gated");
            said.append("gate not-evolved / needs-mastery / open; ");

            // 3. the evolved passive
            PassiveCtx plain = new PassiveCtx((net.minecraft.server.level.ServerPlayer) a, r, passive, 1, false);
            PassiveCtx grown = new PassiveCtx((net.minecraft.server.level.ServerPlayer) a, r, passive, 1, true);
            if (plain.evolved || Math.abs(plain.num("store_pct") - 0.5) > 1e-9) bad.add("an unevolved passive does not use its own numbers");
            if (!grown.evolved || Math.abs(grown.num("store_pct") - 0.6) > 1e-9 || grown.level != 3) bad.add("an evolved passive does not use its evolved numbers at its top level");
            if (Math.abs(grown.num("max_pct") - 0.5) > 1e-9) bad.add("an evolved passive lost a number its block did not override");
            said.append("evolved numbers; ");

            // 4. a new body keeps the progress
            Evolution.bump(a, ROUTE_ID, 3, 2);
            Evolution.onClone(a, b);
            if (Evolution.progress(b, ROUTE_ID) != 2) bad.add("the progress was not copied to the new body");
            said.append("copied on respawn; ");
        } finally {
            LineState.remove(a, LineState.EVO + ROUTE_ID);
            LineState.remove(b, LineState.EVO + ROUTE_ID);
        }
        if (!bad.isEmpty()) return fail(bad);

        said.append(hooks(level, a, bad));
        return bad.isEmpty() ? SelfTests.Result.ok(said.toString()) : fail(bad);
    }

    /** The real hooks on the first shipped line that has an evolution: they count while the skill is owned and on, only for foes, and stop when it is switched off. */
    private static String hooks(ServerLevel level, FakePlayer fp, List<String> bad) {
        SkillRoute r = null;
        for (SkillRoute x : RouteRegistry.all()) {
            if (x.isLine() && x.hasEvolution()) {
                r = x;
                break;
            }
        }
        if (r == null) return "no shipped line has an evolution yet (the hooks were not driven)";
        net.minecraft.server.level.ServerPlayer p = fp;
        if (UniqueBooks.grant(p, r.id(), false) != UniqueBooks.Grant.GRANTED) {
            UniqueBooks.forget(p, r.id());
            return "the stand-in cannot hold " + r.id() + " (the hooks were not driven)";
        }
        Zombie z = null;
        String req = r.evolution().requirement();
        String key = LineState.EVO + r.id();
        try {
            LineState.remove(p, key);
            LineState.remove(p, LineState.EVO_CARRY + r.id());
            z = EntityType.ZOMBIE.create(level);
            z.setPos(2.5, 100, 0.5);
            z.setNoAi(true);
            level.addFreshEntity(z);
            DamageSource src = p.damageSources().generic();
            int driven = drive(p, r, req, z, src);
            int on = Evolution.progress(p, r.id());
            if (driven > 0 && on <= 0) bad.add(r.id() + ": the " + req + " hook counted nothing while the skill is on");
            if (driven == 0 && on != 0) bad.add(r.id() + ": a zombie counted for " + req);
            if ("kills".equals(req) || "damage_dealt".equals(req)) {
                LineState.remove(p, key);
                LineState.remove(p, LineState.EVO_CARRY + r.id());
                Evolution.onKill(p, p);                                         // the player itself is no foe
                Evolution.onDealt(p, p, 5f);
                if (Evolution.progress(p, r.id()) != 0) bad.add("the player counted as their own foe");
            }
            ManasSkillInstance inst = UniqueBooks.held(p, r.id());
            if (inst != null) {
                inst.setToggled(false);
                LineState.remove(p, key);
                LineState.remove(p, LineState.EVO_CARRY + r.id());
                drive(p, r, req, z, src);
                if (Evolution.progress(p, r.id()) != 0) bad.add(r.id() + ": the " + req + " hook counted while the skill is switched off");
            }
            return req + " hook driven on " + r.id() + ", " + on + " counted, none while off";
        } finally {
            LineState.remove(p, key);
            LineState.remove(p, LineState.EVO_CARRY + r.id());
            LineState.remove(p, LineState.EVO_MIRROR + r.id());
            UniqueBooks.forget(p, r.id());
            if (z != null) z.discard();
        }
    }

    /** Calls the hook of one requirement kind with a plausible event; returns 1 when a call was made, 0 when the kind has no plain event here. */
    private static int drive(net.minecraft.server.level.ServerPlayer p, SkillRoute r, String req, Zombie z, DamageSource generic) {
        switch (req) {
            case "kills" -> Evolution.onKill(p, z);
            case "elite_kills", "boss_kills" -> {
                Evolution.onKill(p, z);                                          // a zombie is neither: it must not count
                return 0;
            }
            case "damage_dealt" -> Evolution.onDealt(p, z, 3f);
            case "damage_taken" -> {
                String kind = r.evolution().damageKindOrAny();
                DamageSource s = switch (kind) {
                    case "fire" -> p.damageSources().onFire();
                    case "magic" -> p.damageSources().magic();
                    case "fall" -> p.damageSources().fall();
                    case "freeze" -> p.damageSources().freeze();
                    case "wither" -> p.damageSources().wither();
                    case "any" -> generic;
                    default -> null;
                };
                if (s == null) return 0;
                Evolution.onTaken(p, s, 3f);
            }
            case "lethal_survived" -> {
                p.setHealth(1f);
                Evolution.onTaken(p, generic, 4f);
                p.setHealth(p.getMaxHealth());
            }
            case "arts_cast" -> {
                for (SkillNode n : r.nodes()) {
                    if (n.nodeType() == SkillNode.NodeType.ACTIVE && !n.ultimate()) {
                        Evolution.onArtCast(p, r, n);
                        return 1;
                    }
                }
                return 0;
            }
            case "ally_buffs" -> Evolution.onAllyBuffs(p, r, 2);
            default -> {
                return 0;
            }
        }
        return 1;
    }

    // ================================================================== the Debt
    private static SelfTests.Result debt(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        ServerLevel level = server.overworld();
        FakePlayer fp = SelfTests.standIn(level, "evolution_debt_tester", 0.5, 100, 0.5);
        net.minecraft.server.level.ServerPlayer p = fp;
        UUID owner = p.getUUID();
        DebtLedger ledger = Debts.ledger();
        SkillNode store = node("store", SkillNode.TYPE_PASSIVE, "debt_store", debtValues(0.0), false, EvolvedParams.NONE, 1);
        SkillNode burst = node("burst", SkillNode.TYPE_ACTIVE, "debt_burst",
                Map.of("damage", List.of(0.01), "mult", List.of(1.0), "radius", List.of(8.0), "targets", List.of(3.0)), false, EvolvedParams.NONE, 1);
        SkillRoute r = route(null, store, burst);
        Zombie z = null;
        StringBuilder said = new StringBuilder();
        try {
            ledger.forgetOwner(owner);
            z = EntityType.ZOMBIE.create(level);
            z.setPos(2.5, 100, 0.5);
            z.setNoAi(true);
            level.addFreshEntity(z);
            long now = level.getGameTime();
            double max = z.getMaxHealth();
            PassiveCtx pc = new PassiveCtx(p, r, store, 1);

            // store: a share of the blow, then held at the cap
            Debts.store(pc, z, 10f);
            double owed = ledger.debtOf(owner, z.getUUID(), now);
            if (Math.abs(owed - 5.0) > 1e-6) bad.add("a blow of 10 at 50 % stored " + owed + ", not 5");
            Debts.store(pc, z, 100f);
            owed = ledger.debtOf(owner, z.getUUID(), now);
            if (Math.abs(owed - 0.5 * max) > 1e-6) bad.add("the Debt is " + owed + ", not held at the cap of " + 0.5 * max);
            Debts.store(pc, p, 10f);
            if (ledger.size(owner) != 1) bad.add("a Debt was stored on the owner or twice on one foe (" + ledger.size(owner) + " entries)");
            said.append("stored 5, held at cap ").append(0.5 * max).append("; ");

            // at most 20 foes per player
            for (int i = 0; i < 25; i++) ledger.store(owner, UUID.randomUUID(), 10.0, 0.5, 0.5, 20.0, now, 200, 0);
            if (ledger.size(owner) > DebtLedger.MAX_FOES) bad.add("more than " + DebtLedger.MAX_FOES + " foes are tracked for one player: " + ledger.size(owner));
            said.append("tracked ").append(ledger.size(owner)).append(" of 25 (cap 20); ");
            ledger.forgetOwner(owner);
            Debts.store(pc, z, 100f);
            if (!ledger.has(owner, z.getUUID(), now)) bad.add("the Debt of the test foe was lost");

            // a Debt blow does not store more (the passives skip it) and does hurt
            z.invulnerableTime = 0;
            float before = z.getHealth();
            double owedBefore = ledger.debtOf(owner, z.getUUID(), now);
            if (!LineEvents.debtHurt(p, z, 1f)) bad.add("a Debt blow did not land");
            if (z.getHealth() >= before) bad.add("a Debt blow did no damage");
            if (Math.abs(ledger.debtOf(owner, z.getUUID(), now) - owedBefore) > 1e-9) bad.add("a Debt blow changed the Debt");

            // burst: one blow, base plus what was owed, and the Debt is consumed
            z.setHealth(z.getMaxHealth());
            z.invulnerableTime = 0;
            z.hurtTime = 0;
            owed = ledger.debtOf(owner, z.getUUID(), now);
            ActiveCtx ac = new ActiveCtx(p, r, burst, 1, 0.0);
            if (!Debts.canBurst(ac)) bad.add("a burst with a foe in range was refused");
            Debts.burst(ac);
            double lost = max - Math.max(0f, z.getHealth());
            if (z.isAlive() && lost + 1e-3 < Math.min(owed, max)) bad.add("the burst took " + lost + " health, less than the " + owed + " owed");
            if (ledger.debtOf(owner, z.getUUID(), now) > 0) bad.add("the Debt was not consumed by the burst");
            said.append("burst took ").append(Math.round(lost * 10) / 10.0).append(" for ").append(Math.round(owed * 10) / 10.0).append(" owed; ");

            // cleanup: the foe dies, the owner dies, the server stops
            ledger.store(owner, z.getUUID(), 10.0, 0.5, 0.5, 20.0, now, 200, 0);
            Debts.onDeath(z);
            if (ledger.has(owner, z.getUUID(), now)) bad.add("the Debt of a dead foe was kept");
            ledger.store(owner, UUID.randomUUID(), 10.0, 0.5, 0.5, 20.0, now, 200, 0);
            Debts.onDeath(p);
            if (ledger.hasOwner(owner)) bad.add("a dead owner still owns Debt");
            ledger.store(owner, UUID.randomUUID(), 10.0, 0.5, 0.5, 20.0, now, 200, 0);
            Debts.forgetOwner(p);
            if (ledger.hasOwner(owner)) bad.add("a logged-out owner still owns Debt");
            ledger.store(owner, UUID.randomUUID(), 10.0, 0.5, 0.5, 20.0, now, 200, 0);
            ledger.prune(owner, now + 10_000);
            if (ledger.hasOwner(owner)) bad.add("expired Debt was not pruned");
            said.append("forgotten on foe death, owner death, logout and expiry");
        } finally {
            ledger.forgetOwner(owner);
            if (z != null) z.discard();
        }
        return bad.isEmpty() ? SelfTests.Result.ok(said.toString()) : fail(bad);
    }

    private static SelfTests.Result fail(List<String> bad) {
        return SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(5, bad.size()))));
    }
}
