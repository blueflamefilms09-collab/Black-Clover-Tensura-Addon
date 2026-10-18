package com.newuniverse.nusmp.elite.items.crate;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.elite.api.EliteHooks;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.loot.LootDrops;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

/**
 * Why: the codec registry of crate rewards. A table's {@code type} is looked up here; the five built-in types are registered by the static initialiser, and any other system may add one with
 * {@link #register} (the shop could add {@code title}, the seasons builder {@code legacy_point}). Each type is a tiny class that GIVES something and says what it gave, or returns null when it cannot
 * (the opening then re-rolls the slot without that type), so an economy, a skill tree or an item removed by a datapack never makes a crate pay less. There is deliberately no type that runs a command.
 *
 * <ul>
 *   <li>{@code item}: {@code id} is an item id; the amount is the count (split over stacks, the overflow dropped at the player's feet).</li>
 *   <li>{@code coins}: the amount in copper coins through {@link EliteHooks#wallet}. Skipped (null) when the economy is off: the wallet is asked for its balance before and after, and a balance that did not
 *       move means nobody is minting coins.</li>
 *   <li>{@code skill_book}: one random Skill Book of the skill tree ({@code LootDrops.routeBook}, the same weighted pick and pity as the dungeon drops). Skipped when the skill tree has no route.</li>
 *   <li>{@code sp}: Skill Points through the skill tree's one door. Skipped when the skill tree is off.</li>
 *   <li>{@code xp}: experience points.</li>
 * </ul>
 */
public final class RewardSerializers {
    private RewardSerializers() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<String, RewardType> TYPES = new LinkedHashMap<>();

    /** Adds (or replaces) a reward type. Call from a mod's init; the crate tables can then name it. */
    public static synchronized void register(RewardType type) {
        if (type != null && !type.id().isEmpty()) TYPES.put(type.id(), type);
    }

    public static synchronized RewardType get(String id) { return TYPES.get(id); }

    public static synchronized boolean known(String id) { return TYPES.containsKey(id); }

    public static synchronized java.util.Set<String> ids() { return new java.util.LinkedHashSet<>(TYPES.keySet()); }

    static {
        register(new ItemReward());
        register(new CoinsReward());
        register(new SkillBookReward());
        register(new SpReward());
        register(new XpReward());
    }

    // ------------------------------------------------------------------ item
    static final class ItemReward implements RewardType {
        @Override public String id() { return "item"; }

        @Override
        public String validate(RewardEntry e) {
            ResourceLocation rl = ResourceLocation.tryParse(e.id());
            if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) return "unknown item '" + e.id() + "'";
            return null;
        }

        @Override
        public Component grant(ServerPlayer p, RewardEntry e, int amount, DoubleSupplier dice) {
            ResourceLocation rl = ResourceLocation.tryParse(e.id());
            Item item = rl == null ? null : BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
            if (item == null) return null;
            int left = amount;
            int max = Math.max(1, new ItemStack(item).getMaxStackSize());
            while (left > 0) {
                int n = Math.min(left, max);
                ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(item, n));
                left -= n;
            }
            Component name = new ItemStack(item).getHoverName();
            return amount > 1 ? Component.literal(amount + "x ").append(name) : name.copy();
        }
    }

    // ------------------------------------------------------------------ coins
    static final class CoinsReward implements RewardType {
        @Override public String id() { return "coins"; }

        @Override
        public Component grant(ServerPlayer p, RewardEntry e, int amount, DoubleSupplier dice) {
            try {
                EliteHooks.Wallet w = EliteHooks.wallet;
                long before = w.balance(p);
                w.give(p, amount);
                if (w.balance(p) <= before) return null;                                    // the economy is off: nothing moved
            } catch (RuntimeException ex) {
                LOG.warn("[nusmp] crate: the wallet failed, the coins entry is skipped", ex);
                return null;
            }
            return Component.literal(amount + " " + (e.label().isEmpty() ? "copper coins" : e.label()));
        }
    }

    // ------------------------------------------------------------------ skill_book
    static final class SkillBookReward implements RewardType {
        @Override public String id() { return "skill_book"; }

        @Override
        public Component grant(ServerPlayer p, RewardEntry e, int amount, DoubleSupplier dice) {
            try {
                ItemStack book = LootDrops.routeBook(dice::getAsDouble, p);
                if (book.isEmpty()) return null;
                Component name = book.getHoverName();
                ItemHandlerHelper.giveItemToPlayer(p, book);
                return name.copy();
            } catch (RuntimeException | LinkageError ex) {
                LOG.warn("[nusmp] crate: the skill tree could not give a book, the entry is skipped", ex);
                return null;
            }
        }
    }

    // ------------------------------------------------------------------ sp
    static final class SpReward implements RewardType {
        @Override public String id() { return "sp"; }

        @Override
        public Component grant(ServerPlayer p, RewardEntry e, int amount, DoubleSupplier dice) {
            try {
                if (!SkillTreeApi.isEnabled()) return null;
                int before = SkillTreeApi.sp(p);
                SkillTreeApi.addSp(p, amount, "elite crate");
                if (SkillTreeApi.sp(p) <= before) return null;                              // at the cap: nothing was added
                return Component.literal(amount + " Skill Points");
            } catch (RuntimeException | LinkageError ex) {
                LOG.warn("[nusmp] crate: the skill tree could not take Skill Points, the entry is skipped", ex);
                return null;
            }
        }
    }

    // ------------------------------------------------------------------ xp
    static final class XpReward implements RewardType {
        @Override public String id() { return "xp"; }

        @Override
        public Component grant(ServerPlayer p, RewardEntry e, int amount, DoubleSupplier dice) {
            p.giveExperiencePoints(amount);
            return Component.literal(amount + " experience");
        }
    }
}
