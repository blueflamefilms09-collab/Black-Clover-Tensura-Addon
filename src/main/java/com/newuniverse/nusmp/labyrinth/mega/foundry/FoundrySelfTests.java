package com.newuniverse.nusmp.labyrinth.mega.foundry;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Why: nothing here can be played by its author, so each promise of the Mob Foundry has a SELF-TEST that the CI server smoke test runs in the real game
 * ({@code /multiverse mega selftest}) and that fails loudly with the reason. {@code foundry.roster}: the roster loaded from the data files holds exactly 200 + 200 mobs, 40 per tier
 * per road, every ability and archetype named exists, the sources are spread ({@link FoundryVerify}); every id resolves (its entity key parses back to itself and its model's index
 * entry is named by the data). {@code foundry.scaling}: for every archetype the health, damage and armour rise with the tier and never fall inside a tier. {@code foundry.ai}: a
 * mob of every archetype of the library builds its goals, and every movement style the library uses has a goal on it. {@code foundry.spawn}: one mob of each tier on each road is made
 * in the Mega-Dungeon's level, comes out with its identity, road, the stats the tier asks for (monotone in tier), and is removed again. {@code foundry.encounters}: the draw of the
 * Dungeon Master's glue gives a mob of the asked road and tier, honours a wished faction, and falls back to a nearer tier when a tier is empty.
 */
public final class FoundrySelfTests {
    private FoundrySelfTests() {}

    public static void register() {
        SelfTests.register("foundry.roster", FoundrySelfTests::roster);
        SelfTests.register("foundry.scaling", s -> scaling());
        SelfTests.register("foundry.ai", FoundrySelfTests::ai);
        SelfTests.register("foundry.spawn", FoundrySelfTests::spawn);
        SelfTests.register("foundry.encounters", s -> encounters());
    }

    private static SelfTests.Result roster(MinecraftServer server) {
        FoundryRoster r = FoundryData.roster();
        if (r.size() == 0) return SelfTests.Result.fail("the foundry roster is empty: the data files were not read");
        List<String> bad = FoundryVerify.check(r, FoundryData.library(), true);
        if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " problems, first: " + bad.get(0));
        for (FoundryDef d : r.all()) {
            if (!d.id().equals(FoundryDef.idOfKey(d.entityKey()))) return SelfTests.Result.fail("the id " + d.id() + " does not survive its entity key " + d.entityKey());
            if (r.byId(d.id()) != d) return SelfTests.Result.fail("the id " + d.id() + " does not resolve to its definition");
        }
        return SelfTests.Result.ok(r.size() + " mobs (" + r.count(Polarity.HOLY) + " holy, " + r.count(Polarity.ABYSSAL) + " abyssal), " + FoundryVerify.sourceCount(r) + " sources, "
                + FoundryData.library().abilities().size() + " abilities");
    }

    private static SelfTests.Result scaling() {
        for (FoundryArchetype a : FoundryData.library().archetypes().values()) {
            double h = 0, d = 0, ar = -1;
            for (int tier = 1; tier <= FoundryScaling.TIERS; tier++) {
                int first = (tier - 1) * FoundryScaling.FLOORS_PER_TIER + 1, last = tier * FoundryScaling.FLOORS_PER_TIER;
                double h1 = FoundryScaling.health(a, tier, first), h2 = FoundryScaling.health(a, tier, last);
                double d1 = FoundryScaling.damage(a, tier, first), d2 = FoundryScaling.damage(a, tier, last);
                if (h2 < h1 || d2 < d1) return SelfTests.Result.fail(a.id() + ": tier " + tier + " gets weaker with depth");
                if (h1 <= h || d1 <= d) return SelfTests.Result.fail(a.id() + ": tier " + tier + " is not stronger than the tier above it in health or damage");
                if (FoundryScaling.armor(a, tier) < ar) return SelfTests.Result.fail(a.id() + ": armour falls at tier " + tier);
                h = h2;
                d = d2;
                ar = FoundryScaling.armor(a, tier);
            }
        }
        return SelfTests.Result.ok("health, damage and armour rise with the tier for all " + FoundryData.library().archetypes().size() + " archetypes");
    }

    private static SelfTests.Result ai(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return SelfTests.Result.fail("the Mega-Dungeon dimension is not loaded");
        FoundryRoster r = FoundryData.roster();
        List<String> done = new ArrayList<>();
        for (FoundryArchetype a : FoundryData.library().archetypes().values()) {
            FoundryDef d = null;
            for (FoundryDef x : r.all()) if (x.archetype().equals(a.id())) {
                d = x;
                break;
            }
            if (d == null) continue;                     // an archetype no mob uses has nothing to build
            FoundryMobEntity m = FoundryRegistry.MOB.get().create(level);
            if (m == null) return SelfTests.Result.fail("the entity type does not create");
            try {
                m.setFoundry(d, 0);
                if (m.style() != a.style()) return SelfTests.Result.fail(a.id() + ": the mob's style is " + m.style() + ", the archetype says " + a.style());
                Set<FoundryArchetype.Style> have = EnumSet.noneOf(FoundryArchetype.Style.class);
                boolean ability = false;
                for (WrappedGoal g : m.goals()) {
                    FoundryArchetype.Style st = FoundryGoals.styleOf(g.getGoal());
                    if (st != null) have.add(st);
                    if (g.getGoal() instanceof FoundryGoals.AbilityGoal) ability = true;
                }
                if (!have.contains(a.style())) return SelfTests.Result.fail(a.id() + ": no goal for the style " + a.style());
                if (!ability) return SelfTests.Result.fail(a.id() + ": no ability goal");
                if (m.brain().abilities().isEmpty()) return SelfTests.Result.fail(a.id() + ": the mob has no abilities");
                done.add(a.id());
            } finally {
                m.discard();
            }
        }
        return SelfTests.Result.ok("the goals of " + done.size() + " archetypes build: " + done);
    }

    private static SelfTests.Result spawn(MinecraftServer server) {
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return SelfTests.Result.fail("the Mega-Dungeon dimension is not loaded");
        FoundryRoster r = FoundryData.roster();
        List<FoundryMobEntity> made = new ArrayList<>();
        try {
            for (Polarity road : Polarity.values()) {
                for (int tier = 1; tier <= FoundryScaling.TIERS; tier++) {
                    List<FoundryDef> pool = r.of(road, tier);
                    if (pool.isEmpty()) return SelfTests.Result.fail("no mob on the " + road.id() + " road at tier " + tier);
                    FoundryDef d = pool.get(0);
                    FoundryMobEntity m = FoundryRegistry.MOB.get().create(level);
                    if (m == null) return SelfTests.Result.fail("the entity type does not create");
                    m.moveTo(0.5, 200.0, 0.5 + 4 * tier, 0f, 0f);
                    int floor = road.sign() * ((tier - 1) * FoundryScaling.FLOORS_PER_TIER + 1);
                    m.setFoundry(d, floor);
                    made.add(m);
                    if (!d.id().equals(m.foundryId())) return SelfTests.Result.fail(d.id() + ": the identity is '" + m.foundryId() + "'");
                    if (m.road() != road || m.tier() != tier) return SelfTests.Result.fail(d.id() + ": road or tier lost (" + m.road() + ", " + m.tier() + ")");
                    if (m.getMaxHealth() <= 0f || m.getHealth() != m.getMaxHealth()) return SelfTests.Result.fail(d.id() + ": health " + m.getHealth() + " of " + m.getMaxHealth());
                    FoundryArchetype a = FoundryData.archetypeOf(d);
                    double want = FoundryScaling.health(a, tier, floor) * FoundryConfig.get(FoundryConfig.HEALTH_MULT);
                    if (Math.abs(m.getMaxHealth() - want) > 0.01 * Math.max(1.0, want)) return SelfTests.Result.fail(d.id() + ": max health " + m.getMaxHealth() + ", the tier asks for " + want);
                    CompoundCheck.roundTrip(m, level, d);
                }
            }
            // the same archetype gets stronger with the tier: compare the first mob of each road/tier that shares an archetype with tier 1
            for (Polarity road : Polarity.values()) {
                double prev = 0.0;
                for (int tier = 1; tier <= FoundryScaling.TIERS; tier++) {
                    FoundryMobEntity m = FoundryRegistry.MOB.get().create(level);
                    if (m == null) return SelfTests.Result.fail("the entity type does not create");
                    made.add(m);
                    m.setFoundry(r.of(road, tier).get(0), road.sign() * ((tier - 1) * FoundryScaling.FLOORS_PER_TIER + 1));
                    FoundryArchetype a = m.archetype();
                    double h = FoundryScaling.health(a, tier, 0) / Math.max(0.1, a.health());
                    if (h <= prev) return SelfTests.Result.fail(road.id() + ": the base health of tier " + tier + " (" + h + ") is not above tier " + (tier - 1));
                    prev = h;
                }
            }
        } finally {
            for (FoundryMobEntity m : made) m.discard();
        }
        return SelfTests.Result.ok("one mob of each tier on each road was made with its identity, road and the stats of its tier");
    }

    /** The save round trip of one mob: what is written comes back as the same mob (identity, floor), without the test needing a world file. */
    private static final class CompoundCheck {
        static void roundTrip(FoundryMobEntity m, ServerLevel level, FoundryDef d) {
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            m.addAdditionalSaveData(tag);
            FoundryMobEntity copy = FoundryRegistry.MOB.get().create(level);
            if (copy == null) throw new IllegalStateException("the entity type does not create");
            try {
                copy.readAdditionalSaveData(tag);
                if (!d.id().equals(copy.foundryId())) throw new IllegalStateException(d.id() + " came back as '" + copy.foundryId() + "' from its save data");
                if (copy.floor() != m.floor()) throw new IllegalStateException(d.id() + " lost its floor in the save data");
            } finally {
                copy.discard();
            }
        }
    }

    private static SelfTests.Result encounters() {
        FoundryRoster r = FoundryData.roster();
        Random rnd = new Random(7);
        int draws = 0;
        for (Polarity road : Polarity.values()) {
            for (int tier = 1; tier <= FoundryScaling.TIERS; tier++) {
                for (int i = 0; i < 40; i++) {
                    FoundryDef d = r.pick(road, tier, null, null, rnd);
                    if (d == null || d.road() != road || d.tier() != tier) return SelfTests.Result.fail("a draw for " + road.id() + " tier " + tier + " gave " + (d == null ? "nothing" : d.id() + " of " + d.road().id() + " tier " + d.tier()));
                    draws++;
                }
                int wished = 0;
                for (int i = 0; i < 200; i++) {
                    FoundryDef d = r.pick(road, tier, "undead", null, rnd);
                    if (d != null && d.faction().equals("undead")) wished++;
                }
                boolean any = false;
                for (FoundryDef d : r.of(road, tier)) any |= d.faction().equals("undead");
                if (any && wished == 0) return SelfTests.Result.fail("a wished faction was never drawn for " + road.id() + " tier " + tier);
            }
        }
        FoundryRoster two = new FoundryRoster(r.of(Polarity.HOLY, 1));
        if (two.pick(Polarity.HOLY, 4, null, null, rnd) == null) return SelfTests.Result.fail("an empty tier did not fall back to a nearer one");
        if (two.pick(Polarity.ABYSSAL, 1, null, null, rnd) != null) return SelfTests.Result.fail("a road with no mobs gave a mob");
        return SelfTests.Result.ok(draws + " draws gave the asked road and tier; a wished faction is honoured; an empty tier falls back");
    }
}
