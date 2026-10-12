package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Set;
import java.util.UUID;

/**
 * The reward for cutting a Dungeon Maw's roots (docs/living_dungeon.md section 2). {@link #onBreak} remembers who chops the roots of an
 * open Maw ({@link MawCredit}, in memory, with an expiry); when the Maw closes with every root cut, {@link #onClosed} gives each of them
 * within 32 blocks experience and 1 to 3 Maw Fruit, more the sooner the roots were cut ({@link MawFruitMath}). This is the one thing the
 * Dungeon hands out as loot: it is a reward for the work, not a drop.
 */
public final class MawReward {
    private MawReward() {}

    /** The fruit the cut roots give up: it feeds, mends a little and lifts a wizard's spells like a cooked meal does, a little more. */
    public static final DeferredItem<Item> MAW_FRUIT = NUItems.ITEMS.register("maw_fruit", () -> new DungeonFood.Meal(new Item.Properties().food(
            new FoodProperties.Builder().nutrition(6).saturationModifier(0.6f).alwaysEdible()
                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 200, 0), 1f).build()).rarity(Rarity.EPIC),
            1.15, 1.0, 3600, "Maw-fed: spells hit 15% harder for 3 minutes, and the roots mend your wounds"));

    public static void init() {}

    private static final MawCredit CREDIT = new MawCredit();
    private static final double REACH = 32.0;

    private static String key(String dim, long pos) { return dim + "@" + pos; }

    /** Forgets every credit (the server is stopping, and the next world must not inherit them). */
    public static void clear() { CREDIT.clear(); }

    // ---------------------------------------------------------------- who cuts the roots
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (e.isCanceled() || !(e.getLevel() instanceof ServerLevel sl) || !(e.getPlayer() instanceof ServerPlayer p)) return;
        if (!e.getState().is(DungeonBlocks.MAW_ROOT.get()) || !DungeonConfig.get(DungeonConfig.ENABLED)) return;
        long packed = e.getPos().asLong(), now = sl.getGameTime();
        String dim = sl.dimension().location().toString();
        for (DungeonSave.Maw m : DungeonSave.get(sl.getServer()).maws) {
            if (!m.dim().equals(dim)) continue;
            for (long root : m.roots()) {
                if (root != packed) continue;
                CREDIT.record(key(dim, m.pos()), p.getUUID(), now, Math.max(m.closeAt(), now) + 1200L);
                return;
            }
        }
    }

    // ---------------------------------------------------------------- the Maw closes
    /**
     * Called by DungeonMaw when a Maw closes. {@code rootsCut}: it closed because every root was cut down. {@code closeAt}: when it would
     * have closed by itself, {@code now}: the game time of this closing. The credits of this Maw are dropped either way.
     */
    public static void onClosed(ServerLevel level, BlockPos at, boolean rootsCut, long closeAt, long now) {
        Set<UUID> cutters = CREDIT.take(key(level.dimension().location().toString(), at.asLong()), now);
        if (!rootsCut || cutters.isEmpty()) return;
        long total = DungeonConfig.get(DungeonConfig.MAW_MINUTES) * 1200L, left = closeAt - now;
        int fruit = MawFruitMath.count(left, total), xp = MawFruitMath.xp(left, total);
        for (UUID id : cutters) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
            if (p == null || p.level() != level || p.distanceToSqr(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5) > REACH * REACH) continue;
            try {
                reward(p, fruit, xp);
            } catch (RuntimeException failed) { /* a reward must never keep the Maw from closing */ }
        }
    }

    private static void reward(ServerPlayer p, int fruit, int xp) {
        ServerLevel level = p.serverLevel();
        p.giveExperiencePoints(xp);
        ItemStack stack = new ItemStack(MAW_FRUIT.get(), fruit);
        p.getInventory().add(stack);
        if (!stack.isEmpty()) {                                          // no room in the pack: it lands at their feet
            ItemEntity drop = new ItemEntity(level, p.getX(), p.getY() + 0.3, p.getZ(), stack);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1.0, p.getZ(), 14, 0.5, 0.6, 0.5, 0.02);
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
        p.sendSystemMessage(Component.literal("The roots wither and let go of their fruit: " + fruit + " Maw Fruit and " + xp + " experience.").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
