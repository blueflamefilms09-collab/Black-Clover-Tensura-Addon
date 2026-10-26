package com.newuniverse.nusmp.client.skilltree;

import com.newuniverse.nusmp.skilltree.item.BookLookStack;
import com.newuniverse.nusmp.skilltree.item.BookShimmerRules;
import com.newuniverse.nusmp.skilltree.item.SkillItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: a Skill Book of a rare rarity or better, lying on the ground after a boss or a chest, should be seen from a few blocks away: it gives off a slow particle in its rarity colour.
 * Client only, draws and decides nothing (the rarity comes from the stack, the same reader the item model uses). Cost, fixed by {@link BookShimmerRules}: the item entities are collected
 * from the entity join / leave events (the world is never scanned); at most 24 are tracked; a tracked book is looked at once every 6 ticks, staggered by slot, so at most 4 particles are
 * spawned in any tick; books beyond 24 blocks of the player are skipped; a book that turns out not to be a Skill Book of rare or better is dropped from the list at once, which frees its slot;
 * nothing is kept after the player leaves the world. The particle options are built once per rank, so a tick allocates nothing but the particle itself. Off with the client setting
 * {@code skillbookLook.dropShimmer}.
 */
public final class BookDropFx {
    private BookDropFx() {}

    /** The tracked item entities (client thread only). Never longer than {@link BookShimmerRules#MAX_TRACKED}. */
    private static final List<ItemEntity> TRACKED = new ArrayList<>(BookShimmerRules.MAX_TRACKED);
    /** One options object per rank (index = rank), built once. */
    private static final DustParticleOptions[] OPTIONS = new DustParticleOptions[5];
    private static boolean done;

    static {
        for (int rank = 0; rank < OPTIONS.length; rank++) {
            int c = BookShimmerRules.colorOf(rank);
            OPTIONS[rank] = new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f), 0.9f);
        }
    }

    /** One call from {@link BookClient#init}. Safe to call twice. */
    public static synchronized void init() {
        if (done) return;
        done = true;
        NeoForge.EVENT_BUS.addListener(BookDropFx::onJoin);
        NeoForge.EVENT_BUS.addListener(BookDropFx::onLeave);
        NeoForge.EVENT_BUS.addListener(BookDropFx::onTick);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> TRACKED.clear());
    }

    private static void onJoin(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide || !(e.getEntity() instanceof ItemEntity item)) return;
        // a new item entity has no stack yet (its data follows the spawn packet), so it is kept for a short grace and judged in the tick
        if (TRACKED.size() < BookShimmerRules.MAX_TRACKED && !TRACKED.contains(item)) TRACKED.add(item);
    }

    private static void onLeave(EntityLeaveLevelEvent e) {
        if (!e.getLevel().isClientSide || !(e.getEntity() instanceof ItemEntity item)) return;
        TRACKED.remove(item);
    }

    private static void onTick(ClientTickEvent.Post e) {
        if (TRACKED.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            TRACKED.clear();
            return;
        }
        boolean on = BookLookConfig.dropShimmer;
        long tick = level.getGameTime();
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        int spawned = 0;
        for (int slot = TRACKED.size() - 1; slot >= 0; slot--) {
            ItemEntity item = TRACKED.get(slot);
            if (item.isRemoved() || item.level() != level) {
                TRACKED.remove(slot);
                continue;
            }
            ItemStack stack = item.getItem();
            if (stack.isEmpty()) {
                if (item.tickCount > BookShimmerRules.UNKNOWN_GRACE_TICKS) TRACKED.remove(slot);
                continue;
            }
            if (!stack.is(SkillItems.SKILL_BOOK.get())) {
                TRACKED.remove(slot);
                continue;
            }
            if (!on || spawned >= BookShimmerRules.MAX_PER_TICK || !BookShimmerRules.due(tick, slot)) continue;
            int rank = BookLookStack.rank(stack, SkillItems.bookIdOf(stack));
            if (!BookShimmerRules.shimmers(rank)) {
                TRACKED.remove(slot);
                continue;
            }
            if (!BookShimmerRules.inRange(item.getX() - px, item.getY() - py, item.getZ() - pz)) continue;
            spawn(level, item, OPTIONS[Math.min(rank, OPTIONS.length - 1)]);
            spawned++;
        }
    }

    private static void spawn(Level level, ItemEntity item, DustParticleOptions options) {
        double ox = (level.random.nextDouble() - 0.5) * 0.5, oz = (level.random.nextDouble() - 0.5) * 0.5;
        level.addParticle(options, item.getX() + ox, item.getY() + 0.35, item.getZ() + oz, 0.0, 0.02, 0.0);
    }
}
