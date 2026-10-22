package com.newuniverse.nusmp.client.ui.move;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.Command;
import com.newuniverse.nusmp.client.ui.GuiScale;
import com.newuniverse.nusmp.client.ui.move.GuiMoveLayout.State;
import com.newuniverse.nusmp.client.ui.move.GuiMoveMath.Place;
import com.newuniverse.nusmp.client.ui.move.GuiMoveMath.Rect;
import com.newuniverse.nusmp.client.ui.move.GuiMoveRegistry.Element;
import com.newuniverse.nusmp.client.ui.scale.GuiScaleMath;
import com.newuniverse.nusmp.client.ui.scale.GuiScaleRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: the owner asked that EVERY custom on-screen GUI of the mod can be moved by the player, the way the Solo Leveling System window already could. This is the base
 * every GUI goes through, client only. <b>How a GUI joins, in five lines:</b> add one {@link GuiMoveRegistry} entry (id, name, default place and size); in its draw code
 * <pre>
 *   if (GuiMove.hidden(ID)) return;
 *   boolean moved = GuiMove.begin(g, ID, defX, defY, w, h, pivotX, pivotY);   // where it would be drawn anyway, its size, the point the GUI Size grows it from
 *   try { ...draw exactly as before, at defX/defY... } finally { GuiMove.end(g, moved); }
 * </pre>
 * and for a mouse-driven panel nothing at all (its screen is moved by the screen hooks of {@link GuiScale}; a component that hit-tests by hand asks
 * {@link #toLocalX}/{@link #toLocalY}).
 *
 * <p><b>Nothing moves unless the player moves it:</b> an element without a saved place is drawn exactly as before (the old GUI Size behaviour, bit for bit). A moved
 * element is drawn at {@code total = its own size x the GUI Size size} (one product, applied once, see {@link GuiMoveMath#total}) with its corner at the saved place.
 * Edit mode ({@link GuiMoveScreen}) is opened by the {@link #KEY} (default F9, rebindable under Controls) or {@code /nusmp gui move}; {@code /nusmp gui reset} puts every
 * GUI back. The saved layout is {@code nusmp-gui-layout-client.toml} ({@link GuiMoveOptions}), read once when it loads.
 *
 * <p><b>Cost.</b> A frame costs one map lookup and a few field writes per visible element, plus the pose push of a moved one; nothing is allocated and no string is built.
 * Edit mode draws only while its screen is open.
 */
public final class GuiMove {
    private GuiMove() {}

    /** Open edit mode. Default F9 (next to the GUI Size key F8); rebind it under Controls > Multiverse of Anime. */
    public static final KeyMapping KEY = new KeyMapping("key.nusmp.gui_move", InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_F9, "key.categories.nusmp");

    private static final String PREFIX = "com.newuniverse.nusmp.";
    private static final Map<Class<?>, Object> PANELS = new HashMap<>();
    private static final Object NONE = new Object();
    private static long tick;
    private static boolean openRequested;

    /** Call once from the client section of the mod constructor: the layout config, the key, the command and the hooks that read the file. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, GuiMoveOptions.SPEC, "nusmp-gui-layout-client.toml");
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(KEY));
        modBus.addListener((ModConfigEvent.Loading e) -> GuiMoveOptions.loadAll());
        modBus.addListener((ModConfigEvent.Reloading e) -> GuiMoveOptions.loadAll());
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> onTick());
        NeoForge.EVENT_BUS.addListener(GuiMove::registerCommands);
    }

    private static void onTick() {
        tick++;
        Minecraft mc = Minecraft.getInstance();
        boolean pressed = false;
        while (KEY.consumeClick()) pressed = true;
        if ((pressed || openRequested) && mc.player != null && mc.screen == null) {
            openRequested = false;
            mc.setScreen(new GuiMoveScreen(null));
        }
        if (mc.player == null) openRequested = false;
    }

    /** {@code /nusmp gui move} opens edit mode (a flag the next tick picks up, so the chat screen closing cannot swallow it); {@code /nusmp gui reset} puts every GUI back. */
    private static void registerCommands(RegisterClientCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("nusmp").then(Commands.literal("gui")
                .then(Commands.literal("move").executes(c -> {
                    openRequested = true;
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("reset").executes(c -> {
                    resetAll();
                    c.getSource().sendSuccess(() -> Component.translatableWithFallback("gui_move.cmd.reset", "Every GUI is back in its normal place.").withStyle(ChatFormatting.GOLD), false);
                    return Command.SINGLE_SUCCESS;
                }))));
    }

    /** The client tick count the "seen" stamps of the elements are compared with. */
    public static long tick() { return tick; }

    private static boolean editing;

    /** Is edit mode open? HUD layers that stay quiet while a screen is open draw anyway then, so the player sees the real element move. */
    public static boolean editing() { return editing; }

    static void setEditing(boolean on) { editing = on; }

    // ================================================================ the API of an element
    /** Is the element switched off by the player? Check it first in the draw code and draw nothing when true. */
    public static boolean hidden(String id) {
        Element e = GuiMoveRegistry.byId(id);
        return e != null && e.hidden();
    }

    /** Has the player placed this element? */
    public static boolean moved(String id) {
        Element e = GuiMoveRegistry.byId(id);
        return e != null && e.placed();
    }

    /**
     * Starts the drawing of an element and returns true when a pose was pushed (the caller then calls {@link #end}). The element draws exactly as before, at ({@code defX},
     * {@code defY}) with the size {@code w x h} (its own, unscaled pixels). Not moved: the GUI Size size grows it from ({@code pivotX}, {@code pivotY}) as {@code GuiScale.begin}
     * always did. Moved: it is drawn at the saved place at {@code total} size. Either way the GUI Size size is applied here, once: the caller must not call
     * {@code GuiScale.begin} for the same id. An id that is not registered behaves as {@code GuiScale.begin}.
     */
    public static boolean begin(GuiGraphics g, String id, int defX, int defY, int w, int h, float pivotX, float pivotY) {
        Element e = GuiMoveRegistry.byId(id);
        if (e == null) return GuiScale.begin(g, id, pivotX, pivotY);
        return begin(g, e, defX, defY, w, h, pivotX, pivotY);
    }

    /** {@link #begin} for an element whose size grows from its top-left corner. */
    public static boolean apply(GuiGraphics g, String id, int defX, int defY, int w, int h) { return begin(g, id, defX, defY, w, h, defX, defY); }

    /** Ends what {@link #begin} started. */
    public static void end(GuiGraphics g, boolean pushed) {
        if (pushed) g.pose().popPose();
    }

    static boolean begin(GuiGraphics g, Element e, int defX, int defY, int w, int h, float pivotX, float pivotY) {
        note(e, defX, defY, w, h);
        float gs = guiScale(e);
        e.gs = gs;
        State st = e.state;
        if (st == null || !st.placed) {
            e.t = gs;
            e.drawnX = pivotX + (defX - pivotX) * gs;
            e.drawnY = pivotY + (defY - pivotY) * gs;
            if (GuiScaleMath.isIdentity(gs)) return false;
            GuiScale.around(g, pivotX, pivotY, gs);
            return true;
        }
        float t = GuiMoveMath.total(st.scale, gs);
        double left = GuiMoveMath.leftGui(st.place, g.guiWidth(), t, w, e.margin), top = GuiMoveMath.topGui(st.place, g.guiHeight(), t, h, e.margin);
        e.t = t;
        e.drawnX = left;
        e.drawnY = top;
        g.pose().pushPose();
        g.pose().translate(left, top, 0.0);
        g.pose().scale(t, t, 1f);
        g.pose().translate(-defX, -defY, 0.0);
        return true;
    }

    /**
     * For an element drawn inside a HUD layer that the GUI Size layer hook wraps: reports where it is drawn this frame (its default rectangle, own pixels). The hook uses
     * the numbers of the last frame to move the NEXT one, so a layer whose size changes lags by one frame. Costs one lookup and five field writes.
     */
    public static void report(String id, int defX, int defY, int w, int h) {
        Element e = GuiMoveRegistry.byId(id);
        if (e != null) note(e, defX, defY, w, h);
    }

    /**
     * For an element that places itself from the store (the System window): reports the rectangle it was drawn at, {@code left}/{@code top} in GUI pixels, {@code wOwn} x
     * {@code hOwn} in its own pixels, {@code total} the scale it was drawn at and {@code guiScale} the part of it that came from the GUI Size sliders (the rest is the
     * player's own size).
     */
    public static void reportDrawn(String id, double left, double top, int wOwn, int hOwn, float total, float guiScale) {
        Element e = GuiMoveRegistry.byId(id);
        if (e == null) return;
        note(e, (int) left, (int) top, wOwn, hOwn);
        e.t = total;
        e.gs = guiScale;
        e.drawnX = left;
        e.drawnY = top;
    }

    private static void note(Element e, int defX, int defY, int w, int h) {
        e.seenTick = tick;
        e.dw = w;
        e.dh = h;
        e.defX = defX;
        e.defY = defY;
    }

    private static float guiScale(Element e) {
        return e.scaleEntry == null ? 1f : (float) GuiScale.requested(e.scaleEntry);
    }

    // ================================================================ the mouse of a moved element
    /** A mouse x of the screen in the space the element draws in (its default coordinates), when it has been moved; unchanged when it has not. */
    public static double toLocalX(String id, double mouseX) {
        Element e = GuiMoveRegistry.byId(id);
        return e == null || !e.placed() ? mouseX : GuiMoveMath.toLocal(mouseX, e.drawnX, e.defX, e.t);
    }

    /** A mouse y of the screen in the space the element draws in. */
    public static double toLocalY(String id, double mouseY) {
        Element e = GuiMoveRegistry.byId(id);
        return e == null || !e.placed() ? mouseY : GuiMoveMath.toLocal(mouseY, e.drawnY, e.defY, e.t);
    }

    /** A mouse position as a point (allocates one: for clicks, not for frames). */
    public record Point(double x, double y) {}

    public static Point toLocal(String id, double mouseX, double mouseY) { return new Point(toLocalX(id, mouseX), toLocalY(id, mouseY)); }

    // ================================================================ screens
    /** What the screen hooks need: the screen is drawn as {@code translate(ox, oy) scale(t)}, and a mouse position goes back with {@code (m - o) / t}. */
    public static final class Xf {
        public double t = 1.0, ox, oy;
        /** The player placed this screen: the transform replaces the centred GUI Size scaling. */
        public boolean placed;
    }

    /** The movable panel a mod screen belongs to (its class or the nearest mod superclass), or null. Cached per class. */
    public static Element panelOf(Screen screen) {
        if (screen == null) return null;
        Object cached = PANELS.get(screen.getClass());
        if (cached == null) {
            cached = NONE;
            for (Class<?> c = screen.getClass(); c != null && c.getName().startsWith(PREFIX); c = c.getSuperclass()) {
                Element e = GuiMoveRegistry.byClass(c.getName());
                if (e != null) { cached = e; break; }
            }
            PANELS.put(screen.getClass(), cached);
        }
        return cached == NONE ? null : (Element) cached;
    }

    /**
     * Fills {@code out} with the transform of a screen whose panel the player placed and returns true; false when the screen is not a movable panel or was never moved
     * (the GUI Size centred scaling then applies as before). {@code gsEff} is the size {@code GuiScale} draws the screen at (fit-to-window included): it multiplies with
     * the player's own size.
     */
    public static boolean screenXf(Screen s, double gsEff, Xf out) {
        Element e = panelOf(s);
        if (e == null) return false;
        int w, h;
        double defX, defY;
        if (s instanceof AbstractContainerScreen<?> c) {
            w = c.getXSize();
            h = c.getYSize();
            defX = c.getGuiLeft();
            defY = c.getGuiTop();
        } else {
            w = e.defW;
            h = e.defH;
            GuiScaleRegistry.Entry en = e.scaleEntry;
            if (en != null && en.designW() > 0 && en.designH() > 0) { w = en.designW(); h = en.designH(); }
            defX = (s.width - w) / 2.0;
            defY = (s.height - h) / 2.0;
        }
        note(e, (int) defX, (int) defY, w, h);
        e.gs = (float) gsEff;
        State st = e.state;
        if (st == null || !st.placed) return false;
        float t = GuiMoveMath.total(st.scale, (float) gsEff);
        double left = GuiMoveMath.leftGui(st.place, s.width, t, w, e.margin), top = GuiMoveMath.topGui(st.place, s.height, t, h, e.margin);
        e.t = t;
        e.drawnX = left;
        e.drawnY = top;
        out.t = t;
        out.ox = left - defX * t;
        out.oy = top - defY * t;
        out.placed = true;
        return true;
    }

    // ================================================================ the layer hook (called by GuiScale)
    /** The result of {@link #layerPre}: nothing to do, a pose was pushed (pop it after the layer), or the layer must not be drawn. */
    public static final int LAYER_NONE = 0, LAYER_PUSHED = 1, LAYER_HIDDEN = 2;

    /**
     * Called before a HUD layer of the mod is drawn. A hidden element returns {@link #LAYER_HIDDEN} (the hook cancels the layer); a placed one gets its pose pushed from the
     * rectangle it reported on the last frame ({@link #LAYER_PUSHED}); any other returns {@link #LAYER_NONE} and the plain GUI Size scaling applies.
     */
    public static int layerPre(GuiGraphics g, String id) {
        Element e = GuiMoveRegistry.byId(id);
        if (e == null) return LAYER_NONE;
        State st = e.state;
        if (st == null) return LAYER_NONE;
        if (st.hidden) return LAYER_HIDDEN;
        if (!st.placed || e.seenTick == GuiMoveRegistry.NEVER) return LAYER_NONE;
        float gs = guiScale(e);
        e.gs = gs;
        float t = GuiMoveMath.total(st.scale, gs);
        double left = GuiMoveMath.leftGui(st.place, g.guiWidth(), t, e.dw, e.margin), top = GuiMoveMath.topGui(st.place, g.guiHeight(), t, e.dh, e.margin);
        e.t = t;
        e.drawnX = left;
        e.drawnY = top;
        g.pose().pushPose();
        g.pose().translate(left, top, 0.0);
        g.pose().scale(t, t, 1f);
        g.pose().translate(-e.defX, -e.defY, 0.0);
        return LAYER_PUSHED;
    }

    // ================================================================ edit operations (used by the edit screen and the reset command)
    /** The state of an element, created (not yet placed) when the player had not touched it. */
    static State touch(Element e) {
        State s = e.state;
        if (s == null) {
            s = new State();
            e.state = s;
        }
        return s;
    }

    /** Stores the element's state in the config (in memory) and drops a state that no longer says anything. */
    static void commit(Element e) {
        if (e.state != null && e.state.isDefault()) e.state = null;
        GuiMoveOptions.store(e);
    }

    /** Puts one element back where its own code puts it (the System window also forgets the offsets of its old config). */
    public static void reset(Element e) {
        e.state = null;
        if (e.id.equals("system_hud")) com.newuniverse.nusmp.client.mega.system.SystemClientConfig.forgetLegacyPlace();
        GuiMoveOptions.store(e);
    }

    /** Puts every element back and writes the file. */
    public static void resetAll() {
        for (Element e : GuiMoveRegistry.all()) reset(e);
        GuiMoveOptions.save();
    }

    /** Was the element drawn within the last second? */
    public static boolean visibleNow(Element e) {
        return e.seenTick != GuiMoveRegistry.NEVER && tick - e.seenTick < 20L;
    }

    /** Edit mode only: refreshes the GUI Size part of an element that is not being drawn, so its rectangle and the wheel use the size it would have. */
    static void syncGs(Element e) {
        if (!visibleNow(e)) e.gs = guiScale(e);
    }

    /** The size (own pixels) the edit screen uses for an element: what it reported last, else its registry default. */
    static int widthOf(Element e) { return e.seenTick == GuiMoveRegistry.NEVER ? e.defW : e.dw; }

    static int heightOf(Element e) { return e.seenTick == GuiMoveRegistry.NEVER ? e.defH : e.dh; }

    /**
     * The rectangle of an element on a screen of {@code guiW x guiH} as it is (or would be) drawn now, in GUI pixels: its saved place when placed; else where its code
     * draws it (what it reported on the last frame, or its registry default when it was never seen).
     */
    public static Rect rectOf(Element e, int guiW, int guiH) {
        State st = e.state;
        int w = widthOf(e), h = heightOf(e);
        if (e.how == GuiMoveRegistry.How.SELF && e.seenTick != GuiMoveRegistry.NEVER) return new Rect(e.drawnX, e.drawnY, w * (double) e.t, h * (double) e.t);     // it places itself: what it drew is the truth
        if (st != null && st.placed) return GuiMoveMath.rect(st.place, guiW, guiH, GuiMoveMath.total(st.scale, e.gs), w, h, e.margin);
        if (e.seenTick != GuiMoveRegistry.NEVER && e.kind == GuiMoveRegistry.Kind.HUD) {
            if (e.how == GuiMoveRegistry.How.LAYER && e.scaleEntry != null) {
                float gs = e.gs;
                double px = e.scaleEntry.anchor().pivotX(guiW), py = e.scaleEntry.anchor().pivotY(guiH);
                return new Rect(px + (e.defX - px) * gs, py + (e.defY - py) * gs, w * (double) gs, h * (double) gs);
            }
            return new Rect(e.drawnX, e.drawnY, w * (double) e.t, h * (double) e.t);
        }
        return GuiMoveMath.rect(e.defPlace, guiW, guiH, 1f, w, h, e.margin);
    }

    /** Places an element so that its top-left corner is at ({@code left}, {@code top}) GUI pixels (held on the screen). */
    static void moveTo(Element e, int guiW, int guiH, double left, double top) {
        State st = touch(e);
        float t = GuiMoveMath.total(st.placed ? st.scale : 1f, e.gs);
        if (e.how == GuiMoveRegistry.How.SELF) {                                          // the System window's layout keeps its panel fully on the screen
            left = inside(left, widthOf(e) * (double) t, guiW);
            top = inside(top, heightOf(e) * (double) t, guiH);
        }
        st.place = GuiMoveMath.placeFromGui(guiW, guiH, t, widthOf(e), heightOf(e), e.margin, left, top);
        st.placed = true;
        commit(e);
    }

    /** Gives an element the size {@code moveScale} and keeps its corner at ({@code left}, {@code top}). */
    static void resizeTo(Element e, int guiW, int guiH, float moveScale, double left, double top) {
        State st = touch(e);
        st.scale = GuiMoveMath.clampScale(moveScale);
        float t = GuiMoveMath.total(st.scale, e.gs);
        if (e.how == GuiMoveRegistry.How.SELF) {
            left = inside(left, widthOf(e) * (double) t, guiW);
            top = inside(top, heightOf(e) * (double) t, guiH);
        }
        st.place = GuiMoveMath.placeFromGui(guiW, guiH, t, widthOf(e), heightOf(e), e.margin, left, top);
        st.placed = true;
        commit(e);
    }

    /** A position held so that the whole element is on the screen (the low end wins when it is bigger than the screen). */
    private static double inside(double pos, double size, double screen) { return Math.max(0.0, Math.min(Math.max(0.0, screen - size), pos)); }

    /** The player's own size of an element (1 when never sized). */
    static float scaleOf(Element e) { State s = e.state; return s != null && s.placed ? s.scale : 1f; }

    static void setHidden(Element e, boolean hidden) {
        State st = touch(e);
        st.hidden = hidden;
        commit(e);
    }

    /** The place of an element for the System window's config: the saved one, or null when the player never placed it. */
    public static Place placeOrNull(String id) {
        Element e = GuiMoveRegistry.byId(id);
        State s = e == null ? null : e.state;
        return s != null && s.placed ? s.place : null;
    }

    /** The own size of an element for the System window's config, or NaN when never placed. */
    public static float scaleOrNaN(String id) {
        Element e = GuiMoveRegistry.byId(id);
        State s = e == null ? null : e.state;
        return s != null && s.placed ? s.scale : Float.NaN;
    }

    /** Saves a place (and optionally a size) for an element from outside the edit screen: the System window's own edit screen. */
    public static void storePlace(String id, Place place, float scale) {
        Element e = GuiMoveRegistry.byId(id);
        if (e == null) return;
        State st = touch(e);
        st.placed = true;
        st.place = GuiMoveMath.sanitize(place);
        st.scale = GuiMoveMath.clampScale(scale);
        commit(e);
    }
}
