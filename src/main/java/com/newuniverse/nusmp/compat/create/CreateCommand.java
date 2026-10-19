package com.newuniverse.nusmp.compat.create;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Map;

/**
 * Why: the operators' view of the Create compatibility, as {@code /multiverse compat create ...} (op level 2), registered on its own (the command tree merges it into the
 * {@code multiverse compat} node the mod already has, so no existing command class is edited). {@code status}: whether Create is installed, which parts resolved to Create
 * blocks and which to a stand-in, the sites by state, the contraption guard. {@code plan}: plan the traps of sectors that were generated before Create was installed.
 * {@code arm <family>}: build one trap of that family at a choke near the player (to see it, and to test it). {@code fire}: set off the nearest built trap within 24 blocks.
 */
public final class CreateCommand {
    private CreateCommand() {}

    private static MutableComponent t(String key, String english, Object... args) { return Component.translatableWithFallback("message.nusmp.create." + key, english, args); }

    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("multiverse").then(Commands.literal("compat").then(node())));
    }

    static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("create").requires(s -> s.hasPermission(2)).executes(CreateCommand::status)
                .then(Commands.literal("plan").executes(CreateCommand::plan))
                .then(Commands.literal("arm").then(Commands.argument("family", StringArgumentType.word())
                        .suggests((ctx, b) -> {
                            for (TrapFamily f : TrapFamily.values()) b.suggest(f.id());
                            return b.buildFuture();
                        })
                        .executes(CreateCommand::arm)))
                .then(Commands.literal("fire").executes(CreateCommand::fire));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        boolean loaded = PartResolver.createLoaded();
        src.sendSuccess(() -> t("status.head", "Create compatibility: Create is %s, the master switch is %s.", loaded ? "installed" : "not installed", CreateConfig.get(CreateConfig.ENABLED) ? "on" : "off").withStyle(ChatFormatting.GOLD), false);
        if (!loaded) {
            src.sendSuccess(() -> t("status.absent", "Nothing in the dungeon is changed: it is as it is without Create.").withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        int fromCreate = 0, total = 0;
        StringBuilder stand = new StringBuilder();
        for (Map.Entry<Part, PartResolver.Resolved> en : PartResolver.report().entrySet()) {
            if (en.getKey().vanilla()) continue;
            total++;
            if (en.getValue().create()) fromCreate++;
            else stand.append(stand.length() == 0 ? "" : ", ").append(en.getKey().name().toLowerCase());
        }
        final int fc = fromCreate, tt = total;
        final String sb = stand.toString();
        src.sendSuccess(() -> t("status.parts", "Parts found in Create: %s of %s%s", fc, tt, sb.isEmpty() ? "." : "; vanilla stand-ins for: " + sb).withStyle(ChatFormatting.AQUA), false);
        CreateTrapData d = CreateTrapData.get(src.getServer());
        src.sendSuccess(() -> t("status.sites", "Traps: %s planned, %s built, %s dropped; %s awake near players.",
                d.count(TrapSite.State.PLANNED), d.count(TrapSite.State.BUILT), d.count(TrapSite.State.DROPPED), CreateTrapDirector.liveCount()).withStyle(ChatFormatting.AQUA), false);
        src.sendSuccess(() -> t("status.guard", "Contraption guard: %s.", ContraptionGuard.status()).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int plan(CommandContext<CommandSourceStack> ctx) {
        if (!PartResolver.createLoaded()) {
            ctx.getSource().sendFailure(t("status.absent", "Nothing in the dungeon is changed: it is as it is without Create."));
            return 0;
        }
        int n = CreateTrapDirector.planAll(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> t("plan.done", "Planned %s trap(s) in sectors that had none. They are built when someone comes near.", n).withStyle(ChatFormatting.GREEN), true);
        return n;
    }

    private static int arm(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (!PartResolver.createLoaded()) {
            src.sendFailure(t("status.absent", "Nothing in the dungeon is changed: it is as it is without Create."));
            return 0;
        }
        ServerPlayer p;
        try {
            p = src.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            src.sendFailure(t("player_only", "Only a player has a place to build at."));
            return 0;
        }
        TrapFamily f = TrapFamily.byId(StringArgumentType.getString(ctx, "family"));
        ServerLevel level = src.getLevel();
        if (f == null) {
            src.sendFailure(t("arm.family", "Unknown family. Try saw_run, press, drill_wall, treadmill, pour or sequence."));
            return 0;
        }
        if (!MegaDungeon.in(level)) {
            src.sendFailure(t("arm.dimension", "Stand in the Mega-Dungeon, in a corridor, to build a trap."));
            return 0;
        }
        long seed = level.getGameTime() ^ p.getUUID().getLeastSignificantBits();
        TrapSite site = new TrapSite("cmd" + Long.toHexString(System.nanoTime()), -1, -1, f, TrapRules.pickTrigger(((seed >>> 3) & 1023) / 1024.0), TrapRules.pickCore(((seed >>> 13) & 1023) / 1024.0), seed);
        int cx = p.blockPosition().getX(), cz = p.blockPosition().getZ();
        site.depth = Math.max(1, Math.abs(MegaDungeon.floorAt(level, p.blockPosition())));
        site.threat = Math.max(1, MegaDungeon.threatAt(level, p.blockPosition()));
        if (CreateTrapDirector.tryArm(level, site, cx - 24, cx + 24, cz - 24, cz + 24, MegaGrid.FLOOR_Y)) {
            CreateTrapData.get(src.getServer()).add(site);
            src.sendSuccess(() -> t("arm.done", "Built a %s trap (%s trigger) at %s, running %s.", f.id(), site.trigger.id(), site.origin.toShortString(), site.forward.getName()).withStyle(ChatFormatting.GREEN), true);
            return 1;
        }
        src.sendFailure(t("arm.failed", "No place within 24 blocks took a %s trap: %s", f.id(), site.note));
        return 0;
    }

    private static int fire(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        TrapSite s = CreateTrapDirector.nearest(src.getServer(), net.minecraft.core.BlockPos.containing(src.getPosition()), 24.0);
        ServerLevel level = MegaDungeon.level(src.getServer());
        if (s == null || level == null) {
            src.sendFailure(t("fire.none", "No built trap within 24 blocks."));
            return 0;
        }
        TrapRuntime.Report r = TrapRuntime.step(level, CreateTrapDirector.live(s), level.getGameTime(), true, null);
        src.sendSuccess(() -> t("fire.done", "Set off %s at %s (%s).", s.family.id(), s.origin.toShortString(), r.state()).withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }
}
