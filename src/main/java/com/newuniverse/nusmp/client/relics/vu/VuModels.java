package com.newuniverse.nusmp.client.relics.vu;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.relics.worn.lib.WornCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Why: the hand library ({@code HandForms}) and the worn library ({@code WornLibrary}) each answer "which models is this relic drawn as?" once per relic and cache it. For the
 * relics of the visual upgrade the answer comes from the override table (assets/nusmp/visual_upgrade/overrides.txt, {@link VuOverrides}) while {@code visual.upgrade} is on, and
 * from the older catalogs otherwise. This class reads the table once (forgotten on a resource reload), answers the pieces, and counts a {@link #generation()} that grows when the
 * option flips or the resources reload, so the two libraries drop their caches exactly then. Client thread only.
 */
public final class VuModels {
    private VuModels() {}

    private static VuOverrides table;
    private static int generation;
    private static boolean lastFlag = true;
    private static long retryAt;

    private static final VuOverrides EMPTY = VuOverrides.parse(List.of());

    /** Is the upgrade switched on ({@code visual.upgrade})? */
    public static boolean enabled() { return VuCharmsOptions.upgrade(); }

    /** Grows when the option flips or the resources reload: a cache that remembers the generation it was built in rebuilds when it changes. */
    public static int generation() {
        boolean f = enabled();
        if (f != lastFlag) {
            lastFlag = f;
            generation++;
        }
        return generation;
    }

    /** The override table (an empty one if the file cannot be read yet; the read is tried again after three seconds and an empty answer is never kept). */
    public static VuOverrides table() {
        if (table != null) return table;
        if (System.currentTimeMillis() < retryAt) return EMPTY;
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("nusmp", "visual_upgrade/overrides.txt"));
            if (res.isPresent()) {
                try (BufferedReader in = res.get().openAsReader()) {
                    VuOverrides t = VuOverrides.parse(in.lines().toList());
                    for (String p : t.problems()) LogUtils.getLogger().error("[nusmp] visual_upgrade/overrides.txt: {}", p);
                    table = t;
                }
            } else {
                LogUtils.getLogger().warn("[nusmp] visual_upgrade/overrides.txt is missing; the upgraded relic models are not used");
            }
        } catch (Exception ex) {
            LogUtils.getLogger().error("[nusmp] visual_upgrade/overrides.txt could not be read", ex);
        }
        if (table == null) {
            retryAt = System.currentTimeMillis() + 3000L;
            return EMPTY;
        }
        return table;
    }

    private static List<WornCatalog.Piece> pieces(VuOverrides.Entry e) {
        if (e == null) return null;
        List<WornCatalog.Piece> out = new ArrayList<>(e.pieces().size());
        for (String p : e.pieces()) out.add(WornCatalog.piece(p));
        return List.copyOf(out);
    }

    /** The hand entry of a relic while the upgrade is on, else null. */
    public static VuOverrides.Entry handEntry(String relicId) { return enabled() ? table().hand(relicId) : null; }

    /** The models a relic is held as while the upgrade is on, else null (the older catalog answers). */
    public static List<WornCatalog.Piece> handPieces(String relicId) { return pieces(handEntry(relicId)); }

    /** The models a relic wears while the upgrade is on, else null (the older catalog answers). */
    public static List<WornCatalog.Piece> wornPieces(String relicId) { return enabled() ? pieces(table().worn(relicId)) : null; }

    /** Forgets the table (a resource reload) and tells the caches to rebuild. */
    public static void clear() {
        table = null;
        retryAt = 0L;
        generation++;
    }
}
