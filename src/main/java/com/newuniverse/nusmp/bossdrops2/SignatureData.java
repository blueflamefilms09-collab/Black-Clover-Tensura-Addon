package com.newuniverse.nusmp.bossdrops2;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: the loaded signature tables ({@code data/anime_multiverse/boss_signature/<entity path>.json}, see {@link SignatureTable}), read on every data reload (so {@code /reload} and a data pack work) and
 * swapped in as one immutable map, so a reader on the server thread never sees a half-built one. A bad file is logged and skipped; the others stay. Server side only.
 */
public final class SignatureData extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String DIRECTORY = "boss_signature";
    private static volatile Map<String, SignatureTable.Table> tables = Map.of();

    public SignatureData() { super(new Gson(), DIRECTORY); }

    /** The table of a boss (the entity type path, the file name), or null. */
    public static SignatureTable.Table table(String boss) { return boss == null ? null : tables.get(boss); }

    public static int size() { return tables.size(); }

    public static java.util.Collection<SignatureTable.Table> all() { return tables.values(); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, SignatureTable.Table> out = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            String id = e.getKey().getPath();
            try {
                if (!e.getValue().isJsonObject()) throw new IllegalArgumentException("not an object");
                SignatureTable.Table t = SignatureTable.parse(e.getValue().getAsJsonObject());
                if (!t.boss().equals(id)) throw new IllegalArgumentException("the file is named " + id + " but says boss " + t.boss());
                out.put(id, t);
            } catch (RuntimeException ex) {
                LOG.warn("[anime_multiverse] boss signature table {} skipped: {}", e.getKey(), ex.getMessage());
            }
        }
        tables = Map.copyOf(out);
        LOG.info("[anime_multiverse] {} boss signature table(s) loaded", out.size());
    }
}
