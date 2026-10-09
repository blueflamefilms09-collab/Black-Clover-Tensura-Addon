package com.newuniverse.nusmp.entity.riven;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.newuniverse.nusmp.skill.codex.AnimeSkill;
import com.newuniverse.nusmp.skill.codex.AnimeSkillCodex;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * /nusmp riven summon | codex | accept &lt;file&gt; | research &lt;anime&gt; &lt;ability&gt;. Ops only. Research is off by default (config enableResearch), runs entirely
 * off the server thread, is rate-limited, refuses private/loopback endpoints, and can only ever produce a candidate codex JSON in the inbox: the
 * response is parsed as data and validated against the sandbox allow-list, never executed. No player data is ever sent.
 */
public final class RivenCommands {
    private RivenCommands() {}

    private static long lastResearch;

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nusmp").requires(s -> s.hasPermission(2))
                .then(Commands.literal("riven")
                        .then(Commands.literal("summon").executes(ctx -> {
                            var r = RivenBossEntity.summon(ctx.getSource().getLevel(), ctx.getSource().getPosition());
                            return reply(ctx.getSource(), r == null ? "Riven Remake could not be summoned here." : "Riven Remake takes the stage.");
                        }))
                        .then(Commands.literal("codex").executes(ctx -> reply(ctx.getSource(), AnimeSkillCodex.all().size() + " skills in the codex. Inbox: " + AnimeSkillCodex.inbox())))
                        .then(Commands.literal("accept").then(Commands.argument("file", StringArgumentType.word()).executes(ctx -> {
                            if (!RivenConfig.ALLOW_CODEX_INBOX.get()) return reply(ctx.getSource(), "allowCodexInbox is off in nusmp-riven.toml.");
                            return reply(ctx.getSource(), AnimeSkillCodex.accept(StringArgumentType.getString(ctx, "file")));
                        })))
                        .then(Commands.literal("research").then(Commands.argument("anime", StringArgumentType.word())
                                .then(Commands.argument("ability", StringArgumentType.greedyString()).executes(ctx ->
                                        research(ctx.getSource(), StringArgumentType.getString(ctx, "anime"), StringArgumentType.getString(ctx, "ability"))))))));
    }

    private static int reply(CommandSourceStack s, String msg) {
        s.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int research(CommandSourceStack src, String anime, String ability) {
        if (!RivenConfig.ENABLE_RESEARCH.get()) return reply(src, "Research is off (enableResearch in nusmp-riven.toml). Drop a codex JSON into " + AnimeSkillCodex.inbox() + " and use /nusmp riven accept <file>.");
        String endpoint = RivenConfig.RESEARCH_ENDPOINT.get().trim();
        if (endpoint.isEmpty()) return reply(src, "No researchEndpoint is set. Drop a codex JSON into " + AnimeSkillCodex.inbox() + " and use /nusmp riven accept <file>.");
        long now = System.currentTimeMillis();
        if (now - lastResearch < 30_000) return reply(src, "Research is rate-limited: wait a moment.");
        lastResearch = now;
        var server = src.getServer();
        String safeName = (anime + "_" + ability).toLowerCase().replaceAll("[^a-z0-9]+", "_");
        if (safeName.length() > 48) safeName = safeName.substring(0, 48);
        final String file = safeName;
        reply(src, "Researching '" + ability + "' from " + anime + " in the background...");
        CompletableFuture.supplyAsync(() -> fetch(endpoint, anime, ability)).whenComplete((body, err) -> server.execute(() -> {
            String msg;
            if (err != null || body == null) msg = "Research failed: " + (err == null ? "no answer" : err.getMessage());
            else {
                try {
                    JsonObject o = JsonParser.parseString(body).getAsJsonObject();
                    AnimeSkill s = AnimeSkill.parse(o);                                          // validated against the sandbox allow-list
                    Path dir = AnimeSkillCodex.inbox();
                    Files.createDirectories(dir);
                    Files.writeString(dir.resolve(file + ".json"), o.toString(), StandardCharsets.UTF_8);
                    msg = "Candidate " + s.id() + " written to the inbox as " + file + ".json. Accept it with /nusmp riven accept " + file;
                } catch (Exception ex) {
                    msg = "The answer was not a valid codex skill: " + ex.getMessage();
                }
            }
            String m = msg;
            src.sendSuccess(() -> Component.literal(m), true);
        }));
        return 1;
    }

    /** Off-thread HTTP GET (5 s timeouts, 16 KB cap, no redirects, no private addresses). Sends only the anime and ability names. */
    private static String fetch(String endpoint, String anime, String ability) {
        try {
            URI uri = URI.create(endpoint + (endpoint.contains("?") ? "&" : "?") + "anime=" + URLEncoder.encode(anime, StandardCharsets.UTF_8)
                    + "&ability=" + URLEncoder.encode(ability, StandardCharsets.UTF_8));
            if (!"https".equals(uri.getScheme()) && !"http".equals(uri.getScheme())) throw new IllegalArgumentException("endpoint must be http(s)");
            InetAddress a = InetAddress.getByName(uri.getHost());
            if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress()) throw new IllegalArgumentException("endpoint is a private address");
            HttpURLConnection c = (HttpURLConnection) uri.toURL().openConnection();
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);
            c.setInstanceFollowRedirects(false);
            try (InputStream in = c.getInputStream()) {
                byte[] buf = in.readNBytes(16 * 1024);
                return new String(buf, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }
}
