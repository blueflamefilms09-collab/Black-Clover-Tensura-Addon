package com.newuniverse.nusmp.elite.nations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.nations.contract.Bounty;
import com.newuniverse.nusmp.elite.nations.contract.Contract;
import com.newuniverse.nusmp.elite.nations.contract.ContractRules;
import com.newuniverse.nusmp.elite.nations.contract.ContractTemplate;
import com.newuniverse.nusmp.elite.nations.contract.ContractType;
import com.newuniverse.nusmp.elite.nations.contract.Reward;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/** Why: contract progress is driven by game events and pays out coins; matching, progress, delivery, tiers, board picks, bounties and tax are checked. */
class ContractTest {
    private static final UUID P = UUID.nameUUIDFromBytes("p".getBytes());
    private static final Predicate<String> TAGS = tag -> tag.equals("minecraft:logs") ? false : false;
    private static final Predicate<String> ZOMBIES = tag -> tag.equals("test:undead");

    private static ContractTemplate tpl(String id, ContractType type, String target, int count, int tier, int weight, int minLevel) {
        return new ContractTemplate(id, type, target, count, tier, "", "", ContractRules.defaultReward(tier, count), weight, minLevel, 0);
    }

    private static Contract taken(ContractType type, String target, int goal) {
        Contract c = Contract.from(1, tpl("t", type, target, goal, 1, 10, 1), 0);
        c.state = Contract.State.TAKEN;
        c.taker = P;
        return c;
    }

    @Test
    void matchingRules() {
        assertTrue(ContractRules.matches("", "minecraft:zombie", null));
        assertTrue(ContractRules.matches(null, "minecraft:zombie", null));
        assertTrue(ContractRules.matches("minecraft:zombie", "minecraft:zombie", null));
        assertFalse(ContractRules.matches("minecraft:zombie", "minecraft:husk", null));
        assertFalse(ContractRules.matches("minecraft:zombie", null, null));
        assertTrue(ContractRules.matches("#test:undead", "whatever", ZOMBIES));
        assertFalse(ContractRules.matches("#test:other", "whatever", ZOMBIES));
        assertFalse(ContractRules.matches("#test:undead", "whatever", null));
    }

    @Test
    void killProgressStopsAtGoal() {
        Contract c = taken(ContractType.KILL_MOB, "minecraft:zombie", 3);
        assertEquals(1, ContractRules.advance(c, ContractType.KILL_MOB, "minecraft:zombie", 1, TAGS));
        assertEquals(0, ContractRules.advance(c, ContractType.KILL_MOB, "minecraft:husk", 1, TAGS));
        assertEquals(0, ContractRules.advance(c, ContractType.MINE_BLOCK, "minecraft:zombie", 1, TAGS), "other event type");
        assertEquals(2, ContractRules.advance(c, ContractType.KILL_MOB, "minecraft:zombie", 5, TAGS), "clamped to the goal");
        assertTrue(c.complete());
        assertEquals(0, ContractRules.advance(c, ContractType.KILL_MOB, "minecraft:zombie", 1, TAGS));
    }

    @Test
    void onlyTakenContractsProgress() {
        Contract c = Contract.from(1, tpl("t", ContractType.MINE_BLOCK, "", 5, 1, 10, 1), 0);
        assertEquals(0, ContractRules.advance(c, ContractType.MINE_BLOCK, "minecraft:stone", 1, TAGS), "open, not taken");
        c.state = Contract.State.TAKEN;
        assertEquals(1, ContractRules.advance(c, ContractType.MINE_BLOCK, "minecraft:stone", 1, TAGS));
        assertEquals(0, ContractRules.advance(c, ContractType.MINE_BLOCK, "minecraft:stone", 0, TAGS));
        assertEquals(0, ContractRules.advance(null, ContractType.MINE_BLOCK, "x", 1, TAGS));
    }

    @Test
    void exploreCountsDistinctBiomes() {
        Contract c = taken(ContractType.EXPLORE_BIOME, "", 3);
        assertEquals(1, ContractRules.advance(c, ContractType.EXPLORE_BIOME, "minecraft:plains", 1, TAGS));
        assertEquals(0, ContractRules.advance(c, ContractType.EXPLORE_BIOME, "minecraft:plains", 1, TAGS), "a biome counts once");
        assertEquals(1, ContractRules.advance(c, ContractType.EXPLORE_BIOME, "minecraft:desert", 1, TAGS));
        assertEquals(0, ContractRules.advance(c, ContractType.EXPLORE_BIOME, null, 1, TAGS));
        assertEquals(1, ContractRules.advance(c, ContractType.EXPLORE_BIOME, "minecraft:jungle", 1, TAGS));
        assertTrue(c.complete());
    }

    @Test
    void deliveryIsManual() {
        Contract c = taken(ContractType.DELIVER_ITEM, "minecraft:iron_ingot", 10);
        assertEquals(0, ContractRules.advance(c, ContractType.DELIVER_ITEM, "minecraft:iron_ingot", 5, TAGS), "not event driven");
        assertEquals(4, ContractRules.deliverable(c, 4));
        ContractRules.applyDelivery(c, 4);
        assertEquals(6, ContractRules.deliverable(c, 64), "only what is still missing");
        ContractRules.applyDelivery(c, 6);
        assertTrue(c.complete());
        assertEquals(0, ContractRules.deliverable(c, 64));
        assertEquals(0, ContractRules.deliverable(taken(ContractType.KILL_MOB, "", 3), 5));
    }

    @Test
    void tiersAndRewards() {
        Reward t1 = ContractRules.defaultReward(1, 1), t5 = ContractRules.defaultReward(5, 1);
        assertTrue(t5.coins() > t1.coins() * 10);
        assertTrue(t5.nationXp() > t1.nationXp());
        assertEquals(25, t5.reputation());
        assertTrue(ContractRules.defaultReward(2, 100).coins() > ContractRules.defaultReward(2, 1).coins(), "more actions pay more");
        assertEquals(1, ContractRules.maxTierForLevel(1));
        assertEquals(2, ContractRules.maxTierForLevel(3));
        assertEquals(5, ContractRules.maxTierForLevel(9));
        assertEquals(5, ContractRules.maxTierForLevel(50));
        assertEquals(3, ContractRules.boardSize(3, 1, 0));
        assertEquals(6, ContractRules.boardSize(3, 4, 2));
        assertTrue(ContractRules.mayTake(3, 3));
        assertFalse(ContractRules.mayTake(4, 3));
        Reward scaled = ContractRules.scaleForLevel(new Reward(1000, 10, 5), 6, 1);
        assertEquals(1100L, scaled.coins());
        assertEquals(5, scaled.reputation());
        assertEquals(new Reward(1100, 11, 5), new Reward(1000, 10, 5).withBonusPercent(10));
        assertEquals(new Reward(1000, 10, 5), new Reward(1000, 10, 5).withBonusPercent(0));
    }

    @Test
    void taxSplitNeverCreatesOrLosesCoins() {
        long[] s = ContractRules.splitTax(1000, 15);
        assertEquals(850L, s[0]);
        assertEquals(150L, s[1]);
        assertEquals(999L, ContractRules.splitTax(999, 0)[0]);
        assertEquals(0L, ContractRules.splitTax(999, 100)[0]);
        assertEquals(0L, ContractRules.splitTax(999, 500)[0], "clamped to 100 percent");
        for (int c = 0; c < 300; c += 7) for (int tp = 0; tp <= 100; tp += 9) { long[] r = ContractRules.splitTax(c, tp); assertEquals(c, r[0] + r[1]); }
    }

    @Test
    void boardPicksRespectLevelTierAndDuplicates() {
        List<ContractTemplate> all = new ArrayList<>();
        all.add(tpl("a", ContractType.KILL_MOB, "", 5, 1, 10, 1));
        all.add(tpl("b", ContractType.KILL_MOB, "", 5, 1, 10, 1));
        all.add(tpl("c", ContractType.KILL_MOB, "", 5, 3, 10, 1));
        all.add(tpl("d", ContractType.KILL_MOB, "", 5, 1, 10, 5));
        Set<String> on = new HashSet<>(Set.of("a"));
        for (int seed = 0; seed < 50; seed++) {
            List<ContractTemplate> got = ContractRules.pick(all, on, 1, 5, new Random(seed));
            assertEquals(1, got.size(), "only b is allowed at level 1 (a is on the board, c is tier 3, d needs level 5)");
            assertEquals("b", got.get(0).id());
        }
        List<ContractTemplate> many = ContractRules.pick(all, Set.of(), 9, 10, new Random(1));
        assertEquals(4, many.size());
        assertEquals(4, new HashSet<>(many.stream().map(ContractTemplate::id).toList()).size(), "no template twice");
        assertTrue(ContractRules.pick(all, Set.of(), 1, 0, new Random(1)).isEmpty());
    }

    @Test
    void weightsBiasThePick() {
        List<ContractTemplate> all = List.of(tpl("heavy", ContractType.KILL_MOB, "", 5, 1, 1000, 1), tpl("light", ContractType.KILL_MOB, "", 5, 1, 1, 1));
        int heavy = 0;
        for (int seed = 0; seed < 200; seed++) if (ContractRules.pick(all, Set.of(), 1, 1, new Random(seed)).get(0).id().equals("heavy")) heavy++;
        assertTrue(heavy > 180);
    }

    @Test
    void templateParsing() {
        JsonObject ok = JsonParser.parseString("{\"type\":\"kill_mob\",\"target\":\"minecraft:zombie\",\"count\":20,\"tier\":2,\"reward\":{\"coins\":777,\"xp\":9,\"rep\":4},\"weight\":5,\"min_level\":2,\"time_limit_minutes\":30}").getAsJsonObject();
        ContractTemplate t = ContractTemplate.parse("z", ok);
        assertEquals(ContractType.KILL_MOB, t.type());
        assertEquals(777L, t.reward().coins());
        assertEquals(2, t.minNationLevel());
        assertEquals(30, t.timeLimitMinutes());
        Contract c = Contract.from(9, t, 100);
        assertEquals(100 + 30 * 60 * 20, c.deadline);
        assertFalse(c.expired(100));
        assertTrue(c.expired(c.deadline));
        ContractTemplate d = ContractTemplate.parse("d", JsonParser.parseString("{\"type\":\"mine_block\",\"count\":3}").getAsJsonObject());
        assertEquals(ContractRules.defaultReward(1, 3), d.reward(), "defaults by tier");
        for (String bad : new String[]{"{}", "{\"type\":\"fly\"}", "{\"type\":\"kill_mob\",\"count\":0}", "{\"type\":\"kill_mob\",\"tier\":9}", "{\"type\":\"deliver_item\"}",
                "{\"type\":\"kill_player\"}", "{\"type\":\"kill_mob\",\"count\":\"x\"}"}) {
            boolean threw = false;
            try { ContractTemplate.parse("bad", JsonParser.parseString(bad).getAsJsonObject()); } catch (IllegalArgumentException e) { threw = true; }
            assertTrue(threw, "should reject " + bad);
        }
    }

    @Test
    void autoTitles() {
        assertEquals("Slay 20 minecraft:zombie", ContractRules.autoTitle(ContractType.KILL_MOB, "minecraft:zombie", 20));
        assertEquals("Mine 5 anything", ContractRules.autoTitle(ContractType.MINE_BLOCK, "", 5));
        assertEquals("Discover 4 biomes", ContractRules.autoTitle(ContractType.EXPLORE_BIOME, "", 4));
    }

    @Test
    void takeLimit() {
        List<Contract> board = new ArrayList<>();
        for (int i = 0; i < 3; i++) board.add(taken(ContractType.KILL_MOB, "", 1));
        assertFalse(ContractRules.underLimit(board, P, 3));
        assertTrue(ContractRules.underLimit(board, P, 4));
        assertTrue(ContractRules.underLimit(board, UUID.nameUUIDFromBytes("q".getBytes()), 1));
    }

    @Test
    void bountyClaimRules() {
        UUID target = UUID.nameUUIDFromBytes("t".getBytes()), poster = UUID.nameUUIDFromBytes("poster".getBytes()), killer = UUID.nameUUIDFromBytes("killer".getBytes());
        Bounty b = new Bounty();
        b.kind = Bounty.TargetKind.PLAYER;
        b.target = target.toString();
        b.poster = poster;
        b.posterNation = "red";
        b.coins = 500;
        assertTrue(b.claimableBy(killer, "blue", true, target.toString()));
        assertTrue(b.claimableBy(killer, "", true, target.toString()));
        assertFalse(b.claimableBy(poster, "blue", true, target.toString()), "the poster cannot claim");
        assertFalse(b.claimableBy(killer, "red", true, target.toString()), "a member of the posting nation cannot claim");
        assertFalse(b.claimableBy(killer, "blue", true, "someone else"));
        assertFalse(b.claimableBy(killer, "blue", false, target.toString()), "a mob is not the player");
        assertFalse(b.claimableBy(target, "blue", true, target.toString()), "no self-claim");
        b.source = Bounty.Source.WAR;
        assertTrue(b.claimableBy(killer, "red", true, target.toString()), "a war bounty may be claimed by the posting nation's members");
        Bounty mob = new Bounty();
        mob.kind = Bounty.TargetKind.MOB;
        mob.target = "minecraft:creeper";
        assertTrue(mob.claimableBy(killer, "x", false, "minecraft:creeper"));
        assertFalse(mob.claimableBy(killer, "x", true, "minecraft:creeper"));
        mob.expires = 100;
        assertTrue(mob.expired(100));
        assertFalse(mob.expired(99));
        assertEquals(550L, Bounty.totalWithFee(500, 10));
        assertEquals(500L, Bounty.totalWithFee(500, -5));
    }
}
