package com.newuniverse.nusmp.client.heroweapon;

import com.newuniverse.nusmp.client.ui.move.GuiMove;
import com.newuniverse.nusmp.client.ui.theme.UiBar;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.heroweapon.HeroColors;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroLevels;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * Why: a hero weapon has a level to watch, a form that changes what the keys do, and three or five techniques on cooldowns, and the player needs them at a glance
 * while fighting. This is the small overlay at the bottom left, in the weapon's own colour and the mod's panel theme: the gem and the name, the level and a bar to
 * the next one; while the weapon is out also the active form and one row per technique (the input, the name, and a bar with the seconds left while it recharges);
 * and a red line when another Holy wielder nearby is stopping the experience. It reads {@link ClientHeroBond} only, draws with plain fills, and is moved or
 * switched off by the client config. The curse system draws its own bar below it.
 */
public final class HeroHud {
    private HeroHud() {}

    private static final int W = 152, PAD = 4, HEADER = 12, BAR = 6, ROW = 11;
    private static final String MOVE_ID = "hero_hud";

    /** The GUI layer. A fault in the drawing is logged once and never reaches the frame. */
    public static void render(GuiGraphics g, DeltaTracker delta) {
        SafeCast.client("hero.hud", () -> draw(g));
    }

    private static void draw(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        HeroKind kind = ClientHeroBond.kind();
        if (kind == null || mc.player == null || mc.options.hideGui || !HeroClientConfig.hud()) return;
        Font font = mc.font;
        int rgb = HeroColors.rgb(kind);
        boolean out = ClientHeroBond.summoned();
        boolean repelled = ClientHeroBond.repelled();
        List<String> slots = ClientHeroBond.slots();
        int rows = 0;
        for (String s : slots) if (!s.isEmpty()) rows++;
        int h = PAD + HEADER + BAR + (out ? ROW + rows * ROW : ROW) + (repelled ? ROW : 0) + PAD;

        if (GuiMove.hidden(MOVE_ID)) return;                                           // GUI Move: the player switched the panel off
        float scale = HeroClientConfig.scale();
        int px = HeroClientConfig.x(), pyBottom = g.guiHeight() - HeroClientConfig.y();
        int dw = Math.round(W * scale), dh = Math.round(h * scale);
        // GUI Move + GUI Size: not moved, the panel grows from its bottom-left corner as before; moved, it is drawn at the saved place (this config's x/y/scale stay the default)
        boolean moved = GuiMove.begin(g, MOVE_ID, px, pyBottom - dh, dw, dh, px, pyBottom);
        g.pose().pushPose();
        g.pose().translate(px, pyBottom, 0.0);
        g.pose().scale(scale, scale, 1.0f);
        try {
            float now = UiTheme.now();
            int top = -h;
            if (UiTheme.anime()) UiPanel.panel(g, 0, top, W, h, 0xFF000000 | rgb, 0.7f, 0.92f);
            else {
                g.fill(0, top, W, 0, 0xC0101018);
                g.renderOutline(0, top, W, h, 0xFF000000 | rgb);
            }
            int y = top + PAD;
            gem(g, 5, y + 2, 0xFF000000 | rgb);
            String level = Component.translatable("hero.hud.level", ClientHeroBond.level()).getString();
            int lw = font.width(level);
            g.drawString(font, font.plainSubstrByWidth(kind.display(), W - 14 - lw - 8), 14, y + 1, UiTheme.TEXT, true);
            g.drawString(font, level, W - PAD - lw, y + 1, 0xFF000000 | rgb, true);
            y += HEADER;
            UiBar.draw(g, PAD, y, W - 2 * PAD, 3, (float) HeroLevels.progress(ClientHeroBond.exp()), rgb, now, 1.0f);
            y += BAR;
            if (out) {
                String form = ClientHeroBond.formName().isEmpty() ? ClientHeroBond.form() : ClientHeroBond.formName();
                g.drawString(font, font.plainSubstrByWidth(Component.translatable("hero.hud.form", form).getString(), W - 2 * PAD), PAD + 1, y, UiTheme.TEXT_DIM, false);
                y += ROW;
                for (int i = 0; i < slots.size(); i++) {
                    if (slots.get(i).isEmpty()) continue;
                    skillRow(g, font, i, slots.get(i), y, rgb);
                    y += ROW;
                }
            } else {
                g.drawString(font, Component.translatable("hero.hud.stowed", HeroKeysClient.name(3)).getString(), PAD + 1, y, UiTheme.TEXT_FAINT, false);
                y += ROW;
            }
            if (repelled) g.drawString(font, font.plainSubstrByWidth(Component.translatable("hero.hud.repelled").getString(), W - 2 * PAD), PAD + 1, y, UiTheme.RED, true);
        } finally {
            g.pose().popPose();
            GuiMove.end(g, moved);
        }
    }

    /** One technique: its input, its name, and while it recharges a bar and the seconds left. */
    private static void skillRow(GuiGraphics g, Font font, int slot, String skillId, int y, int rgb) {
        String label = slot == 0 ? Component.translatable("hero.hud.use").getString() : slot == 1 ? Component.translatable("hero.hud.sneak").getString() : HeroKeysClient.name(slot - 2);
        int lw = font.width(label);
        g.drawString(font, label, PAD + 1, y, UiMath.withAlpha(rgb, 0.95f), false);
        HeroSkill skill = HeroLogics.skill(skillId);
        String name = Component.translatableWithFallback("hero.skill." + skillId + ".name", skill == null ? skillId : skill.name()).getString();
        int left = ClientHeroBond.cooldownLeft(skillId);
        int right = left > 0 ? 40 : 0;
        int nameX = PAD + 1 + Math.max(lw, 22) + 4;
        g.drawString(font, font.plainSubstrByWidth(name, W - PAD - right - nameX), nameX, y, left > 0 ? UiTheme.TEXT_DIM : UiTheme.TEXT, false);
        if (left <= 0) return;
        float frac = Math.min(1.0f, left / (float) Math.max(1, ClientHeroBond.cooldownTotal(skillId)));
        String secs = String.format(Locale.ROOT, "%.1f", left / 20.0);
        int sw = font.width(secs);
        g.drawString(font, secs, W - PAD - 24 - 3 - sw, y, UiTheme.TEXT_DIM, false);
        UiBar.draw(g, W - PAD - 24, y + 3, 24, 2, frac, 0xFF8FA3BF, 0f, 1.0f);
    }

    /** The weapon's gem: a small diamond in its colour. */
    private static void gem(GuiGraphics g, int x, int y, int argb) {
        for (int r = 0; r < 7; r++) {
            int half = r <= 3 ? r : 6 - r;
            g.fill(x + 3 - half, y + r, x + 3 + half + 1, y + r + 1, argb);
        }
        g.fill(x + 3, y + 1, x + 4, y + 2, 0xFFFFFFFF);
    }
}
