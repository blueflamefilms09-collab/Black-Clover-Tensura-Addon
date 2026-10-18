package com.newuniverse.nusmp.elite.nations.contract;

import com.google.gson.JsonObject;

/**
 * Why: a contract template loaded from {@code data/nusmp/elite/nations/contracts/*.json} (30+ shipped). The board turns templates into live {@link Contract}s. Fields: {@code type}, {@code target}
 * (an id such as minecraft:zombie, a tag as "#minecraft:logs", or empty for "any"), {@code count}, {@code tier} 1..5, optional {@code title}/{@code description} (otherwise generated from type and target),
 * optional {@code reward} {coins, xp, rep} (otherwise the tier's default), {@code weight}, {@code min_level} and {@code time_limit_minutes} (0 = none). Pure; {@link #parse} throws
 * IllegalArgumentException with a readable reason, which the loader logs and skips.
 */
public record ContractTemplate(String id, ContractType type, String target, int count, int tier, String title, String description,
                               Reward reward, int weight, int minNationLevel, int timeLimitMinutes) {
    public static final int MAX_TIER = 5, MAX_COUNT = 100_000;

    public static ContractTemplate parse(String id, JsonObject o) {
        try {
            ContractType type = ContractType.parse(o.get("type").getAsString());
            if (type == null) throw new IllegalArgumentException("unknown type");
            if (type == ContractType.KILL_PLAYER) throw new IllegalArgumentException("kill_player contracts are bounties, not templates");
            String target = o.has("target") ? o.get("target").getAsString().trim() : "";
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            if (count < 1 || count > MAX_COUNT) throw new IllegalArgumentException("count out of range");
            int tier = o.has("tier") ? o.get("tier").getAsInt() : 1;
            if (tier < 1 || tier > MAX_TIER) throw new IllegalArgumentException("tier out of range");
            if (type == ContractType.DELIVER_ITEM && target.isEmpty()) throw new IllegalArgumentException("deliver needs a target item");
            Reward reward = ContractRules.defaultReward(tier, count);
            if (o.has("reward") && o.get("reward").isJsonObject()) {
                JsonObject r = o.getAsJsonObject("reward");
                reward = new Reward(Math.max(0, r.has("coins") ? r.get("coins").getAsLong() : reward.coins()),
                        Math.max(0, r.has("xp") ? r.get("xp").getAsLong() : reward.nationXp()),
                        r.has("rep") ? r.get("rep").getAsInt() : reward.reputation());
            }
            return new ContractTemplate(id, type, target, count, tier,
                    o.has("title") ? o.get("title").getAsString() : "",
                    o.has("description") ? o.get("description").getAsString() : "",
                    reward,
                    Math.max(1, o.has("weight") ? o.get("weight").getAsInt() : 10),
                    Math.max(1, o.has("min_level") ? o.get("min_level").getAsInt() : 1),
                    Math.max(0, o.has("time_limit_minutes") ? o.get("time_limit_minutes").getAsInt() : 0));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("malformed field (" + e.getMessage() + ")");
        }
    }
}
