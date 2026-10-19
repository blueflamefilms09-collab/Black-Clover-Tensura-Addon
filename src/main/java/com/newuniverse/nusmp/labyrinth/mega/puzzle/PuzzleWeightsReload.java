package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Why: the weights of the puzzle rooms are data ({@code data/nusmp/mega/puzzles/weights.json}), read again on every data reload so the owner can tune them with a datapack and
 * {@code /reload}. A missing or broken file leaves the built-in defaults in place and every problem is logged with its path, so a typo in a number never takes the puzzles away.
 */
final class PuzzleWeightsReload implements ResourceManagerReloadListener {
    private static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath("nusmp", "mega/puzzles/weights.json");

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        List<String> problems = new ArrayList<>();
        PuzzleWeights w = PuzzleWeights.defaults();
        try {
            var res = manager.getResource(FILE);
            if (res.isPresent()) {
                try (InputStream in = res.get().open()) {
                    w = PuzzleWeights.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8), problems);
                }
            } else problems.add("weights.json is missing: using the built-in defaults");
        } catch (java.io.IOException | RuntimeException e) {
            problems.add("weights.json could not be read: " + e);
        }
        PuzzleWeights.install(w);
        PuzzleDirector.forgetPlans();
        for (String p : problems) LogUtils.getLogger().warn("[nusmp] puzzles: {}", p);
        LogUtils.getLogger().info("[nusmp] Puzzle rooms: {} puzzles, {} per floor at most{}", w.types.size(), w.maxPerFloor, w.enabled ? "" : " (switched off)");
    }
}
