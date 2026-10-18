package com.newuniverse.nusmp.elite.forge.station;

import com.newuniverse.nusmp.elite.forge.ForgeConfig;
import com.newuniverse.nusmp.elite.forge.ForgeRegistry;
import com.newuniverse.nusmp.elite.forge.block.ForgeStationBlock;
import com.newuniverse.nusmp.elite.forge.block.ForgeStationBlockEntity;
import com.newuniverse.nusmp.elite.forge.item.UpgradeKitItem;
import com.newuniverse.nusmp.elite.forge.rules.StationTier;
import com.newuniverse.nusmp.elite.forge.session.ForgeSessions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Why: the Station Upgrade handler. A station is upgraded by right clicking it with the upgrade kit of the NEXT tier; the kit (crafted from that tier's materials,
 * which is the cost) is used up, the block becomes the next tier's block in the same place, and the station's own statistics (items forged, best quality) are copied to
 * the new block entity, with its upgrade count raised. The rules, all checked on the server before anything changes: the system is enabled; the kit's target is exactly
 * the station's next tier (no skipping a tier, no re-using a lower kit); nobody is forging at the station right now. Creative players keep their kit. One entry point
 * so the block, the command and the self-test all use the same rule.
 */
public final class StationUpgrade {
    private StationUpgrade() {}

    /** Why an upgrade cannot happen, or null when it can. */
    public static String refusal(ServerLevel level, BlockPos pos, ItemStack kit) {
        if (!ForgeConfig.enabled()) return "nusmp.forge.msg.disabled";
        if (!(kit.getItem() instanceof UpgradeKitItem k)) return "nusmp.forge.upgrade.not_kit";
        StationTier cur = ForgeStationBlock.tierAt(level, pos);
        if (cur == null) return "nusmp.forge.upgrade.not_station";
        if (cur.next() == null) return "nusmp.forge.upgrade.max";
        if (k.target() != cur.next()) return "nusmp.forge.upgrade.wrong_kit";
        if (ForgeSessions.activeAt(level, pos)) return "nusmp.forge.upgrade.in_use";
        return null;
    }

    /** Upgrades the station at the position with the kit if allowed; tells the player either way. Returns true when it changed. */
    public static boolean tryUpgrade(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack kit) {
        String why = refusal(level, pos, kit);
        if (why != null) {
            player.displayClientMessage(Component.translatable(why), true);
            return false;
        }
        StationTier next = ((UpgradeKitItem) kit.getItem()).target();
        if (!upgrade(level, pos, next)) {
            player.displayClientMessage(Component.translatable("nusmp.forge.upgrade.failed"), true);
            return false;
        }
        if (!player.getAbilities().instabuild) kit.shrink(1);
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 0.7F);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6F, 1.4F);
        player.displayClientMessage(Component.translatable("nusmp.forge.upgrade.done", Component.translatable("nusmp.forge.tier." + next.id())), false);
        return true;
    }

    /** Replaces the station block with the tier's block, carrying the block entity's numbers across. */
    public static boolean upgrade(ServerLevel level, BlockPos pos, StationTier next) {
        BlockState old = level.getBlockState(pos);
        if (!(old.getBlock() instanceof ForgeStationBlock)) return false;
        ForgeStationBlockEntity before = level.getBlockEntity(pos) instanceof ForgeStationBlockEntity be ? be : null;
        int forged = before == null ? 0 : before.forged(), best = before == null ? 0 : before.best(), ups = before == null ? 0 : before.upgrades();
        Block block = ForgeRegistry.station(next).get();
        level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof ForgeStationBlockEntity after) after.restore(forged, best, ups + 1);
        return true;
    }
}
