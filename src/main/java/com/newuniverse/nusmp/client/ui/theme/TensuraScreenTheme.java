package com.newuniverse.nusmp.client.ui.theme;

import com.newuniverse.nusmp.client.multiverse.MultiverseStatusClient;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.client.screen.AbilitySelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Field;

/**
 * The anime look on Tensura's own ability windows. The mod does not draw them (Tensura does), so this is a pure overlay on top of
 * {@code ScreenEvent.Render.Post}: nothing in Tensura's screens, clicks or logic is touched.
 * <ul>
 *   <li>{@code AbilitySelectionScreen} (the window that lists the skill / magic names and the three slots): a faint slow magic circle behind
 *       the whole window, glowing corner brackets round Tensura's panel, and below it a Great-Sage readout of the selected ability: its name,
 *       "Analysis complete.", a result line with its mastery and a mastery bar (for a grimoire also how many pages mastery has opened).</li>
 *   <li>{@code SubAbilityModeSelectionScreen} and {@code AbilityCategoriesScreen}: the circle and brackets, plus a one-line notice.</li>
 * </ul>
 * Where Tensura's panel is comes from its protected statics ({@code guiLeft}...), read once by reflection; if that fails the brackets are
 * skipped and the readout sits at the bottom of the screen. Everything is wrapped so that a surprise never breaks a Tensura screen.
 */
public final class TensuraScreenTheme {
    private TensuraScreenTheme() {}

    private static final String PKG = "io.github.manasmods.tensura.client.screen.";
    private static final String SELECT = PKG + "AbilitySelectionScreen", MODE = PKG + "SubAbilityModeSelectionScreen", CATEGORIES = PKG + "AbilityCategoriesScreen";
    private static final SageBlock SAGE = new SageBlock();
    private static final UiBar BAR = new UiBar();
    private static Field fLeft, fRight, fTop, fBottom;
    private static boolean looked, boundsFailed, warned;

    /** Game bus. */
    public static void init() { NeoForge.EVENT_BUS.addListener(TensuraScreenTheme::onRenderPost); }

    private static void onRenderPost(ScreenEvent.Render.Post e) {
        Screen s = e.getScreen();
        String n = s.getClass().getName();
        if (!(n.equals(SELECT) || n.equals(MODE) || n.equals(CATEGORIES)) || !UiTheme.anime()) return;
        try {
            draw(e.getGuiGraphics(), s, n);
        } catch (RuntimeException | LinkageError ex) {
            if (!warned) {
                warned = true;
                com.mojang.logging.LogUtils.getLogger().warn("[nusmp] The anime overlay skipped a Tensura screen it could not read", ex);
            }
        }
    }

    private static void draw(GuiGraphics g, Screen s, String name) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        float now = UiTheme.now();
        int w = s.width, h = s.height;
        int[] box = bounds(w, h);                                  // left, top, right, bottom of Tensura's panel (or a guess), then 1 if it is exact
        int cx = (box[0] + box[2]) / 2, cy = (box[1] + box[3]) / 2;
        float radius = Math.max(40f, Math.min(box[2] - box[0], box[3] - box[1]) * 0.62f);
        MagicCircle.draw(g, cx, cy, radius, now, UiTheme.CYAN, 0.12f, MagicCircle.CALM, 1f);
        if (box[4] == 1) {
            UiPanel.brackets(g, box[0] - 3, box[1] - 3, box[2] - box[0] + 6, box[3] - box[1] + 6, 9, UiMath.withAlpha(UiTheme.CYAN, 0.75f));
            UiPanel.sweep(g, box[0], box[1], box[2] - box[0], box[3] - box[1], now, UiTheme.CYAN, 0.7f);
        }

        // the readout: the text decides how tall the panel is
        int rw = Math.min(280, Math.max(160, box[2] - box[0])), tw = rw - 14;
        ManasSkillInstance inst = name.equals(SELECT) && s instanceof AbilitySelectionScreen sel ? sel.selectedAbility : null;
        float frac = 0f;
        if (inst != null) {
            frac = describe(font, inst, tw, now);
        } else if (name.equals(SELECT)) {
            SAGE.set(font, UiLang.get(UiLang.ANALYZING), UiLang.get(UiLang.NOTICE, UiLang.get("nusmp.ui.tensura.select")), "", tw, now);
        } else {
            SAGE.set(font, UiLang.get(UiLang.ANALYSIS_COMPLETE),
                    UiLang.get(UiLang.NOTICE, UiLang.get(name.equals(MODE) ? "nusmp.ui.tensura.mode" : "nusmp.ui.tensura.category")), "", tw, now);
        }
        int rh = 12 + SAGE.height(font) + (inst != null ? 8 : 0);
        int rx = cx - rw / 2;
        int ry = h - box[3] >= rh + 6 ? box[3] + 4 : box[1] >= rh + 6 ? box[1] - rh - 4 : h - rh - 2;     // below Tensura's panel, else above, else the screen's bottom
        UiPanel.panel(g, rx, ry, rw, rh, UiTheme.CYAN, 0.7f, 1f);
        int ny = SAGE.draw(g, font, rx + 7, ry + 6, now, 1f);
        if (inst != null) {
            BAR.update(frac, now, false);
            BAR.draw(g, rx + 7, ny + 1, tw, 4, UiTheme.VIOLET, now, 1f);
        }
    }

    /** Sets the Sage block for a selected ability and returns its mastery fraction. */
    private static float describe(Font font, ManasSkillInstance inst, int w, float now) {
        String skill = inst.getDisplayName().getString();
        int max = inst.getMaxMastery();
        double mastery = inst.getMastery();
        float frac = max > 0 ? (float) Math.min(1.0, Math.max(0.0, mastery / max)) : 0f;
        String result = max > 0 ? UiLang.get(UiLang.RESULT, UiLang.get("nusmp.ui.tensura.mastery", (int) mastery, max))
                : UiLang.get(UiLang.RESULT, UiLang.get("nusmp.ui.tensura.no_mastery"));
        if (max > 0 && frac >= 1f) result = UiLang.get(UiLang.NOTICE, UiLang.get("nusmp.ui.tensura.mastery_full"));
        ResourceLocation id = inst.getSkillId();
        if (id != null && "nusmp".equals(id.getNamespace()) && id.getPath().contains("grimoire")) {
            CompoundTag gr = MultiverseStatusClient.status().getCompound("Grimoire");
            if (gr.getInt("Pages") > 0) result = result + "  " + UiLang.get("nusmp.ui.tensura.pages", gr.getInt("Unlocked"), gr.getInt("Pages"));
        }
        SAGE.set(font, UiLang.get(UiLang.ANALYSIS_COMPLETE), skill, result, w, now);
        return frac;
    }

    /** Tensura's panel: {left, top, right, bottom, exact} in GUI pixels; exact is 1 when read from Tensura, 0 for a guess. */
    private static int[] bounds(int w, int h) {
        if (!looked) {
            looked = true;
            try {
                Class<?> c = Class.forName(PKG + "templates.SimpleScreen");
                fLeft = field(c, "guiLeft"); fRight = field(c, "guiRight"); fTop = field(c, "guiTop"); fBottom = field(c, "guiBottom");
            } catch (ReflectiveOperationException | RuntimeException ex) {
                boundsFailed = true;
            }
        }
        if (!boundsFailed) {
            try {
                int l = fLeft.getInt(null), r = fRight.getInt(null), t = fTop.getInt(null), b = fBottom.getInt(null);
                if (r - l >= 60 && b - t >= 60 && r <= w + 2 && b <= h + 2 && l >= -2 && t >= -2) return new int[]{l, t, r, b, 1};
            } catch (ReflectiveOperationException | RuntimeException ex) {
                boundsFailed = true;
            }
        }
        return new int[]{w / 2 - 128, h / 2 - 100, w / 2 + 128, h / 2 + 100, 0};
    }

    private static Field field(Class<?> c, String name) throws ReflectiveOperationException {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }
}
