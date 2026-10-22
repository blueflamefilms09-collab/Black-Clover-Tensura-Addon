package com.newuniverse.nusmp.client.shishou;

import com.newuniverse.nusmp.client.ui.move.GuiMove;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.shishou.ShishouAppraisal;
import com.newuniverse.nusmp.shishou.ShishouGuiLayout;
import com.newuniverse.nusmp.shishou.ShishouModes;
import com.newuniverse.nusmp.shishou.ShishouNet;
import com.newuniverse.nusmp.shishou.ShishouSkill;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the sword's panel while he is held: his level and the way to the next one, his mana, the selected skill with its cooldown and the key hint, the windows that are open (barrier, sense, stealth, released)
 * and, below, the Appraisal readout of what the crosshair is on (name, health, a level and a mana estimate, its weak elements, the skill it would give him). One small Akashic panel (dark star field, glowing cyan frame, sword emblem, glowing gauges, drifting motes: {@link AkashicTheme}), drawn
 * through the movable-GUI framework (id {@code shishou_hud}: the player can drag, size and hide it, docs/ui/gui_move.md). Reads {@link ShishouClientState} only; one string per row, no allocation of
 * note per frame beyond the row texts; a fault is logged once and never reaches the frame.
 */
public final class ShishouHud {
    private ShishouHud() {}

    public static final String MOVE_ID = "shishou_hud";
    private static final int W = ShishouGuiLayout.HUD_W, PAD = ShishouGuiLayout.HUD_PAD, ROW = ShishouGuiLayout.HUD_ROW, BAR = ShishouGuiLayout.HUD_BAR;

    public static void render(GuiGraphics g, DeltaTracker delta) { SafeCast.client("shishou.hud", () -> draw(g)); }

    private static void draw(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        ShishouNet.Sync s = ShishouClientState.state();
        ShishouNet.AppraisalReply r = s == null ? null : ShishouClientState.readout();
        if (mc.player == null || mc.options.hideGui || s == null && !GuiMove.editing()) return;
        if (GuiMove.hidden(MOVE_ID)) return;
        Font font = mc.font;
        List<String> lines = new ArrayList<>();
        if (r != null) readoutLines(font, r, lines);
        ShishouSkill sel = s == null || s.selected() < 0 || s.selected() >= ShishouSkill.values().length ? null : ShishouSkill.values()[s.selected()];
        ShishouModes.Mode mode = sel == null ? null : ShishouModes.modeOf(sel);
        int h = PAD + (ROW + 3) + (BAR + 2) + (BAR + 4) + (ROW - 2) + (mode != null ? ROW - 2 : 0) + ROW + (flagsOf(s) > 0 ? ROW : 0) + (lines.isEmpty() ? 0 : 4 + lines.size() * ROW) + PAD;   // the sum of the rows drawn below
        int px = g.guiWidth() - W - 6, py = g.guiHeight() - h - 40;
        boolean moved = GuiMove.begin(g, MOVE_ID, px, py, W, h, px + W, py + h);
        g.pose().pushPose();
        g.pose().translate(px, py, 0.0);
        try {
            boolean flat = !UiTheme.anime();
            float now = UiTheme.now();
            AkashicTheme.panel(g, 0, 0, W, h, now, flat, PAD + ROW - 1);
            if (!flat) AkashicTheme.motes(g, 2, 2, W - 4, h - 4, now, ShishouGuiLayout.MOTES_HUD);
            int y = PAD;
            AkashicTheme.emblem(g, PAD, y - 3, 12);
            String level = Component.translatableWithFallback("shishou.hud.level", "Lv %s", s == null ? 1 : s.level()).getString();
            int lw = font.width(level);
            g.drawString(font, Component.translatableWithFallback("item.nusmp.shishou", "Shishou").getString(), PAD + 15, y, 0xFF000000 | ShishouGuiLayout.CYAN_HI, true);
            g.drawString(font, level, W - PAD - lw, y, 0xFF000000 | ShishouGuiLayout.GOLD, true);
            y += ROW + 3;
            AkashicTheme.gauge(g, PAD, y, W - 2 * PAD, BAR, s == null ? 0f : s.expShare() / 100f, ShishouGuiLayout.EXP_LOW, ShishouGuiLayout.EXP_HIGH, now, flat);
            y += BAR + 2;
            boolean low = s != null && (s.flags() & ShishouNet.Sync.F_LOW) != 0;
            AkashicTheme.gauge(g, PAD, y, W - 2 * PAD, BAR, s == null || s.maxMana() <= 0 ? 0f : s.mana() / (float) s.maxMana(), low ? 0x8A2A2A : ShishouGuiLayout.MANA_LOW,
                    low ? ShishouGuiLayout.LOW : ShishouGuiLayout.MANA_HIGH, now, flat);
            y += BAR + 4;
            String mana = Component.translatableWithFallback("shishou.hud.mana", "Mana %s / %s", s == null ? 0 : s.mana(), s == null ? 0 : s.maxMana()).getString();
            g.drawString(font, mana, PAD + 1, y - 1, UiTheme.TEXT_DIM, false);
            y += ROW - 2;
            if (mode != null) {                                                                   // the mode of the Tensura skill the selected ability belongs to
                g.drawString(font, Component.translatableWithFallback(mode.langKey(), mode.english()).getString(), PAD + 1, y - 1, 0xFF000000 | ShishouGuiLayout.VIOLET, false);
                y += ROW - 2;
            }
            String name = sel == null ? Component.translatableWithFallback("shishou.hud.no_skill", "No skill to cast yet").getString()
                    : Component.translatableWithFallback(sel.langKey(), sel.english()).getString() + " (" + Component.translatableWithFallback(sel.langKey() + ".jp", sel.romaji()).getString() + ")";
            boolean cooling = s != null && s.cooldownShare() > 0;
            g.drawString(font, font.plainSubstrByWidth(name, W - 2 * PAD - (cooling ? 30 : 0)), PAD + 1, y, cooling ? UiTheme.TEXT_DIM : UiTheme.TEXT, false);
            if (cooling) g.drawString(font, s.cooldownShare() + "%", W - PAD - 26, y, UiTheme.TEXT_FAINT, false);
            y += ROW;
            if (flagsOf(s) > 0) {
                g.drawString(font, flagText(s), PAD + 1, y, UiTheme.GREEN, false);
                y += ROW;
            }
            if (!lines.isEmpty()) {
                AkashicTheme.prismLine(g, PAD, y + 1, W - 2 * PAD, now);
                y += 4;
                for (int i = 0; i < lines.size(); i++) {
                    g.drawString(font, font.plainSubstrByWidth(lines.get(i), W - 2 * PAD), PAD + 1, y, i == 0 ? (r.concealed() ? UiTheme.TEXT_DIM : UiTheme.GOLD) : UiTheme.TEXT, i == 0);
                    y += ROW;
                }
            }
        } finally {
            g.pose().popPose();
            GuiMove.end(g, moved);
        }
    }

    private static int flagsOf(ShishouNet.Sync s) { return s == null ? 0 : Integer.bitCount(s.flags() & 15); }

    private static String flagText(ShishouNet.Sync s) {
        StringBuilder b = new StringBuilder();
        if ((s.flags() & ShishouNet.Sync.F_BARRIER) != 0) b.append(Component.translatableWithFallback("shishou.hud.barrier", "Barrier").getString()).append("  ");
        if ((s.flags() & ShishouNet.Sync.F_SENSE) != 0) b.append(Component.translatableWithFallback("shishou.hud.sense", "Sense").getString()).append("  ");
        if ((s.flags() & ShishouNet.Sync.F_STEALTH) != 0) b.append(Component.translatableWithFallback("shishou.hud.stealth", "Veil").getString()).append("  ");
        if ((s.flags() & ShishouNet.Sync.F_RELEASED) != 0) b.append(Component.translatableWithFallback("shishou.hud.released", "Released").getString());
        return b.toString().trim();
    }

    /** The rows of the Appraisal readout of a target. */
    static void readoutLines(Font font, ShishouNet.AppraisalReply r, List<String> out) {
        String name = Component.translatableWithFallback(r.nameKey(), r.nameKey()).getString();
        if (r.kind() == 1) {
            out.add(name + " x" + (int) r.health());
            out.add(Component.translatableWithFallback("shishou.hud.item_value", "Worth %s experience to eat", r.manaish()).getString());
            return;
        }
        out.add(name);
        out.add(Component.translatableWithFallback("shishou.hud.health", "Health %s / %s", (int) Math.ceil(r.health()), (int) Math.ceil(r.maxHealth())).getString());
        out.add(Component.translatableWithFallback("shishou.hud.estimate", "Level ~%s   Mana ~%s", r.levelish(), r.manaish()).getString());
        if (r.weakMask() != 0) {
            StringBuilder w = new StringBuilder(Component.translatableWithFallback("shishou.hud.weak", "Weak to:").getString());
            for (int bit = 1; bit <= ShishouAppraisal.W_HOLY; bit <<= 1)
                if ((r.weakMask() & bit) != 0) w.append(' ').append(Component.translatableWithFallback(ShishouAppraisal.weaknessKey(bit), "?").getString());
            out.add(w.toString());
        }
        if (!r.skillKey().isEmpty()) out.add(Component.translatableWithFallback("shishou.hud.gives", "Skill: %s", Component.translatableWithFallback(r.skillKey(), "?")).getString());
    }
}
