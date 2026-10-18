package com.newuniverse.nusmp.heroweapon.rules;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.bond.HeroLevels;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the weapon's own experience (the series' level of the weapon) must be granted through ONE method, because the interference rule has to be able to refuse
 * it. Blows, kills, skill hits and mastery all call {@link #gain}; the method adds the experience to the bond unless the player is blocked by
 * {@link HeroInterference}, applies the config multiplier and the level ceiling, and returns what was really added. A level gained is told to the wielder and
 * fires the quest hook {@code hero.level} once per level (so "reach level N" is "the hook has fired N times"). Kill experience has its own door,
 * {@link #gainKill}, which turns the victim's health into points with the pure {@link KillXp}.
 */
public final class HeroXp {
    private HeroXp() {}

    /** Gives the player's bond {@code amount} experience (before the config multiplier); returns the experience actually added, 0 when blocked, capped out or unbound. */
    public static long gain(ServerPlayer player, long amount) {
        if (amount <= 0 || !HeroConfig.enabled()) return 0;
        HeroBond b = HeroBonds.of(player);
        if (!b.isBound()) return 0;
        if (HeroInterference.blocked(player)) {
            HeroInterference.noteRepelled(player);
            return 0;
        }
        long scaled = Math.round(amount * HeroConfig.expMultiplier());
        if (scaled <= 0) return 0;
        long room = Math.max(0L, HeroLevels.expFor(HeroConfig.maxLevel()) - b.exp());
        long add = Math.min(scaled, room);
        if (add <= 0) return 0;
        int before = b.level();
        b.addExp(add);
        int after = b.level();
        if (after > before) levelUp(player, b.kind(), before, after);
        return add;
    }

    /** The experience for a kill of {@code victim} by the wielder: its maximum health times the configured share, times the boss factor, capped; 0 when blocked. */
    public static long gainKill(ServerPlayer player, LivingEntity victim) {
        if (victim == null || victim == player) return 0;
        long pts = KillXp.forKill(victim.getMaxHealth(), BalanceLaw.isBoss(victim), HeroConfig.killExpShare(), HeroConfig.bossKillExpFactor(), HeroConfig.maxKillExp());
        return pts <= 0 ? 0 : gain(player, pts);
    }

    private static void levelUp(ServerPlayer p, HeroKind kind, int from, int to) {
        if (HeroConfig.showMessages())
            p.displayClientMessage(Component.translatable("hero.msg.level", Component.translatable("item.nusmp." + kind.itemId()), to).withStyle(ChatFormatting.GOLD), true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.3f);
        for (int l = from + 1; l <= to; l++) NusmpEvents.fire(p, "hero.level", kind.itemId());
    }
}
