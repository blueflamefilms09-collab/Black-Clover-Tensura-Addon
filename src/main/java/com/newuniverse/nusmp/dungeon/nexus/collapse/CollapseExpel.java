package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonEffects;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.List;

/**
 * The end for whoever is still inside when the clock runs out (docs/living_dungeon.md section 8): the Nexus is wiped and they are thrown out,
 * not killed. The roots take heavy damage from them ({@link CollapseRules#EXPEL_SHARE} of their maximum health) and poison them with
 * magicules, and {@link LivingDungeon#leave} sets them down where they came down from.
 *
 * <p>{@link #LETHAL} is the one switch: false (the default) clamps the damage so that half a heart is always left; set it to true and the
 * same blow is dealt as real damage and can kill. Creative and spectating players are not touched.
 */
final class CollapseExpel {
    private CollapseExpel() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** false: the expulsion never kills (health is set, not struck, and half a heart is always left). true: it is struck as magic damage and can kill. */
    private static final boolean LETHAL = false;

    /** Throws out each of these players that may be hurt (survival and adventure). Returns how many were expelled. */
    static int run(ServerLevel level, List<ServerPlayer> inside) {
        int n = 0;
        for (ServerPlayer p : inside) {
            if (!p.isAlive() || p.isSpectator() || p.isCreative() || !LivingDungeon.in(p)) continue;
            try {
                one(level, p);
                n++;
            } catch (RuntimeException | LinkageError failure) {
                LOG.warn("[nusmp] Could not expel {} from the collapsing Nexus", p.getName().getString(), failure);
            }
        }
        return n;
    }

    private static void one(ServerLevel level, ServerPlayer p) {
        float damage = CollapseRules.expelDamage(p.getHealth(), p.getMaxHealth(), LETHAL);
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1.0f, 0.6f);
        if (damage > 0f) {
            if (LETHAL) p.hurt(level.damageSources().magic(), damage);
            else p.setHealth(Math.max(CollapseRules.SPARED_HEALTH, p.getHealth() - damage));
        }
        if (!p.isAlive()) return;                                                       // only reachable with LETHAL
        p.addEffect(new MobEffectInstance(DungeonEffects.MAGICULE_POISONING, CollapseRules.POISON_TICKS, CollapseRules.POISON_LEVEL, false, true));
        CollapseUi.title(p, "THE NEXUS IS GONE", "the roots throw you out", 50, ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        CollapseUi.chat(p, CollapseText.EXPELLED, ChatFormatting.DARK_RED);
        LivingDungeon.leave(p);
    }
}
