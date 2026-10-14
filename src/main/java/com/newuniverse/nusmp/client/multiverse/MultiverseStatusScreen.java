package com.newuniverse.nusmp.client.multiverse;

import com.newuniverse.nusmp.client.ui.theme.MagicCircle;
import com.newuniverse.nusmp.client.ui.theme.SageBlock;
import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiBar;
import com.newuniverse.nusmp.client.ui.theme.UiLang;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Full Multiverse status screen, in the look of the Tensura menus (dark panels, thin borders, gold headings). Read-only: every
 * value comes from the server's last status message (rank, stars, squad, social class, race, grimoire, spirit lord, anti-magic,
 * ceremony). {@link #drawCompact} draws the short version inside the Tensura menu's former "Coming Soon" area.
 * <p>
 * 1.27.0: the anime look (config ui.theme, default) draws the same blocks as translucent analysis panels with glowing edges, an animated bar
 * under the grimoire, spirit and corruption blocks, a magic circle behind them and a typed Great-Sage readout; the classic look is the old drawing.
 */
public class MultiverseStatusScreen extends Screen {
    static final int BG = 0xE0101016, BORDER = 0xFF3B3B4C, GOLD = 0xFFE8C468, TEXT = 0xFFE6E6EE, DIM = 0xFF9A9AAE, RED = 0xFFE05050;

    private final Screen parent;

    public MultiverseStatusScreen(Screen parent) {
        super(Component.literal("Multiverse Status"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(new ThemedButton(width / 2 - 40, height - 26, 80, 18, Component.literal("Back"), b -> onClose()));
        addRenderableWidget(new ThemedButton(width - 96, height - 26, 88, 18, Component.literal("Grimoire Slot"), b -> com.newuniverse.nusmp.client.grimoire.GrimoireSlotClient.open()));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.renderBackground(g, mouseX, mouseY, partial);
        if (UiTheme.anime()) MagicCircle.draw(g, width / 2f, height / 2f, Math.min(width, height) * 0.5f, UiTheme.now(), UiTheme.VIOLET, 0.20f, MagicCircle.CALM, 1f);
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }

    record Block(String title, List<String> lines, boolean warn) {}

    /** 0.100: the status blocks for the Codex ({@code client.ui}): each entry is {title, lines joined by '\n', "warn" or ""}. */
    public static List<String[]> summary(CompoundTag s) {
        List<String[]> out = new ArrayList<>();
        for (Block b : blocks(s)) out.add(new String[]{b.title(), String.join("\n", b.lines()), b.warn() ? "warn" : ""});
        return out;
    }

    static List<Block> blocks(CompoundTag s) {
        List<Block> out = new ArrayList<>();
        out.add(new Block("Magic Knight Rank", List.of(s.getString("Rank").isEmpty() ? "5th Class Junior Magic Knight" : s.getString("Rank")), false));
        out.add(new Block("Stars", List.of("Gold " + s.getInt("Gold") + "   Black " + s.getInt("Black"), "Net " + s.getInt("Net")), false));
        CompoundTag sq = s.getCompound("Squad");
        if (sq.contains("Name")) out.add(new Block("Squad", List.of(sq.getString("Name"), sq.getString("Role") + "  " + sq.getString("Standing"), "Score " + sq.getInt("Score")), false));
        else out.add(new Block("Squad", List.of(sq.getString("State").isEmpty() ? "No squad" : sq.getString("State")), sq.getString("State").startsWith("Requires")));
        out.add(new Block("Social Class", List.of(s.getString("Social").isEmpty() ? "Commoner" : s.getString("Social")), false));
        out.add(new Block("Race", List.of(s.getString("Race").isEmpty() ? "Human" : s.getString("Race")), false));
        CompoundTag g = s.getCompound("Grimoire");
        if (g.contains("Cover")) {
            List<String> l = new ArrayList<>();
            l.add(g.getString("Cover"));
            l.add(g.getString("Magic"));
            if (g.contains("Pages")) l.add("Pages " + g.getInt("Unlocked") + "/" + g.getInt("Pages") + "  slots " + g.getInt("Slots") + "  mastery " + g.getInt("Mastery") + "%");
            out.add(new Block("Grimoire", l, false));
        } else out.add(new Block("Grimoire", List.of("None yet"), false));
        CompoundTag mn = s.getCompound("Mana");                                                  // 1.5.0
        if (mn.contains("Stage")) {
            List<String> ml = new ArrayList<>();
            ml.add(mn.getString("Stage") + "  |  " + mn.getString("Affinity"));
            ml.add("Spells cost " + mn.getInt("Cost") + "%, hit for " + mn.getInt("Power") + "%");
            ml.add("Mana density " + mn.getString("Density") + (mn.getBoolean("Grand") ? " (Grand Magic Zone)" : "")
                    + (mn.getBoolean("Zone") ? "  |  Mana Zone ON" : "") + (mn.getBoolean("Negative") ? "  |  Negative mana" : ""));
            if (mn.contains("SocialName")) { ml.add("Social class: " + mn.getString("SocialName")); ml.add("  + " + mn.getString("SocialBuff")); ml.add("  - " + mn.getString("SocialDebuff")); }
            if (mn.contains("Signature") && !mn.getString("Signature").isEmpty()) ml.add("Signature: " + mn.getString("Signature"));   // 1.16.0
            if (mn.contains("Kingdom")) { ml.add("Hails from the " + mn.getString("Kingdom")); ml.add("  + " + mn.getString("KingdomBuff")); ml.add("  - " + mn.getString("KingdomDebuff")); }
            if (mn.contains("Blood")) { ml.add("Blood of the " + mn.getString("Blood") + " mixed into yours"); ml.add("  + " + mn.getString("BloodBuff")); ml.add("  - " + mn.getString("BloodDebuff")); }
            if (mn.getInt("Corruption") > 0) ml.add("Corruption " + mn.getInt("Corruption") + "/100");
            if (!mn.getString("Offer").isEmpty()) ml.add("A grimoire waits: choose your magic (see chat, /multiverse choose)");
            out.add(new Block("Mana", ml, mn.getInt("Corruption") >= 70));
        }
        CompoundTag sp = s.getCompound("Spirit");
        if (sp.contains("Type")) out.add(new Block("Spirit Lord", List.of(sp.getString("Type") + "  trust " + sp.getInt("Trust") + "/100",
                "Energy " + sp.getInt("Energy") + "%", sp.getBoolean("Incarnated") ? "Incarnated: " + sp.getString("Name") : "Not incarnated"), false));
        else out.add(new Block("Spirit Lord", List.of("No spirit bond"), false));
        CompoundTag a = s.getCompound("Anti");
        if (a.contains("Mode")) out.add(new Block("Anti-Magic", List.of(a.getString("Mode"), "AMP " + a.getInt("AMP")), false));
        else out.add(new Block("Anti-Magic", List.of("-"), false));
        CompoundTag c = s.getCompound("Ceremony");
        List<String> cl = new ArrayList<>();
        cl.add(c.getBoolean("Accepted") ? "Accepted" + (c.getString("Where").isEmpty() ? "" : " at " + c.getString("Where")) : "Not accepted");
        if (!c.getBoolean("Accepted")) cl.add(c.getBoolean("Eligible") ? "Eligible: go to a Grimoire Tower" : "Not eligible");
        cl.add(c.getBoolean("Active") ? "Ceremony in progress" : "Ceremony closed");
        out.add(new Block("Ceremony", cl, false));
        return out;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        if (UiTheme.anime()) { renderAnime(g); return; }
        g.drawCenteredString(font, "MULTIVERSE STATUS", width / 2, 10, GOLD);
        List<Block> blocks = blocks(MultiverseStatusClient.status());
        int cols = 3, gap = 6;
        int pw = Math.min(160, (width - 20 - gap * (cols - 1)) / cols), ph = 52;
        int x0 = (width - (pw * cols + gap * (cols - 1))) / 2, y0 = 26;
        for (int i = 0; i < blocks.size(); i++) {
            int x = x0 + (i % cols) * (pw + gap), y = y0 + (i / cols) * (ph + gap);
            panel(g, font, x, y, pw, ph, blocks.get(i));
        }
    }

    private final SageBlock sage = new SageBlock(), sageLine = new SageBlock();
    private static final java.util.Map<String, UiBar> BARS = new java.util.HashMap<>();

    /** The anime look of the whole screen: title, the block panels, the Great-Sage readout. */
    private void renderAnime(GuiGraphics g) {
        float now = UiTheme.now();
        g.drawCenteredString(font, "MULTIVERSE STATUS", width / 2, 10, UiTheme.GOLD);
        g.fill(width / 2 - 52, 21, width / 2 + 52, 22, UiMath.withAlpha(UiTheme.CYAN, 0.3f));
        g.fill(width / 2 - 14, 21, width / 2 + 14, 22, UiMath.withAlpha(UiTheme.CYAN, 0.95f));
        List<Block> blocks = blocks(MultiverseStatusClient.status());
        int cols = 3, gap = 6;
        int pw = Math.min(160, (width - 20 - gap * (cols - 1)) / cols), ph = 56;
        int x0 = (width - (pw * cols + gap * (cols - 1))) / 2, y0 = 26;
        boolean warn = false;
        for (int i = 0; i < blocks.size(); i++) {
            int x = x0 + (i % cols) * (pw + gap), y = y0 + (i / cols) * (ph + gap);
            panelAnime(g, font, x, y, pw, ph, blocks.get(i), now);
            warn |= blocks.get(i).warn();
        }
        CompoundTag s = MultiverseStatusClient.status();
        String rank = s.getString("Rank").isEmpty() ? "5th Class Junior Magic Knight" : s.getString("Rank");
        int sw = Math.max(120, width / 2 - 56);
        int bottom = y0 + ((blocks.size() + cols - 1) / cols) * (ph + gap);                  // where the last row of panels ends
        String body = warn ? UiLang.get(UiLang.WARNING, UiLang.get("nusmp.ui.status.warn")) : UiLang.get("nusmp.ui.status.read");
        sage.set(font, UiLang.get(UiLang.ANALYSIS_COMPLETE), body, UiLang.get(UiLang.RESULT, rank), sw, now);
        int sy = Math.max(bottom + 2, height - 38);
        if (sy + sage.height(font) <= height - 3) {
            sage.draw(g, font, 10, sy, now, 1f);
        } else {                                                                             // a small window: the panels fill it, so one line in the top left corner
            int tw = Math.max(60, width / 2 - 80);
            sageLine.set(font, "", font.plainSubstrByWidth(UiLang.get(UiLang.ANALYSIS_COMPLETE) + " " + UiLang.get(UiLang.RESULT, rank), tw), "", tw, now);
            sageLine.draw(g, font, 10, 10, now, 1f);
        }
    }

    /** One status block as an analysis panel: glowing edge, gold heading, light lines, and a bar for grimoire mastery, spirit trust and corruption. */
    static void panelAnime(GuiGraphics g, Font font, int x, int y, int w, int h, Block b, float now) {
        UiPanel.panel(g, x, y, w, h, b.warn() ? UiTheme.RED : UiTheme.CYAN, b.warn() ? 0.9f : 0.5f, 1f);
        UiPanel.heading(g, font, b.title(), x + 6, y + 4, w - 12, b.warn() ? UiTheme.RED : UiTheme.GOLD);
        int ly = y + 18;
        for (String line : b.lines()) {
            if (ly + 9 > y + h - 6) break;
            g.drawString(font, font.plainSubstrByWidth(line, w - 12), x + 6, ly, b.warn() ? UiTheme.RED : UiTheme.TEXT, false);
            ly += 10;
        }
        CompoundTag s = MultiverseStatusClient.status();
        float frac = -1f;
        int rgb = UiTheme.VIOLET;
        switch (b.title()) {
            case "Grimoire" -> { if (s.getCompound("Grimoire").contains("Mastery")) frac = s.getCompound("Grimoire").getInt("Mastery") / 100f; }
            case "Spirit Lord" -> { if (s.getCompound("Spirit").contains("Trust")) { frac = s.getCompound("Spirit").getInt("Trust") / 100f; rgb = UiTheme.GREEN; } }
            case "Mana" -> { int c = s.getCompound("Mana").getInt("Corruption"); if (c > 0) { frac = c / 100f; rgb = UiTheme.RED; } }
            default -> { }
        }
        if (frac >= 0f) {
            UiBar bar = BARS.computeIfAbsent(b.title(), k -> new UiBar());
            bar.update(frac, now, true);
            bar.draw(g, x + 6, y + h - 7, w - 12, 3, rgb, now, 1f);
        }
    }

    static void panel(GuiGraphics g, Font font, int x, int y, int w, int h, Block b) {
        g.fill(x, y, x + w, y + h, BG);
        g.renderOutline(x, y, w, h, BORDER);
        g.drawString(font, b.title(), x + 5, y + 4, GOLD, false);
        g.hLine(x + 4, x + w - 5, y + 14, BORDER);
        int ly = y + 18;
        for (String line : b.lines()) {
            if (ly + 9 > y + h) break;
            g.drawString(font, font.plainSubstrByWidth(line, w - 10), x + 5, ly, b.warn() ? RED : TEXT, false);
            ly += 10;
        }
    }

    /** The short panel inside the Tensura menu: the most important lines, scaled to fit the area. Click opens the full screen. */
    public static void drawCompact(GuiGraphics g, Font font, int x, int y, int w, int h, CompoundTag s, boolean hover) {
        boolean anime = UiTheme.anime();
        if (anime) {
            float now = UiTheme.now();
            UiPanel.panel(g, x, y, w, h, hover ? UiTheme.GOLD : UiTheme.CYAN, hover ? 1f : 0.4f, 1f);
            g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
            MagicCircle.draw(g, x + w - h * 0.42f, y + h / 2f, h * 0.55f, now, hover ? UiTheme.GOLD : UiTheme.CYAN, hover ? 0.55f : 0.28f, MagicCircle.STANDARD, 1f);
            g.disableScissor();
        } else {
        g.fill(x, y, x + w, y + h, BG);
        g.renderOutline(x, y, w, h, hover ? GOLD : BORDER);
        }
        List<Block> blocks = blocks(s);
        List<String[]> rows = new ArrayList<>();
        for (Block b : blocks) {
            if (b.title().equals("Social Class") || b.title().equals("Anti-Magic") && b.lines().get(0).equals("-")) continue;
            rows.add(new String[]{b.title(), b.lines().get(0)});
        }
        int lineH = 10, needed = 14 + rows.size() * lineH;
        float scale = Math.min(1f, (h - 4) / (float) needed);
        g.pose().pushPose();
        g.pose().translate(x + 4, y + 3, 0);
        g.pose().scale(scale, scale, 1f);
        int inner = (int) ((w - 8) / scale);
        g.drawString(font, "MULTIVERSE", 0, 0, anime ? UiTheme.GOLD : GOLD, false);
        String more = hover ? "click for more" : "";
        g.drawString(font, more, inner - font.width(more), 0, anime ? UiTheme.TEXT_DIM : DIM, false);
        int ly = 14;
        int labelW = 0;
        for (String[] r : rows) labelW = Math.max(labelW, font.width(r[0]));
        labelW = Math.min(labelW, inner / 2);
        for (String[] r : rows) {
            g.drawString(font, font.plainSubstrByWidth(r[0], labelW), 0, ly, anime ? UiTheme.TEXT_DIM : DIM, false);
            g.drawString(font, font.plainSubstrByWidth(r[1], inner - labelW - 6), labelW + 6, ly, anime ? UiTheme.TEXT : TEXT, false);
            ly += lineH;
        }
        g.pose().popPose();
    }
}
