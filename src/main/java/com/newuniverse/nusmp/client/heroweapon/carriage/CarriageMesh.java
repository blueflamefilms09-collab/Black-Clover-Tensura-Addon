package com.newuniverse.nusmp.client.heroweapon.carriage;

import com.newuniverse.nusmp.mesh.MeshText;
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

    static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("anime_multiverse", path); }

    /** One carriage look: the baked mesh with the skin and glow of its form. */
    record Look(MeshBake.Baked mesh, ResourceLocation skin, ResourceLocation glow) {}

    /** The looks read so far, by "classic:form" (a form id comes from the server, so the map is bounded and cleared when it grows). */
    private static final java.util.HashMap<String, Look> LOOKS = new java.util.HashMap<>();

    /** Forgets the baked models (a resource reload re-reads the meshes). */
    static void clear() { LOOKS.clear(); }

    /** The baked model, read on first use (never null). The plain carriage. */
    static MeshBake.Baked get() { return get("base").mesh(); }

    /**
     * 5.0.0: the carriage of a weapon form, the same mesh and skin the item in the hand uses ({@code weapon_meshes/vassal_carriage__<form>.txt}, {@code textures/entity/weapon/vassal_carriage__<form>.png}
     * and {@code _glow}); a form with no mesh of its own (or the setting {@code formModels} off) gets the plain carriage. With {@code vassalForms.classic} on the pre-remodel meshes are used where they exist.
     */
    static Look get(String form) {
        boolean classic = com.newuniverse.nusmp.client.heroweapon.HeroClientConfig.classicVassalForms();
        boolean forms = com.newuniverse.nusmp.client.heroweapon.HeroClientConfig.formModels();
        String f = !forms || form == null || form.isEmpty() ? "base" : form;
        String key = (classic ? "c:" : "n:") + f;
        Look look = LOOKS.get(key);
        if (look == null) {
            if (LOOKS.size() > 64) LOOKS.clear();
            look = loadLook(f, classic);
            LOOKS.put(key, look);
        }
        return look;
    }

    private static Look loadLook(String form, boolean classic) {
        String id = "base".equals(form) ? "vassal_carriage" : "vassal_carriage__" + form;
        if (classic) {
            Look old = tryLook("weapon_meshes/classic_vassal/" + id + ".txt", "textures/entity/weapon/classic_vassal/" + id);
            if (old != null) return old;
        }
        Look own = tryLook("weapon_meshes/" + id + ".txt", "textures/entity/weapon/" + id);
        if (own != null) return own;
        if (!"base".equals(form)) return loadLook("base", classic);
        MeshBake.Baked fb = MeshBake.fallback();
        return new Look(fb, FALLBACK_SKIN, GLOW);
    }

    private static Look tryLook(String meshPath, String skinBase) {
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl(meshPath));
            if (res.isEmpty()) return null;
            List<String> lines = new ArrayList<>();
            try (BufferedReader in = MeshText.reader(res.get())) {
                String line;
                while ((line = in.readLine()) != null) lines.add(line);
            }
            MeshBake.Baked baked = MeshBake.bake(lines, CarriageNumbers.MODEL_LENGTH);
            if (baked == null) return null;
            ResourceLocation skin = rl(skinBase + ".png");
            if (Minecraft.getInstance().getResourceManager().getResource(skin).isEmpty()) skin = SKIN;
            ResourceLocation glow = rl(skinBase + "_glow.png");
            if (Minecraft.getInstance().getResourceManager().getResource(glow).isEmpty()) glow = GLOW;
            return new Look(baked, skin, glow);
        } catch (Exception ex) {
            LogUtils.getLogger().error("[anime_multiverse] a carriage mesh failed to load: " + meshPath, ex);
            return null;
        }
    }
}
