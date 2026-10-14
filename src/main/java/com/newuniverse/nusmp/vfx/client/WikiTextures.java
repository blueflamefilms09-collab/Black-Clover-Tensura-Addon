package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.WikiStyles;
import com.newuniverse.nusmp.vfx.WikiStyles.Group;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * The textures of the wiki magics (assets/nusmp/textures/particle/wiki_*.png, tools/gen_wiki_magic_textures.py), with a safe fallback.
 *
 * <p>Whether each file exists is checked ONCE, when resources load ({@link #reload}, called from {@link VfxDefinitions}) or on first
 * use, and cached in arrays; the render path only reads the arrays, it never asks the resource manager. A texture that is missing
 * (an old resource pack, a typo, a failed lookup) answers {@link VfxTextures#GLOW} with additive blending, so a wiki effect can
 * degrade to a plain glow but never throws and never shows the purple missing texture.
 */
public final class WikiTextures {
    private WikiTextures() {}

    private static final class Table {
        final ResourceLocation[] accent = new ResourceLocation[WikiStyles.COUNT];
        final boolean[] accentOk = new boolean[WikiStyles.COUNT];
        final ResourceLocation[] bulk = new ResourceLocation[Group.values().length];
        final boolean[] bulkOk = new boolean[Group.values().length];
        final ResourceLocation[] shell = new ResourceLocation[Group.values().length];
        final boolean[] shellOk = new boolean[Group.values().length];
    }

    private static volatile Table table;

    /** Re-reads which files exist (resource reload: F3+T, a resource pack change). */
    public static void reload(ResourceManager manager) {
        Table t = new Table();
        for (int i = 0; i < WikiStyles.COUNT; i++) {
            t.accent[i] = VfxTextures.byName(WikiStyles.texture(i));
            t.accentOk[i] = exists(manager, t.accent[i]);
        }
        for (Group g : Group.values()) {
            int k = g.ordinal();
            t.bulk[k] = VfxTextures.byName(WikiStyles.groupTexture(g));
            t.bulkOk[k] = exists(manager, t.bulk[k]);
            t.shell[k] = VfxTextures.byName(WikiStyles.shellTexture(g));
            t.shellOk[k] = exists(manager, t.shell[k]);
        }
        table = t;
    }

    private static boolean exists(ResourceManager manager, ResourceLocation id) {
        try {
            return manager != null && manager.getResource(id).isPresent();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static Table table() {
        Table t = table;
        if (t == null) {
            ResourceManager manager = null;
            try {
                manager = Minecraft.getInstance().getResourceManager();
            } catch (RuntimeException ignored) {
                // no resource manager yet: everything reads as missing and falls back to the glow
            }
            reload(manager);
            t = table;
        }
        return t;
    }

    /** The accent sprite of a magic (its own motif), or the glow when the file is missing. */
    public static ResourceLocation accent(int style) {
        Table t = table();
        return WikiStyles.valid(style) && t.accentOk[style] ? t.accent[style] : VfxTextures.GLOW;
    }

    public static VfxBlend accentBlend(int style) {
        Table t = table();
        if (!WikiStyles.valid(style) || !t.accentOk[style]) return VfxBlend.ADD;
        return WikiStyles.group(style).additive ? VfxBlend.ADD : VfxBlend.ALPHA;
    }

    /** The bulk sprite of a group, or the glow when the file is missing. */
    public static ResourceLocation bulk(Group g) {
        Table t = table();
        return t.bulkOk[g.ordinal()] ? t.bulk[g.ordinal()] : VfxTextures.GLOW;
    }

    public static VfxBlend bulkBlend(Group g) {
        Table t = table();
        if (!t.bulkOk[g.ordinal()]) return VfxBlend.ADD;
        return g.additive ? VfxBlend.ADD : VfxBlend.ALPHA;
    }

    /** True when the group's own bulk sprite was found (false = the glow stands in). */
    public static boolean hasBulk(Group g) { return table().bulkOk[g.ordinal()]; }

    /** The armour shell pattern of a group (64x64, tileable), or the glow when missing. */
    public static ResourceLocation shell(Group g) {
        Table t = table();
        return t.shellOk[g.ordinal()] ? t.shell[g.ordinal()] : VfxTextures.GLOW;
    }

    public static boolean hasShell(Group g) { return table().shellOk[g.ordinal()]; }
}
