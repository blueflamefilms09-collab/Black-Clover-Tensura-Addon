package com.newuniverse.nusmp.core.magic.profile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.skill.NUSkills;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Runs {@link MagicAudit} against the real game: the real enum, the registered books, the mod's own resources and its en_us lang file.
 * Works on both sides (it reads the jar's resources, not the resource manager). It never throws; at game start it only logs
 * (errors one by one, warnings as a summary by code) and the full list is one command away (/multiverse magic audit).
 */
public final class MagicAuditRuntime {
    private MagicAuditRuntime() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static Set<String> langKeys;

    /** The audit over every enum constant. Never null. */
    public static MagicAudit.Report run() {
        List<MagicAudit.Finding> extra = new ArrayList<>();
        MagicAudit.Report base;
        try {
            base = MagicAudit.run(MagicProfiles.allEnum(), new RuntimeProbe(extra), MagicType.values().length);
        } catch (Throwable t) {
            extra.add(new MagicAudit.Finding(MagicAudit.Severity.ERROR, "*", "AUDIT_FAILED", String.valueOf(t)));
            base = new MagicAudit.Report(new ArrayList<>(), 0);
        }
        return base.plus(extra);
    }

    /** Game start: logs the result. Call once from common setup (after the registries are filled). */
    public static void runAtStartup() {
        try {
            MagicAudit.Report r = run();
            for (MagicAudit.Finding f : r.of(MagicAudit.Severity.ERROR)) LOG.error("[nusmp] magic audit: {}", f);
            if (r.warnings() > 0) LOG.warn("[nusmp] magic audit: {} magics, {} warnings ({}); the full list: /multiverse magic audit", r.checked(), r.warnings(), r.summary());
            else LOG.info("[nusmp] magic audit: {} magics, {} errors, {} warnings", r.checked(), r.errors(), r.warnings());
        } catch (Throwable t) {
            LOG.error("[nusmp] magic audit could not run", t);
        }
    }

    private static synchronized Set<String> lang() {
        if (langKeys != null) return langKeys;
        Set<String> keys = new HashSet<>();
        try (InputStream in = MagicAuditRuntime.class.getResourceAsStream("/assets/nusmp/lang/en_us.json")) {
            if (in != null) {
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (var e : o.entrySet()) { JsonElement v = e.getValue(); if (v != null && v.isJsonPrimitive()) keys.add(e.getKey()); }
            }
        } catch (Exception e) {
            LOG.warn("[nusmp] magic audit could not read en_us.json: {}", e.toString());
        }
        langKeys = keys;
        return keys;
    }

    private static final class RuntimeProbe implements MagicAudit.Probe {
        private final List<MagicAudit.Finding> extra;
        RuntimeProbe(List<MagicAudit.Finding> extra) { this.extra = extra; }

        @Override public boolean hasResource(String assetPath) { return MagicAuditRuntime.class.getResource("/assets/nusmp/" + assetPath) != null; }
        @Override public boolean hasLang(String key) { return lang().contains(key); }

        private static GrimoireBook book(String id) {
            MagicType t = MagicType.lookup(id);
            return t == null ? null : NUSkills.bookFor(t);
        }

        @Override public String bookPath(String id) {
            try { GrimoireBook b = book(id); return b == null ? null : b.getRegistryName().getPath(); }
            catch (Throwable t) { extra.add(new MagicAudit.Finding(MagicAudit.Severity.ERROR, id, "BOOK_THROWS", String.valueOf(t))); return null; }
        }

        @Override public int pageCount(String id) {
            try { GrimoireBook b = book(id); return b == null ? -1 : b.pages().size(); }
            catch (Throwable t) { extra.add(new MagicAudit.Finding(MagicAudit.Severity.ERROR, id, "PAGES_THROW", String.valueOf(t))); return 0; }
        }

        @Override public java.util.List<String> familyPageIds(String id) {
            try {
                GrimoireBook b = book(id);
                if (b == null) return java.util.List.of();
                java.util.List<String> ids = new ArrayList<>();
                for (int k = 0; k < b.familyCount() && k < b.pages().size(); k++) ids.add(b.pages().get(k).id());
                return ids;
            } catch (Throwable t) {
                return java.util.List.of();
            }
        }

        @Override public boolean legacySkill(String id) {
            MagicType t = MagicType.lookup(id);
            return t != null && NUSkills.GRIMOIRES.containsKey(t);
        }
    }
}
