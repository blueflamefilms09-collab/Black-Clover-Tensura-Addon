package com.newuniverse.nusmp.labyrinth.trap;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.dnd.DndConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Why: the snuffer glyph's second half, the refusal to put a light down, has to be an event handler: a block cannot veto the placement
 * of a block that is not itself. {@link BlockEvent.EntityPlaceEvent} is the NeoForge hook fired when an ENTITY (a player with a torch, a
 * dispenser-less case never reaches it) places a block, it is cancellable, and cancelling it makes the server refuse the placement and
 * resync the client. It does not fire for a structure template placing its own lanterns, so the Spire keeps its light. The handler is
 * confined three ways so it cannot become a nuisance elsewhere: Labyrinth dimension only, survival and adventure players only (a
 * creative builder keeps building; the rule is {@link TrapMath#refusesLight}), and only when a glyph stands within eight blocks of the
 * placement. The search is a block scan of a 17-block cube that runs only after the cheap tests passed (light-emitting block, in the
 * Labyrinth, loaded chunks), so an ordinary placement costs nothing. Registered on the game bus by {@link TrapRegistry#register} with
 * {@code addListener}, like the rest of the mod (no annotation scanning).
 */
public final class TrapEvents {
    private TrapEvents() {}

    public static void register(IEventBus gameBus) { gameBus.addListener(TrapEvents::onPlace); }

    static void onPlace(BlockEvent.EntityPlaceEvent e) {
        LevelAccessor acc = e.getLevel();
        if (!(acc instanceof Level level) || level.isClientSide || !Labyrinth.in(level)) return;
        boolean player = e.getEntity() instanceof Player;
        boolean creative = player && ((Player) e.getEntity()).isCreative();
        boolean spectator = e.getEntity() != null && e.getEntity().isSpectator();
        if (!player || !TrapMath.refusesLight(e.getPlacedBlock().getLightEmission(), creative, spectator, DndConfig.get(DndConfig.TRAPS))) return;
        if (!glyphNear(level, e.getPos())) return;
        e.setCanceled(true);
        ((Player) e.getEntity()).displayClientMessage(Component.translatable("message.nusmp.trap.no_light").withStyle(ChatFormatting.DARK_GRAY), true);
    }

    /** Is a snuffer glyph within {@link TrapMath#PLACE_RADIUS} of this position? Scans only chunks that are already loaded. */
    static boolean glyphNear(Level level, BlockPos at) {
        int r = TrapMath.PLACE_RADIUS;
        BlockPos lo = at.offset(-r, -r, -r), hi = at.offset(r, r, r);
        if (!level.hasChunksAt(lo, hi)) return false;
        for (BlockPos p : BlockPos.betweenClosed(lo, hi)) {
            if (level.getBlockState(p).is(TrapRegistry.SNUFFER_GLYPH.get()) && TrapMath.inNoLightZone(p.getX() - at.getX(), p.getY() - at.getY(), p.getZ() - at.getZ())) return true;
        }
        return false;
    }
}
