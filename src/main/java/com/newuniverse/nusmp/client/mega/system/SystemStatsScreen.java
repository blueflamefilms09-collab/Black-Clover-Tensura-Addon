package com.newuniverse.nusmp.client.mega.system;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.mega.system.StatScreenLayout.Act;
import com.newuniverse.nusmp.client.mega.system.StatScreenLayout.Hit;
import com.newuniverse.nusmp.client.mega.system.StatScreenLayout.Rect;
import com.newuniverse.nusmp.client.mega.system.StatScreenLayout.Row;
import com.newuniverse.nusmp.labyrinth.mega.system.PointsText;
import com.newuniverse.nusmp.labyrinth.mega.system.Stat;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemPointsNet;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Why: the owner wants to pick where each stat point goes, with the MOUSE. This is the screen for it, in the look of the System window ({@link OverlayStyle}: the
 * dark-cyan panel, the cyan border and brackets, the amber of things that want attention). It shows the five stats (STR AGI VIT INT SENSE) with their points
 * against the cap and a small bar, what ONE more point does (the real effect and its ceiling, read from {@code stats.json} on the server and sent as
 * {@link SystemPointsNet.PointsSync}; an effect on an attribute that is not installed is left out, as the server leaves it out), the free pool, and the buttons:
 * [+1] (Shift: 5), [+5] and [MAX] per stat, [RESET] (moves every placed point back, when the server allows it) and [MOVE WINDOW] (opens
 * {@link SystemEditScreen}). The mouse wheel over a stat row adds one point from the pool (down: takes one back, when allowed; Shift: five). Hovering a button
 * says what it does. Every click only sends a REQUEST ({@link SystemPointsNet.Allocate} / {@link SystemPointsNet.Reset}); the server decides and the screen shows the
 * numbers the server sends back, so it can never show a point that was not granted. Where each button sits is the pure {@link StatScreenLayout}.
 */
public final class SystemStatsScreen extends Screen {
    private static final Logger LOG = LogUtils.getLogger();

    private final Screen parent;
    private StatScreenLayout.Layout lay;
    private int builtVersion = -1, builtPointsKey = Integer.MIN_VALUE;
    private List<List<String>> effectText = List.of();

    public SystemStatsScreen(Screen parent) {
        super(Component.translatableWithFallback(PointsText.TITLE, PointsText.all().get(PointsText.TITLE)));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    @Override
    protected void init() { builtVersion = -1; }

    private static String tr(String key, Object... args) { return Component.translatableWithFallback(key, SystemText.english(key), args).getString(); }

    // ================================================================ what the screen says
    /** The wrapped effect lines of every stat, rebuilt only when the server's facts or the stat points changed. */
    private void rebuild() {
        int key = 17;
        for (Stat s : Stat.values()) key = key * 31 + ClientSystem.stat(s);
        if (builtVersion == ClientPoints.version() && builtPointsKey == key && lay != null) return;
        builtVersion = ClientPoints.version();
        builtPointsKey = key;
        List<List<String>> all = new ArrayList<>(Stat.COUNT);
        int[] counts = new int[Stat.COUNT];
        int room = StatScreenLayout.textWidth(width);
        for (Stat s : Stat.values()) {
            List<String> out = new ArrayList<>();
            for (ClientPoints.Effect e : ClientPoints.effects(s)) {
                String line = effectLine(e, ClientSystem.stat(s));
                if (line == null) continue;
                out.addAll(TextWrap.wrap(line, room, font::width, 2));
            }
            all.add(out);
            counts[s.ordinal()] = out.size();
        }
        effectText = all;
        lay = StatScreenLayout.of(width, height, counts);
    }

    /** One effect as a sentence, or null when its attribute is not registered here (a mod that is not installed): the server skips it too. */
    private String effectLine(ClientPoints.Effect e, int points) {
        ResourceLocation id = ResourceLocation.tryParse(e.attribute());
        if (id == null) return null;
        Optional<Attribute> attr = BuiltInRegistries.ATTRIBUTE.getOptional(id);
        if (attr.isEmpty()) return null;
        String name = Component.translatable(attr.get().getDescriptionId()).getString();
        String per = PointsText.amount(e.op(), e.perPoint());
        String now = PointsText.amount(e.op(), e.amount(points));
        if (e.cap() > 0) return tr(PointsText.EFFECT, per, name, now, PointsText.amount(e.op(), Math.copySign(e.cap(), e.perPoint())));
        return tr(PointsText.EFFECT_NOCAP, per, name, now);
    }

    // ================================================================ drawing
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0xA0000000);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        try {
            rebuild();
            renderBackground(g, mx, my, partial);
            drawAll(g, mx, my, Minecraft.getInstance().level == null ? 0f : Minecraft.getInstance().level.getGameTime() + partial);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] The stat screen failed to draw and is closed", e);
            onClose();
        }
    }

    private static void fill(GuiGraphics g, int x1, int y1, int x2, int y2, int rgb, float a) {
        if (x2 > x1 && y2 > y1) g.fill(x1, y1, x2, y2, OverlayMath.withAlpha(rgb, a));
    }

    private static void frame(GuiGraphics g, Rect r, int rgb, float a) {
        fill(g, r.x(), r.y(), r.right(), r.y() + 1, rgb, a);
        fill(g, r.x(), r.bottom() - 1, r.right(), r.bottom(), rgb, a);
        fill(g, r.x(), r.y() + 1, r.x() + 1, r.bottom() - 1, rgb, a);
        fill(g, r.right() - 1, r.y() + 1, r.right(), r.bottom() - 1, rgb, a);
    }

    private void drawAll(GuiGraphics g, int mx, int my, float time) {
        Rect p = lay.panel();
        g.fillGradient(p.x(), p.y(), p.right(), p.bottom(), OverlayMath.withAlpha(OverlayStyle.BG_TOP, 0.94f), OverlayMath.withAlpha(OverlayStyle.BG_BOTTOM, 0.94f));
        frame(g, p, OverlayStyle.CYAN, 1f);
        int b = OverlayStyle.BRACKET_LEN;
        int bc = OverlayMath.withAlpha(OverlayStyle.CYAN_BRIGHT, 1f);
        g.fill(p.x() - 1, p.y() - 1, p.x() + b, p.y(), bc);
        g.fill(p.x() - 1, p.y() - 1, p.x(), p.y() + b, bc);
        g.fill(p.right() - b, p.y() - 1, p.right() + 1, p.y(), bc);
        g.fill(p.right(), p.y() - 1, p.right() + 1, p.y() + b, bc);
        g.fill(p.x() - 1, p.bottom(), p.x() + b, p.bottom() + 1, bc);
        g.fill(p.x() - 1, p.bottom() - b, p.x(), p.bottom() + 1, bc);
        g.fill(p.right() - b, p.bottom(), p.right() + 1, p.bottom() + 1, bc);
        g.fill(p.right(), p.bottom() - b, p.right() + 1, p.bottom() + 1, bc);

        int x = p.x() + StatScreenLayout.PAD;
        g.drawString(font, tr(PointsText.TITLE), x, lay.titleY(), OverlayMath.withAlpha(OverlayStyle.CYAN_BRIGHT, 1f), true);
        int free = ClientPoints.free();
        String freeLabel = tr(PointsText.FREE) + "  ";
        g.drawString(font, freeLabel, x, lay.freeY(), OverlayMath.withAlpha(OverlayStyle.CYAN_DIM, 1f), true);
        float pulse = free > 0 ? 0.8f + 0.2f * (float) Math.sin(time * 0.25f) : 1f;
        g.drawString(font, Integer.toString(free), x + font.width(freeLabel), lay.freeY(), OverlayMath.withAlpha(free > 0 ? OverlayStyle.AMBER : OverlayStyle.CYAN_FAINT, pulse), true);
        String right = free > 0 ? tr(PointsText.FREE_HINT) : tr(PointsText.FREE_NONE);
        int rw = font.width(right);
        if (rw < p.w() - 2 * StatScreenLayout.PAD - font.width(freeLabel) - 24) g.drawString(font, right, p.right() - StatScreenLayout.PAD - rw, lay.freeY(), OverlayMath.withAlpha(OverlayStyle.CYAN_DIM, 1f), false);
        if (lay.modeY() >= 0) {
            String mode = ClientPoints.mode() == com.newuniverse.nusmp.labyrinth.mega.system.PointMode.RANDOM ? tr(PointsText.MODE_RANDOM) : tr(PointsText.MODE_CHOOSE);
            g.drawString(font, mode, x, lay.modeY(), OverlayMath.withAlpha(OverlayStyle.CYAN_FAINT, 1f), false);
        }

        Hit over = lay.hit(mx, my);
        Row hoverRow = lay.rowAt(mx, my);
        for (Row r : lay.rows()) drawRow(g, r, over, hoverRow == r);
        button(g, lay.reset(), tr(PointsText.BTN_RESET), ClientPoints.allowRespec() && totalSpent() > 0, over != null && over.act() == Act.RESET, false);
        button(g, lay.move(), tr(PointsText.BTN_MOVE), true, over != null && over.act() == Act.MOVE, false);
        button(g, lay.close(), tr(PointsText.BTN_CLOSE), true, over != null && over.act() == Act.CLOSE, false);
        tooltip(g, mx, my, over, hoverRow);
    }

    private int totalSpent() {
        int t = 0;
        for (Stat s : Stat.values()) t += ClientSystem.stat(s);
        return t;
    }

    private void drawRow(GuiGraphics g, Row r, Hit over, boolean hovered) {
        Stat s = r.stat();
        int have = ClientSystem.stat(s), cap = ClientPoints.cap(), free = ClientPoints.free();
        if (hovered) fill(g, r.rect().x(), r.rect().y(), r.rect().right(), r.rect().bottom(), OverlayStyle.CYAN, 0.08f);
        String name = tr(statShort(s)) + "  " + tr(PointsText.statFullKey(s));
        g.drawString(font, name, r.textX(), r.nameY(), OverlayMath.withAlpha(OverlayStyle.TEXT, 1f), true);
        String value = tr(PointsText.VALUE, have, cap);
        int nameEnd = r.textX() + font.width(name) + 8;
        g.drawString(font, value, nameEnd, r.nameY(), OverlayMath.withAlpha(have > 0 ? OverlayStyle.CYAN_BRIGHT : OverlayStyle.CYAN_FAINT, 1f), true);
        int barX = nameEnd + font.width(value) + 8, barX2 = r.add1().x() - 8;
        if (barX2 - barX >= 20 && cap > 0) {
            fill(g, barX, r.nameY() + 2, barX2, r.nameY() + 6, OverlayStyle.CYAN_FAINT, 0.55f);
            int lit = Math.round((barX2 - barX) * Math.min(1f, have / (float) cap));
            fill(g, barX, r.nameY() + 2, barX + lit, r.nameY() + 6, have >= cap ? OverlayStyle.GOLD : OverlayStyle.CYAN, 1f);
        }
        int ok1 = ClientPoints.clampRequest(s, 1, have), ok5 = ClientPoints.clampRequest(s, 5, have), okMax = ClientPoints.clampRequest(s, Integer.MAX_VALUE, have);
        boolean hov1 = over != null && over.stat() == s && over.act() == Act.ADD1, hov5 = over != null && over.stat() == s && over.act() == Act.ADD5, hovM = over != null && over.stat() == s && over.act() == Act.MAX;
        button(g, r.add1(), tr(PointsText.BTN_ADD1), ok1 > 0, hov1, false);
        button(g, r.add5(), tr(PointsText.BTN_ADD5), ok5 > 0, hov5, false);
        button(g, r.max(), tr(PointsText.BTN_MAX), okMax > 0, hovM, free > 0 && okMax > 0);
        int ey = r.effectY();
        for (String line : effectText.get(s.ordinal())) {
            g.drawString(font, line, r.textX() + 4, ey, OverlayMath.withAlpha(OverlayStyle.CYAN_DIM, 1f), false);
            ey += StatScreenLayout.LINE;
        }
    }

    private static String statShort(Stat s) {
        return switch (s) {
            case STR -> SystemText.STAT_STR;
            case AGI -> SystemText.STAT_AGI;
            case VIT -> SystemText.STAT_VIT;
            case INT -> SystemText.STAT_INT;
            case SENSE -> SystemText.STAT_SENSE;
        };
    }

    /** A button: dim when it cannot be used now, brighter under the mouse, amber for the one that spends everything. */
    private void button(GuiGraphics g, Rect r, String label, boolean enabled, boolean hover, boolean accent) {
        int rgb = !enabled ? OverlayStyle.CYAN_FAINT : accent ? OverlayStyle.AMBER : OverlayStyle.CYAN;
        fill(g, r.x(), r.y(), r.right(), r.bottom(), rgb, !enabled ? 0.10f : hover ? 0.38f : 0.18f);
        frame(g, r, rgb, !enabled ? 0.5f : hover ? 1f : 0.75f);
        int tw = font.width(label);
        g.drawString(font, label, r.x() + (r.w() - tw) / 2, r.y() + (r.h() - 8) / 2 + 1, OverlayMath.withAlpha(enabled ? OverlayStyle.TEXT : OverlayStyle.CYAN_FAINT, 1f), enabled);
    }

    private void tooltip(GuiGraphics g, int mx, int my, Hit over, Row row) {
        String text = null;
        if (over != null) {
            String stat = over.stat() == null ? "" : tr(PointsText.statFullKey(over.stat()));
            int have = over.stat() == null ? 0 : ClientSystem.stat(over.stat());
            boolean full = over.stat() != null && have >= ClientPoints.cap();
            switch (over.act()) {
                case ADD1 -> text = full ? tr(PointsText.TIP_FULL, stat) : ClientPoints.free() <= 0 ? tr(PointsText.TIP_NOFREE) : tr(PointsText.TIP_ADD1, stat);
                case ADD5 -> text = full ? tr(PointsText.TIP_FULL, stat) : ClientPoints.free() <= 0 ? tr(PointsText.TIP_NOFREE) : tr(PointsText.TIP_ADD5, stat);
                case MAX -> text = full ? tr(PointsText.TIP_FULL, stat) : ClientPoints.free() <= 0 ? tr(PointsText.TIP_NOFREE) : tr(PointsText.TIP_MAX, stat);
                case RESET -> text = ClientPoints.allowRespec() ? tr(PointsText.TIP_RESET) : tr(PointsText.TIP_RESET_OFF);
                case MOVE -> text = tr(PointsText.TIP_MOVE);
                case CLOSE -> text = null;
            }
        } else if (row != null) {
            text = tr(PointsText.TIP_ROW);
        }
        if (text == null) return;
        List<FormattedCharSequence> lines = font.split(Component.literal(text), 200);
        g.renderTooltip(font, lines, mx, my);
    }

    // ================================================================ the mouse
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || lay == null) return super.mouseClicked(mx, my, button);
        Hit hit = lay.hit(mx, my);
        if (hit == null) return super.mouseClicked(mx, my, button);
        switch (hit.act()) {
            case ADD1 -> add(hit.stat(), hasShiftDown() ? 5 : 1);
            case ADD5 -> add(hit.stat(), 5);
            case MAX -> add(hit.stat(), Integer.MAX_VALUE);
            case RESET -> {
                if (ClientPoints.allowRespec()) PacketDistributor.sendToServer(new SystemPointsNet.Reset());
            }
            case MOVE -> minecraft.setScreen(new SystemEditScreen(this));
            case CLOSE -> onClose();
        }
        return true;
    }

    /** Asks the server to move points (cut to the pool and the room first, so the request is never an obvious refusal). */
    private void add(Stat s, int wanted) {
        int n = ClientPoints.clampRequest(s, wanted, ClientSystem.stat(s));
        if (n > 0) PacketDistributor.sendToServer(new SystemPointsNet.Allocate(s.ordinal(), n));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (lay == null || scrollY == 0.0) return super.mouseScrolled(mx, my, scrollX, scrollY);
        Row row = lay.rowAt(mx, my);
        if (row == null) return super.mouseScrolled(mx, my, scrollX, scrollY);
        int step = hasShiftDown() ? 5 : 1;
        if (scrollY > 0) add(row.stat(), step);
        else if (ClientPoints.allowRespec() && ClientSystem.stat(row.stat()) > 0) {
            PacketDistributor.sendToServer(new SystemPointsNet.Allocate(row.stat().ordinal(), -Math.min(step, ClientSystem.stat(row.stat()))));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (SystemClient.POINTS.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
