package com.newuniverse.nusmp.relics.djinn;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * A Djinn Seal: the dormant Djinn, as an item. Use it with a Metal Vessel (a Legend-grade relic) in the other hand to bind it ({@link DjinnBinding}).
 * Dropped, it cannot be burned, blown up or despawned, and if it falls out of the world it is put back on the highest block above where it fell:
 * a Djinn is never lost with its Seal. Every Seal glints.
 */
public class DjinnSealItem extends Item {
    private final Djinn djinn;

    public DjinnSealItem(Djinn djinn, Item.Properties props) {
        super(props);
        this.djinn = djinn;
    }

    public Djinn djinn() { return djinn; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack seal = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.sidedSuccess(seal, level.isClientSide);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        DjinnBinding.attempt(p, seal, other);
        return InteractionResultHolder.sidedSuccess(seal, false);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    /** Never lost: no despawn, no damage, and a Seal under the world is lifted back to the surface. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!(entity.level() instanceof ServerLevel sl)) return false;
        entity.setUnlimitedLifetime();
        entity.setInvulnerable(true);
        if (entity.getY() < sl.getMinBuildHeight() - 8) {
            BlockPos up = surface(sl, entity.blockPosition());
            entity.setPos(up.getX() + 0.5, up.getY(), up.getZ() + 0.5);
            entity.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
        if (entity.tickCount % 100 == 7 && sl.getServer() != null)
            DjinnLedger.get(sl.getServer()).seen(djinn, sl.getServer().overworld().getGameTime(), sl.dimension().location().toString(), entity.blockPosition());
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal(djinn.fullName()).withColor(djinn.color()));
        tip.add(Component.literal(djinn.affinity().label + " Djinn, asleep in this seal").withStyle(ChatFormatting.GRAY));
        for (String line : wrap("Use it with a Legend-grade Metal Vessel, attuned to you, in your other hand.", 38)) tip.add(Component.literal(line).withStyle(ChatFormatting.DARK_GRAY));
        for (String line : wrap("Price: a full Magoi bar, a quarter of your EP, three high-quality magic crystals.", 38)) tip.add(Component.literal(line).withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.literal("Only one of each Djinn exists.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    /** Greedy word wrap at {@code width} characters (tooltips must never run off the screen). */
    static List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String w : text.split(" ")) {
            if (line.length() > 0 && line.length() + 1 + w.length() > width) { out.add(line.toString()); line.setLength(0); }
            if (line.length() > 0) line.append(' ');
            line.append(w);
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    /** The block a dropped Seal is lifted to when it falls out of the world: just above the highest block of its column. */
    static BlockPos surface(ServerLevel sl, BlockPos from) {
        int y = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, from.getX(), from.getZ());
        return new BlockPos(from.getX(), Math.max(y, sl.getMinBuildHeight()) + 1, from.getZ());
    }
}
