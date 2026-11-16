package com.newuniverse.nusmp.bossdrops;

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
 * Why: the loaded drop tables of the bosses ({@code data/anime_multiverse/boss_drops/<boss_id>.json}, see {@link BossDropTable}), read on every data reload (so {@code /reload} and a data pack
 * work) and swapped in as one immutable map: a reader on the server thread never sees a half-built one. A bad file is logged and skipped; the others stay. Server side only.
 */
public final class BossDropData extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String DIRECTORY = "boss_drops";
    private static volatile Map<String, BossDropTable.Table> tables = Map.of();

    public BossDropData() { super(new Gson(), DIRECTORY); }

    /** The table of a boss id (the file name), or null. */
    public static BossDropTable.Table table(String bossId) { return tables.get(bossId); }

    public static int size() { return tables.size(); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, BossDropTable.Table> out = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            String id = e.getKey().getPath();
            try {
                if (!e.getValue().isJsonObject()) throw new IllegalArgumentException("not an object");
                BossDropTable.Table t = BossDropTable.parse(e.getValue().getAsJsonObject());
                if (!t.boss().equals(id)) throw new IllegalArgumentException("the file is named " + id + " but says boss " + t.boss());
                out.put(id, t);
            } catch (RuntimeException ex) {
                LOG.warn("[anime_multiverse] boss drop table {} skipped: {}", e.getKey(), ex.getMessage());
            }
        }
        tables = Map.copyOf(out);
        LOG.info("[anime_multiverse] {} boss drop table(s) loaded", out.size());
    }
}
