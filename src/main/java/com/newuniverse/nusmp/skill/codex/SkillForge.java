package com.newuniverse.nusmp.skill.codex;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.entity.riven.RivenConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The research job behind {@code /nusmp riven research}: admin only, config gated (off by default), run on a worker thread, never
 * on the server thread. It fetches plain text from the configured https endpoint, compiles it into a candidate skill that can only
 * use {@link SkillSandbox} primitives, and drops that JSON in {@code config/nusmp/codex-inbox/}. Nothing from the response is
 * ever executed, and nothing about any player is sent: the request carries only the anime and ability names the op typed.
 */
public final class SkillForge {
    private SkillForge() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final AtomicLong LAST_JOB = new AtomicLong();
    private static final long MIN_GAP_MS = 30_000;
    private static final int MAX_BODY = 8192;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).followRedirects(HttpClient.Redirect.NEVER).build();

    public static void research(CommandSourceStack src, String anime, String ability) {
        if (!RivenConfig.ENABLE_RESEARCH.get()) { reply(src, "Research is off (enableResearch in nusmp-riven.toml)."); return; }
        String endpoint = RivenConfig.RESEARCH_ENDPOINT.get().trim();
        if (endpoint.isEmpty()) {
            reply(src, "No researchEndpoint is set. Drop a codex JSON file into config/nusmp/codex-inbox/ and run /nusmp riven accept <file>.");
            return;
        }
        long now = System.currentTimeMillis();
        long last = LAST_JOB.get();
        if (now - last < MIN_GAP_MS || !LAST_JOB.compareAndSet(last, now)) { reply(src, "Research is rate limited (one job per 30 s)."); return; }
        URI uri;
        try {
            uri = URI.create(endpoint + (endpoint.contains("?") ? "&" : "?") + "anime=" + enc(anime) + "&ability=" + enc(ability));
            String bad = rejectEndpoint(uri);
            if (bad != null) { reply(src, "Endpoint rejected: " + bad); return; }
        } catch (RuntimeException e) { reply(src, "researchEndpoint is not a valid URL."); return; }
        var server = src.getServer();
        reply(src, "Researching " + ability + " (" + anime + ")...");
        CompletableFuture.runAsync(() -> {
            String message;
            try {
                HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).header("Accept", "text/plain").GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (r.statusCode() != 200) message = "Research failed: HTTP " + r.statusCode();
                else message = store(compile(anime, ability, r.body().length() > MAX_BODY ? r.body().substring(0, MAX_BODY) : r.body()));
            } catch (IOException | InterruptedException | RuntimeException e) {
                LOG.warn("[nusmp] riven research failed: {}", e.toString());
                message = "Research failed: " + e.getClass().getSimpleName();
            }
            String out = message;
            server.execute(() -> src.sendSuccess(() -> Component.literal(out), true));
        });
    }

    /** Public https only, and never a loopback / private / link-local address. */
    static String rejectEndpoint(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) return "must be an https URL";
        try {
            for (InetAddress a : InetAddress.getAllByName(uri.getHost()))
                if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress()) return "points at a private address";
        } catch (IOException e) { return "host does not resolve"; }
        return null;
    }

    /** Text -> a candidate skill that only uses the allow-list. Keyword driven and deliberately plain. */
    public static AnimeSkill compile(String anime, String ability, String text) {
        String t = (ability + " " + text).toLowerCase(Locale.ROOT);
        List<String> prim = new ArrayList<>();
        List<String> counters = new ArrayList<>();
        if (t.matches("(?s).*\\b(beam|bolt|blast|shot|arrow|projectile|fireball|laser|energy)\\b.*")) { prim.add("projectile"); prim.add("magic_damage"); counters.add("kiter"); }
        if (t.matches("(?s).*\\b(slash|sword|cut|strike|punch|kick|blade|claw)\\b.*")) { prim.add("melee_arc"); prim.add("physical_damage"); counters.add("caster"); }
        if (t.matches("(?s).*\\b(heal|recover|regenerat|restore|cure)\\b.*")) prim.add("heal");
        if (t.matches("(?s).*\\b(shield|barrier|guard|ward|defen[cs]e)\\b.*")) prim.add("shield");
        if (t.matches("(?s).*\\b(teleport|blink|dash|flash|step|warp)\\b.*")) prim.add("blink");
        if (t.matches("(?s).*\\b(pull|grab|grapple|chain|bind|hook)\\b.*")) { prim.add("pull"); counters.add("kiter"); }
        if (t.matches("(?s).*\\b(slow|freeze|paralys|stun|time)\\b.*")) prim.add("slow");
        if (t.matches("(?s).*\\b(seal|silence|nullif|cancel)\\b.*")) { prim.add("silence"); counters.add("caster"); }
        if (t.matches("(?s).*\\b(summon|clone|familiar|spirit|construct)\\b.*")) prim.add("summon_construct");
        if (prim.isEmpty()) { prim.add("projectile"); prim.add("magic_damage"); }
        List<String> limited = prim.stream().distinct().limit(6).toList();
        JsonObject o = new JsonObject();
        o.addProperty("id", "nusmp:" + slug(anime) + "_" + slug(ability));
        o.addProperty("anime", slug(anime));
        o.addProperty("name", titleCase(ability));
        o.addProperty("tier", 2);
        o.addProperty("cast_ticks", 16);
        o.addProperty("range", 14);
        com.google.gson.JsonArray pa = new com.google.gson.JsonArray();
        limited.forEach(pa::add);
        o.add("primitives", pa);
        com.google.gson.JsonArray ca = new com.google.gson.JsonArray();
        counters.stream().distinct().forEach(ca::add);
        o.add("counters", ca);
        o.addProperty("animation", "cast_grimoire");
        return AnimeSkill.parse(o);
    }

    private static String store(AnimeSkill skill) throws IOException {
        String why = SkillSandbox.validate(skill);
        if (why != null) return "The compiled skill was rejected: " + why;
        Files.createDirectories(AnimeSkillCodex.inbox());
        String file = skill.id().substring(skill.id().indexOf(':') + 1) + ".json";
        Path p = AnimeSkillCodex.inbox().resolve(file);
        Files.writeString(p, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(skill.toJson()));
        return "Candidate skill written: config/nusmp/codex-inbox/" + file + ". Review it, then /nusmp riven accept " + file;
    }

    private static void reply(CommandSourceStack src, String msg) { src.sendSuccess(() -> Component.literal(msg), false); }
    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private static String slug(String s) { String r = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", ""); return r.isEmpty() ? "x" : r.substring(0, Math.min(24, r.length())); }
    private static String titleCase(String s) { String t = s.trim(); return t.isEmpty() ? "Unnamed" : (Character.toUpperCase(t.charAt(0)) + t.substring(1)).substring(0, Math.min(40, t.length())); }
}
