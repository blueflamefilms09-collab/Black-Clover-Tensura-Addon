package com.newuniverse.nusmp.client.heroweapon.forms;

import com.newuniverse.nusmp.client.ui.UiDraw;
import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiBar;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.heroweapon.HeroItems;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.Sin;
import com.newuniverse.nusmp.heroweapon.bond.HeroLevels;
import com.newuniverse.nusmp.heroweapon.forms.FormText;
import com.newuniverse.nusmp.heroweapon.forms.FormTags;
import com.newuniverse.nusmp.heroweapon.forms.FormTree;
import com.newuniverse.nusmp.heroweapon.forms.FormsLang;
import com.newuniverse.nusmp.heroweapon.forms.FormsNet;
import com.newuniverse.nusmp.heroweapon.forms.HeroAbsorb;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Why: the owner's mechanic is "weapons absorb raw materials, monster parts or items into a central gem to unlock new specialised forms, stats and skills", and a
 * player needs to SEE that: which forms exist, which are theirs, what each still asks for, what it gives and which of the items in their pockets would help. This is
 * that screen, the Weapon Book (key M, or {@code /multiverse hero form book}). Left, the forms as an indented list with a status mark (in use, learned, available,
 * locked, sealed curse); right, the chosen form: its words, what it needs first, the requirements with item icons and progress bars, the items in the inventory
 * that would feed the gem (click to feed what is needed, shift-click the whole stack), the stats, the permanent bonus of mastery, the techniques and passives
 * (hover for their text) and the mastery bar; below, the buttons Use, Learn and, only while the weapon whispers its curse, Embrace. It shows what the server last said
 * ({@link ClientBook}) and decides nothing: every click is a packet the server checks again. It is drawn in the mod's GUI theme (anime glass by default, parchment in
 * the classic look); the layout is {@link BookLayout}, tested for a grid of window sizes.
 */
public final class WeaponBookScreen extends Screen {
    private static final FormsNet.State NONE = new FormsNet.State(false, "", HeroForm.BASE_ID, 0, false, false, false, 0, 0, "", List.of(), Map.of(), List.of(), Map.of(), List.of());

    private record Entry(HeroForm form, int indent, boolean heading) {}

    @FunctionalInterface
    private interface Draw {
        void draw(GuiGraphics g, int x, int y, int w, int mx, int my);
    }

    private record Block(int h, Draw draw) {}

    private record FeedSlot(BookLayout.Rect rect, String itemId) {}

    private FormsNet.State st = NONE;
    private HeroKind kind;
    private List<HeroForm> forms = List.of();
    private final List<Entry> entries = new ArrayList<>();
    private Set<String> learned = Set.of(), series = Set.of(), mastered = Set.of();
    private String selected = "";
    private int listScroll, paneScroll, contentHeight, lastSignature;
    private boolean dirty = true;
    private BookLayout lay = BookLayout.of(320, 240);
    private final List<Block> blocks = new ArrayList<>();
    private final List<FeedSlot> feedSlots = new ArrayList<>();
    private List<FormattedCharSequence> tip;
    private ThemedButton useButton, learnButton, embraceButton;
    private final UiBar levelBar = new UiBar(), masteryBar = new UiBar();
    private final Map<String, List<ItemStack>> icons = new HashMap<>();

    public WeaponBookScreen() {
        super(FormsLang.t("hero.book.title", "Weapon Book"));
        FormsNet.State s = ClientBook.state();
        st = s != null ? s : NONE;
        rebuildData();
    }

    // ================================================================================================ data
    /** The server sent fresh facts: take them in, keeping the selection and the scroll. */
    public void refresh(FormsNet.State state) {
        if (!state.bound()) {                       // the weapon was released while the book was open: nothing is left to show
            onClose();
            return;
        }
        st = state;
        rebuildData();
        updateButtons();
    }

    private void rebuildData() {
        kind = st.weapon();
        forms = kind == null ? List.of() : HeroForms.of(kind);
        Set<String> l = new HashSet<>(st.learned());
        l.add(HeroForm.BASE_ID);
        learned = l;
        series = new HashSet<>(st.series());
        mastered = new HashSet<>(st.bonuses());
        entries.clear();
        for (FormTree.Row r : FormTree.rows(forms)) {
            if (r.curseHeading()) entries.add(new Entry(null, 0, true));
            entries.add(new Entry(r.form(), r.indent(), false));
        }
        if (selectedForm() == null) {
            HeroForm pick = null;
            for (HeroForm f : forms) if (f.id().equals(st.form())) pick = f;
            if (pick == null && !forms.isEmpty()) pick = forms.get(0);
            selected = pick == null ? "" : pick.id();
        }
        dirty = true;
    }

    private HeroForm selectedForm() {
        for (HeroForm f : forms) if (f.id().equals(selected)) return f;
        return null;
    }

    private FormTree.Status status(HeroForm f) { return FormTree.status(f, st.form(), learned, st.absorbed(), FormTags.MATCHER, series); }

    private static String tr(String key, String english, Object... args) { return FormsLang.t(key, english, args).getString(); }

    /** The text cut to fit {@code w} pixels, with three dots when something was cut. */
    private String fit(String text, int w) {
        if (font.width(text) <= w) return text;
        return font.plainSubstrByWidth(text, Math.max(0, w - font.width("..."))) + "...";
    }

    private void send(int action, String arg, boolean whole) {
        try {
            PacketDistributor.sendToServer(new FormsNet.Act(action, arg, whole));
        } catch (RuntimeException disconnected) {
            // the connection is gone; the screen is about to close anyway
        }
    }

    // ================================================================================================ screen life
    @Override
    protected void init() {
        lay = BookLayout.of(width, height);
        clearWidgets();
        useButton = addRenderableWidget(new ThemedButton(0, 0, 40, BookLayout.BUTTON_H, FormsLang.t("hero.book.use", "Use"), b -> send(FormsNet.ACT_USE, selected, false)));
        learnButton = addRenderableWidget(new ThemedButton(0, 0, 40, BookLayout.BUTTON_H, FormsLang.t("hero.book.learn", "Learn"), b -> send(FormsNet.ACT_LEARN, selected, false)));
        embraceButton = addRenderableWidget(new ThemedButton(0, 0, 40, BookLayout.BUTTON_H, FormsLang.t("hero.book.embrace", "Embrace"), b -> send(FormsNet.ACT_EMBRACE, "", false)));
        updateButtons();
        dirty = true;
    }

    private void updateButtons() {
        if (useButton == null) return;
        boolean has = kind != null;
        HeroForm f = selectedForm();
        FormTree.Status s = f == null ? null : status(f);
        boolean embrace = has && !st.offered().isEmpty() && !series.contains(st.offered());
        int n = embrace ? 3 : 2;
        place(useButton, 0, n);
        place(learnButton, 1, n);
        place(embraceButton, 2, n);
        useButton.visible = has;
        learnButton.visible = has;
        embraceButton.visible = embrace;
        useButton.active = s == FormTree.Status.LEARNED && !st.locked();
        useButton.setMessage(FormsLang.t(s == FormTree.Status.ACTIVE ? "hero.book.in_use" : "hero.book.use", s == FormTree.Status.ACTIVE ? "In use" : "Use"));
        learnButton.active = s == FormTree.Status.AVAILABLE;
    }

    private void place(ThemedButton b, int index, int n) {
        BookLayout.Rect r = lay.button(index, n);
        b.setX(r.x());
        b.setY(r.y());
        b.setWidth(r.w());
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void removed() {
        send(FormsNet.ACT_CLOSE, "", false);
        super.removed();
    }

    @Override
    public void tick() {
        super.tick();
        int sig = inventorySignature();
        if (sig != lastSignature) {
            lastSignature = sig;
            dirty = true;
        }
    }

    /** A number that changes when the part of the inventory the Feed strip shows changes. */
    private int inventorySignature() {
        HeroForm f = selectedForm();
        if (f == null || minecraft == null || minecraft.player == null) return 0;
        int h = 17;
        Inventory inv = minecraft.player.getInventory();
        for (ItemStack s : inv.items) h = h * 31 + signature(f, s);
        for (ItemStack s : inv.offhand) h = h * 31 + signature(f, s);
        return h * 31 + st.absorbed().hashCode();
    }

    private int signature(HeroForm f, ItemStack s) {
        if (s.isEmpty() || !wanted(f, HeroAbsorb.idOf(s))) return 0;
        return HeroAbsorb.idOf(s).hashCode() * 7 + s.getCount();
    }

    // ================================================================================================ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0 || kind == null) return false;
        if (lay.list().contains(mx, my)) {
            int row = listScroll + (int) ((my - lay.list().y() - 1) / BookLayout.ROW_H);
            if (row >= 0 && row < entries.size() && !entries.get(row).heading()) select(entries.get(row).form().id());
            return true;
        }
        if (lay.pane().contains(mx, my)) {
            for (FeedSlot s : feedSlots) {
                if (s.rect().contains(mx, my)) {
                    send(FormsNet.ACT_FEED, s.itemId(), hasShiftDown());
                    click();
                    return true;
                }
            }
        }
        return false;
    }

    private void select(String id) {
        if (id.equals(selected)) return;
        selected = id;
        paneScroll = 0;
        dirty = true;
        updateButtons();
        click();
    }

    private void click() {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (dy == 0) return false;
        if (lay.list().contains(mx, my)) {
            listScroll = clamp(listScroll + (dy < 0 ? 1 : -1), 0, Math.max(0, entries.size() - lay.visibleRows()));
            return true;
        }
        if (lay.pane().contains(mx, my)) {
            paneScroll = clamp(paneScroll - (int) Math.signum(dy) * 14, 0, maxPaneScroll());
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (FormsClient.KEY.matches(key, scan)) {
            onClose();
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_UP -> move(-1);
            case GLFW.GLFW_KEY_DOWN -> move(1);
            case GLFW.GLFW_KEY_PAGE_UP -> paneScroll = clamp(paneScroll - lay.pane().h() / 2, 0, maxPaneScroll());
            case GLFW.GLFW_KEY_PAGE_DOWN -> paneScroll = clamp(paneScroll + lay.pane().h() / 2, 0, maxPaneScroll());
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (useButton != null && useButton.active && useButton.visible) send(FormsNet.ACT_USE, selected, false);
            }
            default -> {
                return super.keyPressed(key, scan, mods);
            }
        }
        return true;
    }

    /** Selects the previous or next form in the list (skipping the heading) and scrolls it into view. */
    private void move(int step) {
        int at = -1;
        for (int i = 0; i < entries.size(); i++) if (!entries.get(i).heading() && entries.get(i).form().id().equals(selected)) at = i;
        for (int i = at + step; i >= 0 && i < entries.size(); i += step) {
            if (entries.get(i).heading()) continue;
            select(entries.get(i).form().id());
            int rows = lay.visibleRows();
            if (i < listScroll) listScroll = i;
            else if (i >= listScroll + rows) listScroll = i - rows + 1;
            return;
        }
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private int maxPaneScroll() { return Math.max(0, contentHeight + 8 - lay.pane().h()); }

    // ================================================================================================ colours and furniture (anime glass, or parchment in the classic look)
    private static boolean anime() { return UiTheme.anime(); }

    private static int cText() { return UiDraw.ink(); }

    private static int cDim() { return UiDraw.inkDim(); }

    private static int cGold() { return UiDraw.goldInk(); }

    private static int cWarn() { return UiDraw.warn(); }

    private static int cOk() { return anime() ? UiTheme.GREEN : 0xFF2E7D4F; }

    private static int cAccent() { return UiDraw.accent(); }

    private static int cLearned() { return anime() ? UiTheme.GREEN : 0xFF2E7D4F; }

    private static int sinColor(Sin s) { return 0xFF000000 | s.rgb(); }

    /** The sin's colour lightened for TEXT on the dark glass (the darker sins, Wrath and Gluttony, would be hard to read as they are). */
    private static int sinText(Sin s) { return anime() ? UiMath.mix(sinColor(s), 0xFFFFFFFF, 0.38f) : sinColor(s); }

    private void panel(GuiGraphics g, BookLayout.Rect r, float glow) {
        if (anime()) UiPanel.panel(g, r.x(), r.y(), r.w(), r.h(), UiTheme.CYAN, glow, 1f);
        else {
            g.fill(r.x(), r.y(), r.right(), r.bottom(), UiDraw.PAPER);
            g.renderOutline(r.x(), r.y(), r.w(), r.h(), UiDraw.COVER_EDGE);
        }
    }

    private void well(GuiGraphics g, BookLayout.Rect r) {
        if (anime()) UiPanel.well(g, r.x(), r.y(), r.w(), r.h(), UiTheme.CYAN, 1f);
        else {
            g.fill(r.x(), r.y(), r.right(), r.bottom(), UiDraw.PAPER_SHADE);
            g.renderOutline(r.x(), r.y(), r.w(), r.h(), UiDraw.RULE);
        }
    }

    private void highlight(GuiGraphics g, int x, int y, int w, int h, float amount) {
        if (anime()) UiPanel.hover(g, x, y, w, h, amount, UiTheme.CYAN);
        else g.fill(x, y, x + w, y + h, UiMath.withAlpha(UiDraw.ACCENT, 0.25f * amount));
    }

    /** The dim scrim, then the book itself: drawn as the screen's background so that the buttons land on top of it and vanilla's blur never touches the panel (no super call on purpose). */
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        if (anime()) UiPanel.scrim(g, width, height, 1f);
        else g.fillGradient(0, 0, width, height, 0xB0101018, 0xC0101018);
        drawBook(g, mx, my);
    }

    // ================================================================================================ drawing
    /** The tooltip is drawn last, over everything; vanilla's render draws the background (the book) and then the buttons. */
    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        tip = null;
        super.render(g, mx, my, partial);
        if (tip != null) g.renderTooltip(font, tip, mx, my);
    }

    private void drawBook(GuiGraphics g, int mx, int my) {
        float now = UiTheme.now();
        panel(g, lay.panel(), 0.6f);
        drawHeader(g, mx, my, now);
        if (kind == null || forms.isEmpty()) {
            drawNone(g);
        } else {
            if (dirty) rebuildBlocks();
            drawList(g, mx, my, now);
            drawPane(g, mx, my, now);
            drawFooterLeft(g, now);
        }
    }

    private void drawNone(GuiGraphics g) {
        int cx = lay.panel().x() + lay.panel().w() / 2, cy = lay.panel().y() + lay.panel().h() / 2 - 10;
        g.drawCenteredString(font, Component.literal(tr("hero.book.none", "You have no hero weapon.")), cx, cy, cDim());
        g.drawCenteredString(font, Component.literal(tr("hero.book.none_hint", "Bind one with a Calling Gem, or ask an operator.")), cx, cy + 12, cDim());
    }

    private void drawHeader(GuiGraphics g, int mx, int my, float now) {
        BookLayout.Rect h = lay.header();
        String name = kind == null ? tr("hero.book.title", "Weapon Book") : kind.display();
        int barW = Math.max(70, Math.min(120, h.w() / 3));
        int leftW = h.w() - barW - 10;
        g.drawString(font, Component.literal(fit(name, leftW)).withStyle(ChatFormatting.BOLD), h.x(), h.y(), cGold(), false);
        if (kind == null) return;
        String sub = kind.heroClass().label() + (kind.holder().isEmpty() ? "" : " \u00b7 " + kind.holder());
        g.drawString(font, fit(sub, leftW), h.x(), h.y() + 11, cDim(), false);
        int level = HeroLevels.level(st.exp());
        int bx = h.right() - barW;
        String levelText = tr("hero.book.level", "Lv %s", level);
        g.drawString(font, levelText, bx, h.y(), cText(), false);
        String exp = level >= HeroLevels.MAX ? tr("hero.book.max", "max") : st.exp() + " / " + HeroLevels.expFor(level + 1);
        if (font.width(levelText) + 8 + font.width(exp) <= barW) g.drawString(font, exp, h.right() - font.width(exp), h.y(), cDim(), false);
        else if (mx >= bx && mx < h.right() && my >= h.y() && my < h.y() + 16) tip = List.of(Component.literal(exp).getVisualOrderText());
        levelBar.update((float) HeroLevels.progress(st.exp()), now, false);
        levelBar.draw(g, bx, h.y() + 12, barW, 4, anime() ? UiTheme.VIOLET : UiDraw.ACCENT, now, 1f);
    }

    private void drawFooterLeft(GuiGraphics g, float now) {
        BookLayout.Rect r = lay.footerLeft();
        int lh = font.lineHeight;
        if (!st.offered().isEmpty() && !series.contains(st.offered())) {
            Sin sin = Sin.byId(st.offered());
            int col = sin == null ? cWarn() : sinText(sin);
            col = UiMath.mix(col, 0xFFFFFFFF, 0.3f * UiMath.pulse(now, 40f, 0f));
            String text = tr("hero.book.whisper", "The weapon whispers: %s", sin == null ? FormText.pretty(st.offered()) : sin.label());
            List<FormattedCharSequence> lines = font.split(Component.literal(text), r.w());
            int n = Math.min(2, lines.size()), y0 = r.y() + (r.h() - n * (lh + 1)) / 2;
            for (int i = 0; i < n; i++) g.drawString(font, lines.get(i), r.x(), y0 + i * (lh + 1), col, false);
            return;
        }
        if (!series.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (String s : series) {
                Sin sin = Sin.byId(s);
                names.add(sin == null ? FormText.pretty(s) : sin.label());
            }
            String text = tr("hero.book.curse_series", "Curse: %s", String.join(", ", names));
            Sin first = Sin.byId(series.iterator().next());
            int col = first == null ? cWarn() : sinText(first);
            boolean bar = st.corruption() > 0;
            g.drawString(font, fit(text, r.w()), r.x(), bar ? r.y() + 3 : r.y() + (r.h() - lh) / 2, col, false);
            if (bar) UiBar.draw(g, r.x(), r.y() + 3 + lh + 3, r.w() - 2, 3, st.corruption() / 1000f, first == null ? cWarn() : sinColor(first), now, 1f);
        }
    }

    // ---------------------------------------------------------------- the list
    private void drawList(GuiGraphics g, int mx, int my, float now) {
        BookLayout.Rect r = lay.list();
        well(g, r);
        int rows = lay.visibleRows();
        listScroll = clamp(listScroll, 0, Math.max(0, entries.size() - rows));
        g.enableScissor(r.x() + 1, r.y() + 1, r.right() - 1, r.bottom() - 1);
        for (int i = 0; i < rows && listScroll + i < entries.size(); i++) {
            Entry e = entries.get(listScroll + i);
            int y = r.y() + 1 + i * BookLayout.ROW_H;
            if (e.heading()) {
                g.drawString(font, tr("hero.book.curse_heading", "Curse Series"), r.x() + 5, y + 3, cWarn(), false);
                continue;
            }
            HeroForm f = e.form();
            FormTree.Status s = status(f);
            boolean sel = f.id().equals(selected), hov = mx >= r.x() && mx < r.right() - 5 && my >= y && my < y + BookLayout.ROW_H && r.contains(mx, my);
            if (sel) highlight(g, r.x() + 1, y, r.w() - 2, BookLayout.ROW_H, 1f);
            else if (hov) highlight(g, r.x() + 1, y, r.w() - 2, BookLayout.ROW_H, 0.5f);
            int ix = r.x() + 5 + Math.min(e.indent(), 5) * 6;
            statusMark(g, ix, y + 3, f, s, now);
            int color = switch (s) {
                case ACTIVE -> anime() ? UiTheme.WHITE : cText();
                case LEARNED -> cText();
                case AVAILABLE -> cGold();
                case LOCKED -> cDim();
                case SEALED -> f.sin() == null ? cDim() : UiMath.mix(sinText(f.sin()), cDim(), 0.35f);
            };
            int tx = ix + 11;
            String shown = fit(f.name(), r.right() - tx - 7);
            g.drawString(font, shown, tx, y + 3, color, false);
            if (hov && !shown.equals(f.name())) tip = List.of(Component.literal(f.name()).getVisualOrderText());
            if (mastered.contains(f.id())) g.fill(r.right() - 9, y + 5, r.right() - 6, y + 8, anime() ? UiTheme.GOLD : UiDraw.GOLD_INK);
        }
        g.disableScissor();
        scrollbar(g, r, entries.size(), rows, listScroll);
    }

    /** The small mark before a form's name: filled when learned, hollow when not, a dot that breathes when it can be learned now, a bar across it when sealed. */
    private void statusMark(GuiGraphics g, int x, int y, HeroForm f, FormTree.Status s, float now) {
        int base = f.curse() && f.sin() != null ? sinColor(f.sin()) : switch (s) {
            case ACTIVE -> anime() ? UiTheme.CYAN : UiDraw.ACCENT;
            case LEARNED -> cLearned();
            case AVAILABLE -> cGold();
            default -> cDim();
        };
        switch (s) {
            case ACTIVE -> {
                g.fill(x, y, x + 7, y + 7, base);
                g.fill(x + 2, y + 2, x + 5, y + 5, 0xFFFFFFFF);
            }
            case LEARNED -> g.fill(x, y, x + 7, y + 7, base);
            case AVAILABLE -> {
                g.renderOutline(x, y, 7, 7, base);
                g.fill(x + 2, y + 2, x + 5, y + 5, UiMath.withAlpha(base, 0.35f + 0.65f * UiMath.pulse(now, 30f, 0f)));
            }
            case LOCKED -> g.renderOutline(x, y, 7, 7, UiMath.withAlpha(base, 0.8f));
            case SEALED -> {
                g.renderOutline(x, y, 7, 7, UiMath.withAlpha(base, 0.8f));
                g.fill(x + 1, y + 3, x + 6, y + 4, UiMath.withAlpha(base, 0.8f));
            }
        }
    }

    private void scrollbar(GuiGraphics g, BookLayout.Rect r, int total, int visible, int offset) {
        if (total <= visible) return;
        int trackH = r.h() - 4, barH = Math.max(10, trackH * visible / total);
        int by = r.y() + 2 + (trackH - barH) * offset / Math.max(1, total - visible);
        g.fill(r.right() - 4, r.y() + 2, r.right() - 2, r.bottom() - 2, UiMath.withAlpha(cDim(), 0.25f));
        g.fill(r.right() - 4, by, r.right() - 2, by + barH, UiMath.withAlpha(cAccent(), 0.85f));
    }

    // ---------------------------------------------------------------- the details pane
    private void drawPane(GuiGraphics g, int mx, int my, float now) {
        BookLayout.Rect r = lay.pane();
        well(g, r);
        feedSlots.clear();
        paneScroll = clamp(paneScroll, 0, maxPaneScroll());
        g.enableScissor(r.x() + 1, r.y() + 1, r.right() - 1, r.bottom() - 1);
        int y = r.y() + 4 - paneScroll;
        int x = r.x() + 6, w = r.w() - 12 - 6;
        for (Block b : blocks) {
            if (y + b.h() > r.y() && y < r.bottom()) b.draw().draw(g, x, y, w, mx, my);
            y += b.h() + 4;
        }
        g.disableScissor();
        scrollbar(g, r, contentHeight + 8, r.h(), paneScroll);
    }

    private boolean over(int mx, int my, int x, int y, int w, int h) { return lay.pane().contains(mx, my) && mx >= x && mx < x + w && my >= y && my < y + h; }

    private void add(int h, Draw d) { blocks.add(new Block(h, d)); }

    /** Lays out the chosen form's details as a stack of blocks, each with its height, so the pane can scroll them. */
    private void rebuildBlocks() {
        dirty = false;
        blocks.clear();
        HeroForm f = selectedForm();
        if (f == null) {
            contentHeight = 0;
            return;
        }
        int w = lay.pane().w() - 12 - 6, lh = font.lineHeight;
        FormTree.Status s = status(f);
        // title and badge
        String where = (f.curse() ? tr("hero.book.curse_form", "Curse form") + (f.sin() == null ? "" : " \u00b7 " + f.sin().label()) + " \u00b7 " : "") + tr("hero.book.tier", "Tier %s", f.tier());
        add(2 * lh + 4, (g, x, y, ww, mx, my) -> {
            String badge = badge(s);
            int bw = font.width(badge);
            g.drawString(font, Component.literal(fit(f.name(), ww - bw - 6)).withStyle(ChatFormatting.BOLD), x, y, cGold(), false);
            g.drawString(font, badge, x + ww - bw, y, badgeColor(s, f), false);
            g.drawString(font, fit(where, ww), x, y + lh + 2, cDim(), false);
        });
        // description
        List<FormattedCharSequence> desc = font.split(Component.literal(f.desc()), w);
        if (!desc.isEmpty()) add(desc.size() * (lh + 1), (g, x, y, ww, mx, my) -> {
            for (int i = 0; i < desc.size(); i++) g.drawString(font, desc.get(i), x, y + i * (lh + 1), cText(), false);
        });
        // what must be learned first
        List<String> missing = new ArrayList<>();
        for (String p : f.parents()) if (!learned.contains(p)) missing.add(nameOf(p));
        if (!missing.isEmpty() && s != FormTree.Status.ACTIVE && s != FormTree.Status.LEARNED) {
            List<FormattedCharSequence> need = font.split(Component.literal(tr("hero.book.needs_first", "Learn first: %s", String.join(", ", missing))), w);
            add(need.size() * (lh + 1), (g, x, y, ww, mx, my) -> {
                for (int i = 0; i < need.size(); i++) g.drawString(font, need.get(i), x, y + i * (lh + 1), cWarn(), false);
            });
        }
        if (s == FormTree.Status.SEALED) {
            List<FormattedCharSequence> sealed = font.split(Component.literal(tr("hero.book.sealed_note", "Sealed: this series opens only when the weapon offers its curse and you embrace it.")), w);
            add(sealed.size() * (lh + 1), (g, x, y, ww, mx, my) -> {
                for (int i = 0; i < sealed.size(); i++) g.drawString(font, sealed.get(i), x, y + i * (lh + 1), cWarn(), false);
            });
        }
        // requirements
        if (!f.requires().isEmpty()) {
            heading(tr("hero.book.requirements", "The gem must absorb"));
            for (HeroForm.Requirement q : f.requires()) add(20, (g, x, y, ww, mx, my) -> requirementRow(g, q, x, y, ww, mx, my));
        }
        // the inventory items that would feed the gem
        List<FeedItem> feed = learned.contains(f.id()) || s == FormTree.Status.SEALED ? List.of() : feedItems(f);
        if (!feed.isEmpty()) {
            heading(tr("hero.book.feed", "Feed the gem"));
            int perRow = Math.max(1, (w + 2) / 20), rowsNeeded = (feed.size() + perRow - 1) / perRow;
            add(rowsNeeded * 20, (g, x, y, ww, mx, my) -> {
                for (int i = 0; i < feed.size(); i++) feedSlot(g, feed.get(i), x + (i % perRow) * 20, y + (i / perRow) * 20, mx, my);
            });
            List<FormattedCharSequence> hint = font.split(Component.literal(tr("hero.book.feed_hint", "Click: feed what is needed. Shift-click: the whole stack. Sneak + use with it in the off hand also feeds.")), w);
            add(hint.size() * (lh + 1), (g, x, y, ww, mx, my) -> {
                for (int i = 0; i < hint.size(); i++) g.drawString(font, hint.get(i), x, y + i * (lh + 1), cDim(), false);
            });
        }
        // stats and the permanent bonus
        if (!f.stats().isEmpty()) {
            heading(tr("hero.book.stats", "While in use"));
            for (Map.Entry<String, Double> e : f.stats().entrySet()) add(lh + 2, line(statText(e.getKey(), e.getValue()), e.getValue() < 0 ? cWarn() : cText()));
        }
        if (!f.abilityBonus().isEmpty()) {
            heading(tr("hero.book.bonus", "Permanent, once mastered"));
            boolean got = mastered.contains(f.id());
            for (Map.Entry<String, Double> e : f.abilityBonus().entrySet()) add(lh + 2, line(statText(e.getKey(), e.getValue()), got ? cOk() : cDim()));
        }
        // techniques and passives
        if (!f.skills().isEmpty()) {
            heading(tr("hero.book.techniques", "Techniques"));
            for (Map.Entry<String, String> e : f.skills().entrySet()) add(lh + 3, (g, x, y, ww, mx, my) -> skillRow(g, e.getKey(), e.getValue(), x, y, ww, mx, my));
        }
        if (!f.passives().isEmpty()) {
            heading(tr("hero.book.passives", "Passives"));
            for (String id : f.passives()) add(lh + 3, (g, x, y, ww, mx, my) -> passiveRow(g, id, x, y, ww, mx, my));
        }
        // mastery
        if (f.mastery() > 0) {
            heading(tr("hero.book.mastery", "Mastery"));
            boolean can = learned.contains(f.id());
            int have = Math.min(f.mastery(), st.mastery().getOrDefault(f.id(), 0));
            add(lh + 8, (g, x, y, ww, mx, my) -> {
                String text = !can ? tr("hero.book.mastery_needed", "%s points once learned", f.mastery()) : mastered.contains(f.id()) ? tr("hero.book.mastered", "Mastered") : have + " / " + f.mastery();
                g.drawString(font, text, x, y, mastered.contains(f.id()) ? cOk() : cDim(), false);
                masteryBar.update(can ? have / (float) f.mastery() : 0f, UiTheme.now(), false);
                masteryBar.draw(g, x, y + lh + 1, ww, 4, mastered.contains(f.id()) ? (anime() ? UiTheme.GREEN : 0xFF2E7D4F) : (anime() ? UiTheme.VIOLET : UiDraw.ACCENT), UiTheme.now(), 1f);
            });
        }
        if (forms.size() == 1) {
            List<FormattedCharSequence> only = font.split(Component.literal(tr("hero.book.only_base", "No other forms are defined for this weapon yet.")), w);
            add(only.size() * (lh + 1), (g, x, y, ww, mx, my) -> {
                for (int i = 0; i < only.size(); i++) g.drawString(font, only.get(i), x, y + i * (lh + 1), cDim(), false);
            });
        }
        int total = 0;
        for (Block b : blocks) total += b.h() + 4;
        contentHeight = Math.max(0, total - 4);
        paneScroll = clamp(paneScroll, 0, maxPaneScroll());
    }

    private void heading(String text) {
        int h = font.lineHeight + 5;
        add(h, (g, x, y, ww, mx, my) -> UiDraw.header(g, font, text, x, y, ww));
    }

    private Draw line(String text, int color) { return (g, x, y, ww, mx, my) -> g.drawString(font, fit(text, ww), x + 4, y, color, false); }

    private String nameOf(String formId) {
        for (HeroForm f : forms) if (f.id().equals(formId)) return f.name();
        return FormText.pretty(formId);
    }

    private static String badge(FormTree.Status s) {
        return switch (s) {
            case ACTIVE -> tr("hero.book.badge.active", "IN USE");
            case LEARNED -> tr("hero.book.badge.learned", "LEARNED");
            case AVAILABLE -> tr("hero.book.badge.available", "READY TO LEARN");
            case LOCKED -> tr("hero.book.badge.locked", "LOCKED");
            case SEALED -> tr("hero.book.badge.sealed", "SEALED");
        };
    }

    private static int badgeColor(FormTree.Status s, HeroForm f) {
        return switch (s) {
            case ACTIVE -> anime() ? UiTheme.CYAN : UiDraw.ACCENT;
            case LEARNED -> cLearned();
            case AVAILABLE -> cGold();
            case LOCKED -> cDim();
            case SEALED -> f.sin() == null ? cWarn() : sinText(f.sin());
        };
    }

    private static String statText(String key, double value) {
        return tr("hero.stat." + key, FormText.statLabel(key)) + " " + FormText.statValue(key, value);
    }

    // ---------------------------------------------------------------- requirement rows and the feed strip
    private void requirementRow(GuiGraphics g, HeroForm.Requirement q, int x, int y, int w, int mx, int my) {
        int have = FormTree.progress(q, st.absorbed(), FormTags.MATCHER);
        boolean done = have >= q.count();
        ItemStack icon = iconFor(q);
        g.pose().pushPose();
        g.pose().translate(x, y, 0f);
        g.pose().scale(0.875f, 0.875f, 1f);
        if (!icon.isEmpty()) g.renderItem(icon, 0, 0);
        g.pose().popPose();
        String label = q.isTag() ? tr("hero.book.any", "Any %s", FormText.pretty(q.tag())) : icon.isEmpty() ? FormText.pretty(q.item()) : icon.getHoverName().getString();
        String count = have + "/" + q.count();
        int cw = font.width(count);
        g.drawString(font, fit(label, w - 18 - cw - 4), x + 18, y, done ? cOk() : cText(), false);
        g.drawString(font, count, x + w - cw, y, done ? cOk() : cDim(), false);
        UiBar.draw(g, x + 18, y + 11, w - 18, 4, have / (float) q.count(), done ? (anime() ? UiTheme.GREEN : 0xFF2E7D4F) : cAccent(), UiTheme.now(), 1f);
        if (over(mx, my, x, y, w, 18)) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.add(Component.literal(label).withStyle(ChatFormatting.GOLD).getVisualOrderText());
            if (q.isTag()) lines.add(Component.literal("#" + q.tag()).withStyle(ChatFormatting.DARK_GRAY).getVisualOrderText());
            lines.add(Component.literal(tr("hero.book.absorbed_of", "Absorbed: %s of %s", have, q.count())).getVisualOrderText());
            tip = lines;
        }
    }

    /** The item to draw for a requirement: the item itself, or for a tag one of its items, changing every second and a half. */
    private ItemStack iconFor(HeroForm.Requirement q) {
        String key = q.isTag() ? "#" + q.tag() : q.item();
        List<ItemStack> list = icons.computeIfAbsent(key, k -> {
            List<ItemStack> out = new ArrayList<>();
            ResourceLocation id = ResourceLocation.tryParse(q.isTag() ? q.tag() : q.item());
            if (id == null) return out;
            if (q.isTag()) {
                Optional<HolderSet.Named<Item>> set = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, id));
                if (set.isPresent()) for (Holder<Item> h : set.get()) {
                    if (out.size() >= 24) break;
                    out.add(new ItemStack(h.value()));
                }
            } else if (BuiltInRegistries.ITEM.containsKey(id)) out.add(new ItemStack(BuiltInRegistries.ITEM.get(id)));
            return out;
        });
        if (list.isEmpty()) return ItemStack.EMPTY;
        return list.get((int) (UiTheme.realNow() / 30f) % list.size());
    }

    private record FeedItem(String id, ItemStack sample, int count) {}

    /** The items in the player's inventory (and off hand) that one of this form's unmet requirements would take, one entry per kind with the total held. */
    private List<FeedItem> feedItems(HeroForm f) {
        if (minecraft == null || minecraft.player == null) return List.of();
        Map<String, ItemStack> sample = new LinkedHashMap<>();
        Map<String, Integer> count = new LinkedHashMap<>();
        Inventory inv = minecraft.player.getInventory();
        List<ItemStack> all = new ArrayList<>(inv.items);
        all.addAll(inv.offhand);
        for (ItemStack s : all) {
            if (s.isEmpty() || HeroItems.kindOf(s) != null) continue;
            String id = HeroAbsorb.idOf(s);
            if (!wanted(f, id)) continue;
            sample.putIfAbsent(id, s);
            count.merge(id, s.getCount(), Integer::sum);
        }
        List<FeedItem> out = new ArrayList<>();
        for (Map.Entry<String, ItemStack> e : sample.entrySet()) out.add(new FeedItem(e.getKey(), e.getValue(), count.get(e.getKey())));
        return out;
    }

    /** True when some still-unmet requirement of the form takes this item. */
    private boolean wanted(HeroForm f, String itemId) {
        for (HeroForm.Requirement q : f.requires()) if (FormTree.matches(q, itemId, FormTags.MATCHER) && !FormTree.met(q, st.absorbed(), FormTags.MATCHER)) return true;
        return false;
    }

    private void feedSlot(GuiGraphics g, FeedItem item, int x, int y, int mx, int my) {
        BookLayout.Rect rect = new BookLayout.Rect(x, y, 18, 18);
        boolean hov = over(mx, my, x, y, 18, 18);
        if (anime()) UiPanel.well(g, x, y, 18, 18, hov ? UiTheme.GOLD : UiTheme.CYAN, 1f);
        else {
            g.fill(x, y, x + 18, y + 18, UiDraw.PAPER_SHADE);
            g.renderOutline(x, y, 18, 18, hov ? UiDraw.GOLD_INK : UiDraw.RULE);
        }
        g.renderItem(item.sample(), x + 1, y + 1);
        g.renderItemDecorations(font, item.sample(), x + 1, y + 1, item.count() > 1 ? String.valueOf(item.count()) : null);
        feedSlots.add(new FeedSlot(rect, item.id()));
        if (hov) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.add(item.sample().getHoverName().copy().withStyle(ChatFormatting.GOLD).getVisualOrderText());
            lines.add(Component.literal(tr("hero.book.feed_click", "Click: feed what is needed")).getVisualOrderText());
            lines.add(Component.literal(tr("hero.book.feed_shift", "Shift-click: feed the whole stack")).withStyle(ChatFormatting.GRAY).getVisualOrderText());
            tip = lines;
        }
    }

    // ---------------------------------------------------------------- techniques and passives
    private void skillRow(GuiGraphics g, String slot, String id, int x, int y, int w, int mx, int my) {
        HeroSkill sk = HeroLogics.skill(id);
        String name = I18n.exists("hero.skill." + id + ".name") ? I18n.get("hero.skill." + id + ".name") : sk != null ? sk.name() : FormText.pretty(id.substring(id.indexOf('.') + 1));
        String slotText = "[" + tr("hero.slot." + slot, FormText.slotLabel(slot)) + "] ";
        g.drawString(font, slotText, x + 4, y, cDim(), false);
        int nx = x + 4 + font.width(slotText);
        g.drawString(font, fit(name, w - (nx - x) - 10), nx, y, sk != null && sk.ultimate() ? cGold() : cText(), false);
        if (sk != null && sk.ultimate()) g.drawString(font, "*", x + w - 6, y, cGold(), false);
        if (!over(mx, my, x, y - 1, w, font.lineHeight + 2)) return;
        String desc = I18n.exists("hero.skill." + id + ".desc") ? I18n.get("hero.skill." + id + ".desc") : sk != null ? sk.desc() : "";
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.literal(name).withStyle(ChatFormatting.GOLD).getVisualOrderText());
        if (!desc.isEmpty()) lines.addAll(font.split(Component.literal(desc), 200));
        if (sk != null) {
            String facts = factsOf(sk);
            if (!facts.isEmpty()) lines.add(Component.literal(facts).withStyle(ChatFormatting.GRAY).getVisualOrderText());
            if (sk.ultimate()) lines.add(Component.literal(tr("hero.book.ultimate", "Ultimate technique")).withStyle(ChatFormatting.GOLD).getVisualOrderText());
        }
        tip = lines;
    }

    /** "Cooldown 8 s, costs 2%": the skill answers the cooldown for a wielder, which the client has no context for, so a skill that cannot answer is left out. */
    private static String factsOf(HeroSkill sk) {
        StringBuilder b = new StringBuilder();
        try {
            int cd = sk.cooldownTicks(null);
            if (cd > 0) b.append(tr("hero.book.cooldown", "Cooldown %s", FormText.seconds(cd)));
        } catch (RuntimeException needsAWielder) {
            // some skills compute their cooldown from the wielder; the book only shows what it can know
        }
        if (sk.magoiShare() > 0) b.append(b.length() > 0 ? ", " : "").append(tr("hero.book.cost", "costs %s", FormText.percent(sk.magoiShare())));
        return b.toString();
    }

    private void passiveRow(GuiGraphics g, String id, int x, int y, int w, int mx, int my) {
        String key = "hero.passive." + (kind == null ? "" : kind.name().toLowerCase(java.util.Locale.ROOT)) + "." + id;
        String name = I18n.exists(key) ? I18n.get(key) : FormText.pretty(id);
        g.drawString(font, fit(name, w - 8), x + 4, y, cText(), false);
        if (I18n.exists(key + ".desc") && over(mx, my, x, y - 1, w, font.lineHeight + 2)) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.add(Component.literal(name).withStyle(ChatFormatting.GOLD).getVisualOrderText());
            lines.addAll(font.split(Component.literal(I18n.get(key + ".desc")), 200));
            tip = lines;
        }
    }
}
