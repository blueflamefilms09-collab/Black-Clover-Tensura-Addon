package com.newuniverse.nusmp.labyrinth.mega;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.LabyrinthConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * Why: the Gateway (the great ruin in the Overworld whose gate leads into the Mega-Dungeon, {@link Labyrinth#MEGA_GATEWAY_TAG}) is the only way in once the
 * legacy gates are switched off, so a player who digs it up, or a creeper that blows the portal out of its arch, would shut every other player of a
 * survival server out of the dungeon for good. The dungeon itself is held by the Dungeon Guard (the whole dimension is unbreakable), but the Gateway
 * stands in an ordinary world, where nothing holds it: this class does, in the two ways blocks usually leave a ruin, mining and explosions. Both ask the
 * structure manager whether the cell is inside a piece of a Gateway structure (a cheap lookup of the chunk's structure references), so the rest of the
 * world pays nothing. A builder (creative mode AND operator) can still edit the ruin, so a server owner is never locked out of their own world; fire,
 * pistons and mob griefing are left alone on purpose: the Gateway's walls are stone and deepslate, and a piston or a wither is an effort nobody makes by
 * accident. The switch is {@code gatewayUnbreakable} in the Labyrinth config; off, the ruin is an ordinary structure.
 */
public final class GatewayGuard {
    private GatewayGuard() {}

    private static final String STAMP = "nusmp_gateway_guard_msg";
    /** A player who keeps swinging at the wall hears the line at most once every 4 seconds. */
    private static final int MESSAGE_INTERVAL_TICKS = 80;

    /** Is this cell part of a Gateway structure piece, in a level where the guard is on (server side, not one of the Labyrinth dimensions, switch on)? */
    public static boolean holds(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || Labyrinth.in(level)) return false;
        if (!LabyrinthConfig.get(LabyrinthConfig.GATEWAY_UNBREAKABLE)) return false;
        return server.structureManager().getStructureWithPieceAt(pos, Labyrinth.MEGA_GATEWAY_TAG).isValid();
    }

    /** May this player edit the ruin? Only a builder: creative mode and operator rights together. */
    public static boolean mayEdit(Player player) {
        return player != null && player.isCreative() && player.hasPermissions(2);
    }

    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getLevel() instanceof Level level) || level.isClientSide()) return;
        Player player = e.getPlayer();
        if (mayEdit(player) || !holds(level, e.getPos())) return;
        e.setCanceled(true);
        tell(player);
    }

    /** An explosion takes everything but the ruin: the positions inside a Gateway piece are dropped from its list before the blocks are destroyed. */
    public static void onExplosion(ExplosionEvent.Detonate e) {
        Level level = e.getLevel();
        if (level == null || level.isClientSide() || e.getAffectedBlocks().isEmpty()) return;
        e.getAffectedBlocks().removeIf(pos -> holds(level, pos));
    }

    private static void tell(Player player) {
        if (player == null) return;
        long now = player.level().getGameTime();
        CompoundTag data = player.getPersistentData();
        if (now - data.getLong(STAMP) < MESSAGE_INTERVAL_TICKS && now >= data.getLong(STAMP)) return;
        data.putLong(STAMP, now);
        player.displayClientMessage(Component.translatable("message.nusmp.mega.gateway.holds").withStyle(ChatFormatting.GRAY), true);
    }
}
