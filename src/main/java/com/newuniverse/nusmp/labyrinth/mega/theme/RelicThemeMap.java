package com.newuniverse.nusmp.labyrinth.mega.theme;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Why: the hand-made half of the relic-to-theme mapping ({@link RelicThemes}) is data the owner will want to tune, so it lives in
 * {@code data/nusmp/mega/relic_themes.json} and is read by this reload listener (the pattern of {@code ThemeDefs} and of the Cursed Spirit roster) into
 * an immutable table that is swapped in with one assignment. A file that is missing or broken never stops the game: the table stays empty and every
 * relic is then placed by its keywords or its hash, with a warning in the log. The warnings of the last load are kept for the self-test that checks the
 * shipped file in the real game.
 */
public final class RelicThemeMap implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath("nusmp", "mega/relic_themes.json");
    private static volatile Map<String, String> EXPLICIT = Map.of();
    private static volatile List<String> WARNINGS = List.of();

    /** Relic key to theme id, as loaded (empty before the first load). */
    public static Map<String, String> explicit() { return EXPLICIT; }

    /** The warnings the last load produced (empty when the file was clean). */
    public static List<String> warnings() { return WARNINGS; }

    /** Replaces the table (the reload, and tests). */
    public static void replace(Map<String, String> table, List<String> warnings) {
        EXPLICIT = Map.copyOf(table);
        WARNINGS = List.copyOf(warnings);
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        RelicThemes.Parsed parsed = new RelicThemes.Parsed(Map.of(), List.of("could not be read"));
        try {
            Optional<Resource> res = manager.getResource(FILE);
            if (res.isPresent()) {
                try (Reader reader = res.get().openAsReader()) {
                    parsed = RelicThemes.parse(JsonTrees.read(reader));
                }
            } else {
                parsed = new RelicThemes.Parsed(Map.of(), List.of(FILE + " not found: every relic is placed by its keywords or its hash"));
            }
        } catch (Exception e) {
            parsed = new RelicThemes.Parsed(Map.of(), List.of(FILE + " could not be read: " + e));
        }
        for (String w : parsed.warnings()) LOG.warn("[nusmp] mega relic themes: {}", w);
        replace(parsed.explicit(), parsed.warnings());
        LOG.info("[nusmp] mega relic themes: {} relics placed by hand", parsed.explicit().size());
    }
}
