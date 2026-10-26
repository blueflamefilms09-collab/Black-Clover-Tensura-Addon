package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.item.SkillBookItem;
import com.newuniverse.nusmp.skilltree.rarity.BlockedFranchises;
import com.newuniverse.nusmp.skilltree.rarity.BookFacts;
import com.newuniverse.nusmp.skilltree.rarity.BookRarity;
import com.newuniverse.nusmp.skilltree.rarity.BossThemes;
import com.newuniverse.nusmp.skilltree.unique.LineIndex;
import com.newuniverse.nusmp.skilltree.unique.UniqueBooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

/**
 * Why: {@link LootRules} says WHETHER a death pays (a Skill Book, and a count of the old SP Books that no longer exist) and this class says WHAT, so the two halves of the odds can be tuned apart: which book is
 * picked is weighted by every route's own {@code loot_weight} through {@link RoutePick} (read live from the registry, so a datapack book with weight 5 is five times as likely as one with weight 1 and a book with
 * weight 0 never drops, and a registry with no book simply drops no book). Since 1.96.0 the count of SP Books in a plan is ignored (Skill Points are gone): the drop is the Skill Book, which teaches a unique
 * skill. It builds plain item stacks through the same factory the creative tab and the shop use, and nothing else: it touches no world, no entity and no inventory, so both entry points (the global loot modifier,
 * which adds the stacks to the rolled loot, and the Guardian listener, which spawns them as item entities) can share it and the self-test can call it. At most one book per plan, so there is nothing to
 * de-duplicate. Server thread only.
 *
 * <p>3.26.0 (rarity tiers, {@code rarityTiers} on): the same class also carries out the {@link LootRules.TieredPlan}: {@link #tieredBook} picks a line of the drawn rarity from the per-rarity weight table
 * ({@link RouteTable.Snap#rarityTable}) with the pity memory of the killer, {@link #guardianBook} makes the Guardian's deterministic themed pick ({@link BossThemes#pick}), and {@link #riftBook} the rift chest's
 * pick (mythic lines only, epic when none is left). With the setting off none of them is called and the old methods above behave exactly as in 3.25. Every pick is one binary search or one small
 * sort per drop (a drop is a kill or a chest, never a tick).
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

    /** The stacks of a decision: the old plan carried out exactly as before, or the tiered plan (3.26.0). {@code site} is the Guardian's sector (road, floor, theme), or null. */
    public static List<ItemStack> materialize(LootRules.Decision decision, Dice dice, ServerPlayer owner, LootVictims.Site site) {
        if (decision == null) return new ArrayList<>();
        if (decision.tiered() == null) return materialize(decision.old(), dice, owner);
        List<ItemStack> out = new ArrayList<>(1);
        LootRules.TieredPlan plan = decision.tiered();
        if (plan.isEmpty()) return out;
        ItemStack book = plan.boss() ? guardianBook(plan.rarity(), site, owner) : tieredBook(plan.rarity(), dice, owner);
        if (!book.isEmpty()) out.add(book);
        return out;
    }

    /**
     * A book of the drawn rarity for a monster's drop: a line of that rarity (never mythic here: it is stepped down to epic) weighted by {@code loot_weight}, blacklisted franchises at weight 0, with the pity rule
     * of the player when there is one. When the rarity has no line that can drop the pick steps down one rarity at a time; an empty stack only when no line at all can drop.
     */
    public static ItemStack tieredBook(BookRarity rarity, Dice dice, ServerPlayer owner) {
        RouteTable.Snap snap = RouteTable.current();
        BookRarity r = rarity == null || rarity == BookRarity.MYTHIC ? (rarity == null ? BookRarity.COMMON : BookRarity.EPIC) : rarity;
        for (int rank = r.rank(); rank >= 0; rank--) {
            WeightedTable table = snap.rarityTable(BookRarity.fromRank(rank));
            if (table.isEmpty()) continue;
            int pick;
            if (owner != null && LootConfig.get(LootConfig.PITY)) {
                pick = BookPity.GLOBAL.pick(table, dice, owner.getUUID(), i -> UniqueBooks.owns(owner, snap.routes()[i].id()), LootConfig.get(LootConfig.PITY_REROLLS));
            } else {
                pick = table.pick(dice);
            }
            ItemStack book = bookOf(snap.at(pick));
            if (!book.isEmpty()) return book;
        }
        return ItemStack.EMPTY;
    }

    /** The Guardian's book: the deterministic themed pick of its rarity for (floor, theme), with the pity walk for the killer, the blacklist applied. Empty when the table has no usable book. */
    public static ItemStack guardianBook(BookRarity rarity, LootVictims.Site site, ServerPlayer owner) {
        int floor = site == null ? 0 : site.floor();
        String theme = site == null ? "" : site.themeId();
        Predicate<String> owned = owner == null ? null : b -> UniqueBooks.owns(owner, b);
        String bare = BossThemes.pick(BookFacts.candidates(BlockedFranchises.setting()), rarity, floor, theme, owned);
        LineIndex.Line line = bare == null ? null : BookFacts.line(bare);
        return line == null ? ItemStack.EMPTY : SkillBookItem.stack(line.book());
    }

    /**
     * The rift chest's book: a MYTHIC line the opener does not own yet (weighted by {@code loot_weight}); when the opener owns every mythic line it falls back to an epic one ({@link com.newuniverse.nusmp.skilltree.rarity.RarityRules#riftRarity}).
     * With no opener the mythic pick stands. No pity memory (the opener's owned lines are the memory).
     */
    public static ItemStack riftBook(Dice dice, ServerPlayer opener) {
        RouteTable.Snap snap = RouteTable.current();
        IntPredicate owned = opener == null ? i -> false : i -> UniqueBooks.owns(opener, snap.routes()[i].id());
        int tries = LootConfig.get(LootConfig.PITY_REROLLS);
        WeightedTable mythic = snap.rarityTable(BookRarity.MYTHIC);
        int pick = mythic.isEmpty() ? -1 : mythic.pickAvoiding(dice, owned, tries);
        boolean mythicLeft = pick >= 0 && !owned.test(pick);
        if (!mythicLeft) {
            WeightedTable epic = snap.rarityTable(BookRarity.EPIC);
            int e = epic.isEmpty() ? -1 : epic.pickAvoiding(dice, owned, tries);
            if (e >= 0) pick = e;
        }
        return bookOf(snap.at(pick));
    }

    private static ItemStack bookOf(SkillRoute route) {
        return route == null || !LineIndex.known(route.id()) ? ItemStack.EMPTY : SkillBookItem.stack(route.book());      // a route with no unique skill would hand out a blank book
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
