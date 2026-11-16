package com.newuniverse.nusmp.labyrinth.mega.cores;

import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialData;
import com.newuniverse.nusmp.labyrinth.mega.guard.LabyrinthGuardianEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Why: the hook the Decennial bosses call when one dies ({@code DecennialLayer.onDeath}, next to the relic): a boss on floor 250 or deeper pays ONE family of core ({@link BossCoreRules}) to the killer, in
 * one stack that sits where the boss fell and neither burns nor despawns. Same guards as the relic: an admin-called boss and a standalone hall never reach this method (the caller checks), the hall pays
 * once (its own memory key, so the relic's key is untouched), and a death with nobody to credit pays nothing and keeps the prize for a death that has a killer. Never throws.
 * Cost: one saved-data lookup and at most one item entity per boss death.
 */
public final class BossCoreAward {
    private BossCoreAward() {}

    /** Pays the cores of the boss of {@code level} (signed) standing on {@code floor} (signed) for this death. */
    public static void award(LabyrinthGuardianEntity g, ServerLevel sl, DamageSource source, int level, int floor, String siteKey) {
        try {
            if (!BossCoreConfig.enabled()) return;
            int count = BossCoreConfig.coresFor(floor);
            if (count <= 0) return;
            MinecraftServer server = sl.getServer();
            if (server == null) return;
            DecennialData data = DecennialData.get(server);
            String memory = "cores:" + (siteKey.isEmpty() ? "l" + level : siteKey);
            if (data.paid(memory)) return;
            ServerPlayer killer = killerOf(g, source, sl);
            if (killer == null) return;
            ItemStack stack = new ItemStack(BossCoreItems.core(BossCoreRules.kitOfLevel(level)), count);
            ItemEntity drop = new ItemEntity(sl, g.getX(), g.getY() + 0.3, g.getZ(), stack);
            drop.setUnlimitedLifetime();
            drop.setInvulnerable(true);
            drop.setGlowingTag(true);
            sl.addFreshEntity(drop);
            data.claimLoot(memory);
            Component line = Component.translatableWithFallback("message.anime_multiverse.boss_core.drop", "%1$s leaves %2$s x%3$s.", g.getName(), stack.getHoverName(), count).withStyle(ChatFormatting.AQUA);
            for (ServerPlayer q : sl.players()) if (q.distanceToSqr(g) <= 64.0 * 64.0) q.sendSystemMessage(line);
        } catch (RuntimeException e) {
            com.mojang.logging.LogUtils.getLogger().warn("[anime_multiverse] The Boss Cores of a Decennial boss could not be paid: {}", e.toString());
        }
    }

    /** The player a kill is credited to: the attacker, the credited player, else the nearest living player within 64 blocks (the rule of the relic). */
    private static ServerPlayer killerOf(LabyrinthGuardianEntity dead, DamageSource source, ServerLevel sl) {
        Entity cause = source == null ? null : source.getEntity();
        if (cause instanceof ServerPlayer p) return p;
        if (dead.getKillCredit() instanceof ServerPlayer credited) return credited;
        ServerPlayer best = null;
        double bestD = 64.0 * 64.0;
        for (ServerPlayer q : sl.players()) {
            if (q.isSpectator() || !q.isAlive()) continue;
            double d = q.distanceToSqr(dead);
            if (d <= bestD) {
                best = q;
                bestD = d;
            }
        }
        return best;
    }
}
