package com.newuniverse.nusmp.arc.arena;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SolidBucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;

/**
 * Why: the story arenas are closed rooms with one guest, and they must stay as they were built. Breaking is already refused by the dungeon guard, which holds
 * this dimension (unless an operator is editing); the rest is refused here, as the Final Hour's arena does: placing a block or emptying a bucket (the floor is the
 * fight's furniture), and any natural spawn (a cow on the plain, a skeleton in the pit: nothing joins the boss). The way home is a block: a click on the
 * lodestone of a way-home pad carries the player home, unless the boss is fighting, when the Chronicle Sigil's ten-second hold is the way out (leaving a lost fight
 * is allowed, but it costs time). Server side only, never throws out of an event.
 */
public final class ArcArenaEvents {
    private ArcArenaEvents() {}

    private static final Logger LOG = LogUtils.getLogger();

    private static boolean registered;

    public static synchronized void register(IEventBus bus) {
        if (registered) return;
        registered = true;
        bus.addListener(EventPriority.HIGHEST, ArcArenaEvents::onPlace);
        bus.addListener(EventPriority.HIGHEST, ArcArenaEvents::onUse);
        bus.addListener(EventPriority.HIGHEST, ArcArenaEvents::onFinalize);
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent e) {
        try {
            if (ArcArena.editing() || !(e.getLevel() instanceof Level l) || l.isClientSide() || !ArcArena.in(l)) return;
            e.setCanceled(true);
        } catch (RuntimeException ex) {
            LOG.error("[nusmp] arena place guard failed: {}", ex.toString());
        }
    }

    private static void onFinalize(FinalizeSpawnEvent e) {
        try {
            if (!ArcArena.in(e.getLevel().getLevel())) return;
            MobSpawnType t = e.getSpawnType();
            boolean natural = t == MobSpawnType.NATURAL || t == MobSpawnType.CHUNK_GENERATION || t == MobSpawnType.PATROL || t == MobSpawnType.REINFORCEMENT;
            if (natural) e.setSpawnCancelled(true);
        } catch (RuntimeException ex) {
            LOG.error("[nusmp] arena spawn guard failed: {}", ex.toString());
        }
    }

    /** Blocks, buckets and snow-like items in hand do not place (the item part of the click is denied); the block part (the way-home lodestone) still works. */
    private static void onUse(PlayerInteractEvent.RightClickBlock e) {
        try {
            if (!ArcArena.in(e.getLevel())) return;
            if (!ArcArena.editing()) {
                ItemStack s = e.getItemStack();
                if (s.getItem() instanceof BlockItem || s.getItem() instanceof BucketItem || s.getItem() instanceof SolidBucketItem) e.setUseItem(TriState.FALSE);
            }
            if (e.getLevel().isClientSide() || e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer sp)) return;
            if (!e.getLevel().getBlockState(e.getPos()).is(Blocks.LODESTONE)) return;
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
            if (ArcArenaDirector.fightRunningAt(sp.getServer(), sp)) {
                sp.displayClientMessage(Component.translatableWithFallback("message.nusmp.arc.arena.sigil_dormant",
                        "The sigil is dormant while the boss fights. Hold the Chronicle Sigil for ten seconds to leave.").withStyle(ChatFormatting.GOLD), true);
                return;
            }
            ArcArenaTravel.leave(sp);
        } catch (RuntimeException ex) {
            LOG.error("[nusmp] arena click guard failed: {}", ex.toString());
        }
    }
}
