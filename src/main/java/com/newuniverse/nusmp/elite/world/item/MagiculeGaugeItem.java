package com.newuniverse.nusmp.elite.world.item;

import com.newuniverse.nusmp.elite.world.ChunkMagicule;
import com.newuniverse.nusmp.elite.world.MagiculeApi;
import com.newuniverse.nusmp.elite.world.MagiculeClientCache;
import com.newuniverse.nusmp.elite.world.WorldGuard;
import com.newuniverse.nusmp.elite.world.rules.MagiculeModel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.List;
import java.util.Locale;

/**
 * Why: the detector item of the world engine ({@code nusmp:magicule_gauge}). Holding it in either hand shows the magicule density of the chunk you stand in on the screen (the HUD layer of
 * the client reads the throttled sync, see {@link MagiculeClientCache}); using it prints the detailed reading (stored density, the depth bonus, crystals, Mother Rocks, ore conversions
 * done) as an action bar message, which is also the exact number for an operator. Its tooltip names the band the chunk you are in belongs to.
 */
public class MagiculeGaugeItem extends Item {
    public MagiculeGaugeItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) sp.displayClientMessage(reading(sl, sp), true);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** The detailed reading at a player's feet. */
    public static Component reading(ServerLevel level, ServerPlayer p) {
        if (!WorldGuard.allowed(level)) return Component.translatable("nusmp.elite.magicule.off").withStyle(ChatFormatting.GRAY);
        double eff = MagiculeApi.effectiveAt(p);
        double stored = MagiculeApi.stored(level, p.blockPosition());
        LevelChunk c = MagiculeApi.loaded(level, p.blockPosition());
        ChunkMagicule d = c == null ? null : MagiculeApi.existing(c);
        int rocks = d == null || c == null ? 0 : MagiculeApi.validateMothers(c, d)[0];
        return Component.translatable("nusmp.elite.magicule.reading", String.format(Locale.ROOT, "%.0f", eff * 100.0), Component.translatable("nusmp.elite.magicule.band." + MagiculeModel.Band.of(eff).id),
                String.format(Locale.ROOT, "%.2f", stored), String.format(Locale.ROOT, "%+.2f", eff - stored), d == null ? 0 : d.crystalScore, rocks, d == null ? 0 : d.ore.total).withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.nusmp.magicule_gauge.tip").withStyle(ChatFormatting.GRAY));
        if (MagiculeClientCache.valid()) {
            double d = MagiculeClientCache.own();
            tooltip.add(Component.translatable("nusmp.elite.magicule.here", String.format(Locale.ROOT, "%.0f", d * 100.0), Component.translatable("nusmp.elite.magicule.band." + MagiculeModel.Band.of(d).id))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
