package com.newuniverse.nusmp.mana.zone;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * The Mana Zone's numbers in force: {@code data/nusmp/mana_zone.json}, read at server start and at every {@code /reload} (so a datapack may retune them).
 * Until a file is read, and when it is broken, the in-code defaults (identical to the shipped file, a test checks it) are in force.
 */
public final class ManaZoneData {
    private ManaZoneData() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath("nusmp", "mana_zone.json");
    private static volatile ManaZoneRules rules = ManaZoneRules.defaults();

    public static ManaZoneRules rules() { return rules; }

    public static ManaZoneRules.Gate gate() { return rules.gate(); }

    public static ManaZoneRules.Zone zone() { return rules.zone(); }

    /** The reload listener (registered by ManaZoneBoot on AddReloadListenerEvent). */
    public static final class Loader extends SimplePreparableReloadListener<ManaZoneRules> {
        @Override
        protected ManaZoneRules prepare(ResourceManager manager, ProfilerFiller profiler) {
            var res = manager.getResource(FILE);
            if (res.isEmpty()) return ManaZoneRules.defaults();
            try (InputStream in = res.get().open()) {
                return ManaZoneRules.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (Exception e) {
                LOG.warn("[nusmp] {} could not be read, the defaults stay in force: {}", FILE, e.toString());
                return ManaZoneRules.defaults();
            }
        }

        @Override
        protected void apply(ManaZoneRules loaded, ResourceManager manager, ProfilerFiller profiler) {
            rules = loaded;
            LOG.info("[nusmp] Mana Zone rules loaded: gate {} EP, step {}, {} boss kills, floor {}, attunement {} s; radius {}..{}", (long) loaded.gate().minEp(), loaded.gate().minKnightStep(),
                    loaded.gate().minBossKills(), loaded.gate().minDeepestFloor(), loaded.gate().attuneSeconds(), loaded.zone().baseRadius(), loaded.zone().maxRadius());
        }
    }
}
