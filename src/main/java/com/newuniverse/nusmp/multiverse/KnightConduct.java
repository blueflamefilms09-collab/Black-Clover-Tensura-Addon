package com.newuniverse.nusmp.multiverse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Optional;

/**
 * Why: a Magic Knight protects the people of the kingdom; harming them costs stars. Killing a villager or a wandering trader, or a member of one's own squad, gives black stars (settings in group
 * {@code [conduct]}), at most one such penalty per cool-down so one rampage does not bury a player. The clock is a field of the player's profile. Cost: one event per death caused by a player.
 */
public final class KnightConduct {
    private KnightConduct() {}

    private static final String SEC = "Conduct";

    /** Black stars for this kill, or 0: the villager rule and the squadmate rule (the larger one if both apply). */
    static int penalty(ServerPlayer killer, net.minecraft.world.entity.LivingEntity victim) {
        int n = 0;
        if (victim instanceof AbstractVillager) n = MultiverseConfig.get(MultiverseConfig.CONDUCT_VILLAGER);
        if (victim instanceof ServerPlayer v && v != killer && Squads.available()) {
            Optional<Squads.Membership> a = Squads.membership(killer), b = Squads.membership(v);
            if (a.isPresent() && b.isPresent() && a.get().teamId().equals(b.get().teamId())) n = Math.max(n, MultiverseConfig.get(MultiverseConfig.CONDUCT_SQUADMATE));
        }
        return n;
    }

    /** Applies the penalty if the cool-down allows it. Returns the black stars given. */
    public static int apply(ServerPlayer killer, net.minecraft.world.entity.LivingEntity victim) {
        int n = penalty(killer, victim);
        if (n <= 0) return 0;
        long now = killer.serverLevel().getGameTime();
        CompoundTag t = MultiverseProfile.section(killer, SEC);
        long gap = 20L * MultiverseConfig.get(MultiverseConfig.CONDUCT_COOLDOWN);
        if (t.contains("Last") && now - t.getLong("Last") < gap && now >= t.getLong("Last")) return 0;
        t.putLong("Last", now);
        MultiverseProfile.putSection(killer, SEC, t);
        MultiverseProfile.addStars(killer, -n);
        killer.sendSystemMessage(Component.literal("A Magic Knight protects the people. " + n + " black star" + (n == 1 ? "" : "s") + " for that.").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        return n;
    }

    public static void onDeath(LivingDeathEvent e) {
        try {
            if (!(e.getSource().getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer || !MultiverseConfig.get(MultiverseConfig.CONDUCT_ENABLED)) return;
            apply(p, e.getEntity());
        } catch (Throwable t) {
            com.mojang.logging.LogUtils.getLogger().warn("[nusmp] conduct failed", t);
        }
    }
}
