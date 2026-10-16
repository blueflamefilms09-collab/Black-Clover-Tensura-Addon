package com.newuniverse.nusmp.quests.pack;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.quests.QuestsConfig;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforgespi.language.IModFileInfo;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Why: {@link PackInstaller} works on plain folders; this class is the thin glue that supplies the real ones (the pack inside the mod jar,
 * the FTB Quests folder in the game's config directory), checks that FTB Quests is installed at all, asks FTB Quests to reload after a change,
 * and does the optional first-run install on server start. Nothing here runs unless an operator uses the command or turns the config on.
 */
public final class QuestsInstall {
    private QuestsInstall() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    public static final String FTB_QUESTS = "ftbquests";

    public static boolean ftbQuestsLoaded() {
        ModList l = ModList.get();
        return l != null && l.isLoaded(FTB_QUESTS);
    }

    /** {@code <config>/ftbquests/quests}, the folder FTB Quests reads (see ServerQuestFile in FTB Quests). */
    public static Path questsDir() {
        return FMLPaths.CONFIGDIR.get().resolve("ftbquests").resolve("quests");
    }

    /** The bundled {@code quest_pack} folder inside the mod jar, or null when this build has none. */
    public static Path packRoot() {
        try {
            IModFileInfo self = ModList.get().getModFileById(NUSMP.MODID);
            if (self == null) return null;
            Path p = self.getFile().findResource("quest_pack");
            return p != null && Files.isDirectory(p) ? p : null;
        } catch (RuntimeException | LinkageError e) {
            LOG.warn("[nusmp] could not locate the bundled quest pack", e);
            return null;
        }
    }

    /** The mod version, written into the marker so {@code status} can say which build installed the pack. */
    public static String modVersion() {
        try {
            return ModList.get().getModContainerById(NUSMP.MODID).map(c -> c.getModInfo().getVersion().toString()).orElse("unknown");
        } catch (RuntimeException e) {
            return "unknown";
        }
    }

    /**
     * Asks FTB Quests to reload its quests through its own command ({@code /ftbquests reload quests}, editor permission), so the operator does
     * not have to. FTB reports its own result; if the command is missing or refused nothing happens and the operator is told to run it.
     */
    public static void requestReload(CommandSourceStack source) {
        try {
            source.getServer().getCommands().performPrefixedCommand(source, "ftbquests reload quests");
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] could not ask FTB Quests to reload", e);
        }
    }

    /** First-run install: before FTB Quests loads its folder, install the pack if the folder is missing entirely and the config allows it. */
    public static void onServerAboutToStart(ServerAboutToStartEvent e) {
        try {
            if (!QuestsConfig.autoInstall() || !ftbQuestsLoaded()) return;
            Path dir = questsDir();
            if (Files.exists(dir)) return;
            Path pack = packRoot();
            if (pack == null) return;
            PackInstaller.Result r = PackInstaller.install(pack, dir, modVersion(), false, System.currentTimeMillis());
            LOG.info("[nusmp] quests.autoInstallOnFirstRun: {}", r.message());
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] quest pack auto-install failed", ex);
        }
    }
}
