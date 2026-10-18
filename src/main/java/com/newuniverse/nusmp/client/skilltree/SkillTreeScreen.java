package com.newuniverse.nusmp.client.skilltree;

import com.newuniverse.nusmp.client.skilltree.layout.ClickTracker;
import com.newuniverse.nusmp.client.skilltree.layout.NodeState;
import com.newuniverse.nusmp.client.skilltree.layout.PurchaseGate;
import com.newuniverse.nusmp.client.skilltree.layout.Rect;
import com.newuniverse.nusmp.client.skilltree.layout.RouteGrouping;
import com.newuniverse.nusmp.client.skilltree.layout.ScreenLayout;
import com.newuniverse.nusmp.client.skilltree.layout.TreeText;
import com.newuniverse.nusmp.client.ui.theme.MagicCircle;
import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.skilltree.PlayerTreeState;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.rules.TreeRules;
import com.newuniverse.nusmp.skilltree.rules.TreeRules.ClassVerdict;
import com.newuniverse.nusmp.skilltree.rules.TreeRules.NodeStatus;
import com.newuniverse.nusmp.skilltree.rules.TreeState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: the owner wants a skill tree the player reaches from the menu they already use, that grows by data (a new magic is a JSON file) and that can never disagree with the server.
 * So this screen is a pure CLIENT view: it is built in {@code ScreenEvent.Init.Post} by {@link MenuInjection} and opened with the host menu as its parent (Escape or Close returns
 * there), it owns no game state, and it decides nothing. Every number it shows is read each frame from the two synced copies, the player's tree ({@code SkillTreeApi.of(player)}, an
 * attachment the core's sync packet overwrites) and the route registry (the core's route packet), so a purchase, an unlock, an SP Book or a datapack reload changes the picture in
 * place on the next frame and no refresh or reply packet is needed. The only things it ever sends are two requests, buy a level of a node and make a class active (see
 * {@link TreeActions}); the server re-checks them, so the screen only PREDICTS the server's rule, by asking the very same pure rule the server runs ({@link NodeState} wraps the core's {@code TreeRules}),
 * to avoid offering a click that would be refused, and when the server does refuse (nothing changes within a moment) it says so in the footer. The layout is a pure function of the window size ({@link ScreenLayout}) so a resize cannot leave
 * anything stale, and the drawing is split by what it draws (route list, canvas, detail card) so each part can be read, tested and previewed on its own. Pan and zoom are remembered
 * per route across openings, in static maps that are cleared when the player leaves the world.
 * <p>
 * Controls: click a route to show it; click a node to select it; double-click it, press Enter or Space, or press Buy to buy one level; drag the canvas to pan; the wheel scrolls,
 * Shift+wheel scrolls sideways, Ctrl+wheel zooms; arrow keys pan; plus and minus zoom; F or Home fits the whole tree; Escape closes.
 */
public class SkillTreeScreen extends Screen {
    // ------------------------------------------------------------------ remembered between openings (client only; cleared on logout)
    private static String lastRoute = "";
    private static final Map<String, NodeCanvas.View> VIEWS = new HashMap<>();
    private static final Map<String, String> LAST_NODE = new HashMap<>();

    /** Forgets the remembered route, views and icons (the player left the world). */
    static void forget() {
        lastRoute = "";
        VIEWS.clear();
        LAST_NODE.clear();
        ItemIcons.clear();
    }

    private static final long NOTICE_MS = 3500;
    private static final int KEY_PAN = 24;

    private final Screen parent;
    private ScreenLayout lay;
    private ThemedButton closeBtn, activeBtn, buyBtn;

    private final RouteList list = new RouteList();
    private final DetailPanel detail = new DetailPanel();
    private final PurchaseGate gate = new PurchaseGate();
    private final ClickTracker clicks = new ClickTracker();
    private final TreeSounds sounds = new TreeSounds();
    private final Map<String, RouteShape> shapes = new HashMap<>();
    private final Map<String, int[]> levelSnapshots = new HashMap<>();

    // the model of the current frame, rebuilt by refresh()
    private TreeState player;
    private List<SkillRoute> routes = List.of();
    private String routeId = "", beganRoute = "", selNodeId = "";
    private SkillRoute sel;
    private RouteShape shape;
    private NodeCanvas.View view;
    private boolean unlocked, revealPending;
    private int sp;
    private NodeState[] states = new NodeState[0];
    private int selNode = -1, hoverNode = -1;
    private float now;
    private long nowMs;

    // input and requests in flight
    private boolean dragging, dragMoved;
    private double pressX, pressY;
    private int pendingNode = -1;
    private String pendingClass = "";
    private long pendingClassAt;
    private String notice = "";
    private int noticeColor;
    private long noticeUntil;

    public SkillTreeScreen(Screen parent) {
        super(SkillTreeLang.tr(SkillTreeLang.TITLE));
        this.parent = parent;
        this.routeId = lastRoute;
    }

    @Override
    protected void init() {
        lay = ScreenLayout.of(width, height);
        // Close is added last: a keyboard player's first Tab (and the initial focus) must not land on the button that leaves the screen
        buyBtn = addRenderableWidget(button(lay.buyButton, SkillTreeLang.tr(SkillTreeLang.BUY), b -> buySelected()));
        activeBtn = addRenderableWidget(button(lay.setActiveButton, SkillTreeLang.tr(SkillTreeLang.SET_ACTIVE), b -> setActive()));
        closeBtn = addRenderableWidget(button(lay.closeButton, SkillTreeLang.tr(SkillTreeLang.CLOSE), b -> onClose()));
        buyBtn.visible = false;                                          // shown by the first frame's refresh, once it knows whether a node is selected
        activeBtn.visible = false;
    }

    private static ThemedButton button(Rect r, Component label, Button.OnPress press) { return new ThemedButton(r.x(), r.y(), r.w(), r.h(), label, press); }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void tick() { sounds.tick(); }

    // ------------------------------------------------------------------ the model of a frame
    private void refresh(int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        now = UiTheme.now();
        nowMs = System.nanoTime() / 1_000_000L;
        player = mc.player == null ? null : new PlayerTreeState(SkillTreeApi.of(mc.player));       // a live view of the synced tree: nothing is copied, every frame reads the newest
        List<SkillRoute> all = RouteRegistry.all();
        routes = all == null ? List.of() : all;
        list.build(RouteGrouping.group(routes), font, lay.routeList.w());
        if (player == null || routes.isEmpty()) {                        // nothing to show: the registry has not arrived (or there are no trees)
            sel = null; shape = null; view = null; states = new NodeState[0]; selNode = -1; hoverNode = -1; unlocked = false; sp = 0;
            syncButtons();
            return;
        }
        sp = player.sp();
        SkillRoute chosen = RouteRegistry.byId(routeId);
        if (chosen == null) chosen = defaultRoute(routes, player);
        routeId = chosen.id();
        sel = chosen;
        if (!sel.id().equals(beganRoute)) beginRoute();
        unlocked = player.hasRoute(sel.id());
        shape = shapes.get(sel.id());
        if (shape == null || shape.route != sel) {                       // a new copy of the route arrived: lay it out again
            shape = RouteShape.of(sel);
            shapes.put(sel.id(), shape);
        }
        view = VIEWS.computeIfAbsent(sel.id(), k -> new NodeCanvas.View());
        if (!view.placed) NodeCanvas.place(view, shape.layout, lay.canvas);
        else NodeCanvas.clamp(view, shape.layout, lay.canvas);
        int n = sel.nodes().size();
        states = new NodeState[n];
        selNode = -1;
        for (int i = 0; i < n; i++) {
            SkillNode node = sel.nodes().get(i);
            states[i] = NodeState.of(player, sel, node);
            if (node.id().equals(selNodeId)) selNode = i;
        }
        if (!unlocked) selNode = -1;                                     // nodes of a sealed route cannot be selected
        detectChanges();
        boolean overChip = lay.zoomOut.contains(mx, my) || lay.zoomIn.contains(mx, my) || lay.zoomFit.contains(mx, my);
        hoverNode = overChip ? -1 : NodeCanvas.nodeAt(mx, my, lay.canvas, view, shape.layout);
        if (revealPending) {
            list.reveal(sel, lay.routeList.h());
            revealPending = false;
        }
        syncButtons();
    }

    /** The route to show when none was chosen: the active class's route, else the first unlocked one, else the first. */
    private static SkillRoute defaultRoute(List<SkillRoute> routes, TreeState player) {
        if (!player.activeClass().isEmpty()) {
            SkillRoute byClass = RouteRegistry.byClass(player.activeClass());
            if (byClass != null && player.hasRoute(byClass.id())) return byClass;
        }
        for (SkillRoute r : routes) if (player.hasRoute(r.id())) return r;
        return routes.get(0);
    }

    /** Called once when a route becomes the shown one: restores its selected node and forgets requests that belonged to the previous one. */
    private void beginRoute() {
        beganRoute = sel.id();
        lastRoute = sel.id();
        selNodeId = LAST_NODE.getOrDefault(sel.id(), "");
        gate.reset();
        pendingNode = -1;
        levelSnapshots.remove(sel.id());                                 // the first frame of a route is a baseline: no chime for what was already bought
        revealPending = true;
    }

    /** Compares this frame's levels with the last frame's: a rise is a purchase that went through (chime, pulse, notice); also settles the requests in flight. */
    private void detectChanges() {
        int[] prev = levelSnapshots.get(sel.id());
        if (prev == null || prev.length != states.length) {
            prev = new int[states.length];
            for (int i = 0; i < prev.length; i++) prev[i] = states[i].level();
            levelSnapshots.put(sel.id(), prev);
        } else {
            for (int i = 0; i < prev.length; i++) {
                int lv = states[i].level();
                if (lv > prev[i]) onBought(i, prev[i], lv);
                prev[i] = lv;
            }
        }
        if (pendingNode >= 0) {
            if (pendingNode < states.length && gate.observe(states[pendingNode].level())) pendingNode = -1;
            else if (gate.refused(nowMs)) {
                pendingNode = -1;
                setNotice(SkillTreeLang.str(SkillTreeLang.NOTICE_DENIED), TreeSkin.bad());
            } else if (!gate.isPending(nowMs)) pendingNode = -1;
        }
        if (!pendingClass.isEmpty()) {
            if (pendingClass.equals(player.activeClass())) {
                pendingClass = "";
                setNotice(SkillTreeLang.str(SkillTreeLang.NOTICE_CLASS_SET, sel.name()), TreeSkin.good());
            } else if (nowMs - pendingClassAt > PurchaseGate.TIMEOUT_MS) {
                pendingClass = "";
                setNotice(SkillTreeLang.str(SkillTreeLang.NOTICE_CLASS_DENIED), TreeSkin.bad());
            }
        }
    }

    private void onBought(int node, int before, int level) {
        NodeCanvas.bought(view, node, before, now);
        sounds.chime();
        setNotice(SkillTreeLang.str(SkillTreeLang.NOTICE_BOUGHT, sel.nodes().get(node).name(), level), TreeSkin.good());
    }

    private void setNotice(String text, int color) {
        notice = text;
        noticeColor = color;
        noticeUntil = nowMs + NOTICE_MS;
    }

    // ------------------------------------------------------------------ the buttons
    private void syncButtons() {
        closeBtn.visible = true;
        boolean classButton = sel != null && unlocked && !sel.classId().isEmpty();
        activeBtn.visible = classButton;
        if (classButton) {
            ClassVerdict verdict = classVerdict();
            activeBtn.active = SkillTreeApi.isEnabled() && verdict == ClassVerdict.OK && pendingClass.isEmpty();
            activeBtn.setMessage(switch (verdict) {
                case SAME -> SkillTreeLang.tr(SkillTreeLang.SET_ACTIVE_NOW);
                case COOLDOWN -> SkillTreeLang.tr(SkillTreeLang.SET_ACTIVE_WAIT, TreeText.secondsUp(SkillTreeApi.classCooldownTicks(Minecraft.getInstance().player)));
                default -> SkillTreeLang.tr(SkillTreeLang.SET_ACTIVE);
            });
        }
        boolean buy = sel != null && unlocked && selNode >= 0;
        buyBtn.visible = buy;
        if (buy) {
            NodeState st = states[selNode];
            boolean waiting = gate.isPending(nowMs);
            buyBtn.active = SkillTreeApi.isEnabled() && st.canBuy() && !waiting;
            buyBtn.setMessage(waiting ? SkillTreeLang.tr(SkillTreeLang.BUY_WAIT) : buyLabel(st));
        }
    }

    /**
     * The server's own verdict on making the selected route's class active, asked of the same rule with the same inputs the server uses (the game time, the cooldown from the synced
     * server config, creative players skip the wait), so the button is disabled exactly while the server would refuse.
     */
    private ClassVerdict classVerdict() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return ClassVerdict.NOT_UNLOCKED;
        return TreeRules.classSwitch(player, sel, mc.level.getGameTime(), SkillTreeApi.classSwitchSeconds() * 20L, mc.player.isCreative());
    }

    /** The Buy button's words for a node: the price when it can be bought, otherwise the first rule that says no (the core's verdict). */
    private MutableComponent buyLabel(NodeState st) {
        return switch (st.verdict()) {
            case OK -> SkillTreeLang.tr(SkillTreeLang.BUY_PRICE, st.price());
            case MAXED -> SkillTreeLang.tr(SkillTreeLang.BUY_MAXED);
            case NO_ROUTE -> SkillTreeLang.tr(SkillTreeLang.BUY_SEALED);
            case NO_SP -> SkillTreeLang.tr(SkillTreeLang.BUY_NEED_SP, st.price());
            case PARENTS, UNKNOWN_NODE -> SkillTreeLang.tr(SkillTreeLang.BUY_LOCKED);
        };
    }

    private void setActive() {
        if (sel == null || !unlocked || sel.classId().isEmpty() || !pendingClass.isEmpty() || !SkillTreeApi.isEnabled() || classVerdict() != ClassVerdict.OK) return;
        pendingClass = sel.classId();
        pendingClassAt = nowMs;
        TreeActions.setClass(sel.classId());
    }

    private void buySelected() { if (selNode >= 0) tryBuy(selNode); }

    /** Sends a buy request for one level of node {@code idx} when the prediction says it will be accepted and no earlier request is still in flight. */
    private void tryBuy(int idx) {
        if (sel == null || idx < 0 || idx >= states.length || !SkillTreeApi.isEnabled()) return;
        NodeState st = states[idx];
        if (!st.canBuy()) {                                              // say why, with the reason the server would give
            setNotice(SkillTreeLang.str(switch (st.verdict()) {
                case MAXED -> SkillTreeLang.NODE_MAXED;
                case NO_ROUTE -> SkillTreeLang.NODE_SEALED;
                case NO_SP -> SkillTreeLang.NODE_NO_SP;
                default -> SkillTreeLang.NODE_PARENTS;
            }), TreeSkin.bad());
            return;
        }
        if (!gate.tryBegin(nowMs, st.level())) return;
        pendingNode = idx;
        TreeActions.buy(sel.id(), sel.nodes().get(idx).id());
    }

    private void selectNode(int idx) {
        if (sel == null || idx < 0 || idx >= states.length || !unlocked) return;
        String id = sel.nodes().get(idx).id();
        if (!id.equals(selNodeId)) {
            selNodeId = id;
            LAST_NODE.put(sel.id(), id);
            gate.reset();
            pendingNode = -1;
            TreeSounds.click();
        }
    }

    private void selectRoute(SkillRoute r) {
        if (r == null || r.id().equals(routeId)) return;
        routeId = r.id();
        TreeSounds.click();
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button == 0 || button == 2) {
            if (button == 0) {
                if (sel != null && lay.zoomOut.contains(mx, my)) { NodeCanvas.step(view, shape.layout, lay.canvas, false); return true; }
                if (sel != null && lay.zoomIn.contains(mx, my)) { NodeCanvas.step(view, shape.layout, lay.canvas, true); return true; }
                if (sel != null && lay.zoomFit.contains(mx, my)) { NodeCanvas.fit(view, shape.layout, lay.canvas); return true; }
                SkillRoute r = list.routeAt(mx, my, lay.routeList);
                if (r != null) { setFocused(null); selectRoute(r); return true; }
            }
            if (sel != null && lay.canvas.contains(mx, my)) {
                setFocused(null);                                        // Enter and Space then buy the selected node instead of re-pressing the last button
                dragging = true;
                dragMoved = false;
                pressX = mx;
                pressY = my;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging && sel != null && (button == 0 || button == 2)) {
            if (Math.abs(mx - pressX) > 3 || Math.abs(my - pressY) > 3) dragMoved = true;
            view.panX += (float) dx;
            view.panY += (float) dy;
            NodeCanvas.clamp(view, shape.layout, lay.canvas);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging && (button == 0 || button == 2)) {
            dragging = false;
            if (button == 0 && !dragMoved && sel != null) {
                int idx = NodeCanvas.nodeAt(mx, my, lay.canvas, view, shape.layout);
                boolean second = clicks.click(nowMs, idx);
                if (idx >= 0) {
                    boolean wasSelected = idx == selNode;
                    selectNode(idx);
                    if (second && wasSelected) tryBuy(idx);
                }
            }
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (sel != null && lay.canvas.contains(mx, my)) {
            if (hasControlDown()) {
                NodeCanvas.zoomBy(view, shape.layout, lay.canvas, sy > 0 ? 1.15f : 1f / 1.15f, mx, my);
            } else if (hasShiftDown() || (sx != 0 && sy == 0)) {
                view.panX += (float) (sy != 0 ? sy : sx) * KEY_PAN;
                NodeCanvas.clamp(view, shape.layout, lay.canvas);
            } else {
                view.panY += (float) sy * KEY_PAN;
                NodeCanvas.clamp(view, shape.layout, lay.canvas);
            }
            return true;
        }
        if (lay.routes.contains(mx, my)) { list.scrollBy((int) (-sy * 18), lay.routeList.h()); return true; }
        if (lay.detail.contains(mx, my)) { detail.scrollBy((int) (-sy * 18), lay.detailText.h()); return true; }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) return super.keyPressed(key, scan, mods);
        boolean canvasKeys = sel != null && view != null;
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> { if (canvasKeys) { view.panX += KEY_PAN; NodeCanvas.clamp(view, shape.layout, lay.canvas); return true; } }
            case GLFW.GLFW_KEY_RIGHT -> { if (canvasKeys) { view.panX -= KEY_PAN; NodeCanvas.clamp(view, shape.layout, lay.canvas); return true; } }
            case GLFW.GLFW_KEY_UP -> { if (canvasKeys) { view.panY += KEY_PAN; NodeCanvas.clamp(view, shape.layout, lay.canvas); return true; } }
            case GLFW.GLFW_KEY_DOWN -> { if (canvasKeys) { view.panY -= KEY_PAN; NodeCanvas.clamp(view, shape.layout, lay.canvas); return true; } }
            case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> { if (canvasKeys) { NodeCanvas.step(view, shape.layout, lay.canvas, true); return true; } }
            case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> { if (canvasKeys) { NodeCanvas.step(view, shape.layout, lay.canvas, false); return true; } }
            case GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_HOME -> { if (canvasKeys) { NodeCanvas.fit(view, shape.layout, lay.canvas); return true; } }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
                if (getFocused() == null && selNode >= 0) { buySelected(); return true; }       // a focused button keeps Enter and Space for itself
            }
            default -> { }
        }
        return super.keyPressed(key, scan, mods);
    }

    // ------------------------------------------------------------------ drawing: the background (panels and wells, under the widgets)
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        int accent = sel != null && unlocked ? sel.color() & 0xFFFFFF : UiTheme.CYAN & 0xFFFFFF;
        if (TreeSkin.anime()) {
            UiPanel.scrim(g, width, height, 0.9f);
            MagicCircle.draw(g, lay.panel.x() + lay.panel.w() / 2f, lay.panel.y() + lay.panel.h() / 2f, Math.min(width, height) * 0.62f, now, UiTheme.VIOLET, 0.20f,
                    MagicCircle.CALM, 1f);
            UiPanel.panelScan(g, lay.panel.x(), lay.panel.y(), lay.panel.w(), lay.panel.h(), accent, 0.9f, 1f, now);
        } else {
            renderTransparentBackground(g);
            TreeSkin.panel(g, lay.panel, accent, 0.9f);
        }
        TreeSkin.well(g, lay.routes, UiTheme.CYAN & 0xFFFFFF);
        if (sel != null && view != null) NodeCanvas.drawBackground(g, lay.canvas, sel.color() & 0xFFFFFF, unlocked, view, now);
        else TreeSkin.well(g, lay.canvas, UiTheme.CYAN & 0xFFFFFF);
        TreeSkin.well(g, lay.detail, accent);
    }

    // ------------------------------------------------------------------ drawing: the content (over the widgets)
    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        refresh(mx, my);
        super.render(g, mx, my, partial);                                // renderBackground, then the three buttons
        Font font = this.font;
        drawHeader(g, font);
        if (player != null) list.draw(g, font, lay.routeList, player, routeId, now, mx, my);
        if (sel != null && view != null) {
            drawStrip(g, font);
            NodeCanvas.draw(g, font, new NodeCanvas.Frame(lay.canvas, shape, view, states, sel.color() & 0xFFFFFF, unlocked, selNode, hoverNode, now));
            NodeCanvas.drawChips(g, font, lay, mx, my);
            detail.update(font, lay.detailText.w(), new DetailPanel.Subject(sel, selNode >= 0 ? sel.nodes().get(selNode) : null, selNode >= 0 ? states[selNode] : null, player));
            detail.draw(g, font, lay.detailText, unlocked ? sel.color() & 0xFFFFFF : TreeSkin.SEALED & 0xFFFFFF);
        } else {
            drawNoRoutes(g, font);
        }
        drawFooter(g, font);
        List<FormattedCharSequence> tip = tooltip(font, mx, my);
        if (tip != null && !tip.isEmpty()) g.renderTooltip(font, tip, mx, my);
    }

    private void drawHeader(GuiGraphics g, Font font) {
        Rect h = lay.header;
        Component title = Component.literal(SkillTreeLang.str(SkillTreeLang.TITLE).toUpperCase(Locale.ROOT)).withStyle(ChatFormatting.BOLD);
        g.drawString(font, title, h.x() + 4, h.y() + 6, TreeSkin.gold(), false);
        int titleEnd = h.x() + 4 + font.width(title);
        String spText = SkillTreeLang.str(SkillTreeLang.SP, sp);
        int spW = font.width(spText) + 11;
        int spX = lay.closeButton.x() - 10 - spW;
        TreeSkin.gem(g, spX, h.y() + 6, UiMath.mix(UiTheme.CYAN, UiTheme.WHITE, 0.15f));
        g.drawString(font, spText, spX + 11, h.y() + 6, sp > 0 ? TreeSkin.text() : TreeSkin.dim(), false);
        int cx = titleEnd + 14, room = spX - 10 - cx;
        if (room > 24) {
            SkillRoute active = player == null || player.activeClass().isEmpty() ? null : RouteRegistry.byClass(player.activeClass());
            String name = player == null || player.activeClass().isEmpty() ? SkillTreeLang.str(SkillTreeLang.CLASS_NONE)
                    : active != null ? active.name() : TreeText.prettify(player.activeClass());
            g.drawString(font, fit(font, SkillTreeLang.str(SkillTreeLang.CLASS, name), room), cx, h.y() + 6, active != null ? TreeSkin.opaque(active.color()) : TreeSkin.dim(), false);
        }
    }

    private void drawStrip(GuiGraphics g, Font font) {
        Rect s = lay.strip;
        int y = s.y() + (s.h() - 8) / 2;
        if (!unlocked) {                                                 // a sealed route: the padlock and what it needs
            TreeSkin.padlock(g, s.x() + 5, s.y() + (s.h() - 9) / 2, TreeSkin.opaque(TreeSkin.SEALED));
            String text = SkillTreeLang.str(SkillTreeLang.REQUIRES, sel.bookName());
            g.drawString(font, fit(font, text, lay.zoomOut.x() - 6 - (s.x() + 17)), s.x() + 17, y, TreeSkin.text(), false);
            return;
        }
        boolean active = !sel.classId().isEmpty() && sel.classId().equals(player.activeClass());
        int room = lay.zoomOut.x() - 6 - (s.x() + 6);
        g.drawString(font, fit(font, sel.name(), room), s.x() + 6, y - 4, TreeSkin.opaque(sel.color()), false);
        g.drawString(font, fit(font, SkillTreeLang.str(active ? SkillTreeLang.STATE_ACTIVE : SkillTreeLang.STATE_UNLOCKED).toUpperCase(Locale.ROOT), room), s.x() + 6, y + 5,
                active ? TreeSkin.gold() : TreeSkin.good(), false);
    }

    private void drawNoRoutes(GuiGraphics g, Font font) {
        Rect c = lay.canvas;
        int cx = c.x() + c.w() / 2, y = c.y() + c.h() / 2 - 18;
        g.drawCenteredString(font, SkillTreeLang.str(SkillTreeLang.NO_ROUTES), cx, y, TreeSkin.gold());
        for (FormattedCharSequence line : font.split(SkillTreeLang.tr(SkillTreeLang.NO_ROUTES_HINT), Math.max(40, c.w() - 20))) {
            y += font.lineHeight + 2;
            g.drawCenteredString(font, line, cx, y + 6, TreeSkin.dim());
        }
    }

    private void drawFooter(GuiGraphics g, Font font) {
        Rect f = lay.footer;
        String text;
        int color;
        if (nowMs < noticeUntil && !notice.isEmpty()) {
            text = notice;
            color = noticeColor;
        } else if (!SkillTreeApi.isEnabled()) {                          // the server switched the Skill Tree off: nothing can be bought, and the footer says why
            text = SkillTreeLang.str(SkillTreeLang.OFF);
            color = TreeSkin.bad();
        } else {
            String longHint = SkillTreeLang.str(SkillTreeLang.HINT_LONG);
            text = font.width(longHint) <= f.w() - 4 ? longHint : SkillTreeLang.str(SkillTreeLang.HINT_SHORT);
            color = TreeSkin.faint();
        }
        g.drawString(font, fit(font, text, f.w() - 4), f.x() + 2, f.y() + 1, color, false);
    }

    /** The text shortened with "..." so that it fits {@code w} pixels. */
    private static String fit(Font font, String text, int w) {
        if (font.width(text) <= w) return text;
        String cut = font.plainSubstrByWidth(text, Math.max(0, w - font.width("...")));
        return cut.stripTrailing() + "...";
    }

    // ------------------------------------------------------------------ tooltips
    private List<FormattedCharSequence> tooltip(Font font, int mx, int my) {
        if (lay.zoomOut.contains(mx, my)) return lines(font, 160, SkillTreeLang.tr(SkillTreeLang.ZOOM_OUT_TIP));
        if (lay.zoomIn.contains(mx, my)) return lines(font, 160, SkillTreeLang.tr(SkillTreeLang.ZOOM_IN_TIP));
        if (lay.zoomFit.contains(mx, my)) return lines(font, 160, SkillTreeLang.tr(SkillTreeLang.FIT_TIP));
        if (sel != null && hoverNode >= 0) {
            SkillNode node = sel.nodes().get(hoverNode);
            NodeState st = states[hoverNode];
            List<FormattedCharSequence> out = new ArrayList<>(lines(font, 180, Component.literal(node.name()).withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD)));
            if (!unlocked) {
                out.addAll(lines(font, 180, SkillTreeLang.tr(SkillTreeLang.NODE_SEALED).withStyle(ChatFormatting.GRAY)));
                return out;
            }
            out.addAll(lines(font, 180, (st.status() == NodeStatus.MAXED ? SkillTreeLang.tr(SkillTreeLang.LEVEL_MAX, st.level()) : SkillTreeLang.tr(SkillTreeLang.LEVEL, st.level(), st.max()))
                    .withStyle(ChatFormatting.AQUA)));
            out.addAll(lines(font, 180, SkillTreeLang.tr(DetailPanel.stateKey(st.status())).withStyle(ChatFormatting.GRAY)));
            if (!node.desc().isEmpty()) out.addAll(lines(font, 180, Component.literal(node.desc())));
            if (st.status() != NodeStatus.MAXED) out.addAll(lines(font, 180, SkillTreeLang.tr(SkillTreeLang.PRICE_NEXT, st.price()).withStyle(sp >= st.price() ? ChatFormatting.GREEN : ChatFormatting.RED)));
            return out;
        }
        SkillRoute r = player == null ? null : list.routeAt(mx, my, lay.routeList);
        if (r != null) {
            List<FormattedCharSequence> out = new ArrayList<>(lines(font, 190, Component.literal(r.name()).withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD)));
            if (!r.desc().isEmpty()) out.addAll(lines(font, 190, Component.literal(r.desc())));
            if (!player.hasRoute(r.id())) out.addAll(lines(font, 190, SkillTreeLang.tr(SkillTreeLang.REQUIRES, r.bookName()).withStyle(ChatFormatting.RED)));
            return out;
        }
        Rect spRect = spRect(font);
        if (spRect.contains(mx, my)) {
            List<FormattedCharSequence> out = new ArrayList<>(lines(font, 190, SkillTreeLang.tr(SkillTreeLang.SP_TIP_HAVE, sp).withStyle(ChatFormatting.AQUA)));
            if (player != null) out.addAll(lines(font, 190, SkillTreeLang.tr(SkillTreeLang.SP_TIP_SPENT, player.spSpent()).withStyle(ChatFormatting.GRAY)));
            return out;
        }
        return null;
    }

    private Rect spRect(Font font) {
        int spW = font.width(SkillTreeLang.str(SkillTreeLang.SP, sp)) + 11;
        return new Rect(lay.closeButton.x() - 10 - spW - 2, lay.header.y(), spW + 4, lay.header.h());
    }

    private static List<FormattedCharSequence> lines(Font font, int w, Component c) { return font.split(c, w); }
}
