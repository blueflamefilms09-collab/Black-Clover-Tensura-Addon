package com.newuniverse.nusmp.elite.skills.evolution;

/**
 * Why: an offering. The player must carry {@code count} of {@code item}, and the evolution takes them. Satisfying it is separate from paying it: {@link #test} only looks; {@link #consume} removes
 * the items and is called by the engine once every trigger of the path has passed, so a player who is short of one other requirement never loses an item. Params: {@code item}, {@code count}.
 */
public final class ItemConsume implements EvolutionTrigger {
    @Override public String id() { return "item_consume"; }

    @Override
    public boolean test(EvolutionContext ctx, Params p, String skillId) {
        String item = p.str("item", "");
        return !item.isEmpty() && ctx.itemCount(item) >= Math.max(1, p.integer("count", 1));
    }

    @Override
    public boolean consume(EvolutionContext ctx, Params p, String skillId) {
        return ctx.consumeItems(p.str("item", ""), Math.max(1, p.integer("count", 1)));
    }

    @Override
    public String describe(Params p, String skillId) { return "offer " + Math.max(1, p.integer("count", 1)) + " x " + p.str("item", "?"); }
}
