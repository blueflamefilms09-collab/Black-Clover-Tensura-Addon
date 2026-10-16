package com.newuniverse.nusmp.quests.pack;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.nio.file.Path;

/**
 * Why: the operator needs one place to install, check, update and remove the bundled FTB Quests questline without copying files by hand:
 * {@code /nusmp quests status|install [force]|update|uninstall}, operator level 2. Each subcommand only calls {@link PackInstaller} and prints
 * its message; all safety rules (refuse over foreign quests, backups, only our own files) live there. With FTB Quests missing, every
 * subcommand except {@code status} refuses and touches nothing.
 */
public final class QuestsCommand {
    private QuestsCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nusmp").then(Commands.literal("quests").requires(s -> s.hasPermission(2))
                .then(Commands.literal("status").executes(ctx -> status(ctx)))
                .then(Commands.literal("install").executes(ctx -> install(ctx, false))
                        .then(Commands.literal("force").executes(ctx -> install(ctx, true))))
                .then(Commands.literal("update").executes(ctx -> update(ctx)))
                .then(Commands.literal("uninstall").executes(ctx -> uninstall(ctx)))));
    }

    private static boolean guard(CommandContext<CommandSourceStack> ctx) {
        if (QuestsInstall.ftbQuestsLoaded()) return true;
        say(ctx, "FTB Quests is not installed on this server, so there is nothing to install the questline into. Nothing was changed.", false);
        return false;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        boolean ftb = QuestsInstall.ftbQuestsLoaded();
        Path dir = QuestsInstall.questsDir();
        Path pack = QuestsInstall.packRoot();
        PackInstaller.Status st = PackInstaller.status(pack, dir);
        say(ctx, "FTB Quests: " + (ftb ? "loaded" : "NOT loaded"), true);
        say(ctx, "Quest folder: " + dir + (st.folderExists() ? (st.folderEmpty() ? " (empty)" : " (has files)") : " (does not exist)"), true);
        say(ctx, "Bundled pack: " + (st.packAvailable() ? st.packFiles() + " files, hash " + st.packHash() + ", mod " + QuestsInstall.modVersion() : "none in this build"), true);
        if (st.installed() == null) {
            say(ctx, "Installed: no" + (st.folderExists() && !st.folderEmpty() ? " (the folder holds other quests; install needs 'force')" : ""), true);
        } else {
            boolean current = st.installed().hash().equals(st.packHash());
            say(ctx, "Installed: yes, pack " + st.installed().hash() + " from mod " + st.installed().version() + ", " + st.installed().files().size()
                    + " files" + (current ? " (up to date)" : " (a newer pack is bundled: /nusmp quests update)"), true);
        }
        return 1;
    }

    private static int install(CommandContext<CommandSourceStack> ctx, boolean force) {
        if (!guard(ctx)) return 0;
        Path pack = QuestsInstall.packRoot();
        if (pack == null) { say(ctx, "This build has no bundled quest pack.", false); return 0; }
        PackInstaller.Result r = PackInstaller.install(pack, QuestsInstall.questsDir(), QuestsInstall.modVersion(), force, System.currentTimeMillis());
        return finish(ctx, r);
    }

    private static int update(CommandContext<CommandSourceStack> ctx) {
        if (!guard(ctx)) return 0;
        Path pack = QuestsInstall.packRoot();
        if (pack == null) { say(ctx, "This build has no bundled quest pack.", false); return 0; }
        return finish(ctx, PackInstaller.update(pack, QuestsInstall.questsDir(), QuestsInstall.modVersion(), System.currentTimeMillis()));
    }

    private static int uninstall(CommandContext<CommandSourceStack> ctx) {
        if (!guard(ctx)) return 0;
        return finish(ctx, PackInstaller.uninstall(QuestsInstall.questsDir(), System.currentTimeMillis()));
    }

    private static int finish(CommandContext<CommandSourceStack> ctx, PackInstaller.Result r) {
        say(ctx, r.message(), r.ok());
        if (!r.ok()) return 0;
        if (r.message().startsWith("Already up to date")) return 1;
        say(ctx, "Asking FTB Quests to reload. If the quests do not change, run /ftbquests reload yourself.", true);
        QuestsInstall.requestReload(ctx.getSource());
        return 1;
    }

    private static void say(CommandContext<CommandSourceStack> ctx, String msg, boolean good) {
        if (good) ctx.getSource().sendSuccess(() -> Component.literal("[Quests] " + msg).withStyle(ChatFormatting.GOLD), true);
        else ctx.getSource().sendFailure(Component.literal("[Quests] " + msg));
    }
}
