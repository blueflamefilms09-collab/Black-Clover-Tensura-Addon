package com.newuniverse.nusmp.elite.items.crate;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.elite.items.EliteItemsConfig;
import com.newuniverse.nusmp.elite.items.EliteSounds;
import com.newuniverse.nusmp.elite.items.api.CrateOpenedEvent;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: everything that happens when a key meets a crate, in order: the guards (the system and the crates switched on, the crate not already mid-opening), the key taken (creative players keep theirs),
 * the table rolled and the rewards GIVEN at once ({@link CrateRoller#open}, so a refused reward is re-rolled), then the show: the lid goes up ({@code open} state plus a scheduled block tick to close
 * it), the lock sound, a magic circle in the tier's colour, Light Rukh (Dark Rukh too for an Elite Crate), and, a moment later, the reveal: a flare, the shimmer and one chat line per reward. The
 * rewards are given BEFORE the animation so a player who logs out or whose game crashes in the middle loses nothing; the animation is only for show. Mod VFX only (VfxSpawn shapes, Rukh).
 */
public final class CrateOpening {
    private CrateOpening() {}

    /** Game time at which a crate position may be opened again (an animation runs 36 ticks by default). */
    private static final Map<Long, Long> BUSY = new HashMap<>();

    /** Opens a crate with the key in {@code key}. Returns false when nothing happened (a switched-off system, a crate that is still opening). Server thread. */
    public static boolean open(ServerPlayer p, ServerLevel level, BlockPos pos, BlockState state, CrateTier tier, ItemStack key) {
        if (!EliteItemsConfig.enabled() || !EliteItemsConfig.get(EliteItemsConfig.CRATES_ENABLED)) {
            p.displayClientMessage(Component.translatableWithFallback("message.nusmp.crate.sealed", "The crates are sealed on this server.").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        long now = level.getGameTime();
        Long until = BUSY.get(pos.asLong());
        if (until != null && now < until) return false;
        BUSY.put(pos.asLong(), now + CrateBlock.animationTicks() + 4L);
        if (BUSY.size() > 512) BUSY.entrySet().removeIf(e -> e.getValue() < now);

        if (!p.getAbilities().instabuild) key.shrink(1);

        CrateTable table = CrateTables.of(tier);
        List<Component> given = CrateRoller.open(table, p.getRandom()::nextDouble, roll -> grant(p, roll));

        // ---- the show
        int anim = CrateBlock.animationTicks();
        if (state.hasProperty(CrateBlock.OPEN)) {
            level.setBlock(pos, state.setValue(CrateBlock.OPEN, true), 3);
            level.scheduleTick(pos, state.getBlock(), anim);
        }
        Vec3 c = Vec3.atCenterOf(pos).add(0, 0.2, 0);
        int argb = 0xFF000000 | tier.color();
        level.playSound(null, pos, EliteSounds.CRATE_UNLOCK.get(), SoundSource.BLOCKS, 1.0f, 0.9f + 0.1f * tier.ordinal());
        VfxSpawn.send(level, VfxShape.MAGIC_CIRCLE, c, c.add(0, 1, 0), argb, anim + 10, 0.8f + 0.25f * tier.ordinal());
        SpellRuntime.later(level, 8, () -> {
            RukhFx.burst(level, c.add(0, 0.6, 0), RukhFx.Alignment.LIGHT, 6 + 3 * tier.ordinal(), 0.5);
            if (tier == CrateTier.ELITE) RukhFx.burst(level, c.add(0, 0.6, 0), RukhFx.Alignment.DARK, 8, 0.5);
        });
        SpellRuntime.later(level, Math.max(10, anim - 6), () -> {
            VfxSpawn.send(level, VfxShape.LIGHT_FLARE, c.add(0, 0.8, 0), c.add(0, 1.8, 0), argb, 20, 0.7f + 0.2f * tier.ordinal());
            level.playSound(null, pos, EliteSounds.CRATE_REVEAL.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            if (!p.isRemoved()) {
                p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.crate.opened", "The %s Crate opens:",
                        Component.translatableWithFallback("crate_tier.nusmp." + tier.id(), CrateKeyItem.cap(tier.id()))).withStyle(Style.of(tier)));
                if (given.isEmpty()) p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.crate.empty", "  ...it is empty.").withStyle(ChatFormatting.GRAY));
                for (Component line : given) p.sendSystemMessage(Component.literal("  - ").withStyle(ChatFormatting.DARK_GRAY).append(line.copy().withStyle(ChatFormatting.WHITE)));
            }
        });

        NeoForge.EVENT_BUS.post(new CrateOpenedEvent(p, tier.id(), given.size()));
        return true;
    }

    private static Component grant(ServerPlayer p, CrateRoller.Roll roll) {
        RewardType type = RewardSerializers.get(roll.entry().type());
        if (type == null) return null;
        try {
            return type.grant(p, roll.entry(), roll.amount(), p.getRandom()::nextDouble);
        } catch (RuntimeException ex) {
            com.mojang.logging.LogUtils.getLogger().warn("[nusmp] crate reward {} failed and is re-rolled", roll.entry().type(), ex);
            return null;
        }
    }

    /** The chat style of a tier (the tier colour, bold). */
    private static final class Style {
        static net.minecraft.network.chat.Style of(CrateTier t) { return net.minecraft.network.chat.Style.EMPTY.withColor(t.color()).withBold(true); }
    }
}
