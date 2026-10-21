package com.newuniverse.nusmp.labyrinth.loot.backpack;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.labyrinth.loot.LuckGauss;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Why: the backpack cannot be played by its author, so the CI server smoke test runs it in the real game. {@code dungeon.backpack} sets one down in the Mega-Dungeon level with a
 * fake player and proves the whole life of the thing: five tiers with 27, 36, 45, 54 and 63 slots and four upgrade items; the block entity has the tier's size and holds a stack;
 * an upgrade item used on the placed block grows it IN PLACE and keeps the items, an upgrade that is not bigger is refused and not spent; breaking it through the REAL loot table
 * hands back a backpack that carries the tier and the contents; a block entity made from such an item has the same contents; the held-backpack container writes into the stack's
 * container component; the window refuses a backpack inside a backpack and takes a diamond; the crafting recipe turns a backpack and an upgrade into the bigger pack with the contents
 * kept and refuses a smaller one; every recipe file loaded; and the placed-block hook reaches an installed listener. {@code dungeon.backpack.drops} runs the drop rules ten
 * thousand times: only plain backpacks on the first floors, never a tier the floor does not allow, and Luck makes drops more frequent. Everything it places it removes.
 */
public final class BackpackSelfTests {
    private BackpackSelfTests() {}

    public static void register() {
        SelfTests.register("dungeon.backpack", BackpackSelfTests::life);
        SelfTests.register("dungeon.backpack.drops", BackpackSelfTests::drops);
    }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    private static BlockPos freeSpot(ServerLevel level, int x, int z) {
        for (int y = 70; y < 200; y++) {
            BlockPos p = new BlockPos(x, y, z);
            if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()) return p;
        }
        return new BlockPos(x, 200, z);
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        DungeonGuard.internal(() -> level.setBlock(pos, state, Block.UPDATE_ALL));
    }

    private static SelfTests.Result life(MinecraftServer server) {
        int[] want = {27, 36, 45, 54, 63};
        BackpackTier[] tiers = BackpackTier.values();
        if (tiers.length != want.length) return fail("the tiers are " + tiers.length + ", the spec says 5");
        for (int i = 0; i < tiers.length; i++) if (tiers[i].slots() != want[i] || tiers[i].slots() % 9 != 0) return fail(tiers[i].id() + " has " + tiers[i].slots() + " slots, the spec says " + want[i]);
        for (BackpackTier t : tiers) if (t.upgradeItemId() != null && BackpackRegistry.upgradeItem(t) == null) return fail("the upgrade item of " + t.id() + " is not registered");
        if (BackpackRegistry.MENU.get() == null || BackpackRegistry.BLOCK_ENTITY.get() == null) return fail("the menu type or the block entity type is missing");
        ServerLevel level = SelfTests.mega(server);
        if (level == null) return fail("no mega dimension");
        FakePlayer player = SelfTests.player(level, "backpack_user", 0.5, 70, 0.5);
        BlockPos pos = freeSpot(level, 6, 6), pos2 = freeSpot(level, 9, 9);
        BlockState before = level.getBlockState(pos), before2 = level.getBlockState(pos2);
        try {
            // ---- the block and its entity
            BlockState state = BackpackRegistry.BLOCK.get().defaultBlockState().setValue(BackpackBlock.FACING, Direction.EAST);
            set(level, pos, state);
            if (!(level.getBlockEntity(pos) instanceof BackpackBlockEntity be)) return fail("the placed backpack has no block entity");
            if (be.getContainerSize() != 27 || be.tier() != BackpackTier.LEATHER) return fail("a new backpack is " + be.getContainerSize() + " slots, tier " + be.tier());
            be.setItem(0, new ItemStack(Items.DIAMOND, 5));
            be.setItem(26, new ItemStack(Items.APPLE, 3));
            if (!BackpackBlock.class.isInstance(level.getBlockState(pos).getBlock())) return fail("the block went missing");

            // ---- an upgrade used on the placed block grows it, items kept
            ItemStack iron = new ItemStack(BackpackRegistry.upgradeItem(BackpackTier.IRON));
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            player.setItemInHand(InteractionHand.MAIN_HAND, iron);
            ItemInteractionResult used = level.getBlockState(pos).useItemOn(iron, level, player, InteractionHand.MAIN_HAND, hit);
            if (!used.consumesAction()) return fail("the iron upgrade was not accepted by a leather backpack: " + used);
            if (level.getBlockState(pos).getValue(BackpackBlock.TIER) != 1) return fail("the block state did not become iron");
            if (!(level.getBlockEntity(pos) instanceof BackpackBlockEntity grown) || grown.getContainerSize() != 36) return fail("the backpack did not grow to 36 slots");
            if (!grown.getItem(0).is(Items.DIAMOND) || grown.getItem(0).getCount() != 5 || !grown.getItem(26).is(Items.APPLE)) return fail("the items were not kept by the upgrade");
            if (!iron.isEmpty()) return fail("the upgrade item was not spent");
            ItemStack again = new ItemStack(BackpackRegistry.upgradeItem(BackpackTier.IRON));
            player.setItemInHand(InteractionHand.MAIN_HAND, again);
            level.getBlockState(pos).useItemOn(again, level, player, InteractionHand.MAIN_HAND, hit);
            if (again.getCount() != 1 || level.getBlockState(pos).getValue(BackpackBlock.TIER) != 1) return fail("an upgrade that is not bigger was spent or changed the pack");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            // ---- breaking it through the real loot table keeps the tier and the contents
            List<ItemStack> drops = Block.getDrops(level.getBlockState(pos), level, pos, level.getBlockEntity(pos));
            ItemStack dropped = drops.stream().filter(s -> s.getItem() instanceof BackpackItem).findFirst().orElse(ItemStack.EMPTY);
            if (dropped.isEmpty()) return fail("breaking the backpack dropped no backpack (the loot table)");
            if (BackpackItem.tierOf(dropped) != BackpackTier.IRON) return fail("the dropped backpack is tier " + BackpackItem.tierOf(dropped));
            if (BackpackItem.filled(dropped) != 2) return fail("the dropped backpack holds " + BackpackItem.filled(dropped) + " stacks, expected 2");

            // ---- a block entity made from that item has the same contents
            set(level, pos2, BackpackRegistry.BLOCK.get().defaultBlockState().setValue(BackpackBlock.TIER, 1));
            if (!(level.getBlockEntity(pos2) instanceof BackpackBlockEntity made)) return fail("the second backpack has no block entity");
            made.applyComponentsFromItemStack(dropped);
            if (!made.getItem(0).is(Items.DIAMOND) || !made.getItem(26).is(Items.APPLE)) return fail("a backpack placed from the item did not get its contents back");

            // ---- the held backpack writes into the stack
            ItemStack held = BackpackItem.of(BackpackTier.GOLD);
            HeldBackpack container = new HeldBackpack(held, BackpackTier.GOLD);
            if (container.getContainerSize() != 45) return fail("a gold backpack in the hand has " + container.getContainerSize() + " slots");
            container.setItem(3, new ItemStack(Items.EMERALD, 9));
            ItemContainerContents inside = held.get(DataComponents.CONTAINER);
            if (inside == null || !inside.getStackInSlot(3).is(Items.EMERALD)) return fail("the held backpack did not write its contents into the stack");
            HeldBackpack reopened = new HeldBackpack(held, BackpackTier.GOLD);
            if (!reopened.getItem(3).is(Items.EMERALD) || reopened.getItem(3).getCount() != 9) return fail("a reopened held backpack lost its contents");

            // ---- the window
            BackpackMenu menu = new BackpackMenu(7, player.getInventory(), container, BackpackTier.GOLD, 0);
            if (menu.slots.size() != 45 + 36) return fail("the gold window has " + menu.slots.size() + " slots, expected 81");
            if (menu.getSlot(0).mayPlace(BackpackItem.of(BackpackTier.LEATHER))) return fail("a backpack can be put inside a backpack");
            if (!menu.getSlot(0).mayPlace(new ItemStack(Items.DIAMOND))) return fail("the window refuses a diamond");
            menu.removed(player);

            // ---- the recipes
            var manager = server.getRecipeManager();
            for (String id : new String[]{"backpack", "backpack_upgrade", "backpack_upgrade_iron", "backpack_upgrade_gold", "backpack_upgrade_diamond", "backpack_upgrade_netherite"}) {
                if (manager.byKey(ResourceLocation.fromNamespaceAndPath("nusmp", id)).isEmpty()) return fail("the recipe nusmp:" + id + " is not loaded");
            }
            Optional<RecipeHolder<?>> holder = manager.byKey(ResourceLocation.fromNamespaceAndPath("nusmp", "backpack_upgrade"));
            if (holder.isEmpty() || !(holder.get().value() instanceof BackpackUpgradeRecipe recipe)) return fail("nusmp:backpack_upgrade is not the upgrade recipe");
            ItemStack pack = dropped.copy();
            ItemStack gold = new ItemStack(BackpackRegistry.upgradeItem(BackpackTier.GOLD));
            CraftingInput good = CraftingInput.of(2, 1, List.of(pack, gold));
            if (!recipe.matches(good, level)) return fail("a backpack and a gold upgrade do not match the upgrade recipe");
            ItemStack result = recipe.assemble(good, level.registryAccess());
            if (BackpackItem.tierOf(result) != BackpackTier.GOLD || BackpackItem.filled(result) != 2) return fail("the upgrade recipe gave tier " + BackpackItem.tierOf(result) + " with " + BackpackItem.filled(result) + " stacks");
            CraftingInput smaller = CraftingInput.of(2, 1, List.of(BackpackItem.of(BackpackTier.NETHERITE), new ItemStack(BackpackRegistry.upgradeItem(BackpackTier.IRON))));
            if (recipe.matches(smaller, level)) return fail("a netherite backpack matched an iron upgrade");
            CraftingInput stray = CraftingInput.of(3, 1, List.of(pack, gold, new ItemStack(Items.STICK)));
            if (recipe.matches(stray, level)) return fail("the upgrade recipe matched with a stray item in the grid");

            // ---- the placed-block hook
            String hook = "no listener was installed by another system, so one was installed and removed";
            if (PlacedHook.installed()) {
                hook = "a listener is installed (the dungeon guard's); called it once";
                PlacedHook.fire(level, pos, player);
            } else {
                AtomicInteger heard = new AtomicInteger();
                PlacedHook.install((lv, p, by) -> heard.incrementAndGet());
                PlacedHook.fire(level, pos, player);
                PlacedHook.install(null);
                if (heard.get() != 1) return fail("the placed-block hook did not reach its listener");
            }
            return SelfTests.Result.ok("5 tiers 27/36/45/54/63, upgrade in place kept items, loot table carried tier and contents, held pack saves, window refuses a pack, recipes loaded; " + hook);
        } finally {
            set(level, pos, before);
            set(level, pos2, before2);
        }
    }

    private static SelfTests.Result drops(MinecraftServer server) {
        int n = 10000;
        SplittableRandom r = new SplittableRandom(31337);
        Dice dice = r::nextDouble;
        // the first floors: only the plain backpack, never an upgrade
        int plainHits = 0;
        for (int i = 0; i < n; i++) {
            BackpackDrops.Drop d = BackpackDrops.roll(dice, 20.0, 3, LuckGauss.Kind.CHEST, 1.0);
            if (d.upgrade() != null) return fail("an upgrade dropped on floor 3");
            if (d.backpack()) plainHits++;
        }
        if (plainHits == 0) return fail("no backpack at all in " + n + " chests of floor 3 with Luck 20");
        // a deep floor: never above the cap
        int[] byTier = new int[BackpackTier.values().length];
        int hits0 = 0, hits20 = 0;
        for (int k = 0; k < 2; k++) {
            double luck = k == 0 ? 0.0 : 20.0;
            for (int i = 0; i < n; i++) {
                BackpackDrops.Drop d = BackpackDrops.roll(dice, luck, 150, LuckGauss.Kind.CHEST, 1.0);
                if (!d.any()) continue;
                if (k == 0) hits0++;
                else hits20++;
                if (d.upgrade() != null) {
                    if (d.upgrade().ordinal() > BackpackDrops.cap(150).ordinal()) return fail("an upgrade above the floor's cap dropped: " + d.upgrade());
                    byTier[d.upgrade().ordinal()]++;
                }
            }
        }
        if (hits20 <= hits0) return fail("Luck did not raise the backpack chance: " + hits0 + " vs " + hits20);
        if (byTier[BackpackTier.NETHERITE.ordinal()] != 0) return fail("a netherite upgrade dropped on floor 150");
        return SelfTests.Result.ok("floor 3: " + plainHits + " plain packs and no upgrade; floor 150: drops " + hits0 + " -> " + hits20 + " with Luck 20, upgrades by tier " + java.util.Arrays.toString(byTier));
    }
}
