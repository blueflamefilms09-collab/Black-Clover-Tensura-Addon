package com.newuniverse.nusmp.skill;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

final class SkillUtil {
    private SkillUtil() {}

    static void actionbar(ServerPlayer player, Component msg) {
        player.displayClientMessage(msg, true);
    }

    static void fail(ServerPlayer player, String text) {
        actionbar(player, Component.literal(text).withStyle(ChatFormatting.RED));
    }

    /** Spends magicules if the player has enough. Returns false (and spends nothing) otherwise. */
    static boolean spendMagicules(ServerPlayer player, double cost) {
        if (cost <= 0) return true;
        IExistence existence = TensuraStorages.getExistenceFrom(player);
        if (existence == null || existence.getMagicule() < cost) {
            fail(player, "Not enough magicules (need " + (long) cost + ").");
            return false;
        }
        existence.setMagicule(existence.getMagicule() - cost);
        existence.markDirty();
        return true;
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

    /** Black Clover-style cast look for the older skills: a spinning circle under the caster and a burst at the hand. */
    static void castVfx(ServerPlayer player, int color) {
        com.newuniverse.nusmp.vfx.VfxSpawn.sendFollowing(player.serverLevel(), com.newuniverse.nusmp.vfx.VfxShape.MAGIC_CIRCLE, player,
                player.position().add(0, 1, 0), color, 24, 0.9f);
        Vec3 hand = player.getEyePosition().add(player.getViewVector(1f).scale(0.8));
        com.newuniverse.nusmp.vfx.VfxSpawn.send(player.serverLevel(), com.newuniverse.nusmp.vfx.VfxShape.MAGIC_CIRCLE_EXPLOSION, hand,
                hand.add(player.getViewVector(1f)), color, 16, 0.4f);
    }
}
