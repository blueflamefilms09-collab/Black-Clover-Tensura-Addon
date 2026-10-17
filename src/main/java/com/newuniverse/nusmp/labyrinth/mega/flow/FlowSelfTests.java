package com.newuniverse.nusmp.labyrinth.mega.flow;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.order.UniqueLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Why: nothing here can be played by its author, so the Rukh flow has SELF-TESTS that the CI server smoke test runs in the real game
 * ({@code /multiverse mega selftest}). They drive the same public entry points the event handlers use, with a {@code FakePlayer} for the caster, and they
 * clean up after themselves (the tracker is emptied for the test player, the health is restored, no entity is added to a level, and no vessel is ever
 * issued). {@code flow.classify}: the data reload installed the table and it classifies known ids as the doc says, and the real list of magic families splits
 * into light, dark and the neutral rest. {@code flow.multiplier}: with no cast, a stale cast or a neutral cast a hit is unchanged; with a cast that matches
 * the flow it is multiplied by the configured factor; an opposing cast is a rejection; an ally is never boosted; and, when sector 0 exists and is on the
 * Holy road, the real cast hook remembers the cast, a matching cast costs nothing, an opposing cast takes the configured share of health exactly once (the cooldown
 * holds), and at 1.5 HP it never kills. {@code flow.neutral_outside}: no road, the Abyssal road at its configured flow, the overworld and the Hub have the flow
 * the rules say (none by default) and leave damage as it was. {@code flow.vessel}: the list of Metal Vessel candidates is not empty and every one can carry a
 * Djinn; the free ones are exactly those the Order's ledger does not hold; the draw is valid at both ends of the roll. {@code flow.reward_log}: the
 * once-per-sector memory survives a save and a load, and the reward's gate answers for made-up sectors (a Holy decennial one, an Abyssal one, none)
 * without touching saved data. A test whose switch is off in the config reports an OK "skipped", never a failure.
 */
public final class FlowSelfTests {
    private FlowSelfTests() {}

    private static final float BASE = 10f;

    /** Registers the tests; called once from {@code FlowRegistry.init}. */
    public static void register() {
        SelfTests.register("flow.classify", FlowSelfTests::classify);
        SelfTests.register("flow.multiplier", FlowSelfTests::multiplier);
        SelfTests.register("flow.neutral_outside", FlowSelfTests::neutralOutside);
        SelfTests.register("flow.vessel", FlowSelfTests::vessel);
        SelfTests.register("flow.reward_log", FlowSelfTests::rewardLog);
    }

    private static SelfTests.Result ok(String detail) { return SelfTests.Result.ok(detail); }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static String firstFew(List<String> problems) {
        return problems.size() + " problem(s): " + String.join(" | ", problems.subList(0, Math.min(5, problems.size())));
    }

    private static boolean near(float a, float b) { return Math.abs(a - b) < 0.01f; }

    // ------------------------------------------------------------------------------------------------ flow.classify
    private static void expect(List<String> bad, MagicAlignments t, Alignment want, String... ids) {
        Alignment got = t.classify(ids);
        if (got != want) bad.add(String.join("+", ids) + " is " + got + " but should be " + want + " (" + t.explain(ids).rule() + ")");
    }

    private static SelfTests.Result classify(MinecraftServer server) {
        MagicAlignments t = AlignmentRegistry.current();
        if (t.isEmpty()) return fail("the classification table is empty: data/nusmp/mega/flow/alignments.json was not loaded by the data reload (is the reload listener registered?)");
        List<String> bad = new ArrayList<>();
        expect(bad, t, Alignment.LIGHT, "light");
        expect(bad, t, Alignment.LIGHT, "judgment", "light");
        expect(bad, t, Alignment.LIGHT, "lightning");
        expect(bad, t, Alignment.LIGHT, "tensura:solar_beam");
        expect(bad, t, Alignment.DARK, "dark");
        expect(bad, t, Alignment.DARK, "demon_fire");
        expect(bad, t, Alignment.DARK, "devil_union", "light");
        expect(bad, t, Alignment.DARK, "tensura:dark_cube");
        expect(bad, t, Alignment.DARK, "fallen_angel_gate", "spatial");
        expect(bad, t, Alignment.NEUTRAL, "flame");
        expect(bad, t, Alignment.NEUTRAL, "curse_cleanse", "curse_warding");
        expect(bad, t, Alignment.NEUTRAL, "tensura:lighten");
        int light = 0, dark = 0, neutral = 0;
        for (MagicType m : MagicType.values()) {
            switch (t.classify(m.name().toLowerCase(Locale.ROOT))) {
                case LIGHT -> light++;
                case DARK -> dark++;
                default -> neutral++;
            }
        }
        if (light < 1 || dark < 10) bad.add("the " + MagicType.values().length + " magic families split " + light + " light / " + dark + " dark / " + neutral + " neutral: expected at least 1 light and 10 dark");
        return bad.isEmpty() ? ok(t.lightKeywords().size() + " light words, " + t.darkKeywords().size() + " dark words, " + t.exactEntries().size() + " exact entries; the "
                + MagicType.values().length + " magic families split " + light + " light / " + dark + " dark / " + neutral + " neutral") : fail(firstFew(bad));
    }

    // ------------------------------------------------------------------------------------------------ flow.multiplier
    private static String[] castIds(Alignment a) { return a == Alignment.DARK ? new String[] {"black_hole", "dark"} : new String[] {"judgment", "light"}; }

    /** The cell the test player stands in inside sector s: its arrival marker, else the middle of its first room, else its entry. */
    private static BlockPos standing(Sector s) {
        if (s.arrival != null) return s.arrival;
        if (!s.rooms.isEmpty()) return s.rooms.get(0).center();
        return s.entry;
    }

    private static SelfTests.Result multiplier(MinecraftServer server) {
        if (!FlowConfig.get(FlowConfig.ENABLED)) return ok("skipped: flow.enabled is false in the config");
        Alignment holy = FlowConfig.holyFlow();
        if (holy == Alignment.NEUTRAL) return ok("skipped: flow.holyFlow is none in the config");
        if (AlignmentRegistry.current().isEmpty()) return fail("the classification table is empty (alignments.json was not loaded)");
        ServerLevel mega = SelfTests.mega(server);
        Sector s0 = MegaData.get(server).sector(0);
        boolean inSector = mega != null && s0 != null && s0.polarity == Polarity.HOLY;
        ServerLevel level = mega != null ? mega : server.overworld();
        BlockPos at = inSector ? standing(s0) : new BlockPos(0, 100, 0);
        FakePlayer p = SelfTests.player(level, "flow_tester_mult", at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        Zombie target = EntityType.ZOMBIE.create(level);
        if (target == null) return fail("a test mob could not be created");
        target.setPos(at.getX() + 3.5, at.getY(), at.getZ() + 0.5);
        FlowTracker tracker = FlowTracker.shared();
        UUID id = p.getUUID();
        List<String> bad = new ArrayList<>();
        int window = FlowConfig.get(FlowConfig.CAST_WINDOW_TICKS);
        double m = FlowRules.clampMultiplier(FlowConfig.get(FlowConfig.HOLY_MULTIPLIER));
        Alignment opposed = holy.opposite();
        boolean cancels = FlowConfig.get(FlowConfig.REJECT_CANCELS_DAMAGE);
        StringBuilder detail = new StringBuilder();
        try {
            tracker.forget(id);
            long now = level.getGameTime();

            FlowEvents.Result r = FlowEvents.adjust(p, target, BASE, Polarity.HOLY);
            if (r.outcome() != FlowRules.Outcome.NONE || !near(r.amount(), BASE) || r.cancel()) bad.add("with no cast a hit changed: " + r);

            tracker.recordCast(id, holy, now);
            r = FlowEvents.adjust(p, target, BASE, Polarity.HOLY);
            if (r.outcome() != FlowRules.Outcome.BOOST || !near(r.amount(), (float) (BASE * m)) || r.cancel()) bad.add("a " + holy + " cast on the " + holy + " road should give x" + m + " = " + (BASE * m) + " but gave " + r);
            detail.append("a ").append(holy.id()).append(" cast made ").append((int) BASE).append(" into ").append(r.amount()).append("; ");

            tracker.recordCast(id, holy, now - window - 1);
            r = FlowEvents.adjust(p, target, BASE, Polarity.HOLY);
            if (r.outcome() != FlowRules.Outcome.NONE || !near(r.amount(), BASE)) bad.add("a cast older than the " + window + "-tick window still boosted: " + r);

            tracker.recordCast(id, Alignment.NEUTRAL, now);
            r = FlowEvents.adjust(p, target, BASE, Polarity.HOLY);
            if (r.outcome() != FlowRules.Outcome.NONE || !near(r.amount(), BASE)) bad.add("a neutral cast changed a hit: " + r);

            tracker.recordCast(id, opposed, now);
            r = FlowEvents.adjust(p, target, BASE, Polarity.HOLY);
            if (r.outcome() != FlowRules.Outcome.REJECT || r.cancel() != cancels || (!cancels && !near(r.amount(), BASE))) bad.add("a " + opposed + " cast on the " + holy + " road should be a rejection (cancel=" + cancels + ") but gave " + r);

            tracker.recordCast(id, holy, now);
            r = FlowEvents.adjust(p, p, BASE, Polarity.HOLY);
            if (r.outcome() != FlowRules.Outcome.NONE || !near(r.amount(), BASE)) bad.add("a hit on the caster themselves was boosted: " + r);
            detail.append("no cast, a stale cast, a neutral cast and a hit on oneself changed nothing; an opposing cast was a rejection; ");

            if (!inSector) {
                detail.append("the real cast hook was not tried: sector 0 is not generated yet or is not on the Holy road");
            } else {
                Polarity here = MegaDungeon.polarityAt(level, FlowEvents.posOf(p));
                if (here != Polarity.HOLY) {
                    bad.add("the test player stands at " + FlowEvents.posOf(p) + ", inside sector 0, but MegaDungeon.polarityAt says " + here);
                } else {
                    hooks(p, target, tracker, id, level, holy, opposed, window, bad, detail);
                }
            }
        } finally {
            tracker.forget(id);
            p.setHealth(p.getMaxHealth());
        }
        return bad.isEmpty() ? ok(detail.toString()) : fail(firstFew(bad));
    }

    /** The real cast hook in a Holy sector: remembers, charges a matching cast nothing, takes the share of an opposing one once, and never kills. */
    private static void hooks(FakePlayer p, Zombie target, FlowTracker tracker, UUID id, ServerLevel level, Alignment holy, Alignment opposed, int window, List<String> bad,
                              StringBuilder detail) {
        tracker.forget(id);
        p.setHealth(p.getMaxHealth());
        long now = level.getGameTime();
        float full = p.getHealth();
        String[] light = castIds(holy);
        FlowCasts.beforeCast(p, light[1], light[0]);                                         // a spell about to run: its instant damage is this tick's
        FlowEvents.Result running = FlowEvents.adjust(p, target, BASE, Polarity.HOLY);
        if (running.outcome() != FlowRules.Outcome.BOOST) bad.add("a " + holy + " spell about to run did not colour the damage of its own tick: " + running);
        if (tracker.lastCast(id) != null) bad.add("beforeCast opened a cast window although the spell had not succeeded");
        tracker.forget(id);
        FlowCasts.onCastIds(p, castIds(holy));
        if (tracker.recentCast(id, now, window) != holy) bad.add("the cast hook did not remember a " + holy + " cast made with " + String.join("+", castIds(holy)));
        if (p.getHealth() != full) bad.add("a cast that matches the flow cost health: " + full + " -> " + p.getHealth());

        FlowCasts.onCastIds(p, castIds(opposed));
        float after = p.getHealth();
        if (tracker.recentCast(id, now, window) != opposed) bad.add("the cast hook did not remember the " + opposed + " cast");
        if (!FlowConfig.get(FlowConfig.NON_LETHAL)) {
            detail.append("nonLethal is off, so the drain itself was not asserted (the hook ran)");
            return;
        }
        double want = FlowRules.rejectionDrain(p.getMaxHealth(), full, FlowConfig.get(FlowConfig.REJECT_PERCENT), FlowConfig.get(FlowConfig.REJECT_MIN_HEALTH), true);
        if (want > 0.0 && !near(full - after, (float) want)) bad.add("an opposing cast should have taken " + want + " HP but took " + (full - after));
        FlowCasts.onCastIds(p, castIds(opposed));
        if (FlowConfig.get(FlowConfig.REJECT_COOLDOWN_TICKS) > 0 && p.getHealth() != after) bad.add("a second rejection inside the cooldown took " + (after - p.getHealth()) + " HP more");

        tracker.forget(id);
        p.setHealth(1.5f);
        FlowCasts.onCastIds(p, castIds(opposed));
        if (p.getHealth() < 1.0f - 0.001f || !p.isAlive()) bad.add("a rejection at 1.5 HP left " + p.getHealth() + " HP (alive " + p.isAlive() + "): nonLethal did not hold");

        tracker.forget(id);
        p.setHealth(p.getMaxHealth());
        FlowCasts.onCastIds(p, "flame_bullet", "flame");
        if (p.getHealth() != p.getMaxHealth()) bad.add("a neutral cast cost health");
        if (tracker.recentCast(id, now, window) != Alignment.NEUTRAL) bad.add("the cast hook did not remember a neutral cast");
        detail.append("in sector 0 the hook remembered the casts; a matching cast was free, an opposing one took ").append(String.format(Locale.ROOT, "%.2f", full - after))
                .append(" HP once (the cooldown held) and never the last 1 HP");
    }

    // ------------------------------------------------------------------------------------------------ flow.neutral_outside
    private static SelfTests.Result neutralOutside(MinecraftServer server) {
        if (!FlowConfig.get(FlowConfig.ENABLED)) return ok("skipped: flow.enabled is false in the config");
        ServerLevel over = server.overworld();
        FakePlayer p = SelfTests.player(over, "flow_tester_out", 0.5, 100.0, 0.5);
        Zombie target = EntityType.ZOMBIE.create(over);
        if (target == null) return fail("a test mob could not be created");
        FlowTracker tracker = FlowTracker.shared();
        UUID id = p.getUUID();
        List<String> bad = new ArrayList<>();
        StringBuilder detail = new StringBuilder();
        try {
            tracker.forget(id);
            tracker.recordCast(id, Alignment.LIGHT, over.getGameTime());          // a perfect Light cast, fresh: it would be boosted wherever the Light flow is
            FlowEvents.Result r = FlowEvents.adjust(p, target, BASE, null);
            if (r.outcome() != FlowRules.Outcome.NONE || !near(r.amount(), BASE)) bad.add("with no road (Floor 0 or outside the dungeon) a fresh Light cast still changed a hit: " + r);

            Alignment abyssal = FlowConfig.abyssalFlow();
            r = FlowEvents.adjust(p, target, BASE, Polarity.ABYSSAL);
            FlowRules.Outcome want = FlowRules.outcome(abyssal, Alignment.LIGHT);
            if (r.outcome() != want) bad.add("on the Abyssal road (flow " + abyssal + ") a Light cast should be " + want + " but gave " + r);
            if (abyssal == Alignment.NEUTRAL && !near(r.amount(), BASE)) bad.add("the Abyssal road is neutral but a hit changed: " + r);
            detail.append("no road and the Abyssal road (flow ").append(abyssal.id()).append(") answer as the rules say; ");

            if (FlowEvents.flowAt(over, FlowEvents.posOf(p)) != Alignment.NEUTRAL) bad.add("the overworld has a flow");
            ServerLevel hub = server.getLevel(Labyrinth.SANCTUARY_DIMENSION);
            if (hub != null) {
                if (FlowEvents.flowAt(hub, new BlockPos(0, 69, 0)) != Alignment.NEUTRAL) bad.add("Floor 0 (the Hub) has a flow");
                detail.append("Floor 0 has no flow; ");
            } else {
                detail.append("Floor 0 is not loaded here; ");
            }
            Sector tail = MegaData.get(server).chainTail(Polarity.ABYSSAL);
            ServerLevel mega = SelfTests.mega(server);
            if (tail != null && mega != null) {
                Alignment inside = FlowEvents.flowAt(mega, standing(tail));
                if (inside != abyssal) bad.add("inside Abyssal sector " + tail.id + " the flow is " + inside + " but the config says " + abyssal);
                detail.append("the Abyssal sector has flow ").append(inside.id());
            } else {
                detail.append("no Abyssal sector exists yet");
            }
        } finally {
            tracker.forget(id);
        }
        return bad.isEmpty() ? ok(detail.toString()) : fail(firstFew(bad));
    }

    // ------------------------------------------------------------------------------------------------ flow.vessel
    private static SelfTests.Result vessel(MinecraftServer server) {
        List<RelicDef> all = DecennialReward.candidates();
        if (all.isEmpty()) return fail("no relic can be handed out as a Metal Vessel: the catalog holds " + Relics.all().size() + " relics and none is a unique, Djinn-capable vessel");
        List<String> bad = new ArrayList<>();
        for (RelicDef d : all) {
            if (!d.grade().djinnCapable()) bad.add(d.id() + " is " + d.grade() + ", which cannot carry a Djinn");
            if (!d.isVessel()) bad.add(d.id() + " is not flagged as a vessel");
            if (!d.isUnique()) bad.add(d.id() + " is a consumable, not a one-of-a-kind relic");
            if (d.isPairHalf()) bad.add(d.id() + " is half of a pair");
        }
        Set<String> alive = UniqueLedger.liveIds(server);
        List<RelicDef> free = DecennialReward.free(server);
        int held = 0;
        for (RelicDef d : all) if (alive.contains(d.id())) held++;
        for (RelicDef d : free) if (alive.contains(d.id())) bad.add(d.id() + " is listed as free but the Order's ledger holds a live copy");
        if (free.size() + held != all.size()) bad.add("free (" + free.size() + ") + in the world (" + held + ") should be all " + all.size());
        List<VesselRules.Candidate> weights = DecennialReward.weights(all);
        for (VesselRules.Candidate c : weights) if (!(c.weight() > 0.0)) bad.add(c.id() + " has the draw weight " + c.weight());
        int first = VesselRules.pick(weights, 0.0), last = VesselRules.pick(weights, 0.9999999);
        if (first < 0 || last < 0 || first >= all.size() || last >= all.size()) bad.add("the draw returned " + first + " / " + last + " for a pool of " + all.size());
        return bad.isEmpty() ? ok(all.size() + " relics can be a Metal Vessel, all Djinn-capable and one of a kind; " + free.size() + " are free, " + held + " are already in the world; nothing was issued")
                : fail(firstFew(bad));
    }

    // ------------------------------------------------------------------------------------------------ flow.reward_log
    private static SelfTests.Result rewardLog(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        FlowData d = new FlowData();                                                       // a fresh object: the stored one is never touched
        if (!d.claim(3, "godking_crown")) bad.add("the first claim of a sector was refused");
        if (d.claim(3, "hunger")) bad.add("a sector paid twice");
        if (!d.claim(7, "")) bad.add("the first claim of another sector was refused");
        CompoundTag tag = d.save(new CompoundTag(), server.registryAccess());
        FlowData back = FlowData.load(tag, server.registryAccess());
        if (!back.rewarded(3) || !back.rewarded(7) || back.rewarded(4)) bad.add("the memory did not survive a save and a load: 3=" + back.rewarded(3) + " 7=" + back.rewarded(7) + " 4=" + back.rewarded(4));
        if (!"godking_crown".equals(back.rewards().relicOf(3))) bad.add("the relic of sector 3 was " + back.rewards().relicOf(3) + " after a save and a load");
        FlowData real = FlowData.get(server);                                              // the stored data must at least be readable
        Sector holy = new Sector(987_654);                                                 // made up, never added to MegaData
        holy.polarity = Polarity.HOLY;
        Sector abyssal = new Sector(987_655);
        abyssal.polarity = Polarity.ABYSSAL;
        boolean enabled = FlowConfig.get(FlowConfig.ENABLED) && FlowConfig.get(FlowConfig.VESSEL_ENABLED);
        DecennialReward.Status forHoly = DecennialReward.gate(holy, server), forAbyssal = DecennialReward.gate(abyssal, server), forNone = DecennialReward.gate(null, server);
        if (enabled) {
            if (forHoly != null) bad.add("a made-up Holy sector (floors +1..+10) should be allowed to pay but the gate said " + forHoly);
            if (forAbyssal != DecennialReward.Status.NOT_HOLY) bad.add("an Abyssal sector should be refused as NOT_HOLY but the gate said " + forAbyssal);
            if (forNone != DecennialReward.Status.NO_SECTOR) bad.add("no sector should be refused as NO_SECTOR but the gate said " + forNone);
        } else if (forHoly != DecennialReward.Status.DISABLED) {
            bad.add("the switch is off, so the gate should say DISABLED but said " + forHoly);
        }
        return bad.isEmpty() ? ok("a sector pays once, the memory round-trips through NBT, " + real.rewards().size() + " sector(s) have paid in this world; the gate answers "
                + (forHoly == null ? "OPEN" : forHoly) + " for a Holy decennial sector, " + forAbyssal + " for an Abyssal one and " + forNone + " for none")
                : fail(firstFew(bad));
    }
}
