package com.newuniverse.nusmp.elite.nations.contract;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Why: the rules of contracts, pure ("progress tracking by game events", "mission tiers with reward structures"): whether an event matches a contract, how progress advances, the default reward of a tier,
 * which tiers a nation level and a reputation tier may use, and how the board picks templates. Everything takes plain values (ids as strings, a tag test as a predicate) so it is tested without the game.
 */
public final class ContractRules {
    private ContractRules() {}

    /** Tier names for display. */
    public static final String[] TIER_NAMES = {"", "Bronze", "Silver", "Gold", "Platinum", "Mythic"};

    /**
     * Does an event key match a contract target? Empty target = anything; "#ns:tag" = {@code tagTest} on the tag id; otherwise equality of the ids.
     */
    public static boolean matches(String target, String eventKey, Predicate<String> tagTest) {
        if (target == null || target.isEmpty()) return true;
        if (eventKey == null) return false;
        if (target.charAt(0) == '#') return tagTest != null && tagTest.test(target.substring(1));
        return target.equals(eventKey);
    }

    /**
     * Applies an event to a TAKEN contract of that taker. KILL/MINE/CRAFT add {@code amount}; EXPLORE counts distinct keys (a biome seen twice counts once). DELIVER_ITEM is not event-driven.
     * Returns the progress gained (0 = no change). Progress never exceeds the goal.
     */
    public static int advance(Contract c, ContractType eventType, String key, int amount, Predicate<String> tagTest) {
        if (c == null || c.state != Contract.State.TAKEN || c.type != eventType || !eventType.eventDriven() || amount <= 0) return 0;
        if (c.progress >= c.goal) return 0;
        if (!matches(c.target, key, tagTest)) return 0;
        int before = c.progress;
        if (c.type == ContractType.EXPLORE_BIOME) {
            if (key == null || !c.seen.add(key)) return 0;
            c.progress = Math.min(c.goal, c.seen.size());
        } else {
            c.progress = (int) Math.min((long) c.goal, (long) c.progress + amount);
        }
        return c.progress - before;
    }

    /** Hand-in: the number of items that can be taken for a deliver contract, given how many the player carries. */
    public static int deliverable(Contract c, int carried) {
        if (c == null || c.state != Contract.State.TAKEN || c.type != ContractType.DELIVER_ITEM) return 0;
        return Math.max(0, Math.min(carried, c.goal - c.progress));
    }

    public static void applyDelivery(Contract c, int delivered) {
        if (delivered > 0) c.progress = Math.min(c.goal, c.progress + delivered);
    }

    /** The default reward of a tier for a contract of {@code count} actions: coins and XP grow with the tier (quadratically) and a little with the count. */
    public static Reward defaultReward(int tier, int count) {
        int t = Math.max(1, Math.min(ContractTemplate.MAX_TIER, tier));
        double countFactor = 1.0 + Math.log10(Math.max(1, count)) * 0.5;
        long coins = Math.round(150.0 * t * t * countFactor);
        long xp = Math.round(8.0 * t * countFactor);
        int rep = 5 * t;
        return new Reward(coins, xp, rep);
    }

    /** The highest contract tier a nation of this level may post on its board: 1 plus one per two levels, up to 5. */
    public static int maxTierForLevel(int nationLevel) { return Math.max(1, Math.min(ContractTemplate.MAX_TIER, 1 + (Math.max(1, nationLevel) - 1) / 2)); }

    /** Open contracts on a board at this level: a base plus one per three levels plus the perk slots. */
    public static int boardSize(int baseSlots, int nationLevel, int perkSlots) { return Math.max(1, baseSlots + (Math.max(1, nationLevel) - 1) / 3 + perkSlots); }

    /** May this player (nation level, reputation tier cap) take a contract of this tier? */
    public static boolean mayTake(int contractTier, int reputationMaxTier) { return contractTier <= reputationMaxTier; }

    /** Tier scaling of rewards by nation level for system contracts: +2 percent per level above the template's minimum. */
    public static Reward scaleForLevel(Reward base, int nationLevel, int minLevel) {
        int extra = Math.max(0, nationLevel - Math.max(1, minLevel));
        return base.withBonusPercent(extra * 2.0);
    }

    /**
     * Picks templates for the empty slots of a board: not already on it (by template id), allowed by level and tier, weighted at random, each template at most once. Returns the chosen templates.
     */
    public static List<ContractTemplate> pick(List<ContractTemplate> all, Set<String> alreadyOnBoard, int nationLevel, int slotsToFill, Random rnd) {
        List<ContractTemplate> pool = new ArrayList<>();
        int maxTier = maxTierForLevel(nationLevel);
        for (ContractTemplate t : all) {
            if (t.minNationLevel() <= nationLevel && t.tier() <= maxTier && !alreadyOnBoard.contains(t.id())) pool.add(t);
        }
        List<ContractTemplate> out = new ArrayList<>();
        Set<String> chosen = new HashSet<>();
        for (int i = 0; i < slotsToFill && !pool.isEmpty(); i++) {
            long total = 0;
            for (ContractTemplate t : pool) total += t.weight();
            long r = (long) (rnd.nextDouble() * total);
            ContractTemplate pick = pool.get(pool.size() - 1);
            for (ContractTemplate t : pool) {
                r -= t.weight();
                if (r < 0) { pick = t; break; }
            }
            if (chosen.add(pick.id())) out.add(pick);
            pool.remove(pick);
        }
        return out;
    }

    /** A readable title when the template has none: "Slay 20 minecraft:zombie". */
    public static String autoTitle(ContractType type, String target, int count) {
        String t = target == null || target.isEmpty() ? "anything" : target;
        return switch (type) {
            case KILL_MOB -> "Slay " + count + " " + t;
            case MINE_BLOCK -> "Mine " + count + " " + t;
            case CRAFT_ITEM -> "Craft " + count + " " + t;
            case EXPLORE_BIOME -> "Discover " + count + " biomes" + (target == null || target.isEmpty() ? "" : " (" + target + ")");
            case DELIVER_ITEM -> "Deliver " + count + " " + t;
            case KILL_PLAYER -> "Hunt a player";
        };
    }

    /** Reward after the nation's tax: the player gets coins minus tax, the treasury the tax. Returns {playerShare, taxShare}. */
    public static long[] splitTax(long coins, int taxPercent) {
        int tp = Math.max(0, Math.min(100, taxPercent));
        long tax = coins * tp / 100;
        return new long[]{coins - tax, tax};
    }

    /** Is the contract's taker allowed to keep it (still a member, not over the limit)? Kept pure for tests: count of contracts a player holds. */
    public static boolean underLimit(List<Contract> board, UUID player, int limit) {
        int n = 0;
        for (Contract c : board) if (c.takenBy(player)) n++;
        return n < limit;
    }
}
