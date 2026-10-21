package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.elite.hub.CoinKind;
import com.newuniverse.nusmp.elite.hub.CoinLootModifier;
import com.newuniverse.nusmp.labyrinth.loot.backpack.BackpackDrops;
import com.newuniverse.nusmp.labyrinth.loot.backpack.BackpackItem;
import com.newuniverse.nusmp.labyrinth.loot.backpack.BackpackRegistry;
import com.newuniverse.nusmp.labyrinth.loot.backpack.BackpackTier;
import com.newuniverse.nusmp.labyrinth.loot.backpack.BackpackUpgradeItem;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.foundry.FoundryMobEntity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * Why: this is the game side of the Luck-scaled loot, the part that touches a loot context. It does three things to the loot of a dungeon kill or a dungeon chest, in this order,
 * and each is a decision made by a pure class: (1) the COUNT of every ordinary stack is raised by the bell curve of {@link LuckGauss} (only ever raised, never past the stack size,
 * never for gear, coins, keys or any item of this mod that is not a part or a meal); (2) with the chance of {@link LuckGauss#armourChance}, ONE piece of dungeon armour with
 * randomised stat lines is added ({@link LuckArmour}), unless the loot already holds a piece of gear; (3) with the chance of {@link BackpackDrops}, ONE backpack or one backpack
 * upgrade is added. The Luck is the largest of the context's Luck and the killer's Luck attribute (the opener of a chest has no killer; the context already carries their Luck). The
 * place rules match the anime loot: a dimension of the mod that is not the Hub; a kill needs a PLAYER's last blow (a mob grinder with no player fighting drops nothing extra), the
 * victim is not a pet or a summoned minion; a chest is any table under {@code chests/}. Nothing here touches the food bar. Server side; any fault leaves the loot as it was.
 */
public final class LuckLoot {
    private LuckLoot() {}

    /** What was done to one loot roll: how many stacks were enlarged, whether an armour piece and whether a backpack item was added. */
    public record Result(int enlarged, boolean armour, boolean backpack) {
        public static final Result NONE = new Result(0, false, false);

        public boolean any() { return enlarged > 0 || armour || backpack; }
    }

    /** Applies the three steps to the loot of this context. */
    public static Result add(ObjectArrayList<ItemStack> loot, LootContext context) {
        ServerLevel level = context.getLevel();
        if (level == null || !LootDrops.dungeon(level)) return Result.NONE;
        Entity victim = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        LuckGauss.Kind kind;
        int floor;
        double luck = context.getLuck();
        if (victim instanceof LivingEntity mob && !(victim instanceof Player) && context.getParamOrNull(LootContextParams.DAMAGE_SOURCE) != null) {
            Player killer = context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER);
            if (killer == null) return Result.NONE;
            if (mob instanceof TamableAnimal t && t.isTame()) return Result.NONE;
            if (mob.getTags().contains(FoundryMobEntity.MINION_TAG)) return Result.NONE;
            kind = BalanceLaw.isBoss(mob) ? LuckGauss.Kind.BOSS : LuckGauss.Kind.KILL;
            floor = LootDrops.depthOf(level, mob);
            luck = Math.max(luck, killer.getLuck());
        } else {
            ResourceLocation table = context.getQueriedLootTableId();
            Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
            if (table == null || origin == null || !CoinLootModifier.isChestTable(table)) return Result.NONE;
            kind = LuckGauss.Kind.CHEST;
            floor = MegaDungeon.in(level) ? Math.abs(MegaDungeon.floorAt(level, BlockPos.containing(origin))) : 0;
            if (victim instanceof Player opener) luck = Math.max(luck, opener.getLuck());
        }
        return apply(loot, context.getRandom()::nextDouble, level, luck, floor, kind);
    }

    /** The three steps with explicit inputs (the self-test calls this with a fixed Luck and floor). */
    public static Result apply(ObjectArrayList<ItemStack> loot, Dice dice, ServerLevel level, double rawLuck, int floor, LuckGauss.Kind kind) {
        double luck = LuckGauss.luck(rawLuck);
        int enlarged = 0;
        if (LootConfig.get(LootConfig.LUCK_COUNTS)) enlarged = enlarge(loot, dice, luck, floor);
        boolean armour = LootConfig.get(LootConfig.LUCK_ARMOUR) && addArmour(loot, dice, luck, floor, kind);
        boolean backpack = LootConfig.get(LootConfig.BACKPACK_DROPS) && addBackpack(loot, dice, luck, floor, kind);
        return new Result(enlarged, armour, backpack);
    }

    // ---------------------------------------------------------------- step 1: the counts
    /** Raises the count of every eligible stack with the bell curve; returns how many were raised. */
    static int enlarge(ObjectArrayList<ItemStack> loot, Dice dice, double luck, int floor) {
        LuckGauss.Params p = LootConfig.luckItems();
        if (LuckGauss.mean(luck, floor, p) <= 0.0) return 0;
        int n = 0;
        for (ItemStack s : loot) {
            if (!scalable(s)) continue;
            int cap = s.getMaxStackSize();
            int now = LuckGauss.count(s.getCount(), luck, floor, dice, p, cap);
            if (now > s.getCount()) {
                s.setCount(now);
                n++;
            }
        }
        return n;
    }

    /** May this stack's count follow the curve? Stackable, not gear, not a coin (coins have their own Luck), not a backpack, and of this mod only a monster part, a meal or an arrow. */
    static boolean scalable(ItemStack s) {
        if (s.isEmpty() || s.getMaxStackSize() <= 1 || s.has(LootComponents.GEAR_ROLL.get())) return false;
        Item item = s.getItem();
        if (item instanceof BackpackItem || item instanceof BackpackUpgradeItem) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) return false;
        if (CoinKind.ofItemId(id.toString()) != null) return false;
        if ("nusmp".equals(id.getNamespace())) return item instanceof LootMaterialItem || item instanceof LootFoodItem || item instanceof BoneArrowItem;
        return true;
    }

    // ---------------------------------------------------------------- step 2: the armour
    static boolean addArmour(ObjectArrayList<ItemStack> loot, Dice dice, double luck, int floor, LuckGauss.Kind kind) {
        for (ItemStack s : loot) if (s.getItem() instanceof LootGear) return false;
        double chance = Math.min(LuckGauss.MAX_ARMOUR, LuckGauss.armourChance(luck, floor, kind) * Math.max(0.0, LootConfig.get(LootConfig.ARMOUR_RATE)));
        if (chance <= 0.0 || dice.next() >= chance) return false;
        LuckArmour.Pick pick = LuckArmour.pick(LootRoster.get(), floor, luck, kind == LuckGauss.Kind.BOSS, dice);
        if (pick == null) return false;
        Item item = LootItems.get(pick.id());
        if (item == null) return false;
        ItemStack stack = GearStacks.create(item, pick.rarity(), pick.seed(), Math.max(1, floor));
        if (stack.isEmpty()) return false;
        loot.add(stack);
        return true;
    }

    // ---------------------------------------------------------------- step 3: the backpack
    static boolean addBackpack(ObjectArrayList<ItemStack> loot, Dice dice, double luck, int floor, LuckGauss.Kind kind) {
        BackpackDrops.Drop drop = BackpackDrops.roll(dice, luck, floor, kind, LootConfig.get(LootConfig.BACKPACK_RATE));
        if (!drop.any()) return false;
        ItemStack stack;
        if (drop.backpack()) {
            stack = BackpackItem.of(BackpackTier.LEATHER);
        } else {
            Item up = BackpackRegistry.upgradeItem(drop.upgrade());
            if (up == null) return false;
            stack = new ItemStack(up);
        }
        loot.add(stack);
        return true;
    }
}
