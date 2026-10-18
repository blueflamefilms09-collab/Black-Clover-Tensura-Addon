package com.newuniverse.nusmp.compat.maps;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Why: FTB Chunks claims keep other players from using blocks in a claimed chunk, which would lock altars and NPCs of the Hub (and mean nothing in the unbreakable dungeon). The
 * switch is FTB Chunks' own {@code claim_dimension_blacklist}; this mod does not write it. Instead, when FTB Chunks is loaded, this reads its config as plain text
 * ({@link FtbChunksCheck}), logs one line at server start when the mod's dimensions can still be claimed, and tells operators once per login. All soft: no FTB class is touched, a
 * missing or unreadable file is "unknown", nothing here can crash a server. Documented in docs/compat/maps.md section 6.
 */
public final class FtbChunksGuard {
    private FtbChunksGuard() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final long CACHE_MS = 30_000L;
    private static volatile long checkedAt;
    private static volatile FtbChunksCheck.Result cached = FtbChunksCheck.unknown();

    /** The ids of the mod's dimensions, as FTB Chunks sees them. */
    public static List<String> ourDimensions() {
        List<String> out = new ArrayList<>();
        for (String n : DimensionAudit.NAMES) out.add("nusmp:" + n);
        return out;
    }

    public static boolean ftbChunksLoaded() {
        try {
            ModList l = ModList.get();
            return l != null && l.isLoaded("ftbchunks");
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /** The claim check for the running server (cached for half a minute); UNKNOWN when FTB Chunks is not loaded or the file is not found. Never throws. */
    public static FtbChunksCheck.Result evaluate(MinecraftServer server) {
        long now = System.currentTimeMillis();
        if (now - checkedAt < CACHE_MS) return cached;
        FtbChunksCheck.Result r = FtbChunksCheck.unknown();
        try {
            if (ftbChunksLoaded()) {
                Path world = server == null ? null : server.getWorldPath(LevelResource.ROOT);
                r = FtbChunksCheck.check(world, FMLPaths.GAMEDIR.get(), ourDimensions());
            }
        } catch (RuntimeException ex) {
            LOG.debug("[nusmp] FTB Chunks claim check failed (ignored)", ex);
        }
        cached = r;
        checkedAt = now;
        return r;
    }

    public static void onStarted(ServerStartedEvent e) {
        try {
            if (!ftbChunksLoaded()) return;
            checkedAt = 0L;
            FtbChunksCheck.Result r = evaluate(e.getServer());
            switch (r.status()) {
                case OPEN -> LOG.warn("[nusmp] FTB Chunks is installed and players can still claim chunks in this mod's dimensions {} (read from {}). Add to FTB Chunks' world config: {}",
                        r.uncovered(), r.source(), FtbChunksCheck.suggestion(r.uncovered()));
                case UNKNOWN -> LOG.info("[nusmp] FTB Chunks is installed but its world config ({}) was not found, so it is unknown whether claims are blocked in this mod's dimensions. "
                        + "Make sure claim_dimension_blacklist covers them: {}", FtbChunksCheck.FILE, FtbChunksCheck.suggestion(ourDimensions()));
                case COVERED -> LOG.info("[nusmp] FTB Chunks claims are blocked in this mod's dimensions (read from {}).", r.source());
            }
        } catch (RuntimeException ex) {
            LOG.debug("[nusmp] FTB Chunks start-up check failed (ignored)", ex);
        }
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        try {
            if (!(e.getEntity() instanceof ServerPlayer p) || !p.hasPermissions(2) || !MapsConfig.get(MapsConfig.WARN_OPS) || !ftbChunksLoaded()) return;
            FtbChunksCheck.Result r = evaluate(p.getServer());
            if (r.status() != FtbChunksCheck.Status.OPEN) return;
            p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.maps.ftb_warn",
                    "FTB Chunks lets players claim chunks in this mod's dimensions (%1$s). Add to its world config: %2$s. Details: /multiverse compat maps",
                    String.join(", ", r.uncovered()), FtbChunksCheck.suggestion(r.uncovered())).withStyle(ChatFormatting.GOLD));
        } catch (RuntimeException ex) {
            LOG.debug("[nusmp] FTB Chunks login warning failed (ignored)", ex);
        }
    }
}
