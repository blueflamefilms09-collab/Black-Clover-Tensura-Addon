package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

/**
 * 0.100: what the bosses leave behind, each a small, usable piece of its anime (tuned so none of them breaks a fight):
 * <ul>
 *   <li><b>Black Bull Bard Relic</b> (Riven Remake, was an inert trophy): sneak-use is Bardic Inspiration, a song that gives you and allies
 *       within 12 blocks Strength, Speed and Regeneration for 20 s (2 min cooldown).</li>
 *   <li><b>Story Page</b> (Riven Remake, consumable): Rewrite, a page rewrites your last few seconds: negative effects are erased and you mend 40%
 *       of your health (30 s cooldown between pages).</li>
 *   <li><b>Gale Feather</b> (Sylph, the Wind Spirit Lord): Gale Leap, a gust flings you forward and up and slows your fall for 8 s (12 s cooldown).</li>
 * </ul>
 * Zagred's drops live in {@link BossRelics}.
 */
public final class BossDrops {
    private BossDrops() {}

    public static void bardicInspiration(ServerPlayer p) {
        for (Player o : p.serverLevel().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(12), x -> x == p || x.isAlliedTo(p))) {
            o.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, 0));
            o.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 0));
            o.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 0));
        }
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.KOTO_AURA, p, p.position(), 0xFF7A5CFF, 60, 1.6f);
        p.displayClientMessage(Component.literal("Bardic Inspiration!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), true);
    }

    public static void rewrite(ServerPlayer p) {
        for (MobEffectInstance e : new ArrayList<>(p.getActiveEffects())) if (!e.getEffect().value().isBeneficial()) p.removeEffect(e.getEffect());
        BalanceLaw.heal(p, p.getMaxHealth() * 0.4f);
        VfxSpawn.send(p.serverLevel(), VfxShape.KOTO_SHATTER, p.getBoundingBox().getCenter(), p.position(), 0xFFD8CCFF, 24, 1.2f);
        p.displayClientMessage(Component.literal("Rewritten.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), true);
    }

    public static void galeLeap(ServerPlayer p) {
        Vec3 look = p.getViewVector(1f);
        p.setDeltaMovement(look.x * 1.7, 0.75 + Math.max(0, look.y) * 0.6, look.z * 1.7);
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0));
        VfxSpawn.send(p.serverLevel(), VfxShape.WIND_RING, p.position(), p.position().add(0, 1, 0), 0xFFB8F0D0, 18, 1.4f);
        p.displayClientMessage(Component.literal("Gale Leap!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true);
    }
}
