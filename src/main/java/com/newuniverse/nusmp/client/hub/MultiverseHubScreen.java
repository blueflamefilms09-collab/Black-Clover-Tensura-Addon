package com.newuniverse.nusmp.client.hub;

import com.newuniverse.nusmp.client.elite.hub.HubClient;
import com.newuniverse.nusmp.client.heroweapon.forms.BookLayout.Rect;
import com.newuniverse.nusmp.client.multiverse.MultiverseStatusClient;
import com.newuniverse.nusmp.client.ui.CodexScreen;
import com.newuniverse.nusmp.client.ui.UiPoses;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.client.ui.theme.UiThemeOptions;
import com.newuniverse.nusmp.client.weaponpage.WeaponPageClient;
import io.github.manasmods.tensura.client.screen.AbilityCategoriesScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.function.ToIntFunction;

/**
 * Why: the Multiverse Hub (docs/ui_hub.md), the new menu that takes over from the old Codex book and the Multiverse Status screen: a ring of eight nodes round a turning core (the Ascensions-style hub, drawn from
 * scratch out of fills and the mod's own magic-circle textures), a boot-up intro that plays once per profile, and one page per node. The Weapons page is a native page (the Weapon page redrawn in the hub's style, over
 * the same data and the same one validated packet as the old Codex page), Status / Quests / Multiverse / Settings are native list pages, Skills / Attunement open the mod's existing screens with the hub as their parent so
 * Esc comes back here, and, since Hub v2, the Codex is a native page too: {@link HubCodexBody} draws every page of the old Codex book inside the hub, behind a left navigation rail grouped into categories.
 * Everything is client only and read only: the server decides every rule, the screen draws and asks. The stage is a fixed 400 x 240 ({@link HubLayout}); since Hub v2 it is a framed window scaled to {@code ui.hubSize}
 * percent of the screen (62 by default, 100 = the old full-screen fit), with the dimmed backdrop kept behind it and the background clipped to it. In the page view the eight nodes are a column of icons on the left
 * (the custom icons of {@link HubIcons}) and the Status page shows the player's own animated model ({@link HubPlayerPanel}).
 * <p>
 * Cost: nothing is drawn or kept while the screen is closed; a frame is at most two {@link com.newuniverse.nusmp.client.ui.theme.MagicCircle}s (8 textured quads each) plus a third for the hovered node, the eight icon
 * quads (16 with the glow of a hovered one), and about 250 flat fills on the ring (a page adds what is visible of it; the Status page one entity render and a floor circle); no per-frame allocation (fixed arrays, a pool
 * of {@link HubMotes#COUNT} motes, texts fitted once); no tick work beyond a 10-tick refresh of the lock facts. It draws nothing before {@code super.render} (the background is a flat scrim, so there is no blur to
 * draw under).
 */
public final class MultiverseHubScreen extends Screen {
    private static final int RING_COLOR = UiTheme.CYAN, DIM = UiTheme.TEXT_DIM;

    private final Screen parent;
    private HubNodes.Tab tab;                                   // null = the ring (HOME), else the open page (TAB)
    private HubLayout.View view = HubLayout.view(1, 1);

    private final HubMotes motes = new HubMotes(0xB10C0DEL, HubLayout.STAGE_W, HubLayout.STAGE_H);
    private final float[] hover = new float[HubNodes.COUNT];
    private final boolean[] locked = new boolean[HubNodes.COUNT];
    private HubText.Fit[] label = new HubText.Fit[HubNodes.COUNT], desc = new HubText.Fit[HubNodes.COUNT], lockHint = new HubText.Fit[HubNodes.COUNT];
    private HubText.Fit keys = new HubText.Fit("", "", false), backHint = new HubText.Fit("", "", false), pageTitle = new HubText.Fit("", "", false);
    private String core1 = "", core2 = "", hubName = "", codexName = "";

    private float startReal = Float.NaN, lastReal = Float.NaN, lastAge = -1f;
    private boolean introPlaying, introMarked;
    private float rippleAt = -1000f, rippleX, rippleY;
    private float tabAt;                                        // the clock when the page opened (its fade-in)

    private HubListBody list;                                   // the list page of the open tab (null for the Weapons and Codex pages)
    private final HubWeaponsBody weapons = new HubWeaponsBody();
    private final HubCodexBody codex = new HubCodexBody();
    private final HubPlayerPanel player = new HubPlayerPanel();
    private boolean weaponsPage, codexPage;
    private String startCodex;                                  // the Codex page to open on (the Wiki book asks for "Wiki")
    private boolean figureDrag, panelDrag;
    private final int[] clip = new int[4];                      // a scissor rectangle in screen units, reused every frame
    private float scroll, scrollTarget;
    private int ticks;
    private boolean weaponsAsked;

    private final HubListBody.Host host = new HubListBody.Host() {
        @Override public Screen screen() { return MultiverseHubScreen.this; }

        @Override public void open(Screen child) { minecraft.setScreen(child); }

        @Override public void openWatched(Screen child) { MultiverseHubClient.openAndReturn(child, MultiverseHubScreen.this); }

        @Override public void refresh() {
            view = HubLayout.view(width, height, UiThemeOptions.hubSize());                    // the window size may have just changed on the Settings page
            codex.setView(view);
            cookPage();
        }

        @Override public void codex(String startTitle) {
            startCodex = startTitle;
            select(HubNodes.Tab.CODEX);
        }
    };

    public MultiverseHubScreen(Screen parent) {
        super(Component.translatable("anime_multiverse.hub.title"));
        this.parent = parent;
        this.introPlaying = animated() && !UiThemeOptions.hubIntroSeen();
    }

    /** Opens the hub on the Codex page whose tab title is {@code title} (null = the first page); no intro. The Wiki book and the Multiverse rows come in through here ({@link MultiverseHubClient#openCodex}). */
    public MultiverseHubScreen startOnCodex(String title) {
        startCodex = title;
        tab = HubNodes.Tab.CODEX;
        tabAt = Float.NaN;
        introPlaying = false;
        return this;
    }

    private static boolean animated() { return UiTheme.motion() && UiThemeOptions.hubFx(); }

    // ---------------------------------------------------------------- setup
    @Override
    protected void init() {
        view = HubLayout.view(width, height, UiThemeOptions.hubSize());
        codex.setView(view);
        cookTexts();
        if (!weaponsAsked) {                                    // one open request for the weapon data (the server answers only when this client's copy is old)
            weaponsAsked = true;
            WeaponPageClient.opened();
            WeaponPageClient.ensureAsked();
        }
        facts();
        if (tab == HubNodes.Tab.CODEX && !codex.ready()) {
            codex.opened(font, startCodex);
            startCodex = null;
            cookTitle();
        }
        if (tab != null) cookPage();
    }

    private static String tr(String key, Object... args) { return I18n.get("anime_multiverse.hub." + key, args); }

    private void cookTexts() {
        ToIntFunction<String> w = font::width;
        for (int i = 0; i < HubNodes.COUNT; i++) {
            HubNodes.Tab t = HubNodes.at(i);
            label[i] = HubText.fit(tr("tab." + t.key), HubLayout.LABEL_MAX_W, w);
            desc[i] = HubText.fit(tr("tab." + t.key) + ": " + tr("desc." + t.key), HubLayout.LINE_W, w);
            lockHint[i] = HubText.fit(tr("tab." + t.key) + ": " + tr("locked." + t.key), HubLayout.LINE_W, w);
        }
        keys = HubText.fit(tr("footer"), HubLayout.LINE_W, w);
        backHint = HubText.fit(tr("back"), HubLayout.LINE_W, w);
        hubName = tr("title").toUpperCase(Locale.ROOT);
        core1 = tr("core.1");
        core2 = tr("core.2");
        codexName = tr("tab.codex");
        cookTitle();
    }

    /** The heading of the open page, fitted once per page (and per Codex page), never per frame. */
    private void cookTitle() {
        if (tab == null) return;
        String name = tr("tab." + tab.key);
        if (tab == HubNodes.Tab.CODEX && codex.ready()) name = codexName + ": " + codex.pageTitle(codex.page());
        pageTitle = HubText.fit(name, HubLayout.TITLE2.w(), font::width);
    }

    private HubNodes.Facts facts() {
        HubNodes.Facts f = new HubNodes.Facts(WeaponPageClient.received(), WeaponPageClient.pages().length, HubClient.hudEntries().size(), HubClient.lastMap() != null);
        for (int i = 0; i < HubNodes.COUNT; i++) locked[i] = HubNodes.locked(HubNodes.at(i), f);
        return f;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void onClose() { minecraft.setScreen(parent); }

    @Override
    public void removed() {
        markIntroSeen();
        player.release();
    }

    private void markIntroSeen() {
        if (introPlaying && !introMarked) {
            introMarked = true;
            UiThemeOptions.setFlag(UiThemeOptions.HUB_INTRO_SEEN, true);
        }
    }

    @Override
    public void tick() {
        ticks++;
        if (ticks % 10 == 0) {
            boolean wasLocked = tab != null && locked[tab.ordinal()];
            facts();
            if (tab != null && locked[tab.ordinal()] != wasLocked) cookPage();                // the server's answer arrived: the Weapons page unlocks
        }
        if (ticks % 20 == 0 && (tab == HubNodes.Tab.STATUS || tab == HubNodes.Tab.QUESTS) && !locked[tab.ordinal()]) cookPage();
    }

    // ---------------------------------------------------------------- pages
    private Rect body() { return HubLayout.body(tab); }

    private void select(HubNodes.Tab t) {
        if (t.launcher) {
            launch(t);
            return;
        }
        tab = t;
        tabAt = Float.NaN;
        scroll = scrollTarget = 0f;
        figureDrag = panelDrag = false;
        if (t == HubNodes.Tab.CODEX) {                          // the Codex takes its pages afresh each time it is opened (registries and the player may have changed)
            codex.opened(font, startCodex);
            startCodex = null;
        }
        cookTitle();
        cookPage();
    }

    private void selectCodexPage(int p) {
        codex.select(p);
        scroll = scrollTarget = 0f;
        figureDrag = false;
        cookTitle();
    }

    private void cookPage() {
        if (tab == null) return;
        int w = body().w();
        weaponsPage = false;
        codexPage = false;
        list = null;
        if (locked[tab.ordinal()]) {
            list = HubCards.locked(font, w, tab);
            return;
        }
        switch (tab) {
            case STATUS -> list = HubCards.status(font, w);
            case QUESTS -> list = HubCards.quests(font, w, host);
            case MULTIVERSE -> list = HubCards.multiverse(font, w, host);
            case SETTINGS -> list = HubCards.settings(font, w, host);
            case CODEX -> codexPage = true;
            case WEAPONS -> {
                weaponsPage = true;
                weapons.opened();
            }
            default -> list = HubCards.locked(font, w, tab);                                   // a tab appended later that this screen does not know yet: show its lock line
        }
    }

    private void launch(HubNodes.Tab t) {
        switch (t) {
            case SKILLS -> MultiverseHubClient.openAndReturn(new AbilityCategoriesScreen(AbilityCategoriesScreen.AbilityType.SKILL), this);
            case CODEX -> minecraft.setScreen(new CodexScreen(this));                          // not reached since Hub v2 (the Codex is a native page); kept for a tab that is flagged as a launcher again
            case ATTUNEMENT -> {
                // the server owns meditation: ask it with its own command; its packet opens the window (it has no parent, so Esc there returns to the game, not here)
                if (minecraft.player != null && minecraft.player.connection != null) {
                    minecraft.setScreen(null);
                    minecraft.player.connection.sendCommand("meditate");
                }
            }
            default -> { }
        }
    }

    private int contentHeight() {
        if (weaponsPage) return weapons.height(font, body().w());
        if (codexPage) return codex.contentHeight(body().w());
        return list == null ? 0 : list.height();
    }

    private int maxScroll() { return tab == null ? 0 : Math.max(0, contentHeight() - body().h()); }

    private boolean overviewOpen() { return codexPage && "overview".equals(codex.pageId()); }

    // ---------------------------------------------------------------- input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (introPlaying && skipIntro()) return true;
        float sx = view.sx(mx), sy = view.sy(my);
        if (animated()) {
            rippleAt = UiTheme.realNow();
            rippleX = sx;
            rippleY = sy;
        }
        if (button == 0) {
            boolean home = tab == null;
            int n = home ? HubLayout.nodeAt(true, sx, sy) : HubLayout.stripNodeAt(sx, sy);
            if (n >= 0) {
                click();
                select(HubNodes.at(n));
                return true;
            }
            if (!home && HubLayout.onBack(sx, sy)) {
                click();
                tab = null;
                return true;
            }
            if (!home) {
                if (tab == HubNodes.Tab.CODEX) {
                    int p = codex.pageAt(sx, sy);
                    if (p >= 0) {
                        click();
                        selectCodexPage(p);
                        return true;
                    }
                    if (HubLayout.SUBRAIL.contains(sx, sy)) return true;
                }
                if (tab == HubNodes.Tab.STATUS && !locked[tab.ordinal()] && HubLayout.PLAYER.contains(sx, sy)) {
                    player.click();
                    panelDrag = true;
                    click();
                    return true;
                }
                Rect body = body();
                if (body.contains(sx, sy)) {
                    boolean hit;
                    if (codexPage) {
                        hit = codex.click(sx, sy, button, body);
                        figureDrag = hit && overviewOpen();
                    } else {
                        double lx = sx - body.x(), ly = sy - body.y() + scroll;
                        hit = weaponsPage ? weapons.click(font, body.w(), lx, ly) : list != null && list.click(lx, ly);
                    }
                    if (hit) click();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (figureDrag) {
            codex.ctx().figureYaw += (float) (dx / view.scale()) * 1.6f;
            return true;
        }
        if (panelDrag) {
            player.drag(dx / view.scale());
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        figureDrag = false;
        panelDrag = false;
        player.release();
        return super.mouseReleased(mx, my, button);
    }

    private void click() { minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f)); }

    /** Jumps the intro to its end. Returns true (the click or key that did it is used up). */
    private boolean skipIntro() {
        float real = UiTheme.realNow();
        if (!Float.isNaN(startReal)) startReal = real - HubTimeline.INTRO;
        introDone();
        return true;
    }

    private void introDone() {
        markIntroSeen();
        introPlaying = false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (tab != null && sy != 0) {
            if (tab == HubNodes.Tab.CODEX && HubLayout.SUBRAIL.contains(view.sx(mx), view.sy(my))) {            // the wheel over the rail scrolls the rail, not the page
                codex.scrollRail(sy);
                return true;
            }
            scrollTarget = Mth.clamp(scrollTarget - (float) sy * 22f, 0f, maxScroll());
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key != GLFW.GLFW_KEY_ESCAPE && introPlaying && skipIntro()) return true;
        if ((!MultiverseHubClient.KEY.isUnbound() && MultiverseHubClient.KEY.matches(key, scan)) || (!MultiverseStatusClient.KEY.isUnbound() && MultiverseStatusClient.KEY.matches(key, scan))) {
            onClose();
            return true;
        }
        if (tab != null) {
            int max = maxScroll(), bodyH = body().h();
            switch (key) {
                case GLFW.GLFW_KEY_BACKSPACE -> { tab = null; return true; }
                case GLFW.GLFW_KEY_DOWN -> { scrollTarget = Mth.clamp(scrollTarget + 22f, 0f, max); return true; }
                case GLFW.GLFW_KEY_UP -> { scrollTarget = Mth.clamp(scrollTarget - 22f, 0f, max); return true; }
                case GLFW.GLFW_KEY_PAGE_DOWN -> { scrollTarget = Mth.clamp(scrollTarget + bodyH - 16, 0f, max); return true; }
                case GLFW.GLFW_KEY_PAGE_UP -> { scrollTarget = Mth.clamp(scrollTarget - bodyH + 16, 0f, max); return true; }
                case GLFW.GLFW_KEY_HOME -> { scrollTarget = 0f; return true; }
                case GLFW.GLFW_KEY_END -> { scrollTarget = max; return true; }
                case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> {
                    if (tab == HubNodes.Tab.CODEX) { codexStep(1); return true; }                  // in the Codex the arrows turn its pages, like the book did
                    if (key == GLFW.GLFW_KEY_RIGHT) { cycle(1); return true; }
                }
                case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A -> {
                    if (tab == HubNodes.Tab.CODEX) { codexStep(-1); return true; }
                    if (key == GLFW.GLFW_KEY_LEFT) { cycle(-1); return true; }
                }
                case GLFW.GLFW_KEY_R -> {
                    if (tab == HubNodes.Tab.STATUS || overviewOpen()) { UiPoses.roll(); return true; }
                }
                default -> { }
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    private void codexStep(int dir) {
        codex.step(dir);
        scroll = scrollTarget = 0f;
        cookTitle();
    }

    /** Moves to the next (or previous) page that is shown inside the hub, skipping the launchers. */
    private void cycle(int dir) {
        int n = tab.ordinal();
        for (int k = 0; k < HubNodes.COUNT; k++) {
            n = Math.floorMod(n + dir, HubNodes.COUNT);
            if (!HubNodes.at(n).launcher) {
                select(HubNodes.at(n));
                return;
            }
        }
    }

    // ---------------------------------------------------------------- drawing
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        UiPanel.scrim(g, width, height, 1f);                                           // flat: no blur to draw under, nothing is drawn before super.render. The dimmed backdrop stays behind the window.
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        boolean fx = animated();
        float real = UiTheme.realNow();
        if (Float.isNaN(startReal)) startReal = real;
        float dt = Float.isNaN(lastReal) ? 0f : Math.min(3f, real - lastReal);
        lastReal = real;
        float age = fx ? real - startReal : 1e6f;
        if (introPlaying && age >= HubTimeline.INTRO) introDone();
        float introAge = introPlaying ? age : 1e6f;
        float openA = !fx || introPlaying ? 1f : HubTimeline.open(age);
        float now = fx ? real : 40f;
        if (introPlaying) {
            if (HubTimeline.crossed(lastAge, age, HubTimeline.SOUND_A)) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f));
            if (HubTimeline.crossed(lastAge, age, HubTimeline.SOUND_B)) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_ACTIVATE, 1.9f));
        }
        lastAge = age;
        if (Float.isNaN(tabAt)) tabAt = real;

        float sx = view.sx(mx), sy = view.sy(my);
        boolean home = tab == null;
        int over = introPlaying ? -1 : home ? HubLayout.nodeAt(true, sx, sy) : HubLayout.stripNodeAt(sx, sy);
        for (int i = 0; i < HubNodes.COUNT; i++) {
            float target = i == over ? 1f : 0f;
            hover[i] = fx ? UiMath.approach(hover[i], target, 0.3f, dt, 0.01f) : target;
        }
        if (fx) motes.step(dt);
        if (!home && (tab == HubNodes.Tab.STATUS || overviewOpen())) player.update(dt, fx, now);           // the pose cycle only runs while a figure is on screen
        scroll += (scrollTarget - scroll) * 0.35f;
        if (Math.abs(scrollTarget - scroll) < 0.3f) scroll = scrollTarget;
        scrollTarget = Mth.clamp(scrollTarget, 0f, maxScroll());

        String tip = null;
        g.pose().pushPose();
        g.pose().translate(view.ox(), view.oy(), 0f);
        g.pose().scale(view.scale(), view.scale(), 1f);
        HubFx.window(g, openA);                                                         // Hub v2: the frame of the window
        g.enableScissor(view.wx(0f), view.wy(0f), view.wx(HubLayout.STAGE_W), view.wy(HubLayout.STAGE_H));   // the background never leaves the window
        if (fx) HubFx.motes(g, motes, now, RING_COLOR, 0.8f * openA * HubTimeline.rings(introAge));
        if (home) drawHome(g, now, introAge, openA, over, fx);
        else tip = drawTab(g, now, openA, over, sx, sy, fx);
        if (fx) {
            HubFx.ripple(g, rippleX, rippleY, HubTimeline.ripple(real - rippleAt), tab == null ? RING_COLOR : 0xFF000000 | tab.rgb);
            if (!introPlaying) HubFx.sweep(g, HubTimeline.sweep(age), RING_COLOR);
        }
        g.disableScissor();
        g.pose().popPose();
        if (tip != null) g.renderTooltip(font, Component.literal(tip), mx, my);
    }

    private void drawHome(GuiGraphics g, float now, float introAge, float openA, int over, boolean fx) {
        int cx = HubLayout.HOME_CX, cy = HubLayout.HOME_CY;
        float rings = HubTimeline.rings(introAge), labels = HubTimeline.labels(introAge) * openA;
        HubFx.backdrop(g, cx, cy, 100f, now, openA, Math.max(0.01f, rings));
        HubFx.ringA(g, cx, cy, HubLayout.HOME_R, now * 0.003f, RING_COLOR, 0.5f * openA, rings);
        HubFx.ringB(g, cx, cy, HubLayout.HOME_R - 24, -now * 0.004f, UiTheme.VIOLET, 0.35f * openA, rings);
        for (int i = 0; i < HubNodes.COUNT; i++) {
            float nx = HubLayout.nodeX(true, i), ny = HubLayout.nodeY(true, i), dx = nx - cx, dy = ny - cy, len = (float) Math.sqrt(dx * dx + dy * dy);
            float ux = dx / len, uy = dy / len;
            HubFx.spoke(g, cx + ux * 34f, cy + uy * 34f, nx - ux * (HubLayout.HOME_NODE + 3), ny - uy * (HubLayout.HOME_NODE + 3), 0xFF000000 | HubNodes.at(i).rgb, openA, HubTimeline.node(introAge, i));
        }
        HubFx.core(g, cx, cy, 52f, now, UiTheme.VIOLET, openA, Math.max(0.01f, HubTimeline.core(introAge)));
        if (labels > 0.05f) {
            HubFx.octagon(g, cx, cy, 24, UiMath.withAlpha(0x0A1024, 0.6f * labels));
            g.drawCenteredString(font, core1, cx, cy - 10, UiMath.withAlpha(UiTheme.GOLD, labels));
            g.drawCenteredString(font, core2, cx, cy + 1, UiMath.withAlpha(UiTheme.CYAN, labels));
        }
        for (int i = 0; i < HubNodes.COUNT; i++) {
            float nx = HubLayout.nodeX(true, i), ny = HubLayout.nodeY(true, i);
            if (fx) HubFx.flare(g, nx, ny, HubLayout.HOME_NODE * 1.9f, now, 0xFF000000 | HubNodes.at(i).rgb, hover[i], openA);
            HubFx.node(g, font, Math.round(nx), Math.round(ny), HubLayout.HOME_NODE, HubNodes.at(i), HubTimeline.node(introAge, i), hover[i], false, locked[i], openA);
            if (labels > 0.05f) {
                Rect l = HubLayout.label(i, font.width(label[i].shown()));
                int color = UiMath.mix(locked[i] ? UiTheme.TEXT_FAINT : UiTheme.TEXT, UiTheme.GOLD, hover[i]);
                g.drawString(font, label[i].shown(), l.x(), l.y(), UiMath.withAlpha(color, labels), false);
            }
        }
        if (labels > 0.05f) {
            Rect line = HubLayout.line();
            HubText.Fit t = over >= 0 ? (locked[over] ? lockHint[over] : desc[over]) : keys;
            g.drawCenteredString(font, t.shown(), line.x() + line.w() / 2, line.y(), UiMath.withAlpha(over >= 0 ? UiTheme.TEXT : DIM, labels));
        }
    }

    /** Draws the TAB view; returns the tooltip text (a cut text under the pointer) or null. */
    private String drawTab(GuiGraphics g, float now, float openA, int over, float sx, float sy, boolean fx) {
        HubFx.backdrop(g, 220f, 122f, 112f, now, openA * 0.7f, 1f);                     // the quiet circle of the ring view, behind the page
        // ---- the strip: a faint line behind the nodes, the back button above them, the eight icon nodes
        int top = Math.round(HubLayout.stripY(0)), bottom = Math.round(HubLayout.stripY(HubNodes.COUNT - 1));
        g.fill(HubLayout.STRIP_CX, top, HubLayout.STRIP_CX + 1, bottom, UiMath.withAlpha(RING_COLOR, 0.16f * openA));
        boolean onBack = HubLayout.onBack(sx, sy);
        HubFx.octagon(g, HubLayout.BACK_CX, HubLayout.BACK_CY, HubLayout.BACK_R, UiMath.withAlpha(onBack ? 0x1A2A55 : 0x0A1024, 0.8f * openA));
        HubFx.octagonEdge(g, HubLayout.BACK_CX, HubLayout.BACK_CY, HubLayout.BACK_R, UiMath.withAlpha(UiTheme.GOLD, (onBack ? 0.95f : 0.55f) * openA));
        HubFx.chevron(g, HubLayout.BACK_CX, HubLayout.BACK_CY, UiMath.withAlpha(onBack ? UiTheme.WHITE : UiTheme.GOLD, openA));
        for (int i = 0; i < HubNodes.COUNT; i++) {
            float nx = HubLayout.stripX(i), ny = HubLayout.stripY(i);
            if (fx) HubFx.flare(g, nx, ny, HubLayout.STRIP_NODE * 1.9f, now, 0xFF000000 | HubNodes.at(i).rgb, hover[i], openA);
            HubFx.node(g, font, Math.round(nx), Math.round(ny), HubLayout.STRIP_NODE, HubNodes.at(i), 1f, hover[i], i == tab.ordinal(), locked[i], openA);
        }
        g.drawString(font, hubName, HubLayout.NAME_X, HubLayout.NAME_Y, UiMath.withAlpha(UiTheme.GOLD, 0.8f * openA), false);
        // ---- the page panel
        float pageA = fx ? HubTimeline.open(UiTheme.realNow() - tabAt) : 1f;
        float a = openA * pageA;
        Rect p = HubLayout.PANEL2;
        UiPanel.panelScan(g, p.x(), p.y(), p.w(), p.h(), 0xFF000000 | tab.rgb, 0.7f, a, now);
        UiPanel.heading(g, font, pageTitle.shown(), HubLayout.TITLE2.x(), HubLayout.TITLE2.y(), HubLayout.TITLE2.w(), tab.rgb);
        Rect body = body();
        String tip = null;
        if (tab == HubNodes.Tab.CODEX && a > 0.02f) {
            Rect r = HubLayout.SUBRAIL;
            clip[0] = view.wx(r.x() - 2);
            clip[1] = view.wy(r.y() - 2);
            clip[2] = view.wx(r.right() + 2);
            clip[3] = view.wy(r.bottom() + 2);
            codex.drawRail(g, font, clip, sx, sy, a);
            g.fill(body.x() - 4, body.y(), body.x() - 3, body.bottom(), UiMath.withAlpha(tab.rgb, 0.25f * a));
            tip = codex.tipAt(sx, sy);
        }
        if (tab == HubNodes.Tab.STATUS && !locked[tab.ordinal()] && a > 0.02f) player.draw(g, font, HubLayout.PLAYER, sx, sy, now, fx, a);
        int lx = Math.round(sx - body.x()), ly = Math.round(sy - body.y() + scroll);
        boolean inBody = body.contains(sx, sy);
        g.enableScissor(view.wx(body.x()), view.wy(body.y()), view.wx(body.right()), view.wy(body.bottom()));
        if (codexPage) {
            if (a > 0.02f) codex.drawBody(g, font, body, Math.round(scroll), sx, sy, now);                // the real pointer always: the page is the live one
        } else {
            g.pose().pushPose();
            g.pose().translate(body.x(), body.y() - Math.round(scroll), 0f);
            int from = Math.round(scroll), to = from + body.h();
            if (a > 0.02f) {
                if (weaponsPage) weapons.draw(g, font, body.w(), from, to, inBody ? lx : -1000, inBody ? ly : -1000, now);
                else if (list != null) list.draw(g, font, from, to, inBody ? lx : -1000, inBody ? ly : -1000, now);
            }
            g.pose().popPose();
        }
        g.disableScissor();
        if (inBody && !codexPage) tip = weaponsPage ? weapons.tipAt(font, body.w(), lx, ly) : list == null ? null : list.tipAt(lx, ly);
        int max = maxScroll();
        if (max > 0) {
            int sbx = body.right() + HubLayout.SCROLLBAR2_GAP, bar = Math.max(12, body.h() * body.h() / (max + body.h())), pos = (int) ((body.h() - bar) * (scroll / max));
            g.fill(sbx, body.y(), sbx + HubLayout.SCROLLBAR2_W, body.bottom(), UiMath.withAlpha(0xFFFFFF, 0.08f));
            g.fill(sbx, body.y() + pos, sbx + HubLayout.SCROLLBAR2_W, body.y() + pos + bar, UiMath.withAlpha(tab.rgb, 0.85f));
        }
        Rect line = HubLayout.line();
        HubText.Fit t = over >= 0 ? (locked[over] ? lockHint[over] : desc[over]) : onBack ? backHint : keys;
        g.drawCenteredString(font, t.shown(), line.x() + line.w() / 2, line.y(), UiMath.withAlpha(over >= 0 || onBack ? UiTheme.TEXT : DIM, openA));
        return tip;
    }
}
