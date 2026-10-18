package com.newuniverse.nusmp.elite.records;

import com.newuniverse.nusmp.quests.QuestTeams;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Optional;

/**
 * Why: the FTB Chunks Claimed condition needs the number of chunks a player's team has claimed, and the mod has no compile-time dependency on FTB Chunks (it is an optional mod; the repository only
 * reaches FTB Teams, by reflection, in {@code quests.QuestTeams}). This bridge does the same for FTB Chunks: it finds {@code FTBChunksAPI.api().getManager()}, asks the manager for the team's
 * {@code ChunkTeamData} and counts {@code getClaimedChunks()}. The team comes from the existing {@link QuestTeams} lookup. Anything that does not resolve (the mod is absent, a method was renamed in a
 * newer release, the manager is not loaded yet) answers {@code null}, which the records system treats as "FTB Chunks unavailable", never as zero. The reflected names follow FTB Chunks 21.1 and could
 * not be run here (no FTB Chunks in the test environment), so a mismatch shows as "unavailable" and a warning, not as a crash.
 */
final class FtbChunksBridge {
    private FtbChunksBridge() {}

    private static Boolean ok;
    private static Method api, isManagerLoaded, getManager, getOrCreateData, getClaimedChunks;
    private static boolean warned;

    private static boolean available() {
        if (ok != null) return ok;
        ok = false;
        if (!ModList.get().isLoaded("ftbchunks")) return false;
        try {
            Class<?> apiCls = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
            Class<?> apiItf = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI$API");
            Class<?> mgr = Class.forName("dev.ftb.mods.ftbchunks.api.ClaimedChunkManager");
            Class<?> data = Class.forName("dev.ftb.mods.ftbchunks.api.ChunkTeamData");
            Class<?> team = Class.forName("dev.ftb.mods.ftbteams.api.Team");
            api = apiCls.getMethod("api");
            isManagerLoaded = apiItf.getMethod("isManagerLoaded");
            getManager = apiItf.getMethod("getManager");
            getOrCreateData = mgr.getMethod("getOrCreateData", team);
            getClaimedChunks = data.getMethod("getClaimedChunks");
            if (!Modifier.isStatic(api.getModifiers())) throw new NoSuchMethodException("api() is not static");
            ok = true;
        } catch (ReflectiveOperationException | LinkageError e) {
            if (!warned) {
                warned = true;
                com.mojang.logging.LogUtils.getLogger().warn("[nusmp] FTB Chunks is loaded but its API could not be reached ({}); the FTB Chunks Claimed records stay unavailable", e.toString());
            }
        }
        return ok;
    }

    /** The number of chunks the player's team has claimed, or null when FTB Chunks cannot say. */
    static Double claimed(ServerPlayer p) {
        if (p == null || !available()) return null;
        try {
            Object a = api.invoke(null);
            if (a == null || !(boolean) isManagerLoaded.invoke(a)) return null;
            Optional<Object> team = QuestTeams.teamOf(p);
            if (team.isEmpty()) return null;
            Object data = getOrCreateData.invoke(getManager.invoke(a), team.get());
            if (data == null) return null;
            Object chunks = getClaimedChunks.invoke(data);
            return chunks instanceof Collection<?> c ? (double) c.size() : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
