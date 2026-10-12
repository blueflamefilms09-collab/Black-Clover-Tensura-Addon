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
 *   <li><b>Gale Feather</b> (Sylph, the Wind Spirit Lord): Gale Leap, a gust flings you forward and up and slows your fall for 8 s (12 s cooldown).</li>
 * </ul>
 * Zagred's drops live in {@link BossRelics}.
 */
public final class BossDrops {
    private BossDrops() {}

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
