package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.item.SkillBookItem;
import com.newuniverse.nusmp.skilltree.unique.LineIndex;
import com.newuniverse.nusmp.skilltree.unique.UniqueBooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: {@link LootRules} says WHETHER a death pays (a Skill Book, and a count of the old SP Books that no longer exist) and this class says WHAT, so the two halves of the odds can be tuned apart: which book is
 * picked is weighted by every route's own {@code loot_weight} through {@link RoutePick} (read live from the registry, so a datapack book with weight 5 is five times as likely as one with weight 1 and a book with
 * weight 0 never drops, and a registry with no book simply drops no book). Since 1.96.0 the count of SP Books in a plan is ignored (Skill Points are gone): the drop is the Skill Book, which teaches a unique
 * skill. It builds plain item stacks through the same factory the creative tab and the shop use, and nothing else: it touches no world, no entity and no inventory, so both entry points (the global loot modifier,
 * which adds the stacks to the rolled loot, and the Guardian listener, which spawns them as item entities) can share it and the self-test can call it. At most one book per plan, so there is nothing to
 * de-duplicate. Server thread only.
 */
public final class LootDrops {
    private LootDrops() {}

    /** The stacks of a plan: at most one Skill Book. Empty when the plan is empty or no book can drop. No player: no pity. */
    public static List<ItemStack> materialize(LootRules.Plan plan, Dice dice) { return materialize(plan, dice, null); }

    /** The same for a drop that goes to {@code owner} (the killer, the chest's opener): the route book then follows the pity rule for that player. */
    public static List<ItemStack> materialize(LootRules.Plan plan, Dice dice, ServerPlayer owner) {
        List<ItemStack> out = new ArrayList<>();
        if (plan == null || plan.isEmpty()) return out;
        if (plan.routeBook()) {
            ItemStack book = routeBook(dice, owner);
            if (!book.isEmpty()) out.add(book);
        }
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
            pick = BookPity.GLOBAL.pick(snap.table(), dice, owner.getUUID(), i -> UniqueBooks.owns(owner, snap.routes()[i].id()), LootConfig.get(LootConfig.PITY_REROLLS));
        } else {
            pick = snap.table().pick(dice);
        }
        SkillRoute route = snap.at(pick);
        return route == null || !LineIndex.known(route.id()) ? ItemStack.EMPTY : SkillBookItem.stack(route.book());      // a route with no unique skill would hand out a blank book
    }
}
