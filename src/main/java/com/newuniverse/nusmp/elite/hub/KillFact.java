package com.newuniverse.nusmp.elite.hub;

/**
 * Why: whether a kill counts for a quest depends on five facts about the dead creature (its tier, its road, its faction, whether it is a Mob Foundry mob, whether it is a decennial boss) and the
 * floor it stood on. The game side reads them off the live entity once ({@link QuestEvents}); the rule that compares them with a quest's filters ({@link QuestRules#matches}) is pure and tested
 * with made-up facts. {@code road} is "holy", "abyssal" or "" (a creature outside every sector), {@code faction} is a faction id or "".
 */
public record KillFact(int tier, String road, String faction, boolean foundry, boolean boss, int floor) {
    public KillFact {
        road = road == null ? "" : road;
        faction = faction == null ? "" : faction;
    }

    /** The facts of an ordinary creature killed on a signed floor: tier and road follow the floor. */
    public static KillFact onFloor(int floor, String faction, boolean foundry, boolean boss) {
        return new KillFact(CoinLoot.tierOf(floor), floor > 0 ? "holy" : floor < 0 ? "abyssal" : "", faction, foundry, boss, floor);
    }
}
