package com.newuniverse.nusmp.skill;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Why: small helpers every skill shares (action-bar messages, spending magicules, the aimed-player lookup and the cast circle). The
 * rebuild made the messages translatable: the {@link Component} overloads are what the skills use now; the String overloads stay because
 * the grimoire skills in this package still call them.
 */
final class SkillUtil {
    private SkillUtil() {}

    static void actionbar(ServerPlayer player, Component msg) {
        if (player != null && msg != null) player.displayClientMessage(msg, true);
    }

    static void fail(ServerPlayer player, Component text) {
        actionbar(player, text.copy().withStyle(ChatFormatting.RED));
    }

    static void fail(ServerPlayer player, String text) {
        actionbar(player, Component.literal(text).withStyle(ChatFormatting.RED));
    }

    /** True if the player has at least this many magicules (nothing is spent). */
    static boolean hasMagicules(ServerPlayer player, double cost) {
        if (cost <= 0) return true;
        IExistence existence = TensuraStorages.getExistenceFrom(player);
        return existence != null && existence.getMagicule() >= cost;
    }

    /** Spends magicules if the player has enough. Returns false (and spends nothing) otherwise. */
    static boolean spendMagicules(ServerPlayer player, double cost) {
        if (cost <= 0) return true;
        IExistence existence = TensuraStorages.getExistenceFrom(player);
        if (existence == null || existence.getMagicule() < cost) {
            fail(player, SkillMsg.t("no_magicules", "Not enough magicules (need %s).", (long) cost));
            return false;
        }
        existence.setMagicule(existence.getMagicule() - cost);
        existence.markDirty();
        return true;
    }

    /** Gives back magicules spent on a power that turned out not to be usable. */
    static void refund(ServerPlayer player, double amount) {
        if (amount <= 0) return;
        IExistence existence = TensuraStorages.getExistenceFrom(player);
        if (existence == null) return;
        existence.setMagicule(existence.getMagicule() + amount);
        existence.markDirty();
    }

    /** Removes every harmful effect from the entity (and fire). Returns how many effects went. */
    static int cleanse(net.minecraft.world.entity.LivingEntity entity) {
        int removed = 0;
        for (net.minecraft.world.effect.MobEffectInstance e : new java.util.ArrayList<>(entity.getActiveEffects())) {
            if (!e.getEffect().value().isBeneficial() && entity.removeEffect(e.getEffect())) removed++;
        }
        entity.clearFire();
        return removed;
    }

    /** The player you are looking at, within range and line of sight, or null. */
    static ServerPlayer lookTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        ServerPlayer best = null;
        double bestDot = 0.97;
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other == player || other.isSpectator()) continue;
            Vec3 to = other.getEyePosition().subtract(eye);
            if (to.length() > range) continue;
            double dot = to.normalize().dot(look);
            if (dot > bestDot && player.hasLineOfSight(other)) {
                bestDot = dot;
                best = other;
            }
        }
        return best;
    }

    /** Black Clover-style cast look: a spinning circle under the caster and a burst at the hand. */
    static void castVfx(ServerPlayer player, int color) {
        SkillFx.follow(player.serverLevel(), com.newuniverse.nusmp.vfx.VfxShape.MAGIC_CIRCLE, player,
                player.position().add(0, 1, 0), color, 24, 0.9f);
        Vec3 hand = player.getEyePosition().add(player.getViewVector(1f).scale(0.8));
        SkillFx.shape(player.serverLevel(), com.newuniverse.nusmp.vfx.VfxShape.MAGIC_CIRCLE_EXPLOSION, hand,
                hand.add(player.getViewVector(1f)), color, 16, 0.4f);
    }
}
