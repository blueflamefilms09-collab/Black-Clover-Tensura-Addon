package com.newuniverse.nusmp.multiverse;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

/**
 * Why: gold stars used to come only from an admin command. Now finishing a mission (a questline advancement of this mod, shown in the advancement tree)
 * gives them by itself: a task 1, a goal 2, a challenge 5 (server settings, group {@code [missions]}). Each mission pays once per player (the ids are kept in
 * the player's profile, so revoking and earning it again gives nothing). The stars go through {@link MultiverseProfile#addStars}, so the social-class
 * bonus, the grimoire mastery, the squad score, the automatic promotion and the Wizard King count all follow. Server side only; one event per advancement a
 * player earns, nothing per tick; a stand-in or fake player never earns them.
 */
public final class MissionStars {
    private MissionStars() {}

    /** The listener for the advancement event (registered once in the mod constructor). */
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p) || p instanceof FakePlayer || !MultiverseConfig.get(MultiverseConfig.MISSION_STARS)) return;
            AdvancementHolder h = e.getAdvancement();
            var display = h.value().display();
            if (!MissionStarRules.counts(h.id().getNamespace(), h.id().getPath(), display.isPresent(), MultiverseConfig.get(MultiverseConfig.MISSION_EXCLUDE))) return;
            award(p, h.id().toString(), display.get().getType().name(), display.get().getTitle());
        } catch (Throwable t) {
            com.mojang.logging.LogUtils.getLogger().warn("[nusmp] mission stars failed", t);
        }
    }

    /** Pays the stars of one mission once. Returns the stars the player got (0 if it had paid already or the setting for its type is 0). */
    public static int award(ServerPlayer p, String missionId, String type, Component title) {
        if (MultiverseProfile.missionDone(p, missionId)) return 0;
        int stars = MissionStarRules.stars(type, MultiverseConfig.get(MultiverseConfig.MISSION_TASK), MultiverseConfig.get(MultiverseConfig.MISSION_GOAL), MultiverseConfig.get(MultiverseConfig.MISSION_CHALLENGE));
        MultiverseProfile.markMission(p, missionId);
        if (stars <= 0) return 0;
        int before = MultiverseProfile.goldStars(p);
        MultiverseProfile.addStars(p, stars);
        int got = MultiverseProfile.goldStars(p) - before;
        p.sendSystemMessage(Component.literal("Mission complete: ").withStyle(ChatFormatting.GOLD)
                .append(title == null ? Component.literal(missionId) : title.copy().withStyle(ChatFormatting.WHITE))
                .append(Component.literal("  +" + got + " gold star" + (got == 1 ? "" : "s")).withStyle(ChatFormatting.YELLOW)));
        return got;
    }
}
