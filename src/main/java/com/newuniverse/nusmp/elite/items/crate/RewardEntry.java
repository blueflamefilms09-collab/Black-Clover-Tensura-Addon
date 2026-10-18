package com.newuniverse.nusmp.elite.items.crate;

/**
 * Why: one weighted line of a crate's reward table. It is data only (a type name that a {@link RewardSerializers} entry understands, a weight, an optional id and a number range) so a datapack can
 * retune or extend a crate with no code. It never holds a command: the parser refuses any key it does not know, so a table can give things but cannot run anything.
 *
 * @param type   the serializer: {@code item}, {@code coins}, {@code skill_book}, {@code sp}, {@code xp}, or one added by another system
 * @param weight the relative chance (a positive whole number)
 * @param id     what the type needs (an item id for {@code item}); empty when it needs none
 * @param min    the least amount (items, coins in copper, points)
 * @param max    the most amount
 * @param label  a short line for the opening message; empty to use the type's own
 */
public record RewardEntry(String type, int weight, String id, int min, int max, String label) {
    public RewardEntry {
        type = type == null ? "" : type;
        id = id == null ? "" : id;
        label = label == null ? "" : label;
        if (weight < 0) weight = 0;
        if (min < 0) min = 0;
        if (max < min) max = min;
    }
}
