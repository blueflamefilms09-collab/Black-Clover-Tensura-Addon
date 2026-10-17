package com.newuniverse.nusmp.labyrinth.mega.system;

/**
 * Why: a floor quest is never given on a boss floor (the Guardian is the challenge there), and "is this a boss floor?" used to be answered by
 * {@code FloorMap.isDecennial(floor, sector.perRegion)}. That question changes meaning with the floor model of 1.58.0: a sector is now ONE floor, so its
 * {@code perRegion} is 1, and "a multiple of 1" would make EVERY floor a boss floor and silence the quests everywhere. The contract
 * (docs/labyrinth/mega_v2.md section 1) fixes the rule in floor numbers, not in sector sizes: every tenth floor, {@code |floor| % 10 == 0}, on both roads. This
 * class is that rule, in one pure place that both the quest start and the window's boss flag ask, and it gives the same answer for an old world whose sectors
 * still span ten floors. Pure.
 */
public final class QuestFloors {
    private QuestFloors() {}

    /** A boss floor comes this often (the contract's "every tenth floor"). */
    public static final int BOSS_EVERY = 10;

    /** Is this a boss floor (+-10, +-20 ...)? Floor 0 (the Hub) is not: it has no quests for another reason. */
    public static boolean isBossFloor(int floor) {
        return floor != 0 && Math.abs(floor) % BOSS_EVERY == 0;
    }
}
