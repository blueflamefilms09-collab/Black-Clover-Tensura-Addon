package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: which monsters stand for "undead", "swarm" or "knight" at which depth is data ({@code data/nusmp/mega/mechanic_groups.json}), read again on every data reload. A missing or
 * broken file leaves the built-in roster in place ({@link MobGroups}), and every problem is logged, so a typo in a datapack never takes the monsters away.
 */
final class MobGroupsReload implements ResourceManagerReloadListener {
    private static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath("nusmp", "mega/mechanic_groups.json");

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        List<String> problems = new ArrayList<>();
        Map<String, List<MobGroups.Entry>> groups = Map.of();
        try {
            var res = manager.getResource(FILE);
            if (res.isPresent()) {
                try (InputStream in = res.get().open()) {
                    groups = MobGroups.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8), problems);
                }
            }
        } catch (java.io.IOException | RuntimeException e) {
            problems.add("mechanic_groups.json could not be read: " + e);
        }
        MobGroups.install(groups);
        for (String p : problems) LogUtils.getLogger().warn("[nusmp] mechanic groups: {}", p);
    }
}
