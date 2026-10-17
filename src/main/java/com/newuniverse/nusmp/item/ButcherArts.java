package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.item.MagicWeaponItem.Kind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * 1.19.0: the Butcher Cleaver, the cook's tool of the dungeon. Right-click on a living thing in front of you is the <b>Anatomical
 * Cut</b>: a precise blow at the weak point that armour does not stop and that bleeds. (Until 1.58.0 it also marked a creature of the Living
 * Dungeon as dressed, so its carcass left an extra portion of meat, and cut the meat out of that carcass along the anatomy; the carcass went with
 * the Living Dungeon.)
 */
final class ButcherArts {
    private ButcherArts() {}

    static boolean technique(ServerPlayer p, ItemStack stack) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        List<LivingEntity> near = GrimoireBook.around(p, p.position(), 4.5).stream()
                .filter(t -> t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) > 0.5)
                .sorted(Comparator.comparingDouble(t -> t.distanceToSqr(p))).toList();
        if (near.isEmpty()) { GrimoireBook.fail(p, "Nothing in reach to cut."); return false; }
        LivingEntity t = near.get(0);
        ServerLevel sl = p.serverLevel();
        t.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.damage(p, t, 9f, 0.6));           // armour does not stop a cut at the weak point
        t.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));                              // it bleeds
        VfxSpawn.send(sl, VfxShape.WIND_SLASH, eye.add(look.scale(0.5)), t.getBoundingBox().getCenter(), 0xFF8E1B2A, 8, 1.2f);
        sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 0.7f);
        return true;
    }

    static boolean isMine(Kind k) { return k == Kind.BUTCHER_CLEAVER; }

    static void tooltip(List<Component> tip) {
        tip.add(Component.literal("\"If it can be killed, it can be cooked.\"").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
