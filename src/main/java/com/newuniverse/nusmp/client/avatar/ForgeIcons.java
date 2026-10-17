package com.newuniverse.nusmp.client.avatar;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.newuniverse.nusmp.avatar.net.ForgeAptitudeRow;
import com.newuniverse.nusmp.avatar.net.ForgeSkillRow;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Why: the Soul Forge grids show a circular icon for every aptitude (a texture of this mod) and for every skill (the skill's OWN icon from the game's skill
 * registry: Tensura's textures for Tensura's skills, this mod's for its own). Nobody can promise that every texture exists in the running resource packs, and
 * drawing a missing one would show the purple-and-black checkerboard, so this asks the resource manager first, remembers the answer and the texture's real size
 * (icons come in 16, 32 and 64 pixels; the whole image is always drawn, whatever its size), and lets the caller draw a lettered disc instead when nothing is
 * found. The cache is emptied on every resource reload and on logout; nothing here ever throws.
 */
final class ForgeIcons {
    private ForgeIcons() {}

    private static final Map<ResourceLocation, int[]> DIMS = new HashMap<>();
    private static final Set<ResourceLocation> MISSING = new HashSet<>();

    static void clear() {
        DIMS.clear();
        MISSING.clear();
    }

    /** The width and height of a texture file, or null when no resource pack has it (both answers are remembered). */
    static int[] dims(ResourceLocation tex) {
        if (tex == null) return null;
        int[] known = DIMS.get(tex);
        if (known != null) return known;
        if (MISSING.contains(tex)) return null;
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(tex);
            if (res.isPresent()) {
                try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
                    int[] d = {Math.max(1, img.getWidth()), Math.max(1, img.getHeight())};
                    DIMS.put(tex, d);
                    return d;
                }
            }
        } catch (Throwable unreadable) {
            // fall through: a texture that cannot be read is a texture that is missing
        }
        MISSING.add(tex);
        return null;
    }

    /**
     * Draws the whole texture into a {@code size} x {@code size} square with the given tint (0xRRGGBB, white = as is) and alpha. Returns false, drawing
     * nothing, when the texture does not exist.
     */
    static boolean draw(GuiGraphics g, ResourceLocation tex, int x, int y, int size, int tint, float alpha) {
        int[] d = dims(tex);
        if (d == null || alpha <= 0.003f) return d != null;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(((tint >> 16) & 255) / 255f, ((tint >> 8) & 255) / 255f, (tint & 255) / 255f, Math.min(1f, alpha));
        g.blit(tex, x, y, size, size, 0f, 0f, d[0], d[1], d[0], d[1]);
        g.setColor(1f, 1f, 1f, 1f);
        return true;
    }

    // ---------------------------------------------------------------- what to draw for a row
    private static ResourceLocation parse(String s) { return s == null || s.isEmpty() ? null : ResourceLocation.tryParse(s); }

    /** The icon of an aptitude row: the texture its data names (or the default one by id). */
    static ResourceLocation aptitudeTexture(ForgeAptitudeRow row) {
        ResourceLocation t = parse(row.icon());
        return t != null ? t : parse("nusmp:textures/gui/soulforge/apt/" + row.id() + ".png");
    }

    /** The registry skill of a row, or null when this client does not know it (never throws). */
    static ManasSkill skill(ForgeSkillRow row) {
        try {
            ResourceLocation id = ResourceLocation.tryParse(row.id());
            return id == null ? null : SkillAPI.getSkillRegistry().get(id);
        } catch (Throwable noSkills) {
            return null;
        }
    }

    /** The icon of a skill row: the data's override when it has one, else the skill's own icon; null when neither is known. */
    static ResourceLocation skillTexture(ForgeSkillRow row) {
        ResourceLocation over = parse(row.icon());
        if (over != null) return over;
        ManasSkill s = skill(row);
        if (s == null) return null;
        try {
            return s.getSkillIcon();
        } catch (Throwable noIcon) {
            return null;
        }
    }

    /** A readable name for a skill id ("tensura:flame_manipulation"): the game's own name when the skill is registered, else the id's last part in words. */
    static String skillLabel(String id) {
        try {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            ManasSkill s = rl == null ? null : SkillAPI.getSkillRegistry().get(rl);
            if (s != null) {
                Component n = s.getName();
                if (n != null && !n.getString().isEmpty()) return n.getString();
            }
        } catch (Throwable noSkills) {
            // fall back to the words in the id
        }
        return ForgeUi.titleCase(id == null ? "" : id.substring(id.indexOf(':') + 1));
    }

    /** The skill's own translated name when the game has one, else the name the data file gave. */
    static Component skillName(ForgeSkillRow row) {
        ManasSkill s = skill(row);
        if (s != null) {
            try {
                Component n = s.getName();
                if (n != null && !n.getString().isEmpty()) return n;
            } catch (Throwable noName) {
                // use the data's name
            }
        }
        return Component.literal(row.name());
    }
}
