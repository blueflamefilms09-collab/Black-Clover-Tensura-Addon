package com.newuniverse.nusmp.particle.fx;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: nobody can judge a particle from a table, so {@code /multiverse particles} (operators, permission level 2) shows any of them in the game: {@code list [group]} prints the library by
 * family, {@code spawn <id> [count] [color]} puts a cloud of one particle three blocks in front of the player (optionally dyed), {@code theme <key>} shows a theme (its four slots in the light
 * flavour on a row and in the dark flavour above it, and prints which theme the key resolved to, its colour and particles), {@code status} prints the two {@code particles.custom} switches.
 * It adds a {@code particles} child to the {@code /multiverse} root that {@code MultiverseCommands} registers (the dispatcher merges two registrations of the same literal), so that file does
 * not change. The spawned particles go through the plain {@code sendParticles}, not the budgets: it is an admin tool and shows exactly what was asked.
 */
public final class FxCommands {
    private FxCommands() {}

    /** Game-bus listener. */
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("multiverse").then(Commands.literal("particles").requires(s -> s.hasPermission(2))
                .executes(FxCommands::status)
                .then(Commands.literal("status").executes(FxCommands::status))
                .then(Commands.literal("list").executes(ctx -> list(ctx, null))
                        .then(Commands.argument("group", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(groups().keySet(), b))
                                .executes(ctx -> list(ctx, StringArgumentType.getString(ctx, "group")))))
                .then(Commands.literal("spawn").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(FxCatalog.ids(), b))
                        .executes(ctx -> spawn(ctx, 16, -1))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 300)).executes(ctx -> spawn(ctx, IntegerArgumentType.getInteger(ctx, "count"), -1))
                                .then(Commands.argument("color", StringArgumentType.word()).executes(ctx -> spawn(ctx, IntegerArgumentType.getInteger(ctx, "count"), FxTint.parseColor(StringArgumentType.getString(ctx, "color"))))))))
                .then(Commands.literal("theme").then(Commands.argument("key", StringArgumentType.word()).executes(FxCommands::theme)))));
    }

    /** The catalog grouped by its group word, in catalog order. */
    public static Map<String, List<String>> groups() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        for (FxCatalog.Def d : FxCatalog.all()) m.computeIfAbsent(d.group(), k -> new ArrayList<>()).add(d.id());
        return m;
    }

    private static void say(CommandContext<CommandSourceStack> ctx, String line, ChatFormatting color) {
        ctx.getSource().sendSuccess(() -> Component.literal(line).withStyle(color), false);
    }

    private static void fail(CommandContext<CommandSourceStack> ctx, String key, String fallback, Object... args) {
        ctx.getSource().sendFailure(Component.translatableWithFallback(key, fallback, args));
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        say(ctx, FxCatalog.all().size() + " custom particles in " + groups().size() + " groups; " + ParticleThemes.table().themes().size() + " themes.", ChatFormatting.AQUA);
        say(ctx, "particles.custom: server " + FxConfig.serverCustom() + ", client (this machine) " + FxConfig.clientCustom() + ". Try /multiverse particles list, spawn <id>, theme <key>.", ChatFormatting.GRAY);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx, String group) {
        Map<String, List<String>> g = groups();
        if (group != null && !g.containsKey(group)) {
            fail(ctx, "nusmp.particles.cmd.nogroup", "No such group: %s. Groups: %s", group, String.join(", ", g.keySet()));
            return 0;
        }
        for (Map.Entry<String, List<String>> e : g.entrySet()) {
            if (group != null && !group.equals(e.getKey())) continue;
            say(ctx, e.getKey() + " (" + e.getValue().size() + "): " + String.join(", ", e.getValue()), ChatFormatting.AQUA);
        }
        return 1;
    }

    private static Vec3 front(ServerPlayer p, double dist) {
        return p.getEyePosition().add(p.getLookAngle().scale(dist));
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx, int count, int rgb) {
        String id = StringArgumentType.getString(ctx, "id");
        TintedParticleType type = FxTypes.type(id);
        if (type == null) {
            fail(ctx, "nusmp.particles.cmd.unknown", "No such custom particle: %s (see /multiverse particles list)", id);
            return 0;
        }
        ServerPlayer p;
        try {
            p = ctx.getSource().getPlayerOrException();
        } catch (Exception e) {
            fail(ctx, "nusmp.particles.cmd.player", "Run this as a player.");
            return 0;
        }
        ServerLevel sl = p.serverLevel();
        Vec3 at = front(p, 3.0);
        ParticleOptions o = rgb >= 0 ? type.of(rgb, 1.0f) : type.plain();
        sl.sendParticles(o, at.x, at.y, at.z, count, 0.5, 0.5, 0.5, 0.02);
        FxCatalog.Def d = FxCatalog.byId(id);
        say(ctx, count + " x " + id + (rgb >= 0 ? " " + FxTint.format(rgb) : "") + (d == null ? "" : " [" + d.group() + ", " + d.frames() + " frames, " + d.px() + "px" + (d.tint() ? ", tintable" : "") + "]"), ChatFormatting.GREEN);
        return 1;
    }

    private static int theme(CommandContext<CommandSourceStack> ctx) {
        String key = StringArgumentType.getString(ctx, "key");
        ThemeKey k = ThemeKey.of(key);
        ParticleThemes.Theme t = k.theme();
        say(ctx, key + " -> theme " + t.id() + ", colour " + FxTint.format(t.rgb()) + ", particles " + String.join(", ", t.particles()) + "; dark: " + FxTint.format(t.darkRgb()) + ", " + String.join(", ", t.darkParticles()), ChatFormatting.AQUA);
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) return 1;
        ServerLevel sl = p.serverLevel();
        Vec3 base = front(p, 4.0);
        Vec3 right = p.getLookAngle().cross(new Vec3(0, 1, 0)).normalize();
        for (FxSlot s : FxSlot.values()) {
            for (FxTone tone : FxTone.values()) {
                ParticleOptions o = Fx.options(k, s, tone);
                if (o == null) continue;
                Vec3 at = base.add(right.scale((s.index() - 1.5) * 1.2)).add(0, tone == FxTone.DARK ? 0.9 : -0.3, 0);
                sl.sendParticles(o, at.x, at.y, at.z, 10, 0.18, 0.18, 0.18, 0.01);
            }
        }
        return 1;
    }
}
