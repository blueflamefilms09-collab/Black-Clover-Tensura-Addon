package com.newuniverse.nusmp.avatar.aptitude;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: the reload listener that fills {@link ForgeSkills} from every {@code data/<namespace>/avatar_skills/*.json}, the twin of {@link AptitudeLoader}:
 * one bad file is logged and skipped, the list is swapped whole, a duplicate skill id keeps the first file in resource order (later packs override by
 * shipping the same path). Server side only.
 */
public final class ForgeSkillLoader implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String DIR = "avatar_skills";

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        List<ForgeSkill> list = new ArrayList<>();
        Map<ResourceLocation, Resource> found = manager.listResources(DIR, rl -> rl.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> e : new java.util.TreeMap<>(found).entrySet()) {
            String path = e.getKey().getPath();
            String file = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
            try (Reader reader = e.getValue().openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                list.add(ForgeSkillParser.parse(file, root));
            } catch (Exception ex) {
                LOG.warn("[nusmp] Skipping avatar skill {}: {}", e.getKey(), ex.toString());
            }
        }
        ForgeSkills.replaceAll(list);
        LOG.info("[nusmp] Loaded {} Soul Forge skills", ForgeSkills.size());
    }
}
