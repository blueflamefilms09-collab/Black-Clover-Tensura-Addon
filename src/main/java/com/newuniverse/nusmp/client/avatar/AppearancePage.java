package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import com.newuniverse.nusmp.avatar.appearance.DynamicAppearanceEngine;
import com.newuniverse.nusmp.avatar.menu.ForgeLayout.Rect;
import com.newuniverse.nusmp.avatar.net.RaceEntry;
import com.newuniverse.nusmp.client.avatar.ForgeControls.ControlList;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Why: the second category of the Soul Forge, "Race & Appearance". Four small tabs keep every control inside the module whatever the window size: the
 * Race list (with what the race means, and why a locked one is locked), the Look (skin tone and shade, hair, outfit), the Form (horns, wings, height, a
 * "surprise me") and the Aura (the colour of the aura and of the eyes, from a palette or by the red, green and blue sliders). Every change goes straight into
 * the Avatar standing in the middle of the screen. The look is clamped by {@code DynamicAppearanceEngine} here and again by the server; the race is only a
 * choice, the server decides whether the creator may use it.
 */
final class AppearancePage extends ForgePage {
    private enum Tab { RACE, LOOK, FORM, AURA }

    private static final int ROW_H = 13;

    private Tab tab = Tab.RACE;
    private final Rect[] tabRects = new Rect[4];
    private Rect content = Rect.EMPTY, list = Rect.EMPTY, details = Rect.EMPTY;
    private int raceScroll, raceFlash;
    private final ControlList lookList = new ControlList(), formList = new ControlList(), auraList = new ControlList();
    private final Random rng = new Random();

    AppearancePage(ForgeHost host) {
        super(host);
        for (int i = 0; i < 4; i++) tabRects[i] = Rect.EMPTY;
        buildLook();
        buildForm();
        buildAura();
    }

    @Override int accent() { return 0x7CF0B0; }

    @Override String title() { return ForgeText.s("nusmp.avatar.forge.page.appearance", "Race & Appearance"); }

    // ================================================================ the controls of the three setting tabs
    private static String lookKey(String what, int id) { return Component.translatable("nusmp.avatar." + what + "." + id).getString(); }

    private static String noneOr(String what, int id) { return id < 0 ? Component.translatable("nusmp.avatar.gui.look.none").getString() : lookKey(what, id); }

    private void setLook(AppearanceData d) {
        h.setLook(d);
        h.changed();
    }

    private void buildLook() {
        lookList.clear();
        lookList.add(new ForgeControls.Heading(() -> Component.translatable("nusmp.avatar.gui.look.skin").getString(), UiTheme.GREEN));
        lookList.add(new ForgeControls.Swatches(DynamicAppearanceEngine.SKIN_PALETTE, () -> h.look().skinTone(), c -> { setLook(h.look().withSkin(c)); h.pulse(c); }));
        lookList.add(new ForgeControls.Slider(0xE8C468, () -> shadeOf(h.look().skinTone()), v -> Component.translatable("nusmp.avatar.gui.look.shade").getString(),
                v -> setLook(h.look().withSkin(shaded(h.look().skinTone(), v)))));
        lookList.add(new ForgeControls.Spacer(2));
        lookList.add(new ForgeControls.Cycler(() -> Component.translatable("nusmp.avatar.gui.look.hair").getString(), () -> lookKey("hair", h.look().hairId()),
                d -> setLook(h.look().withHair(Math.floorMod(h.look().hairId() + d, AppearanceData.HAIR_COUNT)))));
        lookList.add(new ForgeControls.Cycler(() -> Component.translatable("nusmp.avatar.gui.look.outfit").getString(), () -> lookKey("outfit", h.look().outfitId()),
                d -> setLook(h.look().withOutfit(Math.floorMod(h.look().outfitId() + d, AppearanceData.OUTFIT_COUNT)))));
    }

    private void buildForm() {
        formList.clear();
        formList.add(new ForgeControls.Heading(() -> ForgeText.s("nusmp.avatar.forge.form", "Form"), UiTheme.GREEN));
        formList.add(new ForgeControls.Cycler(() -> Component.translatable("nusmp.avatar.gui.look.horn").getString(), () -> noneOr("horn", h.look().hornId()),
                d -> setLook(h.look().withHorn(Math.floorMod(h.look().hornId() + 1 + d, AppearanceData.HORN_COUNT + 1) - 1))));
        formList.add(new ForgeControls.Cycler(() -> Component.translatable("nusmp.avatar.gui.look.wing").getString(), () -> noneOr("wing", h.look().wingId()),
                d -> setLook(h.look().withWing(Math.floorMod(h.look().wingId() + 1 + d, AppearanceData.WING_COUNT + 1) - 1))));
        formList.add(new ForgeControls.Spacer(2));
        formList.add(new ForgeControls.Slider(UiTheme.GREEN & 0xFFFFFF, this::sizeFraction,
                v -> Component.translatable("nusmp.avatar.gui.look.scale", String.format(Locale.ROOT, "%.2f", sizeOf(v))).getString(),
                v -> setLook(h.look().withScale((float) sizeOf(v)))));
        formList.add(new ForgeControls.Spacer(4));
        formList.add(new ForgeControls.Button(() -> Component.translatable("nusmp.avatar.gui.look.random").getString(), 0x7CF0B0, () -> {
            RaceEntry re = h.raceEntry();
            float base = re == null ? 1.0f : re.scale();
            setLook(DynamicAppearanceEngine.defaultsFor(h.state().race(), base, rng.nextLong()));
            h.pulse(h.look().auraRgb());
        }));
    }

    private void buildAura() {
        auraList.clear();
        auraList.add(new ForgeControls.Heading(() -> Component.translatable("nusmp.avatar.gui.look.aura").getString(), UiTheme.VIOLET));
        auraList.add(new ForgeControls.Swatches(DynamicAppearanceEngine.AURA_PALETTE, () -> h.look().auraRgb(), c -> { setLook(h.look().withAura(c)); h.pulse(c); }));
        channels(auraList, true);
        auraList.add(new ForgeControls.Spacer(3));
        auraList.add(new ForgeControls.Heading(() -> Component.translatable("nusmp.avatar.gui.look.eyes").getString(), UiTheme.CYAN));
        auraList.add(new ForgeControls.Swatches(DynamicAppearanceEngine.EYE_PALETTE, () -> h.look().eyeRgb(), c -> setLook(h.look().withEye(c))));
        channels(auraList, false);
    }

    /** Red, green and blue sliders of the aura (or of the eyes). */
    private void channels(ControlList into, boolean aura) {
        int[] tint = {0xFF6A6A, 0x7CF0B0, 0x5CD8FF};
        String[] name = {"R", "G", "B"};
        int[] shift = {16, 8, 0};
        for (int i = 0; i < 3; i++) {
            final int k = i;
            into.add(new ForgeControls.Slider(tint[i], () -> ((rgb(aura) >> shift[k]) & 255) / 255.0, v -> name[k] + " " + (int) Math.round(v * 255),
                    v -> {
                        int cur = rgb(aura);
                        int next = (cur & ~(255 << shift[k])) | ((int) Math.round(v * 255) << shift[k]);
                        setLook(aura ? h.look().withAura(next) : h.look().withEye(next));
                    }));
        }
    }

    private int rgb(boolean aura) { return (aura ? h.look().auraRgb() : h.look().eyeRgb()) & 0xFFFFFF; }

    private float baseScale() {
        RaceEntry re = h.raceEntry();
        return re == null ? 1.0f : re.scale();
    }

    private double sizeFraction() {
        float lo = DynamicAppearanceEngine.minScale(baseScale()), hi = DynamicAppearanceEngine.maxScale(baseScale());
        return hi > lo ? (h.look().scale() - lo) / (hi - lo) : 0.5;
    }

    private double sizeOf(double v) {
        float lo = DynamicAppearanceEngine.minScale(baseScale()), hi = DynamicAppearanceEngine.maxScale(baseScale());
        return lo + v * (hi - lo);
    }

    private static double shadeOf(int rgb) {
        int mx = Math.max((rgb >> 16) & 255, Math.max((rgb >> 8) & 255, rgb & 255));
        return Math.min(1.0, mx / 255.0);
    }

    /** The same hue as {@code rgb} with its brightest channel set to {@code v} (0.25 .. 1.0 of full). */
    private static int shaded(int rgb, double v) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255, mx = Math.max(1, Math.max(r, Math.max(g, b)));
        double k = (0.25 + 0.75 * v) * 255.0 / mx;
        return (Math.min(255, (int) (r * k)) << 16) | (Math.min(255, (int) (g * k)) << 8) | Math.min(255, (int) (b * k));
    }

    // ================================================================ layout
    @Override
    void layout(Rect area) {
        super.layout(area);
        int tabW = (area.w() - 3 * 3) / 4;
        for (int i = 0; i < 4; i++) tabRects[i] = new Rect(area.x() + i * (tabW + 3), area.y(), tabW, 14);
        content = new Rect(area.x(), area.y() + 18, area.w(), Math.max(0, area.h() - 18));
        int listH = Math.max(3 * ROW_H, Math.min(content.h() - 40, 8 * ROW_H));
        listH = Math.min(listH, content.h());
        list = new Rect(content.x(), content.y(), content.w(), listH);
        details = new Rect(content.x(), list.bottom() + 4, content.w(), Math.max(0, content.bottom() - list.bottom() - 4));
        lookList.layout(content, 4);
        formList.layout(content, 4);
        auraList.layout(content, 4);
    }

    private List<RaceEntry> races() { return AvatarClientData.races; }

    private int visibleRaceRows() { return Math.max(1, list.h() / ROW_H); }

    private int maxRaceScroll() { return Math.max(0, races().size() - visibleRaceRows()); }

    // ================================================================ render
    @Override
    void render(GuiGraphics g, int mx, int my, float partial) {
        var font = h.font();
        String[] names = {ForgeText.s("nusmp.avatar.forge.tab.race", "Race"), ForgeText.s("nusmp.avatar.forge.tab.look", "Look"),
                ForgeText.s("nusmp.avatar.forge.tab.form", "Form"), ForgeText.s("nusmp.avatar.forge.tab.aura", "Aura")};
        for (int i = 0; i < 4; i++) {
            Rect t = tabRects[i];
            ForgeUi.chip(g, font, t, names[i], tab.ordinal() == i, h.hover(t, t.contains(mx, my)), 0x7CF0B0);
        }
        switch (tab) {
            case RACE -> renderRaces(g, mx, my);
            case LOOK -> lookList.render(h, g, mx, my);
            case FORM -> formList.render(h, g, mx, my);
            default -> auraList.render(h, g, mx, my);
        }
    }

    private void renderRaces(GuiGraphics g, int mx, int my) {
        var font = h.font();
        List<RaceEntry> rs = races();
        g.fill(list.x(), list.y(), list.right(), list.bottom(), ForgeUi.argb(0x03050A, 0.7f));
        g.renderOutline(list.x(), list.y(), list.w(), list.h(), ForgeUi.argb(0x7CF0B0, 0.3f));
        RaceEntry hover = null;
        int rows = visibleRaceRows();
        for (int i = 0; i < rows; i++) {
            int idx = raceScroll + i;
            if (idx >= rs.size()) break;
            RaceEntry r = rs.get(idx);
            Rect row = new Rect(list.x() + 1, list.y() + i * ROW_H, list.w() - (maxRaceScroll() > 0 ? 6 : 2), ROW_H);
            boolean sel = r.id().equals(h.state().race()), over = row.contains(mx, my);
            if (over) hover = r;
            float hv = h.hover("race" + idx, over);
            if (sel) { g.fill(row.x(), row.y(), row.right(), row.bottom(), ForgeUi.argb(0x7CF0B0, 0.25f)); g.fill(row.x(), row.y(), row.x() + 2, row.bottom(), ForgeUi.argb(0x7CF0B0, 0.95f)); }
            else if (hv > 0.02f) g.fill(row.x(), row.y(), row.right(), row.bottom(), ForgeUi.argb(r.available() ? 0x5CD8FF : 0xFF6A6A, 0.14f * hv));
            int col = !r.available() ? UiTheme.TEXT_FAINT : sel ? UiTheme.WHITE : UiTheme.TEXT;
            String name = Component.translatableWithFallback(r.name(), r.name()).getString();
            ForgeUi.text(g, font, ForgeUi.fit(font, name, row.w() - (r.available() ? 6 : 14)), row.x() + 4, row.y() + 3, col);
            if (!r.available()) ForgeUi.text(g, font, "x", row.right() - 9, row.y() + 3, UiTheme.RED);
        }
        if (rs.isEmpty()) ForgeUi.text(g, font, ForgeUi.fit(font, Component.translatable("nusmp.avatar.gui.race.none").getString(), list.w() - 6), list.x() + 4, list.y() + 4, UiTheme.TEXT_DIM);
        ForgeUi.scrollbar(g, new Rect(list.right() - 4, list.y() + 1, 3, list.h() - 2), raceScroll, maxRaceScroll(), rows, rs.size(), 0x7CF0B0);
        RaceEntry shown = hover != null ? hover : race();
        if (shown != null) drawRaceDetails(g, shown);
        if (hover != null) h.tooltip(raceTooltip(hover));
    }

    private RaceEntry race() {
        for (RaceEntry r : races()) if (r.id().equals(h.state().race())) return r;
        return null;
    }

    private void drawRaceDetails(GuiGraphics g, RaceEntry d) {
        var font = h.font();
        if (details.h() < 12) return;
        int x = details.x(), y = details.y(), w = details.w(), lh = font.lineHeight + 1;
        List<String> lines = new ArrayList<>();
        List<Integer> colours = new ArrayList<>();
        lines.add(Component.translatableWithFallback(d.name(), d.name()).getString()); colours.add(d.available() ? UiTheme.GOLD : UiTheme.TEXT_DIM);
        lines.add(Component.translatable("nusmp.avatar.gui.race.scale", String.format(Locale.ROOT, "%.2f", d.scale())).getString()); colours.add(UiTheme.TEXT);
        lines.add(Component.translatable("nusmp.avatar.gui.race.ep", (long) d.ep()).getString()); colours.add(UiTheme.TEXT);
        lines.add(Component.translatable("nusmp.avatar.gui.race.magicule", (long) d.magicule()).getString()); colours.add(UiTheme.TEXT);
        if (!d.statMults().isEmpty()) { lines.add(String.join(" ", d.statMults()).toUpperCase(Locale.ROOT)); colours.add(UiTheme.TEXT_DIM); }
        if (!d.evolvesTo().isEmpty()) { lines.add(Component.translatable("nusmp.avatar.gui.race.evolves", d.evolvesTo()).getString()); colours.add(UiTheme.VIOLET); }
        if (d.requiresDnd()) { lines.add(Component.translatable("nusmp.avatar.gui.race.dnd").getString()); colours.add(UiTheme.GOLD); }
        if (!d.available()) {
            lines.add(Component.translatable("nusmp.avatar.gui.race.locked").getString());
            colours.add(raceFlash > 0 && raceFlash % 6 < 3 ? UiTheme.WHITE : UiTheme.RED);
            for (String f : d.failed()) { lines.add("- " + Component.translatableWithFallback("nusmp.avatar.cond." + f.replace(':', '.'), f).getString()); colours.add(UiTheme.TEXT_FAINT); }
        }
        for (int i = 0; i < lines.size() && y + lh <= details.bottom() + 1; i++) {
            ForgeUi.text(g, font, ForgeUi.fit(font, lines.get(i), w), x, y, colours.get(i));
            y += lh;
        }
    }

    private List<Component> raceTooltip(RaceEntry d) {
        List<Component> t = new ArrayList<>();
        t.add(Component.translatableWithFallback(d.name(), d.name()).withStyle(d.available() ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        t.add(Component.translatable("nusmp.avatar.gui.race.scale", String.format(Locale.ROOT, "%.2f", d.scale())).withStyle(ChatFormatting.WHITE));
        t.add(Component.translatable("nusmp.avatar.gui.race.ep", (long) d.ep()).withStyle(ChatFormatting.WHITE));
        t.add(Component.translatable("nusmp.avatar.gui.race.magicule", (long) d.magicule()).withStyle(ChatFormatting.WHITE));
        if (!d.statMults().isEmpty()) t.add(Component.literal(String.join(" ", d.statMults()).toUpperCase(Locale.ROOT)).withStyle(ChatFormatting.GRAY));
        if (!d.evolvesTo().isEmpty()) t.add(Component.translatable("nusmp.avatar.gui.race.evolves", d.evolvesTo()).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (d.requiresDnd()) t.add(Component.translatable("nusmp.avatar.gui.race.dnd").withStyle(ChatFormatting.GOLD));
        if (!d.available()) {
            t.add(Component.translatable("nusmp.avatar.gui.race.locked").withStyle(ChatFormatting.RED));
            for (String f : d.failed()) t.add(Component.literal("- ").append(Component.translatableWithFallback("nusmp.avatar.cond." + f.replace(':', '.'), f)).withStyle(ChatFormatting.DARK_GRAY));
        }
        return t;
    }

    // ================================================================ input
    @Override
    void tick() { if (raceFlash > 0) raceFlash--; }

    @Override
    boolean mouseClicked(double mx, double my, int button) {
        for (int i = 0; i < 4; i++) {
            if (tabRects[i].contains(mx, my) && button == 0) {
                if (tab.ordinal() != i) { h.click(); tab = Tab.values()[i]; }
                return true;
            }
        }
        return switch (tab) {
            case RACE -> clickRace(mx, my, button);
            case LOOK -> lookList.click(h, mx, my, button);
            case FORM -> formList.click(h, mx, my, button);
            default -> auraList.click(h, mx, my, button);
        };
    }

    private boolean clickRace(double mx, double my, int button) {
        if (button != 0 || !list.contains(mx, my)) return false;
        int idx = raceScroll + (int) ((my - list.y()) / ROW_H);
        List<RaceEntry> rs = races();
        if (idx < 0 || idx >= rs.size() || idx >= raceScroll + visibleRaceRows()) return true;
        RaceEntry r = rs.get(idx);
        if (r.available()) {
            boolean changed = !r.id().equals(h.state().race());
            h.click();
            h.state().chooseRace(r.id(), true);
            if (changed) h.setLook(DynamicAppearanceEngine.sanitize(h.look().withScale(r.scale()), r.scale()));
            h.pulse(h.look().auraRgb());
            h.changed();
        } else {
            raceFlash = 20;
        }
        return true;
    }

    @Override
    boolean mouseDragged(double mx, double my, int button) {
        return switch (tab) {
            case LOOK -> lookList.drag(mx, my);
            case FORM -> formList.drag(mx, my);
            case AURA -> auraList.drag(mx, my);
            default -> false;
        };
    }

    @Override
    boolean mouseReleased(double mx, double my, int button) {
        lookList.release();
        formList.release();
        auraList.release();
        return false;
    }

    @Override
    boolean mouseScrolled(double mx, double my, double dy) {
        switch (tab) {
            case RACE -> {
                if (!list.contains(mx, my) && !details.contains(mx, my)) return false;
                raceScroll = Math.max(0, Math.min(maxRaceScroll(), raceScroll - (int) Math.signum(dy)));
                return true;
            }
            case LOOK -> { return lookList.scroll(mx, my, dy); }
            case FORM -> { return formList.scroll(mx, my, dy); }
            default -> { return auraList.scroll(mx, my, dy); }
        }
    }

    @Override
    void hidden() {
        lookList.release();
        formList.release();
        auraList.release();
    }
}
