package com.newuniverse.nusmp.multiverse;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Why: in the anime the Magic Knights hold a Star Awards Festival at the end of the year that names the squad with the most stars. Every N in-game days (setting {@code [awards].days}, 28 by default) the standings
 * are announced to everyone and each online member of the top three squads gets gold stars (5 / 3 / 1). Members who are offline at that moment get nothing (documented). Cost: one comparison every 10 seconds;
 * the festival itself is one pass over the squads (bounded by the number of squads) once per period.
 */
public final class StarAwards extends SavedData {
    private int last = -1;

    static StarAwards get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(StarAwards::new, StarAwards::load, null), "nusmp_star_awards");
    }

    private static StarAwards load(CompoundTag tag, HolderLookup.Provider provider) {
        StarAwards s = new StarAwards();
        s.last = tag.contains("Last") ? tag.getInt("Last") : -1;
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("Last", last);
        return tag;
    }

    public static void onTick(ServerTickEvent.Post e) {
        try {
            MinecraftServer server = e.getServer();
            if (server.getTickCount() % 200 != 17 || !MultiverseConfig.get(MultiverseConfig.AWARDS_ENABLED) || !Squads.available()) return;
            StarAwards sa = get(server);
            int period = StarAwardRules.period(server.overworld().getGameTime(), MultiverseConfig.get(MultiverseConfig.AWARDS_DAYS));
            if (sa.last < 0) { sa.last = period; sa.setDirty(); return; }
            if (!StarAwardRules.due(sa.last, period)) return;
            sa.last = period;
            sa.setDirty();
            hold(server);
        } catch (Throwable t) {
            com.mojang.logging.LogUtils.getLogger().warn("[nusmp] star awards failed", t);
        }
    }

    /** Announces the standings and pays the top three squads' online members. Public so an admin or a test can call it. */
    public static void hold(MinecraftServer server) {
        Squads.prune(server);
        List<Squads.Standing> all = Squads.standings(server);
        if (all.isEmpty()) return;
        server.getPlayerList().broadcastSystemMessage(Component.literal("The Star Awards Festival!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        int first = MultiverseConfig.get(MultiverseConfig.AWARDS_FIRST), second = MultiverseConfig.get(MultiverseConfig.AWARDS_SECOND), third = MultiverseConfig.get(MultiverseConfig.AWARDS_THIRD);
        for (int i = 0; i < Math.min(3, all.size()); i++) {
            Squads.Standing st = all.get(i);
            int stars = StarAwardRules.reward(i + 1, first, second, third);
            server.getPlayerList().broadcastSystemMessage(Component.literal(" #" + (i + 1) + " " + st.name() + " with " + st.score() + " stars" + (stars > 0 ? " (+" + stars + " gold stars each)" : ""))
                    .withStyle(i == 0 ? ChatFormatting.GOLD : ChatFormatting.WHITE), false);
            if (stars <= 0) continue;
            Squads.Ftb.byId(st.teamId()).ifPresent(team -> {
                Set<UUID> members = Squads.Ftb.members(team);
                for (UUID id : members) {
                    ServerPlayer on = server.getPlayerList().getPlayer(id);
                    if (on != null) MultiverseProfile.addStars(on, stars);
                }
            });
        }
    }
}
