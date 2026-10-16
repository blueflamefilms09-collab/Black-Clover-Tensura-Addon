package com.newuniverse.nusmp.quests;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Why: FTB Quests keeps quest progress per FTB team, so a squad kill should count for every squad member's advancements and quests. This
 * helper answers "which FTB team is this player in and who of it is online" with reflection on FTB Teams' public API, exactly like
 * {@code multiverse.Squads}, so the mod neither needs FTB Teams to build nor to run: without it every answer is empty and the credit goes to
 * the killer alone.
 */
public final class QuestTeams {
    private QuestTeams() {}

    private static Boolean ok;
    private static Method api, getManager, isManagerLoaded, getTeamForPlayer, teamId, teamName, teamMembers;

    private static boolean available() {
        if (ok != null) return ok;
        ok = false;
        if (!ModList.get().isLoaded("ftbteams")) return false;
        try {
            Class<?> apiCls = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
            Class<?> apiItf = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API");
            Class<?> mgr = Class.forName("dev.ftb.mods.ftbteams.api.TeamManager");
            Class<?> team = Class.forName("dev.ftb.mods.ftbteams.api.Team");
            api = apiCls.getMethod("api");
            getManager = apiItf.getMethod("getManager");
            isManagerLoaded = apiItf.getMethod("isManagerLoaded");
            getTeamForPlayer = mgr.getMethod("getTeamForPlayer", ServerPlayer.class);
            teamId = team.getMethod("getId");
            teamName = team.getMethod("getShortName");
            teamMembers = team.getMethod("getMembers");
            ok = true;
        } catch (ReflectiveOperationException | LinkageError e) {
            com.mojang.logging.LogUtils.getLogger().warn("[nusmp] FTB Teams found but its API could not be reached; squad credit sharing disabled", e);
        }
        return ok;
    }

    /** The player's FTB team (personal teams too: a personal team has just the player), or empty without FTB Teams. */
    public static Optional<Object> teamOf(ServerPlayer p) {
        if (p == null || !available()) return Optional.empty();
        try {
            Object a = api.invoke(null);
            if (a == null || !(boolean) isManagerLoaded.invoke(a)) return Optional.empty();
            Object manager = getManager.invoke(a);
            Optional<?> t = (Optional<?>) getTeamForPlayer.invoke(manager, p);
            return t.map(o -> (Object) o);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /** The team's id, or null. */
    public static UUID idOf(Object team) {
        try { return (UUID) teamId.invoke(team); } catch (ReflectiveOperationException | RuntimeException e) { return null; }
    }

    /** The team's short name, or "". */
    public static String nameOf(Object team) {
        try { return (String) teamName.invoke(team); } catch (ReflectiveOperationException | RuntimeException e) { return ""; }
    }

    /** Ids of everyone in the team (offline members too). */
    @SuppressWarnings("unchecked")
    public static Set<UUID> memberIds(Object team) {
        try { return (Set<UUID>) teamMembers.invoke(team); } catch (ReflectiveOperationException | RuntimeException e) { return Set.of(); }
    }

    /**
     * The online members of the player's FTB team, without the player, in the same dimension and within {@code radius} blocks.
     * Empty when FTB Teams is missing or the player is alone in their team.
     */
    public static List<ServerPlayer> nearbyMembers(ServerPlayer p, double radius) {
        List<ServerPlayer> out = new ArrayList<>();
        Optional<Object> team = teamOf(p);
        if (team.isEmpty() || p.getServer() == null) return out;
        double r2 = radius * radius;
        for (UUID id : memberIds(team.get())) {
            if (id.equals(p.getUUID())) continue;
            ServerPlayer m = p.getServer().getPlayerList().getPlayer(id);
            if (m != null && m.level() == p.level() && m.distanceToSqr(p) <= r2) out.add(m);
        }
        return out;
    }
}
