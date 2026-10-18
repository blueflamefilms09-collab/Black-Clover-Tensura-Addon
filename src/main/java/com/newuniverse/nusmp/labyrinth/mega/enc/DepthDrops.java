package com.newuniverse.nusmp.labyrinth.mega.enc;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Why: the owner wants the dungeon mobs' drops to get better the deeper (or higher) the floor. The coins (CoinLoot) and the equipment (GearRoller) already do, by their own rules, and this listener
 * deliberately leaves them alone; what had no depth at all is everything ELSE a monster leaves, the contents of its own loot table (meat, hides, ore, the items of Tensura's and vanilla's
 * monsters), and the experience of the kill. So it is a LOW-priority {@code LivingDropsEvent} listener that adds to the drops the other listeners decided, never replaces them, with the four curves
 * of {@link DepthDropRules}: it rolls the mob's own loot table again {@code extraRolls} times (raw, so the global loot modifiers do not fire again and a coin or a skill book is not paid twice), with
 * extra luck, adds the same fraction to the count of every stack that can stack (coin stacks are left to the coin rules), and multiplies the experience. It applies on the same terms as the gear
 * and the economy: the Mega-Dungeon, a dungeon monster that is no boss and no servant ({@link DepthMobs#eligible}), killed by a player. The floor is the one the mob was BORN on when it has it
 * ({@link DepthMobs#FLOOR_KEY}), else the floor where it died. At most {@link DepthDropRules#MAX_EXTRA_STACKS} extra stacks per death. A failure is logged once and that death drops as it did before.
 */
public final class DepthDrops {
    private DepthDrops() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean warned;

    public static void onDrops(LivingDropsEvent e) {
        try {
            process(e, new Random());
        } catch (RuntimeException | LinkageError ex) {
            if (!warned) {
                warned = true;
                LOG.warn("[nusmp] The depth drops failed once and are skipped for that death (further failures are not logged)", ex);
            }
        }
    }

    public static void onExperience(LivingExperienceDropEvent e) {
        try {
            if (!DepthConfig.get(DepthConfig.ENABLED) || !DepthConfig.get(DepthConfig.DROPS)) return;
            if (!(e.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level) || !Labyrinth.inMega(level)) return;
            if (e.getAttackingPlayer() == null || !DepthMobs.eligible(mob)) return;
            int floor = floorOf(level, mob);
            if (floor == 0) return;
            e.setDroppedExperience(DepthDropRules.boostedXp(e.getDroppedExperience(), DepthRules.depthOf(floor), DepthConfig.dropParams()));
        } catch (RuntimeException | LinkageError ex) {
            if (!warned) {
                warned = true;
                LOG.warn("[nusmp] The depth experience failed once and is skipped (further failures are not logged)", ex);
            }
        }
    }

    /** The born floor of a mob, else the floor it stands on (0 outside every sector). */
    static int floorOf(ServerLevel level, Mob mob) {
        int born = mob.getPersistentData().getInt(DepthMobs.FLOOR_KEY);
        return born != 0 ? born : MegaDungeon.floorAt(level, mob.blockPosition());
    }

    /** Adds the depth bonus to one death; returns the number of extra stacks added (the boosted counts are not counted). */
    public static int process(LivingDropsEvent e, Random rnd) {
        if (!DepthConfig.get(DepthConfig.ENABLED) || !DepthConfig.get(DepthConfig.DROPS)) return 0;
        if (!(e.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level) || !Labyrinth.inMega(level)) return 0;
        ServerPlayer killer = e.getSource().getEntity() instanceof ServerPlayer sp ? sp : (mob.getKillCredit() instanceof ServerPlayer lp ? lp : null);
        if (killer == null || !DepthMobs.eligible(mob)) return 0;
        int floor = floorOf(level, mob);
        if (floor == 0) return 0;
        int depth = DepthRules.depthOf(floor);
        DepthDropRules.Params p = DepthConfig.dropParams();

        // 1. every stack that already drops is a little bigger (coins have their own rule)
        for (ItemEntity ie : e.getDrops()) {
            ItemStack s = ie.getItem();
            if (isCoin(s)) continue;
            int n = DepthDropRules.boostedCount(s.getCount(), s.getMaxStackSize(), depth, p, rnd.nextDouble());
            if (n != s.getCount()) {
                ItemStack c = s.copy();
                c.setCount(n);
                ie.setItem(c);
            }
        }

        // 2. the mob's own loot table again, raw (no global loot modifiers), with extra luck
        int rolls = DepthDropRules.extraRolls(depth, p, rnd.nextDouble());
        List<ItemStack> extra = rollExtra(level, mob, e.getSource(), killer, rolls, DepthDropRules.luckBonus(depth, p));
        for (ItemStack s : extra) e.getDrops().add(new ItemEntity(level, mob.getX(), mob.getY() + 0.3, mob.getZ(), s));
        return extra.size();
    }

    /** The extra rolls of the mob's own loot table: up to {@code rolls} raw rolls with {@code extraLuck} added to the killer's own luck, never more than the stack cap in total. */
    static List<ItemStack> rollExtra(ServerLevel level, Mob mob, net.minecraft.world.damagesource.DamageSource source, ServerPlayer killer, int rolls, float extraLuck) {
        List<ItemStack> extra = new ArrayList<>();
        if (rolls <= 0) return extra;
        ResourceKey<LootTable> key = mob.getType().getDefaultLootTable();
        if (key == null || key == BuiltInLootTables.EMPTY) return extra;
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        if (table == LootTable.EMPTY) return extra;
        for (int i = 0; i < rolls && extra.size() < DepthDropRules.MAX_EXTRA_STACKS; i++) {
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, mob)
                    .withParameter(LootContextParams.ORIGIN, mob.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                    .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
                    .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity())
                    .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
                    .withLuck(killer.getLuck() + extraLuck)
                    .create(LootContextParamSets.ENTITY);
            LootContext ctx = new LootContext.Builder(params).create(Optional.empty());
            table.getRandomItemsRaw(ctx, s -> {
                if (!s.isEmpty() && extra.size() < DepthDropRules.MAX_EXTRA_STACKS) extra.add(s);
            });
        }
        return extra;
    }

    /** Coin stacks (Tensura's and this mod's) are paid by the coin rules, which already scale with depth. */
    static boolean isCoin(ItemStack s) {
        return BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().contains("coin");
    }
}
