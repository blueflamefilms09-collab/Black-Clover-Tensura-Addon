package com.newuniverse.nusmp.client.elite.forge;

import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.elite.forge.net.ForgeNet;
import com.newuniverse.nusmp.elite.forge.rules.Category;
import com.newuniverse.nusmp.elite.forge.rules.Quality;
import com.newuniverse.nusmp.elite.forge.rules.QualityRoller;
import com.newuniverse.nusmp.elite.forge.rules.Specialization;
import com.newuniverse.nusmp.elite.forge.rules.StationTier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the screen of a forge station. It lists the recipes the server sent (greyed when the station is too low a tier, green when the player has every material),
 * shows the chosen one in detail (materials with what the player has, optional premium materials and the quality points they add, the minigame sequence, and the
 * quality a perfect and an ordinary play would reach at this station, worked out with the same pure {@code QualityRoller} the server rolls with), and has the buttons
 * of the system: Forge (starts a session), the four specializations (with their costs), and Engrave (put one more Tensura engraving on the forged item in the main
 * hand). Every number it shows is the server's (the "have" counts, the progress); it sends only requests. The panel is drawn in {@code renderBackground} (the
 * screen-blur guard test requires it), everything else in {@code render}.
 */
public class ForgeStationScreen extends Screen {
    private ForgeNet.OpenStation data;
    private String selected = "";
    private int scroll;
    private String filter = "";
    private boolean usePremium = true;
    private Component status = Component.empty();
    private int statusColor = UiTheme.TEXT_DIM;

    private int pw, ph, px, py;

    public ForgeStationScreen(ForgeNet.OpenStation data) {
        super(Component.translatable("nusmp.eforge.station.title"));
        this.data = data;
    }

    // ================================================================ data
    /** A refreshed station: keeps the selection and the scroll. */
    void update(ForgeNet.OpenStation d) {
        this.data = d;
        rebuildWidgets();
    }

    void status(Component text, int color) {
        this.status = text;
        this.statusColor = color;
    }

    private StationTier tier() {
        StationTier t = StationTier.byIndex(data.tier());
        return t == null ? StationTier.BASIC : t;
    }

    private List<ForgeNet.RecipeView> visible() {
        List<ForgeNet.RecipeView> out = new ArrayList<>();
        for (ForgeNet.RecipeView r : data.recipes()) if (filter.isEmpty() || filter.equals(r.category())) out.add(r);
        return out;
    }

    private ForgeNet.RecipeView current() {
        for (ForgeNet.RecipeView r : data.recipes()) if (r.id().equals(selected)) return r;
        List<ForgeNet.RecipeView> v = visible();
        return v.isEmpty() ? null : v.get(0);
    }

    private static boolean craftable(ForgeNet.RecipeView r) {
        if (!r.unlocked() || !r.available()) return false;
        for (ForgeNet.StackView s : r.inputs()) if (s.have() < s.count()) return false;
        return true;
    }

    private static ItemStack stack(String id, int count) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(rl), Math.max(1, count));
    }

    // ================================================================ layout and widgets
    private void layout() {
        pw = Math.min(width - 12, 470);
        ph = Math.min(height - 12, 276);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
    }

    private int listX() { return px + 8; }

    private int listY() { return py + 42; }

    private int listW() { return 156; }

    private int listH() { return ph - 42 - 62; }

    private int detX() { return px + 172; }

    private int detY() { return py + 28; }

    private int detW() { return pw - 172 - 8; }

    private int detH() { return ph - 28 - 62; }

    private static final int ROW = 18;

    @Override
    protected void init() {
        layout();
        ForgeNet.RecipeView r = current();
        int by = py + ph - 56;
        // forge and premium
        ThemedButton forge = new ThemedButton(detX(), detY() + detH() - 16, detW(), 16, Component.translatable("nusmp.eforge.station.forge"), b -> {
            ForgeNet.RecipeView c = current();
            if (c != null) PacketDistributor.sendToServer(new ForgeNet.Begin(data.pos(), c.id(), usePremium));
        });
        forge.active = r != null && craftable(r);
        if (r != null && !r.unlocked()) forge.setTooltip(Tooltip.create(Component.translatable("nusmp.eforge.station.tier_needed", Component.translatable("nusmp.eforge.tier." + StationTier.byIndex(r.tier()).id()))));
        else if (r != null && !forge.active) forge.setTooltip(Tooltip.create(Component.translatable("nusmp.eforge.station.missing")));
        addRenderableWidget(forge);
        if (r != null && !r.premium().isEmpty()) {
            addRenderableWidget(new ThemedButton(detX(), detY() + detH() - 34, detW(), 14,
                    Component.translatable(usePremium ? "nusmp.eforge.station.premium_on" : "nusmp.eforge.station.premium_off"), b -> {
                        usePremium = !usePremium;
                        rebuildWidgets();
                    }));
        }
        // specializations and engrave
        int engW = Math.min(120, pw / 4);
        int specW = (pw - 16 - engW - 4 * 4) / 4;
        int sx = px + 8;
        String cur = data.progress().spec();
        for (Specialization s : Specialization.values()) {
            ThemedButton b = new ThemedButton(sx, by + 14, specW, 16, Component.translatable("nusmp.eforge.spec." + s.id()), bb -> PacketDistributor.sendToServer(new ForgeNet.Spec(data.pos(), s.id())));
            b.active = !s.id().equals(cur);
            Component cost = data.progress().changeCosts() ? Component.translatable("nusmp.eforge.spec.cost_tip", data.progress().changeLevels()) : Component.translatable("nusmp.eforge.spec.free_tip");
            b.setTooltip(Tooltip.create(Component.translatable("nusmp.eforge.spec." + s.id() + ".desc").append("\n").append(cost)));
            addRenderableWidget(b);
            sx += specW + 4;
        }
        ThemedButton eng = new ThemedButton(sx, by + 14, engW, 16, Component.translatable("nusmp.eforge.station.engrave"), b -> PacketDistributor.sendToServer(new ForgeNet.Engrave(data.pos())));
        eng.setTooltip(Tooltip.create(Component.translatable("nusmp.eforge.station.engrave_tip", data.progress().engraveLevels())));
        addRenderableWidget(eng);
    }

    // ================================================================ input
    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0) {
            // category chips
            int cx = listX();
            String[] cats = {"", "weapon", "armor", "tool", "rune"};
            for (String c : cats) {
                int w = chipW(c);
                if (x >= cx && x < cx + w && y >= py + 26 && y < py + 38) {
                    filter = c;
                    scroll = 0;
                    rebuildWidgets();
                    return true;
                }
                cx += w + 2;
            }
            // recipe rows
            if (x >= listX() && x < listX() + listW() && y >= listY() && y < listY() + listH()) {
                int idx = scroll + (int) ((y - listY()) / ROW);
                List<ForgeNet.RecipeView> v = visible();
                if (idx >= 0 && idx < v.size()) {
                    selected = v.get(idx).id();
                    rebuildWidgets();
                    return true;
                }
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        int rows = listH() / ROW;
        int max = Math.max(0, visible().size() - rows);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(dy)));
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }

    // ================================================================ drawing
    private int chipW(String c) { return 8 + font.width(Component.translatable(c.isEmpty() ? "nusmp.eforge.station.all" : "nusmp.eforge.cat." + c)); }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.renderBackground(g, mouseX, mouseY, partial);
        layout();
        UiPanel.panelScan(g, px, py, pw, ph, UiTheme.GOLD, 0.8f, 1f, UiTheme.now());
        UiPanel.well(g, listX() - 1, listY() - 1, listW() + 2, listH() + 2, UiTheme.CYAN, 0.9f);
        UiPanel.well(g, detX() - 2, detY() - 2, detW() + 4, detH() + 4, UiTheme.CYAN, 0.7f);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        ForgeNet.Progress pr = data.progress();
        StationTier t = tier();
        g.drawString(font, Component.translatable("nusmp.eforge.station.head", Component.translatable("nusmp.eforge.tier." + t.id())), px + 8, py + 8, UiTheme.GOLD, false);
        Quality best = Quality.ofLevel(pr.best());
        Component line = Component.translatable("nusmp.eforge.station.stats", pr.crafted(), best == null ? Component.translatable("nusmp.eforge.station.none") : Component.translatable("nusmp.eforge.quality." + best.id()),
                data.forgedHere(), pr.skillPoints());
        g.drawString(font, font.plainSubstrByWidth(line.getString(), pw - 190), px + 182, py + 8, UiTheme.TEXT_DIM, false);
        renderChips(g, mouseX, mouseY);
        renderList(g, mouseX, mouseY);
        renderDetail(g);
        int by = py + ph - 56;
        Specialization sp = Specialization.byId(pr.spec());
        g.drawString(font, Component.translatable("nusmp.eforge.station.spec_line", sp == null ? Component.translatable("nusmp.eforge.station.none") : Component.translatable("nusmp.eforge.spec." + sp.id())), px + 8, by + 2, UiTheme.TEXT, false);
        if (!status.getString().isEmpty()) g.drawString(font, font.plainSubstrByWidth(status.getString(), pw - 16), px + 8, by + 38, statusColor, false);
        else g.drawString(font, Component.translatable("nusmp.eforge.station.station_line", Component.translatable("nusmp.eforge.tier." + t.id()), t.qualityBonus(), Component.translatable("nusmp.eforge.quality." + t.ceiling().id())), px + 8, by + 38, UiTheme.TEXT_FAINT, false);
        // hover tooltip of the rows' items
        ForgeNet.RecipeView c = current();
        if (c != null) hoverItems(g, c, mouseX, mouseY);
    }

    private void renderChips(GuiGraphics g, int mouseX, int mouseY) {
        int cx = listX();
        for (String c : new String[]{"", "weapon", "armor", "tool", "rune"}) {
            int w = chipW(c);
            boolean hov = mouseX >= cx && mouseX < cx + w && mouseY >= py + 26 && mouseY < py + 38;
            UiPanel.chip(g, cx, py + 26, w, 12, filter.equals(c), hov, UiTheme.CYAN);
            g.drawString(font, Component.translatable(c.isEmpty() ? "nusmp.eforge.station.all" : "nusmp.eforge.cat." + c), cx + 4, py + 28, UiTheme.TEXT, false);
            cx += w + 2;
        }
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY) {
        List<ForgeNet.RecipeView> v = visible();
        int rows = listH() / ROW;
        ForgeNet.RecipeView cur = current();
        g.enableScissor(listX(), listY(), listX() + listW(), listY() + listH());
        for (int i = 0; i < rows && scroll + i < v.size(); i++) {
            ForgeNet.RecipeView r = v.get(scroll + i);
            int y = listY() + i * ROW;
            boolean sel = cur != null && r.id().equals(cur.id());
            boolean hov = mouseX >= listX() && mouseX < listX() + listW() && mouseY >= y && mouseY < y + ROW;
            if (sel) g.fill(listX(), y, listX() + listW(), y + ROW, ForgeDraw.alpha(ForgeDraw.CYAN, 0.18f));
            else UiPanel.hover(g, listX(), y, listW(), ROW, hov ? 1f : 0f, UiTheme.CYAN);
            ItemStack out = stack(r.outItem(), r.outCount());
            g.renderItem(out, listX() + 2, y + 1);
            int col = !r.available() ? UiTheme.TEXT_FAINT : !r.unlocked() ? UiTheme.RED : craftable(r) ? UiTheme.GREEN : UiTheme.TEXT;
            g.drawString(font, font.plainSubstrByWidth(out.getHoverName().getString(), listW() - 40), listX() + 22, y + 5, col, false);
            g.drawString(font, roman(r.tier() + 1), listX() + listW() - 12, y + 5, UiTheme.TEXT_FAINT, false);
            if (r.known()) g.drawString(font, "*", listX() + listW() - 20, y + 5, UiTheme.GOLD, false);
        }
        g.disableScissor();
        if (v.isEmpty()) g.drawCenteredString(font, Component.translatable("nusmp.eforge.station.no_recipes"), listX() + listW() / 2, listY() + 10, UiTheme.TEXT_DIM);
        int max = Math.max(0, v.size() - rows);
        if (max > 0) {
            int bh = Math.max(10, listH() * rows / v.size());
            int by = listY() + (listH() - bh) * scroll / max;
            g.fill(listX() + listW() - 2, by, listX() + listW(), by + bh, ForgeDraw.alpha(ForgeDraw.CYAN, 0.6f));
        }
    }

    private static String roman(int n) { return n == 1 ? "I" : n == 2 ? "II" : n == 3 ? "III" : "IV"; }

    private void renderDetail(GuiGraphics g) {
        ForgeNet.RecipeView r = current();
        int x = detX(), y = detY(), w = detW();
        if (r == null) {
            g.drawCenteredString(font, Component.translatable("nusmp.eforge.station.pick"), x + w / 2, y + 30, UiTheme.TEXT_DIM);
            return;
        }
        g.enableScissor(x - 1, y - 1, x + w + 1, y + detH() - 36);
        ItemStack out = stack(r.outItem(), r.outCount());
        g.renderItem(out, x + 2, y + 2);
        g.drawString(font, Component.translatable("nusmp.eforge.station.out", out.getHoverName(), r.outCount()), x + 24, y + 2, UiTheme.GOLD, false);
        StationTier need = StationTier.byIndex(r.tier());
        g.drawString(font, Component.translatable("nusmp.eforge.station.needs", Component.translatable("nusmp.eforge.cat." + r.category()), Component.translatable("nusmp.eforge.tier." + need.id())), x + 24, y + 13,
                r.unlocked() ? UiTheme.TEXT_DIM : UiTheme.RED, false);
        int ly = y + 28;
        g.drawString(font, Component.translatable("nusmp.eforge.station.materials"), x + 2, ly, UiTheme.CYAN, false);
        ly += 11;
        for (ForgeNet.StackView s : r.inputs()) {
            ItemStack st = stack(s.item(), s.count());
            g.pose().pushPose();
            g.pose().translate(x + 2, ly - 1, 0);
            g.pose().scale(0.75f, 0.75f, 1f);
            g.renderItem(st, 0, 0);
            g.pose().popPose();
            boolean ok = s.have() >= s.count();
            g.drawString(font, font.plainSubstrByWidth(st.getHoverName().getString(), w - 62), x + 17, ly + 1, ok ? UiTheme.TEXT : UiTheme.RED, false);
            String cnt = Math.min(s.have(), 999) + "/" + s.count();
            g.drawString(font, cnt, x + w - font.width(cnt) - 3, ly + 1, ok ? UiTheme.GREEN : UiTheme.RED, false);
            ly += 12;
        }
        int matPoints = 0;
        if (!r.premium().isEmpty()) {
            ly += 2;
            g.drawString(font, Component.translatable("nusmp.eforge.station.premium"), x + 2, ly, ForgeDraw.VIOLET, false);
            ly += 10;
            for (int i = 0; i < r.premium().size(); i++) {
                ForgeNet.StackView s = r.premium().get(i);
                int bonus = i < r.premiumBonus().size() ? r.premiumBonus().get(i) : 0;
                boolean have = s.have() >= s.count();
                if (usePremium && have) matPoints += bonus;
                String line = stack(s.item(), s.count()).getHoverName().getString() + " x" + s.count() + "  +" + bonus;
                g.drawString(font, font.plainSubstrByWidth(line, w - 6), x + 4, ly, have && usePremium ? UiTheme.GREEN : UiTheme.TEXT_FAINT, false);
                ly += 10;
            }
        }
        ly += 3;
        StringBuilder seq = new StringBuilder();
        for (ForgeNet.StepView s : r.steps()) {
            if (seq.length() > 0) seq.append("  >  ");
            seq.append(Component.translatable("nusmp.eforge.mg.type." + s.type()).getString()).append(' ').append(roman(s.difficulty()));
        }
        g.drawString(font, Component.translatable("nusmp.eforge.station.steps"), x + 2, ly, UiTheme.CYAN, false);
        ly += 10;
        g.drawString(font, font.plainSubstrByWidth(seq.toString(), w - 6), x + 4, ly, UiTheme.TEXT, false);
        ly += 12;
        Category cat = Category.byId(r.category());
        Specialization sp = Specialization.byId(data.progress().spec());
        int specPts = sp == null || cat == null ? 0 : sp.categoryPoints(cat);
        QualityRoller.Result perfect = QualityRoller.roll(new QualityRoller.Inputs(1.0, tier(), r.baseQuality(), matPoints, data.progress().skillPoints(), specPts, 0.0));
        QualityRoller.Result usual = QualityRoller.roll(new QualityRoller.Inputs(0.7, tier(), r.baseQuality(), matPoints, data.progress().skillPoints(), specPts, 0.0));
        g.drawString(font, Component.translatable("nusmp.eforge.station.est_perfect", Component.translatable("nusmp.eforge.quality." + perfect.quality().id()).withStyle(s -> s.withColor(perfect.quality().rgb())), Math.round(perfect.points())), x + 4, ly, UiTheme.TEXT, false);
        ly += 10;
        g.drawString(font, Component.translatable("nusmp.eforge.station.est_usual", Component.translatable("nusmp.eforge.quality." + usual.quality().id()).withStyle(s -> s.withColor(usual.quality().rgb())), Math.round(usual.points())), x + 4, ly, UiTheme.TEXT_DIM, false);
        g.disableScissor();
    }

    private void hoverItems(GuiGraphics g, ForgeNet.RecipeView r, int mouseX, int mouseY) {
        // the output icon in the list and the detail header show the item's own tooltip
        int x = detX() + 2, y = detY() + 2;
        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) g.renderTooltip(font, stack(r.outItem(), r.outCount()), mouseX, mouseY);
    }
}
