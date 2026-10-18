package com.newuniverse.nusmp.elite.hub;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Why: every Hub quest is DATA ({@code data/nusmp/elite/hub/quests/*.json}), not a branch in code, so the owner's example ("Defeat 50 Corrupted Foundations mobs") is one row, a datapack can retune
 * or add a quest without a mod update, and the pure state machine ({@link PlayerQuestData}) works on this one record whatever the quest asks. A quest has a TYPE (a daily one is on the board for a
 * day and can be done once a day; a progression one is a chain that is done once, in order) and an OBJECTIVE of one of four kinds (kill N, clear K floors, deliver items, reach floor F), each
 * narrowed by filters (tier 1..5, road, faction, which kind of mob). {@code reward} is in bronze coins ({@link CoinKind}). {@code minFloor} is the deepest floor (absolute value) the player must have
 * reached before the quest is offered, so a fresh player is not shown a tier-5 hunt, and {@code requires} lists the quests that must be done first. Immutable; the constructor normalises, so
 * no consumer defends against a null or a negative number.
 *
 * @param tier    0 = any, else 1..5 (the same tiers as the Mob Foundry: 200 floors each)
 * @param road    "" = any, "holy" or "abyssal"
 * @param faction "" = any, else one of the dungeon's faction ids
 * @param mob     "any", "foundry" (only {@code nusmp:foundry_mob}) or "boss" (only decennial Guardians)
 * @param item    the item id of a delivery quest ("" for the others)
 * @param floor   the signed floor of a reach-floor quest (0 for the others)
 */
public record QuestDef(String id, QuestType type, ObjectiveKind kind, String title, String desc, int target, int tier, String road, String faction, String mob,
                       String item, int floor, long reward, int minFloor, List<String> requires, int weight) {
    public QuestDef {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(kind, "kind");
        id = id == null ? "" : id;
        title = title == null ? "" : title.trim();
        desc = desc == null ? "" : desc.trim();
        target = Math.max(1, Math.min(1_000_000, target));
        tier = Math.max(0, Math.min(CoinLoot.TIERS, tier));
        road = road == null ? "" : road.trim().toLowerCase(Locale.ROOT);
        if (!road.equals("holy") && !road.equals("abyssal")) road = "";
        faction = faction == null ? "" : faction.trim().toLowerCase(Locale.ROOT);
        mob = mob == null ? "any" : mob.trim().toLowerCase(Locale.ROOT);
        if (!mob.equals("foundry") && !mob.equals("boss")) mob = "any";
        item = item == null ? "" : item.trim().toLowerCase(Locale.ROOT);
        reward = Math.max(0, Math.min(1_000_000_000L, reward));
        minFloor = Math.max(0, minFloor);
        requires = requires == null ? List.of() : List.copyOf(requires);
        weight = Math.max(0, weight);
    }

    public boolean daily() { return type == QuestType.DAILY; }

    /** The road a reach-floor quest asks for ("holy" or "abyssal"), else the road filter. */
    public String effectiveRoad() {
        if (kind == ObjectiveKind.REACH_FLOOR && floor != 0) return floor > 0 ? "holy" : "abyssal";
        return road;
    }
}
