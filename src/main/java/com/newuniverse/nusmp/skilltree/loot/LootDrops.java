package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.item.SPBookItem;
import com.newuniverse.nusmp.skilltree.item.SkillBookItem;
import com.newuniverse.nusmp.skilltree.item.SpKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: {@link LootRules} says WHETHER a death pays (a route book, so many SP Books) and this class says WHAT, so the two halves of the odds can be tuned apart: which route
 * is picked is weighted by every route's own {@code loot_weight} through {@link RoutePick} (read live from the registry, so a datapack route with weight 5 is five times as
 * likely as one with weight 1 and a route with weight 0 never drops, and a registry with no route simply drops no route book), and which primer an SP Book is comes from the
 * kind table's drop weights ({@link SpKind#pickDrop}: the cheap primers for a monster, the good ones for a Guardian). It builds plain item stacks through the same factories
 * the creative tab and the shop use, and nothing else: it touches no world, no entity and no inventory, so both entry points (the global loot modifier, which adds the
 * stacks to the rolled loot, and the Guardian listener, which spawns them as item entities) can share it and the self-test can call it. At most one route book per plan,
 * so there is nothing to de-duplicate. Server thread only.
 */
public final class LootDrops {
    private LootDrops() {}

    /** The stacks of a plan: at most one route book, then the SP Books. Empty when the plan is empty or no route can drop. No player: no pity. */
    public static List<ItemStack> materialize(LootRules.Plan plan, Dice dice) { return materialize(plan, dice, null); }

    /** The same for a drop that goes to {@code owner} (the killer, the chest's opener): the route book then follows the pity rule for that player. */
    public static List<ItemStack> materialize(LootRules.Plan plan, Dice dice, ServerPlayer owner) {
        List<ItemStack> out = new ArrayList<>();
        if (plan == null || plan.isEmpty()) return out;
        if (plan.routeBook()) {
            ItemStack book = routeBook(dice, owner);
            if (!book.isEmpty()) out.add(book);
        }
        for (int i = 0; i < plan.spBooks(); i++) out.add(SPBookItem.stack(SpKind.pickDrop(dice, plan.boss())));
        return out;
    }

    /** One route book, the route weighted by {@code loot_weight}; an empty stack when no route has a positive weight. No pity. */
    public static ItemStack routeBook(Dice dice) { return routeBook(dice, null); }

    /**
     * One route book for a player (or none, null): the line is drawn from the cached weight table of the registry version in force ({@link RouteTable}: no list is built per drop, a pick is a
     * binary search), weighted by {@code loot_weight}; with {@code pity} on in the config, a player whose previous book was of a line they already own gets a line they do not own
     * ({@link BookPity}). An empty stack when no route has a positive weight.
     */
    public static ItemStack routeBook(Dice dice, ServerPlayer owner) {
        RouteTable.Snap snap = RouteTable.current();
        if (snap.table().isEmpty()) return ItemStack.EMPTY;
        int pick;
        if (owner != null && LootConfig.get(LootConfig.PITY)) {
            pick = BookPity.GLOBAL.pick(snap.table(), dice, owner.getUUID(), i -> SkillTreeApi.hasRoute(owner, snap.routes()[i].id()), LootConfig.get(LootConfig.PITY_REROLLS));
        } else {
            pick = snap.table().pick(dice);
        }
        return SkillBookItem.stackFor(snap.at(pick));
    }
}
