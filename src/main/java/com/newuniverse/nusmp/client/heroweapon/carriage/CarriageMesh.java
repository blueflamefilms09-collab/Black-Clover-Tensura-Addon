package com.newuniverse.nusmp.client.heroweapon.carriage;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.heroweapon.weapon.carriage.CarriageNumbers;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Why: the carriage you ride is drawn from the SAME mesh file as the small carriage in the hand ({@code weapon_meshes/vassal_carriage.txt}, built by the art builder
 * with the rest of the weapon models), so there is one model to look after and the two always match. This class is only the part that needs the game: it finds
 * the file in the resource packs, hands its lines to the pure {@link MeshBake} (which maps them into the entity's frame at {@link CarriageNumbers#MODEL_LENGTH}
 * blocks long), and keeps the result until the resources reload. If the mesh is missing or unreadable (the art is not in this build, or a line is broken) a plain
 * wooden wagon of boxes is used instead, so the carriage is never invisible.
 */
final class CarriageMesh {
    private CarriageMesh() {}

    static final ResourceLocation SKIN = rl("textures/entity/weapon/vassal_carriage.png"), GLOW = rl("textures/entity/weapon/vassal_carriage_glow.png"),
            FALLBACK_SKIN = ResourceLocation.withDefaultNamespace("textures/block/spruce_planks.png");

    private static MeshBake.Baked cached;
    private static boolean loaded;

    static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    /** Forgets the baked model (a resource reload re-reads the mesh). */
    static void clear() {
        cached = null;
        loaded = false;
    }

    /** The baked model, read on first use (never null). */
    static MeshBake.Baked get() {
        if (!loaded) {
            loaded = true;
            cached = load();
        }
        return cached;
    }

    private static MeshBake.Baked load() {
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl("weapon_meshes/vassal_carriage.txt"));
            if (res.isPresent()) {
                List<String> lines = new ArrayList<>();
                try (BufferedReader in = res.get().openAsReader()) {
                    String line;
                    while ((line = in.readLine()) != null) lines.add(line);
                }
                MeshBake.Baked baked = MeshBake.bake(lines, CarriageNumbers.MODEL_LENGTH);
                if (baked != null) return baked;
            }
        } catch (Exception ex) {
            LogUtils.getLogger().error("[nusmp] the carriage mesh failed to load; a plain wagon is drawn instead", ex);
        }
        return MeshBake.fallback();
    }
}
